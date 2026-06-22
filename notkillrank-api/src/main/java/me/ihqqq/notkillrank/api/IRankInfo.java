package me.ihqqq.notkillrank.api;

public interface IRankInfo {

    int getMinElo();

    int getMaxElo();

    String getTag();

    String getPlainName();

    default boolean contains(int elo) {
        return elo >= getMinElo() && elo <= getMaxElo();
    }
}
