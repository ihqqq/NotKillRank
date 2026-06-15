package me.ihqqq.notkillrank.language;

public class BuiltInMessages {

    public static final String PREFIX =
            "<dark_gray>[<gold>NotKillRank<dark_gray>] ";

    public static final String NO_PERMISSION =
            "<red>Bạn không có quyền sử dụng lệnh này!";

    public static final String PLAYER_NOT_FOUND =
            "<red>Không tìm thấy người chơi <yellow>{player}<red>!";

    public static final String ELO_INFO =
            "<gold>{player} <white>— Elo: <green>{elo} <white>| Hạng: {rank}";

    public static final String STATS =
            "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                    + "<white>Elo: <green>{elo} | Hạng: {rank}\n"
                    + "<white>K/D: <yellow>{kd} ({kills}/{deaths})\n"
                    + "<white>Streak cao nhất: <red>{streak}\n"
                    + "<white>Elo đỉnh: <gold>{peak}";

    public static final String TOP =
            "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                    + "<gray>{pos}. {rank} <white>{player} <dark_gray>─ <green>{elo} elo";

    public static final String KILL_BROADCAST =
            "<reset>{rank_killer} <white>{killer} <green>+{elo_gained} elo "
                    + "<white>đã chọc chết {rank_victim} <white>{victim} <red>-{elo_lost} elo";

    public static final String STREAK_BREAK =
            "<yellow>{breaker} <white>đã chấm dứt chuỗi <red>{streak} kill "
                    + "<white>của <yellow>{victim}<white>! <green>(+{elo} elo)";

    public static final String BOUNTY_PLACED =
            "<gold>[Bounty] <white>{placer} <white>đã đặt truy nã <green>{amount} elo "
                    + "<white>lên đầu <red>{target}<white>!";

    public static final String BOUNTY_CLAIMED =
            "<gold>[Bounty] <white>{claimer} <white>đã nhận thưởng <green>{amount} elo "
                    + "<white>từ truy nã <red>{target}<white>!";

    public static final String BOUNTY_TARGET_PROTECTED =
            "<red>Không thể đặt bounty lên người chơi đang được bảo vệ người mới!";

    public static final String ANTI_FARM =
            "<red>Bạn đã giết <yellow>{victim} <red>quá nhiều lần trong 1 giờ! "
                    + "Không nhận được elo.";

    public static final String WELCOME =
            "{prefix}<white>Chào mừng <yellow>{player} <white>| Hạng: {rank}{streak} "
                    + "<white>| Elo: <green>{elo}";
}
