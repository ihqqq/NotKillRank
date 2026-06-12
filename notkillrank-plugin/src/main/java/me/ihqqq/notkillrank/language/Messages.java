package me.ihqqq.notkillrank.language;

import me.ihqqq.notkillrank.file.module.MessagesFile;
import org.bukkit.configuration.file.FileConfiguration;

public class Messages {

    public static String PREFIX;
    public static String NO_PERMISSION;
    public static String PLAYER_NOT_FOUND;
    public static String ELO_INFO;
    public static String STATS;
    public static String TOP;
    public static String KILL_BROADCAST;
    public static String STREAK_BREAK;
    public static String BOUNTY_PLACED;
    public static String BOUNTY_CLAIMED;
    public static String BOUNTY_TARGET_PROTECTED;
    public static String ANTI_FARM;
    public static String WELCOME;

    public static void setupValue() {
        var cfg = MessagesFile.get();

        PREFIX               = str(cfg, "prefix",               BuiltInMessages.PREFIX);
        NO_PERMISSION        = str(cfg, "no-permission",         BuiltInMessages.NO_PERMISSION);
        PLAYER_NOT_FOUND     = str(cfg, "player-not-found",      BuiltInMessages.PLAYER_NOT_FOUND);
        ELO_INFO             = str(cfg, "elo-info",              BuiltInMessages.ELO_INFO);
        STATS                = str(cfg, "stats",                 BuiltInMessages.STATS);
        TOP                  = str(cfg, "top",                   BuiltInMessages.TOP);
        KILL_BROADCAST       = str(cfg, "kill-broadcast",        BuiltInMessages.KILL_BROADCAST);
        STREAK_BREAK         = str(cfg, "streak-break",          BuiltInMessages.STREAK_BREAK);
        BOUNTY_PLACED        = str(cfg, "bounty-placed",         BuiltInMessages.BOUNTY_PLACED);
        BOUNTY_CLAIMED       = str(cfg, "bounty-claimed",        BuiltInMessages.BOUNTY_CLAIMED);
        BOUNTY_TARGET_PROTECTED = str(cfg, "bounty-target-protected", BuiltInMessages.BOUNTY_TARGET_PROTECTED);
        ANTI_FARM            = str(cfg, "anti-farm",             BuiltInMessages.ANTI_FARM);
        WELCOME              = str(cfg, "welcome",               BuiltInMessages.WELCOME);
    }

    private static String str(FileConfiguration cfg, String key, String def) {
        if (cfg == null) return def;
        if (cfg.isList(key)) {
            var lines = cfg.getStringList(key);
            return lines.isEmpty() ? def : String.join("\n", lines);
        }
        return cfg.getString(key, def);
    }
}
