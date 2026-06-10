package me.ihqqq.notkillrank.config;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.ihqqq.notkillrank.NotKillRank;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class ConfigManager {

    private static ConfigManager instance;

    private final NotKillRank plugin;
    private FileConfiguration messagesConfig;
    private FileConfiguration topGuiConfig;

    public ConfigManager(NotKillRank plugin) {
        this.plugin = plugin;
        instance = this;
        load();
    }

    public static ConfigManager getInstance() {
        return instance;
    }

    public void load() {
        messagesConfig = loadConfig("messages.yml");
        topGuiConfig   = loadConfig("gui/top-gui.yml");
    }

    private FileConfiguration loadConfig(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(resourcePath, false);
        }

        try {
            ConfigUpdater.update(plugin, resourcePath, file, Collections.emptyList());
        } catch (IOException e) {
            plugin.getLogger().warning("[ConfigManager] Could not update " + resourcePath + ": " + e.getMessage());
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    public FileConfiguration getTopGui() {
        return topGuiConfig;
    }

    public String getMessage(String path) {
        return messagesConfig.getString(path, "");
    }

    public String getMessage(String path, String def) {
        return messagesConfig.getString(path, def);
    }
}
