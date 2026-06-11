package me.ihqqq.notkillrank.storage;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.config.ConfigManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataStorage implements IDataStorage {

    private final File dataFolder;

    public DataStorage() {
        this.dataFolder = new File(NotKillRank.getInstance().getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public PlayerData load(String uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) return null;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int startElo = ConfigManager.getInstance().getEloConfig().getInt("start-elo", 1000);

        String name = cfg.getString("name", "Unknown");
        int elo = cfg.getInt("elo", startElo);
        int kills = cfg.getInt("kills", 0);
        int deaths = cfg.getInt("deaths", 0);
        int killStreak = cfg.getInt("kill-streak", 0);
        int deathStreak = cfg.getInt("death-streak", 0);
        int highestKillStreak = cfg.getInt("highest-kill-streak", 0);
        int peakElo = cfg.getInt("peak-elo", elo);
        String lastKillerUUID = cfg.getString("last-killer-uuid", null);
        long lastKilledTime = cfg.getLong("last-killed-time", 0);
        long lastOnline = cfg.getLong("last-online", System.currentTimeMillis());
        long firstJoinTime = cfg.getLong("first-join-time", System.currentTimeMillis());
        long sessionStart = System.currentTimeMillis();
        long dailyOnlineMs = cfg.getLong("daily-online-ms", 0);
        String currentDay = cfg.getString("current-day", "");
        long noDeathStart = cfg.getLong("no-death-start", System.currentTimeMillis());
        long top1Since = cfg.getLong("top1-since", 0);

        Map<String, List<Long>> killLog = new HashMap<>();
        ConfigurationSection killLogSection = cfg.getConfigurationSection("kill-log");
        if (killLogSection != null) {
            for (String victimUUID : killLogSection.getKeys(false)) {
                List<Long> timestamps = new ArrayList<>();
                for (Object obj : killLogSection.getList(victimUUID, new ArrayList<>())) {
                    if (obj instanceof Long l) timestamps.add(l);
                    else if (obj instanceof Integer i) timestamps.add((long) i);
                    else if (obj instanceof Number n) timestamps.add(n.longValue());
                }
                killLog.put(victimUUID, timestamps);
            }
        }

        Map<String, Integer> bounties = new HashMap<>();
        ConfigurationSection bountiesSection = cfg.getConfigurationSection("bounties");
        if (bountiesSection != null) {
            for (String placerUUID : bountiesSection.getKeys(false)) {
                bounties.put(placerUUID, bountiesSection.getInt(placerUUID, 0));
            }
        }

        return new PlayerData(uuid, name, elo, kills, deaths, killStreak, deathStreak,
                highestKillStreak, peakElo, lastKillerUUID, lastKilledTime, lastOnline,
                firstJoinTime, sessionStart, dailyOnlineMs, currentDay, noDeathStart,
                killLog, bounties, top1Since);
    }

    @Override
    public void save(PlayerData data) {
        File file = new File(dataFolder, data.getUUID() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("uuid", data.getUUID());
        cfg.set("name", data.getName());
        cfg.set("elo", data.getElo());
        cfg.set("kills", data.getKills());
        cfg.set("deaths", data.getDeaths());
        cfg.set("kill-streak", data.getKillStreak());
        cfg.set("death-streak", data.getDeathStreak());
        cfg.set("highest-kill-streak", data.getHighestKillStreak());
        cfg.set("peak-elo", data.getPeakElo());
        cfg.set("last-killer-uuid", data.getLastKillerUUID());
        cfg.set("last-killed-time", data.getLastKilledTime());
        cfg.set("last-online", data.getLastOnline());
        cfg.set("first-join-time", data.getFirstJoinTime());
        cfg.set("daily-online-ms", data.getDailyOnlineMs());
        cfg.set("current-day", data.getCurrentDay());
        cfg.set("no-death-start", data.getNoDeathStart());
        cfg.set("top1-since", data.getTop1Since());

        for (Map.Entry<String, List<Long>> entry : data.getKillLog().entrySet()) {
            cfg.set("kill-log." + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Integer> entry : data.getBounties().entrySet()) {
            cfg.set("bounties." + entry.getKey(), entry.getValue());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            MessageUtil.warn("Failed to save data for " + data.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public List<PlayerData> loadAll() {
        List<PlayerData> list = new ArrayList<>();
        File[] files = dataFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return list;
        for (File file : files) {
            String uuid = file.getName().replace(".yml", "");
            PlayerData data = load(uuid);
            if (data != null) list.add(data);
        }
        return list;
    }

    @Override
    public void close() {
    }
}
