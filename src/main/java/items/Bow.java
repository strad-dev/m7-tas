package items;

import damage.Arrows;
import damage.DungeonClass;
import damage.ItemCategory;
import instructions.Actions;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import plugin.Utils;

import java.util.List;

/**
 * A bow.  Takes the RANGED reforge table, runs Duplex rather than Chimera (§7), and misses most of the sword
 * damage enchantments.
 *
 * <h2>The passives are CONSTANTS, not per-item knobs</h2>
 * Duplex and the Archer's bonus arrows are identical on every bow in the plugin.  They used to be three
 * copy-pasted blocks - one at the tail of {@code terminator()}, one in each branch of {@code onEntityShootBow} -
 * and the copies had already drifted: the Terminator tested the Archer with {@code getName().startsWith} and the
 * other two with {@code contains}.  They are interface constants here, so they are implicitly
 * {@code public static final} and cannot be overridden or retuned per bow, and {@link #onShoot} is the single
 * implementation of the shot itself.
 * <p>
 * Note {@link #ARCHER_BONUS_DELAYS} is a {@code List.of} rather than an array: an interface field is implicitly
 * final, but an ARRAY constant's elements are still writable, which would leave exactly the per-item
 * editability this is meant to rule out.
 */
public interface Bow extends Item {

	/**
	 * Duplex V: ONE extra arrow per shot, at x0.2 of the main arrow's damage and {@link #DUPLEX_DELAY} ticks
	 * behind it.  Its own damage instance, not a multiplier on the shot.  Every bow runs Duplex, which is also
	 * why no bow ever carries Chimera (§7).
	 */
	double DUPLEX_SHARE = 0.2;
	int DUPLEX_DELAY = 3;

	/**
	 * The Archer class passive: two extra arrows per shot at FULL damage.  Unlike the shot itself and its Duplex
	 * arrow, these never build a Last Breath stack - the one case
	 * {@link Arrows#stamp(AbstractArrow, Player, ItemStack, double, double, boolean)}'s flag exists for.
	 */
	double ARCHER_BONUS_SHARE = 1.0;
	List<Integer> ARCHER_BONUS_DELAYS = List.of(5, 10);

	/** Speed a bonus arrow leaves a SHORTBOW at.  A drawn bow reuses its own primary's speed instead. */
	float SHORTBOW_SPEED = 3.175f;

	default ItemCategory category() {
		return ItemCategory.RANGED;
	}

	/** True for a bow that is never drawn (§1.2): every shot is full damage and crits, ignoring draw scaling. */
	default boolean shortbow() {
		return false;
	}

	/** True for a bow whose right-click starts a vanilla draw rather than firing immediately. */
	default boolean holdToDraw() {
		return false;
	}

	/** Scoreboard tags stamped on every arrow this bow fires, its primary and its bonus arrows alike. */
	List<String> arrowTags();

	/**
	 * Whether the shot and its Duplex arrow may build a Last Breath stack.  True on every bow, matching the
	 * five-argument {@code Arrows.stamp} all three used to call: the flag only ever takes the answer away, and the
	 * weapon still has to BE a Last Breath for a stack to land, so it is harmless on the other two.
	 */
	default boolean duplexBuildsLastBreathStacks() {
		return true;
	}

	/**
	 * <b>The one bow shot.</b>  Re-aims the vanilla primary IN PLACE - no cancel, no second entity, just its
	 * velocity overridden with the clean eye direction, which strips the random spread (inaccuracy 1.0) - then
	 * tags it, stamps its damage and fires the passives.
	 * <p>
	 * {@code aimFrom} and {@code speed} are captured ONCE so every staggered bonus arrow leaves from the same
	 * point in the same direction, however the shooter has turned since.
	 *
	 * @param charge vanilla's draw fraction, {@code min(useTicks/20, 1)}.  A drawn bow scales its damage by this
	 *               and loses the whole crit term below a full draw (§1.4), which makes a partial draw much worse
	 *               than the fraction alone suggests.
	 */
	default void onShoot(Player p, ItemStack bow, Arrow primary, double charge) {
		Location aimFrom = p.getEyeLocation().clone();
		float speed = (float) primary.getVelocity().length();
		primary.setVelocity(aimFrom.getDirection().multiply(speed));
		for(String tag : arrowTags()) primary.addScoreboardTag(tag);
		primary.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		Arrows.stamp(primary, p, bow, charge, 1.0);
		fireBonusArrows(p, aimFrom, bow, speed, charge);
	}

	/**
	 * Spawn one bonus arrow, positioned, aimed and given this bow's projectile properties.  Defaults to the
	 * deterministic spawner, which is what both DRAWN bows use so their bonus arrows carry no spread either; the
	 * Terminator overrides it, since it is a shortbow with its own muzzle and pierce level.
	 */
	default Arrow spawnBonusArrow(Player p, Location aimFrom, ItemStack bow, float speed) {
		return Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0);
	}

	/**
	 * The shoot sound for one bonus arrow.  A hook because the drawn bows pitch the Archer's two up to 1.2 and
	 * play at the shooter's location, where the Terminator plays every one locally at 1.0.
	 */
	default void bonusArrowSound(Player p, boolean archerBonus) {
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, archerBonus ? 1.2F : 1.0F);
	}

	/** Whether a bonus arrow that spawns inside a solid block is removed a tick later (the Terminator's rule). */
	default boolean removesArrowInSolid() {
		return false;
	}

	/** Fire this bow's Duplex arrow and, for an Archer, its two bonus arrows. */
	default void fireBonusArrows(Player p, Location aimFrom, ItemStack bow, float speed, double charge) {
		Utils.scheduleTask(() -> fireBonusArrow(p, aimFrom, bow, speed, charge, DUPLEX_SHARE,
				duplexBuildsLastBreathStacks(), false), DUPLEX_DELAY);
		if(DungeonClass.of(p) != DungeonClass.ARCHER) return;
		for(int delay : ARCHER_BONUS_DELAYS) {
			Utils.scheduleTask(() -> fireBonusArrow(p, aimFrom, bow, speed, charge, ARCHER_BONUS_SHARE,
					false, true), delay);
		}
	}

	/** One bonus arrow: spawn it, tag it, stamp its damage (§1.0.5) and sound it. */
	private void fireBonusArrow(Player p, Location aimFrom, ItemStack bow, float speed, double charge,
			double share, boolean buildsLastBreath, boolean archerBonus) {
		Arrow arrow = spawnBonusArrow(p, aimFrom, bow, speed);
		for(String tag : arrowTags()) arrow.addScoreboardTag(tag);
		Arrows.stamp(arrow, p, bow, charge, share, buildsLastBreath);
		bonusArrowSound(p, archerBonus);
		if(removesArrowInSolid()) {
			Utils.scheduleTask(() -> {
				if(arrow.isValid() && arrow.getLocation().getBlock().getType().isSolid()) arrow.remove();
			}, 1);
		}
	}
}
