package me.ihqqq.notkillrank;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.ihqqq.notkillrank.command.*;
import me.ihqqq.notkillrank.file.module.MessagesFile;
import me.ihqqq.notkillrank.file.module.TopGuiFile;
import me.ihqqq.notkillrank.file.module.*;
import me.ihqqq.notkillrank.inventory.TopInventory;
import me.ihqqq.notkillrank.language.Messages;
import me.ihqqq.notkillrank.listener.CombatListener;
import me.ihqqq.notkillrank.listener.PlayerJoinListener;
import me.ihqqq.notkillrank.listener.PlayerQuitListener;
import me.ihqqq.notkillrank.manager.*;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.storage.PluginDataStorage;
import me.ihqqq.notkillrank.support.PlaceholderAPISupport;
import me.ihqqq.notkillrank.task.AutoSaveTask;
import me.ihqqq.notkillrank.task.EloDecayTask;
import me.ihqqq.notkillrank.task.NewbieProtectionTask;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public final class NotKillRank extends JavaPlugin {

    public static NotKillRank plugin;

    @Override
    public void onEnable() {
        plugin = this;

        saveDefaultConfig();
        try {
            ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"),
                    Collections.emptyList());
        } catch (IOException e) {
            getLogger().warning("Could not update config.yml: " + e.getMessage());
        }
        reloadConfig();

        initFileClasses();
        Settings.setupValue();
        Messages.setupValue();

        PluginDataStorage.init(Settings.STORAGE_TYPE);
        PluginDataManager.loadAllDatabase();

        new ModuleManager();
        new EloManager();
        new RankManager();
        new StreakManager();
        new BountyManager();

        registerListeners();
        registerCommands();
        registerTasks();

        if (Settings.MODULE_PLACEHOLDERAPI
                && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPISupport().register();
            MessageUtil.log("PlaceholderAPI detected — expansion registered.");
        }

        MessageUtil.log("NotKillRank v" + getDescription().getVersion() + " loaded successfully!");
    }

    @Override
    public void onDisable() {
        PluginDataManager.saveAllDatabase();
        PluginDataStorage.close();
        MessageUtil.log("NotKillRank disabled. All data saved.");
    }

    public static void reload() {
        plugin.reloadConfig();
        initFileClasses();
        Settings.setupValue();
        Messages.setupValue();
        ModuleManager.reload();
        RankManager.reload();
        MessageUtil.log("NotKillRank config reloaded.");
    }

    private static void initFileClasses() {
        MessagesFile.init();
        TopGuiFile.init();
        EloFile.init();
        AntiFarmFile.init();
        BountyFile.init();
        DecayFile.init();
        ProtectionFile.init();
        RanksFile.init();
        StreaksFile.init();
        VoSongFile.init();
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
        new NewbieProtectionTask();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                PluginDataManager::updateTop1Status, 20L * 60 * 5, 20L * 60 * 5);
    }
}
