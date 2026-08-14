package damage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Gemstone values, keyed {@code (type, quality, item rarity)} (MAP.md §2.2).  Same story as
 * {@link Reforges}: an {@link ItemDef} stores a SLOT LIST, never a resolved number, so changing an item's gems is a
 * one-word edit and correcting a cell here fixes every item at once.
 * <p>
 * Only three gem types feed damage - Jasper to Strength, Sapphire to Intelligence, Onyx to Crit Damage.  The rest
 * (Ruby, Amethyst, Opal and the skill gems) grant stats nothing in this system reads, so they are not modelled.
 * <p>
 * Every gemstone term in §1 and §1.10 turns out to be a Perfect gem at that item's own effective rarity, which is
 * what pins two rarities §1 originally had wrong: the Aspect of the Void is Legendary and the Ragnarock Axe is
 * Epic.  Quality is still an axis rather than a constant so a non-Perfect gem stays expressible.
 */
public final class Gemstones {
	private Gemstones() {}

	/** The three gem types that matter for damage, each feeding exactly one stat. */
	public enum Type {
		JASPER(Stat.STRENGTH),
		SAPPHIRE(Stat.INTELLIGENCE),
		ONYX(Stat.CRIT_DAMAGE);

		private final Stat stat;

		Type(Stat stat) {
			this.stat = stat;
		}

		public Stat stat() {
			return stat;
		}
	}

	public enum Quality {ROUGH, FLAWED, FINE, FLAWLESS, PERFECT}

	/**
	 * A gemstone slot on an item: what the slot ACCEPTS, and what is socketed into it.
	 * <p>
	 * §2.3: slots are typed.  A {@code SAPPHIRE} slot accepts only Sapphire (so it is always Intelligence); a
	 * {@code COMBAT} slot accepts any of the three, which is what makes the Hyperion's Heroic-vs-Fabled gem choice
	 * possible in the same slot.  Recombobulating does not add slots, so the count comes from the base item.
	 */
	public record Slot(Type accepts, Type gem, Quality quality) {
		public Slot {
			if(accepts != null && gem != accepts) {
				throw new IllegalArgumentException("A " + accepts + " slot cannot hold a " + gem + " gemstone");
			}
		}

		/** A Combat slot (accepts any of the three) holding the given gem. */
		public static Slot combat(Type gem, Quality quality) {
			return new Slot(null, gem, quality);
		}

		/** A slot typed to one gem, e.g. the Hyperion's and Ice Spray Wand's dedicated Sapphire slot. */
		public static Slot typed(Type gem, Quality quality) {
			return new Slot(gem, gem, quality);
		}
	}

	// Rows are Rough → Perfect, columns Common → Mythic, transcribed from the wiki's Gemstone Slot § Stat Bonuses.
	private static final Map<Type, int[][]> TABLES = new EnumMap<>(Type.class);

	static {
		TABLES.put(Type.JASPER, new int[][]{
				//   Common Uncommon Rare Epic Legendary Mythic
				/* Rough    */ {1, 1, 1, 2, 3, 4},
				/* Flawed   */ {2, 2, 3, 4, 4, 5},
				/* Fine     */ {3, 3, 4, 5, 6, 7},
				/* Flawless */ {5, 6, 7, 8, 10, 12},
				/* Perfect  */ {6, 7, 9, 11, 13, 16},
		});
		TABLES.put(Type.SAPPHIRE, new int[][]{
				/* Rough    */ {2, 3, 4, 5, 6, 7},
				/* Flawed   */ {5, 5, 6, 8, 10, 10},
				/* Fine     */ {7, 8, 9, 10, 11, 12},
				/* Flawless */ {10, 11, 12, 14, 17, 20},
				/* Perfect  */ {12, 14, 17, 20, 24, 30},
		});
		// Onyx's Fine and Flawless rows really are non-monotonic on the wiki.  Transcribed as published, not a typo.
		TABLES.put(Type.ONYX, new int[][]{
				/* Rough    */ {1, 1, 2, 2, 3, 4},
				/* Flawed   */ {2, 2, 3, 3, 4, 6},
				/* Fine     */ {3, 3, 3, 4, 4, 5},
				/* Flawless */ {2, 2, 3, 3, 4, 6},
				/* Perfect  */ {5, 6, 7, 8, 10, 12},
		});
	}

	/** The stats one socketed slot grants on an item of the given EFFECTIVE rarity. */
	public static StatBlock stats(Slot slot, Rarity rarity) {
		if(slot == null || slot.gem() == null) return StatBlock.EMPTY;
		int value = TABLES.get(slot.gem())[slot.quality().ordinal()][rarity.ordinal()];
		return StatBlock.of(slot.gem().stat(), value);
	}
}
