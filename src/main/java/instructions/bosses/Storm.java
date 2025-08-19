package instructions.bosses;

import instructions.Actions;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftWitherSkeleton;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.Objects;
import java.util.Random;

@SuppressWarnings("DataFlowIssue")
public class Storm {
	private static Wither storm;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;
	private static BossBar stormBossBar;
	private static BukkitTask bossBarUpdateTask;
	private static final Random random = new Random();
	private static final String[] lightningMessage = {"ENERGY HEED MY CALL!", "THUNDER LET ME BE YOUR CATALYST!"};
	private static final String[] crushedMessage = {"Ouch, that hurt!", "Oof"};
	private static final String[] enrageMessage = {"THAT WAS ONLY IN MY WAY!", "Slowing me down will be your greatest accomplishment!", "This factory is too small for me!", "BEGONE PILLAR!"};
	private static final WitherSkeleton[] witherGuards = new WitherSkeleton[44];
	private static final WitherSkeleton[] witherMiners = new WitherSkeleton[200];
	private static final Zombie[] shadowAssassins = new Zombie[4];

	public static void stormInstructions(World temp, boolean doContinue) {
		world = temp;

		if(storm != null) {
			storm.remove();
		}

		if(stormBossBar != null) {
			stormBossBar.removeAll();
			stormBossBar = null;
		}

		if(bossBarUpdateTask != null) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}

		storm = (Wither) world.spawnEntity(new Location(world, 102.5, 182, 53.5, 90f, 0f), EntityType.WITHER);
		storm.setAI(false);
		storm.setSilent(true);
		storm.setPersistent(true);
		storm.setRemoveWhenFarAway(false);
		storm.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Storm" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 500 + "/" + 500);
		storm.setCustomNameVisible(true);
		storm.getAttribute(Attribute.MAX_HEALTH).setBaseValue(500);
		storm.getAttribute(Attribute.ARMOR).setBaseValue(0);
		storm.setHealth(500);
		storm.addScoreboardTag("TASWither");
		Actions.setWitherArmor(storm, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(storm, "Storm"), 1);

		initialMovement();
		spawnMobs();

