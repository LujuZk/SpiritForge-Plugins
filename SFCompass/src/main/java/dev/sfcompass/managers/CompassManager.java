package dev.sfcompass.managers;

import dev.sfcompass.SFCompassPlugin;
import dev.sfcompass.database.CompassDatabase;
import dev.sfcompass.models.Island;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CompassManager {

    private final CompassDatabase db;
    private final int defaultLevel;
    private final NamespacedKey compassKey;
    private final Map<UUID, Integer> levelCache = new ConcurrentHashMap<>();

    public CompassManager(SFCompassPlugin plugin, CompassDatabase db) {
        this.db = db;
        this.defaultLevel = plugin.getConfig().getInt("compass.default-level", 1);
        this.compassKey = new NamespacedKey(plugin, "sfcompass");
    }

    public void loadPlayer(UUID uuid) {
        int level = db.loadLevel(uuid);
        if (level == -1) {
            level = defaultLevel;
            db.saveLevel(uuid, level);
        }
        levelCache.put(uuid, level);
    }

    public void unloadPlayer(UUID uuid) {
        levelCache.remove(uuid);
    }

    public int getLevel(UUID uuid) {
        return levelCache.getOrDefault(uuid, defaultLevel);
    }

    public void setLevel(UUID uuid, int level) {
        levelCache.put(uuid, level);
        db.saveLevel(uuid, level);
    }

    public ItemStack createCompassItem() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        meta.displayName(Component.text("Brújula de Navegación", NamedTextColor.AQUA));
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        return compass;
    }

    public void pointCompassTo(Player player, Island island) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (!isCompassItem(item)) continue;

            CompassMeta meta = (CompassMeta) item.getItemMeta();
            Location target = new Location(
                    player.getServer().getWorld(island.worldName()),
                    island.centerX(), 64, island.centerZ()
            );
            meta.setLodestone(target);
            meta.setLodestoneTracked(false);
            item.setItemMeta(meta);
        }
    }

    public boolean isCompassItem(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(compassKey, PersistentDataType.BYTE);
    }

    public NamespacedKey getCompassKey() {
        return compassKey;
    }
}
