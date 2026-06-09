package me.ihqqq.notkillrank.util;

import me.ihqqq.notkillrank.NotKillRank;
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

    public static void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        player.sendMessage(parse(message));
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) return;
        sender.sendMessage(parse(message));
    }

    public static void sendBroadcast(String message) {
        if (message == null || message.isEmpty()) return;
        Bukkit.broadcast(parse(message));
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
        return NotKillRank.getInstance().getConfig().getString("messages." + path, "");
    }

    public static String getPrefix() {
        return NotKillRank.getInstance().getConfig()
                .getString("messages.prefix", "<dark_gray>[<gold>NotKillRank<dark_gray>] ");
    }

    /**
     * Converts a MiniMessage string to legacy §-format.
     * Used for contexts that still need a legacy String (e.g. old API calls).
     */
    public static String color(String text) {
        if (text == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(MM.deserialize(text));
    }
}
