package commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.Utils;

public class SetSpeed implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}
		if(args.length != 1) {
			sender.sendMessage("Usage: /setspeed <speed>");
			return true;
		}
		int speed;
		try {
			speed = Integer.parseInt(args[0]);
		} catch(NumberFormatException e) {
			sender.sendMessage("Speed must be an integer");
			return true;
		}
		Utils.setSpeed(p, speed);
		p.sendMessage("Speed set to " + speed);
		return true;
	}
}
