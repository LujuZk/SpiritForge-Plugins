package dev.sfcrafting;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class SmelterBehavior implements StationBehavior {

    private static final int SLOT_INPUT_A = 11;
    private static final int SLOT_OUTPUT = 13;
    private static final int SLOT_INPUT_B = 15;
    private static final int SLOT_BUTTON = 22;

    @Override
    public ForgeState.StationType type() {
        return ForgeState.StationType.SMELTER;
    }

    @Override
    public void handleButtonClick(ForgeManager manager, ForgeState state, Player player) {
        if (state.isSmelting()) {
            player.sendMessage(ChatColor.RED + "La forja ya esta fundiendo.");
            return;
        }
        if (state.isForging()) {
            player.sendMessage(ChatColor.RED + "La forja esta ocupada.");
            return;
        }
        Inventory inventory = state.inventory();
        ItemStack output = inventory.getItem(SLOT_OUTPUT);
        if (output != null && !output.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Retira el resultado antes de continuar.");
            return;
        }
        ItemStack a = inventory.getItem(SLOT_INPUT_A);
        ItemStack b = inventory.getItem(SLOT_INPUT_B);
        manager.ensureRarity(a);
        manager.ensureRarity(b);
        ForgeRecipe recipe = manager.findSmelterRecipe(a, b);
        if (recipe == null) {
            player.sendMessage(ChatColor.RED + "No hay receta valida.");
            return;
        }
        int rarity = manager.resolveRarityFromItems(a, b);
        if ("steel".equalsIgnoreCase(recipe.id())) {
            if (a != null && a.getType() == Material.IRON_INGOT) {
                rarity = manager.readRarityLevel(a);
            } else if (b != null && b.getType() == Material.IRON_INGOT) {
                rarity = manager.readRarityLevel(b);
            }
        }
        state.setRarityLevel(rarity);

        ItemStack resultPreview = manager.buildRecipeOutput(recipe);
        boolean outputIsHot = manager.hotItemManager().isHotItem(resultPreview);
        if (!outputIsHot) {
            player.sendMessage(ChatColor.RED + "La forja solo funde lingotes calientes.");
            return;
        }
        manager.consumeSlot(inventory, SLOT_INPUT_A);
        manager.consumeSlot(inventory, SLOT_INPUT_B);
        manager.startSmelt(state, recipe, manager.locationFrom(inventory), SLOT_OUTPUT);
        manager.updateButton(state);
        player.sendMessage(ChatColor.GOLD + "Fundiendo " + recipe.id() + "...");
    }

    @Override
    public void updateButton(ForgeManager manager, ForgeState state) {
        Inventory inventory = state.inventory();
        if (state.isSmelting()) {
            int percent = state.totalTicks() == 0 ? 0 : (int) ((state.progressTicks() * 100.0) / state.totalTicks());
            inventory.setItem(SLOT_BUTTON, manager.buildButton(Material.FIRE_CHARGE,
                ChatColor.GOLD + "Fundiendo... " + percent + "%"));
            return;
        }
        inventory.setItem(SLOT_BUTTON, manager.buildButton(Material.LIME_DYE, ChatColor.GREEN + "Fundir"));
    }

    @Override
    public boolean isInputSlot(int slot) {
        return slot == SLOT_INPUT_A || slot == SLOT_INPUT_B;
    }

    @Override
    public boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public boolean isButtonSlot(int slot) {
        return slot == SLOT_BUTTON;
    }

    @Override
    public int outputSlot() {
        return SLOT_OUTPUT;
    }
}

