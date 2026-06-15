package plugin;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Boss-priority tick scheduler — the mechanism behind the "all boss actions run at the start of the tick" invariant
 * (see CLAUDE.md "Boss Tick Ordering").
 *
 * <p>A single repeating heartbeat, started once at plugin enable ({@link M7tas#onEnable}). Because it is created at
 * enable time, its Bukkit scheduler task id is lower than any per-run choreography (which is only scheduled when
 * {@code /tas} runs), and CraftBukkit executes same-tick tasks in task-id order. So every registered boss ticker runs
 * each tick BEFORE the players' beam/melee choreography — guaranteeing a mage-beam hit reads finalized boss state
 * (stun/enrage/HP) on the same tick instead of the previous one. That is what lets the player routines drop the
 * +1-tick offsets that previously forced the beam to wait a tick for the boss state.
 *
 * <p>Two ways to register work:
 * <ul>
 *   <li>{@link #addTicker} — a per-tick boss simulation step (movement, stun/enrage scans). Runs every tick in
 *       registration order, so a boss that needs "move, then scan" must register the mover first.
 *   <li>{@link #schedule} — a one-shot that fires at the START of a future tick (the boss-lane equivalent of
 *       {@code runTaskLater}), for timed boss state changes a player hit must observe deterministically.
 * </ul>
 *
 * <p>{@link CopyOnWriteArrayList} lets a ticker register/unregister itself (or another) from inside its {@code run()}
 * safely while the heartbeat is iterating.
 */
public final class BossScheduler {
	private BossScheduler() {}

	private static final List<Runnable> tickers = new CopyOnWriteArrayList<>();
	private static BukkitTask heartbeat;
	// Monotonic heartbeat counter, advanced once per tick at the START of the heartbeat (see start()). schedule()
	// uses it to fire a one-shot at the START of an exact future tick.
	private static long bossTick = 0;

	/** Start the heartbeat. Idempotent. Call once at plugin enable, before any boss can spawn. */
	public static void start() {
		if(heartbeat != null && !heartbeat.isCancelled()) return;
		heartbeat = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), () -> {
			// Advance FIRST, so bossTick reads the same value for the whole server tick — whether schedule() is
			// called from the boss lane (a ticker, here) or the player lane (a damage handler that runs later this
			// tick). That keeps delay semantics identical regardless of caller (see schedule()).
			bossTick++;
			// Isolate failures: these tickers used to be independent tasks, so one throwing must not stop the rest.
			for(Runnable ticker : tickers) {
				try {
					ticker.run();
				} catch(Throwable t) {
					M7tas.getInstance().getLogger().warning("Boss ticker threw: " + t);
				}
			}
		}, 0L, 1L);
	}

	/** Stop the heartbeat and drop every registered ticker. Call at plugin disable. */
	public static void stop() {
		if(heartbeat != null && !heartbeat.isCancelled()) heartbeat.cancel();
		heartbeat = null;
		tickers.clear();
	}

	/** Register a per-tick boss simulation step. Runs every tick, in registration order, before all player
	 *  choreography. Returns the handle for {@link #removeTicker}. */
	public static Runnable addTicker(Runnable ticker) {
		tickers.add(ticker);
		return ticker;
	}

	/** Unregister a ticker previously returned by {@link #addTicker} or {@link #schedule}. Safe to call from inside
	 *  the ticker. */
	public static void removeTicker(Runnable ticker) {
		if(ticker != null) tickers.remove(ticker);
	}

	/**
	 * Boss-lane equivalent of {@code runTaskLater}: runs {@code action} ONCE at the START of a future tick, in the
	 * heartbeat (before that tick's player choreography). Invoked while processing tick T (from either lane), the
	 * action fires at the start of tick {@code T + delayTicks}.
	 *
	 * <p>Use this — NOT {@code runTaskLater} / {@link Utils#scheduleTask} — for timed boss state changes a player hit
	 * must observe deterministically: stun→enrage, immunity/cooldown windows ending, interlude ends. Example: Maxor
	 * enters the laser at the start of tick 195 and schedules enrage with delay 160 → enrage fires at the start of
	 * tick 355, so a beam on tick 355 sees the already-re-armored boss. A raw runTaskLater would fire mid-tick, after
	 * the beam.
	 *
	 * @return the handle; pass to {@link #removeTicker} to cancel before it fires.
	 */
	public static Runnable schedule(Runnable action, long delayTicks) {
		final long target = bossTick + Math.max(1, delayTicks);
		final Runnable[] handle = new Runnable[1];
		handle[0] = () -> {
			if(bossTick >= target) {
				removeTicker(handle[0]);
				action.run();
			}
		};
		addTicker(handle[0]);
		return handle[0];
	}
}
