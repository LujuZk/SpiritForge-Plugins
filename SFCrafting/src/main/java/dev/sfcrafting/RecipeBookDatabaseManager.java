package dev.sfcrafting;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
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
    private Connection connection;

    public RecipeBookDatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "crafting.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_discoveries (
                        uuid        TEXT NOT NULL,
                        material_id TEXT NOT NULL,
                        PRIMARY KEY (uuid, material_id)
                    )
                """);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo inicializar la base de datos del recetario", e);
        }
    }

    public Set<String> loadDiscoveries(UUID uuid) {
        Set<String> discoveries = new HashSet<>();
        if (connection == null) {
            return discoveries;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT material_id FROM player_discoveries WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    discoveries.add(rs.getString("material_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al cargar discoveries para " + uuid, e);
        }
        return discoveries;
    }

    public void saveDiscovery(UUID uuid, String materialId) {
        if (connection == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO player_discoveries (uuid, material_id) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, materialId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error al guardar discovery " + materialId + " para " + uuid, e);
            }
        });
    }

    public void saveDiscoveriesBatch(UUID uuid, Set<String> materialIds) {
        if (connection == null || materialIds.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_discoveries (uuid, material_id) VALUES (?, ?)")) {
            for (String materialId : materialIds) {
                ps.setString(1, uuid.toString());
                ps.setString(2, materialId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al guardar discoveries batch para " + uuid, e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error al cerrar conexion de recetario", e);
            }
        }
    }
}
