package damage;

import org.bukkit.entity.Player;

/**
 * The Ragnarock Axe's buff window (DAMAGE_PLAN.md §1.7).
 * <p>
 * The window itself is owned by {@code listeners/CustomItems} - it runs the 3s wind-up, arms the buff and expires
 * it, and marks the state with the {@code RagBuff} scoreboard tag.  This class is only the reader the stat layer
 * goes through, so the stat side never has to know about a scoreboard tag.
 * <p>
 * What the buff GRANTS is not here either: it is +150% of the axe's own Strength stat, computed in
 * {@link Stats#ragnarockStrength} from the axe's authored terms.  It is a bonus STAT, so it goes through the stat
 * layer rather than a vanilla Strength potion effect, and it keeps applying after the axe leaves the hand -
 * casting Ragnarock and then switching to a hitting weapon is the entire point of the item.
 * <p>
 * The real Hypixel gate is "take no damage for 3 seconds", which cannot exist here because players are
 * invulnerable (§4).  The stand-in is "keep the axe in the main hand for those 3 seconds", enforced by
 * {@code CustomItems.ragWindup} - a deliberate substitution, not a reading of the item.
 */
public final class RagnarockBuff {
	private RagnarockBuff() {}

	/** The scoreboard tag {@code CustomItems} marks a live buff with. */
	public static final String TAG = "RagBuff";

	public static boolean isActive(Player p) {
		return p != null && p.getScoreboardTags().contains(TAG);
	}
}
