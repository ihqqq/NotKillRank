package me.ihqqq.notkillrank.api;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.api.event.EloChangeReason;
import me.ihqqq.notkillrank.api.event.NKREloChangeEvent;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.manager.BountyManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class NotKillRankApiImpl implements NKRApi {

    @Override
    public Optional<IPlayerData> getPlayerData(UUID uuid) {
        return Optional.ofNullable(PluginDataManager.getPlayerDatabase(uuid.toString()));
    }

    @Override
    public Optional<IPlayerData> getPlayerData(Player player) {
        return Optional.ofNullable(PluginDataManager.getPlayerDatabase(player.getUniqueId().toString()));
    }

    @Override
    public void lookupPlayerAsync(String name, Consumer<Optional<IPlayerData>> callback) {
        PlayerData fast = PluginDataManager.getPlayerDatabaseByNameNoIO(name);
        if (fast != null) {
            callback.accept(Optional.of(fast));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            PlayerData data = PluginDataManager.getPlayerDatabaseByName(name);
            Optional<IPlayerData> result = Optional.ofNullable(data);
            Bukkit.getScheduler().runTask(NotKillRank.plugin, () -> callback.accept(result));
        });
    }

    @Override
    public List<IPlayerData> getTopPlayers(int limit) {
        return new ArrayList<>(PluginDataManager.getTopPlayers(limit));
    }

    @Override
    public boolean setElo(UUID uuid, int elo) {
        if (elo < 0) return false;
        PlayerData data = PluginDataManager.getPlayerDatabase(uuid.toString());
        if (data == null) return false;

        int oldElo = data.getElo();
        int newElo = Math.max(Settings.ELO_MIN, elo);

        NKREloChangeEvent event = new NKREloChangeEvent(uuid, data.getName(), oldElo, newElo, EloChangeReason.API);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        data.setElo(event.getNewElo());
        if (data.getElo() > data.getPeakElo()) data.setPeakElo(data.getElo());
        PluginDataManager.invalidateTopCache();
        saveAsync(uuid);
        return true;
    }

    @Override
    public boolean giveElo(UUID uuid, int amount) {
        if (amount <= 0) return false;
        PlayerData data = PluginDataManager.getPlayerDatabase(uuid.toString());
        if (data == null) return false;

        int oldElo = data.getElo();
        int newElo = (int) Math.min((long) oldElo + amount, Integer.MAX_VALUE);

        NKREloChangeEvent event = new NKREloChangeEvent(uuid, data.getName(), oldElo, newElo, EloChangeReason.API);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        data.setElo(event.getNewElo());
        if (data.getElo() > data.getPeakElo()) data.setPeakElo(data.getElo());
        PluginDataManager.invalidateTopCache();
        saveAsync(uuid);
        return true;
    }

    @Override
    public boolean takeElo(UUID uuid, int amount) {
        if (amount <= 0) return false;
        PlayerData data = PluginDataManager.getPlayerDatabase(uuid.toString());
        if (data == null) return false;

        int oldElo = data.getElo();
        int newElo = Math.max(Settings.ELO_MIN, oldElo - amount);

        NKREloChangeEvent event = new NKREloChangeEvent(uuid, data.getName(), oldElo, newElo, EloChangeReason.API);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        data.setElo(event.getNewElo());
        PluginDataManager.invalidateTopCache();
        saveAsync(uuid);
        return true;
    }

    @Override
    public void saveAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin,
                () -> PluginDataManager.savePlayerDatabaseToStorage(uuid.toString()));
    }

    @Override
    public String getRankTag(int elo) {
        return RankManager.getInstance().getRankTag(elo);
    }

    @Override
    public String getRankName(int elo) {
        return MiniMessage.miniMessage().stripTags(getRankTag(elo));
    }

    @Override
    public String getNextRankTag(int elo) {
        return RankManager.getInstance().getNextRankTag(elo);
    }

    @Override
    public int getEloNeededForNextRank(int elo) {
        return RankManager.getInstance().getNextRankNeeded(elo);
    }

    @Override
    public List<IRankInfo> getAllRanks() {
        return RankManager.getInstance().getTiers().stream()
                .map(tier -> (IRankInfo) new IRankInfo() {
                    @Override public int getMinElo()     { return tier.min; }
                    @Override public int getMaxElo()     { return tier.max; }
                    @Override public String getTag()     { return tier.tag; }
                    @Override public String getPlainName() {
                        return MiniMessage.miniMessage().stripTags(tier.tag);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isModuleEnabled(NKRModule module) {
        return switch (module) {
            case ANTI_FARM     -> Settings.MODULE_ANTI_FARM;
            case BOUNTY        -> Settings.MODULE_BOUNTY;
            case DECAY         -> Settings.MODULE_DECAY;
            case PROTECTION    -> Settings.MODULE_PROTECTION;
            case REVENGER      -> Settings.MODULE_REVENGER;
            case STREAKS       -> Settings.MODULE_STREAKS;
            case PLACEHOLDERAPI -> Settings.MODULE_PLACEHOLDERAPI;
            case WEBHOOK       -> Settings.MODULE_WEBHOOK;
            case PVPMANAGER    -> Settings.MODULE_PVPMANAGER;
        };
    }

    @Override
    public boolean isNewbie(UUID uuid) {
        PlayerData data = PluginDataManager.getPlayerDatabase(uuid.toString());
        if (data == null) return false;
        return EloManager.getInstance().isNewbie(data);
    }

    @Override
    public boolean isSongSot(UUID uuid) {
        PlayerData data = PluginDataManager.getPlayerDatabase(uuid.toString());
        if (data == null) return false;
        return RankManager.getInstance().isSongSot(data);
    }

    @Override
    public boolean isWeak(UUID uuid) {
        PlayerData data = PluginDataManager.getPlayerDatabase(uuid.toString());
        if (data == null) return false;
        return RankManager.getInstance().isWeak(data);
    }

    @Override
    public int getTotalBounty(UUID uuid) {
        PlayerData data = PluginDataManager.getPlayerDatabase(uuid.toString());
        if (data == null) return 0;
        return BountyManager.getInstance().getTotalBounty(data);
    }

    @Override
    public boolean hasBounty(UUID uuid) {
        return getTotalBounty(uuid) > 0;
    }

    @Override
    public Optional<IKillResult> previewKill(UUID killerUuid, UUID victimUuid) {
        Player killer = Bukkit.getPlayer(killerUuid);
        Player victim = Bukkit.getPlayer(victimUuid);
        if (killer == null || victim == null) return Optional.empty();

        PlayerData killerData = PluginDataManager.getPlayerDatabase(killerUuid.toString());
        PlayerData victimData = PluginDataManager.getPlayerDatabase(victimUuid.toString());
        if (killerData == null || victimData == null) return Optional.empty();

        if (EloManager.getInstance().isNewbie(killerData)
                || EloManager.getInstance().isNewbie(victimData)) {
            return Optional.empty();
        }

        return Optional.of(EloManager.getInstance().calculateBreakdown(killer, killerData, victimData));
    }

    @Override
    public int getStartElo() { return Settings.ELO_START; }

    @Override
    public int getMinElo() { return Settings.ELO_MIN; }
}
