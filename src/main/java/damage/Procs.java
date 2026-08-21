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
 * Damage-over-time procs (MAP.md §7).  <b>Each of these is a separate damage instance on its own timer.</b>
 * They must not be folded into {@code sumAdditive}, and their own instances must not re-trigger themselves.
 *
 * <table>
 *   <caption>The procs</caption>
 *   <tr><th>Source</th><th>Behaviour</th></tr>
 *   <tr><td>Fire Aspect III</td><td>9% of the hit as bonus, at ticks 0, 20, 40, 60, 80 - five procs over 80t.</td></tr>
 *   <tr><td>Thunderlord VII</td><td>Every third hit <b>on the same mob</b>, 60% of that hit's damage as bonus.  Not a
 *       DoT: it fires once, on the hit, and keeps no state beyond the per-target hit counter.</td></tr>
 *   <tr><td>Venomous VII</td><td>Each hit on a mob adds a stack worth 2% of DPS, to a 40-stack cap (so 80% of
 *       DPS), delivered every 20t for 100t.  <b>DPS is 8x the highest hit in the last 100 ticks</b>, so a capped
 *       proc is 640% of that hit.  The ramp expires with the window: stop hitting and the stacks are gone.</td></tr>
 * </table>
 *
 * <h2>What "refresh" means, and what it does not</h2>
 * A running effect has a <b>cadence</b> ({@code nextTick}, every 20t) and an <b>end time</b> ({@code expiresAt}).
 * Re-hitting the same target moves the <b>end time only</b>: it pushes expiry out to {@code now + duration}, updates
 * the per-tick damage, and touches nothing else.  It does <b>not</b> deal damage there and then, and it does not move
 * the cadence - the effect keeps ticking on the 20t phase it has been on since it was first applied.
 * <p>
 * Both halves of that used to be wrong, and together they were the largest damage bug in the system:
 * <ul>
 *   <li>{@code onHit} dealt an <b>immediate</b> Fire and Venomous instance on <i>every</i> primary hit, not just on
 *       a fresh application.  A mage beam lands every 5-7 ticks, so a "5 procs over 80t" effect was really firing
 *       three or four times a second, on top of its own chain - and Venomous is the biggest number in the system, so
 *       this roughly tripled total damage.</li>
 *   <li>refresh restored a {@code procsLeft} COUNTER rather than extending an end time, which is "reset the
 *       cooldown" wearing the word refresh: the remaining lifetime was however long 4 more procs took, so the effect
 *       could never actually end while anyone was hitting, and its phase drifted with every proc.</li>
 * </ul>
 * The shared shape - "a % of the hit, on a fixed cadence, expiry extended but never re-phased by re-hitting" - is why
 * Fire Aspect and Venomous run through one abstraction here rather than two schedulers.
 *
 * <h2>One effect per (ATTACKER, target, kind)</h2>
 * An effect's identity includes <b>who applied it</b>, so two players beating on the same mob each burn it with their
 * own Fire Aspect and poison it with their own Venomous, on their own cadences and end times, and neither one's hit
 * refreshes or overwrites the other's. Every input is the attacker's too: Fire Aspect is 9% of <i>that player's</i>
 * hit, and Venomous reads <i>that player's</i> DPS figure and <i>that player's</i> stack ramp on this mob
 * ({@link CombatState#venomousDps} / {@link CombatState#noteVenomousHit}, both keyed on the player). So an Archer who
 * has been chipping and a Berserk who just landed a 100M swing carry completely different poisons on one Necron.
 * <p>
 * Identity is the attacker's <b>UUID</b>, not the {@link Player} object: Bukkit's equality on entities is entity-ID
 * equality, which a relog changes, so a player who reconnected mid-fight would have failed to match their own running
 * effect and started a duplicate alongside it.  The ticking attacker is looked up from the UUID, and the effect is
 * dropped as soon as that lookup comes back empty.
 * <p>
 * All three are sword-only (§7's bow exclusion list), so a bow hit produces none of them.
 */
public final class Procs {
	private Procs() {}

	private static final double FIRE_ASPECT_SHARE = 0.09;
	private static final int FIRE_ASPECT_INTERVAL = 20;
	/** Fire Aspect's window: procs at 0, 20, 40, 60, 80, so the effect ends 80 ticks after the hit that applied it. */
	private static final int FIRE_ASPECT_DURATION = 80;

	private static final double THUNDERLORD_SHARE = 0.60;
	private static final int THUNDERLORD_EVERY = 3;

	private static final double VENOMOUS_PER_STACK = 0.02;
	private static final int VENOMOUS_INTERVAL = 20;
	/** Venomous's window: "every 20t for 100t", so procs at 0, 20, 40, 60, 80, 100. */
	private static final int VENOMOUS_DURATION = 100;

	/**
	 * One running damage-over-time effect.
	 * <p>
	 * The two timing fields do different jobs and must not be conflated.  {@code nextTick} is the <b>cadence</b>, set
	 * once when the effect is applied and thereafter only ever advanced by {@code interval}, so the effect keeps its
	 * 20t phase for as long as it lives.  {@code expiresAt} is the <b>end time</b>, and is the ONLY field a re-hit
	 * moves (alongside {@code amount}).  There is deliberately no remaining-procs counter: a count is a cooldown in
	 * disguise, and restoring it on every hit is what made these effects unbounded.
	 */
	private static final class Effect {
		final UUID targetId;
		final LivingEntity target;
		/** WHO applied it - part of the effect's identity, so every attacker gets their own chain on one mob. */
		final UUID attackerId;
		final DamageKind kind;
		final int interval;
		double amount;
		int nextTick;
		int expiresAt;

		Effect(LivingEntity target, Player attacker, DamageKind kind, double amount, int interval, int duration) {
			this.targetId = target.getUniqueId();
			this.target = target;
			this.attackerId = attacker.getUniqueId();
			this.kind = kind;
			this.interval = interval;
			this.amount = amount;
			this.nextTick = MinecraftServer.currentTick + interval;
			this.expiresAt = MinecraftServer.currentTick + duration;
		}
	}

	private static final List<Effect> EFFECTS = new ArrayList<>();
	/**
	 * Melee hits per (attacker, target), for Thunderlord's every-third-hit.  Keyed on the TARGET as well as the
	 * attacker: the third hit that procs has to be the third hit on <b>that mob</b>, so hitting something else must
	 * neither advance nor reset the count on the mob you came from.  Outer key attacker, inner key target.
	 */
	private static final java.util.Map<UUID, java.util.Map<UUID, Integer>> thunderlordCount = new java.util.HashMap<>();

	public static void reset() {
		EFFECTS.clear();
		thunderlordCount.clear();
	}

	/** Start the per-tick proc driver.  One repeating task for every effect, rather than one task per proc. */
	public static void start() {
		Bukkit.getScheduler().runTaskTimer(plugin.M7tas.getInstance(), Procs::tick, 1L, 1L);
	}

	/**
	 * Called for every PRIMARY hit.  Thunderlord fires immediately when it is due; Fire Aspect and Venomous apply or
	 * refresh their effects.
	 * <p>
	 * <b>Nothing here deals Fire or Venomous damage directly.</b>  Their tick-0 proc is dealt by {@link #apply} and
	 * only on a FRESH application, so re-hitting an already-burning target adds no extra instance - see the class doc
	 * for why that mattered.
	 */
	public static void onHit(Player attacker, LivingEntity target, double sbDamage, DamagePath path) {
		if(attacker == null || target == null || sbDamage <= 0) return;
		if(!path.isMelee()) return;                                  // sword-only

		// Thunderlord VII: every third hit ON THE SAME MOB, 60% of THAT hit's damage.  Genuinely per-hit, so it stays
		// inline.  A single per-player counter was wrong: three hits spread over three mobs procced, and a mob two
		// hits in lost its progress the moment you clipped something else.
		int hits = thunderlordCount
				.computeIfAbsent(attacker.getUniqueId(), k -> new java.util.HashMap<>())
				.merge(target.getUniqueId(), 1, Integer::sum);
		if(hits % THUNDERLORD_EVERY == 0) {
			Damage.dealSecondary(target, sbDamage * THUNDERLORD_SHARE, DamageKind.THUNDERLORD, attacker);
		}

		// Fire Aspect III: 9% of the hit, five procs over 80 ticks.  Duplex's debuff is FIRE damage only, so it
		// scales these rather than the hit that spawned them.
		double fire = sbDamage * FIRE_ASPECT_SHARE * TargetDebuffs.fireMultiplier(target);
		apply(target, attacker, DamageKind.FIRE, fire, FIRE_ASPECT_INTERVAL, FIRE_ASPECT_DURATION);

		// Venomous VII.  Each hit on a mob adds one stack; one stack is 2% of "DPS", which is defined as EIGHT
		// TIMES the highest hit in the last 100 ticks.  It caps at 40 stacks on the same mob, i.e. 80% of DPS -
		// which is 640% of that highest hit - and it delivers every 20t for 100t.
		//
		// §7's parenthetical "(80% of the hit)" reads as a factor of 8 smaller than its own DPS definition; the
		// owner ruled the DPS definition operative, so a capped proc really is several times the hit that
		// triggered it.  That makes it easily the largest proc in the system, which is intended - and is exactly why
		// it must fire on its own 20t cadence and not once per swing.
		//
		// The ramp is bounded by the WINDOW as well as by the cap: it is cleared the moment this player's poison
		// lapses (see tick), so it counts what they have done to this mob in the last 100 ticks and a player coming
		// back to a mob they left has to build all 40 stacks again.
		int stacks = CombatState.noteVenomousHit(attacker, target.getUniqueId());
		double venom = CombatState.venomousDps(attacker) * VENOMOUS_PER_STACK * stacks;
		if(venom > 0) apply(target, attacker, DamageKind.VENOMOUS, venom, VENOMOUS_INTERVAL, VENOMOUS_DURATION);
	}

	/**
	 * Apply an effect, or refresh the one already running for this (attacker, target, kind).
	 * <p>
	 * A <b>fresh</b> application deals its tick-0 proc immediately and starts the cadence.  A <b>refresh</b> updates
	 * the per-tick damage and pushes the end time out to {@code now + duration}, and does nothing else: no damage
	 * here, and {@code nextTick} is left exactly where it was, so the 20t phase the effect has been running on
	 * survives.  That distinction is the whole point of this method.
	 * <p>
	 * The match is on all three of kind, target AND attacker, so a second player hitting a burning mob is a FRESH
	 * application of their own effect rather than a refresh of somebody else's.
	 */
	private static void apply(LivingEntity target, Player attacker, DamageKind kind, double amount, int interval,
			int duration) {
		int now = MinecraftServer.currentTick;
		UUID targetId = target.getUniqueId();
		UUID attackerId = attacker.getUniqueId();
		for(Effect e : EFFECTS) {
			if(e.kind == kind && e.targetId.equals(targetId) && e.attackerId.equals(attackerId)) {
				e.amount = amount;
				e.expiresAt = now + duration;
				return;
			}
		}
		EFFECTS.add(new Effect(target, attacker, kind, amount, interval, duration));
		Damage.dealSecondary(target, amount, kind, attacker);
	}

	private static void tick() {
		if(EFFECTS.isEmpty()) return;
		int now = MinecraftServer.currentTick;
		for(Iterator<Effect> it = EFFECTS.iterator(); it.hasNext(); ) {
			Effect e = it.next();
			boolean dead = e.target.isDead() || e.target.getHealth() <= 0;
			// A dead mob's Thunderlord progress and Venomous ramp are meaningless, and dropping them here is what keeps
			// the per-target maps from growing for the whole run.  Every melee hit applies Fire Aspect, so every counted
			// target has an Effect and so passes through this loop.  Thunderlord's count deliberately does NOT expire
			// with the effect, only with the mob; the Venomous ramp expires with both (see below).
			if(dead) forgetTarget(e.targetId);
			// Resolved from the UUID rather than held as a Player, so a relog neither strands the effect on a stale
			// object nor lets the reconnected player start a second chain beside their own.
			Player attacker = Bukkit.getPlayer(e.attackerId);
			// Expiry is checked with a strict >, so the proc that lands exactly ON the end tick still lands.
			if(dead || attacker == null || now > e.expiresAt) {
				// The ramp dies with the poison it feeds: once this player's 100t window lapses on this mob their
				// stacks are gone and the next hit starts again at one.  Only THEIR ramp - the mob keeps everyone
				// else's, whose windows are still running on their own clocks.  (A dead target has already had every
				// attacker's ramp dropped by forgetTarget above; removing it twice is a no-op.)
				if(e.kind == DamageKind.VENOMOUS) CombatState.resetVenomous(e.attackerId, e.targetId);
				it.remove();
				continue;
			}
			if(now < e.nextTick) continue;
			Damage.dealSecondary(e.target, e.amount, e.kind, attacker);
			// Advance from the SCHEDULED tick, not from `now`, so the 20t phase cannot drift.  The resync below only
			// matters if the driver ever ran late, and it deliberately drops the missed procs rather than firing a
			// burst to catch up.
			e.nextTick += e.interval;
			if(e.nextTick <= now) e.nextTick = now + e.interval;
		}
	}

	/** Forget every attacker's Thunderlord progress and Venomous ramp on one target, once that target is dead. */
	private static void forgetTarget(UUID targetId) {
		for(java.util.Map<UUID, Integer> perTarget : thunderlordCount.values()) perTarget.remove(targetId);
		CombatState.forgetVenomous(targetId);
	}
}
