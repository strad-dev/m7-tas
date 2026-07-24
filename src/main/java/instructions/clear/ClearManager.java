package instructions.clear;

import commands.Spectate;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import plugin.FakePlayerInventory;
import plugin.FakePlayerManager;
import plugin.M7tas;
import plugin.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The central controller for the dungeon clear phase. Holds all per-run state (found secrets, room
 * checkmarks, bonus counters, blessing tally, score), spawns/tears down the secret entities, runs the
 * per-tick HUD + minimap + item-collection loop, and exposes the score to the end-of-run scoreboard.
 *
 * <p>Every gameplay entry point takes a {@link Player} (real or fake) and all scoring is team-wide, so
 * TAS v3's fake players can drive exactly the same system.
 */
public final class ClearManager {
	private ClearManager() {
	}

	// ---- run state ----
	private static World world;
	private static boolean active;
	private static BukkitTask tickTask;

	// bonus counters
	private static int cryptLurkers;
	private static boolean firstPrince, firstBat, mimicKilled;
	private static int deaths; // not tracked in practice today; kept so the Skill formula matches Hypixel

	// Wizard crystal-ball special (not one of the 47 secrets)
	private static boolean crystalPickedUp, crystalHandedIn;

	// blessing tally — tracked for future use (TAS v3), never consumed by gameplay yet
	private static final Map<Blessing, Integer> blessingTally = new LinkedHashMap<>();

	// per-room last checkmark, so a transition can play its ding exactly once
	private static final Map<Room, Room.Check> lastCheck = new LinkedHashMap<>();

	private static boolean milestone300;

	// entity scoreboard tags for spawned secrets (also targeted by Server.blanketKill)
	public static final String TAG_ITEM = "SecretItem";
	public static final String TAG_BAT = "SecretBat";
	public static final String TAG_ESSENCE = "SecretEssence";
	public static final String TAG_ESSENCE_DISPLAY = "SecretEssenceDisplay";
	public static final String TAG_MIMIC = "SecretMimic";
	public static final String TAG_CRYPT = "SecretCryptLurker";

	public static boolean isActive() {
		return active;
	}

	// ==================== lifecycle ====================

	/** Reset all logical state (called from {@link instructions.Server#resetClearState()} during serverSetup).
	 *  Also deactivates and stops the tick loop; {@link #start} re-arms both right after. */
	public static void reset() {
		active = false;
		if(tickTask != null) {
			tickTask.cancel();
			tickTask = null;
		}
		Rooms.reset();
		cryptLurkers = 0;
		firstPrince = firstBat = mimicKilled = false;
		deaths = 0;
		crystalPickedUp = crystalHandedIn = false;
		milestone300 = false;
		blessingTally.clear();
		lastCheck.clear();
		for(Room r : Rooms.all()) lastCheck.put(r, r.check());
	}

