package me.ihqqq.notkillrank.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PluginDataSQLiteStorage extends PluginDataAbstractSql {

    private final String filePath;

    public PluginDataSQLiteStorage(String filePath) {
        this.filePath = filePath;
    }

    @Override
    protected Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver không tìm thấy", e);
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filePath);
        try (var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
        }
        return conn;
    }
}
