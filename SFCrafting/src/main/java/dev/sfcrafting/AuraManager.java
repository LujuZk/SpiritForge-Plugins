package dev.sfcrafting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class AuraManager {

    public static final String VISUAL_ITEM_ID = "aura";

    private static final double DEFAULT_AURA_Y = 0.35;
    private static final double DEFAULT_AURA_SCALE = 1.0;
    private static final double MIN_AURA_SCALE = 0.1;
    private static final double MAX_AURA_SCALE = 3.0;
    private static final int LIGHT_LEVEL = 15;

    private final SFCraftingPlugin plugin;
    private final OraxenItemResolver resolver;
    private final Map<UUID, ItemDisplay> activeAuras = new HashMap<>();
    private final Map<UUID, BlockPos> playerLightPos = new HashMap<>();
    private final Map<BlockPos, Integer> lightRefs = new HashMap<>();
    private BukkitTask followTask;
    private long tickCounter = 0L;
    private double auraBaseY = DEFAULT_AURA_Y;
    private double auraScale = DEFAULT_AURA_SCALE;

    public AuraManager(SFCraftingPlugin plugin) {
        this.plugin = plugin;
        this.resolver = new OraxenItemResolver(plugin);
        reloadSettings();
    }

    public void start() {
        if (followTask != null) {
            return;
        }
        followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 1L);
    }

    public void shutdown() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        for (ItemDisplay display : activeAuras.values()) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        activeAuras.clear();

        for (BlockPos pos : playerLightPos.values()) {
            removeLightRef(pos);
        }
        playerLightPos.clear();
    }

    public void reloadSettings() {
        FileConfiguration config = plugin.getConfig();
        auraBaseY = readDouble(config, "aura.y", "forge.aura.y", DEFAULT_AURA_Y);

        double configuredScale = readDouble(config, "aura.scale", "forge.aura.scale", DEFAULT_AURA_SCALE);
        auraScale = Math.max(MIN_AURA_SCALE, Math.min(MAX_AURA_SCALE, configuredScale));
        if (configuredScale != auraScale) {
            plugin.getLogger().warning("aura.scale fuera de rango; usando " + auraScale + " (rango "
                + MIN_AURA_SCALE + "-" + MAX_AURA_SCALE + ").");
        }

        // Apply new scale immediately to already-active auras.
        for (ItemDisplay display : activeAuras.values()) {
            if (display != null && !display.isDead()) {
                applyDisplayScale(display);
            }
        }

        plugin.getLogger().info("Aura config: y=" + auraBaseY + ", scale=" + auraScale
            + " (config: " + plugin.getDataFolder().getAbsolutePath() + "\\config.yml)");
    }

    private double readDouble(FileConfiguration config, String path, String legacyPath, double def) {
        if (config.isSet(path)) {
            return config.getDouble(path, def);
        }
        if (config.isSet(legacyPath)) {
            return config.getDouble(legacyPath, def);
        }
        return def;
    }

    public boolean isActive(Player player) {
        return player != null && activeAuras.containsKey(player.getUniqueId());
    }

    public boolean activate(Player player) {
        if (player == null) {
            return false;
        }
        UUID id = player.getUniqueId();
        if (activeAuras.containsKey(id)) {
            return true;
        }

        ItemDisplay display = spawnAura(player);
        if (display == null) {
            return false;
        }

        activeAuras.put(id, display);
        return true;
    }

    public boolean deactivate(Player player) {
        if (player == null) {
            return false;
        }

        UUID id = player.getUniqueId();
        ItemDisplay display = activeAuras.remove(id);
        BlockPos lightPos = playerLightPos.remove(id);
        if (lightPos != null) {
            removeLightRef(lightPos);
        }

        if (display != null && !display.isDead()) {
            display.remove();
            return true;
        }
        return false;
    }

    public boolean toggle(Player player) {
        if (isActive(player)) {
            deactivate(player);
            return false;
        }
        activate(player);
        return true;
    }

    private ItemDisplay spawnAura(Player player) {
        ItemStack visualItem = resolver.buildOraxenItem(VISUAL_ITEM_ID, 1);
        if (visualItem == null || visualItem.getType().isAir()) {
            plugin.getLogger().warning("No se pudo crear item visual de aura (id: aura).");
            return null;
        }

        World world = player.getWorld();
        Location loc = player.getLocation().clone().add(0.0, auraBaseY, 0.0);
        ItemDisplay display = (ItemDisplay) world.spawnEntity(loc, EntityType.ITEM_DISPLAY);

        display.setPersistent(false);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.setGravity(false);
        display.setItemStack(visualItem);
        display.setInterpolationDuration(1);
        display.setInterpolationDelay(0);
        applyDisplayScale(display);

        return display;
    }

    private void applyDisplayScale(ItemDisplay display) {
        Transformation t = display.getTransformation();
        Vector3f scale = new Vector3f((float) auraScale, (float) auraScale, (float) auraScale);
        Transformation updated = new Transformation(
            new Vector3f(t.getTranslation()),
            new Quaternionf(t.getLeftRotation()),
            scale,
            new Quaternionf(t.getRightRotation())
        );
        display.setTransformation(updated);
    }

    private void tick() {
        tickCounter++;
        if (activeAuras.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, ItemDisplay>> iterator = activeAuras.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ItemDisplay> entry = iterator.next();
            UUID id = entry.getKey();
            Player player = Bukkit.getPlayer(id);
            ItemDisplay display = entry.getValue();

            if (player == null || !player.isOnline() || display == null || display.isDead()) {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
                BlockPos lightPos = playerLightPos.remove(id);
                if (lightPos != null) {
                    removeLightRef(lightPos);
                }
                iterator.remove();
                continue;
            }

            double bob = Math.sin((tickCounter * 0.18) + (player.getEntityId() * 0.35)) * 0.1;
            Location loc = player.getLocation().clone().add(0.0, auraBaseY + bob, 0.0);
            display.teleport(loc);
            float yaw = (player.getLocation().getYaw() + ((tickCounter * 4) % 360)) % 360;
            display.setRotation(yaw, 0.0f);

            BlockPos newLightPos = BlockPos.from(player.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            BlockPos oldLightPos = playerLightPos.get(id);
            if (!newLightPos.equals(oldLightPos)) {
                if (oldLightPos != null) {
                    removeLightRef(oldLightPos);
                }
                if (addLightRef(newLightPos)) {
                    playerLightPos.put(id, newLightPos);
                } else {
                    playerLightPos.remove(id);
                }
            }
        }
    }

    private boolean addLightRef(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        Integer ref = lightRefs.get(pos);
        if (ref != null) {
            lightRefs.put(pos, ref + 1);
            return true;
        }

        World world = Bukkit.getWorld(pos.worldId);
        if (world == null) {
            return false;
        }
        Block block = world.getBlockAt(pos.x, pos.y, pos.z);
        Material type = block.getType();
        if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR) {
            return false;
        }

        block.setType(Material.LIGHT, false);
        BlockData data = block.getBlockData();
        if (data instanceof Light light) {
            light.setLevel(LIGHT_LEVEL);
            block.setBlockData(light, false);
        }

        lightRefs.put(pos, 1);
        return true;
    }

    private void removeLightRef(BlockPos pos) {
        if (pos == null) {
            return;
        }
        Integer ref = lightRefs.get(pos);
        if (ref == null) {
            return;
        }
        if (ref > 1) {
            lightRefs.put(pos, ref - 1);
            return;
        }

        lightRefs.remove(pos);
        World world = Bukkit.getWorld(pos.worldId);
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(pos.x, pos.y, pos.z);
        if (block.getType() == Material.LIGHT) {
            block.setType(Material.AIR, false);
        }
    }

    private static final class BlockPos {
        private final UUID worldId;
        private final int x;
        private final int y;
        private final int z;

        private BlockPos(UUID worldId, int x, int y, int z) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static BlockPos from(World world, int x, int y, int z) {
            return new BlockPos(world.getUID(), x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BlockPos other)) {
                return false;
            }
            return x == other.x && y == other.y && z == other.z && worldId.equals(other.worldId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldId, x, y, z);
        }
    }
}

