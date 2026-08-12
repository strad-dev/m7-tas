package instructions.bosses.necron;

import commands.Spectate;
import instructions.Server;
import instructions.bosses.CustomBossBar;
import instructions.bosses.WitherLord;
import instructions.bosses.witherking.WitherKing;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftWither;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import plugin.BossScheduler;
import plugin.Utils;

import java.util.Collections;
import java.util.Random;

/**
 * Necron, the fourth Wither Lord. Damage-driven like {@link instructions.bosses.maxor.Maxor},
 * {@link instructions.bosses.storm.Storm}, and {@link instructions.bosses.goldor.Goldor}: player
 * damage is intercepted via {@link #handleDamage} (hooked from {@code MiscListener.onWitherLordDamage})
 * and the phase transitions emerge from HP thresholds rather than fixed ticks.
 *
 * <p>Unlike the other three, Necron is the only Wither Lord <b>always damageable</b> between events,
 * he flies and chases a player (Maxor-style aggro) until his HP crosses a threshold, at which point he
 * enters a short immune interlude:
 * <ul>
 *   <li><b>80% HP</b> → frenzy: teleport to the middle, blind players, hold still for {@value #FRENZY_DURATION_TICKS}t.</li>
 *   <li><b>25% HP</b> → fireball attack ({@link #destroyPlatform}) in place for {@value #FIREBALL_DURATION_TICKS}t.</li>
 *   <li><b>5% HP</b>  → frenzy again ({@value #FRENZY_DURATION_TICKS}t).</li>
 *   <li><b>0% HP</b>  → death, then chain to the Wither King after {@value #DEATH_TO_WK_TICKS}t.</li>
 * </ul>
 * Only the fireball attack is modeled, because TAS DPS is high enough that no other real ability window
 * (wither-skull barrages, rotating beams, diamond swords, lightning) ever triggers.
 */
public final class Necron extends WitherLord {
	public static final Necron INSTANCE = new Necron();

	private static final int PRE_NECRON_TICKS = 2402;
	private static final Random random = new Random();
	private static final String[] FRENZY_START_MESSAGES = {"Sometimes when you have a problem, you just need to destroy it all and start again.", "WITNESS MY RAW NUCLEAR POWER!"};
	private static final String[] FRENZY_END_MESSAGES = {"ARGH!", "Let's make some space!"};

	// Aggro: mirrors Maxor's live-chase controller.
	private static final double AGGRO_STOP_DISTANCE = 3.0;
	private static final double AGGRO_Y_OFFSET = 1.0;
	private static final double AGGRO_MAX_SPEED = 0.5;

	// Interlude HP thresholds as fractions of max HP, consumed in order.
	private static final double[] THRESHOLD_FRACTIONS = {0.80, 0.25, 0.05};

	private static final int INTRO_END_TICK = 160;       // intro dialogue is exactly 160t; aggro + damageability begin here
	private static final int FRENZY_DURATION_TICKS = 140;
	private static final int FIREBALL_DURATION_TICKS = 60;
	private static final int DEATH_TO_WK_TICKS = 100;

	// Middle of the arena Necron snaps to for a frenzy (his spawn point).
	private static final double MIDDLE_X = 54.5, MIDDLE_Y = 66, MIDDLE_Z = 76.5;
	private static final float MIDDLE_YAW = 0f;
	private static final float MIDDLE_PITCH = 0f;

	// Platform top-center blocks (y=63).  If they are all AIR the platform is already destroyed, which guards the
	// intro destroy.
	private static final int PLATFORM_Y = 63;
	private static final int PLATFORM_X1 = 53, PLATFORM_X2 = 55;
	private static final int PLATFORM_Z1 = 113, PLATFORM_Z2 = 115;

	// Per-fight state.
	private int eventsDone;          // 0 → 80% pending, 1 → 25% pending, 2 → 5% pending, 3 → none left (death only)
	private boolean inInterlude;     // immune window (frenzy or fireball), so damage is rejected
	private boolean damageable;      // false during the intro and during any interlude
	// Immune-interlude end one-shot, run as a boss-lane task (BossScheduler.schedule) so damageability is restored
	// at the start of its tick.  A beam on that tick sees the boss damageable again, not a tick late.
	private Runnable interludeEndTask;

	// Action-bar HUD (see updateActionBar), on its own boss ticker for the whole phase.
	private Runnable barTicker;
	// Phase tick the current interlude ends on, and which of the two it is, so the bar counts down the same clock
	// interludeEndTask fires on rather than a counter of its own.
	private int interludeEndTick;
	private boolean interludeIsFireball;
	// Whether the bar currently shows one of our segments, so the damageable stretch between interludes clears it
	// once instead of broadcasting an empty bar to everyone every tick.
	private boolean barShown;

