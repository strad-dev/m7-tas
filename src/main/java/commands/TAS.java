package commands;

import instructions.Actions;
import instructions.Server;
import instructions.bosses.Watcher;
import instructions.bosses.maxor.Maxor;
import instructions.players.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.FakePlayerInventory;
import plugin.FakePlayerManager;
import plugin.MovementAudit;
import plugin.Utils;

import java.util.Map;

/*
 * TAS
 * - Gives all NPCs the appropriate inventory
 * - Re-teleports all NPCs to their initial locations
 * - Re-spawns all mobs
 * - Runs the TAS script
 */
public class TAS implements CommandExecutor {

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
		runTAS(p.getWorld(), section);
		return true;
	}

	public static void runTAS(World world, String section) {
		Map<String, Player> fakePlayers = FakePlayerManager.getFakePlayers();
		if(fakePlayers.isEmpty()) {
			Bukkit.broadcastMessage(ChatColor.RED + "Could not run TAS!  There are no actors.");
			return;
		}

		// Reset the verbose phase-tick counter immediately so carryover tasks from the previous phase
		// count from 0; the run proper re-marks it again when it actually starts (see Server "Run started").
		Utils.markPhaseStart();

		MovementAudit.cancelAll();
		Actions.cancelAllMovement();
		FakePlayerInventory.setInventories();
		Server.serverSetup(world);

		fakePlayers.values().forEach(p -> Utils.setSpeed(p, 400));

		// Start the boss/world instructions BEFORE the player routines. Both schedule their work for the same
		// tick (+60); submitting this first makes the boss phase activate before the players' tick-0 device
		// interactions fire — e.g. Tank right-clicking the S3 arrow-align frame, which would otherwise hit an
		// inactive phase and be dropped.
		Server.serverInstructions(world, section);

		Archer.archerInstructions(fakePlayers.get("Archer"), section);
		Berserk.berserkInstructions(fakePlayers.get("Mage3"), section);
		Healer.healerInstructions(fakePlayers.get("Mage4"), section);
		Mage.mageInstructions(fakePlayers.get("Mage1"), section);
		Tank.tankInstructions(fakePlayers.get("Mage2"), section);

		// Arm the Watcher with the run's continuation intent + the full Maxor handoff. The handoff fires when a fake
		// steps into the Blood Room's nether portal: it teleports every actor to the boss spawn, spawns Maxor, and
		// kicks off each player's maxor() routine (replacing the old hardcoded tick-742 teleport+maxor in each script).
		if(section.equals("all") || section.equals("clear")) {
			Location boss = new Location(world, 73.5, 221, 14.5, 0f, 0f);
			Runnable maxorHandoff = () -> {
				// Preserve the original handoff offset: the boss spawned 3 ticks before the players teleported in
				// (Watcher tick 739 vs each script's tick 742), so the players' maxor() routines stay in sync with
				// the boss's internal clock.
				Maxor.maxorInstructions(world, true);
				Utils.scheduleTask(() -> {
					Map<String, Player> fakes = FakePlayerManager.getFakePlayers();
					Utils.teleport(fakes.get("Archer"), boss);
					Archer.maxor(true);
					Utils.teleport(fakes.get("Mage3"), boss);
					Berserk.maxor(true);
					Utils.teleport(fakes.get("Mage4"), boss);
					Healer.maxor(true);
					Utils.teleport(fakes.get("Mage1"), boss);
					Mage.maxor(true);
					Utils.teleport(fakes.get("Mage2"), boss);
					Tank.maxor(true);
				}, 3);
			};
			Watcher.INSTANCE.arm(world, section.equals("all"), maxorHandoff);
		}

		// Restart spectator sync so it runs AFTER all instruction tasks in each tick
		Spectate.stopSpectatorSync();
		Spectate.startSpectatorSync();
	}
}
