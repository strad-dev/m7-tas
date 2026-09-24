package damage;

/**
 * Books and enchants whose value is the SAME everywhere; items record only WHICH they carry (MAP.md §2.4).
 * <p>
 * Chimera isn't here: it copies the equipped pet, so it's an {@link ItemDef} flag resolved through {@link Pet}.
 */
public enum Upgrade {
	/** 15 Hot Potato Books on a weapon, +2 Damage and +2 Strength each. */
	POTATO_BOOKS(StatBlock.of(Stat.DAMAGE, 30, Stat.STRENGTH, 30)),
	ART_OF_WAR(StatBlock.of(Stat.STRENGTH, 5)),
	/** Critical VII, the enchant. */
	CRITICAL(StatBlock.of(Stat.CRIT_DAMAGE, 100)),
	/** Overload V's stat half. Its x1.5 bow damage is a §7 multiplicative source. */
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
