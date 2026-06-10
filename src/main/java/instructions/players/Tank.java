package instructions.players;

import instructions.Actions;
import org.bukkit.*;
import org.bukkit.entity.Player;
import plugin.Utils;

public class Tank {
	private static Player tank;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;

	// Tank --> Mage2
	public static void tankInstructions(Player p, String section) {
		tank = p;
		world = Tank.tank.getWorld();
		tank.setGameMode(GameMode.SURVIVAL);
		tank.setFlying(false);

		switch(section) {
			case "all", "clear" -> {
				Utils.teleport(tank, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f));
				Utils.scheduleTask(() -> preclear(section.equals("all")), 60);
			}
			case "maxor", "boss" -> {
				Utils.teleport(tank, new Location(world, 73.5, 221, 14.5, 0f, 0f));
				Actions.swapItems(tank, 1, 28);
				Actions.swapItems(tank, 3, 30);
				Actions.swapItems(tank, 6, 33);
				Actions.swapItems(tank, 7, 34);
				Actions.setHotbarSlot(tank, 3);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
			case "storm" -> {
				Utils.teleport(tank, new Location(world, 46.794, 169, 53.835, -90f, 0f));
				Actions.swapItems(tank, 1, 28);
				Actions.swapItems(tank, 3, 30);
				Actions.swapItems(tank, 6, 33);
				Actions.swapItems(tank, 7, 34);
				Utils.scheduleTask(() -> Actions.swapItems(tank, 7, 33), 1);
				Utils.scheduleTask(() -> storm(false), 60);
			}
			case "goldor" -> {
				Utils.setSpeed(tank, 650);
				Utils.teleport(tank, new Location(world, -0.240, 120, 76.775, 72f, -25f));
				Actions.swapItems(tank, 1, 28);
				Actions.swapItems(tank, 3, 30);
				Actions.swapItems(tank, 6, 33);
				Actions.swapItems(tank, 7, 34);
				Actions.swapItems(tank, 12, 39);
				Actions.setHotbarSlot(tank, 4);
				Utils.scheduleTask(() -> goldor(false), 60);
			}
//			case "necron" -> {
//				Utils.teleport(tank, new Location(world, 54.529, 65, 83.688, 180f, -5f));
//				Actions.swapItems(tank, 1, 28);
//				Actions.swapItems(tank, 3, 30);
//				Actions.swapItems(tank, 5, 32);
//				Actions.swapItems(tank, 6, 33);
//				Utils.scheduleTask(() -> Actions.swapItems(tank, 7, 33), 1);
//				Utils.scheduleTask(() -> necron(false), 60);
//			}
//			case "witherking" -> {
//				Utils.teleport(tank, new Location(world, 22.3, 6, 94.452, 67.3f, 29.7f));
//				Actions.swapItems(tank, 1, 28);
//				Actions.swapItems(tank, 3, 30);
//				Actions.swapItems(tank, 5, 32);
//				Actions.swapItems(tank, 6, 33);
//				Utils.scheduleTask(() -> Actions.swapItems(tank, 7, 33), 1);
//				Utils.scheduleTask(Tank::witherKing, 60);
//			}
		}
	}

