package commands;

import instructions.Actions;
import instructions.Server;
import instructions.bosses.Watcher;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.maxor.Maxor;
import instructions.bosses.storm.Storm;
import instructions.players.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
			// The Watcher teleports the actors to the boss spawn the tick a fake enters the portal, then runs this
			// handoff the next tick — so Maxor and every player's maxor() routine start together.
			Runnable maxorHandoff = () -> {
				Maxor.maxorInstructions(world, true);
				Archer.maxor(true);
				Berserk.maxor(true);
				Healer.maxor(true);
				Mage.maxor(true);
				Tank.maxor(true);
			};
			Watcher.INSTANCE.arm(world, section.equals("all"), maxorHandoff);
		}

		// Arm the boss-to-boss player handoffs: when Maxor/Storm die and spawn the next boss (chainNext), each
		// player's storm()/goldor() routine starts that same tick — replacing the old hardcoded transition ticks
		// (storm(true)@496, goldor(true)@881) in the player scripts.
		if(section.equals("all") || section.equals("boss")) {
			Maxor.INSTANCE.armPlayerHandoff(() -> {
				Archer.storm(true);
				Berserk.storm(true);
				Healer.storm(true);
				Mage.storm(true);
				Tank.storm(true);
			});
			Storm.INSTANCE.armPlayerHandoff(() -> {
				Archer.goldor(true);
				Berserk.goldor(true);
				Healer.goldor(true);
				Mage.goldor(true);
				Tank.goldor(true);
			});
			Goldor.INSTANCE.armPlayerHandoff(() -> {
				Archer.necron(true);
				Berserk.necron(true);
				Healer.necron(true);
				Mage.necron(true);
				Tank.necron(true);
			});
		}

		// Restart spectator sync so it runs AFTER all instruction tasks in each tick
		Spectate.stopSpectatorSync();
		Spectate.startSpectatorSync();
	}
}
