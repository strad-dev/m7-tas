package damage;

/**
 * Kind of damage instance (MAP.md §7a). Plumbed through, not inferred: §7 makes every proc its own instance, so
 * without it the renderer can't tell a Thunderlord proc from the hit behind it. Also overrides the digit cadence.
 */
public enum DamageKind {
	/** Every melee, beam and bow hit crits (§7), so this is effectively the crit form. */
	NORMAL(null, true, true, true),
	FIRE("<gold>", false, false, false),
	VENOMOUS("<dark_green>", false, false, false),
	THUNDERLORD("<blue>", false, false, false),
	/** Magic and any non-crit: abilities, partially drawn bow. Grey, no crit decoration. */
	MAGIC("<gray>", false, true, true),
	/** Separate instance through the same boundary as the main hit, rendered like one. */
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

	/** MiniMessage colour forced on every digit, or null for the crit cadence. */
	public String colour() {
		return colour;
	}

	/** Crit form {@code ✧123✧❤}. Grey magic numbers are bare digits, as on Hypixel. */
	public boolean crit() {
		return crit;
	}

	/**
	 * Only DIRECT hits ring the hurt noise (swing, beam, arrow, Cleave, ability). Fire Aspect and Venomous each fire
	 * FIVE instances per swing and Thunderlord lands the same tick, so one melee hit was playing it six times. The
	 * numbers still show.
	 */
	public boolean playsHurtSound() {
		return hurtSound;
	}

	/**
	 * Only a DIRECT hit may pull boss aggro. Procs are consequences of a swing and Cleave hits a mob you never aimed
	 * at; otherwise five Venomous ticks would keep winning the tie against someone actually attacking, and a Cleave
	 * clip on a boss at the sweep's edge would steal it.
	 * <p>
	 * Differs from {@link #playsHurtSound} on one kind: Cleave sounds but doesn't aggro. Deliberately two flags.
	 * <p>
	 * Lives on the KIND, not just the call site, because {@code Damage.dealSecondary}'s flag is per-overload and
	 * nothing stopped the next caller picking wrong. Both gates must agree.
	 * <p>
	 * Even when true, a hit only aggros if it took health off - see {@code Damage.deal}.
	 */
	public boolean pullsAggro() {
		return aggro;
	}
}
