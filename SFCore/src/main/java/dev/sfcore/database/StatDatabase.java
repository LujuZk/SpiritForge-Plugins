package dev.sfcore.database;

import dev.sfcore.api.StatBonus;
import dev.sfcore.api.StatType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class StatDatabase {

    private static final Logger log = Logger.getLogger("SFCore");

    private final SFDatabase sfDatabase;
    private final SqlDialect dialect;
    private final String table;

    private final String sqlSelect;
    private final String sqlUpsert;
    private final String sqlDelete;
    private final String sqlDeleteByPrefix;
    private final String sqlDeleteAll;

    public StatDatabase(SFDatabase sfDatabase) {
        this.sfDatabase = sfDatabase;
        this.dialect = sfDatabase.dialect();
        this.table = sfDatabase.getTableName("stat_bonuses");

        this.sqlSelect = "SELECT source, stat_type, value FROM " + table
                + " WHERE player_uuid = ? AND character_slot = ?";
        this.sqlUpsert = dialect.upsert(
                table,
                new String[]{"player_uuid", "character_slot", "source"},
                new String[]{"stat_type", "value"});
        this.sqlDelete = "DELETE FROM " + table
                + " WHERE player_uuid = ? AND character_slot = ? AND source = ?";
        this.sqlDeleteByPrefix = "DELETE FROM " + table
                + " WHERE player_uuid = ? AND character_slot = ? AND source LIKE ?";
        this.sqlDeleteAll = "DELETE FROM " + table
                + " WHERE player_uuid = ? AND character_slot = ?";

        try {
            initTables();
            log.info("[SFCore] StatDatabase lista (" + dialect.id() + ", tabla '" + table + "')");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize StatDatabase", e);
        }
    }

    private void initTables() throws SQLException {
        String create = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "player_uuid "    + dialect.uuidType()   + " NOT NULL, "
                + "character_slot " + dialect.intType()    + " NOT NULL, "
                + "source "         + dialect.varchar(255) + " NOT NULL, "
                + "stat_type "      + dialect.varchar(64)  + " NOT NULL, "
                + "value "          + dialect.doubleType() + " NOT NULL, "
                + "PRIMARY KEY (player_uuid, character_slot, source)"
                + ")";
        String index = "CREATE INDEX IF NOT EXISTS idx_" + table + "_player ON " + table + "(player_uuid)";
        String resourcesTable = sfDatabase.getTableName("player_resources");
        String createResources = "CREATE TABLE IF NOT EXISTS " + resourcesTable + " ("
                + "player_uuid " + dialect.uuidType() + " NOT NULL PRIMARY KEY, "
                + "mana_current " + dialect.doubleType() + " NOT NULL"
                + ")";

        try (Connection conn = sfDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(create);
            stmt.execute(createResources);
            try {
                stmt.execute(index);
            } catch (SQLException ignored) {
                // MySQL no tiene CREATE INDEX IF NOT EXISTS — si ya existe, ignorar
            }
        }
    }

    public List<StatBonus> loadBonuses(UUID uuid, int slot) {
        List<StatBonus> bonuses = new ArrayList<>();
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String source = rs.getString("source");
                    StatType type = StatType.fromKey(rs.getString("stat_type"));
                    double value = rs.getDouble("value");
                    if (type != null) bonuses.add(new StatBonus(source, type, value));
                }
            }
        } catch (SQLException e) {
            log.warning("[SFCore] Error loading bonuses for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
        return bonuses;
    }

    public void upsertBonus(UUID uuid, int slot, StatBonus bonus) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.setString(3, bonus.source());
            ps.setString(4, bonus.type().getKey());
            ps.setDouble(5, bonus.value());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error upserting bonus for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    public void deleteBonus(UUID uuid, int slot, String source) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDelete)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.setString(3, source);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting bonus for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    public void deleteBySourcePrefix(UUID uuid, int slot, String prefix) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteByPrefix)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.setString(3, prefix + "%");
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting bonuses by prefix for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    public void upsertBonusesBatch(UUID uuid, int slot, List<StatBonus> bonuses) {
        try (Connection conn = sfDatabase.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
                for (StatBonus bonus : bonuses) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, slot);
                    ps.setString(3, bonus.source());
                    ps.setString(4, bonus.type().getKey());
                    ps.setDouble(5, bonus.value());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.warning("[SFCore] Error batch upserting bonuses for " + uuid + " slot " + slot + ": " + e.getMessage());
        }
    }

    public Double loadMana(UUID uuid) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT mana_current FROM player_resources WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("mana_current");
                }
            }
        } catch (SQLException e) {
            log.warning("[SFCore] Error loading mana for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    public void upsertMana(UUID uuid, double manaCurrent) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO player_resources (player_uuid, mana_current) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, manaCurrent);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error saving mana for " + uuid + ": " + e.getMessage());
        }
    }

    public void deleteResources(UUID uuid) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM player_resources WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting resources for " + uuid + ": " + e.getMessage());
        }
    }

    public void close() {
        // No persistent connection to close; each operation uses its own connection.
    }
}
