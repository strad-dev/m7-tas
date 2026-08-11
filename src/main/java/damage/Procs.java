package damage;

import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Damage-over-time procs (DAMAGE_PLAN.md §7).  <b>Each of these is a separate damage instance on its own timer.</b>
 * They must not be folded into {@code sumAdditive}, and their own instances must not re-trigger themselves.
 *
 * <table>
 *   <caption>The procs</caption>
 *   <tr><th>Source</th><th>Behaviour</th></tr>
 *   <tr><td>Fire Aspect III</td><td>9% of the hit as bonus, at ticks 0, 20, 40, 60, 80 - five procs over 80t.
 *       Re-hitting only refreshes the timer; it does not reset the proc cadence.</td></tr>
 *   <tr><td>Thunderlord VII</td><td>Every third hit, 60% of that hit's damage as bonus.</td></tr>
 *   <tr><td>Venomous VII</td><td>Each hit on a mob adds a stack worth 2% of DPS, to a 40-stack cap (so 80% of
 *       DPS), delivered every 20t for 100t.  <b>DPS is 8x the highest hit in the last 100 ticks</b>, so a capped
 *       proc is 640% of that hit.</td></tr>
 * </table>
 *
 * The shared shape - "a % of the hit, on a fixed cadence, refreshed but not reset by re-hitting" - is why Fire
 * Aspect and Venomous run through one abstraction here rather than two schedulers.
 * <p>
 * All three are sword-only (§7's bow exclusion list), so a bow hit produces none of them.
 */
public final class Procs {
	private Procs() {}

	private static final double FIRE_ASPECT_SHARE = 0.09;
	private static final int FIRE_ASPECT_PROCS = 5;
	private static final int FIRE_ASPECT_INTERVAL = 20;

	private static final double THUNDERLORD_SHARE = 0.60;
	private static final int THUNDERLORD_EVERY = 3;

	private static final double VENOMOUS_PER_STACK = 0.02;
	private static final int VENOMOUS_PROCS = 5;             // every 20t for 100t
	private static final int VENOMOUS_INTERVAL = 20;

	/**
	 * One running proc chain.  {@code amount} and {@code procsLeft} are REFRESHED by a re-hit while
	 * {@code nextTick} is left alone, which is what "refreshed but not reset" means: the cadence keeps its phase.
	 */
	private static final class Chain {
		final UUID targetId;
		final LivingEntity target;
		final Player attacker;
		final DamageKind kind;
		final int interval;
		double amount;
		int procsLeft;
		int nextTick;

		Chain(LivingEntity target, Player attacker, DamageKind kind, double amount, int procs, int interval) {
			this.targetId = target.getUniqueId();
			this.target = target;
			this.attacker = attacker;
			this.kind = kind;
			this.interval = interval;
			this.amount = amount;
			this.procsLeft = procs;
			this.nextTick = MinecraftServer.currentTick + interval;
		}
	}

	private static final List<Chain> CHAINS = new ArrayList<>();
	/** Melee hits per player, for Thunderlord's every-third-hit. */
	private static final java.util.Map<UUID, Integer> thunderlordCount = new java.util.HashMap<>();

	public static void reset() {
		CHAINS.clear();
		thunderlordCount.clear();
	}

	/** Start the per-tick proc driver.  One repeating task for every chain, rather than one task per proc. */
	public static void start() {
		Bukkit.getScheduler().runTaskTimer(plugin.M7tas.getInstance(), Procs::tick, 1L, 1L);
	}

	/**
	 * Called for every PRIMARY hit.  Thunderlord fires immediately when it is due; Fire Aspect and Venomous arm
	 * (or refresh) their chains, and their first proc is the hit itself at tick 0.
	 */
	public static void onHit(Player attacker, LivingEntity target, double sbDamage, DamagePath path) {
		if(attacker == null || target == null || sbDamage <= 0) return;
		if(!path.isMelee()) return;                                  // sword-only

		// Thunderlord VII: every third hit, 60% of THAT hit's damage.
		int hits = thunderlordCount.merge(attacker.getUniqueId(), 1, Integer::sum);
		if(hits % THUNDERLORD_EVERY == 0) {
			Damage.dealSecondary(target, sbDamage * THUNDERLORD_SHARE, DamageKind.THUNDERLORD, attacker);
		}

		// Fire Aspect III: 9% of the hit, five procs over 80 ticks.  Duplex's debuff is FIRE damage only, so it
		// scales these rather than the hit that spawned them.
		double fire = sbDamage * FIRE_ASPECT_SHARE * TargetDebuffs.fireMultiplier(target);
		Damage.dealSecondary(target, fire, DamageKind.FIRE, attacker);
		arm(target, attacker, DamageKind.FIRE, fire, FIRE_ASPECT_PROCS - 1, FIRE_ASPECT_INTERVAL);

		// Venomous VII.  Each hit on a mob adds one stack; one stack is 2% of "DPS", which is defined as EIGHT
		// TIMES the highest hit in the last 100 ticks.  It caps at 40 stacks on the same mob, i.e. 80% of DPS -
		// which is 640% of that highest hit - and each armed chain delivers every 20t for 100t.
		//
		// §7's parenthetical "(80% of the hit)" reads as a factor of 8 smaller than its own DPS definition; the
		// owner ruled the DPS definition operative, so a capped proc really is several times the hit that
		// triggered it.  That makes it easily the largest proc in the system, which is intended.
		int stacks = CombatState.noteVenomousHit(attacker, target.getUniqueId());
		double venom = CombatState.venomousDps(attacker) * VENOMOUS_PER_STACK * stacks;
		if(venom > 0) {
			Damage.dealSecondary(target, venom, DamageKind.VENOMOUS, attacker);
			arm(target, attacker, DamageKind.VENOMOUS, venom, VENOMOUS_PROCS - 1, VENOMOUS_INTERVAL);
		}
	}

	/** Arm a chain, or refresh an existing one for the same (attacker, target, kind) without resetting its phase. */
	private static void arm(LivingEntity target, Player attacker, DamageKind kind, double amount, int procs,
			int interval) {
		for(Chain c : CHAINS) {
			if(c.kind == kind && c.targetId.equals(target.getUniqueId()) && c.attacker.equals(attacker)) {
				c.amount = amount;
				c.procsLeft = procs;
				return;
			}
		}
		CHAINS.add(new Chain(target, attacker, kind, amount, procs, interval));
	}

	private static void tick() {
		if(CHAINS.isEmpty()) return;
		int now = MinecraftServer.currentTick;
		for(Iterator<Chain> it = CHAINS.iterator(); it.hasNext(); ) {
			Chain c = it.next();
			if(c.target.isDead() || c.target.getHealth() <= 0 || !c.attacker.isOnline() || c.procsLeft <= 0) {
				it.remove();
				continue;
			}
			if(now < c.nextTick) continue;
			Damage.dealSecondary(c.target, c.amount, c.kind, c.attacker);
			c.procsLeft--;
			// The cadence keeps its own phase: a re-hit refreshes amount and procsLeft but never nextTick.
			c.nextTick = now + c.interval;
		}
	}
}
