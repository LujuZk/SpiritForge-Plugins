package dev.sfcrafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class HotItemManager {

    private final Plugin plugin;
    private final OraxenItemResolver resolver;
    private final NamespacedKey hotUntilKey;
    private final NamespacedKey rarityKey;
    private final Map<String, HotMapping> hotMappings = new HashMap<>();

    public HotItemManager(Plugin plugin, OraxenItemResolver resolver) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.hotUntilKey = new NamespacedKey(plugin, "hot_until");
        this.rarityKey = new NamespacedKey(plugin, "forge_rarity");
        loadConfig();
    }

    public void loadConfig() {
        hotMappings.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("forge.hot-items");
        if (section == null) {
            return;
        }
        for (String hotId : section.getKeys(false)) {
            String coolInto = section.getString(hotId + ".cool-into");
            int seconds = Math.max(1, section.getInt(hotId + ".cool-time-seconds", 120));
            int startTemp = section.getInt(hotId + ".start-temp", 900);
            int endTemp = section.getInt(hotId + ".end-temp", 40);
            if (coolInto == null || coolInto.isBlank()) {
                continue;
            }
            hotMappings.put(
                normalize(hotId),
                new HotMapping(normalize(hotId), coolInto.trim(), seconds, startTemp, endTemp)
            );
        }
    }

    public ItemStack markHot(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return item;
        }
        String id = resolver.readOraxenId(item);
        if (id == null) {
            return item;
        }
        HotMapping mapping = hotMappings.get(id);
        if (mapping == null) {
            return item;
        }
        var meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(hotUntilKey, PersistentDataType.LONG)) {
            return item;
        }
        long until = System.currentTimeMillis() + mapping.coolTimeSeconds() * 1000L;
        pdc.set(hotUntilKey, PersistentDataType.LONG, until);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean maybeCoolInventory(Inventory inventory) {
        boolean changed = false;
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            ItemStack cooled = maybeCool(item);
            if (cooled != item) {
                inventory.setItem(slot, cooled);
                changed = true;
            }
        }
        return changed;
    }

    public boolean maybeCoolPlayer(PlayerInventory inventory) {
        if (inventory == null) {
            return false;
        }
        boolean changed = false;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            ItemStack cooled = maybeCool(item);
            if (cooled != item) {
                inventory.setItem(slot, cooled);
                changed = true;
            }
        }
        return changed;
    }

    public ItemStack maybeCool(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return item;
        }
        String id = resolver.readOraxenId(item);
        if (id == null) {
            return item;
        }
        HotMapping mapping = hotMappings.get(id);
        if (mapping == null) {
            return item;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Long until = pdc.get(hotUntilKey, PersistentDataType.LONG);
        if (until == null) {
            return item;
        }
        long now = System.currentTimeMillis();
        if (now < until) {
            updateTemperatureLore(item, mapping, until, now);
            return item;
        }
        ItemStack cooled = buildCoolItem(mapping, item.getAmount());
        if (cooled == null) {
            return new ItemStack(Material.AIR);
        }
        copyRarity(item, cooled);
        return cooled;
    }

    public boolean coolIfInWater(Item entity) {
        if (entity == null || entity.isDead()) {
            return false;
        }
        ItemStack stack = entity.getItemStack();
        if (!isHotItem(stack)) {
            return false;
        }
        Block block = entity.getLocation().getBlock();
        if (!isWaterBlock(block)) {
            return false;
        }
        ItemStack cooled = coolImmediately(stack);
        if (cooled == null) {
            return false;
        }
        entity.setItemStack(cooled);
        if (entity.getWorld() != null) {
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 1.2f);
        }
        return true;
    }

    public boolean isHotItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String id = resolver.readOraxenId(item);
        if (id == null) {
            return false;
        }
        return hotMappings.containsKey(id);
    }

    public int getCurrentTemperature(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return -1;
        }
        String id = resolver.readOraxenId(item);
        if (id == null) {
            return -1;
        }
        HotMapping mapping = hotMappings.get(id);
        if (mapping == null) {
            return -1;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Long until = pdc.get(hotUntilKey, PersistentDataType.LONG);
        if (until == null) {
            return -1;
        }
        long now = System.currentTimeMillis();
        int totalSeconds = Math.max(1, mapping.coolTimeSeconds());
        long remainingMs = Math.max(0L, until - now);
        double ratio = Math.min(1.0, remainingMs / (totalSeconds * 1000.0));
        int start = mapping.startTemp();
        int end = mapping.endTemp();
        return (int) Math.round(end + (start - end) * ratio);
    }

    public int getMaxTemperature(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return -1;
        }
        String id = resolver.readOraxenId(item);
        if (id == null) {
            return -1;
        }
        HotMapping mapping = hotMappings.get(id);
        if (mapping == null) {
            return -1;
        }
        return mapping.startTemp();
    }

    public ItemStack coolImmediately(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        String id = resolver.readOraxenId(item);
        if (id == null) {
            return null;
        }
        HotMapping mapping = hotMappings.get(id);
        if (mapping == null) {
            return null;
        }
        ItemStack cooled = buildCoolItem(mapping, item.getAmount());
        if (cooled == null) {
            return null;
        }
        copyRarity(item, cooled);
        return cooled;
    }

    private ItemStack buildCoolItem(HotMapping mapping, int amount) {
        String raw = mapping.coolInto();
        if (raw.toUpperCase(Locale.ROOT).startsWith("ORAXEN:")) {
            String id = raw.substring("ORAXEN:".length()).trim();
            return resolver.buildOraxenItem(id, amount);
        }
        if (raw.toUpperCase(Locale.ROOT).startsWith("MATERIAL:")) {
            String mat = raw.substring("MATERIAL:".length()).trim().toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(mat);
            return material == null ? null : new ItemStack(material, amount);
        }
        Material material = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
        if (material != null) {
            return new ItemStack(material, amount);
        }
        return resolver.buildOraxenItem(raw, amount);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isWaterBlock(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (type == Material.WATER || type == Material.BUBBLE_COLUMN) {
            return true;
        }
        if (block.getBlockData() instanceof Waterlogged waterlogged) {
            return waterlogged.isWaterlogged();
        }
        return type == Material.KELP
            || type == Material.KELP_PLANT
            || type == Material.SEAGRASS
            || type == Material.TALL_SEAGRASS;
    }

    private void updateTemperatureLore(ItemStack item, HotMapping mapping, long until, long now) {
        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        int totalSeconds = Math.max(1, mapping.coolTimeSeconds());
        long remainingMs = Math.max(0L, until - now);
        double ratio = Math.min(1.0, remainingMs / (totalSeconds * 1000.0));
        int start = mapping.startTemp();
        int end = mapping.endTemp();
        int temp = (int) Math.round(end + (start - end) * ratio);

        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        boolean replaced = false;
        for (int i = 0; i < lore.size(); i++) {
            String text = plain.serialize(lore.get(i));
            if (text.toLowerCase(Locale.ROOT).contains("temperatura")) {
                lore.set(i, temperatureComponent(temp));
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lore.add(temperatureComponent(temp));
        }
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
    }

    private void copyRarity(ItemStack source, ItemStack target) {
        if (source == null || target == null) {
            return;
        }
        ItemMeta sourceMeta = source.getItemMeta();
        if (sourceMeta == null) {
            return;
        }
        PersistentDataContainer sourcePdc = sourceMeta.getPersistentDataContainer();
        Integer value = sourcePdc.get(rarityKey, PersistentDataType.INTEGER);
        if (value == null) {
            return;
        }
        int level = clampRarity(value);
        ItemMeta targetMeta = target.getItemMeta();
        if (targetMeta == null) {
            return;
        }
        targetMeta.getPersistentDataContainer().set(rarityKey, PersistentDataType.INTEGER, level);

        List<Component> lore = targetMeta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        boolean replaced = false;
        for (int i = 0; i < lore.size(); i++) {
            String text = plain.serialize(lore.get(i));
            if (text.toLowerCase(Locale.ROOT).contains("rareza")) {
                lore.set(i, rarityComponent(level));
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lore.add(rarityComponent(level));
        }
        targetMeta.lore(lore);
        target.setItemMeta(targetMeta);
    }

    private Component temperatureComponent(int temp) {
        return Component.text("Temperatura: " + temp + " C")
            .color(TextColor.fromHexString("#FF6F00"));
    }

    private Component rarityComponent(int level) {
        return Component.text("Rareza: " + rarityLabel(level))
            .color(TextColor.fromHexString("#9E9E9E"));
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

    private String rarityLabel(int value) {
        return switch (clampRarity(value)) {
            case 0 -> "Comun";
            case 1 -> "Poco comun";
            case 2 -> "Raro";
            case 3 -> "Epico";
            case 4 -> "Legendario";
            default -> "Legendario";
        };
    }

    private record HotMapping(String hotId, String coolInto, int coolTimeSeconds, int startTemp, int endTemp) {}
}

