package instructions.players;

import instructions.Actions;
import instructions.Server;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import plugin.Utils;

import java.util.Objects;

public class Archer {
	private static Player archer;
	private static World world;

	public static void archerInstructions(Player p, String section) {
		archer = p;
		world = archer.getWorld();
		Objects.requireNonNull(archer.getInventory().getItem(4)).addUnsafeEnchantment(Enchantment.POWER, 66);

		switch(section) {
			case "all", "clear" -> {
				Utils.teleport(archer, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f));
				Utils.scheduleTask(() -> preClear(section.equals("all")), 60);
			}
			case "maxor", "boss" -> {
				Utils.teleport(archer, new Location(world, 73.5, 221, 14.5, 0f, 0f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 6, 33);
				Actions.swapItems(archer, 7, 34);
				Actions.swapItems(archer, 9, 36);
				Actions.setHotbarSlot(archer, 5);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
//			case "storm" -> {
//				Utils.teleport(archer, new Location(world, 46.687, 169, 57.747, 177.8f, 0f));
//				Actions.swapItems(archer, 1, 28);
//				Actions.swapItems(archer, 7, 35);
//				Utils.scheduleTask(() -> storm(false), 60);
//			}
//			// lb: -73.1 -46.8, take 36 ticks to hit
//			// term: 5 ticks
//			// -15.4, -7.6 face here
//			case "goldor" -> {
//				Utils.teleport(archer, new Location(world, 63.343, 127, 35.246, -73.1f, -46.8f));
//				Actions.swapItems(archer, 1, 28);
//				Actions.swapItems(archer, 5, 32);
//				Actions.swapItems(archer, 6, 33);
//				Actions.swapItems(archer, 7, 35);
//				Actions.setHotbarSlot(archer, 5);
//				rapidFire(61);
//				Utils.scheduleTask(() -> Actions.lastBreath(archer, 6), 24);
//				Utils.scheduleTask(() -> Actions.lastBreath(archer, 6), 30);
//				Utils.scheduleTask(() -> Actions.lastBreath(archer, 6), 36);
//				Utils.scheduleTask(() -> {
//					Actions.setHotbarSlot(archer, 4);
//					Actions.turnHead(archer, -12f, -8.9f);
//				}, 42);
//				Utils.scheduleTask(Archer::explosiveShot, 54);
//				Utils.scheduleTask(() -> Actions.turnHead(archer, -19.2f, -2f), 55);
//				// rapid fire arrow fires
//				Utils.scheduleTask(() -> Actions.turnHead(archer, -8f, -0.7f), 57);
//				Utils.scheduleTask(Archer::shoot, 58);
//				Utils.scheduleTask(() -> Actions.turnHead(archer, -18.9f, 5.3f), 59);
////				Utils.scheduleTask(() -> {
////					Actions.setHotbarSlot(archer, 1);
////					Actions.turnHead(archer, 90f, 0f);
////                }, 58);
////				Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.403, 0, 0), 1), 59);
//				Utils.scheduleTask(() -> goldor(false), 60);
//			}
//			case "necron" -> {
//				Utils.teleport(archer, new Location(world, 56.488, 64, 111.700, -180f, 0f));
//				Actions.swapItems(archer, 1, 28);
//				Actions.swapItems(archer, 5, 32);
//				Actions.swapItems(archer, 6, 33);
//				Actions.swapItems(archer, 7, 35);
//				Utils.scheduleTask(() -> necron(false), 60);
//			}
//			case "witherking" -> {
//				Utils.teleport(archer, new Location(world, 22.3, 6, 59.408, 65.6f, 29.3f));
//				Actions.swapItems(archer, 1, 28);
//				Actions.swapItems(archer, 3, 30);
//				Actions.swapItems(archer, 5, 32);
//				Actions.swapItems(archer, 6, 33);
//				Actions.swapItems(archer, 7, 35);
//				Actions.swapItems(archer, 11, 39);
//				Utils.scheduleTask(Archer::witherKing, 60);
//			}
		}
	}

	private static void preClear(boolean doContinue) {
		Actions.setHotbarSlot(archer, 1);
		Actions.move(archer, "WPJ", 0);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 15), 13);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -1f, -3.4f), 31);
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
		Utils.scheduleTask(() -> Actions.move(archer, "N", 4), 20);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 21); // etherwarp into fairy
		Utils.scheduleTask(() -> Actions.turnHead(archer, 4f, 24f), 22);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 23); // etherwarp to wither door
		Utils.scheduleTask(() -> {
			Actions.leftClick(archer);
			Server.openWitherDoor(archer);
		}, 25); // open door (waits 1 tick after pickup to ensure no race conditions) | opens tick 45
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 10f, 6.5f);
			Actions.move(archer, "N", 0);
			Actions.setHotbarSlot(archer, 1);
		}, 46); // mage leaps
		Utils.scheduleTask(() -> Actions.rightClick(archer), 47); // etherwarp into room
		Utils.scheduleTask(() -> Actions.turnHead(archer, -57.15f, 1.75f), 48);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 49); // etherwarp into Gravel
		// Blood Rush: 49 ticks

		/*
		 *  ██████╗ ██████╗  █████╗ ██╗   ██╗███████╗██╗
		 * ██╔════╝ ██╔══██╗██╔══██╗██║   ██║██╔════╝██║
		 * ██║  ███╗██████╔╝███████║██║   ██║█████╗  ██║
		 * ██║   ██║██╔══██╗██╔══██║╚██╗ ██╔╝██╔══╝  ██║
		 * ╚██████╔╝██║  ██║██║  ██║ ╚████╔╝ ███████╗███████╗
		 *  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 0f);
			Actions.move(archer, "P", 0);
		}, 50);
		Utils.scheduleTask(() -> Actions.dropItem(archer, true), 51); // blow up crypt | arrows land in 4 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 45.1f, 4.2f);
			Actions.move(archer, "N", 21);
		}, 52);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 53); // etherwarp into room
		Utils.scheduleTask(() -> Actions.turnHead(archer, -44f, 8.5f), 54);
		Utils.scheduleTask(() -> {
			Actions.rightClick(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Crypt 1/5");
		}, 55); // reposition for secret
		Utils.scheduleTask(() -> Actions.turnHead(archer, -106f, 3.3f), 56);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 57); // etherwarp to secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -90f, 7f);
			Actions.setHotbarSlot(archer, 5);
		}, 58);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 59);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 1);
			Actions.leftClick(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 1/6 (Opened Chest)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.CHEST);
		}, 60);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 28.7f, -56f), 61);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 62); // etherwarp up
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -99.7f, -53f);
			Actions.setHotbarSlot(archer, 0);
		}, 63);
		Utils.scheduleTask(() -> {
			Actions.rightClick(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 2/6 (Killed Bat)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.BAT);
		}, 64); // wither impact, kill bat
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 74f, 57.5f);
			Actions.setHotbarSlot(archer, 1);
		}, 65);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 66); // etherwarp down
		Utils.scheduleTask(() -> Actions.turnHead(archer, -86.5f, 2.7f), 67);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 68); // etherwarp to miniboss
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 0f);
			Actions.setHotbarSlot(archer, 4);
		}, 69);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 70); // shoot and kill miniboss | arrows kill in 1 tick
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -85f, 2.25f);
			Actions.setHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel Cleared");
			// archer teleports away before the server can register that they were there to pick up the blessing, so auto-pickup it is
			Utils.scheduleTask(() -> {
				Utils.broadcastBlessing(archer, Utils.BlessingType.LIFE, 5);
				Utils.playLocalSound(archer, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
			}, 200);
		}, 71);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 72); // etherwarp towards quiz
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 0f), 73);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 74); // aotv into quiz
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, -12f);
			Actions.setHotbarSlot(archer, 7);
			Server.Quiz.oruoMessage("I am " + ChatColor.DARK_RED + "Oruo the Omniscient" + ChatColor.WHITE + ".  I have lived many lives.  I have learned all there is to know.");
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
			Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
		}, 75);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 76); // pearl to dino | lands in 25 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, 2.2f);
			Actions.setHotbarSlot(archer, 1);
			Actions.move(archer, "N", 2);
		}, 77);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 78); // etherwarp towards secrets
		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, -38.5f), 79);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 80); // etherwarp into area
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 58f), 81);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 82); // etherwarp to item
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -141f, 10f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 3/6 (Picked Up Item)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.ITEM);
		}, 83);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 84); // etherwarp to wither essence
		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 55f), 85);
		Utils.scheduleTask(() -> {
			Actions.leftClick(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 4/6 (Obtained Wither Essence)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.ESSENCE);
		}, 86);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, -23.5f), 87);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 88); // aotv out
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 52f, -25f);
			Actions.move(archer, "N", 2);
		}, 89);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 90); // etherwarp to final secrets
		Utils.scheduleTask(() -> Actions.turnHead(archer, 42f, 50f), 91);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 92); // reposition
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, -90f), 93);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 1), 94);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 95); // aotv up
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 75f), 96);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 97); // aotv down
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, 36f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 5/6 (Picked Up Item)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.ITEM);
		}, 98);
		Utils.scheduleTask(() -> {
			Actions.leftClick(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 6/6 (Obtained Wither Essence)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.ESSENCE);
		}, 99);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, -5.5f);
			Actions.move(archer, "N", 2);
		}, 100); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(archer), 101); // etherwarp into dino
		// Gravel: 52 ticks

		/*
		 * ██████╗ ██╗███╗   ██╗ ██████╗     ██████╗ ██╗ ██████╗     ███████╗██╗████████╗███████╗
		 * ██╔══██╗██║████╗  ██║██╔═══██╗    ██╔══██╗██║██╔════╝     ██╔════╝██║╚══██╔══╝██╔════╝
		 * ██║  ██║██║██╔██╗ ██║██║   ██║    ██║  ██║██║██║  ███╗    ███████╗██║   ██║   █████╗
		 * ██║  ██║██║██║╚██╗██║██║   ██║    ██║  ██║██║██║   ██║    ╚════██║██║   ██║   ██╔══╝
		 * ██████╔╝██║██║ ╚████║╚██████╔╝    ██████╔╝██║╚██████╔╝    ███████║██║   ██║   ███████╗
		 * ╚═════╝ ╚═╝╚═╝  ╚═══╝ ╚═════╝     ╚═════╝ ╚═╝ ╚═════╝     ╚══════╝╚═╝   ╚═╝   ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 101f, 32f);
			Actions.setHotbarSlot(archer, 4);
		}, 102);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 103); // shoot at miniboss | arrows land in 5 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 46f, -50f);
			Actions.setHotbarSlot(archer, 1);
			Actions.move(archer, "N", 2);
		}, 104);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 105); // etherwarp to secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -163f, 49f);
			Actions.setHotbarSlot(archer, 7);
		}, 106);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 107); // throw pearl | lands in 19 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 13f, 9f);
			Actions.setHotbarSlot(archer, 3);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Dino Dig Site Cleared");
			// archer teleports away before the server can register that they were there to pick up the blessing, so auto-pickup it is
			Utils.scheduleTask(() -> {
				Utils.broadcastBlessing(archer, Utils.BlessingType.LIFE, 5);
				Utils.playLocalSound(archer, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
			}, 200);
		}, 108);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 109); // blow up door
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 1), 110);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 111); // aotv to chest
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 13f), 112);
		Utils.scheduleTask(() -> {
			Actions.leftClick(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Dino Dig Site 1/4 (Opened Chest)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.CHEST);
		}, 113);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, 24.8f), 114);
		Utils.scheduleTask(() -> {
			Actions.rightClick(archer);
			Server.Quiz.oruoMessage("Though I sit stationary in this prison that is " + ChatColor.RED + "The Catacombs" + ChatColor.WHITE + ", my knowledge knows no bounds.");
		}, 115); // aotv out of secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 96f, 59f);
			Actions.move(archer, "N", 2);
		}, 116);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 117); // etherwarp down
		Utils.scheduleTask(() -> Actions.turnHead(archer, 18f, 2f), 118);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 119); // aotv to item
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 135.1f, 30.7f);
			Actions.move(archer, "N", 2);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Dino Dig Site 2/4 (Picked Up Item)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.ITEM);
		}, 120);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 121);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -155f, -16f);
			Actions.setHotbarSlot(archer, 5);
		}, 122);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 123);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 1);
			Actions.leftClick(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Dino Dig Site 3/4 (Opened Chest)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.CHEST);
		}, 124);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -157f, 8f);
			Actions.setHotbarSlot(archer, 7);
		}, 125);
		// tick 126: pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(archer), 127); // throw pearl | lands in 6 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -55f, 62f);
			Actions.setHotbarSlot(archer, 1);
			Actions.move(archer, "N", 2);
		}, 128);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 129); // etherwarp towards bat
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -8.2f, 7.3f);
			Actions.setHotbarSlot(archer, 0);
		}, 130);
		Utils.scheduleTask(() -> {
			Actions.rightClick(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Dino Dig Site 4/4 (Killed Bat)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.BAT);
		}, 131); // kill bat
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 1.8f);
			Actions.setHotbarSlot(archer, 1);
			Actions.move(archer, "N", 5);
		}, 132);
		// tick 133: pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(archer), 134); // etherwarp back to gravel
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 1.45f), 135);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 136); // etherwarp to quiz
		// Dino Dig Site: 35 ticks

		/*
		 *  ██████╗ ██╗   ██╗██╗███████╗
		 * ██╔═══██╗██║   ██║██║╚══███╔╝
		 * ██║   ██║██║   ██║██║  ███╔╝
		 * ██║▄▄ ██║██║   ██║██║ ███╔╝
		 * ╚██████╔╝╚██████╔╝██║███████╗
		 *  ╚══▀▀═╝  ╚═════╝ ╚═╝╚══════╝
		 */
		Server.Quiz.run(archer, world);
		// Quiz: 482 ticks | 543 ticks from open | puzzle solved 503 ticks from open

		Utils.scheduleTask(() -> {
			Actions.swapItems(archer, 1, 28);
			Actions.swapItems(archer, 6, 33);
			Actions.swapItems(archer, 7, 34);
			Actions.swapItems(archer, 9, 36);
			Actions.setHotbarSlot(archer, 5);
		}, 619);
		Utils.scheduleTask(() -> {
			if(doContinue) {
				Utils.teleport(archer, new Location(world, 73.5, 221, 14.5));
				maxor(true);
			}
		}, 742);
	}

	public static void maxor(boolean doContinue) {
		Utils.scheduleTask(() -> Actions.turnHead(archer, 13f, 0f), 1);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 2);
		Utils.scheduleTask(() -> Actions.move(archer, "WN", 0), 25);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 49), 38);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 60);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 61);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 133f, 0f), 63);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -52f, 0f), 88);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 161);
		Utils.scheduleTask(() -> Actions.move(archer, "WN", 0), 162);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 22), 173);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 9, 36), 200);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 239);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 128f, 0f), 240);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 24), 241);
		if(doContinue) {
//			Utils.scheduleTask(() -> storm(true), 499);
		}
	}

