package damage;

import net.minecraft.server.MinecraftServer;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * State on a TARGET, not an attacker (MAP.md §7), so it helps EVERY attacker. None of it stacks with itself.
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
 * <b>The two defense reducers use different arithmetic:</b> Lethality multiplies per stack ({@code 0.91^4} =
 * x0.68575), Last Breath sums then applies once ({@code 1 - 0.50} = x0.5). Together x0.34288: Necron's 2100 defense
 * becomes 720, divisor /22 to /8.2, a 2.68x damage increase and the largest single lever in §7.
 * <p>
 * <b>Stacks land even when damage doesn't.</b> A beam builds Lethality and Venomous on an invulnerable mob, so a Mage
 * can beam an armoured Maxor and have debuffs up the moment it opens.
 * <p>
 * <b>Debuff first, then damage</b>, so a hit benefits from its own debuff. Invisible in classic, but in a live mode
 * it decides whether the opening shot is boosted, and it would be impossible to notice later.
 * <p>
 * Per damage INSTANCE, not per tick: each Terminator arrow debuffs before its own damage; Cleave reads what the main
 * hit left.
 * <p>
 * Durations §7 left [TBD]: Lethality 4s, refreshed by every hit, so it holds while anyone attacks; Last Breath
 * PERMANENT until death or run reset. Live modes only; classic assumes both maxed.
 */
public final class TargetDebuffs {
	private TargetDebuffs() {}

	public static final int LETHALITY_MAX_STACKS = 4;
	public static final int LAST_BREATH_MAX_STACKS = 5;
	/** 4s, refreshed by every stacking hit. */
	private static final int LETHALITY_STACK_TICKS = 80;
	private static final int ICE_SPRAY_TICKS = 100;
	private static final int TWILIGHT_TICKS = 400;
	private static final int DUPLEX_TICKS = 1200;

	/** Lethality multiplies; Last Breath sums, then applies once. */
	private static final double LETHALITY_PER_STACK = 0.91;
	private static final double LAST_BREATH_PER_STACK = 0.10;

	/** Neither stacks with itself; the two multiply together. */
	private static final double DEBUFF_MULTIPLIER = 1.1;
	/** FIRE only: multiplies Fire Aspect procs, not the hit. */
	private static final double DUPLEX_FIRE_MULTIPLIER = 1.5;

	private static final class State {
		int lethalityStacks;
		int lethalityExpiry;
		/** No expiry: Last Breath is permanent. */
		int lastBreathStacks;
		int iceSprayExpiry;
		int twilightExpiry;
		int duplexExpiry;
	}

	private static final Map<UUID, State> STATE = new HashMap<>();

	/** Called at run start with the other combat resets. */
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

	/** Cap 4, before the hit resolves. Each stack refreshes the window; 4s of silence drops the lot. */
	public static void applyLethality(LivingEntity target) {
		if(target == null) return;
		State s = state(target);
		if(!live(s.lethalityExpiry)) s.lethalityStacks = 0;
		s.lethalityStacks = Math.min(s.lethalityStacks + 1, LETHALITY_MAX_STACKS);
		s.lethalityExpiry = MinecraftServer.currentTick + LETHALITY_STACK_TICKS;
	}

	/**
	 * Cap 5, Last Breath arrows only. Permanent, which is why §7's pre-debuff windows matter: Goldor during terminals
	 * and Necron during frenzy still have the x0.5 when they open up.
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

	/** After Lethality and Last Breath. Classic: both maxed (§0), a flag on the input, no second path. */
	public static double reducedDefense(LivingEntity target, double baseDefense) {
		int lethality = LETHALITY_MAX_STACKS;
		int lastBreath = LAST_BREATH_MAX_STACKS;
		if(!Difficulty.debuffsAssumed() && target != null) {
			State s = state(target);
			lethality = live(s.lethalityExpiry) ? s.lethalityStacks : 0;
			lastBreath = s.lastBreathStacks; // permanent, no window
		}
		double reduced = baseDefense * Math.pow(LETHALITY_PER_STACK, lethality);
		return reduced * (1.0 - LAST_BREATH_PER_STACK * lastBreath);
	}

	/** Product of the two x1.1 debuffs currently on the target. */
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
	 * Reads REAL state, ignoring {@link Difficulty#debuffsAssumed()}: only feeds the wand's "debuffed N enemies"
	 * message, which in classic would otherwise call every enemy already debuffed on the first cast.
	 */
	public static boolean iceSprayed(LivingEntity target) {
		return target != null && live(state(target).iceSprayExpiry);
	}

	/** Duplex's x1.5, FIRE only: Fire Aspect procs, never the hit. */
	public static double fireMultiplier(LivingEntity target) {
		if(Difficulty.debuffsAssumed()) return DUPLEX_FIRE_MULTIPLIER;
		return target != null && live(state(target).duplexExpiry) ? DUPLEX_FIRE_MULTIPLIER : 1.0;
	}

	/** E.g. on death. Housekeeping. */
	public static void forget(LivingEntity target) {
		if(target != null) STATE.remove(target.getUniqueId());
	}
}
