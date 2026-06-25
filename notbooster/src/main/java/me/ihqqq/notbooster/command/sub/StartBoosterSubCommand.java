package me.ihqqq.notbooster.command.sub;

import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.booster.*;
import me.ihqqq.notbooster.manager.BoosterManager;
import me.ihqqq.notbooster.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class StartBoosterSubCommand implements BoosterSubCommand {
    @Override public String name() { return "start"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sendUsage(sender);
            return true;
        }
        Optional<BoosterType> type = BoosterType.parse(args[0]);
        Optional<BoosterScope> scope = BoosterScope.parse(args[1]);
        if (type.isEmpty() || scope.isEmpty()) {
            MessageUtil.send(sender, "invalid-arguments");
            sendUsage(sender);
            return true;
        }
        String permission = scope.get() == BoosterScope.GLOBAL ? "notbooster.global" : "notbooster.personal";
        if (!sender.hasPermission(permission)) {
            MessageUtil.send(sender, "no-permission");
            return true;
        }
        UUID ownerUuid = null;
        String ownerName = "GLOBAL";
        if (scope.get() == BoosterScope.PERSONAL) {
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, "invalid-sender");
                return true;
            }
            ownerUuid = player.getUniqueId();
            ownerName = player.getName();
        }
        try {
            long duration = Math.min(TimeParser.parseMillis(args[3]), scope.get() == BoosterScope.GLOBAL ? Settings.MAX_DURATION_GLOBAL_MS : Settings.MAX_DURATION_PERSONAL_MS);
            double multiplier = type.get() == BoosterType.EFFECT ? 1.0D : Math.min(PowerParser.parseMultiplier(args[2]), Settings.getMaxMultiplier(type.get()));
            String effectPreset = type.get() == BoosterType.EFFECT ? args[2] : "";
            Booster booster = BoosterManager.getInstance().add(type.get(), scope.get(), ownerUuid, ownerName, multiplier, duration,
                    effectPreset, sender instanceof Player ? BoosterSourceType.COMMAND : BoosterSourceType.CONSOLE, sender.getName());
            MessageUtil.broadcast("booster-started", "{id}", booster.getId().toString().substring(0, 8), "{type}", booster.getType().name(),
                    "{scope}", booster.getScope().name(), "{power}", CommandUtil.formatPower(type.get(), effectPreset, multiplier), "{time}", CommandUtil.remaining(duration));
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, e.getMessage());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return CommandUtil.filter(CommandUtil.TYPES, args[0]);
        if (args.length == 2) return CommandUtil.filter(CommandUtil.SCOPES, args[1]);
        if (args.length == 3) return CommandUtil.filter(CommandUtil.POWER_EXAMPLES, args[2]);
        if (args.length == 4) return CommandUtil.filter(CommandUtil.DURATION_EXAMPLES, args[3]);
        return List.of();
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.send(sender, "command-header");
    }
}
