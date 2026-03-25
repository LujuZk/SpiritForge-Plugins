package dev.sfcompass.database;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

public class CompassDatabase {

    private final Connection connection;
    private static final Logger log = Logger.getLogger("SFCompass");

    public CompassDatabase(File dataFolder, String fileName) {
        dataFolder.mkdirs();
        File dbFile = new File(dataFolder, fileName);
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA journal_mode=WAL;");
            }
            initTables();
            log.info("[SFCompass] Database initialized at " + dbFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize CompassDatabase", e);
        }
    }

    private void initTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS compass_levels (
                    player_uuid TEXT PRIMARY KEY,
                    level       INTEGER DEFAULT 1
                )
                """);
        }
    }

    public int loadLevel(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT level FROM compass_levels WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("level");
            }
        } catch (SQLException e) {
            log.warning("[SFCompass] Error loading level for " + uuid + ": " + e.getMessage());
        }
        return -1;
    }

    public void saveLevel(UUID uuid, int level) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO compass_levels (player_uuid, level) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCompass] Error saving level for " + uuid + ": " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            log.warning("[SFCompass] Error closing database: " + e.getMessage());
        }
    }
}
