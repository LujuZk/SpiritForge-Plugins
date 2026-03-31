package dev.sfcrafting;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class RecipeBookHolder implements InventoryHolder {

    private final UUID playerUuid;
    private ForgeState.StationType category;
    private int page;
    private Inventory inventory;

    public RecipeBookHolder(UUID playerUuid, ForgeState.StationType category, int page) {
        this.playerUuid = playerUuid;
        this.category = category;
        this.page = page;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public ForgeState.StationType category() {
        return category;
    }

    public void setCategory(ForgeState.StationType category) {
        this.category = category;
    }

    public int page() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