	private Necron() {
		register(this);
	}

	/** Static facade for the boss-chain. */
	public static void necronInstructions(World world, boolean doContinue) {
		INSTANCE.start(world, doContinue);
	}

	@Override protected String name() { return "Necron"; }
	@Override protected String displayName() { return "Necron"; }
	@Override protected Location spawnLocation() { return new Location(world, 54.5, 66, 76.5, 0f, 0f); }
	@Override protected double maxHealth() { return damage.MobStats.NECRON.internalHealth(); }
	@Override protected String displayHealth() { return "1.4B"; }
	@Override protected int previousTicks() { return PRE_NECRON_TICKS; }

	@Override
	protected void resetState() {
		cancelInterludeEndTask();
		cancelBarTicker();
		if(boss != null) clearAggro();
		CustomBossBar.removeStunIndicator();
		eventsDone = 0;
		inInterlude = false;
		damageable = false;
		interludeEndTick = 0;
		interludeIsFireball = false;
	}

	@Override
	protected void onStart() {
		startBarTicker();


		// Goldor's section ends as Necron spawns, so record its end tick for the Wither-King practice scoreboard.
		instructions.bosses.WitherActions.recordSplit("Goldor", Utils.runTick());
		// --- Intro (160t): dialogue + a guarded platform destroy. Necron is not yet damageable and does not fly. ---
		sendChatMessage("You went further than any human before, congratulations.");
		Utils.scheduleTask(() -> {
			sendChatMessage("I'm afraid your journey ends now.");
			destroyPlatform(true); // intro salvo, which may destroy the platform (guarded by platformIntact)
		}, 60);
		Utils.scheduleTask(() -> sendChatMessage("Goodbye."), 120);

		// --- After intro: drop armor, become damageable, and start the Maxor-style aggro chase. ---
		Utils.scheduleTask(() -> {
			setArmor(false);
			damageable = true;
			setAggro(AGGRO_STOP_DISTANCE, AGGRO_Y_OFFSET, AGGRO_MAX_SPEED);
			// The ??? "damageable" indicator is shown ONLY after a frenzy ends, never after the intro and never
			// after the fireball attack (see endInterlude).
			sendChatMessage("That's a very impressive trick.  I guess I'll have to handle this myself.");
		}, INTRO_END_TICK);
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			WitherKing.witherKingInstructions(world, false);
			runPlayerHandoff();
		} else {
			instructions.bosses.WitherActions.signalRunComplete(); // Necron was the last boss of this practice (no Wither-King)
		}
	}

	// ---------- Damage / interludes ----------

	/** Damage interceptor, hooked from {@code MiscListener.onWitherLordDamage}. Necron is always damageable
	 *  except during the intro and the immune interludes. Each threshold (80% / 25% / 5%) is consumed in order:
	 *  a hit that would cross the next threshold is clamped exactly to it and triggers that interlude; a hit at
	 *  0% kills. Modeled on {@link instructions.bosses.storm.Storm#handleDamage}. */
	@Override
	public double clampDamage(double incoming) {
		if(boss == null) return incoming;
		if(dying) return 0;
		if(incoming <= 0) return 0;

		if(inInterlude) {
			// Like Goldor on patrol, Necron stays "damageable" during a frenzy or fireball interlude.  Arrows
			// connect and the hurt flash shows, but the hit never reduces his health.  Blocking the damage
			// suppresses the flash, so render the hurt animation ourselves.
			Utils.broadcastPacket(new ClientboundHurtAnimationPacket(((CraftWither) boss).getHandle()));
			return 0;
		}
		if(!damageable) return 0; // Intro (pre-fight): fully immune, no feedback.

		double currentHp = boss.getHealth();
		double threshold = nextThreshold();

		if(currentHp - incoming <= threshold) {
			if(threshold <= 0.0) {
				// Killing blow: clamp to leave DYING_SLIVER so vanilla doesn't death-despawn the wither before the
				// death dialogue.  enterDyingState pins HP there too, shown as "1" via TASDying.  This used to leave
				// 1% of max - 14M on Necron - which the killing blow then visibly failed to deal.
				enterDyingState();
				return Math.max(0, currentHp - DYING_SLIVER);
			}
			// Clamp the hit so HP lands exactly on the threshold, then start that interlude.
			triggerInterlude(eventsDone);
			return currentHp - threshold;
		}
		return incoming; // otherwise the hit passes through unmodified
	}

	/** Next HP value (absolute) at which the upcoming interlude fires, or 0 (death) once all are consumed. */
	private double nextThreshold() {
		double maxHp = maxHealth();
		if(eventsDone < THRESHOLD_FRACTIONS.length) return maxHp * THRESHOLD_FRACTIONS[eventsDone];
		return 0.0;
	}

	/** Start the immune interlude for the just-crossed threshold. idx 0 & 2 → frenzy, idx 1 → fireball attack. */
	private void triggerInterlude(int idx) {
		inInterlude = true;
		damageable = false;
		eventsDone++;

		clearAggro();
		// Keep the wither shield DOWN during the interlude, like Goldor on patrol, so arrows still connect for
		// feedback.  handleDamage cancels the damage so no health is actually lost.
		setArmor(false);
		CustomBossBar.removeStunIndicator(); // immune now, so drop the "damageable" ??? indicator

		int duration;
		if(idx == 1) {
			// 25%: fireball attack in place, with no teleport and no blindness.
			duration = FIREBALL_DURATION_TICKS;
			Utils.timer("<green>Necron fireball attack at " + formatTick(displayTick()));
			destroyPlatform(false); // 25% replay: fireballs only, never destroy the platform
		} else {
			// 80% and 5% are the frenzy: teleport to the middle, blind players, hold still.
			duration = FRENZY_DURATION_TICKS;
			moveBossToCenter();
			sendChatMessage(FRENZY_START_MESSAGES[random.nextInt(FRENZY_START_MESSAGES.length)]);
			applyBlindness();
			frenzySounds(duration);
			Utils.timer("<green>Necron frenzy at " + formatTick(displayTick()));
		}

		cancelInterludeEndTask();
		interludeEndTask = BossScheduler.schedule(() -> endInterlude(idx), duration);

		// Action-bar anchors, then a re-render: this runs from the damage path, long after the HUD ticker drew this
		// tick's bar from the pre-interlude state.
		interludeEndTick = displayTick() + duration;
		interludeIsFireball = idx == 1;
		updateActionBar();
	}

	/** Interlude over, so resume the chase and become damageable again. */
	private void endInterlude(int idx) {
		if(dying || boss == null || !boss.isValid()) return;
		inInterlude = false;
		damageable = true;
		setArmor(false);
		if(idx != 1) {
			// The ??? "damageable" indicator and the frenzy-end line are shown ONLY after a frenzy (idx 0 or 2),
			// never after the fireball attack (idx 1) or the intro.
			CustomBossBar.spawnAnimatedStunnedIndicator(boss, Integer.MAX_VALUE);
			sendChatMessage(FRENZY_END_MESSAGES[random.nextInt(FRENZY_END_MESSAGES.length)]);
		}
		// After the FIRST frenzy (idx 0) Necron stays planted at the middle with AI off, so the upcoming 25%
		// fireball attack finds him already at the correct spot. He resumes the chase after the other interludes.
		if(idx != 0) setAggro(AGGRO_STOP_DISTANCE, AGGRO_Y_OFFSET, AGGRO_MAX_SPEED);

		// Clear the counter on the tick the interlude really ends: the HUD ticker is registered first, so it already
		// drew this tick's bar (at 0t) before this ran.
		updateActionBar();
	}

	private void cancelInterludeEndTask() {
		if(interludeEndTask != null) BossScheduler.removeTicker(interludeEndTask);
		interludeEndTask = null;
	}

	// ---------- Action-bar tick timers ----------

	/**
	 * Per-tick action-bar QoL for the Necron phase, the same HUD slot as Storm's pad/crush bar and Maxor's
	 * laser/stun one.  One segment at a time, and every one of them counts down a window Necron is IMMUNE for -
	 * which is the only thing worth knowing here, since between them he is simply damageable and chasing:
	 * <ul>
	 *   <li><b>Damageable In</b>: the intro, {@link #INTRO_END_TICK}t of dialogue before the armour drops and the
	 *       chase starts.</li>
	 *   <li><b>Frenzy</b>: the 80% and 5% interludes ({@link #FRENZY_DURATION_TICKS}t).</li>
	 *   <li><b>Fireballs</b>: the 25% interlude ({@link #FIREBALL_DURATION_TICKS}t).</li>
	 * </ul>
	 * The interlude counter runs off a phase tick stamped in {@link #triggerInterlude}, so it can't drift from the
	 * boss-lane task that actually ends the window.  Sent to every real player, spectators included, and the fakes
	 * are skipped.  It doesn't collide with {@code ClearManager}'s bar: that one bails out for anyone outside the
	 * dungeon room grid, which the arena is.
	 */
	private void updateActionBar() {
		int t = displayTick();
		String bar;
		if(inInterlude) {
			int left = Math.max(0, interludeEndTick - t);
			bar = interludeIsFireball ? "<gold>Fireballs <white>" + left + "t" : "<red>Frenzy <white>" + left + "t";
		} else if(!damageable) {
			bar = "<yellow>Damageable In <white>" + Math.max(0, INTRO_END_TICK - t) + "t";
		} else {
			// Damageable and chasing: nothing to count, so clear once on the way in rather than broadcasting an
			// empty bar to everyone every tick.
			if(barShown) {
				barShown = false;
				Utils.broadcastActionBar(Component.empty());
			}
			return;
		}
		barShown = true;
		Utils.broadcastActionBar(Utils.msg(bar));
	}

	private void startBarTicker() {
		cancelBarTicker();
		barTicker = new Runnable() {
			@Override
			public void run() {
				if(boss == null || boss.isDead()) {
					BossScheduler.removeTicker(this);
					barTicker = null;
					barShown = false;
					Utils.broadcastActionBar(Component.empty());
					return;
				}
				updateActionBar();
			}
		};
		BossScheduler.addTicker(barTicker);
	}

	private void cancelBarTicker() {
		if(barTicker != null) {
			BossScheduler.removeTicker(barTicker);
			barTicker = null;
		}
		// Wipe the HUD instead of letting the last "Frenzy 1t" sit on screen for its fade-out.
		barShown = false;
		Utils.broadcastActionBar(Component.empty());
	}

	// ---------- Movement (snap to middle for a frenzy) ----------

	private void moveBossToCenter() {
		net.minecraft.world.entity.LivingEntity nms = ((CraftWither) boss).getHandle();
		nms.absSnapTo(Necron.MIDDLE_X, Necron.MIDDLE_Y, Necron.MIDDLE_Z, Necron.MIDDLE_YAW, Necron.MIDDLE_PITCH);
		nms.setYHeadRot(Necron.MIDDLE_YAW); // undo the aggro look-control so the frenzy faces cleanly forward
		nms.setDeltaMovement(Vec3.ZERO);
		nms.hurtMarked = true;
		PositionMoveRotation pmr = new PositionMoveRotation(new Vec3(Necron.MIDDLE_X, Necron.MIDDLE_Y, Necron.MIDDLE_Z), Vec3.ZERO, Necron.MIDDLE_YAW, Necron.MIDDLE_PITCH);
		ClientboundTeleportEntityPacket pkt = ClientboundTeleportEntityPacket.teleport(nms.getId(), pmr, Collections.emptySet(), nms.onGround());
		Utils.broadcastPacket(pkt);
	}

	// ---------- Frenzy effects ----------

	/** Blind every real, non-spectating player for 1 second at the start of a frenzy. Fake players are client-less
	 *  (no-op); spectating viewers are skipped so the spectated view isn't disrupted. */
	private void applyBlindness() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(Spectate.isSpectating(p)) continue;
			p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false)); // 1 second
		}
	}

	/** Explosion + wither-ambient pulses across the frenzy window. */
	private void frenzySounds(int duration) {
		Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE);
		for(int t = 20; t < duration; t += 20) {
			Utils.scheduleTask(() -> {
				Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
				Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
			}, t);
		}
	}

	// ---------- Platform destroy / fireball attack ----------

	/** Fireball salvo.  Only the intro salvo ({@code allowDestroy=true}) may swap the platform to its destroyed
	*  variant, and only if it's still intact at the destroy tick; a platform the players broke open before then is
	*  left alone.  The 25% replay ({@code allowDestroy=false}) is fireballs only and never touches the platform. */
	private void destroyPlatform(boolean allowDestroy) {
		// Necron is already stationary at the correct spot for both salvos.  The intro one fires from his spawn,
		// and the 25% one fires from the middle where the first frenzy planted him, since AI is not re-enabled
		// after that frenzy, so there's no chase momentum to cancel here.
		shootFireball();
		Utils.scheduleTask(this::shootFireball, 10);
		Utils.scheduleTask(this::shootFireball, 20);
		Utils.scheduleTask(this::shootFireball, 30);
		Utils.scheduleTask(() -> {
			shootFireball();
			// Evaluate intactness HERE, on the destroy tick, so blocks broken any time before this are honoured.
			// Destroyed variant lives at y -10..-6 (correct variant at y -5..-1); clone it up to the live platform.
			if(allowDestroy && platformIntact()) Utils.runCommand("clone 70 -10 120 38 -6 99 38 59 99");
		}, 40);
		Utils.scheduleTask(this::shootFireball, 50);
		Utils.scheduleTask(this::shootFireball, 60);
		Utils.scheduleTask(this::shootFireball, 70);
	}

	/** True unless every platform top-center block (53..55, 63, 113..115) is AIR (i.e. already destroyed). */
	private boolean platformIntact() {
		for(int x = PLATFORM_X1; x <= PLATFORM_X2; x++) {
			for(int z = PLATFORM_Z1; z <= PLATFORM_Z2; z++) {
				if(world.getBlockAt(x, PLATFORM_Y, z).getType() != Material.AIR) return true;
			}
		}
		return false;
	}

	private void shootFireball() {
		Fireball fireball = (Fireball) world.spawnEntity(boss.getLocation().add(0, 3, 0), EntityType.FIREBALL);
		fireball.setVelocity(new Vector(0, -0.25, 1.25));
		Utils.scheduleTask(fireball::remove, 21);
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT);
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
	}

	// ---------- Death ----------

	private void enterDyingState() {
		dying = true;
		boss.addScoreboardTag("TASDying");
		cancelInterludeEndTask();
		cancelBarTicker();
		inInterlude = false;
		damageable = false;
		clearAggro();
		setArmor(false);
		CustomBossBar.removeStunIndicator();
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) {
				try { boss.setHealth(DYING_SLIVER); } catch (IllegalArgumentException ignored) {}
				Utils.changeName(boss);
			}
		}, 1);
		playDeathDialogue();
	}

	private void playDeathDialogue() {
		final int deathTick = displayTick(); // Necron-relative tick of the final blow (t=0 of the death sequence)
		sendChatMessage("All this, for nothing...");
		Server.playWitherDeathSound(boss);
		Utils.timer("<green>Necron killed in " + formatTick(displayTick()));
		// Open the wall to the Wither King's arena 200t after the killing blow (restored on the next /reset).
		Utils.scheduleTask(instructions.bosses.BossTransition::openNecronToWitherKing, 200);
		Utils.scheduleTask(() -> sendChatMessage("I understand your words now, my master."), 60);
		// note: In most mods, the Necron timer ends 2 seconds too early, making Wither King start 2 seconds too early.
		// This TAS fixes that. To compare to those timers, subtract 2 seconds here and add 2 seconds to Wither King time.
		Utils.scheduleTask(() -> {
			Utils.timer("<green>Necron finished in " + formatTick(displayTick()));
			// Stamp the leaderboard duration at the phase's real end (this tick), not the killing blow.  It must
			// come before chainNext, which starts the Wither King and re-anchors the phase clock.
			instructions.bosses.WitherActions.recordPhaseDuration("Necron", displayTick());
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
			chainNext(doContinue);
		}, DEATH_TO_WK_TICKS);
		Utils.scheduleTask(() -> sendChatMessage("The Catacombs... are no more."), DEATH_TO_WK_TICKS + 20);

		/*
		 * note: all of the wither partitions are one-ticked in this TAS, matching DPS achieved in normal f7
		 * thus, there are no timesaves available in normal f7 VS master mode m7
		 */
		// A normal F7 completes 140t after the final blow, i.e. 40t after the t=100 phase transition, which matches
		// the DEATH_TO_WK_TICKS + 40 print delay below, and not on the death tick itself.  Add that offset OUTSIDE
		// overallTick(): in practice mode overallTick() reports the LIVE run tick and ignores its argument, so a
		// forward projection has to start from the overall DEATH tick and add the 140t death→finish gap itself.
		final int normalF7Overall = overallTick(deathTick) + DEATH_TO_WK_TICKS + 40;
		Utils.scheduleTask(() -> {
			double secs = normalF7Overall / 20.0;
			int mins = (int) (secs / 60);
			double rem = secs - mins * 60.0;
			Bukkit.broadcast(Utils.msg("<gold>Normal Floor 7 Finishes Here in " + formatWithSpaces(normalF7Overall)
					+ " ticks (" + String.format("%.2f", secs) + " seconds | " + mins + ":" + String.format("%05.2f", rem) + ")"));
		}, DEATH_TO_WK_TICKS + 40);
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) boss.remove();
		}, DEATH_TO_WK_TICKS + 60);
	}
}
