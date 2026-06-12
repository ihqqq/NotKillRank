package me.ihqqq.notkillrank.storage;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.*;

public class PluginDataManager {

    public static HashMap<String, PlayerData> playerDatabase = new HashMap<>();

    private static List<PlayerData> cachedTop = null;
    private static long topCacheBuiltAt = 0;
    private static final long TOP_CACHE_TTL_MS = 30_000L;

    public static void loadAllDatabase() {
        MessageUtil.log("&a[PluginDataManager] Khởi tạo bộ nhớ đệm dữ liệu người chơi.");
    }

    public static void saveAllDatabase() {
        for (PlayerData data : playerDatabase.values()) {
            PluginDataStorage.savePlayerData(data.getUUID(), data);
        }
        MessageUtil.log("&a[PluginDataManager] Đã lưu " + playerDatabase.size() + " hồ sơ người chơi.");
    }

    public static PlayerData getOrCreate(Player player) {
        String uuid = player.getUniqueId().toString();
        if (playerDatabase.containsKey(uuid)) {
            PlayerData data = playerDatabase.get(uuid);
            data.setName(player.getName());
            return data;
        }
        PlayerData loaded = PluginDataStorage.getPlayerData(uuid);
        if (loaded != null) {
            loaded.setName(player.getName());
            playerDatabase.put(uuid, loaded);
            return loaded;
        }
        PlayerData fresh = new PlayerData(uuid, player.getName(), Settings.ELO_START);
        playerDatabase.put(uuid, fresh);
        return fresh;
    }

    public static PlayerData getPlayerDatabase(String uuid) {
        if (playerDatabase.containsKey(uuid)) return playerDatabase.get(uuid);
        PlayerData loaded = PluginDataStorage.getPlayerData(uuid);
        if (loaded != null) {
            playerDatabase.put(uuid, loaded);
            return loaded;
        }
        return null;
    }

    public static PlayerData getPlayerDatabaseByName(String name) {
        for (PlayerData data : playerDatabase.values()) {
            if (data.getName().equalsIgnoreCase(name)) return data;
        }
        PlayerData loaded = PluginDataStorage.getPlayerDataByName(name);
        if (loaded != null) {
            playerDatabase.put(loaded.getUUID(), loaded);
            return loaded;
        }
        return null;
    }

    public static void savePlayerDatabaseToStorage(String uuid) {
        PlayerData data = playerDatabase.get(uuid);
        if (data != null) PluginDataStorage.savePlayerData(uuid, data);
    }

    public static void savePlayerDatabaseToStorage(String uuid, PlayerData data) {
        PluginDataStorage.savePlayerData(uuid, data);
    }

    public static void savePlayerDatabaseToHashMap(String uuid, PlayerData data) {
        playerDatabase.put(uuid, data);
    }

    public static void clearPlayerDatabase(String uuid) {
        PlayerData data = playerDatabase.remove(uuid);
        if (data != null) PluginDataStorage.savePlayerData(uuid, data);
    }

    public static void evictPlayerDatabase(String uuid) {
        playerDatabase.remove(uuid);
    }

    public static List<PlayerData> getTopPlayers(int limit) {
        long now = System.currentTimeMillis();
        if (cachedTop == null || (now - topCacheBuiltAt) > TOP_CACHE_TTL_MS) {
            List<PlayerData> allFromDisk = PluginDataStorage.getAllPlayerData();
            for (PlayerData d : allFromDisk) {
                playerDatabase.putIfAbsent(d.getUUID(), d);
            }
            cachedTop = new ArrayList<>(playerDatabase.values());
            topCacheBuiltAt = now;
        }
        List<PlayerData> sorted = new ArrayList<>(playerDatabase.values());
        sorted.sort((a, b) -> b.getElo() - a.getElo());
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    public static void invalidateTopCache() {
        topCacheBuiltAt = 0;
    }

    public static void updateTop1Status() {
        if (!Settings.MODULE_VOSONG) return;
        List<PlayerData> top = getTopPlayers(1);
        if (top.isEmpty()) return;
        PlayerData top1 = top.get(0);

        for (PlayerData data : playerDatabase.values()) {
            if (data.getUUID().equals(top1.getUUID())) {
                if (top1.getTop1Since() <= 0) {
                    top1.setTop1Since(System.currentTimeMillis());
                    final String uuid = top1.getUUID();
                    org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                            NotKillRank.plugin, () -> savePlayerDatabaseToStorage(uuid));
                }
            } else {
                if (data.getTop1Since() > 0) {
                    data.setTop1Since(0);
                    final String uuid = data.getUUID();
                    org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                            NotKillRank.plugin, () -> savePlayerDatabaseToStorage(uuid));
                }
            }
        }
    }
}
