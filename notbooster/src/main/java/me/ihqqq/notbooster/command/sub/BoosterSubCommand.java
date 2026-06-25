package me.ihqqq.notbooster.command.sub;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface BoosterSubCommand {
    String name();
    boolean execute(CommandSender sender, String[] args);
    List<String> tabComplete(CommandSender sender, String[] args);
}
