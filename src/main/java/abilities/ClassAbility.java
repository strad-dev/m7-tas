package abilities;

import damage.DungeonClass;
import org.bukkit.entity.Player;

/**
 * Drop-key ability owned by a dungeon class, not an item. Drop is always cancelled (it's an ability key here);
 * sprinting picks which one fires: {@code drop} is the ultimate, {@code drop stack} (not sprinting) the regular one.
 * <p>
 * Kept apart from {@code items.Item}: no ItemStack, lore ID or stats, so faking them as items would break
 * {@code Item}'s contract. Shares the cooldown store with {@code AbilityItem} ({@code plugin/Cooldowns}).
 */
public interface ClassAbility {

	DungeonClass owner();

	/** True for the class's ultimate, the one a non-sprinting drop does not fire. */
	boolean ultimate();

	/** Base cooldown in ticks, before {@link #mageReduced()}. */
	int cooldownTicks();

	/** Whether the Mage cooldown reduction applies. Only Guided Sheep: it's the only one of the five a Mage can cast. */
	default boolean mageReduced() {
		return false;
	}

	/** {@code plugin/Cooldowns} key. Distinct per ability so a class's two never share a clock. */
	default String cooldownKey() {
		return "class/" + owner() + "/" + getClass().getSimpleName();
	}

	/** @return true if it fired, which is what spends the cooldown. */
	boolean cast(Player p);
}
