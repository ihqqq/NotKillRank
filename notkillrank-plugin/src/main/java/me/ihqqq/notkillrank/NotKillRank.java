package me.ihqqq.notkillrank;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.ihqqq.notkillrank.command.*;
import me.ihqqq.notkillrank.file.module.*;
import me.ihqqq.notkillrank.hook.PvPManagerHook;
import me.ihqqq.notkillrank.webhook.SkinUtil;
import me.ihqqq.notkillrank.webhook.WebhookManager;
import me.ihqqq.notkillrank.inventory.StatsInventory;
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
import me.ihqqq.notkillrank.task.BountyExpireTask;
import me.ihqqq.notkillrank.task.EloDecayTask;
import me.ihqqq.notkillrank.task.NewbieProtectionTask;
import me.ihqqq.notkillrank.task.TopEloWebhookTask;
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
        new RevengerManager();
        new RankManager();
        new StreakManager();
        new BountyManager();
        new WebhookManager();
        SkinUtil.init();

        if (Settings.MODULE_PVPMANAGER) {
            new PvPManagerHook();
        }

        registerListeners();
        registerCommands();
        registerTasks();

        boolean papiEnabled = false;
        if (Settings.MODULE_PLACEHOLDERAPI
                && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPISupport().register();
            papiEnabled = true;
        }

        printEnableBanner(papiEnabled);
    }

    @Override
    public void onDisable() {
        printDisableBanner();
        PluginDataManager.saveAllDatabase();
        PluginDataStorage.close();
    }

    public static void reload() {
        plugin.reloadConfig();
        initFileClasses();
        Settings.setupValue();
        Messages.setupValue();
        ModuleManager.reload();
        RankManager.reload();
        if (StreakManager.getInstance() != null) StreakManager.getInstance().invalidateMilestoneCache();
        TopInventory.invalidateTitleCache();
        PluginDataManager.invalidateTopCache();
        AutoSaveTask.scheduleOrRestart();
        BountyExpireTask.scheduleOrRestart();
        EloDecayTask.scheduleOrRestart();
        TopEloWebhookTask.scheduleOrRestart();
        printReloadBanner();
    }

    private void printEnableBanner(boolean papiRegistered) {
        String ver     = getDescription().getVersion();
        String authors = String.join(", ", getDescription().getAuthors());
        String storage = Settings.STORAGE_TYPE.name();

        String pvpmStatus = (PvPManagerHook.getInstance() != null && PvPManagerHook.getInstance().isHooked())
                ? "<green>Hoạt động"
                : (Settings.MODULE_PVPMANAGER ? "<yellow>Bật <dark_gray>(PvPManager chưa cài)" : "<red>Tắt");
        String papiStatus = papiRegistered
                ? "<green>Hoạt động <dark_gray>(expansion đã đăng ký)"
                : "<gray>Không tìm thấy <dark_gray>(bỏ qua)";

        String sep = "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        MessageUtil.log(sep);
        MessageUtil.log("");
        MessageUtil.log("    <bold><aqua>NotKill<dark_aqua>Rank</dark_aqua></bold>  <dark_gray>» <gray>v" + ver);
        MessageUtil.log("    <gray>Plugin thuộc sở hữu của <yellow><bold>NotMC</bold></yellow> <dark_gray>| <gray>Tác giả<dark_gray>: <green>" + authors);
        MessageUtil.log("");
        MessageUtil.log(sep);
        MessageUtil.log("");
        MessageUtil.log("  <dark_gray>» <gray>Lưu trữ        <dark_gray>: <aqua>" + storage);
        MessageUtil.log("  <dark_gray>» <gray>PvPManager      <dark_gray>: " + pvpmStatus);
        MessageUtil.log("  <dark_gray>» <gray>PlaceholderAPI  <dark_gray>: " + papiStatus);
        MessageUtil.log("");
        MessageUtil.log("  <dark_gray>» <gray>Modules<dark_gray>:");

        for (ModuleManager.Module m : ModuleManager.Module.values()) {
            String label  = formatModuleName(m.name());
            String status = m.isEnabled() ? "<green>BẬT" : "<red>TẮT";
            MessageUtil.log("     <gray>" + label + " <dark_gray>— " + status);
        }

        MessageUtil.log("");
        MessageUtil.log(sep);
        MessageUtil.log("  <green>✔ NotKillRank đã khởi động thành công!");
        MessageUtil.log(sep);
    }

    private static void printDisableBanner() {
        String sep = "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        MessageUtil.log(sep);
        MessageUtil.log("  <red>✘ NotKillRank đang tắt...");
        MessageUtil.log("  <gray>Đang lưu dữ liệu người chơi...");
        MessageUtil.log("  <dark_gray>Plugin thuộc sở hữu của <yellow>NotMC</yellow> <dark_gray>— Leak = chó rách!");
        MessageUtil.log(sep);
    }

    private static void printReloadBanner() {
        String sep = "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        MessageUtil.log(sep);
        MessageUtil.log("  <yellow>↺  NotKillRank — Tải lại cấu hình");
        MessageUtil.log("");
        for (ModuleManager.Module m : ModuleManager.Module.values()) {
            String label  = formatModuleName(m.name());
            String status = m.isEnabled() ? "<green>✔ BẬT" : "<red>✘ TẮT";
            MessageUtil.log("     <gray>" + label + " <dark_gray>— " + status);
        }
        MessageUtil.log("");
        MessageUtil.log("  <green>Hoàn tất!");
        MessageUtil.log(sep);
    }

    private static String formatModuleName(String enumName) {
        return switch (enumName) {
            case "ANTI_FARM"    -> "Anti-Farm      ";
            case "BOUNTY"       -> "Bounty         ";
            case "DECAY"        -> "Elo Decay      ";
            case "PROTECTION"   -> "Bảo vệ người mới";
            case "REVENGER"     -> "Báo thù        ";
            case "STREAKS"      -> "Kill Streaks   ";
            case "PLACEHOLDERAPI" -> "PlaceholderAPI ";
            case "WEBHOOK"      -> "Webhook        ";
            case "PVPMANAGER"   -> "PvPManager     ";
            default             -> enumName;
        };
    }

    private static void initFileClasses() {
        MessagesFile.init();
        TopGuiFile.init();
        StatsGuiFile.init();
        EloFile.init();
        RevengerFile.init();
        AntiFarmFile.init();
        BountyFile.init();
        DecayFile.init();
        ProtectionFile.init();
        RanksFile.init();
        StreaksFile.init();
        WebhookFile.init();
        PvPManagerFile.init();
    }

    private void registerListeners() {
        new CombatListener();
        new PlayerJoinListener();
        new PlayerQuitListener();
        new TopInventory();
        new StatsInventory();
    }

    private void registerCommands() {
        new EloCommand();
        new TopCommand();
        new StatsCommand();
        new BountyCommand();
        new AdminCommand();
    }

    private void registerTasks() {
        AutoSaveTask.scheduleOrRestart();
        EloDecayTask.scheduleOrRestart();
        new NewbieProtectionTask();
        BountyExpireTask.scheduleOrRestart();
        TopEloWebhookTask.scheduleOrRestart();
    }
}