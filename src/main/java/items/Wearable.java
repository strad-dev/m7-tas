package items;

import death.CheatDeath;
import org.bukkit.inventory.EquipmentSlot;

/**
 * A worn item: the two armour sets, the wearable heads and the Spring Boots.
 * <p>
 * This is what let the plugin stop identifying its wearables by comparing display-name constants in four
 * separate places ({@code FakePlayerInventory.isCowHat} / {@code isSpiritMask} / {@code isBonzoMask} /
 * {@code isRacingHelmet}, {@code HelmetSpeedSync.impliedSpeed}, {@code ItemUtils.isThermoSet} and
 * {@code CheatDeath.pick}).  Each of those now asks the registry for the item and reads the property off it.
 * <p>
 * Note a wearable affects DAMAGE only through the stats it contributes (§1.10, §8).  The old x0.70 / x0.80
 * outgoing-damage penalties on the Spring Boots, Racing Helmet, Cow Hat and masks are deleted, not relocated
 * here: a helmet slot is exclusive, so wearing a Cow Hat already costs the Storm's Helmet's Intelligence, and a
 * multiplier on top double-penalised the same swap.
 */
public interface Wearable extends Item {

	EquipmentSlot slot();

	/**
	 * The movement speed wearing this implies, or -1 for a piece that does not set one.  Racing Helmet 650, Cow
	 * Hat 550; a helmet that grants neither leaves the player at the default 400.  Each is +50 under the alpha
	 * timings ({@code plugin/Alpha}), and each item owns its own pair.  Read by
	 * {@code plugin/HelmetSpeedSync}, which applies it only on a TRANSITION so a manual {@code /setspeed} is left
	 * alone.
	 */
	default int impliedSpeed() {
		return -1;
	}

	/**
	 * The cheat-death this piece provides while worn, or null.  Bonzo's Mask and the Spirit Mask; the Phoenix pet
	 * is the itemless fallback and so has no {@code Wearable}.  Precedence is {@link CheatDeath.Saver}'s own
	 * declaration order, not anything declared here.
	 */
	default CheatDeath.Saver saver() {
		return null;
	}

	/**
	 * The armour set this belongs to, or "" for a piece in none.  Only the Thermodynamic set has a bonus, and it
	 * is a RATE one: 4/4 raises the attack-speed cap, which is the Terminator's 5-tick cooldown becoming 4.  It is
	 * deliberately not a per-hit multiplier.
	 */
	default String setId() {
		return "";
	}

	/** True if wearing this cancels the Wither-King relic carry debuff (the Cow Hat). */
	default boolean exemptsRelicDebuff() {
		return false;
	}
}
