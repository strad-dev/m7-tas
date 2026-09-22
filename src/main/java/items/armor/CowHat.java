package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import plugin.Alpha;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The Renowned Cow Hat.  Uncommon once recombobulated, so it is the one green item in the game.  Worth 550
 * movement speed (600 under the alpha timings, {@link Alpha}), and wearing it cancels the Wither-King relic
 * carry debuff.
 */
public final class CowHat implements Wearable {
	public static final CowHat INSTANCE = new CowHat();

	private CowHat() {}

	@Override
	public Material material() {
		return Material.PLAYER_HEAD;
	}

	@Override
	public String baseName() {
		return "Cow Hat";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.COMMON;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.RENOWNED;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.head(colouredName(reforge), "cowHat",
				"eyJ0aW1lc3RhbXAiOjE0OTg2ODk0MDczOTQsInByb2ZpbGVJZCI6ImYxNTliMjc0YzIyZTQzNDBiN2MxNTJhYmRlMTQ3NzEzIiwicHJvZmlsZU5hbWUiOiJNSEZfQ293Iiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81ZDZjNmVkYTk0MmY3ZjVmNzFjMzE2MWM3MzA2ZjRhZWQzMDdkODI4OTVmOWQyYjA3YWI0NTI1NzE4ZWRjNSJ9fX0=",
				"OKbGpPO8wQDLCu/zeOrLBt9m6V29AZMNYR4gfumog1q5UvI/3if2IKdKi2X0UcJWyvgMlHW0y1OAGFsY5EZyL4Z+/+Ed2G5IFiMAQpq+sjqyiPgwIUixNLYdu7sMjxsuoRlzMe9y73tW2+Waxk4aWYbMCxGGQmgHv++L1gw+qiAzzFDs09l0GexVnLW8XO/baLAWD6DG7dpOieFVCAaap8cvw4ZVTF7jNBfr3dd3BkhkMbGrmS39WV5s1XsxkbomFRdHPCIh8ba/DEe5qYGhLNQe0mSHZS2k9Y15kMWiLEU+tTDa7UpD8ZYszvPbgbIEbVeKOIF0lGm7m/RIHHSTjSVzemTrYbXTkf54dcAdwsutd8mYm3yZDj8Iw/5y2X2qOalGVyDupFJcPJHabvct9uup7GaLzMBs1o3p4RjomtAdjQLlEX8ktY0YLExyKAhx8bfaI+67i1a7r2eh8eAMMY0A7WBnEJv9iO4vUtsOB0fKIiqbOIXGkUbaJk6Poj37CbNYQC83bSIzEcfXm4inytLwrTg3B6k1zrIL45SQ4XovItWfcWD+g1WzIEz7+LXBLaB8SRZT9EU7jA18GpazvhJBNlP4srIuE9AuVrNSNHg/LtA4FqFX06YhEH1/oxWOOO5uqS6ggUb0xiO6u6BbTLU/FQPh3GyytS76d2Dg0rk=");
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.HEAD;
	}

	/** Movement speed while worn.  The bare-headed default it is measured against is in {@code HelmetSpeedSync}. */
	private static final int SPEED = 550, ALPHA_SPEED = 600;

	@Override
	public int impliedSpeed() {
		return Alpha.enabled() ? ALPHA_SPEED : SPEED;
	}

	@Override
	public boolean exemptsRelicDebuff() {
		return true;
	}
}
