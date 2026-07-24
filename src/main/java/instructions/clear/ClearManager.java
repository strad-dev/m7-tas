package instructions.clear;

import commands.Spectate;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lidded;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import plugin.FakePlayerInventory;
import plugin.FakePlayerManager;
import plugin.M7tas;
import plugin.Utils;

import java.awt.Color;
import java.util.*;

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

	private static boolean milestone300;

	// entity scoreboard tags for spawned secrets (also targeted by Server.blanketKill)
	public static final String TAG_ITEM = "SecretItem";
	public static final String TAG_BAT = "SecretBat";
	public static final String TAG_MIMIC = "SecretMimic";
	public static final String TAG_CRYPT = "SecretCryptLurker";

	/** Wither Essence is a placed wither-skeleton-skull block, right-clicked to collect (not an entity). */
	private static final Material ESSENCE_BLOCK = Material.WITHER_SKELETON_SKULL;

	/** Explicit chest facings (keyed "x,y,z") from the map builder; chests not listed use the auto-orient fallback. */
	private static final Map<String, BlockFace> CHEST_FACING = new HashMap<>();
	static {
		CHEST_FACING.put("-114,69,-35", BlockFace.EAST);
		CHEST_FACING.put("-186,79,-26", BlockFace.WEST);
		CHEST_FACING.put("-186,61,-40", BlockFace.EAST);
		CHEST_FACING.put("-69,69,-61", BlockFace.WEST);
		CHEST_FACING.put("-172,83,-85", BlockFace.WEST);
		CHEST_FACING.put("-186,62,-80", BlockFace.EAST);
		CHEST_FACING.put("-109,82,-89", BlockFace.WEST);
		CHEST_FACING.put("-125,92,-101", BlockFace.SOUTH);
		CHEST_FACING.put("-54,69,-89", BlockFace.SOUTH);
		CHEST_FACING.put("-64,52,-125", BlockFace.EAST);
		CHEST_FACING.put("-70,89,-185", BlockFace.EAST);
		CHEST_FACING.put("-22,88,-188", BlockFace.EAST);
		CHEST_FACING.put("-29,91,-163", BlockFace.EAST);
		CHEST_FACING.put("-71,75,-152", BlockFace.EAST);
		CHEST_FACING.put("-71,75,-154", BlockFace.EAST);
	}

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
		// Tear down last run's placed chests / essence / secret entities so /reset and /setup start clean.
		World w = world != null ? world : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst());
		if(w != null) {
			removeSecretEntities(w);
			teardownSecretBlocks(w);
		}
		Rooms.reset();
		cryptLurkers = 0;
		firstPrince = firstBat = mimicKilled = false;
		deaths = 0;
		crystalPickedUp = crystalHandedIn = false;
		milestone300 = false;
		blessingTally.clear();
	}

	/** Begin the clear phase: spawn secrets, place chest blocks, hand out maps, start the HUD loop. */
	public static void start(World w) {
		world = w;
		reset();
		placeSecretBlocks();
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
			teardownSecretBlocks(w);
		}
		restoreMenus();
	}

	// ==================== spawning ====================

	/** Place the secret chests (oriented toward an open side) and re-arm the essence skulls (wither variant). */
	private static void placeSecretBlocks() {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(s.isChest()) {
					Block b = world.getBlockAt(s.blockX(), s.blockY(), s.blockZ());
					b.setType(Material.CHEST, false);
					if(b.getBlockData() instanceof Directional dir) {
						BlockFace face = CHEST_FACING.getOrDefault(s.blockX() + "," + s.blockY() + "," + s.blockZ(), openFace(b));
						if(face != null) {
							dir.setFacing(face);
							b.setBlockData(dir, false);
						}
					}
				} else if(s.type == Utils.SecretType.ESSENCE) {
					// Essence lives in the static map; just make sure it's the wither variant (a prior run may have
					// converted it to a normal skull on collect). Keep whatever orientation it already has.
					Block b = world.getBlockAt(s.blockX(), s.blockY(), s.blockZ());
					Material m = b.getType();
					if(m == Material.SKELETON_SKULL) convertSkull(b, Material.WITHER_SKELETON_SKULL);
					else if(m == Material.SKELETON_WALL_SKULL) convertSkull(b, Material.WITHER_SKELETON_WALL_SKULL);
					else if(m != Material.WITHER_SKELETON_SKULL && m != Material.WITHER_SKELETON_WALL_SKULL) b.setType(ESSENCE_BLOCK, false);
				}
			}
		}
	}

	/** Undo {@link #placeSecretBlocks}: remove the placed chests and revert any collected essence back to a wither skull. */
	private static void teardownSecretBlocks(World w) {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(s.isChest()) {
					Block b = w.getBlockAt(s.blockX(), s.blockY(), s.blockZ());
					if(b.getType() == Material.CHEST) b.setType(Material.AIR, false);
				} else if(s.type == Utils.SecretType.ESSENCE) {
					Block b = w.getBlockAt(s.blockX(), s.blockY(), s.blockZ());
					if(b.getType() == Material.SKELETON_SKULL) convertSkull(b, Material.WITHER_SKELETON_SKULL);
					else if(b.getType() == Material.SKELETON_WALL_SKULL) convertSkull(b, Material.WITHER_SKELETON_WALL_SKULL);
				}
			}
		}
	}

	/** Change a skull block to {@code target}, preserving its floor rotation / wall facing. */
	private static void convertSkull(Block b, Material target) {
		BlockData old = b.getBlockData();
		BlockData nd = target.createBlockData();
		if(old instanceof Rotatable or && nd instanceof Rotatable nr) nr.setRotation(or.getRotation());
		if(old instanceof Directional od && nd instanceof Directional ndd) ndd.setFacing(od.getFacing());
		b.setBlockData(nd, false);
	}

	/** The first horizontal face whose neighbour isn't a solid block — the side a chest should open toward. */
	private static BlockFace openFace(Block b) {
		for(BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
			if(!b.getRelative(f).getType().isOccluding()) return f;
		}
		return null;
	}

	private static void spawnSecretEntities() {
		int items = 0, bats = 0;
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				// Guard each spawn so one failure can't abort the rest (e.g. leave later items unspawned).
				try {
					switch(s.type) {
						case ITEM -> { spawnItem(s); items++; }
						case BAT -> { spawnBat(s); bats++; }
						default -> {
						}
					}
				} catch(Throwable ex) {
					Bukkit.getLogger().warning("[M7 clear] failed to spawn secret " + s.type + " at " + s.blockX() + "," + s.blockY() + "," + s.blockZ() + ": " + ex);
				}
			}
		}
		Bukkit.getLogger().info("[M7 clear] spawned " + items + " items, " + bats + " bats");
	}

	private static void spawnItem(Secret s) {
		Item item = world.dropItem(s.location(world), new ItemStack(Material.PAPER));
		s.entityId = item.getUniqueId();
		item.addScoreboardTag(TAG_ITEM);
		// "secret" tag so the map's `/kill @e[type=item]` command block can exclude these (tag=!secret).
		item.addScoreboardTag("secret");
		item.setPickupDelay(32767); // "never" auto-picked; we collect manually (3× range)
		item.setPersistent(true);
		item.setGravity(false);
		try { item.setVelocity(new org.bukkit.util.Vector(0, 0, 0)); } catch(Throwable ignored) {}
		try { item.setWillAge(false); } catch(Throwable ignored) {}
		try { item.setUnlimitedLifetime(true); } catch(Throwable ignored) {}
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

	private static void removeSecretEntities(World w) {
		for(Entity e : w.getEntities()) {
			if(e.getScoreboardTags().contains(TAG_ITEM)
					|| e.getScoreboardTags().contains(TAG_BAT)
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
		// Replace slot 8 unless it's already THIS session's dungeon map — a stale filled map from a previous
		// session/build points at a dead MapView and renders blank, so it must be swapped out too.
		if(!DungeonMap.isDungeonMap(p.getInventory().getItem(8))) {
			p.getInventory().setItem(8, DungeonMap.mapItem());
		}
	}

	private static ItemStack skyblockMenu() {
		return FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, FakePlayerInventory.SKYBLOCK_MENU_NAME, "");
	}

	/** Slot 8 holds the dungeon map while inside the clear grid; outside it (e.g. the boss arena) our map is
	 *  swapped back to the SkyBlock Menu. Only ever touches OUR map — never a Maxor crystal or any other item. */
	private static void manageSlot8(Player p) {
		if(Rooms.roomAt(p.getLocation()) != null) {
			setMapSlot(p); // in the clear grid → dungeon map
		} else if(DungeonMap.isDungeonMap(p.getInventory().getItem(8))) {
			p.getInventory().setItem(8, skyblockMenu()); // in the boss (outside the grid) → SkyBlock Menu
		}
	}

	private static void restoreMenus() {
		for(Player p : realPlayers()) {
			if(DungeonMap.isDungeonMap(p.getInventory().getItem(8))) {
				p.getInventory().setItem(8, skyblockMenu());
			}
		}
	}

	// ==================== per-tick loop ====================

	private static void tick() {
		if(!active) return;
		List<Player> players = realPlayers();
		for(Player p : players) {
			manageSlot8(p);
			collectItems(p);
			updateActionBar(p);
		}
		// Wizard has no miniboss — it earns its white check the moment a player first sets foot in it.
		if(!Rooms.WIZARD.cleared) {
			for(Player p : players) {
				if(Rooms.roomAt(p.getLocation()) == Rooms.WIZARD) {
					Rooms.WIZARD.cleared = true;
					afterEvent(Rooms.WIZARD);
					break;
				}
			}
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
		// Bonus flags: M (mimic), P (prince), B (bat) — e.g. "Crypts 3/5 (M, P, B)".
		List<String> flags = new ArrayList<>();
		if(mimicKilled) flags.add("M");
		if(firstPrince) flags.add("P");
		if(firstBat) flags.add("B");
		String crypts = "Crypts <white>" + Math.min(cryptLurkers, 5) + "/5" + (flags.isEmpty() ? "" : " (" + String.join(", ", flags) + ")");
		String bar = color + room.name
				+ " <dark_gray>| " + color + "Secrets <white>" + room.countedSecretFound() + "/" + room.countedSecretTotal()
				+ " <dark_gray>| " + color + "Total <white>" + totalSecretsFound() + "/" + totalSecrets()
				+ " <dark_gray>| " + color + crypts
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
			if(e != null && !(e instanceof LivingEntity)) e.remove(); // dropped items; bats/mimics die naturally
		}
		if(world != null) {
			if(s.isChest() && !s.mimic) {
				// Found chests stay visibly open.
				Block b = world.getBlockAt(s.blockX(), s.blockY(), s.blockZ());
				if(b.getState() instanceof Lidded lid) lid.open();
			} else if(s.type == Utils.SecretType.ESSENCE) {
				// Collected essence turns into a normal skeleton skull, keeping its orientation.
				Block b = world.getBlockAt(s.blockX(), s.blockY(), s.blockZ());
				if(b.getType() == Material.WITHER_SKELETON_SKULL) convertSkull(b, Material.SKELETON_SKULL);
				else if(b.getType() == Material.WITHER_SKELETON_WALL_SKULL) convertSkull(b, Material.SKELETON_WALL_SKULL);
			}
		}
		afterEvent(s.room);
	}

	public static void minibossKilled(Room room, Player killer) {
		if(room == null || room.cleared) return;
		room.cleared = true;
		for(Blessing b : room.clearBlessings) {
			awardBlessing(killer, b);
			// The blessing pickup plays the item-pickup ding to whoever earned it (like the key grants).
			if(killer != null) Utils.playLocalSound(killer, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
		}
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
		p.sendMessage(Utils.msg("<green>You found a Special Crystal!")); // only the picker sees it
	}

	public static void handInCrystal(Player p) {
		if(!hasCrystal()) return;
		crystalHandedIn = true;
		// Reuses the existing Wizard hand-in dialogue (previously the fake-player Berserk routine).
		Utils.playLocalSound(p, Sound.ENTITY_VILLAGER_YES);
		Bukkit.broadcast(Utils.msg("<yellow>[NPC] Wizard<white>: Oh my lovely crystal ball, mi so happy"));
		Utils.scheduleTask(() -> {
			Utils.playLocalSound(p, Sound.ENTITY_VILLAGER_YES);
			Bukkit.broadcast(Utils.msg("<yellow>[NPC] Wizard<white>: You deserve a reward young gobelin"));
		}, 20);
		Utils.scheduleTask(() -> {
			Utils.playLocalSound(p, Sound.ENTITY_VILLAGER_YES);
			Bukkit.broadcast(Utils.msg("<yellow>[NPC] Wizard<white>: Granted your team a <light_purple>Blessing of Wisdom I"));
			awardBlessing(p, new Blessing(Utils.BlessingType.WISDOM, 1));
		}, 60);
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

	/** True if this block is a clear-phase secret you right-click (chest/essence) — such a click owns the
	 *  interaction, so the held item's right-click ability must NOT also fire on top of it. */
	public static boolean isSecretBlock(Block b) {
		return active && b != null && findSecretAtBlock(b.getX(), b.getY(), b.getZ()) != null;
	}

	/** A right-clickable block secret (chest or essence) at these coords, or null. */
	public static Secret findSecretAtBlock(int x, int y, int z) {
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if((s.isChest() || s.type == Utils.SecretType.ESSENCE)
						&& s.blockX() == x && s.blockY() == y && s.blockZ() == z) return s;
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
		// Ice-Fill reward chests can't be opened until the puzzle is solved.
		if(s.room == Rooms.ICE_FILL && !Rooms.ICE_FILL.solved) return;
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
		z.getEquipment();
		z.getEquipment().clear();
		z.getEquipment().setItemInMainHand(new ItemStack(Material.CHEST));
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
		if(!milestone300 && teamScore() >= 300) {
			milestone300 = true;
			int t = Utils.runTick();
			Bukkit.broadcast(Utils.msg("<green><bold>300 score reached</bold> in " + spaced(t) + " ticks (" + String.format("%.2f", t / 20.0) + " seconds)"));
			Utils.playGlobalSound(Sound.ENTITY_ARROW_HIT_PLAYER, 2.0f, 0.5f);
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
		// Blood always counts as completed for scoring (matches Hypixel's live projection), even if its
		// checkmark isn't set on the map yet.
		for(Room r : Rooms.all()) if(r.check() != Room.Check.NONE || r.type == RoomType.BLOOD) cells += r.cells.length;
		return cells;
	}

	/** Skill = 20 base + up to 80 from room clears − 10 per incomplete puzzle − death penalty, clamped [20,100].
	 *  (Matches the real Catacombs formula; deaths are not tracked in practice, so that term is 0.) */
	public static int skill() {
		int skillRooms = (int) Math.min(80, Math.floor(80.0 * checkedCells() / 36.0));
		int puzzlePenalty = 10 * unsolvedPuzzles();
		int deathPenalty = Math.max(0, deaths * 2 - 1);
		return Math.clamp(20 + skillRooms - puzzlePenalty - deathPenalty, 20, 100);
	}

	public static int explore() {
		int checkPts = (int) Math.min(60, Math.floor(60.0 * checkedCells() / 36.0));
		int total = totalSecrets();
		int secretPts = total == 0 ? 0 : (int) Math.min(40, Math.floor((double) totalSecretsFound() / total * 40.0));
		return checkPts + secretPts;
	}

	@SuppressWarnings("SameReturnValue")
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

	/** Space-separated thousands, e.g. 3084 → "3 084". */
	private static String spaced(int n) {
		String s = String.valueOf(n);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < s.length(); i++) {
			if(i > 0 && (s.length() - i) % 3 == 0) sb.append(' ');
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}
}
