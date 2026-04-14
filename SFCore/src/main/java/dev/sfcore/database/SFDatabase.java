package dev.sfcore.database;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handle que los plugins consumidores reciben desde SFCoreAPI para leer/escribir
 * en su "rebanada" de la base de datos compartida. Encapsula el pool Hikari, el
 * dialecto activo y el namespace del consumidor.
 *
 * Los consumidores NO deben cerrar el datasource — lo hace SFCore en onDisable().
 */
public final class SFDatabase {

    private final String namespace;
    private final HikariDataSource dataSource;
    private final SqlDialect dialect;

    SFDatabase(String namespace, HikariDataSource dataSource, SqlDialect dialect) {
        this.namespace = namespace;
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    public String namespace() {
        return namespace;
    }

    public SqlDialect dialect() {
        return dialect;
    }

    /**
     * Pide una conexión al pool. Usar SIEMPRE en try-with-resources:
     * <pre>
     *   try (Connection conn = sfDatabase.getConnection();
     *        PreparedStatement ps = conn.prepareStatement(SQL)) { ... }
     * </pre>
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Traduce un nombre de tabla lógico al nombre real para el motor activo.
     * En SQLite devuelve el nombre tal cual (cada namespace vive en su propio .db);
     * en MySQL lo prefija con el namespace para evitar colisiones en el schema compartido.
     */
    public String getTableName(String table) {
        return dialect.tableName(namespace, table);
    }

    void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
