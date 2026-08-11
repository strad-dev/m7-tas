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
 * 1. Every non-spectator must have a class selected (/class), or the run is refused: no class means no kit and no
 *    class tag, which is a silently broken run rather than an obviously refused one.
 * 2. Equips each of them with their saved /m7loadout kit, refreshed to the current item definitions, and
 *    teleports them to the chosen phase's default location, then starts it.
 * 3. "--no-teleport" skips the teleport so players can start the phase wherever they currently are.  A bare
 *    "classic"/"realistic" arg sets the damage mode for the run (DAMAGE_PLAN.md §0); omitted, the current mode
 *    stands, so a standalone player keeps whatever /toggledungeondifficulty last set.  The network always sends one.
 * 4. Runs the same boss and server instructions as /tas, but WITHOUT the fake-player routines, handoffs, or
 *    spectator sync, so real players can practice the boss fights and mechanics.  The phase begins after a
 *    pre-run delay of 60 ticks (3s) by default.  Pass a bare integer arg to override it: the network plugin
 *    sends "m7tas:m7practice <section> 400" for a 20s get-into-position window.  See Server.serverInstructions.
 *
 * The label: /m7practice.  On the network the BARE label is StradDevHub's queue command (an alias of /m7,
 * force-claimed at boot), so this one is only reachable there as /m7tas:m7practice - which is exactly what
 * M7Bridge dispatches, and what it blocks players from typing.  Standalone, the bare label is ours.
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

		// /m7practice end cancels the current session.
		if(args.length >= 1 && args[0].equalsIgnoreCase("end")) {
			TAS.endPractice(p.getWorld());
			p.sendMessage(Utils.msg("<yellow>Practice session ended"));
			return true;
		}

		String section = "all";
		boolean noTeleport = false;
		// Optional pre-run "get into position" delay in ticks (a bare integer arg). Defaults to 60 (3s); the
		// network plugin passes a longer delay (e.g. 400 = 20s) when it warps a whole party in together.
		int delayTicks = 60;
		// Optional damage difficulty ("classic" / "realistic"). Null means "leave the mode alone", which is what a
		// player running this standalone wants: their /toggledungeondifficulty choice stands. The network ALWAYS
		// passes one, since damage.Difficulty is a server-wide global and a run must not inherit the last party's mode.
		damage.Difficulty difficulty = null;
		for(String arg : args) {
			// Parsed up front so the mode branch below is one test: it has to come BEFORE the section fallback,
			// which swallows any unrecognised word and would otherwise read "classic" as a section name.
			damage.Difficulty mode = damage.Difficulty.parse(arg);
			if(arg.equalsIgnoreCase("--no-teleport") || arg.equalsIgnoreCase("--noteleport")) noTeleport = true;
			else if(arg.matches("\\d+")) delayTicks = Integer.parseInt(arg);
			else if(mode != null) difficulty = mode;
			else section = arg.toLowerCase();
		}
		if(!DEFAULT_LOCATIONS.containsKey(section)) {
			p.sendMessage(Utils.msg("<red>Invalid section specified.  Valid sections: clear boss maxor storm goldor necron witherking"));
			return true;
		}

		World world = p.getWorld();

		// Everyone the run applies to: online and not spectating, by either route, i.e. vanilla spectator mode,
		// which is the idle state on the networked m7 server, or the plugin's own /spectate.  Used for the class
		// check, the kit hand-out and the teleport, so all three always agree on who is taking part.
		List<Player> participants = new ArrayList<>();
		for(Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
			if(online.getGameMode() == GameMode.SPECTATOR || Spectate.isSpectating(online)) continue;
			participants.add(online);
		}

		// Every participant must have picked a class.  Without one they would get no kit and no class tag, i.e. no
		// abilities and no class-gated damage, which is a silently broken run rather than an obviously refused one.
		// The network plugin blocks the same case up front in /m7practice, so this never fires for a bridged run.
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

		// Hand every participant the kit they saved for their selected class. This is THE way to get items now
		// (/getcustomitems is gone): pick a class with /class, tune it with /m7loadout, then /m7practice. applyFor
		// refreshes the saved copies to the current item definitions first, and sets the class scoreboard tag that
		// gates the mage beam and the per-class damage paths.
		// Idempotent on the network: M7Bridge already applied the same loadout from the same file on join.
		for(Player participant : participants) {
			loadout.Loadouts.applyFor(participant);
		}

		// Teleport participants to the phase's default location.  This is skipped entirely with --no-teleport so
		// players start wherever they currently are.

		if(!noTeleport) {
			double[] loc = DEFAULT_LOCATIONS.get(section);
			Location target = new Location(world, loc[0], loc[1], loc[2], (float) loc[3], (float) loc[4]);
			for(Player participant : participants) {
				participant.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
			}
		}

		// Set the mode BEFORE the run arms: every debuff, defense reducer and blessing lookup reads
		// damage.Difficulty live, and RunResult.capture stamps the run with whatever it says at completion, so the
		// leaderboard board a time lands on is decided here.
		if(difficulty != null) damage.Difficulty.set(difficulty);

		TAS.runPractice(world, section, delayTicks);
		return true;
	}
}
