package me.ihqqq.notbooster.task;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.file.ConfigFile;
import me.ihqqq.notbooster.manager.BoosterManager;
import me.ihqqq.notbooster.booster.Booster;
import me.ihqqq.notbooster.booster.BoosterType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class EffectBoostTask implements Runnable {

    private int taskId = -1;

    public EffectBoostTask() {}

    public void start() {
        stop();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(NotBooster.plugin, this, 20L, 200L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public void run() {
        BoosterManager.getInstance().cleanupExpired();
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    private void apply(Player player) {
        List<Booster> boosters = BoosterManager.getInstance().getApplicable(player.getUniqueId(), BoosterType.EFFECT);
        Set<String> appliedPresets = new HashSet<>();
        for (Booster booster : boosters) {
            String preset = booster.getEffectPreset();
            if (preset == null || preset.isBlank() || !appliedPresets.add(preset.toLowerCase(Locale.ROOT))) continue;
            ConfigurationSection effects = ConfigFile.get().getConfigurationSection("effect-presets." + preset + ".effects");
            if (effects == null) continue;
            for (String effectKey : effects.getKeys(false)) {
                PotionEffectType type = resolveEffectType(effectKey);
                if (type == null) continue;
                int amplifier = Math.max(0, effects.getInt(effectKey + ".amplifier", 0));
                boolean ambient = effects.getBoolean(effectKey + ".ambient", false);
                boolean particles = effects.getBoolean(effectKey + ".particles", true);
                player.addPotionEffect(new PotionEffect(type, 240, amplifier, ambient, particles, true));
            }
        }
    }

    private PotionEffectType resolveEffectType(String key) {
        return PotionEffectType.getByName(key.toUpperCase(Locale.ROOT));
    }
}
