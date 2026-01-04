package commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.M7tas;

/*
 * TAS
 * - Gives all NPCs the appropriate inventory
 * - Re-teleports all NPCs to their initial locations
 * - Re-spawns all mobs
 * - Runs the TAS script
 */
public class TAS implements CommandExecutor {
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
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
		M7tas.runTAS(p.getWorld(), section);
		return true;
	}
}