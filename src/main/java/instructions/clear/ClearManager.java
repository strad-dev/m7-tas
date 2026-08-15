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

	// blessing tally: read by damage/Difficulty in realistic mode, and published to other plugins as
	// plugin/BlessingState (see #awardBlessing).
	private static final Map<Blessing, Integer> blessingTally = new LinkedHashMap<>();

	private static boolean milestone300;

	// ---- leaderboard milestones (overall run ticks; -1 = not reached this run) ----
	// Stamped once each, never recomputed. score300Tick was previously only broadcast and thrown away.
	private static int score300Tick = -1;
	private static int bloodDoneTick = -1;
	private static int fullClearTick = -1;

	/** The literal maximum team score: skill 100 + explore 100 + speed 100 + bonus 19 (10 base + 5 crypt
	 *  lurkers + first Prince + first Bat + 2 mimic). A "full clear" is this score AND blood finished. */
	public static final int PERFECT_SCORE = 319;

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
		CHEST_FACING.put("-169,70,-83", BlockFace.WEST);
		CHEST_FACING.put("-186,62,-80", BlockFace.EAST);
		CHEST_FACING.put("-109,82,-89", BlockFace.WEST);
		CHEST_FACING.put("-125,92,-101", BlockFace.SOUTH);
		CHEST_FACING.put("-54,69,-89", BlockFace.WEST);
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
		// Strip the offhand dungeon map (and restore slot 8's menu) from everyone. reset() runs from serverSetup
		// before EVERY section, so this is what pulls the clear map out of the offhand when you jump straight to a
		// boss such as /m7practice witherking, since the clear tick loop that normally removes it is not running.
		restoreMenus();
		// Tear down last run's placed chests / essence / secret entities so /reset and /setup start clean.
		World w = world != null ? world : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst());
		if(w != null) {
			removeSecretEntities(w);
			teardownSecretBlocks(w);
		}
		Rooms.reset();
		DungeonMap.markDirty(); // clear the fog-of-war reveal state so the next run repaints from scratch
		cryptLurkers = 0;
		firstPrince = firstBat = mimicKilled = false;
		deaths = 0;
		crystalPickedUp = crystalHandedIn = false;
		milestone300 = false;
		score300Tick = bloodDoneTick = fullClearTick = -1;
		// Report the clear as well as the awards: a consumer fed only by awardBlessing would keep showing the
		// PREVIOUS run's blessings for the whole of the next one, since a section that collects nothing never
		// fires again.  Only when there was something to clear, so /setup on an idle server is silent.
		if(!blessingTally.isEmpty()) {
			blessingTally.clear();
			publishBlessings();
		}
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
		exploreRoom(Rooms.byName("Start"));    // the green Start room is always explored (players spawn in it)
		exploreRoom(Rooms.byName("Red Blue")); // the entrance room starts already explored on the map
		giveMaps();
		if(tickTask != null) tickTask.cancel();
		tickTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), ClearManager::tick, 1L, 1L);
		// Publish the (empty) opening tally, so a listener's display exists from the start of the clear rather than
		// appearing out of nowhere on the first blessing.  This is also the first report that says "this run HAS a
		// clear phase", which is the difference between "collected nothing" and "there was nothing to collect".
		publishBlessings();
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
					// Mimic chests are TRAPPED chests, the same tell as real Hypixel: that's how you spot one.
					b.setType(s.mimic ? Material.TRAPPED_CHEST : Material.CHEST, false);
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
					if(b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST) b.setType(Material.AIR, false);
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

	/** The first horizontal face whose neighbour isn't a solid block, i.e. the side a chest should open toward. */
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

	// ==================== offhand map ====================

	private static void giveMaps() {
		for(Player p : realPlayers()) manageMapAndMenu(p);
	}

	private static ItemStack skyblockMenu() {
		return FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, FakePlayerInventory.SKYBLOCK_MENU_NAME, "", "SKYBLOCK_MENU");
	}

	/** The Magical Map rides in the OFFHAND while inside the clear grid, and is removed in the boss arena outside
	*  the grid.  Hotbar slot 8 ("slot 9") always holds the SkyBlock Menu during the clear.  This only ever touches
	*  OUR own map or the menu star, never a Maxor crystal or any other item. */
	private static void manageMapAndMenu(Player p) {
		// Slot 8 stays the SkyBlock Menu the whole clear (revert it if it's holding a stale dungeon map / is empty).
		ItemStack slot8 = p.getInventory().getItem(8);
		if(!FakePlayerInventory.isSkyblockMenu(slot8)) {
			if(slot8 == null || slot8.getType() == Material.AIR || DungeonMap.isDungeonMap(slot8)) {
				p.getInventory().setItem(8, skyblockMenu());
			}
		}
		ItemStack off = p.getInventory().getItemInOffHand();
		if(Rooms.inGrid(p.getLocation())) {
			if(!DungeonMap.isDungeonMap(off)) p.getInventory().setItemInOffHand(DungeonMap.mapItem());
		} else if(DungeonMap.isDungeonMap(off)) {
			p.getInventory().setItemInOffHand(new ItemStack(Material.AIR)); // in the boss (outside the grid) → remove
		}
	}

	private static void restoreMenus() {
		for(Player p : realPlayers()) {
			if(DungeonMap.isDungeonMap(p.getInventory().getItemInOffHand())) {
				p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
			}
			ItemStack slot8 = p.getInventory().getItem(8);
			if(DungeonMap.isDungeonMap(slot8)) p.getInventory().setItem(8, skyblockMenu());
		}
	}

	// ==================== per-tick loop ====================

	/** Reveal any room a player is currently standing in. Includes the fake players, so a TAS run reveals the
	 *  map as the fakes clear (real players spectating them share the reveal); spectator-mode viewers don't. */
	private static void markExploration() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.getGameMode() == GameMode.SPECTATOR) continue;
			Room r = Rooms.roomAt(p.getLocation());
			if(r != null && !r.explored) {
				r.explored = true;
				DungeonMap.markDirty();
			}
		}
	}

	/** Force a room fully explored on the map without anyone entering it.  Used when a locked door opens and
	*  "explores" the room behind it (wither door → Deathmite, blood door → Blood). */
	public static void exploreRoom(Room r) {
		if(r != null && !r.explored) {
			r.explored = true;
			DungeonMap.markDirty();
		}
	}

	private static void tick() {
		if(!active) return;
		markExploration();
		List<Player> players = realPlayers();
		for(Player p : players) {
			manageMapAndMenu(p);
			collectItems(p);
			updateActionBar(p);
		}
		// Spectators follow the fakes but are excluded from realPlayers(), so give them the run-stats bar too.
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(FakePlayerManager.getFakePlayers().containsValue(p)) continue; // never the fakes themselves
			if(p.getGameMode() == GameMode.SPECTATOR || Spectate.isSpectating(p)) updateActionBar(p);
		}
		// Wizard has no miniboss, so it earns its white check the moment a player first sets foot in it.
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
		if(!Rooms.inGrid(p.getLocation())) return; // fully outside the dungeon grid, e.g. boss arena, so leave the bar alone
		Room room = Rooms.roomAt(p.getLocation()); // null when standing in a between-room buffer
		// Between rooms there's no specific room to name, so drop the room-name + per-room "Secrets" segments and
		// fall back to a neutral colour; the run-wide Total/Crypts/Score segments stay put.
		String color = room != null ? hex(room.type.color) : "<gray>";
		// Bonus flags: M (mimic), P (prince), B (bat), e.g. "Crypts 3/5 (M, P, B)".
		List<String> flags = new ArrayList<>();
		if(mimicKilled) flags.add("M");
		if(firstPrince) flags.add("P");
		if(firstBat) flags.add("B");
		String crypts = "Crypts <white>" + Math.min(cryptLurkers, 5) + "/5" + (flags.isEmpty() ? "" : " (" + String.join(", ", flags) + ")");
		String bar = "";
		if(room != null) {
			bar += color + room.name
					+ " <dark_gray>| " + color + "Secrets <white>" + room.countedSecretFound() + "/" + room.countedSecretTotal()
					+ " <dark_gray>| ";
		}
		bar += color + "Total <white>" + totalSecretsFound() + "/" + totalSecrets()
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
		puzzleSolved(room, p, true);
	}

	/**
	 * Mark a puzzle solved, which gives the green check and the score straight away.  With {@code awardBlessings}
	 * false the room's clear blessings are left for a later {@link #awardRoomBlessings} call: the Quiz scores the
	 * instant its last question is answered, but Oruo doesn't hand over Time V until his dialogue has played out.
	 */
	public static void puzzleSolved(Room room, Player p, boolean awardBlessings) {
		if(room == null || room.solved) return;
		room.solved = true;
		if(awardBlessings) for(Blessing b : room.clearBlessings) awardBlessing(p, b);
		afterEvent(room);
	}

	/** Award a room's clear blessings on their own, without touching its checkmark (deferred-blessing puzzles). */
	public static void awardRoomBlessings(Room room, Player p) {
		if(room == null) return;
		for(Blessing b : room.clearBlessings) awardBlessing(p, b);
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

	/** True if this block is a clear-phase secret you right-click (chest or essence).  Such a click owns the
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
		if(b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST) b.setType(Material.AIR, false);
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

	/** Nearest real (non-spectator, non-fake) player to a location, for attributing a miniboss or secret kill. */
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
		publishBlessings();
	}

	/** Tell anyone listening that the tally moved (the network plugin puts it in the tab list). */
	private static void publishBlessings() {
		Bukkit.getPluginManager().callEvent(new plugin.BlessingChangeEvent(plugin.BlessingState.capture()));
	}

	/** Recompute checkmark transitions + score milestone after any event. */
	private static void afterEvent(Room room) {
		// Progressing a room's objective (e.g. killing its miniboss with a beam) counts as exploring it, even if
		// nobody stood inside, so a room cleared from outside still fills in and shows its checkmark on the map.
		exploreRoom(room);
		DungeonMap.markDirty();
		if(!milestone300 && teamScore() >= 300) {
			milestone300 = true;
			int t = Utils.runTick();
			score300Tick = t;
			Bukkit.broadcast(Utils.msg("<green><bold>300 score reached</bold> in " + spaced(t) + " ticks (" + String.format("%.2f", t / 20.0) + " seconds)"));
			Utils.playGlobalSound(Sound.ENTITY_ARROW_HIT_PLAYER, 2.0f, 0.5f);
			// Report it NOW, not at run end: the milestone stands on its own, so it must count even if the team
			// resets immediately after.  score300Tick is assigned above first, because the payload reads it.
			instructions.bosses.WitherActions.signalScoreMilestone(300);
		}
		checkFullClear();
	}

	// ==================== leaderboard milestones ====================

	/**
	 * Stamp the "blood finished" tick.  Called from {@code Watcher.bloodCampFinished()}, i.e. the moment the
	 * Watcher vanishes, which on a clear-only practice is also the end of the run.  Safe to call when the clear
	 * phase isn't running, as in a boss-only practice; it simply records nothing.
	 */
	public static void noteBloodDone() {
		if(!active || bloodDoneTick >= 0) return;
		bloodDoneTick = Utils.runTick();
		checkFullClear();
	}

	/**
	 * A "full clear" is the maximum score AND blood finished.  Either can land last: the score can max out after
	 * blood on a late secret, or blood can finish after the score maxes.  So both paths call this, and whichever
	 * completes the pair stamps the tick.
	 */
	private static void checkFullClear() {
		if(!active || fullClearTick >= 0) return;
		if(bloodDoneTick < 0 || teamScore() < PERFECT_SCORE) return;
		fullClearTick = Utils.runTick();
	}

	/** Overall run tick at which the team first hit 300 score, or -1 if it never did. */
	public static int score300Tick() { return score300Tick; }

	/** Overall run tick at which blood finished, or -1 if it never did. */
	public static int bloodDoneTick() { return bloodDoneTick; }

	/** Overall run tick at which the run became a full clear ({@link #PERFECT_SCORE} + blood), or -1. */
	public static int fullClearTick() { return fullClearTick; }

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

	/** Cell-weighted count of "completed" room spaces (Museum = 4, etc.) out of 36, the fraction Hypixel's
	*  room-clear score is built on.  A room contributes its cells the moment it earns any checkmark. */
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

	// ==================== blessing tally ====================

	public static Map<Blessing, Integer> blessingTally() {
		return blessingTally;
	}

	public static int blessingCount(Blessing b) {
		return blessingTally.getOrDefault(b, 0);
	}

	/**
	 * Total level collected of one blessing type: every blessing of that type this run, summed by LEVEL, so a
	 * Power V contributes 5.  This is the figure {@code damage/Difficulty} reads in realistic mode, and the one
	 * {@code plugin/BlessingState} publishes - both go through here rather than walking the tally themselves,
	 * since "level x how many of them" is the sort of sum that is only ever right in one place.
	 */
	public static int collectedLevel(Utils.BlessingType type) {
		int level = 0;
		for(Map.Entry<Blessing, Integer> e : blessingTally.entrySet()) {
			if(e.getKey().type() == type) level += e.getKey().level() * e.getValue();
		}
		return level;
	}

	/** How many separate blessings of one type were collected this run, whatever their levels. */
	public static int collectedCount(Utils.BlessingType type) {
		int n = 0;
		for(Map.Entry<Blessing, Integer> e : blessingTally.entrySet()) {
			if(e.getKey().type() == type) n += e.getValue();
		}
		return n;
	}

	/**
	 * Whether the tally describes THIS session, i.e. whether there is a real chest history to read.  The clear
	 * being live covers a run in progress; a non-empty tally covers the stretch after the clear has handed off to
	 * the boss chain.  False for a boss-only practice, which is exactly when maxed blessings are assumed.
	 */
	public static boolean hasBlessingData() {
		return active || !blessingTally.isEmpty();
	}

	// ==================== helpers ====================

	/** Real, non-spectator, non-fake online players: the ones who get a HUD and map and can collect secrets. */

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

	/** Wither Essence, in both the floor and the wall skull form. */
	private static final Set<Material> ESSENCE_SKULLS =
			EnumSet.of(Material.WITHER_SKELETON_SKULL, Material.WITHER_SKELETON_WALL_SKULL);

	/** What the Ice Fill puzzle is built out of: the three ice layers, and the polished andesite framing them. */
	private static final Set<Material> ICE_FILL_FIXTURES =
			EnumSet.of(Material.ICE, Material.PACKED_ICE, Material.POLISHED_ANDESITE);

	/**
	 * Blocks of the STATIC MAP that must never be broken, <b>at any point</b>: secret chests, the Quiz answer
	 * buttons, Wither Essence skulls, and the Ice Fill puzzle's ice and polished andesite.
	 * <p>
	 * <b>Nothing here is gated on the clear phase</b>, because what it protects against isn't: the Dungeonbreaker is
	 * the one item that breaks anything, it works whatever the run is doing, and its break is permanent
	 * ({@code setType(AIR)} straight into the world).  So one break outside a run edits the map for every run after
	 * it - a chest coordinate with something else sitting in it, an essence skull simply gone, or a hole in an Ice
	 * Fill layer, which {@code PuzzleIceFill.begin} absorbs silently because it scans for the ice that is there.
	 * (The Stonk's break is only a 200-tick removal, but losing any of these for 200 ticks mid-run is no better;
	 * this check sits above the tool branch in {@code onBlockBreak} and so covers both.  Survival/creative would
	 * bypass all of it, which is why nobody should be in either.)
	 * <p>
	 * This used to be two methods - a phase-gated {@code isProtectedBlock} for the chests and buttons alongside this
	 * one - which was a distinction with no reason behind it once both answers were "never breakable".  Every
	 * predicate here reads STATIC data (secret coordinates, button coordinates, materials, room bounds), so none of
	 * it needs a run to be in progress to answer.
	 * <p>
	 * Wither skulls are matched by MATERIAL anywhere, not against the secret list, because a skull that is not a
	 * registered secret is still part of the map and there is no reason to be able to break one.  Ice and polished
	 * andesite are matched only inside the Ice Fill room, since both are ordinary building blocks elsewhere.
	 */
	public static boolean isMapFixture(Block b) {
		if(b == null) return false;
		if(ESSENCE_SKULLS.contains(b.getType())) return true;
		if(ICE_FILL_FIXTURES.contains(b.getType()) && Rooms.roomAt(b.getLocation()) == Rooms.ICE_FILL) return true;
		if(PuzzleQuiz.isButtonArea(b)) return true;
		for(Room r : Rooms.all()) {
			for(Secret s : r.secrets) {
				if(s.isChest() && b.getX() == s.blockX() && b.getY() == s.blockY() && b.getZ() == s.blockZ()) return true;
			}
		}
		return false;
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
