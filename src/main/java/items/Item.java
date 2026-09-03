package items;

import damage.ItemDef;
import damage.Rarity;
import damage.ReforgeId;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.Utils;

import java.util.List;

/**
 * <b>The superinterface every custom item stems from.</b>  One implementation per BASE item, holding that item's
 * identity, its ItemStack factory and (through the subinterfaces) its behaviour, so an item is one file rather
 * than a lore ID in {@code CustomItems}' switch, a builder call in {@code FakePlayerInventory}, a name in
 * {@code Catalog.PALETTE_ORDER} and a term list in {@code damage/Items}.
 *
 * <h2>Reforge is a PARAMETER, not part of the class</h2>
 * A reforge is a separate axis from the item (MAP.md §2.4), so the Heroic and the Withered Hyperion are one
 * {@code Item} resolved at two {@link ReforgeId}s, not two classes.  {@link #reforges()} lists the variants that
 * ship today and {@link #build(ReforgeId)} takes the one to build; the display name is COMPOSED from the reforge
 * rather than written down, which is what makes "any valid reforge on any item" a change to
 * {@link #reforges()} instead of a rewrite.  For now those lists are hardcoded per item.
 *
 * <h2>Colour is DERIVED from rarity</h2>
 * No factory writes a colour any more: {@link #colouredName} is {@code <effectiveRarity().colour()>} plus the
 * composed name, so an item can never read as the wrong tier.  That is why the four Renowned rarities and the
 * Diamond Necron Head's were corrected in {@code damage/Items} - they were the only ones whose registered rarity
 * disagreed with the colour they had always been drawn in.
 *
 * <h2>Two invariants a change here can break silently</h2>
 * <ol>
 *   <li><b>The item ID stays on lore line 0.</b>  {@code ItemUtils.getID} reads it, and
 *       {@code Catalog.paletteKey} ({@code material | plain display name | first lore line}) is what BOTH
 *       {@code loadout/ItemRefresh} copies match a saved loadout against.  {@code damage/StatLore} appends its
 *       stat rows BELOW it.  Move the ID and every saved loadout in the network silently stops updating, with no
 *       migration table that can rescue it.</li>
 *   <li><b>Material and plain display name are identity.</b>  A plain-name change needs a permanent
 *       {@code ItemRefresh.RENAMED} entry in M7's copy AND in StradDevHub's twin; a MATERIAL change has no
 *       migration path at all.  Colour changes are free, since {@code paletteKey} strips styling.</li>
 * </ol>
 */
public interface Item {

	// ===== identity =====

	/**
	 * This item's ID, which {@link #build} writes as lore line 0.  Empty for an item that carries none: the
	 * armour, the wearable heads, the SkyBlock Menu and the Pitchin' Rod.  An empty ID keeps
	 * {@code Utils.firstLorePlain} at {@code ""}, which is the third component every one of those items' palette
	 * keys has always had.
	 */
	default String loreId() {
		return "";
	}

	Material material();

	/**
	 * The name with NO colour and NO reforge word: {@code "Hyperion"}, {@code "Dark Claymore"}.
	 * <p>
	 * <b>Lookalike characters count.</b>  The Ragnarock Axe's is {@code "Ragnarοck Axe"} with a Greek omicron
	 * (U+03BF) rather than an ASCII o, deliberately, so the name stops colliding with one of the owner's client
	 * mods.  The two spellings render identically and are not equal, and getting it wrong drops the item to the
	 * tail of the palette and freezes every saved copy.  Grep it as {@code Ragnar\x{03BF}ck}, never by typing it.
	 */
	String baseName();

	/** The BASE rarity.  Recombobulating is assumed on ({@link #recombobulated}), so read {@link #effectiveRarity}. */
	Rarity baseRarity();

	/**
	 * Whether this item is recombobulated.  Everything in this plugin is (§1.0.9); the two special tiers answer
	 * false through {@link Rarity#recombobulated()} regardless, since a Recombobulator does not apply to them.
	 */
	default boolean recombobulated() {
		return true;
	}

	/** The rarity the item actually reads and is drawn at, and the one the reforge/gemstone tables are keyed on. */
	default Rarity effectiveRarity() {
		return recombobulated() ? baseRarity().recombobulated() : baseRarity();
	}

