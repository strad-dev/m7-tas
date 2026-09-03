package abilities;

import abilities.ClassAbility;
import damage.DungeonClass;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import plugin.*;

import java.util.*;

/**
 * The Berserk's ULTIMATE: melee hits deal x1.5 for 15 seconds (§1.14).
 * <p>
 * The WINDOW itself lives in {@code damage/CombatState}, because that is what the damage math reads; only the
 * cooldown lives here.  Nothing is applied to the player, so there is no buff to strip on teardown.
 */
public final class Ragnarok implements ClassAbility {
	public static final Ragnarok INSTANCE = new Ragnarok();

	private Ragnarok() {}

	@Override
	public DungeonClass owner() {
		return DungeonClass.BERSERK;
	}

	@Override
	public boolean ultimate() {
		return true;
	}

	@Override
	public int cooldownTicks() {
		return 1200; // 60s
	}

	@Override
	public boolean cast(Player p) {
		damage.CombatState.startBerserkUltimate(p);
		Utils.playLocalSound(p, Sound.ENTITY_WITHER_SPAWN, 1.0F, 2.0F);
		p.sendMessage(Utils.msg("<red>Ragnarok! <gray>Your melee hits deal <red>1.5x<gray> damage for 15 seconds."));
		return true;
	}
}
