package dev.sfcore.api;

import dev.sfcore.managers.StatManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SFCoreAPI {

    private static SFCoreAPI instance;
    private final StatManager manager;

    private SFCoreAPI(StatManager manager) {
        this.manager = manager;
    }

    public static SFCoreAPI get() {
        if (instance == null) throw new IllegalStateException("SFCore is not loaded");
        return instance;
    }

    public static void init(StatManager manager) {
        instance = new SFCoreAPI(manager);
    }

    public static void shutdown() {
        instance = null;
    }

    public void addBonus(Player player, String source, StatType stat, double value) {
        manager.addBonus(player, source, stat, value);
    }

    public void removeBonus(Player player, String source) {
        manager.removeBonus(player, source);
    }

    public void clearSource(Player player, String sourcePrefix) {
        manager.clearSource(player, sourcePrefix);
    }

    public double getTotal(Player player, StatType stat) {
        return manager.getTotal(player, stat);
    }

    public Map<StatType, Double> getAllTotals(Player player) {
        return manager.getAllTotals(player);
    }

    // ─── Integration Helpers ─────────────────────────────────────────

    public static boolean isAvailable() {
        return instance != null;
    }

    public static boolean isValidStatKey(String key) {
        return StatType.fromKey(key) != null;
    }

    public static Optional<StatType> findStatType(String key) {
        return Optional.ofNullable(StatType.fromKey(key));
    }

    /**
     * Agrega múltiples bonuses en bulk con un solo reapply al final.
     * Más eficiente que llamar addBonus() N veces.
     */
    public void addBonuses(Player player, List<StatBonus> bonuses) {
        manager.addBonusesBulk(player, bonuses);
    }
}
