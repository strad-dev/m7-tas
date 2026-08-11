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
	NORMAL(null, true),
	FIRE("<gold>", false),
	VENOMOUS("<dark_green>", false),
	THUNDERLORD("<blue>", false),
	/** Magic and any non-critical damage: abilities, and a partially drawn bow.  Grey, and no crit decoration. */
	MAGIC("<gray>", false),
	/**
	 * A Cleave hit.  It is a separate instance going through the same boundary as the main hit, and it renders
	 * like one - only the source differs.
	 */
	CLEAVE(null, true);

	private final String colour;
	private final boolean crit;

	DamageKind(String colour, boolean crit) {
		this.colour = colour;
		this.crit = crit;
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
}
