package damage;

import net.minecraft.server.MinecraftServer;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player and per-(player, target) combat state that damage sources read: the repeated-hit stack, the post-kill
 * buff, the kill combo, the Berserk ultimate window, the Tarantula Ring's every-tenth-hit counter, and the rolling
 * damage history.
 * <p>
 * <b>The rolling history is one abstraction serving four features</b> (MAP.md §1.14).  Berserk's axe throw
 * ("highest hit in the last 60s"), Explosive Shot and Rapid Fire ("highest arrow damage in the last minute") and
 * Venomous's DPS term ("8x the highest hit in the last 100 ticks") are all the same query at different windows, so
 * this is one ring buffer rather than three trackers.
 * <p>
 * Main-thread only, so no synchronisation.  Everything is cleared at run start by {@link #reset()}.
 */
public final class CombatState {
	private CombatState() {}

	/** A hit's size and when it landed, for the rolling history. */
	private record Hit(int tick, double damage) {}

	/** How far back the history can see: 60s, the longest window any consumer asks for. */
	private static final int HISTORY_TICKS = 1200;
	/** Venomous reads a much shorter window than the other three consumers: the last 100 ticks. */
	private static final int VENOMOUS_WINDOW_TICKS = 100;
	/** Venomous's "DPS" is eight times the biggest hit in that window. */
	private static final double VENOMOUS_DPS_FACTOR = 8.0;

	/** Consecutive hits on one target.  Does NOT decay - only switching target resets it (§1.14). */
	private static final Map<UUID, UUID> lastTarget = new HashMap<>();
	private static final Map<UUID, Integer> repeatCount = new HashMap<>();
	/** Server tick the post-kill buff expires at, per player.  Armed by a kill, spent by the next hit either way. */
	private static final Map<UUID, Integer> postKillExpiry = new HashMap<>();
	/** Kill combo: count, and the tick the chain breaks at if no further kill lands. */
	private static final Map<UUID, Integer> comboCount = new HashMap<>();
	private static final Map<UUID, Integer> comboExpiry = new HashMap<>();
	private static final Map<UUID, Integer> berserkUltimateEnd = new HashMap<>();
	/** Melee hits landed, for the Tarantula Ring's every-tenth-hit x1.15. */
	private static final Map<UUID, Integer> meleeHits = new HashMap<>();
	private static final Map<UUID, Deque<Hit>> history = new HashMap<>();
	/** Venomous ramp: hits landed on one target, feeding its 2%-per-hit growth to a 40-hit cap. */
	private static final Map<UUID, Map<UUID, Integer>> venomousHits = new HashMap<>();

	/** Clear every counter.  Called at the start of each run, alongside the ability-cooldown reset. */
	public static void reset() {
		lastTarget.clear();
		repeatCount.clear();
		postKillExpiry.clear();
		comboCount.clear();
		comboExpiry.clear();
		berserkUltimateEnd.clear();
		meleeHits.clear();
		history.clear();
		venomousHits.clear();
	}

	// ===================== repeated-hit stack (Berserk) =====================

	/** Prior consecutive hits on this target: 0 for the first hit.  Read-only. */
	public static int repeatHits(Player p, UUID target) {
		if(p == null || target == null) return 0;
		return target.equals(lastTarget.get(p.getUniqueId())) ? repeatCount.getOrDefault(p.getUniqueId(), 0) : 0;
	}

	/** Advance the stack for a PRIMARY hit.  Switching target resets it; nothing else does. */
	public static void noteHit(Player p, UUID target, DamagePath path) {
		if(p == null || target == null) return;
		UUID id = p.getUniqueId();
		if(target.equals(lastTarget.get(id))) {
			repeatCount.merge(id, 1, Integer::sum);
		} else {
			lastTarget.put(id, target);
			repeatCount.put(id, 1);
		}
		if(path.isMelee()) meleeHits.merge(id, 1, Integer::sum);
	}

	/** True on the FIRST hit this player has landed on this target, i.e. First Strike's window. */
	public static boolean isFirstHitOn(Player p, UUID target) {
		return repeatHits(p, target) == 0;
	}

	/** True for the first three hits on this target, i.e. Triple Strike's window. */
	public static boolean isTripleStrikeHitOn(Player p, UUID target) {
		return repeatHits(p, target) < 3;
	}

	/** True when THIS melee hit is the tenth, which is the Tarantula Ring's x1.15 (§7). */
	public static boolean isTarantulaHit(Player p) {
		if(p == null) return false;
		return (meleeHits.getOrDefault(p.getUniqueId(), 0) + 1) % 10 == 0;
	}

	// ===================== kill-driven windows =====================

	/**
	 * Register a kill.  It arms TWO independent windows and they must not be conflated (§1.14): a one-shot
	 * post-kill buff that expires after 5s if unspent, and the kill combo, which resets when 3s pass between
	 * kills.  One kill does both.
	 */
	public static void noteKill(Player p) {
		if(p == null) return;
		UUID id = p.getUniqueId();
		int now = MinecraftServer.currentTick;
		postKillExpiry.put(id, now + ClassBonuses.BERSERK_POST_KILL_TICKS);
		if(now > comboExpiry.getOrDefault(id, 0)) comboCount.put(id, 0);
		comboCount.merge(id, 1, Integer::sum);
		comboExpiry.put(id, now + 60); // a combo is consecutive kills <=3s apart
	}

	public static boolean hasPostKillBuff(Player p) {
		return p != null && MinecraftServer.currentTick < postKillExpiry.getOrDefault(p.getUniqueId(), 0);
	}

	/** Spend the post-kill buff.  The next hit consumes it whether or not it was still live. */
	public static void spendPostKillBuff(Player p) {
		if(p != null) postKillExpiry.remove(p.getUniqueId());
	}

	/** Combo's additive %: +1% per mob killed in the chain, capped at +50% (§7). */
	public static double comboAdditive(Player p) {
		if(p == null) return 0;
		UUID id = p.getUniqueId();
		if(MinecraftServer.currentTick > comboExpiry.getOrDefault(id, 0)) return 0;
		return Math.min(comboCount.getOrDefault(id, 0), 50);
	}

	// ===================== Berserk ultimate =====================

	public static void startBerserkUltimate(Player p) {
		if(p != null) {
			berserkUltimateEnd.put(p.getUniqueId(), MinecraftServer.currentTick + ClassBonuses.BERSERK_ULTIMATE_TICKS);
		}
	}

	public static boolean berserkUltimateActive(Player p) {
		return p != null && MinecraftServer.currentTick < berserkUltimateEnd.getOrDefault(p.getUniqueId(), 0);
	}

	// ===================== rolling damage history =====================

	/**
	 * Record a finished hit.  <b>Only real hits go in</b> (MAP.md §1.14): {@code Damage.deal} keeps out both
	 * secondary instances (procs, Cleave) and DERIVED ones - the abilities that read this history and copy a figure
	 * out of it.  Either would close a loop where the buffer feeds on its own output.
	 */
	public static void recordDamage(Player p, double sbDamage) {
		if(p == null || sbDamage <= 0) return;
		Deque<Hit> q = history.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
		q.addLast(new Hit(MinecraftServer.currentTick, sbDamage));
		int cutoff = MinecraftServer.currentTick - HISTORY_TICKS;
		// Bounded two ways: by age for the time query, and by a generous count so a Terminator volley cannot grow
		// it without limit.  The by-count query only ever looks at the last HISTORY_HITS entries.
		while(!q.isEmpty() && (q.peekFirst().tick() < cutoff || q.size() > 4096)) q.removeFirst();
	}

	/**
	 * Largest hit within the last {@code ticks}.  All four consumers query this way: Berserk's axe throw,
	 * Explosive Shot and Rapid Fire over 60s, Venomous over 100 ticks.
	 */
	public static double maxInLastTicks(Player p, int ticks) {
		if(p == null) return 0;
		Deque<Hit> q = history.get(p.getUniqueId());
		if(q == null) return 0;
		int cutoff = MinecraftServer.currentTick - ticks;
		double max = 0;
		for(Hit h : q) if(h.tick() >= cutoff) max = Math.max(max, h.damage());
		return max;
	}

	/**
	 * Venomous's "DPS": <b>eight times the biggest hit in the last 100 ticks</b>.  One stack is 2% of it, so the
	 * 40-stack cap is 80% of this figure, i.e. 640% of that biggest hit.
	 * <p>
	 * Only PRIMARY hits reach the history (see {@link Damage}), which is load-bearing here rather than tidy: a
	 * Venomous tick is itself several times the hit that spawned it, so recording one would raise the DPS figure
	 * the next tick reads, and so on.
	 */
	public static double venomousDps(Player p) {
		return maxInLastTicks(p, VENOMOUS_WINDOW_TICKS) * VENOMOUS_DPS_FACTOR;
	}

	/** Venomous's ramp on one target: +2% per hit, capped at 40 hits (so 80% of the DPS figure). */
	public static int venomousStacks(Player p, UUID target) {
		if(p == null || target == null) return 0;
		return venomousHits.getOrDefault(p.getUniqueId(), Map.of()).getOrDefault(target, 0);
	}

	/** Advance the Venomous ramp on one target and return the new stack count, capped at 40. */
	public static int noteVenomousHit(Player p, UUID target) {
		if(p == null || target == null) return 0;
		Map<UUID, Integer> perTarget = venomousHits.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
		int next = Math.min(perTarget.getOrDefault(target, 0) + 1, 40);
		perTarget.put(target, next);
		return next;
	}
}
