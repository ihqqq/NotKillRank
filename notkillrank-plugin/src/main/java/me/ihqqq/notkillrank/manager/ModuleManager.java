package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.util.MessageUtil;

public class ModuleManager {

    private static ModuleManager instance;

    public enum Module {
        ANTI_FARM,
        BOUNTY,
        DECAY,
        PROTECTION,
        STREAKS,
        PLACEHOLDERAPI,
        WEBHOOK,
        PVPMANAGER;

        public boolean isEnabled() {
            return switch (this) {
                case ANTI_FARM      -> Settings.MODULE_ANTI_FARM;
                case BOUNTY         -> Settings.MODULE_BOUNTY;
                case DECAY          -> Settings.MODULE_DECAY;
                case PROTECTION     -> Settings.MODULE_PROTECTION;
                case STREAKS        -> Settings.MODULE_STREAKS;
                case PLACEHOLDERAPI -> Settings.MODULE_PLACEHOLDERAPI;
                case WEBHOOK        -> Settings.MODULE_WEBHOOK;
                case PVPMANAGER     -> Settings.MODULE_PVPMANAGER;
            };
        }
    }

    public ModuleManager() {
        instance = this;
    }

    public static ModuleManager getInstance() {
        return instance;
    }

    public boolean isEnabled(Module module) {
        return module.isEnabled();
    }

    public static void reload() {
        Settings.setupValue();
        logStatus();
    }

    private static void logStatus() {
        StringBuilder sb = new StringBuilder("[ModuleManager] Trạng thái module: ");
        for (Module m : Module.values()) {
            sb.append(m.name().toLowerCase()).append("=").append(m.isEnabled() ? "ON" : "OFF").append(" ");
        }
        MessageUtil.log(sb.toString().trim());
    }
}