//	public static void storm(boolean doContinue) {
//		Actions.setHotbarSlot(archer, 4);
//		Utils.scheduleTask(Archer::shoot, 1);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 2);
//		Utils.scheduleTask(Archer::shoot, 6);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 7);
//		Utils.scheduleTask(Archer::shoot, 11);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 12);
//		Utils.scheduleTask(Archer::shoot, 16);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 17);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 90.9f, -9f), 18);
//		Utils.scheduleTask(Archer::shoot, 21);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 22);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 100.8f, -8.2f), 23);
//		Utils.scheduleTask(Archer::shoot, 26);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 27);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 110f, -7.5f), 28);
//		Utils.scheduleTask(Archer::shoot, 31);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 32);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 0f), 33);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, -1.12242), 8), 34);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 163.6f, -4f), 41);
//		Utils.scheduleTask(Archer::shoot, 44);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 45);
//		Utils.scheduleTask(Archer::shoot, 49);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 50);
//		Utils.scheduleTask(Archer::shoot, 54);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 55);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 159f, -4f), 51);
//		Utils.scheduleTask(Archer::shoot, 54);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 55);
//		Utils.scheduleTask(Archer::shoot, 59);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 60);
//		Utils.scheduleTask(Archer::shoot, 64);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 65);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 154f, -4f), 66);
//		Utils.scheduleTask(Archer::shoot, 69);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 70);
//		Utils.scheduleTask(Archer::shoot, 74);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 75);
//		Utils.scheduleTask(Archer::shoot, 79);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 80);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 0f), 81);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.112242, 0, 0), 11), 82);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -120.9f, 22.7f), 93);
//		Utils.scheduleTask(Archer::shoot, 94);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 95);
//		Utils.scheduleTask(Archer::shoot, 99);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 100);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -104.3f, 14.5f), 101);
//		Utils.scheduleTask(Archer::shoot, 104);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 105);
//		Utils.scheduleTask(Archer::shoot, 109);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 110);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -94.8f, 9.9f), 111);
//		Utils.scheduleTask(Archer::shoot, 114);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 115);
//		Utils.scheduleTask(Archer::shoot, 119);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 120);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -85.7f, 30.4f), 121);
//		Utils.scheduleTask(Archer::shoot, 124);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 125);
//		Utils.scheduleTask(Archer::shoot, 129);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 130);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -87.7f, 16.2f), 131);
//		Utils.scheduleTask(Archer::shoot, 134);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 135);
//		Utils.scheduleTask(Archer::shoot, 139);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 140);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -87.7f, 3.7f), 141);
//		Utils.scheduleTask(Archer::shoot, 144);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 145);
//		Utils.scheduleTask(Archer::shoot, 149);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 150);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -55.1f, 24.7f), 151);
//		Utils.scheduleTask(Archer::shoot, 154);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 155);
//		Utils.scheduleTask(Archer::shoot, 159);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 160);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -66.9f, 14.6f), 161);
//		Utils.scheduleTask(Archer::shoot, 164);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 165);
//		Utils.scheduleTask(Archer::shoot, 169);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 170);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -67.3f, 6.3f), 171);
//		Utils.scheduleTask(Archer::shoot, 174);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 175);
//		Utils.scheduleTask(Archer::shoot, 179);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 180);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -27.4f, 18.2f), 181);
//		Utils.scheduleTask(Archer::shoot, 184);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 185);
//		Utils.scheduleTask(Archer::shoot, 189);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 190);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -43.9f, 11.2f), 191);
//		Utils.scheduleTask(Archer::shoot, 194);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 195);
//		Utils.scheduleTask(Archer::shoot, 199);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 200);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -56.7f, 6f), 201);
//		Utils.scheduleTask(Archer::shoot, 204);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 205);
//		Utils.scheduleTask(Archer::shoot, 209);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 210);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -89f, 12f), 211);
//		Utils.scheduleTask(Archer::shoot, 214);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 215);
//		Utils.scheduleTask(Archer::shoot, 219);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 220);
//		Utils.scheduleTask(Archer::shoot, 224);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 225);
//		Utils.scheduleTask(Archer::shoot, 229);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 230);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 231);
//		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 232);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 5), 233);
//		Utils.scheduleTask(() -> {
//			Actions.swapItems(archer, 5, 32);
//			Actions.swapItems(archer, 6, 33);
//		}, 234);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -108.4f, -83.4f), 654);
//		Utils.scheduleTask(() -> Actions.lastBreath(archer, 30), 654);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(archer, 2);
//			Actions.swapItems(archer, 5, 32);
//		}, 684);
//		Utils.scheduleTask(() -> Actions.leap(archer, Tank.get()), 685);
//		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 728);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(archer, 0f, 0f);
//			Actions.setHotbarSlot(archer, 1);
//		}, 729);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 1.12242), 5), 730);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 8f, 82f), 734);
//		Utils.scheduleTask(() -> Actions.bonzo(archer, new Vector(0.2123, 0.5, 1.5107)), 735);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 8f, 0f), 736);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 0f), 747);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.7937, 0, 0.7937), 3), 748);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -6f, 82f), 750);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.1173, 0, 1.1163), 1), 751);
//		Utils.scheduleTask(() -> Actions.bonzo(archer, new Vector(0.1595, 0.5, 1.5172)), 752);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -6f, 0f), 753);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -5.8f, 0f), 763);
//		Utils.scheduleTask(() -> {
//			Actions.jump(archer);
//			Actions.move(archer, new Vector(0.1144, 0, 1.1166), 1);
//			Actions.setHotbarSlot(archer, 5);
//		}, 764);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.0286, 0, 0.279), 8), 765);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 24.6f, 64.9f), 780);
//		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(96, 120, 121)), 781);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 782);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.8634, 0, 0), 1), 783);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 26.1f), 784);
//		Utils.scheduleTask(() -> {
//			Actions.stonk(archer, world.getBlockAt(96, 121, 122));
//			Actions.move(archer, new Vector(0, 0, 1.12242), 5);
//		}, 785);
//		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(96, 120, 122)), 786);
//		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(96, 121, 123)), 787);
//		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(96, 120, 123)), 788);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 0.2806), 11), 790);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 1), 791);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 800);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.12242, 0, 0), 6), 801);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -128f, -19f), 808);
//		if(doContinue) {
//			Utils.scheduleTask(Archer::explosiveShot, 887);
//			Utils.scheduleTask(() -> {
//				Actions.setHotbarSlot(archer, 1);
//				Actions.turnHead(archer, 90f, 0f);
//			}, 888);
//			Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.403, 0, 0), 1), 889);
//			Utils.scheduleTask(() -> goldor(true), 890);
//		}
//	}
//
//	private static void goldor(boolean doContinue) {
//		// rapid fire arrow fires
//		// explosve shot reaches
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 128 50 emerald_block");
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(archer, -8.0f, 7.2f);
//			// first rapid fire arrow reaches
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 128 50 blue_terracotta");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 66 128 50 emerald_block");
//		}, 1);
//		Utils.scheduleTask(() -> {
//			// first terminator arrows reach
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 66 128 50 blue_terracotta");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 126 50 emerald_block");
//		}, 2);
//		Utils.scheduleTask(Archer::shoot, 3);
//		Utils.scheduleTask(() -> {
//			// second rapid fire arrow reaches
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 126 50 blue_terracotta");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 66 126 50 emerald_block");
//		}, 5);
//		Utils.scheduleTask(() -> {
//			// second terminator arrows reach
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 66 126 50 blue_terracotta");
//			Goldor.broadcastTerminalComplete(archer, "device", 1, 7);
//		}, 7);

