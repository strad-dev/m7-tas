package pets;

import damage.Rarity;

/**
 * The item a pet is holding.  Cosmetic here: it is drawn as the {@code Held Item:} block at the bottom of a pet's
 * lore and nothing reads it back, because {@code damage/Pet}'s figures already have it applied (MAP.md §1.13).
 * It exists so the menu shows the same pets the owner actually runs.
 * <p>
 * <b>A held item's rarity is its own, and says nothing about the pet's.</b>  They are two separate tiers printed
 * in two separate places: {@link #colouredName()} colours the {@code Held Item:} line, and the pet's own
 * {@link PetType#rarity()} colours its name and its footer.  A MYTHIC Black Cat holding an EPIC Unalloyed Speed
 * is the normal case, not a mismatch to reconcile.
 * <p>
 * The relic is on four of the five pets, and its +50% is <b>already baked into</b> the numbers in
 * {@code damage/Pet}: the Crow's +225 Intelligence is 150 x 1.5, the Ender Dragon's +90 Crit Damage is 60 x 1.5.
 * Unalloyed Speed is the exception and multiplies nothing - it raises a speed CAP, which this plugin does not
 * model at all - so the Black Cat's +100 Intelligence is its raw level-100 figure.
 */
public enum HeldPetItem {
	/** What the four stat pets hold.  The +50% is already inside every figure in {@code damage/Pet}. */
	HEPHAESTUS_RELIC("Hephaestus Relic", Rarity.LEGENDARY, "Increases all pet stats by 50%."),
	/** The Black Cat's.  Raises the speed CAP by 50 on top of the pet's own +100, and boosts no stat. */
	UNALLOYED_SPEED("Unalloyed Speed", Rarity.EPIC, "Grants +50 Max Speed Cap.");

	private final String displayName;
	private final Rarity rarity;
	private final String effect;

	HeldPetItem(String displayName, Rarity rarity, String effect) {
		this.displayName = displayName;
		this.rarity = rarity;
		this.effect = effect;
	}

	/** The item's name line, in ITS OWN rarity's colour - the same rule {@code items.Item.colouredName} follows. */
	public String colouredName() {
		return "<" + rarity.colour() + ">" + displayName;
	}

	/** The one line under the name: what the item does. */
	public String effect() {
		return effect;
	}
}
