package dev.sfcore.listeners;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatType;
import dev.sfcore.managers.StatManager;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;

public final class MagicStaffListener implements Listener {

    private static final String SOURCE_PREFIX = "equipment:magic_staff";

    private final StatManager statManager;
    private final JavaPlugin plugin;
    private final Map<String, Double> staffBonuses = new HashMap<>();
    private final Map<String, Long> staffShootCooldowns = new HashMap<>();
    private final Map<String, Double> staffShootMultipliers = new HashMap<>();
    private final Map<UUID, StaffState> lastStates = new HashMap<>();
    private final Map<UUID, Long> staffShootCooldownUntil = new HashMap<>();
    private final long refreshIntervalTicks;
    private final boolean enabled;
    private final boolean shootEnabled;
    private final double shootSpeed;
    private final double shootRange;
    private final int projectileParticleTicks;
    private final NamespacedKey projectileKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey damageKey;

    private record StaffState(String mainId, String offId, double totalBonus) {}

    public MagicStaffListener(JavaPlugin plugin, StatManager statManager, ConfigurationSection section) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.enabled = section == null || section.getBoolean("enabled", true);
        this.refreshIntervalTicks = Math.max(5L, section == null ? 20L : section.getLong("refresh-interval-ticks", 20L));
        ConfigurationSection shootSection = section == null ? null : section.getConfigurationSection("shoot");
        this.shootEnabled = shootSection == null || shootSection.getBoolean("enabled", true);
        this.shootSpeed = Math.max(0.1D, shootSection == null ? 1.8D : shootSection.getDouble("projectile-speed", 1.8D));
        this.shootRange = Math.max(4.0D, shootSection == null ? 32.0D : shootSection.getDouble("projectile-range", 32.0D));
        this.projectileParticleTicks = Math.max(1, shootSection == null ? 6 : shootSection.getInt("projectile-particle-ticks", 6));
        this.projectileKey = new NamespacedKey(plugin, "magic_staff_projectile");
        this.ownerKey = new NamespacedKey(plugin, "magic_staff_owner");
        this.damageKey = new NamespacedKey(plugin, "magic_staff_damage");
        loadConfig(section);
    }

    public long getRefreshIntervalTicks() {
        return refreshIntervalTicks;
    }

    public void syncOnlinePlayers() {
        if (!enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            syncPlayer(player);
        }
    }

    public void syncPlayer(Player player) {
        if (!enabled || player == null || !player.isOnline()) {
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        String mainId = normalize(readOraxenId(main));
        String offId = normalize(readOraxenId(off));
        double mainBonus = staffBonuses.getOrDefault(mainId, 0.0D);
        double offBonus = staffBonuses.getOrDefault(offId, 0.0D);
        double totalBonus = mainBonus + offBonus;

        StaffState current = new StaffState(mainId, offId, totalBonus);
        StaffState previous = lastStates.get(player.getUniqueId());
        if (current.equals(previous)) {
            return;
        }

        statManager.clearSource(player, SOURCE_PREFIX);
        if (mainBonus > 0.0D) {
            statManager.addBonus(player, SOURCE_PREFIX + ":main", StatType.MAGIC_DAMAGE, mainBonus);
        }
        if (offBonus > 0.0D) {
            statManager.addBonus(player, SOURCE_PREFIX + ":off", StatType.MAGIC_DAMAGE, offBonus);
        }
        lastStates.put(player.getUniqueId(), current);
    }

    public void clearPlayer(Player player) {
        if (player == null || !enabled) {
            return;
        }
        statManager.clearSource(player, SOURCE_PREFIX);
        lastStates.remove(player.getUniqueId());
        staffShootCooldownUntil.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> syncPlayer(event.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) {
            return;
        }
        clearPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!enabled) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> syncPlayer(event.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!enabled) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> syncPlayer(event.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> syncPlayer(event.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) {
            return;
        }
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> syncPlayer(player), 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!enabled) {
            return;
        }
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> syncPlayer(player), 1L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled || !shootEnabled) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack eventItem = event.getItem();
        if (eventItem == null || eventItem.getType().isAir()) {
            return;
        }
        String staffId = normalize(readOraxenId(eventItem));
        double staffBonus = staffBonuses.getOrDefault(staffId, 0.0D);
        if (staffBonus <= 0.0D) {
            return;
        }
        long staffCooldownTicks = Math.max(0L, staffShootCooldowns.getOrDefault(staffId, 20L));
        double staffShootMultiplier = Math.max(0.1D, staffShootMultipliers.getOrDefault(staffId, 1.0D));

        long now = System.currentTimeMillis();
        long cooldownUntil = staffShootCooldownUntil.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownUntil) {
            return;
        }
        staffShootCooldownUntil.put(player.getUniqueId(), now + (staffCooldownTicks * 50L));
        event.setCancelled(true);

        double baseMagic = SFCoreAPI.get().getMagicDamage(player);
        double finalDamage = Math.max(0.0D, baseMagic * staffShootMultiplier);
        launchMagicBolt(player, finalDamage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        var pdc = projectile.getPersistentDataContainer();
        if (!pdc.has(projectileKey, PersistentDataType.BYTE)) {
            return;
        }

        String ownerRaw = pdc.get(ownerKey, PersistentDataType.STRING);
        Double damage = pdc.get(damageKey, PersistentDataType.DOUBLE);
        if (ownerRaw == null || damage == null) {
            projectile.remove();
            return;
        }

        Player owner = null;
        try {
            owner = Bukkit.getPlayer(UUID.fromString(ownerRaw));
        } catch (IllegalArgumentException ignored) {
            owner = null;
        }

        if (event.getHitEntity() instanceof LivingEntity target) {
            if (owner == null || !target.getUniqueId().equals(owner.getUniqueId())) {
                if (owner != null && owner.isOnline()) {
                    target.damage(damage, owner);
                } else {
                    target.damage(damage);
                }
            }
        }

        var hitLocation = projectile.getLocation();
        var world = hitLocation.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.END_ROD, hitLocation, 18, 0.18D, 0.18D, 0.18D, 0.02D);
            world.spawnParticle(Particle.PORTAL, hitLocation, 10, 0.15D, 0.15D, 0.15D, 0.08D);
            world.playSound(hitLocation, Sound.ENTITY_BLAZE_HURT, 0.75F, 1.6F);
        }
        projectile.remove();
    }

    private void loadConfig(ConfigurationSection section) {
        staffBonuses.clear();
        if (section == null) {
            return;
        }
        ConfigurationSection items = section.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection itemSection = items.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }
            String normalizedKey = normalize(key);
            double bonus = itemSection.getDouble("magic-damage", 0.0D);
            if (bonus <= 0.0D) {
                continue;
            }
            staffBonuses.put(normalizedKey, bonus);
            staffShootCooldowns.put(normalizedKey, Math.max(0L, itemSection.getLong("shoot-cooldown-ticks", 20L)));
            staffShootMultipliers.put(normalizedKey, Math.max(0.1D, itemSection.getDouble("shoot-damage-multiplier", 1.0D)));
        }
    }

    private String readOraxenId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(new org.bukkit.NamespacedKey("oraxen", "item_id"), org.bukkit.persistence.PersistentDataType.STRING);
        if (id == null) {
            id = pdc.get(new org.bukkit.NamespacedKey("oraxen", "id"), org.bukkit.persistence.PersistentDataType.STRING);
        }
        if (id == null) {
            id = pdc.get(new org.bukkit.NamespacedKey("oraxen", "oraxen_item_id"), org.bukkit.persistence.PersistentDataType.STRING);
        }
        return id;
    }

    private void launchMagicBolt(Player player, double damage) {
        org.bukkit.Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        org.bukkit.Location spawnLocation = eye.clone().add(0.0D, 0.10D, 0.0D);
        Vector velocity = direction.multiply(shootSpeed);
        Snowball projectile = player.getWorld().spawn(spawnLocation, Snowball.class, spawned -> {
            spawned.setGravity(false);
            spawned.setSilent(true);
            spawned.setVelocity(velocity);
            spawned.setShooter(player);
        });

        var pdc = projectile.getPersistentDataContainer();
        pdc.set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(damageKey, PersistentDataType.DOUBLE, damage);

        var world = player.getWorld();
        world.playSound(spawnLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.5F);
        world.spawnParticle(Particle.END_ROD, spawnLocation, 8, 0.08D, 0.08D, 0.08D, 0.03D);
        world.spawnParticle(Particle.PORTAL, spawnLocation, 6, 0.10D, 0.10D, 0.10D, 0.06D);

        new BukkitRunnable() {
            private int age = 0;

            @Override
            public void run() {
                if (!projectile.isValid() || age >= projectileParticleTicks) {
                    cancel();
                    return;
                }

                var tickLocation = projectile.getLocation();
                var tickWorld = tickLocation.getWorld();
                if (tickWorld != null) {
                    tickWorld.spawnParticle(Particle.END_ROD, tickLocation, 2, 0.04D, 0.04D, 0.04D, 0.0D);
                    tickWorld.spawnParticle(Particle.PORTAL, tickLocation, 1, 0.03D, 0.03D, 0.03D, 0.01D);
                }
                age++;
            }
        }.runTaskTimer(plugin, 1L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (projectile.isValid()) {
                projectile.remove();
            }
        }, Math.max(20L, (long) Math.ceil(shootRange / Math.max(0.1D, shootSpeed))));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
