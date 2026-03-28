package dev.sfcrafting;

import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

public final class ForgeState {
    public enum StationType {
        SMELTER,
        ANVIL
    }

    private final Inventory inventory;
    private final StationType stationType;
    private BukkitTask task;
    private BukkitTask forgeTask;
    private ForgeRecipe activeRecipe;
    private int progressTicks;
    private int totalTicks;

    private int baseQuality = 100;
    private int hitBonus;
    private int tempValue;
    private int tempMax;
    private int tempBonus;
    private int rarityLevel = 0;
    private int moldRarity = 0;
    private int materialRarity = 0;
    private String materialId = "";
    private int hits;
    private int failures;
    private String tempTier = "";
    private String temperType = "";

    private int barIndex;
    private int barDirection = 1;
    private int barSpeedTicks = 4;
    private int barTickCounter;

    public ForgeState(Inventory inventory, StationType stationType) {
        this.inventory = inventory;
        this.stationType = stationType;
    }

    public Inventory inventory() {
        return inventory;
    }

    public StationType stationType() {
        return stationType;
    }

    public BukkitTask task() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }

    public BukkitTask forgeTask() {
        return forgeTask;
    }

    public void setForgeTask(BukkitTask forgeTask) {
        this.forgeTask = forgeTask;
    }

    public ForgeRecipe activeRecipe() {
        return activeRecipe;
    }

    public void setActiveRecipe(ForgeRecipe activeRecipe) {
        this.activeRecipe = activeRecipe;
    }

    public int progressTicks() {
        return progressTicks;
    }

    public void setProgressTicks(int progressTicks) {
        this.progressTicks = progressTicks;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public void setTotalTicks(int totalTicks) {
        this.totalTicks = totalTicks;
    }

    public boolean isRunning() {
        return task != null;
    }

    public boolean isSmelting() {
        return task != null;
    }

    public boolean isForging() {
        return activeRecipe != null && totalTicks > 0;
    }

    public int baseQuality() {
        return baseQuality;
    }

    public void setBaseQuality(int baseQuality) {
        this.baseQuality = baseQuality;
    }

    public int hitBonus() {
        return hitBonus;
    }

    public void setHitBonus(int hitBonus) {
        this.hitBonus = hitBonus;
    }

    public int tempValue() {
        return tempValue;
    }

    public void setTempValue(int tempValue) {
        this.tempValue = tempValue;
    }

    public int tempMax() {
        return tempMax;
    }

    public void setTempMax(int tempMax) {
        this.tempMax = tempMax;
    }

    public int tempBonus() {
        return tempBonus;
    }

    public void setTempBonus(int tempBonus) {
        this.tempBonus = tempBonus;
    }

    public int rarityLevel() {
        return rarityLevel;
    }

    public void setRarityLevel(int rarityLevel) {
        this.rarityLevel = rarityLevel;
    }

    public int moldRarity() {
        return moldRarity;
    }

    public void setMoldRarity(int moldRarity) {
        this.moldRarity = moldRarity;
    }

    public int materialRarity() {
        return materialRarity;
    }

    public void setMaterialRarity(int materialRarity) {
        this.materialRarity = materialRarity;
    }

    public String materialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId == null ? "" : materialId;
    }

    public int hits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public int failures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public String tempTier() {
        return tempTier;
    }

    public void setTempTier(String tempTier) {
        this.tempTier = tempTier == null ? "" : tempTier;
    }

    public String temperType() {
        return temperType;
    }

    public void setTemperType(String temperType) {
        this.temperType = temperType == null ? "" : temperType;
    }

    public int barIndex() {
        return barIndex;
    }

    public void setBarIndex(int barIndex) {
        this.barIndex = barIndex;
    }

    public int barDirection() {
        return barDirection;
    }

    public void setBarDirection(int barDirection) {
        this.barDirection = barDirection;
    }

    public int barSpeedTicks() {
        return barSpeedTicks;
    }

    public void setBarSpeedTicks(int barSpeedTicks) {
        this.barSpeedTicks = barSpeedTicks;
    }

    public int barTickCounter() {
        return barTickCounter;
    }

    public void setBarTickCounter(int barTickCounter) {
        this.barTickCounter = barTickCounter;
    }
}

