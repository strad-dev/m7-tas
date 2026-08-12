package damage;

import net.minecraft.server.MinecraftServer;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * State applied to a TARGET rather than to an attacker (DAMAGE_PLAN.md §7).  Because it lives on the target it
 * helps <b>every</b> attacker, not just the one who applied it, and none of it stacks with itself.
 *
 * <table>
 *   <caption>What lives here</caption>
 *   <tr><th>Debuff</th><th>Effect</th><th>Applied by</th><th>Duration</th></tr>
 *   <tr><td>Ice Spray Wand</td><td>x1.1 damage taken</td><td>every enemy within 8 blocks of the caster's eyes</td><td>5s</td></tr>
 *   <tr><td>Twilight Arrow Poison</td><td>x1.1 damage taken</td><td>any enemy hit by a bow</td><td>20s</td></tr>
 *   <tr><td>Duplex V</td><td>x1.5 FIRE damage</td><td>any enemy hit by that bow</td><td>60s</td></tr>
 *   <tr><td>Lethality VI</td><td>x0.91 defense per stack, MULTIPLICATIVE, 4 stacks</td><td>hitting</td><td>4s, refreshed by every further hit</td></tr>
 *   <tr><td>Last Breath</td><td>-10% defense per stack, ADDITIVE then applied once, 5 stacks</td><td>landing arrows</td><td><b>permanent</b></td></tr>
 * </table>
 *
 * <b>The two defense reducers stack with different arithmetic</b>, which looks like a detail and is not:
 * Lethality multiplies each stack ({@code 0.91^4} = x0.68575) while Last Breath sums its scalars and applies them
 * once ({@code 1 - 0.50} = x0.5).  Together x0.34288, which turns Necron's 2100 defense into 720 and its divisor
 * from /22 into /8.2 - a 2.68x damage increase, the largest single lever in §7.
 * <p>
 * <b>Stacks land even when the damage does not.</b>  A mage beam still builds Lethality and Venomous on a mob that
 * is currently invulnerable, so the stack step is not merely ordered before the damage step - it is independent of
 * whether the damage is applied at all.  That is what lets a Mage beam an armoured Maxor and have the debuffs
 * already in place the moment it opens up.
 * <p>
 * <b>Ordering: debuff first, then damage.</b>  A hit that both debuffs and damages applies its debuff FIRST, so it
 * benefits from its own debuff.  Mostly invisible in classic mode, where all four are assumed permanently applied
 * anyway, but it decides whether the opening shot of a fight is weak or already boosted in realistic mode - and it
 * is one line of ordering that would be impossible to notice later.
 * <p>
 * Applies per damage INSTANCE, not per tick: each of the Terminator's three arrows applies its own debuff before
 * its own damage, and a Cleave hit reads whatever state the main hit left behind.
 * <p>
 * The two stack durations, which §7 left {@code [TBD]}: <b>Lethality lasts 4s and every further hit refreshes
 * it</b>, so it holds for free while anyone is attacking; <b>Last Breath is PERMANENT</b> and only clears when the
 * target dies or the run resets. They only matter in realistic mode - classic assumes both at max.
 */
public final class TargetDebuffs {
	private TargetDebuffs() {}

	public static final int LETHALITY_MAX_STACKS = 4;
	public static final int LAST_BREATH_MAX_STACKS = 5;
	/** 4s, refreshed by every hit that adds a stack - so it never lapses under sustained fire. */
	private static final int LETHALITY_STACK_TICKS = 80;
	private static final int ICE_SPRAY_TICKS = 100;
	private static final int TWILIGHT_TICKS = 400;
	private static final int DUPLEX_TICKS = 1200;

	/** Per-stack factors.  Lethality multiplies; Last Breath sums, then applies once. */
	private static final double LETHALITY_PER_STACK = 0.91;
	private static final double LAST_BREATH_PER_STACK = 0.10;

	/** The x1.1 both target debuffs apply.  Neither stacks with itself, and the two multiply with each other. */
	private static final double DEBUFF_MULTIPLIER = 1.1;
	/** Duplex's debuff is FIRE damage only, so it multiplies the Fire Aspect procs, not the hit. */
	private static final double DUPLEX_FIRE_MULTIPLIER = 1.5;

	private static final class State {
		int lethalityStacks;
		int lethalityExpiry;
		/** No expiry field: Last Breath's defense reduction is permanent on the target. */
		int lastBreathStacks;
		int iceSprayExpiry;
		int twilightExpiry;
		int duplexExpiry;
	}

	private static final Map<UUID, State> STATE = new HashMap<>();

	/** Clear every target's debuff state.  Called at run start alongside the other combat resets. */
	public static void reset() {
		STATE.clear();
	}

