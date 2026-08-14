package damage;

/**
 * The stats this damage system reads.  See MAP.md §1.
 * <p>
 * {@link #core} is what {@link Scale#SB_CATA_MULT} applies to: MAP.md §1.0.1 names Damage, Strength, Crit
 * Damage and Intelligence as "the four core stats" that get the full x6.66 Catacombs scaling, and §1.0.2 puts
 * everything else on the stars-only x1.81.  Crit Chance is not core; nothing reads it either (§7 rules every hit a
 * crit), it is only stored because reforges grant it.
 */
public enum Stat {
	DAMAGE("Damage", "<red>", true),
	STRENGTH("Strength", "<red>", true),
	CRIT_DAMAGE("Crit Damage", "<blue>", true),
	INTELLIGENCE("Intelligence", "<aqua>", true),
	CRIT_CHANCE("Crit Chance", "<blue>", false),
	ABILITY_DAMAGE("Ability Damage", "<red>", false);

	// The SkyBlock glyphs (❁ ☠ ✎ ☣ ๑) used to live here, one per stat, and were appended to every lore row and every
	// /eq row.  They are gone rather than merely unused: Hypixel renders them from a resource-pack font, so on a
	// vanilla client they came out as tofu boxes, and the stat's name already says which stat it is.

	private final String display;
	private final String colour;
	private final boolean core;

	Stat(String display, String colour, boolean core) {
		this.display = display;
		this.colour = colour;
		this.core = core;
	}

	/** Human-readable name, as it appears on item lore ("Crit Damage"). */
	public String display() {
		return display;
	}

	/** MiniMessage colour tag this stat is rendered in. */
	public String colour() {
		return colour;
	}

	/** True for the four stats that take the full Catacombs multiplier rather than the stars-only one (§1.0.1-2). */
	public boolean core() {
		return core;
	}
}
