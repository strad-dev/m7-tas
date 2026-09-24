package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.*;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Piercing throw for 10% of melee; CONSECUTIVE throws double it to a x16 cap (§1.9). Projectile is
 * {@code ItemUtils.throwAxe}, shared with Berserk {@code drop stack} (no pierce, passes a FINISHED figure).
 */
public final class AxeOfTheShredded implements Weapon, AbilityItem {
	public static final AxeOfTheShredded INSTANCE = new AxeOfTheShredded();

	private AxeOfTheShredded() {}

	@Override
	public String loreId() {
		return "skyblock/combat/aots";
	}

	@Override
	public Material material() {
		return Material.DIAMOND_AXE;
	}

	@Override
	public String baseName() {
		return "Axe of the Shredded";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.SUSPICIOUS;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "AXE_OF_THE_SHREDDED");
	}

	@Override
	public boolean hasRightClick() {
		return true;
	}

	@Override
	public boolean onRightClick(Cast cast) {
		aots(cast.player());
		return true;
	}

	// A throw within AOTS_STREAK_TICKS of the last one is consecutive.
	private static final double AOTS_THROW_SHARE = 0.10;
	private static final double AOTS_THROW_CAP = 16.0;
	private static final int AOTS_STREAK_TICKS = 100;
	private static final Map<UUID, Integer> aotsStreak = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> aotsStreakExpiry = new ConcurrentHashMap<>();

	/**
	 * Consecutive throws double damage (and mana cost) to x16 (MAP.md §1.9). Must <b>take aggro on a wither hit</b>,
	 * a deliberate requirement.
	 */
	public static void aots(Player p) {
		UUID id = p.getUniqueId();
		int now = MinecraftServer.currentTick;
		// Lapsing the window resets to x1.
		int streak = now <= aotsStreakExpiry.getOrDefault(id, 0) ? aotsStreak.getOrDefault(id, 0) + 1 : 0;
		aotsStreak.put(id, streak);
		aotsStreakExpiry.put(id, now + AOTS_STREAK_TICKS);
		double core = damage.Damage.meleeCore(p) * AOTS_THROW_SHARE
				* Math.min(Math.pow(2, streak), AOTS_THROW_CAP);
		ItemUtils.throwAxe(p, "Throwing Axe", core, true, false);
	}

	/** Run reset, so a new run never inherits x16. */
	public static void reset() {
		aotsStreak.clear();
		aotsStreakExpiry.clear();
	}

}
