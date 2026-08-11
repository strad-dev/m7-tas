package damage;

import net.minecraft.server.MinecraftServer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The player stat aggregate (DAMAGE_PLAN.md §2's {@code aggregate(player, path)}).
 * <pre>
 * aggregate(player, path) = stats(weapon) + stats(helmet) + stats(chest) + stats(legs) + stats(boots)
 *                         + Equipment(class, path)      // ASSUMED, never items   (§1.11)
 *                         + Power(powerId, magicalPower) + Tunings(magicalPower)  (§1.12)
 *                         + Profile(player)                                       (§1.13)
 *                         + ClassBonuses(player, soloOnClass)                     (§1.14)
 * then per stat: x (1 + sum additive%) x product(multiplicative)                   (§1.13)
 * </pre>
 * <b>The cache key is {@code (player, path)}, not {@code player}</b> (§7): a Mage's equipment and Accessory Power
 * are both path-dependent, so the same Mage has different Strength, Crit Damage, Intelligence and Ability Damage
 * on a beam than on a cast.
 * <p>
 * The cache is invalidated on <b>equipment</b> change as well as inventory change - the masks and hats are hotbar
 * items that get worn mid-fight, and a helmet swap is worth thousands of Intelligence (§1.10).  It also expires on
 * its own after a short window, because several inputs move without any inventory event at all: Legion counts
 * players within 30 blocks, the Berserk stack and combo change per hit, and the Ragnarock buff comes and goes.
 */
public final class Stats {
	private Stats() {}

	/**
	 * How long a cached aggregate stays valid.  Short enough that a Legion stack walking into range or the
	 * Ragnarock buff landing shows up promptly, long enough that a Terminator volley does not recompute per arrow.
	 */
	private static final int CACHE_TICKS = 5;

	private record Key(UUID player, DamagePath path) {}

	private record Cached(int tick, StatBlock stats) {}

	private static final Map<Key, Cached> CACHE = new HashMap<>();

	/** Drop every cached aggregate.  Cheap, and the safe response to anything that might have changed a stat. */
	public static void invalidateAll() {
		CACHE.clear();
	}

	/** Drop one player's cached aggregates, on every path. */
	public static void invalidate(Player p) {
		if(p == null) return;
		CACHE.keySet().removeIf(k -> k.player().equals(p.getUniqueId()));
	}

	/** This player's finished stat aggregate on a damage path. */
	public static StatBlock of(Player p, DamagePath path) {
		if(p == null) return StatBlock.EMPTY;
		Key key = new Key(p.getUniqueId(), path);
		Cached hit = CACHE.get(key);
		int now = MinecraftServer.currentTick;
		if(hit != null && now - hit.tick() < CACHE_TICKS) return hit.stats();
		StatBlock computed = compute(p, path);
		CACHE.put(key, new Cached(now, computed));
		return computed;
	}

	/**
	 * The same aggregate, itemised by source, for {@code /verbose super} and {@code /eq}.  Never cached: it is
	 * only built when someone is actually looking at it.
	 */
	public static Map<String, StatBlock> breakdown(Player p, DamagePath path) {
		Map<String, StatBlock> out = new LinkedHashMap<>();
		if(p == null) return out;
		DungeonClass clazz = DungeonClass.of(p);
		boolean solo = DungeonClass.isSoloOnClass(p);
		Pet pet = Pet.forPlayer(p, path);
		PlayerInventory inv = p.getInventory();

		put(out, "weapon", itemStats(inv.getItemInMainHand(), pet));
		put(out, "helmet", itemStats(inv.getHelmet(), pet));
		put(out, "chestplate", itemStats(inv.getChestplate(), pet));
		put(out, "leggings", itemStats(inv.getLeggings(), pet));
		put(out, "boots", itemStats(inv.getBoots(), pet));
		put(out, "equipment", Equipment.forClass(clazz, path));
		put(out, "power + tunings", Powers.forClass(clazz, path));
		put(out, "profile", Profile.base());
		put(out, "pet (" + pet + ")", pet.ownStats());
		put(out, "class bonus", ClassBonuses.stats(clazz, solo));
		put(out, "ragnarock buff", StatBlock.of(Stat.STRENGTH, ragnarockStrength(p, pet)));
		return out;
	}

	private static void put(Map<String, StatBlock> out, String label, StatBlock stats) {
		if(stats != null && !stats.isEmpty()) out.put(label, stats);
	}

	private static StatBlock compute(Player p, DamagePath path) {
		StatBlock sum = StatBlock.EMPTY;
		for(StatBlock part : breakdown(p, path).values()) sum = sum.plus(part);

		// The stat stage.  Per stat, and WHOLE-stat: the sum above (items + armour + equipment + power + profile)
		// is what gets multiplied, not just the profile half.
		StatBlock staged = StatBlock.EMPTY;
		Pet pet = Pet.forPlayer(p, path);
		for(Stat stat : Stat.values()) {
			double value = sum.get(stat)
					* (1.0 + Profile.additivePercent(p, stat, pet) / 100.0)
					* Profile.multiplicative(stat);
			staged = staged.plus(stat, value);
		}
		return staged;
	}

	/** One worn/held item's contribution, or nothing if it is not a registered stat item. */
	private static StatBlock itemStats(ItemStack item, Pet pet) {
		ItemDef def = Items.of(item);
		return def == null ? StatBlock.EMPTY : def.stats(pet);
	}

	/**
	 * The Ragnarock Axe's ability: <b>+150% of that weapon's own Strength stat</b> for 10s (§1.7), not a Strength
	 * potion effect and not a constant.  The plan's "+939" is what this evaluates to at the authored terms, so it
	 * never appears in code - retuning the axe moves the buff with it, automatically.
	 * <p>
	 * The full 626 counts, Chimera's +300 included: Chimera is an ITEM source, so its copy is part of "this
	 * weapon's Strength" exactly like the reforge and gemstone terms.  The value is read off the axe's definition
	 * rather than off whatever is in the hand, because <b>the buff must keep applying after the axe leaves the
	 * hand</b> - casting Ragnarock and then switching to a hitting weapon is the entire point of the item.
	 */
	public static double ragnarockStrength(Player p, Pet pet) {
		if(p == null || !RagnarockBuff.isActive(p)) return 0;
		ItemDef axe = Items.byName("Withered Ragnarock Axe");
		if(axe == null) return 0;
		return 1.5 * axe.stats(pet).get(Stat.STRENGTH);
	}
}
