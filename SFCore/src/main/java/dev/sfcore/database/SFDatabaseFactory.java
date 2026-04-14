package dev.sfcore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Construye y cachea instancias de {@link SFDatabase} por namespace.
 *
 * En SQLite se crea un pool por archivo .db (uno por namespace).
 * En MySQL se podría compartir un único pool para todos los namespaces, pero
 * por simplicidad y aislamiento de métricas también se crea uno por namespace —
 * con pool-size bajo por defecto.
 */
public final class SFDatabaseFactory {

    private static final Logger log = Logger.getLogger("SFCore");

    private final ConfigurationSection config;
    private final File dataFolder;
    private final SqlDialect dialect;
    private final Map<String, SFDatabase> cache = new HashMap<>();

    public SFDatabaseFactory(ConfigurationSection databaseConfig, File dataFolder) {
        this.config = databaseConfig;
        this.dataFolder = dataFolder;
        String type = databaseConfig.getString("type", "sqlite").toLowerCase();
        this.dialect = switch (type) {
            case "mysql" -> new MysqlDialect();
            case "sqlite" -> new SqliteDialect();
            default -> throw new IllegalArgumentException(
                    "Tipo de database desconocido en config.yml: '" + type + "' (usar 'sqlite' o 'mysql')");
        };
        log.info("[SFCore] Database dialect: " + dialect.id());
    }

    public SqlDialect dialect() {
        return dialect;
    }

    /**
     * Devuelve (y cachea) una SFDatabase para el namespace dado.
     * Llamadas subsiguientes con el mismo namespace retornan la misma instancia.
     */
    public synchronized SFDatabase get(String namespace) {
        SFDatabase cached = cache.get(namespace);
        if (cached != null) return cached;

        HikariConfig hc = new HikariConfig();
        hc.setDriverClassName(dialect.driverClass());
        hc.setJdbcUrl(dialect.jdbcUrl(config, dataFolder, namespace));
        hc.setPoolName("SFCore-" + dialect.id() + "-" + namespace);

        if ("mysql".equals(dialect.id())) {
            hc.setUsername(config.getString("mysql.username", "sfuser"));
            hc.setPassword(config.getString("mysql.password", ""));
            hc.setMaximumPoolSize(config.getInt("mysql.pool-size", 10));
            hc.setMinimumIdle(2);
            hc.setConnectionTimeout(5000);
            hc.setIdleTimeout(300_000);
            hc.setMaxLifetime(600_000);
            hc.addDataSourceProperty("cachePrepStmts", "true");
            hc.addDataSourceProperty("prepStmtCacheSize", "250");
            hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            // SQLite: pool pequeño — SQLite serializa internamente igualmente.
            hc.setMaximumPoolSize(4);
            hc.setMinimumIdle(1);
            hc.setConnectionTimeout(5000);
        }

        HikariDataSource ds;
        try {
            ds = new HikariDataSource(hc);
        } catch (RuntimeException e) {
            throw new RuntimeException(
                    "[SFCore] No se pudo conectar a la base de datos (" + dialect.id() + ") para namespace '"
                            + namespace + "': " + e.getMessage(), e);
        }

        // Hook de arranque (PRAGMA en SQLite, no-op en MySQL)
        try (Connection conn = ds.getConnection()) {
            dialect.onStartup(conn);
        } catch (SQLException e) {
            ds.close();
            throw new RuntimeException("[SFCore] Fallo en onStartup del dialecto para '" + namespace + "'", e);
        }

        SFDatabase db = new SFDatabase(namespace, ds, dialect);
        cache.put(namespace, db);
        log.info("[SFCore] Pool creado para namespace '" + namespace + "' (" + dialect.id() + ")");
        return db;
    }

    public synchronized void closeAll() {
        for (SFDatabase db : cache.values()) {
            try {
                db.close();
            } catch (Exception e) {
                log.warning("[SFCore] Error cerrando pool de '" + db.namespace() + "': " + e.getMessage());
            }
        }
        cache.clear();
    }
}
