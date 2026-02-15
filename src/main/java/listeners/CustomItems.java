package listeners;

import commands.Spectate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import plugin.DebugType;
import plugin.M7tas;
import plugin.Utils;

import java.util.*;

public class CustomItems implements Listener {
	private static final Map<UUID, Integer> cooldowns = new HashMap<>();

	public static String getID(ItemStack item) {
		if(item == null || !item.hasItemMeta()) {
			return "";
		} else if(!item.getItemMeta().hasLore()) {
			return "";
		} else return item.getItemMeta().getLore().getFirst();
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

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		handleCustomItems(e, e.getHand(), e.getItem(), e.getAction(), e.getPlayer());
	}

	@EventHandler
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
		handleCustomItems(e, e.getHand(), e.getPlayer().getInventory().getItemInMainHand(), Action.RIGHT_CLICK_AIR, e.getPlayer());
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if(e.getEntity() instanceof LivingEntity entity) {
			Utils.scheduleTask(() -> Utils.changeName(entity), 1);
		}
		if(e.getDamager() instanceof Player p) {
			handleCustomItems(e, EquipmentSlot.HAND, p.getInventory().getItemInMainHand(), Action.LEFT_CLICK_AIR, p);
		} else if(e.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player p && arrow.getScoreboardTags().contains("TerminatorArrow") && e.getEntity() instanceof LivingEntity entity) {
			e.setCancelled(true);
			entity.setNoDamageTicks(0);
			entity.damage(arrow.getDamage());
			p.playSound(p, Sound.ENTITY_ARROW_HIT_PLAYER, 0.75f, 0.79368752611448590621283707774885f);
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if(getID(e.getPlayer().getInventory().getItemInMainHand()).equals("skyblock/combat/stonk")) {
			stonk(e.getBlock());
		}
	}

