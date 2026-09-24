package items;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Acts on BLOCKS: Dungeonbreaker and Infinityboom TNT. A hook returning true stops {@code CustomItems.onBlockBreak}
 * and {@code onInfinityboomPlace} falling through to default handling.
 */
public interface Tool extends Item {

	/**
	 * Vanilla's break is ALWAYS cancelled first and the removal done here without physics: vanilla's
	 * {@code updateNeighbourShapes} tears out carpets, torches, rails, portal frames, and fires no
	 * {@code BlockPhysicsEvent} a listener could veto.
	 *
	 * @return true if handled.
	 */
	default boolean onBreak(Player p, Block block) {
		return false;
	}

	/**
	 * Vanilla chose to place this as a block, so no ability consumed the click. Already cancelled by the caller.
	 *
	 * @param against the block vanilla says was interacted with, same as the click path
	 * @return true if handled.
	 */
	default boolean onPlace(Player p, Block against) {
		return false;
	}

	/** NEVER breaks a block, whatever its can-break stamp says. */
	default boolean neverBreaks() {
		return false;
	}

	/**
	 * Never left as a real block: vanilla placement is vetoed and {@link #onPlace} runs. The Infinityboom TNT has a
	 * place-on-anything stamp for adventure, so it would otherwise place for real whenever no ability fired.
	 */
	default boolean vetoesPlacement() {
		return false;
	}
}
