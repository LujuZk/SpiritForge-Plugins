package dev.sfcore.database;

import dev.sfcore.api.StatBonus;
import dev.sfcore.api.StatType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class StatDatabase {

    private final Connection connection;
    private static final Logger log = Logger.getLogger("SFCore");

    public StatDatabase(File dataFolder) {
        dataFolder.mkdirs();
        File dbFile = new File(dataFolder, "stats.db");
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA journal_mode=WAL;");
            }
            initTables();
            log.info("[SFCore] StatDatabase initialized at " + dbFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize StatDatabase", e);
        }
    }

    private void initTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stat_bonuses (
                    player_uuid TEXT NOT NULL,
                    source      TEXT NOT NULL,
                    stat_type   TEXT NOT NULL,
                    value       REAL NOT NULL,
                    PRIMARY KEY (player_uuid, source)
                )
                """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_player ON stat_bonuses(player_uuid)
                """);
        }
    }

    public List<StatBonus> loadBonuses(UUID uuid) {
        List<StatBonus> bonuses = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT source, stat_type, value FROM stat_bonuses WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String source = rs.getString("source");
                StatType type = StatType.fromKey(rs.getString("stat_type"));
                double value = rs.getDouble("value");
                if (type != null) bonuses.add(new StatBonus(source, type, value));
            }
        } catch (SQLException e) {
            log.warning("[SFCore] Error loading bonuses for " + uuid + ": " + e.getMessage());
        }
        return bonuses;
    }

    public void upsertBonus(UUID uuid, StatBonus bonus) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO stat_bonuses (player_uuid, source, stat_type, value) VALUES (?, ?, ?, ?)")) {
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
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM stat_bonuses WHERE player_uuid = ? AND source = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, source);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting bonus for " + uuid + ": " + e.getMessage());
        }
    }

    public void deleteBySourcePrefix(UUID uuid, String prefix) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM stat_bonuses WHERE player_uuid = ? AND source LIKE ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, prefix + "%");
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting bonuses by prefix for " + uuid + ": " + e.getMessage());
        }
    }

    public void deleteAll(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM stat_bonuses WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCore] Error deleting all bonuses for " + uuid + ": " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            log.warning("[SFCore] Error closing database: " + e.getMessage());
        }
    }
}
