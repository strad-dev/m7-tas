package damage;

/**
 * Item rarity.  Reforge (§2.1) and gemstone (§2.2) values are keyed on it.
 * <p>
 * MAP.md §2.4: <b>rarity is an input, not a stat.</b> An {@link ItemDef} stores its BASE rarity plus a
 * recombobulated flag and derives the effective one, so changing whether an item is recombed is a one-word edit.
 * Everything in this plugin is recombed (§1.0.9) except the two special tiers, which cannot be.
 * <p>
 * <b>The effective rarity IS the item's display colour</b>, via {@link #colour()}.  {@code items.Item} composes
 * every display name as {@code <colour>Reforge BaseName}, so no item factory hardcodes a colour any more and an
 * item can never read as the wrong tier.  That also means the tables must always be read at the effective
 * rarity, never the base.
 */
public enum Rarity {
	COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC,
	/**
	 * The red tiers.  Nothing recombobulates into or out of them, and they are not "above MYTHIC" on the reforge
	 * tables either - a reforge row for one has to be authored explicitly (see {@link Reforges}).
	 */
	SPECIAL, VERY_SPECIAL;

	/**
	 * One tier up, i.e. what recombobulating does.  MYTHIC is the ceiling, and the two special tiers stay put
	 * because a Recombobulator does not apply to them at all.
	 */
	public Rarity recombobulated() {
		return ordinal() >= MYTHIC.ordinal() ? this : values()[ordinal() + 1];
	}

	/**
	 * The MiniMessage colour this rarity is written in.  SkyBlock's own tooltip colours, with both special tiers
	 * red - which is why the Diamond Necron Head and the Dungeonbreaker are red.
	 */
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

	/** The tier's own name as SkyBlock writes it in an item's rarity footer, e.g. {@code VERY SPECIAL}. */
	public String display() {
		return name().replace('_', ' ');
	}
}
