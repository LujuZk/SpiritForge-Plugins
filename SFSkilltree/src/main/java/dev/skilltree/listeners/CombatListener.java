package dev.skilltree.listeners;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.SkillType;
import dev.skilltree.weapons.WeaponUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {

    private final SkillTreePlugin plugin;

    // Acumula el daño que cada jugador le hizo a cada entidad
    // Map< UUID del jugador, Map< UUID del mob, daño acumulado > >
    private final Map<UUID, Map<UUID, Double>> damageTracker = new HashMap<>();

    public CombatListener(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Trackear daño en tiempo real ────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player) return;

        Player attacker = getAttackingPlayer(event);
        if (attacker == null) return;

        // Solo trackear si el arma da XP
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (WeaponUtil.getWeaponSkill(weapon) == null &&
                !(event.getDamager() instanceof Arrow) &&
                !(event.getDamager() instanceof Trident)) return;

        // Daño real = no más que la HP actual del mob (no contar overkill)
        double realDamage = Math.min(event.getFinalDamage(), target.getHealth());

        damageTracker
                .computeIfAbsent(attacker.getUniqueId(), k -> new HashMap<>())
                .merge(target.getUniqueId(), realDamage, Double::sum);
    }

    // ─── Al morir el mob, calcular y dar XP ──────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (mob instanceof Player) return;

        SkillType weaponSkill = detectKillWeapon(killer, event);
        if (weaponSkill == null) {
            cleanTracker(killer.getUniqueId(), mob.getUniqueId());
            return;
        }

        double xp = calculateXP(killer, mob, weaponSkill);
        plugin.getSkillManager().addXP(killer, weaponSkill, xp);

        cleanTracker(killer.getUniqueId(), mob.getUniqueId());
    }

    // ─── Cálculo de XP ───────────────────────────────────────────────────────

    /**
     * Fórmula: XP = dañoHecho × (hpMaxima / 20) × multiplicadorArma
     *
     * - dañoHecho      → cuánto daño le hizo este jugador al mob (no overkill)
     * - hpMaxima / 20  → factor de dificultad (20 HP = factor 1.0, un Wither = 15x)
     * - multiplicador  → definido en config.yml por arma
     */
    private double calculateXP(Player killer, LivingEntity mob, SkillType skill) {
        double damageDealt = damageTracker
                .getOrDefault(killer.getUniqueId(), Map.of())
                .getOrDefault(mob.getUniqueId(), 0.0);

        // Si no hay daño trackeado (ej: kill de un solo golpe antes del evento)
        // usamos la HP máxima como fallback
        if (damageDealt <= 0) {
            damageDealt = getMaxHp(mob);
        }

        double maxHp       = getMaxHp(mob);
        double difficulty  = maxHp / 20.0;  // factor de dificultad relativo a 20 HP
        double multiplier  = getMultiplier(skill);

        double xp = damageDealt * difficulty * multiplier;

        // Opcional: log para testeo, podés comentarlo después
        plugin.getLogger().fine(String.format(
                "[XP] %s mató %s | daño=%.1f | hpMax=%.1f | dificultad=%.2f | mult=%.1f | XP=%.1f",
                killer.getName(), mob.getType(), damageDealt, maxHp, difficulty, multiplier, xp
        ));

        return xp;
    }

    private double getMaxHp(LivingEntity entity) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    private double getMultiplier(SkillType skill) {
        return plugin.getConfig().getDouble(
                "weapon-skills." + skill.getKey() + ".xp-multiplier", 1.0);
    }

    // ─── Detección de arma ───────────────────────────────────────────────────

    private SkillType detectKillWeapon(Player killer, EntityDeathEvent event) {
        if (event.getEntity().getLastDamageCause() instanceof
                org.bukkit.event.entity.EntityDamageByEntityEvent edEvent) {

            if (edEvent.getDamager() instanceof Arrow arrow &&
                    arrow.getShooter() instanceof Player shooter &&
                    shooter.equals(killer)) {
                return SkillType.WEAPON_BOW;
            }

            if (edEvent.getDamager() instanceof Trident trident &&
                    trident.getShooter() instanceof Player shooter &&
                    shooter.equals(killer)) {
                return SkillType.WEAPON_TRIDENT;
            }
        }

        ItemStack mainHand = killer.getInventory().getItemInMainHand();
        return WeaponUtil.getWeaponSkill(mainHand);
    }

    private Player getAttackingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Arrow a &&
                a.getShooter() instanceof Player p) return p;
        if (event.getDamager() instanceof Trident t &&
                t.getShooter() instanceof Player p) return p;
        return null;
    }

    private void cleanTracker(UUID playerUUID, UUID mobUUID) {
        Map<UUID, Double> mobMap = damageTracker.get(playerUUID);
        if (mobMap != null) {
            mobMap.remove(mobUUID);
            if (mobMap.isEmpty()) damageTracker.remove(playerUUID);
        }
    }
}