	@EventHandler
	public void onPlayerAnimation(PlayerAnimationEvent e) {
		Player p = e.getPlayer();
		if(e.getAnimationType().equals(PlayerAnimationType.ARM_SWING) && M7tas.getFakePlayers().containsValue(p) && Spectate.getReverseSpectatorMap().containsKey(p)) {
			for(Player spectator : Spectate.getReverseSpectatorMap().get(p)) {
				spectator.swingMainHand();
			}
		}
	}

	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent e) {
		if(e.getEntity() instanceof Player p) {
			String id = getID(p.getInventory().getItemInMainHand());
			if(id != null && id.equals("skyblock/combat/last_breath")) {
				Entity temp = e.getProjectile();
				Arrow arrow;
				if(temp instanceof Arrow) {
					arrow = (Arrow) temp;
				} else {
					return;
				}
				arrow.setDamage(1.0);
				arrow.addScoreboardTag("TerminatorArrow");
				Utils.scheduleTask(() -> {
					// Fire second arrow
					Arrow arrow2 = p.launchProjectile(Arrow.class);
					arrow2.setVelocity(arrow.getVelocity());
					arrow2.setDamage(0.2);
					arrow2.setShooter(p);
					arrow2.addScoreboardTag("TerminatorArrow");
					arrow2.setWeapon(p.getInventory().getItemInMainHand());

					// Play bow shoot sound again
					p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
				}, 3);

				if(p.getName().contains("Archer")) {
					Utils.scheduleTask(() -> {
						// Fire third arrow
						Arrow arrow2 = p.launchProjectile(Arrow.class);
						arrow2.setVelocity(arrow.getVelocity());
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
						arrow2.setVelocity(arrow.getVelocity());
						arrow2.setDamage(1.0);
						arrow2.setShooter(p);
						arrow2.addScoreboardTag("TerminatorArrow");
						arrow2.setWeapon(p.getInventory().getItemInMainHand());

						// Play bow shoot sound again
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
					}, 10);
				}
			}
		}
	}

	@SuppressWarnings({"DuplicateExpressions", "RedundantSuppression"})
	public static void handleCustomItems(Cancellable e, EquipmentSlot hand, ItemStack item, Action action, Player p) {
		if(Objects.equals(hand, EquipmentSlot.HAND)) {
			String id = getID(item);
			if((item.getType() == Material.IRON_SWORD || item.getType() == Material.STONE_SWORD) && (p.getName().startsWith("Mage") || p.getScoreboardTags().contains("Mage"))) {
				if(action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)) {
					e.setCancelled(true);
					mageBeam(p);
				}
			} else if(id != null) {
				if(action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)) {
					switch(id) {
						case "skyblock/combat/terminator" -> {
							e.setCancelled(true);
							salvation(p);
						}
						case "skyblock/combat/gyro" -> {
							e.setCancelled(true);
							gyro(p);
						}
					}
				}
				if(action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
					int currentTick = MinecraftServer.currentTick;
					if(currentTick >= cooldowns.getOrDefault(p.getUniqueId(), 0)) {
						cooldowns.put(p.getUniqueId(), currentTick + 1);
						switch(id) {
							case "skyblock/combat/scylla" -> {
								e.setCancelled(true);
								witherImpact(p);
							}
							case "skyblock/combat/aotv" -> {
								e.setCancelled(true);
								aotv(p);
							}
							case "skyblock/combat/rag" -> {
								e.setCancelled(true);
								rag(p);
							}
							case "skyblock/combat/aots" -> {
								e.setCancelled(true);
								aots(p);
							}
							case "skyblock/combat/ice_spray" -> {
								e.setCancelled(true);
								iceSpray(p);
							}
							case "skyblock/combat/flaming_flay" -> {
								e.setCancelled(true);
								flamingFlay(p);
							}
							case "skyblock/combat/bonzo" -> {
								e.setCancelled(true);
								bonzo(p);
							}
							case "skyblock/combat/terminator" -> {
								e.setCancelled(true);
								terminator(p);
							}
							case "skyblock/combat/tac" -> {
								e.setCancelled(true);
								tac(p);
							}
						}
					}
				}
			}
		}
	}

	public static void witherImpact(Player p) {
		// raytrace???
		// 1-block tall gap is ok because crawling i guess??
		// hit block, determine via blockface
		//		hit bottom
		//			check if 2 blocks below is possible, otherwise tp to 1 block below
		//		hit sides
		//			teleport to the block before the hit block in the raytrace
		//		hit top
		//			teleport to the block above
		// otherwise just move X blocks
		Location origin = p.getLocation().clone();
		RayTraceResult result = p.rayTraceBlocks(11.65);
		if(result == null) {
			Location l = p.getLocation().add(p.getLocation().getDirection().multiply(10));
			l.setX(Math.floor(l.getX()) + 0.5);
			l.setY(Math.floor(l.getY()));
			l.setZ(Math.floor(l.getZ()) + 0.5);

			// Check if the target location is safe
			Block feetBlock = l.getBlock();
			Block headBlock = feetBlock.getRelative(BlockFace.UP);

			// If either block is solid, we need to adjust
			if(!feetBlock.isPassable() || !headBlock.isPassable()) {
				// Try to move up until we find a safe spot or reach original height
				double originalY = p.getLocation().getY();
				Location checkLoc = l.clone();
				boolean foundSafe = false;

				// Check up to 10 blocks up or until at original height
				for(int i = 0; i < 10; i++) {
					checkLoc.add(0, 1, 0);
					Block checkFeet = checkLoc.getBlock();
					Block checkHead = checkFeet.getRelative(BlockFace.UP);

					// Check if this position is safe (2 blocks of air)
					if(checkFeet.isPassable() && checkHead.isPassable()) {
						// Also check we're not in a 1-block gap if above original height
						if(checkLoc.getY() >= originalY) {
							Block aboveHead = checkHead.getRelative(BlockFace.UP);
							if(!aboveHead.isPassable()) {
								// This is a 1-block gap at or above original height - skip it
								continue;
							}
						}

						l = checkLoc.clone();
						foundSafe = true;
						break;
					}

					// Stop if we've reached or passed original height and no safe spot
					if(checkLoc.getY() >= originalY) {
						break;
					}
				}

				// If no safe spot found, don't teleport
				if(!foundSafe) {
					p.sendMessage("§cNo safe teleport location found!");
					return;
				}
			}

			// Additional check for 1-block tall spaces when below original height
			if(l.getY() < p.getLocation().getY()) {
				Block aboveHead = l.getBlock().getRelative(BlockFace.UP, 2);
				if(!aboveHead.isPassable()) {
					// This would put player in crawl mode below their starting position
					// Try to find a better spot
					for(int i = 1; i <= 3; i++) {
						Location upLoc = l.clone().add(0, i, 0);
						Block upFeet = upLoc.getBlock();
						Block upHead = upFeet.getRelative(BlockFace.UP);
						Block upAbove = upHead.getRelative(BlockFace.UP);

						if(upFeet.isPassable() && upHead.isPassable() && upAbove.isPassable()) {
							l = upLoc;
							break;
						}
					}
				}
			}

			l.setYaw(origin.getYaw());
			l.setPitch(origin.getPitch());
			p.teleport(l);
			Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
		} else {
			switch(result.getHitBlockFace()) {
				case SELF -> {
					// empty case
				}
				case UP -> {
					Location l = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
					l.setYaw(origin.getYaw());
					l.setPitch(origin.getPitch());
					p.teleport(l);
					Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
				}
				case DOWN -> {
					Location l = result.getHitBlock().getLocation().add(0.5, -2, 0.5);
					l.setYaw(origin.getYaw());
					l.setPitch(origin.getPitch());
					p.teleport(l);
					Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
				}
				default -> {
					// Hit a side face - backtrack until we find a safe spot
					Location hitLocation = result.getHitPosition().toLocation(p.getWorld());
					Vector direction = origin.getDirection().normalize();

					// Calculate max backtrack distance (don't go past player's origin)
					double maxBacktrack = origin.distance(hitLocation);

					// Backtrack from the exact hit point
					Location checkLoc = hitLocation.clone();
					Location lastSafe = null;
					double totalBacktracked = 0;

					// Backtrack in smaller increments for more precision
					for(int i = 0; i < 100; i++) { // 120 * 0.1 = 12 blocks
						// Backtrack by 0.1 blocks for precision
						checkLoc.subtract(direction.clone().multiply(0.1));
						totalBacktracked += 0.1;

						// Don't go past the player's starting position
						if(totalBacktracked > maxBacktrack) {
							break;
						}

						// Check current block
						Block feetBlock = checkLoc.getBlock();
						Block headBlock = feetBlock.getRelative(BlockFace.UP);

						if(feetBlock.isPassable() && headBlock.isPassable()) {
							// This spot is safe, but keep checking for the optimal position
							lastSafe = checkLoc.clone();

							// Check if we've backtracked enough (at least 0.5 blocks from wall)
							if(checkLoc.distance(hitLocation) >= 0.5) {
								// Center on the block we're in
								Location l = new Location(checkLoc.getWorld(), Math.floor(checkLoc.getX()) + 0.5, Math.floor(checkLoc.getY()), Math.floor(checkLoc.getZ()) + 0.5);
								l.setYaw(origin.getYaw());
								l.setPitch(origin.getPitch());
								p.teleport(l);
								Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
								p.setFallDistance(0);
								Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
								Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
								Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 0.66666f);
								return;
							}
						}
					}

					// If we found a safe spot but didn't teleport yet
					if(lastSafe != null) {
						Location l = new Location(lastSafe.getWorld(), Math.floor(lastSafe.getX()) + 0.5, Math.floor(lastSafe.getY()), Math.floor(lastSafe.getZ()) + 0.5);
						l.setYaw(origin.getYaw());
						l.setPitch(origin.getPitch());
						p.teleport(l);
						Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
					}
				}
			}
		}
		p.setFallDistance(0);
		Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);

		// implosion
		p.getWorld().spawnParticle(Particle.EXPLOSION, p.getEyeLocation(), 20);
		List<Entity> entities = p.getNearbyEntities(10, 10, 10);
		List<EntityType> doNotKill = CustomItems.doNotKill();
		int damaged = 0;
		double damage = 0;
		for(Entity entity : entities) {
			if(!doNotKill.contains(entity.getType()) && !entity.equals(p) && entity instanceof LivingEntity entity1 && entity1.getHealth() > 0) {
				entity1.damage(1);
				damaged += 1;
				damage += 1;
			}
		}
		if(damaged > 0) {
			p.sendMessage(ChatColor.RED + "Your Implosion hit " + damaged + " enemies for " + ((int) damage) + " damage");
		}
		Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

		// wither shield
		// does not affect anything
		Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 0.66666f);
	}

	public static void aotv(Player p) {
		if(p.isSneaking()) {
			RayTraceResult result = p.rayTraceBlocks(61);
			if(result != null) {
				Block b = result.getHitBlock();
				Location l = b.getLocation().add(0.5, 1, 0.5);
				if(l.getBlock().getType().isSolid() || l.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
					return;
				}
				l.setYaw(p.getEyeLocation().getYaw());
				l.setPitch(p.getEyeLocation().getPitch());
				p.setFallDistance(0);
				Utils.playLocalSound(p, Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.50F);
				Utils.debug(DebugType.SERVER, "Etherwarping " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
				p.teleport(l);
			}
		} else {
			Location origin = p.getLocation().clone();
			RayTraceResult result = p.rayTraceBlocks(13.65);
			if(result == null) {
				Location l = p.getLocation().add(p.getLocation().getDirection().multiply(12));
				l.setX(Math.floor(l.getX()) + 0.5);
				l.setY(Math.floor(l.getY()));
				l.setZ(Math.floor(l.getZ()) + 0.5);

				// Check if the target location is safe
				Block feetBlock = l.getBlock();
				Block headBlock = feetBlock.getRelative(BlockFace.UP);

				// If either block is solid, we need to adjust
				if(!feetBlock.isPassable() || !headBlock.isPassable()) {
					// Try to move up until we find a safe spot or reach original height
					double originalY = p.getLocation().getY();
					Location checkLoc = l.clone();
					boolean foundSafe = false;

					// Check up to 10 blocks up or until at original height
					for(int i = 0; i < 10; i++) {
						checkLoc.add(0, 1, 0);
						Block checkFeet = checkLoc.getBlock();
						Block checkHead = checkFeet.getRelative(BlockFace.UP);

						// Check if this position is safe (2 blocks of air)
						if(checkFeet.isPassable() && checkHead.isPassable()) {
							// Also check we're not in a 1-block gap if above original height
							if(checkLoc.getY() >= originalY) {
								Block aboveHead = checkHead.getRelative(BlockFace.UP);
								if(!aboveHead.isPassable()) {
									// This is a 1-block gap at or above original height - skip it
									continue;
								}
							}

							l = checkLoc.clone();
							foundSafe = true;
							break;
						}

						// Stop if we've reached or passed original height and no safe spot
						if(checkLoc.getY() >= originalY) {
							break;
						}
					}

					// If no safe spot found, don't teleport
					if(!foundSafe) {
						return;
					}
				}

				// Additional check for 1-block tall spaces when below original height
				if(l.getY() < p.getLocation().getY()) {
					Block aboveHead = l.getBlock().getRelative(BlockFace.UP, 2);
					if(!aboveHead.isPassable()) {
						// This would put player in crawl mode below their starting position
						// Try to find a better spot
						for(int i = 1; i <= 3; i++) {
							Location upLoc = l.clone().add(0, i, 0);
							Block upFeet = upLoc.getBlock();
							Block upHead = upFeet.getRelative(BlockFace.UP);
							Block upAbove = upHead.getRelative(BlockFace.UP);

							if(upFeet.isPassable() && upHead.isPassable() && upAbove.isPassable()) {
								l = upLoc;
								break;
							}
						}
					}
				}

				l.setYaw(origin.getYaw());
				l.setPitch(origin.getPitch());
				p.teleport(l);
				Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
			} else {
				switch(result.getHitBlockFace()) {
					case SELF -> {
						// empty case
					}
					case UP -> {
						Location l = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
						l.setYaw(origin.getYaw());
						l.setPitch(origin.getPitch());
						p.teleport(l);
						Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
					}
					case DOWN -> {
						Location l = result.getHitBlock().getLocation().add(0.5, -2, 0.5);
						l.setYaw(origin.getYaw());
						l.setPitch(origin.getPitch());
						p.teleport(l);
						Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
					}
					default -> {
						// Hit a side face - backtrack until we find a safe spot
						Location hitLocation = result.getHitPosition().toLocation(p.getWorld());
						Vector direction = origin.getDirection().normalize();

						// Calculate max backtrack distance (don't go past player's origin)
						double maxBacktrack = origin.distance(hitLocation);

						// Backtrack from the exact hit point
						Location checkLoc = hitLocation.clone();
						Location lastSafe = null;
						double totalBacktracked = 0;

						// Backtrack in smaller increments for more precision
						for(int i = 0; i < 120; i++) { // 120 * 0.1 = 12 blocks
							// Backtrack by 0.1 blocks for precision
							checkLoc.subtract(direction.clone().multiply(0.1));
							totalBacktracked += 0.1;

							// Don't go past the player's starting position
							if(totalBacktracked > maxBacktrack) {
								break;
							}

							// Check current block
							Block feetBlock = checkLoc.getBlock();
							Block headBlock = feetBlock.getRelative(BlockFace.UP);

							if(feetBlock.isPassable() && headBlock.isPassable()) {
								// This spot is safe, but keep checking for the optimal position
								lastSafe = checkLoc.clone();

								// Check if we've backtracked enough (at least 0.5 blocks from wall)
								if(checkLoc.distance(hitLocation) >= 0.5) {
									// Center on the block we're in
									Location l = new Location(checkLoc.getWorld(), Math.floor(checkLoc.getX()) + 0.5, Math.floor(checkLoc.getY()), Math.floor(checkLoc.getZ()) + 0.5);
									l.setYaw(origin.getYaw());
									l.setPitch(origin.getPitch());
									p.teleport(l);
									Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
									p.setFallDistance(0);
									Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
									return;
								}
							}
						}

						// If we found a safe spot but didn't teleport yet
						if(lastSafe != null) {
							Location l = new Location(lastSafe.getWorld(), Math.floor(lastSafe.getX()) + 0.5, Math.floor(lastSafe.getY()), Math.floor(lastSafe.getZ()) + 0.5);
							l.setYaw(origin.getYaw());
							l.setPitch(origin.getPitch());
							p.teleport(l);
							Utils.debug(DebugType.SERVER, "Teleporting " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
						}
					}
				}
			}
			p.setFallDistance(0);
			Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
		}
	}

	public static void stonk(Block b) {
		// Capture block data
		Material m = b.getType();
		BlockData data = b.getBlockData().clone();
		Utils.debug(DebugType.SERVER, "Stonking block at " + b.getLocation().getX() + " " + b.getLocation().getY() + " " + b.getLocation().getZ());

		// Schedule restoration with Java's scheduler
		Utils.scheduleTask(() -> {
			b.setType(m);
			b.setBlockData(data);
		}, 200);
	}

	public static void rag(Player p) {
		Utils.playLocalSound(p, Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F), 20);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F), 40);
		Utils.scheduleTask(() -> {
			Utils.playLocalSound(p, Sound.ENTITY_WOLF_WHINE, 1.0F, 1.5F);
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
		for(int i = 0; i < 640 && pierce > 0; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			assert world != null;
			ArrayList<Entity> entities = (ArrayList<Entity>) world.getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				if(!damagedEntities.contains(entity) && !doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerabilityTicks() != 0)) {
					damagedEntities.add(entity);
					entity1.damage(2.5 + add);
					pierce--;
				}
			}
			Particle.DustOptions particle = new Particle.DustOptions(Color.RED, 1.0F);
			world.spawnParticle(Particle.DUST, l, 1, particle);
			l.add(v);
		}
		Utils.playLocalSound(p, Sound.ENTITY_GUARDIAN_DEATH, 0.5f, 2.0F);
	}

	public static void aots(Player p) {
		Utils.playLocalSound(p, Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);

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

	public static void iceSpray(Player p) {
		Location l = p.getEyeLocation();
		p.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 1000);
		List<Entity> entities = (List<Entity>) p.getWorld().getNearbyEntities(l, 8, 8, 8);
		List<EntityType> doNotKill = doNotKill();
		for(Entity entity : entities) {
			if(!doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerabilityTicks() != 0)) {
				entity1.damage(1);
			}
		}
		Utils.playLocalSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
	}

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
						entity1.damage(1);
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

	public static void gyro(Player p) {
		RayTraceResult result = p.rayTraceBlocks(24);
		if(result == null) {
			return;
		}
		Location l = result.getHitBlock().getLocation();
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
				Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, pitch);
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

	public static void bonzo(Player p) {
		Location l = p.getEyeLocation();
		WindCharge windCharge = (WindCharge) l.getWorld().spawnEntity(l, EntityType.WIND_CHARGE);
		windCharge.addScoreboardTag("Bonzo");
		windCharge.setShooter(p);
	}

	public static void terminator(Player p) {
		// you don't need arrows
		p.getInventory().remove(Material.ARROW);
		p.getInventory().remove(Material.TIPPED_ARROW);
		p.getInventory().remove(Material.SPECTRAL_ARROW);

		// Get NMS world and player
		ServerLevel nmsWorld = ((CraftWorld) p.getWorld()).getHandle();
		ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();

		// Calculate directions
		Vector baseDirection = p.getEyeLocation().getDirection().normalize();
		Vector leftDirection = baseDirection.clone().rotateAroundY(Math.toRadians(-5));
		Vector rightDirection = baseDirection.clone().rotateAroundY(Math.toRadians(5));

		// Calculate spawn position
		Location l = p.getEyeLocation().add(baseDirection.clone());

		// Calculate rotations
		float baseYaw = p.getEyeLocation().getYaw();
		float basePitch = p.getEyeLocation().getPitch();

		// Create NMS arrows directly
		net.minecraft.world.entity.projectile.arrow.Arrow nmsLeft = new net.minecraft.world.entity.projectile.arrow.Arrow(net.minecraft.world.entity.EntityType.ARROW, nmsWorld);
		net.minecraft.world.entity.projectile.arrow.Arrow nmsMiddle = new net.minecraft.world.entity.projectile.arrow.Arrow(net.minecraft.world.entity.EntityType.ARROW, nmsWorld);
		net.minecraft.world.entity.projectile.arrow.Arrow nmsRight = new net.minecraft.world.entity.projectile.arrow.Arrow(net.minecraft.world.entity.EntityType.ARROW, nmsWorld);

		// Set positions and rotations directly
		nmsLeft.setPos(l.getX(), l.getY(), l.getZ());
		nmsLeft.setYRot(baseYaw - 5f);
		nmsLeft.setXRot(basePitch);

		nmsMiddle.setPos(l.getX(), l.getY(), l.getZ());
		nmsMiddle.setYRot(baseYaw);
		nmsMiddle.setXRot(basePitch);

		nmsRight.setPos(l.getX(), l.getY(), l.getZ());
		nmsRight.setYRot(baseYaw + 5f);
		nmsRight.setXRot(basePitch);

		// Set velocities
		double speed = 4.0;
		Vec3 leftVel = new Vec3(leftDirection.getX() * speed, leftDirection.getY() * speed, leftDirection.getZ() * speed);
		Vec3 middleVel = new Vec3(baseDirection.getX() * speed, baseDirection.getY() * speed, baseDirection.getZ() * speed);
		Vec3 rightVel = new Vec3(rightDirection.getX() * speed, rightDirection.getY() * speed, rightDirection.getZ() * speed);

		nmsLeft.setDeltaMovement(leftVel);
		nmsMiddle.setDeltaMovement(middleVel);
		nmsRight.setDeltaMovement(rightVel);

		// Set other properties
		nmsLeft.setOwner(nmsPlayer);
		nmsMiddle.setOwner(nmsPlayer);
		nmsRight.setOwner(nmsPlayer);

		// Add to world
		nmsWorld.addFreshEntity(nmsLeft);
		nmsWorld.addFreshEntity(nmsMiddle);
		nmsWorld.addFreshEntity(nmsRight);

		// Get Bukkit wrappers for further modification
		Arrow left = (Arrow) nmsLeft.getBukkitEntity();
		Arrow middle = (Arrow) nmsMiddle.getBukkitEntity();
		Arrow right = (Arrow) nmsRight.getBukkitEntity();

		// Calculate bonuses
		double powerBonus;
		try {
			int power = p.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.POWER);
			powerBonus = power * 0.25;
		} catch(Exception exception) {
			powerBonus = 0;
		}

		double strengthBonus;
		try {
			strengthBonus = 0.75 + 0.75 * p.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier();
		} catch(Exception exception) {
			strengthBonus = 0;
		}

		double add = powerBonus + strengthBonus;

		// Set Bukkit properties
		for(Arrow arrow : Arrays.asList(left, middle, right)) {
			arrow.setDamage(2.5 + add);
			arrow.setPierceLevel(4);
			arrow.setShooter(p);
			arrow.setWeapon(p.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
		}

		Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);

		// Duplex Arrow
		Utils.scheduleTask(() -> {
			Arrow arrow = p.getWorld().spawnArrow(l, l.getDirection(), 4, 0);
			arrow.setDamage(0.5 + add * 0.2);
			arrow.setPierceLevel(4);
			arrow.setShooter(p);
			arrow.setWeapon(p.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
			Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
		}, 3);

		if(p.getName().equals("Archer")) {
			Utils.scheduleTask(() -> {
				Arrow arrow = p.getWorld().spawnArrow(l, l.getDirection(), 4, 0);
				arrow.setDamage(2.5 + add);
				arrow.setPierceLevel(4);
				arrow.setShooter(p);
				arrow.setWeapon(p.getInventory().getItemInMainHand());
				arrow.addScoreboardTag("TerminatorArrow");
				Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
			}, 5);

			Utils.scheduleTask(() -> {
				Arrow arrow = p.getWorld().spawnArrow(l, l.getDirection(), 4, 0);
				arrow.setDamage(2.5 + add);
				arrow.setPierceLevel(4);
				arrow.setShooter(p);
				arrow.setWeapon(p.getInventory().getItemInMainHand());
				arrow.addScoreboardTag("TerminatorArrow");
				p.playSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
			}, 10);
		}
	}

	public static void tac(Player p) {
		Location l = p.getLocation();
		Utils.debug(DebugType.SERVER, "Activating Tactical Insertion at " + l.getX() + " " + l.getY() + " " + l.getZ());
		Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.707107F);
		Utils.playLocalSound(p, Sound.ITEM_FLINTANDSTEEL_USE, 1.0F, 1.0F);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.793701F), 10);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.890899F), 20);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.943874F), 30);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 1F), 40);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 1.059463F), 50);
		Utils.scheduleTask(() -> {
			Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);
			p.teleport(l);
			Utils.debug(DebugType.SERVER, "Returning " + p.getName() + " to " + l.getX() + " " + l.getY() + " " + l.getZ());
			p.setVelocity(new Vector(0, 0, 0));
			Utils.scheduleTask(() -> p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 1000), 1);
		}, 60);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F), 63);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F), 66);
	}

	public static void mageBeam(Player p) {
		Location l = p.getLocation();

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

		// Get the eye location and direction
		Location eyeLocation = p.getEyeLocation();
		Vector eyeDirection = eyeLocation.getDirection();

		// Raytrace to find what the player is actually looking at
		Vector targetPoint = findTargetPoint(p, eyeLocation, eyeDirection);

		// Calculate the direction from hand to the target point
		Vector handToTarget = targetPoint.clone().subtract(l.toVector());
		handToTarget.normalize();

		// Scale down the vector for per-iteration movement
		Vector v = handToTarget.multiply(0.2);

		for(int i = 0; i < 175; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			boolean shouldBreak = false;
			ArrayList<Entity> entities = (ArrayList<Entity>) p.getWorld().getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				if(entity instanceof LivingEntity temp && !(temp instanceof Player) && !entity.isDead() && !entity.isInvulnerable() && !(temp.hasPotionEffect(PotionEffectType.RESISTANCE) && temp.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255)) {
					double damage = p.getScoreboardTags().contains("RagBuff") ? (temp instanceof Wither ? 275 : 180) : (temp instanceof Wither ? 220 : 145);
					temp.damage(damage, DamageSource.builder(DamageType.GENERIC_KILL).build());
					Utils.changeName(temp);
					shouldBreak = true;
					break;
				}
			}
			spawnFireworkParticle(l);
			l.add(v);
			if(shouldBreak) {
				for(int j = 0; j < 6; j++) {
					spawnFireworkParticle(l);
					l.add(v);
				}
				spawnFireworkParticle(l);
				break;
			}
		}
	}

	private static Vector findTargetPoint(Player p, Location eyeLocation, Vector eyeDirection) {
		World world = p.getWorld();

		// Raytrace for blocks
		RayTraceResult blockResult = world.rayTraceBlocks(eyeLocation, eyeDirection, 35, FluidCollisionMode.NEVER, true);

		// Raytrace for entities (excluding the player)
		RayTraceResult entityResult = world.rayTraceEntities(eyeLocation, eyeDirection, 35, 0.5,
				entity -> entity instanceof LivingEntity && !(entity instanceof Player) && !entity.isDead());

		double blockDist = 35;
		double entityDist = 35;

		if(blockResult != null) {
			blockDist = eyeLocation.toVector().distance(blockResult.getHitPosition());
		}

		if(entityResult != null) {
			entityDist = eyeLocation.toVector().distance(entityResult.getHitPosition());
		}

		// Return the closest hit point, or max distance if nothing was hit
		if(entityResult != null && entityDist <= blockDist) {
			return entityResult.getHitPosition();
		} else if(blockResult != null) {
			return blockResult.getHitPosition();
		} else {
			// Nothing hit - target 35 blocks out
			return eyeLocation.toVector().add(eyeDirection.clone().multiply((double) 35));
		}
	}

	public static void spawnFireworkParticle(Location l) {
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.FIREWORK, false, false, l.getX(), l.getY(), l.getZ(), 0.0f, 0.0f, 0.0f, 0.0f, 1);
		Utils.broadcastPacket(packet);
	}
}