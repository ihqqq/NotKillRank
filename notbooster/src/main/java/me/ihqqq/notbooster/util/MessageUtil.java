package me.ihqqq.notbooster.util;

import me.ihqqq.notbooster.file.MessagesFile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;

public final class MessageUtil {

    private static final boolean MINIMESSAGE_AVAILABLE = isMiniMessageAvailable();

    private MessageUtil() {}

    public static void send(CommandSender sender, String path, String... replacements) {
        sendRaw(sender, getPrefix() + getMessage(path, path, replacements));
    }

    public static void broadcast(String path, String... replacements) {
        String message = getPrefix() + getMessage(path, path, replacements);
        for (CommandSender receiver : Bukkit.getOnlinePlayers()) {
            sendRaw(receiver, message);
        }
        sendRaw(Bukkit.getConsoleSender(), message);
    }

    public static String getPrefix() {
        return MessagesFile.get().getString("prefix", "<gold>NotBooster <dark_gray>» ");
    }

    public static String getMessage(String path, String fallback, String... replacements) {
        String message = MessagesFile.get().getString(path, fallback);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }

    public static String color(String input) {
        return colorLegacy(input);
    }

    private static void sendRaw(CommandSender sender, String input) {
        if (MINIMESSAGE_AVAILABLE) {
            try {
                Object miniMessage = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage")
                        .getMethod("miniMessage")
                        .invoke(null);
                Object component = miniMessage.getClass()
                        .getMethod("deserialize", String.class)
                        .invoke(miniMessage, input);
                Method sendMessage = sender.getClass().getMethod("sendMessage",
                        Class.forName("net.kyori.adventure.text.Component"));
                sendMessage.invoke(sender, component);
                return;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Fall through to legacy color codes when MiniMessage is unavailable at runtime.
            }
        }
        sender.sendMessage(colorLegacy(input));
    }

    private static boolean isMiniMessageAvailable() {
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Class.forName("net.kyori.adventure.text.Component");
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static String colorLegacy(String input) {
        return ChatColor.translateAlternateColorCodes('&', input)
                .replace("<gold>", "§6")
                .replace("</gold>", "")
                .replace("<yellow>", "§e")
                .replace("</yellow>", "")
                .replace("<gray>", "§7")
                .replace("</gray>", "")
                .replace("<dark_gray>", "§8")
                .replace("</dark_gray>", "")
                .replace("<red>", "§c")
                .replace("</red>", "")
                .replace("<green>", "§a")
                .replace("</green>", "")
                .replace("<white>", "§f")
                .replace("</white>", "")
                .replace("<aqua>", "§b")
                .replace("</aqua>", "")
                .replace("<bold>", "§l")
                .replace("</bold>", "");
    }
}
