package instructions;

import instructions.bosses.CustomBossBar;
import instructions.bosses.Maxor;
import instructions.bosses.Storm;
import instructions.bosses.Watcher;
import listeners.CustomItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.Utils;

import java.util.*;

public class Server {
	private static final Zombie[] archaeologists = new Zombie[10];
	private static Zombie yellowShadowAssassin = null;
	private static final LivingEntity[] trashMobs = new LivingEntity[18]; // each 1x1 has 6 mobs spawned

	public static void serverInstructions(World world, String section) {
		// Begin with 3 seconds of delay
		Bukkit.broadcastMessage("TAS starts in 3 seconds");

		Utils.scheduleTask(() -> {
			switch(section) {
				case "all", "clear" -> {
					Utils.scheduleTask(() -> Utils.debug(Utils.DebugType.SERVER, "Out of Bounds Check Triggered!"), 40);
					Utils.scheduleTask(() -> Utils.debug(Utils.DebugType.SERVER, "Out of Bounds Check Triggered!"), 80);
					Utils.scheduleTask(() -> Utils.debug(Utils.DebugType.SERVER, "Out of Bounds Check Triggered!"), 120);

					// 5-second countdown
					Utils.scheduleTask(() -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 5 seconds");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 26);
					Utils.scheduleTask(() -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 4 seconds");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 46);
					Utils.scheduleTask(() -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 3 seconds");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 66);
					Utils.scheduleTask(() -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 2 seconds");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 86);
					Utils.scheduleTask(() -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 1 seconds");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 106);
					Utils.scheduleTask(() -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Run started");
						Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 1.0F);
						Watcher.watcherInstructions(world, section.equals("all"));
						openFirstDoor();
					}, 126);
				}
				case "boss" -> Maxor.maxorInstructions(world, true);
				case "maxor" -> Maxor.maxorInstructions(world, false);
//				case "storm" -> {
//					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 91 165 41 95 167 40 minecraft:air");
//					Storm.stormInstructions(world, false);
//				}
//				case "goldor" -> {
//					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 62 136 142 58 133 142 minecraft:lever[face=wall,facing=north,powered=true]");
//					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 58 136 143 62 133 143 minecraft:redstone_lamp[lit=true]");
//					Goldor.goldorInstructions(world, false);
//				}
//				case "necron" -> Necron.necronInstructions(world, false);
//				case "witherking" -> WitherKing.witherKingInstructions(world);
			}
		}, 60);
	}

	public static void serverSetup(World world) {
		CustomItems.flushStonkRestorations();
		spawnMinibosses(world);
		resetGoldorCheese();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -170 -120 72 -168 minecraft:chiseled_stone_bricks");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -69 82 -155 -69 74 -151 minecraft:iron_bars replace minecraft:air");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -120 69 -106 -122 72 -104 minecraft:coal_block");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -74 -120 72 -72 minecraft:red_terracotta");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 62 136 142 58 133 142 minecraft:lever[face=wall,facing=north,powered=false]");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 58 136 143 62 133 143 minecraft:redstone_lamp[lit=false]");
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 43 196 38 49 175 44 43 175 62");
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 43 196 38 49 175 44 97 175 62");
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 91 -2 45 89 -1 46 89 131 45");
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 96 -2 121 96 -1 123 96 120 121");
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 70 -5 120 38 -1 99 38 59 99");
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 54 64 79 minecraft:jungle_planks");
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 54 64 80 minecraft:jungle_stairs");
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 54 63 79 minecraft:stone_brick_slab[type=top]");
		CustomBossBar.forceCleanup();
		Watcher.forceCleanup();
		Storm.cleanupMobs();
//		turnArrow(world, false);
	}

