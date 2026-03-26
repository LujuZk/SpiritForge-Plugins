package dev.sfcrafting;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class OraxenItemResolver {

    private final Plugin plugin;
    private final NamespacedKey oraxenItemIdKey = new NamespacedKey("oraxen", "item_id");
    private final NamespacedKey oraxenIdKey = new NamespacedKey("oraxen", "id");
    private final NamespacedKey oraxenLegacyKey = new NamespacedKey("oraxen", "oraxen_item_id");
    private boolean warnedBuildFailure = false;

    public OraxenItemResolver(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOraxenItem(ItemStack item, String expectedId) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        String id = readOraxenId(item);
        return id != null && id.equals(normalize(expectedId));
    }

    public String readOraxenId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(oraxenItemIdKey, PersistentDataType.STRING);
        if (id == null) {
            id = pdc.get(oraxenIdKey, PersistentDataType.STRING);
        }
        if (id == null) {
            id = pdc.get(oraxenLegacyKey, PersistentDataType.STRING);
        }
        return id == null ? null : normalize(id);
    }

    public ItemStack buildOraxenItem(String id, int amount) {
        try {
            Class<?> itemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            var getItemById = itemsClass.getMethod("getItemById", String.class);
            Object oraxenItem = getItemById.invoke(null, id);
            if (oraxenItem == null) {
                return null;
            }
            var buildMethod = oraxenItem.getClass().getMethod("build");
            ItemStack item = (ItemStack) buildMethod.invoke(oraxenItem);
            if (item == null) {
                return null;
            }
            item.setAmount(Math.max(1, amount));
            return item;
        } catch (Exception ex) {
            if (!warnedBuildFailure) {
                plugin.getLogger().warning("No se pudo construir item de Oraxen via API. Revisar dependencia.");
                warnedBuildFailure = true;
            }
            return null;
        }
    }

    public String readOraxenBlockId(Block block) {
        if (block == null) {
            return null;
        }
        Object mechanic = resolveBlockMechanic(block);
        if (mechanic == null) {
            return null;
        }
        String id = tryInvokeString(mechanic, "getItemID")
            .or(() -> tryInvokeString(mechanic, "getItemId"))
            .or(() -> tryInvokeString(mechanic, "getId"))
            .orElse(null);
        return id == null ? null : normalize(id);
    }

    private Object resolveBlockMechanic(Block block) {
        try {
            Class<?> blocksClass = Class.forName("io.th0rgal.oraxen.api.OraxenBlocks");
            Object mechanic = tryInvoke(blocksClass, "getOraxenBlock", block);
            if (mechanic != null) {
                return mechanic;
            }
            mechanic = tryInvoke(blocksClass, "getBlockMechanic", block);
            if (mechanic != null) {
                return mechanic;
            }
            mechanic = tryInvoke(blocksClass, "getNoteBlockMechanic", block);
            if (mechanic != null) {
                return mechanic;
            }
            mechanic = tryInvoke(blocksClass, "getStringMechanic", block);
            if (mechanic != null) {
                return mechanic;
            }
            return tryInvoke(blocksClass, "getChorusMechanic", block);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object tryInvoke(Class<?> clazz, String method, Object arg) {
        try {
            return clazz.getMethod(method, Block.class).invoke(null, arg);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Optional<String> tryInvokeString(Object target, String method) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return value == null ? Optional.empty() : Optional.of(value.toString());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

