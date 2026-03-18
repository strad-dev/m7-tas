package instructions;

import com.mojang.datafixers.util.Pair;
import commands.Spectate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import plugin.M7tas;
import plugin.Utils;

import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings("unused")
public class Actions {

	/**
	 * Simulates a Player pressing movement input keys.  Only call this when the player changes which keys are pressed.
	 *
	 * @param player        The Player sending movement packets
	 * @param input         The input keys being held down<br>Valid Input contains the following characters: W, A, S, D, J, P, N<br>WASD: movement<br>J: Jump<br>P: sPrint<br>N: sNeak
	 * @param durationTicks How long the keys are held down for, or 0 for manual override<br>NOTE: Jumping will NOT be held down if duration is 0
	 */
	public static void move(Player player, String input, int durationTicks) {
		// only handle CraftLivingEntity/NMS and positive duration
		Utils.debug(Utils.DebugType.CLIENT, player.getName() + " moving " + input + " for " + durationTicks);
		if(!(player instanceof CraftPlayer craftPlayer)) return;

		ServerPlayer serverPlayer = craftPlayer.getHandle();
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

		if(input.contains("J")) {
			new BukkitRunnable() {
				int ticks = 0;

				@Override
				public void run() {
					if(ticks++ >= durationTicks || serverPlayer.isRemoved()) {
						cancel();
						return;
					}
					if(serverPlayer.onGround()) {
						serverPlayer.setJumping(true);
					}
				}
			}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
		}
		if(durationTicks > 0) {
			Utils.scheduleTask(() -> {
				if(player.isValid()) {
					serverPlayer.xxa = 0.0F;
					serverPlayer.zza = 0.0F;
					serverPlayer.setSprinting(false);
					serverPlayer.setShiftKeyDown(false);
					serverPlayer.setJumping(false);
				}
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
	public static void move(LivingEntity entity, Vector perTick, int durationTicks) {
		// only handle CraftLivingEntity/NMS and positive duration
		if(!(entity instanceof CraftLivingEntity cle) || durationTicks <= 0) return;

		net.minecraft.world.entity.LivingEntity nmsEntity = cle.getHandle();

		// This variable stores the requested motion from the caller.
		Vec3 motion = new Vec3(perTick.getX(), perTick.getY(), perTick.getZ());

		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if(ticks++ >= durationTicks || nmsEntity.isRemoved()) {
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
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
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

		// Tell that player's client "your held slot is now hotbarIndex"
		ClientboundSetHeldSlotPacket heldPkt = new ClientboundSetHeldSlotPacket(hotbarIndex);
		cp.getHandle().connection.send(heldPkt);

		// Broadcast the new main-hand item to all viewers AND sync to spectators
		Utils.syncHand(p); // This now handles both!
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
		Utils.syncInventory(p); // This updates spectators

		// If either swapped slot was in the hotbar, update the hand-item too
		if(slotA < 9 || slotB < 9) {
			Utils.syncHand(p); // This now handles both viewers and spectators
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
		serverPlayer.setXRot(pitch);
		serverPlayer.refreshDimensions();
		Utils.debug(Utils.DebugType.CLIENT, player.getName() + " turning head to " + yaw + " " + pitch);
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

		RayTraceResult entityRay = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 5.0, entity -> {
			if(entity == p) return false;
			if(!(entity instanceof LivingEntity)) return false;
			if(entity instanceof org.bukkit.entity.Player player) {
				if(M7tas.getFakePlayers().containsValue(player)) return false;
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

		RayTraceResult blockRay = p.rayTraceBlocks(5.0);

		if(blockRay != null && blockRay.getHitBlock() != null) {
			if(p.getInventory().getItemInMainHand().getType() != Material.DIAMOND_PICKAXE) return;

			Block block = blockRay.getHitBlock();
			BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
			Direction direction = CraftBlock.blockFaceToNotch(blockRay.getHitBlockFace());

			ServerboundPlayerActionPacket breakPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, 0);
			Utils.simulatePacket(p, breakPacket);
			ServerboundPlayerActionPacket breakStopPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction, 0);
			Utils.simulatePacket(p, breakStopPacket);
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

		// Check for block target FIRST (before entity)
		RayTraceResult blockRay = p.rayTraceBlocks(p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue());

		if(blockRay != null && blockRay.getHitBlock() != null) {
			// Use item on block
			Block block = blockRay.getHitBlock();
			BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
			Direction direction = CraftBlock.blockFaceToNotch(blockRay.getHitBlockFace());

			Vec3 hitVec = new Vec3(blockRay.getHitPosition().getX(), blockRay.getHitPosition().getY(), blockRay.getHitPosition().getZ());

			BlockHitResult blockHit = new BlockHitResult(hitVec, direction, pos, false);

			ServerboundUseItemOnPacket useOnBlockPacket = new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, blockHit, 0);
			Utils.simulatePacket(p, useOnBlockPacket);
			return;
		}

		// Check for entity target - EXCLUDE all players, skip for BOW items (prevents bow drawing on entity interact)
		if(p.getInventory().getItemInMainHand().getType() != Material.BOW) {
			RayTraceResult entityRay = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 5.0, entity -> entity != p && entity instanceof LivingEntity && !(entity instanceof Player target && Spectate.getSpectatingPlayers(p).contains(target)));

			if(entityRay != null && entityRay.getHitEntity() != null) {
				// Interact with the entity
				net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entityRay.getHitEntity()).getHandle();

				ServerboundInteractPacket interactPacket = ServerboundInteractPacket.createInteractionPacket(nmsEntity, serverPlayer.isShiftKeyDown(), InteractionHand.MAIN_HAND);
				Utils.simulatePacket(p, interactPacket);
			}
		}

		// Right click air (eat food, throw pearl, use item ability) - THIS IS WHAT YOU WANT FOR ETHERWARP
		ServerboundUseItemPacket useItemPacket = new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, serverPlayer.getYRot(), serverPlayer.getXRot());
		Utils.simulatePacket(p, useItemPacket);
	}

	public static void stopRightClick(Player p) {
		ServerPlayer sp = ((CraftPlayer) p).getHandle();
		ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, sp.blockPosition(), Direction.DOWN, 0);
		Utils.simulatePacket(p, packet);
	}

	public static void dropItem(Player p) {
		ServerboundPlayerActionPacket.Action action = p.isSprinting() ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : ServerboundPlayerActionPacket.Action.DROP_ITEM;
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
			if(!doNotKill.contains(entity.getType()) && !entity.equals(p) && entity instanceof LivingEntity entity1 && entity1.getHealth() > 0) {
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
		BlockData blockdata = b.getBlockData();
		b.setType(Material.AIR);
		Utils.scheduleTask(() -> {
			b.setType(material);
			b.setBlockData(blockdata);
		}, 6);
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
		zombie.setCustomName("Crypt Undead " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + "2M/2M");
		zombie.setCustomNameVisible(true);
		zombie.setAI(false);
		zombie.setSilent(true);
		zombie.setAdult();
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-2);
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
		Location targetLoc = target.getLocation();
		teleport(p, targetLoc);

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
	}

	public static void mimicChest(Player p, Block b) {
		stonk(p, b);

		Zombie zombie = (Zombie) p.getWorld().spawnEntity(b.getLocation().add(0.5, 0, 0.5), EntityType.ZOMBIE);
		zombie.setCustomName("Mimic " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + "4M/4M");
		zombie.setCustomNameVisible(true);
		zombie.setAI(false);
		zombie.setSilent(true);
		zombie.setBaby();
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-2);
		Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2);
		zombie.setHealth(2);

		Utils.scheduleTask(() -> {
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
					Arrow arrow1 = p.launchProjectile(Arrow.class);
					arrow1.setVelocity(l.clone().getDirection().multiply(velocity));
					arrow1.setDamage(1.0);
					arrow1.setShooter(p);
					arrow1.addScoreboardTag("TerminatorArrow");
					arrow1.setWeapon(p.getInventory().getItemInMainHand());

					// Play bow shoot sound
					p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);

					// Schedule second arrow for 3 ticks later
					Utils.scheduleTask(() -> {
						// Fire second arrow
						Arrow arrow2 = p.launchProjectile(Arrow.class);
						arrow2.setVelocity(l.clone().getDirection().multiply(velocity));
						arrow2.setDamage(0.2);
						arrow2.setShooter(p);
						arrow2.addScoreboardTag("TerminatorArrow");
						arrow2.setWeapon(p.getInventory().getItemInMainHand());

						// Play bow shoot sound again
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
					}, 3);

					if(p.getName().contains("Archer")) {
						Utils.scheduleTask(() -> {
							// Fire third arrow
							Arrow arrow2 = p.launchProjectile(Arrow.class);
							arrow2.setVelocity(l.clone().getDirection().multiply(velocity));
							arrow2.setDamage(1.0);
							arrow2.setShooter(p);
							arrow2.addScoreboardTag("TerminatorArrow");
							arrow2.setWeapon(p.getInventory().getItemInMainHand());

							// Play bow shoot sound again
							p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
						}, 5);

						Utils.scheduleTask(() -> {
							// Fire fourth arrow
							Arrow arrow2 = p.launchProjectile(Arrow.class);
							arrow2.setVelocity(l.clone().getDirection().multiply(velocity));
							arrow2.setDamage(1.0);
							arrow2.setShooter(p);
							arrow2.addScoreboardTag("TerminatorArrow");
							arrow2.setWeapon(p.getInventory().getItemInMainHand());

							// Play bow shoot sound again
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
	 * Simulates the Ice Spray ability, dealing damage but not applying increased damage.
	 *
	 * @param p The player using the ability
	 */
	@Deprecated(forRemoval = true, since = "2.0.0<br>Use new rightClick() while holding the correct item")
	public static void iceSpray(Player p) {
		Location l = p.getEyeLocation();
		p.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 1000);
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
		p.getWorld().spawnParticle(Particle.PORTAL, l, 1000);
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
			if(e instanceof LivingEntity entity && !(entity instanceof Player) && !(entity instanceof Wither) && !(entity instanceof EnderDragon)) {
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
		world.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 350, 0, 0, 0, 0.75);

		// Add critical particles for texture variety
		world.spawnParticle(Particle.CRIT, location, 150, 0, 0, 0, 2);

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
					move(p, v, 1);
					firstTick = false;
				} else {
					if(nmsEntity.onGround()) {
						cancel();
						return;
					}
					move(p, impulseVector, 1);
				}
			}
		};
		runnalbe.runTaskTimer(M7tas.getInstance(), 0L, 1L);
		return runnalbe;
	}

	public static void lavaJump(Player p, boolean big) {
		p.teleport(p.getLocation().add(0, 3.5, 0));
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);
		Utils.scheduleTask(() -> {
			if(!(p instanceof CraftPlayer cp)) {
				return;
			}

			ServerPlayer npc = cp.getHandle();

			Vec3 motion = npc.getDeltaMovement();
			if(big) {
				npc.setDeltaMovement(new Vec3(motion.x(), 3.4D, motion.z()));
			} else {
				npc.setDeltaMovement(new Vec3(motion.x(), 1.7D, motion.z()));
			}
		}, 1);

		new BukkitRunnable() {
			int tickCount = 0;

			@Override
			public void run() {
				tickCount++;

				if(tickCount >= 3) {
					if(((LivingEntity) p).isOnGround()) {
						cancel();
					}
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private static BukkitTask armorTask = null;

	public static void setWitherArmor(Wither wither, boolean showArmor) {

		// Cancel any existing task
		if(armorTask != null && !armorTask.isCancelled()) {
			armorTask.cancel();
			armorTask = null;
		}

		if(showArmor) {
			// Start the armor maintenance task
			armorTask = new BukkitRunnable() {
				@Override
				public void run() {
					// Reapply invulnerability ticks
					wither.setInvulnerabilityTicks(3);
				}
			}.runTaskTimer(M7tas.getInstance(), 0L, 1L); // Start immediately, repeat every 20 ticks (1 second)
		} else {
			// Remove armor immediately
			wither.setInvulnerabilityTicks(0);
		}
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