package instructions;

import jline.internal.Nullable;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugin.M7tas;
import plugin.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class Actions {
	private static final Map<LivingEntity, Vec3> entityVelocities = new ConcurrentHashMap<>();

	/**
	 * Moves a LivingEntity in this Direction for t ticks. The Vector refers to the number of blocks per tick.
	 * The Y component of the vector is ignored. Vertical motion is left to gravity or other methods.
	 *
	 * @param entity        The LivingEntity to move
	 * @param perTick       The Distance to be moved per tick
	 * @param durationTicks The total number of Ticks to move
	 */
	public static void move(LivingEntity entity, Vector perTick, int durationTicks) {
		// only handle CraftLivingEntity/NMS and positive duration
		if(!(entity instanceof CraftLivingEntity cle) || durationTicks <= 0) return;

		net.minecraft.world.entity.LivingEntity nmsEntity = cle.getHandle();

		// This variable stores the requested motion from the caller.
		Vec3 motion = new Vec3(perTick.getX(), 0, perTick.getZ());

		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if(ticks++ >= durationTicks || nmsEntity.isRemoved()) {
					cancel();
					return;
				}

				boolean onGround = nmsEntity.onGround();

				// This variable stores the motion that can be manipulated if the entity is not on the ground
				Vec3 finalMotion = motion;

				if(!onGround) {
					// This is the current velocity of the entity. Empty entry = no velocity
					Vec3 currentVelocity = entityVelocities.getOrDefault(entity, new Vec3(0, 0, 0));
					double current = Math.sqrt(currentVelocity.x() * currentVelocity.x() + currentVelocity.z() * currentVelocity.z());
					double requested = Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z());

					if(current > requested) {
						// Apply air friction to current velocity
						finalMotion = new Vec3(currentVelocity.x() * 0.91, 0, currentVelocity.z() * 0.91);
						double newSpeed = Math.sqrt(finalMotion.x() * finalMotion.x() + finalMotion.z() * finalMotion.z());

						// If we've decelerated below the requested speed, use the requested speed
						if(newSpeed < requested) {
							finalMotion = motion;
						}
					}
				}

				// Update tracked velocity
				entityVelocities.put(entity, new Vec3(finalMotion.x(), 0, finalMotion.z()));

				// Just move the entity with the calculated motion
				nmsEntity.move(MoverType.SELF, finalMotion);
				System.out.println(entity.getName() + " is now Moving!  On Ground?  " + onGround + " | " + finalMotion);
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}


	/**
	 * Makes a Player entity perform a jump if it is currently on the ground.
	 *
	 * @param p The Player entity that is to perform the jump. Requires the player to be on the ground.
	 */
	public static void jump(Player p) {
		if(!(p instanceof CraftPlayer cp)) return;

		ServerPlayer npc = cp.getHandle();

		if(npc.onGround()) { // onGround check
			Vec3 motion = npc.getDeltaMovement();
			npc.setDeltaMovement(new Vec3(motion.x(), 0.42D, motion.z()));
		}
	}

	/**
	 * Change which hotbar slot (0–8) the fake player is “holding”,
	 * and broadcast that slot change.
	 *
	 * @param p           The player whose hotbar slot is to be changed.
	 * @param hotbarIndex The index of the new hotbar slot (0–8).
	 */
	public static void setFakePlayerHotbarSlot(Player p, int hotbarIndex) {
		if(!(p instanceof CraftPlayer cp)) return;
		ServerPlayer npc = cp.getHandle();

		// Update the NMS held-slot index
		npc.getInventory().setSelectedHotbarSlot(hotbarIndex);

		// Tell that player's client "your held slot is now hotbarIndex"
		ClientboundSetHeldSlotPacket heldPkt = new ClientboundSetHeldSlotPacket(hotbarIndex);
		cp.getHandle().connection.send(heldPkt);

		// Broadcast the new main-hand item to all viewers AND sync to spectators
		Utils.syncFakePlayerHand(p); // This now handles both!
	}

	/**
	 * Swap two inventory slots in the fake player’s inventory and
	 * immediately broadcast both slot changes.
	 *
	 * @param slotA any slot index (0–8 hotbar, 9–35 main inv, 36–39 armor, 40 offhand)
	 * @param slotB same range
	 */
	public static void swapFakePlayerInventorySlots(Player p, int slotA, int slotB) {
		if(!(p instanceof CraftPlayer cp)) return;
		ServerPlayer npc = cp.getHandle();
		Inventory inv = npc.getInventory();

		// Swap internally
		net.minecraft.world.item.ItemStack a = inv.getItem(slotA);
		net.minecraft.world.item.ItemStack b = inv.getItem(slotB);
		inv.setItem(slotA, b);
		inv.setItem(slotB, a);

		// Re-send the entire inventory window to fake player AND spectators
		Utils.syncInventoryToSpectators(p); // This updates spectators

		// If either swapped slot was in the hotbar, update the hand-item too
		if(slotA < 9 || slotB < 9) {
			Utils.syncFakePlayerHand(p); // This now handles both viewers and spectators
		}
	}

	/**
	 * Rotates a player’s body & head to the given yaw/pitch
	 * and broadcasts that new orientation to every real client.
	 * <p>
	 * Works on both real and fake Players (CraftPlayer instances).
	 *
	 * @param p     the Bukkit Player (real or NPC)
	 * @param yaw   body + head yaw, in degrees (0 = south, 90 = west…)
	 * @param pitch look pitch, in degrees (–90 = straight up, +90 = straight down)
	 */
	public static void turnHead(Player p, float yaw, float pitch) {
		Location to = p.getLocation();
		to.setYaw(yaw);
		to.setPitch(pitch);

		Utils.teleport(p, to);
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
	public static void simulateWitherImpact(Player p, @Nullable Location to) {
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
				EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(p, entity, EntityDamageEvent.DamageCause.MAGIC, DamageSource.builder(DamageType.MAGIC).build(), 1);

				Bukkit.getPluginManager().callEvent(damageEvent);
				damaged += 1;
			}
		}
		if(damaged > 0) {
			p.sendMessage(ChatColor.RED + "Your Implosion hit " + damaged + " enemies for " + damaged + " damage.");
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
	public static void simulateEtherwarp(Player p, Location to) {
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
	public static void simulateAOTV(Player p, Location to) {
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
	public static void simulateStonking(Player p, Block b) {
		if(p != null) {
			p.swingMainHand();
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0F, 1.0F);
		}
		Material material = b.getType();
		BlockData blockdata = b.getBlockData();
		b.setType(Material.AIR);
		Utils.scheduleTask(() -> {
			b.setType(material);
			b.setBlockData(blockdata);
		}, 5);
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
	public static void simulateGhostPick(Player p, Block b) {
		if(p != null) {
			p.swingMainHand();
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
	public static void simulateSuperboom(Player p, int x1, int y1, int z1, int x2, int y2, int z2) {
		Actions.simulateLeftClickAir(p);
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
	public static void simulateCrypt(Player p, int x1, int y1, int z1, int x2, int y2, int z2) {
		Actions.simulateLeftClickAir(p);
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:air");
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
		Zombie zombie = (Zombie) p.getWorld().spawnEntity(new Location(p.getWorld(), (double) (x1 + x2) / 2, Math.min(y1, y2), (double) (z1 + z2) / 2), EntityType.ZOMBIE);
		zombie.setCustomName("Crypt Undead " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + 1 + "/" + 1);
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

	public static void simulateLeap(Player p, Player target) {
		Location targetLoc = target.getLocation();
		teleport(p, targetLoc);

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
	}

	public static void mimicChest(Player p, Block b) {
		simulateStonking(p, b);

		Zombie zombie = (Zombie) p.getWorld().spawnEntity(b.getLocation().add(0.5, 0, 0.5), EntityType.ZOMBIE);
		zombie.setCustomName("Mimic " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + 2 + "/" + 2);
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
	public static void simulatePearlThrow(Player p) {
		EnderPearl pearl = (EnderPearl) p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.ENDER_PEARL);
		pearl.setGravity(true);
		pearl.setShooter(null);
		pearl.setVelocity(p.getLocation().getDirection().normalize().multiply(3)); // Normal pearl speed is ~3.0

		Actions.simulateLeftClickAir(p);
		p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount() - 1);
	}

	/**
	 * Simulates the actions performed when using the "Rag Axe" ability, which involves
	 * playing a series of lever click sounds, applying a temporary "RagBuff" tag to
	 * the player, and then removing the tag after a specific duration.
	 *
	 * @param p The player for whom the "Rag Axe" ability simulation will be performed.
	 */
	public static void simulateRagAxe(Player p) {
		p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F), 20);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F), 40);
		Utils.scheduleTask(() -> {
			p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0F, 1.5F);
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
	public static void simulateSalvation(Player p) {
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
		damagedEntities.add(p);
		int pierce = 5;
		for(int i = 0; i < 320 && pierce > 0; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			assert world != null;
			ArrayList<Entity> entities = (ArrayList<Entity>) world.getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				if(entity instanceof LivingEntity temp && !damagedEntities.contains(entity)) {
					damagedEntities.add(entity);
					Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(p, temp, EntityDamageByEntityEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), 2.5 + add));
					pierce--;
				}
			}
			Particle.DustOptions particle = new Particle.DustOptions(Color.RED, 1.0F);
			world.spawnParticle(Particle.DUST, l, 1, particle);
			l.add(v);
		}
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GUARDIAN_DEATH, 1.0F, 2.0F);
	}

	public static void simulateSpringBoots(Player p) {
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
			if(!(p instanceof CraftPlayer cp)) return;

			ServerPlayer npc = cp.getHandle();

			if(npc.onGround()) { // onGround check
				Vec3 motion = npc.getDeltaMovement();
				npc.setDeltaMovement(new Vec3(motion.x(), 2.045D, motion.z()));
			}
		}, 17);
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
	@SuppressWarnings("ConstantConditions")
	public static void simulateLeftClickAir(Player p) {
		// 1) do the swing animation
		p.swingMainHand();

		List<Player> spectators = M7tas.getSpectatingPlayers(p);
		for(Player spectator : spectators) {
			spectator.swingMainHand();
		}

		// 2) build & fire the same event Bukkit would normally fire
		PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.LEFT_CLICK_AIR, p.getInventory().getItemInMainHand(), null, null);
		Bukkit.getPluginManager().callEvent(ev);
	}

	/**
	 * Simulate a right‐click (use) in the air.  Your other plugin
	 * will see a PlayerInteractEvent with Action.RIGHT_CLICK_AIR.
	 */
	@SuppressWarnings("ConstantConditions")
	public static void simulateRightClickAir(Player p) {
		PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.RIGHT_CLICK_AIR, p.getInventory().getItemInMainHand(), null, null);
		Bukkit.getPluginManager().callEvent(ev);
	}

	@SuppressWarnings("ConstantConditions")
	public static void simulateRightClickAirWithSpectators(Player p) {
		for(Player spectator : M7tas.getSpectatingPlayers(p)) {
			PlayerInteractEvent ev = new PlayerInteractEvent(spectator, Action.RIGHT_CLICK_AIR, p.getInventory().getItemInMainHand(), null, null);
			Bukkit.getPluginManager().callEvent(ev);
		}

		simulateRightClickAir(p);
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