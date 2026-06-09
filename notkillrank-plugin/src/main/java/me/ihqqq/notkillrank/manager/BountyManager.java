package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.Map;

public class BountyManager {

    private static BountyManager instance;

    public BountyManager() {
        instance = this;
    }

    public static BountyManager getInstance() {
        return instance;
    }

    public boolean placeBounty(Player placer, Player target, int amount) {
        int minAmount = NotKillRank.getInstance().getConfig().getInt("bounty.min-amount", 100);
        if (amount < minAmount) {
            MessageUtil.sendMessage(placer, "&cSo luong elo bounty toi thieu la &e" + minAmount + "&c!");
            return false;
        }

        PlayerData placerData = DataManager.getInstance().getOrCreate(placer);
        if (placerData.getElo() < amount) {
            MessageUtil.sendMessage(placer, "&cBan khong du elo! Elo hien tai: &e" + placerData.getElo());
            return false;
        }

        String targetUUID = target.getUniqueId().toString();
        PlayerData targetData = DataManager.getInstance().getOrCreate(target);

        placerData.setElo(Math.max(
                NotKillRank.getInstance().getConfig().getInt("elo.min-elo", 0),
                placerData.getElo() - amount));

        int current = targetData.getBounties().getOrDefault(placer.getUniqueId().toString(), 0);
        targetData.getBounties().put(placer.getUniqueId().toString(), current + amount);

        DataManager.getInstance().save(placer.getUniqueId().toString());
        DataManager.getInstance().save(targetUUID);

        String msg = NotKillRank.getInstance().getConfig()
                .getString("messages.bounty-placed",
                        "&6[Bounty] &f{placer} &fda dat truy na &a{amount} elo &flen &c{target}&f!")
                .replace("{placer}", placer.getName())
                .replace("{amount}", String.valueOf(amount))
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

    public void claimBounties(Player claimer, PlayerData targetData) {
        int total = getTotalBounty(targetData);
        if (total <= 0) return;

        PlayerData claimerData = DataManager.getInstance().getOrCreate(claimer);
        claimerData.setElo(claimerData.getElo() + total);
        if (claimerData.getElo() > claimerData.getPeakElo()) {
            claimerData.setPeakElo(claimerData.getElo());
        }

        targetData.getBounties().clear();
        DataManager.getInstance().save(claimer.getUniqueId().toString());

        String msg = NotKillRank.getInstance().getConfig()
                .getString("messages.bounty-claimed",
                        "&6[Bounty] &f{claimer} &fda nhan thuong &a{amount} elo &ftu truy na &c{target}&f!")
                .replace("{claimer}", claimer.getName())
                .replace("{amount}", String.valueOf(total))
                .replace("{target}", targetData.getName());
        MessageUtil.sendBroadcast(msg);
    }
}
