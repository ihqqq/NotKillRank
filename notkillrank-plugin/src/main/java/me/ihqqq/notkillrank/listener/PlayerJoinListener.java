package me.ihqqq.notkillrank.listener;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDate;

public class PlayerJoinListener implements Listener {

    public PlayerJoinListener() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.getInstance());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.getInstance(), () -> {
            PlayerData data = DataManager.getInstance().getOrCreate(player);

            EloManager.getInstance().applyEloDecay(data);

            String today = LocalDate.now().toString();
            if (!today.equals(data.getCurrentDay())) {
                data.setCurrentDay(today);
                data.setDailyOnlineMs(0);
            }

            data.setSessionStart(System.currentTimeMillis());
            data.setLastOnline(System.currentTimeMillis());

            DataManager.getInstance().save(player.getUniqueId().toString());

            Bukkit.getScheduler().runTask(NotKillRank.getInstance(), () -> {
                if (!player.isOnline()) return;
                String rank = RankManager.getInstance().getRankTag(data.getElo());
                String streakTag = RankManager.getInstance().getStreakTag(data);
                String streakPart = streakTag.isEmpty() ? "" : " " + streakTag;
                MessageUtil.sendMessage(player,
                        MessageUtil.getPrefix()
                                + "<white>Chào mừng <yellow>" + player.getName()
                                + " <white>| Hạng: " + rank + streakPart
                                + " <white>| Elo: <green>" + data.getElo());
            });
        });
    }
}
