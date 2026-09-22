package pets;

import damage.Rarity;

/**
 * The item a pet is holding.  Cosmetic here: it is drawn as the {@code Held Item:} block at the bottom of a pet's
 * lore and nothing reads it back, because {@code damage/Pet}'s figures already bake in whatever item and level
 * this project assumes (MAP.md §1.13).  It exists so the menu shows the same pets the owner actually runs.
 * <p>
 * <b>"Hephaestus Tiger Plushie" is not a real item</b>, and it is what was asked for on the Phoenix.  It
 * conflates two that are: the <i>Hephaestus Plushie</i> (legendary pet item, +50 Attack Speed) and the
 * <i>Crochet Tiger Plushie</i> (epic, +35 Attack Speed) - and the first is literally the upgrade of the second,
 * which is how the two names merged.  The Phoenix carries the Hephaestus Plushie below; swap it for a new
 * constant here if the intent was the Crochet Tiger Plushie instead.
 */
public enum HeldPetItem {
	/** What the three stat pets hold: the flat +50% that the pet figures in {@code damage/Pet} assume. */
	HEPHAESTUS_RELIC("Hephaestus Relic", Rarity.LEGENDARY, "Increases all pet stats by 50%."),
	/** The Phoenix's, and the correction described on this enum. */
	HEPHAESTUS_PLUSHIE("Hephaestus Plushie", Rarity.LEGENDARY, "Grants +50 Attack Speed.");

	private final String displayName;
	private final Rarity rarity;
	private final String effect;

	HeldPetItem(String displayName, Rarity rarity, String effect) {
		this.displayName = displayName;
		this.rarity = rarity;
		this.effect = effect;
	}

	/** The item's name line, in its own rarity's colour - the same rule {@code items.Item.colouredName} follows. */
	public String colouredName() {
		return "<" + rarity.colour() + ">" + displayName;
	}

	/** The one line under the name: what the item does. */
	public String effect() {
		return effect;
	}
}
