package items;

import instructions.Server;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.maxor.Maxor;
import items.armor.ThermodynamicHelmet;
import listeners.LavaJump;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import plugin.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The item behaviour that is genuinely SHARED, and the per-run block state that goes with it.
 * <p>
 * Everything here was a private static on {@code listeners/CustomItems} and has more than one owner, so it could
 * not move onto an item class without one item reaching into another.  Three groups:
 * <ol>
 *   <li><b>Shared combat</b> - the mage beam and its hand-rolled ray-AABB test (every mage weapon fires it), the
 *       thrown axe (the Axe of the Shredded and a Berserk's {@code drop stack}), the guided-carrier flight (the
 *       Spirit Sceptre's bat and the Mage's sheep), the Superboom radius (the TNT, Explosive Shot and both
 *       carriers) and the entity-type blacklist every AoE reads.</li>
 *   <li><b>World state with teardown obligations</b> - the stonk, crypt and Superboom-wall restorations.  These
 *       keep their RAW {@code Bukkit.getScheduler().runTaskLater}, deliberately NOT {@code Utils.scheduleTask}:
 *       a tracked task dies with {@code cancelAllScheduled()}, which would leave permanent AIR holes and
 *       orphaned crypt mobs, so the flushes below are the force-restore instead.</li>
 *   <li><b>Class and set predicates</b> - {@link #isMageClass}, {@link #effectiveCooldown}, {@link #isThermoSet}.</li>
 * </ol>
 * The dispatcher keeps only what is a property of the CLICK rather than of an item: the rate gates, the melee
 * path and the mage-beam/left-click-is-an-ability tests, which depend on the holder's class.
 */
public final class ItemUtils {
	private ItemUtils() {}

	// True while mageBeam's damage call is on the stack.  Damage is applied synchronously, so
	// damage/Damage.witherHurtSound reads this to skip its at-location broadcast for beam hits: the beam routes
	// its own constant-volume hurt sound to the beamer, so an at-location one would double up and be
	// distance-attenuated.
	public static boolean beamDamageInProgress = false;

	private static final Map<UUID, Integer> lastWitherShieldSoundTick = new ConcurrentHashMap<>();

	public static final Map<Location, BlockData> pendingStonkRestorations = new HashMap<>();
	public static final Map<Location, BukkitTask> pendingStonkTasks = new HashMap<>();

	// Crypt + Superboom-wall restorations. Mirrors the stonk maps above: a crypt/wall is temporarily set to AIR and
	// restored after SUPERBOOM_REGEN_TICKS via a raw scheduler task (NOT Utils.scheduleTask), so /reset and /setup can
	// flush them immediately via flushBlockRestorations(). Using Utils.scheduleTask here would let Reset's
	// cancelAllScheduled() kill the pending restoration, leaving permanent AIR holes and orphaned crypt mobs.
	private static final Map<Location, BlockData> pendingBlockRestorations = new HashMap<>();
	private static final List<BukkitTask> pendingBlockTasks = new ArrayList<>();
	private static final List<Zombie> pendingCryptMobs = new ArrayList<>();

	// Ticks a crypt or a Superboom'd cracked-brick wall stays open before it grows back. Shared by both so the two
	// halves of one explosion can't regenerate at different times.
	private static final int SUPERBOOM_REGEN_TICKS = 100;

	// DETECTION radius of every explosion that routes through triggerSuperboomRadius: Superboom TNT, Explosive Shot
	// and Guided Sheep.  This is the FIRST of the two searches, a cube half-extent around the impact block scanned
	// for a *valid* crypt/wall block.  It uses Chebyshev distance with no line-of-sight test, so air neither triggers
	// nor blocks it.  2 → a 5x5x5 box.  The SECOND search runs per hit block in triggerSuperboomAt: the crypt
	// rectangle validation in checkAndActivateCrypt and the cracked-brick 6-face flood-fill, which decide how much is
	// actually removed.  That one is deliberately NOT scaled by this constant.  Reach is separate again, and comes
	// from vanilla's interaction range (see superboom).
	private static final int SUPERBOOM_RADIUS = 2;

	// Tick of the last Superboom-TNT detonation per player.  The TNT detonates either from the ability dispatch (any
	// click path) or from a raw vanilla placement caught in onInfinityboomPlace, so this caps it to one blast per
	// player per tick.  A click that somehow reaches both paths won't double-boom.
	private static final Map<UUID, Integer> lastSuperboomTick = new ConcurrentHashMap<>();

	/** Crypts already blown up this run, keyed by min-corner.  A crypt can't be farmed for repeated kills. */
	private static final Set<String> activatedCrypts = new HashSet<>();

	/** How far the beam may travel from a mob's true hitbox and still count as a hit (each face inflated by this). */
	private static final double MAGE_BEAM_LENIENCY = 0.5;

	public static String getID(ItemStack item) {
		if(item == null || !item.hasItemMeta()) {
			return "";
		} else if(!item.getItemMeta().hasLore()) {
			return "";
		} else return Utils.firstLorePlain(item.getItemMeta());
	}

	public static List<EntityType> doNotKill() {
		List<EntityType> doNotKill = new ArrayList<>();
		doNotKill.add(EntityType.ACACIA_BOAT);
		doNotKill.add(EntityType.ACACIA_CHEST_BOAT);
		doNotKill.add(EntityType.ALLAY);
		doNotKill.add(EntityType.ARMOR_STAND);
		doNotKill.add(EntityType.ARROW);
		doNotKill.add(EntityType.AXOLOTL);
		doNotKill.add(EntityType.BLOCK_DISPLAY);
		doNotKill.add(EntityType.BIRCH_BOAT);
		doNotKill.add(EntityType.BIRCH_CHEST_BOAT);
		doNotKill.add(EntityType.CAT);
		doNotKill.add(EntityType.CHERRY_BOAT);
		doNotKill.add(EntityType.CHERRY_CHEST_BOAT);
		doNotKill.add(EntityType.CHEST_MINECART);
		doNotKill.add(EntityType.COMMAND_BLOCK_MINECART);
		doNotKill.add(EntityType.DARK_OAK_BOAT);
		doNotKill.add(EntityType.DARK_OAK_CHEST_BOAT);
		doNotKill.add(EntityType.DONKEY);
		doNotKill.add(EntityType.DRAGON_FIREBALL);
		doNotKill.add(EntityType.FIREBALL);
		doNotKill.add(EntityType.EGG);
		doNotKill.add(EntityType.ENDER_PEARL);
		doNotKill.add(EntityType.EXPERIENCE_BOTTLE);
		doNotKill.add(EntityType.EXPERIENCE_ORB);
		doNotKill.add(EntityType.FALLING_BLOCK);
		doNotKill.add(EntityType.FIREWORK_ROCKET);
		doNotKill.add(EntityType.FISHING_BOBBER);
		doNotKill.add(EntityType.FURNACE_MINECART);
		doNotKill.add(EntityType.GLOW_ITEM_FRAME);
		doNotKill.add(EntityType.HOPPER_MINECART);
		doNotKill.add(EntityType.HORSE);
		doNotKill.add(EntityType.ITEM_FRAME);
		doNotKill.add(EntityType.ITEM_DISPLAY);
		doNotKill.add(EntityType.INTERACTION);
		doNotKill.add(EntityType.JUNGLE_BOAT);
		doNotKill.add(EntityType.JUNGLE_CHEST_BOAT);
		doNotKill.add(EntityType.LEASH_KNOT);
		doNotKill.add(EntityType.LIGHTNING_BOLT);
		doNotKill.add(EntityType.LLAMA);
		doNotKill.add(EntityType.LLAMA_SPIT);
		doNotKill.add(EntityType.MANGROVE_BOAT);
		doNotKill.add(EntityType.MANGROVE_CHEST_BOAT);
		doNotKill.add(EntityType.MARKER);
		doNotKill.add(EntityType.MINECART);
		doNotKill.add(EntityType.MULE);
		doNotKill.add(EntityType.OAK_BOAT);
		doNotKill.add(EntityType.OAK_CHEST_BOAT);
		doNotKill.add(EntityType.OCELOT);
		doNotKill.add(EntityType.PAINTING);
		doNotKill.add(EntityType.PARROT);
		doNotKill.add(EntityType.SHULKER_BULLET);
		doNotKill.add(EntityType.SKELETON_HORSE);
		doNotKill.add(EntityType.SMALL_FIREBALL);
		doNotKill.add(EntityType.SNOWBALL);
		doNotKill.add(EntityType.SPAWNER_MINECART);
		doNotKill.add(EntityType.SPECTRAL_ARROW);
		doNotKill.add(EntityType.SPRUCE_BOAT);
		doNotKill.add(EntityType.SPRUCE_CHEST_BOAT);
		doNotKill.add(EntityType.TEXT_DISPLAY);
		doNotKill.add(EntityType.TNT);
		doNotKill.add(EntityType.TRIDENT);
		doNotKill.add(EntityType.UNKNOWN);
		doNotKill.add(EntityType.VILLAGER);
		doNotKill.add(EntityType.WITHER_SKULL);
		doNotKill.add(EntityType.WOLF);
		return doNotKill;
	}

	public static void playWitherShieldSound(Player p) {
		int currentTick = MinecraftServer.currentTick;
		Integer lastTick = lastWitherShieldSoundTick.get(p.getUniqueId());
		if(lastTick == null || currentTick - lastTick >= 100) {
			Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 0.66666f);
			lastWitherShieldSoundTick.put(p.getUniqueId(), currentTick);
		}
	}

	/**
	 * Teleport a player without touching where they are looking. Yaw/pitch go out as RELATIVE in the position packet,
	 * so the client applies a delta to whatever it is currently looking at instead of being snapped to an absolute
	 * rotation.  A high-ping player who turned their head between clicking and the teleport landing keeps the head
	 * they turned to, rather than being yanked back to the rotation the server last knew about.
	 * <br>
	 * The rotation delta must be ZERO (hence the 0/0 below): relative components are OFFSETS from the current
	 * rotation, not absolutes (vanilla {@code PositionMoveRotation.calculateAbsolute}), so passing the player's own
	 * yaw in would add it on top and spin them.
	 * <br>
	 * Goes through the connection rather than {@code Player#teleport(Location, cause, TeleportFlag...)}: Paper
	 * deprecated {@code TeleportFlag.Relative.X/Y/Z/YAW/PITCH} for removal in 1.21.3, leaving no Bukkit-API way to
	 * ask for a relative rotation.  This overload is CraftBukkit's own, so it still fires {@code PlayerTeleportEvent}
	 * (cause PLUGIN) and honours a cancel.  Same-world only, which every caller here is.
	 */
	public static void noRotateTeleport(Player p, Location l) {
		ServerPlayer sp = ((CraftPlayer) p).getHandle();
		sp.connection.teleport(
				new PositionMoveRotation(new Vec3(l.getX(), l.getY(), l.getZ()), Vec3.ZERO, 0f, 0f),
				Set.of(Relative.Y_ROT, Relative.X_ROT),
				PlayerTeleportEvent.TeleportCause.PLUGIN);
	}

	/** Clear the per-run crypt-farm guard (called at run start). */
	public static void resetCrypts() {
		activatedCrypts.clear();
	}

	public static boolean checkAndActivateCrypt(Block clicked, Player p) {
		// There are no crypts in the boss arena - it's a clear-phase secret - but the arena's decorative
		// smooth-stone-slab / stone-brick-stair terrain passes the rectangle test anyway (a lone bottom slab with
		// air under it is a valid 1x1 crypt, and any gold block in that layer makes it a Prince).  So every
		// Superboom thrown in there opened a hole in the floor and handed out a free Crypt Lurker or Prince,
		// which then counted toward the clear phase's bonus score.  Refuse before validating anything; returning
		// false lets triggerSuperboomAt fall through to the cracked-brick flood-fill, which IS wanted in there.
		if(LavaJump.isInBossArena(clicked.getLocation())) return false;
		Material type = clicked.getType();
		int slabY;

		if(type == Material.SMOOTH_STONE_SLAB || type == Material.GOLD_BLOCK) {
			slabY = clicked.getY();
		} else if(type == Material.STONE_BRICK_STAIRS) {
			slabY = clicked.getY() + 1;
		} else {
			return false;
		}

		// Flood-fill horizontally to collect slab-layer blocks
		Set<Block> slabBlocks = new HashSet<>();
		Queue<Block> queue = new LinkedList<>();
		World world = clicked.getWorld();
		Block startBlock = world.getBlockAt(clicked.getX(), slabY, clicked.getZ());
		Material startType = startBlock.getType();
		if(startType != Material.SMOOTH_STONE_SLAB && startType != Material.GOLD_BLOCK) return false;
		if(startType == Material.SMOOTH_STONE_SLAB) {
			BlockData bd = startBlock.getBlockData();
			if(bd instanceof Slab slab && slab.getType() == Slab.Type.TOP) return false;
		}
		queue.add(startBlock);
		slabBlocks.add(startBlock);

		while(!queue.isEmpty() && slabBlocks.size() <= 1000) {
			Block current = queue.poll();
			for(BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
				Block neighbor = current.getRelative(face);
				if(slabBlocks.contains(neighbor)) continue;
				Material nType = neighbor.getType();
				if(nType == Material.SMOOTH_STONE_SLAB) {
					BlockData bd = neighbor.getBlockData();
					if(bd instanceof Slab slab && slab.getType() == Slab.Type.TOP) continue;
					slabBlocks.add(neighbor);
					queue.add(neighbor);
				} else if(nType == Material.GOLD_BLOCK) {
					slabBlocks.add(neighbor);
					queue.add(neighbor);
				}
			}
		}
		if(slabBlocks.size() > 1000) return false;

		// Compute bounding box
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for(Block b : slabBlocks) {
			if(b.getX() < minX) minX = b.getX();
			if(b.getX() > maxX) maxX = b.getX();
			if(b.getZ() < minZ) minZ = b.getZ();
			if(b.getZ() > maxZ) maxZ = b.getZ();
		}

		// Verify full rectangle
		int expectedSize = (maxX - minX + 1) * (maxZ - minZ + 1);
		if(slabBlocks.size() != expectedSize) return false;
		Set<Long> slabPositions = new HashSet<>();
		for(Block b : slabBlocks) {
			slabPositions.add(((long) (b.getX() - minX)) * 10000 + (b.getZ() - minZ));
		}
		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				if(!slabPositions.contains(((long) (x - minX)) * 10000 + (z - minZ))) return false;
			}
		}

		// Validate bottom layer
		Set<Block> stairBlocks = new HashSet<>();
		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				Block below = world.getBlockAt(x, slabY - 1, z);
				Material bType = below.getType();
				if(bType == Material.STONE_BRICK_STAIRS) {
					stairBlocks.add(below);
				} else if(bType != Material.AIR) {
					return false;
				}
			}
		}

		boolean isPrince = slabBlocks.stream().anyMatch(b -> b.getType() == Material.GOLD_BLOCK);

		// A crypt can only be farmed once: it still opens + restores visually, but no new lurker spawns on repeat.
		String cryptKey = minX + "," + slabY + "," + minZ;
		boolean alreadyBlownUp = !activatedCrypts.add(cryptKey);
		if(alreadyBlownUp) {
			p.sendMessage(Utils.msg("<red>You have already blown up this crypt!"));
		}

		// Store block data
		Map<Location, BlockData> stored = new HashMap<>();
		for(Block b : slabBlocks) {
			stored.put(b.getLocation(), b.getBlockData().clone());
			b.setType(Material.AIR, false);
		}
		for(Block b : stairBlocks) {
			stored.put(b.getLocation(), b.getBlockData().clone());
			b.setType(Material.AIR, false);
		}

		Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

		double centerX = (minX + maxX) / 2.0 + 0.5;
		double centerZ = (minZ + maxZ) / 2.0 + 0.5;
		Location spawnLoc = new Location(world, centerX, slabY - 1, centerZ);
		Zombie mob = alreadyBlownUp ? null : Server.spawnCryptLurker(spawnLoc, isPrince);

		pendingBlockRestorations.putAll(stored);
		if(mob != null) pendingCryptMobs.add(mob);
		BukkitTask[] holder = new BukkitTask[1];
		holder[0] = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			for(Map.Entry<Location, BlockData> entry : stored.entrySet()) {
				entry.getKey().getBlock().setBlockData(entry.getValue(), false);
				pendingBlockRestorations.remove(entry.getKey());
			}
			if(mob != null) {
				if(mob.isValid()) {
					// The lurker/prince was never killed before the crypt regenerated, so this crypt doesn't
					// count as "used".  Un-mark it so it can be blown up again for another attempt at the kill.
					// A killed lurker leaves the mob invalid here, so the key stays and the crypt is spent.
					mob.remove();
					activatedCrypts.remove(cryptKey);
				}
				pendingCryptMobs.remove(mob);
			}
			pendingBlockTasks.remove(holder[0]);
		}, SUPERBOOM_REGEN_TICKS);
		pendingBlockTasks.add(holder[0]);

		return true;
	}

	/**
	 * Detonate a Superboom TNT centred on {@code center}, at most once per player per tick.  Shared by the ability
	 * dispatch (every click path) and by {@code CustomItems.onCustomBlockPlace}, which covers the paths where
	 * vanilla physically places the TNT block instead of a click firing the ability.
	 */
	public static void superboomAt(Player p, Location center) {
		int currentTick = MinecraftServer.currentTick;
		if(currentTick == lastSuperboomTick.getOrDefault(p.getUniqueId(), -1)) return;
		lastSuperboomTick.put(p.getUniqueId(), currentTick);
		triggerSuperboomRadius(center, p);
	}

	public static void triggerSuperboomRadius(Location center, Player p) {
		triggerSuperboomRadius(center, p, new HashSet<>());
	}

	public static void triggerSuperboomRadius(Location center, Player p, Set<Block> visited) {
		// Notify Goldor of any explosion-style impact (Superboom, Explosive Shot, Guided Sheep all route through here).
		instructions.bosses.goldor.Goldor.INSTANCE.notifyExplosionAt(center);
		World world = center.getWorld();
		int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
		for(int dx = -SUPERBOOM_RADIUS; dx <= SUPERBOOM_RADIUS; dx++) {
			for(int dy = -SUPERBOOM_RADIUS; dy <= SUPERBOOM_RADIUS; dy++) {
				for(int dz = -SUPERBOOM_RADIUS; dz <= SUPERBOOM_RADIUS; dz++) {
					Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
					Material type = b.getType();
					if((type == Material.SMOOTH_STONE_SLAB || type == Material.GOLD_BLOCK || type == Material.STONE_BRICK_STAIRS || type == Material.CRACKED_STONE_BRICKS) && visited.add(b)) {
						triggerSuperboomAt(b, p);
					}
				}
			}
		}
	}

	public static void triggerSuperboomAt(Block block, Player p) {
		// 1. Try crypt
		if(block.getType() == Material.SMOOTH_STONE_SLAB || block.getType() == Material.GOLD_BLOCK || block.getType() == Material.STONE_BRICK_STAIRS) {
			if(checkAndActivateCrypt(block, p)) return;
		}

		// 2. Cracked stone bricks flood-fill
		if(block.getType() != Material.CRACKED_STONE_BRICKS) return;

		Set<Block> connected = new HashSet<>();
		Queue<Block> queue = new LinkedList<>();
		queue.add(block);
		connected.add(block);
		while(!queue.isEmpty()) {
			Block current = queue.poll();
			for(BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
				Block neighbor = current.getRelative(face);
				if(neighbor.getType() == Material.CRACKED_STONE_BRICKS && connected.add(neighbor)) {
					queue.add(neighbor);
				}
			}
		}

		Map<Location, BlockData> original = new HashMap<>();
		for(Block b : connected) {
			original.put(b.getLocation(), b.getBlockData().clone());
			b.setType(Material.AIR, false);
		}
		Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
		pendingBlockRestorations.putAll(original);
		BukkitTask[] holder = new BukkitTask[1];
		holder[0] = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			for(Map.Entry<Location, BlockData> entry : original.entrySet()) {
				entry.getKey().getBlock().setBlockData(entry.getValue(), false);
				pendingBlockRestorations.remove(entry.getKey());
			}
			pendingBlockTasks.remove(holder[0]);
		}, SUPERBOOM_REGEN_TICKS);
		pendingBlockTasks.add(holder[0]);
	}

	// ===================== guided carriers =====================
	// The Spirit Sceptre's Guided Bat and the Mage's Guided Sheep are the same projectile with a different animal
	// and a different damage rule, so the FLIGHT lives here once.  They used to disagree: the sheep only noticed
	// solid blocks and flew straight through every mob on the way.

	/** How far a guided carrier moves each tick, in blocks. */
	private static final double GUIDED_SPEED = 1;

	/** How long a carrier may fly before it gives up and detonates where it is: <b>10 seconds</b>. */
	private static final int GUIDED_MAX_TICKS = 200;

	/** How close a mob has to be to the carrier's next position to stop it. */
	private static final double GUIDED_HIT_RANGE = 1;

	/**
	 * Marks a carrier in flight.  Two abilities can be airborne at once, and both spawn a living animal that is not
	 * on the {@link #doNotKill()} list, so without this a bat would stop dead on a sheep - and on itself, since its
	 * own hitbox is inside the sphere it scans a block ahead.  <b>Not</b> a test on {@code isInvulnerable()}, which
	 * would look equivalent and is not: Goldor is set invulnerable for his whole phase and still takes ability
	 * damage through his clamp, so that test would quietly make him immune to both abilities.
	 */
	private static final String GUIDED_TAG = "TASGuidedCarrier";

	/**
	 * Fly an already-spawned animal at {@link #GUIDED_SPEED} a tick in the direction its caster is <b>currently</b>
	 * looking, then detonate it on the first mob or solid block it reaches.
	 * <p>
	 * <b>The direction is re-read EVERY tick, which is what "guided" means</b> - the Spirit Sceptre's tooltip says
	 * the bat "follows your aim", so turning your head steers it in flight, and it will happily come back at you.
	 * Only the DAMAGE is settled at launch (see below); the heading is live.  The caller spawns the carrier so it
	 * can set whatever is specific to its species (the sheep's colour, the bat's awake flag); everything that makes
	 * it an inert projectile is applied here.
	 * <p>
	 * The blast routes through {@link #triggerSuperboomRadius}, so both abilities open crypts and cracked-brick
	 * walls exactly as the TNT does - which is what the Guided Sheep already did and is the whole reason it is
	 * useful in a clear.
	 * <p>
	 * <b>A raw {@code runTaskTimer}, not {@code Utils.scheduleTask}</b>, matching the flight the sheep always had:
	 * the carrier is invulnerable and self-removing, so the worst a teardown mid-flight can leave is one animal that
	 * detonates harmlessly a moment later.
	 * <p>
	 * <b>This knows nothing about damage.</b>  The caster settles the figure and writes it onto the carrier through
	 * {@code damage/GuidedCarriers} before launching, and the blast reads it back off the entity - the same "carry
	 * your damage with you" rule arrows follow (§1.0.5).  An unstamped carrier flies and explodes for nothing, which
	 * is a legitimate thing to want.
	 *
	 * @param blastRadius how far from the impact point mobs are hit, in blocks
	 */
	public static void launchGuided(Player p, LivingEntity carrier, double blastRadius) {
		carrier.setAI(false);
		carrier.setGravity(false);
		carrier.setInvulnerable(true);
		carrier.setSilent(true);
		carrier.customName(null);
		carrier.setCustomNameVisible(false);
		carrier.setCollidable(false);
		carrier.addScoreboardTag("TASNoName");
		carrier.addScoreboardTag(GUIDED_TAG);
		PlayerCollision.addEntityToNoCollisionTeam(carrier);

		// Built ONCE per launch, not per tick: doNotKill() allocates its list on every call and this scans for a
		// target every tick for up to GUIDED_MAX_TICKS.
		List<EntityType> doNotKill = doNotKill();

		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				// Out of time, gone, or the caster left - there is nothing left to steer it, so it goes off where
				// it is.  The damage was stamped at launch, so a carrier whose caster has quit still hits for the
				// figure they cast it with.
				if(ticks++ >= GUIDED_MAX_TICKS || !carrier.isValid() || !p.isOnline()) {
					detonateGuided(p, carrier, carrier.getLocation(), blastRadius, doNotKill);
					cancel();
					return;
				}
				Vector velocity = p.getEyeLocation().getDirection().normalize().multiply(GUIDED_SPEED);
				Location next = carrier.getLocation().add(velocity);
				if(next.getBlock().getType().isSolid() || firstMobNear(next, doNotKill) != null) {
					detonateGuided(p, carrier, next, blastRadius, doNotKill);
					cancel();
					return;
				}
				// Point it where it is going, or a steered carrier keeps the heading it spawned facing and reads as
				// a sheep sliding sideways through the air.
				next.setDirection(velocity);
				carrier.teleport(next);
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	/**
	 * Blow a carrier up at {@code center}: the effect, the Superboom pass, then the stamped damage against every mob
	 * in range, then the "hit N enemies" line.  The carrier is removed LAST, because the stamp is read off it.
	 * <p>
	 * Only a hit that {@code deal} <b>reported</b> above zero is counted or summed, so a blast that catches an
	 * armoured wither or a villager NPC does not claim it.
	 */
	private static void detonateGuided(Player p, LivingEntity carrier, Location center, double blastRadius,
			List<EntityType> doNotKill) {
		center.getWorld().spawnParticle(Particle.EXPLOSION, center, 10, 0.5, 0.5, 0.5, 0);
		center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
		triggerSuperboomRadius(center, p);
		int damaged = 0;
		double dealt = 0;
		for(Entity nearby : center.getWorld().getNearbyEntities(center, blastRadius, blastRadius, blastRadius)) {
			if(!isGuidedTarget(nearby, doNotKill)) continue;
			double hit = damage.GuidedCarriers.hit(carrier, p, (LivingEntity) nearby);
			if(hit > 0) {
				dealt += hit;
				damaged++;
			}
		}
		damage.Damage.reportAoe(p, damage.GuidedCarriers.abilityName(carrier), damaged, dealt);
		PlayerCollision.removeEntityFromNoCollisionTeam(carrier);
		carrier.remove();
	}

	/** The first mob close enough to {@code point} to stop a carrier, or null. */
	private static LivingEntity firstMobNear(Location point, List<EntityType> doNotKill) {
		return firstMobNear(point, GUIDED_HIT_RANGE, doNotKill);
	}

	/**
	 * The first mob within {@code range} of {@code point}, or null.  Shared with Explosive Shot, whose arrows stop
	 * and detonate on contact rather than piercing, and which wants the same "what counts as a mob" rule.
	 */
	public static LivingEntity firstMobNear(Location point, double range) {
		return firstMobNear(point, range, doNotKill());
	}

	private static LivingEntity firstMobNear(Location point, double range, List<EntityType> doNotKill) {
		for(Entity nearby : point.getWorld().getNearbyEntities(point, range, range, range)) {
			if(isGuidedTarget(nearby, doNotKill)) return (LivingEntity) nearby;
		}
		return null;
	}

	/**
	 * Whether a guided carrier may stop on, and damage, this entity.  The same rule the other AoE abilities use -
	 * never a player, real, fake or spectating - plus one of its own: never another carrier, or itself.
	 */
	private static boolean isGuidedTarget(Entity e, List<EntityType> doNotKill) {
		if(!(e instanceof LivingEntity mob) || e instanceof Player) return false;
		if(e.getScoreboardTags().contains(GUIDED_TAG) || doNotKill.contains(e.getType())) return false;
		if(mob.isDead() || mob.getHealth() <= 0) return false;
		return !(e instanceof Wither wither && wither.getInvulnerableTicks() != 0);
	}

	public static void stonk(Player p, Block b) {
		if(Goldor.INSTANCE.isProtected(b) || Maxor.INSTANCE.isProtected(b)) return;
		if(b.getType().getHardness() != -1) {
			BlockData data = b.getBlockData().clone();
			Location loc = b.getLocation();
			Utils.debug(Utils.DebugType.SERVER, p.getName() + " Stonking block at " + Utils.round(loc.getX(), 3) + " " + Utils.round(loc.getY(), 5) + " " + Utils.round(loc.getZ(), 3));

			b.setType(Material.AIR, false); // no-physics: attached neighbours (carpets, portals, …) don't pop off
			pendingStonkRestorations.put(loc, data);
			BukkitTask task = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
				// applyPhysics=false, same as the break above and every other restore path.  With physics the block
				// runs its own canSurvive check on placement, so a carpet whose support was stonked too pops straight
				// back off and never comes back.  It also re-shapes the six neighbours, tearing off whatever is
				// attached to them.  setBlockData carries the material, so no separate setType is needed.
				b.setBlockData(data, false);
				pendingStonkRestorations.remove(loc);
				pendingStonkTasks.remove(loc);
			}, 200);
			pendingStonkTasks.put(loc, task);
		}
	}

	public static void flushStonkRestorations() {
		pendingStonkTasks.values().forEach(BukkitTask::cancel);
		pendingStonkTasks.clear();
		for(Map.Entry<Location, BlockData> entry : pendingStonkRestorations.entrySet()) {
			entry.getKey().getBlock().setBlockData(entry.getValue(), false);
		}
		pendingStonkRestorations.clear();
	}

	/**
	 * Immediately restore every superboomed wall / crypt currently set to AIR and despawn any active crypt mobs,
	 * cancelling their pending 40-tick restorations. Mirrors {@link #flushStonkRestorations()}; called from
	 * Server.serverSetup so /reset and /setup replace all crypts and walls at once.
	 */
	public static void flushBlockRestorations() {
		pendingBlockTasks.forEach(BukkitTask::cancel);
		pendingBlockTasks.clear();
		for(Map.Entry<Location, BlockData> entry : pendingBlockRestorations.entrySet()) {
			entry.getKey().getBlock().setBlockData(entry.getValue(), false);
		}
		pendingBlockRestorations.clear();
		for(Zombie mob : pendingCryptMobs) {
			if(mob.isValid()) mob.remove();
		}
		pendingCryptMobs.clear();
	}

	/**
	 * The shared thrown-axe projectile: an ItemDisplay flying 100 blocks, spinning, damaging what it passes
	 * through.  Used by the Axe of the Shredded ({@code pierce} true) and by a Berserk's {@code drop stack}
	 * ability, which copies it but does NOT pierce (§1.14).
	 *
	 * @param ability the ability's display name, for the "hit N enemies" line it prints when the axe is spent
	 * @param derived what {@code core} IS.  False for the Axe of the Shredded, whose core is a stat core and still
	 *                needs the target half at {@code meleeFinish}.  True for the Berserk throw, whose core was read
	 *                out of the damage history and is therefore a FINISHED hit: running the target half on it would
	 *                charge for the Rulers, the repeated-hit stack and the class multiplier a second time, and
	 *                recording the result would let each throw read the last one's inflated output (see
	 *                {@link damage.Damage#dealDerived}).
	 */
	public static void throwAxe(Player p, String ability, double core, boolean pierce, boolean derived) {
		Utils.playLocalSound(p, Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);
		ItemStack weapon = p.getInventory().getItemInMainHand();

		// Create the axe item display
		Location startLoc = p.getEyeLocation();
		Vector direction = startLoc.getDirection().normalize();

		// Calculate the horizontal perpendicular to the direction of travel
		// Project direction onto the XZ plane and get perpendicular
		double dx = direction.getX();
		double dz = direction.getZ();

		// The perpendicular in the XZ plane (rotate 90 degrees clockwise when viewed from above)
		Vector spinAxis = new Vector(-dz, 0, dx).normalize();

		// If looking straight up/down (no horizontal component), use player yaw
		if(Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
			float yaw = startLoc.getYaw();
			spinAxis = new Vector(-Math.cos(Math.toRadians(yaw)), 0, -Math.sin(Math.toRadians(yaw)));
		}

		// Spawn an ItemDisplay entity
		ItemDisplay axe = p.getWorld().spawn(startLoc, ItemDisplay.class);
		axe.setItemStack(new ItemStack(Material.DIAMOND_AXE));
		axe.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);

		Vector finalSpinAxis = spinAxis;
		new BukkitRunnable() {
			int distance = 0;
			Location currentLoc = startLoc.clone();
			float spinRotation = 0;
			boolean notedAggro = false;
			final Set<UUID> hit = new HashSet<>();
			// The tally for the "hit N enemies" line, printed ONCE when the axe is spent rather than per mob: a
			// piercing throw can pass through several, and one line per victim would be noise.
			int damaged = 0;
			double dealt = 0;

			private void finish() {
				damage.Damage.reportAoe(p, ability, damaged, dealt);
				axe.remove();
				cancel();
			}

			@Override
			public void run() {
				if(distance >= 100 || !axe.isValid()) {
					finish();
					return;
				}

				// Check if we hit a wall (solid block)
				Location nextLoc = currentLoc.clone().add(direction);
				if(nextLoc.getBlock().getType().isSolid()) {
					finish();
					return;
				}

				// Move 1 block per tick
				currentLoc = nextLoc;

				// Taking aggro when it hits a wither is a REQUIREMENT of this ability, not incidental, so it is
				// noted the first tick the axe overlaps a boss whether or not the damage lands.  These projectiles
				// are one of only three things allowed to aggro a fully shielded wither - the mage beam and the
				// Flaming Flay arc are the others; everything else needs the hit to have dealt real damage.
				boolean stop = false;
				for(Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 2.0, 1.0)) {
					if(!notedAggro && e instanceof Wither w && w.getScoreboardTags().contains("TASWither")) {
						instructions.bosses.WitherActions.noteDamager(p);
						notedAggro = true;
					}
					if(!(e instanceof LivingEntity mob) || e instanceof Player) continue;
					if(mob.isDead() || mob.getHealth() <= 0 || !hit.add(mob.getUniqueId())) continue;
					if(e instanceof Wither w2 && w2.getInvulnerableTicks() != 0) continue;
					double reported;
					if(derived) {
						// The debuffs this hit carries still land (Lethality is a property of the hit, not of the
						// formula); only the damage half is skipped, because it is already in the figure.
						damage.Damage.applyOnHitDebuffs(p, mob, damage.DamagePath.MELEE, weapon);
						reported = damage.Damage.dealDerived(mob, core, damage.DamageKind.NORMAL, p, damage.DamagePath.MELEE);
					} else {
						double sbDamage = damage.Damage.meleeFinish(p, mob, weapon, core);
						reported = damage.Damage.deal(mob, sbDamage, damage.DamageKind.NORMAL, p, damage.DamagePath.MELEE);
					}
					if(reported > 0) {
						dealt += reported;
						damaged++;
					}
					if(!pierce) stop = true;
				}
				if(stop) {
					finish();
					return;
				}

				// Update spin rotation
				spinRotation += 36; // Positive for forward spin

				// Create rotation using axis-angle rotation around the spin axis
				Quaternionf rotation = new Quaternionf().rotateAxis((float) Math.toRadians(spinRotation), (float) finalSpinAxis.getX(), (float) finalSpinAxis.getY(), (float) finalSpinAxis.getZ());

				axe.setTransformation(new Transformation(new Vector3f(0, 0, 0), // No translation offset
						rotation, new Vector3f(1, 1, 1), // Normal scale
						new Quaternionf() // No right rotation
				));

				// Teleport to new position
				axe.teleport(currentLoc);

				distance++;
			}
		}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
	}

	/** True if {@code p} is the Mage CLASS, which drives the ability-cooldown reduction.  Real players carry an
	*  exclusive class scoreboard tag (set by /class); fake players carry none and are identified by name.  All four
	*  fake "MageN" players run the Mage inventory and cast Mage abilities, so every "Mage*"-named fake counts as a
	*  Mage.  Mage2/3/4 cosplay Tank/Berserk/Healer's ROLE but are mechanically mages. */
	public static boolean isMageClass(Player p) {
		if(p.getScoreboardTags().contains("Mage")) return true;
		for(String other : new String[]{"Archer", "Berserk", "Healer", "Tank"}) {
			if(p.getScoreboardTags().contains(other)) return false;
		}
		return p.getName().startsWith("Mage");
	}

	/** A base ability cooldown after the Mage class's cooldown reduction: a SOLO mage gets −75% (quarter cooldown),
	*  but with two or more Mage-class players it's the standard −50% (half).  Non-mages are unchanged.  NOT used for
	*  the Terminator or Salvation, which are weapons, not abilities. */
	public static int effectiveCooldown(Player p, int baseTicks) {
		if(!isMageClass(p)) return baseTicks;
		return mageCount() <= 1 ? baseTicks / 4 : baseTicks / 2;
	}

	/** Number of Mage-class players currently online (see {@link #isMageClass}). */
	private static long mageCount() {
		return Bukkit.getOnlinePlayers().stream().filter(ItemUtils::isMageClass).count();
	}

	/** Tell {@code p} their ability is on cooldown, showing the remaining time in seconds (e.g. "...for 3.45
	*  seconds!").  {@code ticksRemaining} is the ticks left until the ability is usable again. */
	public static void sendCooldownMessage(Player p, int ticksRemaining) {
		String format = String.format("%.2f", Math.max(0, ticksRemaining) / 20.0);
		p.sendMessage(Utils.msg("<red>This ability is on cooldown for " + format + " seconds!"));
		// During a TAS run (not practice) an ability fired on cooldown means the choreography mistimed it, so flag
		// it with the offending tick and player.  Gated behind regular verbose (ON+) so it doesn't spam the console.
		if(!instructions.bosses.WitherActions.isPracticeMode() && Utils.isVerbose()) {
			ItemStack held = p.getInventory().getItemInMainHand();
			String ability = held.hasItemMeta() && held.getItemMeta().hasDisplayName()
					? Utils.displayName(held.getItemMeta()) : String.valueOf(held.getType());
			Utils.debug(Utils.DebugType.ERROR, Utils.getRealName(p) + " tried to use " + ability + Utils.mmLegacy("<red>")
					+ " on cooldown (" + Math.max(0, ticksRemaining) + "t / " + format + "s left)");
		}
	}

	/**
	 * True if the player is wearing the full (4/4) Thermodynamic armor set, which is the one set bonus in the
	 * plugin: it raises the attack-speed cap, i.e. the Terminator's 5-tick cooldown becomes 4.
	 * <p>
	 * Asks each piece which SET it belongs to ({@code Wearable.setId}) instead of substring-matching
	 * "Thermodynamic" in its display name, so an unrelated item that happens to contain the word cannot count
	 * towards it and a second set needs no new predicate.
	 */
	public static boolean isThermoSet(Player p) {
		org.bukkit.inventory.PlayerInventory inv = p.getInventory();
		return wearsSet(inv.getHelmet(), ThermodynamicHelmet.THERMODYNAMIC)
				&& wearsSet(inv.getChestplate(), ThermodynamicHelmet.THERMODYNAMIC)
				&& wearsSet(inv.getLeggings(), ThermodynamicHelmet.THERMODYNAMIC)
				&& wearsSet(inv.getBoots(), ThermodynamicHelmet.THERMODYNAMIC);
	}

	private static boolean wearsSet(ItemStack item, String setId) {
		Wearable worn = ItemRegistry.wearable(item);
		return worn != null && worn.setId().equals(setId);
	}

	public static void mageBeam(Player p) {
		Location l = p.getLocation();

		// Three range tiers, each doubling the previous total (MAP.md §7): 10+15 = 25 by default,
		// 35+15 = 50 in the boss arena, 70+30 = 100 in the Wither King fight.  The Wither King tier needs a PHASE
		// check rather than a coordinate one, because the WK arena already sits inside the boss-arena box.
		double range = damage.Damage.beamRange(p).maxRange();

		// Get player's yaw in radians
		double yaw = Math.toRadians(l.getYaw());

		// Calculate perpendicular vector (90 degrees to the right)
		double rightYaw = yaw + Math.toRadians(90);

		// Calculate offsets (16 pixels = 1 block)
		double offsetX = -Math.sin(rightYaw) * (5.0 / 16.0);
		double offsetZ = Math.cos(rightYaw) * (5.0 / 16.0);
		double offsetY = 1.62 - (13.0 / 16.0);

		// Apply offsets
		l.add(offsetX, offsetY, offsetZ);

		// GEOMETRY: the beam is DRAWN from the right hand (`l`) but AIMED from the eye, so it converges on the
		// crosshair.  The trail is a straight line from the hand to wherever the crosshair ray terminates, so the two
		// coincide at the target end (a mob under the crosshair is where the beam visibly lands) while still leaving
		// the hand, which is what it looks like on Hypixel.
		//
		// This supersedes the earlier fix that cast the damage ray FROM the hand along the look direction.  That made
		// the ray match the trail exactly, but the trail was then *parallel* to the crosshair and permanently offset
		// ~0.31 blocks right and ~0.81 down from it, so the beam visibly never went where you were aiming.  Converging
		// the trail instead fixes the same visual/hit mismatch from the other side, and it re-aligns the real beam
		// with Actions.mageBeamWouldHit, the fake-player fire gate, which has always aimed from the eye.
		//
		// Consequence to keep in mind: only the ENDPOINT is shared.  A mob straddling the hand→target segment but not
		// the crosshair ray is crossed by the trail without being hit.  That's what MAGE_BEAM_LENIENCY (0.5 per face)
		// absorbs, and it's inherent to any hand-origin trail that aims by crosshair.
		Location eye = p.getEyeLocation();
		Vector direction = eye.getDirection();
		Vector eyeVec = eye.toVector();

		// Raytrace both entities and blocks from the eye.  Whichever is closer along the ray is what it actually
		// hits, so if a wall is between the player and an entity, the wall stops the beam and the entity takes no
		// damage.  Entity hits get a MAGE_BEAM_LENIENCY-block margin (see findTargetEntity); blocks stay precise.
		RayTraceResult entityResult = findTargetEntity(p, eye, direction, range);
		RayTraceResult blockResult = p.getWorld().rayTraceBlocks(eye, direction, range, FluidCollisionMode.NEVER, true);

		double entityDist = entityResult != null ? entityResult.getHitPosition().distance(eyeVec) : Double.MAX_VALUE;
		double blockDist = blockResult != null ? blockResult.getHitPosition().distance(eyeVec) : Double.MAX_VALUE;

		Vector targetPoint;
		Entity targetEntity;
		if(entityResult != null && entityDist <= blockDist) {
			targetEntity = entityResult.getHitEntity();
			targetPoint = entityResult.getHitPosition();
		} else if(blockResult != null) {
			targetEntity = null;
			targetPoint = blockResult.getHitPosition();
		} else {
			// Nothing in range: aim at the far end of the crosshair ray so the trail still converges toward it.
			targetEntity = null;
			targetPoint = eyeVec.clone().add(direction.clone().multiply(range));
		}

		// The trail runs hand → target point: that convergence is what makes the beam land on the crosshair.
		Vector handToTarget = targetPoint.clone().subtract(l.toVector());
		double distance = handToTarget.length();
		handToTarget.normalize();

		// Iterations based on distance to target, not max range
		int iterations = (int) (distance / 0.33333);
		Vector v = handToTarget.multiply(0.33333);

		for(int i = 0; i < iterations; i++) {
			spawnFireworkParticle(l);
			l.add(v);
		}

		// A dead mob takes no real damage, so the beam passing through it shouldn't emit a hurt sound.  This also
		// covers a boss wither pinned in its dying state (TASDying, HP frozen at 1, so isDead/health won't flag it).
		boolean targetDead = targetEntity instanceof LivingEntity dead
				&& (dead.isDead() || dead.getHealth() <= 0 || dead.getScoreboardTags().contains("TASDying"));

		// Beam hit sounds are routed ONLY to the beamer (and their spectators) at constant volume.
		// There is no at-location sound, so volume doesn't depend on how far the target is.
		ItemStack held = p.getInventory().getItemInMainHand();
		if(targetEntity instanceof Wither wither && wither.getInvulnerableTicks() != 0) {
			// Armored, e.g. mid-intro before the fight is live: no damage lands, but still record the damager so
			// the boss aggros whoever was hitting it the moment its intro completes and aggro turns on.  The mage
			// beam is one of only three things allowed to do that - the thrown-axe projectiles and the Flaming Flay
			// arc are the others; every other path needs the hit to have dealt real damage (see damage/Damage.deal).
			if(wither.getScoreboardTags().contains("TASWither")) instructions.bosses.WitherActions.noteDamager(p);
			// Debuff stacks land even when the damage does not (MAP.md §7): a beam on an armoured boss
			// still builds Lethality, so the moment it opens up the stacks are already there.  Maxor and Storm
			// cannot be arrow-debuffed before they become vulnerable, which is exactly the case this covers.
			damage.Damage.applyOnHitDebuffs(p, wither, damage.DamagePath.BEAM, held);
			if(!targetDead) Utils.playLocalSound(p, Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
		} else if(targetEntity instanceof LivingEntity temp) {
			// The beam is the MELEE hit rescaled by the Mage Staff passive, then faded by distance across the
			// three range tiers (§7).  Everything the old hardcoded table did is now a formula output: the
			// Hyperion's "-33% against a non-wither" was this same mechanic written inside out (1/1.5 = 0.667)
			// and is now the Hyperion's x1.5 vs Wither; the Rag buff is +150% of the axe's Strength through the
			// stat layer; and the Spring Boots / Racing Helmet penalties are deleted outright, those wearables
			// now costing only the stats their slot would otherwise carry.
			double sbDamage = damage.Damage.beam(p, temp, held, distance);
			// Silence the target during the hit so vanilla doesn't broadcast its hurt sound at the
			// target's location; beamDamageInProgress tells onWitherHurtSound to skip its manual
			// broadcast the same way (withers are permanently silent, so silence can't signal that).
			boolean wasSilent = temp.isSilent();
			temp.setSilent(true);
			beamDamageInProgress = true;
			try {
				damage.Damage.deal(temp, sbDamage, damage.DamageKind.NORMAL, p, damage.DamagePath.BEAM);
			} finally {
				beamDamageInProgress = false;
				temp.setSilent(wasSilent);
			}
			if(!targetDead) {
				String hurtSound = Utils.getHurtSoundKey(temp);
				if(hurtSound != null) Utils.playLocalSound(p, hurtSound, 1.0f, 1.0f);
			}
		}
	}

	/**
	 * The NEAREST mob the beam's ray enters, by {@link #rayBoxDistance} against each hitbox expanded by
	 * {@link #MAGE_BEAM_LENIENCY} - not Bukkit's own ray trace, which stops at the first entity it happens to
	 * find rather than the closest.  Skips players (real, fake and spectating) and anything under Resistance 255,
	 * which is how a mob that must not be hit at all is marked.
	 */
	private static RayTraceResult findTargetEntity(Player p, Location origin, Vector direction, double range) {
		Vector start = origin.toVector();
		double reach = range + MAGE_BEAM_LENIENCY;
		Entity best = null;
		double bestT = Double.MAX_VALUE;
		Vector bestHit = null;
		for(Entity e : p.getWorld().getNearbyEntities(origin, reach, reach, reach)) {
			if(!(e instanceof LivingEntity le) || e instanceof Player || e.isDead()) continue;
			if(le.hasPotionEffect(PotionEffectType.RESISTANCE) && le.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255) continue;
			double t = rayBoxDistance(start, direction, e.getBoundingBox().expand(MAGE_BEAM_LENIENCY), range);
			if(t >= 0 && t < bestT) {
				bestT = t;
				best = e;
				bestHit = start.clone().add(direction.clone().multiply(t));
			}
		}
		return best == null ? null : new RayTraceResult(bestHit, best);
	}

	/**
	 * Distance {@code t} along {@code start + t*direction} (direction assumed unit-length) at which the ray first
	 * enters {@code box}, or {@code -1} if it never does within {@code maxDist}. Returns {@code 0} when the origin
	 * is already inside the box (slab method, clamped at 0).
	 */
	private static double rayBoxDistance(Vector start, Vector direction, BoundingBox box, double maxDist) {
		double[] o = {start.getX(), start.getY(), start.getZ()};
		double[] d = {direction.getX(), direction.getY(), direction.getZ()};
		double[] lo = {box.getMinX(), box.getMinY(), box.getMinZ()};
		double[] hi = {box.getMaxX(), box.getMaxY(), box.getMaxZ()};
		double tmin = 0.0, tmax = maxDist;
		for(int i = 0; i < 3; i++) {
			if(Math.abs(d[i]) < 1e-8) {
				if(o[i] < lo[i] || o[i] > hi[i]) return -1; // parallel to this slab and outside it
			} else {
				double t1 = (lo[i] - o[i]) / d[i];
				double t2 = (hi[i] - o[i]) / d[i];
				if(t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
				tmin = Math.max(tmin, t1);
				tmax = Math.min(tmax, t2);
				if(tmin > tmax) return -1;
			}
		}
		return tmin;
	}

	public static void spawnFireworkParticle(Location l) {
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.FIREWORK, false, false, l.getX(), l.getY(), l.getZ(), 0.0f, 0.0f, 0.0f, 0.0f, 1);
		Utils.broadcastPacket(packet);
	}
}
