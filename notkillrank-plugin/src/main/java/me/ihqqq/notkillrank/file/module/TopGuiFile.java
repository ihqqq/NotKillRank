package me.ihqqq.notkillrank.file.module;

import me.ihqqq.notkillrank.NotKillRank;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class TopGuiFile {

    private static FileConfiguration config;

    public static void init() {
        config = load("gui/top-gui.yml");
    }

    public static FileConfiguration get() {
        return config;
    }

    private static FileConfiguration load(String name) {
        File file = new File(NotKillRank.plugin.getDataFolder(), name);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            NotKillRank.plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
