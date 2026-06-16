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
 * (Racing Helmet → 650, Cow Hat → 550, neither → 400) — the same transitions {@code Actions.swapItems}
 * applies for fake players, but driven by a per-tick poll because real players equip helmets through the
 * vanilla inventory (right-click, drag, shift-click), which never calls {@code swapItems}.
 *
 * <p>Fake players are skipped (their speed is script-managed). Speed is only re-applied on a helmet-type
 * TRANSITION, so a real player's manually-set speed (e.g. {@code /setspeed}) is left untouched unless they
 * actually change which speed helmet they're wearing.
 */
public final class HelmetSpeedSync {
	private HelmetSpeedSync() {}

	private static BukkitTask task;
	/** Speed implied by each real player's helmet as of last tick (650/550/400); used to detect transitions. */
	private static final Map<UUID, Integer> lastHelmetSpeed = new HashMap<>();

	public static void start() {
		if(task != null && !task.isCancelled()) task.cancel();
		task = new BukkitRunnable() {
			@Override
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					if(FakePlayerManager.getFakePlayers().containsValue(p)) continue; // fakes are script-managed
					int implied = impliedSpeed(p.getInventory().getHelmet());
					Integer prev = lastHelmetSpeed.put(p.getUniqueId(), implied);
					// Only on a real transition — never on first observation (avoids clobbering a manual speed).
					if(prev != null && prev != implied) {
						Utils.setSpeed(p, implied);
					}
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private static int impliedSpeed(ItemStack helmet) {
		if(FakePlayerInventory.isRacingHelmet(helmet)) return 650;
		if(FakePlayerInventory.isCowHat(helmet)) return 550;
		return 400;
	}
}
