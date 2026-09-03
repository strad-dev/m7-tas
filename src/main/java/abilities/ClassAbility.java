package abilities;

import damage.DungeonClass;
import org.bukkit.entity.Player;

/**
 * A DROP-KEY ability, which belongs to a dungeon CLASS rather than to an item.
 * <p>
 * The drop key is an ability key in this plugin, never a way to lose an item, so the drop event is cancelled for
 * everyone; whether the presser has an ability to fire is this tree's question.  Sprinting picks which of the
 * two a class gets: {@code drop} is the <b>ultimate</b>, {@code drop stack} (i.e. not sprinting) the regular one.
 * <p>
 * Kept deliberately separate from {@code items.Item}: these have no ItemStack, no lore ID and no stats, so
 * modelling them as pseudo-items would put a permanent hole in {@code Item}'s contract.  What they DO share with
 * an {@code AbilityItem} is the cooldown store, so both spend {@code plugin/Cooldowns}.
 */
public interface ClassAbility {

	/** The class that has this ability. */
	DungeonClass owner();

	/** True if this is the class's ULTIMATE, i.e. the one a non-sprinting drop does not fire. */
	boolean ultimate();

	/** Base cooldown in ticks, before any {@link #mageReduced()} reduction. */
	int cooldownTicks();

	/**
	 * Whether {@link #cooldownTicks()} takes the Mage class's cooldown reduction.  Only Guided Sheep does, which
	 * is not a rule so much as an observation: it is the only one of the five a Mage can cast.
	 */
	default boolean mageReduced() {
		return false;
	}

	/** The {@code plugin/Cooldowns} key.  Distinct per ability, so the two a class has never share a clock. */
	default String cooldownKey() {
		return "class/" + owner() + "/" + getClass().getSimpleName();
	}

	/** Fire it.  @return true if it fired, which is what spends the cooldown. */
	boolean cast(Player p);
}
