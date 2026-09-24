package damage;

/**
 * Item rarity; reforge (§2.1) and gem (§2.2) values key on it.
 * <p>
 * §2.4: rarity is an input, not a stat. {@link ItemDef} stores BASE rarity plus a recombobulated flag and derives the
 * effective one. Everything is recombed (§1.0.9) except the two special tiers, which can't be.
 * <p>
 * Effective rarity IS the display colour ({@link #colour()}): {@code items.Item} composes every name as
 * {@code <colour>Reforge BaseName}, so no factory hardcodes a colour. Tables are always read at effective rarity.
 */
public enum Rarity {
	COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC,
	/** Red tiers. Never recombobulated, and not "above MYTHIC" on reforge tables; a row must be authored ({@link Reforges}). */
	SPECIAL, VERY_SPECIAL;

	/** One tier up. MYTHIC is the ceiling; special tiers stay put. */
	public Rarity recombobulated() {
		return ordinal() >= MYTHIC.ordinal() ? this : values()[ordinal() + 1];
	}

	/** SkyBlock tooltip colours; both special tiers red (Diamond Necron Head, Dungeonbreaker). */
	public String colour() {
		return switch(this) {
			case COMMON -> "white";
			case UNCOMMON -> "green";
			case RARE -> "blue";
			case EPIC -> "dark_purple";
			case LEGENDARY -> "gold";
			case MYTHIC -> "light_purple";
			case SPECIAL, VERY_SPECIAL -> "red";
		};
	}

	/** As written in an item's rarity footer, e.g. {@code VERY SPECIAL}. */
	public String display() {
		return name().replace('_', ' ');
	}
}
