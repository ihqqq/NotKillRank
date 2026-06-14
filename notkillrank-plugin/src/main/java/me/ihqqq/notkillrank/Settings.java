package me.ihqqq.notkillrank;

import me.ihqqq.notkillrank.enums.StorageType;
import me.ihqqq.notkillrank.file.module.*;

public class Settings {
    public static StorageType STORAGE_TYPE;
    public static String STORAGE_SQLITE_FILE;
    public static String STORAGE_H2_FILE;

    public static boolean DATABASE_AUTO_SAVE_ENABLED;
    public static int DATABASE_AUTO_SAVE_SECONDS;

    public static boolean MODULE_ANTI_FARM;
    public static boolean MODULE_BOUNTY;
    public static boolean MODULE_DECAY;
    public static boolean MODULE_PROTECTION;
    public static boolean MODULE_STREAKS;
    public static boolean MODULE_PLACEHOLDERAPI;
    public static boolean MODULE_WEBHOOK;
    public static boolean MODULE_PVPMANAGER;

    public static int ELO_START;
    public static int ELO_KILL_PERCENT;
    public static int ELO_MIN;
    public static double ELO_HIGH_MULTIPLIER;
    public static double ELO_LOW_MULTIPLIER;
    public static int ELO_REVENGE_WINDOW_SECONDS;
    public static int ELO_REVENGE_BONUS_PERCENT;

    public static int ANTI_FARM_LIMIT_KILLS_PER_HOUR;

    public static int PROTECTION_NEWBIE_HOURS;
    public static int PROTECTION_NEWBIE_ELO;

    public static int DECAY_OFFLINE_DAYS;
    public static int DECAY_DAILY_PERCENT;
    public static int DECAY_MAX_PERCENT;

    public static boolean STREAKS_RESET_ON_LOGOUT;

    public static int BOUNTY_MIN_AMOUNT;
    public static int BOUNTY_EXPIRE_HOURS;

    public static void setupValue() {
        var main = NotKillRank.plugin.getConfig();

        STORAGE_TYPE        = StorageType.fromString(main.getString("storage.type", "YAML"));
        STORAGE_SQLITE_FILE = main.getString("storage.sqlite.file", "playerdata");
        STORAGE_H2_FILE     = main.getString("storage.h2.file", "playerdata");

        DATABASE_AUTO_SAVE_ENABLED = main.getBoolean("auto-save.enabled", true);
        DATABASE_AUTO_SAVE_SECONDS = main.getInt("auto-save.interval-seconds", 300);

        MODULE_ANTI_FARM    = main.getBoolean("modules.enabled.anti-farm", true);
        MODULE_BOUNTY       = main.getBoolean("modules.enabled.bounty", true);
        MODULE_DECAY        = main.getBoolean("modules.enabled.decay", true);
        MODULE_PROTECTION   = main.getBoolean("modules.enabled.protection", true);
        MODULE_STREAKS      = main.getBoolean("modules.enabled.streaks", true);
        MODULE_PLACEHOLDERAPI = main.getBoolean("modules.enabled.placeholderapi", true);
        MODULE_WEBHOOK      = main.getBoolean("modules.enabled.webhook", false);
        MODULE_PVPMANAGER   = main.getBoolean("modules.enabled.pvpmanager", true);

        var elo = EloFile.get();
        ELO_START                = elo.getInt("start-elo", 1000);
        ELO_KILL_PERCENT         = elo.getInt("kill-percent", 10);
        ELO_MIN                  = elo.getInt("min-elo", 0);
        ELO_HIGH_MULTIPLIER      = elo.getDouble("high-elo-multiplier", 1.5);
        ELO_LOW_MULTIPLIER       = elo.getDouble("low-elo-multiplier", 0.5);
        ELO_REVENGE_WINDOW_SECONDS = elo.getInt("revenge-window-seconds", 300);
        ELO_REVENGE_BONUS_PERCENT  = elo.getInt("revenge-bonus-percent", 20);

        ANTI_FARM_LIMIT_KILLS_PER_HOUR = AntiFarmFile.get().getInt("limit-kills-per-hour", 3);

        PROTECTION_NEWBIE_HOURS = ProtectionFile.get().getInt("newbie-hours", 10);
        PROTECTION_NEWBIE_ELO   = ProtectionFile.get().getInt("newbie-protect-elo", 100);

        DECAY_OFFLINE_DAYS  = DecayFile.get().getInt("offline-days", 7);
        DECAY_DAILY_PERCENT = DecayFile.get().getInt("daily-percent", 1);
        DECAY_MAX_PERCENT   = DecayFile.get().getInt("max-percent", 10);

        STREAKS_RESET_ON_LOGOUT = StreaksFile.get().getBoolean("reset-on-logout", true);

        BOUNTY_MIN_AMOUNT    = BountyFile.get().getInt("min-amount", 100);
        BOUNTY_EXPIRE_HOURS  = BountyFile.get().getInt("expire-hours", 24);
    }
}
