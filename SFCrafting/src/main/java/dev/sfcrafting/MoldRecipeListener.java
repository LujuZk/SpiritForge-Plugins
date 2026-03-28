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

    private enum TargetType { SWORD, PICKAXE, HAMMER }

    private record IngotRequirement(Type type, Material material, String oraxenId) {
        enum Type { MATERIAL, ORAXEN }

        static IngotRequirement material(Material material) {
            return new IngotRequirement(Type.MATERIAL, material, null);
        }

        static IngotRequirement oraxen(String oraxenId) {
            return new IngotRequirement(Type.ORAXEN, null, oraxenId);
        }
    }

    private record MoldRecipe(String moldId, IngotRequirement ingot, TargetType targetType) {}

    private final ForgeManager manager;
    private final OraxenItemResolver resolver;
    private final List<MoldRecipe> recipes = new ArrayList<>();

    public MoldRecipeListener(SFCraftingPlugin plugin, ForgeManager manager) {
        this.manager = manager;
        this.resolver = new OraxenItemResolver(plugin);

        recipes.addAll(Arrays.asList(
            new MoldRecipe("mold_sword_copper", IngotRequirement.material(Material.COPPER_INGOT), TargetType.SWORD),
            new MoldRecipe("mold_sword_bronze", IngotRequirement.oraxen("bronze_ingot"), TargetType.SWORD),
            new MoldRecipe("mold_sword_iron", IngotRequirement.material(Material.IRON_INGOT), TargetType.SWORD),
            new MoldRecipe("mold_sword_steel", IngotRequirement.oraxen("steel_ingot"), TargetType.SWORD),
            new MoldRecipe("mold_sword_silver", IngotRequirement.oraxen("silver_ingot"), TargetType.SWORD),
            new MoldRecipe("mold_sword_gold", IngotRequirement.material(Material.GOLD_INGOT), TargetType.SWORD),
            new MoldRecipe("mold_sword_platinum", IngotRequirement.oraxen("platinum_ingot"), TargetType.SWORD),
            new MoldRecipe("mold_sword_mithil", IngotRequirement.oraxen("mithil_ingot"), TargetType.SWORD),
            new MoldRecipe("mold_sword_orichalcum", IngotRequirement.oraxen("orichalcum_ingot"), TargetType.SWORD)
        ));

        recipes.addAll(Arrays.asList(
            new MoldRecipe("mold_pickaxe_bronze", IngotRequirement.oraxen("bronze_ingot"), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_iron", IngotRequirement.material(Material.IRON_INGOT), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_steel", IngotRequirement.oraxen("steel_ingot"), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_silver", IngotRequirement.oraxen("silver_ingot"), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_gold", IngotRequirement.material(Material.GOLD_INGOT), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_platinum", IngotRequirement.oraxen("platinum_ingot"), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_mithil", IngotRequirement.oraxen("mithil_ingot"), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_orichalcum", IngotRequirement.oraxen("orichalcum_ingot"), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_diamond", IngotRequirement.material(Material.DIAMOND), TargetType.PICKAXE),
            new MoldRecipe("mold_pickaxe_obsidian", IngotRequirement.material(Material.OBSIDIAN), TargetType.PICKAXE)
        ));

        recipes.addAll(Arrays.asList(
            new MoldRecipe("mold_hammer_bronze", IngotRequirement.oraxen("bronze_ingot"), TargetType.HAMMER),
            new MoldRecipe("mold_hammer_iron", IngotRequirement.material(Material.IRON_INGOT), TargetType.HAMMER),
            new MoldRecipe("mold_hammer_steel", IngotRequirement.oraxen("steel_ingot"), TargetType.HAMMER),
            new MoldRecipe("mold_hammer_silver", IngotRequirement.oraxen("silver_ingot"), TargetType.HAMMER),
            new MoldRecipe("mold_hammer_platinum", IngotRequirement.oraxen("platinum_ingot"), TargetType.HAMMER),
            new MoldRecipe("mold_hammer_mithil", IngotRequirement.oraxen("mithil_ingot"), TargetType.HAMMER),
            new MoldRecipe("mold_hammer_orichalcum", IngotRequirement.oraxen("orichalcum_ingot"), TargetType.HAMMER)
        ));
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = buildResultIfMatch(event.getInventory());
        if (result != null) {
            event.getInventory().setResult(result);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = buildResultIfMatch(event.getInventory());
        if (result != null) {
            event.getInventory().setResult(result);
            event.setCurrentItem(result);
        }
    }

    private ItemStack buildResultIfMatch(CraftingInventory inventory) {
        ItemStack[] matrix = inventory.getMatrix();
        if (matrix == null || matrix.length < 9) {
            return null;
        }

        ItemStack center = matrix[4];
        TargetType targetType = detectTargetType(center);
        if (targetType == null) {
            return null;
        }

        for (MoldRecipe recipe : recipes) {
            if (recipe.targetType() != targetType) {
                continue;
            }
            if (!matchesIngotRing(matrix, recipe.ingot())) {
                continue;
            }

            ItemStack mold = resolver.buildOraxenItem(recipe.moldId(), 1);
            if (mold == null || mold.getType().isAir()) {
                return null;
            }

            int rarity = manager.readRarityLevel(center);
            manager.applyRarity(mold, rarity);
            return mold;
        }

        return null;
    }

    private TargetType detectTargetType(ItemStack center) {
        if (isSword(center)) {
            return TargetType.SWORD;
        }
        if (isHammer(center)) {
            return TargetType.HAMMER;
        }
        if (isPickaxe(center)) {
            return TargetType.PICKAXE;
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
        if (ingot.type() == IngotRequirement.Type.MATERIAL) {
            return item.getType() == ingot.material();
        }
        String id = resolver.readOraxenId(item);
        return id != null && id.equalsIgnoreCase(ingot.oraxenId());
    }

    private boolean isSword(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getType().name().endsWith("_SWORD");
    }

    private boolean isPickaxe(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getType().name().endsWith("_PICKAXE");
    }

    private boolean isHammer(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String id = resolver.readOraxenId(item);
        return id != null && id.toLowerCase().endsWith("_hammer");
    }
}