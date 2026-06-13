package me.ihqqq.notkillrank.storage;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PluginDataManager {

    public static final ConcurrentHashMap<String, PlayerData> playerDatabase = new ConcurrentHashMap<>();

    private static volatile List<PlayerData> cachedTop = null;
    private static volatile long topCacheBuiltAt = 0;
    private static final long TOP_CACHE_TTL_MS = 30_000L;

    public static void loadAllDatabase() {
        MessageUtil.log("&a[PluginDataManager] Khởi tạo bộ nhớ đệm dữ liệu người chơi.");
    }

    public static void saveAllDatabase() {
        List<PlayerData> snapshot = new ArrayList<>(playerDatabase.values());
        for (PlayerData data : snapshot) {
            PluginDataStorage.savePlayerData(data.getUUID(), data);
        }
        MessageUtil.log("&a[PluginDataManager] Đã lưu " + snapshot.size() + " hồ sơ người chơi.");
    }

    public static PlayerData getOrCreate(Player player) {
        String uuid = player.getUniqueId().toString();
        PlayerData existing = playerDatabase.get(uuid);
        if (existing != null) {
            existing.setName(player.getName());
            return existing;
        }
        PlayerData loaded = PluginDataStorage.getPlayerData(uuid);
        if (loaded != null) {
            loaded.setName(player.getName());
            PlayerData prev = playerDatabase.putIfAbsent(uuid, loaded);
            return prev != null ? prev : loaded;
        }
        PlayerData fresh = new PlayerData(uuid, player.getName(), Settings.ELO_START);
        PlayerData prev = playerDatabase.putIfAbsent(uuid, fresh);
        return prev != null ? prev : fresh;
    }

    public static PlayerData getPlayerDatabase(String uuid) {
        PlayerData cached = playerDatabase.get(uuid);
        if (cached != null) return cached;
        PlayerData loaded = PluginDataStorage.getPlayerData(uuid);
        if (loaded != null) {
            playerDatabase.putIfAbsent(uuid, loaded);
            return playerDatabase.get(uuid);
        }
        return null;
    }

    public static PlayerData getPlayerDatabaseByName(String name) {
        for (PlayerData data : playerDatabase.values()) {
            if (data.getName().equalsIgnoreCase(name)) return data;
        }
        PlayerData loaded = PluginDataStorage.getPlayerDataByName(name);
        if (loaded != null) {
            playerDatabase.putIfAbsent(loaded.getUUID(), loaded);
            return playerDatabase.get(loaded.getUUID());
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
            Map<String, PlayerData> merged = new java.util.HashMap<>();
            for (PlayerData d : allFromDisk) {
                merged.put(d.getUUID(), d);
            }
            for (Map.Entry<String, PlayerData> e : playerDatabase.entrySet()) {
                merged.put(e.getKey(), e.getValue());
            }
            List<PlayerData> sorted = new ArrayList<>(merged.values());
            sorted.sort((a, b) -> b.getElo() - a.getElo());
            cachedTop = sorted;
            topCacheBuiltAt = now;
        }
        List<PlayerData> result = cachedTop;
        return new ArrayList<>(result.subList(0, Math.min(limit, result.size())));
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
