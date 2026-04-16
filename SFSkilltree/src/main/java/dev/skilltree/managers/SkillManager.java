package dev.skilltree.managers;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatType;
import dev.sfcore.database.AsyncDatabaseExecutor;
import dev.sfcore.util.CharacterSlotResolver;
import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.PlayerSkillData;
import dev.skilltree.models.SkillGraph;
import dev.skilltree.models.SkillNode;
import dev.skilltree.models.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SkillManager {

    private final SkillTreePlugin plugin;
    private final Map<UUID, PlayerSkillData> cache = new HashMap<>();
    private final Map<UUID, Integer> slotCache = new HashMap<>();
    private final Set<UUID> debugPlayers = new HashSet<>();

    public SkillManager(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Cache ────────────────────────────────────────────────────────────────

    public PlayerSkillData getData(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> {
            int slot = CharacterSlotResolver.resolve(uuid);
            slotCache.put(uuid, slot);
            return plugin.getDatabaseManager().loadPlayer(uuid, slot);
        });
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        int slot = CharacterSlotResolver.resolve(uuid);
        slotCache.put(uuid, slot);
        AsyncDatabaseExecutor async = SFCoreAPI.get().getAsyncExecutor();
        async.thenOnMain(async.supplyAsync(() -> plugin.getDatabaseManager().loadPlayer(uuid, slot)), data -> {
            cache.put(uuid, data);
            if (player.isOnline()) syncSFCoreBonuses(player);
        });
    }

    public void reloadForActiveSlot(Player player) {
        UUID uuid = player.getUniqueId();
        AsyncDatabaseExecutor async = SFCoreAPI.get().getAsyncExecutor();
        // Guardar datos del slot viejo async, luego cargar nuevo
        PlayerSkillData oldData = cache.get(uuid);
        Integer oldSlot = slotCache.get(uuid);

        int newSlot = CharacterSlotResolver.resolve(uuid);
        slotCache.put(uuid, newSlot);

        Runnable loadNew = () -> async.thenOnMain(
                async.supplyAsync(() -> plugin.getDatabaseManager().loadPlayer(uuid, newSlot)),
                data -> {
                    cache.put(uuid, data);
                    if (player.isOnline()) syncSFCoreBonuses(player);
                }
        );

        if (oldData != null && oldSlot != null) {
            PlayerSkillData snapshot = oldData.copy();
            CompletableFuture<Void> saveFuture = async.supplyAsync(() -> {
                plugin.getDatabaseManager().savePlayer(snapshot, oldSlot);
                return null;
            });
            async.thenOnMain(saveFuture, v -> loadNew.run());
        } else {
            loadNew.run();
        }
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
        UUID uuid = player.getUniqueId();
        PlayerSkillData data = cache.remove(uuid);
        Integer slot = slotCache.remove(uuid);
        if (data != null && slot != null) {
            PlayerSkillData snapshot = data.copy();
            SFCoreAPI.get().getAsyncExecutor().runAsync(
                    () -> plugin.getDatabaseManager().savePlayer(snapshot, slot));
        }
    }

    public void saveAll() {
        cache.forEach((uuid, data) -> {
            Integer slot = slotCache.get(uuid);
            if (slot != null) {
                plugin.getDatabaseManager().savePlayer(data, slot);
            }
        });
    }

    // ─── Debug ──────────────────────────────────────────────────────────────

    public boolean toggleDebug(Player player) {
        UUID uuid = player.getUniqueId();
        if (debugPlayers.contains(uuid)) {
            debugPlayers.remove(uuid);
            return false;
        } else {
            debugPlayers.add(uuid);
            return true;
        }
    }

    public boolean isDebug(Player player) {
        return debugPlayers.contains(player.getUniqueId());
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

        // Debug message
        if (isDebug(player)) {
            double required = data.getXPRequired(skill, baseXP, multiplier);
            player.sendMessage(Component.text(
                    "[DEBUG] +" + String.format("%.1f", amount) + " XP → "
                            + skill.getDisplayName()
                            + " (Total: " + String.format("%.1f", data.getXP(skill))
                            + "/" + String.format("%.0f", required)
                            + ", Lv " + levelAfter + ")",
                    NamedTextColor.GRAY));
        }

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
        int slot = slotCache.getOrDefault(uuid, CharacterSlotResolver.resolve(uuid));
        plugin.getDatabaseManager().resetPlayer(uuid, slot);
    }
}