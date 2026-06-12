package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;

public class ModuleManager {

    private static ModuleManager instance;

    public enum Module {
        ANTI_FARM("anti-farm"),
        BOUNTY("bounty"),
        DECAY("decay"),
        PROTECTION("protection"),
        STREAKS("streaks"),
        VOSONG("vosong"),
        PLACEHOLDERAPI("placeholderapi");

        public final String key;

        Module(String key) {
            this.key = key;
        }
    }

    private final boolean[] states = new boolean[Module.values().length];

    public ModuleManager() {
        instance = this;
        reload();
    }

    public static ModuleManager getInstance() {
        return instance;
    }

    public void reload() {
        FileConfiguration cfg = NotKillRank.getInstance().getConfig();
        for (Module m : Module.values()) {
            boolean enabled = cfg.getBoolean("modules.enabled." + m.key, true);
            states[m.ordinal()] = enabled;
        }
        logStatus();
    }

    public boolean isEnabled(Module module) {
        return states[module.ordinal()];
    }

    private void logStatus() {
        StringBuilder sb = new StringBuilder("[ModuleManager] Trạng thái module: ");
        for (Module m : Module.values()) {
            sb.append(m.key).append("=").append(states[m.ordinal()] ? "ON" : "OFF").append(" ");
        }
        MessageUtil.log(sb.toString().trim());
    }
}