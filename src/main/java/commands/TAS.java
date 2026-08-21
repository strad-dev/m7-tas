package commands;

import instructions.Actions;
import instructions.Server;
import instructions.bosses.Watcher;
import instructions.bosses.WitherActions;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.maxor.Maxor;
import instructions.bosses.necron.Necron;
import instructions.bosses.storm.Storm;
// import instructions.players.*; // TAS-only player routines, disabled in the practice fork
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.BossScheduler;
import plugin.FakePlayerManager;
import plugin.MovementAudit;
import plugin.Utils;

/*
 * TAS
 * 1. Gives all NPCs the appropriate inventory
 * 2. Re-teleports all NPCs to their initial locations
 * 3. Re-spawns all mobs
 * 4. Runs the TAS script
 */
public class TAS implements CommandExecutor {

	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage(Utils.msg("Only players can run this"));
			return true;
		}

		// /tas is disabled in the practice fork (TAS removed). The command is also unregistered in M7tas.
		p.sendMessage(Utils.msg("<red>The TAS is disabled on the practice server."));
		return true;
	}

	/* TAS-only, disabled in the practice fork since it references the commented-out player routines.  Original in git history (main).
	public static void runTAS(World world, String section) {
		Map<String, Player> fakePlayers = FakePlayerManager.getFakePlayers();
		if(fakePlayers.isEmpty()) {
			Bukkit.broadcastMessage(ChatColor.RED + "Could not run TAS!  There are no actors.");
			return;
		}

		// A prior /m7practice may have left practice-mode aggro on, so turn it back off for a real TAS run.
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

		// Start the boss and world instructions BEFORE the player routines.  Both schedule their work for the same
		// tick (+60), and submitting this first makes the boss phase activate before the players' tick-0 device
		// interactions fire, e.g. Tank right-clicking the S3 arrow-align frame, which would otherwise hit an
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
			// handoff the next tick, so Maxor and every player's maxor() routine start together.
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

		// Arm the boss-to-boss player handoffs: when Maxor or Storm die and spawn the next boss (chainNext), each
		// player's storm() or goldor() routine starts that same tick.  This replaces the old hardcoded transition
		// ticks (storm(true)@496, goldor(true)@881) in the player scripts.
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
				Archer.witherKing();
				Berserk.witherKing();
				Healer.witherKing();
				Mage.witherKing();
				Tank.witherKing();
			});
		}

		// Restart spectator sync so it runs AFTER all instruction tasks in each tick
		Spectate.stopSpectatorSync();
		Spectate.startSpectatorSync();
	}
	*/

	/**
	 * Like runTAS but runs ONLY the boss and server instructions, with no fake-player routines, no player
	 * handoffs and no spectator sync, so real players can practice the boss fights and mechanics.  Bosses still
	 * chain (e.g. {@code /m7practice boss} runs the full Maxor→Storm→Goldor→Necron gauntlet) because each boss's
	 * chainNext spawns the next; runPlayerHandoff is simply a no-op since no handoff is armed here.
	 *
	 * <p>Note: Maxor/Storm/Necron aggro a fake player (e.g. {@code Tank.get()}), so those expect the fake
	 * actors to be spawned (idle is fine). Goldor (terminals/patrol) needs no actors.
	 */
	public static void runPractice(World world, String section) {
		runPractice(world, section, 60);
	}

	/**
	 * @param delayTicks pre-run "get into position" window before the section starts (default 60 = 3s; the
	 *   network plugin passes 400 = 20s when it warps a party in). Forwarded to {@link Server#serverInstructions}.
	 */
	public static void runPractice(World world, String section, int delayTicks) {
		// Kick all fake actors, because practice is for real players, who become the boss's aggro target.
		FakePlayerManager.stopCustomConnection();
		FakePlayerManager.kickAllFakes();
		WitherActions.setPracticeMode(true);
		// Clear any section splits from a previous run; this run records its own for the Wither-King scoreboard.
		WitherActions.clearSplits();
		// Remember which section this run is + mint a fresh run id, so the reports this run makes (the 300-score
		// milestone and the run-complete payload) can be recognised as coming from the same run.
		WitherActions.startRunTracking(section);
		// Clear game-mode-change tracking (the practice scoreboard's golden-name anti-cheat).
		WitherActions.clearGameModeChanges();
		// Reset Berserk's per-mob damage-ramp counters.
		listeners.CustomItems.resetBerserkDamage();
		// Reset terminator firing cooldown state.
		listeners.CustomItems.resetTerminatorCooldowns();
		// Reset class-ability (drop) cooldowns.
		listeners.CustomItems.resetAbilityCooldowns();
		// Reset the per-run crypt-farm guard.
		listeners.CustomItems.resetCrypts();
		// Reset ultra-realistic death state: no ghosts and no cheat-death cooldowns carried in from a previous run.
		death.Deaths.reset();

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
		// Purge every stray entity a prior (possibly aborted) run leaked before we spawn this run's own. Must come
		// BEFORE serverSetup (spawns minibosses) and serverInstructions (spawns bosses) so it never nukes what this
		// run just staged.  That is the same hardMobCleanup-then-serverSetup order /reset and /setup use.  It catches
		// untracked withers and crystals the targeted forceCleanups in serverSetup can't, since they only free
		// tracked refs.
		Server.hardMobCleanup();
		Server.serverSetup(world);
		Server.serverInstructions(world, section, delayTicks);
	}

	/**
	 * Cancels the current practice session: stops all scripted choreography + movement, disarms the
	 * boss chain so nothing re-spawns, turns practice mode off, and clears the boss entities.
	 */
	public static void endPractice(World world) {
		endPractice(world, true);
	}

	/**
	 * @param toSpectator whether to drop every player into spectator at the end, the idle state on m7.  Only
	 *   {@code death/Deaths}' party wipe passes false: it has already decided where the wiped party goes and what
	 *   game mode they are in, and flipping them to spectator here just to be undone a line later is a visible
	 *   flicker.  <b>The network is unaffected either way</b> - it dispatches this as a command (so it always gets
	 *   the spectator flip) and {@code M7Bridge.resetToSpectators} re-asserts spectator itself right afterwards.
	 */
	public static void endPractice(World world, boolean toSpectator) {
		WitherActions.setPracticeMode(false);
		// Before the mass spectator flip below, so a pending revival can't fight it, and so the saver durability
		// bars come off the masks rather than being saved into someone's loadout.
		death.Deaths.reset();
		instructions.clear.ClearManager.stop(world); // remove secrets/chests, restore hotbar map slot, stop HUD loop
		Utils.cancelAllScheduled();
		MovementAudit.cancelAll();
		Actions.cancelAllMovement();
		Maxor.INSTANCE.armPlayerHandoff(null);
		Storm.INSTANCE.armPlayerHandoff(null);
		Goldor.INSTANCE.armPlayerHandoff(null);
		Necron.INSTANCE.armPlayerHandoff(null);
		Watcher.INSTANCE.arm(world, false, null);
		// arm() only sets fields, so the Watcher's raw portal detector would keep scanning every player - including
		// the spectators we make below - and warp them to Maxor after the run ended.  forceCleanup stops it and
		// closes the portal.
		Watcher.forceCleanup();
		// Drop the boss lane before tearing the bosses down: Utils.cancelAllScheduled above cannot reach it, and its
		// un-held one-shots (Maxor's crystal respawn, the Wither King's) would otherwise fire into a dead session.
		BossScheduler.clearAll();
		// The only thing that calls each boss's resetState.  Without it an early end left Goldor's phase active with
		// its section gates still blown open, the core entrance an invisible barrier, and every boss's flags set.
		Maxor.INSTANCE.forceEndPhase();
		Storm.INSTANCE.forceEndPhase();
		Goldor.INSTANCE.forceEndPhase();
		Necron.INSTANCE.forceEndPhase();
		// Clear the boss entities + energy crystals + Wither King dragons/relics so the dungeon resets.
		for(org.bukkit.entity.Entity e : world.getEntities()) {
			if(e instanceof org.bukkit.entity.Wither
					|| e instanceof org.bukkit.entity.EnderCrystal
					|| e.getScoreboardTags().contains("SkyblockBoss")
					|| e.getScoreboardTags().contains("TASWitherKing")
					|| e.getScoreboardTags().contains("WitherKingDragon")
					|| e.getScoreboardTags().contains("TASWitherKingRelic")
					|| e.getScoreboardTags().contains("TASWatcher")) {
				e.remove();
			}
		}
		// Put all practicers back into spectator mode.
		if(toSpectator) {
			for(Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
				if(p.getGameMode() != org.bukkit.GameMode.SPECTATOR) p.setGameMode(org.bukkit.GameMode.SPECTATOR);
			}
		}
	}
}
