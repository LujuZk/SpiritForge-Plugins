package dev.skilltree.database;

import dev.sfcore.database.SFDatabase;
import dev.sfcore.database.SqlDialect;
import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.PlayerSkillData;
import dev.skilltree.models.SkillType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final SkillTreePlugin plugin;
    private final SFDatabase sfDatabase;
    private final String skillsTable;
    private final String nodesTable;

    private final String sqlSelectSkills;
    private final String sqlSelectNodes;
    private final String sqlUpsertSkill;
    private final String sqlDeleteNodesByPlayer;
    private final String sqlInsertNode;
    private final String sqlDeleteSkillsByPlayer;
    private final String sqlDeleteNodesByPlayerAndSkill;

    public DatabaseManager(SkillTreePlugin plugin, SFDatabase sfDatabase) {
        this.plugin = plugin;
        this.sfDatabase = sfDatabase;
        SqlDialect dialect = sfDatabase.dialect();
        this.skillsTable = sfDatabase.getTableName("player_skills");
        this.nodesTable  = sfDatabase.getTableName("player_nodes");

        this.sqlSelectSkills = "SELECT skill, level, xp, points FROM " + skillsTable
                + " WHERE uuid = ? AND character_slot = ?";
        this.sqlSelectNodes  = "SELECT skill, node_id FROM " + nodesTable
                + " WHERE uuid = ? AND character_slot = ?";
        this.sqlUpsertSkill  = dialect.upsert(
                skillsTable,
                new String[]{"uuid", "character_slot", "skill"},
                new String[]{"level", "xp", "points"});
        this.sqlDeleteNodesByPlayer        = "DELETE FROM " + nodesTable
                + " WHERE uuid = ? AND character_slot = ?";
        this.sqlInsertNode                 = "INSERT INTO " + nodesTable
                + " (uuid, character_slot, skill, node_id) VALUES (?, ?, ?, ?)";
        this.sqlDeleteSkillsByPlayer       = "DELETE FROM " + skillsTable
                + " WHERE uuid = ? AND character_slot = ?";
        this.sqlDeleteNodesByPlayerAndSkill = "DELETE FROM " + nodesTable
                + " WHERE uuid = ? AND character_slot = ? AND skill = ?";

        String createSkills = "CREATE TABLE IF NOT EXISTS " + skillsTable + " ("
                + "uuid "           + dialect.uuidType()   + " NOT NULL, "
                + "character_slot " + dialect.intType()    + " NOT NULL, "
                + "skill "          + dialect.varchar(64)  + " NOT NULL, "
                + "level "          + dialect.intType()    + " NOT NULL DEFAULT 1, "
                + "xp "             + dialect.doubleType() + " NOT NULL DEFAULT 0, "
                + "points "         + dialect.intType()    + " NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (uuid, character_slot, skill)"
                + ")";
        String createNodes = "CREATE TABLE IF NOT EXISTS " + nodesTable + " ("
                + "uuid "           + dialect.uuidType()   + " NOT NULL, "
                + "character_slot " + dialect.intType()    + " NOT NULL, "
                + "skill "          + dialect.varchar(64)  + " NOT NULL, "
                + "node_id "        + dialect.varchar(128) + " NOT NULL, "
                + "PRIMARY KEY (uuid, character_slot, skill, node_id)"
                + ")";

        try (Connection conn = sfDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSkills);
            stmt.execute(createNodes);
            plugin.getLogger().info("Base de datos lista (" + dialect.id() + ", tablas '" + skillsTable + "', '" + nodesTable + "')");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SFSkilltree DatabaseManager", e);
        }
    }

    /** Carga los datos de un jugador para un slot de personaje. */
    public PlayerSkillData loadPlayer(UUID uuid, int slot) {
        PlayerSkillData data = new PlayerSkillData(uuid);

        try (Connection conn = sfDatabase.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlSelectSkills)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, slot);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        SkillType type = SkillType.fromKey(rs.getString("skill"));
                        if (type != null) {
                            data.setLevel(type, rs.getInt("level"));
                            data.setXP(type, rs.getDouble("xp"));
                            data.setAvailablePoints(type, rs.getInt("points"));
                        }
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlSelectNodes)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, slot);
                try (ResultSet rs = ps.executeQuery()) {
                    Map<SkillType, Set<String>> nodeMap = new HashMap<>();
                    while (rs.next()) {
                        SkillType type = SkillType.fromKey(rs.getString("skill"));
                        if (type != null) {
                            nodeMap.computeIfAbsent(type, k -> new HashSet<>())
                                    .add(rs.getString("node_id"));
                        }
                    }
                    nodeMap.forEach(data::setUnlockedNodes);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error cargando datos de " + uuid + " (slot " + slot + ")", e);
        }

        return data;
    }

    /** Guarda todos los skills de un jugador en el slot indicado. */
    public void savePlayer(PlayerSkillData data, int slot) {
        try (Connection conn = sfDatabase.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlUpsertSkill)) {
                for (SkillType type : SkillType.values()) {
                    ps.setString(1, data.getPlayerUUID().toString());
                    ps.setInt(2, slot);
                    ps.setString(3, type.getKey());
                    ps.setInt(4, data.getLevel(type));
                    ps.setDouble(5, data.getXP(type));
                    ps.setInt(6, data.getAvailablePoints(type));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement del = conn.prepareStatement(sqlDeleteNodesByPlayer)) {
                del.setString(1, data.getPlayerUUID().toString());
                del.setInt(2, slot);
                del.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlInsertNode)) {
                for (SkillType type : SkillType.values()) {
                    for (String nodeId : data.getUnlockedNodes(type)) {
                        ps.setString(1, data.getPlayerUUID().toString());
                        ps.setInt(2, slot);
                        ps.setString(3, type.getKey());
                        ps.setString(4, nodeId);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error guardando datos de " + data.getPlayerUUID() + " (slot " + slot + ")", e);
        }
    }

    /** Resetea todos los datos de un jugador en el slot indicado. */
    public void resetPlayer(UUID uuid, int slot) {
        try (Connection conn = sfDatabase.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteSkillsByPlayer)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, slot);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteNodesByPlayer)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, slot);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error reseteando datos de " + uuid + " (slot " + slot + ")", e);
        }
    }

    /** Resetea solo los nodos de un skill (para el reset del árbol) en el slot indicado. */
    public void resetNodes(UUID uuid, int slot, SkillType skill) {
        try (Connection conn = sfDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteNodesByPlayerAndSkill)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slot);
            ps.setString(3, skill.getKey());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error reseteando nodos de " + uuid + " (slot " + slot + ")", e);
        }
    }
}
