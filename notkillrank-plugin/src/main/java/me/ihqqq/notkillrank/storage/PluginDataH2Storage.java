package me.ihqqq.notkillrank.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PluginDataH2Storage extends PluginDataAbstractSql {

    private final String jdbcUrl;

    public PluginDataH2Storage(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    protected Connection openConnection() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 JDBC driver không tìm thấy", e);
        }
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }
}
