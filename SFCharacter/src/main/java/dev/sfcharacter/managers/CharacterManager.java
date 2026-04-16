package dev.sfcharacter.managers;

import dev.sfcharacter.SFCharacterPlugin;
import dev.sfcharacter.database.CharacterDatabase;
import dev.sfcharacter.models.CharacterClass;
import dev.sfcharacter.models.CharacterData;
import dev.sfcharacter.models.LocationData;
import dev.sfcharacter.util.InventorySerializer;
import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.database.AsyncDatabaseExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CharacterManager {

    private final SFCharacterPlugin plugin;
    private final CharacterDatabase db;

    private final Map<UUID, List<CharacterData>> characterCache = new HashMap<>();
    private final Map<UUID, Integer> activeSlotCache = new HashMap<>();
    private final Map<UUID, Integer> pendingSlotSelection = new HashMap<>();
    private final Set<UUID> inCharacterSelection = new HashSet<>();

    private Location lobbyLocation;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CharacterManager(SFCharacterPlugin plugin, CharacterDatabase db) {
        this.plugin = plugin;
        this.db = db;
    }

    // ─── Player Lifecycle ───────────────────────────────────────────────────

    public void loadPlayer(UUID uuid, Runnable onComplete) {
        characterCache.put(uuid, new ArrayList<>());
        activeSlotCache.put(uuid, -1);
        AsyncDatabaseExecutor async = SFCoreAPI.get().getAsyncExecutor();
        async.thenOnMain(async.supplyAsync(() -> {
            List<CharacterData> characters = db.loadCharacters(uuid);
            int activeSlot = db.loadActiveSlot(uuid);
            return new Object[]{characters, activeSlot};
        }), result -> {
            @SuppressWarnings("unchecked")
            List<CharacterData> characters = (List<CharacterData>) result[0];
            int activeSlot = (int) result[1];
            characterCache.put(uuid, new ArrayList<>(characters));
            activeSlotCache.put(uuid, activeSlot);
            if (onComplete != null) onComplete.run();
        });
    }

    public void unloadPlayer(UUID uuid) {
        characterCache.remove(uuid);
        activeSlotCache.remove(uuid);
        pendingSlotSelection.remove(uuid);
        inCharacterSelection.remove(uuid);
    }

    // ─── Character Queries ──────────────────────────────────────────────────

    public List<CharacterData> getCharacters(UUID uuid) {
        return characterCache.getOrDefault(uuid, Collections.emptyList());
    }

    public CharacterData getCharacter(UUID uuid, int slot) {
        for (CharacterData cd : getCharacters(uuid)) {
            if (cd.slot() == slot) return cd;
        }
        return null;
    }

    public boolean hasActiveCharacter(UUID uuid) {
        return activeSlotCache.getOrDefault(uuid, -1) >= 0;
    }

    public CharacterData getActiveCharacter(UUID uuid) {
        int slot = activeSlotCache.getOrDefault(uuid, -1);
        if (slot < 0) return null;
        return getCharacter(uuid, slot);
    }

    // ─── Character Selection State ──────────────────────────────────────────

    public boolean isInCharacterSelection(UUID uuid) {
        return inCharacterSelection.contains(uuid);
    }

    public void setInCharacterSelection(UUID uuid, boolean value) {
        if (value) {
            inCharacterSelection.add(uuid);
        } else {
            inCharacterSelection.remove(uuid);
        }
    }

    // ─── Character Actions ──────────────────────────────────────────────────

    public void setActiveSlot(UUID uuid, int slot) {
        activeSlotCache.put(uuid, slot);
        SFCoreAPI.get().getAsyncExecutor().runAsync(() -> db.saveActiveSlot(uuid, slot));
    }

    public CharacterData createCharacter(UUID uuid, int slot, CharacterClass clazz) {
        long count = getCharacters(uuid).stream()
                .filter(c -> c.characterClass() == clazz)
                .count();
        String displayName = clazz.getDisplayName() + " #" + (count + 1);
        String createdAt = LocalDateTime.now().format(DATE_FORMAT);

        CharacterData data = new CharacterData(uuid, slot, clazz, displayName, createdAt);
        SFCoreAPI.get().getAsyncExecutor().runAsync(() -> db.saveCharacter(data));

        List<CharacterData> list = characterCache.computeIfAbsent(uuid, k -> new ArrayList<>());
        list.removeIf(c -> c.slot() == slot);
        list.add(data);

        setActiveSlot(uuid, slot);

        return data;
    }

    // ─── Character State (Inventory + Location) ─────────────────────────────

    /**
     * Saves current player inventory and location for the active character.
     * @param sync true para guardar sincrono (onDisable), false para async (quit/switch)
     */
    public void saveCharacterState(Player player, boolean sync) {
        int slot = activeSlotCache.getOrDefault(player.getUniqueId(), -1);
        if (slot < 0) return;

        UUID uuid = player.getUniqueId();

        // Serializar en main thread (accede Bukkit API)
        byte[] inventoryData = InventorySerializer.serializeItemArray(player.getInventory().getContents());
        byte[] armorData = InventorySerializer.serializeItemArray(player.getInventory().getArmorContents());
        byte[] offhandData = InventorySerializer.serializeItem(player.getInventory().getItemInOffHand());

        Location loc = player.getLocation();
        String world = loc.getWorld().getName();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        float yaw = loc.getYaw(), pitch = loc.getPitch();

        Runnable dbWrite = () -> {
            db.saveInventory(uuid, slot, inventoryData, armorData, offhandData);
            db.saveLocation(uuid, slot, world, x, y, z, yaw, pitch);
        };

        if (sync) {
            dbWrite.run();
        } else {
            SFCoreAPI.get().getAsyncExecutor().runAsync(dbWrite);
        }
    }

    /** Shortcut async (default para quit y switch). */
    public void saveCharacterState(Player player) {
        saveCharacterState(player, false);
    }

    /**
     * Restores inventory and teleports to saved location for the active character.
     * Reads from DB async, then applies on main thread via callback.
     */
    public void loadCharacterState(Player player) {
        int slot = activeSlotCache.getOrDefault(player.getUniqueId(), -1);
        if (slot < 0) return;

        UUID uuid = player.getUniqueId();
        player.getInventory().clear();

        AsyncDatabaseExecutor async = SFCoreAPI.get().getAsyncExecutor();
        CompletableFuture<Object[]> future = async.supplyAsync(() -> {
            byte[][] invData = db.loadInventory(uuid, slot);
            LocationData locData = db.loadLocation(uuid, slot);
            return new Object[]{invData, locData};
        });

        async.thenOnMain(future, result -> {
            if (!player.isOnline()) return;

            byte[][] invData = (byte[][]) result[0];
            LocationData locData = (LocationData) result[1];

            // Restore inventory
            if (invData != null) {
                ItemStack[] contents = InventorySerializer.deserializeItemArray(invData[0]);
                ItemStack[] armor = InventorySerializer.deserializeItemArray(invData[1]);
                ItemStack offhand = InventorySerializer.deserializeItem(invData[2]);

                if (contents != null) player.getInventory().setContents(contents);
                if (armor != null) player.getInventory().setArmorContents(armor);
                if (offhand != null) player.getInventory().setItemInOffHand(offhand);
            }

            // Restore location
            Location targetLoc;
            if (locData != null) {
                World world = Bukkit.getWorld(locData.world());
                if (world != null) {
                    targetLoc = new Location(world, locData.x(), locData.y(), locData.z(),
                            locData.yaw(), locData.pitch());
                } else {
                    plugin.getLogger().warning("World '" + locData.world() + "' not loaded, using spawn");
                    targetLoc = Bukkit.getWorlds().getFirst().getSpawnLocation();
                }
            } else {
                targetLoc = Bukkit.getWorlds().getFirst().getSpawnLocation();
            }

            player.teleport(targetLoc);
            player.setAllowFlight(false);
            player.setFlying(false);
            inCharacterSelection.remove(uuid);
        });
    }

    /**
     * Clears player inventory, resets health/food, teleports to lobby.
     */
    public void clearAndSendToLobby(Player player) {
        player.getInventory().clear();
        var maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            player.setHealth(maxHealthAttr.getValue());
        }
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setAllowFlight(true);
        player.teleport(getLobbyLocation());
        inCharacterSelection.add(player.getUniqueId());
    }

    /**
     * Gets the lobby location from config, cached after first read.
     */
    public Location getLobbyLocation() {
        if (lobbyLocation == null) {
            String worldName = plugin.getConfig().getString("lobby.world", "world");
            double x = plugin.getConfig().getDouble("lobby.x", 0.5);
            double y = plugin.getConfig().getDouble("lobby.y", 100.0);
            double z = plugin.getConfig().getDouble("lobby.z", 0.5);
            float yaw = (float) plugin.getConfig().getDouble("lobby.yaw", 0.0);
            float pitch = (float) plugin.getConfig().getDouble("lobby.pitch", 0.0);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Lobby world '" + worldName + "' not found, using default");
                world = Bukkit.getWorlds().getFirst();
            }
            lobbyLocation = new Location(world, x, y, z, yaw, pitch);
        }
        return lobbyLocation;
    }

    // ─── Pending Slot Selection (between GUIs) ─────────────────────────────

    public void setPendingSlot(UUID uuid, int slot) {
        pendingSlotSelection.put(uuid, slot);
    }

    public int getPendingSlot(UUID uuid) {
        return pendingSlotSelection.getOrDefault(uuid, -1);
    }

    public void clearPendingSlot(UUID uuid) {
        pendingSlotSelection.remove(uuid);
    }
}
