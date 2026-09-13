package abilities;

import damage.DungeonClass;
import damage.GuidedCarriers;
import items.ItemUtils;
import org.bukkit.DyeColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

/**
 * The Mage's regular drop ability: an AI-less, gravity-less sheep flown a block a tick along whatever the caster is
 * looking at, until it hits a mob or something solid, then detonated.
 * <p>
 * The flight is {@link ItemUtils#launchGuided}, shared with the Spirit Sceptre's Guided Bat - the two are the same
 * projectile with a different animal and a different damage rule.  Only the rule is here.  <b>It steers</b>: the
 * heading is re-read every tick, so a Mage can walk a sheep round a corner, or into themselves.
 * <p>
 * The Mage has no ultimate, so a sprinting drop does nothing for them.
 */
public final class GuidedSheep implements ClassAbility {
	public static final GuidedSheep INSTANCE = new GuidedSheep();

	private GuidedSheep() {}

	/**
	 * <b>A FLAT figure, and a very low one.</b>  Guided Sheep damage comes from the Mage's CLASS LEVEL and nothing
	 * else - no Intelligence, no Ability Damage, no gear, none of the additive or multiplicative package - which is
	 * exactly why the ability is weak at endgame while a Wither Impact off the same Mage casts from 66,500 and
	 * lands in the hundreds of millions.  410,025 is what the owner's own sheep hits for at Mage 50; the wiki
	 * publishes only a per-level increment ("+100-2000 Guided Sheep Damage" per level, "+500-10000" on its
	 * Dungeons page), and the two pages disagree, so the measured figure is the source here.
	 * <p>
	 * Dealt through {@code dealDerived}, because a flat figure is <b>already a finished hit</b>: putting it back
	 * through the target half would charge for the Rulers, Smite and the class multiplier it never had, and letting
	 * it feed the rolling damage history would seed what Explosive Shot and Rapid Fire read.
	 */
	private static final double DAMAGE = 410_025;

	/**
	 * Blast radius, in blocks.  <b>Undocumented</b> - no wiki page or guide states one - so it is matched to the
	 * Spirit Sceptre's Guided Bat, the same mechanic on an item.  The block-breaking half is separate and unchanged:
	 * that is the Superboom radius, inside {@code ItemUtils.triggerSuperboomRadius}.
	 */
	private static final double BLAST_RADIUS = 6;

	@Override
	public DungeonClass owner() {
		return DungeonClass.MAGE;
	}

	@Override
	public boolean ultimate() {
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 600; // 30s
	}

	@Override
	public boolean mageReduced() {
		return true;
	}

	@Override
	public boolean cast(Player p) {
		guidedSheep(p);
		return true;
	}

	public static void guidedSheep(Player p) {
		Sheep sheep = (Sheep) p.getWorld().spawnEntity(p.getEyeLocation().add(0, -0.65, 0), EntityType.SHEEP);
		sheep.setColor(DyeColor.WHITE);
		GuidedCarriers.stampFlat(sheep, "Guided Sheep", DAMAGE);
		ItemUtils.launchGuided(p, sheep, BLAST_RADIUS);
	}
}
