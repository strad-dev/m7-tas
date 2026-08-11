package damage;

/**
 * Global upgrades: books and enchantments whose value is the SAME everywhere they appear, so they are defined once
 * here and an item records only WHICH it carries (DAMAGE_PLAN.md §2.4).  Retuning potato books then moves every
 * item at once, which is the correct behaviour.
 * <p>
 * Chimera is deliberately absent: it is an item source with a player-scoped input (it copies the equipped pet), so
 * it lives on {@link ItemDef} as a flag and resolves through {@link Pet}.
 */
public enum Upgrade {
	/** 15 Hot Potato Books on a weapon: +2 Damage and +2 Strength each. */
	POTATO_BOOKS(StatBlock.of(Stat.DAMAGE, 30, Stat.STRENGTH, 30)),
	ART_OF_WAR(StatBlock.of(Stat.STRENGTH, 5)),
	/** Critical VII, the enchantment - not the Crit Damage stat itself. */
	CRITICAL(StatBlock.of(Stat.CRIT_DAMAGE, 100)),
	/** Overload V's stat half.  Its x1.5 on bow damage is a §7 multiplicative source, not a stat. */
	OVERLOAD(StatBlock.of(Stat.CRIT_DAMAGE, 5)),
	BIG_BRAIN(StatBlock.of(Stat.INTELLIGENCE, 25)),
	SMARTY_PANTS(StatBlock.of(Stat.INTELLIGENCE, 25)),
	REFLECTION(StatBlock.of(Stat.INTELLIGENCE, 10));

	private final StatBlock stats;

	Upgrade(StatBlock stats) {
		this.stats = stats;
	}

	public StatBlock stats() {
		return stats;
	}
}
