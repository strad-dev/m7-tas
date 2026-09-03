package items;

/**
 * An item with a CLICK ability: the direct analogue of {@code SkyBlock in Vanilla}'s {@code items/AbilityItem},
 * and the interface that replaced the two {@code switch(id)} blocks inside {@code CustomItems.handleCustomItems}.
 * <p>
 * A hook returns <b>true if the ability actually fired</b>.  That is what spends the cooldown, so an ability that
 * declines (no block in range, wrong game mode, nothing to hit) costs nothing.
 *
 * <h2>What stays in the dispatcher, and why</h2>
 * The gates that are not per-item stay above these hooks in {@code CustomItems}, because they are properties of
 * the CLICK rather than of the item: the spectator gate, the trap-room rule, the drop-key overlap, the two-tick
 * right-click gate, the {@code lastRightBlockTick} guard that drops the AIR trailing a BLOCK click, and the
 * one-per-tick left-click cap.  Per-ability cooldowns live in {@code plugin/Cooldowns}, keyed on
 * {@link #cooldownKey()}.
 * <p>
 * <b>An in-arena or in-mode restriction stays INSIDE the ability body</b> (the Aspect of the Void and Tactical
 * Insertion both refuse to work in the boss arena in adventure mode).  Deliberately not hoisted to a method here:
 * those two still report as fired, so the click is still consumed and the rate gate is still stamped, and hoisting
 * the test would quietly change that.
 */
public interface AbilityItem extends Item {

	// ===== the hooks =====

	default boolean hasRightClick() {
		return false;
	}

	default boolean hasLeftClick() {
		return false;
	}

	/** @return true if the ability fired, which is what spends {@link #cooldownTicks()}. */
	default boolean onRightClick(Cast cast) {
		return false;
	}

	/** @return true if the ability fired, which is what spends {@link #cooldownTicks()}. */
	default boolean onLeftClick(Cast cast) {
		return false;
	}

	// ===== cooldown =====

	/** Base cooldown in ticks, BEFORE the Mage class's reduction.  0 = none beyond the dispatcher's rate gate. */
	default int cooldownTicks() {
		return 0;
	}

	/**
	 * Whether {@link #cooldownTicks()} takes the Mage class's ability-cooldown reduction (half, or a quarter for
	 * a solo Mage).  False for the Terminator and its Salvation beam, which are WEAPONS, not abilities.
	 */
	default boolean mageReduced() {
		return true;
	}

	/** The {@code plugin/Cooldowns} key.  The lore ID where there is one, so it is stable across renames. */
	default String cooldownKey() {
		String id = loreId();
		return id.isEmpty() ? getClass().getName() : id;
	}

	/**
	 * Whether a click on cooldown tells the player so.  False for the rate caps that are not really cooldowns,
	 * which would otherwise spam a message on every click.
	 */
	default boolean announcesCooldown() {
		return true;
	}
}
