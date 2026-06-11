package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.config.ConfigManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
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
        int minAmount = ConfigManager.getInstance().getBountyConfig().getInt("min-amount", 100);
        if (amount < minAmount) {
            MessageUtil.sendMessage(placer, "<red>So luong elo bounty toi thieu la <yellow>"
                    + minAmount + "<red>!");
            return false;
        }

        PlayerData placerData = DataManager.getInstance().getOrCreate(placer);
        if (placerData.getElo() < amount) {
            MessageUtil.sendMessage(placer, "<red>Ban khong du elo! Elo hien tai: <yellow>"
                    + placerData.getElo());
            return false;
        }

        String targetUUID = target.getUniqueId().toString();
        PlayerData targetData = DataManager.getInstance().getOrCreate(target);

        int minElo = ConfigManager.getInstance().getEloConfig().getInt("min-elo", 0);
        placerData.setElo(Math.max(minElo, placerData.getElo() - amount));

        int current = targetData.getBounties().getOrDefault(placer.getUniqueId().toString(), 0);
        targetData.getBounties().put(placer.getUniqueId().toString(), current + amount);

        DataManager.getInstance().save(placer.getUniqueId().toString());
        DataManager.getInstance().save(targetUUID);

        String msg = MessageUtil.getMessage("bounty-placed",
                        "<gold>[Bounty] <white>{placer} <white>da dat truy na <green>{amount} elo <white>len dau <red>{target}<white>!")
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

        String msg = MessageUtil.getMessage("bounty-claimed",
                        "<gold>[Bounty] <white>{claimer} <white>da nhan thuong <green>{amount} elo <white>tu truy na <red>{target}<white>!")
                .replace("{claimer}", claimer.getName())
                .replace("{amount}", String.valueOf(total))
                .replace("{target}", targetData.getName());
        MessageUtil.sendBroadcast(msg);
    }
}
