package commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.Utils;

public class Verbose implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		if(args.length < 1) {
			p.sendMessage(ChatColor.RED + "Usage: /verbose <true|false|super>");
			return true;
		}

		if(args[0].equalsIgnoreCase("super")) {
			boolean newValue = !Utils.isSuperVerbose();
			Utils.setSuperVerbose(newValue);
			p.sendMessage(ChatColor.GREEN + "Super Verbose Mode: " + (newValue ? "ON" : "OFF"));
		} else {
			boolean value = Boolean.parseBoolean(args[0]);
			Utils.setVerbose(value);
			p.sendMessage(ChatColor.GREEN + "Verbose Mode: " + (value ? "ON" : "OFF"));
		}
		return true;
	}
}
