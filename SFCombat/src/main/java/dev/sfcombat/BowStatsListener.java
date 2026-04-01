package dev.sfcombat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public final class BowStatsListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey bowDamageDataKey = new NamespacedKey("customforge", "bow_damage");
    private final NamespacedKey bowSpeedDataKey = new NamespacedKey("customforge", "bow_speed");
    private final NamespacedKey bowVelocityDataKey = new NamespacedKey("customforge", "bow_velocity_multiplier");
    private final NamespacedKey bowGravityDataKey = new NamespacedKey("customforge", "arrow_gravity");
    private final NamespacedKey projectileDamageKey;
    private final NamespacedKey heavyArrowRadiusKey;
    private final double defaultArrowGravity;
    private final int baseDrawTicks;
    private final long maxDrawTrackMs;
    private final long gravityTaskPeriodTicks;
    private final boolean heavyArrowEnabled;
    private final String heavyArrowOraxenId;
    private final double heavyArrowHitboxRadius;
    private final boolean longbowZoomEnabled;
    private final int longbowZoomAmplifier;
    private final boolean chargeBarEnabled;
    private final long chargeBarTaskPeriodTicks;
    private final BossBar.Color chargeBarLoadingColor;
    private final BossBar.Color chargeBarReadyColor;
    private final Map<UUID, Double> gravityOverrides = new HashMap<>();
    private final Map<UUID, Long> drawStartMs = new HashMap<>();
    private final Map<UUID, BossBar> chargeBars = new HashMap<>();
    private final Map<UUID, Boolean> zoomApplied = new HashMap<>();

    public BowStatsListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.projectileDamageKey = new NamespacedKey(plugin, "bow_projectile_damage");
        this.heavyArrowRadiusKey = new NamespacedKey(plugin, "heavy_arrow_hitbox");
        this.defaultArrowGravity = plugin.getConfig().getDouble("bow.default-arrow-gravity", 0.05D);
        this.baseDrawTicks = Math.max(1, plugin.getConfig().getInt("bow.base-draw-ticks", 60));
        this.maxDrawTrackMs = Math.max(1000L, plugin.getConfig().getLong("bow.max-draw-track-ms", 15000L));
        this.gravityTaskPeriodTicks = Math.max(1L, plugin.getConfig().getLong("bow.gravity-task-period-ticks", 1L));
        this.heavyArrowEnabled = plugin.getConfig().getBoolean("bow.heavy-arrow.enabled", true);
        this.heavyArrowOraxenId = normalize(plugin.getConfig().getString("bow.heavy-arrow.oraxen-id", "heavy_arrow"));
        this.heavyArrowHitboxRadius = Math.max(0.0D, plugin.getConfig().getDouble("bow.heavy-arrow.hitbox-radius", 1.2D));
        this.longbowZoomEnabled = plugin.getConfig().getBoolean("bow.longbow-zoom.enabled", true);
        this.longbowZoomAmplifier = Math.max(0, plugin.getConfig().getInt("bow.longbow-zoom.slowness-amplifier", 4));
        this.chargeBarEnabled = plugin.getConfig().getBoolean("bow.charge-bar.enabled", true);
        this.chargeBarTaskPeriodTicks = Math.max(1L, plugin.getConfig().getLong("bow.charge-bar.update-period-ticks", 2L));
        this.chargeBarLoadingColor = parseColor(plugin.getConfig().getString("bow.charge-bar.color-loading"), BossBar.Color.YELLOW);
        this.chargeBarReadyColor = parseColor(plugin.getConfig().getString("bow.charge-bar.color-ready"), BossBar.Color.GREEN);
        startGravityTask();
        startChargeBarTask();
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        ItemStack bow = event.getBow();
        Entity shotEntity = event.getProjectile();
        if (bow == null || !(shotEntity instanceof Projectile projectile)) {
            return;
        }
        ItemMeta meta = bow.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer bowPdc = meta.getPersistentDataContainer();
        double speed = bowPdc.getOrDefault(bowSpeedDataKey, PersistentDataType.DOUBLE, 1.0D);
        if (speed <= 0.0D) {
            speed = 1.0D;
        }
        int effectiveDrawTicks = Math.max(1, (int) Math.round(baseDrawTicks / speed));
        long now = System.currentTimeMillis();
        Long started = drawStartMs.remove(event.getEntity().getUniqueId());
        removeZoom(event.getEntity().getUniqueId());
        hideChargeBar(event.getEntity().getUniqueId());
        double elapsedTicks = started == null
            ? (event.getForce() * 20.0D)
            : Math.max(0.0D, (now - started) / 50.0D);
        double chargeRatio = clamp01(elapsedTicks / effectiveDrawTicks);
        double customForce = chargeCurve(chargeRatio);
        double vanillaForce = Math.max(0.0001D, event.getForce());
        double drawCorrection = customForce / vanillaForce;

        Double velocityMultiplier = bowPdc.get(bowVelocityDataKey, PersistentDataType.DOUBLE);
        double velocityFactor = velocityMultiplier == null ? 1.0D : Math.max(0.0D, velocityMultiplier);
        double finalVelocityFactor = Math.max(0.0D, velocityFactor * drawCorrection);
        if (Math.abs(finalVelocityFactor - 1.0D) > 0.0001D) {
            projectile.setVelocity(projectile.getVelocity().multiply(finalVelocityFactor));
        }
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }
        Double configuredGravity = bowPdc.get(bowGravityDataKey, PersistentDataType.DOUBLE);
        if (configuredGravity != null && configuredGravity <= 0.0001D) {
            arrow.setGravity(false);
            gravityOverrides.remove(arrow.getUniqueId());
        }
        Double configuredDamage = bowPdc.get(bowDamageDataKey, PersistentDataType.DOUBLE);
        if (configuredDamage != null && configuredDamage > 0.0D) {
            double finalDamage = Math.max(0.1D, configuredDamage * customForce);
            arrow.setDamage(finalDamage);
            arrow.getPersistentDataContainer().set(projectileDamageKey, PersistentDataType.DOUBLE, finalDamage);
        }
        if (isHeavyArrowShot(event)) {
            arrow.getPersistentDataContainer().set(heavyArrowRadiusKey, PersistentDataType.DOUBLE, heavyArrowHitboxRadius);
        }
        if (configuredGravity != null && configuredGravity > 0.0001D
            && Math.abs(configuredGravity - defaultArrowGravity) > 0.0001D) {
            gravityOverrides.put(arrow.getUniqueId(), configuredGravity);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowUseStart(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir() || item.getType() != org.bukkit.Material.BOW) {
            return;
        }
        drawStartMs.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        drawStartMs.remove(event.getPlayer().getUniqueId());
        removeZoom(event.getPlayer().getUniqueId());
        hideChargeBar(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) {
            return;
        }
        Double damage = arrow.getPersistentDataContainer().get(projectileDamageKey, PersistentDataType.DOUBLE);
        if (damage != null && damage > 0.0D) {
            event.setDamage(damage);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        gravityOverrides.remove(arrow.getUniqueId());
        if (event.getHitEntity() != null) {
            return;
        }
        Double radius = arrow.getPersistentDataContainer().get(heavyArrowRadiusKey, PersistentDataType.DOUBLE);
        if (radius == null || radius <= 0.0D) {
            return;
        }
        LivingEntity target = findNearestTarget(arrow, radius);
        if (target == null) {
            return;
        }
        double damage = resolveArrowDamage(arrow);
        ProjectileSource shooter = arrow.getShooter();
        if (shooter instanceof Player player) {
            target.damage(damage, player);
        } else {
            target.damage(damage);
        }
        arrow.remove();
    }

    private void startGravityTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (gravityOverrides.isEmpty()) {
                return;
            }
            Iterator<Map.Entry<UUID, Double>> it = gravityOverrides.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Double> entry = it.next();
                Entity entity = plugin.getServer().getEntity(entry.getKey());
                if (!(entity instanceof AbstractArrow arrow) || !arrow.isValid() || arrow.isDead() || arrow.isInBlock()) {
                    it.remove();
                    continue;
                }
                double targetGravity = entry.getValue();
                double delta = targetGravity - defaultArrowGravity;
                if (Math.abs(delta) < 0.0001D) {
                    continue;
                }
                var velocity = arrow.getVelocity();
                velocity.setY(velocity.getY() - delta);
                arrow.setVelocity(velocity);
            }
        }, 1L, gravityTaskPeriodTicks);
    }

    private void startChargeBarTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                if (!player.isHandRaised()) {
                    hideChargeBar(playerId);
                    removeZoom(playerId);
                    drawStartMs.remove(playerId);
                    continue;
                }
                ItemStack active = player.getActiveItem();
                if (active == null || active.getType() != org.bukkit.Material.BOW) {
                    hideChargeBar(playerId);
                    removeZoom(playerId);
                    drawStartMs.remove(playerId);
                    continue;
                }
                updateZoom(player, active);
                long started = drawStartMs.computeIfAbsent(playerId, id -> now);
                if (now - started > maxDrawTrackMs) {
                    drawStartMs.put(playerId, now);
                    started = now;
                }
                double speed = resolveBowSpeed(active);
                int effectiveDrawTicks = Math.max(1, (int) Math.round(baseDrawTicks / speed));
                double elapsedTicks = Math.max(0.0D, (now - started) / 50.0D);
                double ratio = clamp01(elapsedTicks / effectiveDrawTicks);
                if (chargeBarEnabled) {
                    showOrUpdateChargeBar(player, ratio);
                }
            }
            Iterator<Map.Entry<UUID, Long>> it = drawStartMs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) {
                    hideChargeBar(entry.getKey());
                    removeZoom(entry.getKey());
                    it.remove();
                }
            }
        }, 1L, chargeBarTaskPeriodTicks);
    }

    private void showOrUpdateChargeBar(Player player, double ratio) {
        BossBar bar = chargeBars.computeIfAbsent(player.getUniqueId(), id ->
            BossBar.bossBar(
                Component.empty(),
                0.0F,
                chargeBarLoadingColor,
                BossBar.Overlay.PROGRESS
            )
        );
        bar.name(Component.empty());
        bar.progress((float) clamp01(ratio));
        bar.color(ratio >= 1.0D ? chargeBarReadyColor : chargeBarLoadingColor);
        player.showBossBar(bar);
    }

    private void hideChargeBar(UUID playerId) {
        BossBar bar = chargeBars.remove(playerId);
        if (bar == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.hideBossBar(bar);
        }
    }

    private double resolveBowSpeed(ItemStack bow) {
        if (bow == null || !bow.hasItemMeta()) {
            return 1.0D;
        }
        Double speed = bow.getItemMeta().getPersistentDataContainer().get(bowSpeedDataKey, PersistentDataType.DOUBLE);
        if (speed == null || speed <= 0.0D) {
            return 1.0D;
        }
        return speed;
    }

    private double clamp01(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        return Math.min(1.0D, value);
    }

    private double chargeCurve(double x) {
        double curve = (x * x + (x * 2.0D)) / 3.0D;
        return clamp01(curve);
    }

    private BossBar.Color parseColor(String value, BossBar.Color fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return BossBar.Color.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private boolean isHeavyArrowShot(EntityShootBowEvent event) {
        if (!heavyArrowEnabled || heavyArrowHitboxRadius <= 0.0D || !(event.getEntity() instanceof Player player)) {
            return false;
        }
        ItemStack ammo = resolveConsumable(event, player);
        if (ammo == null || ammo.getType().isAir() || !ammo.hasItemMeta()) {
            return false;
        }
        String oraxenId = readOraxenId(ammo);
        return !heavyArrowOraxenId.isBlank() && heavyArrowOraxenId.equals(oraxenId);
    }

    private ItemStack resolveConsumable(EntityShootBowEvent event, Player player) {
        ItemStack consumable = event.getConsumable();
        if (consumable != null) {
            return consumable;
        }
        ItemStack offhand = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
        if (offhand != null && offhand.getType() == org.bukkit.Material.ARROW) {
            return offhand;
        }
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content != null && content.getType() == org.bukkit.Material.ARROW) {
                return content;
            }
        }
        return null;
    }

    private String readOraxenId(ItemStack item) {
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(new NamespacedKey("oraxen", "item_id"), PersistentDataType.STRING);
        if (id == null) {
            id = pdc.get(new NamespacedKey("oraxen", "id"), PersistentDataType.STRING);
        }
        if (id == null) {
            id = pdc.get(new NamespacedKey("oraxen", "oraxen_item_id"), PersistentDataType.STRING);
        }
        return normalize(id);
    }

    private LivingEntity findNearestTarget(AbstractArrow arrow, double radius) {
        LivingEntity nearest = null;
        double bestDist = Double.MAX_VALUE;
        ProjectileSource shooter = arrow.getShooter();
        for (Entity nearby : arrow.getWorld().getNearbyEntities(arrow.getLocation(), radius, radius, radius)) {
            if (!(nearby instanceof LivingEntity living) || living.isDead() || !living.isValid()) {
                continue;
            }
            if (shooter instanceof Entity shooterEntity && nearby.getUniqueId().equals(shooterEntity.getUniqueId())) {
                continue;
            }
            double dist = living.getLocation().distanceSquared(arrow.getLocation());
            if (dist < bestDist) {
                bestDist = dist;
                nearest = living;
            }
        }
        return nearest;
    }

    private double resolveArrowDamage(AbstractArrow arrow) {
        Double custom = arrow.getPersistentDataContainer().get(projectileDamageKey, PersistentDataType.DOUBLE);
        if (custom != null && custom > 0.0D) {
            return custom;
        }
        return Math.max(0.1D, arrow.getDamage());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private void updateZoom(Player player, ItemStack activeItem) {
        if (!longbowZoomEnabled || !isLongbow(activeItem)) {
            removeZoom(player.getUniqueId());
            return;
        }
        var current = player.getPotionEffect(PotionEffectType.SLOWNESS);
        boolean alreadyApplied = zoomApplied.containsKey(player.getUniqueId());
        if (current != null && !alreadyApplied) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, longbowZoomAmplifier, false, false, false));
        zoomApplied.put(player.getUniqueId(), Boolean.TRUE);
    }

    private void removeZoom(UUID playerId) {
        if (!zoomApplied.containsKey(playerId)) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
        zoomApplied.remove(playerId);
    }

    private boolean isLongbow(ItemStack bow) {
        if (bow == null || !bow.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = bow.getItemMeta().getPersistentDataContainer();
        String bowType = normalize(pdc.get(new NamespacedKey("customforge", "bow_type"), PersistentDataType.STRING));
        if (bowType.equals("long_bow")) {
            return true;
        }
        String oraxenId = readOraxenId(bow);
        return oraxenId.startsWith("long_bow");
    }
}
