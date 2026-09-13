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
 * The Infinileap.  It IS an ender pearl, and it must never be thrown: leaping goes through
 * {@code Actions.leap}, so the interact cancel is the first line of defence and
 * {@code listeners/PearlHelper} is the hard backstop.  A right-click opens the Spirit Leap menu, which
 * {@code listeners/SpiritLeapListener} gates on practice mode and on there being someone to leap to.
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
