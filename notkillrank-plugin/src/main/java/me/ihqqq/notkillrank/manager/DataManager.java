package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.config.ConfigManager;
import me.ihqqq.notkillrank.storage.IDataStorage;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.StorageFactory;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.*;

public class DataManager {

    private static DataManager instance;
    private final IDataStorage storage;
    private final Map<String, PlayerData> cache = new HashMap<>();

    private List<PlayerData> cachedTopAll = null;
    private long topCacheBuiltAt = 0;

    private static final long TOP_CACHE_TTL_MS = 30_000L;

    public DataManager() {
        this.storage = StorageFactory.create();
        instance = this;
    }

    public static DataManager getInstance() {
        return instance;
    }

    public PlayerData getOrCreate(Player player) {
        String uuid = player.getUniqueId().toString();
        if (cache.containsKey(uuid)) {
            PlayerData data = cache.get(uuid);
            data.setName(player.getName());
            return data;
        }
        PlayerData loaded = storage.load(uuid);
        if (loaded != null) {
            loaded.setName(player.getName());
            cache.put(uuid, loaded);
            return loaded;
        }
        int startElo = ConfigManager.getInstance().getEloConfig().getInt("start-elo", 1000);
        PlayerData fresh = new PlayerData(uuid, player.getName(), startElo);
        cache.put(uuid, fresh);
        return fresh;
    }

    public PlayerData get(String uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        PlayerData loaded = storage.load(uuid);
        if (loaded != null) {
            cache.put(uuid, loaded);
            return loaded;
        }
        return null;
    }

    public PlayerData getByName(String name) {
        for (PlayerData data : cache.values()) {
            if (data.getName().equalsIgnoreCase(name)) return data;
        }
        PlayerData loaded = storage.loadByName(name);
        if (loaded != null) {
            cache.put(loaded.getUUID(), loaded);
            return loaded;
        }
        return null;
    }

    public void save(String uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) storage.save(data);
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            storage.save(data);
        }
        MessageUtil.log("&aSaved " + cache.size() + " player profiles.");
    }

    public void unload(String uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) storage.save(data);
    }

    public void evict(String uuid) {
        cache.remove(uuid);
    }

    public List<PlayerData> getTopPlayers(int limit) {
        long now = System.currentTimeMillis();
        boolean cacheExpired = (now - topCacheBuiltAt) > TOP_CACHE_TTL_MS;

        if (cachedTopAll == null || cacheExpired) {
            List<PlayerData> allLoaded = storage.loadAll();
            for (PlayerData d : allLoaded) {
                cache.putIfAbsent(d.getUUID(), d);
            }
            cachedTopAll = new ArrayList<>(cache.values());
            topCacheBuiltAt = now;
        }

        List<PlayerData> sorted = new ArrayList<>(cache.values());
        sorted.sort((a, b) -> b.getElo() - a.getElo());
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    public void invalidateTopCache() {
        topCacheBuiltAt = 0;
    }

    public IDataStorage getStorage() {
        return storage;
    }

    public Map<String, PlayerData> getCache() {
        return cache;
    }
}