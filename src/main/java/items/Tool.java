package items;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * An item that acts on BLOCKS rather than on entities: the Dungeonbreaker and the Infinityboom TNT.
 * <p>
 * Both hooks return true if the tool handled the event, which is what stops {@code CustomItems.onBlockBreak} and
 * {@code onInfinityboomPlace} falling through to their default handling.
 */
public interface Tool extends Item {

	/**
	 * This tool broke a block.  Vanilla's own break is ALWAYS cancelled before this runs and the removal done
	 * here instead, without physics: a vanilla break runs {@code updateNeighbourShapes} on the six neighbours and
	 * tears out anything support-dependent (carpets, torches, rails, a nether portal's frame), through a path
	 * that fires no {@code BlockPhysicsEvent} and so cannot be vetoed from a listener.
	 *
	 * @return true if this tool handled the removal itself.
	 */
	default boolean onBreak(Player p, Block block) {
		return false;
	}

	/**
	 * Vanilla decided to place this item as a block, which means no ability consumed the click.  The placement is
	 * already cancelled by the caller; this is the chance to do whatever the item does instead.
	 *
	 * @param against the block clicked against, i.e. the same "block vanilla says was interacted with" that the
	 *                click path uses
	 * @return true if this tool handled it.
	 */
	default boolean onPlace(Player p, Block against) {
		return false;
	}

	/** True for an item that must NEVER actually break a block, whatever its can-break stamp says. */
	default boolean neverBreaks() {
		return false;
	}

	/**
	 * True for an item that must never be left in the world as a real block, so a vanilla placement is vetoed and
	 * {@link #onPlace} run instead.  The Infinityboom TNT: it carries a can-place-on-anything stamp for
	 * adventure mode, so every path where no ability consumed the click would otherwise place it for real.
	 */
	default boolean vetoesPlacement() {
		return false;
	}
}
