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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import plugin.*;

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

		// Which speed-granting helmet was worn before this swap? (captured pre-swap so we can detect a transition)
		boolean racingHelmetBefore = (slotA == 39 || slotB == 39)
				&& plugin.FakePlayerInventory.isRacingHelmet(p.getInventory().getHelmet());
		boolean cowHatBefore = (slotA == 39 || slotB == 39)
				&& plugin.FakePlayerInventory.isCowHat(p.getInventory().getHelmet());

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

		// Auto-manage speed on speed-helmet transitions: Racing Helmet → 650, Cow Hat → 550, neither → 400.
		// Swaps between two non-speed helmets (Bonzo/Spirit masks, armor sets) leave speed untouched. Same-tick
		// as the swap, so it's frame-accurate; a manual setSpeed() placed AFTER the swap call still wins
		// (e.g. Tank's 550 preleap with the Bonzo Mask on).
		if(slotA == 39 || slotB == 39) {
			boolean racingHelmetAfter = plugin.FakePlayerInventory.isRacingHelmet(p.getInventory().getHelmet());
			boolean cowHatAfter = plugin.FakePlayerInventory.isCowHat(p.getInventory().getHelmet());
			if(racingHelmetAfter && !racingHelmetBefore) {
				Utils.setSpeed(p, 650);
			} else if(cowHatAfter && !cowHatBefore) {
				Utils.setSpeed(p, 550);
			} else if((racingHelmetBefore && !racingHelmetAfter) || (cowHatBefore && !cowHatAfter)) {
				// A speed helmet came off (and neither is now on) — drop back to base speed.
				Utils.setSpeed(p, 400);
			}
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
	 * Turns the given player's head toward the nearest living entity whose custom name contains
	 * {@code nameContains} (case-insensitive) within a 32-block cube. No LOS or boss/player filtering —
	 * the caller is naming a specific mob. No-op if none match. Aim math delegates to
	 * {@link #snapHeadAtEntity}.
	 */
	public static void snapHeadAtNearestNamed(Player p, String nameContains) {
		LivingEntity nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Entity entity : p.getNearbyEntities(32, 32, 32)) {
			if(!(entity instanceof LivingEntity living)) continue;
			String livingName = Utils.plain(living.customName());
			if(livingName.isEmpty() || !livingName.toLowerCase().contains(nameContains.toLowerCase())) continue;
			double distance = p.getLocation().distance(living.getLocation());
			if(distance < nearestDistance) {
				nearestDistance = distance;
				nearest = living;
			}
		}
		if(nearest != null) snapHeadAtEntity(p, nearest);
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
		Double angle = solveLaunchAngle(dh, dy);
		if(angle == null) return; // out of ballistic range

		int now = MinecraftServer.currentTick;
		int flightTicks = simulateArrowFlightTicks(angle, dh);
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
	 * <br>
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
	 * <br>
	 * Uses the same entity filter as {@link #mageBeamWouldHit} (skips Players, dead, and
	 * resistance-255 mobs; includes Withers so Storm/etc. count as valid targets). The
	 * trajectory is walked tick-by-tick with Minecraft arrow physics ({@code v.xz *= 0.99},
	 * {@code v.y = v.y * 0.99 - 0.05}); the walk bails when horizontal distance exceeds
	 * {@link #TERMINATOR_MAX_RANGE} or horizontal velocity collapses.
	 * <br>
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
		Double angle = solveLaunchAngle(dh, dy);
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
	private static Double solveLaunchAngle(double dh, double dy) {
		double aimY = dy;
		double angle = Math.atan2(aimY, dh);
		for(int i = 0; i < 12; i++) {
			Double yHit = simulateArrowYAtDistance(angle, dh);
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
	private static int simulateArrowFlightTicks(double angleRad, double targetH) {
		double vh = 3.175 * Math.cos(angleRad);
		double vy = 3.175 * Math.sin(angleRad);
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
	private static Double simulateArrowYAtDistance(double angleRad, double targetH) {
		double vh = 3.175 * Math.cos(angleRad);
		double vy = 3.175 * Math.sin(angleRad);
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
			if(le instanceof Wither w && w.getInvulnerableTicks() != 0) return false;
			if(le instanceof org.bukkit.entity.Player player) {
				if(FakePlayerManager.getFakePlayers().containsValue(player)) return false;
				return !Spectate.getSpectatingPlayers(p).contains(player);
			}
			return true;
		});

		if(entityRay != null && entityRay.getHitEntity() != null) {
			net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entityRay.getHitEntity()).getHandle();
			// 26.2: ServerboundInteractPacket is a flat record. Attack = null hand & null location.
			ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(nmsEntity.getId(), null, null, serverPlayer.isShiftKeyDown());
			Utils.simulatePacket(p, attackPacket);
			return;
		}

		if(blockRay != null && blockRay.getHitBlock() != null) {
			// Wither/Blood door: left-clicking a door block opens it (a no-op without the matching key). Handled
			// here because a fake's non-pickaxe left-click routes through handleCustomItems, not a PlayerInteractEvent.
			Block hitBlock = blockRay.getHitBlock();
			if(Server.inWitherDoor(hitBlock)) { Server.tryOpenWitherDoor(p); return; }
			if(Server.inBloodDoor(hitBlock)) { Server.tryOpenBloodDoor(); return; }

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

			// 26.2: flat record. interact-at = hand + location (relative to entity); interact = hand, null location.
			ServerboundInteractPacket interactAtPacket = new ServerboundInteractPacket(nmsEntity.getId(), InteractionHand.MAIN_HAND, localHit, serverPlayer.isShiftKeyDown());
			Utils.simulatePacket(p, interactAtPacket);
			ServerboundInteractPacket interactPacket = new ServerboundInteractPacket(nmsEntity.getId(), InteractionHand.MAIN_HAND, null, serverPlayer.isShiftKeyDown());
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

	public static void leap(Player p, Player target) {
		// Spirit Leap requires the Infinileap (ender pearl) in hand — bail if the player isn't holding it.
		ItemStack held = p.getInventory().getItemInMainHand();
		if(!"skyblock/utility/infinileap".equals(CustomItems.getID(held))) {
			String heldDesc = held.getType().isAir() ? "an empty hand"
					: held.getType() + (held.hasItemMeta() && held.getItemMeta().hasDisplayName() ? " (" + Utils.displayName(held.getItemMeta()) + ")" : "")
					+ (CustomItems.getID(held) != null ? " [id=" + CustomItems.getID(held) + "]" : "");
			Utils.debug(Utils.DebugType.ERROR, p.getName() + " tried to leap while holding " + heldDesc + " instead of an Infinileap");
			return;
		}

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
		zombie.customName(Utils.msg("Mimic <yellow>4M<red>❤"));
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
	 * Spawns a deterministic arrow — no random spread — and returns the Bukkit {@link Arrow} for the caller to
	 * tag/configure. Bypasses {@code Player.launchProjectile} / vanilla bow release, whose CraftBukkit path calls
	 * {@code Arrow.shootFromRotation} with inaccuracy=1.0F (a random, run-to-run-varying spread direction). Goes
	 * directly through NMS {@code shoot(..., 0)} for a perfectly clean, repeatable trajectory — the same pattern
	 * as {@code CustomItems.terminator}'s shotgun arrows.
	 * <br>
	 * Both the spawn position (with the vanilla -0.1 Y offset {@code launchProjectile} applies) and the flight
	 * direction come from {@code aimFrom}, so a caller can capture one aim {@link Location} at fire time and reuse
	 * it for delayed bonus arrows — those then spawn at the same point and direction, not wherever the shooter has
	 * since moved.
	 */
	public static Arrow fireDeterministicArrow(Player p, Location aimFrom, float speed, double damage) {
		ServerLevel nmsWorld = ((CraftWorld) p.getWorld()).getHandle();
		ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();
		Vector dir = aimFrom.getDirection();

		net.minecraft.world.entity.projectile.arrow.Arrow nmsArrow = new net.minecraft.world.entity.projectile.arrow.Arrow(
				nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
		nmsArrow.setPos(aimFrom.getX(), aimFrom.getY() - 0.1, aimFrom.getZ());
		nmsArrow.shoot(dir.getX(), dir.getY(), dir.getZ(), speed, 0);
		nmsArrow.setOwner(nmsPlayer);
		nmsWorld.addFreshEntity(nmsArrow);

		Arrow arrow = (Arrow) nmsArrow.getBukkitEntity();
		arrow.setDamage(damage);
		arrow.setShooter(p);
		arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		arrow.setWeapon(p.getInventory().getItemInMainHand());
		return arrow;
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

}