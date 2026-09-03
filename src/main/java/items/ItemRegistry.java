package items;

import damage.ReforgeId;
import items.armor.BonzoMask;
import items.armor.CowHat;
import items.armor.NecronBoots;
import items.armor.NecronChestplate;
import items.armor.NecronHead;
import items.armor.NecronLeggings;
import items.armor.RacingHelmet;
import items.armor.SpiritMask;
import items.armor.SpringBootsItem;
import items.armor.StormBoots;
import items.armor.StormChestplate;
import items.armor.StormHelmet;
import items.armor.StormLeggings;
import items.armor.ThermodynamicBoots;
import items.armor.ThermodynamicChestplate;
import items.armor.ThermodynamicHelmet;
import items.armor.ThermodynamicLeggings;
import items.armor.WitherGoggles;
import items.bows.DeathBow;
import items.bows.ExplosiveBow;
import items.bows.LastBreath;
import items.bows.Terminator;
import items.combat.AspectOfTheVoid;
import items.combat.AxeOfTheShredded;
import items.combat.BonzoStaff;
import items.combat.DarkClaymore;
import items.combat.FlamingFlay;
import items.combat.GolemSword;
import items.combat.Hyperion;
import items.combat.IceSprayWand;
import items.combat.JerrychineGun;
import items.combat.RagnarockAxe;
import items.combat.SpiritSceptre;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every custom item, indexed two ways.
 *
 * <h2>Why two indexes</h2>
 * Neither key alone covers the roster, which is why {@code SkyBlock in Vanilla}'s single lore-ID map could not
 * simply be copied:
 * <ul>
 *   <li><b>By lore ID</b> - the ability dispatch's hot path.  Armour and the wearable heads carry no lore ID at
 *       all, so they are absent from this index, and the two Hyperion reforges SHARE one, so it maps to the base
 *       item rather than to a variant.</li>
 *   <li><b>By plain display name</b> - one entry per REFORGE VARIANT, which is the key {@code damage/Items},
 *       {@code Catalog.paletteKey} and both {@code loadout/ItemRefresh} copies already use.  This is what lets a
 *       wearable be looked up at all.</li>
 * </ul>
 * {@link #of(ItemStack)} tries the name first and the lore ID second, so it answers for everything.
 *
 * <h2>Order</h2>
 * {@link #ALL} is grouped by package, NOT by the loadout editor's page layout - that lives in
 * {@code Catalog.PALETTE_ORDER}, which is a hand-picked UI ordering in blocks of nine.  {@code Catalog.verify()}
 * cross-checks the two at boot so an item can never be added here and quietly go missing from the palette.
 */
public final class ItemRegistry {
	private ItemRegistry() {}

	/** Every item, grouped by package.  Adding an item means adding it here and to {@code PALETTE_ORDER}. */
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

	/** The item with this lore ID, or null.  Empty and unknown IDs both answer null. */
	public static Item byLoreId(String loreId) {
		return loreId == null || loreId.isEmpty() ? null : BY_LORE_ID.get(loreId);
	}

	/** The item with this plain display name at any of its reforges, or null. */
	public static Item byName(String displayName) {
		return displayName == null ? null : BY_NAME.get(displayName);
	}

	/** The reforge a plain display name resolves to, or null if the name is not a known variant. */
	public static ReforgeId reforgeOf(String displayName) {
		return displayName == null ? null : REFORGE_BY_NAME.get(displayName);
	}

	/**
	 * The item this stack is, or null.
	 * <p>
	 * <b>Name first, lore ID second.</b>  The name is the finer key - it is what separates the two Hyperions and
	 * what identifies a wearable - and the ID is the fallback that still answers for a stack whose name has
	 * drifted from any current variant, which is exactly the stale-saved-loadout case
	 * {@code loadout/ItemRefresh} exists to repair.
	 */
	public static Item of(ItemStack stack) {
		if(stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return null;
		ItemMeta meta = stack.getItemMeta();
		if(meta == null) return null;
		Item byName = BY_NAME.get(Utils.plain(meta.displayName()));
		if(byName != null && byName.material() == stack.getType()) return byName;
		return byLoreId(Utils.firstLorePlain(meta));
	}

	/** Every reforge variant's plain display name, in {@link #ALL} order. */
	public static List<String> variantNames() {
		return new ArrayList<>(BY_NAME.keySet());
	}

	/** Every item that is worn, for the equipment-driven lookups (speed, cheat death, set bonuses). */
	public static Wearable wearable(ItemStack stack) {
		Item item = of(stack);
		return item instanceof Wearable worn ? worn : null;
	}

	/**
	 * The item that fired this projectile, matched on its scoreboard TAG, or null.
	 * <p>
	 * By tag rather than by the shooter's held item on purpose: the shot has already left, and by the time it
	 * lands the shooter may well have swapped.  A map rather than a scan of {@link #ALL}, because a projectile
	 * impact is a hot event and only two items are ever a source.
	 */
	public static ProjectileItem projectileSource(Set<String> tags) {
		if(tags == null || tags.isEmpty()) return null;
		for(Map.Entry<String, ProjectileItem> e : BY_PROJECTILE_TAG.entrySet()) {
			if(tags.contains(e.getKey())) return e.getValue();
		}
		return null;
	}
}
