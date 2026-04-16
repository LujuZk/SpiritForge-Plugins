package dev.sfcrafting;

import dev.sfcore.util.CharacterSlotResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class RecipeBookManager {

    private final Plugin plugin;
    private final ForgeManager forgeManager;
    private final RecipeBookDatabaseManager database;
    private final SkillBridge skillBridge;
    private final Map<UUID, Set<String>> discoveryCache = new HashMap<>();
    private final Map<UUID, Integer> slotCache = new HashMap<>();
    private final Set<String> trackableDiscoveries = new HashSet<>();
    private List<ForgeRecipe> smelterRecipes;
    private List<ForgeRecipe> anvilRecipes;

    public RecipeBookManager(Plugin plugin, ForgeManager forgeManager,
                             RecipeBookDatabaseManager database, SkillBridge skillBridge) {
        this.plugin = plugin;
        this.forgeManager = forgeManager;
        this.database = database;
        this.skillBridge = skillBridge;
        categorizeRecipes();
        buildTrackableDiscoveries();
    }

    private void categorizeRecipes() {
        smelterRecipes = new ArrayList<>();
        anvilRecipes = new ArrayList<>();
        for (ForgeRecipe recipe : forgeManager.getRecipes()) {
            ItemStack output = forgeManager.buildRecipeOutput(recipe);
            if (output != null && forgeManager.hotItemManager().isHotItem(output)) {
                smelterRecipes.add(recipe);
            } else {
                anvilRecipes.add(recipe);
            }
        }
    }

    private void buildTrackableDiscoveries() {
        trackableDiscoveries.clear();
        for (ForgeRecipe recipe : forgeManager.getRecipes()) {
            RecipeUnlockRequirement req = recipe.unlockRequirement();
            if (req != null) {
                trackableDiscoveries.addAll(req.requiredDiscoveries());
            }
        }
    }

    public void reload() {
        categorizeRecipes();
        buildTrackableDiscoveries();
    }

    public void loadPlayer(UUID uuid) {
        int slot = CharacterSlotResolver.resolve(uuid);
        slotCache.put(uuid, slot);
        discoveryCache.put(uuid, database.loadDiscoveries(uuid, slot));
    }

    public void saveAndUnload(UUID uuid) {
        Set<String> discoveries = discoveryCache.remove(uuid);
        Integer slot = slotCache.remove(uuid);
        if (discoveries != null && !discoveries.isEmpty() && slot != null) {
            database.saveDiscoveriesBatch(uuid, slot, discoveries);
        }
    }

    public void saveAll() {
        for (var entry : discoveryCache.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Integer slot = slotCache.get(entry.getKey());
                if (slot != null) {
                    database.saveDiscoveriesBatch(entry.getKey(), slot, entry.getValue());
                }
            }
        }
    }

    public void reloadForActiveSlot(UUID uuid) {
        discoveryCache.remove(uuid);
        slotCache.remove(uuid);
        loadPlayer(uuid);
    }

    public boolean addDiscovery(UUID uuid, String materialId) {
        Set<String> discoveries = discoveryCache.computeIfAbsent(uuid, k -> new HashSet<>());
        if (discoveries.add(materialId)) {
            Integer slot = slotCache.get(uuid);
            if (slot != null) {
                database.saveDiscovery(uuid, slot, materialId);
            }
            return true;
        }
        return false;
    }

    public boolean hasDiscovered(UUID uuid, String materialId) {
        Set<String> discoveries = discoveryCache.get(uuid);
        return discoveries != null && discoveries.contains(materialId);
    }

    public boolean isTrackable(String materialId) {
        return trackableDiscoveries.contains(materialId);
    }

    public Set<String> trackableDiscoveries() {
        return trackableDiscoveries;
    }

    public boolean isRecipeUnlocked(Player player, ForgeRecipe recipe) {
        RecipeUnlockRequirement req = recipe.unlockRequirement();
        if (req == null) {
            return true;
        }

        boolean hasRequirement = false;
        boolean skillMet = false;
        boolean discoveryMet = false;

        if (req.skillKey() != null && req.levelRequired() > 0) {
            hasRequirement = true;
            int playerLevel = skillBridge.getSkillLevel(player, req.skillKey());
            if (playerLevel >= req.levelRequired()) {
                skillMet = true;
            }
        }

        if (req.requiredDiscoveries() != null && !req.requiredDiscoveries().isEmpty()) {
            hasRequirement = true;
            UUID uuid = player.getUniqueId();
            for (String materialId : req.requiredDiscoveries()) {
                if (hasDiscovered(uuid, materialId)) {
                    discoveryMet = true;
                    break;
                }
            }
        }

        if (!hasRequirement) {
            return true;
        }

        return skillMet || discoveryMet;
    }

    public List<ForgeRecipe> getSmelterRecipes() {
        return smelterRecipes;
    }

    public List<ForgeRecipe> getAnvilRecipes() {
        return anvilRecipes;
    }

    public void openRecipeBook(Player player, ForgeState.StationType category, int page) {
        RecipeBookGUI.open(player, this, forgeManager, skillBridge, category, page);
    }

    public ForgeManager forgeManager() {
        return forgeManager;
    }

    public SkillBridge skillBridge() {
        return skillBridge;
    }
}
