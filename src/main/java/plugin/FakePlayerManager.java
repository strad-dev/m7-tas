package plugin;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * TAS-only fake-player system, stubbed to no-ops for practice: no fakes, bosses aggro real players. The NMS
 * implementation is in git history on {@code main}.
 * <p>
 * Kept because ~15 call sites use it as an "is this a fake?" guard, always false now ({@link #getFakePlayers()} is
 * empty).
 */
@SuppressWarnings("EmptyMethod")
public class FakePlayerManager {
	// Always empty in practice.
	private static final Map<String, Player> fakePlayers = new HashMap<>();

	public static Map<String, Player> getFakePlayers() {
		return fakePlayers;
	}

	/** No-op: no fakes to kick. */
	public static void kickAllFakes() {
		fakePlayers.clear();
	}

	/** No-op: no fakes hold custom connections. */
	public static void stopCustomConnection() {
	}

	/** No-op: launch impulses were fake-only. */
	public static void launch(Player fake, Vector velocity) {
	}
}
