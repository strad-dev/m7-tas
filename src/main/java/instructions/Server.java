package instructions;

import instructions.bosses.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugin.M7tas;
import plugin.Utils;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;

public class Server {
	private static final Zombie[] archaeologists = new Zombie[10];
	private static Zombie yellowShadowAssassin = null;
	private static final LivingEntity[] trashMobs = new LivingEntity[18]; // each 1x1 has 6 mobs spawned

	public static void serverInstructions(World world, String section) {
		// Begin with 3 seconds of delay
		Bukkit.broadcastMessage("TAS starts in 3 seconds.");

		Utils.scheduleTask(() -> {
			switch(section) {
				case "all", "clear" -> {
					// 5-second countdown
					Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 5 seconds.");
					Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 4 seconds.");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 20);
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 3 seconds.");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 40);
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 2 seconds.");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 60);
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 1 seconds.");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 80);
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
						Bukkit.broadcastMessage(ChatColor.GREEN + "Run started.");
						Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
					}, 100);
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 1.0F), 100);
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Watcher.watcherInstructions(world, section.equals("all")), 101);
				}
				case "boss" -> Maxor.maxorInstructions(world, true);
				case "maxor" -> Maxor.maxorInstructions(world, false);
				case "storm" -> {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 91 165 41 95 167 40 minecraft:air");
					Storm.stormInstructions(world, false);
				}
				case "goldor" -> {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 62 136 142 58 133 142 minecraft:lever[face=wall,facing=north,powered=true]");
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 58 136 143 62 133 143 minecraft:redstone_lamp[lit=true]");
					Goldor.goldorInstructions(world, false);
				}
				case "necron" -> Necron.necronInstructions(world, false);
