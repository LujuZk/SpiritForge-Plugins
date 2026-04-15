package dev.sfcrafting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Score;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class SpellCastManager implements Listener {

    private enum ProjectileVisualMode {
        ITEM,
        BETTERMODEL
    }

    private final Plugin plugin;
    private final SFCoreBridge sfCoreBridge;
    private final SkillBridge skillBridge;
    private final OraxenItemResolver resolver;
    private final String attackSkillName;
    private final String projectileSkillName;
    private final String buffSkillName;

    private final NamespacedKey spellTemplateKey;
    private final NamespacedKey spellTypeKey;
    private final NamespacedKey spellValueDamageKey;
    private final NamespacedKey spellValueAreaKey;
    private final NamespacedKey spellValueCastSpeedKey;
    private final NamespacedKey spellValueCooldownKey;
    private final NamespacedKey spellValueManaCostKey;
    private final NamespacedKey spellValueDamageMultiplierKey;
    private final NamespacedKey spellValuePowerKey;
    private final NamespacedKey spellValueDurationKey;
    private final NamespacedKey spellValueProjectileSpeedKey;
    private final NamespacedKey spellValueProjectileRangeKey;
    private final NamespacedKey spellProjectileModelKey;
    private final NamespacedKey spellProjectileScaleKey;
    private final NamespacedKey spellMythicSkillKey;
    private final NamespacedKey projectileTemplateKey;
    private final NamespacedKey projectileTagKey;
    private final NamespacedKey projectileDamageKey;
    private final NamespacedKey projectileOwnerKey;

    private final Map<UUID, Map<String, Long>> cooldownByPlayer = new HashMap<>();
    private final Map<UUID, BukkitTask> castBarsByPlayer = new HashMap<>();
    private final Map<UUID, BossBar> castBossBars = new HashMap<>();
    private final Map<UUID, BukkitTask> targetAuraTasks = new HashMap<>();
    private final Set<UUID> playersCasting = new HashSet<>();
    private final Map<UUID, Long> lastInteractMs = new HashMap<>();
    private final Map<UUID, Long> interactLockUntilMs = new HashMap<>();
    private final Map<UUID, ArrayDeque<PendingSpellHit>> pendingSpellHits = new HashMap<>();

    private final Object mythicApiHelper;
    private final java.lang.reflect.Method castSkillMethod;
    private final java.lang.reflect.Method castSkillWithPowerMethod;

    private record PendingSpellHit(int damage, long expiresAt) {}

    public SpellCastManager(Plugin plugin) {
        this.plugin = plugin;
        this.sfCoreBridge = new SFCoreBridge(plugin);
        this.skillBridge = new SkillBridge(plugin);
        this.resolver = new OraxenItemResolver(plugin);
        this.attackSkillName = plugin.getConfig().getString("spellcraft.cast-skill-map.attack_aoe", "SF_Dynamic_AttackAOE");
        this.projectileSkillName = plugin.getConfig().getString("spellcraft.cast-skill-map.attack_projectile", "SF_Dynamic_Projectile");
        this.buffSkillName = plugin.getConfig().getString("spellcraft.cast-skill-map.buff", "SF_Dynamic_Buff");

        this.spellTemplateKey = new NamespacedKey(plugin, "spell_template");
        this.spellTypeKey = new NamespacedKey(plugin, "spell_type");
        this.spellValueDamageKey = new NamespacedKey(plugin, "spell_val_damage");
        this.spellValueAreaKey = new NamespacedKey(plugin, "spell_val_area");
        this.spellValueCastSpeedKey = new NamespacedKey(plugin, "spell_val_cast_speed");
        this.spellValueCooldownKey = new NamespacedKey(plugin, "spell_val_cooldown");
        this.spellValueManaCostKey = new NamespacedKey(plugin, "spell_val_mana_cost");
        this.spellValueDamageMultiplierKey = new NamespacedKey(plugin, "spell_val_damage_multiplier");
        this.spellValuePowerKey = new NamespacedKey(plugin, "spell_val_power");
        this.spellValueDurationKey = new NamespacedKey(plugin, "spell_val_duration");
        this.spellValueProjectileSpeedKey = new NamespacedKey(plugin, "spell_val_projectile_speed");
        this.spellValueProjectileRangeKey = new NamespacedKey(plugin, "spell_val_projectile_range");
        this.spellProjectileModelKey = new NamespacedKey(plugin, "spell_projectile_model");
        this.spellProjectileScaleKey = new NamespacedKey(plugin, "spell_projectile_scale");
        this.spellMythicSkillKey = new NamespacedKey(plugin, "spell_mythic_skill");
        this.projectileTemplateKey = new NamespacedKey(plugin, "sf_spell_template");
        this.projectileTagKey = new NamespacedKey(plugin, "sf_spell_projectile");
        this.projectileDamageKey = new NamespacedKey(plugin, "sf_spell_projectile_damage");
        this.projectileOwnerKey = new NamespacedKey(plugin, "sf_spell_projectile_owner");

        Object helper = null;
        java.lang.reflect.Method castNoPower = null;
        java.lang.reflect.Method castWithPower = null;
        try {
            Plugin mythicPlugin = plugin.getServer().getPluginManager().getPlugin("MythicMobs");
            if (mythicPlugin != null) {
                java.lang.reflect.Method getApiHelper = mythicPlugin.getClass().getMethod("getAPIHelper");
                helper = getApiHelper.invoke(mythicPlugin);
                if (helper != null) {
                    castNoPower = helper.getClass().getMethod("castSkill", org.bukkit.entity.Entity.class, String.class);
                    castWithPower = helper.getClass().getMethod("castSkill", org.bukkit.entity.Entity.class, String.class, float.class);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo inicializar el puente de casteo con MythicMobs: " + e.getMessage());
        }
        this.mythicApiHelper = helper;
        this.castSkillMethod = castNoPower;
        this.castSkillWithPowerMethod = castWithPower;
    }

    public boolean isReady() {
        return mythicApiHelper != null && castSkillMethod != null && sfCoreBridge.isAvailable();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerUseSpell(PlayerInteractEvent event) {
        if (!isReady()) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
            }
            default -> {
                return;
            }
        }
        handleSpellInteract(event.getPlayer(), hand, event.getItem(), event);
    }

    private void handleSpellInteract(Player player, EquipmentSlot hand, ItemStack eventItem, org.bukkit.event.Cancellable event) {
        UUID playerId = player.getUniqueId();
        long nowInteract = System.currentTimeMillis();
        long lockUntil = interactLockUntilMs.getOrDefault(playerId, 0L);
        if (nowInteract < lockUntil) {
            event.setCancelled(true);
            return;
        }
        long last = lastInteractMs.getOrDefault(playerId, 0L);
        // Evita spam por mantener click derecho (eventos repetidos muy seguidos).
        if (nowInteract - last < 180L) {
            event.setCancelled(true);
            return;
        }
        lastInteractMs.put(playerId, nowInteract);

        if (playersCasting.contains(playerId)) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = eventItem;
        if (item == null || item.getType().isAir()) {
            item = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        }
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String template = pdc.get(spellTemplateKey, PersistentDataType.STRING);
        if (template == null || template.isBlank()) {
            return;
        }
        String spellType = normalize(pdc.get(spellTypeKey, PersistentDataType.STRING));
        if (spellType.isBlank()) {
            return;
        }
        event.setCancelled(true);

        String missingRequirement = findMissingRequiredSkill(player, template);
        if (missingRequirement != null) {
            player.sendMessage(Component.text(missingRequirement, NamedTextColor.RED));
            return;
        }

        double castSpeed = getDouble(pdc, spellValueCastSpeedKey, 1.0D);
        int castDelayTicks = Math.max(0, (int) Math.round(20.0D / Math.max(0.1D, castSpeed)));
        int manaCost = Math.max(1, (int) Math.round(Math.max(1.0D, getDouble(pdc, spellValueManaCostKey, 10.0D))));
        int cooldownSeconds = Math.max(1, (int) Math.round(Math.max(1.0D, getDouble(pdc, spellValueCooldownKey, 5.0D))));
        long holdGuardMs = Math.max(450L, (castDelayTicks * 50L) + 120L);
        interactLockUntilMs.put(playerId, nowInteract + holdGuardMs);

        String cooldownKey = template + "|" + spellType;
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldownByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>());
        long readyAt = playerCooldowns.getOrDefault(cooldownKey, 0L);
        if (readyAt > now) {
            double left = (readyAt - now) / 1000.0D;
            player.sendMessage(Component.text("Cooldown: " + fmt(left) + "s", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        int damage = Math.max(1, (int) Math.round(Math.max(1.0D, getDouble(pdc, spellValueDamageKey, 8.0D))));
        int radius = Math.max(1, (int) Math.round(Math.max(1.0D, getDouble(pdc, spellValueAreaKey, 2.0D))));
        double power = Math.max(0.1D, getDouble(pdc, spellValuePowerKey, 1.0D));
        int durationSeconds = Math.max(1, (int) Math.round(Math.max(1.0D, getDouble(pdc, spellValueDurationKey, 5.0D))));
        int durationTicks = durationSeconds * 20;
        int regenLevel = clamp((int) Math.floor((power - 1.0D) / 0.25D), 0, 5);
        int resistLevel = clamp((int) Math.floor((power - 1.0D) / 0.50D), 0, 3);
        double projectileSpeed = Math.max(8.0D, getDouble(pdc, spellValueProjectileSpeedKey, 34.0D));
        double projectileRange = Math.max(4.0D, getDouble(pdc, spellValueProjectileRangeKey, 18.0D));
        String projectileModelItem = normalize(pdc.get(spellProjectileModelKey, PersistentDataType.STRING));
        double projectileScale = Math.max(0.1D, getDouble(pdc, spellProjectileScaleKey, 1.0D));
        String mythicSkillOverride = pdc.get(spellMythicSkillKey, PersistentDataType.STRING);
        if (mythicSkillOverride == null) {
            mythicSkillOverride = "";
        } else {
            mythicSkillOverride = mythicSkillOverride.trim();
        }
        ProjectileVisualMode projectileVisualMode = getProjectileVisualMode();
        String configuredTemplateSkill = plugin.getConfig().getString("spellcraft.templates." + template + ".mythic-skill", "");
        if (configuredTemplateSkill == null) {
            configuredTemplateSkill = "";
        } else {
            configuredTemplateSkill = configuredTemplateSkill.trim();
        }

        setScore(player, "sf_spell_damage", damage);
        setScore(player, "sf_spell_radius", radius);
        setScore(player, "sf_spell_duration_ticks", durationTicks);
        setScore(player, "sf_spell_regen_level", regenLevel);
        setScore(player, "sf_spell_resist_level", resistLevel);
        setScore(player, "sf_spell_projectile_speed", (int) Math.round(projectileSpeed));
        setScore(player, "sf_spell_projectile_range", (int) Math.round(projectileRange));
        setScore(player, "sf_spell_mana_cost", manaCost);
        setScore(player, "sf_spell_cooldown", cooldownSeconds);

        String defaultSkillName = switch (spellType) {
            case "attack_aoe" -> attackSkillName;
            case "attack_projectile" -> projectileSkillName;
            case "buff" -> buffSkillName;
            default -> "";
        };
        String skillName = defaultSkillName;
        boolean usedFallback = false;
        if (projectileVisualMode == ProjectileVisualMode.BETTERMODEL && spellType.equals("attack_projectile") && !configuredTemplateSkill.isBlank()) {
            skillName = configuredTemplateSkill;
        } else if (!mythicSkillOverride.isBlank() && (!spellType.equals("attack_projectile") || projectileVisualMode == ProjectileVisualMode.BETTERMODEL)) {
            skillName = mythicSkillOverride;
            if (!defaultSkillName.isBlank() && skillName.equalsIgnoreCase(defaultSkillName)) {
                // Usa el casing canónico del fallback para evitar fallos por skills en minúsculas guardadas en PDC.
                skillName = defaultSkillName;
            }
        } else if (!defaultSkillName.isBlank()) {
            usedFallback = true;
        }
        if (skillName.isBlank() && !spellType.equals("attack_projectile")) {
            player.sendMessage(Component.text("No hay skill mapeada para tipo: " + spellType, NamedTextColor.RED));
            return;
        }
        final String finalSkillName = skillName;
        final String debugMythicSkillOverride = mythicSkillOverride;
        final String debugDefaultSkillName = defaultSkillName;
        final String debugSelectedSkill = skillName;
        final boolean debugUsedFallback = usedFallback;

        playersCasting.add(playerId);
        if (castDelayTicks > 0) {
            startCastBar(player, castDelayTicks);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                stopCastBar(player);
                double currentMana = sfCoreBridge.getMana(player);
                if (currentMana < manaCost) {
                    player.sendMessage(Component.text("Mana insuficiente: " + fmt(currentMana) + "/" + manaCost, NamedTextColor.RED));
                    return;
                }
                int magicDamage = getScore(player, "sf_magic_damage");
                double damageMultiplier = Math.max(0.1D, getDouble(pdc, spellValueDamageMultiplierKey, 1.0D));
                int dynamicDamage = Math.max(1, (int) Math.round(magicDamage * damageMultiplier));
                final int debugMagicDamage = magicDamage;
                final double debugDamageMultiplier = damageMultiplier;
                final int debugDynamicDamage = dynamicDamage;
                final int debugRadius = radius;
                debugSkillSelection(player, template, spellType, projectileVisualMode, debugMythicSkillOverride, debugDefaultSkillName, debugSelectedSkill, debugUsedFallback, debugMagicDamage, debugDamageMultiplier, debugDynamicDamage, debugRadius);

                boolean casted;
                if (spellType.equals("attack_projectile")) {
                    casted = castProjectileSpell(player, template, dynamicDamage, projectileSpeed, projectileRange, projectileModelItem, projectileScale);
                } else if (spellType.equals("attack_aoe")) {
                    casted = castAoeSpell(player, template, dynamicDamage, radius);
                } else {
                    casted = castSkill(player, finalSkillName, (float) power);
                }
                if (!casted) {
                    player.sendMessage(Component.text("No se pudo castear el hechizo: " + finalSkillName, NamedTextColor.RED));
                    return;
                }
                if (!sfCoreBridge.spendMana(player, manaCost)) {
                    player.sendMessage(Component.text("Mana insuficiente.", NamedTextColor.RED));
                    return;
                }
                long next = System.currentTimeMillis() + (cooldownSeconds * 1000L);
                playerCooldowns.put(cooldownKey, next);
            } finally {
                playersCasting.remove(playerId);
            }
        }, castDelayTicks);
    }

    public boolean applyQueuedSpellHit(UUID casterId, UUID targetId) {
        if (casterId == null || targetId == null) {
            return false;
        }
        PendingSpellHit hit = pollPendingSpellHit(casterId);
        if (hit == null) {
            return false;
        }
        var entity = Bukkit.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()) {
            return false;
        }
        if (casterId.equals(target.getUniqueId())) {
            return false;
        }

        Player owner = Bukkit.getPlayer(casterId);
        if (owner != null && owner.isOnline()) {
            target.damage(hit.damage(), owner);
        } else {
            target.damage(hit.damage());
        }
        target.getWorld().spawnParticle(org.bukkit.Particle.FLAME, target.getLocation(), 12, 0.15, 0.15, 0.15, 0.01);
        return true;
    }

    private void queuePendingSpellHit(UUID casterId, int damage, long ttlMs) {
        long expiresAt = System.currentTimeMillis() + Math.max(500L, ttlMs);
        ArrayDeque<PendingSpellHit> queue = pendingSpellHits.computeIfAbsent(casterId, id -> new ArrayDeque<>());
        queue.addLast(new PendingSpellHit(damage, expiresAt));
        pruneExpiredHits(queue);
    }

    private PendingSpellHit pollPendingSpellHit(UUID casterId) {
        ArrayDeque<PendingSpellHit> queue = pendingSpellHits.get(casterId);
        if (queue == null) {
            return null;
        }
        pruneExpiredHits(queue);
        PendingSpellHit hit = queue.pollFirst();
        if (queue.isEmpty()) {
            pendingSpellHits.remove(casterId);
        }
        return hit;
    }

    private void pruneExpiredHits(ArrayDeque<PendingSpellHit> queue) {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty() && queue.peekFirst().expiresAt() < now) {
            queue.pollFirst();
        }
    }

    private String findMissingRequiredSkill(Player player, String template) {
        ConfigurationSection requiredSection = plugin.getConfig().getConfigurationSection("spellcraft.templates." + template + ".required-skills");
        if (requiredSection == null) {
            return null;
        }
        for (String requiredSkill : requiredSection.getKeys(false)) {
            int needed = Math.max(0, requiredSection.getInt(requiredSkill, 0));
            if (needed <= 0) {
                continue;
            }
            int current = resolveSkillLevel(player, requiredSkill);
            if (current < needed) {
                return "Requiere " + requiredSkill + " " + needed + " (actual " + current + ")";
            }
        }
        return null;
    }

    private int resolveSkillLevel(Player player, String skillKey) {
        int best = skillBridge.getSkillLevel(player, skillKey);
        ConfigurationSection aliasesSection = plugin.getConfig().getConfigurationSection("spellcraft.skill-aliases");
        if (aliasesSection == null) {
            return best;
        }
        for (String alias : aliasesSection.getStringList(skillKey)) {
            if (alias == null || alias.isBlank()) {
                continue;
            }
            best = Math.max(best, skillBridge.getSkillLevel(player, alias));
        }
        return best;
    }

    private void startCastBar(Player player, int totalTicks) {
        stopCastBar(player);

        AtomicInteger elapsedTicks = new AtomicInteger(0);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopCastBar(player);
                return;
            }

            int elapsed = elapsedTicks.addAndGet(2);
            double progress = Math.min(1.0D, elapsed / (double) totalTicks);
            showOrUpdateCastBar(player, progress);
            if (progress >= 1.0D) {
                stopCastBar(player);
            }
        }, 0L, 2L);
        castBarsByPlayer.put(player.getUniqueId(), task);
    }

    private void stopCastBar(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = castBarsByPlayer.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        BossBar bar = castBossBars.remove(playerId);
        if (bar != null && player.isOnline()) {
            player.hideBossBar(bar);
        }
    }

    private void showOrUpdateCastBar(Player player, double ratio) {
        BossBar bar = castBossBars.computeIfAbsent(player.getUniqueId(), id ->
            BossBar.bossBar(
                Component.empty(),
                0.0F,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
            )
        );
        double progress = Math.max(0.0D, Math.min(1.0D, ratio));
        bar.name(Component.empty());
        bar.progress((float) progress);
        bar.color(progress >= 1.0D ? BossBar.Color.GREEN : BossBar.Color.YELLOW);
        player.showBossBar(bar);
    }

    private boolean castSkill(Player caster, String skillName, float power) {
        try {
            if (castSkillWithPowerMethod != null) {
                Object result = castSkillWithPowerMethod.invoke(mythicApiHelper, caster, skillName, power);
                if (result instanceof Boolean b) {
                    return b;
                }
            }
            Object result = castSkillMethod.invoke(mythicApiHelper, caster, skillName);
            if (result instanceof Boolean b) {
                return b;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error casteando skill Mythic '" + skillName + "': " + e.getMessage());
        }
        return false;
    }

    private boolean castProjectileSpell(Player caster, String template, int damage, double speedBlocksPerSecond, double rangeBlocks, String projectileModelItem, double projectileScale) {
        if (caster == null || !caster.isOnline()) {
            return false;
        }

        double speedPerTick = Math.max(0.05D, speedBlocksPerSecond / 20.0D);
        int maxTicks = Math.max(10, (int) Math.ceil(rangeBlocks / speedPerTick));
        final double impactRadius = plugin.getConfig().getDouble("spellcraft.templates." + template + ".impact-radius", 0.0D);
        boolean targeted = plugin.getConfig().getBoolean("spellcraft.templates." + template + ".projectile-targeted", false);
        double spawnHeight = Math.max(4.0D, plugin.getConfig().getDouble("spellcraft.templates." + template + ".projectile-spawn-height", 18.0D));
        org.bukkit.Location castLocation = caster.getEyeLocation();
        org.bukkit.Location spawnLocation = targeted ? resolveTargetedProjectileSpawn(caster, rangeBlocks, spawnHeight) : castLocation.clone();
        Vector projectileVelocity = targeted
            ? new Vector(0.0D, -Math.max(speedPerTick, 0.15D), 0.0D)
            : castLocation.getDirection().normalize().multiply(speedPerTick);

        Snowball projectile = caster.getWorld().spawn(spawnLocation, Snowball.class, spawned -> {
            spawned.setVelocity(projectileVelocity);
            spawned.setGravity(false);
            spawned.setSilent(true);
        });

        if (projectileModelItem != null && !projectileModelItem.isBlank()) {
            ItemStack modelItem = resolver.buildOraxenItem(projectileModelItem, 1);
            if (modelItem != null && !modelItem.getType().isAir()) {
                projectile.setItem(modelItem);
            }
        }
        applyProjectileScale(projectile, projectileScale);

        var pdc = projectile.getPersistentDataContainer();
        pdc.set(projectileTagKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(projectileTemplateKey, PersistentDataType.STRING, normalize(template));
        pdc.set(projectileDamageKey, PersistentDataType.INTEGER, damage);
        pdc.set(projectileOwnerKey, PersistentDataType.STRING, caster.getUniqueId().toString());
        playTemplateEffects(template, "cast", targeted ? castLocation : projectile.getLocation(), caster, projectile, null);
        startProjectileTickEffects(projectile, maxTicks);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (projectile.isValid()) {
                if (impactRadius > 0.0D && normalize(template).equals("meteor")) {
                    Player owner = Bukkit.getPlayer(caster.getUniqueId());
                    org.bukkit.Location impactLocation = projectile.getLocation().clone();
                    applyImpactDamageInRadius(impactLocation, owner, damage, impactRadius);
                    playTemplateEffectsInSphere(template, "hit", impactLocation, owner, projectile, impactRadius);
                    playMeteorBlockImpact(impactLocation);
                }
                projectile.remove();
            }
        }, maxTicks);
        return true;
    }

    private org.bukkit.Location resolveTargetedProjectileSpawn(Player caster, double rangeBlocks, double spawnHeight) {
        org.bukkit.Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        org.bukkit.World world = caster.getWorld();
        RayTraceResult trace = world.rayTraceBlocks(eye, direction, rangeBlocks, FluidCollisionMode.NEVER, true);
        org.bukkit.Location targetPoint = trace != null && trace.getHitPosition() != null
            ? trace.getHitPosition().toLocation(world)
            : eye.clone().add(direction.multiply(rangeBlocks));
        double x = targetPoint.getX();
        double z = targetPoint.getZ();
        double y = Math.min(world.getMaxHeight() - 2.0D, targetPoint.getY() + spawnHeight);
        if (y < world.getMinHeight() + 1.0D) {
            y = world.getMinHeight() + 1.0D;
        }
        return new org.bukkit.Location(world, x, y, z, eye.getYaw(), eye.getPitch());
    }

    private void applyProjectileScale(Snowball projectile, double scale) {
        if (projectile == null || projectile.isDead() || projectile.isValid() == false) {
            return;
        }
        if (scale <= 0.0D) {
            return;
        }
        var item = projectile.getItem();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }
        var meta = item.getItemMeta();
        try {
            meta.setCustomModelData(Math.max(1, (int) Math.round(scale * 1000.0D)));
            item.setItemMeta(meta);
            projectile.setItem(item);
        } catch (Exception ignored) {
        }
    }

    private boolean castAoeSpell(Player caster, String template, int damage, int radius) {
        if (caster == null || !caster.isOnline()) {
            return false;
        }

        playTemplateEffects(template, "cast", caster.getLocation(), caster, null, null);

        var world = caster.getWorld();
        var origin = caster.getLocation();
        int hits = 0;
        for (var entity : world.getNearbyEntities(origin, radius, radius, radius, e -> e instanceof LivingEntity living && living.isValid() && !living.isDead())) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }
            if (target.getUniqueId().equals(caster.getUniqueId())) {
                continue;
            }
            if (caster.hasLineOfSight(target) || target.getLocation().distanceSquared(origin) <= (double) radius * radius) {
                target.damage(damage, caster);
                hits++;
            }
        }
        playTemplateEffectsInSphere(template, "hit", origin, caster, null, radius);
        world.spawnParticle(org.bukkit.Particle.FLAME, origin, Math.max(8, radius * 6), radius * 0.25D, 0.2D, radius * 0.25D, 0.02D);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        var pdc = projectile.getPersistentDataContainer();
        if (!pdc.has(projectileTagKey, PersistentDataType.BYTE)) {
            return;
        }

        String template = pdc.get(projectileTemplateKey, PersistentDataType.STRING);
        String ownerRaw = pdc.get(projectileOwnerKey, PersistentDataType.STRING);
        Integer damage = pdc.get(projectileDamageKey, PersistentDataType.INTEGER);
        if (ownerRaw == null || damage == null) {
            projectile.remove();
            return;
        }

        double impactRadius = plugin.getConfig().getDouble("spellcraft.templates." + template + ".impact-radius", 0.0D);
        org.bukkit.Location hitLocation = resolveImpactLocation(projectile, event);
        Player owner = null;
        try {
            owner = Bukkit.getPlayer(UUID.fromString(ownerRaw));
        } catch (IllegalArgumentException ignored) {
            owner = null;
        }

        if (impactRadius > 0.0D) {
            applyImpactDamageInRadius(hitLocation, owner, damage, impactRadius);
        } else if (event.getHitEntity() instanceof LivingEntity target) {
            try {
                UUID ownerId = UUID.fromString(ownerRaw);
                if (!target.getUniqueId().equals(ownerId)) {
                    if (owner != null && owner.isOnline()) {
                        target.damage(damage, owner);
                    } else {
                        target.damage(damage);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                target.damage(damage);
            }
        }
        if (template != null && !template.isBlank()) {
            if (impactRadius > 0.0D) {
                playTemplateEffectsInSphere(template, "hit", hitLocation, owner, projectile, impactRadius);
            } else {
                playTemplateEffects(template, "hit", hitLocation, owner, projectile, event.getHitEntity() instanceof LivingEntity living ? living : null);
            }
        }
        if (event.getHitBlock() != null && impactRadius > 0.0D && template != null && normalize(template).equals("meteor")) {
            playMeteorBlockImpact(hitLocation);
        }
        projectile.remove();
    }

    private void applyImpactDamageInRadius(org.bukkit.Location center, Player owner, int damage, double radius) {
        if (center == null || center.getWorld() == null || radius <= 0.0D) {
            return;
        }
        var world = center.getWorld();
        UUID ownerId = owner != null ? owner.getUniqueId() : null;
        for (var entity : world.getNearbyEntities(center, radius, radius, radius, e -> e instanceof LivingEntity living && living.isValid() && !living.isDead())) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }
            if (ownerId != null && target.getUniqueId().equals(ownerId)) {
                continue;
            }
            double distSq = target.getLocation().distanceSquared(center);
            if (distSq > radius * radius) {
                continue;
            }
            if (owner != null && owner.isOnline()) {
                target.damage(damage, owner);
            } else {
                target.damage(damage);
            }
        }
        world.spawnParticle(org.bukkit.Particle.EXPLOSION, center, Math.max(1, (int) Math.round(radius * 2.0D)), radius * 0.25D, radius * 0.18D, radius * 0.25D, 0.0D);
    }

    private void playMeteorBlockImpact(org.bukkit.Location center) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        var world = center.getWorld();
        world.playSound(center, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.25F, 0.85F);
        world.spawnParticle(org.bukkit.Particle.EXPLOSION, center, 4, 0.35D, 0.25D, 0.35D, 0.0D);
        world.spawnParticle(org.bukkit.Particle.FLAME, center, 32, 0.5D, 0.35D, 0.5D, 0.02D);
        world.spawnParticle(org.bukkit.Particle.SMOKE, center, 20, 0.45D, 0.30D, 0.45D, 0.01D);
        try {
            world.spawnParticle(org.bukkit.Particle.valueOf("EXPLOSION_EMITTER"), center, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        } catch (IllegalArgumentException ignored) {
        }
        ignitePartialGround(center, 4.0D, 60);
    }

    private void ignitePartialGround(org.bukkit.Location center, double radius, int fireTicks) {
        if (center == null || center.getWorld() == null || radius <= 0.0D || fireTicks <= 0) {
            return;
        }
        var world = center.getWorld();
        int minX = (int) Math.floor(center.getX() - radius);
        int maxX = (int) Math.ceil(center.getX() + radius);
        int minZ = (int) Math.floor(center.getZ() - radius);
        int maxZ = (int) Math.ceil(center.getZ() + radius);
        int targetY = center.getBlockY() - 1;
        int index = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = x + 0.5D - center.getX();
                double dz = z + 0.5D - center.getZ();
                if ((dx * dx) + (dz * dz) > radius * radius) {
                    continue;
                }
                if ((index++ % 3) != 0) {
                    continue;
                }
                var ground = world.getBlockAt(x, targetY, z);
                if (ground.isPassable() || ground.isLiquid()) {
                    continue;
                }
                var fireBlock = ground.getRelative(0, 1, 0);
                if (!fireBlock.getType().isAir()) {
                    continue;
                }
                fireBlock.setType(org.bukkit.Material.FIRE, false);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (fireBlock.getType() == org.bukkit.Material.FIRE) {
                        fireBlock.setType(org.bukkit.Material.AIR, false);
                    }
                }, fireTicks);
            }
        }
    }

    private org.bukkit.Location resolveImpactLocation(Projectile projectile, ProjectileHitEvent event) {
        if (projectile == null) {
            return null;
        }
        org.bukkit.Location location = projectile.getLocation().clone();
        if (event == null) {
            return location;
        }
        if (event.getHitBlock() == null) {
            return location;
        }
        org.bukkit.Location blockCenter = event.getHitBlock().getLocation().clone().add(0.5D, 0.5D, 0.5D);
        BlockFace face = event.getHitBlockFace();
        if (face != null) {
            blockCenter.add(face.getModX() * 0.5D, face.getModY() * 0.5D, face.getModZ() * 0.5D);
        }
        return blockCenter;
    }

    private void startProjectileTickEffects(Projectile projectile, int maxTicks) {
        AtomicInteger livedTicks = new AtomicInteger(0);
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!projectile.isValid() || projectile.isDead()) {
                task.cancel();
                return;
            }
            int ticks = livedTicks.incrementAndGet();
            if (ticks > maxTicks) {
                task.cancel();
                return;
            }
            String template = projectile.getPersistentDataContainer().get(projectileTemplateKey, PersistentDataType.STRING);
            if (template != null && !template.isBlank()) {
                Player owner = null;
                String ownerRaw = projectile.getPersistentDataContainer().get(projectileOwnerKey, PersistentDataType.STRING);
                if (ownerRaw != null) {
                    try {
                        owner = Bukkit.getPlayer(UUID.fromString(ownerRaw));
                    } catch (IllegalArgumentException ignored) {
                        owner = null;
                    }
                }
        playTemplateEffects(template, "tick", projectile.getLocation(), owner, projectile, null);
            }
        }, 1L, 1L);
    }

    private void playTemplateEffects(String template, String stage, org.bukkit.Location location, Player caster, Projectile projectile, LivingEntity target) {
        if (template == null || template.isBlank() || location == null || location.getWorld() == null) {
            return;
        }
        String path = "spellcraft.templates." + template + ".effects." + stage;
        for (String raw : plugin.getConfig().getStringList(path)) {
            applyEffectSpec(raw, location, caster, projectile, target);
        }
    }

    private void playTemplateEffectsInSphere(String template, String stage, org.bukkit.Location center, Player caster, Projectile projectile, double radius) {
        if (template == null || template.isBlank() || center == null || center.getWorld() == null) {
            return;
        }
        String path = "spellcraft.templates." + template + ".effects." + stage;
        for (String raw : plugin.getConfig().getStringList(path)) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] parts = raw.split("\\|");
            String kind = parts.length > 0 ? parts[0].trim().toLowerCase(Locale.ROOT) : "";
            if (!kind.equals("particle")) {
                applyEffectSpec(raw, center, caster, projectile, null);
                continue;
            }
            spawnParticleSphere(raw, center, radius);
        }
    }

    private void spawnParticleSphere(String raw, org.bukkit.Location center, double radius) {
        if (raw == null || raw.isBlank() || center == null || center.getWorld() == null) {
            return;
        }
        String[] parts = raw.split("\\|");
        if (parts.length < 7) {
            return;
        }
        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
            int amount = Math.max(1, Integer.parseInt(parts[2].trim()));
            double extra = Double.parseDouble(parts[3].trim());
            double offsetX = Double.parseDouble(parts[4].trim());
            double offsetY = Double.parseDouble(parts[5].trim());
            double offsetZ = Double.parseDouble(parts[6].trim());
            int samples = Math.max(1, amount * 4);
            for (int i = 0; i < samples; i++) {
                double u = Math.random();
                double v = Math.random();
                double theta = 2.0D * Math.PI * u;
                double phi = Math.acos(2.0D * v - 1.0D);
                double r = radius * Math.cbrt(Math.random());
                double x = r * Math.sin(phi) * Math.cos(theta);
                double y = r * Math.cos(phi);
                double z = r * Math.sin(phi) * Math.sin(theta);
                center.getWorld().spawnParticle(
                    particle,
                    center.getX() + x,
                    center.getY() + y,
                    center.getZ() + z,
                    amount,
                    offsetX,
                    offsetY,
                    offsetZ,
                    extra
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void applyEffectSpec(String raw, org.bukkit.Location location, Player caster, Projectile projectile, LivingEntity target) {
        if (raw == null || raw.isBlank() || location.getWorld() == null) {
            return;
        }
        String[] parts = raw.split("\\|");
        if (parts.length == 0) {
            return;
        }
        String kind = parts[0].trim().toLowerCase(Locale.ROOT);
        switch (kind) {
            case "sound" -> {
                if (parts.length < 4) {
                    return;
                }
                try {
                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
                    float volume = Float.parseFloat(parts[2].trim());
                    float pitch = Float.parseFloat(parts[3].trim());
                    location.getWorld().playSound(location, sound, volume, pitch);
                } catch (Exception ignored) {
                }
            }
            case "particle" -> {
                if (parts.length < 7) {
                    return;
                }
                try {
                    org.bukkit.Particle particle = org.bukkit.Particle.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
                    int amount = Math.max(1, Integer.parseInt(parts[2].trim()));
                    double extra = Double.parseDouble(parts[3].trim());
                    double offsetX = Double.parseDouble(parts[4].trim());
                    double offsetY = Double.parseDouble(parts[5].trim());
                    double offsetZ = Double.parseDouble(parts[6].trim());
                    location.getWorld().spawnParticle(particle, location, amount, offsetX, offsetY, offsetZ, extra);
                } catch (Exception ignored) {
                }
            }
            case "ignite" -> {
                if (target == null || parts.length < 2) {
                    return;
                }
                try {
                    int ticks = Math.max(1, Integer.parseInt(parts[1].trim()));
                    target.setFireTicks(ticks);
                } catch (Exception ignored) {
                }
            }
            case "potion" -> {
                if (target == null || parts.length < 5) {
                    return;
                }
                try {
                    PotionEffectType type = PotionEffectType.getByName(parts[1].trim().toUpperCase(Locale.ROOT));
                    if (type == null) {
                        return;
                    }
                    int amplifier = Math.max(0, Integer.parseInt(parts[2].trim()));
                    int duration = Math.max(1, Integer.parseInt(parts[3].trim()));
                    boolean ambient = Boolean.parseBoolean(parts[4].trim());
                    boolean particles = parts.length >= 6 ? Boolean.parseBoolean(parts[5].trim()) : true;
                    boolean icon = parts.length >= 7 ? Boolean.parseBoolean(parts[6].trim()) : true;
                    target.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles, icon), true);
                } catch (Exception ignored) {
                }
            }
            case "aura" -> {
                if (target == null || parts.length < 7) {
                    return;
                }
                try {
                    org.bukkit.Particle particle = org.bukkit.Particle.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
                    int duration = Math.max(1, Integer.parseInt(parts[2].trim()));
                    int amount = Math.max(1, Integer.parseInt(parts[3].trim()));
                    double offsetX = Double.parseDouble(parts[4].trim());
                    double offsetY = Double.parseDouble(parts[5].trim());
                    double offsetZ = Double.parseDouble(parts[6].trim());
                    double extra = parts.length >= 8 ? Double.parseDouble(parts[7].trim()) : 0.0D;
                    startTargetAura(target, particle, duration, amount, offsetX, offsetY, offsetZ, extra);
                } catch (Exception ignored) {
                }
            }
            default -> {
            }
        }
    }

    private void startTargetAura(LivingEntity target, org.bukkit.Particle particle, int durationTicks, int amount, double offsetX, double offsetY, double offsetZ, double extra) {
        if (target == null || !target.isValid() || target.isDead()) {
            return;
        }
        UUID uuid = target.getUniqueId();
        BukkitTask existing = targetAuraTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) {
                    cancel();
                    targetAuraTasks.remove(uuid);
                    return;
                }
                var loc = target.getLocation().add(0.0D, target.getHeight() * 0.65D, 0.0D);
                loc.getWorld().spawnParticle(particle, loc, amount, offsetX, offsetY, offsetZ, extra);
            }
        }.runTaskTimer(plugin, 0L, 1L);
        targetAuraTasks.put(uuid, task);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitTask current = targetAuraTasks.remove(uuid);
            if (current != null) {
                current.cancel();
            }
        }, durationTicks);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopCastBar(event.getPlayer());
        UUID playerId = event.getPlayer().getUniqueId();
        playersCasting.remove(playerId);
        lastInteractMs.remove(playerId);
        interactLockUntilMs.remove(playerId);
    }

    private double getDouble(PersistentDataContainer pdc, NamespacedKey key, double fallback) {
        Double value = pdc.get(key, PersistentDataType.DOUBLE);
        return value == null ? fallback : value;
    }

    private void setScore(Player player, String objectiveName, int value) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard();
        if (scoreboard == null) {
            return;
        }
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
        }
        objective.getScore(player.getName()).setScore(value);
    }

    private int getScore(Player player, String objectiveName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard();
        if (scoreboard == null) {
            return 0;
        }
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        Score score = objective.getScore(player.getName());
        return score.isScoreSet() ? score.getScore() : 0;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void debugSkillSelection(
        Player player,
        String template,
        String spellType,
        ProjectileVisualMode mode,
        String mythicOverride,
        String fallbackSkill,
        String selectedSkill,
        boolean usedFallback,
        int baseMagicDamage,
        double damageMultiplier,
        int finalDamage,
        int radius
    ) {
        if (!plugin.getConfig().getBoolean("spellcraft.debug.fallback", false)) {
            return;
        }
        String source = usedFallback ? "fallback" : "template";
        String msg = "[SpellDebug] template=" + template
            + " type=" + spellType
            + " mode=" + mode
            + " source=" + source
            + " selected=" + selectedSkill
            + " templateSkill=" + (mythicOverride == null || mythicOverride.isBlank() ? "-" : mythicOverride)
            + " fallbackSkill=" + (fallbackSkill == null || fallbackSkill.isBlank() ? "-" : fallbackSkill)
            + " baseMagic=" + baseMagicDamage
            + " mult=" + fmt(damageMultiplier)
            + " finalDamage=" + finalDamage
            + (spellType.equals("attack_aoe") ? " radius=" + radius : "");
        player.sendMessage(Component.text(msg, usedFallback ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
        plugin.getLogger().info(msg + " player=" + player.getName());
    }

    private ProjectileVisualMode parseProjectileVisualMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProjectileVisualMode.ITEM;
        }
        try {
            return ProjectileVisualMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ProjectileVisualMode.ITEM;
        }
    }

    private ProjectileVisualMode getProjectileVisualMode() {
        return parseProjectileVisualMode(plugin.getConfig().getString("spellcraft.projectile-visual.mode", "ITEM"));
    }

    private String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
