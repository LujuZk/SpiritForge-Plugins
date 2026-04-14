package dev.sfcore.database;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public final class MysqlDialect implements SqlDialect {

    @Override
    public String id() {
        return "mysql";
    }

    @Override
    public String upsert(String table, String[] keyCols, String[] valueCols) {
        StringBuilder cols = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < keyCols.length; i++) {
            if (i > 0) { cols.append(", "); placeholders.append(", "); }
            cols.append(keyCols[i]);
            placeholders.append("?");
        }
        for (int i = 0; i < valueCols.length; i++) {
            cols.append(", ").append(valueCols[i]);
            placeholders.append(", ?");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(table)
           .append(" (").append(cols).append(") VALUES (").append(placeholders).append(")");
        if (valueCols.length > 0) {
            sql.append(" ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < valueCols.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(valueCols[i]).append(" = VALUES(").append(valueCols[i]).append(")");
            }
        } else {
            // Solo PKs, sin columnas actualizables — fallback equivalente a INSERT IGNORE
            sql.append(" ON DUPLICATE KEY UPDATE ").append(keyCols[0]).append(" = ").append(keyCols[0]);
        }
        return sql.toString();
    }

    @Override
    public String insertIgnore(String table, String[] cols) {
        StringBuilder c = new StringBuilder();
        StringBuilder p = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) { c.append(", "); p.append(", "); }
            c.append(cols[i]);
            p.append("?");
        }
        return "INSERT IGNORE INTO " + table + " (" + c + ") VALUES (" + p + ")";
    }

    @Override public String uuidType()          { return "VARCHAR(36)"; }
    @Override public String varchar(int length) { return "VARCHAR(" + length + ")"; }
    @Override public String doubleType()        { return "DOUBLE"; }
    @Override public String intType()           { return "INT"; }
    @Override public String blobType()          { return "MEDIUMBLOB"; }

    @Override
    public String driverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String jdbcUrl(ConfigurationSection cfg, File dataFolder, String namespace) {
        String host = cfg.getString("mysql.host", "localhost");
        int port = cfg.getInt("mysql.port", 3306);
        String database = cfg.getString("mysql.database", "spiritforge");
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&useUnicode=true&characterEncoding=UTF-8"
                + "&connectionCollation=utf8mb4_unicode_ci"
                + "&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    @Override
    public void onStartup(Connection conn) {
        // InnoDB ya tiene su propio WAL; nada que hacer al arrancar el pool.
    }

    @Override
    public String tableName(String namespace, String table) {
        return namespace + "_" + table;
    }
}
