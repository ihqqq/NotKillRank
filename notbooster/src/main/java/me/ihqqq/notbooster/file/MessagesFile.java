package me.ihqqq.notbooster.file;

import me.ihqqq.notbooster.NotBooster;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class MessagesFile {

    private static FileConfiguration config;

    private MessagesFile() {}

    public static void init() {
        File file = new File(NotBooster.plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) NotBooster.plugin.saveResource("messages.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get() {
        return config;
    }
}
