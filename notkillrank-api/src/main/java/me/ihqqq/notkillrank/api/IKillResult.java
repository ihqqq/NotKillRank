package me.ihqqq.notkillrank.api;

public interface IKillResult {

    int getBaseElo();

    double getMultiplier();

    String getMultiplierLabel();

    int getRevengeBonusPercent();

    int getStreakBonusPercent();

    int getEloGained();

    int getWeakPenalty();

    int getTotalVictimLoss();

    boolean isStreakBreak();

    int getBrokenStreak();
}
