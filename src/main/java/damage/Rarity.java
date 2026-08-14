package damage;

/**
 * Item rarity.  Reforge (§2.1) and gemstone (§2.2) values are keyed on it.
 * <p>
 * MAP.md §2.4: <b>rarity is an input, not a stat.</b> An {@link ItemDef} stores its BASE rarity plus a
 * recombobulated flag and derives the effective one, so changing whether an item is recombed is a one-word edit.
 * Everything in this plugin is recombed (§1.0.9), which is why the effective rarity is also the item's display
 * colour - and why the tables must always be read at the effective rarity, never the base.
 */
public enum Rarity {
	COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC;

	/** One tier up, i.e. what recombobulating does.  MYTHIC is the ceiling and stays put. */
	public Rarity recombobulated() {
		return this == MYTHIC ? MYTHIC : values()[ordinal() + 1];
	}
}
