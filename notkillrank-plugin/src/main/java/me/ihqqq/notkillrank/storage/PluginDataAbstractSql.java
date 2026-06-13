package me.ihqqq.notkillrank.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.ihqqq.notkillrank.util.MessageUtil;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

public abstract class PluginDataAbstractSql implements PluginStorage {

    private static final Gson GSON = new Gson();
    private static final Type KILL_LOG_TYPE =
            new TypeToken<Map<String, List<Long>>>() {}.getType();
    private static final Type BOUNTIES_TYPE =
            new TypeToken<Map<String, Integer>>() {}.getType();
    private static final Type BOUNTY_TIMESTAMPS_TYPE =
            new TypeToken<Map<String, Long>>() {}.getType();

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS nkr_players (
              uuid                VARCHAR(36) PRIMARY KEY,
              name                VARCHAR(64)  NOT NULL,
              elo                 INT          DEFAULT 1000,
              kills               INT          DEFAULT 0,
              deaths              INT          DEFAULT 0,
              kill_streak         INT          DEFAULT 0,
              death_streak        INT          DEFAULT 0,
              highest_kill_streak INT          DEFAULT 0,
              peak_elo            INT          DEFAULT 1000,
              last_killer_uuid    VARCHAR(36),
              last_killed_time    BIGINT       DEFAULT 0,
              last_online         BIGINT       DEFAULT 0,
              first_join_time     BIGINT       DEFAULT 0,
              session_start       BIGINT       DEFAULT 0,
              daily_online_ms     BIGINT       DEFAULT 0,
              current_day         VARCHAR(32)  DEFAULT '',
              no_death_start      BIGINT       DEFAULT 0,
              kill_log            TEXT         DEFAULT '{}',
              bounties            TEXT         DEFAULT '{}',
              bounty_timestamps   TEXT         DEFAULT '{}',
              top1_since          BIGINT       DEFAULT 0
            )
            """;

    private static final String SELECT_BY_UUID  = "SELECT * FROM nkr_players WHERE uuid = ?";
    private static final String SELECT_BY_NAME  = "SELECT * FROM nkr_players WHERE LOWER(name) = LOWER(?)";
    private static final String SELECT_ALL      = "SELECT * FROM nkr_players";
    private static final String DELETE_BY_UUID  = "DELETE FROM nkr_players WHERE uuid = ?";
    private static final String INSERT_SQL = """
            INSERT INTO nkr_players (
              uuid, name, elo, kills, deaths, kill_streak, death_streak,
              highest_kill_streak, peak_elo, last_killer_uuid, last_killed_time,
              last_online, first_join_time, session_start, daily_online_ms,
              current_day, no_death_start, kill_log, bounties, bounty_timestamps, top1_since
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

    protected Connection connection;

    protected abstract Connection openConnection() throws SQLException;

    public void init() throws SQLException {
        connection = openConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_TABLE);
        }
        // Migration: add bounty_timestamps column to existing databases
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE nkr_players ADD COLUMN bounty_timestamps TEXT DEFAULT '{}'");
        } catch (SQLException ignored) {
            // Column already exists — safe to ignore
        }
    }

    protected synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = openConnection();
        }
        return connection;
    }

    @Override
    public synchronized PlayerData load(String uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement(SELECT_BY_UUID)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromRow(rs);
            }
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Không thể tải " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized PlayerData loadByName(String name) {
        try (PreparedStatement ps = getConnection().prepareStatement(SELECT_BY_NAME)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromRow(rs);
            }
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Không thể tải theo tên " + name + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized void save(PlayerData data) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(DELETE_BY_UUID)) {
                    del.setString(1, data.getUUID());
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(INSERT_SQL)) {
                    ins.setString(1,  data.getUUID());
                    ins.setString(2,  data.getName());
                    ins.setInt(3,     data.getElo());
                    ins.setInt(4,     data.getKills());
                    ins.setInt(5,     data.getDeaths());
                    ins.setInt(6,     data.getKillStreak());
                    ins.setInt(7,     data.getDeathStreak());
                    ins.setInt(8,     data.getHighestKillStreak());
                    ins.setInt(9,     data.getPeakElo());
                    ins.setString(10, data.getLastKillerUUID());
                    ins.setLong(11,   data.getLastKilledTime());
                    ins.setLong(12,   data.getLastOnline());
                    ins.setLong(13,   data.getFirstJoinTime());
                    ins.setLong(14,   data.getSessionStart());
                    ins.setLong(15,   data.getDailyOnlineMs());
                    ins.setString(16, data.getCurrentDay());
                    ins.setLong(17,   data.getNoDeathStart());
                    ins.setString(18, GSON.toJson(data.getKillLog()));
                    ins.setString(19, GSON.toJson(data.getBounties()));
                    ins.setString(20, GSON.toJson(data.getBountyTimestamps()));
                    ins.setLong(21,   data.getTop1Since());
                    ins.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Không thể lưu " + data.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public synchronized List<PlayerData> loadAll() {
        List<PlayerData> list = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {
            while (rs.next()) list.add(fromRow(rs));
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Không thể tải toàn bộ dữ liệu: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    private PlayerData fromRow(ResultSet rs) throws SQLException {
        Map<String, List<Long>> killLog = GSON.fromJson(rs.getString("kill_log"), KILL_LOG_TYPE);
        Map<String, Integer> bounties   = GSON.fromJson(rs.getString("bounties"), BOUNTIES_TYPE);
        String btJson = rs.getString("bounty_timestamps");
        Map<String, Long> bountyTimestamps = btJson != null
                ? GSON.fromJson(btJson, BOUNTY_TIMESTAMPS_TYPE) : null;
        if (killLog == null) killLog = new HashMap<>();
        if (bounties == null) bounties = new HashMap<>();
        if (bountyTimestamps == null) bountyTimestamps = new HashMap<>();
        return new PlayerData(
                rs.getString("uuid"),
                rs.getString("name"),
                rs.getInt("elo"),
                rs.getInt("kills"),
                rs.getInt("deaths"),
                rs.getInt("kill_streak"),
                rs.getInt("death_streak"),
                rs.getInt("highest_kill_streak"),
                rs.getInt("peak_elo"),
                rs.getString("last_killer_uuid"),
                rs.getLong("last_killed_time"),
                rs.getLong("last_online"),
                rs.getLong("first_join_time"),
                System.currentTimeMillis(),
                rs.getLong("daily_online_ms"),
                rs.getString("current_day"),
                rs.getLong("no_death_start"),
                killLog,
                bounties,
                bountyTimestamps,
                rs.getLong("top1_since")
        );
    }
}
