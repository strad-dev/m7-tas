package pets;

import damage.Pet;
import damage.Rarity;
import items.ItemFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * The five pets a realistic-mode player can summon, and how their heads are drawn in {@code /pets}.
 * <p>
 * <b>This is presentation, and only presentation.</b>  Every line below is transcribed from the real in-game
 * item; nothing reads a number back off it.  What a pet is WORTH to the damage pipeline is {@link Pet} and
 * nothing else (MAP.md §1.13), which is why the stat lines here are authored strings rather than a
 * {@code StatBlock}: a Golden Dragon's Attack Speed and Magic Find are real parts of its tooltip and are not
 * stats this plugin models, and building them into a StatBlock would imply otherwise.
 * <p>
 * <b>The stats shown are the ones the HELD ITEM has already been applied to</b>, exactly as the game shows them.
 * Four of the five hold a Hephaestus Relic, so their figures are the base x 1.5 and match {@link Pet} line for
 * line: the Crow's +225 Intelligence is 150 x 1.5, the Ender Dragon's +90 Crit Damage is 60 x 1.5.  The Black
 * Cat's Unalloyed Speed multiplies nothing, so its numbers are raw.  Change a held item and the stat lines here
 * and the figures in {@link Pet} both move.
 * <p>
 * <b>The SkyBlock glyphs are stripped</b> (the PUA codepoints behind ✦ ✯ ♣ ❁ ☠ ⊙ ❈ in the source tooltips).
 * Hypixel renders those from a resource-pack font and they come out as tofu boxes on a vanilla client, which is
 * the same call {@code damage/Stat} already made for its own stat rows.
 * <p>
 * Each constant carries the {@link Pet} it equips, so the menu and the damage pipeline can never disagree about
 * what "the Phoenix" is.  {@link Pet#BLACK_CAT} is reachable two ways and they do not conflict: a hat forces it
 * on you in the assumed modes, and it is pickable here in realistic.
 */
public enum PetType {
	/**
	 * <b>Level 200, not 100.</b>  The Golden Dragon hatches at 100 and levels to 200, and every figure this
	 * plugin uses for it is the level-200 one - {@link Pet#GOLDEN_DRAGON}'s +300 Strength is a level-200 number,
	 * so showing a level-100 tooltip next to it would be showing a pet the model does not use.
	 */
	GOLDEN_DRAGON(Pet.GOLDEN_DRAGON, "Golden Dragon", Rarity.LEGENDARY, 200,
			"ewogICJ0aW1lc3RhbXAiIDogMTYyMDM1MDA5ODgyNiwKICAicHJvZmlsZUlkIiA6ICJiNWRkZTVmODJlYjM0OTkzYmMwN2Q0MGFiNWY2ODYyMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJsdXhlbWFuIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJlOWY5YjFmYzAxNDE2NmNiNDZhMDkzZTUzNDliMmJmNmVkZDIwMWI2ODBkNjJlNDhkYmYzYWY5YjA0NTkxMTYiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
			HeldPetItem.HEPHAESTUS_RELIC,
			List.of("<gray>Strength: <red>+300", "<gray>Attack Speed: <yellow>+50%", "<gray>Magic Find: <aqua>+30"),
			List.of(
					new Ability("Gold's Power", List.of(
							"Increase the potency of Midas'",
							"Sword and Midas Staff's Greed Ability",
							"by 10%.")),
					new Ability("Shining Scales", List.of(
							"Grants +11.1 Strength and +2.2",
							"Magic Find to your pet for each digit",
							"in your Gold Collection.",
							"(Max 100M collection)")),
					new Ability("Dragon's Greed", List.of(
							"Grants +0.5% Strength per 5",
							"Magic Find. (Max +5%)")),
					new Ability("Legendary Treasure", List.of(
							"Gain 0.25% damage for every million",
							"coins in your bank. (Max 250%)")),
					new Ability("Symbiosis", List.of(
							"If you own a level 200 Golden",
							"Dragon, gain +5 coins per monster",
							"kill for every other unique maxed",
							"Combat Pet that you own.")))),

	ENDER_DRAGON(Pet.ENDER_DRAGON, "Ender Dragon", Rarity.LEGENDARY, 100,
			"eyJ0aW1lc3RhbXAiOjE1MTg5ODE1ODYzNTksInByb2ZpbGVJZCI6ImQxY2VjOWFkMWRhODQxNzliMWU1NjA0ZjcyYmZiMjI2IiwicHJvZmlsZU5hbWUiOiJydXRnZXI0NjUiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FlYzNmZjU2MzI5MGIxM2ZmM2JjYzM2ODk4YWY3ZWFhOTg4YjZjYzE4ZGMyNTQxNDdmNTgzNzRhZmU5YjIxYjkifX19",
			HeldPetItem.HEPHAESTUS_RELIC,
			List.of("<gray>Strength: <red>+75", "<gray>Crit Chance: <blue>+15%", "<gray>Crit Damage: <blue>+90%"),
			List.of(
					new Ability("End Strike", List.of(
							"Deal 200% more damage to Ender",
							"mobs.")),
					new Ability("One With The Dragon", List.of(
							"Buffs the Aspect of the Dragons",
							"sword by 50 Damage and 30",
							"Strength.")),
					new Ability("Superior", List.of(
							"Increases all Combat stats and",
							"Magic Find by 10%.")))),

	/**
	 * <b>VERY SPECIAL is deliberate, and live SkyBlock still shows LEGENDARY.</b>  The wiki's Alpha Hypixel
	 * Network changelog dated 2026-09-21 records "Changed rarity of the Phoenix Pet from Epic/Legendary to
	 * Special/Very Special", so this is where the item is going and it is the tier the owner asked for.  <b>Do not
	 * "fix" it back to LEGENDARY</b> - the stat block below is still the LEGENDARY one, because that is the set of
	 * numbers the change moved across unaltered.
	 * <p>
	 * A red tier is not free elsewhere: {@code Rarity.SPECIAL}/{@code VERY_SPECIAL} never recombobulate and need
	 * their own reforge row.  Neither applies here, because a pet is not a reforgeable item.
	 */
	PHOENIX(Pet.PHOENIX, "Phoenix", Rarity.VERY_SPECIAL, 100,
			"ewogICJ0aW1lc3RhbXAiIDogMTY0Mjg2NTc3MTM5MSwKICAicHJvZmlsZUlkIiA6ICJiYjdjY2E3MTA0MzQ0NDEyOGQzMDg5ZTEzYmRmYWI1OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJsYXVyZW5jaW8zMDMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjZiMWI1OWJjODkwYzljOTc1Mjc3ODdkZGUyMDYwMGM4Yjg2ZjZiOTkxMmQ1MWE2YmZjZGIwZTRjMmFhM2M5NyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
			HeldPetItem.HEPHAESTUS_RELIC,
			List.of("<gray>Strength: <red>+90", "<gray>Intelligence: <aqua>+225"),
			List.of(
					new Ability("Rekindle", List.of(
							"Before death, become immune and",
							"gain 30 Strength for 4 seconds",
							"60s cooldown")),
					new Ability("Fourth Flare", List.of(
							"On 4th melee strike, ignite mobs,",
							"dealing 15x your Crit Damage",
							"each second for 5 seconds")),
					new Ability("Magic Bird", List.of(
							"You may always fly on your private",
							"island and garden")),
					new Ability("Eternal Coins", List.of(
							"Don't lose coins from death.")))),

	CROW(Pet.CROW, "Crow", Rarity.LEGENDARY, 100,
			"ewogICJ0aW1lc3RhbXAiIDogMTc2MjgwMjExODUxMCwKICAicHJvZmlsZUlkIiA6ICJmODJmNTQ1MDIzZDA0MTFkYmVlYzU4YWI4Y2JlMTNjNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZXNwb25kZW50cyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMzFlN2UxYzYxNDYzOWJiYzk1YTBkODFmZjdlNWY3ZjYwNmQ5YjMzMTE1Y2YyYmYyYTQwY2RjYmJiNTMxMzI0IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
			HeldPetItem.HEPHAESTUS_RELIC,
			List.of("<gray>Intelligence: <aqua>+225", "<gray>Ability Damage: <red>+30%"),
			List.of(
					new Ability("Quick Hands", List.of(
							"Lowers the cooldown of your Weapon",
							"abilities by 15%.")),
					new Ability("Camouflage", List.of(
							"After casting an ability, increase",
							"your Defense by +20 for 20",
							"seconds.",
							"Capped at 500 Defense")),
					new Ability("Insightful", List.of(
							"Gives a 15% chance to not consume",
							"Mana when using an ability.")))),

	/**
	 * The one pet here that is not a damage pick.  Its Intelligence is real ({@link Pet#BLACK_CAT}) but the rest
	 * of its tooltip - Speed, Magic Find, Pet Luck and the speed cap its held item raises - is nothing this plugin
	 * models, so summoning it is a deliberate trade of a dragon's Strength for a mana pool.
	 * <p>
	 * <b>Speed +125 already contains Hunter's +100</b>, which is why the two are not added together: Hypixel
	 * folded the Hunter, Omen and Supernatural amounts into the displayed base stats in 0.18.5, so the ability
	 * lines restate them rather than stacking on top.
	 */
	BLACK_CAT(Pet.BLACK_CAT, "Black Cat", Rarity.MYTHIC, 100,
			"ewogICJ0aW1lc3RhbXAiIDogMTcwODczNzEyMTIzNSwKICAicHJvZmlsZUlkIiA6ICJmY2ZhYTg0MzA0YjE0NDUxOThkNWYxNzQ3ZjI0Y2Q5MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdGV3eVdvbGZ5IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzgyODJiNWE5YmJlMmNkMzIyMzcyNDAyM2NkNGY2YWQ0MTNmNWJiOWUwZWRlZjgxNzAwYjhhZmMzMDcyZDA0YTUiCiAgICB9CiAgfQp9",
			HeldPetItem.UNALLOYED_SPEED,
			List.of("<gray>Intelligence: <aqua>+100", "<gray>Speed: <white>+125",
					"<gray>Magic Find: <aqua>+15", "<gray>Pet Luck: <light_purple>+15"),
			List.of(
					new Ability("Hunter", List.of(
							"Increases your Speed and speed",
							"cap by +100.")),
					new Ability("Omen", List.of(
							"Grants +15 Pet Luck.")),
					new Ability("Supernatural", List.of(
							"Grants +15 Magic Find.")),
					new Ability("Looting", List.of(
							"Gain 15% more collection items from",
							"monsters!"))));

	/**
	 * One named pet ability: the gold header line and the grey lines under it.
	 * <p>
	 * The lines are pre-wrapped rather than word-wrapped at render time.  A tooltip has no width the server can
	 * measure (the client's font and GUI scale decide it), so a wrapper here would be guessing; breaking each
	 * description by hand is how the rest of the plugin writes multi-line lore.
	 */
	public record Ability(String name, List<String> lines) {}

	/** Every pet here is a Combat Pet, which is the line the real tooltip opens with. */
	private static final String PET_TYPE_LINE = "Combat Pet";

	private final Pet pet;
	private final String displayName;
	private final Rarity rarity;
	/** The level this pet is shown at.  100 for four of them; see {@link #GOLDEN_DRAGON} for the one that is 200. */
	private final int level;
	private final String texture;
	private final HeldPetItem held;
	private final List<String> statLines;
	private final List<Ability> abilities;

	PetType(Pet pet, String displayName, Rarity rarity, int level, String texture, HeldPetItem held,
			List<String> statLines, List<Ability> abilities) {
		this.pet = pet;
		this.displayName = displayName;
		this.rarity = rarity;
		this.level = level;
		this.texture = texture;
		this.held = held;
		this.statLines = statLines;
		this.abilities = abilities;
	}

	/** The damage-side constant this pet IS.  One direction only; {@link #of(Pet)} is the way back. */
	public Pet pet() {
		return pet;
	}

	public String displayName() {
		return displayName;
	}

	public Rarity rarity() {
		return rarity;
	}

	/**
	 * The name as it appears everywhere a pet is mentioned: {@code [Lvl N] Name}.
	 * <p>
	 * <b>Two colours, not one.</b>  The level bracket is always grey and the rarity colour belongs to the NAME -
	 * {@code <gray>[Lvl 200] <gold>Golden Dragon}.  This used to paint the whole string in the rarity colour,
	 * which made the level read as part of it.
	 */
	public String colouredName() {
		return "<gray>[Lvl " + level + "] <" + rarity.colour() + ">" + displayName;
	}

	/** The menu head this pet is drawn as.  {@code equipped} decides the glint and the last lore line. */
	public ItemStack icon(boolean equipped, boolean editing) {
		// ItemFactory.head, not a second copy of the profile/NBT assembly: that duplication is what CLAUDE.md
		// warns makes saved stacks stop matching, and it is the same builder every custom head in the plugin uses.
		// Two things it does for a WEARABLE head have to come back off for a menu icon, though:
		//   - it adds Protection 5, which would print an enchantment line and glint every pet unconditionally;
		//   - StatLore.apply runs, which is a no-op here since no pet is a registered stat item.
		// The signature is null: a head renders from the texture value alone on this server, which never
		// authenticates a profile, so carrying one per pet would be dead weight in the enum.
		ItemStack head = ItemFactory.head(colouredName(), "pet" + name(), texture, null);
		for(Enchantment e : new ArrayList<>(head.getEnchantments().keySet())) head.removeEnchantment(e);

		ItemMeta meta = head.getItemMeta();
		if(meta == null) return head;
		// Component-based glint, which is what the rest of the codebase uses to mark a chosen item
		// (GoldorTerminalGui.glint, and loadout/ItemRefresh already tests for the override).  Set either way
		// rather than only when true, so the flag is never left over from a stack built elsewhere.
		meta.setEnchantmentGlintOverride(equipped);
		meta.lore(lore(equipped, editing));
		head.setItemMeta(meta);
		return head;
	}

	/** The pet's whole tooltip: type, stats, abilities, held item, rarity footer, then what a click does. */
	private List<Component> lore(boolean equipped, boolean editing) {
		List<String> out = new ArrayList<>();
		out.add("<dark_gray>" + PET_TYPE_LINE);
		out.add("");
		out.addAll(statLines);
		for(Ability a : abilities) {
			out.add("");
			out.add("<gold>" + a.name());
			for(String line : a.lines()) out.add("<gray>" + line);
		}
		out.add("");
		// The label is gold and the item's name carries ITS OWN rarity colour, which is not the pet's: a MYTHIC
		// Black Cat holding an EPIC Unalloyed Speed prints a light_purple name over a dark_purple one.
		out.add("<gold>Held Item: " + held.colouredName());
		out.add("<gray>" + held.effect());
		out.add("");
		// SkyBlock's rarity footer, with PET on the end the way a pet's own tooltip writes it.  The real item
		// drops this in favour of a tooltip_style component, which a server cannot set per stack, so it stays.
		out.add("<" + rarity.colour() + "><bold>" + rarity.display() + " PET");
		out.add("");
		if(editing) {
			out.add("<yellow>Click to pick this pet up");
		} else if(equipped) {
			out.add("<green><bold>CURRENTLY SUMMONED");
		} else {
			out.add("<yellow>Click to summon!");
		}

		List<Component> rendered = new ArrayList<>(out.size());
		for(String line : out) rendered.add(Utils.mm(line));
		return rendered;
	}

	/** The menu pet backing a damage-side {@link Pet}, or null if that pet has no menu entry. */
	public static PetType of(Pet pet) {
		for(PetType t : values()) if(t.pet == pet) return t;
		return null;
	}

	/** Parse a stored enum name back, or null if the file holds a pet that no longer exists. */
	public static PetType parse(String name) {
		if(name == null) return null;
		for(PetType t : values()) if(t.name().equals(name)) return t;
		return null;
	}
}
