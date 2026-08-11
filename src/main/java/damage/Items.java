package damage;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every item's stat definition, authored as TERMS (DAMAGE_PLAN.md §1.1-1.10 via §2.4).  This is the one place
 * item numbers live.
 * <p>
 * <b>Keyed on the PLAIN DISPLAY NAME</b>, not the lore ID: armour carries no lore ID at all, and the two Hyperions
 * share one.  The name already encodes the reforge, which is exactly the axis that distinguishes the variants
 * ("Heroic Hyperion" vs "Withered Hyperion" off one base, §2.4).  An item whose name is not registered contributes
 * nothing - true of the utility items (Dungeonbreaker, Infinileap, the TNT, the wands' non-stat cousins), which is
 * correct rather than a gap.
 * <p>
 * <b>Cross-checks that the tables are right, not coincidences.</b> Every total in §1 falls out of these terms
 * without being authored: Heroic Hyperion 340/235/100/860, Fabled Hyperion 340/560/162/480, Terminator
 * 340/119/325, Dark Claymore 530/510/274, Last Breath 240/259/125, Ice Spray 192/70/100/749.5, Aspect of the Void
 * 315/295/100/174, Ragnarock 270/626/100.  If a change here stops reproducing one of those, the change is wrong.
 * The one deliberate departure is the Flaming Flay, which now carries Chimera (230/692/150, not §1.8's 392
 * Strength) - see its entry.
 */
public final class Items {
	private Items() {}

	private static final Map<String, ItemDef> BY_NAME = new LinkedHashMap<>();

	private static void register(ItemDef def) {
		BY_NAME.put(def.displayName(), def);
	}

	static {
		// ============================== §1.1 Hyperion - Heroic and Fabled ==============================
		// One base, two reforges, two gem sets.  Legendary -> recombed Mythic.
		register(ItemDef.of("Heroic Hyperion", ItemCategory.SWORD)
				.loreId("skyblock/combat/scylla").rarity(Rarity.LEGENDARY)
				.base(Stat.DAMAGE, 260).cataLevel(Stat.DAMAGE, 50)
				.base(Stat.STRENGTH, 150)
				.base(Stat.INTELLIGENCE, 350).cataLevel(Stat.INTELLIGENCE, 100)
				.with(Upgrade.POTATO_BOOKS, Upgrade.ART_OF_WAR, Upgrade.CRITICAL)
				.reforge(ReforgeId.HEROIC)
				// 1 Sapphire slot + 1 Combat slot; the Heroic build puts a Sapphire in the Combat slot too (§2.3).
				.typedGem(Gemstones.Type.SAPPHIRE).combatGem(Gemstones.Type.SAPPHIRE)
				.chimera()
				.ability(10_000, 0.3) // Wither Impact (§7)
				.build());
		register(ItemDef.of("Withered Hyperion", ItemCategory.SWORD)
				.loreId("skyblock/combat/scylla").rarity(Rarity.LEGENDARY)
				.base(Stat.DAMAGE, 260).cataLevel(Stat.DAMAGE, 50)
				.base(Stat.STRENGTH, 150)
				.base(Stat.INTELLIGENCE, 350).cataLevel(Stat.INTELLIGENCE, 100)
				.with(Upgrade.POTATO_BOOKS, Upgrade.ART_OF_WAR, Upgrade.CRITICAL)
				.reforge(ReforgeId.FABLED)
				.typedGem(Gemstones.Type.SAPPHIRE).combatGem(Gemstones.Type.ONYX)
				.chimera()
				.ability(10_000, 0.3)
				.build());

		// ============================== §1.2 Terminator - Precise ==============================
		// A SHORTBOW: never drawn, so draw scaling never applies and every shot is a full-damage crit.
		register(ItemDef.of("Precise Terminator", ItemCategory.RANGED)
				.loreId("skyblock/combat/terminator").rarity(Rarity.LEGENDARY)
				.base(Stat.DAMAGE, 310)
				.base(Stat.STRENGTH, 50)
				.base(Stat.CRIT_DAMAGE, 250)
				.with(Upgrade.POTATO_BOOKS, Upgrade.ART_OF_WAR, Upgrade.OVERLOAD)
				.reforge(ReforgeId.PRECISE)
				.shortbow()
				.build());

		// ============================== §1.3 Dark Claymore - Fabled ==============================
		register(ItemDef.of("Withered Dark Claymore", ItemCategory.SWORD)
				.loreId("skyblock/combat/claymore").rarity(Rarity.LEGENDARY)
				.base(Stat.DAMAGE, 500)
				.base(Stat.STRENGTH, 100)
				.base(Stat.CRIT_DAMAGE, 100)
				.with(Upgrade.POTATO_BOOKS, Upgrade.ART_OF_WAR, Upgrade.CRITICAL)
				.reforge(ReforgeId.FABLED)
				.combatGem(Gemstones.Type.ONYX).combatGem(Gemstones.Type.ONYX)
				.chimera()
				.build());

		// ============================== §1.4 Last Breath - Precise ==============================
		// A DRAWN bow: damage scales by the vanilla charge fraction, and a partial draw loses the crit term
		// entirely (§1.4).  Both live in Damage's bow path, not here.
		register(ItemDef.of("Precise Last Breath", ItemCategory.RANGED)
				.loreId("skyblock/combat/last_breath").rarity(Rarity.LEGENDARY)
				.base(Stat.DAMAGE, 210)
				.base(Stat.STRENGTH, 190)
				.base(Stat.CRIT_DAMAGE, 50)
				.with(Upgrade.POTATO_BOOKS, Upgrade.ART_OF_WAR, Upgrade.OVERLOAD)
				.reforge(ReforgeId.PRECISE)
				.build());

		// ============================== §1.5 Ice Spray Wand - Heroic ==============================
		// Note it carries NO Art of War: §1.5's Strength is 30 potato + 40 reforge, full stop.
		register(ItemDef.of("Heroic Ice Spray Wand", ItemCategory.SWORD)
				.loreId("skyblock/combat/ice_spray").rarity(Rarity.EPIC)
				.base(Stat.DAMAGE, 108).cataLevel(Stat.DAMAGE, 54)
				.base(Stat.INTELLIGENCE, 267).cataLevel(Stat.INTELLIGENCE, 133.5)
				.with(Upgrade.POTATO_BOOKS, Upgrade.CRITICAL)
				.reforge(ReforgeId.HEROIC)
				.typedGem(Gemstones.Type.SAPPHIRE)
				.chimera()
				.ability(19_000, 0.1) // Ice Spray (§7).  Its cast also debuffs everything within 8 blocks.
				.build());

		// ============================== §1.6 Aspect of the Void - Warped, NOT dungeon-scaled ==============
		register(ItemDef.of("Warped Aspect of the Void", ItemCategory.SWORD)
				.loreId("skyblock/combat/aotv").rarity(Rarity.EPIC).notDungeon()
				.base(Stat.DAMAGE, 120)
				.base(Stat.STRENGTH, 100)
				.with(Upgrade.POTATO_BOOKS, Upgrade.CRITICAL)
				.reforge(ReforgeId.WARPED)
				.typedGem(Gemstones.Type.SAPPHIRE)
				.build());

		// ============================== §1.7 Ragnarock Axe - Withered, NOT dungeon-scaled ==============
		// Its reforge really IS Withered, a different reforge that happens to share the Fabled alias's display
		// name, so it does NOT get Fabled's x1.15 (§1.0.6).  Rare -> recombed Epic, pinned by its +11 Jasper.
		register(ItemDef.of("Withered Ragnarock Axe", ItemCategory.SWORD)
				.loreId("skyblock/combat/rag").rarity(Rarity.RARE).notDungeon()
				.base(Stat.DAMAGE, 200).stars(Stat.DAMAGE, 40)
				.base(Stat.STRENGTH, 100).stars(Stat.STRENGTH, 20)
				.with(Upgrade.POTATO_BOOKS, Upgrade.ART_OF_WAR, Upgrade.CRITICAL)
				.reforge(ReforgeId.WITHERED)
				.combatGem(Gemstones.Type.JASPER)
				.chimera()
				.build());

		// ============================== §1.8 Flaming Flay - Fabled ==============================
		// §1.8's table omitted a Chimera term (its 392 Strength is base + books + art of war + gems + reforge
		// only), which read as a contradiction with §7's "every sword runs Chimera".  Ruled by the owner in favour
		// of §7: it carries Chimera like the other Fabled swords, so a Golden Dragon adds +300 to its Strength -
		// and being an ITEM source that copy is cata-scaled, worth +1998.  Strength is therefore 692 -> 4608.72,
		// not §1.8's 392 -> 2610.72.
		register(ItemDef.of("Withered Flaming Flay", ItemCategory.SWORD)
				.loreId("skyblock/combat/flaming_flay").rarity(Rarity.LEGENDARY)
				.base(Stat.DAMAGE, 200)
				.base(Stat.STRENGTH, 250)
				.with(Upgrade.POTATO_BOOKS, Upgrade.ART_OF_WAR, Upgrade.CRITICAL)
				.reforge(ReforgeId.FABLED)
				.combatGem(Gemstones.Type.JASPER).combatGem(Gemstones.Type.JASPER)
				.chimera()
				.build());

		// ============================== §1.9 The five deferred weapons ==============================
		// Base item stats from the wiki, reforges assigned by the owner, read at the RECOMBED rarity.  None of
		// them carries gemstones, and books/stars are deliberately still unassigned.
		register(ItemDef.of("Heroic Bonzo Staff", ItemCategory.SWORD)
				.loreId("skyblock/combat/bonzo").rarity(Rarity.RARE)
				.base(Stat.DAMAGE, 160)
				.base(Stat.INTELLIGENCE, 250)
				.reforge(ReforgeId.HEROIC)
				.ability(1000, 0.2) // Showtime
				.build());
		// The "gun" takes the sword reforge table; ItemCategory is the table axis, not the Bukkit material.
		register(ItemDef.of("Heroic Jerry-chine Gun", ItemCategory.SWORD)
				.loreId("skyblock/combat/jerrychine").rarity(Rarity.EPIC).notDungeon()
				.base(Stat.DAMAGE, 80)
				.base(Stat.INTELLIGENCE, 200)
				.reforge(ReforgeId.HEROIC)
				.ability(500, 0.2)
				.build());
		register(ItemDef.of("Precise Explosive Bow", ItemCategory.RANGED)
				.loreId("skyblock/combat/explosive_bow").rarity(Rarity.EPIC).notDungeon()
				.base(Stat.DAMAGE, 100)
				.base(Stat.STRENGTH, 20)
				.reforge(ReforgeId.PRECISE)
				.build());
		register(ItemDef.of("Suspicious Axe of the Shredded", ItemCategory.SWORD)
				.loreId("skyblock/combat/aots").rarity(Rarity.LEGENDARY).notDungeon()
				.base(Stat.DAMAGE, 140)
				.base(Stat.STRENGTH, 115)
				.reforge(ReforgeId.SUSPICIOUS)
				.build());
		// Our Golem Sword's ability is the Y-velocity zero, a movement tech, not Iron Punch - but its stat block
		// still comes from the real item (§1.9).
		register(ItemDef.of("Suspicious Golem Sword", ItemCategory.SWORD)
				.loreId("skyblock/combat/golem_sword").rarity(Rarity.RARE).notDungeon()
				.base(Stat.DAMAGE, 80)
				.base(Stat.STRENGTH, 125)
				.reforge(ReforgeId.SUSPICIOUS)
				.build());

		// ============================== §1.10 Armour ==============================
		// No armour piece carries a Damage stat.
		// The Necron Head Bonus is x2 and ALWAYS applies here, because this plugin only ever runs M7.  It doubles
		// only the helmet's own stats, and commutes with the x6.66, so it sits on this item's own pipeline.
		register(ItemDef.of("Ancient Diamond Necron Head", ItemCategory.ARMOR)
				.rarity(Rarity.LEGENDARY)
				.base(Stat.STRENGTH, 40)
				.with(Upgrade.BIG_BRAIN)
				.reforge(ReforgeId.ANCIENT)
				.selfMultiplier(2.0)
				.build());
		register(necronPiece("Ancient Necron's Chestplate").with(Upgrade.REFLECTION).build());
		register(necronPiece("Ancient Necron's Leggings").with(Upgrade.SMARTY_PANTS).build());
		register(necronPiece("Ancient Necron's Boots").build());

		register(ancientStorm("Ancient Storm's Helmet", 400).with(Upgrade.BIG_BRAIN).build());
		register(ancientStorm("Ancient Storm's Chestplate", 250).with(Upgrade.REFLECTION).build());
		register(ancientStorm("Ancient Storm's Leggings", 250).with(Upgrade.SMARTY_PANTS).build());
		register(ancientStorm("Ancient Storm's Boots", 250).build());

		// The ability loadout's helmet: Intelligence and the only Ability Damage on any armour piece.
		register(ItemDef.of("Necrotic Wither Goggles", ItemCategory.ARMOR)
				.rarity(Rarity.EPIC)
				.base(Stat.INTELLIGENCE, 300)
				.base(Stat.ABILITY_DAMAGE, 45)
				.with(Upgrade.BIG_BRAIN)
				.reforge(ReforgeId.NECROTIC)
				.build());

		// Alternate-reforge Storm's ("RCM"): Intelligence only - no Strength, no Crit Damage - trading the Ancient
		// reforge's 35/62 for a much larger Intelligence block, and both slots run Sapphire instead of Sapphire+Onyx.
		register(altStorm("Loving Storm's Chestplate", ReforgeId.LOVING).with(Upgrade.REFLECTION).build());
		register(altStorm("Necrotic Storm's Leggings", ReforgeId.NECROTIC).with(Upgrade.SMARTY_PANTS).build());
		register(altStorm("Necrotic Storm's Boots", ReforgeId.NECROTIC).build());

		// Wearable hotbar heads.  The Ancient reforge is rarity-keyed, which is why the Mythic Spirit Mask gets
		// 35/50/25 where the Epic Bonzo's Mask gets 18/50/16 - exactly the behaviour the table model wants.
		register(ItemDef.of("Ancient Spirit Mask", ItemCategory.ARMOR)
				.rarity(Rarity.LEGENDARY)
				.base(Stat.INTELLIGENCE, 25)
				.with(Upgrade.BIG_BRAIN)
				.reforge(ReforgeId.ANCIENT)
				.build());
		register(ItemDef.of("Ancient Bonzo's Mask", ItemCategory.ARMOR)
				.rarity(Rarity.RARE)
				.base(Stat.INTELLIGENCE, 150)
				.with(Upgrade.BIG_BRAIN)
				.reforge(ReforgeId.ANCIENT)
				.build());

		// Not dungeon items, so nothing scales and their own stats are ZERO at this scale (§1.10).  They are
		// registered anyway so Profile can count them for the Renowned +1%-per-piece additive, and so the old
		// x0.70/x0.80 worn-item damage penalties stay deleted rather than quietly reappearing: a wearable now
		// affects damage only through the stats it contributes, and these contribute none.
		for(String renowned : List.of("Renowned Spring Boots", "Renowned Racing Helmet", "Renowned Cow Hat",
				"Renowned Thermodynamic Helmet", "Renowned Thermodynamic Chestplate",
				"Renowned Thermodynamic Leggings", "Renowned Thermodynamic Boots")) {
			register(ItemDef.of(renowned, ItemCategory.ARMOR).rarity(Rarity.EPIC).notDungeon()
					.reforge(ReforgeId.RENOWNED).build());
		}
	}

	/** The three Ancient Necron's body pieces: identical but for their one Intelligence enchantment. */
	private static ItemDef.Builder necronPiece(String name) {
		return ItemDef.of(name, ItemCategory.ARMOR)
				.rarity(Rarity.LEGENDARY)
				.base(Stat.STRENGTH, 40)
				.base(Stat.CRIT_DAMAGE, 30)
				.base(Stat.INTELLIGENCE, 10)
				.reforge(ReforgeId.ANCIENT)
				.combatGem(Gemstones.Type.JASPER).combatGem(Gemstones.Type.JASPER);
	}

	/** The four Ancient Storm's pieces, which differ only in base Intelligence and their enchantment. */
	private static ItemDef.Builder ancientStorm(String name, double baseIntelligence) {
		return ItemDef.of(name, ItemCategory.ARMOR)
				.rarity(Rarity.LEGENDARY)
				.base(Stat.INTELLIGENCE, baseIntelligence)
				.reforge(ReforgeId.ANCIENT)
				// Two slots: one typed Sapphire, one Combat.  The alternates below put a Sapphire in the Combat
				// slot as well, which is what makes their 2 x Sapphire legal in the same two slots (§2.3).
				.typedGem(Gemstones.Type.SAPPHIRE).combatGem(Gemstones.Type.ONYX);
	}

	/** A Loving/Necrotic Storm's piece: the same base, both slots Sapphire, and no Strength or Crit Damage. */
	private static ItemDef.Builder altStorm(String name, ReforgeId reforge) {
		return ItemDef.of(name, ItemCategory.ARMOR)
				.rarity(Rarity.LEGENDARY)
				.base(Stat.INTELLIGENCE, 250)
				.reforge(reforge)
				.typedGem(Gemstones.Type.SAPPHIRE).combatGem(Gemstones.Type.SAPPHIRE);
	}

	/** The definition for an ItemStack, or null if the item grants no stats (utility items, an empty slot). */
	public static ItemDef of(ItemStack item) {
		if(item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
		ItemMeta meta = item.getItemMeta();
		if(meta == null) return null;
		return BY_NAME.get(Utils.plain(meta.displayName()));
	}

	/** The definition registered under an exact display name, or null. */
	public static ItemDef byName(String displayName) {
		return BY_NAME.get(displayName);
	}

	/** Every registered definition, in authoring order.  Used by the lore renderer and the self-check. */
	public static Collection<ItemDef> all() {
		return new ArrayList<>(BY_NAME.values());
	}
}
