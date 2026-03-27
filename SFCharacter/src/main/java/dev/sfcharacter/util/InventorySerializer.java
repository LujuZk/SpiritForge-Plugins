package dev.sfcharacter.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

public final class InventorySerializer {

    private static final Logger log = Logger.getLogger("SFCharacter");

    private InventorySerializer() {}

    // ─── Array Serialization ────────────────────────────────────────────────

    public static byte @Nullable [] serializeItemArray(ItemStack @Nullable [] items) {
        if (items == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeInt(items.length);
            for (ItemStack item : items) {
                boos.writeObject(item); // handles null
            }
            boos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.warning("[SFCharacter] Error serializing item array: " + e.getMessage());
            return null;
        }
    }

    public static ItemStack @Nullable [] deserializeItemArray(byte @Nullable [] data) {
        if (data == null || data.length == 0) return null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
            int length = bois.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) bois.readObject();
            }
            return items;
        } catch (Exception e) {
            log.warning("[SFCharacter] Error deserializing item array: " + e.getMessage());
            return null;
        }
    }

    // ─── Single Item Serialization ──────────────────────────────────────────

    public static byte @Nullable [] serializeItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeObject(item);
            boos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.warning("[SFCharacter] Error serializing item: " + e.getMessage());
            return null;
        }
    }

    public static @Nullable ItemStack deserializeItem(byte @Nullable [] data) {
        if (data == null || data.length == 0) return null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) bois.readObject();
        } catch (Exception e) {
            log.warning("[SFCharacter] Error deserializing item: " + e.getMessage());
            return null;
        }
    }
}
