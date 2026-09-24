package damage;

import org.bukkit.entity.Player;

/**
 * Ragnarock Axe buff window (MAP.md §1.7). {@code listeners/CustomItems} owns the window (3s wind-up, arm, expire)
 * and marks it with the {@code RagBuff} tag; this is just the reader, so the stat side never sees a tag.
 * <p>
 * The buff is +150% of the axe's own Strength, computed in {@link Stats#ragnarockStrength}. A bonus STAT, not a
 * vanilla potion, and it keeps applying after the axe leaves the hand; casting then swapping is the point.
 * <p>
 * Hypixel's gate is "take no damage for 3 seconds", impossible with invulnerable players (§4). Stand-in: keep the axe
 * in the main hand for 3s ({@code items.combat.RagnarockAxe.ragWindup}), a deliberate substitution.
 */
public final class RagnarockBuff {
	private RagnarockBuff() {}

	/** Tag {@code CustomItems} marks a live buff with. */
	public static final String TAG = "RagBuff";

	public static boolean isActive(Player p) {
		return p != null && p.getScoreboardTags().contains(TAG);
	}
}
