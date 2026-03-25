package dev.sfcompass.listeners;

import dev.sfcompass.managers.CompassManager;
import dev.sfcompass.managers.IslandManager;
import dev.sfcompass.models.Island;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ZoneVisualTask extends BukkitRunnable {

    // Cómo de lejos (bloques) puede verse la pared antes de entrar al warning range
    private static final int WALL_VIEW_EXTRA = 48;

    private final CompassManager compassManager;
    private final IslandManager islandManager;

    private final int warningRadius;
    private final boolean bossbarEnabled;
    private final BarColor bossbarColor;
    private final boolean particlesEnabled;
    private final Color borderParticleColor;
    private final float particleSize;
    private final double wallSpacing;   // bloques entre puntos del arco de la pared
    private final boolean darknessEnabled;
    private final boolean zoneParticlesEnabled;
    private final int zoneParticleCount;

    // Active boss bars: playerUUID -> (islandId -> BossBar)
    private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();
    private final Random random = new Random();

    // Tick counter: la pared se refresca cada 4 ticks; el resto cada 20
    private int tickCount = 0;

    public ZoneVisualTask(CompassManager compassManager, IslandManager islandManager,
                          int warningRadius, boolean bossbarEnabled, BarColor bossbarColor,
                          boolean particlesEnabled, Color borderParticleColor, float particleSize,
                          double wallSpacing, boolean darknessEnabled,
                          boolean zoneParticlesEnabled, int zoneParticleCount) {
        this.compassManager = compassManager;
        this.islandManager = islandManager;
        this.warningRadius = warningRadius;
        this.bossbarEnabled = bossbarEnabled;
        this.bossbarColor = bossbarColor;
        this.particlesEnabled = particlesEnabled;
        this.borderParticleColor = borderParticleColor;
        this.particleSize = particleSize;
        this.wallSpacing = wallSpacing;
        this.darknessEnabled = darknessEnabled;
        this.zoneParticlesEnabled = zoneParticlesEnabled;
        this.zoneParticleCount = zoneParticleCount;
    }

    @Override
    public void run() {
        tickCount++;
        // Efectos "lentos" (bossbar, darkness, niebla de zona): cada 5 llamadas = 20 ticks
        boolean doSlowEffects = (tickCount % 5 == 0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) continue;

            int playerLevel = compassManager.getLevel(player.getUniqueId());
            double playerX = player.getLocation().getX();
            double playerZ = player.getLocation().getZ();
            double playerY = player.getLocation().getY();
            World playerWorld = player.getWorld();

            for (Island island : islandManager.getAllIslands()) {
                if (!island.worldName().equals(playerWorld.getName())) continue;

                if (playerLevel >= island.requiredLevel()) {
                    removeBossBar(player, island.id());
                    continue;
                }

                double dist = island.distanceTo(playerX, playerZ);
                double outerRadius = island.outerRadius();
                double wallRadius = outerRadius + warningRadius;
                // Distancia al borde de la pared (negativo = dentro, positivo = fuera)
                double distToWall = dist - wallRadius;
                // Distancia al borde de la zona de daño
                double distToEdge = dist - outerRadius;

                // ── Pared de partículas ───────────────────────────────────────────────
                // Visible ANTES de entrar (WALL_VIEW_EXTRA bloques de antelación)
                // y desde dentro del warning range
                if (particlesEnabled && distToWall <= WALL_VIEW_EXTRA && distToEdge >= 0) {
                    spawnBorderWall(player, island, playerX, playerZ, playerY, wallRadius);
                }

                // ── Efectos lentos: solo dentro del warning range ─────────────────────
                boolean inWarningRange = distToEdge >= 0 && distToEdge <= warningRadius;

                if (!inWarningRange) {
                    if (doSlowEffects) removeBossBar(player, island.id());
                    continue;
                }

                if (doSlowEffects) {
                    // BossBar
                    if (bossbarEnabled) {
                        String title = "§c⚠ " + island.displayName() + " §7— Nivel requerido: §f" + island.requiredLevel();
                        // 0 = lejos del borde, 1 = justo en el borde de daño
                        double progress = 1.0 - (distToEdge / warningRadius);
                        updateBossBar(player, island.id(), title, progress);
                    }

                    // DARKNESS: se aplica cada 20 ticks con duración 25 → pulsa y decae
                    // naturalmente, mucho más suave que la BLINDNESS de la zona de daño
                    if (darknessEnabled) {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.DARKNESS, 25, 0, true, false, false));
                    }

                    // Niebla atmosférica dentro del volumen
                    if (zoneParticlesEnabled) {
                        spawnZoneAtmosphere(player, island, playerY);
                    }
                }
            }
        }
    }

    /**
     * Pared vertical de partículas DUST en el borde de la zona de advertencia.
     * Cubre ±45° del arco más cercano al jugador, con densidad fija (wallSpacing bloques).
     * Visible desde fuera (jugador acercándose) y desde dentro (advertencia al alejarse).
     */
    private void spawnBorderWall(Player player, Island island,
                                  double playerX, double playerZ, double playerY,
                                  double wallRadius) {
        // Ángulo desde el centro de la isla hasta el jugador
        double playerAngle = Math.atan2(playerZ - island.centerZ(), playerX - island.centerX());
        Particle.DustOptions dust = new Particle.DustOptions(borderParticleColor, particleSize);

        // Arco visible: ±45° centrado en la dirección del jugador
        double arcHalf = Math.PI / 4; // 45°
        double arcLength = wallRadius * 2 * arcHalf; // longitud en bloques
        // 1 punto cada wallSpacing bloques, máximo 200 para no saturar
        int numPoints = Math.min(200, Math.max(20, (int) (arcLength / wallSpacing)));

        for (int i = 0; i <= numPoints; i++) {
            double t = (double) i / numPoints; // 0..1
            double angle = playerAngle - arcHalf + t * 2 * arcHalf;

            double px = island.centerX() + wallRadius * Math.cos(angle);
            double pz = island.centerZ() + wallRadius * Math.sin(angle);

            // Columna vertical: -2 a +8 sobre la altura del jugador (6 niveles)
            for (double dy = -2; dy <= 8; dy += 2) {
                player.spawnParticle(Particle.DUST, px, playerY + dy, pz, 1, 0, 0, 0, 0, dust);
            }
        }
    }

    /**
     * Partículas ASH + LARGE_SMOKE dispersas dentro del volumen de la zona.
     * Crean el efecto de "isla oscura/neblinosa" visible desde lejos.
     */
    private void spawnZoneAtmosphere(Player player, Island island, double playerY) {
        double r = island.outerRadius();

        for (int i = 0; i < zoneParticleCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double d = r * Math.sqrt(random.nextDouble());
            double px = island.centerX() + d * Math.cos(angle);
            double pz = island.centerZ() + d * Math.sin(angle);
            double py = playerY + random.nextDouble() * 22 - 2;

            if (i % 3 == 0) {
                player.spawnParticle(Particle.LARGE_SMOKE, px, py, pz, 1, 0.3, 0.3, 0.3, 0.005);
            } else {
                player.spawnParticle(Particle.ASH, px, py, pz, 1, 0.5, 0.5, 0.5, 0.01);
            }
        }
    }

    // ── BossBar helpers ───────────────────────────────────────────────────────────

    private void updateBossBar(Player player, String islandId, String title, double progress) {
        Map<String, BossBar> playerBars = activeBars.computeIfAbsent(
                player.getUniqueId(), k -> new HashMap<>());
        BossBar bar = playerBars.get(islandId);
        if (bar == null) {
            bar = Bukkit.createBossBar(title, bossbarColor, BarStyle.SOLID);
            bar.addPlayer(player);
            playerBars.put(islandId, bar);
        } else {
            bar.setTitle(title);
        }
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    private void removeBossBar(Player player, String islandId) {
        Map<String, BossBar> playerBars = activeBars.get(player.getUniqueId());
        if (playerBars == null) return;
        BossBar bar = playerBars.remove(islandId);
        if (bar != null) bar.removeAll();
    }

    /** Llamado en PlayerQuitEvent para evitar leaks de boss bars. */
    public void cleanup(Player player) {
        Map<String, BossBar> playerBars = activeBars.remove(player.getUniqueId());
        if (playerBars != null) {
            playerBars.values().forEach(BossBar::removeAll);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────────

    private static double angleDiff(double a, double b) {
        double diff = Math.abs(a - b) % (2 * Math.PI);
        return diff > Math.PI ? 2 * Math.PI - diff : diff;
    }
}
