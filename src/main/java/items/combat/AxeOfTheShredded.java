package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Cast;
import items.ItemFactory;
import items.ItemUtils;
import items.Weapon;
import net.minecraft.server.MinecraftServer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import plugin.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Axe of the Shredded.  Throws a piercing axe for 10% of the wielder's melee damage, and CONSECUTIVE throws
 * double that up to a x16 cap (§1.9).  The projectile itself is {@code ItemUtils.throwAxe}, shared with a
 * Berserk's {@code drop stack} - which copies it but does not pierce, and passes an already-FINISHED figure.
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

	// Axe of the Shredded: the throw deals 10% of melee, and CONSECUTIVE throws double it to a x16 cap (§1.9).
	// A throw counts as consecutive if it lands inside this window of the previous one.
	private static final double AOTS_THROW_SHARE = 0.10;
	private static final double AOTS_THROW_CAP = 16.0;
	private static final int AOTS_STREAK_TICKS = 100;
	private static final Map<UUID, Integer> aotsStreak = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> aotsStreakExpiry = new ConcurrentHashMap<>();

	/**
	 * The Axe of the Shredded's throw: <b>10% of the wielder's melee damage</b>, with consecutive throws doubling
	 * it (and their mana cost) up to a x16 cap (MAP.md §1.9).  It also has to <b>take aggro when it hits a
	 * wither</b>, which is a deliberate requirement rather than incidental, and is what the axe's flight already
	 * did before it dealt any damage at all.
	 */
	public static void aots(Player p) {
		UUID id = p.getUniqueId();
		int now = MinecraftServer.currentTick;
		// A "consecutive" throw is one inside the streak window; letting it lapse resets the doubling to x1.
		int streak = now <= aotsStreakExpiry.getOrDefault(id, 0) ? aotsStreak.getOrDefault(id, 0) + 1 : 0;
		aotsStreak.put(id, streak);
		aotsStreakExpiry.put(id, now + AOTS_STREAK_TICKS);
		double core = damage.Damage.meleeCore(p) * AOTS_THROW_SHARE
				* Math.min(Math.pow(2, streak), AOTS_THROW_CAP);
		ItemUtils.throwAxe(p, core, true, false);
	}

	/** Forget every throw streak.  Part of the run reset, so a new run never inherits a x16 multiplier. */
	public static void reset() {
		aotsStreak.clear();
		aotsStreakExpiry.clear();
	}

}
