package commands;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NonNull;
import plugin.Utils;

import java.util.Map;

/*
 * Practice
 * - Teleports all online players (spectators excluded) to the chosen phase's default location, then starts it.
 * - "--no-teleport" skips the teleport so players can start the phase wherever they currently are.
 * - Runs the same boss/server instructions as /tas, but WITHOUT the fake-player routines, handoffs, or
 *   spectator sync, so real players can practice the boss fights and mechanics. The phase begins after the
 *   standard 3-second pre-run countdown (see Server.serverInstructions).
 */
public class Practice implements CommandExecutor {

	/** Default teleport location per phase: {x, y, z, yaw, pitch}. */
	private static final Map<String, double[]> DEFAULT_LOCATIONS = Map.of(
			"all", new double[]{-120.5, 71, -183.5, 0f, 0f},
			"clear", new double[]{-120.5, 71, -183.5, 0f, 0f},
			"boss", new double[]{73.5, 221, 14.5, 0f, 0f},
			"maxor", new double[]{73.5, 221, 14.5, 0f, 0f},
			"storm", new double[]{73.5, 165, 53.5, 0f, 0f},
			"goldor", new double[]{100.5, 116.06250, 40.5, 0f, 0f},
			"necron", new double[]{54.5, 64, 114.5, 180f, 0f},
			"witherking", new double[]{54.5, 5, 76.5, 180f, 0f});

	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage(Utils.msg("Only players can run this"));
			return true;
		}

		// /practice end - cancel the current session.
		if(args.length >= 1 && args[0].equalsIgnoreCase("end")) {
			TAS.endPractice(p.getWorld());
			p.sendMessage(Utils.msg("<yellow>Practice session ended"));
			return true;
		}

		String section = "all";
		boolean noTeleport = false;
		for(String arg : args) {
			if(arg.equalsIgnoreCase("--no-teleport") || arg.equalsIgnoreCase("--noteleport")) noTeleport = true;
			else section = arg.toLowerCase();
		}
		if(!DEFAULT_LOCATIONS.containsKey(section)) {
			p.sendMessage(Utils.msg("<red>Invalid section specified.  Valid sections: clear boss maxor storm goldor necron witherking"));
			return true;
		}

		World world = p.getWorld();

		// Teleport every online player to the phase's default location — but leave spectators alone, and skip
		// entirely with --no-teleport so players start wherever they currently are.
		if(!noTeleport) {
			double[] loc = DEFAULT_LOCATIONS.get(section);
			Location target = new Location(world, loc[0], loc[1], loc[2], (float) loc[3], (float) loc[4]);
			for(Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
				if(online.getGameMode() == GameMode.SPECTATOR || Spectate.isSpectating(online)) continue;
				online.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
			}
		}

		TAS.runPractice(world, section);
		return true;
	}
}
