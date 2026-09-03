package items.bows;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Bow;
import items.Cast;
import items.ItemFactory;
import net.minecraft.world.InteractionHand;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import plugin.*;

import java.util.*;

/**
 * The Last Breath.  A DRAWN bow, so its damage scales by the vanilla charge fraction and a partial draw loses
 * the crit term entirely (§1.4) - both of which live in {@code Damage}'s bow path, not here.  Its shot and its
 * Duplex arrow build the Last Breath stack; the Archer's two bonus arrows never do.
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

	/** Start the vanilla draw.  The shot itself arrives as {@code EntityShootBowEvent} and runs {@code onShoot}. */
	@Override
	public boolean onRightClick(Cast cast) {
		((CraftPlayer) cast.player()).getHandle().startUsingItem(InteractionHand.MAIN_HAND);
		return true;
	}

}
