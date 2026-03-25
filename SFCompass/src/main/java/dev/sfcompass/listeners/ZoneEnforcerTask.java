package dev.sfcompass.listeners;

import dev.sfcompass.managers.CompassManager;
import dev.sfcompass.managers.IslandManager;
import dev.sfcompass.models.Island;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneEnforcerTask extends BukkitRunnable {

    private final CompassManager compassManager;
    private final IslandManager islandManager;
    private final double baseDamagePercent;
    private final double scaleFactor;
    private final int blindnessDuration;
    private final int nauseaDuration;
    private final Set<UUID> respawnGrace = ConcurrentHashMap.newKeySet();

    public ZoneEnforcerTask(CompassManager compassManager, IslandManager islandManager,
                            double baseDamagePercent, double scaleFactor,
                            int blindnessDuration, int nauseaDuration) {
        this.compassManager = compassManager;
        this.islandManager = islandManager;
        this.baseDamagePercent = baseDamagePercent;
        this.scaleFactor = scaleFactor;
        this.blindnessDuration = blindnessDuration;
        this.nauseaDuration = nauseaDuration;
    }

    public void addRespawnGrace(UUID uuid, org.bukkit.plugin.Plugin plugin) {
        respawnGrace.add(uuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> respawnGrace.remove(uuid), 60L); // 3 seconds
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) continue;
            if (respawnGrace.contains(player.getUniqueId())) continue;

            int playerLevel = compassManager.getLevel(player.getUniqueId());
            double playerX = player.getLocation().getX();
            double playerZ = player.getLocation().getZ();

            for (Island island : islandManager.getAllIslands()) {
                if (!island.worldName().equals(player.getWorld().getName())) continue;
                if (playerLevel >= island.requiredLevel()) continue;

                double pen = island.penetration(playerX, playerZ);

                if (pen == -1) {
                    // Outside danger zone
                    continue;
                }

                double damageMultiplier;
                if (pen == -2) {
                    // Inside island without level — maximum damage (anti-teleport)
                    damageMultiplier = 1.0 + scaleFactor;
                } else {
                    // In buffer zone: scales with penetration
                    damageMultiplier = 1.0 + pen * scaleFactor;
                }

                double maxHP = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                double damage = maxHP * baseDamagePercent * damageMultiplier;

                player.damage(damage);

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS, blindnessDuration, 0,
                        true, false, false));

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.NAUSEA, nauseaDuration, 0,
                        true, false, false));
            }
        }
    }
}
