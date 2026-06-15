package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.file.module.RevengerFile;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.entity.Player;

public class RevengerManager {

    private static RevengerManager instance;

    public RevengerManager() {
        instance = this;
    }

    public static RevengerManager getInstance() {
        return instance;
    }

    /**
     * Kiểm tra xem kill này có phải báo thù không.
     *
     * <p>Kịch bản: phitv giết phi trước → phi trả thù giết lại phitv.
     * Trong event phi giết phitv: killer = phi, victim = phitv.
     * Kiểm tra: nạn nhân (phitv) có phải người đã giết kẻ giết (phi) không?
     * → {@code killerData.lastKillerUUID == victim.UUID} VÀ trong khoảng thời gian cho phép.
     *
     * @param killerData dữ liệu kẻ đang giết (người trả thù)
     * @param victimUUID UUID nạn nhân (người bị trả thù)
     */
    public boolean isRevenge(PlayerData killerData, String victimUUID) {
        if (!Settings.MODULE_REVENGER) return false;
        if (killerData.getLastKillerUUID() == null) return false;
        if (!killerData.getLastKillerUUID().equals(victimUUID)) return false;
        long elapsed = System.currentTimeMillis() - killerData.getLastKilledTime();
        return elapsed <= (long) Settings.ELO_REVENGE_WINDOW_SECONDS * 1000;
    }

    /**
     * Phát thông báo toàn server khi báo thù thành công.
     * Không broadcast nếu cấu hình broadcast = false.
     */
    public void broadcastRevenge(Player killer, Player victim) {
        if (!RevengerFile.get().getBoolean("broadcast", true)) return;
        String msg = MessageUtil.getMessage("revenge-kill",
                        "<gold>⚔ <white>{player} <gray>đã báo thù <red>{target}<gray>!")
                .replace("{player}", killer.getName())
                .replace("{target}", victim.getName());
        MessageUtil.sendBroadcast(msg);
    }
}
