package me.ihqqq.notkillrank.storage;

import me.ihqqq.notkillrank.api.IPlayerData;

import java.util.*;

public class PlayerData implements IPlayerData {

    private final String uuid;
    private String name;
    private int elo;
    private int kills;
    private int deaths;
    private int killStreak;
    private int deathStreak;
    private int highestKillStreak;
    private int peakElo;
    private String lastKillerUUID;
    private long lastKilledTime;
    private long lastOnline;
    private final long firstJoinTime;
    private long sessionStart;
    private long dailyOnlineMs;
    private String currentDay;
    private long noDeathStart;
    private Map<String, List<Long>> killLog;
    private Map<String, Integer> bounties;
    private long top1Since;

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
        this.killLog = new HashMap<>();
        this.bounties = new HashMap<>();
        this.top1Since = 0;
    }

    public PlayerData(String uuid, String name, int elo, int kills, int deaths,
                      int killStreak, int deathStreak, int highestKillStreak, int peakElo,
                      String lastKillerUUID, long lastKilledTime, long lastOnline,
                      long firstJoinTime, long sessionStart, long dailyOnlineMs,
                      String currentDay, long noDeathStart,
                      Map<String, List<Long>> killLog, Map<String, Integer> bounties,
                      long top1Since) {
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
        this.killLog = killLog != null ? killLog : new HashMap<>();
        this.bounties = bounties != null ? bounties : new HashMap<>();
        this.top1Since = top1Since;
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
    @Override public long getTop1Since() { return top1Since; }
    @Override public void setTop1Since(long time) { this.top1Since = time; }
}
