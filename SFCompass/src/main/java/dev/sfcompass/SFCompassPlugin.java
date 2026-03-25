package dev.sfcompass;

import dev.sfcompass.commands.CompassCommand;
import dev.sfcompass.database.CompassDatabase;
import dev.sfcompass.listeners.CompassListener;
import dev.sfcompass.listeners.ZoneEnforcerTask;
import dev.sfcompass.listeners.ZoneVisualTask;
import dev.sfcompass.managers.CompassManager;
import dev.sfcompass.managers.IslandManager;
import org.bukkit.Color;
import org.bukkit.boss.BarColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SFCompassPlugin extends JavaPlugin implements Listener {

    private CompassDatabase db;
    private IslandManager islandManager;
    private CompassManager compassManager;
    private ZoneEnforcerTask zoneEnforcerTask;
    private ZoneVisualTask zoneVisualTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String dbFile = getConfig().getString("database.file", "compass.db");
        db = new CompassDatabase(getDataFolder(), dbFile);

        islandManager = new IslandManager(getConfig());
        compassManager = new CompassManager(this, db);

        CompassCommand cmd = new CompassCommand(this);
        var compassCmd = getCommand("compass");
        if (compassCmd != null) {
            compassCmd.setExecutor(cmd);
            compassCmd.setTabCompleter(cmd);
        }

        var pm = getServer().getPluginManager();
        CompassListener compassListener = new CompassListener(compassManager);
        pm.registerEvents(compassListener, this);
        pm.registerEvents(this, this);

        // Zone enforcer (damage / blindness / nausea)
        int tickInterval = getConfig().getInt("zone-effects.damage-tick-interval", 10);
        double baseDmg = getConfig().getDouble("zone-effects.base-damage-percent", 0.05);
        double scaleFactor = getConfig().getDouble("zone-effects.scale-factor", 5.0);
        int blindness = getConfig().getInt("zone-effects.blindness-duration", 60);
        int nausea = getConfig().getInt("zone-effects.nausea-duration", 100);

        zoneEnforcerTask = new ZoneEnforcerTask(compassManager, islandManager,
                baseDmg, scaleFactor, blindness, nausea);
        zoneEnforcerTask.runTaskTimer(this, 20L, tickInterval);

        // Zone visual task (bossbar, darkness, particles)
        int warningRadius = getConfig().getInt("zone-warning.warning-radius", 30);
        boolean bossbarEnabled = getConfig().getBoolean("zone-warning.bossbar-enabled", true);
        BarColor bossbarColor = parseBarColor(getConfig().getString("zone-warning.bossbar-color", "RED"));
        boolean particlesEnabled = getConfig().getBoolean("zone-warning.particles-enabled", true);
        int r = getConfig().getInt("zone-warning.particle-color.r", 255);
        int g = getConfig().getInt("zone-warning.particle-color.g", 80);
        int b = getConfig().getInt("zone-warning.particle-color.b", 30);
        float particleSize = (float) getConfig().getDouble("zone-warning.particle-size", 2.5);
        double wallSpacing = getConfig().getDouble("zone-warning.particle-wall-spacing", 2.0);
        boolean darknessEnabled = getConfig().getBoolean("zone-warning.darkness-enabled", true);
        boolean zoneParticlesEnabled = getConfig().getBoolean("zone-warning.zone-particles-enabled", true);
        int zoneParticleCount = getConfig().getInt("zone-warning.zone-particle-count", 20);

        zoneVisualTask = new ZoneVisualTask(compassManager, islandManager,
                warningRadius, bossbarEnabled, bossbarColor,
                particlesEnabled, Color.fromRGB(r, g, b), particleSize,
                wallSpacing, darknessEnabled,
                zoneParticlesEnabled, zoneParticleCount);
        // 4 ticks: refresca la pared antes de que expiren las partículas DUST
        zoneVisualTask.runTaskTimer(this, 20L, 4L);

        compassListener.setZoneVisualTask(zoneVisualTask);

        getLogger().info("SFCompass enabled — " + islandManager.getAllIslands().size() + " islands loaded.");
    }

    @Override
    public void onDisable() {
        if (zoneEnforcerTask != null) zoneEnforcerTask.cancel();
        if (zoneVisualTask != null) zoneVisualTask.cancel();
        if (db != null) db.close();
        getLogger().info("SFCompass disabled.");
    }

    private static BarColor parseBarColor(String name) {
        try {
            return BarColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.RED;
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (zoneEnforcerTask != null) {
            zoneEnforcerTask.addRespawnGrace(event.getPlayer().getUniqueId(), this);
        }
    }

    public CompassManager getCompassManager() {
        return compassManager;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }
}
