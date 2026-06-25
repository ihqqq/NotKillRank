package me.ihqqq.notbooster.command.sub;

import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.booster.*;
import me.ihqqq.notbooster.manager.BoosterManager;
import me.ihqqq.notbooster.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public final class GiveBoosterSubCommand implements BoosterSubCommand {
    @Override public String name() { return "give"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("notbooster.admin")) {
            MessageUtil.send(sender, "no-permission");
            return true;
        }
        if (args.length != 5) {
            MessageUtil.send(sender, "give-usage");
            return true;
        }
        Optional<BoosterType> type = BoosterType.parse(args[2]);
        if (type.isEmpty()) {
            MessageUtil.send(sender, "invalid-arguments");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            MessageUtil.send(sender, "player-not-found", "{player}", args[1]);
            return true;
        }
        try {
            long duration = Math.min(TimeParser.parseMillis(args[4]), Settings.MAX_DURATION_PERSONAL_MS);
            double multiplier = type.get() == BoosterType.EFFECT ? 1.0D : Math.min(PowerParser.parseMultiplier(args[3]), Settings.getMaxMultiplier(type.get()));
            String effectPreset = type.get() == BoosterType.EFFECT ? args[3] : "";
            Booster booster = BoosterManager.getInstance().add(type.get(), BoosterScope.PERSONAL, target.getUniqueId(), target.getName(), multiplier,
                    duration, effectPreset, sender instanceof Player ? BoosterSourceType.COMMAND : BoosterSourceType.CONSOLE, sender.getName());
            MessageUtil.send(sender, "booster-given", "{player}", target.getName(), "{id}", booster.getId().toString().substring(0, 8),
                    "{type}", booster.getType().name(), "{power}", CommandUtil.formatPower(type.get(), effectPreset, multiplier), "{time}", CommandUtil.remaining(duration));
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, e.getMessage());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return CommandUtil.filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        if (args.length == 3) return CommandUtil.filter(CommandUtil.TYPES, args[2]);
        if (args.length == 4) return CommandUtil.filter(CommandUtil.POWER_EXAMPLES, args[3]);
        if (args.length == 5) return CommandUtil.filter(CommandUtil.DURATION_EXAMPLES, args[4]);
        return List.of();
    }
}
