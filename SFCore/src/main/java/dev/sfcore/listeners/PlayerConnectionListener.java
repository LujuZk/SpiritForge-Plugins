package dev.sfcore.listeners;

import dev.sfcore.managers.StatManager;
import dev.sfcore.managers.TestMonitorManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final StatManager statManager;
    private final TestMonitorManager testMonitor;

    public PlayerConnectionListener(StatManager statManager, TestMonitorManager testMonitor) {
        this.statManager = statManager;
        this.testMonitor = testMonitor;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        statManager.loadPlayer(player.getUniqueId());
        statManager.reapplyAll(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        statManager.saveAndUnload(uuid);
        testMonitor.disableAll(uuid);
    }
}
