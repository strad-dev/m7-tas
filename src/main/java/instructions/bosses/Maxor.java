package instructions.bosses;

import instructions.Actions;
import instructions.Server;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.Random;

@SuppressWarnings("DataFlowIssue")
public class Maxor {
	private static Wither maxor;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;
	private static BossBar maxorBossBar;
	private static BukkitTask bossBarUpdateTask;
	private static EnderCrystal leftCrystal;
	private static EnderCrystal rightCrystal;
	private static final Random random = new Random();
	private static final String[] laserMessage = {"YOU TRICKED ME!", "THAT BEAM!  IT HURTS!  IT HURTS!"};
	private static final WitherSkeleton[] miners = new WitherSkeleton[10];

	public static void maxorInstructions(World temp, boolean doContinue) {
		world = temp;

		if(maxor != null) {
			maxor.remove();
		}

		if(maxorBossBar != null) {
			maxorBossBar.removeAll();
			maxorBossBar = null;
		}

		if(bossBarUpdateTask != null) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}

		maxor = (Wither) world.spawnEntity(new Location(world, 73.5, 226, 53.5, 0f, 0f), EntityType.WITHER);
		maxor.setAI(false);
		maxor.setSilent(true);
		maxor.setPersistent(true);
		maxor.setRemoveWhenFarAway(false);
		maxor.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Maxor" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 400 + "/" + 400);
		maxor.setCustomNameVisible(true);
		maxor.getAttribute(Attribute.MAX_HEALTH).setBaseValue(400);
		maxor.getAttribute(Attribute.ARMOR).setBaseValue(0);
		maxor.setHealth(400);
		maxor.addScoreboardTag("TASWither");
		Actions.setWitherArmor(maxor, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(maxor, "Maxor"), 1);

		spawnCrystals();

		sendChatMessage("WELL WELL WELL, LOOK WHO'S HERE!");
		Utils.scheduleTask(() -> sendChatMessage("I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU."), 60);
		Utils.scheduleTask(() -> sendChatMessage("DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE."), 120);
		Utils.scheduleTask(() -> {
			Actions.move(maxor, new Vector(0, 0, 0.5), 38);
			spawnMiners();
			Utils.playGlobalSound(Sound.ENTITY_WITHER_SPAWN);
			Utils.playGlobalSound(Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 2.0F);
		}, 160);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "The Energy Laser is charging up!");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:red_stained_glass");
		}, 190);
		Utils.scheduleTask(() -> {
			sendChatMessage(laserMessage[random.nextInt(2)]);
			Actions.setWitherArmor(maxor, false);
			CustomBossBar.spawnAnimatedStunnedIndicator(maxor, 160);
		}, 198);
		Utils.scheduleTask(() -> {
			Maxor.spawnCrystals();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
		}, 238);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "The Energy Laser is charging up!");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:red_stained_glass");
		}, 292);
		Utils.scheduleTask(() -> {
			Actions.setWitherArmor(maxor, true);
			Bukkit.broadcastMessage(ChatColor.RED + "⚠ Maxor is enraged! ⚠");
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle("", ChatColor.RED + "⚠ Maxor is enraged! ⚠", 0, 40, 0);
			}
			Actions.turnHead(maxor, 0f, 30f);
			Actions.move(maxor, new Vector(0, 0, 0.5), 4);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
		}, 358);
		Utils.scheduleTask(() -> {
			sendChatMessage(laserMessage[random.nextInt(2)]);
			Actions.setWitherArmor(maxor, false);
			CustomBossBar.spawnAnimatedStunnedIndicator(maxor, 2);
		}, 398);
		Utils.scheduleTask(() -> {
			sendChatMessage("I’M TOO YOUNG TO DIE AGAIN!");
			Server.playWitherDeathSound(maxor);
			Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor killed in 399 ticks (19.95 seconds) | Overall: 1 426 ticks (71.30 seconds)");
		}, 399);
		Utils.scheduleTask(() -> {
			Maxor.spawnCrystals();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
		}, 438);
		Utils.scheduleTask(() -> sendChatMessage("I’LL MAKE YOU REMEMBER MY DEATH!"), 459);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor finished in 499 ticks (24.95 seconds) | Overall: 1 526 ticks (76.30 seconds)");
			if(doContinue) {
				Storm.stormInstructions(world, true);
			}
		}, 499);
	}

	private static void spawnCrystals() {
		if(leftCrystal != null) {
			leftCrystal.remove();
		}

		if(rightCrystal != null) {
			rightCrystal.remove();
		}
		leftCrystal = (EnderCrystal) world.spawnEntity(new Location(world, 82.5, 238.48, 50.5), EntityType.END_CRYSTAL);
		leftCrystal.setCustomName("Energy Crystal");
		leftCrystal.setCustomNameVisible(true);
		rightCrystal = (EnderCrystal) world.spawnEntity(new Location(world, 64.5, 238.48, 50.5), EntityType.END_CRYSTAL);
		rightCrystal.setCustomName("Energy Crystal");
		rightCrystal.setCustomNameVisible(true);
	}

	public static void pickUpCrystal(Player p) {
		if(p.getName().contains("Berserk") && rightCrystal != null) {
			rightCrystal.remove();
		} else if(p.getName().contains("Mage") && leftCrystal != null) {
			leftCrystal.remove();
		}
	}

	public static void placeCrystal(Player p) {
		if(p.getName().contains("Berserk") && rightCrystal != null) {
			rightCrystal = (EnderCrystal) world.spawnEntity(new Location(world, 52.5, 224.48, 41.5), EntityType.END_CRYSTAL);
			rightCrystal.setCustomName("Energy Crystal");
			rightCrystal.setCustomNameVisible(true);
		} else if(p.getName().contains("Mage") && leftCrystal != null) {
			leftCrystal = (EnderCrystal) world.spawnEntity(new Location(world, 94.5, 224.48, 41.5), EntityType.END_CRYSTAL);
			leftCrystal.setCustomName("Energy Crystal");
			leftCrystal.setCustomNameVisible(true);
		}
		Bukkit.broadcastMessage(ChatColor.RED + "1" + ChatColor.GREEN + "/2 Energy Crystals are now active!");
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] Maxor" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}

	private static void spawnMiners() {
		// Remove any old Wither Skeletons
		for(WitherSkeleton witherSkeleton : miners) {
			if(witherSkeleton != null && witherSkeleton.isValid()) {
				witherSkeleton.remove();
			}
		}

		for(int i = 0; i < 10; i++) {
			// Random location within 3 blocks of center (73.5, 225, 73.5)
			double x = 73.5 + (random.nextDouble() * 6 - 3); // -3 to +3
			double z = 73.5 + (random.nextDouble() * 6 - 3); // -3 to +3
			Location spawnLoc = new Location(world, x, 225, z);

			WitherSkeleton miner = (WitherSkeleton) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);

			// Set health to 3 HP
			miner.getAttribute(Attribute.MAX_HEALTH).setBaseValue(3.0);
			miner.setHealth(3.0);
			miner.setAI(false);

			// Give stone pickaxe
			miner.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_PICKAXE));

			// Optional: Set custom name
			miner.setCustomName("Wither Miner " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 3 + "/" + 3);
			miner.setCustomNameVisible(true);

			// Store in array
			miners[i] = miner;
		}
	}
}
