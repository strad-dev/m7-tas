package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Weapon;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import plugin.*;

import java.util.*;

/**
 * The Dark Claymore.  A pure mage weapon: no ability of its own at all, so it implements {@link Weapon} and
 * nothing else - the whole of its behaviour is that a Mage's left click beams with it.
 */
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
