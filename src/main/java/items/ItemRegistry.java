package items;

import damage.ReforgeId;
import items.armor.*;
import items.bows.DeathBow;
import items.bows.ExplosiveBow;
import items.bows.LastBreath;
import items.bows.Terminator;
import items.combat.*;
import items.tools.Dungeonbreaker;
import items.tools.GyrokineticWand;
import items.tools.InfinityboomTNT;
import items.tools.TacticalInsertion;
import items.util.Infinileap;
import items.util.PitchinRod;
import items.util.SkyblockMenu;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.Utils;

import java.util.*;

/**
 * Every custom item, indexed two ways, since neither key covers the roster alone:
 * <ul>
 *   <li><b>By lore ID</b>: dispatch hot path. Armour and heads have none; the two Hyperions SHARE one, so it maps
 *       to the base item.</li>
 *   <li><b>By plain name</b>: one per REFORGE VARIANT, the key {@code damage/Items}, {@code Catalog.paletteKey} and
 *       both {@code ItemRefresh} copies use. The only way to look up a wearable.</li>
 * </ul>
 * {@link #ALL} is grouped by package, not palette layout ({@code Catalog.PALETTE_ORDER}); {@code Catalog.verify()}
 * cross-checks them at boot so nothing goes missing from the palette.
 */
public final class ItemRegistry {
	private ItemRegistry() {}

	/** Adding an item means adding it here and to {@code PALETTE_ORDER}. */
	public static final List<Item> ALL = List.of(
			// weapons
			Hyperion.INSTANCE, DarkClaymore.INSTANCE, IceSprayWand.INSTANCE, AspectOfTheVoid.INSTANCE,
			RagnarockAxe.INSTANCE, FlamingFlay.INSTANCE, AxeOfTheShredded.INSTANCE, GolemSword.INSTANCE,
			BonzoStaff.INSTANCE, JerrychineGun.INSTANCE, SpiritSceptre.INSTANCE,
			// bows
			Terminator.INSTANCE, LastBreath.INSTANCE, ExplosiveBow.INSTANCE, DeathBow.INSTANCE,
			// tools and utility wands
			InfinityboomTNT.INSTANCE, Dungeonbreaker.INSTANCE, GyrokineticWand.INSTANCE,
			TacticalInsertion.INSTANCE,
			// armour and wearable heads
			NecronHead.INSTANCE, NecronChestplate.INSTANCE, NecronLeggings.INSTANCE, NecronBoots.INSTANCE,
			StormHelmet.INSTANCE, StormChestplate.INSTANCE, StormLeggings.INSTANCE, StormBoots.INSTANCE,
			WitherGoggles.INSTANCE, SpiritMask.INSTANCE, BonzoMask.INSTANCE, RacingHelmet.INSTANCE,
			CowHat.INSTANCE, ThermodynamicHelmet.INSTANCE, ThermodynamicChestplate.INSTANCE,
			ThermodynamicLeggings.INSTANCE, ThermodynamicBoots.INSTANCE, SpringBootsItem.INSTANCE,
			// no ability, no stats
			Infinileap.INSTANCE, SkyblockMenu.INSTANCE, PitchinRod.INSTANCE);

	private static final Map<String, Item> BY_LORE_ID = new LinkedHashMap<>();
	private static final Map<String, Item> BY_NAME = new LinkedHashMap<>();
	private static final Map<String, ReforgeId> REFORGE_BY_NAME = new LinkedHashMap<>();
	private static final Map<String, ProjectileItem> BY_PROJECTILE_TAG = new LinkedHashMap<>();

	static {
		for(Item item : ALL) {
			if(!item.loreId().isEmpty()) BY_LORE_ID.putIfAbsent(item.loreId(), item);
			for(ReforgeId reforge : item.reforges()) {
				BY_NAME.put(item.displayName(reforge), item);
				REFORGE_BY_NAME.put(item.displayName(reforge), reforge);
			}
			if(item instanceof ProjectileItem source) BY_PROJECTILE_TAG.put(source.projectileTag(), source);
		}
	}

	public static Item byLoreId(String loreId) {
		return loreId == null || loreId.isEmpty() ? null : BY_LORE_ID.get(loreId);
	}

	/** Any reforge. */
	public static Item byName(String displayName) {
		return displayName == null ? null : BY_NAME.get(displayName);
	}

	/** Null if the name isn't a known variant. */
	public static ReforgeId reforgeOf(String displayName) {
		return displayName == null ? null : REFORGE_BY_NAME.get(displayName);
	}

	/**
	 * Name first (the finer key: separates the Hyperions, identifies wearables), lore ID second, which still
	 * answers for a stale saved stack whose name has drifted, the case {@code loadout/ItemRefresh} repairs.
	 */
	public static Item of(ItemStack stack) {
		if(stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return null;
		ItemMeta meta = stack.getItemMeta();
		if(meta == null) return null;
		Item byName = BY_NAME.get(Utils.plain(meta.displayName()));
		if(byName != null && byName.material() == stack.getType()) return byName;
		return byLoreId(Utils.firstLorePlain(meta));
	}

	/** In {@link #ALL} order. */
	public static List<String> variantNames() {
		return new ArrayList<>(BY_NAME.keySet());
	}

	/** For equipment lookups (speed, cheat death, set bonuses). */
	public static Wearable wearable(ItemStack stack) {
		Item item = of(stack);
		return item instanceof Wearable worn ? worn : null;
	}

	/**
	 * Matched on scoreboard TAG, not held item: the shooter may have swapped by impact. A map, not a scan of
	 * {@link #ALL}, since impacts are hot and only two items are sources.
	 */
	public static ProjectileItem projectileSource(Set<String> tags) {
		if(tags == null || tags.isEmpty()) return null;
		for(Map.Entry<String, ProjectileItem> e : BY_PROJECTILE_TAG.entrySet()) {
			if(tags.contains(e.getKey())) return e.getValue();
		}
		return null;
	}
}
