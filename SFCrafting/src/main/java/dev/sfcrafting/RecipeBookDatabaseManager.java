package dev.sfcrafting;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.database.SFDatabase;
import dev.sfcore.database.SqlDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class RecipeBookDatabaseManager {

    private final Plugin plugin;
    private final SFDatabase sfDatabase;
    private final String table;

    private final String sqlSelect;
    private final String sqlInsertIgnore;

    public RecipeBookDatabaseManager(Plugin plugin, SFDatabase sfDatabase) {
        this.plugin = plugin;
        this.sfDatabase = sfDatabase;
        SqlDialect dialect = sfDatabase.dialect();
        this.table = sfDatabase.getTableName("player_discoveries");

        this.sqlSelect = "SELECT material_id FROM " + table
                + " WHERE uuid = ? AND character_slot = ?";
        this.sqlInsertIgnore = dialect.insertIgnore(table,
                new String[]{"uuid", "character_slot", "material_id"});

        String create = "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid "           + dialect.uuidType()   + " NOT NULL, "
                + "character_slot " + dialect.intType()    + " NOT NULL, "
                + "material_id "    + dialect.varchar(128) + " NOT NULL, "
                + "PRIMARY KEY (uuid, character_slot, material_id)"
                + ")";

        try (Connection conn = sfDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(create);
            plugin.getLogger().info("Recetario DB lista (" + dialect.id() + ", tabla '" + table + "')");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SFCrafting recipe-book database", e);
        }
    }

    public Set<String> loadDiscoveries(UUID uuid, int slot) {
        Set<String> discoveries = new HashSet<>();
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    discoveries.add(rs.getString("material_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al cargar discoveries para " + uuid + " (slot " + slot + ")", e);
        }
        return discoveries;
    }

    public void saveDiscovery(UUID uuid, int slot, String materialId) {
        SFCoreAPI.get().getAsyncExecutor().runAsync(() -> {
            try (Connection conn = sfDatabase.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlInsertIgnore)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, slot);
                ps.setString(3, materialId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error al guardar discovery " + materialId + " para " + uuid + " (slot " + slot + ")", e);
            }
        });
    }

    public void saveDiscoveriesBatch(UUID uuid, int slot, Set<String> materialIds) {
        if (materialIds.isEmpty()) {
            return;
        }
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlInsertIgnore)) {
            for (String materialId : materialIds) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, slot);
                ps.setString(3, materialId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al guardar discoveries batch para " + uuid + " (slot " + slot + ")", e);
        }
    }
}
