package abilities;

import damage.DungeonClass;
import items.ItemUtils;
import org.bukkit.entity.Player;
import plugin.Cooldowns;

import java.util.List;

/**
 * The registry of drop-key abilities, and the one place a drop is turned into a cast.
 * <p>
 * This replaced a chain of {@code if(p.getName().equals("Archer") || tags.contains("Archer"))} branches, each
 * with its own inline cooldown map, cooldown check and cooldown message.  Two things came out of collapsing it:
 * <ul>
 *   <li><b>One class-resolution rule.</b>  The branches used {@code equals} for the Archer and {@code startsWith}
 *       for the Mage, and disagreed with {@code DungeonClass.of} and with the mage-beam gate.  A player TAGGED
 *       Berserk whose name happened to start with {@code Archer} got Archer abilities and Berserk everything
 *       else.  Now there is one answer, and it is the one {@code /class} sets.</li>
 *   <li><b>No class still means no ability.</b>  {@link DungeonClass#of} defaults to MAGE, which would have
 *       handed Guided Sheep to anyone with no class at all, so {@link #hasExplicitClass} gates it.</li>
 * </ul>
 */
public final class ClassAbilities {
	private ClassAbilities() {}

	/** Every drop ability.  Healer and Tank appear nowhere, because they have none. */
	private static final List<ClassAbility> ALL = List.of(
			RapidFire.INSTANCE, ExplosiveShot.INSTANCE, GuidedSheep.INSTANCE, Ragnarok.INSTANCE,
			AxeThrow.INSTANCE);

	/**
	 * The ability a drop press fires for this player, or null if they have none: no class, or a class with
	 * nothing on that side of the key (a Mage sprinting, a Healer or Tank at all).
	 *
	 * @param ultimate true for a plain {@code drop} (not sprinting), which is the ultimate side of the key
	 */
	public static ClassAbility forDrop(Player p, boolean ultimate) {
		if(!hasExplicitClass(p)) return null;
		DungeonClass owner = DungeonClass.of(p);
		for(ClassAbility ability : ALL) {
			if(ability.owner() == owner && ability.ultimate() == ultimate) return ability;
		}
		return null;
	}

	/**
	 * Fire the ability for this drop press, honouring its cooldown and reporting one that has not elapsed.
	 * Does nothing at all when the presser has no ability, which is most of the roster.
	 */
	public static void dispatch(Player p, boolean ultimate) {
		ClassAbility ability = forDrop(p, ultimate);
		if(ability == null) return;
		int remaining = Cooldowns.remaining(p, ability.cooldownKey());
		if(remaining > 0) {
			ItemUtils.sendCooldownMessage(p, remaining);
			return;
		}
		if(!ability.cast(p)) return;
		int cooldown = ability.mageReduced()
				? ItemUtils.effectiveCooldown(p, ability.cooldownTicks())
				: ability.cooldownTicks();
		Cooldowns.start(p, ability.cooldownKey(), cooldown);
	}

	/**
	 * True if this player has actually been given a class, by tag or by fake-player name.  Mirrors
	 * {@link DungeonClass#of}'s two lookups, minus its MAGE fallback - which is the whole reason this exists.
	 */
	private static boolean hasExplicitClass(Player p) {
		if(p == null) return false;
		for(DungeonClass c : DungeonClass.values()) {
			String name = DungeonClass.name(c);
			if(p.getScoreboardTags().contains(name) || p.getName().startsWith(name)) return true;
		}
		return false;
	}
}