		sendChatMessage("Pathetic Maxor, just like expected.");
		Utils.scheduleTask(() -> sendChatMessage("Don't boast about beating this simple-minded Wither."), 60);
		Utils.scheduleTask(() -> sendChatMessage("My abilities are unparalleled, in may ways I am the last bastion."), 120);
		Utils.scheduleTask(() -> sendChatMessage("The memory of your death will be your fondest, focus up!"), 180);
		Utils.scheduleTask(() -> {
			sendChatMessage("The power of lightning is quite phenomenal.  A single strike can vaporize a person whole.");
			Actions.turnHead(storm, 90f, 0f);
		}, 400);
		Utils.scheduleTask(() -> {
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle(ChatColor.DARK_RED + "4", "", 0, 25, 0);
			}
		}, 440);
		Utils.scheduleTask(() -> sendChatMessage("I'd be happy to show you what that's like!"), 460);
		Utils.scheduleTask(() -> {
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle(ChatColor.DARK_RED + "3", "", 0, 25, 0);
			}
		}, 465);
		Utils.scheduleTask(() -> {
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle(ChatColor.DARK_RED + "2", "", 0, 25, 0);
			}
		}, 490);
		Utils.scheduleTask(() -> {
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle(ChatColor.DARK_RED + "1", "", 0, 25, 0);
			}
		}, 515);
		Utils.scheduleTask(() -> sendChatMessage(lightningMessage[random.nextInt(lightningMessage.length)]), 525);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 2.0F);
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0F, 0.6F);
		}, 535);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 2.0F);
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0F, 0.6F);
		}, 545);
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] Storm" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}

	private static void initialMovement() {
		Actions.turnHead(storm, 45f, 0f);
		Actions.move(storm, new Vector(-0.29, 0, 0.29), 100);
		Utils.scheduleTask(() -> {
			Actions.turnHead(storm, 135f, 0f);
			Actions.move(storm, new Vector(-0.29, 0, -0.29), 100);
		}, 100);
		Utils.scheduleTask(() -> {
			Actions.turnHead(storm, -135f, 0f);
			Actions.move(storm, new Vector(0.29, 0, -0.29), 100);
		}, 200);
		Utils.scheduleTask(() -> {
			Actions.turnHead(storm, -45f, 0f);
			Actions.move(storm, new Vector(0.29, 0, 0.29), 100);
		}, 300);
	}

	private static void spawnMobs() {
		Location[] shadowAssassinLocations = {new Location(world, 26.5, 170, 100.5), new Location(world, 26.5, 170, 6.5), new Location(world, 120.5, 170, 6.5), new Location(world, 120.5, 170, 100.5)};
		Location[] sentryLocations = {new Location(world, 114.5, 175, 35.5), new Location(world, 114.5, 175, 45.5), new Location(world, 114.5, 175, 61.5), new Location(world, 114.5, 175, 71.5), new Location(world, 86.5, 175, 35.5), new Location(world, 86.5, 175, 45.5), new Location(world, 86.5, 175, 61.5), new Location(world, 86.5, 175, 71.5), new Location(world, 60.5, 175, 35.5), new Location(world, 60.5, 175, 45.5), new Location(world, 60.5, 175, 61.5), new Location(world, 60.5, 175, 71.5), new Location(world, 32.5, 175, 35.5), new Location(world, 32.5, 175, 45.5), new Location(world, 32.5, 175, 61.5), new Location(world, 32.5, 175, 71.5), new Location(world, 79.5, 170, 104.5), new Location(world, 77.5, 170, 103.5), new Location(world, 75.5, 170, 103.5), new Location(world, 73.5, 170, 103.5), new Location(world, 71.5, 170, 103.5), new Location(world, 69.5, 170, 103.5), new Location(world, 67.5, 170, 104.5), new Location(world, 22.5, 172, 59.5), new Location(world, 23.5, 172, 57.5), new Location(world, 23.5, 172, 55.5), new Location(world, 23.5, 172, 53.5), new Location(world, 23.5, 172, 51.5), new Location(world, 23.5, 172, 49.5), new Location(world, 22.5, 172, 47.5), new Location(world, 67.5, 170, 2.5), new Location(world, 69.5, 170, 3.5), new Location(world, 71.5, 170, 3.5), new Location(world, 73.5, 170, 3.5), new Location(world, 75.5, 170, 3.5), new Location(world, 77.5, 170, 3.5), new Location(world, 79.5, 170, 2.5), new Location(world, 124.5, 172, 47.5), new Location(world, 123.5, 172, 49.5), new Location(world, 123.5, 172, 51.5), new Location(world, 123.5, 172, 53.5), new Location(world, 123.5, 172, 55.5), new Location(world, 123.5, 172, 57.5), new Location(world, 124.5, 172, 59.5)};
		Location[] minerCenters = {new Location(world, 114.5, 170, 94.5), new Location(world, 80.5, 163, 96.5), new Location(world, 66.5, 163, 96.5), new Location(world, 32.5, 170, 94.5), new Location(world, 28.5, 165, 59.5), new Location(world, 28.5, 165, 47.5), new Location(world, 32.5, 170, 12.5), new Location(world, 66.5, 163, 10.5), new Location(world, 80.5, 163, 10.5), new Location(world, 114.5, 170, 12.5), new Location(world, 118.5, 165, 47.5), new Location(world, 118.5, 165, 59.5), new Location(world, 100.5, 169, 57.5), new Location(world, 100.5, 169, 49.5), new Location(world, 79.5, 165, 59.5), new Location(world, 79.5, 165, 47.5), new Location(world, 67.5, 165, 47.5), new Location(world, 67.5, 165, 59.5), new Location(world, 46.5, 169, 57.5), new Location(world, 46.5, 169, 49.5)};

		cleanupMobs();

		for(Location l : minerCenters) {
			int index = 0;
			for(int i = 0; i < 10; i ++) {
				// Random location within 3 blocks of center (73.5, 225, 73.5)
				double x = l.getX() + (random.nextDouble() * 10 - 5); // -3 to +3
				double z = l.getZ() + (random.nextDouble() * 10 - 5); // -3 to +3
				Location spawnLoc = new Location(world, x, l.getY(), z);

				WitherSkeleton miner = (WitherSkeleton) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);

				// Set health to 3 HP
				miner.getAttribute(Attribute.MAX_HEALTH).setBaseValue(3.0);
				miner.setHealth(3.0);

				// Give stone pickaxe
				miner.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_PICKAXE));

				// Optional: Set custom name
				miner.setCustomName("Wither Miner " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 3 + "/" + 3);
				miner.setCustomNameVisible(true);

				Location targetLoc = new Location(world, 73.5, spawnLoc.getY(), 53.5);
				Vector direction = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();
				float yaw = (float) (Math.atan2(-direction.getX(), direction.getZ()) * 180.0 / Math.PI);
				float pitch = (float) (Math.asin(-direction.getY()) * 180.0 / Math.PI);

				Location facingLoc = spawnLoc.clone();
				facingLoc.setYaw(yaw);
				facingLoc.setPitch(pitch);
				miner.teleport(facingLoc);

				net.minecraft.world.entity.monster.WitherSkeleton nmsWitherSkeleton = (net.minecraft.world.entity.monster.WitherSkeleton) ((CraftWitherSkeleton) miner).getHandle();

				// This single line makes the skeleton raise its arms
				nmsWitherSkeleton.setAggressive(true);

				// Store in array
				witherMiners[index] = miner;
				index ++;
			}
		}

		for(int i = 0; i < sentryLocations.length; i++) {
			WitherSkeleton sentry = (WitherSkeleton) world.spawnEntity(sentryLocations[i], EntityType.WITHER_SKELETON);

			// Set health to 3 HP
			sentry.getAttribute(Attribute.MAX_HEALTH).setBaseValue(3.0);
			sentry.setHealth(3.0);
			sentry.setAI(false);

			// Give stone pickaxe
			sentry.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));

			// Optional: Set custom name
			sentry.setCustomName("Wither Guard " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 3 + "/" + 3);
			sentry.setCustomNameVisible(true);

			Location targetLoc = new Location(world, 73.5, sentryLocations[i].getY(), 53.5);
			Vector direction = targetLoc.toVector().subtract( sentryLocations[i].toVector()).normalize();
			float yaw = (float) (Math.atan2(-direction.getX(), direction.getZ()) * 180.0 / Math.PI);
			float pitch = (float) (Math.asin(-direction.getY()) * 180.0 / Math.PI);

			Location facingLoc =  sentryLocations[i].clone();
			facingLoc.setYaw(yaw);
			facingLoc.setPitch(pitch);
			sentry.teleport(facingLoc);

			net.minecraft.world.entity.monster.WitherSkeleton nmsWitherSkeleton = (net.minecraft.world.entity.monster.WitherSkeleton) ((CraftWitherSkeleton) sentry).getHandle();

			// This single line makes the skeleton raise its arms
			nmsWitherSkeleton.setAggressive(true);

			// Store in array
			witherGuards[i] = sentry;
		}

		for(int i = 0; i < shadowAssassinLocations.length; i ++) {
			Zombie shadowAssassin = (Zombie) world.spawnEntity(shadowAssassinLocations[i], EntityType.ZOMBIE);
			shadowAssassin.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Shadow Assassin " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + 15 + "/" + 15);
			shadowAssassin.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0));
			shadowAssassin.setCustomNameVisible(true);
			shadowAssassin.setAI(false);
			shadowAssassin.setSilent(true);
			shadowAssassin.setAdult();
			shadowAssassin.setPersistent(true);
			shadowAssassin.setRemoveWhenFarAway(false);
			Objects.requireNonNull(shadowAssassin.getAttribute(Attribute.ARMOR)).setBaseValue(-3);
			Objects.requireNonNull(shadowAssassin.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(15);
			shadowAssassin.setHealth(15);

			ItemStack boots = Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.PURPLE, ChatColor.LIGHT_PURPLE + "Shadow Assassin Boots");
			assert shadowAssassin.getEquipment() != null;
			shadowAssassin.getEquipment().setBoots(boots);
			shadowAssassin.getEquipment().setLeggings(new ItemStack(Material.AIR));
			shadowAssassin.getEquipment().setChestplate(new ItemStack(Material.AIR));
			shadowAssassin.getEquipment().setHelmet(new ItemStack(Material.AIR));
			shadowAssassin.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));

			shadowAssassins[i] = shadowAssassin;
		}
	}

	public static void cleanupMobs() {
		for(WitherSkeleton witherSkeleton : witherGuards) {
			if(witherSkeleton != null && witherSkeleton.isValid()) {
				witherSkeleton.remove();
			}
		}

		for(WitherSkeleton witherSkeleton : witherMiners) {
			if(witherSkeleton != null && witherSkeleton.isValid()) {
				witherSkeleton.remove();
			}
		}

		for(Zombie zombie: shadowAssassins) {
			if(zombie != null && zombie.isValid()) {
				zombie.remove();
			}
		}
	}

	private static final int CLONE_DELAY = 4;
	private static final int AIR_START_TICK = 48;
	private static final int AIR_DELAY = 4;
	private static final int CLONE_RESUME_TICK = 148;

	// Pad configurations
	private static final PadConfig PURPLE_PAD = new PadConfig(103, 97, 68, 62);
	private static final PadConfig YELLOW_PAD = new PadConfig(49, 43, 68, 62);

	private record PadConfig(int x1, int x2, int z1, int z2) {
	}

	public static void prepadPurple() {
		prepadSequence(PURPLE_PAD);
	}

	public static void prepadYellow() {
		prepadSequence(YELLOW_PAD);
	}

	public static void crushPurple() {
		crushSequence(PURPLE_PAD);
	}

	public static void crushYellow() {
		crushSequence(YELLOW_PAD);
	}

	private static void prepadSequence(PadConfig pad) {
		// Phase 1: Clone downwards (175 -> 170)
		for (int i = 0; i < 6; i++) {
			final int sourceY = 175 - i;
			final int targetY = 174 - i;
			scheduleCommand(i * CLONE_DELAY,
					String.format("clone %d %d %d %d %d %d %d %d %d",
							pad.x1, sourceY, pad.z1,
							pad.x2, sourceY, pad.z2,
							pad.x2, targetY, pad.z2));
		}

		// Phase 2: Clear with air (169 -> 188)
		int tick = AIR_START_TICK;
		for (int y = 169; y <= 188; y++) {
			scheduleCommand(tick,
					String.format("fill %d %d %d %d %d %d minecraft:air",
							pad.x1, y, pad.z1,
							pad.x2, y, pad.z2));
			tick += AIR_DELAY;
		}

		// Phase 3: Clone downwards (189 -> 182)
		tick = CLONE_RESUME_TICK;
		for (int i = 0; i < 8; i++) {
			final int sourceY = 189 - i;
			final int targetY = 188 - i;
			scheduleCommand(tick,
					String.format("clone %d %d %d %d %d %d %d %d %d",
							pad.x1, sourceY, pad.z1,
							pad.x2, sourceY, pad.z2,
							pad.x2, targetY, pad.z2));
			tick += CLONE_DELAY;
		}
	}

	private static void crushSequence(PadConfig pad) {
		// Clone downwards (181 -> 176)
		for (int i = 0; i < 5; i++) {
			final int sourceY = 181 - i;
			final int targetY = 180 - i;
			scheduleCommand(i * CLONE_DELAY,
					String.format("clone %d %d %d %d %d %d %d %d %d",
							pad.x1, sourceY, pad.z1,
							pad.x2, sourceY, pad.z2,
							pad.x2, targetY, pad.z2));
		}
	}

	private static void scheduleCommand(int delay, String command) {
		Utils.scheduleTask(() -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			playPistonSound();
		}, delay);
	}

	private static void playPistonSound() {
		Utils.playGlobalSound(Sound.BLOCK_PISTON_CONTRACT, 2.0f, 1.0f);
	}
}