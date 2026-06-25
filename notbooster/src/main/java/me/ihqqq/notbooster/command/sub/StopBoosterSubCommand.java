package me.ihqqq.notbooster.command.sub;

import me.ihqqq.notbooster.booster.Booster;
import me.ihqqq.notbooster.manager.BoosterManager;
import me.ihqqq.notbooster.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

public final class StopBoosterSubCommand implements BoosterSubCommand {
    @Override public String name() { return "stop"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("notbooster.admin")) {
            MessageUtil.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "invalid-arguments");
            return true;
        }
        UUID id = resolveBoosterId(args[1]);
        if (id == null || !BoosterManager.getInstance().remove(id)) {
            MessageUtil.send(sender, "booster-not-found");
            return true;
        }
        MessageUtil.send(sender, "booster-stopped");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandUtil.filter(BoosterManager.getInstance().getActiveBoosters().stream()
                    .map(booster -> booster.getId().toString())
                    .toList(), args[1]);
        }
        return List.of();
    }

    private UUID resolveBoosterId(String input) {
        for (Booster booster : BoosterManager.getInstance().getActiveBoosters()) {
            if (booster.getId().toString().startsWith(input)) return booster.getId();
        }
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
