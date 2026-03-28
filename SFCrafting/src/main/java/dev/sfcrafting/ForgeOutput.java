package dev.sfcrafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ForgeOutput {

    private final ForgeIngredient.Type type;
    private final String oraxenId;
    private final Material material;
    private final int amount;

    private ForgeOutput(ForgeIngredient.Type type, String oraxenId, Material material, int amount) {
        this.type = type;
        this.oraxenId = oraxenId;
        this.material = material;
        this.amount = amount;
    }

    public static ForgeOutput material(Material material, int amount) {
        return new ForgeOutput(ForgeIngredient.Type.MATERIAL, null, material, amount);
    }

    public static ForgeOutput oraxen(String oraxenId, int amount) {
        return new ForgeOutput(ForgeIngredient.Type.ORAXEN, oraxenId, null, amount);
    }

    public ForgeIngredient.Type type() {
        return type;
    }

    public String oraxenId() {
        return oraxenId;
    }

    public Material material() {
        return material;
    }

    public int amount() {
        return amount;
    }

    public ItemStack build(OraxenItemResolver resolver) {
        if (type == ForgeIngredient.Type.MATERIAL) {
            return new ItemStack(material, amount);
        }
        ItemStack item = resolver.buildOraxenItem(oraxenId, amount);
        return item == null ? null : item;
    }
}

