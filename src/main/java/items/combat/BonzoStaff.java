package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Cast;
import items.ItemFactory;
import items.Weapon;
import net.minecraft.server.MinecraftServer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import plugin.*;

import java.util.*;

/**
 * The Bonzo Staff.  Fires a wind charge whose fire tick is recorded so {@code MiscListener} and {@code Actions}
 * can age it: the charge itself is vanilla, the timing is ours.
 */
public final class BonzoStaff implements Weapon, AbilityItem {
	public static final BonzoStaff INSTANCE = new BonzoStaff();

	private BonzoStaff() {}

	@Override
	public String loreId() {
		return "skyblock/combat/bonzo";
	}

	@Override
	public Material material() {
		return Material.BREEZE_ROD;
	}

	@Override
	public String baseName() {
		return "Bonzo Staff";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.RARE;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.HEROIC;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "STARRED_BONZO_STAFF");
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
		bonzo(cast.player());
		return true;
	}

	public static final Map<Integer, Integer> bonzoFireTick = new HashMap<>();

	public static void bonzo(Player p) {
		Location l = p.getEyeLocation();
		WindCharge windCharge = (WindCharge) l.getWorld().spawnEntity(l, EntityType.WIND_CHARGE);
		windCharge.addScoreboardTag("Bonzo");
		windCharge.setShooter(p);
		bonzoFireTick.put(windCharge.getEntityId(), MinecraftServer.currentTick);
	}
}
