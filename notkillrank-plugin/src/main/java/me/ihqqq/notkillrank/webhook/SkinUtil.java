package me.ihqqq.notkillrank.webhook;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.file.module.WebhookFile;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class SkinUtil {

    private static boolean srPresent = false;

    public static void init() {
        srPresent = Bukkit.getPluginManager().getPlugin("SkinsRestorer") != null;
        if (srPresent) {
            NotKillRank.plugin.getLogger().info(
                    "[Webhook] SkinsRestorer detected — avatar sẽ dùng skin thực của người chơi.");
        }
    }

    public static boolean isSrPresent() {
        return srPresent;
    }

    public static String getAvatarUrl(String name, UUID uuid) {
        if (srPresent) {
            try {
                String textureUrl = SkinsRestorerHook.getTextureUrl(uuid, name);
                if (textureUrl != null && !textureUrl.isBlank()) {
                    String hash = extractHash(textureUrl);
                    String template = getConfig("avatar.skinsrestorer-renderer",
                            "https://mc-heads.net/avatar/{name}/64");
                    return applyPlaceholders(template, name, uuid, textureUrl, hash);
                }
            } catch (Throwable ignored) {
            }
        }

        String template = getConfig("avatar.renderer",
                "https://mc-heads.net/avatar/{name}/64");
        return applyPlaceholders(template, name, uuid, "", "");
    }

    public static String getTextureUrl(String name, UUID uuid) {
        if (!srPresent) return "";
        try {
            String url = SkinsRestorerHook.getTextureUrl(uuid, name);
            return url != null ? url : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static String extractHash(String textureUrl) {
        if (textureUrl == null || textureUrl.isBlank()) return "";
        int last = textureUrl.lastIndexOf('/');
        return last >= 0 ? textureUrl.substring(last + 1) : textureUrl;
    }

    private static String applyPlaceholders(String template, String name, UUID uuid,
                                            String textureUrl, String textureHash) {
        String uuidStr = uuid != null ? uuid.toString() : name;
        return template
                .replace("{name}", name)
                .replace("{uuid}", uuidStr)
                .replace("{texture_url}", textureUrl != null ? textureUrl : "")
                .replace("{texture_hash}", textureHash != null ? textureHash : "");
    }

    private static String getConfig(String path, String def) {
        FileConfiguration cfg = WebhookFile.get();
        return cfg != null ? cfg.getString(path, def) : def;
    }
}
