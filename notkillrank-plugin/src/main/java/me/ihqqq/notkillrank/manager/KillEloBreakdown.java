package me.ihqqq.notkillrank.manager;

public class KillEloBreakdown {

    public final int baseElo;

    public final double multiplier;

    public final String multiplierLabel;

    public final int revengeBonusPct;

    public final int streakBonusPct;

    public final int eloGained;

    public final int weakPenalty;

    public final int totalVictimLoss;

    public final boolean isStreakBreak;

    public final int brokenStreak;

    public KillEloBreakdown(int baseElo, double multiplier, String multiplierLabel,
                            int revengeBonusPct, int streakBonusPct,
                            int eloGained, int weakPenalty, int totalVictimLoss,
                            boolean isStreakBreak, int brokenStreak) {
        this.baseElo = baseElo;
        this.multiplier = multiplier;
        this.multiplierLabel = multiplierLabel;
        this.revengeBonusPct = revengeBonusPct;
        this.streakBonusPct = streakBonusPct;
        this.eloGained = eloGained;
        this.weakPenalty = weakPenalty;
        this.totalVictimLoss = totalVictimLoss;
        this.isStreakBreak = isStreakBreak;
        this.brokenStreak = brokenStreak;
    }

    public String buildBreakdownString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<green>+").append(eloGained).append(" elo");

        boolean hasMultiplier = multiplier != 1.0;
        boolean hasRevenge = revengeBonusPct > 0;
        boolean hasStreak = streakBonusPct > 0;

        if (hasMultiplier || hasRevenge || hasStreak) {
            sb.append(" <dark_gray>[");
            sb.append("<white>base:").append(baseElo);

            if (hasMultiplier) {
                sb.append(" <gray>× ").append(multiplierLabel);
            }
            if (hasRevenge) {
                sb.append(" <gold>+báo thù ").append(revengeBonusPct).append("%");
            }
            if (hasStreak) {
                sb.append(" <red>+streak ").append(streakBonusPct).append("%");
            }
            sb.append("<dark_gray>]");
        }

        return sb.toString();
    }

    public String buildVictimLossString() {
        if (weakPenalty > 0) {
            return "<red>-" + totalVictimLoss + " elo <dark_gray>[<red>-" + eloGained
                    + " <dark_gray>+ <dark_red>yếu -" + weakPenalty + "<dark_gray>]";
        }
        return "<red>-" + totalVictimLoss + " elo";
    }
}
