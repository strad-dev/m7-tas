package commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;
import plugin.FakePlayerManager;

public class KickAllFakes implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		// Tear down in the same order spawnAllFakes does: stop the connection ticker, then kick + clear the map.
		FakePlayerManager.stopCustomConnection();
		FakePlayerManager.kickAllFakes();
		sender.sendMessage("Kicked all fake players");
		return true;
	}
}
