package commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

/*
 * Practice
 * - Runs the same boss/server instructions as /tas, but WITHOUT the fake-player routines, handoffs, or
 *   spectator sync, so real players can practice the boss fights and mechanics.
 */
public class Practice implements CommandExecutor {

	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		String section = "all";
		if(args.length >= 1) {
			section = args[0].toLowerCase();
			if(!section.equals("all") && !section.equals("clear") && !section.equals("boss") && !section.equals("maxor") && !section.equals("storm") && !section.equals("goldor") && !section.equals("necron") && !section.equals("witherking")) {
				p.sendMessage(ChatColor.RED + "Invalid section specified.  Valid sections: clear boss maxor storm goldor necron witherking");
				return true;
			}
		}
		TAS.runPractice(p.getWorld(), section);
		return true;
	}
}
