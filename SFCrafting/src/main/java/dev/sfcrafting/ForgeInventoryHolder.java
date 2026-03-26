package dev.sfcrafting;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ForgeInventoryHolder implements InventoryHolder {

    private final Location location;
    private Inventory inventory;

    public ForgeInventoryHolder(Location location) {
        this.location = location;
    }

    public Location location() {
        return location;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

