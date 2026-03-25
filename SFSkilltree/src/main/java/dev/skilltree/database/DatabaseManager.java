package dev.skilltree.database;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.PlayerSkillData;
import dev.skilltree.models.SkillType;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final SkillTreePlugin plugin;
    private Connection connection;

    public DatabaseManager(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            String dbFile = plugin.getConfig().getString("database.file", "skilltree.db");
            File file = new File(plugin.getDataFolder(), dbFile);
            plugin.getDataFolder().mkdirs();

            String url = "jdbc:sqlite:" + file.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            createTables();
            plugin.getLogger().info("Base de datos SQLite conectada.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al conectar a SQLite", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_skills (
                    uuid        TEXT NOT NULL,
                    skill       TEXT NOT NULL,
                    level       INTEGER DEFAULT 1,
                    xp          REAL DEFAULT 0.0,
                    points      INTEGER DEFAULT 0,
                    PRIMARY KEY (uuid, skill)
                );
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_nodes (
                    uuid        TEXT NOT NULL,
                    skill       TEXT NOT NULL,
                    node_id     TEXT NOT NULL,
                    PRIMARY KEY (uuid, skill, node_id)
                );
                """);
        }
    }

    /** Carga los datos de un jugador. Si no existen, retorna datos nuevos (nivel 1). */
    public PlayerSkillData loadPlayer(UUID uuid) {
        PlayerSkillData data = new PlayerSkillData(uuid);

        // Cargar skills (nivel, xp, puntos)
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT skill, level, xp, points FROM player_skills WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SkillType type = SkillType.fromKey(rs.getString("skill"));
                if (type != null) {
                    data.setLevel(type, rs.getInt("level"));
                    data.setXP(type, rs.getDouble("xp"));
                    data.setAvailablePoints(type, rs.getInt("points"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error cargando skills de " + uuid, e);
        }

        // Cargar nodos desbloqueados
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT skill, node_id FROM player_nodes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            Map<SkillType, java.util.Set<String>> nodeMap = new java.util.HashMap<>();
            while (rs.next()) {
                SkillType type = SkillType.fromKey(rs.getString("skill"));
                if (type != null) {
                    nodeMap.computeIfAbsent(type, k -> new java.util.HashSet<>())
                            .add(rs.getString("node_id"));
                }
            }
            nodeMap.forEach(data::setUnlockedNodes);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error cargando nodos de " + uuid, e);
        }

        return data;
    }

    /** Guarda todos los skills de un jugador en la base de datos. */
    public void savePlayer(PlayerSkillData data) {
        // Guardar skills
        String sqlSkills = """
            INSERT INTO player_skills (uuid, skill, level, xp, points)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(uuid, skill) DO UPDATE SET
                level = excluded.level, xp = excluded.xp, points = excluded.points
            """;
        try (PreparedStatement ps = connection.prepareStatement(sqlSkills)) {
            for (SkillType type : SkillType.values()) {
                ps.setString(1, data.getPlayerUUID().toString());
                ps.setString(2, type.getKey());
                ps.setInt(3, data.getLevel(type));
                ps.setDouble(4, data.getXP(type));
                ps.setInt(5, data.getAvailablePoints(type));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error guardando skills de " + data.getPlayerUUID(), e);
        }

        // Guardar nodos: borrar los del jugador y reinsertar
        try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM player_nodes WHERE uuid = ?")) {
            del.setString(1, data.getPlayerUUID().toString());
            del.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error borrando nodos de " + data.getPlayerUUID(), e);
        }

        String sqlNodes = "INSERT INTO player_nodes (uuid, skill, node_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sqlNodes)) {
            for (SkillType type : SkillType.values()) {
                for (String nodeId : data.getUnlockedNodes(type)) {
                    ps.setString(1, data.getPlayerUUID().toString());
                    ps.setString(2, type.getKey());
                    ps.setString(3, nodeId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error guardando nodos de " + data.getPlayerUUID(), e);
        }
    }

    /** Resetea todos los datos de un jugador. */
    public void resetPlayer(UUID uuid) {
        for (String table : new String[]{"player_skills", "player_nodes"}) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM " + table + " WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error reseteando " + table + " de " + uuid, e);
            }
        }
    }

    /** Resetea solo los nodos de un skill (para el reset del árbol). */
    public void resetNodes(UUID uuid, SkillType skill) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM player_nodes WHERE uuid = ? AND skill = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, skill.getKey());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error reseteando nodos de " + uuid, e);
        }
    }

    private void migrateDatabase() throws SQLException {
        // Agregar columna points si no existe
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE player_skills ADD COLUMN points INTEGER DEFAULT 0");
        } catch (SQLException ignored) {
            // Si ya existe, SQLite tira error — lo ignoramos
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error cerrando conexión SQLite", e);
        }
    }
}