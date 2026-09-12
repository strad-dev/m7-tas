package items.combat;

import damage.GuidedCarriers;
import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Cast;
import items.ItemFactory;
import items.ItemUtils;
import items.Weapon;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

/**
 * The Spirit Sceptre, in its starred (Thorn Fragment) build: 190 Damage, 330 Intelligence, and a Guided Bat that
 * casts from 2,250 at 0.2 Intelligence scaling.
 * <p>
 * <b>The bat's HEADING is live; its DAMAGE is not.</b>  It follows your aim, so turning your head steers it for the
 * whole ten seconds it may be airborne - but {@code Damage.abilityCore} settles the base, Intelligence and Ability
 * Damage the moment it leaves and {@code damage.GuidedCarriers} stamps that onto the entity, so only the
 * target-dependent half (the Rulers, Smite, the target's own debuffs) waits for the impact.  Swapping weapons or
 * losing a buff mid-flight therefore cannot change what it hits for, which is the same rule arrows follow (§1.0.5).
 * <p>
 * The flight itself is {@link ItemUtils#launchGuided}, shared with the Mage's Guided Sheep.
 */
public final class SpiritSceptre implements Weapon, AbilityItem {
	public static final SpiritSceptre INSTANCE = new SpiritSceptre();

	private SpiritSceptre() {}

	/** Blast radius, in blocks - the wiki's figure for the Guided Bat. */
	private static final double BLAST_RADIUS = 6;

	@Override
	public String loreId() {
		return "skyblock/combat/spirit_sceptre";
	}

	/**
	 * An ALLIUM, which is a placeable flower - so the right-click that fires the ability must also swallow the
	 * placement.  {@link #cancelsInteract()} is true by default and does exactly that, which is why there is no
	 * override here: a wand is not one of the two items whose right-click has to reach the world.
	 */
	@Override
	public Material material() {
		return Material.ALLIUM;
	}

	@Override
	public String baseName() {
		return "Spirit Sceptre";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.HEROIC;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "STARRED_BAT_WAND");
	}

	@Override
	public boolean mageBeams() {
		return true;
	}

	@Override
	public boolean hasRightClick() {
		return true;
	}

	@Override
	public boolean onRightClick(Cast cast) {
		guidedBat(cast.player(), cast.stack());
		return true;
	}

	/**
	 * Spawn the bat, stamp the cast onto it, launch it - <b>and the click is over</b>.  Nothing about this ability
	 * outlives the call: the blast a second or two later reads the figure back off the entity.
	 */
	public static void guidedBat(Player p, ItemStack wand) {
		Bat bat = (Bat) p.getWorld().spawnEntity(p.getEyeLocation().add(0, -0.65, 0), EntityType.BAT);
		// Awake, or the client draws it hanging upside down from a ceiling it is nowhere near.
		bat.setAwake(true);
		// The chat line names the ITEM, not the ability: "Your Spirit Sceptre hit 1 enemy for 66,342.2 damage."
		GuidedCarriers.stamp(bat, "Spirit Sceptre", wand, damage.Damage.abilityCore(p, wand));
		ItemUtils.launchGuided(p, bat, BLAST_RADIUS);
	}
}
