package loadout;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.Catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a SAVED loadout in step with the CURRENT item definitions.
 * <p>
 * A saved loadout holds frozen copies of whatever the items looked like when the player picked them. Every later
 * change to an item, whether new lore, a new attribute modifier, a {@code can_break} stamp, or the whole stat
 * block DAMAGE_PLAN.md will hang on these items, would otherwise never reach a player who already saved one.  This class
 * is the reason that migration is automatic: on every open of the editor and on every kit hand-out, each saved
 * stack is looked up in the live catalog and REPLACED by today's definition. Nothing needs a one-off migration
 * script when items change; just change the item factory.
 * <p>
 * <b>Invariant this depends on:</b> {@link #key} is material + display name + the item's ID, and the ID is the
 * FIRST lore line ({@code CustomItems.getID()} reads {@code getLore().getFirst()}). So new lore may be APPENDED
 * freely, whether stat lines, ability text or rarity, and matching still works.  Prepending a line above the ID, or moving
 * the ID out of lore, breaks this silently: every saved item stops matching, is left frozen (see {@link #refresh})
 * and quietly stops receiving updates. Keep the ID on line 0.
 * <p>
 * The template supplies the item definition; the saved stack keeps its amount and its enchants (applied ON TOP of
 * the template's, with the saved level winning).  That is what keeps a Tank's Power 17 Terminator from being "upgraded" to
 * the palette's Archer Power 70 copy.
 * <p>
 * Note that the network plugin has a twin ({@code loadout/ItemRefresh.java}) for the servers M7 isn't on: same key
 * rule, same {@link #RENAMED} table, but its templates come from the exported JSON instead of the live factories.
 * <b>Keep the two in sync.</b>
 */
public final class ItemRefresh {
	private ItemRefresh() {}

	/**
	 * Display names that have been RENAMED, old -> new. {@link #key} folds the display name in, so a rename makes a
	 * saved item stop matching its template: it would be left frozen with the old label forever (and stop picking up
	 * real definition changes).  Each entry is a pure rename, with the same material and item ID, so the old key can be
	 * rewritten to the new one and matched normally.
	 * <p>
	 * These are permanent migration entries: a player who has not opened the editor since the rename is still out
	 * there, so <b>do not prune this map</b>. Add a line whenever an item's display name changes, here AND in the
	 * network plugin's copy.
	 */
	private static final Map<String, String> RENAMED = Map.of(
			"Withered Golem Sword", "Suspicious Golem Sword",
			"Withered Axe of the Shredded", "Suspicious Axe of the Shredded",
			"Withered Ragnarok Axe", "Withered Ragnarοck Axe",
			"Bonzo Staff", "Heroic Bonzo Staff");

	/**
	 * Display names of items that no longer exist and must be CLEARED out of saved loadouts, leaving the slot empty.
	 * <p>
	 * This is the deliberate exception to {@link #refresh}'s "an item with no template is left alone" rule. That
	 * rule is right for an item that is merely absent from the palette, since it may come back and silently eating the
	 * slot a player chose would be worse. It is wrong for an item that has been deleted outright: the player would
	 * carry a dead stack forever, since nothing would ever match it again.
	 * <p>
	 * Matched on display name alone (across any material), so only list names that are unmistakably one item. Like
	 * {@link #RENAMED} these entries are permanent, because someone who has not opened the editor since the removal
	 * still needs them, and they must be mirrored in the network plugin's copy.
	 * <ul>
	 *   <li>Rapid Bonemerang: deleted from the Archer kit.  Its throw was never implemented, so it did nothing.</li>
	 * </ul>
	 */
	private static final Set<String> REMOVED = Set.of("Rapid Bonemerang");

	/**
	 * Bring every item in every class this player has saved up to date, re-saving the file if anything changed.
	 * Returns how many slots changed (0 if they have nothing saved).
	 */
	public static int refreshSaved(UUID uuid) {
		Map<String, ItemStack> templates = templates();
		if(templates.isEmpty()) return 0;
		Loadouts.LoadoutFile f = Loadouts.load(uuid);
		int changed = 0;
		for(Map.Entry<String, List<String>> e : f.perClass.entrySet()) {
			ItemStack[] arr = Loadouts.fromSer(e.getValue());
			int n = refreshAll(arr, templates);
			if(n > 0) {
				e.setValue(Loadouts.toSer(arr));
				changed += n;
			}
		}
		if(changed > 0) Loadouts.save(uuid, f);
		return changed;
	}

	/** Refresh every slot of a 41-slot loadout array in place; returns how many slots changed. */
	public static int refreshAll(ItemStack[] arr, Map<String, ItemStack> templates) {
		if(arr == null || templates.isEmpty()) return 0;
		int changed = 0;
		for(int i = 0; i < arr.length; i++) {
			if(isRemoved(arr[i])) { // deleted item: empty the slot rather than leave a dead stack behind
				arr[i] = null;
				changed++;
				continue;
			}
			ItemStack fresh = refresh(arr[i], templates);
			if(fresh != null) {
				arr[i] = fresh;
				changed++;
			}
		}
		return changed;
	}

	/** True if this saved stack is an item that has been deleted outright ({@link #REMOVED}). */
	public static boolean isRemoved(ItemStack it) {
		String k = key(it);
		String name = k == null ? null : nameIn(k);
		return name != null && REMOVED.contains(name);
	}

	/**
	 * The current version of one saved item, or null if it is already current or has no template. An item the
	 * catalog no longer offers is left exactly as it is rather than deleted, because a removed item should not silently
	 * eat the slot a player put it in.
	 */
	public static ItemStack refresh(ItemStack saved, Map<String, ItemStack> templates) {
		String k = key(saved);
		ItemStack tmpl = k == null ? null : templates.get(k);
		// Direct miss: the saved copy may predate a rename, so retry under the item's current name.
		if(tmpl == null && k != null) {
			String renamed = renamedKey(k);
			if(renamed != null) tmpl = templates.get(renamed);
		}
		if(tmpl == null) return null;

		ItemStack fresh = tmpl.clone();
		ItemMeta fm = fresh.getItemMeta();
		if(fm == null || !fm.hasEnchantmentGlintOverride()) fresh.addUnsafeEnchantments(saved.getEnchantments());
		fresh.setAmount(saved.getAmount());
		// isSimilar compares everything but the amount, which was just copied, so this is a full compare.
		return saved.isSimilar(fresh) ? null : fresh;
	}

	/**
	 * Current version of every item a loadout can hold, keyed by {@link #key}. Palette first (the selectable items,
	 * and the canonical copy of each), then every class's default kit, which is what supplies the items the palette
	 * deliberately withholds (the SkyBlock Menu) and each class's own Terminator Power.
	 */
	public static Map<String, ItemStack> templates() {
		Map<String, ItemStack> out = new HashMap<>();
		for(ItemStack it : Catalog.palette()) put(out, it);
		for(String role : Loadouts.CLASSES) {
			for(ItemStack it : Catalog.defaultFor(role)) put(out, it);
		}
		return out;
	}

	private static void put(Map<String, ItemStack> out, ItemStack it) {
		String k = key(it);
		if(k != null) out.putIfAbsent(k, it); // first source wins: the palette is the canonical version
	}

	/**
	 * Item identity for template matching: material + display name + item ID (first lore line). This is
	 * {@code Catalog.paletteKey} applied to an arbitrary stack, so change one and change the other, plus the network
	 * plugin's copy). Null for an empty slot.
	 */
	public static String key(ItemStack it) {
		if(it == null || it.getType().isAir()) return null;
		return Catalog.paletteKey(it);
	}

	/**
	 * The display-name segment of a {@link #key}, or null if the string isn't one. The key is
	 * {@code MATERIAL|display name|item id} and neither the material nor an item ID contains a {@code |}, so the
	 * first and last separators bracket the name exactly.
	 */
	private static String nameIn(String key) {
		int start = key.indexOf('|'), end = key.lastIndexOf('|');
		return start < 0 || end < start ? null : key.substring(start + 1, end);
	}

	/**
	 * The same key with its display-name segment swapped for the item's current name, or null if that name was
	 * never renamed.
	 */
	private static String renamedKey(String key) {
		String name = nameIn(key);
		String now = name == null ? null : RENAMED.get(name);
		if(now == null) return null;
		return key.substring(0, key.indexOf('|') + 1) + now + key.substring(key.lastIndexOf('|'));
	}
}