	// ===== reforges =====

	ReforgeId defaultReforge();

	/**
	 * Every reforge this item ships in, i.e. every variant the palette offers.  One entry for almost everything;
	 * the Hyperion is {@code [HEROIC, FABLED]} and three Storm's pieces carry their alternate-reforge twin.
	 */
	default List<ReforgeId> reforges() {
		return List.of(defaultReforge());
	}

	// ===== names =====

	/** The PLAIN display name at a reforge, i.e. the {@code damage/Items} key and the {@code paletteKey} component. */
	default String displayName(ReforgeId reforge) {
		String prefix = reforge == null ? "" : reforge.displayName();
		return prefix.isEmpty() ? baseName() : prefix + " " + baseName();
	}

	default String displayName() {
		return displayName(defaultReforge());
	}

	/** The MiniMessage display name: the effective rarity's colour, then the composed plain name. */
	default String colouredName(ReforgeId reforge) {
		return "<" + effectiveRarity().colour() + ">" + displayName(reforge);
	}

	default String colouredName() {
		return colouredName(defaultReforge());
	}

	// ===== stacks =====

	/** A FRESH stack of this item at the given reforge.  Must end with {@code damage.StatLore.apply}. */
	ItemStack build(ReforgeId reforge);

	default ItemStack build() {
		return build(defaultReforge());
	}

	/** Every variant of this item, in {@link #reforges()} order.  What the palette and the templates are built from. */
	default List<ItemStack> variants() {
		return reforges().stream().map(this::build).toList();
	}

	// ===== stats =====

	/**
	 * This item's stat definition at a reforge, or null if it grants none.  The bridge to {@code damage/Items},
	 * which stays the one place item NUMBERS live and is still keyed on the plain display name.
	 */
	default ItemDef stats(ReforgeId reforge) {
		return damage.Items.byName(displayName(reforge));
	}

	// ===== click plumbing =====
	// These are properties of the ITEM rather than of an ability, and they live here rather than on
	// AbilityItem because the items that need them are not all ability items: the old rule keyed on "the lore ID
	// starts with skyblock/", which catches the Infinileap (a menu) and the Spring Boots (a wearable) too.

	/**
	 * Whether a right-click on this item cancels the interact event, suppressing vanilla item use (a bow draw, a
	 * TNT placement, throwing the Infinileap's pearl).  False only for the items whose right-click is not an
	 * ability at all and must still interact with the world: the Gyrokinetic Wand and the Dungeonbreaker.
	 * <p>
	 * <b>Only consulted for an item with a lore ID.</b>  Armour and the wearable heads carry none, so a
	 * right-click holding one has always gone through untouched.
	 */
	default boolean cancelsInteract() {
		return true;
	}

	/** Whether right-clicking an ENTITY with this in hand is allowed through (item frames and the like). */
	default boolean allowsEntityInteract() {
		return false;
	}

	/**
	 * Whether this item's LEFT click is an ability rather than a swing, so it must NEVER break a block - even
	 * when the ability is on cooldown or capped by the one-per-tick guard - and must never be accompanied by a
	 * melee hit.  The mage beam adds to this dynamically, since whether a sword beams depends on the holder's
	 * class, which is not a property of the item.
	 */
	default boolean suppressesBlockBreak() {
		return false;
	}

	/**
	 * Whether this item still works inside the Trap room, where the clear phase disables right-click abilities.
	 * Only the Dungeonbreaker does, alongside vanilla ender pearls, which are not an {@code Item} at all.
	 */
	default boolean usableInTrapRoom() {
		return false;
	}

	// ===== matching =====

	/** True if {@code stack} is this item at any of its reforges.  Material + plain display name, as paletteKey. */
	default boolean matches(ItemStack stack) {
		if(stack == null || stack.getType() != material() || !stack.hasItemMeta()) return false;
		ItemMeta meta = stack.getItemMeta();
		if(meta == null) return false;
		String name = Utils.plain(meta.displayName());
		for(ReforgeId reforge : reforges()) {
			if(displayName(reforge).equals(name)) return true;
		}
		return false;
	}
}