	private static State state(LivingEntity target) {
		return STATE.computeIfAbsent(target.getUniqueId(), k -> new State());
	}

	private static boolean live(int expiry) {
		return MinecraftServer.currentTick < expiry;
	}

	// ===================== applying =====================

	/**
	 * Add a Lethality stack, capped at 4.  Called before the hit that added it resolves.  Every stack refreshes
	 * the whole window, so sustained fire holds all four indefinitely and 4s of silence drops the lot.
	 */
	public static void applyLethality(LivingEntity target) {
		if(target == null) return;
		State s = state(target);
		if(!live(s.lethalityExpiry)) s.lethalityStacks = 0;
		s.lethalityStacks = Math.min(s.lethalityStacks + 1, LETHALITY_MAX_STACKS);
		s.lethalityExpiry = MinecraftServer.currentTick + LETHALITY_STACK_TICKS;
	}

	/**
	 * Add a Last Breath stack, capped at 5.  Only a Last Breath arrow does this.
	 * <p>
	 * <b>Permanent</b>: the reduction never lapses, so the five stacks are built once per target and then simply
	 * held.  That is what makes the pre-debuff windows in §7 matter - Goldor during terminals and Necron during
	 * frenzy are worth using precisely because the x0.5 is still there when the boss opens up.
	 */
	public static void applyLastBreath(LivingEntity target) {
		if(target == null) return;
		State s = state(target);
		s.lastBreathStacks = Math.min(s.lastBreathStacks + 1, LAST_BREATH_MAX_STACKS);
	}

	public static void applyIceSpray(LivingEntity target) {
		if(target != null) state(target).iceSprayExpiry = MinecraftServer.currentTick + ICE_SPRAY_TICKS;
	}

	public static void applyTwilightPoison(LivingEntity target) {
		if(target != null) state(target).twilightExpiry = MinecraftServer.currentTick + TWILIGHT_TICKS;
	}

	public static void applyDuplexFire(LivingEntity target) {
		if(target != null) state(target).duplexExpiry = MinecraftServer.currentTick + DUPLEX_TICKS;
	}

	// ===================== reading =====================

	/**
	 * The target's defense after Lethality and Last Breath.  In classic mode both are at max unconditionally
	 * (§0), which is exactly the "flag on the input" shape - there is no second code path here.
	 */
	public static double reducedDefense(LivingEntity target, double baseDefense) {
		int lethality = LETHALITY_MAX_STACKS;
		int lastBreath = LAST_BREATH_MAX_STACKS;
		if(!Difficulty.debuffsAssumed() && target != null) {
			State s = state(target);
			lethality = live(s.lethalityExpiry) ? s.lethalityStacks : 0;
			lastBreath = s.lastBreathStacks; // permanent, so no window to check
		}
		double reduced = baseDefense * Math.pow(LETHALITY_PER_STACK, lethality);
		return reduced * (1.0 - LAST_BREATH_PER_STACK * lastBreath);
	}

	/** The product of the two x1.1 target debuffs currently on this target. */
	public static double damageMultiplier(LivingEntity target) {
		boolean ice = true;
		boolean twilight = true;
		if(!Difficulty.debuffsAssumed() && target != null) {
			State s = state(target);
			ice = live(s.iceSprayExpiry);
			twilight = live(s.twilightExpiry);
		}
		double m = 1.0;
		if(ice) m *= DEBUFF_MULTIPLIER;
		if(twilight) m *= DEBUFF_MULTIPLIER;
		return m;
	}

	/**
	 * Whether the target's Ice Spray window is currently live.  Deliberately reads the REAL state and ignores
	 * {@link Difficulty#debuffsAssumed()}, unlike {@link #damageMultiplier}: this only feeds the wand's
	 * "debuffed N enemies" message, and in classic mode the assumed-everything answer would report every
	 * enemy as already debuffed on the very first cast.
	 */
	public static boolean iceSprayed(LivingEntity target) {
		return target != null && live(state(target).iceSprayExpiry);
	}

	/** Duplex's x1.5, which applies to FIRE damage only - so to the Fire Aspect procs, never to the hit. */
	public static double fireMultiplier(LivingEntity target) {
		if(Difficulty.debuffsAssumed()) return DUPLEX_FIRE_MULTIPLIER;
		return target != null && live(state(target).duplexExpiry) ? DUPLEX_FIRE_MULTIPLIER : 1.0;
	}

	/** Forget a target's state, e.g. when it dies.  Purely housekeeping. */
	public static void forget(LivingEntity target) {
		if(target != null) STATE.remove(target.getUniqueId());
	}
}
