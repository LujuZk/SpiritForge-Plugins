package dev.sfcrafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class ForgeManager {

    enum TemperatureTier {
        INCANDESCENTE("Incandescente", 15),
        CALIENTE("Caliente", 8),
        TIBIO("Tibio", 2),
        FRIO("Frio", -5);

        private final String label;
        private final int bonus;

        TemperatureTier(String label, int bonus) {
            this.label = label;
            this.bonus = bonus;
        }

        public String label() {
            return label;
        }

        public int bonus() {
            return bonus;
        }
    }

    private enum TemperType {
        WATER("Agua", 8),
        OIL("Aceite", 10),
        NONE("", 0);

        private final String label;
        private final int bonus;

        TemperType(String label, int bonus) {
            this.label = label;
            this.bonus = bonus;
        }

        public String label() {
            return label;
        }

        public int bonus() {
            return bonus;
        }
    }

    private static final int[] FORGE_BAR_SLOTS = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private final Plugin plugin;
    private final Map<ForgeKey, ForgeState> forges = new HashMap<>();
    private final List<ForgeRecipe> recipes = new ArrayList<>();
    private final OraxenItemResolver oraxenResolver;
    private final HotItemManager hotItemManager;
    private final String smelterItemId;
    private final String anvilItemId;
    private final String hammerButtonItemId;
    private final String smelterBlockId;
    private final String nameContains;
    private final String smelterTitle;
    private final String anvilTitle;
    private final int defaultCookTime;
    private final boolean debug;
    private final int maxForgeHits;
    private final boolean requireHotInput;
    private final double tempBonusBase;
    private final double tempBonusTargetRatio;
    private final double tempLossMinPercent;
    private final double tempLossMaxPercent;
    private final int[] hitBonusByOffset;
    private final Map<String, Integer> hammerDurabilityLoss;
    private final Map<ForgeState.StationType, StationBehavior> behaviors = new HashMap<>();
    private final Random rarityRandom = new Random();

    private final NamespacedKey pendingKey;
    private final NamespacedKey outputKey;
    private final NamespacedKey outputAmountKey;
    private final NamespacedKey qualityKey;
    private final NamespacedKey tempKey;
    private final NamespacedKey tempTierKey;
    private final NamespacedKey forgedByKey;
    private final NamespacedKey temperKey;
    private final NamespacedKey rarityKey;

    public ForgeManager(Plugin plugin) {
        this.plugin = plugin;
        this.oraxenResolver = new OraxenItemResolver(plugin);
        this.hotItemManager = new HotItemManager(plugin, oraxenResolver);
        this.smelterItemId = normalize(plugin.getConfig().getString(
            "forge.smelter-item-id",
            plugin.getConfig().getString("forge.oraxen-item-id", "spirit_forge")
        ));
        this.anvilItemId = normalize(plugin.getConfig().getString("forge.anvil-item-id", "large_anvil"));
        this.hammerButtonItemId = normalize(plugin.getConfig().getString("forge.hammer-button-item-id", ""));
        this.smelterBlockId = normalize(plugin.getConfig().getString(
            "forge.smelter-block-id",
            plugin.getConfig().getString("forge.oraxen-block-id", "spirit_forge")
        ));
        this.nameContains = normalize(stripColor(plugin.getConfig().getString("forge.display-name-contains", "Forja")));
        this.smelterTitle = translateColor(decodeUnicodeEscapes(plugin.getConfig().getString("forge.gui-title-smelter", "Forja")));
        this.anvilTitle = translateColor(decodeUnicodeEscapes(plugin.getConfig().getString("forge.gui-title-anvil", "\\u00A7")));
        this.defaultCookTime = Math.max(20, plugin.getConfig().getInt("forge.default-cook-time-ticks", 200));
        this.debug = plugin.getConfig().getBoolean("forge.debug", false);
        this.maxForgeHits = Math.max(1, plugin.getConfig().getInt("forge.forging.max-hits", 10));
        this.requireHotInput = plugin.getConfig().getBoolean("forge.forging.require-hot-input", true);
        this.tempBonusBase = plugin.getConfig().getDouble("forge.forging.temp-bonus-base", 20.0);
        this.tempBonusTargetRatio = plugin.getConfig().getDouble("forge.forging.temp-bonus-target-ratio", 0.7);
        this.tempLossMinPercent = plugin.getConfig().getDouble("forge.forging.temp-loss-min-percent", 0.01);
        this.tempLossMaxPercent = plugin.getConfig().getDouble("forge.forging.temp-loss-max-percent", 0.02);
        List<Integer> offsetList = plugin.getConfig().getIntegerList("forge.forging.hit-bonus-by-offset");
        if (offsetList == null || offsetList.size() < 5) {
            this.hitBonusByOffset = new int[] {2, 1, 0, -1, -2};
        } else {
            this.hitBonusByOffset = new int[] {offsetList.get(0), offsetList.get(1), offsetList.get(2), offsetList.get(3), offsetList.get(4)};
        }
        ConfigurationSection hammerSection = plugin.getConfig().getConfigurationSection("forge.forging.hammer-durability-loss");
        Map<String, Integer> lossMap = new HashMap<>();
        if (hammerSection != null) {
            for (String key : hammerSection.getKeys(false)) {
                lossMap.put(normalize(key), Math.max(0, hammerSection.getInt(key)));
            }
        }
        if (!lossMap.containsKey("default")) {
            lossMap.put("default", 1);
        }
        this.hammerDurabilityLoss = lossMap;
        this.behaviors.put(ForgeState.StationType.SMELTER, new SmelterBehavior());
        this.behaviors.put(ForgeState.StationType.ANVIL, new AnvilBehavior());

        this.pendingKey = new NamespacedKey(plugin, "forge_pending");
        this.outputKey = new NamespacedKey(plugin, "forge_output");
        this.outputAmountKey = new NamespacedKey(plugin, "forge_output_amount");
        this.qualityKey = new NamespacedKey(plugin, "forge_quality");
        this.tempKey = new NamespacedKey(plugin, "forge_temp");
        this.tempTierKey = new NamespacedKey(plugin, "forge_temp_tier");
        this.forgedByKey = new NamespacedKey(plugin, "forge_forged_by");
        this.temperKey = new NamespacedKey(plugin, "forge_temper");
        this.rarityKey = new NamespacedKey(plugin, "forge_rarity");

        loadRecipes();
    }

    public boolean isForgeEntity(Entity entity) {
        return isSmelterEntity(entity) || isAnvilEntity(entity);
    }

    public boolean isSmelterEntity(Entity entity) {
        ItemStack stack = getEntityItemStack(entity);
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        String oraxenId = oraxenResolver.readOraxenId(stack);
        if (oraxenId != null && !smelterItemId.isBlank() && oraxenId.equals(smelterItemId)) {
            return true;
        }
        String display = stripColor(stack.getItemMeta().getDisplayName());
        if (!display.isBlank() && !nameContains.isBlank()) {
            return normalize(display).contains(nameContains);
        }
        return false;
    }

    public boolean isAnvilEntity(Entity entity) {
        ItemStack stack = getEntityItemStack(entity);
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        String oraxenId = oraxenResolver.readOraxenId(stack);
        return oraxenId != null && !anvilItemId.isBlank() && oraxenId.equals(anvilItemId);
    }

    public ForgeState.StationType getStationTypeForEntity(Entity entity) {
        if (isAnvilEntity(entity)) {
            return ForgeState.StationType.ANVIL;
        }
        if (isSmelterEntity(entity)) {
            return ForgeState.StationType.SMELTER;
        }
        return null;
    }

    public ForgeState.StationType getStationTypeForBlock(org.bukkit.block.Block block) {
        if (block == null) {
            return null;
        }
        if (smelterBlockId == null || smelterBlockId.isBlank()) {
            return null;
        }
        String id = oraxenResolver.readOraxenBlockId(block);
        if (id != null && id.equals(smelterBlockId)) {
            return ForgeState.StationType.SMELTER;
        }
        return null;
    }

    private ItemStack getEntityItemStack(Entity entity) {
        if (entity instanceof ItemDisplay display) {
            return display.getItemStack();
        }
        if (entity instanceof ItemFrame frame) {
            return frame.getItem();
        }
        return null;
    }

    public boolean isForgeBlock(org.bukkit.block.Block block) {
        if (block == null) {
            return false;
        }
        if (smelterBlockId == null || smelterBlockId.isBlank()) {
            return false;
        }
        String id = oraxenResolver.readOraxenBlockId(block);
        return id != null && id.equals(smelterBlockId);
    }

    public Entity findForgeEntityAt(Location blockLocation) {
        if (blockLocation == null || blockLocation.getWorld() == null) {
            return null;
        }
        Location block = blockLocation.getBlock().getLocation();
        Location center = block.clone().add(0.5, 0.5, 0.5);
        for (Entity entity : blockLocation.getWorld().getNearbyEntities(center, 0.6, 1.2, 0.6)) {
            if (!isForgeEntity(entity)) {
                continue;
            }
            Location entityBlock = entity.getLocation().getBlock().getLocation();
            if (entityBlock.equals(block)) {
                return entity;
            }
        }
        return null;
    }

    public void openForge(Player player, Location location, ForgeState.StationType stationType) {
        ForgeState state = getOrCreateState(location, stationType);
        refreshDecor(state);
        updateButton(state);
        player.openInventory(state.inventory());
    }

    public boolean isForgeInventory(Inventory inventory) {
        return inventory.getHolder() instanceof ForgeInventoryHolder;
    }

    public ForgeState getStateForInventory(Inventory inventory) {
        return getState(inventory);
    }

    public boolean isRunning(ForgeState state) {
        return state != null && (state.isSmelting() || state.isForging());
    }

    public void handleButtonClick(Inventory inventory, Player player) {
        ForgeState state = getState(inventory);
        StationBehavior behavior = behaviorFor(state);
        if (state == null || behavior == null) {
            return;
        }
        behavior.handleButtonClick(this, state, player);
    }

    public void handleInventoryClose(Inventory inventory) {
        ForgeState state = getState(inventory);
        if (state == null || state.isSmelting() || state.isForging()) {
            return;
        }
    }

    public void shutdown() {
        for (ForgeState state : forges.values()) {
            BukkitTask task = state.task();
            if (task != null) {
                task.cancel();
            }
            BukkitTask forgeTask = state.forgeTask();
            if (forgeTask != null) {
                forgeTask.cancel();
            }
        }
        forges.clear();
    }

    public HotItemManager hotItemManager() {
        return hotItemManager;
    }

    public List<Inventory> getForgeInventories() {
        List<Inventory> list = new ArrayList<>();
        for (ForgeState state : forges.values()) {
            list.add(state.inventory());
        }
        return list;
    }

    public void updateButton(ForgeState state) {
        StationBehavior behavior = behaviorFor(state);
        if (behavior == null) {
            return;
        }
        behavior.updateButton(this, state);
    }

    public boolean isInputSlot(ForgeState state, int slot) {
        StationBehavior behavior = behaviorFor(state);
        return behavior != null && behavior.isInputSlot(slot);
    }

    public boolean isOutputSlot(ForgeState state, int slot) {
        StationBehavior behavior = behaviorFor(state);
        return behavior != null && behavior.isOutputSlot(slot);
    }

    public boolean isButtonSlot(ForgeState state, int slot) {
        StationBehavior behavior = behaviorFor(state);
        return behavior != null && behavior.isButtonSlot(slot);
    }

    private StationBehavior behaviorFor(ForgeState state) {
        if (state == null) {
            return null;
        }
        return behaviors.get(state.stationType());
    }

    private ForgeState getOrCreateState(Location location, ForgeState.StationType stationType) {
        ForgeKey key = ForgeKey.from(location);
        ForgeState state = forges.get(key);
        if (state != null) {
            return state;
        }
        ForgeInventoryHolder holder = new ForgeInventoryHolder(location.getBlock().getLocation());
        Inventory inventory = Bukkit.createInventory(holder, 27,
            stationType == ForgeState.StationType.ANVIL ? anvilTitle : smelterTitle);
        holder.setInventory(inventory);
        ForgeState created = new ForgeState(inventory, stationType);
        forges.put(key, created);
        refreshDecor(created);
        updateButton(created);
        return created;
    }

    private ForgeState getState(Inventory inventory) {
        if (!(inventory.getHolder() instanceof ForgeInventoryHolder holder)) {
            return null;
        }
        ForgeKey key = ForgeKey.from(holder.location());
        return forges.get(key);
    }

    Location locationFrom(Inventory inventory) {
        if (inventory.getHolder() instanceof ForgeInventoryHolder holder) {
            return holder.location();
        }
        return null;
    }

    boolean isHammerEquipped(Player player) {
        if (player == null) {
            return false;
        }
        return isHammer(player.getInventory().getItemInMainHand());
    }

    boolean isHammer(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String id = oraxenResolver.readOraxenId(item);
        return id != null && id.toLowerCase(Locale.ROOT).endsWith("_hammer");
    }

    boolean damageEquippedHammer(Player player, ForgeState state) {
        if (player == null) {
            return false;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isHammer(item)) {
            return false;
        }
        int loss = hammerDurabilityLoss(state);
        if (loss <= 0) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return true;
        }
        int max = item.getType().getMaxDurability();
        if (max <= 0) {
            return true;
        }
        int next = damageable.getDamage() + loss;
        if (next >= max) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            return false;
        }
        damageable.setDamage(next);
        item.setItemMeta(meta);
        return true;
    }

    private int hammerDurabilityLoss(ForgeState state) {
        String key = normalizeMaterialKey(state == null ? "" : state.materialId());
        Integer value = hammerDurabilityLoss.get(key);
        if (value == null) {
            value = hammerDurabilityLoss.get("default");
        }
        return value == null ? 1 : Math.max(0, value);
    }

    private String normalizeMaterialKey(String raw) {
        if (raw == null) {
            return "";
        }
        String key = normalize(raw);
        if (key.startsWith("oraxen:")) {
            key = key.substring("oraxen:".length());
        }
        if (key.startsWith("mold_sword_")) {
            key = key.substring("mold_sword_".length());
        }
        if (key.endsWith("_ingot_hot")) {
            key = key.substring(0, key.length() - "_ingot_hot".length());
        }
        if (key.endsWith("_ingot")) {
            key = key.substring(0, key.length() - "_ingot".length());
        }
        if (key.endsWith("_sword")) {
            key = key.substring(0, key.length() - "_sword".length());
        }
        return key;
    }

    String readOraxenId(ItemStack item) {
        return oraxenResolver.readOraxenId(item);
    }
    int readRarityLevel(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return 0;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer value = pdc.get(rarityKey, PersistentDataType.INTEGER);
        if (value == null) {
            return 0;
        }
        return clampRarity(value);
    }

    void ensureRarity(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer value = pdc.get(rarityKey, PersistentDataType.INTEGER);
        if (value != null) {
            return;
        }
        applyRarity(item, 0);
    }
    boolean hasRarityTag(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(rarityKey, PersistentDataType.INTEGER);
    }


    int resolveRarityFromItems(ItemStack a, ItemStack b) {
        boolean aValid = a != null && !a.getType().isAir();
        boolean bValid = b != null && !b.getType().isAir();
        if (aValid && !bValid) {
            return readRarityLevel(a);
        }
        if (bValid && !aValid) {
            return readRarityLevel(b);
        }
        if (!aValid && !bValid) {
            return 0;
        }
        int ra = readRarityLevel(a);
        int rb = readRarityLevel(b);
        int min = Math.min(ra, rb);
        int max = Math.max(ra, rb);
        if (min == max) {
            return min;
        }
        int range = max - min + 1;
        int roll = (int) Math.floor(Math.random() * range);
        return min + roll;
    }

    void applyRarity(ItemStack item, int level) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        int value = clampRarity(level);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(rarityKey, PersistentDataType.INTEGER, value);
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }
        boolean replaced = false;
        for (int i = 0; i < lore.size(); i++) {
            String line = ChatColor.stripColor(lore.get(i));
            if (line != null && line.toLowerCase(Locale.ROOT).contains("rareza")) {
                lore.set(i, rarityLine(value));
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lore.add(rarityLine(value));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

        private int clampRarity(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 4) {
            return 4;
        }
        return value;
    }

    String rarityLabel(int value) {
        return switch (clampRarity(value)) {
            case 0 -> "Comun";
            case 1 -> "Poco comun";
            case 2 -> "Raro";
            case 3 -> "Epico";
            case 4 -> "Legendario";
            default -> "Legendario";
        };
    }

    private String rarityLine(int value) {
        int clamped = clampRarity(value);
        ChatColor color = switch (clamped) {
            case 0 -> ChatColor.WHITE;
            case 1 -> ChatColor.GREEN;
            case 2 -> ChatColor.BLUE;
            case 3 -> ChatColor.DARK_PURPLE;
            case 4 -> ChatColor.GOLD;
            default -> ChatColor.WHITE;
        };
        return ChatColor.GRAY + "Rareza: " + color + rarityLabel(clamped);
    }

int rollRarity(int moldRarity, int materialRarity, double intelligence, double maxIntelligence, double passiveBias) {
        return rollRarity(moldRarity, materialRarity, intelligence, maxIntelligence, passiveBias, rarityRandom, null);
    }

int rollRarity(
        int moldRarity,
        int materialRarity,
        double intelligence,
        double maxIntelligence,
        double passiveBias,
        Random random,
        Map<Integer, Double> outProbabilities
    ) {
        Map<Integer, Double> distribution = buildRarityDistribution(
            moldRarity,
            materialRarity,
            intelligence,
            maxIntelligence,
            passiveBias
        );
        if (distribution.isEmpty()) {
            return moldRarity;
        }
        if (outProbabilities != null) {
            outProbabilities.clear();
            outProbabilities.putAll(distribution);
        }
        double roll = random.nextDouble();
        double cumulative = 0.0;
        int lastKey = moldRarity;
        for (var entry : distribution.entrySet()) {
            lastKey = entry.getKey();
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }
        return lastKey;
    }

Map<Integer, Double> buildRarityDistribution(
        int moldRarity,
        int materialRarity,
        double intelligence,
        double maxIntelligence,
        double passiveBias
    ) {
        int min = Math.min(moldRarity, materialRarity);
        int max = Math.max(moldRarity, materialRarity);
        if (moldRarity == materialRarity) {
            max = Math.min(max + 1, 4);
        }

        double intNorm = maxIntelligence == 0.0 ? 0.0 : (intelligence / maxIntelligence);
        double intBias = (intNorm - 0.5) * 2.0;
        double bias = (intBias * 0.5) + passiveBias;

        double k = 1.2;
        Map<Integer, Double> weights = new LinkedHashMap<>();
        double sum = 0.0;
        for (int i = min; i <= max; i++) {
            double pos = (max == min) ? 0.5 : (i - min) / (double) (max - min);
            double weight = Math.exp(k * bias * (pos - 0.5));
            weights.put(i, weight);
            sum += weight;
        }

        Map<Integer, Double> probabilities = new LinkedHashMap<>();
        for (var entry : weights.entrySet()) {
            probabilities.put(entry.getKey(), entry.getValue() / sum);
        }
        return probabilities;
    }

    int hitBonusForIndex(ForgeState state) {
        int index = state.barIndex();
        int offset = Math.abs(index - 4);
        if (offset < 0) {
            offset = 0;
        }
        if (offset >= hitBonusByOffset.length) {
            return hitBonusByOffset[hitBonusByOffset.length - 1];
        }
        return hitBonusByOffset[offset];
    }

    void updateForgeBar(ForgeState state) {
        if (state == null || state.stationType() != ForgeState.StationType.ANVIL) {
            return;
        }
        Inventory inventory = state.inventory();
        for (int i = 0; i < FORGE_BAR_SLOTS.length; i++) {
            int slot = FORGE_BAR_SLOTS[i];
            inventory.setItem(slot, buildBarItem(Material.GRAY_STAINED_GLASS_PANE));
        }
        int idx = Math.max(0, Math.min(8, state.barIndex()));
        int indicatorSlot = FORGE_BAR_SLOTS[idx];
        int offset = Math.abs(idx - 4);
        Material color;
        switch (offset) {
            case 0 -> color = Material.GREEN_STAINED_GLASS_PANE;
            case 1 -> color = Material.LIME_STAINED_GLASS_PANE;
            case 2 -> color = Material.YELLOW_STAINED_GLASS_PANE;
            case 3 -> color = Material.ORANGE_STAINED_GLASS_PANE;
            default -> color = Material.RED_STAINED_GLASS_PANE;
        }
        inventory.setItem(indicatorSlot, buildBarItem(color));
    }

    void clearForgeBar(ForgeState state) {
        if (state == null || state.stationType() != ForgeState.StationType.ANVIL) {
            return;
        }
        Inventory inventory = state.inventory();
        for (int slot : FORGE_BAR_SLOTS) {
            inventory.setItem(slot, null);
        }
    }


    int computeTempBonus(int currentTemp, int maxTemp) {
        if (maxTemp <= 0 || currentTemp < 0) {
            return 0;
        }
        double ratio = currentTemp / (double) maxTemp;
        double distancia = Math.abs(ratio - tempBonusTargetRatio);
        double bonus = tempBonusBase * (1.0 - (distancia / tempBonusTargetRatio));
        return (int) Math.round(Math.max(0.0, bonus));
    }

    void applyHeatLoss(ForgeState state) {
        int max = state.tempMax();
        if (max <= 0) {
            return;
        }
        double min = Math.min(tempLossMinPercent, tempLossMaxPercent);
        double maxPct = Math.max(tempLossMinPercent, tempLossMaxPercent);
        double pct = min + (Math.random() * (maxPct - min));
        int loss = (int) Math.round(max * pct);
        if (loss < 1) {
            loss = 1;
        }
        int next = Math.max(0, state.tempValue() - loss);
        state.setTempValue(next);
    }    void refreshDecor(ForgeState state) {
        // No decor slots; leave inventory background empty.
    }

        ItemStack buildButton(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    ItemStack buildButton(Material material, String name, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    ItemStack buildHammerButton(String name, java.util.List<String> lore) {
        ItemStack item = null;
        if (hammerButtonItemId != null && !hammerButtonItemId.isBlank()) {
            item = oraxenResolver.buildOraxenItem(hammerButtonItemId, 1);
        }
        if (item == null || item.getType().isAir()) {
            item = new ItemStack(Material.IRON_AXE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBarItem(Material material) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    void consumeSlot(Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        int amount = item.getAmount() - 1;
        if (amount <= 0) {
            inventory.setItem(slot, null);
            return;
        }
        item.setAmount(amount);
        inventory.setItem(slot, item);
    }

    ForgeRecipe findSmelterRecipe(ItemStack a, ItemStack b) {
        for (ForgeRecipe recipe : recipes) {
            if (recipe.matchesSmelter(a, b, oraxenResolver)) {
                return recipe;
            }
        }
        return null;
    }

    ForgeRecipe findAnvilRecipe(ItemStack mold, ItemStack ingot, ItemStack extra) {
        for (ForgeRecipe recipe : recipes) {
            if (recipe.matchesAnvil(mold, ingot, extra, oraxenResolver)) {
                return recipe;
            }
        }
        return null;
    }

    ItemStack buildRecipeOutput(ForgeRecipe recipe) {
        return recipe == null ? null : recipe.buildOutput(oraxenResolver);
    }

    void startSmelt(ForgeState state, ForgeRecipe recipe, Location location, int outputSlot) {
        state.setActiveRecipe(recipe);
        state.setProgressTicks(0);
        state.setTotalTicks(recipe.cookTimeTicks());
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int next = state.progressTicks() + 10;
            state.setProgressTicks(next);
            updateButton(state);
            if (next < state.totalTicks()) {
                return;
            }
            finishSmelt(state, recipe, location, outputSlot);
        }, 10L, 10L);
        state.setTask(task);
    }

    void finishSmelt(ForgeState state, ForgeRecipe recipe, Location location, int outputSlot) {
        BukkitTask task = state.task();
        if (task != null) {
            task.cancel();
        }
        state.setTask(null);
        state.setActiveRecipe(null);
        state.setProgressTicks(0);
        state.setTotalTicks(0);
        Inventory inventory = state.inventory();
        ItemStack output = inventory.getItem(outputSlot);
        ItemStack result = buildRecipeOutput(recipe);
        if (result == null) {
            plugin.getLogger().warning("Resultado de forja nulo. Revisar receta o API de Oraxen.");
            updateButton(state);
            return;
        }
        if (debug) {
            String oraxenId = oraxenResolver.readOraxenId(result);
            plugin.getLogger().info("Forge result -> recipe=" + recipe.id()
                + " type=" + result.getType()
                + " oraxen=" + (oraxenId == null ? "<none>" : oraxenId));
        }
        result = hotItemManager.markHot(result);
        applyRarity(result, state.rarityLevel());
        if (output == null || output.getType().isAir()) {
            inventory.setItem(outputSlot, result);
        } else if (output.isSimilar(result) && output.getAmount() + result.getAmount() <= output.getMaxStackSize()) {
            output.setAmount(output.getAmount() + result.getAmount());
            inventory.setItem(outputSlot, output);
        } else if (location != null && location.getWorld() != null) {
            location.getWorld().dropItemNaturally(location, result);
        }
        updateButton(state);
    }

    void startForge(ForgeState state) {
        state.setProgressTicks(0);
        state.setTotalTicks(maxForgeHits);
        state.setBarIndex(0);
        state.setBarDirection(1);
        state.setBarSpeedTicks(4);
        state.setBarTickCounter(0);
        updateForgeBar(state);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!state.isForging()) {
                    cancel();
                    return;
                }
                int counter = state.barTickCounter() + 1;
                int speed = Math.max(1, state.barSpeedTicks());
                if (counter < speed) {
                    state.setBarTickCounter(counter);
                    return;
                }
                state.setBarTickCounter(0);
                int next = state.barIndex() + state.barDirection();
                if (next <= 0) {
                    next = 0;
                    state.setBarDirection(1);
                } else if (next >= 8) {
                    next = 8;
                    state.setBarDirection(-1);
                }
                state.setBarIndex(next);
                updateForgeBar(state);
            }
        }.runTaskTimer(plugin, 0L, 1L);
        state.setForgeTask(task);
    }

    void finishForge(ForgeState state, Location location, int outputSlot, Player player) {
        ForgeRecipe recipe = state.activeRecipe();
        state.setActiveRecipe(null);
        state.setProgressTicks(0);
        state.setTotalTicks(0);
        BukkitTask forgeTask = state.forgeTask();
        if (forgeTask != null) {
            forgeTask.cancel();
            state.setForgeTask(null);
        }
        clearForgeBar(state);
        ItemStack result = buildRecipeOutput(recipe);
        if (result == null) {
            plugin.getLogger().warning("Resultado de forja nulo. Revisar receta o API de Oraxen.");
            updateButton(state);
            return;
        }
                Inventory inventory = state.inventory();
        ItemStack output = inventory.getItem(outputSlot);
        int finalRarity = rollRarity(
            state.moldRarity(),
            state.materialRarity(),
            0.0,
            100.0,
            0.0
        );
        state.setRarityLevel(finalRarity);
        ItemStack pending = buildPendingItem(state, recipe, player);
        if (output == null || output.getType().isAir()) {
            inventory.setItem(outputSlot, pending);
        } else if (output.isSimilar(pending) && output.getAmount() + pending.getAmount() <= output.getMaxStackSize()) {
            output.setAmount(output.getAmount() + pending.getAmount());
            inventory.setItem(outputSlot, output);
        } else if (location != null && location.getWorld() != null) {
            location.getWorld().dropItemNaturally(location, pending);
        }
        state.setHitBonus(0);
        state.setTempValue(0);
        state.setTempMax(0);
        state.setTempBonus(0);
        state.setHits(0);
        state.setFailures(0);
        state.setTempTier("");
        updateButton(state);
    }

    int maxForgeHits() {
        return maxForgeHits;
    }

    boolean requireHotInput() {
        return requireHotInput;
    }

    public boolean isPendingItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Byte flag = pdc.get(pendingKey, PersistentDataType.BYTE);
        return flag != null && flag == (byte) 1;
    }

    public boolean tryTemperItem(Item entity) {
        if (entity == null || entity.isDead()) {
            return false;
        }
        ItemStack stack = entity.getItemStack();
        if (!isPendingItem(stack)) {
            return false;
        }
        TemperType temperType = resolveTemperType(entity.getLocation().getBlock());
        if (temperType == TemperType.NONE) {
            return false;
        }
        ItemStack finalItem = buildFinalItemFromPending(stack, temperType);
        if (finalItem == null) {
            return false;
        }
        entity.setItemStack(finalItem);
        return true;
    }

    public void scheduleTempering(Item item) {
        if (item == null) {
            return;
        }
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (item.isDead() || !item.isValid()) {
                    cancel();
                    return;
                }
                if (tryTemperItem(item)) {
                    cancel();
                    return;
                }
                ticks += 10;
                if (ticks >= 200) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private TemperType resolveTemperType(Block block) {
        if (block == null) {
            return TemperType.NONE;
        }
        Material type = block.getType();
        if (type == Material.WATER || type == Material.BUBBLE_COLUMN) {
            return TemperType.WATER;
        }
        if (type == Material.LAVA) {
            return TemperType.OIL;
        }
        return TemperType.NONE;
    }

    private ItemStack buildPendingItem(ForgeState state, ForgeRecipe recipe, Player player) {
        ForgeOutput output = recipe.output();
        String outputRef;
        int amount = output.amount();
        if (output.type() == ForgeIngredient.Type.ORAXEN) {
            outputRef = "ORAXEN:" + output.oraxenId();
        } else {
            outputRef = "MATERIAL:" + output.material().name();
        }
        ItemStack pending = new ItemStack(Material.IRON_INGOT, amount);
        ItemMeta meta = pending.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Arma en proceso");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Tirar al agua o aceite");
        lore.add(ChatColor.GRAY + "para templar el arma");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(pendingKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(outputKey, PersistentDataType.STRING, outputRef);
        pdc.set(outputAmountKey, PersistentDataType.INTEGER, amount);
        pdc.set(qualityKey, PersistentDataType.INTEGER, computeFinalQuality(state, TemperType.NONE));
        pdc.set(tempKey, PersistentDataType.INTEGER, state.tempValue());
        pdc.set(tempTierKey, PersistentDataType.STRING, state.tempTier());
        if (player != null) {
            pdc.set(forgedByKey, PersistentDataType.STRING, player.getName());
        }
        pending.setItemMeta(meta);
        applyRarity(pending, state.rarityLevel());
        return pending;
    }

    private ItemStack buildFinalItemFromPending(ItemStack pending, TemperType temperType) {
        if (pending == null || pending.getType().isAir() || !pending.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = pending.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String outputRef = pdc.get(outputKey, PersistentDataType.STRING);
        Integer amount = pdc.get(outputAmountKey, PersistentDataType.INTEGER);
        Integer baseQuality = pdc.get(qualityKey, PersistentDataType.INTEGER);
        Integer tempValue = pdc.get(tempKey, PersistentDataType.INTEGER);
        String tempTier = pdc.get(tempTierKey, PersistentDataType.STRING);
        String forgedBy = pdc.get(forgedByKey, PersistentDataType.STRING);
        Integer rarityValue = pdc.get(rarityKey, PersistentDataType.INTEGER);
        int rarityLevel = rarityValue == null ? 0 : clampRarity(rarityValue);
        if (outputRef == null) {
            return null;
        }
        ItemStack built = buildOutputFromRef(outputRef, amount == null ? 1 : amount);
        if (built == null) {
            return null;
        }
        int quality = (baseQuality == null ? 100 : baseQuality) + temperType.bonus();
        ItemMeta builtMeta = built.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Calidad: " + ChatColor.WHITE + quality);
        if (temperType != TemperType.NONE) {
            lore.add(ChatColor.AQUA + "Templado: " + ChatColor.WHITE + temperType.label());
        }
        if (forgedBy != null && !forgedBy.isBlank()) {
            lore.add(ChatColor.DARK_GRAY + "Forjado por: " + ChatColor.GRAY + forgedBy);
        }
        builtMeta.setLore(lore);
        PersistentDataContainer finalPdc = builtMeta.getPersistentDataContainer();
        finalPdc.set(qualityKey, PersistentDataType.INTEGER, quality);
        finalPdc.set(temperKey, PersistentDataType.STRING, temperType.label());
        if (forgedBy != null) {
            finalPdc.set(forgedByKey, PersistentDataType.STRING, forgedBy);
        }
        if (temperType == TemperType.OIL) {
            AttributeModifier bonus = new AttributeModifier(UUID.randomUUID(), "forge_oil_bonus", 1.0, AttributeModifier.Operation.ADD_NUMBER);
            builtMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, bonus);
            builtMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        built.setItemMeta(builtMeta);
        applyRarity(built, rarityLevel);
        return built;
    }

    private ItemStack buildOutputFromRef(String ref, int amount) {
        if (ref == null) {
            return null;
        }
        String value = ref.trim();
        if (value.toUpperCase(Locale.ROOT).startsWith("ORAXEN:")) {
            String id = value.substring("ORAXEN:".length()).trim();
            return oraxenResolver.buildOraxenItem(id, amount);
        }
        if (value.toUpperCase(Locale.ROOT).startsWith("MATERIAL:")) {
            String id = value.substring("MATERIAL:".length()).trim().toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(id);
            return material == null ? null : new ItemStack(material, amount);
        }
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material != null) {
            return new ItemStack(material, amount);
        }
        return oraxenResolver.buildOraxenItem(value, amount);
    }

    private int computeFinalQuality(ForgeState state, TemperType temperType) {
        int quality = state.baseQuality();
        quality += state.tempBonus();
        quality += state.hitBonus();
        quality += temperType.bonus();
        return quality;
    }

    TemperatureTier resolveTemperatureTier(int temp) {
        if (temp >= 900) {
            return TemperatureTier.INCANDESCENTE;
        }
        if (temp >= 700) {
            return TemperatureTier.CALIENTE;
        }
        if (temp >= 400) {
            return TemperatureTier.TIBIO;
        }
        return TemperatureTier.FRIO;
    }



    private void loadRecipes() {
        recipes.clear();
        var section = plugin.getConfig().getConfigurationSection("forge.recipes");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String input1 = section.getString(key + ".input-1");
            String input2 = section.getString(key + ".input-2");
            String input3 = section.getString(key + ".input-3");
            String output = section.getString(key + ".output");
            int amount = Math.max(1, section.getInt(key + ".output-amount", 1));
            int time = Math.max(20, section.getInt(key + ".time-ticks", defaultCookTime));
            ForgeIngredient ingredientA = parseIngredient(input1);
            ForgeIngredient ingredientB = parseIngredient(input2);
            ForgeIngredient ingredientC = parseIngredient(input3);
            ForgeOutput result = parseOutput(output, amount);
            if (ingredientA == null || result == null) {
                continue;
            }
            recipes.add(new ForgeRecipe(key, ingredientA, ingredientB, ingredientC, result, time));
        }
    }

    private ForgeIngredient parseIngredient(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.toUpperCase(Locale.ROOT).startsWith("ORAXEN:")) {
            return ForgeIngredient.oraxen(value.substring("ORAXEN:".length()).trim().toLowerCase(Locale.ROOT));
        }
        if (value.toUpperCase(Locale.ROOT).startsWith("MATERIAL:")) {
            String materialId = value.substring("MATERIAL:".length()).trim().toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(materialId);
            return material == null ? null : ForgeIngredient.material(material);
        }
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material != null) {
            return ForgeIngredient.material(material);
        }
        return ForgeIngredient.oraxen(value.toLowerCase(Locale.ROOT));
    }

    private ForgeOutput parseOutput(String raw, int amount) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.toUpperCase(Locale.ROOT).startsWith("ORAXEN:")) {
            String id = value.substring("ORAXEN:".length()).trim().toLowerCase(Locale.ROOT);
            return ForgeOutput.oraxen(id, amount);
        }
        if (value.toUpperCase(Locale.ROOT).startsWith("MATERIAL:")) {
            String materialId = value.substring("MATERIAL:".length()).trim().toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(materialId);
            return material == null ? null : ForgeOutput.material(material, amount);
        }
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material != null) {
            return ForgeOutput.material(material, amount);
        }
        return ForgeOutput.oraxen(value.toLowerCase(Locale.ROOT), amount);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String stripColor(String value) {
        return value == null ? "" : ChatColor.stripColor(value);
    }

    private String translateColor(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String decodeUnicodeEscapes(String text) {
        if (text == null || !text.contains("\\u")) {
            return text == null ? "" : text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 5 < text.length() && text.charAt(i + 1) == 'u') {
                String hex = text.substring(i + 2, i + 6);
                try {
                    int code = Integer.parseInt(hex, 16);
                    out.append((char) code);
                    i += 6;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }
}






































