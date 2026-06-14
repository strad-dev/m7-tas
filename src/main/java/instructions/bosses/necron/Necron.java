package instructions.bosses.necron;

import commands.Spectate;
import instructions.Server;
import instructions.bosses.CustomBossBar;
import instructions.bosses.WitherLord;
import instructions.bosses.witherking.WitherKing;
import instructions.players.Tank;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftWither;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugin.M7tas;
import plugin.Utils;

import java.util.Collections;
import java.util.Random;

/**
 * Necron, the fourth Wither Lord. Damage-driven like {@link instructions.bosses.maxor.Maxor},
 * {@link instructions.bosses.storm.Storm}, and {@link instructions.bosses.goldor.Goldor}: player
 * damage is intercepted via {@link #handleDamage} (hooked from {@code MiscListener.onWitherLordDamage})
 * and the phase transitions emerge from HP thresholds rather than fixed ticks.
 *
 * <p>Unlike the other three, Necron is the only Wither Lord <b>always damageable</b> between events —
 * he flies and chases a player (Maxor-style aggro) until his HP crosses a threshold, at which point he
 * enters a short immune interlude:
 * <ul>
 *   <li><b>80% HP</b> → frenzy: teleport to the middle, blind players, hold still for {@value #FRENZY_DURATION_TICKS}t.</li>
 *   <li><b>25% HP</b> → fireball attack ({@link #destroyPlatform}) in place for {@value #FIREBALL_DURATION_TICKS}t.</li>
 *   <li><b>5% HP</b>  → frenzy again ({@value #FRENZY_DURATION_TICKS}t).</li>
 *   <li><b>0% HP</b>  → death, then chain to the Wither King after {@value #DEATH_TO_WK_TICKS}t.</li>
 * </ul>
 * Only the fireball attack is modeled — TAS DPS is high enough that no other real ability window
 * (wither-skull barrages, rotating beams, diamond swords, lightning) ever triggers.
 */
@SuppressWarnings("DataFlowIssue")
public final class Necron extends WitherLord {
	public static final Necron INSTANCE = new Necron();

	private static final int PRE_NECRON_TICKS = 2766;
	private static final Random random = new Random();
	private static final String[] FRENZY_START_MESSAGES = {"Sometimes when you have a problem, you just need to destroy it all and start again.", "WITNESS MY RAW NUCLEAR POWER!"};
	private static final String[] FRENZY_END_MESSAGES = {"ARGH!", "Let's make some space!"};

	// Aggro — mirror Maxor's live-chase controller.
	private static final double AGGRO_STOP_DISTANCE = 3.0;
	private static final double AGGRO_Y_OFFSET = 1.0;
	private static final double AGGRO_MAX_SPEED = 0.5;

	// Interlude HP thresholds as fractions of max HP, consumed in order.
	private static final double[] THRESHOLD_FRACTIONS = {0.80, 0.25, 0.05};

	private static final int INTRO_END_TICK = 160;       // intro dialogue is exactly 160t; aggro + damageability begin here
	private static final int FRENZY_DURATION_TICKS = 120;
	private static final int FIREBALL_DURATION_TICKS = 60;
	private static final int DEATH_TO_WK_TICKS = 100;

	// Middle of the arena Necron snaps to for a frenzy (his spawn point).
	private static final double MIDDLE_X = 54.5, MIDDLE_Y = 66, MIDDLE_Z = 76.5;
	private static final float MIDDLE_YAW = 0f;

	// Platform top-center blocks (y=63). If all AIR the platform is already destroyed — guards the intro destroy.
	private static final int PLATFORM_Y = 63;
	private static final int PLATFORM_X1 = 53, PLATFORM_X2 = 55;
	private static final int PLATFORM_Z1 = 113, PLATFORM_Z2 = 115;

	// Per-fight state.
	private int eventsDone;          // 0 → 80% pending, 1 → 25% pending, 2 → 5% pending, 3 → none left (death only)
	private boolean inInterlude;     // immune window (frenzy or fireball) — damage is rejected
	private boolean damageable;      // false during the intro and during any interlude
	private BukkitTask interludeEndTask;

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
	@Override protected double maxHealth() { return 1000; }
	@Override protected String displayHealth() { return "1.4B"; }
	@Override protected int previousTicks() { return PRE_NECRON_TICKS; }

	@Override
	protected void resetState() {
		cancelInterludeEndTask();
		if(boss != null) clearAggro();
		CustomBossBar.removeStunIndicator();
		eventsDone = 0;
		inInterlude = false;
		damageable = false;
	}

