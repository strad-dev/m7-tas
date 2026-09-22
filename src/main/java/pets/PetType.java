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
 * The four pets a realistic-mode player can actually summon, and how their heads are drawn in {@code /pets}.
 * <p>
 * <b>This is presentation, and only presentation.</b>  Every figure below is the level-100 tooltip as the NEU
 * repo and the wiki's {@code Module:Pet/Data} write it; nothing reads a number back off it.  What a pet is WORTH
 * to the damage pipeline is {@link Pet} and nothing else (MAP.md §1.13), which is why the stat lines here are
 * authored strings rather than a {@code StatBlock}: a Golden Dragon's Attack Speed and Magic Find are real parts
 * of its tooltip and are not stats this plugin models, and building them into a StatBlock would imply otherwise.
 * <p>
 * <b>The SkyBlock glyphs are stripped</b> (❁ ☠ ✯ ⊙ ❈ in the source tooltips).  Hypixel renders those from a
 * resource-pack font and they come out as tofu boxes on a vanilla client, which is the same call
 * {@code damage/Stat} already made for its own stat rows.
 * <p>
 * Each constant carries the {@link Pet} it equips, so the menu and the damage pipeline can never disagree about
 * what "the Phoenix" is.  {@link Pet#BLACK_CAT} has no constant here: nobody picks it, it is what a hat forces on
 * you in the assumed modes.
 */
public enum PetType {
	/**
	 * <b>Level 100 is the Golden Dragon's MINIMUM level, not its max</b> - it hatches at 100 and levels to 200,
	 * which is why Symbiosis below talks about owning a level 200 one.  The other three here max at 100.
	 */
	GOLDEN_DRAGON(Pet.GOLDEN_DRAGON, "Golden Dragon", Rarity.LEGENDARY, "Combat Pet",
			"ewogICJ0aW1lc3RhbXAiIDogMTYyMDM1MDA5ODgyNiwKICAicHJvZmlsZUlkIiA6ICJiNWRkZTVmODJlYjM0OTkzYmMwN2Q0MGFiNWY2ODYyMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJsdXhlbWFuIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJlOWY5YjFmYzAxNDE2NmNiNDZhMDkzZTUzNDliMmJmNmVkZDIwMWI2ODBkNjJlNDhkYmYzYWY5YjA0NTkxMTYiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
			HeldPetItem.HEPHAESTUS_RELIC,
			List.of("<gray>Strength: <red>+25", "<gray>Attack Speed: <yellow>+25%", "<gray>Magic Find: <aqua>+5"),
			List.of(
					new Ability("Gold's Power", List.of(
							"Increase the potency of Midas' Sword and",
							"Midas Staff's Greed Ability by 5%.")),
					new Ability("Shining Scales", List.of(
							"Grants +11.1 Strength and +2.2 Magic Find to",
							"your pet for each digit in your Gold",
							"Collection. (Max 100M collection)")),
					new Ability("Dragon's Greed", List.of(
							"Grants +0.25% Strength per 5 Magic Find.",
							"(Max +2.5%)")),
					new Ability("Legendary Treasure", List.of(
							"Gain 0.13% damage for every million coins in",
							"your bank. (Max 125%)")),
					new Ability("Symbiosis", List.of(
							"If you own a level 200 Golden Dragon, gain +5",
							"coins per monster kill for every other unique",
							"maxed Combat Pet that you own.")))),

	ENDER_DRAGON(Pet.ENDER_DRAGON, "Ender Dragon", Rarity.LEGENDARY, "Combat Pet",
			"eyJ0aW1lc3RhbXAiOjE1MTg5ODE1ODYzNTksInByb2ZpbGVJZCI6ImQxY2VjOWFkMWRhODQxNzliMWU1NjA0ZjcyYmZiMjI2IiwicHJvZmlsZU5hbWUiOiJydXRnZXI0NjUiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FlYzNmZjU2MzI5MGIxM2ZmM2JjYzM2ODk4YWY3ZWFhOTg4YjZjYzE4ZGMyNTQxNDdmNTgzNzRhZmU5YjIxYjkifX19",
			HeldPetItem.HEPHAESTUS_RELIC,
			List.of("<gray>Strength: <red>+50", "<gray>Crit Damage: <blue>+50%", "<gray>Crit Chance: <blue>+10%"),
			List.of(
					new Ability("End Strike", List.of(
							"Deal 200% more damage to Ender mobs.")),
					new Ability("One With The Dragon", List.of(
							"Buffs the Aspect of the Dragons sword by 50",
							"Damage and 30 Strength.")),
					new Ability("Superior", List.of(
							"Increases all Combat stats and Magic Find by",
							"10%.")))),

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
	PHOENIX(Pet.PHOENIX, "Phoenix", Rarity.VERY_SPECIAL, null,
			"ewogICJ0aW1lc3RhbXAiIDogMTY0Mjg2NTc3MTM5MSwKICAicHJvZmlsZUlkIiA6ICJiYjdjY2E3MTA0MzQ0NDEyOGQzMDg5ZTEzYmRmYWI1OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJsYXVyZW5jaW8zMDMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjZiMWI1OWJjODkwYzljOTc1Mjc3ODdkZGUyMDYwMGM4Yjg2ZjZiOTkxMmQ1MWE2YmZjZGIwZTRjMmFhM2M5NyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
			HeldPetItem.HEPHAESTUS_PLUSHIE,
			List.of("<gray>Strength: <red>+60", "<gray>Intelligence: <aqua>+150"),
			List.of(
					new Ability("Rekindle", List.of(
							"Before death, become immune and gain 30",
							"Strength for 4 seconds. 60s cooldown")),
					new Ability("Fourth Flare", List.of(
							"On 4th melee strike, ignite mobs, dealing 15x",
							"your Crit Damage each second for 5 seconds")),
					new Ability("Magic Bird", List.of(
							"You may always fly on your private island and",
							"garden")),
					new Ability("Eternal Coins", List.of(
							"Don't lose coins from death.")))),

	CROW(Pet.CROW, "Crow", Rarity.LEGENDARY, "Combat Pet",
			"ewogICJ0aW1lc3RhbXAiIDogMTc2MjgwMjExODUxMCwKICAicHJvZmlsZUlkIiA6ICJmODJmNTQ1MDIzZDA0MTFkYmVlYzU4YWI4Y2JlMTNjNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZXNwb25kZW50cyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMzFlN2UxYzYxNDYzOWJiYzk1YTBkODFmZjdlNWY3ZjYwNmQ5YjMzMTE1Y2YyYmYyYTQwY2RjYmJiNTMxMzI0IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
			HeldPetItem.HEPHAESTUS_RELIC,
			List.of("<gray>Intelligence: <aqua>+150", "<gray>Ability Damage: <red>+20"),
			List.of(
					new Ability("Quick Hands", List.of(
							"Lowers the cooldown of your Weapon abilities",
							"by +15%.")),
					new Ability("Camouflage", List.of(
							"After casting an ability, increase your",
							"Defense by +20% for 20 seconds. (Capped at 500",
							"Defense)")),
					new Ability("Insightful", List.of(
							"Gives a 15% chance to not consume Mana when",
							"using an ability."))));

	/**
	 * One named pet ability: the gold header line and the grey lines under it.
	 * <p>
	 * The lines are pre-wrapped rather than word-wrapped at render time.  A tooltip has no width the server can
	 * measure (the client's font and GUI scale decide it), so a wrapper here would be guessing; breaking each
	 * description by hand is how the rest of the plugin writes multi-line lore.
	 */
	public record Ability(String name, List<String> lines) {}

	/** The level every pet here is assumed to be at.  See {@link #GOLDEN_DRAGON} for the one that is a floor. */
	public static final int LEVEL = 100;

	private final Pet pet;
	private final String displayName;
	private final Rarity rarity;
	/**
	 * The "Combat Pet" line above the stats, or null for a pet whose tooltip has none.  The Phoenix is the only
	 * one: the tooltip this was transcribed from does not carry the line, and inventing it would be authoring
	 * data rather than copying it.
	 */
	private final String petTypeLine;
	private final String texture;
	private final HeldPetItem held;
	private final List<String> statLines;
	private final List<Ability> abilities;

	PetType(Pet pet, String displayName, Rarity rarity, String petTypeLine, String texture, HeldPetItem held,
			List<String> statLines, List<Ability> abilities) {
		this.pet = pet;
		this.displayName = displayName;
		this.rarity = rarity;
		this.petTypeLine = petTypeLine;
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

	/** The name as it appears everywhere a pet is mentioned: {@code [Lvl 100] Name} in the rarity's colour. */
	public String colouredName() {
		return "<" + rarity.colour() + ">[Lvl " + LEVEL + "] " + displayName;
	}

	/** The menu head this pet is drawn as.  {@code equipped} decides the glint and the last lore line. */
	public ItemStack icon(boolean equipped, boolean editing) {
		// ItemFactory.head, not a second copy of the profile/NBT assembly: that duplication is what CLAUDE.md
		// warns makes saved stacks stop matching, and it is the same builder every custom head in the plugin uses.
		// Two things it does for a WEARABLE head have to come back off for a menu icon, though:
		//   - it adds Protection 5, which would print an enchantment line and glint every pet unconditionally;
		//   - StatLore.apply runs, which is a no-op here since no pet is a registered stat item.
		// The signature is null because these four textures were taken from the texture URL rather than from a
		// signed profile.  A head renders from the value alone on this server, which never authenticates them.
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
		if(petTypeLine != null) out.add("<gray>" + petTypeLine);
		out.add("");
		out.addAll(statLines);
		for(Ability a : abilities) {
			out.add("");
			out.add("<gold>" + a.name() + "<gray>:");
			for(String line : a.lines()) out.add("<gray>" + line);
		}
		out.add("");
		out.add("<gray>Held Item: " + held.colouredName());
		out.add("<gray>" + held.effect());
		out.add("");
		// SkyBlock's rarity footer, with PET on the end the way a pet's own tooltip writes it.
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

	/** The menu pet backing a damage-side {@link Pet}, or null for one nobody can pick (the Black Cat). */
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
