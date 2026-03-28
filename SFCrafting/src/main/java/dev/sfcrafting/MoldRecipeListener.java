package dev.sfcrafting;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MoldRecipeListener implements Listener {
    private final ForgeManager manager;
    private final OraxenItemResolver resolver;
    private final List<MoldRecipe> recipes;

    public MoldRecipeListener(SFCraftingPlugin plugin, ForgeManager manager) {
        this.manager = manager;
        this.resolver = new OraxenItemResolver((Plugin) plugin);
        this.recipes = new ArrayList<>();

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
        CraftingInventory inventory = event.getInventory();
        ItemStack result = buildResultIfMatch(inventory);
        if (result != null) {
            inventory.setResult(result);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
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
        TargetType targetType = detectTargetType(center);
        if (targetType == null) {
            return null;
        }

        for (MoldRecipe recipe : recipes) {
            if (recipe.targetType != targetType) {
                continue;
            }
            if (!matchesIngotRing(matrix, recipe.ingot)) {
                continue;
            }

            ItemStack result = resolver.buildOraxenItem(recipe.moldId, 1);
            if (result == null || result.getType().isAir()) {
                return null;
            }

            int rarity = manager.readRarityLevel(center);
            manager.applyRarity(result, rarity);
            return result;
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

    private boolean matchesIngotRing(ItemStack[] matrix, IngotRequirement requirement) {
        for (int slot = 0; slot < matrix.length; slot++) {
            if (slot == 4) {
                continue;
            }

            ItemStack item = matrix[slot];
            if (item == null || item.getType().isAir()) {
                return false;
            }
            if (!matchesIngot(item, requirement)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesIngot(ItemStack item, IngotRequirement requirement) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        if (requirement.type == IngotType.MATERIAL) {
            return item.getType() == requirement.material;
        }

        String id = resolver.readOraxenId(item);
        return id != null && id.equalsIgnoreCase(requirement.oraxenId);
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

    private static final class MoldRecipe {
        private final String moldId;
        private final IngotRequirement ingot;
        private final TargetType targetType;

        private MoldRecipe(String moldId, IngotRequirement ingot, TargetType targetType) {
            this.moldId = moldId;
            this.ingot = ingot;
            this.targetType = targetType;
        }
    }

    private static final class IngotRequirement {
        private final IngotType type;
        private final Material material;
        private final String oraxenId;

        private IngotRequirement(IngotType type, Material material, String oraxenId) {
            this.type = type;
            this.material = material;
            this.oraxenId = oraxenId;
        }

        private static IngotRequirement material(Material material) {
            return new IngotRequirement(IngotType.MATERIAL, material, null);
        }

        private static IngotRequirement oraxen(String oraxenId) {
            return new IngotRequirement(IngotType.ORAXEN, null, oraxenId);
        }
    }

    private enum IngotType {
        MATERIAL,
        ORAXEN
    }

    private enum TargetType {
        SWORD,
        PICKAXE,
        HAMMER
    }
}