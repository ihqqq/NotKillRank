package me.ihqqq.notkillrank;

import me.ihqqq.notkillrank.command.*;
import me.ihqqq.notkillrank.inventory.TopInventory;
import me.ihqqq.notkillrank.listener.CombatListener;
import me.ihqqq.notkillrank.listener.PlayerJoinListener;
import me.ihqqq.notkillrank.listener.PlayerQuitListener;
import me.ihqqq.notkillrank.manager.*;
import me.ihqqq.notkillrank.support.PlaceholderAPISupport;
import me.ihqqq.notkillrank.task.AutoSaveTask;
import me.ihqqq.notkillrank.task.EloDecayTask;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NotKillRank extends JavaPlugin {

    private static NotKillRank instance;

    private DataManager dataManager;
    private EloManager eloManager;
    private RankManager rankManager;
    private StreakManager streakManager;
    private BountyManager bountyManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        dataManager = new DataManager();
        rankManager = new RankManager();
        eloManager = new EloManager();
        streakManager = new StreakManager();
        bountyManager = new BountyManager();

        registerListeners();
        registerCommands();
        registerTasks();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPISupport().register();
            MessageUtil.log("&aPlaceholderAPI detected — expansion registered.");
        }

        MessageUtil.log("&aNotKillRank &fv" + getDescription().getVersion() + " &aloaded successfully!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        MessageUtil.log("&cNotKillRank disabled. All data saved.");
    }

    private void registerListeners() {
        new CombatListener();
        new PlayerJoinListener();
        new PlayerQuitListener();
        new TopInventory();
    }

    private void registerCommands() {
        new EloCommand();
        new TopCommand();
        new StatsCommand();
        new BountyCommand();
        new AdminCommand();
    }

    private void registerTasks() {
        new AutoSaveTask();
        new EloDecayTask();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::updateTop1Status,
                20L * 60 * 5, 20L * 60 * 5);
    }

    private void updateTop1Status() {
        var top = dataManager.getTopPlayers(1);
        if (top.isEmpty()) return;
        var top1 = top.get(0);

        for (var data : dataManager.getCache().values()) {
            if (data.getUUID().equals(top1.getUUID())) {
                if (top1.getTop1Since() <= 0) {
                    top1.setTop1Since(System.currentTimeMillis());
                }
            } else {
                if (data.getTop1Since() > 0) {
                    data.setTop1Since(0);
                }
            }
        }
    }

    public static NotKillRank getInstance() {
        return instance;
    }

    public DataManager getDataManager() { return dataManager; }
    public EloManager getEloManager() { return eloManager; }
    public RankManager getRankManager() { return rankManager; }
    public StreakManager getStreakManager() { return streakManager; }
    public BountyManager getBountyManager() { return bountyManager; }
}
