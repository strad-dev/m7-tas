package instructions.bosses;

import instructions.Actions;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.Utils;

import java.util.Random;

@SuppressWarnings({"unused", "DataFlowIssue"})
public class WitherKing {
	private static World world;
	private static Wither witherKing;
	private static BossBar witherKingBossBar;
	private static BukkitTask bossBarUpdateTask;
	private static final EnderDragon[] dragons = new EnderDragon[5];
	private static final Random random = new Random();
	private static final String[] dragonDieMessage = {"Oh, this one hurts!", "I have more of those.", "My soul is disposable."};

	public static void witherKingInstructions(World temp) {
		world = temp;

		if(witherKing != null) {
			witherKing.remove();
		}

		for(EnderDragon dragon : dragons) {
			if(dragon != null) {
				dragon.remove();
			}
		}

		Utils.scheduleTask(WitherKing::startWitherKingSequence, 41);
	}

	public static void startWitherKingSequence() {
		for(int i = 20; i <= 101; i += 20) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_IRON_GOLEM_REPAIR, 2.0f, 0.5f), i);
		}
		for(int i = 20; i <= 261; i += 20) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f), i);
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f), i);
		}
		Utils.scheduleTask(() -> sendChatMessage("You... again?"), 100);
		Utils.scheduleTask(() -> {
			if(witherKingBossBar != null) {
				witherKingBossBar.removeAll();
				witherKingBossBar = null;
			}

			if(bossBarUpdateTask != null) {
				bossBarUpdateTask.cancel();
				bossBarUpdateTask = null;
			}

			witherKing = (Wither) world.spawnEntity(new Location(world, 54.5, 6, 32.5, 0f, 0f), EntityType.WITHER);
			witherKing.setAI(false);
			witherKing.setSilent(true);
			witherKing.setPersistent(true);
			witherKing.setRemoveWhenFarAway(false);
			witherKing.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + ChatColor.MAGIC + "Wither-King" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 5 + "/" + 5);
			witherKing.setCustomNameVisible(true);
			witherKing.getAttribute(Attribute.MAX_HEALTH).setBaseValue(5);
			witherKing.getAttribute(Attribute.ARMOR).setBaseValue(0);
			witherKing.getAttribute(Attribute.SCALE).setBaseValue(4);
			witherKing.setHealth(5);
			witherKing.addScoreboardTag("TASWither");
			Actions.setWitherArmor(witherKing, true);

			Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(witherKing, ChatColor.MAGIC + "Wither-King"), 1);

			sendChatMessage("I no longer wish to fight, but I know that will not stop you.");
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 160);
		for(int i = 200; i <= 261; i += 5) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f), i);
		}
		Utils.scheduleTask(() -> sendChatMessage("We will decide it all, here, now."), 220);
		// non-deterministic piece of shit
		// will take the lowest time seen across all testing (30/9/6/6/6)
		Utils.scheduleTask(() -> spawnDragon("purple"), 260); // tick after start: 301; dragon spawns 401
		Utils.scheduleTask(() -> spawnDragon("blue"), 260); // tick after start: 301; dragon spawns 401
		Utils.scheduleTask(() -> spawnDragon("orange"), 600); // tick after start: 641; dragon spawns 741
		Utils.scheduleTask(() -> spawnDragon("red"), 706); // tick after start: 747; dragon spawns: 847
		Utils.scheduleTask(() -> spawnDragon("green"), 812); // tick after start: 853; dragon spawns: 953
		// end happens 70 ticks after last dragon dies
	}

	public static void spawnDragon(String color) {
		for(int i = 0; i < 81; i += 20) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f), i);
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f), i);
		}
		String dragonName;
		Location spawnLocation;
		int loc;
		switch(color) {
			case "orange" -> {
				dragonName = ChatColor.GOLD + "" + ChatColor.BOLD + "Flame Dragon";
				spawnLocation = new Location(world, 86.5, 15, 56.5, 180f, 0f);
				loc = 0;
			}
			case "green" -> {
				dragonName = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Apex Dragon";
				spawnLocation = new Location(world, 26.5, 15, 94.5, 0f, 0f);
				loc = 1;
			}
			case "red" -> {
				dragonName = ChatColor.RED + "" + ChatColor.BOLD + "Power Dragon";
				spawnLocation = new Location(world, 26.5, 15, 59.5, 45f, 0f);
				loc = 2;
			}
			case "blue" -> {
				dragonName = ChatColor.BLUE + "" + ChatColor.BOLD + "Ice Dragon";
				spawnLocation = new Location(world, 85.5, 15, 94.5, 180f, 0f);
				loc = 3;
			}
			case "purple" -> {
				dragonName = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Soul Dragon";
				spawnLocation = new Location(world, 56.5, 14, 126.5, 0f, 0f);
				loc = 4;
			}
			default -> {
				dragonName = ChatColor.GRAY + "" + ChatColor.BOLD + "Unknown Dragon";
				loc = 0;
				spawnLocation = new Location(world, 54.5, 15, 76.5);
			}
		}

		Bukkit.broadcastMessage(ChatColor.YELLOW + "The " + dragonName + ChatColor.RESET + ChatColor.YELLOW + " is spawning!");

		Utils.scheduleTask(() -> {
			EnderDragon dragon = (EnderDragon) world.spawnEntity(spawnLocation, EntityType.ENDER_DRAGON);
			dragons[loc] = dragon;
			dragon.setSilent(true);
			dragon.setPersistent(true);
			dragon.setRemoveWhenFarAway(false);
			dragon.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + dragonName + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 500 + "/" + 500);
			dragon.setCustomNameVisible(true);
			dragon.getAttribute(Attribute.MAX_HEALTH).setBaseValue(500);
			dragon.getAttribute(Attribute.ARMOR).setBaseValue(0);
			dragon.setHealth(500);
			dragon.addScoreboardTag("WitherKingDragon");
			Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 1.0f);
		}, 100);
	}

	public static void playDragonDeathSound(boolean sendMessage) {
		for(int i = 0; i < 181; i += 10) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f), i);
		}
		Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f), 190);
		if(witherKing.getHealth() == 1) {
			witherKing.setHealth(0.001);
		} else {
			witherKing.setHealth(witherKing.getHealth() - 1);
		}
		if(sendMessage) {
			sendChatMessage(dragonDieMessage[random.nextInt(dragonDieMessage.length)]);
		}
	}

	public static void pickUpRelic(Player player) {
		ItemStack itemStack;
		String name;
		switch(player.getName()) {
			case "Archer" -> {
				itemStack = new ItemStack(Material.RED_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.RED + "Corrupted Red Relic");
				itemStack.setItemMeta(meta);
				name = "akc0303";
			}
			case "Berserk" -> {
				itemStack = new ItemStack(Material.ORANGE_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.GOLD + "Corrupted Orange Relic");
				itemStack.setItemMeta(meta);
				name = "AsapIcey";
			}
			case "Healer" -> {
				itemStack = new ItemStack(Material.PURPLE_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Corrupted Purple Relic");
				itemStack.setItemMeta(meta);
				name = "Meepy_";
			}
			case "Mage" -> {
				itemStack = new ItemStack(Material.BLUE_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.BLUE + "Corrupted Blue Relic");
				itemStack.setItemMeta(meta);
				name = "Beethoven_";
			}
			case "Tank" -> {
				itemStack = new ItemStack(Material.GREEN_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.DARK_GREEN + "Corrupted Green Relic");
				itemStack.setItemMeta(meta);
				name = "cookiethebald";
			}
			default -> {
				itemStack = new ItemStack(Material.BLACK_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.BLACK + "Corrupted Unknown Relic");
				itemStack.setItemMeta(meta);
				name = "Unknown";
			}
		}
		Bukkit.broadcastMessage(ChatColor.GOLD + name + ChatColor.GREEN + " picked up the " + itemStack.getItemMeta().getDisplayName() + ChatColor.GREEN + "!");
		player.getInventory().setItem(8, itemStack);
		Actions.setHotbarSlot(player, 8);
		Utils.playGlobalSound(Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);
	}

	public static void placeRelic(Player player) {
		Actions.swingHand(player);
		Utils.playGlobalSound(Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);
		player.getInventory().setItem(8, M7tas.getSkyBlockItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Menu (Click)", ""));
		Actions.setHotbarSlot(player, 8);
	}

	public static void deathSequence() {
		sendChatMessage("Incredible.  You did what I couldn't do myself.");
		Utils.scheduleTask(() -> sendChatMessage("In a way, I should thank you.  I lost all hope centuries ago that it would ever end."), 60);
		Utils.scheduleTask(() -> sendChatMessage("I hope you'll become the Heroes I could never be."), 120);
		Utils.scheduleTask(() -> sendChatMessage("So long champions of this mad world!"), 180);
		Utils.scheduleTask(() -> sendChatMessage("My strengths are depleting.  This... this is it."), 240);
		Utils.scheduleTask(() -> witherKing.remove(), 300);

		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
			Bukkit.broadcastMessage("                " + ChatColor.RED + "Master Mode The Catacombs " + ChatColor.DARK_GRAY + "- " + ChatColor.YELLOW + "Floor VII");
			Bukkit.broadcastMessage("");
			Bukkit.broadcastMessage("                           " + ChatColor.WHITE + "Team Score: " + ChatColor.GREEN + "306 " + ChatColor.WHITE + "(" + ChatColor.AQUA + ChatColor.BOLD + "S+" + ChatColor.RESET + ChatColor.WHITE + ")");
			Bukkit.broadcastMessage("  " + ChatColor.RED + "☠ " + ChatColor.YELLOW + "Defeated " + ChatColor.RED + "Maxor, Storm, Goldor, and Necron " + ChatColor.YELLOW + "in " + ChatColor.GREEN + "4 304 ticks");
			Bukkit.broadcastMessage("                         " + ChatColor.GREEN + "215.20 seconds | 3:35.20");
			Bukkit.broadcastMessage("");
			Bukkit.broadcastMessage("                              " + ChatColor.GOLD + "> " + ChatColor.YELLOW + ChatColor.BOLD + "EXTRA INFO " + ChatColor.RESET + ChatColor.GOLD + "<");
			Bukkit.broadcastMessage("                                   " + ChatColor.GREEN + ChatColor.BOLD + "SPLITS");
			Bukkit.broadcastMessage("    " + ChatColor.BLUE + ChatColor.BOLD + "Clear" + ChatColor.RESET + ChatColor.WHITE + ": 1 027 ticks | " + ChatColor.AQUA + ChatColor.BOLD + "Maxor" + ChatColor.RESET + ChatColor.WHITE + ": 499 ticks | " + ChatColor.RED + ChatColor.BOLD + "Storm" + ChatColor.RESET + ChatColor.WHITE + ": 890 ticks");
			Bukkit.broadcastMessage(" " + ChatColor.YELLOW + ChatColor.BOLD + "Terminals" + ChatColor.RESET + ChatColor.WHITE + ": 236 ticks | " + ChatColor.GOLD + ChatColor.BOLD + "Goldor" + ChatColor.RESET + ChatColor.WHITE + ": 114 ticks | " + ChatColor.DARK_RED + ChatColor.BOLD + "Necron" + ChatColor.RESET + ChatColor.WHITE + ": 609 ticks");
			Bukkit.broadcastMessage("                         " + ChatColor.GRAY + ChatColor.BOLD + "Wither King" + ChatColor.RESET + ChatColor.WHITE + ": 1 029 ticks");
			Bukkit.broadcastMessage("");
			Bukkit.broadcastMessage("     " + ChatColor.GREEN + ChatColor.BOLD + "TAS by " + ChatColor.RESET + ChatColor.AQUA + "Stradivarius Violin" + ChatColor.GREEN + ", also known as " + ChatColor.AQUA + "Beethoven_");
			Bukkit.broadcastMessage("    " + ChatColor.RED + ChatColor.BOLD + "YOUTUBE" + ChatColor.AQUA + ": https://www.youtube.com/@Stradivarius_Violin");
			Bukkit.broadcastMessage("               " + ChatColor.BLUE + ChatColor.BOLD + "DISCORD" + ChatColor.AQUA + ": https://discord.gg/gNfPwa8");
			Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
		}, 70);
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] " + ChatColor.MAGIC + "Wither-King" + ChatColor.RESET + ChatColor.RED + ": " + message);
	}
}