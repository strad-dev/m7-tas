package commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Practice
 * 1. Every non-spectator must have a class (/class) or the run is refused: no class means no kit and no class
 *    tag, a silently broken run.
 * 2. Equips each with their saved /m7loadout kit, refreshed to current item definitions, teleports them to the
 *    phase's default location, then starts it.
 * 3. "--no-teleport" skips the teleport. Bare "classic"/"perfect_rng"/"rta" sets the damage mode (MAP.md §0),
 *    "paul"/"derpy"/"other" the mayor, "on"/"off" the alpha timings; omitted, current settings stand, so
 *    standalone keeps whatever /dungeonsettings set. The network always sends all three.
 * 4. Runs the same boss and server instructions as /tas, WITHOUT fake-player routines, handoffs or spectator
 *    sync. Pre-run delay is 60 ticks (3s); a bare integer overrides it: the network sends
 *    "m7tas:m7practice <section> 400" for a 20s window. See Server.serverInstructions.
 *
 * On the network the bare /m7practice label is StradDevHub's queue command (alias of /m7, force-claimed at boot),
 * so this is only reachable as /m7tas:m7practice, which M7Bridge dispatches and blocks players from typing.
 * Standalone, the bare label is ours.
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

		if(args.length >= 1 && args[0].equalsIgnoreCase("end")) {
			TAS.endPractice(p.getWorld());
			p.sendMessage(Utils.msg("<yellow>Practice session ended"));
			return true;
		}

		String section = "all";
		boolean noTeleport = false;
		// Optional pre-run delay in ticks (bare integer). Default 60 (3s); network passes 400 (20s) when it warps
		// a party in.
		int delayTicks = 60;
		// Optional difficulty ("classic" / "perfect_rng" / "rta"). Null leaves it alone, so standalone keeps its
		// /dungeonsettings choice. The network always passes one: damage.Difficulty is server-wide and a run must
		// not inherit the last party's mode, which decides whether anyone can die (both live modes kill).
		damage.Difficulty difficulty = null;
		// Optional mayor ("paul" / "derpy" / "other"). Null leaves it alone, same reason: damage.Mayor is
		// server-wide, and it decides whether every mob has double health.
		damage.Mayor mayorArg = null;
		// Optional alpha timings ("on" / "off"). Null leaves it alone, same reason: plugin.Alpha is server-wide,
		// and an inherited flag times the run under timings nobody chose, so leaderboards refuse it.
		plugin.Alpha alphaArg = null;
		for(String arg : args) {
			// Parsed up front, before the section fallback, which swallows any unknown word and would read
			// "classic" as a section name.
			damage.Difficulty mode = damage.Difficulty.parse(arg);
			damage.Mayor mayor = damage.Mayor.parse(arg);
			plugin.Alpha alpha = plugin.Alpha.parse(arg);
			if(arg.equalsIgnoreCase("--no-teleport") || arg.equalsIgnoreCase("--noteleport")) noTeleport = true;
			else if(arg.matches("\\d+")) delayTicks = Integer.parseInt(arg);
			else if(mode != null) difficulty = mode;
			else if(mayor != null) mayorArg = mayor;
			else if(alpha != null) alphaArg = alpha;
			else section = arg.toLowerCase();
		}
		if(!DEFAULT_LOCATIONS.containsKey(section)) {
			p.sendMessage(Utils.msg("<red>Invalid section specified.  Valid sections: clear boss maxor storm goldor necron witherking"));
			return true;
		}

		World world = p.getWorld();

		// Participants: online and not spectating by either route (vanilla spectator, the idle state on networked
		// m7, or our /spectate). Shared by class check, kit hand-out and teleport so all three agree.
		List<Player> participants = new ArrayList<>();
		for(Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
			if(online.getGameMode() == GameMode.SPECTATOR || Spectate.isSpectating(online)) continue;
			participants.add(online);
		}

		// Every participant needs a class: without one, no kit and no class tag, so no abilities or class-gated
		// damage. The network blocks this up front in /m7practice, so this never fires for a bridged run.
		List<String> noClass = new ArrayList<>();
		for(Player participant : participants) {
			if(loadout.Loadouts.getSelectedClass(participant.getUniqueId()) == null) noClass.add(participant.getName());
		}
		if(!noClass.isEmpty()) {
			boolean solo = noClass.size() == 1 && noClass.getFirst().equals(p.getName());
			p.sendMessage(solo
					? Utils.msg("<red>Pick a class first with <white>/class <c></white> before starting practice.",
							Placeholder.unparsed("c", "<archer|mage|tank|berserk|healer>"))
					: Utils.msg("<red>Some players have not picked a class yet!  <white><who></white>  <gray>(they need /class <c>)",
							Placeholder.unparsed("who", String.join(", ", noClass)),
							Placeholder.unparsed("c", "<archer|mage|tank|berserk|healer>")));
			return true;
		}

		// Saved kit for the selected class. The only way to get items now (/getcustomitems is gone): /class,
		// /m7loadout, then /m7practice. applyFor refreshes to current item definitions and sets the class tag
		// (gates mage beam and per-class damage). Idempotent on the network: M7Bridge applied the same file on join.
		for(Player participant : participants) {
			loadout.Loadouts.applyFor(participant);
		}

		// Skipped with --no-teleport so players start where they are.

		if(!noTeleport) {
			double[] loc = DEFAULT_LOCATIONS.get(section);
			Location target = new Location(world, loc[0], loc[1], loc[2], (float) loc[3], (float) loc[4]);
			for(Player participant : participants) {
				participant.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
			}
		}

		// Set BEFORE the run arms: debuff, defense reducer and blessing lookups read damage.Difficulty live, and
		// RunResult.capture stamps it at completion, so this picks the leaderboard. Also decides whether death is
		// on, which TAS.runPractice reads when clearing death state.
		if(difficulty != null) damage.Difficulty.set(difficulty);
		// Mayor too, and it matters more: mob HP is written once at spawn, so it must be right before anything spawns.
		if(mayorArg != null) damage.Mayor.set(mayorArg);
		// Alpha too: a phase arms its whole schedule the tick it starts.
		if(alphaArg != null) plugin.Alpha.set(alphaArg);
		if(plugin.Alpha.enabled()) {
			org.bukkit.Bukkit.broadcast(Utils.msg("<gold><bold>ALPHA TIMINGS<reset><gray> are on.  "
					+ "<red>This run is not valid for the leaderboards."));
		}

		TAS.runPractice(world, section, delayTicks);
		return true;
	}
}
