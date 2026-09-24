package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Renowned Cow Hat. Uncommon recombobulated, the one green item. Also cancels the Wither King relic carry debuff.
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

	/**
	 * No Max Speed of its own; stated explicitly because it used to report 550. That was base 400 + the Black Cat's
	 * 150, which the hat only FORCES in assumed modes ({@code damage/Pet.forPlayer}). In realistic it changes nothing.
	 */
	private static final int MAX_SPEED_BONUS = 0;

	@Override
	public int maxSpeedBonus() {
		return MAX_SPEED_BONUS;
	}

	@Override
	public boolean exemptsRelicDebuff() {
		return true;
	}
}
