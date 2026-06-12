package me.ihqqq.notkillrank.file.module;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.ihqqq.notkillrank.NotKillRank;
import org.bukkit.configuration.file.FileConfiguration;;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class DecayFile {

    private static FileConfiguration config;

    public static void init() {
        config = load("modules/decay.yml");
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
        try {
            ConfigUpdater.update(NotKillRank.plugin, name, file, Collections.emptyList());
        } catch (IOException e) {
            NotKillRank.plugin.getLogger().warning("[DecayFile] Không thể cập nhật " + name + ": " + e.getMessage());
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
