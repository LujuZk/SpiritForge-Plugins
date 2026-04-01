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

    private enum TargetType { SWORD, AXE, PICKAXE, HAMMER }

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

        registerMaterial("copper", IngotRequirement.material(Material.COPPER_INGOT));
        registerMaterial("bronze", IngotRequirement.oraxen("bronze_ingot"));
        registerMaterial("iron", IngotRequirement.material(Material.IRON_INGOT));
        registerMaterial("steel", IngotRequirement.oraxen("steel_ingot"));
        registerMaterial("silver", IngotRequirement.oraxen("silver_ingot"));
        registerMaterial("gold", IngotRequirement.material(Material.GOLD_INGOT));
        registerMaterial("platinum", IngotRequirement.oraxen("platinum_ingot"));
        registerMaterial("diamond", IngotRequirement.material(Material.DIAMOND));
        registerMaterial("obsidian", IngotRequirement.oraxen("obsidian_shard"));
        registerMaterial("dark_steel", IngotRequirement.oraxen("dark_steel_ingot"));
        registerMaterial("netherite", IngotRequirement.material(Material.NETHERITE_INGOT));
        registerMaterial("mithil", IngotRequirement.oraxen("mithil_ingot"));
        registerMaterial("orichalcum", IngotRequirement.oraxen("orichalcum_ingot"));
        registerMaterial("adamantite", IngotRequirement.oraxen("adamantite_ingot"));
        registerMaterial("demonite", IngotRequirement.oraxen("demonite_ingot"));
        registerMaterial("dragonite", IngotRequirement.oraxen("dragonite_ingot"));
        registerMaterial("aetherium", IngotRequirement.oraxen("aetherium_ingot"));
        registerMaterial("astralium", IngotRequirement.oraxen("astralium_ingot"));
        registerMaterial("eternium", IngotRequirement.oraxen("eternium_ingot"));
        registerMaterial("deus_matter", IngotRequirement.oraxen("deus_matter_ingot"));
    }

    private void registerMaterial(String materialKey, IngotRequirement ingot) {
        recipes.addAll(Arrays.asList(
                new MoldRecipe("mold_sword_" + materialKey, ingot, TargetType.SWORD),
                new MoldRecipe("mold_axe_" + materialKey, ingot, TargetType.AXE),
                new MoldRecipe("mold_pickaxe_" + materialKey, ingot, TargetType.PICKAXE),
                new MoldRecipe("mold_hammer_" + materialKey, ingot, TargetType.HAMMER)
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
        if (!(event.getInventory() instanceof CraftingInventory inventory)) {
            return;
        }
        ItemStack result = buildResultIfMatch(inventory);
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
        if (isAxe(center)) {
            return TargetType.AXE;
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
        return item != null && !item.getType().isAir() && item.getType().name().endsWith("_SWORD");
    }

    private boolean isPickaxe(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getType().name().endsWith("_PICKAXE");
    }

    private boolean isAxe(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getType().name().endsWith("_AXE");
    }

    private boolean isHammer(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String id = resolver.readOraxenId(item);
        return id != null && id.toLowerCase().endsWith("_hammer");
    }
}