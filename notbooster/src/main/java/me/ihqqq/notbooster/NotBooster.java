package me.ihqqq.notbooster;

import me.ihqqq.notbooster.command.NotBoosterCommand;
import me.ihqqq.notbooster.inventory.BoosterInventory;
import me.ihqqq.notbooster.file.BoostersFile;
import me.ihqqq.notbooster.file.ConfigFile;
import me.ihqqq.notbooster.file.MessagesFile;
import me.ihqqq.notbooster.hook.catamines.CataminesHook;
import me.ihqqq.notbooster.listener.EloBoostListener;
import me.ihqqq.notbooster.listener.ExpBoostListener;
import me.ihqqq.notbooster.listener.ItemBoostListener;
import me.ihqqq.notbooster.manager.BoosterManager;
import me.ihqqq.notbooster.storage.BoosterStorageFactory;
import me.ihqqq.notbooster.support.PlaceholderAPISupport;
import me.ihqqq.notbooster.task.BoosterExpireTask;
import me.ihqqq.notbooster.task.EffectBoostTask;
import me.ihqqq.notkillrank.api.NKRProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class NotBooster extends JavaPlugin {

    public static NotBooster plugin;

    private EffectBoostTask effectBoostTask;
    private BoosterExpireTask boosterExpireTask;
    private BoosterInventory boosterInventory;

    @Override
    public void onEnable() {
        plugin = this;

        initFileClasses();
        Settings.setupValue();

        if (!NKRProvider.isAvailable()) {
            getLogger().severe("NotKillRank API is not available. Disabling NotBooster.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initManagers();
        registerHooks();
        registerListeners();
        registerCommands();
        registerTasks();

        getLogger().info("NotBooster enabled with booster engine and NotKillRank API hook.");
    }

    @Override
    public void onDisable() {
        if (effectBoostTask != null) effectBoostTask.stop();
        if (boosterExpireTask != null) boosterExpireTask.stop();
        if (BoosterManager.getInstance() != null) BoosterManager.getInstance().flushSave();
        getLogger().info("NotBooster disabled.");
        plugin = null;
    }

    public static void reload() {
        initFileClasses();
        Settings.setupValue();
        if (BoosterManager.getInstance() != null) {
            BoosterManager.getInstance().setStackingMode(Settings.STACKING_MODE);
        }
    }

    private static void initFileClasses() {
        ConfigFile.init();
        MessagesFile.init();
        BoostersFile.init();
    }

    private void initManagers() {
        BoosterManager boosterManager = new BoosterManager(BoosterStorageFactory.create(Settings.STORAGE_TYPE));
        boosterManager.setStackingMode(Settings.STACKING_MODE);
        boosterManager.load();
    }

    private void registerHooks() {
        if (Settings.CATAMINES_ENABLED) new CataminesHook();
    }

    private void registerListeners() {
        new EloBoostListener();
        new ExpBoostListener();
        new ItemBoostListener();
        boosterInventory = new BoosterInventory();
    }

    private void registerCommands() {
        new NotBoosterCommand(boosterInventory);
        if (Settings.PLACEHOLDERAPI_ENABLED && getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPISupport().register();
        }
    }

    private void registerTasks() {
        effectBoostTask = new EffectBoostTask();
        effectBoostTask.start();
        boosterExpireTask = new BoosterExpireTask();
        boosterExpireTask.start();
    }
}
