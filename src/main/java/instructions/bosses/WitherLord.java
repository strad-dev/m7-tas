package instructions.bosses;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.Utils;

import java.util.List;

/**
 * Shared base class for the four "real" wither bosses (Maxor, Storm, Goldor, Necron).
 * WitherKing is intentionally excluded — its 5-HP scale, MAGIC name formatting,
 * dragon-driven HP decrement, and single-arg constructor don't fit the abstraction.
 *
 * Each subclass exposes a {@code public static final <Subclass> INSTANCE = new <Subclass>();}
 * singleton. The instance is reused across fights; {@link #start(World, boolean)} resets
 * all per-fight state via {@link #resetState()} so each fight begins clean.
 */
@SuppressWarnings("DataFlowIssue")
public abstract class WitherLord {
	protected Wither boss;
	protected World world;
	protected int tick;
	protected BukkitTask tickerTask;
	protected boolean dying;
	protected boolean doContinue;
	/** Player-side transition to the next phase, run from {@link #chainNext} the tick this boss chains. Armed by TAS.runTAS. */
	protected Runnable playerHandoff;

	/**
	 * Entry point for a fresh fight. Cleans previous state, spawns the boss with the
	 * shared boilerplate, then hands off to {@link #onStart()} for boss-specific setup.
	 */
	public final void start(World w, boolean doContinue) {
		this.world = w;
		this.doContinue = doContinue;

		// Clean previous fight's entity + ticker
		if(boss != null) {
			boss.remove();
			boss = null;
		}
		if(tickerTask != null && !tickerTask.isCancelled()) {
			tickerTask.cancel();
			tickerTask = null;
		}
		resetState();

		// Reset base-class flags
		this.tick = 0;
		Utils.markPhaseStart();
		this.dying = false;

		spawn();
		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(boss, displayName()), 1);

		onStart();
		startTicker();
	}

	private void spawn() {
		boss = (Wither) world.spawnEntity(spawnLocation(), EntityType.WITHER);
		boss.setAI(false);
		boss.setSilent(true);
		boss.setPersistent(true);
		boss.setRemoveWhenFarAway(false);
		boss.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + displayName() + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.YELLOW + displayHealth() + ChatColor.RED + "❤");
		boss.setCustomNameVisible(true);
		boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth());
		boss.getAttribute(Attribute.ARMOR).setBaseValue(-30);
		boss.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(-20);
		boss.setHealth(maxHealth());
		boss.addScoreboardTag("TASWither");
		boss.addScoreboardTag("TAS" + name());
		boss.removeScoreboardTag("TASDying");
		WitherActions.setWitherArmor(boss, true);
	}

	private void startTicker() {
		tickerTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), () -> tick++, 0L, 1L);
	}

	/**
	 * Accurate, ordering-independent phase tick for DISPLAY ONLY.
	 *
	 * The {@link #tick} field is an increment counter advanced by a repeating task in the scheduler heartbeat.
	 * A scheduled action (terminal click, crystal pickup, death dialogue) runs in that same heartbeat AFTER the
	 * increment, so it reads {@code tick} already +1 for that tick; an entity-physics event (a sharpshooter
	 * arrow hit) runs before the heartbeat and reads it un-incremented. That split is the off-by-one.
	 *
	 * This instead subtracts the phase-start server tick from the server's own tick counter, which is constant
	 * across the whole server tick — so a scheduled action at phase-delay D and an entity event D ticks in BOTH
	 * read exactly D, with no +1, in every boss fight. {@link #tick} is deliberately left untouched for
	 * behavior/relative checks (Goldor patrol slow window, Storm crush poll) so this is purely a display fix.
	 */
	protected final int displayTick() {
		return Utils.phaseTick();
	}

	// --- Subclass hooks ---

	/** Raw identifier (no formatting) — used in scoreboard tags as "TAS" + name(). */
	protected abstract String name();

	/** Display string used in custom-name, boss-bar, and chat. Same as name() for the four real subclasses. */
	protected abstract String displayName();

	protected abstract Location spawnLocation();

	/** Internal HP scale (300 for Maxor, 600 for Storm, etc.). Not the display HP. */
	protected abstract double maxHealth();

	/** Display HP string for the custom name suffix, e.g. "800M", "1B", "1.2B". */
	protected abstract String displayHealth();

	/** PRE_<NAME>_TICKS offset used by {@link #formatTick(int)} to render the run-overall column. */
	protected abstract int previousTicks();

	/** Subclass-specific fight setup: dialogue, movement, mob spawns, etc. */
	protected abstract void onStart();

	/** Chain to the next boss in the run. Each subclass decides its own doContinue semantics. */
	protected abstract void chainNext(boolean doContinue);

	/** Zero out per-fight flags, counters, scheduled tasks, and collections. Called at the start of every {@link #start}. */
	protected abstract void resetState();

	// --- Shared helpers ---

	protected final void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] " + displayName() + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}

	protected final String formatTick(int t) {
		int overall = t + previousTicks();
		return ChatColor.GREEN + String.format("%s ticks (%.2f seconds) | Overall: %s ticks (%.2f seconds)",
				formatWithSpaces(t), t / 20.0, formatWithSpaces(overall), overall / 20.0);
	}

	protected static String formatWithSpaces(int n) {
		StringBuilder sb = new StringBuilder();
		String s = String.valueOf(n);
		for(int i = 0; i < s.length(); i++) {
			if(i > 0 && (s.length() - i) % 3 == 0) sb.append(' ');
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}

	protected final void setArmor(boolean on) {
		WitherActions.setWitherArmor(boss, on);
	}

	protected final void setAggro(LivingEntity target, double stopDistance, double yOffset, double maxSpeed) {
		WitherActions.setWitherAggro(boss, target, stopDistance, yOffset, maxSpeed);
	}

	protected final void clearAggro() {
		WitherActions.clearWitherAggro(boss);
	}

	/** Arm the player-side transition fired when this boss chains to the next (see {@link #runPlayerHandoff}). Armed by TAS.runTAS. */
	public final void armPlayerHandoff(Runnable handoff) {
		this.playerHandoff = handoff;
	}

	/** Run the armed player-side transition, if any. Called from subclass {@link #chainNext} the tick the next boss spawns. */
	protected final void runPlayerHandoff() {
		if(playerHandoff != null) playerHandoff.run();
	}

	// --- Generic-listener helper: find the WitherLord that owns this wither entity ---

	private static final List<WitherLord> SUBCLASS_INSTANCES = new java.util.ArrayList<>();

	/** Registers a subclass singleton so {@link #activeFor(Wither)} can find it. Called from each subclass's static initializer. */
	protected static void register(WitherLord instance) {
		SUBCLASS_INSTANCES.add(instance);
	}

	/** Returns the WitherLord whose currently-spawned boss matches {@code w}, or null if none match. */
	public static WitherLord activeFor(Wither w) {
		for(WitherLord lord : SUBCLASS_INSTANCES) {
			if(lord.boss != null && lord.boss.equals(w)) return lord;
		}
		return null;
	}

	/** Returns the currently-spawned boss entity (or null between fights). */
	public final Wither getBoss() {
		return boss;
	}

	public final boolean isDying() {
		return dying;
	}
}
