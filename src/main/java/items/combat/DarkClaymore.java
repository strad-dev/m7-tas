package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Weapon;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Pure mage weapon, no ability: a Mage's left click beams with it. */
public final class DarkClaymore implements Weapon {
	public static final DarkClaymore INSTANCE = new DarkClaymore();

	private DarkClaymore() {}

	@Override
	public String loreId() {
		return "skyblock/combat/claymore";
	}

	@Override
	public Material material() {
		return Material.STONE_SWORD;
	}

	@Override
	public String baseName() {
		return "Dark Claymore";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.FABLED;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "DARK_CLAYMORE");
	}

	@Override
	public boolean mageBeams() {
		return true;
	}

}