	@Override
	protected void onStart() {
		// --- Intro (160t): dialogue + a guarded platform destroy. Necron is not yet damageable and does not fly. ---
		sendChatMessage("You went further than any human before, congratulations.");
		Utils.scheduleTask(() -> {
			sendChatMessage("I'm afraid your journey ends now.");
			destroyPlatform(true); // intro salvo — may destroy the platform (guarded by platformIntact)
		}, 60);
		Utils.scheduleTask(() -> sendChatMessage("Goodbye."), 120);

		// --- After intro: drop armor, become damageable, and start the Maxor-style aggro chase. ---
		Utils.scheduleTask(() -> {
			setArmor(false);
			damageable = true;
			setAggro(target(), AGGRO_STOP_DISTANCE, AGGRO_Y_OFFSET, AGGRO_MAX_SPEED);
			sendChatMessage("That's a very impressive trick.  I guess I'll have to handle this myself.");
		}, INTRO_END_TICK);
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			WitherKing.witherKingInstructions(world);
			runPlayerHandoff();
		}
	}

	/** Aggro/leap target for the fight — mirrors Maxor (Tank). */
	private LivingEntity target() {
		return Tank.get();
	}

	// ---------- Damage / interludes ----------

	/** Damage interceptor, hooked from {@code MiscListener.onWitherLordDamage}. Necron is always damageable
	 *  except during the intro and the immune interludes. Each threshold (80% / 25% / 5%) is consumed in order:
	 *  a hit that would cross the next threshold is clamped exactly to it and triggers that interlude; a hit at
	 *  0% kills. Modeled on {@link instructions.bosses.storm.Storm#handleDamage}. */
	public void handleDamage(EntityDamageEvent e) {
		if(boss == null || !boss.equals(e.getEntity())) return;
		if(e.isCancelled()) return;
		if(dying) {
			e.setCancelled(true);
			return;
		}
		if(inInterlude || !damageable) {
			e.setCancelled(true);
			return;
		}

		double finalDmg = e.getFinalDamage();
		if(finalDmg <= 0) return;

		double currentHp = boss.getHealth();
		double threshold = nextThreshold();

		if(currentHp - finalDmg <= threshold) {
			if(threshold <= 0.0) {
				// Killing blow — clamp to leave a 1% sliver so vanilla doesn't death-despawn the wither
				// before the death dialogue; enterDyingState pins HP to 1 (shown as "1" via TASDying).
				double onePercent = maxHealth() * 0.01;
				scaleEventDamage(e, finalDmg, Math.max(0, currentHp - onePercent));
				enterDyingState();
			} else {
				// Clamp the hit so HP lands exactly on the threshold, then start that interlude.
				scaleEventDamage(e, finalDmg, currentHp - threshold);
				triggerInterlude(eventsDone);
			}
		}
		// Otherwise the hit passes through unmodified.
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
		setArmor(true);

		int duration;
		if(idx == 1) {
			// 25% — fireball attack in place (no teleport, no blindness).
			duration = FIREBALL_DURATION_TICKS;
			Utils.timer(ChatColor.GREEN + "Necron fireball attack at " + formatTick(displayTick()));
			destroyPlatform(false); // 25% replay — fireballs only, never destroy the platform
		} else {
			// 80% / 5% — frenzy: teleport to the middle, blind players, hold still.
			duration = FRENZY_DURATION_TICKS;
			moveBossTo(MIDDLE_X, MIDDLE_Y, MIDDLE_Z, MIDDLE_YAW);
			sendChatMessage(FRENZY_START_MESSAGES[random.nextInt(FRENZY_START_MESSAGES.length)]);
			applyBlindness(duration);
			frenzySounds(duration);
			CustomBossBar.spawnAnimatedStunnedIndicator(boss, duration);
			Utils.timer(ChatColor.GREEN + "Necron frenzy at " + formatTick(displayTick()));
		}

		cancelInterludeEndTask();
		interludeEndTask = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> endInterlude(idx), duration);
	}

	/** Interlude over — resume the chase and become damageable again. */
	private void endInterlude(int idx) {
		if(dying || boss == null || !boss.isValid()) return;
		inInterlude = false;
		damageable = true;
		setArmor(false);
		CustomBossBar.removeStunIndicator();
		if(idx != 1) sendChatMessage(FRENZY_END_MESSAGES[random.nextInt(FRENZY_END_MESSAGES.length)]);
		// After the FIRST frenzy (idx 0) Necron stays planted at the middle with AI off, so the upcoming 25%
		// fireball attack finds him already at the correct spot. He resumes the chase after the other interludes.
		if(idx != 0) setAggro(target(), AGGRO_STOP_DISTANCE, AGGRO_Y_OFFSET, AGGRO_MAX_SPEED);
	}

	private void cancelInterludeEndTask() {
		if(interludeEndTask != null && !interludeEndTask.isCancelled()) interludeEndTask.cancel();
		interludeEndTask = null;
	}

	/** Vanilla-shape damage scaling so {@code e.getFinalDamage()} becomes {@code targetFinal} (copied from
	 *  {@link instructions.bosses.storm.Storm} / {@link instructions.bosses.maxor.Maxor}). */
	private static void scaleEventDamage(EntityDamageEvent e, double currentFinal, double targetFinal) {
		if(currentFinal <= 0) return;
		double scale = Math.max(0, targetFinal) / currentFinal;
		e.setDamage(e.getDamage() * scale);
	}

	// ---------- Movement (snap to middle for a frenzy) ----------

	private void moveBossTo(double x, double y, double z, float yaw) {
		net.minecraft.world.entity.LivingEntity nms = ((CraftWither) boss).getHandle();
		nms.absSnapTo(x, y, z, yaw, nms.getXRot());
		nms.setDeltaMovement(Vec3.ZERO);
		nms.hurtMarked = true;
		PositionMoveRotation pmr = new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, nms.getXRot());
		ClientboundTeleportEntityPacket pkt = ClientboundTeleportEntityPacket.teleport(nms.getId(), pmr, Collections.emptySet(), nms.onGround());
		Utils.broadcastPacket(pkt);
	}

	// ---------- Frenzy effects ----------

	/** Blind every real, non-spectating player for the frenzy duration. Fake players are client-less (no-op);
	 *  spectating viewers are skipped so the spectated view isn't disrupted. */
	private void applyBlindness(int duration) {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(Spectate.isSpectating(p)) continue;
			p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, false));
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

	/** Fireball salvo. Only the intro salvo ({@code allowDestroy=true}) may swap the platform to its destroyed
	 *  variant, and only if it's still intact when called (a pre-destroyed platform is left alone). The 25% replay
	 *  ({@code allowDestroy=false}) is fireballs only — it never touches the platform, which may still be intact
	 *  (e.g. restored by stonk). The intact check is captured at call time. */
	private void destroyPlatform(boolean allowDestroy) {
		// Necron is already stationary at the correct spot for both salvos — the intro one fires from his spawn,
		// and the 25% one fires from the middle where the first frenzy planted him (AI is not re-enabled after
		// that frenzy), so there's no chase momentum to cancel here.
		boolean doClone = allowDestroy && platformIntact();
		shootFireball();
		Utils.scheduleTask(this::shootFireball, 10);
		Utils.scheduleTask(this::shootFireball, 20);
		Utils.scheduleTask(this::shootFireball, 30);
		Utils.scheduleTask(() -> {
			shootFireball();
			// destroyed variant lives at y -10..-6 (correct variant at y -5..-1); clone it up to the live platform.
			if(doClone) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 70 -10 120 38 -6 99 38 59 99");
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
		inInterlude = false;
		damageable = false;
		clearAggro();
		setArmor(false);
		CustomBossBar.removeStunIndicator();
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) {
				try { boss.setHealth(1.0); } catch (IllegalArgumentException ignored) {}
				Utils.changeName(boss);
			}
		}, 1);
		playDeathDialogue();
	}

	private void playDeathDialogue() {
		sendChatMessage("All this, for nothing...");
		Server.playWitherDeathSound(boss);
		Utils.timer(ChatColor.GREEN + "Necron killed in " + formatTick(displayTick()));
		Utils.scheduleTask(() -> sendChatMessage("I understand your words now, my master."), 60);
		// note: in skytils, the Necron timer ends 2 seconds too early, making Wither King start 2 seconds too early.
		// This TAS fixes that. To compare to Skytils, subtract 2 seconds here and add 2 seconds to Wither King time.
		Utils.scheduleTask(() -> {
			Utils.timer(ChatColor.GREEN + "Necron finished in " + formatTick(displayTick()));
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
			chainNext(doContinue);
		}, DEATH_TO_WK_TICKS);
		Utils.scheduleTask(() -> sendChatMessage("The Catacombs... are no more."), DEATH_TO_WK_TICKS + 20);
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) boss.remove();
		}, DEATH_TO_WK_TICKS + 60);
	}
}
