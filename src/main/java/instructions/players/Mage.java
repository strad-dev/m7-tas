package instructions.players;

import instructions.Actions;
import instructions.Server;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

public class Mage {
	private static Player mage;
	private static World world;

	public static void mageInstructions(Player p, String section) {
		mage = p;
		world = Mage.mage.getWorld();

		switch(section) {
			case "all", "clear" -> {
				Utils.teleport(mage, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f));
				Utils.scheduleTask(() -> preClear(section.equals("all")), 60);
			}
//			case "maxor", "boss" -> {
//				Utils.teleport(mage, new Location(world, 73.5, 221, 14.5, 0f, 0f));
//				Actions.swapItems(mage, 9, 36);
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 3, 30);
//				Actions.swapItems(mage, 5, 32);
//				if(section.equals("maxor")) {
//					Utils.scheduleTask(() -> maxor(false), 60);
//				} else {
//					Utils.scheduleTask(() -> maxor(true), 60);
//				}
//			}
//			case "storm" -> {
//				Utils.teleport(mage, new Location(world, 46.576, 169, 49.503, 1.4f, 22.4f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 3, 30);
//				Actions.swapItems(mage, 5, 32);
//				Utils.scheduleTask(() -> storm(false), 60);
//			}
//			case "goldor" -> {
//				Utils.teleport(mage, new Location(world, 108.308, 120, 94.675, -139.3f, 1.6f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 3, 30);
//				Actions.swapItems(mage, 6, 33);
////				Utils.scheduleTask(() -> goldor(false), 60);
//			}
//			case "necron" -> {
//				Utils.teleport(mage, new Location(world, 56.488, 64, 111.700, -180f, 0f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 3, 30);
//				Actions.swapItems(mage, 5, 32);
//				Actions.swapItems(mage, 6, 33);
//				Utils.scheduleTask(() -> necron(false), 60);
//			}
//			case "witherking" -> {
//				Utils.teleport(mage, new Location(world, 89.7, 6, 94.406, -75.6f, 18.8f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 5, 32);
//				Actions.swapItems(mage, 6, 33);
//				Utils.scheduleTask(Mage::witherKing, 60);
//			}
		}
	}

	private static void preClear(boolean doContinue) {
		Actions.turnHead(mage, 180f, -90f);
		Actions.swapItems(mage, 2, 29);
		Actions.setHotbarSlot(mage, 7);
		Utils.scheduleTask(() -> Actions.move(mage, "WP", 38), 1); // move to pearl spot
		Utils.scheduleTask(() -> Actions.rightClick(mage), 39); // lands in 10 ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(mage, 1);
			Actions.move(mage, "N", 0);
		}, 40);
		// dodge tick 40 teleport
		Utils.scheduleTask(() -> Actions.rightClick(mage), 49); // etherwarp to top
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 0.254f), 50);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 51); // etherwarp forward
		Utils.scheduleTask(() -> Actions.turnHead(mage, 1.3f, 0.6f), 52);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 53); // etherwarp onto first checkmark
		Utils.scheduleTask(() -> Actions.turnHead(mage, -25.9f, 3.55f), 54);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 55); // etherwarp to edge of blood
		Utils.scheduleTask(() -> Actions.move(mage, "WP", 1), 56); // fall into void to facilitate pearls
		Utils.scheduleTask(() -> Actions.turnHead(mage, 45f, 90f), 57);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 58);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 59);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 60); // tp down fast
		Utils.scheduleTask(() -> Actions.turnHead(mage, 35.1f, 20f), 61);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 62); // reposition
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, -90f);
			Actions.setHotbarSlot(mage, 7);
		}, 63);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 64); // throw pearl to enter bedrock, lands in 5 ticks
		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 2), 65);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 69); // activate tac, procs in 60 ticks (129)
		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 7), 70);
		// tick 80: get teleported back
		Utils.scheduleTask(() -> Utils.teleport(mage, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f)), 80);
		// tick 120: dodged
		// tick 128: run starts
		Utils.scheduleTask(() -> clear(doContinue), 128);
	}

	private static void clear(boolean doContinue) {
		/*
		 * ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗███╗   ███╗██╗████████╗███████╗
		 * ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║████╗ ████║██║╚══██╔══╝██╔════╝
		 * ██║  ██║█████╗  ███████║   ██║   ███████║██╔████╔██║██║   ██║   █████╗
		 * ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║██║╚██╔╝██║██║   ██║   ██╔══╝
		 * ██████╔╝███████╗██║  ██║   ██║   ██║  ██║██║ ╚═╝ ██║██║   ██║   ███████╗
		 * ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚══════╝
		 */
		// tick 1: tac TP back
		Utils.scheduleTask(() -> Actions.rightClick(mage), 2);
		// tick 3: blood opens
		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 4);
		Utils.scheduleTask(() -> Actions.leap(mage, Tank.get()), 50);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -65f, 3f);
			Actions.setHotbarSlot(mage, 7);
		}, 51);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 52); // pearl to next secret | lands in 8 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 72f, -3f);
			Actions.setHotbarSlot(mage, 1);
		}, 53);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 54); // aotv to secret
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 0f), 55);
		Utils.scheduleTask(() -> {
			Actions.move(mage, "WP", 3);
			Actions.dropItem(mage, true);
		}, 56); // blow up wall with Guided Sheep | cooldown for 300 ticks
		Utils.scheduleTask(() -> {
			Actions.swapItems(mage, 4, 31);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 1/6 (Obtained Item)");
			Utils.playSecretFoundSound(mage, Utils.SecretType.ITEM);
		}, 59);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 162f, -66.2f);
			Actions.setHotbarSlot(mage, 7);
		}, 60); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(mage), 61); // pearl to top | lands in 11 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 90f);
			Actions.setHotbarSlot(mage, 5);
		}, 62);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 63); // stonk down
		Utils.scheduleTask(() -> Actions.leftClick(mage), 64);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 65);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 1), 66);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 67); // aotv down
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 2/6 (Opened Chest)");
			Utils.broadcastBlessing(mage, Utils.BlessingType.LIFE, 2);
			Utils.playSecretFoundSound(mage, Utils.SecretType.BLESSING_CHEST);
		}, 68);
		// tick 72: pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(mage), 73); // reposition
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 74);
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 3/6 (Opened Chest)");
			Utils.broadcastBlessing(mage, Utils.BlessingType.LIFE, 2);
			Utils.playSecretFoundSound(mage, Utils.SecretType.BLESSING_CHEST);
		}, 75);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 110.2f, 0.97f);
			Actions.move(mage, "N", 2);
		}, 76);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 77); // etherwarp to next secret
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(mage, 7);
			Actions.turnHead(mage, -105.5f, 4.5f);
		}, 78);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 79); // pearl towards crypts | lands in 7 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -138f, -60f);
			Actions.setHotbarSlot(mage, 1);
			Actions.move(mage, "N", 2);
		}, 80);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 81); // etherwarp to top compartment
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -180f, 90f);
			Actions.setHotbarSlot(mage, 0);
		}, 82);
		Utils.scheduleTask(() -> {
			Actions.rightClick(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 4/6 (Killed Bat)");
			Utils.playSecretFoundSound(mage, Utils.SecretType.BAT);
		}, 83); // kill bat
		Utils.scheduleTask(() -> Actions.turnHead(mage, -180f, 30f), 84);
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 5/6 (Opened Chest)");
			Utils.broadcastBlessing(mage, Utils.BlessingType.LIFE, 2);
			Utils.playSecretFoundSound(mage, Utils.SecretType.BLESSING_CHEST);
		}, 85);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -90f, -6.5f);
			Actions.setHotbarSlot(mage, 1);
			Actions.move(mage, "N", 2);
		}, 86); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(mage), 87); // etherwarp to crypts
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 30f), 88);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 89); // aotv to correct spot
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -125f, 10f);
			Actions.setHotbarSlot(mage, 4);
		}, 90);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 91); // blow up crypt #1
		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 92);
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Crypt 2/5");
		}, 93);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 120f, 10f);
			Actions.setHotbarSlot(mage, 4);
		}, 94);
		// infinityboom technically has a 20-tick cooldown with dupe mage
		// but let's assume normal superboom is being used (which has no cooldown)
		Utils.scheduleTask(() -> Actions.rightClick(mage), 95); // blow up crypt #2
		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 96);
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Crypt 3/5");
		}, 98); // mage beam cooldown is 5 ticks with max attack speed
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -5f, 16f);
			Actions.setHotbarSlot(mage, 1);
		}, 98);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 99); // aotv towards secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -110f, 67f);
			Actions.move(mage, "N", 2);
		}, 100);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 101); // etherwarp to gate
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 60f, -20f);
			Actions.setHotbarSlot(mage, 7);
		}, 102);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 103); // throw pearl back towards blood | lands in 13 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -90f, 36f);
			Actions.setHotbarSlot(mage, 5);
		}, 104);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 105);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 106); // stonk gate
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -90f, -12f);
			Actions.setHotbarSlot(mage, 1);
			Actions.move(mage, "N", 0);
		}, 107);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 108); // etherwarp towards chest
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -90f, 18f);
			Actions.setHotbarSlot(mage, 3);
			Actions.move(mage, "WP", 1);
		}, 109); // walk forward to reposition
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Actions.mimicChest(mage, world.getBlockAt(-54, 69, -89));
		}, 110);
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Mimic Killed!");
		}, 111); // kill mimic
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 68f, 11.5f);
			Actions.setHotbarSlot(mage, 1);
			Actions.move(mage, "N", 6);
		}, 116); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(mage), 117); // etherwarp back towards blood
		Utils.scheduleTask(() -> Actions.turnHead(mage, 124f, 3f), 118);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 119); // etherwarp opposite of blood (positioning)
		Utils.scheduleTask(() -> Actions.turnHead(mage, -2f, 3.5f), 120);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 121); // etherwarp to blood
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 0f);
			Actions.setHotbarSlot(mage, 3);
		}, 122);
		// Deathmite: 72 ticks

		/*
		 * ██████╗ ██╗      ██████╗  ██████╗ ██████╗      ██████╗ █████╗ ███╗   ███╗██████╗
		 * ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔════╝██╔══██╗████╗ ████║██╔══██╗
		 * ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██║     ███████║██╔████╔██║██████╔╝
		 * ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██║     ██╔══██║██║╚██╔╝██║██╔═══╝
		 * ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ╚██████╗██║  ██║██║ ╚═╝ ██║██║
		 * ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝      ╚═════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Server.openBloodDoor();
		}, 353);
		Utils.scheduleTask(() -> snapHead("Diamante Giant"), 374);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 375);
		Utils.scheduleTask(() -> snapHead("Bonzo"), 376);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 380);
		Utils.scheduleTask(() -> snapHead("Nucleararmadillo"), 381);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 385);
		Utils.scheduleTask(() -> snapHead("Jamie_2013"), 386);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 390);
		Utils.scheduleTask(() -> {
			Actions.move(mage, "WP", 10);
			Actions.turnHead(mage, 0f, -31f);
		}, 391);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 433);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 450);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 475);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 489);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 506);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 519);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 532);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 545);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 562);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 578);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 603);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 617);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 634);
		Utils.scheduleTask(() -> Actions.leftClick(mage), 647);
		Utils.scheduleTask(() -> {
			Actions.leftClick(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Blood Camp Finished in 661 Ticks (33.05 seconds)");
		}, 661);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 3.3f);
			Actions.setHotbarSlot(mage, 1);
			Actions.move(mage, "N", 2);
		}, 662);
		Utils.scheduleTask(() -> Actions.rightClick(mage), 663); // etherwarp to portal
		Utils.scheduleTask(() -> {
			Actions.swapItems(mage, 1, 28);
			Actions.swapItems(mage, 3, 30);
			Actions.swapItems(mage, 6, 33);
			Actions.swapItems(mage, 7, 34);
			Actions.setHotbarSlot(mage, 5);
		}, 664);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Entered Boss in 742 Ticks (37.10 seconds)");
			// these are not picked up as boss is entered immediately
			Utils.scheduleTask(() -> {
				Utils.broadcastBlessing(mage, Utils.BlessingType.POWER, 5);
				Utils.broadcastBlessing(mage, Utils.BlessingType.LIFE, 5);
			}, 200);
			if(doContinue) {
//				Utils.teleport(mage, new Location(world, 73.5, 221, 14.5));
//				maxor(true);
			}
		}, 742);
	}

	//
//	public static void maxor(boolean doContinue) {
//		Actions.setHotbarSlot(mage, 5);
//		Actions.move(mage, new Vector(0.22, 0, 1.1), 28);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -11.31f, 0f), 1);
//		Utils.scheduleTask(() -> {
//			Actions.move(mage, new Vector(0.051, 0, 0.255), 16);
//			Actions.springBoots(mage);
//		}, 28);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -33f, 0f), 45);
//		Utils.scheduleTask(() -> {
//			Maxor.pickUpCrystal(mage);
//			Bukkit.broadcastMessage(ChatColor.GOLD + "Beethoven_" + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!");
//		}, 56);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1403, 0, 0.243), 2), 57);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 58);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2806, 0, 0), 16), 59);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -131.2925f, 0f), 75);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 79);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2108, 0, -0.1852), 19), 81);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 100);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1954, 0, -0.1716), 1), 101);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 48.7075f, 0f), 102);
//		Utils.scheduleTask(() -> {
//			Maxor.placeCrystal(mage);
//			Actions.move(mage, new Vector(-0.1954, 0, 0.1716), 16);
//			Actions.springBoots(mage);
//		}, 160);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.2108, 0, 0.1852), 27), 177);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.0488, 0, 0.0429), 1), 206);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 9, 36), 207);
//		Utils.scheduleTask(() -> {
//			Maxor.pickUpCrystal(mage);
//			Bukkit.broadcastMessage(ChatColor.GOLD + "Beethoven_" + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!");
//		}, 239);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -131.2925f, 0f), 240);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 241);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2108, 0, -0.1852), 19), 242);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 261);
//		Utils.scheduleTask(() -> Maxor.placeCrystal(mage), 262);
//		Utils.scheduleTask(() -> Actions.rag(mage), 263);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 324);
//		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 325);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 326);
//		Utils.scheduleTask(Mage::mageBeam, 399);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 400);
//		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 401);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, 0f), 402);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-1.12242, 0, 0), 11), 403);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 1.4f, 22.4f);
//			Actions.setHotbarSlot(mage, 6);
//		}, 414);
//		if(doContinue) {
//			Utils.scheduleTask(() -> storm(true), 499);
//		}
//	}
//
//	public static void storm(boolean doContinue) {
//		Actions.setHotbarSlot(mage, 6);
//		Utils.scheduleTask(() -> Actions.gyro(mage, new Location(world, 46.5, 169, 53.5)), 1); // gyro will be up in 7.5 seconds (150 ticks)
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -105.7f, -19.8f);
//			Actions.setHotbarSlot(mage, 3);
//		}, 2);
//		Utils.scheduleTask(Mage::mageBeam, 8);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -135.4f, -18.2f), 9);
//		Utils.scheduleTask(Mage::mageBeam, 13);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 135f, -18.5f), 14);
//		Utils.scheduleTask(Mage::mageBeam, 18);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 106.3f, -20.8f), 19);
//		Utils.scheduleTask(Mage::mageBeam, 23);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 50f, -17.2f), 24);
//		Utils.scheduleTask(Mage::mageBeam, 28);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 32.5f, -14.2f), 29);
//		Utils.scheduleTask(Mage::mageBeam, 33);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -32.1f, -14.2f), 34);
//		Utils.scheduleTask(Mage::mageBeam, 38);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -49.2f, -17.8f), 39);
//		Utils.scheduleTask(Mage::mageBeam, 43);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 44);
//		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 45);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -106.4f, -21.8f);
//			Actions.setHotbarSlot(mage, 3);
//		}, 46);
//		Utils.scheduleTask(Mage::mageBeam, 48);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -135.1f, -18.2f), 49);
//		Utils.scheduleTask(Mage::mageBeam, 53);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 135.6f, -18.2f), 54);
//		Utils.scheduleTask(Mage::mageBeam, 58);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 106f, -22f), 59);
//		Utils.scheduleTask(Mage::mageBeam, 63);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 50f, -17.8f), 64);
//		Utils.scheduleTask(Mage::mageBeam, 68);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 32.4f, -14.2f), 69);
//		Utils.scheduleTask(Mage::mageBeam, 73);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -32.5f, -13.9f), 74);
//		Utils.scheduleTask(Mage::mageBeam, 78);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -49.9f, -17.5f), 79);
//		Utils.scheduleTask(Mage::mageBeam, 83);
//		for(int tick = 85; tick <= 200; tick += 5) {
//			Utils.scheduleTask(() -> {
//				List<Entity> nearbyEntities = mage.getNearbyEntities(10, 10, 10);
//
//				for(Entity entity : nearbyEntities) {
//					if(entity instanceof WitherSkeleton) {
//						Location healerLoc = mage.getLocation();
//						Location witherLoc = entity.getLocation();
//
//						double deltaX = witherLoc.getX() - healerLoc.getX();
//						double deltaY = witherLoc.getY() - healerLoc.getY();
//						double deltaZ = witherLoc.getZ() - healerLoc.getZ();
//
//						float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
//						float pitch = (float) -(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * 180.0 / Math.PI);
//
//						Actions.turnHead(mage, yaw, pitch);
//
//						Utils.scheduleTask(Mage::mageBeam, 1);
//
//						break;
//					}
//				}
//			}, tick);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 202);
//		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 203);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 5), 204);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 6, 33), 205);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -172.9f, -42.5f), 544);
//		Utils.scheduleTask(() -> {
//			Mage.mageBeam();
//			Actions.move(mage, new Vector(-0.1067, 0, 0.8568), 7);
//		}, 545);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -172.9f, -35.6f), 549);
//		Utils.scheduleTask(Mage::mageBeam, 550);
//		Utils.scheduleTask(Mage::mageBeam, 555);
//		Utils.scheduleTask(Mage::mageBeam, 560);
//		Utils.scheduleTask(Mage::mageBeam, 565);
//		Utils.scheduleTask(Mage::mageBeam, 570);
//		Utils.scheduleTask(Mage::mageBeam, 575);
//		Utils.scheduleTask(Mage::mageBeam, 580);
//		Utils.scheduleTask(() -> {
//			Mage.mageBeam();
//			Actions.rag(mage);
//		}, 585);
//		Utils.scheduleTask(Mage::mageBeam, 590);
//		Utils.scheduleTask(Mage::mageBeam, 595);
//		Utils.scheduleTask(Mage::mageBeam, 600);
//		Utils.scheduleTask(Mage::mageBeam, 605);
//		Utils.scheduleTask(Mage::mageBeam, 610);
//		Utils.scheduleTask(Mage::mageBeam, 615);
//		Utils.scheduleTask(Mage::mageBeam, 620);
//		Utils.scheduleTask(Mage::mageBeam, 625);
//		Utils.scheduleTask(Mage::mageBeam, 630);
//		Utils.scheduleTask(Mage::mageBeam, 635);
//		Utils.scheduleTask(Mage::mageBeam, 640);
//		Utils.scheduleTask(Mage::mageBeam, 645);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 6), 650);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 33), 651);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 2), 685);
//		Utils.scheduleTask(() -> Actions.iceSpray(mage), 686);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 687);
//		Utils.scheduleTask(Mage::mageBeam, 688);
//		Utils.scheduleTask(Mage::mageBeam, 693);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 694);
//		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 695);
//		Utils.scheduleTask(() -> {
//			Actions.move(mage, new Vector(-0.8634, 0, 0), 6);
//			Actions.setHotbarSlot(mage, 3);
//			mageBeam();
//		}, 696);
//		Utils.scheduleTask(Mage::mageBeam, 701);
//		Utils.scheduleTask(() -> Actions.jump(mage), 702);
//		Utils.scheduleTask(Mage::mageBeam, 706);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -7f), 707);
//		Utils.scheduleTask(Mage::mageBeam, 711);
//		Utils.scheduleTask(() -> Actions.jump(mage), 714);
//		Utils.scheduleTask(Mage::mageBeam, 716);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -8f), 717);
//		Utils.scheduleTask(Mage::mageBeam, 721);
//		Utils.scheduleTask(() -> {
//			mageBeam();
//			Actions.jump(mage);
//		}, 726);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -9f), 727);
//		Utils.scheduleTask(Mage::mageBeam, 731);
//		Utils.scheduleTask(Mage::mageBeam, 736);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -11f), 737);
//		Utils.scheduleTask(() -> Actions.jump(mage), 738);
//		Utils.scheduleTask(Mage::mageBeam, 741);
//		Utils.scheduleTask(Mage::mageBeam, 746);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -13f), 747);
//		Utils.scheduleTask(() -> Actions.jump(mage), 750);
//		Utils.scheduleTask(Mage::mageBeam, 751);
//		Utils.scheduleTask(Mage::mageBeam, 756);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -15f), 757);
//		Utils.scheduleTask(Mage::mageBeam, 761);
//		Utils.scheduleTask(() -> Actions.jump(mage), 762);
//		Utils.scheduleTask(Mage::mageBeam, 766);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -18f), 767);
//		Utils.scheduleTask(Mage::mageBeam, 771);
//		Utils.scheduleTask(() -> Actions.jump(mage), 774);
//		Utils.scheduleTask(Mage::mageBeam, 776);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -21f), 777);
//		Utils.scheduleTask(Mage::mageBeam, 781);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -24f), 783);
//		Utils.scheduleTask(() -> Actions.jump(mage), 784);
//		Utils.scheduleTask(Mage::mageBeam, 785);
//		Utils.scheduleTask(Mage::mageBeam, 790);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 791);
//		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 792);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 0f), 793);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 0.26), 3), 794);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -139.3f, 1.6f), 797);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 5, 32), 798);
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
//		Actions.setHotbarSlot(mage, 5);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 1);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 2);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 3);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 4);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 5);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 6);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 7);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 8);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 9);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 10);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 11);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 12);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 13);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 14);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 15);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 67.5f, 0f), 16);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-1.282, 0, 0.5707), 2), 17);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 67.5f, 82f);
//			Actions.setHotbarSlot(mage, 1);
//		}, 18);
//		Utils.scheduleTask(() -> Actions.bonzo(mage, new Vector(-1.3936, 0.5, 0.6205)), 19);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 67.5f, 0f), 20);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, 0f), 29);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-1.08, 0, 0), 3), 30);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 65.8f, 15.8f), 33);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(mage);
//			Actions.move(mage, new Vector(0, 0, -1.08), 1);
//		}, 34);
//		Utils.scheduleTask(() -> {
//			Goldor.broadcastTerminalComplete(mage, "terminal", 3, 7);
//			// note: in real hypixel, momentum carries over even after a terminal is opened; as such this is a valid move sequence
//			Actions.move(mage, new Vector(0, 0, -0.2806), 9);
//		}, 35);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 180f, 0f);
//		}, 36);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, 17.1f), 45);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 50);
//		Utils.scheduleTask(() -> {
//			Goldor.broadcastTerminalComplete(mage, "terminal", 7, 7);
//			Bukkit.broadcastMessage(ChatColor.GREEN + "S1 finished in 51 ticks (2.55 seconds) | Overall: 2 467 ticks (123.35 seconds)");
//		}, 51);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 52);
//		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 53);
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
//			Actions.turnHead(mage, -174f, 0f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 58);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1467, 0, -1.395), 1), 59);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.022, 0, -0.2797), 4), 60);
//		Utils.scheduleTask(() -> Actions.lavaJump(mage, false), 67);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.022, 0, -0.2797), 6), 68);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -160f, -10.5f), 73);
//		Utils.scheduleTask(() -> Actions.swingHand(mage), 74);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(mage, "terminal", 2, 8), 75);
//
//		/*
//		 *  ██████╗ ██████╗ ██████╗ ███████╗
//		 * ██╔════╝██╔═══██╗██╔══██╗██╔════╝
//		 * ██║     ██║   ██║██████╔╝█████╗
//		 * ██║     ██║   ██║██╔══██╗██╔══╝
//		 * ╚██████╗╚██████╔╝██║  ██║███████╗
//		 *  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -174f, 0f), 76);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.022, 0, -0.2797), 9), 77);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -180f, 90f), 93);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 123, 122)), 94);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -180f, 35f);
//			Actions.move(mage, new Vector(0, 0, -1.12242), 5);
//		}, 95);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 124, 121)), 96);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 123, 121)), 97);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 124, 120)), 98);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 123, 120)), 99);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 134.99f, 55.7f); // 135f gets the wrong blockface
//			Actions.stonk(mage, world.getBlockAt(58, 124, 119));
//		}, 100);
//		Utils.scheduleTask(() -> {
//			Actions.stonk(mage, world.getBlockAt(58, 123, 119));
//			Actions.move(mage, new Vector(-0.9921, 0, -0.9921), 1);
//		}, 101);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(57, 124, 119)), 102);
//		Utils.scheduleTask(() -> {
//			Actions.stonk(mage, world.getBlockAt(57, 123, 119));
//			Actions.move(mage, new Vector(-0.9921, 0, -0.9921), 1);
//		}, 103);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 177.4f, 35f);
//			Actions.stonk(mage, world.getBlockAt(57, 124, 118));
//		}, 104);
//		Utils.scheduleTask(() -> {
//			Actions.stonk(mage, world.getBlockAt(57, 123, 118));
//			Actions.move(mage, new Vector(-0.0636, 0, -1.4016), 2);
//		}, 105);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 177.4f, 0f), 106);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.01273, 0, -0.2803), 16), 107);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.0636, 0, -1.4016), 33), 123);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 180f, 58f), 156);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 58)), 157);
//		Utils.scheduleTask(() -> {
//			Actions.move(mage, new Vector(0, 0, -1.08), 8);
//			Actions.stonk(mage, world.getBlockAt(54, 114, 57));
//		}, 158);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 56)), 159);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 55)), 160);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 115, 54)), 161);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 54)), 162);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 53)), 163);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 52)), 164);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 51)), 165);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 180f, 0f);
//			Actions.jump(mage);
//		}, 166);
//
//		/*
//		 * ██╗  ██╗
//		 * ██║  ██║
//		 * ███████║
//		 * ╚════██║
//		 *      ██║
//		 *      ╚═╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 66.5f, 0f), 198);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.99, 0, 0.4307), 2), 199);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 60f), 200);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(52, 114, 52)), 201);
//		Utils.scheduleTask(() -> {
//			Actions.move(mage, new Vector(0, 0, 1.08), 5);
//			Actions.stonk(mage, world.getBlockAt(52, 114, 53));
//		}, 202);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(52, 115, 54)), 203);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(52, 114, 54)), 204);
//		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(52, 114, 55)), 205);
//		Utils.scheduleTask(() -> {
//			Actions.jump(mage);
//			Actions.turnHead(mage, -90f, 0f);
//		}, 207);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 5, 32), 208);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 5), 209);
//		Utils.scheduleTask(() -> Actions.rag(mage), 210);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(1.08, 0, 0), 2), 213);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 180f, 0f), 215);
//		// swap to gdrag
//
//		/*
//		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
//		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
//		 * █████╗  ██║██║  ███╗███████║   ██║
//		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
//		 * ██║     ██║╚██████╔╝██║  ██║   ██║
//		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
//		 */
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, -1.403), 11), 256);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 261);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -82.5f, -5f), 262);
//		Utils.scheduleTask(Mage::mageBeam, 280); // wait for debuff
//		Utils.scheduleTask(Mage::mageBeam, 285);
//		Utils.scheduleTask(Mage::mageBeam, 290);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 291);
//		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 292);
//		if(doContinue) {
//			Utils.scheduleTask(() -> necron(true), 350);
//		}
//	}
//
//	private static void necron(boolean doContinue) {
//		Actions.setHotbarSlot(mage, 5);
//		Utils.scheduleTask(() -> Actions.rag(mage), 59);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 120);
//		Utils.scheduleTask(() -> Actions.leap(mage, Tank.get()), 121);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 6), 122);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 36), 123);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 160);
//		for(int i = 161; i < 295; i += 5) {
//			Utils.scheduleTask(Mage::mageBeam, i);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 5), 239);
//		Utils.scheduleTask(() -> Actions.rag(mage), 240);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 301);
//		for(int i = 302; i < 360; i += 5) {
//			Utils.scheduleTask(Mage::mageBeam, i);
//		}
//		for(int i = 368; i < 500; i += 5) {
//			Utils.scheduleTask(Mage::mageBeam, i);
//		}
//		Utils.scheduleTask(Mage::mageBeam, 509);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 510);
//		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 511);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -67f, 0f), 512);
//		// tick 513: equip black cat
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(1.2915, 0, 0.5482), 19), 514);
//		Utils.scheduleTask(() -> Actions.jump(mage), 532);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2583, 0, 0.1096), 9), 533);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(1.2915, 0, 0.5482), 3), 542);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -75.6f, 18.8f), 544);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 3, 30), 545);
//		if(doContinue) {
//			Utils.scheduleTask(Mage::witherKing, 609);
//		}
//	}
//
//	private static void witherKing() {
//		Utils.scheduleTask(() -> WitherKing.pickUpRelic(mage), 1);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 2);
//		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 25);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 74f, 0f);
//			Actions.setHotbarSlot(mage, 8);
//		}, 26);
//		Utils.scheduleTask(() -> WitherKing.placeRelic(mage), 27);
//		// tick 28: equip greg
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -21.2f, 0f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 29);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.406, 0, 1.0465), 4), 30);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1015, 0, 0.2616), 5), 35);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.406, 0, 1.0465), 35), 40);
//		Utils.scheduleTask(() -> Actions.jump(mage), 74);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1015, 0, 0.2616), 9), 75);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.406, 0, 1.0465), 3), 84);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, -90f), 81);
//		Utils.scheduleTask(() -> Actions.rag(mage), 160);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 6), 221);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 350);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 360);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 370);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 380);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 390);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 401);
//		Utils.scheduleTask(Mage::mageBeam, 402);
//		Utils.scheduleTask(Mage::mageBeam, 407);
//		Utils.scheduleTask(Mage::mageBeam, 412);
//		Utils.scheduleTask(Mage::mageBeam, 417);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(mage, 5);
//			Actions.turnHead(mage, -176.5f, 0f);
//		}, 418);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.0685, 0, -1.12), 15), 419);
//		Utils.scheduleTask(() -> Actions.jump(mage), 433);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.0171, 0, -0.2801), 11), 434);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.0685, 0, -1.12), 12), 445);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, -90f), 457);
//		Utils.scheduleTask(() -> Actions.rag(mage), 495);
//		// wears off tick 755
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 6), 556);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 690);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 700);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 710);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 720);
//		Utils.scheduleTask(() -> Actions.lastBreath(mage, 10), 730);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 741);
//		Utils.scheduleTask(Mage::mageBeam, 742);
//		Utils.scheduleTask(() -> {
//			Mage.mageBeam();
//			WitherKing.playDragonDeathSound(true);
//			Bukkit.broadcastMessage(ChatColor.GOLD + "Flame Dragon " + ChatColor.GREEN + "killed in 6 ticks (0.30 seconds) | Wither King: 747 ticks (37.35 seconds) | Overall: 4 122 ticks (206.10 seconds)");
//		}, 747);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 91f, 0f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 748);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-1.1223, 0, -0.0196), 6), 749);
//		Utils.scheduleTask(() -> Actions.rag(mage), 750);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.2805, 0, -0.049), 5), 755);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-1.1223, 0, -0.0196), 38), 760);
//		Utils.scheduleTask(() -> Actions.jump(mage), 797);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.2805, 0, -0.049), 5), 798);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -135f, -90f), 803);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 811);
//		Utils.scheduleTask(Mage::mageBeam, 848);
//		Utils.scheduleTask(() -> {
//			Mage.mageBeam();
//			WitherKing.playDragonDeathSound(true);
//			Bukkit.broadcastMessage(ChatColor.RED + "Power Dragon " + ChatColor.GREEN + "killed in 6 ticks (0.30 seconds) | Wither King: 853 ticks (42.65 seconds) | Overall: 4 228 ticks (211.40 seconds)");
//		}, 853);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 0f), 854);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 1.12242), 5), 855);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 0.2806), 5), 860);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 1.12242), 10), 865);
//		Utils.scheduleTask(() -> Actions.jump(mage), 874);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 0.2806), 9), 875);
//		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 1.12242), 7), 884);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -180f, -90f), 891);
//		Utils.scheduleTask(Mage::mageBeam, 954);
//		Utils.scheduleTask(() -> {
//			Mage.mageBeam();
//			WitherKing.playDragonDeathSound(false);
//			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Apex Dragon " + ChatColor.GREEN + "killed in 6 ticks (0.30 seconds) | Wither King: 959 ticks (47.95 seconds) | Overall: 4 334 ticks (216.70 seconds)");
//			WitherKing.deathSequence();
//		}, 959);
//	}
//
	private static void snapHead(String target) {
		// Find the nearest mob with the target name
		Entity nearestMob = null;
		double nearestDistance = Double.MAX_VALUE;

		// Search through all nearby entities
		for(Entity entity : mage.getNearbyEntities(32, 32, 32)) { // 50 block search radius
			// Check if entity is a living entity (mob)
			if(entity instanceof LivingEntity) {
				// Check if the entity has a custom name containing the target
				if(entity.getCustomName() != null && entity.getCustomName().toLowerCase().contains(target.toLowerCase())) {
					double distance = mage.getLocation().distance(entity.getLocation());
					if(distance < nearestDistance) {
						nearestDistance = distance;
						nearestMob = entity;
					}
				}
			}
		}

		// If we found a target, turn the player's head to face it
		if(nearestMob != null) {
			Location playerLoc = mage.getEyeLocation();
			Location targetLoc = nearestMob.getLocation().add(0, nearestMob.getHeight() / 2, 0); // Aim at center of mob

			// Calculate the direction vector
			Vector direction = targetLoc.subtract(playerLoc).toVector().normalize();

			// Convert to yaw and pitch
			float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
			float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));

			// Turn the player's head
			Actions.turnHead(mage, yaw, pitch);
		}
	}

	@SuppressWarnings("unused")
	public static Player get() {
		return mage;
	}
}
