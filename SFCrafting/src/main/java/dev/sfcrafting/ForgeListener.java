package dev.sfcrafting;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class ForgeListener implements Listener {

    private final ForgeManager manager;

    public ForgeListener(ForgeManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onForgeEntityInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        ForgeState.StationType type = manager.getStationTypeForEntity(entity);
        if (type == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (entity instanceof ItemDisplay display) {
            display.setRotation(player.getLocation().getYaw(), 0.0f);
        }
        manager.openForge(player, entity.getLocation(), type);
    }

    @EventHandler
    public void onForgeBarrierInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Block clicked = event.getClickedBlock();
        ForgeState.StationType blockType = manager.getStationTypeForBlock(clicked);
        if (blockType != null) {
            event.setCancelled(true);
            manager.openForge(event.getPlayer(), clicked.getLocation(), blockType);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            Entity forgeEntity = manager.findForgeEntityAt(clicked.getLocation());
            if (forgeEntity == null) {
                return;
            }

            ForgeState.StationType entityType = manager.getStationTypeForEntity(forgeEntity);
            if (entityType == null) {
                return;
            }

            event.setCancelled(true);
            manager.openForge(event.getPlayer(), forgeEntity.getLocation(), entityType);
        }
    }

    @EventHandler
    public void onForgeDisplaySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof ItemDisplay display)) {
            return;
        }

        if (!manager.isForgeEntity(display)) {
            return;
        }

        float yaw = display.getLocation().getYaw();
        display.setRotation(yaw, 0.0f);
    }

    @EventHandler
    public void onForgeItemDrop(PlayerDropItemEvent event) {
        if (!manager.isPendingItem(event.getItemDrop().getItemStack())) {
            return;
        }
        manager.scheduleTempering(event.getItemDrop());
    }

    @EventHandler
    public void onForgeInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (event.getWhoClicked() instanceof Player player) {
            manager.hotItemManager().maybeCoolPlayer(player.getInventory());
        }

        if (!manager.isForgeInventory(inventory)) {
            return;
        }

        ForgeState state = manager.getStateForInventory(inventory);
        if (state == null) {
            return;
        }

        if (event.isShiftClick()
            && event.getClickedInventory() != null
            && event.getClickedInventory() != inventory) {
            event.setCancelled(true);
            if (manager.isRunning(state)) {
                return;
            }
            handleShiftIntoForgeInputs(event, inventory, state);
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (manager.isButtonSlot(state, slot)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                manager.handleButtonClick(inventory, player);
            }
            return;
        }

        if (manager.isRunning(state)) {
            event.setCancelled(true);
            return;
        }

        if (manager.isInputSlot(state, slot) || manager.isOutputSlot(state, slot)) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
    }

    private void handleShiftIntoForgeInputs(InventoryClickEvent event, Inventory topInventory, ForgeState state) {
        ItemStack source = event.getCurrentItem();
        if (source == null || source.getType().isAir()) {
            return;
        }

        int targetSlot = findShiftTargetSlot(topInventory, state, source, true);
        if (targetSlot < 0) {
            targetSlot = findShiftTargetSlot(topInventory, state, source, false);
        }
        if (targetSlot < 0) {
            return;
        }

        ItemStack target = topInventory.getItem(targetSlot);
        int maxStack = source.getMaxStackSize();
        int movable;
        if (target == null || target.getType().isAir()) {
            movable = Math.min(source.getAmount(), maxStack);
            topInventory.setItem(targetSlot, source.asQuantity(movable));
        } else {
            int free = maxStack - target.getAmount();
            if (free <= 0) {
                return;
            }
            movable = Math.min(source.getAmount(), free);
            target.setAmount(target.getAmount() + movable);
            topInventory.setItem(targetSlot, target);
        }

        int remaining = source.getAmount() - movable;
        if (remaining <= 0) {
            event.setCurrentItem(null);
        } else {
            source.setAmount(remaining);
            event.setCurrentItem(source);
        }
    }

    private int findShiftTargetSlot(Inventory topInventory, ForgeState state, ItemStack source, boolean preferMerge) {
        for (int slot = 0; slot < topInventory.getSize(); slot++) {
            if (!manager.isInputSlot(state, slot) || !manager.canShiftPlaceInInputSlot(state, slot, source)) {
                continue;
            }
            ItemStack target = topInventory.getItem(slot);
            boolean empty = target == null || target.getType().isAir();
            if (preferMerge) {
                if (!empty && target.isSimilar(source) && target.getAmount() < source.getMaxStackSize()) {
                    return slot;
                }
            } else {
                if (empty) {
                    return slot;
                }
            }
        }
        return -1;
    }

    @EventHandler
    public void onForgeInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!manager.isForgeInventory(inventory)) {
            return;
        }

        ForgeState state = manager.getStateForInventory(inventory);
        if (state == null) {
            return;
        }

        int topSize = inventory.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 0 || rawSlot >= topSize) {
                continue;
            }

            boolean invalidTarget = manager.isRunning(state)
                    || manager.isButtonSlot(state, rawSlot)
                    || manager.isOutputSlot(state, rawSlot)
                    || !manager.isInputSlot(state, rawSlot);
            if (invalidTarget) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onForgeInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!manager.isForgeInventory(inventory)) {
            return;
        }
        manager.handleInventoryClose(inventory);
    }
}
