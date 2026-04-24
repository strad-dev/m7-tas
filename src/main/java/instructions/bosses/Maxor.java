package instructions.bosses;

import listeners.CustomItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import plugin.FakePlayerInventory;
import plugin.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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

	private static final String ENERGY_CRYSTAL_ID = "skyblock/game/energy_crystal";
	private static final Map<UUID, ItemStack> previousSlot8 = new HashMap<>();

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
		maxor.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Maxor" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤" + ChatColor.YELLOW + "800M");
		maxor.setCustomNameVisible(true);
		maxor.getAttribute(Attribute.MAX_HEALTH).setBaseValue(300);
		maxor.getAttribute(Attribute.ARMOR).setBaseValue(-30);
		maxor.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(-20);
		maxor.setHealth(300);
		maxor.addScoreboardTag("TASWither");
		maxor.addScoreboardTag("TASMaxor");
		WitherActions.setWitherArmor(maxor, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(maxor, "Maxor"), 1);

		resetCrystals();

		sendChatMessage("WELL WELL WELL, LOOK WHO'S HERE!");
		Utils.scheduleTask(() -> sendChatMessage("I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU."), 60);
		Utils.scheduleTask(() -> sendChatMessage("DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE."), 120);
		Utils.scheduleTask(() -> {
//			Actions.forceMove(maxor, new Vector(0, 0, 0.5), 38);
			WitherActions.setWitherAggro(maxor, Bukkit.getPlayer("Beethoven_"));
//			spawnMiners();
			Utils.playGlobalSound(Sound.ENTITY_WITHER_SPAWN);
			Utils.playGlobalSound(Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 2.0F);
		}, 160);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.GREEN + "The Energy Laser is charging up!");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:red_stained_glass");
//		}, 190);
//		Utils.scheduleTask(() -> {
//			sendChatMessage(laserMessage[random.nextInt(2)]);
//			WitherActions.setWitherArmor(maxor, false);
//			CustomBossBar.spawnAnimatedStunnedIndicator(maxor, 160);
//		}, 198);
//		Utils.scheduleTask(() -> maxor.setHealth(124), 230);
//		Utils.scheduleTask(() -> {
//			Maxor.spawnCrystals();
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
//		}, 238);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.GREEN + "The Energy Laser is charging up!");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:red_stained_glass");
//		}, 292);
//		Utils.scheduleTask(() -> {
//			WitherActions.setWitherArmor(maxor, true);
//			Bukkit.broadcastMessage(ChatColor.RED + "⚠ Maxor is enraged! ⚠");
//			for(Player player : Bukkit.getOnlinePlayers()) {
//				player.sendTitle("", ChatColor.RED + "⚠ Maxor is enraged! ⚠", 0, 40, 0);
//			}
//			Actions.turnHead(maxor, 0f, 30f);
//			Actions.forceMove(maxor, new Vector(0, 0, 0.5), 4);
//			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
//		}, 358);
//		Utils.scheduleTask(() -> {
//			sendChatMessage(laserMessage[random.nextInt(2)]);
//			WitherActions.setWitherArmor(maxor, false);
//			CustomBossBar.spawnAnimatedStunnedIndicator(maxor, 2);
//		}, 398);
//		Utils.scheduleTask(() -> {
//			sendChatMessage("I’M TOO YOUNG TO DIE AGAIN!");
//			Server.playWitherDeathSound(maxor);
//			Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor killed in 399 ticks (19.95 seconds) | Overall: 1 426 ticks (71.30 seconds)");
//		}, 399);
//		Utils.scheduleTask(() -> {
//			Maxor.spawnCrystals();
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
//		}, 438);
//		Utils.scheduleTask(() -> sendChatMessage("I’LL MAKE YOU REMEMBER MY DEATH!"), 459);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor finished in 499 ticks (24.95 seconds) | Overall: 1 526 ticks (76.30 seconds)");
//			if(doContinue) {
//				Storm.stormInstructions(world, true);
//			}
//		}, 499);
	}

	public static ItemStack getEnergyCrystalItem() {
		return FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, ChatColor.RED + "Energy Crystal", ENERGY_CRYSTAL_ID);
	}

	public static boolean notEnergyCrystal(Entity e) {
		return !(e instanceof EnderCrystal) || (!e.equals(leftCrystal) && !e.equals(rightCrystal));
	}

	public static void resetCrystals() {
		if(world == null) world = Bukkit.getWorlds().getFirst();

		if(leftCrystal != null) {
			leftCrystal.remove();
		}
		if(rightCrystal != null) {
			rightCrystal.remove();
		}
		leftCrystal = spawnEnergyCrystal(new Location(world, 82.5, 238.48, 50.5));
		rightCrystal = spawnEnergyCrystal(new Location(world, 64.5, 238.48, 50.5));
	}

	public static void pickUp(Player p, EnderCrystal crystal) {
		if(notEnergyCrystal(crystal)) return;

		ItemStack prev = p.getInventory().getItem(8);
		previousSlot8.put(p.getUniqueId(), prev == null ? null : prev.clone());
		p.getInventory().setItem(8, getEnergyCrystalItem());

		crystal.remove();
		if(crystal.equals(leftCrystal)) leftCrystal = null;
		else if(crystal.equals(rightCrystal)) rightCrystal = null;
	}

	public static void placeAtPlate(Player p, Location plate) {
		ItemStack slot8 = p.getInventory().getItem(8);
		if(slot8 == null || !ENERGY_CRYSTAL_ID.equals(CustomItems.getID(slot8))) return;

		if(world == null) world = Bukkit.getWorlds().getFirst();
		int px = plate.getBlockX(), py = plate.getBlockY(), pz = plate.getBlockZ();

		if(px == 94 && py == 224 && pz == 41) {
			if(leftCrystal != null) return;
			leftCrystal = spawnEnergyCrystal(new Location(world, 94.5, 224.48, 41.5));
		} else if(px == 52 && py == 224 && pz == 41) {
			if(rightCrystal != null) return;
			rightCrystal = spawnEnergyCrystal(new Location(world, 52.5, 224.48, 41.5));
		} else {
			return;
		}

		ItemStack restore = previousSlot8.remove(p.getUniqueId());
		p.getInventory().setItem(8, restore != null ? restore : FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Menu (Click)", ""));

		Bukkit.broadcastMessage(ChatColor.RED + "1" + ChatColor.GREEN + "/2 Energy Crystals are now active!");
	}

	private static EnderCrystal spawnEnergyCrystal(Location loc) {
		EnderCrystal c = (EnderCrystal) world.spawnEntity(loc, EntityType.END_CRYSTAL);
		c.setCustomName("Energy Crystal");
		c.setCustomNameVisible(true);
		return c;
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
			miner.getAttribute(Attribute.ARMOR).setBaseValue(-30);
			miner.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(-20);
			miner.setAI(false);

			// Give stone pickaxe
			miner.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_PICKAXE));

			// Optional: Set custom name
			miner.setCustomName("Wither Miner " + ChatColor.RED + "❤" + ChatColor.YELLOW + "6M");
			miner.setCustomNameVisible(true);

			// Store in array
			miners[i] = miner;
		}
	}
}
