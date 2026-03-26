package dev.sfcrafting;

import org.bukkit.inventory.ItemStack;

public final class ForgeRecipe {

    private final String id;
    private final ForgeIngredient inputA;
    private final ForgeIngredient inputB;
    private final ForgeIngredient inputC;
    private final ForgeOutput output;
    private final int cookTimeTicks;

    public ForgeRecipe(String id, ForgeIngredient inputA, ForgeIngredient inputB, ForgeIngredient inputC, ForgeOutput output, int cookTimeTicks) {
        this.id = id;
        this.inputA = inputA;
        this.inputB = inputB;
        this.inputC = inputC;
        this.output = output;
        this.cookTimeTicks = cookTimeTicks;
    }

    public String id() {
        return id;
    }

    public int cookTimeTicks() {
        return cookTimeTicks;
    }

    public ForgeOutput output() {
        return output;
    }

    public ItemStack buildOutput(OraxenItemResolver resolver) {
        return output.build(resolver);
    }

    public boolean matchesSmelter(ItemStack slotA, ItemStack slotB, OraxenItemResolver resolver) {
        boolean hasB = inputB != null;
        if (!hasB) {
            return inputA.matches(slotA, resolver) && (slotB == null || slotB.getType().isAir())
                || inputA.matches(slotB, resolver) && (slotA == null || slotA.getType().isAir());
        }
        boolean direct = inputA.matches(slotA, resolver) && inputB.matches(slotB, resolver);
        boolean swapped = inputA.matches(slotB, resolver) && inputB.matches(slotA, resolver);
        return direct || swapped;
    }

    public boolean matchesAnvil(ItemStack mold, ItemStack ingot, ItemStack extra, OraxenItemResolver resolver) {
        if (!inputA.matches(mold, resolver)) {
            return false;
        }
        if (inputB == null || !inputB.matches(ingot, resolver)) {
            return false;
        }
        if (inputC == null) {
            return extra == null || extra.getType().isAir();
        }
        return inputC.matches(extra, resolver);
    }
}