	public static void resetGoldorCheese() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 91 167 40 91 165 41 minecraft:coal_block");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 92 167 40 95 165 41 minecraft:stone_bricks");
	}

	private static void spawnMinibosses(World world) {
		for(Zombie zombie : archaeologists) {
			if(zombie != null) {
				zombie.remove();
			}
		}

		Location[] locations = {new Location(world, -120.5, 69, -152.5, 90f, 0f), // Red Blue (I)
				new Location(world, -120.5, 67, -88.5, -90f, 0f), // Deathmite (II)
				new Location(world, -24.5, 69, -184.5, 0f, 0f), // Well (III)
				new Location(world, -56.5, 69, -48.5, -90f, 0f), // Gravel (III)
				new Location(world, -168.5, 65.0625, -104.5f, -180, 0f), // Museum (III)
				new Location(world, -35.5, 69, -120.5, 90f, 0f), // Dino Dig Site (IV)
				new Location(world, -184.5, 69, -28.5, -90f, 0f), // Market (IV)
				new Location(world, -152.5, 69, -24.5, -90f, 0f), // Hallway (V)
		};

		double[] healthValues = {15, 16, 17, 17, 17, 18, 18, 19};

		for(int i = 0; i < locations.length; i++) {
			Zombie zombie = (Zombie) world.spawnEntity(locations[i], EntityType.ZOMBIE);
			zombie.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Angry Archaeologist " + ChatColor.RESET + ChatColor.YELLOW + ((int) healthValues[i] * 2) + "M" + ChatColor.RED + "❤");
			zombie.setCustomNameVisible(true);
			zombie.setAI(false);
			zombie.setSilent(true);
			zombie.setAdult();
			zombie.setPersistent(true);
			zombie.setRemoveWhenFarAway(false);
			Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
			Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
			Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(healthValues[i]);
			zombie.setHealth(healthValues[i]);

			assert zombie.getEquipment() != null;
			zombie.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
			zombie.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
			zombie.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
			zombie.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
			zombie.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));

			archaeologists[i] = zombie;
		}

		if(yellowShadowAssassin != null) {
			yellowShadowAssassin.remove();
		}

		yellowShadowAssassin = (Zombie) world.spawnEntity(new Location(world, -184.5, 69, -184.5, 0f, 0f), EntityType.ZOMBIE);
		yellowShadowAssassin.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Shadow Assassin " + ChatColor.RESET + ChatColor.YELLOW + "60M" + ChatColor.RED + "❤");
		yellowShadowAssassin.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0));
		yellowShadowAssassin.setCustomNameVisible(true);
		yellowShadowAssassin.setAI(false);
		yellowShadowAssassin.setSilent(true);
		yellowShadowAssassin.setAdult();
		yellowShadowAssassin.setPersistent(true);
		yellowShadowAssassin.setRemoveWhenFarAway(false);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(30);
		yellowShadowAssassin.setHealth(30);

		ItemStack boots = Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.PURPLE, ChatColor.LIGHT_PURPLE + "Shadow Assassin Boots");
		assert yellowShadowAssassin.getEquipment() != null;
		yellowShadowAssassin.getEquipment().setBoots(boots);
		yellowShadowAssassin.getEquipment().setLeggings(new ItemStack(Material.AIR));
		yellowShadowAssassin.getEquipment().setChestplate(new ItemStack(Material.AIR));
		yellowShadowAssassin.getEquipment().setHelmet(new ItemStack(Material.AIR));
		yellowShadowAssassin.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
	}

	public static void openFirstDoor() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -170 -120 72 -168 minecraft:glass");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -170 -120 72 -168 minecraft:air"), 20);
	}

	public static void openWitherDoor(Player p) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -120 69 -106 -122 72 -104 minecraft:glass");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -120 69 -106 -122 72 -104 minecraft:air"), 20);
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		Bukkit.broadcastMessage(ChatColor.GOLD + Utils.getRealName(p) + ChatColor.GREEN + " opend a " + ChatColor.DARK_GRAY + ChatColor.BOLD + "WITHER " + ChatColor.RESET + ChatColor.GREEN + "door!");
	}

	public static void openBloodDoor() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -74 -120 72 -72 minecraft:glass");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -74 -120 72 -72 minecraft:air"), 20);
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		Utils.playGlobalSound(Sound.ENTITY_GHAST_HURT, 1.0F, 0.5F);
		Bukkit.broadcastMessage(ChatColor.RED + "The " + ChatColor.BOLD + "BLOOD DOOR" + ChatColor.RESET + ChatColor.RED + " has been opened!");
		Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "A shiver runs down your spine...");
	}

	public static void openIceFillRewards() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -69 82 -155 -69 74 -151 minecraft:air replace minecraft:iron_bars");
	}

	public static void removeS3Gate() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 19 135 128 16 115 136 minecraft:air");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 19 -21 128 16 -1 136 16 115 128"), 100);
	}

	public static void openCore() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 56 121 54 52 115 54 minecraft:glass");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 56 121 54 52 115 54 minecraft:air"), 20);
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 56 121 54 52 115 54 minecraft:gold_block"), 200);
	}

	private static Location getRandomLocation(Location center) {
		double x = center.getX() + (Math.random() * 2 - 1) * 1.25;
		double z = center.getZ() + (Math.random() * 2 - 1) * 1.25;
		return new Location(center.getWorld(), x, center.getY(), z, (float) (Math.random() * 360) - 180, 0);
	}

	public static void playWitherDeathSound(Wither wither) {
		Utils.playGlobalSound(Sound.ENTITY_WITHER_DEATH);
		wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 4);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 10);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 14);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 20);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 24);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 30);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 34);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 40);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 44);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 50);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 54);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 60);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 70);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 80);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 90);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 100);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 110);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 120);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 130);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 140);
		Utils.scheduleTask(wither::remove, 160);
	}

	public static Zombie spawnCryptLurker(Location loc, boolean isPrince) {
		Zombie zombie = (Zombie) Objects.requireNonNull(loc.getWorld())
				.spawnEntity(loc, EntityType.ZOMBIE);
		zombie.setAdult();
		// Clear random armor and prevent chicken jockey from finalizeSpawn
		assert zombie.getEquipment() != null;
		zombie.getEquipment().setHelmet(null);
		zombie.getEquipment().setChestplate(null);
		zombie.getEquipment().setLeggings(null);
		zombie.getEquipment().setBoots(null);
		if(zombie.isInsideVehicle()) {
			Entity vehicle = zombie.getVehicle();
			zombie.leaveVehicle();
			if(vehicle != null) vehicle.remove();
		}
		zombie.setAI(false);
		zombie.setSilent(true);
		zombie.setPersistent(true);
		zombie.setRemoveWhenFarAway(false);
		zombie.setCustomNameVisible(true);
		Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(1);
		zombie.setHealth(1);
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
		zombie.getEquipment().setItemInMainHand(new ItemStack(Material.BONE));
		String mobName = isPrince ? "Prince" : "Crypt Lurker";
		zombie.setCustomName(ChatColor.RED + mobName + ChatColor.RESET
				+ ChatColor.RED + " ❤" + ChatColor.YELLOW + "2M");
		return zombie;
	}

	private static LivingEntity spawnTrashMob(Location loc) {
		boolean isZombie = Math.random() < 0.5;
		LivingEntity mob = (LivingEntity) Objects.requireNonNull(loc.getWorld()).spawnEntity(loc, isZombie ? EntityType.ZOMBIE : EntityType.SKELETON);

		int health = new Random().nextInt(5) + 1; // 1-5 HP
		mob.setCustomNameVisible(true);
		if(mob instanceof Zombie) ((Zombie) mob).setAdult();
		mob.setAI(false);
		mob.setSilent(true);
		mob.setPersistent(true);
		mob.setRemoveWhenFarAway(false);

		Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(health);
		mob.setHealth(health);

		Color helmetColor = Color.WHITE;
		Color chestplateColor = Color.WHITE;
		Color leggingsColor = Color.WHITE;
		Color bootsColor = Color.WHITE;
		String name = "";
		Material weapon = Material.AIR;
		double defense = 0;

		if(mob instanceof Zombie) {
			switch(health) {
				case 1 -> {
					name = "Crypt Lurker";
					weapon = Material.BONE;
				}
				case 2 -> {
					name = "Zombie Soldier";
					helmetColor = Color.fromRGB(0xD07F00);
					chestplateColor = Color.fromRGB(0xD07F00);
					leggingsColor = Color.fromRGB(0xD07F00);
					bootsColor = Color.fromRGB(0xD07F00);
				}
				case 3 -> {
					name = "Tank Zombie";
					helmetColor = Color.fromRGB(0xFFFFFE);
					chestplateColor = Color.fromRGB(0x828282);
					leggingsColor = Color.fromRGB(0x828282);
					bootsColor = Color.fromRGB(0xFFFFFE);
				}
				case 4 -> {
					name = "Super Tank Zombie";
					weapon = Material.STONE_SWORD;
					helmetColor = Color.fromRGB(0xE6E6E6);
					chestplateColor = Color.fromRGB(0x5A6464);
					leggingsColor = Color.fromRGB(0x5A6464);
					bootsColor = Color.fromRGB(0xE6E6E6);
				}
				case 5 -> {
					name = "Zombie Commander";
					weapon = Material.FISHING_ROD;
					helmetColor = Color.fromRGB(0xD51230);
					chestplateColor = Color.fromRGB(0xD51230);
					leggingsColor = Color.fromRGB(0xD51230);
					bootsColor = Color.fromRGB(0xD51230);
				}
			}
		} else {
			switch(health) {
				case 1 -> {
					name = "Skeleton Soldier";
					weapon = Material.BOW;
					helmetColor = Color.fromRGB(0xFFBC0B);
					chestplateColor = Color.fromRGB(0xFFBC0B);
					leggingsColor = Color.fromRGB(0xFFBC0B);
					bootsColor = Color.fromRGB(0xFFBC0B);
				}
				case 2 -> {
					name = "Skeleton Master";
					weapon = Material.BOW;
					helmetColor = Color.fromRGB(0xFF6B0B);
					chestplateColor = Color.fromRGB(0xFF6B0B);
					leggingsColor = Color.fromRGB(0xFF6B0B);
					bootsColor = Color.fromRGB(0xFF6B0B);
				}
				case 3 -> {
					name = "Skeleton Lord";
					weapon = Material.BOW;
					helmetColor = Color.fromRGB(0xFFFF55);
					chestplateColor = Color.fromRGB(0x268105);
					leggingsColor = Color.fromRGB(0x268105);
					bootsColor = Color.fromRGB(0x268105);
				}
				case 4 -> {
					name = "Skeletor Prime";
					weapon = Material.BONE;
					helmetColor = Color.fromRGB(0xAAAAAA);
					chestplateColor = Color.fromRGB(0x191919);
					leggingsColor = Color.fromRGB(0x191919);
					bootsColor = Color.fromRGB(0x191919);
				}
				case 5 -> {
					name = "Super Archer";
					weapon = Material.BOW;
				}
			}
		}

		mob.setCustomName(ChatColor.RED + name + ChatColor.RESET + ChatColor.RED + " ❤" + ChatColor.YELLOW + (health * 2) + "M");
		Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);

		if(weapon != Material.AIR) {
			assert mob.getEquipment() != null;
			mob.getEquipment().setItemInMainHand(new ItemStack(weapon));
		}

		if(helmetColor != Color.WHITE) {
			assert mob.getEquipment() != null;
			mob.getEquipment().setHelmet(Utils.createLeatherArmor(Material.LEATHER_HELMET, helmetColor, name + " Helmet"));
			mob.getEquipment().setChestplate(Utils.createLeatherArmor(Material.LEATHER_CHESTPLATE, chestplateColor, name + " Chestplate"));
			mob.getEquipment().setLeggings(Utils.createLeatherArmor(Material.LEATHER_LEGGINGS, leggingsColor, name + " Leggings"));
			mob.getEquipment().setBoots(Utils.createLeatherArmor(Material.LEATHER_BOOTS, bootsColor, name + " Boots"));
		}

		return mob;
	}

	public static void spawn1x1Mobs(World world) {
		Location zodd = new Location(world, -90.5, 67, -90.5);
		Location admin = new Location(world, -88.5, 69, -24.5);
		Location tomioka = new Location(world, -216.5, 69, -120.5);

		// Remove existing mobs
		for(LivingEntity mob : trashMobs) {
			if(mob != null) {
				mob.remove();
			}
		}

		int index = 0;
		Location[] spawnPoints = {zodd, admin, tomioka};

		for(Location center : spawnPoints) {
			for(int i = 0; i < 6; i++) {
				Location spawnLoc = getRandomLocation(center);
				trashMobs[index++] = spawnTrashMob(spawnLoc);
			}
		}
	}

	public static void turnArrow(World world, boolean isCompleting) {
		Block block = world.getBlockAt(-2, 122, 77);
		Collection<Entity> entities = world.getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 1, 1, 1);

		for(Entity entity : entities) {
			if(entity instanceof ItemFrame itemFrame) {
				// Check if this item frame is actually at the block we want
				if(entity.getLocation().getBlockX() == -2 &&
						entity.getLocation().getBlockY() == 122 &&
						entity.getLocation().getBlockZ() == 77) {

					if(isCompleting) {
						itemFrame.setRotation(Rotation.CLOCKWISE_45);
					} else {
						itemFrame.setRotation(Rotation.NONE);
					}
					break;
				}
			}
		}
	}

	public static class Quiz {
		private static final Location PARTICLE_START = new Location(null, -24.5, 85.5, -23.0);
		private static final double[][] OPTION_COORDS = {{-19.5, 71.5, -33.5}, {-24.5, 71.5, -30.5}, {-29.5, 71.5, -33.5}};
		private static final String[] OPTION_LABELS = {"ⓐ", "ⓑ", "ⓒ"};
		private static final float[] OPTION_PITCHES = {0.6f, 0.7f, 0.8f};
		private static final String ORUO = ChatColor.DARK_RED + "[STATUE] Oruo the Omniscient" + ChatColor.WHITE + ": ";

		public static void oruoMessage(String message) {
			Bukkit.broadcastMessage(ORUO + message);
			Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
		}

		private static TextDisplay spawnOption(World world, Location loc, String text) {
			TextDisplay td = (TextDisplay) world.spawnEntity(loc, EntityType.TEXT_DISPLAY);
			td.setText(text);
			td.setAlignment(TextDisplay.TextAlignment.CENTER);
			td.setBillboard(Display.Billboard.CENTER);
			return td;
		}

		private static void spawnParticleTrail(World world, Location start, Location end) {
			int duration = 40;
			double dx = (end.getX() - start.getX()) / duration;
			double dy = (end.getY() - start.getY()) / duration;
			double dz = (end.getZ() - start.getZ()) / duration;
			for(int i = 0; i <= duration; i++) {
				final int tick = i;
				Utils.scheduleTask(() -> world.spawnParticle(Particle.HAPPY_VILLAGER, start.getX() + dx * tick, start.getY() + dy * tick, start.getZ() + dz * tick, 1, 0, 0, 0, 0), i);
			}
		}

		private static void spawnQuestion(int questionNum, String questionText, String[] answers, TextDisplay[] options, Location particleStart, Player player, World world) {
			Bukkit.broadcastMessage("");
			Bukkit.broadcastMessage(ChatColor.GOLD + "                                " + ChatColor.BOLD + "Question #" + questionNum);
			Bukkit.broadcastMessage(ChatColor.GOLD + questionText);
			Bukkit.broadcastMessage("");
			for(int i = 0; i < 3; i++) {
				Bukkit.broadcastMessage(ChatColor.GOLD + "     " + OPTION_LABELS[i] + " " + ChatColor.GREEN + answers[i]);
			}
			Bukkit.broadcastMessage("");
			Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
			for(int i = 0; i < 3; i++) {
				final int idx = i;
				Utils.scheduleTask(() -> spawnParticleTrail(world, particleStart, new Location(world, OPTION_COORDS[idx][0], OPTION_COORDS[idx][1], OPTION_COORDS[idx][2])), idx * 10);
			}
			for(int i = 0; i < 3; i++) {
				final int idx = i;
				Utils.scheduleTask(() -> {
					options[idx] = spawnOption(world, new Location(world, OPTION_COORDS[idx][0], OPTION_COORDS[idx][1], OPTION_COORDS[idx][2]), ChatColor.GOLD + OPTION_LABELS[idx] + " " + ChatColor.GREEN + answers[idx]);
					Utils.playLocalSound(player, Sound.ENTITY_ITEM_PICKUP, 2.0f, OPTION_PITCHES[idx]);
				}, 40 + idx * 10);
			}
			Utils.scheduleTask(() -> {
				answeredCorrectly(questionNum, options);
				Actions.rightClick(player);
			}, 61);
		}

		private static void answeredCorrectly(int questionNum, TextDisplay[] options) {
			Bukkit.broadcastMessage(ORUO + ChatColor.GOLD + "akc0303 " + ChatColor.GREEN + "answered " + ChatColor.GOLD + "Question #" + questionNum + ChatColor.GREEN + " correctly!");
			Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
			Utils.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 0.75f);
			options[0].remove();
			options[1].remove();
			options[2].remove();
		}

		public static void run(Player player, World world) {
			Utils.scheduleTask(() -> {
				Actions.turnHead(player, 0f, 5f);
				Actions.setHotbarSlot(player, 5);
			}, 137);
			Utils.scheduleTask(() -> oruoMessage("Prove your knowledge by answering 3 questions and I shall reward you in ways that transcend time!"), 155);
			Utils.scheduleTask(() -> oruoMessage("Answer incorrectly, and your moment of ineptitude will live on for generations."), 195);
			final TextDisplay[] options = new TextDisplay[3];
			Location particleStart = PARTICLE_START.clone();
			particleStart.setWorld(world);
			Utils.scheduleTask(() -> spawnQuestion(1, "                      How is the run going so far?", new String[]{"Alright", "Trash", "Literally tick-perfect"}, options, particleStart, player, world), 235);
			Utils.scheduleTask(() -> oruoMessage("2 question left... then you will have proven your worth to me!"), 336);
			Utils.scheduleTask(() -> spawnQuestion(2, "Did you know that you can sub scribe to Stradivarius Violin to                         see more content like this?!", new String[]{"Oh wow, I should sub scribe!", "Oh wow, I should sub scribe!!", "Oh wow, I should sub scribe!!!"}, options, particleStart, player, world), 376);
			Utils.scheduleTask(() -> oruoMessage("One more question!"), 477);
			Utils.scheduleTask(() -> spawnQuestion(3, "                             Is akc0303 bald?", new String[]{"No", "Yes", "Decline to Answer"}, options, particleStart, player, world), 517);
			Utils.scheduleTask(() -> {
				Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Quiz Cleared");
				Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Clear Finished in 578 Ticks (28.90 seconds)");
			}, 578);
			Utils.scheduleTask(() -> oruoMessage("I bestow upon you all the power of a hundred years!"), 598);
			Utils.scheduleTask(() -> Utils.broadcastBlessing(player, Utils.BlessingType.TIME, 5), 618);
		}
	}

	public static class IceFill {
		private static BukkitTask iceFillTask;
		private static final Set<Block> frozenBlocks = new HashSet<>();

		private static void playIceFillSounds(int level, Player player) {
			switch(level) {
				case 1 -> {
					Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.189446f);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.3352f), 5);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.41436f), 10);
				}
				case 2 -> {
					Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.4987f);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.5878f), 5);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.6821f), 10);
				}
				case 3 -> {
					Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.782f);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.888f), 5);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 2.0f), 10);
				}
			}
		}

		private static void startIceFillTask(Player player) {
			if(iceFillTask != null) {
				iceFillTask.cancel();
			}

			frozenBlocks.clear();

			iceFillTask = new BukkitRunnable() {
				@Override
				public void run() {
					Block below = player.getLocation().subtract(0, 1, 0).getBlock();
					if(below.getType() == Material.ICE) {
						below.setType(Material.PACKED_ICE);
						frozenBlocks.add(below);
						Utils.playGlobalSound(Sound.BLOCK_SNOW_BREAK, 2.0f, 1.0f);
					}
				}
			}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
		}

		public static void stopIceFillTask() {
			if(iceFillTask != null) {
				iceFillTask.cancel();
				iceFillTask = null;
			}

			for(Block block : frozenBlocks) {
				if(block.getType() == Material.PACKED_ICE) {
					block.setType(Material.ICE);
				}
			}
			frozenBlocks.clear();
		}

		public static void run(Player player, World world) {
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 121);
			Utils.scheduleTask(() -> {
				startIceFillTask(player);
				Actions.rightClick(player);
			}, 122);
			Utils.scheduleTask(() -> Actions.rightClick(player), 123);
			Utils.scheduleTask(() -> Actions.rightClick(player), 124);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 125);
			Utils.scheduleTask(() -> Actions.rightClick(player), 126);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 127);
			Utils.scheduleTask(() -> Actions.rightClick(player), 128);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 129);
			Utils.scheduleTask(() -> Actions.rightClick(player), 130);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 131);
			Utils.scheduleTask(() -> {
				Actions.rightClick(player);
				playIceFillSounds(1, player);
			}, 132);
			Utils.scheduleTask(() -> Actions.rightClick(player), 133);
			Utils.scheduleTask(() -> Actions.rightClick(player), 134);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 135);
			Utils.scheduleTask(() -> Actions.rightClick(player), 136);
			Utils.scheduleTask(() -> Actions.rightClick(player), 137);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 138);
			Utils.scheduleTask(() -> Actions.rightClick(player), 139);
			Utils.scheduleTask(() -> Actions.rightClick(player), 140);
			Utils.scheduleTask(() -> Actions.rightClick(player), 141);
			Utils.scheduleTask(() -> Actions.rightClick(player), 142);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 143);
			Utils.scheduleTask(() -> Actions.rightClick(player), 144);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 145);
			Utils.scheduleTask(() -> Actions.rightClick(player), 146);
			Utils.scheduleTask(() -> Actions.rightClick(player), 147);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 148);
			Utils.scheduleTask(() -> Actions.rightClick(player), 149);
			Utils.scheduleTask(() -> Actions.rightClick(player), 150);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 151);
			Utils.scheduleTask(() -> Actions.rightClick(player), 152);
			Utils.scheduleTask(() -> Actions.rightClick(player), 153);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 154);
			Utils.scheduleTask(() -> Actions.rightClick(player), 155);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 156);
			Utils.scheduleTask(() -> Actions.rightClick(player), 157);
			Utils.scheduleTask(() -> Actions.rightClick(player), 158);
			Utils.scheduleTask(() -> Actions.rightClick(player), 159);
			Utils.scheduleTask(() -> Actions.rightClick(player), 160);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 161);
			Utils.scheduleTask(() -> Actions.rightClick(player), 162);
			Utils.scheduleTask(() -> Actions.rightClick(player), 163);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 164);
			Utils.scheduleTask(() -> {
				Actions.rightClick(player);
				playIceFillSounds(2, player);
			}, 165);
			Utils.scheduleTask(() -> Actions.rightClick(player), 166);
			Utils.scheduleTask(() -> Actions.rightClick(player), 167);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 168);
			Utils.scheduleTask(() -> Actions.rightClick(player), 169);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 170);
			Utils.scheduleTask(() -> Actions.rightClick(player), 171);
			Utils.scheduleTask(() -> Actions.rightClick(player), 172);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 173);
			Utils.scheduleTask(() -> Actions.rightClick(player), 174);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 175);
			Utils.scheduleTask(() -> Actions.rightClick(player), 176);
			Utils.scheduleTask(() -> Actions.rightClick(player), 177);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 178);
			Utils.scheduleTask(() -> Actions.rightClick(player), 179);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 180);
			Utils.scheduleTask(() -> Actions.rightClick(player), 181);
			Utils.scheduleTask(() -> Actions.rightClick(player), 182);
			Utils.scheduleTask(() -> Actions.rightClick(player), 183);
			Utils.scheduleTask(() -> Actions.rightClick(player), 184);
			Utils.scheduleTask(() -> Actions.rightClick(player), 185);
			Utils.scheduleTask(() -> Actions.rightClick(player), 186);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 187);
			Utils.scheduleTask(() -> Actions.rightClick(player), 188);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 189);
			Utils.scheduleTask(() -> Actions.rightClick(player), 190);
			Utils.scheduleTask(() -> Actions.rightClick(player), 191);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 192);
			Utils.scheduleTask(() -> Actions.rightClick(player), 193);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 194);
			Utils.scheduleTask(() -> Actions.rightClick(player), 195);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 196);
			Utils.scheduleTask(() -> Actions.rightClick(player), 197);
			Utils.scheduleTask(() -> Actions.rightClick(player), 198);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 199);
			Utils.scheduleTask(() -> Actions.rightClick(player), 200);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 201);
			Utils.scheduleTask(() -> Actions.rightClick(player), 202);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 203);
			Utils.scheduleTask(() -> Actions.rightClick(player), 204);
			Utils.scheduleTask(() -> Actions.rightClick(player), 205);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 206);
			Utils.scheduleTask(() -> Actions.rightClick(player), 207);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 208);
			Utils.scheduleTask(() -> Actions.rightClick(player), 209);
			Utils.scheduleTask(() -> Actions.rightClick(player), 210);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 211);
			Utils.scheduleTask(() -> Actions.rightClick(player), 212);
			Utils.scheduleTask(() -> Actions.rightClick(player), 213);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 214);
			Utils.scheduleTask(() -> Actions.rightClick(player), 215);
			Utils.scheduleTask(() -> Actions.rightClick(player), 216);
			Utils.scheduleTask(() -> Actions.rightClick(player), 217);
			Utils.scheduleTask(() -> Actions.rightClick(player), 218);
			Utils.scheduleTask(() -> Actions.rightClick(player), 219);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 220);
			Utils.scheduleTask(() -> Actions.rightClick(player), 221);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 222);
			Utils.scheduleTask(() -> Actions.rightClick(player), 223);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 224);
			Utils.scheduleTask(() -> Actions.rightClick(player), 225);
			Utils.scheduleTask(() -> Actions.rightClick(player), 226);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 227);
			Utils.scheduleTask(() -> {
				Actions.rightClick(player);
				playIceFillSounds(3, player);
				Server.openIceFillRewards();
				Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Ice Fill Cleared");
			}, 228);
			Utils.scheduleTask(() -> Actions.move(player, "WP", 1), 229);
			Utils.scheduleTask(() -> Actions.turnHead(player, 68f, -33f), 230);
			Utils.scheduleTask(() -> {
				Actions.leftClick(player);
				Utils.broadcastBlessing(player, Utils.BlessingType.POWER, 5);
				Utils.playSecretFoundSound(player, Utils.SecretType.BLESSING_CHEST);
			}, 248);
			Utils.scheduleTask(() -> Actions.turnHead(player, 112f, -33f), 249);
			Utils.scheduleTask(() -> {
				Actions.leftClick(player);
				Utils.broadcastBlessing(player, Utils.BlessingType.POWER, 5);
				Utils.playSecretFoundSound(player, Utils.SecretType.BLESSING_CHEST);
				Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Clear Finished in 250 Ticks (12.50 seconds)");
			}, 250);
		}
	}
}