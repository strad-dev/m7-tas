package commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.Utils;
import plugin.Utils.VerboseLevel;

public class Verbose implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage(Utils.msg("Only players can run this"));
			return true;
		}

		if(args.length < 1) {
			p.sendMessage(Utils.msg("<red>Usage: /verbose \\<off|timer|on|super>"));
			return true;
		}

		VerboseLevel level;
		switch(args[0].toLowerCase()) {
			case "off", "false" -> level = VerboseLevel.OFF;
			case "timer" -> level = VerboseLevel.TIMER;
			case "on", "true" -> level = VerboseLevel.ON; // from SUPER this drops back to normal, not a no-op
			// `super` toggles: engage it, or fall back to normal verbosity if it's already on.
			case "super" -> level = (Utils.getVerboseLevel() == VerboseLevel.SUPER) ? VerboseLevel.ON : VerboseLevel.SUPER;
			default -> {
				p.sendMessage(Utils.msg("<red>Usage: /verbose \\<off|timer|on|super>"));
				return true;
			}
		}
		Utils.setVerboseLevel(level);
		p.sendMessage(Utils.msg("<green>Verbose Mode: " + level));
		return true;
	}
}