//		/*
//		 *  ██╗
//		 * ███║
//		 * ╚██║
//		 *  ██║
//		 *  ██║
//		 *  ╚═╝
//		 */
//		Goldor.broadcastTerminalComplete(archer, "gate", 1, 3);
//		Actions.move(archer, new Vector(-0.2806, 0, 0), 13);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 75f, 82f), 12);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.355, 0, 0.363), 1), 13);
//		Utils.scheduleTask(() -> Actions.bonzo(archer, new Vector(-1.4735, 0.5, 0.3948)), 14);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 77.5f, 0f), 15);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 126.6f, 0f), 27);
//		Utils.scheduleTask(() -> {
//			Actions.jump(archer);
//			Actions.move(archer, new Vector(-1.1264, 0, -0.837), 1);
//		}, 28);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.2253, 0, -0.1673), 8), 29);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 37);
//		// jump ends on tick 41
//
//		/*
//		 * ██████╗
//		 * ╚════██╗
//		 *  █████╔╝
//		 * ██╔═══╝
//		 * ███████╗
//		 * ╚══════╝
//		 */
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.403, 0, 0), 7), 54);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.2806, 0, 0), 10), 61);
//		Utils.scheduleTask(() -> Actions.lavaJump(archer, false), 68);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 30f), 69);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, -0.2806), 10), 76);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0.001, -1.403), 3), 89);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(archer);
//			Bukkit.broadcastMessage(ChatColor.BLUE + "Party " + ChatColor.DARK_GRAY + "> " + ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303" + ChatColor.WHITE + ": THIS TERMINAL IS BALDER THAN ME");
//		}, 92);
//		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.BLUE + "Party " + ChatColor.DARK_GRAY + "> " + ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303" + ChatColor.WHITE + ": THIS TERMINAL IS BALDER THAN ME 1/4"), 93);
//		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.BLUE + "Party " + ChatColor.DARK_GRAY + "> " + ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303" + ChatColor.WHITE + ": THIS TERMINAL IS BALDER THAN ME 2/4"), 94);
//		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.BLUE + "Party " + ChatColor.DARK_GRAY + "> " + ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303" + ChatColor.WHITE + ": THIS TERMINAL IS BALDER THAN ME 3/4"), 95);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(archer, "terminal", 3, 8), 96);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 25f), 97);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.2806, 0, 0), 15), 98);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 7), 99);
//		Utils.scheduleTask(() -> Actions.swingHand(archer), 100); // equip phoenix
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 101);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.12242, 0, 0), 1), 113);
//		Utils.scheduleTask(() -> Actions.swingHand(archer), 114);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(archer, "terminal", 6, 8), 115);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 116);
//		Utils.scheduleTask(() -> Actions.leap(archer, Berserk.get()), 117);
//
//		/*
//		 * ██████╗
//		 * ╚════██╗
//		 *  █████╔╝
//		 *  ╚═══██╗
//		 * ██████╔╝
//		 * ╚═════╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -146.3f, 8.6f), 118);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Phoenix Procced!");
//			world.playSound(archer.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1f, 1.6f);
//		}, 120);
//		Utils.scheduleTask(() -> Actions.lavaJump(archer, false), 131);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.1557, 0, -0.2335), 7), 132);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(archer);
//			Goldor.broadcastTerminalComplete(archer, "lever", 1, 7);
//		}, 140);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(archer, 3);
//			Actions.turnHead(archer, 180f, 0f);
//			Actions.clearVelocity(archer);
//		}, 141);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, -0.2806), 10), 142);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(archer);
//			Goldor.broadcastTerminalComplete(archer, "gate", 3, 3);
//		}, 152);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 7), 153);
//		Utils.scheduleTask(() -> Actions.swingHand(archer), 154); // equip black cat
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 1), 155);
//		Utils.scheduleTask(() -> Actions.swapItems(archer, 9, 39), 156);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 85f, 82f), 162);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.3977, 0, 0.1223), 1), 163);
//		final BukkitRunnable[] temp = new BukkitRunnable[1];
//		Utils.scheduleTask(() -> temp[0] = Actions.bonzo(archer, new Vector(-1.5197, 0.5, 0.133)), 164);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 85f, 33f), 165);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(archer);
//			Goldor.broadcastTerminalComplete(archer, "lever", 3, 7);
//		}, 169);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 170);
//		Utils.scheduleTask(() -> {
//			temp[0].cancel();
//			Actions.leap(archer, Mage.get());
//		}, 171);
//
//		/*
//		 * ██╗  ██╗
//		 * ██║  ██║
//		 * ███████║
//		 * ╚════██║
//		 *      ██║
//		 *      ╚═╝
//		 */
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(archer, -148f, 82f);
//			Actions.setHotbarSlot(archer, 1);
//		}, 172);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7435, 0, -1.19), 1), 173);
//		Utils.scheduleTask(() -> Actions.bonzo(archer, new Vector(0.8967, 0.5, -1.2342)), 174);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -148f, 0f), 175);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Bonzo Procced!");
//			world.playSound(archer.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 2f);
//		}, 180);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7435, 0, -1.19), 2), 188);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.1649, 0, -0.227), 6), 190);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 45f), 196);
//		Utils.scheduleTask(() -> Actions.swingHand(archer), 197);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(archer, "terminal", 2, 7), 198);
//		Utils.scheduleTask(() -> world.playSound(archer.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f), 200);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 1.08), 1), 202);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 82f), 203);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.403, 0, 0), 1), 204);
//		Utils.scheduleTask(() -> temp[0] = Actions.bonzo(archer, new Vector(1.52552, 0, 0)), 205);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 0f), 206);
//		Utils.scheduleTask(() -> Actions.swapItems(archer, 9, 39), 207);
//		Utils.scheduleTask(() -> {
//			temp[0].cancel();
//			Actions.lavaJump(archer, false);
//		}, 219);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(archer, -76.5f, -4.9f);
//			world.playSound(archer.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
//		}, 220);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickLever(archer);
//			Goldor.broadcastTerminalComplete(archer, "lever", 5, 7);
//		}, 227);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 3), 228);
//		Utils.scheduleTask(() -> Actions.leap(archer, Mage.get()), 229);
//
//
//		/*
//		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
//		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
//		 * █████╗  ██║██║  ███╗███████║   ██║
//		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
//		 * ██║     ██║╚██████╔╝██║  ██║   ██║
//		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
//		 */
//		Utils.scheduleTask(() -> Actions.swapItems(archer, 5, 32), 230);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 5), 231);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, -1.403), 11), 256);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -82.5f, -10f), 266);
//		// tick 267: swap to gdrag
//		Utils.scheduleTask(() -> Actions.lastBreath(archer, 10), 268);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 279);
//		Utils.scheduleTask(Archer::shoot, 280);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 284);
//		Utils.scheduleTask(Archer::shoot, 285);
//		Utils.scheduleTask(() -> Actions.salvation(archer), 289);
//		Utils.scheduleTask(Archer::shoot, 290);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 291);
//		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 292);
//		if(doContinue) {
//			Utils.scheduleTask(() -> necron(true), 350);
//		}
//	}

