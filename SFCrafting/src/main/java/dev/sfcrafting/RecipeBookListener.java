package dev.sfcrafting;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class RecipeBookListener implements Listener {

    private final RecipeBookManager manager;

    public RecipeBookListener(RecipeBookManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof RecipeBookHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= RecipeBookGUI.SIZE) {
            return;
        }

        if (slot == RecipeBookGUI.SLOT_PREV) {
            int newPage = holder.page() - 1;
            if (newPage >= 0) {
                manager.openRecipeBook(player, holder.category(), newPage);
            }
        } else if (slot == RecipeBookGUI.SLOT_NEXT) {
            manager.openRecipeBook(player, holder.category(), holder.page() + 1);
        } else if (slot == RecipeBookGUI.SLOT_TAB_SMELTER) {
            if (holder.category() != ForgeState.StationType.SMELTER) {
                manager.openRecipeBook(player, ForgeState.StationType.SMELTER, 0);
            }
        } else if (slot == RecipeBookGUI.SLOT_TAB_ANVIL) {
            if (holder.category() != ForgeState.StationType.ANVIL) {
                manager.openRecipeBook(player, ForgeState.StationType.ANVIL, 0);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RecipeBookHolder) {
            event.setCancelled(true);
        }
    }
}
