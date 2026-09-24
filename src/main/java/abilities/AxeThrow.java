package abilities;

import damage.DungeonClass;
import items.ItemUtils;
import org.bukkit.entity.Player;

/**
 * Berserk regular drop ability: throws an axe for the player's highest hit in the last 60s.
 * <p>
 * Axe of the Shredded projectile but no pierce, one target. The figure is an already-finished hit, so it's passed
 * as DERIVED: rerunning the target half would apply Rulers, repeated-hit stack and class multiplier twice, and
 * recording it would let each throw read the last one's inflated output.
 */
public final class AxeThrow implements ClassAbility {
	public static final AxeThrow INSTANCE = new AxeThrow();

	private AxeThrow() {}

	@Override
	public DungeonClass owner() {
		return DungeonClass.BERSERK;
	}

	@Override
	public boolean ultimate() {
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 100; // 5s
	}

	@Override
	public boolean cast(Player p) {
		ItemUtils.throwAxe(p, "Throwing Axe", damage.CombatState.maxInLastTicks(p, 1200), false, true);
		return true;
	}
}
