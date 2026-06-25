package me.ihqqq.notbooster.file;

import me.ihqqq.notbooster.NotBooster;

import java.io.File;

public final class BoostersFile {

    private static File file;

    private BoostersFile() {}

    public static void init() {
        file = new File(NotBooster.plugin.getDataFolder(), "boosters.yml");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }

    public static File getFile() {
        return file;
    }
}
