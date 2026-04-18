package instructions.players;

import instructions.Actions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import plugin.Utils;

public class Healer {
	private static Player healer;
	private static World world;

	public static void healerInstructions(Player p, String section) {
		healer = p;
		world = healer.getWorld();

		switch(section) {
			case "all", "clear" -> {
				Utils.teleport(healer, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f));
				Utils.scheduleTask(() -> preClear(section.equals("all")), 60);
			}
			case "maxor", "boss" -> {
				Utils.teleport(healer, new Location(world, 73.5, 221, 14.5, 0f, 0f));
				Actions.swapItems(healer, 1, 28);
				Actions.swapItems(healer, 3, 30);
				Actions.swapItems(healer, 6, 33);
				Actions.swapItems(healer, 7, 34);
				Actions.setHotbarSlot(healer, 5);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
//			case "storm" -> {
//				Utils.teleport(healer, new Location(world, 111.719, 170, 92.386, -53.2f, 24.7f));
//				Actions.swapItems(healer, 1, 28);
//				Actions.swapItems(healer, 3, 30);
//				Actions.swapItems(healer, 7, 34);
//				Utils.scheduleTask(() -> storm(false), 60);
//			}
//			case "goldor" -> {
//				Utils.teleport(healer, new Location(world, 108.308, 120, 93.895, -132.4f, 2.3f));
//				Actions.swapItems(healer, 1, 28);
//				Actions.swapItems(healer, 3, 30);
//				Actions.swapItems(healer, 7, 34);
////				Utils.scheduleTask(() -> goldor(false), 60);
//			}
//			case "necron" -> {
//				Utils.teleport(healer, new Location(world, 56.488, 64, 111.700, -180f, 0f));
//				Actions.swapItems(healer, 1, 28);
//				Actions.swapItems(healer, 3, 30);
//				Actions.swapItems(healer, 6, 33);
//				Actions.swapItems(healer, 7, 34);
//				Utils.scheduleTask(() -> necron(false), 60);
//			}
//			case "witherking" -> {
//				Utils.teleport(healer, new Location(world, 56.326, 8, 130.7, -16.2f, 18.8f));
//				Actions.swapItems(healer, 1, 28);
//				Actions.swapItems(healer, 3, 30);
//				Actions.swapItems(healer, 6, 33);
//				Actions.swapItems(healer, 7, 34);
//				Utils.scheduleTask(Healer::witherKing, 60);
//			}
		}
	}

