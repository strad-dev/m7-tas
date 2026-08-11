package damage;

/**
 * What kind of damage instance this is (DAMAGE_PLAN.md §7a).  It has to be plumbed through the damage call rather
 * than inferred, because §7 deliberately makes every proc its own instance - without the kind, the renderer cannot
 * tell a Thunderlord proc from the hit that triggered it.
 * <p>
 * The kind also overrides the floating number's random digit palette entirely.
 */
public enum DamageKind {
	/** A normal hit.  Every melee, beam and bow hit crits (§7), so this is effectively the crit form. */
	NORMAL(null, true, true, true),
	FIRE("<gold>", false, false, false),
	VENOMOUS("<dark_green>", false, false, false),
	THUNDERLORD("<blue>", false, false, false),
	/** Magic and any non-critical damage: abilities, and a partially drawn bow.  Grey, and no crit decoration. */
	MAGIC("<gray>", false, true, true),
	/**
	 * A Cleave hit.  It is a separate instance going through the same boundary as the main hit, and it renders
	 * like one - only the source differs.
	 */
	CLEAVE(null, true, true, false);

	private final String colour;
	private final boolean crit;
	private final boolean hurtSound;
	private final boolean aggro;

	DamageKind(String colour, boolean crit, boolean hurtSound, boolean aggro) {
		this.colour = colour;
		this.crit = crit;
		this.hurtSound = hurtSound;
		this.aggro = aggro;
	}

	/**
	 * The MiniMessage colour this kind forces on every digit, or null to use the random crit palette (white /
	 * orange / light green / red).
	 */
	public String colour() {
		return colour;
	}

	/**
	 * True if the number is drawn in the crit form, {@code ✧123✧❤}.  Grey magic numbers drop the decoration and
	 * are rendered as bare digits, as they are on Hypixel.
	 */
	public boolean crit() {
		return crit;
	}

	/**
	 * Whether a hit of this kind rings the target's hurt noise.
	 * <p>
	 * Only <b>direct</b> hits do: a melee swing, a beam, an arrow, a Cleave sweep, an ability.  The three proc kinds
	 * do not, and that is the point - Fire Aspect and Venomous each fire FIVE instances off one swing and
	 * Thunderlord lands on the same tick as the hit that spawned it, so a single melee hit was ringing the hurt
	 * sound six times over.  The numbers still show; only the noise is direct-hits-only.
	 */
	public boolean playsHurtSound() {
		return hurtSound;
	}

	/**
	 * Whether a hit of this kind can pull a boss's aggro.
	 * <p>
	 * <b>Only a DIRECT hit counts.</b>  Fire Aspect, Venomous and Thunderlord are the swing's consequences rather
	 * than a swing, and a Cleave sweep is a hit on a mob the player never aimed at - so none of them may decide who
	 * a Wither Lord chases.  Otherwise standing still while five Venomous ticks land would keep re-winning the
	 * aggro tie against someone actually attacking, and one Cleave clip of a boss on the edge of the sweep would
	 * steal it outright.
	 * <p>
	 * Note this differs from {@link #playsHurtSound} on exactly one kind: a Cleave hit <b>does</b> sound (it really
	 * did hit that mob) but does <b>not</b> aggro.  The two questions are close enough to look like one flag, so
	 * they are deliberately two.
	 * <p>
	 * This lives on the KIND rather than only on the call site because the flag {@code Damage.dealSecondary} passes is
	 * per-overload: every current caller happens to pick the right one, and nothing stopped the next one from picking
	 * wrong.  Both gates have to agree.
	 * <p>
	 * Separate question from <b>when</b> a hit aggros. Even a `true` here only counts if the hit actually took health
	 * off the target - see {@code Damage.deal}.
	 */
	public boolean pullsAggro() {
		return aggro;
	}
}
