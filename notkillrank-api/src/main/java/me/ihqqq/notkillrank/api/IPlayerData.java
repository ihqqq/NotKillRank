package me.ihqqq.notkillrank.api;

import java.util.List;
import java.util.Map;

public interface IPlayerData {

    String getUUID();

    String getName();
    void setName(String name);

    int getElo();
    void setElo(int elo);

    int getKills();
    void setKills(int kills);

    int getDeaths();
    void setDeaths(int deaths);

    int getKillStreak();
    void setKillStreak(int streak);

    int getDeathStreak();
    void setDeathStreak(int streak);

    int getHighestKillStreak();
    void setHighestKillStreak(int streak);

    int getPeakElo();
    void setPeakElo(int peak);

    String getLastKillerUUID();
    void setLastKillerUUID(String uuid);

    long getLastKilledTime();
    void setLastKilledTime(long time);

    long getLastOnline();
    void setLastOnline(long time);

    long getFirstJoinTime();

    long getSessionStart();
    void setSessionStart(long time);

    long getDailyOnlineMs();
    void setDailyOnlineMs(long ms);

    String getCurrentDay();
    void setCurrentDay(String day);

    long getNoDeathStart();
    void setNoDeathStart(long time);

    Map<String, List<Long>> getKillLog();

    Map<String, Integer> getBounties();

    Map<String, Long> getBountyTimestamps();
}
