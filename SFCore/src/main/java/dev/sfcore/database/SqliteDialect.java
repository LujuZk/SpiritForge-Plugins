package dev.sfcore.database;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqliteDialect implements SqlDialect {

    @Override
    public String id() {
        return "sqlite";
    }

    @Override
    public String upsert(String table, String[] keyCols, String[] valueCols) {
        StringBuilder cols = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        int total = keyCols.length + valueCols.length;
        for (int i = 0; i < keyCols.length; i++) {
            if (i > 0) { cols.append(", "); placeholders.append(", "); }
            cols.append(keyCols[i]);
            placeholders.append("?");
        }
        for (int i = 0; i < valueCols.length; i++) {
            cols.append(", ").append(valueCols[i]);
            placeholders.append(", ?");
        }
        return "INSERT OR REPLACE INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
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
        return "INSERT OR IGNORE INTO " + table + " (" + c + ") VALUES (" + p + ")";
    }

    @Override public String uuidType()          { return "TEXT"; }
    @Override public String varchar(int length) { return "TEXT"; }
    @Override public String doubleType()        { return "REAL"; }
    @Override public String intType()           { return "INTEGER"; }
    @Override public String blobType()          { return "BLOB"; }

    @Override
    public String driverClass() {
        return "org.sqlite.JDBC";
    }

    @Override
    public String jdbcUrl(ConfigurationSection cfg, File dataFolder, String namespace) {
        String dir = cfg.getString("sqlite.directory", "db");
        File dbDir = new File(dataFolder, dir);
        if (!dbDir.exists()) dbDir.mkdirs();
        File dbFile = new File(dbDir, namespace + ".db");
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    @Override
    public void onStartup(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        }
    }

    @Override
    public String tableName(String namespace, String table) {
        return table;
    }
}