	public static void preClear(boolean doContinue) {
		Actions.setHotbarSlot(healer, 1);
		Actions.move(healer, "WPJ", 0);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 15), 13);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -9f, 2.9f), 31);
		Utils.scheduleTask(() -> clear(doContinue), 126);
	}

	public static void clear(boolean doContinue) {
		/*
		 * ████████╗██████╗  █████╗ ██████╗
		 * ╚══██╔══╝██╔══██╗██╔══██╗██╔══██╗
		 *    ██║   ██████╔╝███████║██████╔╝
		 *    ██║   ██╔══██╗██╔══██║██╔═══╝
		 *    ██║   ██║  ██║██║  ██║██║
		 *    ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝
		 */
		Utils.scheduleTask(() -> Actions.move(healer, "N", 6), 20);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 21); // etherwarp into red blue
		Utils.scheduleTask(() -> Actions.turnHead(healer, 109.5f, 1.75f), 22);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 23); // etherwarp towards trap
		Utils.scheduleTask(() -> Actions.turnHead(healer, -159f, 4.5f), 24);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 25); // etherwarp into trap
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -110f, 11f);
			Actions.setHotbarSlot(healer, 7);
		}, 26);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 27); // throw pearl into spot | lands in 5 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 180f, 35f);
			Actions.setHotbarSlot(healer, 5);
		}, 28);
		// tick 32: pearl lands
		Utils.scheduleTask(() -> {
			Actions.move(healer, "WP", 6);
			Actions.leftClick(healer);
		}, 33);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 34);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 35);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 36);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 37);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 38);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, 35f), 39);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 175f, 9.5f);
			Actions.setHotbarSlot(healer, 7);
		}, 39);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 40); // throw pearl across | lands in 12 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, -160f, 55f), 41);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Trap 1/3 (Opened Chest)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.CHEST);
		}, 42);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 109.5f, -68f), 43);
		// Tick 52: pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(healer), 53); // throw pearl up | lands in 21 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 32f, 16f), 54);
		// Tick 74: pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(healer), 75); // throw pearl towards bat | lands in 4 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 76);
		// Tick 79: pearl lands
		Utils.scheduleTask(() -> Actions.move(healer, "AWP", 0), 80);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 3), 82);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 60f, 6f), 84);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 86); // pearl to dig spot | lands in 3 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 78f, -22f);
			Actions.setHotbarSlot(healer, 0);
		}, 87);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Trap 2/3 (Killed Bat)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.BAT);
		}, 88);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, 35f);
			Actions.setHotbarSlot(healer, 5);
		}, 89); // pearl lands
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Actions.move(healer, "WP", 9);
		}, 90);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 91);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 92);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 93);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 94);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 95);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 96);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 97);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 20f);
			Actions.setHotbarSlot(healer, 7);
		}, 99);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 100); // throw pearl towards chest | lands in 4 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 50f), 101);
		// Tick 104: pearl lands
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Trap 3/3 (Opened Chest)");
			Utils.broadcastBlessing(healer, Utils.BlessingType.POWER, 2);
			Utils.playSecretFoundSound(healer, Utils.SecretType.BLESSING_CHEST);
		}, 105);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 81.5f);
			Actions.setHotbarSlot(healer, 5);
		}, 106);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 107); // stonk block
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 7), 108);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 109); // throw pearl down | lands in 8 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 30f);
			Actions.setHotbarSlot(healer, 5);
		}, 110);
		// Tick 117: pearl lands
		Utils.scheduleTask(() -> {
			Actions.move(healer, "WP", 0);
			Actions.leftClick(healer);
		}, 118);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 119);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 120);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 121);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 122);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 123);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 124); // stonk outwards
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -58.5f, 38f);
			Actions.move(healer, "N", 2);
			Actions.setHotbarSlot(healer, 7);
		}, 126);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 127); // throw pearl down | lands in 10 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, -47f, 20f), 128);
		// Tick 137: pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(healer), 138); // throw pearl out of trap | lands in 3 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 16f, 2.9f);
			Actions.move(healer, "N", 4);
			Actions.setHotbarSlot(healer, 1);
		}, 139);
		// Tick 141: pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(healer), 142); // etherwarp to red blue
		// Trap: 142 ticks | Time from Enter: 117 ticks

		/*
		 * ██████╗ ███████╗██████╗     ██████╗ ██╗     ██╗   ██╗███████╗
		 * ██╔══██╗██╔════╝██╔══██╗    ██╔══██╗██║     ██║   ██║██╔════╝
		 * ██████╔╝█████╗  ██║  ██║    ██████╔╝██║     ██║   ██║█████╗
		 * ██╔══██╗██╔══╝  ██║  ██║    ██╔══██╗██║     ██║   ██║██╔══╝
		 * ██║  ██║███████╗██████╔╝    ██████╔╝███████╗╚██████╔╝███████╗
		 * ╚═╝  ╚═╝╚══════╝╚═════╝     ╚═════╝ ╚══════╝ ╚═════╝ ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 65f, 0f);
			Actions.setHotbarSlot(healer, 5);
		}, 143);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 144);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 1);
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Red Blue 1/4 (Opened Chest)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.CHEST);
		}, 145);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, -90f);
			Actions.move(healer, "N", 0);
		}, 146);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 147); // etherwarp to prince
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, 0f);
			Actions.swapItems(healer, 5, 31); // Stonk -> 31, Infinityboom TNT -> 5
			Actions.setHotbarSlot(healer, 5);
		}, 148);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 149); // blow up crypt
		Utils.scheduleTask(() -> {
			Actions.swapItems(healer, 5, 31); // restore: Stonk -> 5, Infinityboom TNT -> 31
			Actions.setHotbarSlot(healer, 3); // Claymore
		}, 150);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 151); // kill prince | claymore swing
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 180f, -5f);
			Actions.setHotbarSlot(healer, 1);
		}, 152);
		Utils.scheduleTask(() -> {
			Actions.rightClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Prince Killed | Crypt 5/5");
		}, 153); // etherwarp across
		Utils.scheduleTask(() -> Actions.turnHead(healer, 100f, 10f), 154);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 155); // etherwarp to item
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -97f, -4f);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Red Blue 2/4 (Picked Up Item)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.ITEM);
		}, 156);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 157); // etherwarp to chest
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -115f, 0f);
			Actions.setHotbarSlot(healer, 5);
		}, 158);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 159);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 160); // stonk towards chest
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 1);
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Red Blue 3/4 (Opened Chest)");
			Utils.broadcastBlessing(healer, Utils.BlessingType.POWER, 2);
			Utils.playSecretFoundSound(healer, Utils.SecretType.BLESSING_CHEST);
		}, 161);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 57f, 39f), 162);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 163); // etherwarp down
		Utils.scheduleTask(() -> Actions.turnHead(healer, 118f, -39f), 164);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 165); // etherwarp to wither essence
		Utils.scheduleTask(() -> Actions.turnHead(healer, 23f, 30f), 166);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Red Blue 4/4 (Opened Wither Essence)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.ESSENCE);
		}, 167);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 176f, 44.4f), 168);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 169); // etherwarp towards yellow
		// Red Blue: 26 ticks

		/*
		 * ██╗   ██╗███████╗██╗     ██╗      ██████╗ ██╗    ██╗
		 * ╚██╗ ██╔╝██╔════╝██║     ██║     ██╔═══██╗██║    ██║
		 *  ╚████╔╝ █████╗  ██║     ██║     ██║   ██║██║ █╗ ██║
		 *   ╚██╔╝  ██╔══╝  ██║     ██║     ██║   ██║██║███╗██║
		 *    ██║   ███████╗███████╗███████╗╚██████╔╝╚███╔███╔╝
		 *    ╚═╝   ╚══════╝╚══════╝╚══════╝ ╚═════╝  ╚══╝╚══╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, 5.5f), 170);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 171); // etherwarp up to miniboss
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 172);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 173);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 178); // claymore swing at miniboss
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Yellow Cleared");
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Clear Finished in 179 ticks (8.95 seconds)");
			Utils.broadcastBlessing(healer, Utils.BlessingType.WISDOM, 5);
		}, 179);
		// Yellow: 10 ticks

		Utils.scheduleTask(() -> {
			Actions.swapItems(healer, 1, 28);
			Actions.swapItems(healer, 3, 30);
			Actions.swapItems(healer, 6, 33);
			Actions.swapItems(healer, 7, 34);
			Actions.setHotbarSlot(healer, 5);
		}, 180);
		Utils.scheduleTask(() -> {
			if(doContinue) {
				Utils.teleport(healer, new Location(world, 73.5, 221, 14.5));
				maxor(true);
			}
		}, 742);
	}

	public static void maxor(boolean doContinue) {
		Utils.setSpeed(healer, 500); // change pet to black cat (autopet)
		Utils.scheduleTask(() -> Actions.turnHead(healer, -15f, 45f), 1);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 14), 2);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 15);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 16); // stonk maxor floor
		Utils.scheduleTask(() -> Actions.turnHead(healer, -60f, 0f), 17);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 48), 18); // fall to storm floor
		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 30f), 66);
		Utils.scheduleTask(() -> {
			Actions.move(healer, "W", 11);
			Actions.leftClick(healer);
		}, 67); // walk forward + fall to goldor floor
		Utils.scheduleTask(() -> Actions.leftClick(healer), 68);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 69);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 70);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 71);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 72);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 73);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 74);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 75);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 76); // stonk into goldor
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 80f); // adjust movement angle
			Actions.setHotbarSlot(healer, 1);
		}, 78);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 79);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 12f, 80f), 119);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 121); // bonzo 1 of 4
		// tick 122: propelled forward for 12 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 134);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -12f, 80f), 143);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 144); // bonzo 2 of 4
		// tick 145: propelled forward for 10 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 3f, 80f), 154);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 156); // jump to gain slightly more distance
		Utils.scheduleTask(() -> {
			Actions.move(healer, "WPD", 0); // microadjustment
		}, 157);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 158); // bonzo 3 of 4 | need to bonzo a little earlier to get the needed momentum and height
		// tick 160: propelled forward for 13 ticks
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 121), 161);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 5f, 80f), 172);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 173); // bonzo 4 of 4
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 30f);
			Actions.setHotbarSlot(healer, 5);
		}, 174);
		// tick 175: propelled forward for 7 ticks
		// tick 182: walk forward and fall for 7 ticks
		Utils.scheduleTask(() -> Actions.leftClick(healer), 190);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 191);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 192);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 193);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 194);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 195);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 1), 196);
		// tick 197: start falling for 13 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 81f, 80f), 199);
		// tick 210: reach ground
		Utils.scheduleTask(() -> Actions.rightClick(healer), 214); // bonzo to lights
		// tick 215: propelled forward for 23 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 75f, 90f);
			Actions.setHotbarSlot(healer, 7);
		}, 216);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 238); // soul sand to lava jump higher
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, -23f);
			Actions.setHotbarSlot(healer, 5);
		}, 280);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 283);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, -40f), 284);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 285);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -43f, -45f), 286);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 287);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 44f, -45f), 288);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 289);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -42f, 3f), 290);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 291);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 43f, 3f), 292);
		Utils.scheduleTask(() -> {
			Actions.rightClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Predev Finished in 293 Ticks (14.65 seconds) | Overall: 1 035 ticks (51.75 seconds)");
		}, 293);

//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(healer);
//			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Predev Finished in 317 Ticks (15.85 seconds) | Overall: 1 344 ticks (67.20 seconds)");
//		}, 317);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 334);
//		Utils.scheduleTask(() -> Actions.leap(healer, Berserk.get()), 335);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(healer, 1);
//			Actions.move(healer, new Vector(1.0936, 0, 0.2525), 33);
//		}, 336);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -31.8f, 0f), 368);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.5915, 0, 0.954), 20), 369);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -31.8f, 82f), 388);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(0.804, 0.5, 1.296)), 389);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -31.8f, 0f), 390);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(healer, 6);
//			Actions.move(healer, new Vector(0.5915, 0, 0.954), 7);
//		}, 400);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -53.2f, 24.7f), 407);
//		if(doContinue) {
////			Utils.scheduleTask(() -> storm(true), 499);
//		}
	}
//
//	public static void storm(boolean doContinue) {
//		Storm.prepadPurple();
//		Actions.setHotbarSlot(healer, 6);
//		Utils.scheduleTask(() -> Actions.gyro(healer, new Location(world, 114, 169, 94)), 1);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -53.2f, 0f);
//			Actions.setHotbarSlot(healer, 4);
//		}, 2);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 3);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 4);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 8);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 9);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 13);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 14);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 18);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 19);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 23);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 24);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 28);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 29);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 0f), 30);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(1.12242, 0, 0), 5), 31);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -180f, 9f), 36);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 37);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 38);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 42);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 43);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 47);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 48);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -180f, 6f), 49);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 52);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 53);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 57);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 58);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 62);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 63);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -180f, 3f), 64);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 67);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 68);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 72);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 73);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 77);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 78);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 0f), 79);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-1.12242, 0, 0), 5), 80);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 73f, -2f), 85);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 86);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 87);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 91);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 92);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 80f, 9f), 93);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 96);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 97);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 101);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 102);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 88f, 9f), 103);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 106);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 107);
//		Utils.scheduleTask(() -> Actions.rightClickOld(healer), 111);
//		Utils.scheduleTask(() -> Actions.salvation(healer), 112);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 157.8f, 2.7f), 173);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(healer, 1);
//			Actions.move(healer, new Vector(-0.4241, 0, -1.0392), 6);
//		}, 174);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 157.8f, 82f), 179);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(-0.5764, 0.5, -1.4124)), 180);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 157.8f, 0f), 181);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.4241, 0, -1.0392), 9), 195);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 4), 201);
//		for(int tick = 205; tick <= 545; tick += 5) {
//			Utils.scheduleTask(() -> {
//				List<Entity> nearbyEntities = healer.getNearbyEntities(10, 10, 10);
//
//				for(Entity entity : nearbyEntities) {
//					if(entity instanceof WitherSkeleton) {
//						Location healerLoc = healer.getLocation();
//						Location witherLoc = entity.getLocation();
//
//						double deltaX = witherLoc.getX() - healerLoc.getX();
//						double deltaY = witherLoc.getY() - healerLoc.getY();
//						double deltaZ = witherLoc.getZ() - healerLoc.getZ();
//
//						float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
//						float pitch = (float) -(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * 180.0 / Math.PI);
//
//						Actions.turnHead(healer, yaw, pitch);
//
//						Utils.scheduleTask(() -> Actions.rightClickOld(healer), 1);
//
//						break;
//					}
//				}
//			}, tick);
//		}
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -22.2f, 0f);
//			Actions.setHotbarSlot(healer, 1);
//		}, 546);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.4241, 0, 1.0392), 12), 547);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -22.2f, 82f), 557);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(0.5764, 0.5, 1.4124)), 559);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -22.2f, 0f);
//			Actions.setHotbarSlot(healer, 3);
//		}, 560);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.4241, 0, 1.0392), 4), 570);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.4241, 0, 1.0392), 2), 653);
//		Utils.scheduleTask(Storm::crushPurple, 655);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 656);
//		Utils.scheduleTask(() -> Actions.leap(healer, Berserk.get()), 665);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.8634, 0, 0), 2), 666);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.2806, 0, 0), 5), 668);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 0f, 0f);
//			Actions.setHotbarSlot(healer, 1);
//		}, 673);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.1984, 0, 0.1984), 15), 674);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 15f, 0f), 689);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.12242), 3), 690);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 15f, 82f), 692);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(-0.4714, 0.5, 1.451)), 693);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 15f, 0f), 694);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 706);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.12242), 11), 707);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -4f, 82f), 717);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(0.1064, 0.5, 1.5218)), 718);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -4f, 0f), 719);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -14f, 0f), 728);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.2715, 0, 1.089), 3), 729);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -56.4f, 0f), 731);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.935, 0, 0.6211), 2), 732);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -56.4f, 82f), 733);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(1.271, 0.5, 0.8442)), 734);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -56.4f, 2.3f);
//			Actions.setHotbarSlot(healer, 5);
//		}, 735);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 0f), 747);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -132.4f, 2.3f), 796);
//		if(doContinue) {
//			Utils.scheduleTask(() -> goldor(true), 890);
//		}
//	}
//
//	private static void goldor(boolean doContinue) {
//		/*
//		 *  ██╗
//		 * ███║
//		 * ╚██║
//		 *  ██║
//		 *  ██║
//		 *  ╚═╝
//		 */
//		Actions.setHotbarSlot(healer, 5);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 1);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 2);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 3);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 121 93 minecraft:sea_lantern");
//		}, 4);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 121 93 minecraft:obsidian");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 121 94 minecraft:sea_lantern");
//		}, 5);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 6);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 121 94 minecraft:obsidian");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 122 94 minecraft:sea_lantern");
//		}, 7);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 8);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 9);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 122 94 minecraft:obsidian");
//		}, 10);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 122 93 minecraft:sea_lantern");
//		}, 11);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 12);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 122 93 minecraft:obsidian");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 123 93 minecraft:sea_lantern");
//		}, 13);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 14);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Goldor.broadcastTerminalComplete(healer, "device", 1, 7);
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 111 123 93 minecraft:obsidian");
//		}, 15);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 5.4f, 0f);
//			Actions.setHotbarSlot(healer, 1);
//		}, 16);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.132, 0, 1.397), 3), 17);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 5.4f, 82f), 19);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(-0.1436, 0.5, 1.5188)), 20);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 5.4f, 0f), 21);
//		Utils.scheduleTask(() -> {
//			Actions.jump(healer);
//			Actions.move(healer, new Vector(-0.132, 0, 1.397), 1);
//			Actions.setHotbarSlot(healer, 5);
//		}, 31);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.0264, 0, 0.2794), 3), 32);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 35);
//		Utils.scheduleTask(() -> Actions.jump(healer), 40);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(healer);
//			Goldor.broadcastTerminalComplete(healer, "lever", 5, 7);
//		}, 41);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -90f, 45f);
//			Actions.setHotbarSlot(healer, 1);
//		}, 42);
//		final BukkitRunnable[] temp = new BukkitRunnable[1];
//		Utils.scheduleTask(() -> temp[0] = Actions.bonzo(healer, new Vector(-1.52552, 0, 0)), 43);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 0f), 44);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 64.6f, 27.9f), 49);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(healer);
//			Goldor.broadcastTerminalComplete(healer, "lever", 6, 7);
//		}, 50);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 51);
//		Utils.scheduleTask(() -> {
//			temp[0].cancel();
//			Actions.leap(healer, Archer.get());
//		}, 52);
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
//			Actions.turnHead(healer, 0f, 0f);
//			Actions.setHotbarSlot(healer, 1);
//		}, 53);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.405), 5), 54);
//		Utils.scheduleTask(() -> Actions.lavaJump(healer, true), 66);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 0.2806), 4), 88);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -8.2f, 1.7f), 94);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Goldor.broadcastTerminalComplete(healer, "device", 5, 8);
//		}, 100);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 0f), 101);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-1.403, 0, 0), 2), 102);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 82f), 103);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(-1.52552, 0.5, 0)), 104);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 0f), 105);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 90f, 82f);
//			Actions.move(healer, new Vector(-1.403, 0, 0), 1);
//		}, 121);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(-1.52552, 0.5, 0)), 122);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 36.6f), 123);
//		Utils.scheduleTask(() -> {
//			Actions.jump(healer);
//			Actions.move(healer, new Vector(-1.403, 0, 0), 1);
//		}, 134);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.2806, 0, 0), 4), 135);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(healer);
//			Goldor.broadcastTerminalComplete(healer, "lever", 8, 8);
//			Bukkit.broadcastMessage(ChatColor.GREEN + "S2 finished in 87 ticks (4.35 seconds) | Terminals: 138 ticks (6.90 seconds) | Overall: 2 554 ticks (127.70 seconds)");
//			Server.removeS3Gate();
//		}, 138);
//		Utils.scheduleTask(() -> Actions.leap(healer, Berserk.get()), 139);
//
//		/*
//		 * ██████╗
//		 * ╚════██╗
//		 *  █████╔╝
//		 *  ╚═══██╗
//		 * ██████╔╝
//		 * ╚═════╝
//		 */
//		Utils.scheduleTask(() -> {
//			Actions.move(healer, new Vector(-0.2677, 0, 1.377), 1);
//			Actions.setHotbarSlot(healer, 1);
//		}, 140);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.05354, 0, 0.2754), 2), 141);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 81.1f, 11.2f), 142);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.2992, 0, 0.127), 1), 143);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 144);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(healer, "terminal", 2, 7), 145);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -49f, 82f), 146);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(1.059, 0, 0.9205), 1), 147);
//		Utils.scheduleTask(() -> temp[0] = Actions.bonzo(healer, new Vector(1.151, 0.5, 1.001)), 148);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -49f, 0f), 149);
//		Utils.scheduleTask(() -> {
//			temp[0].cancel();
//			Actions.lavaJump(healer, false);
//			Actions.turnHead(healer, -22f, 0f);
//		}, 166);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.1051, 0, 0.2602), 27), 167);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 9.2f), 193);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 194);
//		Utils.scheduleTask(() -> {howev
//			Goldor.broadcastTerminalComplete(healer, "terminal", 7, 7);
//			Bukkit.broadcastMessage(ChatColor.GREEN + "S3 finished in 57 ticks (2.85 seconds) | Terminals: 195 ticks (9.75 seconds) | Overall: 2 611 ticks (130.55 seconds)");
//		}, 195);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 196);
//		Utils.scheduleTask(() -> Actions.leap(healer, Mage.get()), 197);
//
//		/*
//		 * ██╗  ██╗
//		 * ██║  ██║
//		 * ███████║
//		 * ╚════██║
//		 *      ██║
//		 *      ╚═╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 145f, 82f), 198);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.8047, 0, -1.1493), 1), 199);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(-0.875, 0.5, -1.25)), 200);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 145f, 0f), 201);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.8047, 0, -1.1493), 1), 218);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.161, 0, -0.23), 6), 219);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, 42.2f), 224);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 225);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(healer, "terminal", 4, 7), 226);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 227);
//		Utils.scheduleTask(() -> Actions.leap(healer, Mage.get()), 228);
//
//		/*
//		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
//		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
//		 * █████╗  ██║██║  ███╗███████║   ██║
//		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
//		 * ██║     ██║╚██████╔╝██║  ██║   ██║
//		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
//		 */
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -1.9f, 0f);
//			Actions.setHotbarSlot(healer, 5);
//		}, 229);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.04652, 0, 1.4022), 3), 230);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.0093, 0, 0.2805), 5), 233);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.04652, 0, 1.4022), 32), 238);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.0108, 0, 0.3248), 4), 270);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -1.9f, 85.1f), 271);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(56, 113, 110)), 272);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(56, 113, 111)), 273);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 274);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.08), 1), 275);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, 0f), 276);
//		Utils.scheduleTask(() -> Actions.swapItems(healer, 6, 33), 277);
//		if(doContinue) {
//			Utils.scheduleTask(() -> necron(true), 350);
//		}
//	}
//
//	private static void necron(boolean doContinue) {
//		Actions.setHotbarSlot(healer, 3);
//		Utils.scheduleTask(() -> Actions.leap(healer, Tank.get()), 121);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 6), 122);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 36), 123);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(healer, 7);
//			Actions.move(healer, new Vector(0, 0, -1.12242), 2);
//		}, 160);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, 40f), 161);
//		Utils.scheduleTask(() -> Actions.ghostPick(healer, world.getBlockAt(54, 64, 80)), 162);
//		Utils.scheduleTask(() -> Actions.ghostPick(healer, world.getBlockAt(54, 64, 79)), 163);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, -1.12242), 2), 164);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 180f, 90f);
//			Actions.setHotbarSlot(healer, 5);
//		}, 165);
//		Utils.scheduleTask(() -> Actions.ghostPick(healer, world.getBlockAt(54, 63, 79)), 166);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, 0f), 167);
//		// tick 168: equip black cat
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -2f, 0f), 512);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.049, 0, 1.4022), 11), 513);
//		Utils.scheduleTask(() -> Actions.jump(healer), 523);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.00979, 0, 0.2804), 9), 524);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.049, 0, 1.4022), 3), 533);
//		Utils.scheduleTask(() -> Actions.jump(healer), 535);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.00979, 0, 0.2804), 9), 536);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.049, 0, 1.4022), 2), 545);
//		Utils.scheduleTask(() -> Actions.jump(healer), 546);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.00979, 0, 0.2804), 9), 547);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.049, 0, 1.4022), 4), 556);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -16.2f, 18.8f), 559);
//		if(doContinue) {
//			Utils.scheduleTask(Healer::witherKing, 609);
//		}
//	}
//
//	private static void witherKing() {
//		Utils.scheduleTask(() -> WitherKing.pickUpRelic(healer), 1);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 2);
//		Utils.scheduleTask(() -> Actions.leap(healer, Archer.get()), 29);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -165.4f, 0f);
//			Actions.setHotbarSlot(healer, 8);
//		}, 30);
//		Utils.scheduleTask(() -> {
//			WitherKing.placeRelic(healer);
//			Bukkit.broadcastMessage(ChatColor.GREEN + "Relics placed in 31 ticks (1.55 seconds) | Overall: 3 406 ticks (170.30 seconds)");
//		}, 31);
//		Utils.scheduleTask(() -> Actions.swapItems(healer, 5, 32), 32);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -30.5f, 0f);
//			Actions.setHotbarSlot(healer, 6);
//		}, 33);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.7121, 0, 1.209), 1), 34);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.1424, 0, 0.2418), 5), 35);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.7121, 0, 1.209), 31), 40);
//		Utils.scheduleTask(() -> Actions.jump(healer), 70);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.1424, 0, 0.2418), 9), 71);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.7121, 0, 1.209), 2), 80);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, -90f), 81);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 350);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 360);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 370);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 380);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 390);
//		Utils.scheduleTask(() -> Actions.jump(healer), 396);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 2), 401);
//		Utils.scheduleTask(() -> Actions.iceSpray(healer), 402);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 5), 403);
//		Utils.scheduleTask(() -> Actions.flamingFlay(healer), 404);
//		Utils.scheduleTask(() -> Actions.flamingFlay(healer), 416);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -177.5f, 0f);
//			Actions.setHotbarSlot(healer, 6);
//		}, 417);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.0612, 0, -1.402), 13), 418);
//		Utils.scheduleTask(() -> Actions.jump(healer), 430);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.0122, 0, -0.2803), 11), 431);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.0612, 0, -1.402), 8), 442);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, -90f), 450);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 690);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 700);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 710);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 720);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 730);
//		Utils.scheduleTask(() -> Actions.jump(healer), 736);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 2), 741);
//		Utils.scheduleTask(() -> Actions.iceSpray(healer), 742);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 5), 743);
//		Utils.scheduleTask(() -> Actions.flamingFlay(healer), 744);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 91f, 0f);
//			Actions.setHotbarSlot(healer, 6);
//		}, 745);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-1.4028, 0, -0.0245), 5), 748);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.2805, 0, -0.0049), 5), 753);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-1.4028, 0, -0.0245), 28), 758);
//		Utils.scheduleTask(() -> Actions.jump(healer), 785);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.2805, 0, -0.0049), 6), 786);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -135f, -90f), 792);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 796);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 806);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 816);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 826);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 836);
//		Utils.scheduleTask(() -> Actions.jump(healer), 842);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 2), 847);
//		Utils.scheduleTask(() -> Actions.iceSpray(healer), 848);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 5), 849);
//		Utils.scheduleTask(() -> Actions.flamingFlay(healer), 850);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 0f, 0f);
//			Actions.setHotbarSlot(healer, 6);
//		}, 851);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.403), 3), 854);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 0.2806), 5), 857);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.403), 9), 862);
//		Utils.scheduleTask(() -> Actions.jump(healer), 870);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 0.2806), 9), 871);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.403), 4), 880);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -180f, -90f), 884);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 902);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 912);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 922);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 923);
//		Utils.scheduleTask(() -> Actions.lastBreath(healer, 10), 942);
//		Utils.scheduleTask(() -> Actions.jump(healer), 948);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 2), 953);
//		Utils.scheduleTask(() -> Actions.iceSpray(healer), 954);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 5), 955);
//		Utils.scheduleTask(() -> Actions.flamingFlay(healer), 956);
//	}

	@SuppressWarnings("unused")
	public static Player get() {
		return healer;
	}
}
