package pets;

import damage.Rarity;

/**
 * Pet's held item. Cosmetic: drawn as the {@code Held Item:} block at the bottom of the lore, nothing reads it
 * back, since {@code damage/Pet}'s figures already include it (MAP.md §1.13). Exists so the menu shows the pets
 * the owner actually runs.
 * <p>
 * Held item rarity is its own, separate from the pet's: {@link #colouredName()} colours the {@code Held Item:}
 * line, {@link PetType#rarity()} colours the pet name and footer. MYTHIC Black Cat with EPIC Unalloyed Speed is
 * normal.
 * <p>
 * Relic is on four of five pets and its +50% is already baked into {@code damage/Pet}: Crow's +225 Intelligence
 * is 150 x 1.5, Ender Dragon's +90 Crit Damage is 60 x 1.5. Unalloyed Speed multiplies nothing (raises a speed
 * cap, not modelled), so Black Cat's +100 Intelligence is its raw level-100 figure.
 */
public enum HeldPetItem {
	/** Held by the four stat pets. +50% is already in every {@code damage/Pet} figure. */
	HEPHAESTUS_RELIC("Hephaestus Relic", Rarity.LEGENDARY, "Increases all pet stats by 50%."),
	/** Black Cat's. Raises speed cap by 50 on top of the pet's +100, boosts no stat. */
	UNALLOYED_SPEED("Unalloyed Speed", Rarity.EPIC, "Grants +50 Max Speed Cap.");

	private final String displayName;
	private final Rarity rarity;
	private final String effect;

	HeldPetItem(String displayName, Rarity rarity, String effect) {
		this.displayName = displayName;
		this.rarity = rarity;
		this.effect = effect;
	}

	/** Name line in the item's own rarity colour, same rule as {@code items.Item.colouredName}. */
	public String colouredName() {
		return "<" + rarity.colour() + ">" + displayName;
	}

	/** Line under the name: what the item does. */
	public String effect() {
		return effect;
	}
}
