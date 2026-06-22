package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.api.event.NKRBountyClaimedEvent;
import me.ihqqq.notkillrank.api.event.NKRBountyExpiredEvent;
import me.ihqqq.notkillrank.api.event.NKRBountyPlacedEvent;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.storage.PluginDataStorage;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyManager {

    private static BountyManager instance;

    public BountyManager() {
        instance = this;
    }

    public static BountyManager getInstance() {
        return instance;
    }

    public boolean placeBounty(Player placer, Player target, int amount) {
        int minAmount = Settings.BOUNTY_MIN_AMOUNT;
        if (amount < minAmount) {
            MessageUtil.sendMessage(placer, MessageUtil.getMessage("bounty-min-amount",
                            "<red>⚠ Số elo bounty tối thiểu là <yellow>{min}<red>!")
                    .replace("{min}", String.valueOf(minAmount)));
            return false;
        }

        PlayerData placerData = PluginDataManager.getOrCreate(placer);
        int currentElo = placerData.getElo();

        if (currentElo <= Settings.ELO_MIN) {
            MessageUtil.sendMessage(placer, MessageUtil.getMessage("bounty-no-elo",
                            "<red>⚠ Bạn không đủ elo để đặt bounty! Elo hiện tại: <yellow>{elo}")
                    .replace("{elo}", String.valueOf(currentElo)));
            return false;
        }

        int actualAmount = currentElo - Math.max(Settings.ELO_MIN, currentElo - amount);

        if (actualAmount < minAmount) {
            MessageUtil.sendMessage(placer, MessageUtil.getMessage("bounty-not-enough-elo",
                            "<red>⚠ Bạn không đủ elo! Cần ít nhất <yellow>{required} elo <red>để đặt bounty tối thiểu <yellow>{min}<red>. Elo hiện tại: <yellow>{elo}")
                    .replace("{required}", String.valueOf(Settings.ELO_MIN + minAmount))
                    .replace("{min}", String.valueOf(minAmount))
                    .replace("{elo}", String.valueOf(currentElo)));
            return false;
        }

        NKRBountyPlacedEvent event = new NKRBountyPlacedEvent(placer, target, actualAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        actualAmount = event.getAmount();

        String placerUuid = placer.getUniqueId().toString();
        String targetUUID = target.getUniqueId().toString();
        PlayerData targetData = PluginDataManager.getOrCreate(target);

        placerData.setElo(currentElo - actualAmount);

        int current = targetData.getBounties().getOrDefault(placerUuid, 0);
        targetData.getBounties().put(placerUuid, current + actualAmount);
        targetData.getBountyTimestamps().put(placerUuid, System.currentTimeMillis());

        final int finalAmount = actualAmount;
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            PluginDataManager.savePlayerDatabaseToStorage(placerUuid);
            PluginDataManager.savePlayerDatabaseToStorage(targetUUID);
        });

        String msg = MessageUtil.getMessage("bounty-placed",
                        "<gold>[Bounty] <white>{placer} <white>đã đặt truy nã <green>{amount} elo "
                                + "<white>lên đầu <red>{target}<white>!")
                .replace("{placer}", placer.getName())
                .replace("{amount}", String.valueOf(finalAmount))
                .replace("{target}", target.getName());
        MessageUtil.sendBroadcast(msg);
        return true;
    }

    public void expireBounties(PlayerData targetData, String targetName) {
        if (Settings.BOUNTY_EXPIRE_HOURS <= 0) return;

        long expireMs = (long) Settings.BOUNTY_EXPIRE_HOURS * 60 * 60 * 1000;
        long now = System.currentTimeMillis();

        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, Long> entry : targetData.getBountyTimestamps().entrySet()) {
            String placerUuid = entry.getKey();
            long placedAt = entry.getValue();
            if (now - placedAt >= expireMs && targetData.getBounties().containsKey(placerUuid)) {
                expired.add(placerUuid);
            }
        }

        if (expired.isEmpty()) return;

        for (String placerUuid : expired) {
            int refundAmount = targetData.getBounties().getOrDefault(placerUuid, 0);
            if (refundAmount <= 0) continue;

            targetData.getBounties().remove(placerUuid);
            targetData.getBountyTimestamps().remove(placerUuid);

            refundToPlacer(placerUuid, refundAmount, targetName);
        }

        final String targetUuid = targetData.getUUID();
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin,
                () -> PluginDataManager.savePlayerDatabaseToStorage(targetUuid));
    }

    private void refundToPlacer(String placerUuid, int amount, String targetName) {
        String expiredMsg = MessageUtil.getMessage("bounty-expired",
                        "<gold>[Bounty] <gray>Bounty <green>{amount} elo <gray>lên đầu <red>{target} "
                                + "<gray>đã hết hạn — elo đã được hoàn trả")
                .replace("{amount}", String.valueOf(amount))
                .replace("{target}", targetName);

        UUID placerUUID;
        try {
            placerUUID = UUID.fromString(placerUuid);
        } catch (IllegalArgumentException e) {
            return;
        }

        Bukkit.getPluginManager().callEvent(new NKRBountyExpiredEvent(placerUUID, targetName, amount));

        Player onlinePlacer = Bukkit.getPlayer(placerUUID);

        if (onlinePlacer != null && onlinePlacer.isOnline()) {
            final Player finalPlacer = onlinePlacer;
            Bukkit.getScheduler().runTask(NotKillRank.plugin, () -> {
                PlayerData placerData = PluginDataManager.getPlayerDatabase(placerUuid);
                if (placerData != null) {
                    placerData.setElo(placerData.getElo() + amount);
                    if (placerData.getElo() > placerData.getPeakElo()) {
                        placerData.setPeakElo(placerData.getElo());
                    }
                    MessageUtil.sendMessage(finalPlacer, expiredMsg);
                    Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin,
                            () -> PluginDataManager.savePlayerDatabaseToStorage(placerUuid));
                }
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
                PlayerData diskData = PluginDataStorage.getPlayerData(placerUuid);
                if (diskData != null) {
                    diskData.setElo(diskData.getElo() + amount);
                    if (diskData.getElo() > diskData.getPeakElo()) {
                        diskData.setPeakElo(diskData.getElo());
                    }
                    PluginDataStorage.savePlayerData(placerUuid, diskData);
                }
            });
        }
    }

    public int getTotalBounty(PlayerData data) {
        return data.getBounties().values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean hasBounty(PlayerData data) {
        return getTotalBounty(data) > 0;
    }

    public void claimBounties(Player claimer, PlayerData killerData, PlayerData targetData) {
        int total = getTotalBounty(targetData);
        if (total <= 0) return;

        killerData.setElo(killerData.getElo() + total);
        if (killerData.getElo() > killerData.getPeakElo()) {
            killerData.setPeakElo(killerData.getElo());
        }

        targetData.getBounties().clear();
        targetData.getBountyTimestamps().clear();

        NKRBountyClaimedEvent event = new NKRBountyClaimedEvent(claimer, targetData, total);
        Bukkit.getPluginManager().callEvent(event);

        String msg = MessageUtil.getMessage("bounty-claimed",
                        "<gold>[Bounty] <white>{claimer} <white>đã nhận thưởng <green>{amount} elo "
                                + "<white>từ truy nã <red>{target}<white>!")
                .replace("{claimer}", claimer.getName())
                .replace("{amount}", String.valueOf(total))
                .replace("{target}", targetData.getName());
        MessageUtil.sendBroadcast(msg);
    }
}
