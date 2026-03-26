package dev.sfcrafting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public final class AuraListener implements Listener {

    private static final long DOUBLE_SHIFT_WINDOW_MS = 350L;

    private final AuraManager auraManager;
    private final Map<UUID, Long> lastSneakPress = new HashMap<>();

    public AuraListener(AuraManager auraManager) {
        this.auraManager = auraManager;
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long previous = lastSneakPress.put(id, now);
        if (previous == null) {
            return;
        }

        if (now - previous > DOUBLE_SHIFT_WINDOW_MS) {
            return;
        }

        lastSneakPress.remove(id);
        boolean enabled = auraManager.toggle(player);
        player.sendMessage(enabled
            ? ChatColor.AQUA + "Aura activada."
            : ChatColor.AQUA + "Aura desactivada.");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!auraManager.isActive(event.getPlayer())) {
            return;
        }
        if (event.getFrom().getX() == event.getTo().getX()
            && event.getFrom().getY() == event.getTo().getY()
            && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }
        disableByAction(event.getPlayer(), "Aura desactivada por movimiento.");
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!auraManager.isActive(player)) {
            return;
        }
        disableByAction(player, "Aura desactivada por recibir dano.");
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = resolveDamagingPlayer(event.getDamager());
        if (attacker == null || !auraManager.isActive(attacker)) {
            return;
        }
        disableByAction(attacker, "Aura desactivada por atacar.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastSneakPress.remove(player.getUniqueId());
        auraManager.deactivate(player);
    }

    private Player resolveDamagingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private void disableByAction(Player player, String reason) {
        if (auraManager.deactivate(player)) {
            player.sendMessage(ChatColor.YELLOW + reason);
        }
    }
}

