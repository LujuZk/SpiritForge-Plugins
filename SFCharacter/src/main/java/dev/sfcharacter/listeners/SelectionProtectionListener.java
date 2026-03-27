package dev.sfcharacter.listeners;

import dev.sfcharacter.SFCharacterPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class SelectionProtectionListener implements Listener {

    private final SFCharacterPlugin plugin;

    public SelectionProtectionListener(SFCharacterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!isInSelection(event.getPlayer())) return;
        // Allow head rotation, block position change
        if (event.getFrom().toBlockLocation().equals(event.getTo().toBlockLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (isInSelection(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isInSelection(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isInSelection(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isInSelection(event.getPlayer())) return;

        String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/character") || cmd.startsWith("/personaje") || cmd.startsWith("/char ")) {
            return; // allow
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(
                Component.text("Debes seleccionar un personaje primero.", NamedTextColor.RED));
    }

    private boolean isInSelection(Player player) {
        return plugin.getCharacterManager().isInCharacterSelection(player.getUniqueId());
    }
}
