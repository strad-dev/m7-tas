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
	 * <p>
	 * <b>6.65, not 6.66</b> - the General's Medallion was nerfed and took 0.01x off the maxed buff on every stat,
	 * which moves this and {@link #SB_STAR_MULT} together.  Both are measured rather than derived, so if the
	 * medallion moves again they move again.
	 * <p>
	 * It applies to an ABILITY'S BASE DAMAGE as well, not only to stats (§7) - the wiki's damage-calculation page
	 * lists the base as "increased by Catacombs Stat Bonus" like anything else on the item.  That is what pins the
	 * value: Wither Impact's tooltip is {@code base x (1 + Int/100 x 0.3) x (1 + AbilityDamage/100)}, so dividing a
	 * real tooltip by the two factors recovers the base exactly.  Four readings, eight significant figures, 10,000
	 * outside dungeons and 66,500 = 10,000 x 6.65 inside:
	 * <pre>
	 * Int  6,175.88  AbilityDamage 144.5  ->    477,450.6  = 10,000 x  47.745
	 * Int  3,390.55  AbilityDamage  90    ->    212,261.4  = 10,000 x  21.226
	 * Int 17,695.39  AbilityDamage 134    ->  8,416,350.1  = 66,500 x 126.562
	 * Int 23,403.71  AbilityDamage 220    -> 15,153,727.7  = 66,500 x 227.876
	 * </pre>
	 * The base used to skip the stage entirely, a flat 6.65x understatement of <b>every</b> ability in the plugin -
	 * which is what made Wither Impact read ~65M on a 300M Wither Miner where a real M7 Mage does hundreds of
	 * millions.  {@link Damage#abilityBase} is the one place it is applied.
	 */
	public static final double SB_CATA_MULT = 6.65;

	/**
	 * Stars-only scaling for every non-core stat on a dungeon item (§1.0.2), e.g. Ability Damage.  <b>1.80, not
	 * 1.81</b>, for the same General's Medallion nerf as {@link #SB_CATA_MULT}.
	 */
	public static final double SB_STAR_MULT = 1.80;

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
