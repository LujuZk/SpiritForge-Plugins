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

        this.sqlSelect = "SELECT source, stat_type, value FROM " + table + " WHERE player_uuid = ?";
        this.sqlUpsert = dialect.upsert(
                table,
                new String[]{"player_uuid", "source"},
                new String[]{"stat_type", "value"});
        this.sqlDelete = "DELETE FROM " + table + " WHERE player_uuid = ? AND source = ?";
        this.sqlDeleteByPrefix = "DELETE FROM " + table + " WHERE player_uuid = ? AND source LIKE ?";
        this.sqlDeleteAll = "DELETE FROM " + table + " WHERE player_uuid = ?";

        try {
            initTables();
            log.info("[SFCore] StatDatabase lista (" + dialect.id() + ", tabla '" + table + "')");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize StatDatabase", e);
        }
    }

    private void initTables() throws SQLException {
        String create = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "player_uuid " + dialect.uuidType() + " NOT NULL, "
                + "source "      + dialect.varchar(255) + " NOT NULL, "
                + "stat_type "   + dialect.varchar(64) + " NOT NULL, "
                + "value "       + dialect.doubleType() + " NOT NULL, "
                + "PRIMARY KEY (player_uuid, source)"
                + ")";
        String index = "CREATE INDEX IF NOT EXISTS idx_" + table + "_player ON " + table + "(player_uuid)";

        try (Connection conn = sfDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(create);
            try {
                stmt.execute(index);
            } catch (SQLException ignored) {
                // MySQL no tiene CREATE INDEX IF NOT EXISTS — si ya existe, ignorar
            }
        }
    }

    public List<StatBonus> loadBonuses(UUID uuid) {
        List<StatBonus> bonuses = new ArrayList<>();
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String source = rs.getString("source");
                    StatType type = StatType.fromKey(rs.getString("stat_type"));
                    double value = rs.getDouble("value");
                    if (type != null) bonuses.add(new StatBonus(source, type, value));
                }
            }
        } catch (SQLException e) {
            log.warning("[SFCore] Error loading bonuses for " + uuid + ": " + e.getMessage());
        }
        return bonuses;
    }

    public void upsertBonus(UUID uuid, StatBonus bonus) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, bonus.source());
            ps.setString(3, bonus.type().getKey());
            ps.setDouble(4, bonus.value());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error upserting bonus for " + uuid + ": " + e.getMessage());
        }
    }

    public void deleteBonus(UUID uuid, String source) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDelete)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, source);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting bonus for " + uuid + ": " + e.getMessage());
        }
    }

    public void deleteBySourcePrefix(UUID uuid, String prefix) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteByPrefix)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, prefix + "%");
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting bonuses by prefix for " + uuid + ": " + e.getMessage());
        }
    }

    public void deleteAll(UUID uuid) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteAll)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting all bonuses for " + uuid + ": " + e.getMessage());
        }
    }
}
