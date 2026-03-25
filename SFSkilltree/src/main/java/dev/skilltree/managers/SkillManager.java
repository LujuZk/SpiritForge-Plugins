package dev.skilltree.managers;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatType;
import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.PlayerSkillData;
import dev.skilltree.models.SkillGraph;
import dev.skilltree.models.SkillNode;
import dev.skilltree.models.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {

    private final SkillTreePlugin plugin;
    // Cache en memoria mientras el jugador está online
    private final Map<UUID, PlayerSkillData> cache = new HashMap<>();

    public SkillManager(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Cache ────────────────────────────────────────────────────────────────

    public PlayerSkillData getData(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(),
                uuid -> plugin.getDatabaseManager().loadPlayer(uuid));
    }

    public void loadPlayer(Player player) {
        cache.put(player.getUniqueId(),
                plugin.getDatabaseManager().loadPlayer(player.getUniqueId()));
        // Sync bonuses with SFCore 1 tick later so SFCore's MONITOR join handler fires first
        plugin.getServer().getScheduler().runTask(plugin, () -> syncSFCoreBonuses(player));
    }

    private void syncSFCoreBonuses(Player player) {
        if (!player.isOnline()) return;
        PlayerSkillData data = cache.get(player.getUniqueId());
        if (data == null) return;

        // Clear all previous sfskills bonuses and re-register from current unlocked nodes
        SFCoreAPI.get().clearSource(player, "sfskills:");
        for (SkillType skill : SkillType.values()) {
            SkillGraph graph = plugin.getTreeManager().getTree(skill);
            if (graph == null) continue;
            for (String nodeId : data.getUnlockedNodes(skill)) {
                SkillNode node = graph.getNode(nodeId);
                if (node == null || node.getEffectType() == null || node.getEffectType().isBlank()) continue;
                StatType stat = StatType.fromKey(node.getEffectType());
                if (stat == null) continue;
                SFCoreAPI.get().addBonus(player, "sfskills:" + skill.getKey() + ":" + nodeId, stat, node.getEffectValue());
            }
        }
    }

    public void saveAndUnload(Player player) {
        PlayerSkillData data = cache.remove(player.getUniqueId());
        if (data != null) {
            plugin.getDatabaseManager().savePlayer(data);
        }
    }

    public void saveAll() {
        cache.values().forEach(plugin.getDatabaseManager()::savePlayer);
    }

    // ─── XP y niveles ────────────────────────────────────────────────────────

    public void addXP(Player player, SkillType skill, double amount) {
        PlayerSkillData data = getData(player);
        double baseXP = plugin.getConfig().getDouble("xp-per-level", 100);
        double multiplier = plugin.getConfig().getDouble("xp-multiplier", 1.5);
        int maxLevel = plugin.getConfig().getInt("max-level", 50);

        if (data.getLevel(skill) >= maxLevel) return;

        data.addXP(skill, amount);

        int levelBefore = data.getLevel(skill);

        // Verificar si subió de nivel (puede subir varios a la vez)
        while (data.getLevel(skill) < maxLevel) {
            double required = data.getXPRequired(skill, baseXP, multiplier);
            if (data.getXP(skill) >= required) {
                data.setXP(skill, data.getXP(skill) - required);
                data.setLevel(skill, data.getLevel(skill) + 1);
            } else {
                break;
            }
        }

        int levelAfter = data.getLevel(skill);
        int levelsGained = levelAfter - levelBefore;

        if (levelsGained > 0) {
            // 1 punto por cada nivel subido
            data.addPoints(skill, levelsGained);
            notifyLevelUp(player, skill, levelAfter);
        }
    }

    private void notifyLevelUp(Player player, SkillType skill, int newLevel) {
        String msg = plugin.getConfig().getString("messages.level-up",
                        "&a¡Subiste a nivel &e{level}&a en &b{skill}&a!")
                .replace("{level}", String.valueOf(newLevel))
                .replace("{skill}", skill.getDisplayName())
                .replace("&a", "").replace("&e", "").replace("&b", "");

        player.sendMessage(Component.text("✨ " + msg, NamedTextColor.GREEN));
        player.sendActionBar(Component.text(
                skill.getDisplayName() + " → Nivel " + newLevel + "!", NamedTextColor.GOLD));
        player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    /**
     * Calcula el porcentaje de progreso hacia el siguiente nivel (0.0 - 1.0)
     */
    public double getProgress(Player player, SkillType skill) {
        PlayerSkillData data = getData(player);
        double baseXP = plugin.getConfig().getDouble("xp-per-level", 100);
        double multiplier = plugin.getConfig().getDouble("xp-multiplier", 1.5);
        double required = data.getXPRequired(skill, baseXP, multiplier);
        return Math.min(1.0, data.getXP(skill) / required);
    }

    public void resetPlayer(UUID uuid) {
        cache.remove(uuid);
        plugin.getDatabaseManager().resetPlayer(uuid);
    }
}