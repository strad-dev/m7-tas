package instructions;

import com.mojang.datafixers.util.Pair;
import commands.Spectate;
import listeners.CustomItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R7.block.CraftBlock;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import plugin.FakePlayerManager;
import plugin.M7tas;
import plugin.MovementAudit;
import plugin.PlayerInventoryBackup;
import plugin.Utils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class Actions {

	private static final Map<UUID, BukkitTask> jumpTasks = new HashMap<>();
	private static final Map<UUID, String> activeInputs = new HashMap<>();
	private static final Set<BukkitRunnable> forceMoveTasks = new HashSet<>();
	/** Bumped by {@link #cancelAllMovement()}; move() cleanup tasks scheduled before the bump
	 *  belong to a cancelled run and must not zero out inputs set by the new run. */
	private static int movementGeneration = 0;
	/** UUIDs of mobs already claimed by an in-flight Terminator arrow, mapped to the
	 *  server tick when the claim expires (i.e. when the arrow should have landed). */
	private static final Map<UUID, Integer> terminatorClaimedUntil = new HashMap<>();

	/** Max effective mage-beam range during a boss fight (matches the range used by
	 *  {@link listeners.CustomItems#mageBeam} when {@code LavaJump.isInBossArena} is true).
	 *  Beam-firing/aim helpers short-circuit if no candidate sits within this radius. */
	private static final double MAGE_BEAM_RANGE = 50.0;
	/** Practical max range a Terminator arrow can ballistically reach with a sensible launch
	 *  angle. Used as the candidate-search radius for the auto-aim variant — past this distance
	 *  the trajectory either falls into terrain or takes long enough that the target moves. */
	private static final double TERMINATOR_MAX_RANGE = 100.0;

	public static String getActiveInput(UUID id) {
		return activeInputs.getOrDefault(id, "");
	}

	/**
	 * Forcibly clears a player's movement inputs for the current tick — used to simulate
	 * the server opening a GUI (terminal click in Goldor phase). Does NOT touch activeInputs,
	 * so a fake player's per-tick input ticker will repress the keys on the next tick.
	 */
	public static void clearMovementInput(Player p) {
		if(!(p instanceof CraftPlayer cp)) return;
		ServerPlayer sp = cp.getHandle();
		sp.xxa = 0f;
		sp.zza = 0f;
		sp.setSprinting(false);
		sp.setShiftKeyDown(false);
		sp.setJumping(false);
	}

	// Spigot-mojang compile name is detectEquipmentUpdatesPublic; Paper runtime has only detectEquipmentUpdates.
	// Resolve whichever exists at class-load time.
	private static final java.lang.reflect.Method DETECT_EQUIPMENT_UPDATES = resolveDetectEquipmentUpdates();

	private static java.lang.reflect.Method resolveDetectEquipmentUpdates() {
		for(String name : new String[]{"detectEquipmentUpdates", "detectEquipmentUpdatesPublic"}) {
			try {
				java.lang.reflect.Method m = net.minecraft.world.entity.LivingEntity.class.getMethod(name);
				m.setAccessible(true);
				return m;
			} catch (NoSuchMethodException ignored) {
			}
		}
		throw new IllegalStateException("LivingEntity.detectEquipmentUpdates[Public] not found on runtime");
	}

	/**
	 * Simulates a Player pressing movement input keys.  Only call this when the player changes which keys are pressed.
	 *
	 * @param entity        The LivingEntity sending movement packets
	 * @param input         The input keys being held down<br>Valid Input contains the following characters: W, A, S, D, J, P, N<br>WASD: movement<br>J: Jump<br>P: sPrint<br>N: sNeak
	 * @param durationTicks How long the keys are held down for, or 0 for manual override
	 */
	public static void move(LivingEntity entity, String input, int durationTicks) {
		// only handle CraftLivingEntity/NMS and positive duration
		Utils.debug(Utils.DebugType.CLIENT, entity.getName() + " moving " + input + " for " + durationTicks);
		if(!(entity instanceof CraftLivingEntity craftEntity)) return;
		activeInputs.put(entity.getUniqueId(), input);

		net.minecraft.world.entity.LivingEntity serverPlayer = craftEntity.getHandle();
		if(input.contains("A") && input.contains("D")) {
			serverPlayer.xxa = 0.0F;
		} else {
			serverPlayer.xxa = input.contains("A") ? 1.0F : (input.contains("D") ? -1.0F : 0.0F);
		}

		if(input.contains("W") && input.contains("S")) {
			serverPlayer.zza = 0.0F;
		} else {
			serverPlayer.zza = input.contains("W") ? 1.0F : (input.contains("S") ? -1.0F : 0.0F);
		}

		if(input.contains("W") && !input.contains("S") && !input.contains("N")) {
			serverPlayer.setSprinting(input.contains("P"));
		} else {
			serverPlayer.setSprinting(false);
		}

		serverPlayer.setShiftKeyDown(input.contains("N"));
		if(input.contains("N")) {
			serverPlayer.xxa *= 0.3F;
			serverPlayer.zza *= 0.3F;
		}

		BukkitTask existingJump = jumpTasks.remove(entity.getUniqueId());
		if(existingJump != null) existingJump.cancel();

		if(input.contains("J")) {
			BukkitTask task = new BukkitRunnable() {
				int ticks = 0;

				@Override
				public void run() {
					if(isCancelled() || (durationTicks > 0 && ticks++ >= durationTicks) || serverPlayer.isRemoved()) {
						jumpTasks.remove(entity.getUniqueId());
						cancel();
						return;
					}
					if(serverPlayer.onGround()) {
						serverPlayer.setJumping(true);
						Utils.debug(Utils.DebugType.CLIENT, entity.getName() + " jumped at " + Utils.round(serverPlayer.getX(), 3) + " " + Utils.round(serverPlayer.getY(), 5) + " " + Utils.round(serverPlayer.getZ(), 3));
						if(entity instanceof Player player) MovementAudit.startAirborneAudit(player, "jump");
					}
				}
			}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
			jumpTasks.put(entity.getUniqueId(), task);
		} else {
			serverPlayer.setJumping(false);
		}
		if(durationTicks > 0) {
			int generation = movementGeneration;
			Utils.scheduleTask(() -> {
				if(generation != movementGeneration) return; // stale cleanup from a cancelled run
				if(entity.isValid()) {
					serverPlayer.xxa = 0.0F;
					serverPlayer.zza = 0.0F;
					serverPlayer.setSprinting(false);
					serverPlayer.setShiftKeyDown(false);
					serverPlayer.setJumping(false);
				}
				activeInputs.remove(entity.getUniqueId());
			}, durationTicks);
		}
	}

	/**
	 * Forces a LivingEntity to move in a given direction regardless of physics
	 *
	 * @param entity        The Entity being moved
	 * @param perTick       The distance to move per tick
	 * @param durationTicks Number of ticks to move
	 */
	public static void forceMove(LivingEntity entity, Vector perTick, int durationTicks) {
		// only handle CraftLivingEntity/NMS and positive duration
		if(!(entity instanceof CraftLivingEntity cle) || durationTicks <= 0) return;

		net.minecraft.world.entity.LivingEntity nmsEntity = cle.getHandle();

		// This variable stores the requested motion from the caller.
		Vec3 motion = new Vec3(perTick.getX(), perTick.getY(), perTick.getZ());

		BukkitRunnable runnable = new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if(ticks++ >= durationTicks || nmsEntity.isRemoved()) {
					forceMoveTasks.remove(this);
					cancel();
					return;
				}

				// Get current position
				Vec3 currentPos = nmsEntity.position();

				// Calculate new position by adding the per-tick motion
				Vec3 newPos = currentPos.add(motion);

				// Force teleport to the new position
				nmsEntity.absSnapTo(newPos.x(), newPos.y(), newPos.z(), nmsEntity.getYRot(), nmsEntity.getXRot());

				// Set the delta movement to maintain client-side interpolation
				nmsEntity.setDeltaMovement(motion);

				nmsEntity.hurtMarked = true;

				// Send position update to all nearby players
				if(nmsEntity instanceof ServerPlayer serverPlayer) {
					// For players, use the connection teleport
					serverPlayer.connection.teleport(newPos.x(), newPos.y(), newPos.z(), nmsEntity.getYRot(), nmsEntity.getXRot());
				} else {
					// For non-player entities, send teleport packet to all tracking players
					PositionMoveRotation posRotation = new PositionMoveRotation(newPos, Vec3.ZERO, nmsEntity.getYRot(), nmsEntity.getXRot());
					ClientboundTeleportEntityPacket teleportPacket = ClientboundTeleportEntityPacket.teleport(nmsEntity.getId(), posRotation, Set.of(), nmsEntity.onGround());
					Utils.broadcastPacket(teleportPacket);
				}
			}
		};
		forceMoveTasks.add(runnable);
		runnable.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	/**
	 * Hard-stops all fake-player movement: held input keys, jump tasks, forceMove runnables,
	 * and momentum. Pending move() cleanup tasks from before this call become no-ops, so they
	 * cannot zero out inputs set by a freshly started run. Called on TAS start and /reset.
	 */
	public static void cancelAllMovement() {
		movementGeneration++;

		for(BukkitTask task : jumpTasks.values()) task.cancel();
		jumpTasks.clear();
		for(BukkitRunnable runnable : new ArrayList<>(forceMoveTasks)) runnable.cancel();
		forceMoveTasks.clear();
		activeInputs.clear();

		for(Player p : FakePlayerManager.getFakePlayers().values()) {
			if(!(p instanceof CraftPlayer craftPlayer)) continue;
			ServerPlayer serverPlayer = craftPlayer.getHandle();
			serverPlayer.xxa = 0.0F;
			serverPlayer.zza = 0.0F;
			serverPlayer.setSprinting(false);
			serverPlayer.setShiftKeyDown(false);
			serverPlayer.setJumping(false);
			serverPlayer.setDeltaMovement(Vec3.ZERO);
			serverPlayer.hurtMarked = true;
		}
	}

	/**
	 * Makes a Player entity perform a jump if it is currently on the ground.
	 *
	 * @param p The Player entity that is to perform the jump. Requires the player to be on the ground.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new move() with jump input being set to true")
	public static void jump(Player p) {
		if(!(p instanceof CraftPlayer cp)) {
			return;
		}

		ServerPlayer npc = cp.getHandle();

		if(npc.onGround()) { // onGround check
			Vec3 motion = npc.getDeltaMovement();
			npc.setDeltaMovement(new Vec3(motion.x(), 0.42D, motion.z()));
		} else {
			Bukkit.getLogger().warning("Failed jump due to not being on ground!");
		}
	}

	/**
	 * Change which hotbar slot (0–8) the player is “holding”,
	 * and broadcast that slot change.
	 *
	 * @param p           The player whose hotbar slot is to be changed.
	 * @param hotbarIndex The index of the new hotbar slot (0–8).
	 */
	public static void setHotbarSlot(Player p, int hotbarIndex) {
		if(!(p instanceof CraftPlayer cp)) {
			return;
		}
		ServerPlayer npc = cp.getHandle();

		// Update the NMS held-slot index
		npc.getInventory().setSelectedSlot(hotbarIndex);

		// Reconcile item-attached attribute modifiers (e.g. Dungeonbreaker's BLOCK_BREAK_SPEED)
		// with the AttributeMap on the same frame as the slot change, instead of waiting for
		// the level's next entity-tick phase.
		try {
			DETECT_EQUIPMENT_UPDATES.invoke(npc);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}

		// Tell that player's client "your held slot is now hotbarIndex"
		ClientboundSetHeldSlotPacket heldPkt = new ClientboundSetHeldSlotPacket(hotbarIndex);
		cp.getHandle().connection.send(heldPkt);

		// Broadcast the new main-hand item to all viewers AND sync to spectators
		PlayerInventoryBackup.syncHand(p); // This now handles both!
	}

	/**
	 * Swap two items in the inventory and
	 * immediately broadcast both slot changes.
	 *
	 * @param slotA any slot index (0–8 hotbar, 9–35 main inv, 36–39 armor, 40 offhand)
	 * @param slotB same range
	 */
	public static void swapItems(Player p, int slotA, int slotB) {
		if(!(p instanceof CraftPlayer cp)) {
			return;
		}
		ServerPlayer npc = cp.getHandle();
		Inventory inv = npc.getInventory();

		// Swap internally
		net.minecraft.world.item.ItemStack a = inv.getItem(slotA);
		net.minecraft.world.item.ItemStack b = inv.getItem(slotB);
		inv.setItem(slotA, b);
		inv.setItem(slotB, a);

		// Re-send the entire inventory window to player AND spectators
		PlayerInventoryBackup.syncInventory(p); // This updates spectators

		// If either swapped slot was in the hotbar, update the hand-item too
		if(slotA < 9 || slotB < 9) {
			PlayerInventoryBackup.syncHand(p); // This now handles both viewers and spectators
		}

		if(slotA >= 36 || slotB >= 36) {
			List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> equipmentList = new ArrayList<>();
			equipmentList.add(new Pair<>(EquipmentSlot.HEAD, inv.getItem(39)));
			equipmentList.add(new Pair<>(EquipmentSlot.CHEST, inv.getItem(38)));
			equipmentList.add(new Pair<>(EquipmentSlot.LEGS, inv.getItem(37)));
			equipmentList.add(new Pair<>(EquipmentSlot.FEET, inv.getItem(36)));
			ClientboundSetEquipmentPacket equipmentPkt = new ClientboundSetEquipmentPacket(npc.getId(), equipmentList);
			Utils.broadcastPacket(equipmentPkt);
		}
	}

	/**
	 * Rotates a player’s body & head to the given yaw/pitch
	 * and broadcasts that new orientation to every real client.
	 * <p>
	 * Works on both real and fake Players (CraftPlayer instances).
	 *
	 * @param entity the entity (real or NPC)
	 * @param yaw    body + head yaw, in degrees (0 = south, 90 = west…)
	 * @param pitch  look pitch, in degrees (–90 = straight up, +90 = straight down)
	 */
	public static void turnHead(Entity entity, float yaw, float pitch) {
		if(!(entity instanceof Player player)) {
			// For non-player entities, use regular teleport
			Location to = entity.getLocation();
			to.setYaw(yaw);
			to.setPitch(pitch);
			entity.teleport(to);
			return;
		}

		// For fake players, set rotation directly
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.setYRot(yaw);
		serverPlayer.setYBodyRot(yaw);
		serverPlayer.setYHeadRot(yaw);
		serverPlayer.setXRot(pitch);
		serverPlayer.refreshDimensions();
		Utils.debug(Utils.DebugType.CLIENT, player.getName() + " turning head to " + yaw + " " + pitch);

		for (Player spectator : Spectate.getSpectatingPlayers(player)) {
			if (spectator instanceof CraftPlayer craftSpectator) {
				ServerPlayer nmsSpectator = craftSpectator.getHandle();
				nmsSpectator.connection.send(new ClientboundPlayerRotationPacket(yaw, false, pitch, false));
				Spectate.updateLastSentRotation(spectator, yaw, pitch);
			}
		}
	}

	/**
	 * Turns the given player's head directly toward the center of the nearest non-Player,
	 * non-Boss living entity that has clear line of sight from the player's eye. Intended
	 * for Mage hitscan abilities (mage beam, etc.) where the projectile travels in a
	 * straight line; targets behind walls are skipped in favor of the next-nearest visible
	 * mob.
	 */
	public static void snapHeadToNearestEnemy(Player p) {
		LivingEntity target = findNearestEnemy(p, MAGE_BEAM_RANGE, le -> hasLineOfSight(p, le));
		if(target == null) return;
		snapHeadAtEntity(p, target);
	}

	/**
	 * Turns the given player's head directly toward the center of the given target's
	 * bounding box — no LOS check, no boss/player filtering. Use when the caller has
	 * already chosen the target (e.g. aiming the mage beam at Storm during the boss fight).
	 */
	public static void snapHeadAtEntity(Player p, LivingEntity target) {
		Location eye = p.getEyeLocation();
		Vector center = target.getBoundingBox().getCenter();
		double dx = center.getX() - eye.getX();
		double dy = center.getY() - eye.getY();
		double dz = center.getZ() - eye.getZ();

		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		turnHead(p, yaw, pitch);
	}

	/**
	 * Turns the given player's head so that an arrow shot from a Terminator (spawn pos
	 * {@code eyeLocation + (0, -0.1, 0)}, speed 3.175) would land on the center of the
	 * nearest non-Player, non-Boss living entity reachable by ballistic trajectory.
	 * Compensates for arrow gravity (0.05/t) and air drag (0.99/t) by iteratively solving
	 * the launch angle, and skips any candidate whose trajectory would be blocked by
	 * terrain before reaching its bounding box.
	 */
	public static void aimTerminatorAtNearestEnemy(Player p) {
		int now = MinecraftServer.currentTick;
		terminatorClaimedUntil.entrySet().removeIf(e -> e.getValue() <= now);

		LivingEntity target = findNearestEnemy(p, TERMINATOR_MAX_RANGE, le ->
				!terminatorClaimedUntil.containsKey(le.getUniqueId()) && canTerminatorReach(p, le));
		if(target == null) return;
		aimTerminatorAtEntity(p, target);
	}

	/**
	 * Turns the given player's head so that a Terminator arrow would land on the center
	 * of the given target's bounding box — no boss/player filter, no terrain check.
	 * Use when the caller has already chosen the target (e.g. aiming at Storm during the
	 * boss fight). Returns silently if the target is out of ballistic range. Registers
	 * a flight-time claim so the auto-pick variant won't re-target this mob while the
	 * shot is in the air.
	 */
	public static void aimTerminatorAtEntity(Player p, LivingEntity target) {
		Location spawn = p.getEyeLocation().add(0, -0.1, 0);
		Vector center = target.getBoundingBox().getCenter();
		double dx = center.getX() - spawn.getX();
		double dy = center.getY() - spawn.getY();
		double dz = center.getZ() - spawn.getZ();
		double dh = Math.sqrt(dx * dx + dz * dz);

		final double speed = 3.175;
		Double angle = solveLaunchAngle(speed, dh, dy);
		if(angle == null) return; // out of ballistic range

		int now = MinecraftServer.currentTick;
		int flightTicks = simulateArrowFlightTicks(speed, angle, dh);
		terminatorClaimedUntil.put(target.getUniqueId(), now + flightTicks + 3);

		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(angle); // angle>0 (upward) → MC pitch<0
		turnHead(p, yaw, pitch);
	}

	private static LivingEntity findNearestEnemy(Player p, double range, Predicate<LivingEntity> canHit) {
		Vector origin = p.getEyeLocation().toVector();
		double rangeSq = range * range;
		List<LivingEntity> candidates = new ArrayList<>();
		// getNearbyEntities takes half-extents — passing `range` gives a 2·range cube around the
		// player, which is the smallest AABB that fully contains the desired sphere.
		for(Entity e : p.getNearbyEntities(range, range, range)) {
			if(!(e instanceof LivingEntity le)) continue;
			if(le instanceof Player) continue;
			if(le instanceof Wither) continue;
			if(le.isDead()) continue;
			if(le.getBoundingBox().getCenter().distanceSquared(origin) > rangeSq) continue;
			candidates.add(le);
		}
		if(candidates.isEmpty()) return null; // skip sort/predicate when nothing is in range
		candidates.sort(Comparator.comparingDouble(le -> le.getBoundingBox().getCenter().distanceSquared(origin)));
		for(LivingEntity le : candidates) {
			if(canHit.test(le)) return le;
		}
		return null;
	}

	/**
	 * Mage-beam loop variant of {@link #leftClick}: only sends the swing packet (and thus
	 * the mage-beam dispatch in {@link nms.TASGamePacketListenerImpl#handleAnimate}) if a
	 * mage beam fired from the player's current facing direction would actually intercept
	 * a damageable entity within {@link #MAGE_BEAM_RANGE}. Avoids the thousands of no-op
	 * swing/dispatch packets that would otherwise be sent during Storm-phase beam spam.
	 *
	 * Mirrors the raytrace logic in {@code CustomItems.mageBeam}: ignores Players and dead
	 * or resistance-255 living entities, but DOES include Withers (the boss is a valid
	 * target — the beam plays a stun sound even when the wither is invulnerable). Also
	 * skips firing if a solid block sits closer than the entity along the line of sight.
	 */
	public static void loopLeftClick(Player p) {
		if(!mageBeamWouldHit(p)) return;
		leftClick(p);
	}

	private static boolean mageBeamWouldHit(Player p) {
		Location eye = p.getEyeLocation();
		Vector dir = eye.getDirection();
		RayTraceResult entityHit = p.getWorld().rayTraceEntities(eye, dir, MAGE_BEAM_RANGE, 0, entity -> {
			if(!(entity instanceof LivingEntity le)) return false;
			if(le instanceof Player) return false;
			if(le.isDead()) return false;
			return !(le.hasPotionEffect(PotionEffectType.RESISTANCE)
					&& le.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255);
		});
		if(entityHit == null) return false;
		// If a solid block is closer than the entity along the line of sight, the beam
		// would stop at the block and do no damage — skip the fire.
		RayTraceResult blockHit = p.getWorld().rayTraceBlocks(eye, dir, MAGE_BEAM_RANGE, FluidCollisionMode.NEVER, true);
		if(blockHit == null) return true;
		Vector eyeVec = eye.toVector();
		return entityHit.getHitPosition().distanceSquared(eyeVec) <= blockHit.getHitPosition().distanceSquared(eyeVec);
	}

	/**
	 * Terminator-loop variant of {@link #rightClick}: simulates the middle Terminator arrow's
	 * ballistic trajectory from the player's current facing direction and only sends the
	 * use-item packet (which triggers {@code CustomItems.terminator}) if the arrow would
	 * intercept a valid living entity before hitting terrain or running out of horizontal
	 * range. Mirrors {@link #loopLeftClick}'s purpose: avoid the per-tick arrow-spawn cost
	 * when no target is in the line of fire.
	 *
	 * Uses the same entity filter as {@link #mageBeamWouldHit} (skips Players, dead, and
	 * resistance-255 mobs; includes Withers so Storm/etc. count as valid targets). The
	 * trajectory is walked tick-by-tick with Minecraft arrow physics ({@code v.xz *= 0.99},
	 * {@code v.y = v.y * 0.99 - 0.05}); the walk bails when horizontal distance exceeds
	 * {@link #TERMINATOR_MAX_RANGE} or horizontal velocity collapses.
	 *
	 * Only checks the middle arrow of the 3-arrow spread — left/right are ±5° and rarely
	 * matter when the middle misses.
	 */
	public static void loopRightClick(Player p) {
		if(!terminatorWouldHit(p)) return;
		rightClick(p);
	}

	private static boolean terminatorWouldHit(Player p) {
		Location spawn = p.getEyeLocation().add(0, -0.1, 0);
		Vector dir = p.getEyeLocation().getDirection();

		final double speed = 3.175;
		double vx = dir.getX() * speed;
		double vy = dir.getY() * speed;
		double vz = dir.getZ() * speed;

		Vector pos = spawn.toVector();
		Vector start = pos.clone();
		World world = p.getWorld();
		double maxRangeSq = TERMINATOR_MAX_RANGE * TERMINATOR_MAX_RANGE;

		for(int t = 0; t < 200; t++) {
			Vector vel = new Vector(vx, vy, vz);
			double stepLen = vel.length();
			if(stepLen < 1e-4) return false;
			Vector dirNorm = vel.clone().multiply(1.0 / stepLen);

			RayTraceResult entityHit = world.rayTraceEntities(pos.toLocation(world), dirNorm, stepLen, 0, entity -> {
				if(!(entity instanceof LivingEntity le)) return false;
				if(le instanceof Player) return false;
				if(le.isDead()) return false;
				return !(le.hasPotionEffect(PotionEffectType.RESISTANCE)
						&& le.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255);
			});
			RayTraceResult blockHit = world.rayTraceBlocks(pos.toLocation(world), dirNorm, stepLen, FluidCollisionMode.NEVER, true);

			if(entityHit != null) {
				if(blockHit == null) return true;
				double entityD2 = entityHit.getHitPosition().distanceSquared(pos);
				double blockD2 = blockHit.getHitPosition().distanceSquared(pos);
				return entityD2 <= blockD2;
			}
			if(blockHit != null) return false;

			pos.add(vel);
			vx *= 0.99;
			vz *= 0.99;
			vy = vy * 0.99 - 0.05;

			double dx = pos.getX() - start.getX();
			double dz = pos.getZ() - start.getZ();
			if(dx * dx + dz * dz > maxRangeSq) return false;
		}
		return false;
	}

	private static boolean hasLineOfSight(Player p, LivingEntity target) {
		Location eye = p.getEyeLocation();
		Vector dir = target.getBoundingBox().getCenter().subtract(eye.toVector());
		double dist = dir.length();
		if(dist < 1e-6) return true;
		RayTraceResult hit = p.getWorld().rayTraceBlocks(eye, dir.multiply(1.0 / dist), dist, FluidCollisionMode.NEVER, true);
		return hit == null;
	}

	private static boolean canTerminatorReach(Player p, LivingEntity target) {
		Location spawn = p.getEyeLocation().add(0, -0.1, 0);
		Vector center = target.getBoundingBox().getCenter();
		double dx = center.getX() - spawn.getX();
		double dy = center.getY() - spawn.getY();
		double dz = center.getZ() - spawn.getZ();
		double dh = Math.sqrt(dx * dx + dz * dz);
		if(dh < 1e-6) return hasLineOfSight(p, target);

		final double speed = 3.175;
		Double angle = solveLaunchAngle(speed, dh, dy);
		if(angle == null) return false;

		// Walk the trajectory tick-by-tick in world coords; abort if a block intervenes
		// before the arrow's path crosses the target's bounding box.
		double yawRad = Math.atan2(-dx, dz);
		double vh = speed * Math.cos(angle);
		double vy = speed * Math.sin(angle);
		double vx = -Math.sin(yawRad) * vh;
		double vz = Math.cos(yawRad) * vh;

		Vector pos = spawn.toVector();
		BoundingBox box = target.getBoundingBox();
		World world = p.getWorld();

		for(int t = 0; t < 200; t++) {
			Vector vel = new Vector(vx, vy, vz);
			double stepLen = vel.length();
			if(stepLen < 1e-6) return false;
			Vector dirNorm = vel.clone().multiply(1.0 / stepLen);

			RayTraceResult boxHit = box.rayTrace(pos, dirNorm, stepLen);
			RayTraceResult blockHit = world.rayTraceBlocks(pos.toLocation(world), dirNorm, stepLen, FluidCollisionMode.NEVER, true);

			if(boxHit != null) {
				if(blockHit == null) return true;
				double boxD2 = boxHit.getHitPosition().distanceSquared(pos);
				double blockD2 = blockHit.getHitPosition().distanceSquared(pos);
				return boxD2 <= blockD2;
			}
			if(blockHit != null) return false;

			pos.add(vel);
			vx *= 0.99;
			vz *= 0.99;
			vy = vy * 0.99 - 0.05;

			// Safety: arrow has clearly fallen past the target
			if(pos.getY() < box.getMinY() - 20) return false;
		}
		return false;
	}

	/** Iteratively solves the upward launch angle (radians) that hits offset (dh, dy) given
	 *  the projectile's 3D speed. Returns null if no feasible angle was found. */
	private static Double solveLaunchAngle(double speed, double dh, double dy) {
		double aimY = dy;
		double angle = Math.atan2(aimY, dh);
		for(int i = 0; i < 12; i++) {
			Double yHit = simulateArrowYAtDistance(speed, angle, dh);
			if(yHit == null) return null;
			double err = dy - yHit;
			if(Math.abs(err) < 0.01) return angle;
			aimY += err;
			angle = Math.atan2(aimY, dh);
		}
		return angle;
	}

	/** Returns the number of ticks an arrow launched at {@code angleRad} (radians above horizontal)
	 *  with the given 3D speed takes to travel {@code targetH} horizontal distance under Minecraft
	 *  arrow physics. Returns 60 as a conservative fallback if the arrow can't reach. */
	private static int simulateArrowFlightTicks(double speed, double angleRad, double targetH) {
		double vh = speed * Math.cos(angleRad);
		double vy = speed * Math.sin(angleRad);
		double h = 0;
		for(int t = 1; t <= 400; t++) {
			h += vh;
			vh *= 0.99;
			vy = vy * 0.99 - 0.05;
			if(h >= targetH) return t;
			if(vh < 1e-4) return 60;
		}
		return 60;
	}

	/** Simulates a Minecraft arrow launched at {@code angleRad} above horizontal with the given
	 *  3D speed; returns its Y offset (relative to spawn) when it crosses {@code targetH}
	 *  horizontal distance, or null if it never reaches that distance. */
	private static Double simulateArrowYAtDistance(double speed, double angleRad, double targetH) {
		double vh = speed * Math.cos(angleRad);
		double vy = speed * Math.sin(angleRad);
		double h = 0, y = 0;
		double prevH, prevY;
		for(int t = 0; t < 400; t++) {
			prevH = h;
			prevY = y;
			h += vh;
			y += vy;
			vh *= 0.99;
			vy = vy * 0.99 - 0.05;
			if(h >= targetH) {
				double frac = (targetH - prevH) / (h - prevH);
				return prevY + frac * (y - prevY);
			}
			if(vh < 1e-4) return null;
		}
		return null;
	}

	/**
	 * Simulates a fake player performing a left click.
	 *
	 * @param p The fake player performing the left click
	 */
	public static void leftClick(Player p) {
		if(!(p instanceof CraftPlayer cp)) return;

		ServerPlayer serverPlayer = cp.getHandle();

		ServerboundSwingPacket swingPacket = new ServerboundSwingPacket(InteractionHand.MAIN_HAND);
		Utils.simulatePacket(p, swingPacket);

		// Vanilla's crosshair pick raytraces blocks first and caps the entity ray at the block
		// hit, so entities occluded by a block can't be targeted.
		RayTraceResult blockRay = p.rayTraceBlocks(p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue());
		double entityRange = p.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).getValue();
		if(blockRay != null) {
			entityRange = Math.min(entityRange, p.getEyeLocation().toVector().distance(blockRay.getHitPosition()));
		}

		RayTraceResult entityRay = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), entityRange, entity -> {
			if(entity == p) return false;
			if(entity.isDead()) return false;
			// Only LivingEntity is attackable — TASGamePacketListenerImpl.handleInteract rejects
			// ItemEntity / ExperienceOrb / non-attackable arrows and disconnects the fake player
			// ("Attempting to attack an invalid entity"). Storm death dropping XP orbs near the
			// player was triggering this.
			if(!(entity instanceof LivingEntity le)) return false;
			if(le.hasPotionEffect(PotionEffectType.RESISTANCE) && le.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255) return false;
			if(le instanceof Wither w && w.getInvulnerabilityTicks() != 0) return false;
			if(le instanceof org.bukkit.entity.Player player) {
				if(FakePlayerManager.getFakePlayers().containsValue(player)) return false;
				return !Spectate.getSpectatingPlayers(p).contains(player);
			}
			return true;
		});

		if(entityRay != null && entityRay.getHitEntity() != null) {
			net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entityRay.getHitEntity()).getHandle();
			ServerboundInteractPacket attackPacket = ServerboundInteractPacket.createAttackPacket(nmsEntity, serverPlayer.isShiftKeyDown());
			Utils.simulatePacket(p, attackPacket);
			return;
		}

		if(blockRay != null && blockRay.getHitBlock() != null) {
			if(p.getInventory().getItemInMainHand().getType() != Material.DIAMOND_PICKAXE) {
				// No entity hit, block hit but not a pickaxe — dispatch left-click ability
				CustomItems.handleCustomItems(null, org.bukkit.inventory.EquipmentSlot.HAND, p.getInventory().getItemInMainHand(), Action.LEFT_CLICK_BLOCK, p);
				return;
			}

			Block block = blockRay.getHitBlock();
			BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
			Direction direction = CraftBlock.blockFaceToNotch(blockRay.getHitBlockFace());

			ServerboundPlayerActionPacket breakPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, 0);
			Utils.simulatePacket(p, breakPacket);
			ServerboundPlayerActionPacket breakStopPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction, 0);
			Utils.simulatePacket(p, breakStopPacket);
		} else {
			// No entity hit, no block hit — dispatch left-click ability
			CustomItems.handleCustomItems(null, org.bukkit.inventory.EquipmentSlot.HAND, p.getInventory().getItemInMainHand(), Action.LEFT_CLICK_AIR, p);
		}
	}

	/**
	 * Swings the fake player's hand
	 * DO NOT USE unless you are obtaining a secret while holding Dungeonbreaker or some other action that should swing the main hand without triggering an interaction
	 *
	 * @param p The fake player performing the swing
	 */
	public static void swingHand(Player p) {
		if(!(p instanceof CraftPlayer cp)) return;

		ServerPlayer serverPlayer = cp.getHandle();

		ServerboundSwingPacket swingPacket = new ServerboundSwingPacket(InteractionHand.MAIN_HAND);
		Utils.simulatePacket(p, swingPacket);
	}

	/**
	 * Simulates a fake player performing a right click.
	 *
	 * @param p The fake player performing the right click
	 */
	public static void rightClick(Player p) {
		if(!(p instanceof CraftPlayer cp)) return;

		ServerPlayer serverPlayer = cp.getHandle();

		// Last Breath / Explosive Bow: BowItem.use() bails when the player has no arrows and isn't
		// creative. Bypass the packet round-trip for these bows specifically and start the using-item
		// state directly so the draw animation syncs (via entity-data LIVING_ENTITY_FLAGS bit 0)
		// without needing real projectiles in the fake player's inventory. Other bows (e.g.
		// Terminator) go through the normal packet path so their ability dispatch still fires.
		String heldId = CustomItems.getID(p.getInventory().getItemInMainHand());
		if("skyblock/combat/last_breath".equals(heldId) || "skyblock/combat/explosive_bow".equals(heldId)) {
			// Only click path that sends no packet at all, so simulatePacket never logs it — log the click here
			Utils.debug(Utils.DebugType.CLIENT, p.getName() + " Right Clicked" + (Utils.isSuperVerbose() ? (" at " + Utils.round(p.getLocation().getX(), 3) + " " + Utils.round(p.getLocation().getY(), 5) + " " + Utils.round(p.getLocation().getZ(), 3) + " " + p.getLocation().getYaw() + " " + p.getLocation().getPitch()) : ""));
			serverPlayer.startUsingItem(InteractionHand.MAIN_HAND);
			return;
		}

		// Vanilla's crosshair pick raytraces blocks first and caps the entity ray at the block
		// hit, so entities occluded by a block can't be targeted.
		RayTraceResult blockRay = p.rayTraceBlocks(p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue());
		double entityRange = p.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).getValue();
		if(blockRay != null) {
			entityRange = Math.min(entityRange, p.getEyeLocation().toVector().distance(blockRay.getHitPosition()));
		}

		RayTraceResult entityRay = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), entityRange, entity -> entity != p && !(entity instanceof Player target && (FakePlayerManager.getFakePlayers().containsValue(target) || Spectate.getSpectatingPlayers(p).contains(target))));

		if(entityRay != null && entityRay.getHitEntity() != null) {
			net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entityRay.getHitEntity()).getHandle();

			Vec3 entityPos = nmsEntity.position();
			Vec3 hitPos = new Vec3(entityRay.getHitPosition().getX(), entityRay.getHitPosition().getY(), entityRay.getHitPosition().getZ());
			Vec3 localHit = hitPos.subtract(entityPos);

			ServerboundInteractPacket interactAtPacket = ServerboundInteractPacket.createInteractionPacket(nmsEntity, serverPlayer.isShiftKeyDown(), InteractionHand.MAIN_HAND, localHit);
			Utils.simulatePacket(p, interactAtPacket);
			ServerboundInteractPacket interactPacket = ServerboundInteractPacket.createInteractionPacket(nmsEntity, serverPlayer.isShiftKeyDown(), InteractionHand.MAIN_HAND);
			Utils.simulatePacket(p, interactPacket);
			for(Player spectator : Spectate.getSpectatingPlayers(p)) spectator.swingMainHand();

			// Vanilla clients stop here when the entity interaction consumes the action and never
			// send a UseItem packet, so the held item's right-click ability must not fire. Mirror the
			// real-client exemption list at CustomItems#onPlayerInteractAtEntity (ItemFrame/Interaction)
			// — without this, right-clicking a Goldor terminal would still trigger the ability.
			if(entityRay.getHitEntity() instanceof ItemFrame || entityRay.getHitEntity() instanceof Interaction) {
				return;
			}
		}
		// Check for block target
		else {
			if(blockRay != null && blockRay.getHitBlock() != null) {
				Block block = blockRay.getHitBlock();
				BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
				Direction direction = CraftBlock.blockFaceToNotch(blockRay.getHitBlockFace());

				Vec3 hitVec = new Vec3(blockRay.getHitPosition().getX(), blockRay.getHitPosition().getY(), blockRay.getHitPosition().getZ());

				BlockHitResult blockHit = new BlockHitResult(hitVec, direction, pos, false);

				ServerboundUseItemOnPacket useOnBlockPacket = new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, blockHit, 0);
				Utils.simulatePacket(p, useOnBlockPacket);
				if(block.getType().isInteractable()) {
					for(Player spectator : Spectate.getSpectatingPlayers(p)) spectator.swingMainHand();
				}
				return;
			}
		}

		// Right click air (eat food, throw pearl, use item ability)
		ServerboundUseItemPacket useItemPacket = new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, serverPlayer.getYRot(), serverPlayer.getXRot());
		Utils.simulatePacket(p, useItemPacket);
	}

	public static void stopRightClick(Player p) {
		ServerPlayer sp = ((CraftPlayer) p).getHandle();
		boolean was = sp.getAbilities().instabuild;
		sp.getAbilities().instabuild = true;
		ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, sp.blockPosition(), Direction.DOWN, 0);
		Utils.simulatePacket(p, packet);
		sp.getAbilities().instabuild = was;
	}

	public static void dropItem(Player p, boolean dropAll) {
		ServerboundPlayerActionPacket.Action action = dropAll
				? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
				: ServerboundPlayerActionPacket.Action.DROP_ITEM;
		ServerPlayer sp = ((CraftPlayer) p).getHandle();
		ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(action, sp.blockPosition(), Direction.DOWN, 0);
		Utils.simulatePacket(p, packet);
	}

	/**
	 * Simulates the impact of a wither ability on the player and surrounding entities.
	 * This involves teleportation, damage dealing to nearby entities, sound effects,
	 * particle effects, and applying absorption shield mechanics.
	 *
	 * @param p  The player who activates the wither impact. This player will be teleported,
	 *           have sounds and effects applied, and serve as the source of damage to nearby entities.
	 * @param to The location the player will be teleported to. Use null if this ability is being used in the boss fight.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void witherImpact(Player p, @Nullable Location to) {
		p.setFallDistance(0);

		Location effectLocation = to != null ? to : p.getLocation();
		if(to != null) {
			Utils.teleport(p, to);
		}

		p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
		p.getWorld().spawnParticle(Particle.EXPLOSION, effectLocation, 1);
		List<Entity> entities = p.getNearbyEntities(10, 10, 10);
		List<EntityType> doNotKill = doNotKill();
		int damaged = 0;

		for(Entity entity : entities) {
			if(!doNotKill.contains(entity.getType()) && !entity.equals(p) && entity instanceof LivingEntity entity1 && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerabilityTicks() != 0)) {
				EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(p, entity, EntityDamageEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), 1);

				Bukkit.getPluginManager().callEvent(damageEvent);
				damaged += 1;
			}
		}
		if(damaged > 0) {
			p.sendMessage(ChatColor.RED + "Your Implosion hit " + damaged + " enemies for " + damaged + " damage");
		}
		p.playSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

		// wither shield
		int absorptionLevel = -1;
		if(p.hasPotionEffect(PotionEffectType.ABSORPTION)) {
			PotionEffect effect = p.getPotionEffect(PotionEffectType.ABSORPTION);
			if(effect != null) {
				absorptionLevel = effect.getAmplifier();
			}
		}

		// Apply absorption shield if not already at level 2
		if(absorptionLevel != 2) {
			p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 101, 2));
			p.playSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 2.0F, 0.65F);

			// Schedule healing conversion after 5 seconds
			Utils.scheduleTask(() -> {
				double healAmount = p.getAbsorptionAmount() / 2;
				double currentHealth = p.getHealth();
				double maxHealth = Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH)).getValue();

				p.setHealth(Math.min(currentHealth + healAmount, maxHealth));
				p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2.0F, 2.0F);
			}, 101L);
		}

		// Apply damage reduction tag
		if(!p.getScoreboardTags().contains("WitherShield")) {
			p.addScoreboardTag("WitherShield");
			Utils.scheduleTask(() -> p.removeScoreboardTag("WitherShield"), 101);
		}
	}

	/**
	 * Simulates the teleportation of a Player to a specified location while preserving
	 * their orientation (yaw and pitch). This method resets the player's velocity,
	 * adjusts their yaw and pitch to match the origin location, and teleports any spectator
	 * viewers who are spectating the player to the same destination. A teleportation sound
	 * effect is played at the player's location.
	 *
	 * @param p  The player to be teleported and simulated.
	 * @param to The target location to which the player will be teleported.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void etherwarp(Player p, Location to) {
		Location from = p.getLocation();
		to.setYaw(from.getYaw());
		to.setPitch(from.getPitch());

		p.setVelocity(new Vector(0, 0, 0));
		Utils.teleport(p, to);

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.50F);
	}

	/**
	 * Simulates the teleportation of a Player to a specified location while preserving
	 * their orientation (yaw and pitch). Additionally, resets the player's velocity
	 * and teleports any spectator viewers following the player to the same location.
	 * A teleportation sound effect is played at the player's location.
	 * <p>
	 * This function is also used when an Ender Pearl lands.
	 *
	 * @param p  The player to be teleported and simulated.
	 * @param to The target location to which the player will be teleported.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void AOTV(Player p, Location to) {
		Location from = p.getLocation();
		to.setYaw(from.getYaw());
		to.setPitch(from.getPitch());

		p.setVelocity(new Vector(0, 0, 0));
		Utils.teleport(p, to);

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
	}

	/**
	 * Simulates the "stonking" action on a given block. This involves temporarily
	 * changing the block to an AIR block and then resetting it to its original
	 * material type after a short delay.
	 *
	 * @param p The player that is breaking the block
	 * @param b The Block to simulate the stonking action on.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void stonk(Player p, Block b) {
		if(p != null) {
			swingHand(p);
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0F, 1.0F);
		}
		Material material = b.getType();
		BlockData blockdata = b.getBlockData().clone();
		Location loc = b.getLocation();
		b.setType(Material.AIR);
		CustomItems.pendingStonkRestorations.put(loc, blockdata);
		BukkitTask task = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			b.setType(material);
			b.setBlockData(blockdata);
			CustomItems.pendingStonkRestorations.remove(loc);
			CustomItems.pendingStonkTasks.remove(loc);
		}, 6);
		CustomItems.pendingStonkTasks.put(loc, task);
	}

	/**
	 * Simulates the action of a player using a ghost block pick, which involves
	 * the player swinging their main hand, playing a sound effect, and replacing
	 * the targeted block with air.
	 * WARNING: MUST REPLACE THESE BLOCKS IN server.java
	 *
	 * @param p The player performing the ghost pick action. This player will swing their main hand
	 *          and cause a sound effect to play in their current world at their location.
	 * @param b The block that will be replaced with air as part of the ghost pick action.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Removed functionality.")
	public static void ghostPick(Player p, Block b) {
		if(p != null) {
			swingHand(p);
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0F, 1.0F);
		}
		b.setType(Material.AIR);
	}

	/**
	 * Simulates a "Superboom" explosion action within a specified cuboid area.
	 * The method triggers a left-click air interaction, clears the specified area by
	 * filling it with air, plays an explosion sound for the player, and then fills
	 * the area with cracked stone bricks after a short delay.
	 *
	 * @param p  The player for whom the superboom simulation is performed.
	 * @param x1 The x-coordinate of the first corner of the cuboid area.
	 * @param y1 The y-coordinate of the first corner of the cuboid area.
	 * @param z1 The z-coordinate of the first corner of the cuboid area.
	 * @param x2 The x-coordinate of the opposite corner of the cuboid area.
	 * @param y2 The y-coordinate of the opposite corner of the cuboid area.
	 * @param z2 The z-coordinate of the opposite corner of the cuboid area.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void superboom(Player p, int x1, int y1, int z1, int x2, int y2, int z2) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:air replace minecraft:cracked_stone_bricks");
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:cracked_stone_bricks replace minecraft:air"), 21);
	}

	/**
	 * Simulates a "Blow Up Crypt" action within a specified cuboid area. The method performs
	 * a sequence of operations: simulating a left-click air interaction, clearing
	 * the specified area by filling it with air, playing a sound effect, and then
	 * cloning a specified structure after a short delay.
	 *
	 * @param p  The player for whom the crypt simulation is performed.
	 * @param x1 The x-coordinate of the first corner of the cuboid area.
	 * @param y1 The y-coordinate of the first corner of the cuboid area.
	 * @param z1 The z-coordinate of the first corner of the cuboid area.
	 * @param x2 The x-coordinate of the opposite corner of the cuboid area.
	 * @param y2 The y-coordinate of the opposite corner of the cuboid area.
	 * @param z2 The z-coordinate of the opposite corner of the cuboid area.
	 */
	public static void crypt(Player p, int x1, int y1, int z1, int x2, int y2, int z2) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:air");
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
		Zombie zombie = (Zombie) p.getWorld().spawnEntity(new Location(p.getWorld(), (double) (x1 + x2) / 2, Math.min(y1, y2), (double) (z1 + z2) / 2), EntityType.ZOMBIE);
		zombie.setCustomName("Crypt Undead " + ChatColor.RESET + ChatColor.YELLOW + "2M" + ChatColor.RED + "❤");
		zombie.setCustomNameVisible(true);
		zombie.setAI(false);
		zombie.setSilent(true);
		zombie.setAdult();
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
		Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(1);
		zombie.setHealth(1);

		assert zombie.getEquipment() != null;
		zombie.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.BONE));
		Utils.scheduleTask(() -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone " + x1 + " " + 0 + " " + z1 + " " + x2 + " " + Math.abs(y2 - y1) + " " + z2 + " " + Math.min(x1, x2) + " " + Math.min(y1, y2) + " " + Math.min(z1, z2));
			try {
				zombie.remove();
			} catch(Exception exception) {
				// nothing here
			}
		}, 21);
	}

	public static void leap(Player p, Player target) {
		// A leap teleports the player, so any in-progress bonzo staff motion is void. Cancel the airborne
		// audit (otherwise it reports the teleport as a giant delta and a bogus "landed" line) and remove
		// any still-in-flight Bonzo wind charges this player fired so a late hit can't push them off course.
		MovementAudit.cancelAirborneAudit(p.getUniqueId());
		for(WindCharge windCharge : p.getWorld().getEntitiesByClass(WindCharge.class)) {
			if(windCharge.getScoreboardTags().contains("Bonzo") && windCharge.getShooter() == p) {
				CustomItems.bonzoFireTick.remove(windCharge.getEntityId());
				windCharge.remove();
			}
		}

		Location targetLoc = target.getLocation();
		teleport(p, targetLoc);

		if(p instanceof CraftPlayer cp) {
			ServerPlayer npc = cp.getHandle();
			npc.setDeltaMovement(Vec3.ZERO);
			// A leap usually fires mid-air (out of a bonzo launch/jump) or lands on a mid-jump target, so the
			// inherited onGround is unreliable: when false, the next move() runs travel() with air physics
			// (~0.02/tick, ignoring the speed attribute) instead of the intended ground sprint. Force grounded
			// so the following move() always accelerates off the ground — gravity still applies, so the player
			// keeps falling if the landing spot is above the floor; onGround only governs horizontal friction.
			npc.setOnGround(true);
			npc.hurtMarked = true;
			// teleport() relies on the vanilla entity tracker to inform observers, which lags a tick — long
			// enough for their clients to keep extrapolating the pre-leap momentum and glide the entity
			// forward before snapping. Broadcast the landed position with zero velocity so observer clients
			// snap this tick and stop extrapolating.
			PositionMoveRotation pmr = PositionMoveRotation.of(npc);
			Utils.broadcastPacket(ClientboundTeleportEntityPacket.teleport(npc.getId(), pmr, EnumSet.noneOf(net.minecraft.world.entity.Relative.class), npc.onGround()));
			Utils.broadcastPacket(new ClientboundSetEntityMotionPacket(npc.getId(), Vec3.ZERO));
		}

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
		Utils.debug(Utils.DebugType.CLIENT, p.getName() + " leaped to " + target.getName() + " at " + Utils.round(targetLoc.getX(), 3) + " " + Utils.round(targetLoc.getY(), 5) + " " + Utils.round(targetLoc.getZ(), 3));
	}

	public static void mimicChest(Player p, Block b) {
		BlockData originalData = b.getBlockData().clone();
		org.bukkit.block.BlockState originalState = b.getState();
		b.setType(Material.AIR);

		Zombie zombie = (Zombie) p.getWorld().spawnEntity(b.getLocation().add(0.5, 0, 0.5), EntityType.ZOMBIE);
		zombie.setCustomName("Mimic " + ChatColor.RESET + ChatColor.YELLOW + "4M" + ChatColor.RED + "❤");
		zombie.setCustomNameVisible(true);
		zombie.setAI(false);
		zombie.setSilent(true);
		zombie.setBaby();
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
		Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2);
		zombie.setHealth(2);

		Utils.scheduleTask(() -> {
			originalState.update(true, false);
			try {
				zombie.remove();
			} catch(Exception exception) {
				// nothing here
			}
		}, 21);
	}

	/**
	 * Simulates the action of a player throwing a pearl in the direction they are facing.
	 * This method is typically used for replicating the behavior of an ender pearl throw,
	 * including potentially initiating movement or teleportation.
	 *
	 * @param p The player for whom the pearl throw is being simulated.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void throwPearl(Player p) {
		EnderPearl pearl = (EnderPearl) p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.ENDER_PEARL);
		pearl.setGravity(true);
		pearl.setShooter(null);
		pearl.setVelocity(p.getLocation().getDirection().normalize().multiply(3)); // Normal pearl speed is ~3.0

		Actions.swingHand(p);
		p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount() - 1);
	}

	/**
	 * Simulates the actions performed when using the "Rag Axe" ability, which involves
	 * playing a series of lever click sounds, applying a temporary "RagBuff" tag to
	 * the player, and then removing the tag after a specific duration.
	 * <br>
	 * Does NOT have anything to do with Retrieval Augmented Generation.
	 *
	 * @param p The player for whom the "Rag Axe" ability simulation will be performed.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void rag(Player p) {
		p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F), 20);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F), 40);
		Utils.scheduleTask(() -> {
			p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WOLF_WHINE, 1.0F, 1.5F);
			p.addScoreboardTag("RagBuff");
			if(p.getName().equals("Archer")) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
			} else if(p.getName().equals("Berserk")) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0));
			}
			// mage: contolled by method | healer/tank: never uses rag
		}, 60);
		Utils.scheduleTask(() -> p.removeScoreboardTag("RagBuff"), 260);
	}

	/**
	 *
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new leftClick() while holding the correct item")
	public static void salvation(Player p) {
		double powerBonus;
		try {
			int power = Objects.requireNonNull(p.getInventory().getItem(p.getInventory().getHeldItemSlot())).getEnchantmentLevel(Enchantment.POWER);
			powerBonus = power * 0.25;
			if(power == 7) {
				powerBonus += 0.25;
			}
		} catch(Exception exception) {
			powerBonus = 0;
		}

		double strengthBonus;
		try {
			strengthBonus = 0.75 + Objects.requireNonNull(p.getPotionEffect(PotionEffectType.STRENGTH)).getAmplifier();
		} catch(Exception exception) {
			strengthBonus = 0;
		}

		// shoot the three arrows
		double add = powerBonus + strengthBonus;
		Location l = p.getLocation();
		l.add(0, 1.62, 0);

		Vector v = l.getDirection();
		v.setX(v.getX() / 5);
		v.setY(v.getY() / 5);
		v.setZ(v.getZ() / 5);
		World world = l.getWorld();
		Set<Entity> damagedEntities = new HashSet<>();
		List<EntityType> doNotKill = doNotKill();
		damagedEntities.add(p);
		int pierce = 5;
		for(int i = 0; i < 320 && pierce > 0; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			assert world != null;
			ArrayList<Entity> entities = (ArrayList<Entity>) world.getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				if(!damagedEntities.contains(entity) && !doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerabilityTicks() != 0)) {
					damagedEntities.add(entity);
					Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(p, entity1, EntityDamageByEntityEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), 2.5 + add));
					pierce--;
				}
			}
			Particle.DustOptions particle = new Particle.DustOptions(Color.RED, 1.0F);
			world.spawnParticle(Particle.DUST, l, 1, particle);
			l.add(v);
		}
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GUARDIAN_DEATH, 0.5f, 2.0F);
	}

	public static void springBoots(Player p) {
		p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.7087F);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.7087F), 2);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.8428F), 4);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.8428F), 6);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.8428F), 8);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.8428F), 10);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.8428F), 12);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.8428F), 14);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.8929F), 16);
		Utils.scheduleTask(() -> {
			if(!(p instanceof CraftPlayer cp)) {
				return;
			}

			ServerPlayer npc = cp.getHandle();

			if(npc.onGround()) { // onGround check
				Vec3 motion = npc.getDeltaMovement();
				npc.setDeltaMovement(new Vec3(motion.x(), 2.045D, motion.z()));
			}
		}, 17);
	}

	/**
	 * Shoots a Diamond Axe projectile in the direction the player is looking from that lasts 64 blocks.
	 *
	 * @param p Where the axe originates from
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void AOTS(Player p) {
		p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);

		// Create the axe item display
		Location startLoc = p.getEyeLocation();
		Vector direction = startLoc.getDirection().normalize();

		// Calculate the horizontal perpendicular axis (cross product with up vector)
		Vector up = new Vector(0, 1, 0);
		Vector spinAxis = direction.clone().crossProduct(up).normalize();

		// Spawn an ItemDisplay entity
		ItemDisplay axe = p.getWorld().spawn(startLoc, ItemDisplay.class);
		axe.setItemStack(new ItemStack(Material.DIAMOND_AXE));
		axe.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);

		new BukkitRunnable() {
			int distance = 0;
			Location currentLoc = startLoc.clone();
			float spinRotation = 0;

			@Override
			public void run() {
				if(distance >= 100 || !axe.isValid()) {
					axe.remove();
					cancel();
					return;
				}

				// Check if we hit a wall (solid block)
				Location nextLoc = currentLoc.clone().add(direction);
				if(nextLoc.getBlock().getType().isSolid()) {
					axe.remove();
					cancel();
					return;
				}

				// Move 1 block per tick
				currentLoc = nextLoc;

				// Update spin rotation
				spinRotation += 18; // 360 / 20 = 18 degrees per tick

				// Create rotation using axis-angle rotation around the spin axis
				Quaternionf rotation = new Quaternionf().rotateAxis((float) Math.toRadians(spinRotation), (float) spinAxis.getX(), (float) spinAxis.getY(), (float) spinAxis.getZ());

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

	/**
	 * Draws a bow back for this many ticks, then releasing two arrows that do 1 damage and 0.2 damage, respectively.
	 * The arrow velocity should mimic the speed of an arrow that was fired at that many ticks.
	 * The second arrow should be fired 3 ticks after the first and travel at the same velocity as the first.
	 *
	 * @param p     The player firing the arrow.
	 * @param ticks How long the player should draw the bow back for.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void lastBreath(Player p, int ticks) {
		// Calculate arrow velocity based on draw time
		float charge = Math.min((float) ticks / 20.0F, 1.0F); // 20 ticks = full charge
		float velocity = charge * 3.0F;

		// Start bow drawing animation for the fake player
		if(p instanceof CraftPlayer craftPlayer) {
			ServerPlayer serverPlayer = craftPlayer.getHandle();
			serverPlayer.startUsingItem(InteractionHand.MAIN_HAND);

			// Send the animation packet to all players
			ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(serverPlayer.getId(), serverPlayer.getEntityData().getNonDefaultValues());

			for(Player viewer : p.getWorld().getPlayers()) {
				((CraftPlayer) viewer).getHandle().connection.send(dataPacket);
			}
		}

		// Also start bow drawing for any real players spectating this fake player
		Set<Player> spectators = Spectate.getSpectatingPlayers(p);
		for(Player spectator : spectators) {
			if(spectator instanceof CraftPlayer craftSpectator) {
				ServerPlayer serverSpectator = craftSpectator.getHandle();

				// Make the spectator start using bow
				serverSpectator.startUsingItem(InteractionHand.MAIN_HAND);

				// Send their animation to all viewers (including themselves in F5)
				ClientboundSetEntityDataPacket spectatorDataPacket = new ClientboundSetEntityDataPacket(serverSpectator.getId(), serverSpectator.getEntityData().getNonDefaultValues());

				// Send to all players who can see the spectator
				for(Player viewer : spectator.getWorld().getPlayers()) {
					((CraftPlayer) viewer).getHandle().connection.send(spectatorDataPacket);
				}
			}
		}

		Location l = p.getLocation();

		new BukkitRunnable() {
			int currentTick = 0;

			@Override
			public void run() {
				currentTick++;

				if(currentTick == ticks) {
					// Stop drawing animation for fake player
					if(p instanceof CraftPlayer craftPlayer) {
						ServerPlayer serverPlayer = craftPlayer.getHandle();
						serverPlayer.stopUsingItem();

						// Send updated entity data
						ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(serverPlayer.getId(), serverPlayer.getEntityData().getNonDefaultValues());

						for(Player viewer : p.getWorld().getPlayers()) {
							((CraftPlayer) viewer).getHandle().connection.send(dataPacket);
						}
					}

					// Stop drawing animation for spectators
					for(Player spectator : spectators) {
						if(spectator instanceof CraftPlayer craftSpectator) {
							ServerPlayer serverSpectator = craftSpectator.getHandle();
							serverSpectator.stopUsingItem();

							// Send updated entity data
							ClientboundSetEntityDataPacket spectatorDataPacket = new ClientboundSetEntityDataPacket(serverSpectator.getId(), serverSpectator.getEntityData().getNonDefaultValues());

							for(Player viewer : spectator.getWorld().getPlayers()) {
								((CraftPlayer) viewer).getHandle().connection.send(spectatorDataPacket);
							}
						}
					}

					// Fire first arrow
					fireLastBreathArrow(p, l, velocity, 1.0);
					p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);

					// Schedule second arrow for 3 ticks later
					Utils.scheduleTask(() -> {
						fireLastBreathArrow(p, l, velocity, 0.2);
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
					}, 3);

					if(p.getName().contains("Archer")) {
						Utils.scheduleTask(() -> {
							fireLastBreathArrow(p, l, velocity, 1.0);
							p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
						}, 5);

						Utils.scheduleTask(() -> {
							fireLastBreathArrow(p, l, velocity, 1.0);
							p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
						}, 10);
					}

					// Cancel the runnable after firing first arrow
					cancel();
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	/**
	 * Spawns a deterministic Last-Breath arrow. Bypasses {@code Player.launchProjectile},
	 * whose CraftBukkit implementation internally calls {@code Arrow.shootFromRotation} with
	 * inaccuracy=1.0F (consuming RNG and stamping a spread-direction rotation on the arrow
	 * that we'd then have to overwrite). Goes directly through NMS with {@code shoot(..., 0)}
	 * for a perfectly clean trajectory — mirrors the pattern used by {@code CustomItems.terminator}.
	 *
	 * Spawns at the player's current eye location (with the vanilla -0.1 Y offset that
	 * {@code launchProjectile} applies); uses {@code aimFrom.getDirection()} for the flight
	 * direction, so the caller can capture the aim at draw-start and have it survive any
	 * head movement between draw and release.
	 */
	private static void fireLastBreathArrow(Player p, Location aimFrom, float speed, double damage) {
		ServerLevel nmsWorld = ((CraftWorld) p.getWorld()).getHandle();
		ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();
		Vector dir = aimFrom.getDirection();
		Location spawn = p.getEyeLocation().add(0, -0.1, 0);

		net.minecraft.world.entity.projectile.arrow.Arrow nmsArrow = new net.minecraft.world.entity.projectile.arrow.Arrow(
				net.minecraft.world.entity.EntityType.ARROW, nmsWorld);
		nmsArrow.setPos(spawn.getX(), spawn.getY(), spawn.getZ());
		nmsArrow.shoot(dir.getX(), dir.getY(), dir.getZ(), speed, 0);
		nmsArrow.setOwner(nmsPlayer);
		nmsWorld.addFreshEntity(nmsArrow);

		Arrow arrow = (Arrow) nmsArrow.getBukkitEntity();
		arrow.setDamage(damage);
		arrow.setShooter(p);
		arrow.addScoreboardTag("TerminatorArrow");
		arrow.setWeapon(p.getInventory().getItemInMainHand());
	}

	/**
	 * Simulates the Ice Spray ability, dealing damage but not applying increased damage.
	 *
	 * @param p The player using the ability
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void iceSpray(Player p) {
		Location l = p.getEyeLocation();
		p.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 512);
		List<Entity> entities = (List<Entity>) p.getWorld().getNearbyEntities(l, 8, 8, 8);
		List<EntityType> doNotKill = doNotKill();
		for(Entity entity : entities) {
			if(!doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerabilityTicks() != 0)) {
				Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(p, entity1, EntityDamageByEntityEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), 1));
			}
		}
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
	}

	/**
	 * Spawns a trail of particles in the direction the player is looking.
	 * The particle trail's path is affected by gravity.
	 *
	 * @param p The player originating.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new leftClick() while holding the correct item")
	public static void flamingFlay(Player p) {
		Location startLoc = p.getEyeLocation();

		// Angle up by 5 degrees from player's look direction
		Vector direction = startLoc.getDirection().normalize();
		float pitch = Math.max(startLoc.getPitch() - 15, -90); // Subtract 5 degrees (negative pitch is up)
		float yaw = startLoc.getYaw();

		// Recalculate direction with adjusted pitch
		double xz = Math.cos(Math.toRadians(pitch));
		direction.setX(-xz * Math.sin(Math.toRadians(yaw)));
		direction.setY(-Math.sin(Math.toRadians(pitch)));
		direction.setZ(xz * Math.cos(Math.toRadians(yaw)));

		Vector velocity = direction.normalize().multiply(1.5); // Initial velocity

		new BukkitRunnable() {
			final Location currentLoc = startLoc.clone();
			final Vector currentVelocity = velocity.clone();
			int colorIndex = 0;
			double totalDistance = 0;
			final Set<Entity> hitEntities = new HashSet<>(); // Track hit entities to avoid duplicate damage

			@Override
			public void run() {
				// Apply reduced gravity to velocity
				currentVelocity.add(new Vector(0, -0.08, 0)); // Reduced gravity

				// Move the particle location
				Location previousLoc = currentLoc.clone();
				currentLoc.add(currentVelocity);

				// Calculate distance traveled this tick
				double distanceThisTick = previousLoc.distance(currentLoc);
				int particleCount = (int) (distanceThisTick * 5); // 5 particles per block

				// Spawn particles along the path
				for(int i = 0; i < particleCount; i++) {
					double t = (double) i / particleCount;
					Location particleLoc = previousLoc.clone().add(currentVelocity.clone().multiply(t));

					// Cycle through colors: Red, Yellow, Green
					Particle.DustOptions dust = switch(colorIndex % 3) {
						case 0 -> new Particle.DustOptions(Color.RED, 1.5f);
						case 1 -> new Particle.DustOptions(Color.YELLOW, 1.5f);
						default -> new Particle.DustOptions(Color.LIME, 1.5f); // Green
					};

					p.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dust);
					colorIndex++;
				}

				// Check for mob hits
				List<EntityType> doNotKill = doNotKill();
				for(Entity entity : Objects.requireNonNull(currentLoc.getWorld()).getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
					if(!hitEntities.contains(entity) && !doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerabilityTicks() != 0)) {
						// Deal 1 damage using Bukkit damage event
						Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(p, entity, EntityDamageByEntityEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), 1));
						hitEntities.add(entity);
					}
				}

				totalDistance += distanceThisTick;

				// Stop conditions
				if(currentLoc.getBlock().getType().isSolid() || // Hit a block
						currentLoc.getY() < -64 || // Fell into void
						totalDistance > 12) { // Max distance reduced to 15 blocks
					cancel();
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new leftClick() while holding the correct item")
	public static void gyro(Player p, Location l) {
		p.getWorld().spawnParticle(Particle.PORTAL, l, 512);
		l.setY(l.getY() + 1);
		new BukkitRunnable() {
			float pitch = 0.5f;

			@Override
			public void run() {
				if(pitch >= 2.0f) {
					cancel();
					return;
				}
				p.getWorld().playSound(l, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, pitch);
				pitch += 0.025f;
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);

		Material[] blockTypes = {Material.OBSIDIAN, Material.PURPLE_CONCRETE, Material.PURPLE_STAINED_GLASS};
		List<FallingBlock> fallingBlocks = new ArrayList<>();
		boolean[] effectEnded = {false}; // Flag to track if effect has ended

		// Initial block spawning
		for(double angle = 0; angle < Math.PI * 2; angle += Math.PI / 32) {
			double x = l.getX() + Math.cos(angle) * 10;
			double z = l.getZ() + Math.sin(angle) * 10;

			// Find the next air block going upwards from the requested location
			double y = l.getY();
			while(!Objects.requireNonNull(l.getWorld()).getBlockAt((int) x, (int) y, (int) z).getType().isAir() && y < l.getWorld().getMaxHeight()) {
				y++;
			}
			y += 0.05; // Place falling block 0.05 above the air block
			Location blockLoc = new Location(l.getWorld(), x, Math.max(y, l.getY()), z);

			Material blockType = blockTypes[(int) (Math.random() * blockTypes.length)];
			FallingBlock block = l.getWorld().spawnFallingBlock(blockLoc, blockType.createBlockData());
			block.setGravity(false);
			block.setDropItem(false);
			block.setInvulnerable(true);

			block.setHurtEntities(false);
			block.setPersistent(true);
			block.setCancelDrop(true);

			fallingBlocks.add(block);
		}

		// Ensure blocks don't place by constantly checking and respawning if needed
		BukkitRunnable blockChecker = new BukkitRunnable() {
			int bald;

			@Override
			public void run() {
				if(effectEnded[0]) {
					cancel();
					return;
				}

				List<FallingBlock> toRemove = new ArrayList<>();
				List<FallingBlock> toAdd = new ArrayList<>();

				for(FallingBlock block : fallingBlocks) {
					if(block.isDead() || !block.isValid()) {
						// The block has turned into a real block or disappeared
						toRemove.add(block);

						// Create a new block at the same location
						Material blockType = blockTypes[(int) (Math.random() * blockTypes.length)];
						Location respawnLoc = block.getLocation().clone().add(0, 0.1, 0);

						FallingBlock newBlock = Objects.requireNonNull(respawnLoc.getWorld()).spawnFallingBlock(respawnLoc, blockType.createBlockData());
						newBlock.setGravity(false);
						newBlock.setDropItem(false);
						newBlock.setInvulnerable(true);
						newBlock.setHurtEntities(false);
						newBlock.setPersistent(true);
						newBlock.setCancelDrop(true);

						toAdd.add(newBlock);
					}
				}

				// Update our block list
				fallingBlocks.removeAll(toRemove);
				fallingBlocks.addAll(toAdd);

				if(fallingBlocks.isEmpty()) {
					cancel();
				}
			}
		};
		blockChecker.runTaskTimer(M7tas.getInstance(), 1L, 1L);

		new BukkitRunnable() {
			int tick = 0;
			final List<Location> initialPositions = new ArrayList<>();
			final double TOTAL_TICKS = 60.0; // 3 seconds
			final double CONVERGE_TICKS = 40.0; // 2 seconds to converge

			@Override
			public void run() {
				if(tick == 0) {
					for(FallingBlock block : fallingBlocks) {
						initialPositions.add(block.getLocation());
					}
				}

				if(tick >= TOTAL_TICKS) {
					effectEnded[0] = true;
					fallingBlocks.forEach(block -> {
						if(block.isValid()) {
							block.remove();
						}
					});
					fallingBlocks.clear();
					cancel();
					return;
				}

				double progress = Math.min(tick / CONVERGE_TICKS, 1.0);

				for(int i = 0; i < fallingBlocks.size(); i++) {
					FallingBlock block = fallingBlocks.get(i);
					if(!block.isValid()) continue; // Skip invalid blocks

					Location startLoc = i < initialPositions.size() ? initialPositions.get(i) : block.getLocation();

					// Linear interpolation between start and rift
					Vector direction = l.toVector().subtract(startLoc.toVector());
					Vector currentPos = startLoc.toVector().add(direction.multiply(progress));

					if(progress < 1.0) {
						// Add constant wobble effect
						Vector wobble = new Vector((Math.random() - 0.5) * 4, (Math.random() - 0.5) * 4, (Math.random() - 0.5) * 4);

						currentPos.add(wobble);
					} else {
						// Keep blocks at center with slight wobble
						Vector wobble = new Vector((Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2);
						currentPos = l.toVector().add(wobble);
					}

					Location targetLoc = currentPos.toLocation(l.getWorld());

					// Ensure minimum height above ground
					// Find the next air block going upwards from the requested location
					double minY = l.getY();
					while(!Objects.requireNonNull(l.getWorld()).getBlockAt(targetLoc.getBlockX(), (int) minY, targetLoc.getBlockY()).getType().isAir() && minY < l.getWorld().getMaxHeight()) {
						minY++;
					}
					minY += 0.05; // Place falling block 0.05 above the air block
					targetLoc.setY(Math.max(targetLoc.getY(), minY));

					// Calculate velocity vector based on distance to target with increased speed
					Vector velocity = targetLoc.toVector().subtract(block.getLocation().toVector()).multiply(0.3);

					block.setVelocity(velocity);
				}

				tick++;
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);


		for(Entity e : Objects.requireNonNull(l.getWorld()).getNearbyEntities(l, 10, 10, 10)) {
			if(e instanceof LivingEntity entity && !(entity instanceof Player) && !(entity instanceof Wither)) {
				new BukkitRunnable() {
					int tick = 0;

					@Override
					public void run() {
						if(tick >= 60) { // 3 seconds * 20 ticks
							cancel();
							return;
						}

						if(tick < 10) { // First 0.5 seconds - pull in
							Location entityLoc = entity.getLocation();
							double x = (l.getX() - entityLoc.getX()) / 5;
							double y = (l.getY() - entityLoc.getY()) / 5;
							double z = (l.getZ() - entityLoc.getZ()) / 5;
							entity.setVelocity(new Vector(x, y, z));
						} else { // Next 2.5 seconds - keep at rift
							entity.teleport(l);
						}

						tick++;
					}
				}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
			}
		}
	}

	/**
	 * Simulates the Bonzo effect on the provided player with the given vector.
	 * <br>
	 * Note: The y-value of the Vector is ignored.  It will always be set to 0.5 upwards.
	 *
	 * @param p the Player instance on which the Bonzo simulation will be applied
	 * @param v the Vector representing the direction and magnitude for the simulation
	 * @return Returns the BukkitRunnable that can be cancelled.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static BukkitRunnable bonzo(Player p, Vector v) {
		if(!(p instanceof CraftPlayer cp)) {
			return null;
		}
		net.minecraft.world.entity.LivingEntity nmsEntity = cp.getHandle();

		// Set initial Y velocity
		Vec3 currentMovement = nmsEntity.getDeltaMovement();
		nmsEntity.setDeltaMovement(0, 0.5, 0);
		v.setY(0);

		Location location = p.getLocation();
		World world = p.getWorld();
		Random random = new Random();

		// Spawn 20 critical particles with random directions
		world.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 128, 0, 0, 0, 0.75);

		// Add critical particles for texture variety
		world.spawnParticle(Particle.CRIT, location, 64, 0, 0, 0, 2);

		p.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0F, 1.0F);

		double magnitude = Math.sqrt(v.getX() * v.getX() + v.getZ() * v.getZ());
		Vector impulseVector = v.clone().normalize().multiply(0.2806);

		// Apply horizontal movement
		BukkitRunnable runnalbe = new BukkitRunnable() {
			boolean firstTick = true;
			int tickCount = 0;

			@Override
			public void run() {
				tickCount++;

				if(firstTick) {
					forceMove(p, v, 1);
					firstTick = false;
				} else {
					if(nmsEntity.onGround()) {
						cancel();
						return;
					}
					forceMove(p, impulseVector, 1);
				}
			}
		};
		runnalbe.runTaskTimer(M7tas.getInstance(), 0L, 1L);
		return runnalbe;
	}

/**
	 * Sets up the player's initial location by resetting their velocity, teleporting them
	 * to the specified location, and updating the location of any spectators.
	 *
	 * @param player   The player whose initial location is being set.
	 * @param location The location where the player will be teleported.
	 */
	public static void teleport(Player player, Location location) {
		player.setVelocity(new Vector(0, 0, 0));
		player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
	}

	/**
	 * Simulate a left‐click (attack) in the air.  Your other plugin
	 * will see a PlayerInteractEvent with Action.LEFT_CLICK_AIR.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new leftClick() while holding the correct item")
	@SuppressWarnings("ConstantConditions")
	public static void oldSwingHand(Player p) {
		// 1) do the swing animation
		p.swingMainHand();

		Set<Player> spectators = Spectate.getSpectatingPlayers(p);
		for(Player spectator : spectators) {
			spectator.swingMainHand();
		}
	}

	/**
	 * Simulate a right‐click (use) in the air.  Your other plugin
	 * will see a PlayerInteractEvent with Action.RIGHT_CLICK_AIR.
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	@SuppressWarnings("ConstantConditions")
	public static void rightClickOld(Player p) {
		PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.RIGHT_CLICK_AIR, p.getInventory().getItemInMainHand(), null, null);
		Bukkit.getPluginManager().callEvent(ev);
	}

	@SuppressWarnings("ConstantConditions")
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void rightClickWithSpectators(Player p) {
		for(Player spectator : Spectate.getSpectatingPlayers(p)) {
			PlayerInteractEvent ev = new PlayerInteractEvent(spectator, Action.RIGHT_CLICK_AIR, p.getInventory().getItemInMainHand(), null, null);
			Bukkit.getPluginManager().callEvent(ev);
		}

		rightClickOld(p);
	}

	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void rightClickLever(Player p) {
		// Get the NMS player
		ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();

		// Perform ray trace
		RayTraceResult rayTrace = p.rayTraceBlocks(10);
		if(rayTrace == null || rayTrace.getHitBlock() == null) {
			return;
		}

		// Get block position
		BlockPos blockPos = new BlockPos(rayTrace.getHitBlock().getX(), rayTrace.getHitBlock().getY(), rayTrace.getHitBlock().getZ());

		// Get the block state
		BlockState blockState = nmsPlayer.level().getBlockState(blockPos);

		// Check if it's actually a lever
		if(!(blockState.getBlock() instanceof LeverBlock)) {
			return;
		}

		// Convert Bukkit BlockFace to NMS Direction
		assert rayTrace.getHitBlockFace() != null;
		Direction direction = convertBlockFace(rayTrace.getHitBlockFace());

		// Get the exact hit location
		Vec3 hitVec = new Vec3(rayTrace.getHitPosition().getX(), rayTrace.getHitPosition().getY(), rayTrace.getHitPosition().getZ());

		// Create BlockHitResult for the interaction
		BlockHitResult hitResult = new BlockHitResult(hitVec, direction, blockPos, false);

		// Get the item in hand (might be empty)
		net.minecraft.world.item.ItemStack itemInHand = nmsPlayer.getItemInHand(InteractionHand.MAIN_HAND);

		// Perform the interaction directly through the block's use method
		// For levers, we use useWithoutItem since they don't require an item
		InteractionResult result = blockState.useWithoutItem(nmsPlayer.level(), nmsPlayer, hitResult);

		// Check if interaction was successful
		if(result.consumesAction()) {

			// Swing arm for visual feedback
			nmsPlayer.swing(InteractionHand.MAIN_HAND, true);
		} else {

			// Try alternative approach with item
			result = blockState.useItemOn(itemInHand, nmsPlayer.level(), nmsPlayer, InteractionHand.MAIN_HAND, hitResult);

			if(result.consumesAction()) {
				nmsPlayer.swing(InteractionHand.MAIN_HAND, true);
			}
		}
	}

	private static Direction convertBlockFace(org.bukkit.block.BlockFace face) {
		return switch(face) {
			case DOWN -> Direction.DOWN;
			case NORTH -> Direction.NORTH;
			case SOUTH -> Direction.SOUTH;
			case WEST -> Direction.WEST;
			case EAST -> Direction.EAST;
			default -> Direction.UP;
		};
	}

	private static List<EntityType> doNotKill() {
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
		doNotKill.add(EntityType.PLAYER);
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
}