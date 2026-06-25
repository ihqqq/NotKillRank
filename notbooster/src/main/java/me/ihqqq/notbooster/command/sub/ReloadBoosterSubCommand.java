package me.ihqqq.notbooster.command.sub;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class ReloadBoosterSubCommand implements BoosterSubCommand {
    @Override public String name() { return "reload"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("notbooster.reload")) {
            MessageUtil.send(sender, "no-permission");
            return true;
        }
        NotBooster.reload();
        MessageUtil.send(sender, "reloaded");
        return true;
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
