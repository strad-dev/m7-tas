package damage;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One item's stat definition, built from independent terms rather than totals (MAP.md §2.4).
 * <p>
 * The four axes §2.4 requires to stay separate are separate fields here, so any one of them can gain a variant
 * without touching the others:
 * <ol>
 *   <li><b>Base item stats</b> - {@link #terms}, authored once per item.</li>
 *   <li><b>Reforge</b> - an id only; the values come from {@link Reforges} at {@code (reforge, category, rarity)}.</li>
 *   <li><b>Gemstones</b> - a slot LIST (never a scalar: the same piece can run 1 Sapphire + 1 Onyx or 2 Sapphire),
 *       resolved through {@link Gemstones} at the item's rarity.</li>
 *   <li><b>Global upgrades</b> - {@link Upgrade} ids; the item records WHICH it has, not their values.</li>
 * </ol>
 * One base entry serves every reforge variant: the Heroic and Withered (Fabled) Hyperions are not two items, they
 * are one base resolved with two reforge ids and two gem sets.
 */
public final class ItemDef {
	private final String displayName;
	private final String loreId;
	private final ItemCategory category;
	private final Rarity baseRarity;
	private final boolean recombobulated;
	private final boolean dungeonItem;
	private final List<StatTerm> terms;
	private final Set<Upgrade> upgrades;
	private final ReforgeId reforge;
	private final List<Gemstones.Slot> gemSlots;
	private final boolean chimera;
	private final double selfMultiplier;
	private final boolean shortbow;
	private final Ability ability;

	private ItemDef(Builder b) {
		this.displayName = b.displayName;
		this.loreId = b.loreId;
		this.category = b.category;
		this.baseRarity = b.baseRarity;
		this.recombobulated = b.recombobulated;
		this.dungeonItem = b.dungeonItem;
		this.terms = List.copyOf(b.terms);
		this.upgrades = b.upgrades.isEmpty() ? EnumSet.noneOf(Upgrade.class) : EnumSet.copyOf(b.upgrades);
		this.reforge = b.reforge;
		this.gemSlots = List.copyOf(b.gemSlots);
		this.chimera = b.chimera;
		this.selfMultiplier = b.selfMultiplier;
		this.shortbow = b.shortbow;
		this.ability = b.ability;
	}

	/** A right-click ability's own numbers (§7): its base damage and its per-ability Intelligence scalar. */
	public record Ability(double baseDamage, double intelligenceScaling) {}

	public String displayName() {
		return displayName;
	}

	public String loreId() {
		return loreId;
	}

	public ItemCategory category() {
		return category;
	}

	/**
	 * The EFFECTIVE rarity, i.e. the one the reforge and gemstone tables must be read at (§1.0.9).  Never store this
	 * - it is derived, so flipping {@code recombobulated} is a one-word edit rather than a sweep through every item.
	 */
	public Rarity rarity() {
		return recombobulated ? baseRarity.recombobulated() : baseRarity;
	}

	public boolean dungeonItem() {
		return dungeonItem;
	}

	public ReforgeId reforge() {
		return reforge;
	}

	/** True for a bow that is never drawn (§1.2): every shot is full damage and crits, ignoring draw scaling. */
	public boolean shortbow() {
		return shortbow;
	}

	/** This item's right-click ability, or null if it has none the damage system computes. */
	public Ability ability() {
		return ability;
	}

	/**
	 * This item's finished stat contribution, dungeon-scaled.
	 *
	 * @param pet the pet Chimera copies.  Ignored unless the item carries Chimera.
	 */
	public StatBlock stats(Pet pet) {
		StatBlock sum = StatBlock.EMPTY;
		for(Map.Entry<String, StatBlock> e : breakdown(pet).entrySet()) sum = sum.plus(e.getValue());
		return finish(sum);
	}

	/**
	 * The same computation, itemised by source, for {@code /verbose super} and for checking a table against §1.
	 * <p>
	 * The values here are UNSCALED (they are the plan's "terms" column); {@link #finish} is what applies the
	 * item-wide multiplier and the dungeon stage, and it must be applied to the SUM, never to a single term.
	 */
	public Map<String, StatBlock> breakdown(Pet pet) {
		Map<String, StatBlock> out = new LinkedHashMap<>();
		for(StatTerm t : terms) {
			out.merge(t.source().name().toLowerCase(java.util.Locale.ROOT),
					StatBlock.of(t.stat(), t.value()), StatBlock::plus);
		}
		for(Upgrade u : upgrades) out.merge(u.name().toLowerCase(java.util.Locale.ROOT), u.stats(), StatBlock::plus);
		StatBlock reforgeStats = Reforges.stats(reforge, category, rarity());
		if(!reforgeStats.isEmpty()) out.put("reforge (" + reforge.displayName() + ")", reforgeStats);
		StatBlock gems = StatBlock.EMPTY;
		for(Gemstones.Slot slot : gemSlots) gems = gems.plus(Gemstones.stats(slot, rarity()));
		if(!gems.isEmpty()) out.put("gemstones", gems);
		if(chimera && pet != null && !pet.chimeraCopy().isEmpty()) {
			out.put("chimera (" + pet + ")", pet.chimeraCopy());
		}
		return out;
	}

	/** The item-wide multiplier and the dungeon stage, applied once to the summed terms. */
	private StatBlock finish(StatBlock sum) {
		StatBlock scaled = sum.times(selfMultiplier);
		return dungeonItem ? scaled.scaled(Scale.SB_CATA_MULT, Scale.SB_STAR_MULT) : scaled;
	}

	public static Builder of(String displayName, ItemCategory category) {
		return new Builder(displayName, category);
	}

	public static final class Builder {
		private final String displayName;
		private final ItemCategory category;
		private String loreId = "";
		private Rarity baseRarity = Rarity.COMMON;
		private boolean recombobulated = true; // everything in this plugin is recombed (§1.0.9)
		private boolean dungeonItem = true;
		private final List<StatTerm> terms = new ArrayList<>();
		private final Set<Upgrade> upgrades = EnumSet.noneOf(Upgrade.class);
		private ReforgeId reforge = ReforgeId.NONE;
		private final List<Gemstones.Slot> gemSlots = new ArrayList<>();
		private boolean chimera = false;
		private double selfMultiplier = 1.0;
		private boolean shortbow = false;
		private ItemDef.Ability ability = null;

		private Builder(String displayName, ItemCategory category) {
			this.displayName = displayName;
			this.category = category;
		}

		public Builder loreId(String id) {
			this.loreId = id;
			return this;
		}

		/** The item's BASE rarity.  Recombobulating is on by default, so the effective rarity is one tier up. */
		public Builder rarity(Rarity base) {
			this.baseRarity = base;
			return this;
		}

		public Builder notRecombobulated() {
			this.recombobulated = false;
			return this;
		}

		/** Mark this item as non-dungeon, so neither the x6.66 nor the x1.81 applies to it (§1.0.3). */
		public Builder notDungeon() {
			this.dungeonItem = false;
			return this;
		}

		public Builder base(Stat stat, double value) {
			terms.add(StatTerm.base(stat, value));
			return this;
		}

		public Builder cataLevel(Stat stat, double value) {
			terms.add(StatTerm.cataLevel(stat, value));
			return this;
		}

		public Builder stars(Stat stat, double value) {
			terms.add(StatTerm.stars(stat, value));
			return this;
		}

		public Builder with(Upgrade... u) {
			java.util.Collections.addAll(upgrades, u);
			return this;
		}

		public Builder reforge(ReforgeId r) {
			this.reforge = r;
			return this;
		}

		/** A Combat slot (accepts any of the three combat gems) holding a Perfect gem of the given type. */
		public Builder combatGem(Gemstones.Type gem) {
			gemSlots.add(Gemstones.Slot.combat(gem, Gemstones.Quality.PERFECT));
			return this;
		}

		/** A slot typed to one gem, e.g. the Hyperion's and Ice Spray Wand's dedicated Sapphire slot (§2.3). */
		public Builder typedGem(Gemstones.Type gem) {
			gemSlots.add(Gemstones.Slot.typed(gem, Gemstones.Quality.PERFECT));
			return this;
		}

		/** This weapon carries Chimera V, so it copies the equipped pet's base stats onto ITSELF (§2). */
		public Builder chimera() {
			this.chimera = true;
			return this;
		}

		/**
		 * A multiplier on this item's OWN stats and nothing else.  Exists for the Necron Head Bonus (§1.10), which
		 * doubles the helmet's stats in M7 and nothing else the player is wearing.
		 */
		public Builder selfMultiplier(double m) {
			this.selfMultiplier = m;
			return this;
		}

		public Builder shortbow() {
			this.shortbow = true;
			return this;
		}

		public Builder ability(double baseDamage, double intelligenceScaling) {
			this.ability = new ItemDef.Ability(baseDamage, intelligenceScaling);
			return this;
		}

		public ItemDef build() {
			return new ItemDef(this);
		}
	}
}
