package plugin;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * TAS-only movement auditing — a verbose dev tool that logged per-tick displacement of fake/real
 * players for speedrun analysis. Stubbed to no-ops for the practice fork; the original NMS
 * implementation is in git history on {@code main}.
 *
 * <p>Call sites remain across practice code (jump / spring boots / lava jump / bonzo staff, and run
 * resets); they now do nothing. {@code auditMove(...)} was driven only by the (removed) fake-player
 * ticker, so it's gone.
 */
public class MovementAudit {
	public static boolean hasAirborneAudit(UUID id) {
		return false;
	}

	public static void startAirborneAudit(Player p, String source) {
	}

	public static void cancelAirborneAudit(UUID id) {
	}

	public static void cancelAll() {
	}
}
