package damage;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.M7tas;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Renders an item's stats into its lore (MAP.md §7b).
 * <p>
 * <b>Lore is output, never input.</b>  Nothing reads a stat back off a lore line; the numbers are computed from
 * {@link Items}' term lists, the same lists the damage math reads, so the two cannot drift.  Hand-writing them
 * would create a second source of truth, which is exactly the failure §2.4 exists to prevent.
 *
 * <h2>The hard constraint: the ID stays on lore line 0</h2>
 * {@code items.ItemUtils.getID} reads the FIRST lore line, and {@code Catalog.paletteKey}
 * ({@code material | display name | first lore line}) is what {@code loadout/ItemRefresh} matches saved loadouts
 * against.  So <b>stat lines are APPENDED below line 0, never inserted above it</b>.  A SkyBlock-style tooltip
 * normally puts stats at the top; doing that here would move the ID off line 0 and every custom item would stop
 * being recognised (no abilities) <i>and</i> every saved loadout in the network would silently stop updating.
 * <p>
 * {@link #strip} carries the same constraint: it will never delete line 0 and never deletes a line it did not
 * positively recognise as a row this class wrote.
 * <p>
 * The number shown is the DUNGEON-SCALED one, because that is how items read in dungeons: a Fabled Hyperion shows
 * {@code +3729.6} Strength, not {@code +560}.  Non-dungeon items show their flat value, which for them is the same
 * number anyway.  Only stats the item actually grants get a row - no zero rows - and the reforge and rarity are
 * already in the display name, so lore does not repeat them.
 *
 * <h2>Two renderings, because the pet moves</h2>
 * The pet reaches exactly one term: {@code ItemDef.breakdown} adds a {@code chimera (<pet>)} entry when the item
 * carries Chimera, copying the pet's base stats onto the weapon, cata-scaled.  <b>So only a Chimera item has
 * pet-dependent lore</b>, and only a Chimera item is ever re-rendered.
 * <ul>
 *   <li>{@link #apply(ItemStack)} is the TEMPLATE rendering: the item factories, {@code plugin/FakePlayerInventory}
 *       and {@code plugin/Utils} build stacks with no player in hand, so it renders the assumed default
 *       (§1.13's Golden Dragon).  It is not a claim about anybody's pet.</li>
 *   <li>{@link #refreshChimeraLore(Player)} is the LIVE rendering: it re-renders the Chimera items a player is
 *       actually carrying for the pet they actually have out, in every mode.</li>
 * </ul>
 */
public final class StatLore {
	private StatLore() {}

	/** The order stat rows are listed in, matching SkyBlock's own tooltip order. */
	private static final Stat[] ROWS = {Stat.DAMAGE, Stat.STRENGTH, Stat.CRIT_CHANCE, Stat.CRIT_DAMAGE,
			Stat.INTELLIGENCE, Stat.ABILITY_DAMAGE};

	/**
	 * The plain-text shape of one rendered row, built from {@link #ROWS} so it can never drift from what
	 * {@link #apply(ItemStack, Pet)} writes: a stat's display name, {@code ": +"}, and a number - anchored at both
	 * ends, so a line has to be nothing BUT a row to match.
	 * <p>
	 * It is the stat-name prefix that makes this safe to strip on.  The only other lore a stat item carries is
	 * line 0, which is the item ID ({@code skyblock/combat/scylla}) or, on armour, an empty separator; neither can
	 * read as {@code "Strength: +..."}.  The number half is deliberately looser than {@code roundCommas}' exact
	 * output, because a false NEGATIVE only leaves a stale row in place for one re-render while a false POSITIVE
	 * eats somebody's real lore line.
	 */
	private static final Pattern ROW_SHAPE = rowShape();

	private static Pattern rowShape() {
		StringBuilder names = new StringBuilder();
		for(Stat stat : ROWS) {
			if(!names.isEmpty()) names.append('|');
			names.append(Pattern.quote(stat.display()));
		}
		// apply writes the '+' unconditionally, so a negative stat would render "+-5" - hence the optional sign
		// INSIDE the plus rather than instead of it.
		return Pattern.compile("^(?:" + names + "): \\+-?[0-9][0-9,]*(?:\\.[0-9]+)?$");
	}

	/**
	 * Append this item's stat rows to its lore, if it is a registered stat item.  Returns the same stack, so it
	 * can be chained onto an item factory's return.
	 * <p>
	 * <b>The TEMPLATE rendering</b>, at the assumed default pet: this is the form every no-player caller wants -
	 * the ~40 {@code Item.freshStack} implementations, {@code plugin/FakePlayerInventory} and {@code plugin/Utils},
	 * all building palette stacks and default kits where there is nobody to ask.  A Chimera item rendered this way
	 * is showing what the item is worth with a Golden Dragon out, not a claim about whoever ends up holding it;
	 * {@link #refreshChimeraLore(Player)} corrects it once it reaches a player.
	 * <p>
	 * Idempotent by construction: the factories build a fresh stack each time and this is the only thing that adds
	 * these rows, so an item is never double-annotated.  Calling it TWICE on one stack would duplicate them, which
	 * is what {@link #strip} exists for.  {@code loadout/ItemRefresh} re-matches saved loadouts against the live
	 * factories on every editor open, so a change to a stat term propagates to everyone's saved kit with no
	 * migration - provided line 0 is untouched.
	 */
	public static ItemStack apply(ItemStack item) {
		return apply(item, Pet.GOLDEN_DRAGON);
	}

	/**
	 * The same rendering at a given pet.  The pet reaches one term and one term only - Chimera's copy - so for
	 * everything else this is {@link #apply(ItemStack)} with extra steps.
	 */
	public static ItemStack apply(ItemStack item, Pet pet) {
		ItemDef def = Items.of(item);
		if(def == null) return item;
		StatBlock stats = def.stats(pet);
		if(stats.isEmpty()) return item;

		ItemMeta meta = item.getItemMeta();
		if(meta == null) return item;
		List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(Objects.requireNonNull(meta.lore()));
		// An item with no ability carries no lore ID, so it has no lore at all - the armour pieces and the
		// wearable heads.  Its palette key's third component is therefore the empty string, and appending a stat
		// row straight onto nothing would make the FIRST stat line the key instead, silently orphaning every saved
		// loadout that contains it.  A blank line 0 keeps firstLorePlain at "" and reads as a normal SkyBlock
		// tooltip separator, so the key is untouched.
		if(lore.isEmpty()) lore.add(Component.empty());
		for(Stat stat : ROWS) {
			double value = stats.get(stat);
			if(value == 0) continue;
			// No SkyBlock glyph on the end: the real client renders ❁ ☠ ✎ from a resource-pack font, and on a vanilla
			// client they come out as tofu boxes, so the stat name carries the whole meaning here.
			lore.add(Utils.mm("<gray>" + stat.display() + ": " + stat.colour() + "+" + trim(value)));
		}
		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Re-render a stack that is ALREADY carrying rows: strip what is there, then append today's.
	 * <p>
	 * {@link #apply} appends, so it cannot be used on its own for this - a second call just doubles the rows.
	 */
	public static ItemStack rerender(ItemStack item, Pet pet) {
		strip(item);
		return apply(item, pet);
	}

	/**
	 * Remove the trailing stat rows from a stack's lore, leaving everything else exactly as it was.
	 * <p>
	 * Two rules, and both matter:
	 * <ol>
	 *   <li><b>From the TAIL only</b>, stopping at the first line that is not a row.  {@link #apply} appends the
	 *       rows last and in one run, so they are always the final block; walking up from the end means a line
	 *       above them can never be reached, whatever it says.</li>
	 *   <li><b>Never line 0.</b>  That is the item ID (or armour's blank separator) and it is two thirds of
	 *       {@code Catalog.paletteKey} - eat it and every saved loadout holding the item stops matching its
	 *       template, silently and permanently.  The loop bound is {@code > 1} for that reason alone, so do not
	 *       "simplify" it to {@code > 0}.</li>
	 * </ol>
	 */
	private static void strip(ItemStack item) {
		if(item == null) return;
		ItemMeta meta = item.getItemMeta();
		if(meta == null || meta.lore() == null) return;
		List<Component> lore = new ArrayList<>(Objects.requireNonNull(meta.lore()));
		int end = lore.size();
		while(end > 1 && ROW_SHAPE.matcher(Utils.plain(lore.get(end - 1))).matches()) end--;
		if(end == lore.size()) return; // nothing recognised: an un-rendered stack, leave it alone entirely
		meta.lore(new ArrayList<>(lore.subList(0, end)));
		item.setItemMeta(meta);
	}

	/**
	 * Re-render the Chimera items in this player's inventory for the pet they have out RIGHT NOW.
	 * <p>
	 * <b>Chimera items only.</b>  Every other stat item's lore is pet-independent, so rebuilding the whole
	 * inventory would be churn at best - and writing over a player's live inventory is the machinery
	 * {@code loadout/LoadoutEditor} carries scars about.  The test is {@link ItemDef#chimera()}, never a list of
	 * weapon names: a new Chimera weapon then needs no edit here.
	 * <p>
	 * <b>Skipped while the loadout editor is open</b>, because there the player's 36 slots ARE the kit being
	 * edited and {@code LoadoutEditor.finish} saves them verbatim - a live-pet rendering written in mid-session
	 * would be baked into the saved loadout.  Nothing is lost by waiting: closing the editor is itself one of the
	 * events {@code damage/StatListener} refreshes on.
	 */
	public static void refreshChimeraLore(Player p) {
		if(p == null) return;
		// NEXT TICK, always, and that is the method's contract rather than each caller's problem.  Every caller is
		// inside something still deciding what the inventory looks like: StatListener sits at MONITOR on an
		// InventoryClickEvent, whose own result is not applied yet, and Pets.equip is reached from a menu click.
		// Writing slots from inside one of those races the event's write.  Lore is cosmetic, so a tick costs
		// nothing.  isEnabled guards the shutdown path, where the scheduler refuses new tasks.
		if(!M7tas.getInstance().isEnabled()) return;
		Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> rerenderChimeraNow(p));
	}

	/** {@link #refreshChimeraLore} once the tick it was asked on is over.  Never call this one directly. */
	private static void rerenderChimeraNow(Player p) {
		if(!p.isOnline()) return;
		if(p.getOpenInventory().getTopInventory().getHolder() instanceof loadout.LoadoutEditor.EditorHolder) return;
		// MELEE, not the path of any particular hit: lore is read outside combat, so there is no hit to take a
		// path from, and in the assumed modes the table branches on it (an ABILITY cast assumes the Crow).  The
		// non-ability default is the honest one for a tooltip.  In realistic mode the argument is dead weight
		// anyway - manualPets() short-circuits to pets/Pets above every path test in Pet.forPlayer.
		Pet pet = Pet.forPlayer(p, DamagePath.MELEE);
		PlayerInventory inv = p.getInventory();
		for(int slot = 0; slot < inv.getSize(); slot++) {
			ItemStack before = inv.getItem(slot);
			ItemDef def = Items.of(before);
			if(def == null || !def.chimera()) continue;
			ItemStack after = rerender(before.clone(), pet);
			// isSimilar ignores the amount, which the clone already carried over, so this is a full compare. Most
			// calls land here with nothing to do (the pet usually has not moved), and skipping the write keeps the
			// listener from resending an inventory slot on every click.
			if(!before.isSimilar(after)) inv.setItem(slot, after);
		}
	}

	/** Drop a trailing {@code .0} so whole numbers read as {@code +999} rather than {@code +999.0}. */
	private static String trim(double value) {
		String s = Utils.roundCommas(value, 2);
		if(s.endsWith(".00")) return s.substring(0, s.length() - 3);
		if(s.endsWith("0")) return s.substring(0, s.length() - 1);
		return s;
	}
}
