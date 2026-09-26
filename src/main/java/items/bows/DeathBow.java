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
 * DRAWN bow: damage scales by charge and a partial draw loses the crit term (§1.4), both in {@code Damage}'s bow path.
 * <p>
 * <b>x2 vs Undead</b> lives in {@code Damage.multiplicative} keyed on the lore ID, beside Hyperion's x1.5 vs Wither,
 * so it follows the WEAPON and the Archer arrows (landing ticks later) get it too.
 * <p>
 * Runs <b>Swarm V</b>, not Duplex ({@code ItemDef.swarm}): no 0.2x arrow, no fire debuff.
 * <p>
 * The 50% arrow bounce to another target is <b>not modelled</b>.
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
	 * {@code TerminatorArrow} is a misnomer for "arrow from one of our ordinary bows": removed on block hit, phases
	 * through Gyrokinetic falling blocks, no pickup, honours a boss made vulnerable on the hit's tick. A plain bow
	 * wants all four, same as the Last Breath.
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

	/** Starts the vanilla draw; the shot arrives as {@code EntityShootBowEvent} and runs {@code onShoot}. */
	@Override
	public boolean onRightClick(Cast cast) {
		((CraftPlayer) cast.player()).getHandle().startUsingItem(InteractionHand.MAIN_HAND);
		return true;
	}
}
