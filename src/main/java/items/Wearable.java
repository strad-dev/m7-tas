package items;

import death.CheatDeath;
import org.bukkit.inventory.EquipmentSlot;

/**
 * A worn item: the two armour sets, the wearable heads and the Spring Boots.
 * <p>
 * This is what let the plugin stop identifying its wearables by comparing display-name constants in four
 * separate places ({@code FakePlayerInventory.isCowHat} / {@code isSpiritMask} / {@code isBonzoMask} /
 * {@code isRacingHelmet}, {@code MaxSpeedSync}, {@code ItemUtils.isThermoSet} and
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
	 * What this piece adds to the wearer's <b>Max Speed</b>, or 0 for a piece that adds nothing.
	 * <p>
	 * <b>A BONUS, not a total.</b>  It used to be the finished number a helmet implied (400 / 550 / 650) and that
	 * was three different facts added together, which is why the Cow Hat and the Racing Helmet disagreed about a
	 * stat neither of them owns: both force the Black Cat in the assumed modes, and it was the CAT's +150 in
	 * those numbers.  {@code plugin/MaxSpeedSync} sums the parts now - base, alpha shard, pet, helmet - so each
	 * one is stated once, where it belongs.
	 */
	default int maxSpeedBonus() {
		return 0;
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
