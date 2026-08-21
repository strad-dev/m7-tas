package death;

import instructions.Server;
import instructions.bosses.WitherActions;
import instructions.clear.ClearManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.FakePlayerManager;
import plugin.M7tas;
import plugin.PlayerInventoryBackup;
import plugin.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Death and revival, the one thing ultra-realistic mode adds that the rest of the plugin models as impossible
 * (MAP.md § Ultra-realistic).  Players stay invulnerable in every mode - <b>nothing here is HP-driven</b>.  Every
 * death is an explicit instakill reported by a mechanic:
 * <ul>
 *   <li>Storm's lightning volley, when a player is not fully under a pillar ({@code storm/Storm.strikeUnsheltered});</li>
 *   <li>a Storm pillar closing on a player ({@code storm/Storm.pollPlayerCrush});</li>
 *   <li>Goldor's 60-tick invalid-location sweep ({@code goldor/Goldor.pollInvalidLocations});</li>
 *   <li>a relic placed in the wrong cauldron ({@code listeners/WitherKingListener}).</li>
 * </ul>
 *
 * <p><b>{@link #kill} is the only way in</b>, and it owns the whole decision: the mode gate, the run gate, who
 * counts as killable, the {@link CheatDeath} proc and the wipe check.  A mechanic says "this player should die
 * now" and nothing else - do not let a call site pre-screen any of it, or the five sites will drift.
 *
 * <p><b>A ghost is a vanilla spectator.</b>  That is deliberate: {@code Utils.isSpectator} is already the one gate
 * every player-driven mechanic checks, so becoming a ghost locks a player out of terminals, devices, relics,
 * crystals, chests and pads with no new checks anywhere.  Two consequences worth knowing:
 * <ul>
 *   <li>{@code ClearManager.isRealPlayer} goes false, so a ghost drops off the HUD and out of {@code realPlayers()}.
 *       That is why {@link #kill} banks them on the run roster ({@code WitherActions.noteInRun}) BEFORE the game-mode
 *       flip: the roster is what the network derives group size from, and a player who died at 4:00 is still a
 *       participant.  Same reasoning as the quit hook.</li>
 *   <li>{@code MiscListener.onGameModeChange} would read the flip as a manual mode change and burn the player's
 *       golden name, so the two flips this class makes are announced with {@link #expectGameModeChange}.</li>
 * </ul>
 *
 * <p><b>Revival is automatic and self-served.</b>  Infinite Revive Stones are assumed, so a ghost is always coming
 * back: the countdown starts on the tick they die and {@link #REVIVE_TICKS} later they are restored where they are
 * standing, with the inventory they died with.  They revive themselves, so the revival line names them twice.
 *
 * <p><b>A party wipe is the exception to all of that</b> ({@link #wipe}).  The last death never produces a ghost at
 * all - there is nothing to come back to - so nobody is flipped into spectator only to be flipped straight back.
 * The wipe ends the practice session itself, restores every earlier ghost's game mode and inventory, and gathers
 * the whole party on one spot.
 */
public final class Deaths {
	private Deaths() {}

	/** How long a revival takes, start to finish. */
	private static final int REVIVE_TICKS = 100;
	/** How long each countdown title stays up: one tick longer than its 20-tick slot, so the titles don't flicker apart. */
	private static final int TITLE_STAY_TICKS = 21;

	/**
	 * One dead player, keyed by uuid and held in death order.
	 *
	 * @param inventory what they were carrying, restored verbatim on revival
	 * @param gameMode  the mode they died in, so revival puts them back in it rather than assuming Adventure
	 * @param diedAt    absolute server tick of the death, the anchor the countdown and the revival both read
	 */
	private record Ghost(UUID uuid, PlayerInventoryBackup inventory, GameMode gameMode, int diedAt) {}

	private static final Map<UUID, Ghost> ghosts = new LinkedHashMap<>();

	/** Players whose next {@code PlayerGameModeChangeEvent} is ours, not theirs.  Consumed by {@link #ownsGameModeChange}. */
	private static final java.util.Set<UUID> expectedModeChange = new java.util.HashSet<>();

	private static BukkitTask driver;

	// ==================== the kill ====================

	/**
	 * Kill {@code p}, attributed to {@code killer}, unless something saves them.
	 *
	 * @param killer the mob name for the death message, as plain text (inserted unparsed, so it can never carry tags)
	 * @return true if the player actually became a ghost.  False covers every refusal alike - wrong mode, no live
	 *         run, not a runner, already dead, or a {@link CheatDeath} proc - because no call site has anything
	 *         different to do about them.
	 */
	public static boolean kill(Player p, String killer) {
		if(!damage.Difficulty.deathsEnabled()) return false;
		if(!Server.isRunStarted()) return false;
		if(!appliesTo(p)) return false;

		// They were HIT.  This is the cue for the hit, not for the death, so it fires before anything decides
		// whether the hit lands: a blocked one is still a hit, and a mask proc with no sound behind it reads as
		// nothing having happened at all.
		playHurtSound(p);

		if(CheatDeath.tryProc(p)) return false;

		// Bank the participant while they are still readable as one: a ghost is out of realPlayers(), and the run
		// payload's roster is what the leaderboards derive group size from.
		WitherActions.noteInRun(p);

		announceDeath(p, killer);
		// The party-wide cue that somebody is DOWN, as opposed to the positional hurt sound above, which only says
		// somebody was hit.  playGlobalSound plays AT each listener, so this carries wherever they are in the arena.
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2.0f, 0.5f);
		Utils.debug(Utils.DebugType.SERVER, Utils.getRealName(p) + " was killed by " + killer);

		// Asked BEFORE any ghosting: if this is the last one standing there is nothing to come back to, so they
		// never become a ghost at all.  Flipping them into spectator only for the wipe to flip them straight back
		// is a visible flicker and a pointless inventory round trip.
		if(isLastAlive(p)) {
			wipe(p);
			return true;
		}

		ghosts.put(p.getUniqueId(),
				new Ghost(p.getUniqueId(), new PlayerInventoryBackup(p), p.getGameMode(), Utils.serverTick()));
		// Hand back anything they were holding open - a terminal puzzle is one player's at a time, and a ghost
		// must not be sitting on it.  The close handler clears the terminal's pending flag.
		p.closeInventory();
		expectGameModeChange(p);
		p.setGameMode(GameMode.SPECTATOR);
		showReviveTitle(p, REVIVE_TICKS);
		return true;
	}

	/**
	 * Who an instakill applies to: someone actually running the phase.  Public so a mechanic can gate its own
	 * VISUALS on the same answer {@link #kill} will give - Storm's lightning bolt should not spawn on a spectator -
	 * without pre-screening the kill itself, which stays this class's decision alone.
	 * <p>
	 * SURVIVAL and ADVENTURE only, an allowlist rather than "not creative", for exactly the reason
	 * {@code OutOfBounds} uses the same one - creative is the map-editing mode and is exempt from every protection,
	 * and there is no game mode this should newly apply to without someone saying so.  Spectators are excluded twice
	 * over (by mode, and by the plugin's own spectate state, which leaves the player in Adventure), which also
	 * covers a player who is already a ghost.
	 */
	public static boolean appliesTo(Player p) {
		if(p == null || !p.isOnline()) return false;
		GameMode gm = p.getGameMode();
		if(gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) return false;
		if(Utils.isSpectator(p)) return false;
		return !FakePlayerManager.getFakePlayers().containsValue(p);
	}

	/**
	 * The "you were hit" cue: the vanilla player-hurt sound at the player's own position, so they hear it and so does
	 * anybody standing near them.
	 * <p>
	 * <b>World-positioned, not {@code Utils.playLocalSound}</b> - that one only ever reaches the single player, and
	 * the point here is that a teammate nearby hears where it happened.  Volume 1 for the ordinary vanilla radius.
	 */
	private static void playHurtSound(Player p) {
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
	}

	/** True if {@code p} is currently dead and waiting on a revival. */
	public static boolean isGhost(Player p) {
		return p != null && ghosts.containsKey(p.getUniqueId());
	}

	/**
	 * True if {@code p} is the only one left who could still finish the run, i.e. their death is a party wipe.
	 * <p>
	 * Asked while {@code p} is still in Adventure, so they are themselves still in
	 * {@code ClearManager.realPlayers()} - the question is whether anybody ELSE is.  That list already excludes
	 * spectators (so every existing ghost) and fakes; it is the one definition of "in the run" the roster and the
	 * clear HUD share, rather than a fourth opinion about it.  A creative-mode onlooker counts as alive and will
	 * hold a run open, the same way they are exempt from every other protection.
	 */
	private static boolean isLastAlive(Player p) {
		for(Player other : ClearManager.realPlayers()) {
			if(!other.equals(p)) return false;
		}
		return true;
	}

	// ==================== the wipe ====================

	/** Where a wiped party is put down, facing yaw 0 / pitch 0. */
	private static final double[] WIPE_RETURN = {28.5, 166, 118.5};

	/**
	 * Everybody is dead, so the run is over and lost.  {@code lastToDie} never became a ghost (see {@link #kill}).
	 * <p>
	 * <b>This ends the session itself</b>, which the other failure path does not: standalone nothing listens to the
	 * run-complete event, so a wiped party would otherwise be stranded as spectators inside a run that never stops -
	 * bosses still ticking, Goldor still patrolling, and no way to start another.  So it does what
	 * {@code /m7practice end} does, and then hands everybody their body back: game mode restored, inventory
	 * restored, and the whole party teleported to one spot rather than scattered around a torn-down boss room.
	 * <p>
	 * Order matters in three places.  The ghosts are restored <b>before</b> the teardown, so
	 * {@code ClearManager.stop}'s hotbar cleanup sees them as real players instead of skipping them.  The
	 * run-complete signal also goes out <b>before</b> the teardown, while the run's state is still intact for
	 * {@code RunResult.capture}.  And {@code endPractice} is passed {@code toSpectator = false}, because this method
	 * has already decided where everyone goes.
	 * <p>
	 * The signal is the same one Storm's all-pillars-gone failure uses, so the network still frees its slot and runs
	 * its own session end - which re-asserts spectator, and that is its call: spectator is the m7 idle state.  The
	 * payload still carries every phase duration and clear milestone the party reached, which is what lets the
	 * leaderboards keep the sections they finished (see {@code Leaderboards.submit}).
	 */
	private static void wipe(Player lastToDie) {
		World world = lastToDie.getWorld();
		Bukkit.broadcast(Utils.msg("<red>Your whole party is dead!  You failed the run."));

		// Everyone to hand back: the ghosts, plus the one who just died without becoming one.
		List<Player> party = new ArrayList<>();
		party.add(lastToDie);
		for(Ghost g : ghosts.values()) {
			Player p = Bukkit.getPlayer(g.uuid());
			if(p == null || !p.isOnline()) continue; // banked on the roster already; a relog re-enters normally
			expectGameModeChange(p);
			p.setGameMode(g.gameMode());
			g.inventory().restore(p);
			p.clearTitle();
			party.add(p);
		}
		ghosts.clear(); // no revivals: there is nothing left to come back to

		WitherActions.signalRunComplete(false);
		commands.TAS.endPractice(world, false);

		Location home = new Location(world, WIPE_RETURN[0], WIPE_RETURN[1], WIPE_RETURN[2], 0f, 0f);
		for(Player p : party) p.teleport(home, PlayerTeleportEvent.TeleportCause.PLUGIN);
	}

	// ==================== revival ====================

	/**
	 * The per-tick driver: revival countdowns, the revivals themselves, the 20-tick durability refresh, and the
	 * action-bar fallback.
	 * <p>
	 * Registered from {@code M7tas.onEnable} as a raw repeating task, so it is untracked and survives
	 * {@code Utils.cancelAllScheduled} - a revival must not be cancellable by a boss teardown flushing the
	 * scheduler, or a ghost would be stranded in spectator for the rest of the run.
	 */
	public static void start() {
		if(driver != null) return;
		driver = new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
	}

	public static void stop() {
		if(driver != null) {
			driver.cancel();
			driver = null;
		}
		ghosts.clear();
	}

	private static void tick() {
		int now = Utils.serverTick();

		if(!ghosts.isEmpty()) {
			// Copy, because reviving mutates the map.
			for(Ghost g : ghosts.values().toArray(new Ghost[0])) {
				int elapsed = now - g.diedAt();
				Player p = Bukkit.getPlayer(g.uuid());
				if(p == null || !p.isOnline()) continue; // banked as a participant already; a relog re-enters normally
				if(elapsed >= REVIVE_TICKS) revive(p, g);
				else if(elapsed > 0 && elapsed % 20 == 0) showReviveTitle(p, REVIVE_TICKS - elapsed);
			}
		}

		// EVERY tick, not on a 20-grid: CheatDeath throttles the DRAWING itself and needs to hear about the tick a
		// cooldown actually expires on, so a ready mask never sits there showing a part-empty bar.
		if(damage.Difficulty.deathsEnabled()) {
			for(Player p : Bukkit.getOnlinePlayers()) {
				if(FakePlayerManager.getFakePlayers().containsValue(p)) continue;
				CheatDeath.refreshDurability(p);
			}
		}

		// Cooldowns have to keep ticking down on screen through a stretch no boss HUD owns - a phase without one, or
		// the gap between phases.  Only fill in the ticks nobody else claimed, so a live HUD is never fought over:
		// Utils.sendActionBar stamps the tick, and every boss HUD runs at the start of the tick, ahead of this.
		//
		// PASS AN EMPTY BASE.  sendActionBar is the thing that appends the cooldown segments, and handing it the
		// segments as the bar to append them TO printed every timer twice.
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(FakePlayerManager.getFakePlayers().containsValue(p)) continue;
			if(Utils.actionBarOwnedThisTick(p)) continue;
			if(CheatDeath.hasCooldowns(p)) Utils.sendActionBar(p, Component.empty());
		}
	}

	/** The countdown title.  No fade in or out - it is a clock, and a fade makes it look like it is drifting. */
	private static void showReviveTitle(Player p, int ticksLeft) {
		int seconds = ticksLeft / 20;
		p.showTitle(Title.title(
				Utils.msg("<yellow>BEING REVIVED"),
				Utils.msg("<green>You will be revived in " + seconds + "s."),
				Title.Times.times(Duration.ZERO, Duration.ofMillis(TITLE_STAY_TICKS * 50L), Duration.ZERO)));
	}

	/**
	 * Put a ghost back in the run, where they are standing.
	 * <p>
	 * <b>No teleport.</b>  A ghost has spent five seconds flying around as a spectator and revives at whatever
	 * position that left them in, which is what makes the death cost distance as well as time.  If that position is
	 * out of bounds, {@code OutOfBounds} kills them a tick later, the same as it would anyone else standing there.
	 */
	private static void revive(Player p, Ghost g) {
		ghosts.remove(g.uuid());
		expectGameModeChange(p);
		p.setGameMode(g.gameMode());
		g.inventory().restore(p);
		p.clearTitle();

		String name = "<gold>" + Utils.getRealName(p);
		Bukkit.broadcast(Utils.msg("<green> ❣ " + name + " <green>was revived by " + name + "<green>!"));
		Utils.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
	}

	// ==================== messages ====================

	/**
	 * The two death lines: what the dead player reads ("You were killed by …") and what everyone else reads
	 * ("&lt;name&gt; was killed by …").  Two different messages about one event, so this is sent per player rather
	 * than broadcast.  Names and the mob go in unparsed, so neither can inject MiniMessage tags.
	 */
	private static void announceDeath(Player p, String killer) {
		TagResolver mob = Placeholder.unparsed("mob", killer);
		TagResolver who = Placeholder.unparsed("who", Utils.getRealName(p));
		for(Player other : Bukkit.getOnlinePlayers()) {
			if(FakePlayerManager.getFakePlayers().containsValue(other)) continue;
			other.sendMessage(other.equals(p)
					? Utils.msg("<red> ☠ <gray>You were killed by <red><mob> <gray>and became a ghost.", mob)
					: Utils.msg("<red> ☠ <gold><who> <gray>was killed by <red><mob> <gray>and became a ghost.", who, mob));
		}
	}

	// ==================== lifecycle ====================

	/**
	 * Announce that the next game-mode change on {@code p} is this class's, not the player's, so
	 * {@code MiscListener.onGameModeChange} does not count it against their golden name.  Dying is not cheating.
	 */
	private static void expectGameModeChange(Player p) {
		expectedModeChange.add(p.getUniqueId());
	}

	/**
	 * True if the game-mode change now being reported for {@code id} is one this class made.  <b>Consumes the
	 * flag</b>, so the very next change is the player's own again.
	 */
	public static boolean ownsGameModeChange(UUID id) {
		return expectedModeChange.remove(id);
	}

	/** A ghost who logged out can't be revived; they stay on the roster, banked by the quit hook. */
	public static void onQuit(Player p) {
		ghosts.remove(p.getUniqueId());
		expectedModeChange.remove(p.getUniqueId());
	}

	/**
	 * Clear all death state.  Called at the start and the end of every practice session, so a run never inherits a
	 * previous one's ghosts or its saver cooldowns.
	 */
	public static void reset() {
		ghosts.clear();
		expectedModeChange.clear();
		CheatDeath.reset();
	}
}
