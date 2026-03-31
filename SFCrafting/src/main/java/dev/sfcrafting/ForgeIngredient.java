package dev.sfcrafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ForgeIngredient {

    public enum Type {
        MATERIAL,
        ORAXEN
    }

    private final Type type;
    private final Material material;
    private final String oraxenId;

    private ForgeIngredient(Type type, Material material, String oraxenId) {
        this.type = type;
        this.material = material;
        this.oraxenId = oraxenId;
    }

    public static ForgeIngredient material(Material material) {
        return new ForgeIngredient(Type.MATERIAL, material, null);
    }

    public static ForgeIngredient oraxen(String oraxenId) {
        return new ForgeIngredient(Type.ORAXEN, null, oraxenId);
    }

    public Type type() {
        return type;
    }

    public Material material() {
        return material;
    }

    public String oraxenId() {
        return oraxenId;
    }

    public String displayId() {
        return type == Type.ORAXEN ? oraxenId : (material != null ? material.name().toLowerCase() : "unknown");
    }

    public boolean matches(ItemStack item, OraxenItemResolver resolver) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return switch (type) {
            case MATERIAL -> item.getType() == material;
            case ORAXEN -> resolver.isOraxenItem(item, oraxenId);
        };
    }
}

