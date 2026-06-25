package me.ihqqq.notbooster.file;

import me.ihqqq.notbooster.NotBooster;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class ConfigFile {

    private static FileConfiguration config;

    private ConfigFile() {}

    public static void init() {
        File file = new File(NotBooster.plugin.getDataFolder(), "config.yml");
        if (!file.exists()) NotBooster.plugin.saveDefaultConfig();
        NotBooster.plugin.reloadConfig();
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get() {
        return config;
    }
}
