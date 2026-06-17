package commands;

import instructions.Actions;
import instructions.Server;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NonNull;
import plugin.FakePlayerManager;
import plugin.MovementAudit;
import plugin.Utils;

public class Reset implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		MovementAudit.cancelAll();
		Actions.cancelAllMovement();
		Utils.cancelAllScheduled();
		Location hide = new Location(Bukkit.getWorld("world"), -120.5, 71, -183.5);
		FakePlayerManager.getFakePlayers().values().forEach(npc -> npc.teleport(hide, PlayerTeleportEvent.TeleportCause.PLUGIN));
		Server.serverSetup(Bukkit.getWorld("world"));
		p.sendMessage("Reset server and all NPC locations");
		return true;
	}
}