//				case "witherking" -> WitherKing.witherKingInstructions(world, false);
			}
		}, 60);
	}

	public static void serverSetup(World world) {
		spawnMinibosses(world);
		spawn1x1Mobs(world);
		resetGoldorCheese();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -120 69 -136 -122 72 -138 minecraft:coal_block");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -106 -120 72 -104 minecraft:red_terracotta");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 62 136 142 58 133 142 minecraft:lever[face=wall,facing=north,powered=false]");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 58 136 143 62 133 143 minecraft:redstone_lamp[lit=false]");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 43 196 38 49 175 44 43 175 62");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 43 196 38 49 175 44 97 175 62");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 91 -2 45 89 -1 46 89 131 45");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 96 -2 121 96 -1 123 96 120 121");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 70 -5 120 38 -1 99 38 59 99");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 54 64 79 minecraft:jungle_planks");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 54 64 80 minecraft:jungle_stairs");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 54 63 79 minecraft:stone_brick_slab[type=top]");
		CustomBossBar.forceCleanup();
		Watcher.forceCleanup();
		Storm.cleanupMobs();
		turnArrow(world, false);
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

		Location[] locations = { new Location(world, -120.5, 69, -184.5, 90, 0), // Red Blue (I)
				new Location(world, -216.5, 69, -184.5, 0, 0), // Spider (II)
				new Location(world, -120.5, 67, -120.5, -90, 0), // Deathmite (II)
				new Location(world, -35.5, 69, -152.5, 90, 0), // Dino Dig Site (III)
				new Location(world, -88.5, 69.0625, -215.5, 0, 0), // Catwalk (IV)
				new Location(world, -156.5, 69, -120.5, 90, 0), // Well (III)
				new Location(world, -177.5, 69, -80.5, -90, 0), // Gravel (V)
				new Location(world, -136.5, 65.0625, -40.5, -180, 0), // Museum (VI)
				new Location(world, -211.5, 69, -56.5, -90, 0), // Market (VII)
				new Location(world, -88.5, 69, -56.5, -90, 0) // Melon (VIII)
		};

		double[] healthValues = {15, 16, 16, 17, 18, 17, 19, 20, 21, 22};

		for(int i = 0; i < locations.length; i++) {
			Zombie zombie = (Zombie) world.spawnEntity(locations[i], EntityType.ZOMBIE);
			zombie.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Angry Archaeologist " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + (int) healthValues[i] + "/" + (int) healthValues[i]);
			zombie.setCustomNameVisible(true);
			zombie.setAI(false);
			zombie.setSilent(true);
			zombie.setAdult();
			zombie.setPersistent(true);
			zombie.setRemoveWhenFarAway(false);
			Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-20);
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

		yellowShadowAssassin = (Zombie) world.spawnEntity(new Location(world, -216.5, 69, -24.5, -180, 0), EntityType.ZOMBIE);
		yellowShadowAssassin.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Shadow Assassin " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + 30 + "/" + 30);
		yellowShadowAssassin.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0));
		yellowShadowAssassin.setCustomNameVisible(true);
		yellowShadowAssassin.setAI(false);
		yellowShadowAssassin.setSilent(true);
		yellowShadowAssassin.setAdult();
		yellowShadowAssassin.setPersistent(true);
		yellowShadowAssassin.setRemoveWhenFarAway(false);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.ARMOR)).setBaseValue(-3);
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

	public static void openWitherDoor() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -120 69 -136 -122 72 -138 minecraft:glass");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -120 69 -136 -122 72 -138 minecraft:air"), 20);
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		Bukkit.broadcastMessage(ChatColor.GOLD + "Beethoven_ " + ChatColor.GREEN + "opend a " + ChatColor.DARK_GRAY + ChatColor.BOLD + "WITHER " + ChatColor.RESET + ChatColor.GREEN + "door!");
	}

	public static void openBloodDoor() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -106 -120 72 -104 minecraft:glass");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -122 69 -106 -120 72 -104 minecraft:air"), 20);
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		Utils.playGlobalSound(Sound.ENTITY_GHAST_HURT, 1.0F, 0.5F);
		Bukkit.broadcastMessage(ChatColor.RED + "The " + ChatColor.BOLD + "BLOOD DOOR" + ChatColor.RESET + ChatColor.RED + " has been opened!");
		Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "A shiver runs down your spine...");
	}

	public static void activatePirateDoor() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -24 82 -98 -27 89 -99 glass");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -24 82 -98 -27 89 -99 minecraft:air"), 20);
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone -24 0 -98 -27 7 -99 -27 82 -99"), 100);
	}

	public static void activateSpiderGate() {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -212 87 -166 -212 83 -170 air replace minecraft:cobblestone_wall");
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone -212 0 -166 -212 4 -170 -212 83 -170"), 100);
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
					defense = 2;
				}
				case 2 -> {
					name = "Zombie Soldier";
					defense = 9;
					helmetColor = Color.fromRGB(0xD07F00);
					chestplateColor = Color.fromRGB(0xD07F00);
					leggingsColor = Color.fromRGB(0xD07F00);
					bootsColor = Color.fromRGB(0xD07F00);
				}
				case 3 -> {
					name = "Tank Zombie";
					defense = 9;
					helmetColor = Color.fromRGB(0xFFFFFE);
					chestplateColor = Color.fromRGB(0x828282);
					leggingsColor = Color.fromRGB(0x828282);
					bootsColor = Color.fromRGB(0xFFFFFE);
				}
				case 4 -> {
					name = "Super Tank Zombie";
					weapon = Material.STONE_SWORD;
					defense = 9;
					helmetColor = Color.fromRGB(0xE6E6E6);
					chestplateColor = Color.fromRGB(0x5A6464);
					leggingsColor = Color.fromRGB(0x5A6464);
					bootsColor = Color.fromRGB(0xE6E6E6);
				}
				case 5 -> {
					name = "Zombie Commander";
					weapon = Material.FISHING_ROD;
					defense = 9;
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
					defense = 7;
					helmetColor = Color.fromRGB(0xFFBC0B);
					chestplateColor = Color.fromRGB(0xFFBC0B);
					leggingsColor = Color.fromRGB(0xFFBC0B);
					bootsColor = Color.fromRGB(0xFFBC0B);
				}
				case 2 -> {
					name = "Skeleton Master";
					weapon = Material.BOW;
					defense = 7;
					helmetColor = Color.fromRGB(0xFF6B0B);
					chestplateColor = Color.fromRGB(0xFF6B0B);
					leggingsColor = Color.fromRGB(0xFF6B0B);
					bootsColor = Color.fromRGB(0xFF6B0B);
				}
				case 3 -> {
					name = "Skeleton Lord";
					weapon = Material.BOW;
					defense = 7;
					helmetColor = Color.fromRGB(0xFFFF55);
					chestplateColor = Color.fromRGB(0x268105);
					leggingsColor = Color.fromRGB(0x268105);
					bootsColor = Color.fromRGB(0x268105);
				}
				case 4 -> {
					name = "Skeletor Prime";
					weapon = Material.BONE;
					defense = 7;
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

		mob.setCustomName(ChatColor.RED + name + ChatColor.RESET + ChatColor.RED + " ❤ " + ChatColor.YELLOW + health + "/" + health);
		Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-defense);

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

		for (Entity entity : entities) {
			if (entity instanceof ItemFrame itemFrame) {
				// Check if this item frame is actually at the block we want
				if (entity.getLocation().getBlockX() == -2 &&
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
}