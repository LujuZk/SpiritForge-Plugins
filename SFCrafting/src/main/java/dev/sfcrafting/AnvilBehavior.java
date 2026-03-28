package dev.sfcrafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class AnvilBehavior implements StationBehavior {

    private static final int SLOT_MOLD = 10;
    private static final int SLOT_INGOT = 12;
    private static final int SLOT_EXTRA = 14;
    private static final int SLOT_OUTPUT = 16;
    private static final int SLOT_BUTTON = 22;

    @Override
    public ForgeState.StationType type() {
        return ForgeState.StationType.ANVIL;
    }

    @Override
    public void handleButtonClick(ForgeManager manager, ForgeState state, Player player) {
        if (state.isSmelting()) {
            player.sendMessage(ChatColor.RED + "El yunque esta ocupado.");
            return;
        }
        if (state.isForging()) {
            handleForgeHit(manager, state, player);
            return;
        }
        Inventory inventory = state.inventory();
        ItemStack output = inventory.getItem(SLOT_OUTPUT);
        if (output != null && !output.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Retira el resultado antes de continuar.");
            return;
        }
        ItemStack mold = inventory.getItem(SLOT_MOLD);
        ItemStack ingot = inventory.getItem(SLOT_INGOT);
        ItemStack extra = inventory.getItem(SLOT_EXTRA);
        manager.ensureRarity(mold);
        manager.ensureRarity(ingot);
        ForgeRecipe recipe = manager.findAnvilRecipe(mold, ingot, extra);
        if (recipe == null) {
            player.sendMessage(ChatColor.RED + "No hay receta valida.");
            return;
        }
        ItemStack resultPreview = manager.buildRecipeOutput(recipe);
        boolean outputIsHot = manager.hotItemManager().isHotItem(resultPreview);
        if (outputIsHot) {
            player.sendMessage(ChatColor.RED + "El yunque no funde lingotes.");
            return;
        }
        boolean ingotHot = manager.hotItemManager().isHotItem(ingot);
        if (manager.requireHotInput() && !ingotHot) {
            player.sendMessage(ChatColor.RED + "Necesitas un lingote caliente.");
            return;
        }
        int temp = manager.hotItemManager().getCurrentTemperature(ingot);
        int tempMax = manager.hotItemManager().getMaxTemperature(ingot);
        ForgeManager.TemperatureTier tier = manager.resolveTemperatureTier(temp);
        int tempBonus = manager.computeTempBonus(temp, tempMax);

        if (!manager.isHammerEquipped(player)) {
            player.sendMessage(ChatColor.RED + "Necesitas un martillo equipado.");
            return;
        }

        manager.consumeSlot(inventory, SLOT_MOLD);
        manager.consumeSlot(inventory, SLOT_INGOT);
        manager.consumeSlot(inventory, SLOT_EXTRA);

        int moldRarity = manager.readRarityLevel(mold);
        int materialRarity = manager.readRarityLevel(ingot);

        String materialId = manager.readOraxenId(ingot);
        if (materialId == null || materialId.isBlank()) {
            materialId = ingot == null ? "" : ingot.getType().name().toLowerCase(Locale.ROOT);
        }
        state.setMaterialId(materialId);

        state.setBaseQuality(100);
        state.setHitBonus(0);
        state.setHits(0);
        state.setFailures(0);
        state.setTempValue(Math.max(0, temp));
        state.setTempMax(Math.max(0, tempMax));
        state.setTempTier(tier.label());
        state.setTempBonus(tempBonus);
        state.setMoldRarity(moldRarity);
        state.setMaterialRarity(materialRarity);
        state.setRarityLevel(manager.resolveRarityFromItems(mold, ingot));

        state.setActiveRecipe(recipe);
        manager.startForge(state);
        manager.updateButton(state);
        player.sendMessage(ChatColor.GOLD + "Forjando " + recipe.id() + "...");
    }

    private void handleForgeHit(ForgeManager manager, ForgeState state, Player player) {
        if (!manager.isHammerEquipped(player)) {
            player.sendMessage(ChatColor.RED + "Necesitas un martillo equipado.");
            return;
        }
        if (!manager.damageEquippedHammer(player, state)) {
            player.sendMessage(ChatColor.RED + "Tu martillo se rompio.");
        }
        int hits = state.hits() + 1;
        state.setHits(hits);
        state.setProgressTicks(hits);
        int speed = Math.max(1, state.barSpeedTicks() - 1);
        manager.applyHeatLoss(state);
        state.setTempBonus(manager.computeTempBonus(state.tempValue(), state.tempMax()));
        state.setTempTier(manager.resolveTemperatureTier(state.tempValue()).label());
        state.setBarSpeedTicks(speed);

        int bonus = manager.hitBonusForIndex(state);
        if (bonus >= 2) {
            player.sendMessage(ChatColor.GREEN + "Golpe perfecto.");
        } else if (bonus == 1) {
            player.sendMessage(ChatColor.YELLOW + "Buen golpe.");
        } else if (bonus == 0) {
            player.sendMessage(ChatColor.GRAY + "Golpe normal.");
        } else if (bonus == -1) {
            player.sendMessage(ChatColor.RED + "Golpe malo.");
        } else {
            player.sendMessage(ChatColor.DARK_RED + "Golpe fallido.");
        }
        state.setHitBonus(state.hitBonus() + bonus);
        if (bonus < 0) {
            state.setFailures(state.failures() + 1);
        }

        manager.updateButton(state);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, 0.9f, 1.2f);
        if (hits < state.totalTicks()) {
            return;
        }
        manager.finishForge(state, manager.locationFrom(state.inventory()), SLOT_OUTPUT, player);
        player.sendMessage(ChatColor.GREEN + "Forja terminada. Templar el arma en agua o aceite.");
    }

    @Override
    public void updateButton(ForgeManager manager, ForgeState state) {
        Inventory inventory = state.inventory();
        List<String> lore = new ArrayList<>();
        int hits = Math.max(0, state.progressTicks());
        int maxHits = Math.max(1, state.totalTicks());
        int quality = state.baseQuality() + state.tempBonus() + state.hitBonus();
        if (state.tempValue() > 0) {
            lore.add(ChatColor.GRAY + "Temperatura: " + state.tempTier() + " (" + state.tempValue() + "C)");
        }
                lore.add(ChatColor.GRAY + "Calidad: " + quality);
        lore.add(ChatColor.GRAY + "Rango rareza: " + manager.rarityLabel(state.moldRarity()) + "-" + manager.rarityLabel(state.materialRarity()));
        String rarityLine = buildRarityChanceLine(manager, state);
        if (rarityLine != null) {
            lore.add(rarityLine);
        }
        lore.add(ChatColor.GRAY + "Martillazos: " + hits + "/" + maxHits);

        if (state.isForging()) {
            inventory.setItem(SLOT_BUTTON, manager.buildHammerButton(
                ChatColor.GOLD + "Forjando... " + hits + "/" + maxHits, lore));
            return;
        }
        inventory.setItem(SLOT_BUTTON, manager.buildHammerButton( ChatColor.GREEN + "Martillar", lore));
    }

    private String buildRarityChanceLine(ForgeManager manager, ForgeState state) {
        Map<Integer, Double> dist = manager.buildRarityDistribution(
            state.moldRarity(),
            state.materialRarity(),
            0.0,
            100.0,
            0.0
        );
        if (dist == null || dist.isEmpty()) {
            return null;
        }
        StringBuilder line = new StringBuilder();
        line.append(ChatColor.GRAY).append("Rareza: ");
        int idx = 0;
        for (var entry : dist.entrySet()) {
            if (idx > 0) {
                line.append(" | ");
            }
            line.append(manager.rarityLabel(entry.getKey()))
                .append(" ")
                .append(String.format(Locale.US, "%.1f%%", entry.getValue() * 100.0));
            idx++;
        }
        return line.toString();
    }

    @Override
    public boolean isInputSlot(int slot) {
        return slot == SLOT_MOLD || slot == SLOT_INGOT || slot == SLOT_EXTRA;
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





