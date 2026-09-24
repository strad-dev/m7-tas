package damage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Gemstone values keyed {@code (type, quality, item rarity)} (MAP.md §2.2). Like {@link Reforges}, an
 * {@link ItemDef} stores a SLOT LIST, never a number, so fixing a cell here fixes every item.
 * <p>
 * Only Jasper (Strength), Sapphire (Int) and Onyx (Crit Damage) feed damage; the rest aren't modelled.
 * <p>
 * Every gem term in §1 and §1.10 is a Perfect gem at the item's effective rarity, which pinned two rarities §1 had
 * wrong: AOTV is Legendary, Ragnarock Axe Epic. Quality stays an axis so non-Perfect gems are expressible.
 */
public final class Gemstones {
	private Gemstones() {}

	/** Each feeds exactly one stat. */
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
	 * What the slot ACCEPTS and what's socketed. §2.3: slots are typed; SAPPHIRE takes only Sapphire, COMBAT any of
	 * the three (hence Hyperion's Heroic-vs-Fabled gem choice). Recombobulating adds no slots.
	 */
	public record Slot(Type accepts, Type gem, Quality quality) {
		public Slot {
			if(accepts != null && gem != accepts) {
				throw new IllegalArgumentException("A " + accepts + " slot cannot hold a " + gem + " gemstone");
			}
		}

		/** Combat slot (accepts any of the three). */
		public static Slot combat(Type gem, Quality quality) {
			return new Slot(null, gem, quality);
		}

		/** Typed to one gem, e.g. Hyperion's dedicated Sapphire slot. */
		public static Slot typed(Type gem, Quality quality) {
			return new Slot(gem, gem, quality);
		}
	}

	// Rows Rough → Perfect, columns Common → Mythic, from the wiki's Gemstone Slot § Stat Bonuses.
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
		// Onyx Fine/Flawless really are non-monotonic on the wiki. Not a typo.
		TABLES.put(Type.ONYX, new int[][]{
				/* Rough    */ {1, 1, 2, 2, 3, 4},
				/* Flawed   */ {2, 2, 3, 3, 4, 6},
				/* Fine     */ {3, 3, 3, 4, 4, 5},
				/* Flawless */ {2, 2, 3, 3, 4, 6},
				/* Perfect  */ {5, 6, 7, 8, 10, 12},
		});
	}

	/** Stats one slot grants at the item's EFFECTIVE rarity. */
	public static StatBlock stats(Slot slot, Rarity rarity) {
		if(slot == null || slot.gem() == null) return StatBlock.EMPTY;
		int value = TABLES.get(slot.gem())[slot.quality().ordinal()][rarity.ordinal()];
		return StatBlock.of(slot.gem().stat(), value);
	}
}
