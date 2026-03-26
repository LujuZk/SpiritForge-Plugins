package dev.sfcrafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public final class MoldRecipeListener implements Listener {

    private record IngotRequirement(Type type, Material material, String oraxenId) {
        enum Type { MATERIAL, ORAXEN }

        static IngotRequirement material(Material material) {
            return new IngotRequirement(Type.MATERIAL, material, null);
        }

        static IngotRequirement oraxen(String oraxenId) {
            return new IngotRequirement(Type.ORAXEN, null, oraxenId);
        }
    }

    private record MoldRecipe(String moldId, IngotRequirement ingot) {}

    private final ForgeManager manager;
    private final OraxenItemResolver resolver;
    private final List<MoldRecipe> recipes = new ArrayList<>();

    public MoldRecipeListener(SFCraftingPlugin plugin, ForgeManager manager) {
        this.manager = manager;
        this.resolver = new OraxenItemResolver(plugin);
        recipes.addAll(Arrays.asList(
            new MoldRecipe("mold_sword_copper", IngotRequirement.material(Material.COPPER_INGOT)),
            new MoldRecipe("mold_sword_bronze", IngotRequirement.oraxen("bronze_ingot")),
            new MoldRecipe("mold_sword_iron", IngotRequirement.material(Material.IRON_INGOT)),
            new MoldRecipe("mold_sword_steel", IngotRequirement.oraxen("steel_ingot")),
            new MoldRecipe("mold_sword_silver", IngotRequirement.oraxen("silver_ingot")),
            new MoldRecipe("mold_sword_gold", IngotRequirement.material(Material.GOLD_INGOT)),
            new MoldRecipe("mold_sword_platinum", IngotRequirement.oraxen("platinum_ingot")),
            new MoldRecipe("mold_sword_mithil", IngotRequirement.oraxen("mithil_ingot")),
            new MoldRecipe("mold_sword_orichalcum", IngotRequirement.oraxen("orichalcum_ingot"))
        ));
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }
        ItemStack result = buildResultIfMatch(inventory);
        if (result != null) {
            inventory.setResult(result);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }
        ItemStack result = buildResultIfMatch(inventory);
        if (result != null) {
            inventory.setResult(result);
            event.setCurrentItem(result);
        }
    }

    private ItemStack buildResultIfMatch(CraftingInventory inventory) {
        ItemStack[] matrix = inventory.getMatrix();
        if (matrix == null || matrix.length < 9) {
            return null;
        }
        ItemStack center = matrix[4];
        if (!isSword(center)) {
            return null;
        }
        for (MoldRecipe recipe : recipes) {
            if (matchesIngotRing(matrix, recipe.ingot)) {
                ItemStack mold = resolver.buildOraxenItem(recipe.moldId, 1);
                if (mold == null || mold.getType().isAir()) {
                    return null;
                }
                int rarity = manager.readRarityLevel(center);
                manager.applyRarity(mold, rarity);
                return mold;
            }
        }
        return null;
    }

    private boolean matchesIngotRing(ItemStack[] matrix, IngotRequirement ingot) {
        for (int i = 0; i < matrix.length; i++) {
            if (i == 4) {
                continue;
            }
            ItemStack item = matrix[i];
            if (item == null || item.getType().isAir()) {
                return false;
            }
            if (!matchesIngot(item, ingot)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesIngot(ItemStack item, IngotRequirement ingot) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (ingot.type == IngotRequirement.Type.MATERIAL) {
            return item.getType() == ingot.material;
        }
        String id = resolver.readOraxenId(item);
        return id != null && id.equalsIgnoreCase(ingot.oraxenId);
    }

    private boolean isSword(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getType().name().endsWith("_SWORD");
    }
}

