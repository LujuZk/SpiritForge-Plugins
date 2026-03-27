package dev.sfdrops.listener;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatType;
import dev.sfdrops.SFDropsPlugin;
import dev.sfdrops.model.Rarity;
import dev.sfdrops.service.MiningRarityGaussian;
import dev.sfdrops.service.RarityRoller;
import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.SkillType;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class OreDropListener implements Listener {

    private final SFDropsPlugin plugin;

    public OreDropListener(SFDropsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        String blockId = resolveBlockId(block);
        boolean debug = plugin.isDebug(player);

        if (debug) {
            player.sendMessage(String.format(Locale.US,
                    "[SFDrops DEBUG] type=%s blockId=%s cancelled=%s",
                    block.getType().name(),
                    blockId == null ? "null" : blockId,
                    event.isCancelled()));
        }

        if (blockId == null) return;

        String dropId = findDropId(blockId);
        if (debug) {
            player.sendMessage("[SFDrops DEBUG] mappedDrop=" + (dropId == null ? "null" : dropId));
            plugin.getLogger().info("[DEBUG] player=" + player.getName() + " blockId=" + blockId + " dropId=" + dropId + " cancelled=" + event.isCancelled());
        }

        if (dropId == null || dropId.isBlank()) return;

        event.setDropItems(false);

        double str = readStr(player);
        double mining = readMining(player);
        double clazz = plugin.getConfig().getDouble("defaultClassBonus", 0.0);
        double pickaxe = readPickaxePower(player.getInventory().getItemInMainHand());

        Map<Rarity, Double> chances = MiningRarityGaussian.calculate(
                str,
                mining,
                clazz,
                pickaxe,
                plugin.getConfig().getDouble("normalization.strMax", 100.0),
                plugin.getConfig().getDouble("normalization.miningMax", 200.0),
                plugin.getConfig().getDouble("normalization.classMax", 100.0),
                plugin.getConfig().getDouble("normalization.pickaxeMax", 10.0),
                plugin.getConfig().getDouble("weights.str", 0.30),
                plugin.getConfig().getDouble("weights.mining", 0.30),
                plugin.getConfig().getDouble("weights.class", 0.20),
                plugin.getConfig().getDouble("weights.pickaxe", 0.20),
                plugin.getConfig().getDouble("gaussian.curveExponent", 1.5),
                plugin.getConfig().getDouble("gaussian.width", 0.8),
                plugin.getConfig().getDouble("minimumWeight", 0.01)
        );

        Rarity rarity = RarityRoller.roll(chances);

        if (debug) {
            player.sendMessage(String.format(Locale.US,
                    "[SFDrops DEBUG] str=%.2f mining=%.2f class=%.2f pickaxe=%.2f | C=%.2f U=%.2f R=%.2f E=%.2f L=%.2f | rolled=%s",
                    str,
                    mining,
                    clazz,
                    pickaxe,
                    chances.getOrDefault(Rarity.COMMON, 0.0) * 100.0,
                    chances.getOrDefault(Rarity.UNCOMMON, 0.0) * 100.0,
                    chances.getOrDefault(Rarity.RARE, 0.0) * 100.0,
                    chances.getOrDefault(Rarity.EPIC, 0.0) * 100.0,
                    chances.getOrDefault(Rarity.LEGENDARY, 0.0) * 100.0,
                    rarity
            ));
        }

        ItemStack stack = buildDropItem(dropId);
        if (stack == null || stack.getType().isAir()) {
            if (debug) player.sendMessage("[SFDrops DEBUG] item no existe: " + dropId);
            return;
        }

        applyRarity(stack, rarity);

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(BlockDropItemEvent event) {
        String blockId = resolveBlockId(event.getBlockState());
        if (blockId == null) return;

        String dropId = findDropId(blockId);
        if (dropId == null || dropId.isBlank()) return;

        event.getItems().clear();
    }

    private String resolveBlockId(Block block) {
        String oraxenId = resolveOraxenId(block);
        if (oraxenId != null) return normalizeId(oraxenId);
        return block.getType().name().toLowerCase(Locale.ROOT);
    }

    private String resolveBlockId(BlockState state) {
        String oraxenId = resolveOraxenId(state);
        if (oraxenId != null) return normalizeId(oraxenId);
        return state.getType().name().toLowerCase(Locale.ROOT);
    }

    private String resolveOraxenId(Block block) {
        var blockMechanic = OraxenBlocks.getBlockMechanic(block);
        if (blockMechanic != null) return blockMechanic.getItemID();

        var genericMechanic = OraxenBlocks.getOraxenBlock(block.getLocation());
        if (genericMechanic != null) return genericMechanic.getItemID();

        return null;
    }

    private String resolveOraxenId(BlockState state) {
        var blockMechanic = OraxenBlocks.getBlockMechanic(state.getBlock());
        if (blockMechanic != null) return blockMechanic.getItemID();

        var genericMechanic = OraxenBlocks.getOraxenBlock(state.getBlockData());
        if (genericMechanic != null) return genericMechanic.getItemID();

        return null;
    }

    private String findDropId(String blockId) {
        String direct = plugin.getConfig().getString("oreDrops." + blockId);
        if (direct != null && !direct.isBlank()) return direct;

        String normalized = normalizeId(blockId);
        String normalizedPath = plugin.getConfig().getString("oreDrops." + normalized);
        if (normalizedPath != null && !normalizedPath.isBlank()) return normalizedPath;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("oreDrops");
        if (section == null) return null;

        for (String key : section.getKeys(false)) {
            if (normalizeId(key).equalsIgnoreCase(normalized)) {
                String value = section.getString(key);
                if (value != null && !value.isBlank()) return value;
            }
        }
        return null;
    }

    private String normalizeId(String id) {
        int index = id.indexOf(':');
        return index >= 0 ? id.substring(index + 1) : id;
    }

    private ItemStack buildDropItem(String dropId) {
        ItemBuilder builder = OraxenItems.getItemById(dropId);
        if (builder != null) {
            ItemStack oraxenStack = builder.build();
            if (oraxenStack != null && !oraxenStack.getType().isAir()) return oraxenStack;
        }

        String normalized = dropId;
        int idx = normalized.indexOf(':');
        if (idx >= 0) normalized = normalized.substring(idx + 1);
        normalized = normalized.trim().toUpperCase(Locale.ROOT);

        Material material = Material.matchMaterial(normalized);
        if (material == null || material.isAir()) return null;

        return new ItemStack(material, 1);
    }
    private void applyRarity(ItemStack item, Rarity rarity) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int value = switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
        };

        NamespacedKey sfcraftingKey = new NamespacedKey("sfcrafting", "forge_rarity");
        NamespacedKey customforgeKey = new NamespacedKey("customforge", "forge_rarity");
        meta.getPersistentDataContainer().set(sfcraftingKey, PersistentDataType.INTEGER, value);
        meta.getPersistentDataContainer().set(customforgeKey, PersistentDataType.INTEGER, value);

        java.util.List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        lore.removeIf(line -> line.toLowerCase(Locale.ROOT).contains("rareza:"));
        lore.add("Rareza: " + rarityLabel(rarity));
        meta.setLore(lore);

        item.setItemMeta(meta);
    }

    private String rarityLabel(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "Comun";
            case UNCOMMON -> "Poco comun";
            case RARE -> "Raro";
            case EPIC -> "Epico";
            case LEGENDARY -> "Legendario";
        };
    }

    private double readStr(Player player) {
        if (!SFCoreAPI.isAvailable()) return 0;
        try {
            return SFCoreAPI.get().getTotal(player, StatType.STR);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double readMining(Player player) {
        try {
            SkillTreePlugin skill = SkillTreePlugin.getInstance();
            if (skill == null) return 0;
            var data = skill.getSkillManager().getData(player);
            return data.getLevel(SkillType.MINING);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double readPickaxePower(ItemStack tool) {
        if (tool == null) return 0;
        Material material = tool.getType();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("pickaxePower");
        if (section == null) return 0;
        return section.getDouble(material.name(), 0.0);
    }
}





