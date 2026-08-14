package damage;

/**
 * One named contribution to one stat on one item (MAP.md §2.4).
 * <p>
 * <b>Author terms, never totals.</b> The Ragnarock Axe's Strength is seven independent terms - base, stars, potato
 * books, art of war, reforge, gemstone, Chimera - not the number 626.  Editing the base moves nothing else; retuning
 * stars moves nothing else.  §2.4's hard rule is that no pre-summed constant (340, 626, 749.5) and no pre-scaled one
 * (2264.4, 1278.72, 5727.6) may appear anywhere in the code, because they are outputs.
 * <p>
 * Only {@link Source#BASE}, {@link Source#CATA_LEVEL} and {@link Source#STARS} are authored as values here.  Every
 * other source stores an ID and looks its value up: {@link Upgrade} for books and enchantments, {@link Reforges} for
 * the reforge, {@link Gemstones} for a slot, {@link Pet} for Chimera.
 */
public record StatTerm(Source source, Stat stat, double value) {

	public enum Source {
		/** The item's intrinsic stat, as the wiki prints it. */
		BASE,
		/** The item's Catacombs-level bonus, itemised beside the base so the x6.66 stays a pipeline stage. */
		CATA_LEVEL,
		/** A non-dungeon item's star bonus, which appears as a flat term because nothing scales it (§1.0.3). */
		STARS,
		BOOKS,
		ENCHANT,
		REFORGE,
		GEMSTONE,
		CHIMERA
	}

	public static StatTerm base(Stat stat, double value) {
		return new StatTerm(Source.BASE, stat, value);
	}

	public static StatTerm cataLevel(Stat stat, double value) {
		return new StatTerm(Source.CATA_LEVEL, stat, value);
	}

	public static StatTerm stars(Stat stat, double value) {
		return new StatTerm(Source.STARS, stat, value);
	}
}
