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
 * Ability is a Y-velocity zero (movement tech), not the real Iron Punch; stats are the real item's (§1.9).
 * Beams for a Mage, as it always did: it's an {@code IRON_SWORD} and the old gate tested material.
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
	 * Zeroes Y velocity, leaves X/Z. The client's {@code lerpMotion} SETS delta movement, so a fall or leap stalls
	 * on the spot. 3s cooldown, Mage-reduced ({@link #effectiveCooldown}).
	 */
	public static void golemSword(Player p) {
		Vector v = p.getVelocity();
		v.setY(0);
		p.setVelocity(v);
	}
}
