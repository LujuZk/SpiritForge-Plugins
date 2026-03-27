package dev.sfcharacter.listeners;

import dev.sfcharacter.SFCharacterPlugin;
import dev.sfcharacter.managers.CharacterManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final SFCharacterPlugin plugin;

    public PlayerConnectionListener(SFCharacterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CharacterManager manager = plugin.getCharacterManager();
        manager.loadPlayer(player.getUniqueId());

        // Always send to lobby and show character selection
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            manager.clearAndSendToLobby(player);
            plugin.getSelectGUI().open(player);
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CharacterManager manager = plugin.getCharacterManager();

        // Save state only if player has active character and is NOT in selection screen
        if (manager.hasActiveCharacter(player.getUniqueId())
                && !manager.isInCharacterSelection(player.getUniqueId())) {
            manager.saveCharacterState(player);
        }

        manager.unloadPlayer(player.getUniqueId());
    }
}