//	private static void necron(boolean doContinue) {
//		Actions.setHotbarSlot(archer, 2);
//		Utils.scheduleTask(() -> Actions.swapItems(archer, 3, 30), 1);
//		Utils.scheduleTask(() -> Actions.leap(archer, Tank.get()), 121);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 5), 122);
//		Utils.scheduleTask(() -> Actions.lastBreath(archer, 36), 123);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 160);
//		for(int i = 161; i < 368; i += 5) {
//			Utils.scheduleTask(Archer::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(archer), i + 4);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 3), 368);
//		Utils.scheduleTask(() -> Actions.swingHand(archer), 369);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 410);
//		for(int i = 411; i < 507; i += 5) {
//			Utils.scheduleTask(Archer::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(archer), i + 4);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 507);
//		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 508);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 121.3f, 0f), 509);
//		// tick 510: equip black cat
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.198, 0, -0.729), 20), 511);
//		Utils.scheduleTask(() -> Actions.jump(archer), 530);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.2398, 0, -0.1458), 9), 531);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.198, 0, -0.729), 2), 540);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 65.6f, 29.3f), 541);
//		Utils.scheduleTask(() -> {
//			Actions.swapItems(archer, 5, 32);
//			Actions.swapItems(archer, 11, 39);
//		}, 542);
//		if(doContinue) {
//			Utils.scheduleTask(Archer::witherKing, 609);
//		}
//	}
//
//	private static void witherKing() {
//		Utils.scheduleTask(() -> WitherKing.pickUpRelic(archer), 1);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -115f, 0f), 2);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.2716, 0, -0.5929), 6), 3);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.2543, 0, -0.1186), 5), 9);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.2716, 0, -0.5929), 15), 14);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 146.1f, 0f), 28);
//		Utils.scheduleTask(() -> WitherKing.placeRelic(archer), 29);
//		// tick 30: equip greg
//		Utils.scheduleTask(() -> Actions.swapItems(archer, 11, 39), 31);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(archer, 36f, 0f);
//			Actions.setHotbarSlot(archer, 6);
//		}, 32);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.6597, 0, 0.9081), 1), 33);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.1649, 0, 0.227), 5), 34);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.6597, 0, 0.9081), 31), 39);
//		Utils.scheduleTask(() -> Actions.jump(archer), 69);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.1649, 0, 0.227), 9), 70);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.6597, 0, 0.9081), 4), 79);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -37.5f, -16f), 82);
//		Utils.scheduleTask(() -> Actions.rag(archer), 169);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 230);
//		// arrows take 13 ticks to reach the Dragon
//		// rag on tick 169
//		// rag is activated on tick 229
//		// start shooting tick 368
//		// begin moving tick 388
//		// Dragon spawns tick 401
//		// last arrow fired tick 428
//		// rag wears off tick 429
//		// rag is back tick 569
//		for(int i = 368; i < 430; i += 5) {
//			Utils.scheduleTask(Archer::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(archer), i + 1);
//		}
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7033, 0, 0.8747), 27), 388);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -37.5f, -18.5f), 395);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -37.5f, -22f), 405);
//		Utils.scheduleTask(() -> Actions.jump(archer), 414);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(archer, -37.5f, -28f);
//			Actions.move(archer, new Vector(0.1758, 0, 0.2187), 9);
//		}, 415);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7033, 0, 0.8747), 6), 424);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(archer, 177.6f, 0f);
//			WitherKing.playDragonDeathSound(true);
//			Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Soul Dragon " + ChatColor.GREEN + "killed in 30 ticks (1.50 seconds) | Wither King: 431 ticks (21.55 seconds) | Overall: 3 806 ticks (190.30 seconds)");
//		}, 433);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.047, 0, -1.121), 8), 434);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.0118, 0, -0.2804), 5), 442);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.047, 0, -1.121), 9), 447);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.0118, 0, -0.2804), 5), 456);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -134.6f, -17f), 461);
//		// arrows take 13 ticks to reach the Dragon
//		// start shooting tick 718
//		// begin moving tick 728
//		// last arrow fired tick 738
//		// Dragon spawns tick 741
//		for(int i = 718; i < 740; i += 5) {
//			Utils.scheduleTask(Archer::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(archer), i + 1);
//		}
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7992, 0, -0.788), 10), 728);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -134.6f, -20f), 735);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(archer, -151.4f, 0f);
//			Actions.setHotbarSlot(archer, 6);
//		}, 739);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.5373, 0, -0.9855), 35), 740);
//		Utils.scheduleTask(() -> Actions.rag(archer), 750);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 80.7f, -17f), 775);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 811);
//		// arrows take 13 ticks to reach the Dragon
//		// start shooting tick 814
//		// begin moving tick 834
//		// last arrow fired tick 844
//		// Dragon spawns tick 847
//		Utils.scheduleTask(() -> rapidFire(201), 814);
//		for(int i = 814; i < 850; i += 5) {
//			Utils.scheduleTask(Archer::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(archer), i + 1);
//		}
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.1077, 0, 0.1814), 10), 834);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 80.7f, -20f), 840);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, -18f, 0f), 851);
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.3469, 0, 1.0675), 23), 852);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 70.6f, -17f), 875);
//		// arrows take 13 ticks to reach the Dragon
//		// start shooting tick 920
//		// begin moving tick 940
//		// last arrow fired tick 950
//		// Dragon spawns tick 953
//		for(int i = 920; i < 955; i += 5) {
//			Utils.scheduleTask(Archer::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(archer), i + 1);
//		}
//		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.0587, 0, 0.373), 10), 940);
//		Utils.scheduleTask(() -> Actions.turnHead(archer, 70.6f, -20f), 950);
//	}
//
//	private static void shoot() {
//		Actions.rightClickOld(archer);
//		Location l = archer.getEyeLocation();
//		l.add(l.getDirection());
//		int power = archer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.POWER);
//		int strength;
//		try {
//			strength = Objects.requireNonNull(archer.getPotionEffect(PotionEffectType.STRENGTH)).getAmplifier();
//		} catch(Exception exception) {
//			strength = 0;
//		}
//		double powerBonus;
//		try {
//			powerBonus = power * 0.05;
//			if(power == 7) {
//				powerBonus += 0.05;
//			}
//		} catch(Exception exception) {
//			powerBonus = 0;
//		}
//
//		double strengthBonus;
//		try {
//			strengthBonus = 0.15 + 0.15 * strength;
//		} catch(Exception exception) {
//			strengthBonus = 0;
//		}
//
//		double add = powerBonus + strengthBonus;
//
//		// Duplex Arrow
//		double finalAdd = add;
//		Utils.scheduleTask(() -> {
//			Arrow arrow = world.spawnArrow(l, l.getDirection(), 4, 0);
//			arrow.setDamage(0.5 + finalAdd);
//			arrow.setPierceLevel(4);
//			arrow.setShooter(archer);
//			arrow.setWeapon(archer.getInventory().getItemInMainHand());
//			arrow.addScoreboardTag("TerminatorArrow");
//		}, 3);
//
//		// Archer Bonus Arrows
//		add *= 5;
//
//		double finalAdd1 = add;
//		Utils.scheduleTask(() -> {
//			Arrow arrow = world.spawnArrow(l, l.getDirection(), 4, 0);
//			arrow.setDamage(2.5 + finalAdd1);
//			arrow.setPierceLevel(4);
//			arrow.setShooter(archer);
//			arrow.setWeapon(archer.getInventory().getItemInMainHand());
//			arrow.addScoreboardTag("TerminatorArrow");
//		}, 5);
//
//		double finalAdd2 = add;
//		Utils.scheduleTask(() -> {
//			Arrow arrow = world.spawnArrow(l, l.getDirection(), 4, 0);
//			arrow.setDamage(2.5 + finalAdd2);
//			arrow.setPierceLevel(4);
//			arrow.setShooter(archer);
//			arrow.setWeapon(archer.getInventory().getItemInMainHand());
//			arrow.addScoreboardTag("TerminatorArrow");
//		}, 10);
//	}
//

	public static Player get() {
		return archer;
	}
}
