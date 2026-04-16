package dev.sfcore.managers;

import dev.sfcore.api.StatType;
import dev.sfcore.database.StatDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ManaManager {

    private final Plugin plugin;
    private final StatDatabase db;
    private final StatManager statManager;
    private final Map<UUID, Double> manaCache = new HashMap<>();

    private final boolean enabled;
    private final double baseMax;
    private final double baseRegen;
    private final double intToMax;
    private final double intToRegen;
    private final boolean startFull;

    public ManaManager(Plugin plugin, StatDatabase db, StatManager statManager) {
        this.plugin = plugin;
        this.db = db;
        this.statManager = statManager;
        this.enabled = plugin.getConfig().getBoolean("mana.enabled", true);
        this.baseMax = Math.max(1.0D, plugin.getConfig().getDouble("mana.base-max", 100.0D));
        this.baseRegen = Math.max(0.0D, plugin.getConfig().getDouble("mana.base-regen", 2.0D));
        this.intToMax = plugin.getConfig().getDouble("mana.int-to-max", 5.0D);
        this.intToRegen = plugin.getConfig().getDouble("mana.int-to-regen", 0.1D);
        this.startFull = plugin.getConfig().getBoolean("mana.start-full", true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void loadPlayer(Player player) {
        if (!enabled || player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        double max = getMaxMana(player);
        Double persisted = db.loadMana(uuid);
        double current;
        if (persisted == null) {
            current = startFull ? max : 0.0D;
        } else {
            current = clamp(persisted, 0.0D, max);
        }
        manaCache.put(uuid, current);
        db.upsertMana(uuid, current);
    }

    public void saveAndUnload(UUID uuid) {
        if (!enabled || uuid == null) {
            return;
        }
        Double value = manaCache.remove(uuid);
        if (value != null) {
            db.upsertMana(uuid, value);
        }
    }

    public double getMana(Player player) {
        if (!enabled || player == null) {
            return 0.0D;
        }
        UUID uuid = player.getUniqueId();
        if (!manaCache.containsKey(uuid)) {
            loadPlayer(player);
        }
        return manaCache.getOrDefault(uuid, 0.0D);
    }

    public double getMaxMana(Player player) {
        if (!enabled || player == null) {
            return 0.0D;
        }
        double intTotal = statManager.getTotal(player, StatType.INT);
        double bonusMax = statManager.getTotal(player, StatType.MANA_MAX);
        return Math.max(1.0D, baseMax + (intTotal * intToMax) + bonusMax);
    }

    public double getManaRegenPerSecond(Player player) {
        if (!enabled || player == null) {
            return 0.0D;
        }
        double intTotal = statManager.getTotal(player, StatType.INT);
        double bonusRegen = statManager.getTotal(player, StatType.MANA_REGEN);
        return Math.max(0.0D, baseRegen + (intTotal * intToRegen) + bonusRegen);
    }

    public void setMana(Player player, double mana) {
        if (!enabled || player == null) {
            return;
        }
        double max = getMaxMana(player);
        double clamped = clamp(mana, 0.0D, max);
        manaCache.put(player.getUniqueId(), clamped);
        db.upsertMana(player.getUniqueId(), clamped);
    }

    public void addMana(Player player, double amount) {
        if (!enabled || player == null || amount == 0.0D) {
            return;
        }
        setMana(player, getMana(player) + amount);
    }

    public boolean spendMana(Player player, double amount) {
        if (!enabled || player == null) {
            return true;
        }
        double cost = Math.max(0.0D, amount);
        double current = getMana(player);
        if (current < cost) {
            return false;
        }
        setMana(player, current - cost);
        return true;
    }

    public void tickRegen() {
        if (!enabled) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            double regen = getManaRegenPerSecond(player);
            if (regen <= 0.0D) {
                continue;
            }
            addMana(player, regen);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