	private static void preclear(boolean doContinue) {
		Actions.setHotbarSlot(tank, 3);
		Actions.move(tank, "WPJ", 12);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 25); // left click mort to start the run
		// tick 26: click start button
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 3), 27); // run up to door
		Utils.scheduleTask(() -> clear(doContinue), 126);
	}

	private static void clear(boolean doContinue) {
		/*
		 * ██████╗ ██╗      ██████╗  ██████╗ ██████╗     ██████╗ ██╗   ██╗███████╗██╗  ██╗
		 * ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔══██╗██║   ██║██╔════╝██║  ██║
		 * ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██████╔╝██║   ██║███████╗███████║
		 * ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██╔══██╗██║   ██║╚════██║██╔══██║
		 * ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ██║  ██║╚██████╔╝███████║██║  ██║
		 * ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝     ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue Cleared");
		}, 21); // kill miniboss instantly
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 0f, 3.98f);
			Actions.move(tank, "N", 2);
			Actions.setHotbarSlot(tank, 1);
		}, 22);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 23); // etherwarp to miniboss death location
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(tank, 2);
			Utils.broadcastBlessing(tank, Utils.BlessingType.POWER, 5);
			Utils.playLocalSound(tank, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
			Bukkit.broadcastMessage(ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] cookiethebald " + ChatColor.GREEN + "has obtained " + ChatColor.DARK_GRAY + "Wither Key" + ChatColor.GREEN + "!");
		}, 24);
		Utils.scheduleTask(() -> Actions.leap(tank, Archer.get()), 25);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 0f, 6f);
			Actions.setHotbarSlot(tank, 3);
			Actions.swapItems(tank, 4, 31);
		}, 26);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Deathmite Cleared");
		}, 46); // kill miniboss instantly
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(tank, 1);
			Actions.turnHead(tank, 0f, 15f);
		}, 47);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 48);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 49); // aotv to pick up blessing | reposition
		Utils.scheduleTask(() -> {
			Utils.broadcastBlessing(tank, Utils.BlessingType.POWER, 5);
			Utils.playLocalSound(tank, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
			Bukkit.broadcastMessage(ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] cookiethebald " + ChatColor.GREEN + "has obtained " + ChatColor.RED + "Blood Key" + ChatColor.GREEN + "!");
			Actions.turnHead(tank, 94f, -6f);
			Actions.move(tank, "N", 0);
		}, 50);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 51); // etherwarp into museum
		// Blood Rush: 51 ticks

		/*
		 * ███╗   ███╗██╗   ██╗███████╗███████╗██╗   ██╗███╗   ███╗
		 * ████╗ ████║██║   ██║██╔════╝██╔════╝██║   ██║████╗ ████║
		 * ██╔████╔██║██║   ██║███████╗█████╗  ██║   ██║██╔████╔██║
		 * ██║╚██╔╝██║██║   ██║╚════██║██╔══╝  ██║   ██║██║╚██╔╝██║
		 * ██║ ╚═╝ ██║╚██████╔╝███████║███████╗╚██████╔╝██║ ╚═╝ ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚══════╝ ╚═════╝ ╚═╝     ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(tank, 155f, 10f), 52);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 53); // etherwarp into position
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 0f, 13f);
			Actions.setHotbarSlot(tank, 3);
		}, 54);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Museum Cleared");
			Utils.scheduleTask(() -> {
				Utils.broadcastBlessing(tank, Utils.BlessingType.WISDOM, 5);
				Utils.playLocalSound(tank, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
			}, 200);
		}, 55);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 180f, 1.4f);
			Actions.setHotbarSlot(tank, 1);
		}, 56);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 57); // etherwarp to secret
		Utils.scheduleTask(() -> Actions.dropItem(tank, true), 58); // blow up wall | cooldown for 255 ticks
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Museum 1/5 (Opened Chest)");
			Utils.broadcastBlessing(tank, Utils.BlessingType.STONE, 2);
			Utils.playSecretFoundSound(tank, Utils.SecretType.BLESSING_CHEST);
		}, 59);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 45f, -43f), 60);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 61); // etherwarp up
		Utils.scheduleTask(() -> Actions.turnHead(tank, 110f, -60f), 62);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 63); // etherwarp to wither essence
		Utils.scheduleTask(() -> Actions.turnHead(tank, -147f, 20f), 64);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Museum 2/5 (Obtained Wither Essence)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.ESSENCE);
		}, 65);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -26f, 64f), 66);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 67); // etherwarp down
		Utils.scheduleTask(() -> Actions.turnHead(tank, -10f, 2.5f), 68);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 69); // etherwarp across
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 1f, 15f);
			Actions.setHotbarSlot(tank, 5);
		}, 70);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 71);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 72); // stonk wall | you cannot etherwarp through the gap
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 1), 73);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 74); // etherwarp in
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 0f), 75);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Museum 3/5 (Opened Chest)");
			Utils.broadcastBlessing(tank, Utils.BlessingType.STONE, 2);
			Utils.playSecretFoundSound(tank, Utils.SecretType.BLESSING_CHEST);
		}, 76);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 72.5f);
			Actions.move(tank, "WP", 1);
			Actions.setHotbarSlot(tank, 5);
		}, 77);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 78);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 79); // stonk floor
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 90f);
			Actions.move(tank, "N", 0);
			Actions.setHotbarSlot(tank, 1);
		}, 80);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 81); // etherwarp to floor
		Utils.scheduleTask(() -> Actions.turnHead(tank, -66f, 0f), 82);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Museum 4/5 (Opened Chest)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.CHEST);
		}, 83);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 75f, 0f), 84);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 85); // etherwarp to wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 90f, 60f);
			Actions.move(tank, "WP", 2);
			Actions.setHotbarSlot(tank, 5);
		}, 86);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 87); // break wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 30f, 19f);
			Actions.setHotbarSlot(tank, 7);
		}, 88);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 89); // throw pearl towards market | lands in 7 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 66f, 66f);
			Actions.setHotbarSlot(tank, 4);
		}, 90);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 91); // blow up floor
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 41f, 51f);
			Actions.move(tank, "N", 0);
			Actions.setHotbarSlot(tank, 1);
		}, 92);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 93); // etherwarp down
		Utils.scheduleTask(() -> Actions.turnHead(tank, 165f, 0f), 94);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Museum 5/5 (Opened Chest)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.CHEST);
		}, 95);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 27f, 7f), 96); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(tank), 97); // etherwarp into market
		// Museum: 46 ticks

		/*
		 * ███╗   ███╗ █████╗ ██████╗ ██╗  ██╗███████╗████████╗
		 * ████╗ ████║██╔══██╗██╔══██╗██║ ██╔╝██╔════╝╚══██╔══╝
		 * ██╔████╔██║███████║██████╔╝█████╔╝ █████╗     ██║
		 * ██║╚██╔╝██║██╔══██║██╔══██╗██╔═██╗ ██╔══╝     ██║
		 * ██║ ╚═╝ ██║██║  ██║██║  ██║██║  ██╗███████╗   ██║
		 * ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝   ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -10f, 0f);
			Actions.setHotbarSlot(tank, 7);
		}, 98);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 99); // throw pearl to reposition | lands in 9 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 54.2f, 63.9f);
			Actions.setHotbarSlot(tank, 4);
		}, 100);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 101); // blow up floor
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 1), 102);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 103); // etherwarp down
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -72f, 5f);
			Actions.setHotbarSlot(tank, 0);
		}, 104);
		Utils.scheduleTask(() -> {
			Actions.rightClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Market 1/5 (Killed Bat)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.BAT);
		}, 105);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 180f, 20f);
			Actions.setHotbarSlot(tank, 7);
		}, 106);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Market: 2/5 (Obtain Wither Essence)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.ESSENCE);
		}, 107);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -12f, -3.5f), 108);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 109); // throw pearl | lands in 10 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 53f, -78.5f);
			Actions.setHotbarSlot(tank, 1);
		}, 110);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 111); // etherwarp up
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 90f, 35f);
			Actions.move(tank, "WP", 5);
			Actions.setHotbarSlot(tank, 5);
		}, 112);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 113);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 114);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 115);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 116); // break through wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 124f, 16f);
			Actions.setHotbarSlot(tank, 7);
		}, 117);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Market 3/5 (Opened Chest)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.CHEST);
		}, 118);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 15f, 6f), 119); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(tank), 120); // throw pearl to reposition | lands in 9 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 73f);
			Actions.setHotbarSlot(tank, 1);
		}, 121);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 122); // aotv down
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 90f);
			Actions.setHotbarSlot(tank, 5);
		}, 123);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 124); // break block
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 1), 125);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 126); // aotv down
		Utils.scheduleTask(() -> Actions.turnHead(tank, 66f, -8f), 127);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Market 4/5 (Opened Chest)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.CHEST);
		}, 128);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -45f, 0f);
			Actions.setHotbarSlot(tank, 3);
		}, 129); // pearl lands
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Market Cleared");
		}, 130); // kill miniboss
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -10f, 4f);
			Actions.setHotbarSlot(tank, 4);
			Utils.playLocalSound(tank, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
			Utils.broadcastBlessing(tank, Utils.BlessingType.STONE, 5);
		}, 131);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 132); // blow up crypt
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 3), 133);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Crypt 4/5");
		}, 134);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -45f, -70f);
			Actions.setHotbarSlot(tank, 1);
			Actions.move(tank, "N", 13);
		}, 135);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 136); // etherwarp up
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 20f, -46f);
			Actions.setHotbarSlot(tank, 5);
		}, 137);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 138); // break block
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(tank, 1);
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Market 5/5 (Opened Chest)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.CHEST);
		}, 139);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -71.5f, 23f), 140);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 141); // etherwarp into hallway
		// Market: 44 ticks

		/*
		 * ██╗  ██╗ █████╗ ██╗     ██╗     ██╗    ██╗ █████╗ ██╗   ██╗
		 * ██║  ██║██╔══██╗██║     ██║     ██║    ██║██╔══██╗╚██╗ ██╔╝
		 * ███████║███████║██║     ██║     ██║ █╗ ██║███████║ ╚████╔╝
		 * ██╔══██║██╔══██║██║     ██║     ██║███╗██║██╔══██║  ╚██╔╝
		 * ██║  ██║██║  ██║███████╗███████╗╚███╔███╔╝██║  ██║   ██║
		 * ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚══════╝ ╚══╝╚══╝ ╚═╝  ╚═╝   ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(tank, -97f, 0f), 142);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 143); // etherwarp towards miniboss
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 9f);
			Actions.setHotbarSlot(tank, 0);
		}, 144);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Hallway Cleared");
			Utils.scheduleTask(() -> {
				Utils.playLocalSound(tank, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
				Utils.broadcastBlessing(tank, Utils.BlessingType.LIFE, 5);
			}, 200);
		}, 145);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -99f, 8f);
			Actions.setHotbarSlot(tank, 1);
		}, 146);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 147); // etherwarp towards chest
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -57f, 6f);
			Actions.setHotbarSlot(tank, 7);
		}, 148);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 149); // throw pearl for reposiiton | lands in 6 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -180f, 40f);
			Actions.setHotbarSlot(tank, 5);
			Actions.move(tank, "WP", 4);
		}, 150);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 151);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 152); // stonk iron bars
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 1), 153); // move for 2 more ticks
		Utils.scheduleTask(() -> Actions.turnHead(tank, 166f, 15f), 154);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Hallway 1/3 (Opened Chest)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.CHEST);
		}, 155);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -122.5f, 2f);
			Actions.move(tank, "N", 2);
		}, 156); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(tank), 157); // etherwarp to next secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 30f);
			Actions.setHotbarSlot(tank, 5);
		}, 158);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Actions.move(tank, "WP", 4);
		}, 159);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 160);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 161);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 162);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -150f, 43f);
			Actions.setHotbarSlot(tank, 1);
		}, 163);
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Hallway 2/3 (Opened Chest)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.CHEST);
		}, 164);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -75f, -55f);
			 Actions.move(tank, "N", 0);
		}, 165);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 166); // etherwarp up
		Utils.scheduleTask(() -> Actions.turnHead(tank, -104.5f, -5f), 167);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 168); // etherwarp to next secret
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, -81f), 169);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 170); // etherwarp up
		Utils.scheduleTask(() -> Actions.turnHead(tank, -22.5f, 17f), 171);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 172); // etherwarp to secret
		Utils.scheduleTask(() -> {
			Actions.leftClick(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Hallway 3/3 (Opened Chest)");
			Utils.playSecretFoundSound(tank, Utils.SecretType.CHEST);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Clear Finished in 173 ticks (8.65 seconds)");
		}, 173);
		// Hallway: 32 ticks

		Utils.scheduleTask(() -> {
			Actions.swapItems(tank, 1, 28);
			Actions.swapItems(tank, 3, 30);
			Actions.swapItems(tank, 6, 33);
			Actions.swapItems(tank, 7, 34);
			Actions.setHotbarSlot(tank, 3);
		}, 179);
		Utils.scheduleTask(() -> {
			if(doContinue) {
				Utils.teleport(tank, new Location(world, 73.5, 221, 14.5));
				maxor(true);
			}
		}, 742);
	}

	public static void maxor(boolean doContinue) {
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 0), 1); // move to aggro spot
		Utils.scheduleTask(() -> Actions.move(tank, "WPJ", 0), 50);
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 0), 52);
		Utils.scheduleTask(() -> {
			Actions.move(tank, "N", 10);
			Actions.turnHead(tank, 180f, -5f);
		}, 63);
		for(int i = 65; i <= 160; i += 5) {
			Utils.scheduleTask(() -> Actions.leftClick(tank), i);
		}
		Utils.scheduleTask(() -> Actions.rightClick(tank), 161); // Wither Impact has a 3-tick cooldown
		Utils.scheduleTask(() -> Actions.rightClick(tank), 164);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 167);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 170); // clear out wither miners
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 6), 171);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 180f, -20f);
			Actions.rightClick(tank);
		}, 172);
		Utils.scheduleTask(() -> Actions.stopRightClick(tank), 197);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 199);
		Utils.scheduleTask(() -> Actions.stopRightClick(tank), 204);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 205);
		Utils.scheduleTask(() -> Actions.stopRightClick(tank), 211); // debuff
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 3), 212);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 355); // hit 1, avoid insta-enrage
		Utils.scheduleTask(() -> Actions.leftClick(tank), 397); // hit 2 to kill
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 4), 398);
		Utils.scheduleTask(() -> Actions.leap(tank, Healer.get()), 399);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 156f, 0f);
			Actions.setHotbarSlot(tank, 0);
		}, 400);
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 12), 401);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 0f), 413);
		if(doContinue) {
			Utils.scheduleTask(() -> storm(true), 497);
		}
	}

	public static void storm(boolean doContinue) {
		for(int i = 0; i <= 10; i += 3) {
			Utils.scheduleTask(() -> Actions.rightClick(tank), i);
		} // clear platform
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 3), 11);
		for(int i = 15; i <= 530; i += 5) {
			Utils.scheduleTask(() -> Actions.snapHeadToNearestEnemy(tank), i - 1);
			Utils.scheduleTask(() -> Actions.loopLeftClick(tank), i);
		} // kill outstanding wither skeletons
		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 4), 531);
		Utils.scheduleTask(() -> Actions.leap(tank, Healer.get()), 532);
		Utils.scheduleTask(() -> {
			Utils.setSpeed(tank, 650);
			Actions.turnHead(tank, 73f, 0f);
			Actions.setHotbarSlot(tank, 5);
			Actions.swapItems(tank, 12, 39); // swap to black cat racing helmet
		}, 533);
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 0), 546);
		Utils.scheduleTask(() -> Actions.move(tank, "WPJ", 0), 554);
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 0), 556); // move to i3 dig spot
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 45f), 573);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 574); // 19/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 575); // 18/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 576); // 17/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 577); // 16/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 578); // 15/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 579); // 14/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 580); // 13/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 581); // 12/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 582); // 11/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 583); // 10/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 584); // 10/20 regen 1
		Utils.scheduleTask(() -> Actions.leftClick(tank), 585); // 9/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 586); // 8/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 587); // 7/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 588); // 6/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 589); // 5/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 590); // 4/20
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 90f), 591);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 592); // 3/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 593); // 2/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 594); // 2/20 regen 1
		Utils.scheduleTask(() -> Actions.leftClick(tank), 595); // 1/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 596); // 0/20
		Utils.scheduleTask(() -> Actions.leftClick(tank), 604);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 614);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 624);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 634);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 644);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 654);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 664);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 674);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 684);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 694);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 704);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 714);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 724);
		Utils.scheduleTask(() -> Actions.leftClick(tank), 734); // move for 22 ticks before landing
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 72f, -25f);
			Actions.setHotbarSlot(tank, 4);
		}, 735);
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 24), 736);
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 4), 766); // move to in front of dev3
		if(doContinue) {
			Utils.scheduleTask(() -> goldor(true), 881);
		}
	}

	private static void goldor(boolean doContinue) {
		Actions.rightClick(tank); // complete i2
		Utils.scheduleTask(() -> Actions.leap(tank, Healer.get()), 1);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 179f, 0f);
			Actions.setHotbarSlot(tank, 5);
		}, 2);
		Utils.scheduleTask(() -> Actions.move(tank, "WP", 3), 3);
//		/*
//		 *  ██╗
//		 * ███║
//		 * ╚██║
//		 *  ██║
//		 *  ██║
//		 *  ╚═╝
//		 */
//		Actions.setHotbarSlot(tank, 5);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 1);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 2);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 3);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 4);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 5);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 6);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 7);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 8);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 9);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 10);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 11);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 12);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 13);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 14);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 15);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 179f, 0f), 16);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.0245, 0, -1.4028), 6), 17);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.0049, 0, -0.2805), 2), 23);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -101f, 17.2f), 24);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 25);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 2, 7), 26);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -160f, 0f), 27);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.48, 0, -1.3184), 1), 28);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.096, 0, -0.2637), 4), 29);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -125f, 67.6f), 33);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 38);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 4, 7), 39);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 3), 40);
//		Utils.scheduleTask(() -> Actions.leap(tank, Archer.get()), 41);
//
//		/*
//		 * ██████╗
//		 * ╚════██╗
//		 *  █████╔╝
//		 * ██╔═══╝
//		 * ███████╗
//		 * ╚══════╝
//		 */
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(tank, -126.6f, 82f);
//			Actions.setHotbarSlot(tank, 1);
//		}, 42);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(1.1264, 0, -0.837), 1), 43);
//		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(1.2247, 0.5, -0.91)), 44);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -126.6f, 0f), 45);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -180f, 90f), 57);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 58);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 1, 8), 59);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 53.5f, 82f), 60);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.1278, 0, 0.8345), 1), 61);
//		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-1.2263, 0.5, 0.907)), 62);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 53.5f, 0f), 63);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.1278, 0, 0.8345), 2), 76);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 53.5f, 82f), 77);
//		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-1.2263, 0.5, 0.907)), 78);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 53.5f, 0f), 79);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 89f, 0f), 91);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.4028, 0, 0.0245), 4), 92);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 0f, 27f), 95);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 96);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 4, 8), 97);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 0f), 98);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.403, 0, 0), 2), 99);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.2806, 0, 0), 6), 101);
//		Utils.scheduleTask(() -> Actions.lavaJump(tank, false), 108);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -15.5f, 0f), 109);
//		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-0.4077, 0.5, -1.47)), 120);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 164.5f, 35f), 121);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(tank);
//			Goldor.broadcastTerminalComplete(tank, "lever", 7, 8);
//		}, 131);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 0f), 133);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.403, 0, 0), 2), 134);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.2806, 0, 0), 15), 136);
//		Utils.scheduleTask(() -> {
//			for(int i = 0; i < 21; i += 5) {
//				Utils.scheduleTask(() -> {
//					world.playSound(tank.getLocation(), Sound.BLOCK_CROP_BREAK, 2.0f, 1.0f);
//					world.playSound(tank.getLocation(), Sound.BLOCK_GRASS_BREAK, 2.0f, 1.0f);
//				}, i);
//			}
//			Goldor.broadcastTerminalComplete(tank, "gate", 2, 3);
//		}, 137);
//
//		/*
//		 * ██████╗1
//		 * ╚════██╗
//		 *  █████╔╝
//		 *  ╚═══██╗
//		 * ██████╔╝
//		 * ╚═════╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 140f, 0f), 150);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.9018, 0, -1.0748), 5), 151);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 140f, 82f), 155);
//		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-0.9806, 0.5, -1.169)), 156);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 140f, 0f), 157);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 70f, 16.2f), 178);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 179);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 5, 7), 180);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 3), 181);
//		Utils.scheduleTask(() -> Actions.leap(tank, Mage.get()), 182);
//
//		/*
//		 * ██╗  ██╗
//		 * ██║  ██║
//		 * ███████║
//		 * ╚════██║
//		 *      ██║
//		 *      ╚═╝
//		 */
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, -1.403), 3), 183); // forceMove to get over the carpet
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(tank, -90f, 0f);
//			Actions.setHotbarSlot(tank, 1);
//		}, 185);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(1.403, 0, 0), 3), 186);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 82f), 188);
//		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(1.52552, 0.5, 0)), 189);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 0f), 190);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -56.1f, 19.8f), 203);
//		Utils.scheduleTask(() -> Actions.swingHand(tank), 204);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 3, 7), 205);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 0f), 206);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(1.403, 0, 0), 2), 207);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.2806, 0, 0), 15), 209);
//		Utils.scheduleTask(() -> Actions.lavaJump(tank, true), 223);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -108.2f, -32.3f), 224);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(tank);
//			Goldor.broadcastTerminalComplete(tank, "lever", 6, 7);
//		}, 229);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 2), 230);
//		Utils.scheduleTask(() -> Actions.leap(tank, Mage.get()), 231);
//
//		/*
//		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
//		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
//		 * █████╗  ██║██║  ███╗███████║   ██║
//		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
//		 * ██║     ██║╚██████╔╝██║  ██║   ██║
//		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
//		 */
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 6), 232);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, -1.403), 11), 256);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -82.5f, -10f), 266);
//		// tick 267: switch to baby yeti
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 268);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 26.6f, 0f), 278);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.5026, 0, 1.004), 4), 279);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.1256, 0, 0.251), 15), 283);
//		Utils.scheduleTask(() -> Actions.lavaJump(tank, true), 298);
//		Utils.scheduleTask(() -> {
//			Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 0.5f);
//			Bukkit.broadcastMessage(ChatColor.RED + " ☠ " + ChatColor.GOLD + "cookiethebald" + ChatColor.GRAY + " burned to death and became a ghost.");
//			tank.setGameMode(GameMode.SPECTATOR);
//			tank.setFlying(true);
//		}, 314);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 3), 315);
//		Utils.scheduleTask(() -> Actions.leap(tank, Healer.get()), 316);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(tank, 176f, 0f);
//			Actions.move(tank, new Vector(0, 0.42, 0), 4);
//		}, 317);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.07534, 0.001, -1.0774), 26), 321);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 180f, -10f), 347);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.GREEN + " ❣ " + ChatColor.GOLD + "cookiethebald" + ChatColor.GREEN + " was revived by " + ChatColor.GOLD + "cookiethebald" + ChatColor.GREEN + "!");
//			tank.setGameMode(GameMode.SURVIVAL);
//			tank.setFlying(false);
//		}, 414);
//		if(doContinue) {
//			Utils.scheduleTask(() -> necron(true), 350);
//		}
	}
