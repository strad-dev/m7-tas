package items.bows;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Bow;
import items.Cast;
import items.ItemFactory;
import net.minecraft.world.InteractionHand;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The Death Bow.  A DRAWN bow, so its damage scales by the vanilla charge fraction and a partial draw loses the
 * crit term entirely (§1.4) - both of which live in {@code Damage}'s bow path, not here.
 * <p>
 * Its one ability is a <b>x2 against Undead-type mobs</b> ("deals +100% damage to Undead mobs"), and it lives in
 * {@code Damage.multiplicative} keyed on this item's lore ID, alongside the Hyperion's x1.5 against Withers.  That
 * is deliberate rather than a flag here: it has to follow the WEAPON, so the bow's Duplex arrow and an Archer's two
 * bonus arrows - which stamp the same weapon and land ticks later - get it too.
 * <p>
 * Its second real ability, the 50% chance for an arrow to bounce to another target, is <b>not modelled</b>.
 */
public final class DeathBow implements Bow, AbilityItem {
	public static final DeathBow INSTANCE = new DeathBow();

	private DeathBow() {}

	@Override
	public String loreId() {
		return "skyblock/combat/death_bow";
	}

	@Override
	public Material material() {
		return Material.BOW;
	}

	@Override
	public String baseName() {
		return "Death Bow";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.EPIC;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.PRECISE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "DEATH_BOW");
	}

	@Override
	public boolean holdToDraw() {
		return true;
	}

	/**
	 * {@code TerminatorArrow} is misnamed history: it means "an arrow from one of our ordinary bows", and it is what
	 * removes the arrow on a block hit, phases it through a Gyrokinetic Wand's falling blocks, blocks pickup and
	 * honours a boss made vulnerable on the hit's own tick.  A plain bow wants all four, so it carries the tag for
	 * the same reason the Last Breath does.
	 */
	@Override
	public List<String> arrowTags() {
		return List.of("TerminatorArrow");
	}

	@Override
	public boolean allowsEntityInteract() {
		return true;
	}

	@Override
	public boolean hasRightClick() {
		return true;
	}

	/** Start the vanilla draw.  The shot itself arrives as {@code EntityShootBowEvent} and runs {@code onShoot}. */
	@Override
	public boolean onRightClick(Cast cast) {
		((CraftPlayer) cast.player()).getHandle().startUsingItem(InteractionHand.MAIN_HAND);
		return true;
	}
}
