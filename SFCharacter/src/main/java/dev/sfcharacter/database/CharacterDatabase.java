package dev.sfcharacter.database;

import dev.sfcharacter.models.CharacterClass;
import dev.sfcharacter.models.CharacterData;
import dev.sfcharacter.models.LocationData;
import dev.sfcore.database.SFDatabase;
import dev.sfcore.database.SqlDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class CharacterDatabase {

    private static final Logger log = Logger.getLogger("SFCharacter");

    private final SFDatabase sfDatabase;
    private final String tCharacters;
    private final String tActive;
    private final String tInventories;
    private final String tLocations;

    private final String sqlSelectCharacters;
    private final String sqlUpsertCharacter;
    private final String sqlDeleteCharacter;
    private final String sqlSelectActive;
    private final String sqlUpsertActive;
    private final String sqlDeleteActive;
    private final String sqlUpsertInventory;
    private final String sqlSelectInventory;
    private final String sqlDeleteInventory;
    private final String sqlUpsertLocation;
    private final String sqlSelectLocation;
    private final String sqlDeleteLocation;

    public CharacterDatabase(SFDatabase sfDatabase) {
        this.sfDatabase = sfDatabase;
        SqlDialect dialect = sfDatabase.dialect();

        this.tCharacters  = sfDatabase.getTableName("characters");
        this.tActive      = sfDatabase.getTableName("active_character");
        this.tInventories = sfDatabase.getTableName("character_inventories");
        this.tLocations   = sfDatabase.getTableName("character_locations");

        this.sqlSelectCharacters = "SELECT slot, class_name, display_name, created_at FROM "
                + tCharacters + " WHERE uuid = ? ORDER BY slot";
        this.sqlUpsertCharacter = dialect.upsert(
                tCharacters,
                new String[]{"uuid", "slot"},
                new String[]{"class_name", "display_name", "created_at"});
        this.sqlDeleteCharacter = "DELETE FROM " + tCharacters + " WHERE uuid = ? AND slot = ?";

        this.sqlSelectActive = "SELECT slot FROM " + tActive + " WHERE uuid = ?";
        this.sqlUpsertActive = dialect.upsert(
                tActive,
                new String[]{"uuid"},
                new String[]{"slot"});
        this.sqlDeleteActive = "DELETE FROM " + tActive + " WHERE uuid = ?";

        this.sqlUpsertInventory = dialect.upsert(
                tInventories,
                new String[]{"uuid", "slot"},
                new String[]{"inventory", "armor", "offhand"});
        this.sqlSelectInventory = "SELECT inventory, armor, offhand FROM "
                + tInventories + " WHERE uuid = ? AND slot = ?";
        this.sqlDeleteInventory = "DELETE FROM " + tInventories + " WHERE uuid = ? AND slot = ?";

        this.sqlUpsertLocation = dialect.upsert(
                tLocations,
                new String[]{"uuid", "slot"},
                new String[]{"world", "x", "y", "z", "yaw", "pitch"});
        this.sqlSelectLocation = "SELECT world, x, y, z, yaw, pitch FROM "
                + tLocations + " WHERE uuid = ? AND slot = ?";
        this.sqlDeleteLocation = "DELETE FROM " + tLocations + " WHERE uuid = ? AND slot = ?";

        try {
            initTables(dialect);
            log.info("[SFCharacter] Database lista (" + dialect.id() + ")");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize CharacterDatabase", e);
        }
    }

    private void initTables(SqlDialect d) throws SQLException {
        String createCharacters = "CREATE TABLE IF NOT EXISTS " + tCharacters + " ("
                + "uuid "         + d.uuidType()   + " NOT NULL, "
                + "slot "         + d.intType()    + " NOT NULL, "
                + "class_name "   + d.varchar(64)  + " NOT NULL, "
                + "display_name " + d.varchar(64)  + " NOT NULL, "
                + "created_at "   + d.varchar(32)  + " NOT NULL, "
                + "PRIMARY KEY (uuid, slot)"
                + ")";
        String createActive = "CREATE TABLE IF NOT EXISTS " + tActive + " ("
                + "uuid " + d.uuidType() + " PRIMARY KEY, "
                + "slot " + d.intType()  + " NOT NULL"
                + ")";
        String createInventories = "CREATE TABLE IF NOT EXISTS " + tInventories + " ("
                + "uuid "      + d.uuidType() + " NOT NULL, "
                + "slot "      + d.intType()  + " NOT NULL, "
                + "inventory " + d.blobType() + ", "
                + "armor "     + d.blobType() + ", "
                + "offhand "   + d.blobType() + ", "
                + "PRIMARY KEY (uuid, slot)"
                + ")";
        String createLocations = "CREATE TABLE IF NOT EXISTS " + tLocations + " ("
                + "uuid "  + d.uuidType()   + " NOT NULL, "
                + "slot "  + d.intType()    + " NOT NULL, "
                + "world " + d.varchar(64) + ", "
                + "x "     + d.doubleType() + ", "
                + "y "     + d.doubleType() + ", "
                + "z "     + d.doubleType() + ", "
                + "yaw "   + d.doubleType() + ", "
                + "pitch " + d.doubleType() + ", "
                + "PRIMARY KEY (uuid, slot)"
                + ")";

        try (Connection conn = sfDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createCharacters);
            stmt.execute(createActive);
            stmt.execute(createInventories);
            stmt.execute(createLocations);
        }
    }

    // ─── Characters ─────────────────────────────────────────────────────────

    public List<CharacterData> loadCharacters(UUID uuid) {
        List<CharacterData> list = new ArrayList<>();
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelectCharacters)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error loading characters for " + uuid + ": " + e.getMessage());
        }
        return list;
    }

    public void saveCharacter(CharacterData data) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpsertCharacter)) {
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
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteCharacter)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error deleting character for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
        deleteInventory(uuid, slot);
        deleteLocation(uuid, slot);
    }

    // ─── Active Character ───────────────────────────────────────────────────

    public int loadActiveSlot(UUID uuid) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelectActive)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("slot");
            }
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error loading active slot for " + uuid + ": " + e.getMessage());
        }
        return -1;
    }

    public void saveActiveSlot(UUID uuid, int slot) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpsertActive)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error saving active slot for " + uuid + ": " + e.getMessage());
        }
    }

    public void clearActiveSlot(UUID uuid) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteActive)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error clearing active slot for " + uuid + ": " + e.getMessage());
        }
    }

    // ─── Character Inventories ──────────────────────────────────────────────

    public void saveInventory(UUID uuid, int slot, byte[] inventory, byte[] armor, byte[] offhand) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpsertInventory)) {
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
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelectInventory)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new byte[][]{
                            rs.getBytes("inventory"),
                            rs.getBytes("armor"),
                            rs.getBytes("offhand")
                    };
                }
            }
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error loading inventory for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
        return null;
    }

    public void deleteInventory(UUID uuid, int slot) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteInventory)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error deleting inventory for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    // ─── Character Locations ────────────────────────────────────────────────

    public void saveLocation(UUID uuid, int slot, String world, double x, double y, double z, float yaw, float pitch) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpsertLocation)) {
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
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelectLocation)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error loading location for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
        return null;
    }

    public void deleteLocation(UUID uuid, int slot) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteLocation)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCharacter] Error deleting location for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }
}
