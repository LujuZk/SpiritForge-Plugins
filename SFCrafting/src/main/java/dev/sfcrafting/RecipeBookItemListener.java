package dev.sfcrafting;

import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class RecipeBookItemListener implements Listener {

    private final RecipeBookManager manager;
    private final OraxenItemResolver resolver;
    private final String openItemId;

    public RecipeBookItemListener(Plugin plugin, RecipeBookManager manager, OraxenItemResolver resolver) {
        this.manager = manager;
        this.resolver = resolver;
        this.openItemId = plugin.getConfig().getString("forge.recetario.open-item-id", "recetario_book")
                .trim().toLowerCase(Locale.ROOT);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (openItemId.isBlank()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            return;
        }
        if (!resolver.isOraxenItem(item, openItemId)) {
            return;
        }
        event.setCancelled(true);
        manager.openRecipeBook(player, ForgeState.StationType.SMELTER, 0);
    }
}