	/** Begin the clear phase: spawn secrets, place chest blocks, hand out maps, start the HUD loop. */
	public static void start(World w) {
		world = w;
		reset();
		placeChests();
		spawnSecretEntities();
		PuzzleQuiz.reset();
		PuzzleIceFill.begin(w);
		active = true;
		giveMaps();
		if(tickTask != null) tickTask.cancel();
		tickTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), ClearManager::tick, 1L, 1L);
	}

	/** End the clear phase: remove secret entities, restore chest blocks + hotbar slot 8, stop the loop. */
	public static void stop(World w) {
		active = false;
		if(tickTask != null) {
			tickTask.cancel();
			tickTask = null;
		}
		PuzzleQuiz.stop();
		PuzzleIceFill.stop();
		if(w != null) {
			removeSecretEntities(w);
			clearChests(w);
		}
		restoreMenus();
	}

	// ==================== spawning ====================

	private static void placeChests() {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(s.isChest()) {
					Utils.runCommand("setblock " + s.blockX() + " " + s.blockY() + " " + s.blockZ() + " minecraft:chest");
				}
			}
		}
	}

	private static void clearChests(World w) {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(s.isChest()) {
					Block b = w.getBlockAt(s.blockX(), s.blockY(), s.blockZ());
					if(b.getType() == Material.CHEST) b.setType(Material.AIR, false);
				}
			}
		}
	}

	private static void spawnSecretEntities() {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				switch(s.type) {
					case ITEM -> spawnItem(s);
					case BAT -> spawnBat(s);
					case ESSENCE -> spawnEssence(s);
					default -> {
					}
				}
			}
		}
	}

	private static void spawnItem(Secret s) {
		Item item = world.dropItem(s.location(world), new ItemStack(Material.PAPER));
		item.setUnlimitedLifetime(true);
		item.setWillAge(false);
		item.setPickupDelay(32767); // "never" auto-picked; we collect manually (3× range)
		item.setPersistent(true);
		item.setGravity(false);
		item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
		item.addScoreboardTag(TAG_ITEM);
		s.entityId = item.getUniqueId();
	}

	private static void spawnBat(Secret s) {
		Bat bat = (Bat) world.spawnEntity(s.location(world), EntityType.BAT);
		bat.setAI(false);
		bat.setSilent(true);
		bat.setAwake(true);
		bat.setGravity(false);
		bat.setPersistent(true);
		bat.setRemoveWhenFarAway(false);
		bat.setCustomNameVisible(false);
		Objects.requireNonNull(bat.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(1);
		bat.setHealth(1);
		bat.addScoreboardTag(TAG_BAT);
		s.entityId = bat.getUniqueId();
	}

	private static void spawnEssence(Secret s) {
		Interaction hit = (Interaction) world.spawnEntity(s.location(world), EntityType.INTERACTION);
		hit.setInteractionWidth(1.0f);
		hit.setInteractionHeight(1.0f);
		hit.setResponsive(true);
		hit.setPersistent(true);
		hit.addScoreboardTag(TAG_ESSENCE);
		s.entityId = hit.getUniqueId();

		// A small floating skull so the essence is visible; purely decorative (click hits the Interaction).
		ItemDisplay disp = (ItemDisplay) world.spawnEntity(s.location(world).add(0, 0.4, 0), EntityType.ITEM_DISPLAY);
		disp.setItemStack(new ItemStack(Material.WITHER_SKELETON_SKULL));
		disp.setBillboard(Display.Billboard.CENTER);
		org.bukkit.util.Transformation t = disp.getTransformation();
		disp.setTransformation(new org.bukkit.util.Transformation(
				t.getTranslation(), t.getLeftRotation(),
				new org.joml.Vector3f(0.5f, 0.5f, 0.5f), t.getRightRotation()));
		disp.setPersistent(true);
		disp.addScoreboardTag(TAG_ESSENCE_DISPLAY);
	}

	private static void removeSecretEntities(World w) {
		for(Entity e : w.getEntities()) {
			if(e.getScoreboardTags().contains(TAG_ITEM)
					|| e.getScoreboardTags().contains(TAG_BAT)
					|| e.getScoreboardTags().contains(TAG_ESSENCE)
					|| e.getScoreboardTags().contains(TAG_ESSENCE_DISPLAY)
					|| e.getScoreboardTags().contains(TAG_MIMIC)) {
				e.remove();
			}
		}
	}

	// ==================== hotbar map ====================

	private static void giveMaps() {
		for(Player p : realPlayers()) setMapSlot(p);
	}

	private static void setMapSlot(Player p) {
		ItemStack cur = p.getInventory().getItem(8);
		if(cur == null || cur.getType() != Material.FILLED_MAP) {
			p.getInventory().setItem(8, DungeonMap.mapItem());
		}
	}

	private static void restoreMenus() {
		for(Player p : realPlayers()) {
			ItemStack cur = p.getInventory().getItem(8);
			if(cur != null && cur.getType() == Material.FILLED_MAP) {
				p.getInventory().setItem(8, FakePlayerInventory.getSkyBlockItem(
						Material.NETHER_STAR, FakePlayerInventory.SKYBLOCK_MENU_NAME, ""));
			}
		}
	}

	// ==================== per-tick loop ====================

	private static void tick() {
		if(!active) return;
		List<Player> players = realPlayers();
		for(Player p : players) {
			setMapSlot(p);
			collectItems(p);
			updateActionBar(p);
		}
		PuzzleQuiz.tick(world, players);
		PuzzleIceFill.tick(world, players);
	}

	private static void collectItems(Player p) {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(s.type != Utils.SecretType.ITEM || s.found || s.entityId == null) continue;
				Entity e = Bukkit.getEntity(s.entityId);
				if(e == null) continue;
				// 3× the ~1-block vanilla pickup range.
				if(e.getLocation().distanceSquared(p.getLocation()) <= 9.0) {
					secretFound(p, s);
				}
			}
		}
	}

	private static void updateActionBar(Player p) {
		Room room = Rooms.roomAt(p.getLocation());
		if(room == null) return; // outside the dungeon grid (e.g. boss arena) — leave the action bar alone
		String color = hex(room.type.color);
		String bar = color + room.name
				+ " <dark_gray>| " + color + "Secrets <white>" + room.countedSecretFound() + "/" + room.countedSecretTotal()
				+ " <dark_gray>| " + color + "Total <white>" + totalSecretsFound() + "/" + totalSecrets()
				+ " <dark_gray>| " + color + "Score <white>" + teamScore();
		p.sendActionBar(Utils.msg(bar));
	}

	// ==================== gameplay events (Player-generic) ====================

	public static void secretFound(Player p, Secret s) {
		if(s == null || s.found) return;
		s.found = true;
		Utils.playSecretFoundSound(p, s.type);
		if(s.blessing != null) awardBlessing(p, s.blessing);
		// Trap earns its white checkmark the moment its Power-II chest is opened.
		if(s.room != null && s.room.type == RoomType.TRAP && s.blessing != null
				&& s.blessing.type() == Utils.BlessingType.POWER && s.blessing.level() == 2) {
			s.room.cleared = true;
		}
		if(s.entityId != null) {
			Entity e = Bukkit.getEntity(s.entityId);
			if(e != null && !(e instanceof LivingEntity)) e.remove(); // items/essence hitboxes; bats die naturally
		}
		// Essence also has a floating display entity beside its hitbox — clear it too.
		if(s.type == Utils.SecretType.ESSENCE && world != null) {
			Location at = s.location(world);
			for(Entity e : world.getNearbyEntities(at, 1.5, 1.5, 1.5)) {
				if(e.getScoreboardTags().contains(TAG_ESSENCE_DISPLAY)) e.remove();
			}
		}
		afterEvent(s.room);
	}

	public static void minibossKilled(Room room, Player killer) {
		if(room == null || room.cleared) return;
		room.cleared = true;
		for(Blessing b : room.clearBlessings) awardBlessing(killer, b);
		afterEvent(room);
	}

	public static void puzzleSolved(Room room, Player p) {
		if(room == null || room.solved) return;
		room.solved = true;
		for(Blessing b : room.clearBlessings) awardBlessing(p, b);
		afterEvent(room);
	}

	public static void cryptKilled(boolean isPrince) {
		cryptLurkers++;
		if(isPrince && !firstPrince) firstPrince = true;
		afterEvent(null);
	}

	public static void mimicKilledEvent(Player p, Secret mimicSecret) {
		if(!mimicKilled) mimicKilled = true;
		secretFound(p, mimicSecret); // +2 bonus already flagged; this adds the regular secret progress
		afterEvent(mimicSecret == null ? null : mimicSecret.room);
	}

	public static void noteBatKill() {
		if(!firstBat) {
			firstBat = true;
			afterEvent(null);
		}
	}

	// ---- Wizard crystal ball (special, not a counted secret) ----
	public static boolean hasCrystal() {
		return crystalPickedUp && !crystalHandedIn;
	}

	public static void pickUpCrystal(Player p) {
		if(crystalPickedUp) return;
		crystalPickedUp = true;
		Utils.playSecretFoundSound(p, Utils.SecretType.ITEM);
		Bukkit.broadcast(Utils.msg("<light_purple><name> <green>picked up the <white>Crystal Ball<green>! Hand it to the Wizard.",
				Placeholder.unparsed("name", Utils.getRealName(p))));
	}

	public static void handInCrystal(Player p) {
		if(!hasCrystal()) return;
		crystalHandedIn = true;
		Bukkit.broadcast(Utils.msg("<dark_aqua>[NPC] Wizard<white>: Oh, my lovely crystal ball! You deserve a reward, young gobelin."));
		awardBlessing(p, new Blessing(Utils.BlessingType.WISDOM, 1));
	}

	// ---- lookups for the listener ----
	public static Secret findChestSecret(int x, int y, int z) {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(s.isChest() && s.blockX() == x && s.blockY() == y && s.blockZ() == z) return s;
			}
		}
		return null;
	}

	public static Secret findSecretByEntity(java.util.UUID id) {
		if(id == null) return null;
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(id.equals(s.entityId)) return s;
			}
		}
		return null;
	}

	/** Open a chest secret (right-clicked). A mimic chest instead spawns its Mimic (secret completes on kill). */
	public static void openChest(Player p, Secret s) {
		if(s == null || s.found) return;
		if(s.mimic) {
			if(s.entityId != null && Bukkit.getEntity(s.entityId) != null) return; // mimic already out
			spawnMimic(p.getWorld(), s);
			return;
		}
		secretFound(p, s);
	}

	private static void spawnMimic(World w, Secret s) {
		Block b = w.getBlockAt(s.blockX(), s.blockY(), s.blockZ());
		if(b.getType() == Material.CHEST) b.setType(Material.AIR, false);
		Zombie z = (Zombie) w.spawnEntity(s.location(w).add(0.5, 0, 0.5), EntityType.ZOMBIE);
		z.setBaby();
		z.setAI(false);
		z.setSilent(true);
		z.setPersistent(true);
		z.setRemoveWhenFarAway(false);
		z.setCustomNameVisible(true);
		z.customName(Utils.msg("<red>Mimic <yellow>4M<red>❤"));
		Objects.requireNonNull(z.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2);
		z.setHealth(2);
		Objects.requireNonNull(z.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(z.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
		if(z.getEquipment() != null) {
			z.getEquipment().clear();
			z.getEquipment().setItemInMainHand(new ItemStack(Material.CHEST));
		}
		z.addScoreboardTag(TAG_MIMIC);
		s.entityId = z.getUniqueId();
	}

	/** Nearest real (non-spectator, non-fake) player to a location — for attributing a miniboss/secret kill. */
	public static Player nearestRealPlayer(Location loc) {
		Player best = null;
		double bestSq = Double.MAX_VALUE;
		for(Player p : realPlayers()) {
			double d = p.getLocation().distanceSquared(loc);
			if(d < bestSq) {
				bestSq = d;
				best = p;
			}
		}
		return best;
	}

	public static void awardBlessing(Player p, Blessing b) {
		Utils.broadcastBlessing(p, b.type(), b.level());
		blessingTally.merge(b, 1, Integer::sum);
	}

	/** Recompute checkmark transitions + score milestone after any event. */
	private static void afterEvent(Room room) {
		DungeonMap.markDirty();
		if(room != null) checkTransition(room);
		if(!milestone300 && teamScore() >= 300) {
			milestone300 = true;
			Bukkit.broadcast(Utils.msg("<green><bold>Your team reached a score of <yellow>300<green>! <gray>(<white><t><gray>)",
					Placeholder.unparsed("t", formatTime(Utils.runTick()))));
			Utils.playGlobalSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f);
		}
	}

	private static void checkTransition(Room room) {
		Room.Check now = room.check();
		Room.Check was = lastCheck.getOrDefault(room, Room.Check.NONE);
		if(now == was) return;
		lastCheck.put(room, now);
		if(now == Room.Check.WHITE) {
			Bukkit.broadcast(Utils.msg("<green>✔ <white>" + room.name + " <gray>cleared!"));
			Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
		} else if(now == Room.Check.GREEN) {
			Bukkit.broadcast(Utils.msg("<green><bold>✔ " + room.name + " <reset><green>fully cleared!"));
			Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
		}
	}

	// ==================== scoring ====================

	public static int totalSecrets() {
		int n = 0;
		for(Room r : Rooms.all()) n += r.countedSecretTotal();
		return n;
	}

	public static int totalSecretsFound() {
		int n = 0;
		for(Room r : Rooms.all()) n += r.countedSecretFound();
		return n;
	}

	private static int unsolvedPuzzles() {
		int n = 0;
		for(Room r : Rooms.all()) if(r.type == RoomType.PUZZLE && !r.solved) n++;
		return n;
	}

	/** Cell-weighted count of "completed" room spaces (Museum = 4, etc.) out of 36 — the fraction Hypixel's
	 *  room-clear score is built on. A room contributes its cells the moment it earns any checkmark. */
	private static int checkedCells() {
		int cells = 0;
		for(Room r : Rooms.all()) if(r.check() != Room.Check.NONE) cells += r.cells.length;
		return cells;
	}

	/** Skill = 20 base + up to 80 from room clears − 10 per incomplete puzzle − death penalty, clamped [20,100].
	 *  (Matches the real Catacombs formula; deaths are not tracked in practice, so that term is 0.) */
	public static int skill() {
		int skillRooms = (int) Math.min(80, Math.floor(80.0 * checkedCells() / 36.0));
		int puzzlePenalty = 10 * unsolvedPuzzles();
		int deathPenalty = Math.max(0, deaths * 2 - 1);
		return Math.max(20, Math.min(100, 20 + skillRooms - puzzlePenalty - deathPenalty));
	}

	public static int explore() {
		int checkPts = (int) Math.min(60, Math.floor(60.0 * checkedCells() / 36.0));
		int total = totalSecrets();
		int secretPts = total == 0 ? 0 : (int) Math.min(40, Math.floor((double) totalSecretsFound() / total * 40.0));
		return checkPts + secretPts;
	}

	public static int speed() {
		return 100;
	}

	public static int bonus() {
		return 10 + Math.min(cryptLurkers, 5) + (firstPrince ? 1 : 0) + (firstBat ? 1 : 0) + (mimicKilled ? 2 : 0);
	}

	public static int teamScore() {
		return skill() + explore() + speed() + bonus();
	}

	public static String grade() {
		int s = teamScore();
		if(s >= 300) return "S+";
		if(s >= 270) return "S";
		if(s >= 230) return "A";
		if(s >= 160) return "B";
		if(s >= 100) return "C";
		return "D";
	}

	// ==================== blessing tally (future use) ====================

	public static Map<Blessing, Integer> blessingTally() {
		return blessingTally;
	}

	public static int blessingCount(Blessing b) {
		return blessingTally.getOrDefault(b, 0);
	}

	// ==================== helpers ====================

	/** Real, non-spectator, non-fake online players — the ones who get a HUD/map and can collect secrets. */
	public static List<Player> realPlayers() {
		List<Player> out = new ArrayList<>();
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.getGameMode() == GameMode.SPECTATOR) continue;
			if(Spectate.isSpectating(p)) continue;
			if(FakePlayerManager.getFakePlayers().containsValue(p)) continue;
			out.add(p);
		}
		return out;
	}

	/** Is a broken block protected from Dungeonbreaker (a secret chest, or a Quiz answer button area)? */
	public static boolean isProtectedBlock(Block b) {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(s.isChest() && b.getX() == s.blockX() && b.getY() == s.blockY() && b.getZ() == s.blockZ()) return true;
			}
		}
		return PuzzleQuiz.isButtonArea(b);
	}

	private static String hex(Color c) {
		return String.format("<#%02X%02X%02X>", c.getRed(), c.getGreen(), c.getBlue());
	}

	/** m:ss.SS from a tick count. */
	static String formatTime(int ticks) {
		double secs = ticks / 20.0;
		int mins = (int) (secs / 60);
		double rem = secs - mins * 60.0;
		return String.format("%d:%05.2f", mins, rem);
	}
}
