package dev.sfcore.listeners;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.TextDisplay;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;

public class CombatStatListener implements Listener {

    private final JavaPlugin plugin;
    private final boolean damagePopupsEnabled;
    private final boolean showPlayerDamagePopups;
    private final double popupYOffset;
    private final double popupRisePerTick;
    private final long popupDurationTicks;

    public CombatStatListener(JavaPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        ConfigurationSection popupSection = config != null ? config.getConfigurationSection("damage-popups") : null;
        this.damagePopupsEnabled = popupSection == null || popupSection.getBoolean("enabled", true);
        this.showPlayerDamagePopups = popupSection != null && popupSection.getBoolean("show-players", false);
        this.popupYOffset = popupSection == null ? 0.45D : popupSection.getDouble("y-offset", 0.45D);
        this.popupRisePerTick = popupSection == null ? 0.025D : popupSection.getDouble("rise-per-tick", 0.025D);
        this.popupDurationTicks = popupSection == null ? 20L : Math.max(5L, popupSection.getLong("duration-ticks", 20L));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        double lifesteal = SFCoreAPI.get().getTotal(player, StatType.LIFESTEAL);
        if (lifesteal <= 0) return;

        double heal = event.getFinalDamage() * lifesteal;
        var maxHealthInst = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthInst == null) return;

        double maxHp = maxHealthInst.getValue();
        double healCapped = Math.min(heal, maxHp - player.getHealth());
        if (healCapped > 0) player.setHealth(player.getHealth() + healCapped);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamagePopup(EntityDamageEvent event) {
        if (!damagePopupsEnabled) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!showPlayerDamagePopups && living instanceof Player) return;

        double damage = event.getFinalDamage();
        if (damage <= 0.0D) return;

        spawnDamagePopup(living, damage);
    }

    private void spawnDamagePopup(LivingEntity target, double damage) {
        Location spawnLocation = target.getEyeLocation().clone().add(0.0D, popupYOffset, 0.0D);
        String formattedDamage = String.format(Locale.ROOT, "%.1f", damage);

        TextDisplay display = target.getWorld().spawn(spawnLocation, TextDisplay.class, text -> {
            text.text(Component.text("-" + formattedDamage, NamedTextColor.RED));
            text.setBillboard(Display.Billboard.CENTER);
            text.setSeeThrough(true);
            text.setShadowed(false);
            text.setInterpolationDelay(0);
            text.setInterpolationDuration(1);
            text.setViewRange(24.0F);
        });

        new BukkitRunnable() {
            private long age = 0L;
            private Location current = spawnLocation.clone();

            @Override
            public void run() {
                if (!display.isValid() || age >= popupDurationTicks) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    cancel();
                    return;
                }

                current = current.clone().add(0.0D, popupRisePerTick, 0.0D);
                display.teleport(current);
                age++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
