package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
            MessageUtil.sendMessage(placer, "<red>Số lượng elo bounty tối thiểu là <yellow>"
                    + minAmount + "<red>!");
            return false;
        }

        PlayerData placerData = PluginDataManager.getOrCreate(placer);
        int currentElo = placerData.getElo();

        if (currentElo <= Settings.ELO_MIN) {
            MessageUtil.sendMessage(placer, "<red>Bạn không đủ elo để đặt bounty! Elo hiện tại: <yellow>"
                    + currentElo);
            return false;
        }

        int actualAmount = currentElo - Math.max(Settings.ELO_MIN, currentElo - amount);

        if (actualAmount < minAmount) {
            MessageUtil.sendMessage(placer, "<red>Bạn không đủ elo! Cần ít nhất <yellow>"
                    + (Settings.ELO_MIN + minAmount) + " elo <red>để đặt bounty tối thiểu <yellow>"
                    + minAmount + "<red>. Elo hiện tại: <yellow>" + currentElo);
            return false;
        }

        String targetUUID = target.getUniqueId().toString();
        PlayerData targetData = PluginDataManager.getOrCreate(target);

        placerData.setElo(currentElo - actualAmount);

        int current = targetData.getBounties().getOrDefault(placer.getUniqueId().toString(), 0);
        targetData.getBounties().put(placer.getUniqueId().toString(), current + actualAmount);

        final String placerUuid = placer.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            PluginDataManager.savePlayerDatabaseToStorage(placerUuid);
            PluginDataManager.savePlayerDatabaseToStorage(targetUUID);
        });

        String msg = MessageUtil.getMessage("bounty-placed",
                        "<gold>[Bounty] <white>{placer} <white>đã đặt truy nã <green>{amount} elo "
                                + "<white>lên đầu <red>{target}<white>!")
                .replace("{placer}", placer.getName())
                .replace("{amount}", String.valueOf(actualAmount))
                .replace("{target}", target.getName());
        MessageUtil.sendBroadcast(msg);
        return true;
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

        String msg = MessageUtil.getMessage("bounty-claimed",
                        "<gold>[Bounty] <white>{claimer} <white>đã nhận thưởng <green>{amount} elo "
                                + "<white>từ truy nã <red>{target}<white>!")
                .replace("{claimer}", claimer.getName())
                .replace("{amount}", String.valueOf(total))
                .replace("{target}", targetData.getName());
        MessageUtil.sendBroadcast(msg);
    }
}
