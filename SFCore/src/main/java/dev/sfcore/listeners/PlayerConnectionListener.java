package dev.sfcore.listeners;

import dev.sfcore.managers.StatManager;
import dev.sfcore.managers.TestMonitorManager;
import dev.sfcore.managers.ManaManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final StatManager statManager;
    private final TestMonitorManager testMonitor;
    private final ManaManager manaManager;

    public PlayerConnectionListener(StatManager statManager, TestMonitorManager testMonitor, ManaManager manaManager) {
        this.statManager = statManager;
        this.testMonitor = testMonitor;
        this.manaManager = manaManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        statManager.loadPlayer(player);
        statManager.reapplyAll(player);
        if (manaManager != null) {
            manaManager.loadPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        statManager.saveAndUnload(uuid);
        if (manaManager != null) {
            manaManager.saveAndUnload(uuid);
        }
        testMonitor.disableAll(uuid);
    }
}
