package dev.sfcrafting;

import org.bukkit.inventory.ItemStack;

public final class ForgeRecipe {

    private final String id;
    private final ForgeIngredient inputA;
    private final int inputAAmount;
    private final ForgeIngredient inputB;
    private final int inputBAmount;
    private final ForgeIngredient inputC;
    private final int inputCAmount;
    private final ForgeOutput output;
    private final int cookTimeTicks;
    private final RecipeUnlockRequirement unlockRequirement;

    public ForgeRecipe(
        String id,
        ForgeIngredient inputA,
        int inputAAmount,
        ForgeIngredient inputB,
        int inputBAmount,
        ForgeIngredient inputC,
        int inputCAmount,
        ForgeOutput output,
        int cookTimeTicks,
        RecipeUnlockRequirement unlockRequirement
    ) {
        this.id = id;
        this.inputA = inputA;
        this.inputAAmount = Math.max(1, inputAAmount);
        this.inputB = inputB;
        this.inputBAmount = Math.max(1, inputBAmount);
        this.inputC = inputC;
        this.inputCAmount = Math.max(1, inputCAmount);
        this.output = output;
        this.cookTimeTicks = cookTimeTicks;
        this.unlockRequirement = unlockRequirement;
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

    public ForgeIngredient inputA() {
        return inputA;
    }

    public int inputAAmount() {
        return inputAAmount;
    }

    public ForgeIngredient inputB() {
        return inputB;
    }

    public int inputBAmount() {
        return inputBAmount;
    }

    public ForgeIngredient inputC() {
        return inputC;
    }

    public int inputCAmount() {
        return inputCAmount;
    }

    public RecipeUnlockRequirement unlockRequirement() {
        return unlockRequirement;
    }

    public ItemStack buildOutput(OraxenItemResolver resolver) {
        return output.build(resolver);
    }

    public boolean matchesSmelter(ItemStack slotA, ItemStack slotB, OraxenItemResolver resolver) {
        if (inputB == null) {
            return matchesInput(slotA, inputA, inputAAmount, resolver) && isEmpty(slotB)
                || matchesInput(slotB, inputA, inputAAmount, resolver) && isEmpty(slotA);
        }
        return matchesSmelterDirect(slotA, slotB, resolver) || matchesSmelterSwapped(slotA, slotB, resolver);
    }

    public boolean matchesSmelterDirect(ItemStack slotA, ItemStack slotB, OraxenItemResolver resolver) {
        if (inputB == null) {
            return matchesInput(slotA, inputA, inputAAmount, resolver) && isEmpty(slotB);
        }
        return matchesInput(slotA, inputA, inputAAmount, resolver)
            && matchesInput(slotB, inputB, inputBAmount, resolver);
    }

    public boolean matchesSmelterSwapped(ItemStack slotA, ItemStack slotB, OraxenItemResolver resolver) {
        if (inputB == null) {
            return matchesInput(slotB, inputA, inputAAmount, resolver) && isEmpty(slotA);
        }
        return matchesInput(slotB, inputA, inputAAmount, resolver)
            && matchesInput(slotA, inputB, inputBAmount, resolver);
    }

    public boolean matchesAnvil(ItemStack mold, ItemStack ingot, ItemStack extra, OraxenItemResolver resolver) {
        if (!matchesInput(mold, inputA, inputAAmount, resolver)) {
            return false;
        }
        if (inputB == null || !matchesInput(ingot, inputB, inputBAmount, resolver)) {
            return false;
        }
        if (inputC == null) {
            return isEmpty(extra);
        }
        return matchesInput(extra, inputC, inputCAmount, resolver);
    }

    private boolean matchesInput(ItemStack item, ForgeIngredient ingredient, int requiredAmount, OraxenItemResolver resolver) {
        return ingredient != null
            && ingredient.matches(item, resolver)
            && item != null
            && item.getAmount() >= Math.max(1, requiredAmount);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
