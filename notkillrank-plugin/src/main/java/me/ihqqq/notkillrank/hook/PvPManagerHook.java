package me.ihqqq.notkillrank.hook;

import me.chancesd.pvpmanager.player.CombatPlayer;
import me.ihqqq.notkillrank.file.module.PvPManagerFile;
import me.ihqqq.notkillrank.listener.PvPManagerListener;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PvPManagerHook {

    private static PvPManagerHook instance;
    private final boolean hooked;

    private final Map<UUID, UUID> lastAttackerMap = new ConcurrentHashMap<>();

    public PvPManagerHook() {
        instance = this;
        Plugin pvpm = Bukkit.getPluginManager().getPlugin("PvPManager");
        if (pvpm != null && pvpm.isEnabled()) {
            hooked = true;
            new PvPManagerListener();
            MessageUtil.log("<green>[PvPManager] <gray>Hook thành công với PvPManager v"
                    + pvpm.getDescription().getVersion());
        } else {
            hooked = false;
            MessageUtil.log("<yellow>[PvPManager] <gray>Không tìm thấy PvPManager — module bị tắt.");
        }
    }

    public static PvPManagerHook getInstance() { return instance; }

    public boolean isHooked() { return hooked; }

    public boolean isInCombat(Player player) {
        if (!hooked) return false;
        CombatPlayer cp = CombatPlayer.get(player);
        return cp != null && cp.isInCombat();
    }

    public boolean shouldSkipElo(Player victim) {
        if (!hooked) return false;
        if (!PvPManagerFile.get().getBoolean("skip-elo-if-victim-pvp-off", true)) return false;
        CombatPlayer cp = CombatPlayer.get(victim);
        return cp != null && !cp.hasPvPEnabled();
    }

    public boolean isCombatLogging(Player player) {
        if (!hooked) return false;
        CombatPlayer cp = CombatPlayer.get(player);
        return cp != null && cp.hasPvPLogged();
    }

    public void recordAttacker(UUID tagged, UUID attacker) {
        lastAttackerMap.put(tagged, attacker);
    }

    public UUID popLastAttacker(UUID player) {
        return lastAttackerMap.remove(player);
    }

    public void clearPlayer(UUID player) {
        lastAttackerMap.remove(player);
    }
}
