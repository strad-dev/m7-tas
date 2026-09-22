package plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Auto-sets a REAL player's movement speed when they equip or remove a speed-granting helmet
 * (Racing Helmet → 650, Cow Hat → 550, neither → 400; 700 / 600 / 450 under the alpha timings), the same
 * transitions {@code Actions.swapItems}
 * applies for fake players, but driven by a per-tick poll because real players equip helmets through the
 * vanilla inventory (right-click, drag, shift-click), which never calls {@code swapItems}.
 *
 * <p>Fake players are skipped (their speed is script-managed). Speed is only re-applied on a helmet-type
 * TRANSITION, so a real player's manually-set speed (e.g. {@code /setspeed}) is left untouched unless they
 * actually change which speed helmet they're wearing.
 *
 * <p>The same poll also applies a 50% movement-speed debuff (a separate, composing modifier) while a real
 * player carries a Wither-King relic without a Cow Hat equipped, and is cleared once the relic is placed or a Cow
 * Hat is worn.
 */
public final class HelmetSpeedSync {
	private HelmetSpeedSync() {}

	private static BukkitTask task;
	/** Bare-headed movement speed: what a helmet granting none leaves a player on. */
	private static final int BARE_SPEED = 400, ALPHA_BARE_SPEED = 450;

	/**
	 * Speed implied by each real player's helmet as of last tick; used to detect transitions.
	 * <p>
	 * A flip of {@link Alpha} moves all three numbers without anyone touching a helmet, which the poll below reads
	 * as an ordinary transition and re-applies on the next tick.  That is the wanted behaviour and it is why the
	 * numbers are read live rather than latched: {@code /m7practice} sets the flag before the run arms, so the
	 * party is on the right speed before the first phase starts.
	 */
	private static final Map<UUID, Integer> lastHelmetSpeed = new HashMap<>();
	/** Whether each real player had the relic carry-debuff applied as of last tick; used to detect transitions. */
	private static final Map<UUID, Boolean> lastRelicDebuff = new HashMap<>();

	public static void start() {
		if(task != null && !task.isCancelled()) task.cancel();
		task = new BukkitRunnable() {
			@Override
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					if(FakePlayerManager.getFakePlayers().containsValue(p)) continue; // fakes are script-managed
					ItemStack helmet = p.getInventory().getHelmet();
					int implied = impliedSpeed(helmet);
					Integer prev = lastHelmetSpeed.put(p.getUniqueId(), implied);
					// Only on a real transition, never on first observation, which avoids clobbering a manual speed.
					if(prev != null && prev != implied) {
						Utils.setSpeed(p, implied);
					}

					// Carrying a relic without a Cow Hat equipped → 50% speed debuff until it's placed.
					boolean debuff = carryingRelic(p) && !exemptsRelicDebuff(helmet);
					Boolean prevDebuff = lastRelicDebuff.put(p.getUniqueId(), debuff);
					if(prevDebuff == null || prevDebuff != debuff) {
						Utils.setRelicDebuff(p, debuff);
					}
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	/** Set a real player's speed from their current helmet (400 default, 550 Cow Hat, 650 Racing Helmet; +50 on
	 *  each under alpha) and seed the transition map so the poll doesn't immediately re-apply. Called on join. */
	public static void initSpeed(Player p) {
		int implied = impliedSpeed(p.getInventory().getHelmet());
		Utils.setSpeed(p, implied);
		lastHelmetSpeed.put(p.getUniqueId(), implied);
	}

	/** Drop a player's cached transition state (on quit) so a relog re-evaluates speed + relic debuff cleanly. */
	public static void forget(UUID id) {
		lastHelmetSpeed.remove(id);
		lastRelicDebuff.remove(id);
	}

	/** True if any slot of the player's inventory holds a genuine Wither-King relic. */
	private static boolean carryingRelic(Player p) {
		for(ItemStack item : p.getInventory().getContents()) {
			if(instructions.bosses.witherking.WitherKing.relicColorOfItem(item) != null) return true;
		}
		return false;
	}

	/**
	 * The speed a helmet implies, straight off the worn item ({@code Wearable.impliedSpeed}), with
	 * {@link #BARE_SPEED} for a helmet that grants none.  This used to be two hardcoded display-name comparisons and two magic numbers; the
	 * numbers now live on the Racing Helmet and the Cow Hat themselves, so adding a third speed helmet is a
	 * one-line change to that item and nothing here.
	 */
	private static int impliedSpeed(ItemStack helmet) {
		items.Wearable worn = items.ItemRegistry.wearable(helmet);
		int implied = worn == null ? -1 : worn.impliedSpeed();
		return implied < 0 ? (Alpha.enabled() ? ALPHA_BARE_SPEED : BARE_SPEED) : implied;
	}

	/** True if the worn helmet cancels the relic carry debuff.  Only the Cow Hat does. */
	private static boolean exemptsRelicDebuff(ItemStack helmet) {
		items.Wearable worn = items.ItemRegistry.wearable(helmet);
		return worn != null && worn.exemptsRelicDebuff();
	}
}
