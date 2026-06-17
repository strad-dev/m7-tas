package commands;

import instructions.Actions;
import instructions.Server;
import instructions.bosses.Watcher;
import instructions.bosses.WitherActions;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.maxor.Maxor;
import instructions.bosses.necron.Necron;
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

		// A prior /practice may have left practice-mode aggro on — turn it back off for a real TAS run.
		WitherActions.setPracticeMode(false);
		// Clear any section splits recorded by a previous run (used by the Wither-King practice scoreboard).
		WitherActions.clearSplits();
		// Clear game-mode-change tracking (the practice scoreboard's golden-name anti-cheat).
		WitherActions.clearGameModeChanges();
		// Reset Berserk's per-mob damage-ramp counters.
		listeners.CustomItems.resetBerserkDamage();
		// Reset terminator firing cooldown state.
		listeners.CustomItems.resetTerminatorCooldowns();
		// Reset class-ability (drop) cooldowns.
		listeners.CustomItems.resetAbilityCooldowns();
		// Clear any one-shot choreography still queued from a previous run before this one schedules its own.
		Utils.cancelAllScheduled();

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
			Necron.INSTANCE.armPlayerHandoff(() -> {
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

	/**
	 * Like {@link #runTAS} but runs ONLY the boss/server instructions — no fake-player routines, no player
	 * handoffs, no spectator sync — so real players can practice the boss fights and mechanics. Bosses still
	 * chain (e.g. {@code /practice boss} runs the full Maxor→Storm→Goldor→Necron gauntlet) because each boss's
	 * chainNext spawns the next; runPlayerHandoff is simply a no-op since no handoff is armed here.
	 *
	 * <p>Note: Maxor/Storm/Necron aggro a fake player (e.g. {@code Tank.get()}), so those expect the fake
	 * actors to be spawned (idle is fine). Goldor (terminals/patrol) needs no actors.
	 */
	public static void runPractice(World world, String section) {
		// Kick all fake actors — practice is for real players, who become the boss's aggro target.
		FakePlayerManager.stopCustomConnection();
		FakePlayerManager.kickAllFakes();
		WitherActions.setPracticeMode(true);
		// Clear any section splits from a previous run; this run records its own for the Wither-King scoreboard.
		WitherActions.clearSplits();
		// Clear game-mode-change tracking (the practice scoreboard's golden-name anti-cheat).
		WitherActions.clearGameModeChanges();
		// Reset Berserk's per-mob damage-ramp counters.
		listeners.CustomItems.resetBerserkDamage();
		// Reset terminator firing cooldown state.
		listeners.CustomItems.resetTerminatorCooldowns();
		// Reset class-ability (drop) cooldowns.
		listeners.CustomItems.resetAbilityCooldowns();

		// Practice runs ZERO player routines. Cancel any choreography still queued from a previous /tas, and
		// disarm every player-side handoff + the Watcher so the boss chain spawns each boss WITHOUT starting a
		// fake-player routine (the source of stray lines like "… used Spirit Mask!" and broken phase gating).
		Utils.cancelAllScheduled();
		Maxor.INSTANCE.armPlayerHandoff(null);
		Storm.INSTANCE.armPlayerHandoff(null);
		Goldor.INSTANCE.armPlayerHandoff(null);
		Necron.INSTANCE.armPlayerHandoff(null);
		Watcher.INSTANCE.arm(world, section.equals("all"), null);

		// Anchor the live overall-run timer at the first boss spawn (the "Overall" column reads it in practice).
		Utils.markRunStart();
		MovementAudit.cancelAll();
		Actions.cancelAllMovement();
		Server.serverSetup(world);
		Server.serverInstructions(world, section);
	}
}
