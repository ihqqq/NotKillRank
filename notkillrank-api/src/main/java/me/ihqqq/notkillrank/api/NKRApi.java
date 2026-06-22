package me.ihqqq.notkillrank.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface NKRApi {

    Optional<IPlayerData> getPlayerData(UUID uuid);

    Optional<IPlayerData> getPlayerData(Player player);

    void lookupPlayerAsync(String name, Consumer<Optional<IPlayerData>> callback);

    List<IPlayerData> getTopPlayers(int limit);

    boolean setElo(UUID uuid, int elo);

    boolean giveElo(UUID uuid, int amount);

    boolean takeElo(UUID uuid, int amount);

    void saveAsync(UUID uuid);

    String getRankTag(int elo);

    String getRankName(int elo);

    String getNextRankTag(int elo);

    int getEloNeededForNextRank(int elo);

    List<IRankInfo> getAllRanks();

    boolean isModuleEnabled(NKRModule module);

    boolean isNewbie(UUID uuid);

    boolean isSongSot(UUID uuid);

    boolean isWeak(UUID uuid);

    int getTotalBounty(UUID uuid);

    boolean hasBounty(UUID uuid);

    Optional<IKillResult> previewKill(UUID killer, UUID victim);

    int getStartElo();

    int getMinElo();
}
