package me.ihqqq.notkillrank.storage;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.enums.StorageType;
import me.ihqqq.notkillrank.util.MessageUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class PluginDataStorage {

    private static PluginStorage STORAGE;

    public static void init(StorageType type) {
        switch (type) {
            case SQLITE -> STORAGE = createSQLite();
            case H2     -> STORAGE = createH2();
            default     -> {
                MessageUtil.log("&7[Storage] Đang dùng lưu trữ YAML.");
                STORAGE = new PluginDataYAMLStorage();
            }
        }
    }

    public static PlayerData getPlayerData(String uuid) {
        return STORAGE.load(uuid);
    }

    public static PlayerData getPlayerDataByName(String name) {
        return STORAGE.loadByName(name);
    }

    public static void savePlayerData(String uuid, PlayerData data) {
        STORAGE.save(data);
    }

    public static List<PlayerData> getAllPlayerData() {
        return STORAGE.loadAll();
    }

    public static void close() {
        if (STORAGE != null) STORAGE.close();
    }

    private static PluginStorage createSQLite() {
        String fileName = Settings.STORAGE_SQLITE_FILE;
        File dbFile = new File(NotKillRank.plugin.getDataFolder(), fileName + ".db");
        dbFile.getParentFile().mkdirs();
        try {
            PluginDataSQLiteStorage storage = new PluginDataSQLiteStorage(dbFile.getAbsolutePath());
            storage.init();
            MessageUtil.log("&7[Storage] Đang dùng lưu trữ SQLite: " + dbFile.getName());
            return storage;
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Không thể khởi tạo SQLite: " + e.getMessage() + " — quay về YAML.");
            return new PluginDataYAMLStorage();
        }
    }

    private static PluginStorage createH2() {
        String fileName = Settings.STORAGE_H2_FILE;
        File dataFolder = NotKillRank.plugin.getDataFolder();
        dataFolder.mkdirs();
        String url = "jdbc:h2:" + dataFolder.getAbsolutePath() + "/" + fileName
                + ";AUTO_SERVER=FALSE;MODE=MySQL";
        try {
            PluginDataH2Storage storage = new PluginDataH2Storage(url);
            storage.init();
            MessageUtil.log("&7[Storage] Đang dùng lưu trữ H2: " + fileName);
            return storage;
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Không thể khởi tạo H2: " + e.getMessage() + " — quay về YAML.");
            return new PluginDataYAMLStorage();
        }
    }
}
