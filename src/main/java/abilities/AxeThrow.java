package abilities;

import abilities.ClassAbility;
import damage.DungeonClass;
import items.ItemUtils;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import plugin.*;

import java.util.*;

/**
 * The Berserk's regular drop ability: throws an axe for the player's highest hit in the last 60 seconds.
 * <p>
 * It copies the Axe of the Shredded's projectile but does NOT pierce, so one target only.  The figure it throws
 * for is an already-FINISHED hit, which is why it is passed as DERIVED: running the target half on it again
 * would charge for the Rulers, the repeated-hit stack and the class multiplier a second time, and recording the
 * result would let each throw read the last one's inflated output.
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
