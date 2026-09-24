package items.combat;

import damage.GuidedCarriers;
import damage.Rarity;
import damage.ReforgeId;
import items.*;
import org.bukkit.Material;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Starred (Thorn Fragment) build: 190 Damage, 330 Intelligence, Guided Bat casting from 2,250 at 0.2 Int scaling.
 * <p>
 * HEADING is live (steerable for up to 10s), DAMAGE is not: {@code Damage.abilityCore} settles base, Int and Ability
 * Damage at launch and {@code damage.GuidedCarriers} stamps it on; only the target half (Rulers, Smite, debuffs)
 * waits for impact. So a mid-flight swap can't change it, like arrows (§1.0.5). Flight is
 * {@link ItemUtils#launchGuided}, shared with the Mage's sheep.
 */
public final class SpiritSceptre implements Weapon, AbilityItem {
	public static final SpiritSceptre INSTANCE = new SpiritSceptre();

	private SpiritSceptre() {}

	/** Blocks, the wiki's figure. */
	private static final double BLAST_RADIUS = 6;

	@Override
	public String loreId() {
		return "skyblock/combat/spirit_sceptre";
	}

	/** A placeable flower; the default {@link #cancelsInteract()} swallows the placement. */
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

	/** Spawn, stamp, launch, and the click is over. The blast reads the figure back off the entity. */
	public static void guidedBat(Player p, ItemStack wand) {
		Bat bat = (Bat) p.getWorld().spawnEntity(p.getEyeLocation().add(0, -0.65, 0), EntityType.BAT);
		// Awake, or the client draws it hanging upside down.
		bat.setAwake(true);
		// Chat names the ITEM, not the ability: "Your Spirit Sceptre hit 1 enemy for 66,342.2 damage."
		GuidedCarriers.stamp(bat, "Spirit Sceptre", wand, damage.Damage.abilityCore(p, wand));
		ItemUtils.launchGuided(p, bat, BLAST_RADIUS);
	}
}
