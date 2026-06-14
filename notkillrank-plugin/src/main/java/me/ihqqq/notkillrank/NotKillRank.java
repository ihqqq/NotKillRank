package me.ihqqq.notkillrank;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.ihqqq.notkillrank.command.*;
import me.ihqqq.notkillrank.file.module.*;
import me.ihqqq.notkillrank.webhook.SkinUtil;
import me.ihqqq.notkillrank.webhook.WebhookManager;
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
        new WebhookManager();
        SkinUtil.init();

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
        printReloadBanner();
    }

    private void printEnableBanner(boolean papiRegistered) {
        String ver     = getDescription().getVersion();
        String authors = String.join(", ", getDescription().getAuthors());
        String storage = Settings.STORAGE_TYPE.name();

        String papiStatus = papiRegistered
                ? "&aHoạt động &8(expansion đã đăng ký)"
                : "&7Không tìm thấy &8(bỏ qua)";

        String sep = "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        MessageUtil.log(sep);
        MessageUtil.log("&r");
        MessageUtil.log("&r    &b&lNotKill&3&lRank  &8» &7v" + ver);
        MessageUtil.log("&r    &7Plugin thuộc sở hữu của &e&lNotMC &8| &7Tác giả&8: &a" + authors);
        MessageUtil.log("&r");
        MessageUtil.log(sep);
        MessageUtil.log("&r");
        MessageUtil.log("&r  &8» &7Lưu trữ        &8: &b" + storage);
        MessageUtil.log("&r  &8» &7PlaceholderAPI  &8: " + papiStatus);
        MessageUtil.log("&r");
        MessageUtil.log("&r  &8» &7Modules&8:");

        for (ModuleManager.Module m : ModuleManager.Module.values()) {
            String label  = formatModuleName(m.name());
            String status = m.isEnabled() ? "&a BẬT" : "&c TẮT";
            MessageUtil.log("&r     &7" + label + " &8— " + status);
        }

        MessageUtil.log("&r");
        MessageUtil.log(sep);
        MessageUtil.log("&r  &a✔ NotKillRank đã khởi động thành công!");
        MessageUtil.log(sep);
    }

    private static void printDisableBanner() {
        String sep = "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        MessageUtil.log(sep);
        MessageUtil.log("&r  &c✘ NotKillRank đang tắt...");
        MessageUtil.log("&r  &7Đang lưu dữ liệu người chơi...");
        MessageUtil.log("&r  &8Plugin thuộc sở hữu của &eNotMC &8— Leak = chó rách!");
        MessageUtil.log(sep);
    }

    private static void printReloadBanner() {
        String sep = "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        MessageUtil.log(sep);
        MessageUtil.log("&r  &e↺  NotKillRank — Tải lại cấu hình");
        MessageUtil.log("&r");
        for (ModuleManager.Module m : ModuleManager.Module.values()) {
            String label  = formatModuleName(m.name());
            String status = m.isEnabled() ? "&a✔ BẬT" : "&c✘ TẮT";
            MessageUtil.log("&r     &7" + label + " &8— " + status);
        }
        MessageUtil.log("&r");
        MessageUtil.log("&r  &aHoàn tất!");
        MessageUtil.log(sep);
    }

    private static String formatModuleName(String enumName) {
        return switch (enumName) {
            case "ANTI_FARM"    -> "Anti-Farm      ";
            case "BOUNTY"       -> "Bounty         ";
            case "DECAY"        -> "Elo Decay      ";
            case "PROTECTION"   -> "Bảo vệ người mới";
            case "STREAKS"      -> "Kill Streaks   ";
            case "PLACEHOLDERAPI" -> "PlaceholderAPI ";
            case "WEBHOOK"      -> "Webhook        ";
            default             -> enumName;
        };
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
        WebhookFile.init();
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
        new BountyExpireTask();

    }
}
