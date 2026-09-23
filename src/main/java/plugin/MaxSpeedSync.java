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
 * Keeps a REAL player's <b>Max Speed</b> in step with what they are wearing and which pet they have out.
 *
 * <h2>Max Speed, not speed</h2>
 * This plugin models the SkyBlock Max Speed <i>cap</i> and assumes a player is always AT it, so one number is
 * the whole of it.  It is a <b>sum of parts</b>, each stated once where it belongs:
 * <pre>
 *   {@link #BASE_MAX_SPEED} 400
 * + {@link #ALPHA_SHARD} 50          when the alpha timings are on (an alpha-exclusive shard)
 * + {@code damage.Pet.maxSpeedBonus} 150 for the Black Cat (100 pet + 50 Unalloyed Speed), 0 for every other pet
 * + {@code items.Wearable.maxSpeedBonus} 100 for the Racing Helmet, 0 for everything else
 * </pre>
 *
 * <p><b>Those four terms reproduce every number this used to hardcode</b>, which is how the old model was found
 * to be wrong.  It read one finished figure off the helmet - 400 bare, 550 in a Cow Hat, 650 in a Racing Helmet
 * - and those are not three helmet facts: both hats force the Black Cat in the assumed modes, so 550 was
 * 400 + the cat, and 650 was 400 + the cat + the helmet's own 100.  Adding the parts instead gets all three
 * back, and gets the cases the old model could not express:
 * <ul>
 *   <li><b>realistic mode</b>, where the player picks their own pet, so a Black Cat is worth its 150 <b>in a
 *       Bonzo Mask</b> - which the old model scored at 400, since no hat was on;</li>
 *   <li>a Racing Helmet <b>without</b> the Black Cat (500), which the old model could not produce at all.</li>
 * </ul>
 *
 * <h2>No mode branch</h2>
 * The pet comes from {@code damage.Pet.forPlayer}, which already answers "assumed from the hat" in classic and
 * Perfect RNG and "whatever they summoned" in realistic.  So this class needs no {@code manualPets()} test: one
 * formula, and the mode decides only where the pet term comes from.
 *
 * <h2>The poll</h2>
 * Driven by a per-tick poll because real players change helmets through the vanilla inventory (right-click,
 * drag, shift-click), which calls nothing, and because a pet can change from four autopet triggers that are not
 * inventory events either.  Fake players are skipped - their speed is script-managed.
 *
 * <p>Speed is re-applied only on a <b>transition</b>, so a manually-set speed ({@code /setspeed}) survives until
 * something that really does feed the sum changes.  That covers an {@link Alpha} flip and a
 * {@code /dungeonsettings} mode change for free: both move the total without anyone touching a helmet, and the
 * next tick reads it as an ordinary transition.
 *
 * <p>The same poll also applies a 50% movement-speed debuff (a separate, composing modifier) while a real player
 * carries a Wither-King relic without a Cow Hat equipped, cleared once the relic is placed or a Cow Hat is worn.
 * That one is about carrying a relic and has nothing to do with pets or with the sum above.
 */
public final class MaxSpeedSync {
	private MaxSpeedSync() {}

	private static BukkitTask task;

	/** What a player with no pet bonus and no speed helmet is on. */
	private static final int BASE_MAX_SPEED = 400;
	/** The alpha-exclusive shard, worth +50 Max Speed while the alpha timings are on. */
	private static final int ALPHA_SHARD = 50;

	/** Each real player's Max Speed as of last tick; used to detect transitions. */
	private static final Map<UUID, Integer> lastMaxSpeed = new HashMap<>();
	/** Whether each real player had the relic carry-debuff applied as of last tick; used to detect transitions. */
	private static final Map<UUID, Boolean> lastRelicDebuff = new HashMap<>();

	public static void start() {
		if(task != null && !task.isCancelled()) task.cancel();
		task = new BukkitRunnable() {
			@Override
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					if(FakePlayerManager.getFakePlayers().containsValue(p)) continue; // fakes are script-managed
					int now = maxSpeed(p);
					Integer prev = lastMaxSpeed.put(p.getUniqueId(), now);
					// Only on a real transition, never on first observation, which avoids clobbering a manual speed.
					if(prev != null && prev != now) {
						Utils.setSpeed(p, now);
					}

					// Carrying a relic without a Cow Hat equipped → 50% speed debuff until it's placed.
					boolean debuff = carryingRelic(p) && !exemptsRelicDebuff(p.getInventory().getHelmet());
					Boolean prevDebuff = lastRelicDebuff.put(p.getUniqueId(), debuff);
					if(prevDebuff == null || prevDebuff != debuff) {
						Utils.setRelicDebuff(p, debuff);
					}
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	/**
	 * The Max Speed this player should be on right now: the sum in the class note.
	 * <p>
	 * Public because {@code instructions/Actions.swapItems} applies the same number for a fake player on a helmet
	 * swap - one formula, so the two halves cannot drift the way the hardcoded triple did.
	 */
	public static int maxSpeed(Player p) {
		int total = BASE_MAX_SPEED + (Alpha.enabled() ? ALPHA_SHARD : 0);
		// MELEE is a placeholder, the same one StatLore passes when it needs a pet rather than a path's pet:
		// the assumed table only branches on the path AFTER the hat check, and the Black Cat is the one pet with
		// a Max Speed bonus, so every path gives the same answer here.  A second pet with a bonus would make the
		// path matter, and this is the line that would have to grow a real one.
		total += damage.Pet.forPlayer(p, damage.DamagePath.MELEE).maxSpeedBonus();
		items.Wearable worn = items.ItemRegistry.wearable(p.getInventory().getHelmet());
		if(worn != null) total += worn.maxSpeedBonus();
		return total;
	}

	/** Set a real player's Max Speed from the sum above and seed the transition map.  Called on join. */
	public static void initSpeed(Player p) {
		int now = maxSpeed(p);
		Utils.setSpeed(p, now);
		lastMaxSpeed.put(p.getUniqueId(), now);
	}

	/** Drop a player's cached transition state (on quit) so a relog re-evaluates speed + relic debuff cleanly. */
	public static void forget(UUID id) {
		lastMaxSpeed.remove(id);
		lastRelicDebuff.remove(id);
	}

	/** True if any slot of the player's inventory holds a genuine Wither-King relic. */
	private static boolean carryingRelic(Player p) {
		for(ItemStack item : p.getInventory().getContents()) {
			if(instructions.bosses.witherking.WitherKing.relicColorOfItem(item) != null) return true;
		}
		return false;
	}

	/** True if the worn helmet cancels the relic carry debuff.  Only the Cow Hat does. */
	private static boolean exemptsRelicDebuff(ItemStack helmet) {
		items.Wearable worn = items.ItemRegistry.wearable(helmet);
		return worn != null && worn.exemptsRelicDebuff();
	}
}
