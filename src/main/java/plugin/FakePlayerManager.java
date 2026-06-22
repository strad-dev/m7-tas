package plugin;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * TAS-only fake-player system — stubbed to no-ops for the practice-only fork.
 *
 * <p>The practice server has NO fake players: bosses aggro real players. The heavy NMS
 * implementation (spawning real {@code ServerPlayer}s with dummy connections, the per-tick fake
 * ticker, skin fetching, launch impulses, custom connections) lived here and is preserved in git
 * history on {@code main}.
 *
 * <p>The methods below are kept as no-ops because they're referenced pervasively across practice
 * code — almost always as "is this player a fake?" guards, which are simply always false now
 * ({@link #getFakePlayers()} is empty), preserving correct behaviour without editing ~15 call sites.
 */
public class FakePlayerManager {
	// Always empty in the practice fork — there are no fake players.
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

	/** No-op: launch impulses only applied to fake players, of which there are none. */
	public static void launch(Player fake, Vector velocity) {
	}
}
