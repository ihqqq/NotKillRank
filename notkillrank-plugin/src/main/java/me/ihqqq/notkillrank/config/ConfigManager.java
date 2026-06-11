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

    private FileConfiguration eloConfig;
    private FileConfiguration antiFarmConfig;
    private FileConfiguration protectionConfig;
    private FileConfiguration decayConfig;
    private FileConfiguration ranksConfig;
    private FileConfiguration streaksConfig;
    private FileConfiguration bountyConfig;
    private FileConfiguration voSongConfig;

    public ConfigManager(NotKillRank plugin) {
        this.plugin = plugin;
        instance = this;
        load();
    }

    public static ConfigManager getInstance() {
        return instance;
    }

    public void load() {
        messagesConfig  = loadConfig("messages.yml");
        topGuiConfig    = loadConfig("gui/top-gui.yml");
        eloConfig       = loadConfig("modules/elo.yml");
        antiFarmConfig  = loadConfig("modules/anti-farm.yml");
        protectionConfig = loadConfig("modules/protection.yml");
        decayConfig     = loadConfig("modules/decay.yml");
        ranksConfig     = loadConfig("modules/ranks.yml");
        streaksConfig   = loadConfig("modules/streaks.yml");
        bountyConfig    = loadConfig("modules/bounty.yml");
        voSongConfig    = loadConfig("modules/vosong.yml");
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
            plugin.getLogger().warning("[ConfigManager] Could not update "
                    + resourcePath + ": " + e.getMessage());
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getMessages()     { return messagesConfig; }
    public FileConfiguration getTopGui()       { return topGuiConfig; }
    public FileConfiguration getEloConfig()    { return eloConfig; }
    public FileConfiguration getAntiFarmConfig() { return antiFarmConfig; }
    public FileConfiguration getProtectionConfig() { return protectionConfig; }
    public FileConfiguration getDecayConfig()  { return decayConfig; }
    public FileConfiguration getRanksConfig()  { return ranksConfig; }
    public FileConfiguration getStreaksConfig() { return streaksConfig; }
    public FileConfiguration getBountyConfig() { return bountyConfig; }
    public FileConfiguration getVoSongConfig() { return voSongConfig; }

    public String getMessage(String path) {
        return messagesConfig.getString(path, "");
    }

    public String getMessage(String path, String def) {
        return messagesConfig.getString(path, def);
    }
}
