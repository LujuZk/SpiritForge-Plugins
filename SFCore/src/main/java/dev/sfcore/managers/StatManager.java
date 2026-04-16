package dev.sfcore.managers;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatBonus;
import dev.sfcore.api.StatType;
import dev.sfcore.database.AsyncDatabaseExecutor;
import dev.sfcore.database.StatDatabase;
import dev.sfcore.util.CharacterSlotResolver;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import dev.sfcore.api.StatChangeEvent;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class StatManager {

    private static final Logger log = Logger.getLogger("SFCore");
    private final StatDatabase db;
    private final Map<UUID, List<StatBonus>> cache = new HashMap<>();

    public StatManager(StatDatabase db) {
        this.db = db;
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        int slot = CharacterSlotResolver.resolve(uuid);
        cache.put(uuid, new ArrayList<>()); // cache vacia temporal
        AsyncDatabaseExecutor async = SFCoreAPI.get().getAsyncExecutor();
        async.thenOnMain(async.supplyAsync(() -> db.loadBonuses(uuid, slot)), bonuses -> {
            cache.put(uuid, new ArrayList<>(bonuses));
            if (player.isOnline()) reapplyAll(player);
        });
    }

    public void saveAndUnload(UUID uuid) {
        // Bonuses are persisted immediately on addBonus/removeBonus; just clear cache.
        cache.remove(uuid);
    }

    public void saveAll() {
        // Nothing to flush — every mutation already hits the DB immediately.
    }

    /**
     * Recarga los bonuses del slot activo actual del jugador. Se llama al cambiar de
     * personaje (CharacterSelectEvent) — el slot nuevo se resuelve vía
     * {@link CharacterSlotResolver}, que ya refleja el estado post-switch.
     */
    public void reloadForActiveSlot(Player player) {
        UUID uuid = player.getUniqueId();
        int slot = CharacterSlotResolver.resolve(uuid);
        cache.put(uuid, new ArrayList<>()); // limpiar cache del slot viejo
        AsyncDatabaseExecutor async = SFCoreAPI.get().getAsyncExecutor();
        async.thenOnMain(async.supplyAsync(() -> db.loadBonuses(uuid, slot)), bonuses -> {
            cache.put(uuid, new ArrayList<>(bonuses));
            if (player.isOnline()) reapplyAll(player);
        });
    }

    public void addBonus(Player player, String source, StatType stat, double value) {
        UUID uuid = player.getUniqueId();
        int slot = CharacterSlotResolver.resolve(uuid);
        List<StatBonus> bonuses = cache.computeIfAbsent(uuid, u -> new ArrayList<>());
        bonuses.removeIf(b -> b.source().equals(source) && b.type() == stat);
        StatBonus bonus = new StatBonus(source, stat, value);
        bonuses.add(bonus);
        reapplyAll(player);
        SFCoreAPI.get().getAsyncExecutor().runAsync(() -> db.upsertBonus(uuid, slot, bonus));
    }

    public void removeBonus(Player player, String source) {
        UUID uuid = player.getUniqueId();
        int slot = CharacterSlotResolver.resolve(uuid);
        List<StatBonus> bonuses = cache.get(uuid);
        if (bonuses != null) bonuses.removeIf(b -> b.source().equals(source));
        reapplyAll(player);
        SFCoreAPI.get().getAsyncExecutor().runAsync(() -> db.deleteBonus(uuid, slot, source));
    }

    public void clearSource(Player player, String sourcePrefix) {
        UUID uuid = player.getUniqueId();
        int slot = CharacterSlotResolver.resolve(uuid);
        List<StatBonus> bonuses = cache.get(uuid);
        if (bonuses != null) bonuses.removeIf(b -> b.source().startsWith(sourcePrefix));
        reapplyAll(player);
        SFCoreAPI.get().getAsyncExecutor().runAsync(() -> db.deleteBySourcePrefix(uuid, slot, sourcePrefix));
    }

    public void addBonusesBulk(Player player, java.util.List<StatBonus> newBonuses) {
        UUID uuid = player.getUniqueId();
        int slot = CharacterSlotResolver.resolve(uuid);
        List<StatBonus> bonuses = cache.computeIfAbsent(uuid, u -> new ArrayList<>());
        for (StatBonus b : newBonuses) {
            bonuses.removeIf(existing -> existing.source().equals(b.source()) && existing.type() == b.type());
            bonuses.add(b);
        }
        reapplyAll(player);
        List<StatBonus> snapshot = List.copyOf(newBonuses);
        SFCoreAPI.get().getAsyncExecutor().runAsync(() -> db.upsertBonusesBatch(uuid, slot, snapshot));
    }

    public double getTotal(Player player, StatType stat) {
        List<StatBonus> bonuses = cache.get(player.getUniqueId());
        if (bonuses == null) return 0;
        return bonuses.stream()
                .filter(b -> b.type() == stat)
                .mapToDouble(StatBonus::value)
                .sum();
    }

    public Map<StatType, Double> getAllTotals(Player player) {
        Map<StatType, Double> totals = new EnumMap<>(StatType.class);
        List<StatBonus> bonuses = cache.get(player.getUniqueId());
        if (bonuses == null) return totals;
        for (StatBonus b : bonuses) {
            totals.merge(b.type(), b.value(), Double::sum);
        }
        return totals;
    }

    public void reapplyAll(Player player) {
        // 1. Remove all sfcore attribute modifiers
        for (StatType stat : StatType.values()) {
            if (stat.getMode() == StatType.ApplyMode.LISTENER) continue;
            AttributeInstance inst = player.getAttribute(stat.getAttribute());
            if (inst == null) continue;
            UUID modUuid = UUID.nameUUIDFromBytes(("sfcore:" + stat.getKey()).getBytes(StandardCharsets.UTF_8));
            inst.getModifiers().stream()
                    .filter(m -> m.getUniqueId().equals(modUuid))
                    .forEach(inst::removeModifier);
        }

        // 2. Apply ATTRIBUTE-based stats
        for (StatType stat : StatType.values()) {
            if (stat.getMode() == StatType.ApplyMode.LISTENER) continue;
            double total = getTotal(player, stat);
            if (total == 0) continue;
            AttributeInstance inst = player.getAttribute(stat.getAttribute());
            if (inst == null) continue;
            UUID modUuid = UUID.nameUUIDFromBytes(("sfcore:" + stat.getKey()).getBytes(StandardCharsets.UTF_8));
            AttributeModifier.Operation op = stat.getMode() == StatType.ApplyMode.ADD_SCALAR
                    ? AttributeModifier.Operation.ADD_SCALAR
                    : AttributeModifier.Operation.ADD_NUMBER;
            inst.addModifier(new AttributeModifier(modUuid, "sfcore:" + stat.getKey(), total, op));
        }

        // 3. MINING_SPEED → Haste potion effect
        double miningSpeed = getTotal(player, StatType.MINING_SPEED);
        player.removePotionEffect(PotionEffectType.HASTE);
        if (miningSpeed > 0) {
            int amplifier = (int) Math.floor(miningSpeed) - 1; // 1.0 → Haste I, 2.0 → Haste II
            amplifier = Math.max(0, amplifier);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, amplifier, true, false, false));
        }

        // 4. Disparar StatChangeEvent
        player.getServer().getPluginManager().callEvent(new StatChangeEvent(player, getAllTotals(player)));
    }
}
