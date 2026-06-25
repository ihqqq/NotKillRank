package me.ihqqq.notbooster.command.sub;

import me.ihqqq.notbooster.booster.Booster;
import me.ihqqq.notbooster.manager.BoosterManager;
import me.ihqqq.notbooster.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public final class ListBoosterSubCommand implements BoosterSubCommand {
    @Override public String name() { return "list"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        long now = System.currentTimeMillis();
        MessageUtil.send(sender, "list-header");
        for (Booster booster : BoosterManager.getInstance().getActiveBoosters()) {
            sender.sendMessage(MessageUtil.color("<gray>- <yellow>" + booster.getId().toString().substring(0, 8)
                    + " <white>" + booster.getType().name()
                    + " <gray>" + booster.getScope().name()
                    + " <aqua>" + String.format(Locale.US, "%.2fx", booster.getMultiplier())
                    + " <gray>" + CommandUtil.remaining(booster.getRemainingMillis(now))));
        }
        return true;
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
