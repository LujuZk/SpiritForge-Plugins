package dev.sfcharacter.database;

import dev.sfcharacter.models.CharacterClass;
import dev.sfcharacter.models.CharacterData;
import dev.sfcharacter.models.LocationData;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class CharacterDatabase {

    private final Connection connection;
    private static final Logger log = Logger.getLogger("SFCharacter");

    public CharacterDatabase(File dataFolder, String fileName) {
        dataFolder.mkdirs();
        File dbFile = new File(dataFolder, fileName);
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA journal_mode=WAL;");
            }
            initTables();
            log.info("[SFCharacter] Database initialized at " + dbFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize CharacterDatabase", e);
        }
    }

    private void initTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS characters (
                    uuid         TEXT    NOT NULL,
                    slot         INTEGER NOT NULL,
                    class_name   TEXT    NOT NULL,
                    display_name TEXT    NOT NULL,
                    created_at   TEXT    NOT NULL,
                    PRIMARY KEY (uuid, slot)
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS active_character (
                    uuid TEXT PRIMARY KEY,
                    slot INTEGER NOT NULL
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS character_inventories (
                    uuid      TEXT    NOT NULL,
                    slot      INTEGER NOT NULL,
                    inventory BLOB,
                    armor     BLOB,
                    offhand   BLOB,
                    PRIMARY KEY (uuid, slot)
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS character_locations (
                    uuid  TEXT    NOT NULL,
                    slot  INTEGER NOT NULL,
                    world TEXT,
                    x     DOUBLE,
                    y     DOUBLE,
                    z     DOUBLE,
                    yaw   REAL,
                    pitch REAL,
                    PRIMARY KEY (uuid, slot)
                )
                """);
        }
    }

    // ─── Characters ─────────────────────────────────────────────────────────

    public List<CharacterData> loadCharacters(UUID uuid) {
        List<CharacterData> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT slot, class_name, display_name, created_at FROM characters WHERE uuid = ? ORDER BY slot")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CharacterClass cc = CharacterClass.fromName(rs.getString("class_name"));
                if (cc == null) continue;
                list.add(new CharacterData(
                        uuid,
                        rs.getInt("slot"),
                        cc,
                        rs.getString("display_name"),
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error loading characters for " + uuid + ": " + e.getMessage());
        }
        return list;
    }

    public void saveCharacter(CharacterData data) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO characters (uuid, slot, class_name, display_name, created_at) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, data.playerUuid().toString());
            ps.setInt(2, data.slot());
            ps.setString(3, data.characterClass().name());
            ps.setString(4, data.displayName());
            ps.setString(5, data.createdAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error saving character for " + data.playerUuid() + ": " + e.getMessage());
        }
    }

    public void deleteCharacter(UUID uuid, int slot) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM characters WHERE uuid = ? AND slot = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error deleting character for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
        // Cascade to inventory and location
        deleteInventory(uuid, slot);
        deleteLocation(uuid, slot);
    }

    // ─── Active Character ───────────────────────────────────────────────────

    public int loadActiveSlot(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT slot FROM active_character WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("slot");
            }
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error loading active slot for " + uuid + ": " + e.getMessage());
        }
        return -1;
    }

    public void saveActiveSlot(UUID uuid, int slot) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO active_character (uuid, slot) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error saving active slot for " + uuid + ": " + e.getMessage());
        }
    }

    public void clearActiveSlot(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM active_character WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error clearing active slot for " + uuid + ": " + e.getMessage());
        }
    }

    // ─── Character Inventories ──────────────────────────────────────────────

    public void saveInventory(UUID uuid, int slot, byte[] inventory, byte[] armor, byte[] offhand) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO character_inventories (uuid, slot, inventory, armor, offhand) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.setBytes(3, inventory);
            ps.setBytes(4, armor);
            ps.setBytes(5, offhand);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error saving inventory for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    /**
     * Returns [inventory, armor, offhand] byte arrays, or null if no saved data.
     */
    public byte[][] loadInventory(UUID uuid, int slot) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT inventory, armor, offhand FROM character_inventories WHERE uuid = ? AND slot = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new byte[][]{
                        rs.getBytes("inventory"),
                        rs.getBytes("armor"),
                        rs.getBytes("offhand")
                };
            }
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error loading inventory for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
        return null;
    }

    public void deleteInventory(UUID uuid, int slot) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM character_inventories WHERE uuid = ? AND slot = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error deleting inventory for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    // ─── Character Locations ────────────────────────────────────────────────

    public void saveLocation(UUID uuid, int slot, String world, double x, double y, double z, float yaw, float pitch) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO character_locations (uuid, slot, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.setString(3, world);
            ps.setDouble(4, x);
            ps.setDouble(5, y);
            ps.setDouble(6, z);
            ps.setFloat(7, yaw);
            ps.setFloat(8, pitch);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error saving location for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    public LocationData loadLocation(UUID uuid, int slot) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT world, x, y, z, yaw, pitch FROM character_locations WHERE uuid = ? AND slot = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new LocationData(
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
            }
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error loading location for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
        return null;
    }

    public void deleteLocation(UUID uuid, int slot) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM character_locations WHERE uuid = ? AND slot = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error deleting location for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error closing database: " + e.getMessage());
        }
    }
}
