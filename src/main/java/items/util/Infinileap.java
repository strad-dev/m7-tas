package items.util;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.MenuItem;
import listeners.SpiritLeapMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * An ender pearl that must never be thrown (leaps go through {@code Actions.leap}): interact cancel first,
 * {@code listeners/PearlHelper} as backstop. Right-click opens Spirit Leap, gated by
 * {@code listeners/SpiritLeapListener} on practice mode and someone to leap to.
 */
public final class Infinileap implements MenuItem {
	public static final Infinileap INSTANCE = new Infinileap();

	private Infinileap() {}

	@Override
	public String loreId() {
		return "skyblock/utility/infinileap";
	}

	@Override
	public Material material() {
		return Material.ENDER_PEARL;
	}

	@Override
	public String baseName() {
		return "Infinileap";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.EPIC;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.NONE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "INFINITE_SPIRIT_LEAP");
	}

	@Override
	public void open(Player p) {
		SpiritLeapMenu.open(p);
	}
}
