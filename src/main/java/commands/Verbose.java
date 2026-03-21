package commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.Utils;

public class Verbose implements CommandExecutor {
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		if(args.length < 1) {
			p.sendMessage(ChatColor.RED + "Usage: /verbose <true|false>");
			return true;
		}

		boolean value = Boolean.parseBoolean(args[0]);
		Utils.setVerbose(value);
		p.sendMessage(ChatColor.GREEN + "Verbose Mode: " + (value ? "ON" : "OFF"));
		return true;
	}
}
