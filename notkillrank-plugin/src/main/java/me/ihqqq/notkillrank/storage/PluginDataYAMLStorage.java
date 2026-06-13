package me.ihqqq.notkillrank.storage;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PluginDataYAMLStorage implements PluginStorage {

    private final File dataFolder;

    public PluginDataYAMLStorage() {
        this.dataFolder = new File(NotKillRank.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    @Override
    public PlayerData load(String uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) return null;
        return fromFile(uuid, YamlConfiguration.loadConfiguration(file));
    }

    @Override
    public PlayerData loadByName(String name) {
        File[] files = dataFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return null;
        for (File file : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            if (cfg.getString("name", "").equalsIgnoreCase(name)) {
                String uuid = file.getName().replace(".yml", "");
                return fromFile(uuid, cfg);
            }
        }
        return null;
    }

    @Override
    public void save(PlayerData data) {
        File file = new File(dataFolder, data.getUUID() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("uuid",                data.getUUID());
        cfg.set("name",                data.getName());
        cfg.set("elo",                 data.getElo());
        cfg.set("kills",               data.getKills());
        cfg.set("deaths",              data.getDeaths());
        cfg.set("kill-streak",         data.getKillStreak());
        cfg.set("death-streak",        data.getDeathStreak());
        cfg.set("highest-kill-streak", data.getHighestKillStreak());
        cfg.set("peak-elo",            data.getPeakElo());
        cfg.set("last-killer-uuid",    data.getLastKillerUUID());
        cfg.set("last-killed-time",    data.getLastKilledTime());
        cfg.set("last-online",         data.getLastOnline());
        cfg.set("first-join-time",     data.getFirstJoinTime());
        cfg.set("daily-online-ms",     data.getDailyOnlineMs());
        cfg.set("current-day",         data.getCurrentDay());
        cfg.set("no-death-start",      data.getNoDeathStart());
        cfg.set("top1-since",          data.getTop1Since());

        for (Map.Entry<String, List<Long>> e : data.getKillLog().entrySet()) {
            cfg.set("kill-log." + e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Integer> e : data.getBounties().entrySet()) {
            cfg.set("bounties." + e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Long> e : data.getBountyTimestamps().entrySet()) {
            cfg.set("bounty-timestamps." + e.getKey(), e.getValue());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            MessageUtil.warn("[Storage] Không thể lưu dữ liệu " + data.getName() + ": " + e.getMessage());
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

    private PlayerData fromFile(String uuid, FileConfiguration cfg) {
        int startElo = Settings.ELO_START;

        Map<String, List<Long>> killLog = new HashMap<>();
        ConfigurationSection klSection = cfg.getConfigurationSection("kill-log");
        if (klSection != null) {
            for (String victimUUID : klSection.getKeys(false)) {
                List<Long> timestamps = new ArrayList<>();
                for (Object obj : klSection.getList(victimUUID, new ArrayList<>())) {
                    if (obj instanceof Long l) timestamps.add(l);
                    else if (obj instanceof Integer i) timestamps.add((long) i);
                    else if (obj instanceof Number n) timestamps.add(n.longValue());
                }
                killLog.put(victimUUID, timestamps);
            }
        }

        Map<String, Integer> bounties = new HashMap<>();
        ConfigurationSection bSection = cfg.getConfigurationSection("bounties");
        if (bSection != null) {
            for (String k : bSection.getKeys(false)) {
                bounties.put(k, bSection.getInt(k, 0));
            }
        }

        Map<String, Long> bountyTimestamps = new HashMap<>();
        ConfigurationSection btSection = cfg.getConfigurationSection("bounty-timestamps");
        if (btSection != null) {
            for (String k : btSection.getKeys(false)) {
                bountyTimestamps.put(k, btSection.getLong(k, 0));
            }
        }

        int elo = cfg.getInt("elo", startElo);
        return new PlayerData(
                uuid,
                cfg.getString("name", "Unknown"),
                elo,
                cfg.getInt("kills", 0),
                cfg.getInt("deaths", 0),
                cfg.getInt("kill-streak", 0),
                cfg.getInt("death-streak", 0),
                cfg.getInt("highest-kill-streak", 0),
                cfg.getInt("peak-elo", elo),
                cfg.getString("last-killer-uuid", null),
                cfg.getLong("last-killed-time", 0),
                cfg.getLong("last-online", System.currentTimeMillis()),
                cfg.getLong("first-join-time", System.currentTimeMillis()),
                System.currentTimeMillis(),
                cfg.getLong("daily-online-ms", 0),
                cfg.getString("current-day", ""),
                cfg.getLong("no-death-start", System.currentTimeMillis()),
                killLog,
                bounties,
                bountyTimestamps,
                cfg.getLong("top1-since", 0)
        );
    }
}
