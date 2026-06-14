package me.ihqqq.notkillrank.storage;

import me.ihqqq.notkillrank.api.IPlayerData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerData implements IPlayerData {

    private final String uuid;
    private volatile String name;
    private volatile int elo;
    private volatile int kills;
    private volatile int deaths;
    private volatile int killStreak;
    private volatile int deathStreak;
    private volatile int highestKillStreak;
    private volatile int peakElo;
    private volatile String lastKillerUUID;
    private volatile long lastKilledTime;
    private volatile long lastOnline;
    private final long firstJoinTime;
    private volatile long sessionStart;
    private volatile long dailyOnlineMs;
    private volatile String currentDay;
    private volatile long noDeathStart;

    private final Map<String, List<Long>> killLog;
    private final Map<String, Integer> bounties;
    private final Map<String, Long> bountyTimestamps;

    public PlayerData(String uuid, String name, int startElo) {
        this.uuid = uuid;
        this.name = name;
        this.elo = startElo;
        this.kills = 0;
        this.deaths = 0;
        this.killStreak = 0;
        this.deathStreak = 0;
        this.highestKillStreak = 0;
        this.peakElo = startElo;
        this.lastKillerUUID = null;
        this.lastKilledTime = 0;
        this.lastOnline = System.currentTimeMillis();
        this.firstJoinTime = System.currentTimeMillis();
        this.sessionStart = System.currentTimeMillis();
        this.dailyOnlineMs = 0;
        this.currentDay = "";
        this.noDeathStart = System.currentTimeMillis();
        this.killLog = new ConcurrentHashMap<>();
        this.bounties = new ConcurrentHashMap<>();
        this.bountyTimestamps = new ConcurrentHashMap<>();
    }

    public PlayerData(String uuid, String name, int elo, int kills, int deaths,
                      int killStreak, int deathStreak, int highestKillStreak, int peakElo,
                      String lastKillerUUID, long lastKilledTime, long lastOnline,
                      long firstJoinTime, long sessionStart, long dailyOnlineMs,
                      String currentDay, long noDeathStart,
                      Map<String, List<Long>> killLog, Map<String, Integer> bounties,
                      Map<String, Long> bountyTimestamps) {
        this.uuid = uuid;
        this.name = name;
        this.elo = elo;
        this.kills = kills;
        this.deaths = deaths;
        this.killStreak = killStreak;
        this.deathStreak = deathStreak;
        this.highestKillStreak = highestKillStreak;
        this.peakElo = peakElo;
        this.lastKillerUUID = lastKillerUUID;
        this.lastKilledTime = lastKilledTime;
        this.lastOnline = lastOnline;
        this.firstJoinTime = firstJoinTime;
        this.sessionStart = sessionStart;
        this.dailyOnlineMs = dailyOnlineMs;
        this.currentDay = currentDay;
        this.noDeathStart = noDeathStart;

        this.killLog = new ConcurrentHashMap<>();
        if (killLog != null) {
            for (Map.Entry<String, List<Long>> e : killLog.entrySet()) {
                this.killLog.put(e.getKey(), new CopyOnWriteArrayList<>(e.getValue()));
            }
        }
        this.bounties = new ConcurrentHashMap<>(bounties != null ? bounties : Collections.emptyMap());
        this.bountyTimestamps = new ConcurrentHashMap<>(bountyTimestamps != null ? bountyTimestamps : Collections.emptyMap());
    }


    public PlayerData snapshot() {
        Map<String, List<Long>> killLogCopy = new HashMap<>();
        for (Map.Entry<String, List<Long>> e : killLog.entrySet()) {
            killLogCopy.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        Map<String, Integer> bountiesCopy = new HashMap<>(bounties);
        Map<String, Long> bountyTimestampsCopy = new HashMap<>(bountyTimestamps);

        return new PlayerData(
                uuid, name, elo, kills, deaths,
                killStreak, deathStreak, highestKillStreak, peakElo,
                lastKillerUUID, lastKilledTime, lastOnline,
                firstJoinTime, sessionStart, dailyOnlineMs,
                currentDay, noDeathStart,
                killLogCopy, bountiesCopy, bountyTimestampsCopy
        );
    }


    @Override public String getUUID() { return uuid; }
    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }
    @Override public int getElo() { return elo; }
    @Override public void setElo(int elo) { this.elo = elo; }
    @Override public int getKills() { return kills; }
    @Override public void setKills(int kills) { this.kills = kills; }
    @Override public int getDeaths() { return deaths; }
    @Override public void setDeaths(int deaths) { this.deaths = deaths; }
    @Override public int getKillStreak() { return killStreak; }
    @Override public void setKillStreak(int streak) { this.killStreak = streak; }
    @Override public int getDeathStreak() { return deathStreak; }
    @Override public void setDeathStreak(int streak) { this.deathStreak = streak; }
    @Override public int getHighestKillStreak() { return highestKillStreak; }
    @Override public void setHighestKillStreak(int streak) { this.highestKillStreak = streak; }
    @Override public int getPeakElo() { return peakElo; }
    @Override public void setPeakElo(int peak) { this.peakElo = peak; }
    @Override public String getLastKillerUUID() { return lastKillerUUID; }
    @Override public void setLastKillerUUID(String uuid) { this.lastKillerUUID = uuid; }
    @Override public long getLastKilledTime() { return lastKilledTime; }
    @Override public void setLastKilledTime(long time) { this.lastKilledTime = time; }
    @Override public long getLastOnline() { return lastOnline; }
    @Override public void setLastOnline(long time) { this.lastOnline = time; }
    @Override public long getFirstJoinTime() { return firstJoinTime; }
    @Override public long getSessionStart() { return sessionStart; }
    @Override public void setSessionStart(long time) { this.sessionStart = time; }
    @Override public long getDailyOnlineMs() { return dailyOnlineMs; }
    @Override public void setDailyOnlineMs(long ms) { this.dailyOnlineMs = ms; }
    @Override public String getCurrentDay() { return currentDay; }
    @Override public void setCurrentDay(String day) { this.currentDay = day; }
    @Override public long getNoDeathStart() { return noDeathStart; }
    @Override public void setNoDeathStart(long time) { this.noDeathStart = time; }


    @Override public Map<String, List<Long>> getKillLog() { return killLog; }
    @Override public Map<String, Integer> getBounties() { return bounties; }
    @Override public Map<String, Long> getBountyTimestamps() { return bountyTimestamps; }


    public List<Long> getOrCreateKillTimestamps(String victimUUID) {
        return killLog.computeIfAbsent(victimUUID, k -> new CopyOnWriteArrayList<>());
    }
}