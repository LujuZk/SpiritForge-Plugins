package dev.skilltree.managers;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatType;
import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.NodeState;
import dev.skilltree.models.PlayerSkillData;
import dev.skilltree.models.SkillGraph;
import dev.skilltree.models.SkillNode;
import dev.skilltree.models.SkillType;
import org.bukkit.entity.Player;

/**
 * Maneja los puntos de habilidad y el desbloqueo de nodos del grafo.
 */
public class SkillPointManager {

    private final SkillTreePlugin plugin;

    public SkillPointManager(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Puntos ──────────────────────────────────────────────────────────────

    public int getAvailablePoints(Player player, SkillType skill) {
        return plugin.getSkillManager().getData(player).getAvailablePoints(skill);
    }

    public void addPoint(Player player, SkillType skill, int amount) {
        plugin.getSkillManager().getData(player).addPoints(skill, amount);
    }

    // ─── Desbloqueo de nodos ─────────────────────────────────────────────────

    public UnlockResult tryUnlock(Player player, SkillType skill, String nodeId) {
        PlayerSkillData data   = plugin.getSkillManager().getData(player);
        SkillGraph graph       = plugin.getTreeManager().getTree(skill);

        if (graph == null) return UnlockResult.NO_TREE;

        SkillNode node = graph.getNode(nodeId);
        if (node == null) return UnlockResult.INVALID_NODE;

        NodeState state = graph.getNodeState(nodeId, data.getUnlockedNodes(skill));

        if (state == NodeState.UNLOCKED)            return UnlockResult.ALREADY_UNLOCKED;
        if (state == NodeState.LOCKED)              return UnlockResult.PREREQUISITES_NOT_MET;
        if (state == NodeState.EXCLUSIVE_BLOCKED)   return UnlockResult.BLOCKED_BY_EXCLUSIVE;

        // Verificar puntos
        if (data.getAvailablePoints(skill) < node.getCost()) return UnlockResult.NOT_ENOUGH_POINTS;

        // Desbloquear
        data.spendPoints(skill, node.getCost());
        data.unlockNode(skill, nodeId);

        // Registrar bonus en SFCore si el nodo tiene efecto
        if (node.getEffectType() != null && !node.getEffectType().isBlank()) {
            StatType stat = StatType.fromKey(node.getEffectType());
            if (stat != null) {
                String source = "sfskills:" + skill.getKey() + ":" + nodeId;
                SFCoreAPI.get().addBonus(player, source, stat, node.getEffectValue());
            }
        }

        return UnlockResult.SUCCESS;
    }

    // ─── Reset ───────────────────────────────────────────────────────────────

    public void resetTree(Player player, SkillType skill) {
        PlayerSkillData data = plugin.getSkillManager().getData(player);

        // Devolver los puntos gastados
        SkillGraph graph = plugin.getTreeManager().getTree(skill);
        if (graph != null) {
            int spent = data.getUnlockedNodes(skill).stream()
                    .mapToInt(nodeId -> {
                        SkillNode n = graph.getNode(nodeId);
                        return n != null ? n.getCost() : 0;
                    })
                    .sum();
            data.addPoints(skill, spent);
        }

        // Eliminar bonuses de este skill en SFCore
        SFCoreAPI.get().clearSource(player, "sfskills:" + skill.getKey() + ":");

        data.resetNodes(skill);
    }

    public enum UnlockResult {
        SUCCESS,
        NO_TREE,
        INVALID_NODE,
        ALREADY_UNLOCKED,
        PREREQUISITES_NOT_MET,
        BLOCKED_BY_EXCLUSIVE,
        NOT_ENOUGH_POINTS
    }
}