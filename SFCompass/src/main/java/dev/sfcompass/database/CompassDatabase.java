package dev.sfcompass.database;

import dev.sfcore.database.SFDatabase;
import dev.sfcore.database.SqlDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

public class CompassDatabase {

    private static final Logger log = Logger.getLogger("SFCompass");

    private final SFDatabase sfDatabase;
    private final String table;

    private final String sqlSelect;
    private final String sqlUpsert;

    public CompassDatabase(SFDatabase sfDatabase) {
        this.sfDatabase = sfDatabase;
        SqlDialect dialect = sfDatabase.dialect();
        this.table = sfDatabase.getTableName("compass_levels");

        this.sqlSelect = "SELECT level FROM " + table + " WHERE player_uuid = ?";
        this.sqlUpsert = dialect.upsert(
                table,
                new String[]{"player_uuid"},
                new String[]{"level"});

        String create = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "player_uuid " + dialect.uuidType() + " PRIMARY KEY, "
                + "level "       + dialect.intType() + " NOT NULL DEFAULT 1"
                + ")";

        try (Connection conn = sfDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(create);
            log.info("[SFCompass] Database lista (" + dialect.id() + ", tabla '" + table + "')");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize CompassDatabase", e);
        }
    }

    public int loadLevel(UUID uuid) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("level");
            }
        } catch (SQLException e) {
            log.warning("[SFCompass] Error loading level for " + uuid + ": " + e.getMessage());
        }
        return -1;
    }

    public void saveLevel(UUID uuid, int level) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[SFCompass] Error saving level for " + uuid + ": " + e.getMessage());
        }
    }
}
