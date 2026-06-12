package me.ihqqq.notkillrank.util;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Component parse(String text) {
        if (text == null) return Component.empty();
        return MM.deserialize(text);
    }

    public static Component parseLore(String text) {
        if (text == null) return Component.empty();
        if (text.isEmpty()) return Component.empty();
        String lower = text.toLowerCase();
        boolean hasExplicitItalic = lower.contains("<italic>") || lower.contains("<italic:true>") || lower.contains("<i>");
        if (hasExplicitItalic) {
            return MM.deserialize(text);
        }
        return MM.deserialize("<italic:false>" + text);
    }

    public static void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        for (String line : message.split("\n", -1)) {
            player.sendMessage(parse(line));
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) return;
        for (String line : message.split("\n", -1)) {
            sender.sendMessage(parse(line));
        }
    }

    public static void sendBroadcast(String message) {
        if (message == null || message.isEmpty()) return;
        for (String line : message.split("\n", -1)) {
            Bukkit.broadcast(parse(line));
        }
    }

    public static String stripTags(String text) {
        if (text == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(MM.deserialize(text));
    }

    public static String toLegacyString(String miniMessageText) {
        if (miniMessageText == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(MM.deserialize(miniMessageText));
    }

    public static void log(String message) {
        NotKillRank.getInstance().getLogger().info(stripTags(message));
    }

    public static void warn(String message) {
        NotKillRank.getInstance().getLogger().warning(stripTags(message));
    }

    public static String getMessage(String path) {
        ConfigManager cm = ConfigManager.getInstance();
        if (cm != null) return cm.getMessage(path);
        return NotKillRank.getInstance().getConfig().getString("messages." + path, "");
    }

    public static String getMessage(String path, String def) {
        ConfigManager cm = ConfigManager.getInstance();
        if (cm != null) return cm.getMessage(path, def);
        return NotKillRank.getInstance().getConfig().getString("messages." + path, def);
    }

    public static String getPrefix() {
        return getMessage("prefix", "<dark_gray>[<gold>NotKillRank<dark_gray>] ");
    }

    public static String color(String text) {
        if (text == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(MM.deserialize(text));
    }
}