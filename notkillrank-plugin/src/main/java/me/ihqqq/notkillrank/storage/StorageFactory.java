package me.ihqqq.notkillrank.storage;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.util.MessageUtil;

import java.io.File;
import java.sql.SQLException;

public class StorageFactory {

    public static IDataStorage create() {
        String type = NotKillRank.getInstance().getConfig()
                .getString("storage.type", "YAML").toUpperCase();

        return switch (type) {
            case "SQLITE" -> createSqlite();
            case "H2" -> createH2();
            default -> {
                MessageUtil.log("&7[Storage] Using YAML storage.");
                yield new DataStorage();
            }
        };
    }

    private static IDataStorage createSqlite() {
        String fileName = NotKillRank.getInstance().getConfig()
                .getString("storage.sqlite.file", "playerdata");
        File dbFile = new File(NotKillRank.getInstance().getDataFolder(), fileName + ".db");
        dbFile.getParentFile().mkdirs();
        try {
            SqliteStorage storage = new SqliteStorage(dbFile.getAbsolutePath());
            storage.init();
            MessageUtil.log("&7[Storage] Using SQLite storage: " + dbFile.getName());
            return storage;
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Failed to initialize SQLite: " + e.getMessage()
                    + " — falling back to YAML.");
            return new DataStorage();
        }
    }

    private static IDataStorage createH2() {
        String fileName = NotKillRank.getInstance().getConfig()
                .getString("storage.h2.file", "playerdata");
        File dataFolder = NotKillRank.getInstance().getDataFolder();
        dataFolder.mkdirs();
        String url = "jdbc:h2:" + dataFolder.getAbsolutePath() + "/" + fileName
                + ";AUTO_SERVER=FALSE;MODE=MySQL";
        try {
            H2Storage storage = new H2Storage(url);
            storage.init();
            MessageUtil.log("&7[Storage] Using H2 storage: " + fileName);
            return storage;
        } catch (SQLException e) {
            MessageUtil.warn("[Storage] Failed to initialize H2: " + e.getMessage()
                    + " — falling back to YAML.");
            return new DataStorage();
        }
    }
}
