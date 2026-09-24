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
 * DRAWN bow: damage scales by charge and a partial draw loses the crit term (§1.4), both in {@code Damage}'s bow
 * path. Shot and Duplex arrow build the Last Breath stack; the Archer's two never do.
 */
public final class LastBreath implements Bow, AbilityItem {
	public static final LastBreath INSTANCE = new LastBreath();

	private LastBreath() {}

	@Override
	public String loreId() {
		return "skyblock/combat/last_breath";
	}

	@Override
	public Material material() {
		return Material.BOW;
	}

	@Override
	public String baseName() {
		return "Last Breath";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.PRECISE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "STARRED_LAST_BREATH");
	}

	@Override
	public boolean holdToDraw() {
		return true;
	}

	@Override
	public List<String> arrowTags() {
		return List.of("TerminatorArrow", "LastBreathArrow");
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