//
//	private static void necron(boolean doContinue) {
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 6), 71);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 39), 120);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 2), 160);
//		Utils.scheduleTask(() -> Actions.iceSpray(tank), 161);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 6), 162);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 163);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 183);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 203);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 226);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 238);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 250);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 262);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 274);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 2), 277);
//		Utils.scheduleTask(() -> Actions.iceSpray(tank), 278);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 5), 279);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 286);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 298);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 310);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 322);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 334);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 346);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 358);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 370);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 6), 371);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 372);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 392);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 412);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 2), 413);
//		Utils.scheduleTask(() -> Actions.iceSpray(tank), 414);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 5), 415);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 416);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 428);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 440);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 452);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 464);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 476);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 488);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 500);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 2), 501);
//		Utils.scheduleTask(() -> Actions.leap(tank, Healer.get()), 502);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 65f, 0f), 503);
//		// tick 504: equip black cat
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.2716, 0, 0.5929), 16), 505);
//		Utils.scheduleTask(() -> Actions.jump(tank), 530);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.2543, 0, 0.1186), 9), 531);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.2716, 0, 0.5929), 4), 540);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 67.3f, 29.7f), 543);
//		if(doContinue) {
//			Utils.scheduleTask(Tank::witherKing, 609);
//		}
//	}
//
//	private static void witherKing() {
//		Utils.scheduleTask(() -> WitherKing.pickUpRelic(tank), 1);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 3), 2);
//		Utils.scheduleTask(() -> Actions.leap(tank, Archer.get()), 28);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(tank, 112.5f, -11.2f);
//			Actions.setHotbarSlot(tank, 8);
//		}, 29);
//		Utils.scheduleTask(() -> WitherKing.placeRelic(tank), 30);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(tank, 1.7f, 0f);
//			Actions.setHotbarSlot(tank, 6);
//		}, 31);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.0416, 0, 1.4024), 1), 32);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.00832, 0, 0.2805), 4), 33);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.0416, 0, 1.4024), 29), 37);
//		Utils.scheduleTask(() -> Actions.jump(tank), 65);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.00832, 0, 0.2805), 9), 66);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.0416, 0, 1.4024), 4), 75);
//		Utils.scheduleTask(() -> Actions.jump(tank), 78);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.00832, 0, 0.2805), 9), 79);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.0416, 0, 1.4024), 3), 88);
//		Utils.scheduleTask(() -> Actions.jump(tank), 90);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.00832, 0, 0.2805), 8), 91);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -180f, -90f), 98);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 8), 360);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 8), 368);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 8), 376);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 8), 384);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 8), 392);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 2), 401);
//		Utils.scheduleTask(() -> Actions.iceSpray(tank), 402);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 5), 403);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 404);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 416);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 428);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(tank, -154.7f, 0f);
//			Actions.setHotbarSlot(tank, 6);
//		}, 429);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.6, 0, -1.268), 3), 430);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.12, 0, -0.2537), 5), 433);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.6, 0, -1.268), 4), 438);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.12, 0, -0.2537), 5), 442);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.6, 0, -1.268), 9), 447);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.12, 0, -0.2537), 5), 456);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.6, 0, -1.268), 17), 461);
//		Utils.scheduleTask(() -> Actions.jump(tank), 477);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.12, 0, -0.2537), 9), 478);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.6, 0, -1.268), 3), 487);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, 0f, -90f), 488);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 690);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 700);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 710);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 720);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 730);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 5), 740);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 741);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(tank, 91f, 0f);
//			Actions.setHotbarSlot(tank, 6);
//		}, 742);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.4028, 0, -0.0245), 5), 743);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.2805, 0, -0.0049), 5), 748);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.4028, 0, -0.0245), 28), 753);
//		Utils.scheduleTask(() -> Actions.jump(tank), 780);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.2805, 0, -0.0049), 6), 781);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -135f, -90f), 787);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 796);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 806);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 816);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 826);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 836);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 5), 846);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 847);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(tank, 0f, 0f);
//			Actions.setHotbarSlot(tank, 6);
//		}, 848);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, 1.403), 4), 849);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, 0.2806), 5), 853);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, 1.403), 7), 858);
//		Utils.scheduleTask(() -> Actions.jump(tank), 864);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, 0.2806), 9), 865);
//		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, 1.403), 4), 874);
//		Utils.scheduleTask(() -> Actions.turnHead(tank, -180f, -90f), 878);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 902);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 912);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 922);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 932);
//		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 942);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(tank, 5), 952);
//		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 953);
//	}

	@SuppressWarnings("unused")
	public static Player get() {
		return tank;
	}
}