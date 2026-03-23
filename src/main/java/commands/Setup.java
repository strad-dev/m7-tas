package commands;

import instructions.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.M7tas;

/*
 * Setup
 * - Clears all NPCs and spawns new ones
 * - Teleports all NPCs to their initial locations
 * - Does an initial mob spawning (to test without running the full TAS)
 */
public class Setup implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		TAS.spawnAllFakes(p.getWorld());
		Server.serverSetup(p.getWorld());
		p.sendMessage("Cleared all NPCs and spawned new ones");
		return true;
	}
}
