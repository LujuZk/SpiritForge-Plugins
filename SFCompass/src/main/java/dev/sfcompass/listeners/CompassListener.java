package dev.sfcompass.listeners;

import dev.sfcompass.managers.CompassManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CompassListener implements Listener {

    private final CompassManager compassManager;
    private ZoneVisualTask zoneVisualTask;

    public CompassListener(CompassManager compassManager) {
        this.compassManager = compassManager;
    }

    public void setZoneVisualTask(ZoneVisualTask zoneVisualTask) {
        this.zoneVisualTask = zoneVisualTask;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        compassManager.loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        compassManager.unloadPlayer(event.getPlayer().getUniqueId());
        if (zoneVisualTask != null) {
            zoneVisualTask.cleanup(event.getPlayer());
        }
    }
}
