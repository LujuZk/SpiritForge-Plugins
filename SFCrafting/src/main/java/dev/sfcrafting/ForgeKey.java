package dev.sfcrafting;

import java.util.UUID;
import org.bukkit.Location;

public record ForgeKey(UUID worldId, int x, int y, int z) {

    public static ForgeKey from(Location location) {
        Location block = location.getBlock().getLocation();
        UUID worldId = block.getWorld() == null ? new UUID(0L, 0L) : block.getWorld().getUID();
        return new ForgeKey(worldId, block.getBlockX(), block.getBlockY(), block.getBlockZ());
    }
}

