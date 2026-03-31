package dev.sfcrafting;

import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class DiscoveryListener implements Listener {

    private final Plugin plugin;
    private final RecipeBookManager manager;
    private final OraxenItemResolver resolver;
    private final String discoveryMessage;

    public DiscoveryListener(Plugin plugin, RecipeBookManager manager, OraxenItemResolver resolver) {
        this.plugin = plugin;
        this.manager = manager;
        this.resolver = resolver;
        this.discoveryMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("forge.recetario.discovery-message",
                        "&a¡Has descubierto: &e%material%&a!"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.saveAndUnload(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        checkDiscovery(player, event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof RecipeBookHolder) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current != null && !current.getType().isAir()) {
            checkDiscovery(player, current);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        checkDiscovery(player, item);
                    }
                }
            }
        }, 1L);
    }

    private void checkDiscovery(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        String materialId = resolveMaterialId(item);
        if (materialId == null || !manager.isTrackable(materialId)) {
            return;
        }
        if (manager.addDiscovery(player.getUniqueId(), materialId)) {
            String msg = discoveryMessage.replace("%material%", materialId);
            player.sendMessage(msg);
        }
    }

    private String resolveMaterialId(ItemStack item) {
        String oraxenId = resolver.readOraxenId(item);
        if (oraxenId != null && !oraxenId.isBlank()) {
            return oraxenId.toLowerCase(Locale.ROOT);
        }
        return item.getType().name().toLowerCase(Locale.ROOT);
    }
}
