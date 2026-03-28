package dev.sfcrafting;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
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
import org.bukkit.inventory.PlayerInventory;

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
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemDisplay display)) {
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
        Item dropped = event.getItemDrop();
        if (!manager.isPendingItem(dropped.getItemStack())) {
            return;
        }

        manager.scheduleTempering(dropped);
    }

    @EventHandler
    public void onForgeInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();

        HumanEntity clicker = event.getWhoClicked();
        if (clicker instanceof Player player) {
            PlayerInventory playerInventory = player.getInventory();
            manager.hotItemManager().maybeCoolPlayer(playerInventory);
        }

        if (!manager.isForgeInventory(inventory)) {
            return;
        }

        ForgeState state = manager.getStateForInventory(inventory);
        if (state == null) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (manager.isButtonSlot(state, slot)) {
            event.setCancelled(true);
            HumanEntity whoClicked = event.getWhoClicked();
            if (whoClicked instanceof Player player) {
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