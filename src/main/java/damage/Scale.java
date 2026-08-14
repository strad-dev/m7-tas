package damage;

/**
 * The handful of scale constants the whole damage system is built on.  MAP.md §2.4's hard rule is that
 * nothing pre-scaled or pre-summed may appear anywhere else in the code, so these are each used in exactly one
 * place and every "in dungeons" figure in the plan is derived from them at runtime.
 */
public final class Scale {
	private Scale() {}

	/**
	 * SkyBlock HP per point of Minecraft health (§5).  Every HP and every damage number in this package is a real
	 * SkyBlock value; this is the only conversion, applied once at the application boundary in
	 * {@link Damage#apply}.  {@code double} is exact for integers to 2^53, so billions carry no precision risk.
	 */
	public static final double SB_PER_MC_HP = 1_000_000.0;

	/**
	 * Catacombs scaling for the four core stats on a DUNGEON item (§1.0.1).  Folds in stars and cata level
	 * together, which is why the authored terms are the plain unscaled SkyBlock values.
	 */
	public static final double SB_CATA_MULT = 6.66;

	/** Stars-only scaling for every non-core stat on a dungeon item (§1.0.2), e.g. Ability Damage. */
	public static final double SB_STAR_MULT = 1.81;

	/**
	 * The inherent damage resistance every dungeon boss and mini-boss carries (§5): a flat x0.1 on top of defense,
	 * independent of it.  Without this the whole chain comes out an order of magnitude too fast.
	 */
	public static final double BOSS_RESISTANCE = 0.10;

	/**
	 * The player's own {@code +5} Damage (§1.0.4), the {@code 5 +} term of the melee/bow formula.  It belongs to
	 * the player, not to any weapon, so it is never folded into an item's stat block and never dungeon-scaled.
	 */
	public static final double PLAYER_BASE_DAMAGE = 5.0;

	/**
	 * Hypixel's damage reduction curve (§5): a target takes {@code sbDamage / (1 + defense/100)}.  Deliberately
	 * NOT vanilla's {@code min(20, armor)/25}, which is a different function - see §5 for why
	 * {@code minecraft:armor} stays at 0 on every mob rather than standing in for this.
	 */
	public static double defenseDivisor(double defense) {
		return 1.0 + Math.max(0.0, defense) / 100.0;
	}
}
