package dev.sfcore.database;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstrae las diferencias SQL entre SQLite y MySQL para que los consumidores
 * de SFCore no tengan que escribir SQL motor-específico.
 */
public interface SqlDialect {

    /** Identificador corto — "sqlite" o "mysql". */
    String id();

    // ─── Sintaxis de upserts ─────────────────────────────────────────────

    /**
     * Construye un INSERT que reemplaza al colisionar por PK.
     * SQLite: INSERT OR REPLACE INTO ...
     * MySQL : INSERT INTO ... ON DUPLICATE KEY UPDATE col=VALUES(col), ...
     */
    String upsert(String table, String[] keyCols, String[] valueCols);

    /**
     * Construye un INSERT que ignora la fila si ya existe.
     * SQLite: INSERT OR IGNORE INTO ...
     * MySQL : INSERT IGNORE INTO ...
     */
    String insertIgnore(String table, String[] cols);

    // ─── Tipos SQL motor-específicos ─────────────────────────────────────

    /** Tipo para PKs de UUIDs/strings cortos. SQLite: TEXT. MySQL: VARCHAR(36). */
    String uuidType();

    /** Tipo para strings medianos (source, stat_type, etc.). SQLite: TEXT. MySQL: VARCHAR(length). */
    String varchar(int length);

    /** Tipo numérico de precisión doble. SQLite: REAL. MySQL: DOUBLE. */
    String doubleType();

    /** Tipo entero. SQLite: INTEGER. MySQL: INT. */
    String intType();

    /** BLOB grande (inventarios de SFCharacter pueden superar 64 KB). SQLite: BLOB. MySQL: MEDIUMBLOB. */
    String blobType();

    // ─── Conexión ────────────────────────────────────────────────────────

    /** Nombre completo de la clase del driver JDBC. */
    String driverClass();

    /**
     * Construye el JDBC URL para un namespace concreto.
     * SQLite: jdbc:sqlite:<dataFolder>/<directory>/<namespace>.db
     * MySQL : jdbc:mysql://<host>:<port>/<database>?useSSL=false&...
     */
    String jdbcUrl(ConfigurationSection cfg, File dataFolder, String namespace);

    /**
     * Hook invocado una vez al abrir la primera conexión de un pool.
     * SQLite: PRAGMA journal_mode=WAL. MySQL: no-op.
     */
    void onStartup(Connection conn) throws SQLException;

    /**
     * Aplica el prefijo de namespace al nombre de tabla si el motor lo requiere.
     * SQLite usa archivo por namespace, así que no prefija.
     * MySQL comparte schema, así que prefija.
     */
    String tableName(String namespace, String table);
}
