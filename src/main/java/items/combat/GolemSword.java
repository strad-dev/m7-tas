package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Cast;
import items.ItemFactory;
import items.Weapon;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * The Golem Sword.  Our ability is the Y-velocity zero, a movement tech, not the real item's Iron Punch - but
 * its stat block still comes from the real item (§1.9).
 * <p>
 * It beams for a Mage, which is not a new decision: the old gate tested the MATERIAL, and this is an
 * {@code IRON_SWORD} like the Hyperion, so a Mage holding it has always beamed rather than swung.
 */
public final class GolemSword implements Weapon, AbilityItem {
	public static final GolemSword INSTANCE = new GolemSword();

	private GolemSword() {}

	@Override
	public String loreId() {
		return "skyblock/combat/golem_sword";
	}

	@Override
	public Material material() {
		return Material.IRON_SWORD;
	}

	@Override
	public String baseName() {
		return "Golem Sword";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.RARE;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.SUSPICIOUS;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "GOLEM_SWORD");
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
	public int cooldownTicks() {
		return 60; // 3s
	}

	@Override
	public boolean onRightClick(Cast cast) {
		golemSword(cast.player());
		return true;
	}

	/**
	 * Golem Sword: kills the holder's vertical momentum.  Y velocity is zeroed, X/Z are left alone.  For a real
	 * player this rides out as a velocity packet, and the client's {@code lerpMotion} SETS its delta movement, so
	 * a fall or leap stalls on the spot instead of the value being added to whatever it was already doing.
	 * 3s cooldown, halved or quartered for a Mage like every other ability (see {@link #effectiveCooldown}).
	 */
	public static void golemSword(Player p) {
		Vector v = p.getVelocity();
		v.setY(0);
		p.setVelocity(v);
	}
}
