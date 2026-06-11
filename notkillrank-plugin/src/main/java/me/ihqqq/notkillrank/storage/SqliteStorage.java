package me.ihqqq.notkillrank.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqliteStorage extends AbstractSqlStorage {

    private final String filePath;

    public SqliteStorage(String filePath) {
        this.filePath = filePath;
    }

    @Override
    protected Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filePath);
        try (var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
        }
        return conn;
    }
}
