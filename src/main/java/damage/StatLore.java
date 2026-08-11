package damage;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders an item's stats into its lore (DAMAGE_PLAN.md §7b).
 * <p>
 * <b>Lore is output, never input.</b>  Nothing reads a stat back off a lore line; the numbers are computed from
 * {@link Items}' term lists, the same lists the damage math reads, so the two cannot drift.  Hand-writing them
 * would create a second source of truth, which is exactly the failure §2.4 exists to prevent.
 *
 * <h2>The hard constraint: the ID stays on lore line 0</h2>
 * {@code CustomItems.getID} reads the FIRST lore line, and {@code Catalog.paletteKey}
 * ({@code material | display name | first lore line}) is what {@code loadout/ItemRefresh} matches saved loadouts
 * against.  So <b>stat lines are APPENDED below line 0, never inserted above it</b>.  A SkyBlock-style tooltip
 * normally puts stats at the top; doing that here would move the ID off line 0 and every custom item would stop
 * being recognised (no abilities) <i>and</i> every saved loadout in the network would silently stop updating.
 * <p>
 * The number shown is the DUNGEON-SCALED one, because that is how items read in dungeons: a Fabled Hyperion shows
 * {@code +3729.6} Strength, not {@code +560}.  Non-dungeon items show their flat value, which for them is the same
 * number anyway.  Only stats the item actually grants get a row - no zero rows - and the reforge and rarity are
 * already in the display name, so lore does not repeat them.
 */
public final class StatLore {
	private StatLore() {}

	/** The order stat rows are listed in, matching SkyBlock's own tooltip order. */
	private static final Stat[] ROWS = {Stat.DAMAGE, Stat.STRENGTH, Stat.CRIT_CHANCE, Stat.CRIT_DAMAGE,
			Stat.INTELLIGENCE, Stat.ABILITY_DAMAGE};

	/**
	 * Append this item's stat rows to its lore, if it is a registered stat item.  Returns the same stack, so it
	 * can be chained onto an item factory's return.
	 * <p>
	 * Idempotent by construction: the factories build a fresh stack each time and this is the only thing that adds
	 * these rows, so an item is never double-annotated.  {@code loadout/ItemRefresh} re-matches saved loadouts
	 * against the live factories on every editor open, so a change to a stat term propagates to everyone's saved
	 * kit with no migration - provided line 0 is untouched.
	 */
	public static ItemStack apply(ItemStack item) {
		ItemDef def = Items.of(item);
		if(def == null) return item;
		// The pet only matters for a Chimera item, and lore is shown outside any hit, so the default assumption
		// (§1.13's table) is the honest one to render.
		StatBlock stats = def.stats(Pet.GOLDEN_DRAGON);
		if(stats.isEmpty()) return item;

		ItemMeta meta = item.getItemMeta();
		if(meta == null) return item;
		List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
		// An item with no ability carries no lore ID, so it has no lore at all - the armour pieces and the
		// wearable heads.  Its palette key's third component is therefore the empty string, and appending a stat
		// row straight onto nothing would make the FIRST stat line the key instead, silently orphaning every saved
		// loadout that contains it.  A blank line 0 keeps firstLorePlain at "" and reads as a normal SkyBlock
		// tooltip separator, so the key is untouched.
		if(lore.isEmpty()) lore.add(Component.empty());
		for(Stat stat : ROWS) {
			double value = stats.get(stat);
			if(value == 0) continue;
			lore.add(Utils.mm("<gray>" + stat.display() + ": " + stat.colour() + "+" + trim(value)
					+ " " + stat.symbol()));
		}
		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}

	/** Drop a trailing {@code .0} so whole numbers read as {@code +999} rather than {@code +999.0}. */
	private static String trim(double value) {
		String s = Utils.round(value, 2);
		if(s.endsWith(".00")) return s.substring(0, s.length() - 3);
		if(s.endsWith("0")) return s.substring(0, s.length() - 1);
		return s;
	}
}
