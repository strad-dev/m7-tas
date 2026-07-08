// ===== TAS-ONLY: fully disabled for the practice fork (see PUBLIC_SERVER.md). Original in git history (main). =====
//package instructions.players;
//
//import instructions.Actions;
//import instructions.bosses.storm.Storm;
//import org.bukkit.ChatColor;
//import org.bukkit.Location;
//import org.bukkit.World;
//import org.bukkit.entity.Player;
//import plugin.Utils;
//
//public class Mage {
//	private static Player mage;
//	private static World world;
//
//	public static void mageInstructions(Player p, String section) {
//		mage = p;
//		world = Mage.mage.getWorld();
//
//		switch(section) {
//			case "all", "clear" -> {
//				Utils.teleport(mage, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f));
//				Utils.scheduleTask(() -> preClear(section.equals("all")), 60);
//			}
//			case "maxor", "boss" -> {
//				Utils.teleport(mage, new Location(world, 73.5, 221, 14.5, 0f, 0f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 3, 30);
//				Actions.swapItems(mage, 6, 33);
//				Actions.swapItems(mage, 7, 34);
//				Actions.swapItems(mage, 11, 36);
//				Actions.setHotbarSlot(mage, 5);
//				if(section.equals("maxor")) {
//					Utils.scheduleTask(() -> maxor(false), 60);
//				} else {
//					Utils.scheduleTask(() -> maxor(true), 60);
//				}
//			}
//			case "storm" -> {
//				Utils.teleport(mage, new Location(world, 100.504, 169, 53.534, 90f, 0f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 3, 30);
//				Actions.swapItems(mage, 6, 33);
//				Actions.swapItems(mage, 7, 34);
//				Actions.setHotbarSlot(mage, 0);
//				Utils.scheduleTask(() -> storm(false), 60);
//			}
//			case "goldor" -> {
//				Utils.teleport(mage, new Location(world, 108.5, 120, 94.496, -141f, 1f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 3, 30);
//				Actions.swapItems(mage, 6, 33);
//				Actions.swapItems(mage, 7, 34);
//				Actions.swapItems(mage, 12, 39);
//				Actions.setHotbarSlot(mage, 5);
//				Utils.scheduleTask(() -> goldor(false), 60);
//			}
//			case "necron" -> {
//				Utils.teleport(mage, new Location(world, 54.524, 64, 100.707, 180f, -6f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 3, 30);
//				Actions.swapItems(mage, 6, 33);
//				Actions.swapItems(mage, 7, 34);
//				Utils.scheduleTask(() -> necron(false), 60);
//			}
//			case "witherking" -> {
//				Utils.teleport(mage, new Location(world, 89.7, 6, 94.296, -90f, 0f));
//				Actions.swapItems(mage, 1, 28);
//				Actions.swapItems(mage, 6, 33);
//				Actions.swapItems(mage, 7, 34);
//				Actions.swapItems(mage, 13, 39);
//				Actions.setHotbarSlot(mage, 5);
//				Utils.scheduleTask(Mage::witherKing, 60);
//			}
//		}
//	}
//
//	private static void preClear(boolean doContinue) {
//		Actions.turnHead(mage, 180f, -89f);
//		Actions.swapItems(mage, 2, 29);
//		Actions.setHotbarSlot(mage, 7);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 35), 1); // move to pearl spot
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 37); // lands in 10 ticks
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(mage, 1);
//			Actions.move(mage, "N", 0);
//		}, 38);
//		// dodge tick 40 teleport
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 47); // etherwarp to top
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 0.254f), 48);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 49); // etherwarp forward
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 1.3f, 0.6f), 50);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 51); // etherwarp onto first checkmark
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -25.9f, 3.55f), 52);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 53); // etherwarp to edge of blood
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 1), 54); // fall into void to facilitate pearls
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 45f, 90f), 55);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 56);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 57);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 58); // tp down fast
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 35.1f, 20f), 59);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 60); // reposition
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 0f, -90f);
//			Actions.setHotbarSlot(mage, 7);
//		}, 61);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 62); // throw pearl to enter bedrock, lands in 5 ticks
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 2), 63);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 67); // activate tac, procs in 60 ticks (127)
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 7), 68);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 2, 29), 69);
//		// tick 80: get teleported back
//		Utils.scheduleTask(() -> Utils.teleport(mage, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f)), 80);
//		// tick 120: dodged
//		// tick 126: run starts
//		Utils.scheduleTask(() -> clear(doContinue), 126);
//	}
//
//	private static void clear(boolean doContinue) {
//		/*
//		 * ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗███╗   ███╗██╗████████╗███████╗
//		 * ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║████╗ ████║██║╚══██╔══╝██╔════╝
//		 * ██║  ██║█████╗  ███████║   ██║   ███████║██╔████╔██║██║   ██║   █████╗
//		 * ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║██║╚██╔╝██║██║   ██║   ██╔══╝
//		 * ██████╔╝███████╗██║  ██║   ██║   ██║  ██║██║ ╚═╝ ██║██║   ██║   ███████╗
//		 * ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚══════╝
//		 */
//		// tick 1: tac TP back
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 2);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 4);
//		Utils.scheduleTask(() -> Actions.leap(mage, Tank.get()), 50);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -65f, 3f);
//			Actions.setHotbarSlot(mage, 7);
//		}, 51);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 52); // pearl to next secret | lands in 8 ticks
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 72f, -3f);
//			Actions.setHotbarSlot(mage, 1);
//		}, 53);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 54); // aotv to secret
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 0f), 55);
//		Utils.scheduleTask(() -> {
//			Actions.move(mage, "WP", 3);
//			Actions.dropItem(mage, true);
//		}, 56); // blow up wall with Guided Sheep | cooldown for 300 ticks
//		Utils.scheduleTask(() -> {
//			Actions.swapItems(mage, 4, 31);
//			Utils.timer(ChatColor.AQUA + "Mage: Deathmite 1/6 (Obtained Item)");
//			Utils.playSecretFoundSound(mage, Utils.SecretType.ITEM);
//		}, 59);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 162f, -66.2f);
//			Actions.setHotbarSlot(mage, 7);
//		}, 60); // pearl lands
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 61); // pearl to top | lands in 11 ticks
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 0f, 90f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 62);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 63); // stonk down
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 64);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 65);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 1), 66);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 67); // aotv down
//		Utils.scheduleTask(() -> {
//			Actions.leftClick(mage);
//			Utils.timer(ChatColor.AQUA + "Mage: Deathmite 2/6 (Opened Chest)");
//			Utils.broadcastBlessing(mage, Utils.BlessingType.LIFE, 2);
//			Utils.playSecretFoundSound(mage, Utils.SecretType.BLESSING_CHEST);
//		}, 68);
//		// tick 72: pearl lands
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 73); // reposition
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 74);
//		Utils.scheduleTask(() -> {
//			Actions.leftClick(mage);
//			Utils.timer(ChatColor.AQUA + "Mage: Deathmite 3/6 (Opened Chest)");
//			Utils.broadcastBlessing(mage, Utils.BlessingType.LIFE, 2);
//			Utils.playSecretFoundSound(mage, Utils.SecretType.BLESSING_CHEST);
//		}, 75);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 110.2f, 0.97f);
//			Actions.move(mage, "N", 2);
//		}, 76);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 77); // etherwarp to next secret
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(mage, 7);
//			Actions.turnHead(mage, -105.5f, 4.5f);
//		}, 78);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 79); // pearl towards crypts | lands in 7 ticks
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -138f, -60f);
//			Actions.setHotbarSlot(mage, 1);
//			Actions.move(mage, "N", 2);
//		}, 80);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 81); // etherwarp to top compartment
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -180f, 90f);
//			Actions.setHotbarSlot(mage, 0);
//		}, 82);
//		Utils.scheduleTask(() -> {
//			Actions.rightClick(mage);
//			Utils.timer(ChatColor.AQUA + "Mage: Deathmite 4/6 (Killed Bat)");
//			Utils.playSecretFoundSound(mage, Utils.SecretType.BAT);
//		}, 83); // kill bat
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -180f, 30f), 84);
//		Utils.scheduleTask(() -> {
//			Actions.leftClick(mage);
//			Utils.timer(ChatColor.AQUA + "Mage: Deathmite 5/6 (Opened Chest)");
//			Utils.broadcastBlessing(mage, Utils.BlessingType.LIFE, 2);
//			Utils.playSecretFoundSound(mage, Utils.SecretType.BLESSING_CHEST);
//		}, 85);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -90f, -6.5f);
//			Actions.setHotbarSlot(mage, 1);
//			Actions.move(mage, "N", 2);
//		}, 86); // pearl lands
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 87); // etherwarp to crypts
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 30f), 88);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 89); // aotv to correct spot
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -125f, 10f);
//			Actions.setHotbarSlot(mage, 4);
//		}, 90);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 91); // blow up crypt #1
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 92);
//		Utils.scheduleTask(() -> {
//			Actions.leftClick(mage);
//			Utils.timer(ChatColor.AQUA + "Mage: Crypt 2/5");
//		}, 93);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 120f, 10f);
//			Actions.setHotbarSlot(mage, 4);
//		}, 94);
//		// infinityboom technically has a 20-tick cooldown with dupe mage
//		// but let's assume normal superboom is being used (which has no cooldown)
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 95); // blow up crypt #2
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 96);
//		Utils.scheduleTask(() -> {
//			Actions.leftClick(mage);
//			Utils.timer(ChatColor.AQUA + "Mage: Crypt 3/5");
//		}, 98); // mage beam cooldown is 5 ticks with max attack speed
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -5f, 16f);
//			Actions.setHotbarSlot(mage, 1);
//		}, 98);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 99); // aotv towards secret
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -110f, 67f);
//			Actions.move(mage, "N", 2);
//		}, 100);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 101); // etherwarp to gate
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 60f, -20f);
//			Actions.setHotbarSlot(mage, 7);
//		}, 102);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 103); // throw pearl back towards blood | lands in 13 ticks
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -90f, 36f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 104);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 105);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 106); // stonk gate
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -90f, -12f);
//			Actions.setHotbarSlot(mage, 1);
//			Actions.move(mage, "N", 0);
//		}, 107);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 108); // etherwarp towards chest
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -90f, 18f);
//			Actions.setHotbarSlot(mage, 3);
//			Actions.move(mage, "WP", 1);
//		}, 109); // walk forward to reposition
//		Utils.scheduleTask(() -> {
//			Actions.leftClick(mage);
//			Actions.mimicChest(mage, world.getBlockAt(-54, 69, -89));
//		}, 110);
//		Utils.scheduleTask(() -> {
//			Actions.leftClick(mage);
//			Utils.timer(ChatColor.AQUA + "Mage: Mimic Killed!");
//		}, 111); // kill mimic
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 68f, 11.5f);
//			Actions.setHotbarSlot(mage, 1);
//			Actions.move(mage, "N", 6);
//		}, 116); // pearl lands
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 117); // etherwarp back towards blood
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 124f, 3f), 118);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 119); // etherwarp opposite of blood (positioning)
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -2f, 3.5f), 120);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 121); // etherwarp to blood
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 0f, 0f);
//			Actions.setHotbarSlot(mage, 3);
//		}, 122);
//		// Deathmite: 72 ticks
//
//		/*
//		 * ██████╗ ██╗      ██████╗  ██████╗ ██████╗      ██████╗ █████╗ ███╗   ███╗██████╗
//		 * ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔════╝██╔══██╗████╗ ████║██╔══██╗
//		 * ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██║     ███████║██╔████╔██║██████╔╝
//		 * ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██║     ██╔══██║██║╚██╔╝██║██╔═══╝
//		 * ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ╚██████╗██║  ██║██║ ╚═╝ ██║██║
//		 * ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝      ╚═════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝
//		 */
//		// Left-click the blood door to open it — requires the Blood Key (handled by Actions.leftClick's door
//		// detection; the old hard openBloodDoor() call is gone).
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 350);
//		Utils.scheduleTask(() -> Actions.snapHeadAtNearestNamed(mage, "Diamante Giant"), 371);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 372);
//		Utils.scheduleTask(() -> Actions.snapHeadAtNearestNamed(mage, "Bonzo"), 373);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 377);
//		Utils.scheduleTask(() -> Actions.snapHeadAtNearestNamed(mage, "Nucleararmadillo"), 378);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 382);
//		Utils.scheduleTask(() -> Actions.snapHeadAtNearestNamed(mage, "Jamie_2013"), 383);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 387);
//		Utils.scheduleTask(() -> {
//			Actions.move(mage, "WP", 10);
//			Actions.turnHead(mage, 0f, -35f);
//		}, 388);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 430);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 447);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 472);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 486);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 503);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 516);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 529);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 542);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 559);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 575);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 600);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 614);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 631);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 644);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 658);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 0f, 3.5f);
//			Actions.setHotbarSlot(mage, 1);
//			Actions.move(mage, "N", 2);
//		}, 659);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 660); // etherwarp to portal
//		Utils.scheduleTask(() -> {
//			Actions.swapItems(mage, 1, 28);
//			Actions.swapItems(mage, 3, 30);
//			Actions.swapItems(mage, 4, 31);
//			Actions.swapItems(mage, 6, 33);
//			Actions.swapItems(mage, 7, 34);
//			Actions.swapItems(mage, 11, 36);
//			Actions.setHotbarSlot(mage, 5);
//		}, 661);
//		// The "Entered Boss" milestone + blood-room blessings, and the Maxor handoff (teleport to boss spawn +
//		// maxor(true)), are all owned by the Watcher's portal entry — see Watcher.enterPortal / the maxorHandoff
//		// armed in TAS.runTAS.
//	}
//
//	public static void maxor(boolean doContinue) {
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -13f, 0f), 1);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 2);
//		Utils.scheduleTask(() -> Actions.move(mage, "WN", 0), 25); // spring boots to left crystal
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 38);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 60);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 61);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -135f, 0f), 63);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -132f, 45f), 67); // minor repositioning to avoid bonking
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 68); // stonk block to avoid bonking
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -132f, 0f), 69);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 11, 36), 86); // temporarily remove spring boots so that pos readjustment doesnt trigger ability
//		Utils.scheduleTask(() -> Actions.move(mage, "WN", 1), 87);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 52f, 0f), 88);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 11, 36), 89);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 161);
//		Utils.scheduleTask(() -> Actions.move(mage, "WN", 0), 162); // spring boots to left crystal
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 22), 173);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 11, 36), 200);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 240);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -128f, 0f), 241);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 24), 242);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 243);
//		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 339);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -121f, 0f);
//			Actions.setHotbarSlot(mage, 0);
//		}, 340);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 9), 341); // move to storm clear spot
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, 0f), 350);
//		// storm() is now started by Maxor.chainNext (player handoff armed in TAS.runTAS).
//	}
//
//	public static void storm(boolean doContinue) {
//		for(int i = 0; i <= 10; i += 3) {
//			Utils.scheduleTask(() -> Actions.rightClick(mage), i);
//		} // clear platform
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 11);
//		for(int i = 15; i <= 530; i += 5) {
//			Utils.scheduleTask(() -> Actions.snapHeadToNearestEnemy(mage), i - 1);
//			Utils.scheduleTask(() -> Actions.loopLeftClick(mage), i);
//		} // kill outstanding wither skeletons
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 531);
//		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 532);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -26f, 0f);
//			Actions.setHotbarSlot(mage, 1);
//			Actions.swapItems(mage, 5, 32);
//		}, 533);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 546);
//		Utils.scheduleTask(() -> Actions.move(mage, "WPJ", 0), 554);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -26f, 80f), 556);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 557); // bonzo back to pad
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -26f, 0f);
//			Actions.move(mage, "WP", 15);
//			Actions.setHotbarSlot(mage, 3);
//		}, 558);
//		Utils.scheduleTask(() -> Actions.snapHeadAtEntity(mage, Storm.INSTANCE.getBoss()), 574);
//		for(int i = 575; i <= 630; i += 5) {
//			Utils.scheduleTask(() -> Actions.leftClick(mage), i);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 5), 585);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 586); // rag buff
//		Utils.scheduleTask(() -> Actions.move(mage, "S", 0), 631);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 180f, 0f), 637);
//		Utils.scheduleTask(() -> Actions.move(mage, "A", 22), 638);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(mage, 6);
//			Actions.swapItems(mage, 5, 32);
//		}, 647);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 648);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -171.5f, -29f), 649);
//		Utils.scheduleTask(() -> Actions.stopRightClick(mage), 667);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(mage, 3);
//			Actions.snapHeadAtEntity(mage, Storm.INSTANCE.getBoss());
//		}, 679);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 680);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 681);
//		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 682);
//		Utils.scheduleTask(() -> {
//			Actions.move(mage, "S", 29);
//			Actions.setHotbarSlot(mage, 3);
//		}, 683);
//		for(int i = 685; i <= 760; i += 5) {
//			Utils.scheduleTask(() -> Actions.snapHeadAtEntity(mage, Storm.INSTANCE.getBoss()), i - 1);
//			Utils.scheduleTask(() -> Actions.leftClick(mage), i);
//		}
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 11, 36), 734);
//		Utils.scheduleTask(() -> Actions.move(mage, "N", 2), 735);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 11, 36), 738);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(mage, 4);
//			Actions.swapItems(mage, 12, 39); // put on racing helmet -> speed auto-set to 650
//		}, 800);
//		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 811);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 5), 812);
//		// goldor() is now started by Storm.chainNext (player handoff armed in TAS.runTAS).
//	}
//
//	public static void goldor(boolean doContinue) {
//		/*
//		 *  ██╗
//		 * ███║
//		 * ╚██║
//		 *  ██║
//		 *  ██║
//		 *  ╚═╝
//		 */
//		Actions.rightClick(mage);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 1);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 2);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 3);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 4);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 5);
//
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 65f, 80f);
//			Actions.setHotbarSlot(mage, 1);
//		}, 6);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 16), 7);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 8);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 90f, 5f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 16);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 87f, 5f), 22);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 23); // s1 4
//		// tick 24: terminal completes
//
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -180f, 0f), 25);
//		Utils.scheduleTask(() -> Actions.move(mage, "WPD", 0), 26);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 1), 28);
//		Utils.scheduleTask(() -> Actions.move(mage, "WPA", 13), 32);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, 9f), 35);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 44); // s1 3
//		// tick 45: terminal completes
//
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 46);
//		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 48);
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
//			Actions.turnHead(mage, 155f, 0f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 49);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 20), 69);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -85f, 30), 89);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 90); // s2 2
//		// tick 91: terminal completes
//
//		/*
//		 *  ██████╗ ██████╗ ██████╗ ███████╗
//		 * ██╔════╝██╔═══██╗██╔══██╗██╔════╝
//		 * ██║     ██║   ██║██████╔╝█████╗
//		 * ██║     ██║   ██║██╔══██╗██╔══╝
//		 * ╚██████╗╚██████╔╝██║  ██║███████╗
//		 *  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 155f, 25f), 92);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 93);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 94);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 62), 95);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 177.4f, 45f), 96);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 97);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 98);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 99);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 100);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 101);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 102);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 180f, 45f), 147);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 148);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 149);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 180f, 0f), 150);
//
//		// turn head to save a tick on other players having to turn head
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -91f, 0f), 157);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -96f, 80f), 159);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -128f, 10f), 170);
//
//		/*
//		 * ██╗  ██╗
//		 * ██║  ██║
//		 * ███████║
//		 * ╚════██║
//		 *      ██║
//		 *      ╚═╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 0f), 172);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 8), 173);
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
//			Actions.turnHead(mage, 180f, 0f);
//			Actions.setHotbarSlot(mage, 3);
//		}, 200);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 5), 219);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -111f, -2f);
//			Actions.swapItems(mage, 12, 39);
//		}, 223);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 224);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 225);
//		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 245);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 246);
//		Utils.scheduleTask(() -> Actions.move(mage, "WN", 0), 287);
//		Utils.scheduleTask(() -> Actions.move(mage, "N", 10), 297);
//	}
//
//	public static void necron(boolean doContinue) {
//		Actions.setHotbarSlot(mage, 6);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 1);
//		Utils.scheduleTask(() -> Actions.stopRightClick(mage), 152);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 153);
//		for(int i = 155; i <= 500; i += 5) {
//			Utils.scheduleTask(() -> Actions.leftClick(mage), i);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 501);
//		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 502);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 13, 39), 503);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -67f, 0f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 504);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 505);
//		Utils.scheduleTask(() -> Actions.move(mage, "WPJ", 0), 521);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 12), 522);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 534);
//		Utils.scheduleTask(() -> Actions.swapItems(mage, 3, 30), 535);
//	}
//
//	public static void witherKing() {
//		Actions.rightClick(mage);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 4), 1);
//		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 22);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 90f, 0f);
//			Actions.setHotbarSlot(mage, 8);
//		}, 23);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 24);
//		Utils.scheduleTask(() -> Actions.turnHead(mage, -20f, 0f), 25);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 26);
//		Utils.scheduleTask(() -> Actions.move(mage, "WPJ", 0), 59);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 8), 61);
//		Utils.scheduleTask(() -> {
//			Actions.swapItems(mage, 5, 32);
//			Actions.swapItems(mage, 13, 39);
//		}, 69);
//		Utils.scheduleTask(() -> {
//			Actions.setHotbarSlot(mage, 5);
//			Actions.turnHead(mage, 180f, -90f);
//		}, 101);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 200);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 6), 261);
//		int lbFor = 10;
//		for(int i = 290; i <= 386 - lbFor - 1; i += lbFor + 1) {
//			Utils.scheduleTask(() -> Actions.rightClick(mage), i);
//			Utils.scheduleTask(() -> Actions.stopRightClick(mage), i + lbFor);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 387);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 388);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 393);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -177.5f, -90f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 394);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 395);
//		Utils.scheduleTask(() -> Actions.move(mage, "WPJ", 0), 411);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 21), 413);
//		Utils.scheduleTask(() -> Actions.rightClick(mage), 529);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 6), 590);
//		for(int i = 630; i <= 726 - lbFor - 1; i += lbFor + 1) {
//			Utils.scheduleTask(() -> Actions.rightClick(mage), i);
//			Utils.scheduleTask(() -> Actions.stopRightClick(mage), i + lbFor);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 727);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 728);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, 92.5f, -90f);
//			Actions.setHotbarSlot(mage, 5);
//		}, 729);
//		Utils.scheduleTask(() -> {
//			Actions.rightClick(mage);
//			Actions.move(mage, "WP", 0);
//		}, 730);
//		Utils.scheduleTask(() -> Actions.move(mage, "WPJ", 2), 780);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 6), 791);
//		for(int i = 792; i <= 826 - lbFor - 1; i += lbFor + 1) {
//			Utils.scheduleTask(() -> Actions.rightClick(mage), i);
//			Utils.scheduleTask(() -> Actions.stopRightClick(mage), i + lbFor);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 827);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 828);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(mage, -3f, -90f);
//			Actions.setHotbarSlot(mage, 6);
//		}, 829);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 0), 830);
//		Utils.scheduleTask(() -> Actions.move(mage, "WPJ", 0), 851);
//		Utils.scheduleTask(() -> Actions.move(mage, "WP", 14), 853);
//		for(int i = 858; i <= 926 - lbFor - 1; i += lbFor + 1) {
//			Utils.scheduleTask(() -> Actions.rightClick(mage), i);
//			Utils.scheduleTask(() -> Actions.stopRightClick(mage), i + lbFor);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(mage, 3), 927);
//		Utils.scheduleTask(() -> Actions.leftClick(mage), 928);
//	}
//
//	@SuppressWarnings("unused")
//	public static Player get() {
//		return mage;
//	}
//}
