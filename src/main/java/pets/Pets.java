package pets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import damage.Difficulty;
import damage.Pet;
import org.bukkit.entity.Player;
import plugin.Catalog;
import plugin.M7tas;
import plugin.Utils;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who has which pet out, where their pets sit in the menu, and their autopet rules - the whole of realistic mode's
 * pet state, in memory and on disk.
 *
 * <h2>The damage pipeline's entry point</h2>
 * {@link #equippedDamagePet(Player)} is what {@code damage/Pet.forPlayer} calls the moment
 * {@link Difficulty#manualPets()} is on, and it runs <b>per stat aggregate</b> - several times a swing.  So it is
 * a map read: the file is loaded once per player (on join, or on the first miss) and everything after that is
 * {@link #CACHE}.  Nothing in this class may ever touch the disk on a read path.
 *
 * <h2>Its own file, on purpose</h2>
 * This writes {@code <data>/pets/&lt;uuid&gt;.json}, resolved through {@link Catalog#dataDir()} exactly as
 * {@code loadout/Loadouts} resolves {@code loadouts/<uuid>.json}.  <b>It must not be a few extra fields on the
 * loadout file.</b>  That file is one of only two in the shared data folder with two writers - this plugin and
 * StradDevHub - and the workspace CLAUDE.md is explicit about what that costs: StradDevHub deserialises the
 * loadout file into its own twin of {@code LoadoutFile}, which has no pet fields, so the next write it makes in
 * the lobby drops them silently.  A separate file only M7 knows about has one writer and cannot lose anything.
 *
 * <h2>Every mutator saves</h2>
 * A pet choice is worth exactly one small file write, and the alternative (save on quit) loses the lot to a crash
 * or a {@code /stop} that outruns the handler.  Writes are temp+rename, like {@code Loadouts.save}.
 */
public final class Pets {
	private Pets() {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	/**
	 * What a player has out before they have ever opened the menu.
	 * <p>
	 * The Golden Dragon, which is also what the ASSUMED table in {@code damage/Pet.forPlayer} falls through to.
	 * So turning realistic mode on changes nobody's damage until they actually pick something, which is the only
	 * default that cannot surprise a player mid-run.
	 */
	public static final PetType DEFAULT_PET = PetType.GOLDEN_DRAGON;

	/** Loaded profiles, keyed on player uuid.  The only thing any read path is allowed to consult. */
	private static final Map<UUID, Profile> CACHE = new HashMap<>();

	/**
	 * One player's pet state.  Mutable and owned by {@link Pets}: everything that changes it goes through a
	 * setter here, so there is exactly one place that also writes the file and drops the stat cache.
	 */
	public static final class Profile {
		/** Menu slot -> the pet sitting in it.  Every {@link PetType} appears at most once. */
		final Map<Integer, PetType> layout = new LinkedHashMap<>();
		/** The pet that is out.  Never null once the profile exists. */
		PetType equipped = DEFAULT_PET;
		/** Per-trigger autopet rules.  An absent trigger is off, the same as a rule with a null pet. */
		final EnumMap<Autopet.Trigger, Autopet.Rule> rules = new EnumMap<>(Autopet.Trigger.class);
		/**
		 * The Rod Swap cycle, in order.  Empty is off.
		 * <p>
		 * <b>Repeats are legal.</b>  [Golden Dragon, Crow, Golden Dragon, Phoenix] is a four-throw rotation, not
		 * a three-pet one with a mistake in it, so this is a List and nothing anywhere dedupes it.
		 */
		final List<PetType> rodCycle = new ArrayList<>();
	}

	// ==================== reads ====================

	/**
	 * Which {@link Pet} this player has out, for the damage pipeline.  <b>Never null</b>: a null here would NPE
	 * every stat aggregate, so a player with no file, an unreadable file or a file naming a deleted pet all get
	 * {@link #DEFAULT_PET}.
	 */
	public static Pet equippedDamagePet(Player p) {
		return equipped(p).pet();
	}

	/** Which pet this player has out.  Never null. */
	public static PetType equipped(Player p) {
		return p == null ? DEFAULT_PET : profile(p.getUniqueId()).equipped;
	}

	/** This player's menu layout: slot -> pet.  A live view of the profile, so callers must not mutate it. */
	public static Map<Integer, PetType> layout(Player p) {
		return profile(p.getUniqueId()).layout;
	}

	/** This player's rule for one trigger, or null when the trigger is off. */
	public static Autopet.Rule rule(Player p, Autopet.Trigger t) {
		return profile(p.getUniqueId()).rules.get(t);
	}

	/** The Rod Swap cycle, in order, repeats and all.  A live view of the profile, so callers must not mutate it. */
	public static List<PetType> rodCycle(Player p) {
		return profile(p.getUniqueId()).rodCycle;
	}

	// ==================== writes ====================

	/**
	 * Summon a pet, replacing whatever was out.
	 * <p>
	 * <b>This is the one place a pet changes</b>, so it is also the one place that drops the stat cache.  That
	 * matters because most equips are NOT inventory events: {@code damage/StatListener} invalidates on a click,
	 * a close, a held-slot change and so on, which covers a click in {@code /pets} but covers none of the four
	 * autopet triggers.  Without the call below, a rod-swap equip would keep serving the old pet's Strength for
	 * up to {@code Stats.CACHE_TICKS}, i.e. through the first swing after the swap.
	 *
	 * @return true if the pet actually changed, so a caller can stay quiet when it did not.
	 */
	public static boolean equip(Player p, PetType pet, String announcement) {
		if(p == null || pet == null) return false;
		Profile prof = profile(p.getUniqueId());
		if(prof.equipped == pet) return false;
		prof.equipped = pet;
		save(p.getUniqueId(), prof);
		damage.Stats.invalidate(p);
		// The two travel together: the cache drop fixes what the next HIT is worth, this fixes what the player's
		// Chimera weapons SAY they are worth.  Both are stale for the same reason and at the same moment.
		damage.StatLore.refreshChimeraLore(p);
		// The caller owns the whole line except the pet's name, which always renders in the pet's rarity colour.
		// No punctuation is bolted on here: the old trailing "!" landed inside a sentence autopet does not end with.
		if(announcement != null) p.sendMessage(Utils.msg(announcement + pet.colouredName()));
		return true;
	}

	/** Move the layout wholesale (the arranging menu's save).  Slots are not validated here; the menu owns that. */
	public static void setLayout(Player p, Map<Integer, PetType> layout) {
		Profile prof = profile(p.getUniqueId());
		prof.layout.clear();
		prof.layout.putAll(layout);
		save(p.getUniqueId(), prof);
	}

	/** Put the five pets back on the first five valid slots. */
	public static void resetLayout(Player p) {
		Profile prof = profile(p.getUniqueId());
		prof.layout.clear();
		prof.layout.putAll(defaultLayout());
		save(p.getUniqueId(), prof);
	}

	public static void setRule(Player p, Autopet.Trigger t, Autopet.Rule rule) {
		Profile prof = profile(p.getUniqueId());
		if(rule == null || rule.isOff()) prof.rules.remove(t);
		else prof.rules.put(t, rule);
		save(p.getUniqueId(), prof);
	}

	public static void setRodCycle(Player p, List<PetType> cycle) {
		Profile prof = profile(p.getUniqueId());
		prof.rodCycle.clear();
		prof.rodCycle.addAll(cycle);
		save(p.getUniqueId(), prof);
	}

	// ==================== the default layout ====================

	/**
	 * The five pets on the first five valid slots, ascending: 10, 11, 12, 13, 14.
	 * <p>
	 * Derived from {@link PetMenu#petSlots()} rather than written out, so moving a filler pane moves the defaults
	 * with it instead of leaving a pet on a slot the menu no longer treats as one.
	 */
	static Map<Integer, PetType> defaultLayout() {
		Map<Integer, PetType> out = new LinkedHashMap<>();
		List<Integer> slots = PetMenu.petSlots();
		PetType[] order = PetType.values();
		for(int i = 0; i < order.length && i < slots.size(); i++) out.put(slots.get(i), order[i]);
		return out;
	}

	// ==================== lifecycle ====================

	/**
	 * Load a player's file into the cache.  Called on join so the damage path never has to.
	 * <p>
	 * {@link #profile} loads on a miss anyway, so this is an optimisation and not a correctness requirement - but
	 * the miss path is reachable from inside a damage calculation, and a disk read there is exactly what the
	 * "map read, not a file read" contract in {@code Pet.forPlayer} promises will not happen.
	 */
	public static void preload(UUID uuid) {
		profile(uuid);
	}

	/** Drop a player's profile on quit.  Already saved: every mutator writes, so there is nothing to flush. */
	public static void unload(UUID uuid) {
		CACHE.remove(uuid);
	}

	/** Forget every loaded profile.  Plugin disable only; the files are already current. */
	public static void clearCache() {
		CACHE.clear();
	}

	static Profile profile(UUID uuid) {
		Profile cached = CACHE.get(uuid);
		if(cached != null) return cached;
		Profile fresh = read(uuid);
		CACHE.put(uuid, fresh);
		return fresh;
	}

	// ==================== file I/O ====================

	private static Path file(UUID uuid) throws Exception {
		return Catalog.dataDir().resolve("pets/" + uuid + ".json");
	}

	private static Profile read(UUID uuid) {
		PetsFile f = null;
		try {
			Path p = file(uuid);
			if(Files.exists(p)) {
				try(Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
					f = GSON.fromJson(r, PetsFile.class);
				}
			}
		} catch(Exception e) {
			M7tas.getInstance().getLogger().warning("Failed to read pets for " + uuid + ": " + e);
		}
		Profile prof = new Profile();
		if(f == null) {
			prof.layout.putAll(defaultLayout());
			return prof;
		}
		// Every field is parsed defensively and a value that no longer resolves is DROPPED, not defaulted: a file
		// written before a pet was renamed must come back as "that slot is empty", never as a different pet.  The
		// slot is checked against the menu's own list too, since a file written before a pane moved could name a
		// square the menu no longer draws, which would hide that pet with no way to get it back.
		if(f.layout != null) {
			for(Map.Entry<String, String> e : f.layout.entrySet()) {
				PetType pet = PetType.parse(e.getValue());
				Integer slot = parseSlot(e.getKey());
				if(pet == null || slot == null || !PetMenu.petSlots().contains(slot)) continue;
				if(!prof.layout.containsValue(pet)) prof.layout.put(slot, pet);
			}
		}
		// A file that resolved to nothing (empty, or every pet in it deleted) is indistinguishable from a first
		// open, and the menu has to show every pet either way.
		if(prof.layout.isEmpty()) prof.layout.putAll(defaultLayout());
		// A pet missing from the layout still needs a home, or it would be unreachable until a reset.
		for(PetType pet : PetType.values()) if(!prof.layout.containsValue(pet)) placeInFirstFree(prof.layout, pet);

		PetType equipped = PetType.parse(f.equipped);
		prof.equipped = equipped == null ? DEFAULT_PET : equipped;

		if(f.autopet != null) {
			for(Map.Entry<String, RuleEntry> e : f.autopet.entrySet()) {
				Autopet.Trigger t = Autopet.Trigger.parse(e.getKey());
				if(t == null || e.getValue() == null) continue;
				Autopet.Rule rule = new Autopet.Rule(PetType.parse(e.getValue().pet), readExceptions(e.getValue()));
				if(!rule.isOff()) prof.rules.put(t, rule);
			}
		}
		// REPEATS ARE KEPT.  This used to drop a pet already in the cycle, back when the menu could only append
		// each pet once; the cycle editor now allows any pet at any position, so a filter here would silently
		// shorten a saved rotation the first time it was loaded.
		if(f.rodCycle != null) {
			for(String name : f.rodCycle) {
				PetType pet = PetType.parse(name);
				if(pet != null) prof.rodCycle.add(pet);
			}
		}
		return prof;
	}

	/**
	 * One saved rule's exceptions, folding the pre-list scalar field in.
	 * <p>
	 * <b>The old {@code exception} field is read forever.</b>  Rules were a single optional exception before the
	 * list, and a file written by that jar is still a valid file - the player never did anything wrong, and a
	 * backup or a rolled-back server can hand one over at any time, which is the same reason StradDevHub's
	 * {@code m7/lb/Leaderboards} keeps its own migrations permanently.  It costs one null check per rule.
	 * <p>
	 * Nothing writes it again: {@link #save} leaves it null, Gson omits a null field, so the first save after a
	 * load moves the value into {@code exceptions} and the old key disappears on its own.  {@code Autopet.Rule}'s
	 * constructor dedupes, so folding a value that is already in the list is harmless.
	 */
	private static List<PetType> readExceptions(RuleEntry entry) {
		List<PetType> out = new ArrayList<>();
		PetType legacy = PetType.parse(entry.exception);
		if(legacy != null) out.add(legacy);
		if(entry.exceptions != null) {
			for(String name : entry.exceptions) {
				PetType pet = PetType.parse(name);
				if(pet != null) out.add(pet);
			}
		}
		return out;
	}

	/** Put a pet on the lowest valid slot nothing else occupies.  There are 28 slots and five pets, so one exists. */
	static void placeInFirstFree(Map<Integer, PetType> layout, PetType pet) {
		for(int slot : PetMenu.petSlots()) {
			if(!layout.containsKey(slot)) {
				layout.put(slot, pet);
				return;
			}
		}
	}

	private static Integer parseSlot(String s) {
		try {
			return Integer.valueOf(s);
		} catch(NumberFormatException e) {
			return null;
		}
	}

	private static void save(UUID uuid, Profile prof) {
		PetsFile f = new PetsFile();
		f.layout = new LinkedHashMap<>();
		for(Map.Entry<Integer, PetType> e : prof.layout.entrySet()) f.layout.put(String.valueOf(e.getKey()), e.getValue().name());
		f.equipped = prof.equipped.name();
		f.autopet = new LinkedHashMap<>();
		for(Map.Entry<Autopet.Trigger, Autopet.Rule> e : prof.rules.entrySet()) {
			RuleEntry entry = new RuleEntry();
			entry.pet = e.getValue().pet() == null ? null : e.getValue().pet().name();
			// entry.exception is left null on purpose - see readExceptions.  Gson omits a null field, so the
			// pre-list key vanishes from the file the first time the rule is saved, and never comes back.
			entry.exceptions = new ArrayList<>();
			for(PetType except : e.getValue().exceptions()) entry.exceptions.add(except.name());
			f.autopet.put(e.getKey().name(), entry);
		}
		f.rodCycle = new ArrayList<>();
		for(PetType pet : prof.rodCycle) f.rodCycle.add(pet.name());

		try {
			Path file = file(uuid);
			Path parent = file.getParent();
			if(parent != null) Files.createDirectories(parent);
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			try(Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(f, w);
			}
			try {
				Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch(AtomicMoveNotSupportedException ex) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch(Exception e) {
			M7tas.getInstance().getLogger().warning("Failed to save pets for " + uuid + ": " + e);
		}
	}

	/**
	 * On-disk shape.  Enum NAMES, not ordinals: reordering {@link PetType} or {@link Autopet.Trigger} must not
	 * silently repoint everybody's saved rules at a different pet.
	 */
	private static final class PetsFile {
		Map<String, String> layout;
		String equipped;
		Map<String, RuleEntry> autopet;
		List<String> rodCycle;
	}

	/**
	 * One rule on disk.  {@code exception} is the PRE-LIST field and is read-only: {@link #readExceptions} folds
	 * it into {@code exceptions} and {@link #save} never writes it again.  Deleting it would turn every rule in
	 * a file older than the list into "no exception" without a word.
	 */
	private static final class RuleEntry {
		String pet;
		String exception;
		List<String> exceptions;
	}
}
