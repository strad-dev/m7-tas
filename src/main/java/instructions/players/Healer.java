package instructions.players;

import instructions.Actions;
import instructions.Server;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.Utils;

import java.util.HashSet;
import java.util.Set;

public class Healer {
	private static Player healer;
	private static World world;

	// Healer --> Mage3
	public static void healerInstructions(Player p, String section) {
		healer = p;
		world = healer.getWorld();

		switch(section) {
			case "all", "clear" -> {
				Utils.teleport(healer, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f));
				Utils.scheduleTask(() -> preClear(section.equals("all")), 60);
//				Utils.scheduleTask(() -> Actions.swapItems(healer, 2, 29), 60);
//				Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 2), 61);
//				Utils.scheduleTask(() -> Actions.rightClick(healer), 101);
//				Utils.scheduleTask(() -> {
//					Actions.setHotbarSlot(healer, 1);
//					Actions.move(healer, new Vector(0, 0, 0.8634), 5);
//				}, 102);
//				Utils.scheduleTask(() -> {
//					Utils.teleport(healer, new Location(healer.getWorld(), -120.5, 75, -220.5));
//					Actions.swapItems(healer, 2, 29);
//				}, 141);
//				Utils.scheduleTask(() -> clear(section.equals("all")), 162);
			}
//			case "maxor", "boss" -> {
//				Utils.teleport(healer, new Location(world, 73.5, 221, 13.5, 0f, 0f));
//				Actions.swapItems(healer, 1, 28);
//				Actions.swapItems(healer, 3, 30);
//				Actions.swapItems(healer, 7, 34);
//				if(section.equals("maxor")) {
//					Utils.scheduleTask(() -> maxor(false), 60);
//				} else {
//					Utils.scheduleTask(() -> maxor(true), 60);
//				}
//			}
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

	private static void preClear(boolean doContinue) {
		Actions.turnHead(healer, 180f, -90f);
		Actions.swapItems(healer, 2, 29);
		Actions.swapItems(healer, 4, 31);
		Actions.setHotbarSlot(healer, 7);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 38), 1); // move to pearl spot
		Utils.scheduleTask(() -> Actions.rightClick(healer), 39); // lands in 10 ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 1);
			Actions.move(healer, "N", 0);
		}, 40);
		// dodge tick 40 teleport
		Utils.scheduleTask(() -> Actions.rightClick(healer), 49); // etherwarp to top
		Utils.scheduleTask(() -> Actions.turnHead(healer, -16f, 0.26f), 50);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 51); // etherwarp to Wizard middle
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 2.2f), 52);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 53); // etherwarp to correct X
		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 90f), 54);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 1), 55); // walk off edge and unsneak
		Utils.scheduleTask(() -> Actions.rightClick(healer), 56);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 57);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 58);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 59);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 60); // teleport down
		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 4f), 61);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 62); // aotv into position
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 7);
			Actions.turnHead(healer, -90f, -55f);
		}, 63);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 64); // throw pearl | lands in 6 ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 2);
			Actions.turnHead(healer, 125f, -5f);
		}, 65);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 70); // activate tac, procs in 60 ticks (130)
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 1), 71);
		// tick 80: get teleported back
		Utils.scheduleTask(() -> Utils.teleport(healer, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f)), 80);
		// tick 120: dodged
		// tick 128: run starts
		Utils.scheduleTask(() -> clear(doContinue), 128);
	}

	private static void clear(boolean doContinue) {
		/*
		 * ██╗    ██╗██╗███████╗ █████╗ ██████╗ ██████╗
		 * ██║    ██║██║╚══███╔╝██╔══██╗██╔══██╗██╔══██╗
		 * ██║ █╗ ██║██║  ███╔╝ ███████║██████╔╝██║  ██║
		 * ██║███╗██║██║ ███╔╝  ██╔══██║██╔══██╗██║  ██║
		 * ╚███╔███╔╝██║███████╗██║  ██║██║  ██║██████╔╝
		 * ╚══╝╚══╝ ╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝
		 */
		// tick 2: tac tp back, sneak
		Utils.scheduleTask(() -> Actions.move(healer, "N", 2), 2);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 3);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 4);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 110f, -72f);
			Actions.setHotbarSlot(healer, 7);
		}, 5);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 6); // throw pearl towards wizard crystal | lands in 8 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 159.5f, -15.5f), 7);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 8); // throw pearl towards lower part | lands in 19 ticks
		Utils.scheduleTask(() -> {
			Actions.move(healer, "N", 1);
			Actions.setHotbarSlot(healer, 1);
		}, 13);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 14); // etherwarp into compartment with crystal
		Utils.scheduleTask(() -> Actions.turnHead(healer, 28f, 7f), 15);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 16); // teleport to crystal
		Utils.scheduleTask(() -> Actions.turnHead(healer, 45f, 45f), 17);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer found a Special Crystal!");
		}, 18); // pick up crystal
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 180f, -25f);
			Actions.move(healer, "N", 0);
		}, 19);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 20); // etherwarp up
		Utils.scheduleTask(() -> Actions.turnHead(healer, -161f, -72f), 21);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 22); // etherwarp to secret compartment
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, -32f), 23);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 24); // etherwarp to secret
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 25);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Wizard 1/4 (Opened Chest)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.CHEST);
		}, 26); // obtain secret 1/4
		Utils.scheduleTask(() -> Actions.turnHead(healer, -139f, 2.5f), 27); // pearl lands tick 27
		Utils.scheduleTask(() -> Actions.rightClick(healer), 28); // etherwarp towards bat
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -149.7f, 17.5f);
			Actions.setHotbarSlot(healer, 1);
		}, 29);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 30); // etherwarp into bat area
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 145f, 6f);
			Actions.setHotbarSlot(healer, 0);
		}, 31);
		Utils.scheduleTask(() -> {
			Actions.rightClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Wizard 2/4 (Killed Bat)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.BAT);
		}, 32); // kill bat
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -23f, -20f);
			Actions.setHotbarSlot(healer, 1);
		}, 33);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 34); // etherwarp out
		Utils.scheduleTask(() -> Actions.turnHead(healer, 50f, -25f), 35);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 36); // etherwarp up
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 7);
			Actions.turnHead(healer, -180f, -14.5f);
			Actions.move(healer, "S", 1);
		}, 37);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 38); // throw pearl to correct spot for well | lands in 26 (?) ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 1);
			Actions.turnHead(healer, 180f, 0.97f);
			Actions.move(healer, "N", 0);
		}, 39);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 40); // etherwarp to position
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 91f, -68f);
			Actions.setHotbarSlot(healer, 7);
		}, 41);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 42); // throw pearl to dig spot | lands in 4 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, 0f);
			Actions.setHotbarSlot(healer, 5);
		}, 43);
		// pearl lands tick 46
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Actions.move(healer, "WP", 0);
		}, 47);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 48); // stonk through wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -90f, -90f);
			Actions.setHotbarSlot(healer, 1);
			Actions.move(healer, "N", 0);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Wizard 3/4 (Picked Up Item)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.ITEM);
		}, 49);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 50); // etherwarp up
		Utils.scheduleTask(() -> Actions.turnHead(healer, -76f, -21f), 51);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 52); // etherwarp to wizard
		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, 0f), 53);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 1), 54); // walk forward 1 tick to be in range
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Utils.playLocalSound(healer, Sound.ENTITY_VILLAGER_YES);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "[NPC] Wizard" + ChatColor.WHITE + ": Oh my lovely crystal ball, mi so happy");
			Utils.scheduleTask(() -> {
				Utils.playLocalSound(healer, Sound.ENTITY_VILLAGER_YES);
				Bukkit.broadcastMessage(ChatColor.YELLOW + "[NPC] Wizard" + ChatColor.WHITE + ": You deserve a reward young gobelin");
			}, 20);
			Utils.scheduleTask(() -> {
				Utils.playLocalSound(healer, Sound.ENTITY_VILLAGER_YES);
				Bukkit.broadcastMessage(ChatColor.YELLOW + "[NPC] Wizard" + ChatColor.WHITE + ": Granted your team a " + ChatColor.LIGHT_PURPLE + "Blessing of Wisdom I");
				Utils.broadcastBlessing(healer, Utils.BlessingType.WISDOM, 1);
			}, 60);
		}, 55);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 102f, 1f);
			Actions.move(healer, "N", 24);
		}, 56);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 57); // etherwarp to wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 41f, 1.21f);
			Actions.setHotbarSlot(healer, 4);
		}, 58);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 59); // blow up wall
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 1), 60);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 61); // etherwarp to secret
		Utils.scheduleTask(() -> Actions.turnHead(healer, -10f, 13f), 62);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Wizard 4/4 (Opened Chest)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.CHEST);
		}, 63);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -92f, 1f), 64);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 65); // etherwarp into well
		// Wizard: 65 ticks

		/*
		 * ██╗    ██╗███████╗██╗     ██╗
		 * ██║    ██║██╔════╝██║     ██║
		 * ██║ █╗ ██║█████╗  ██║     ██║
		 * ██║███╗██║██╔══╝  ██║     ██║
		 * ╚███╔███╔╝███████╗███████╗███████╗
		 *  ╚══╝╚══╝ ╚══════╝╚══════╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, -60.1f), 66);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 67); // etherwarp up to secret
		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, -36f), 68);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Well 1/7 (Opened Chest)");
			Utils.playLocalSound(healer, Sound.BLOCK_CHEST_OPEN);
			Utils.playSecretFoundSound(healer, Utils.SecretType.CHEST);
		}, 69);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -67f, -18f), 70);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 71); // reposition for next secret
		Utils.scheduleTask(() -> Actions.turnHead(healer, 85f, 17.5f), 72);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 73); // etherwarp to secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -96.3f, 3.1f);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Well 2/7 (Picked Up Item)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.ITEM);
		}, 74);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 75); // etherwarp out
		Utils.scheduleTask(() -> Actions.turnHead(healer, -158f, 24f), 76);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 77); // etherwarp down
		Utils.scheduleTask(() -> Actions.turnHead(healer, -98f, 1.5f), 78);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 79); // etherwarp to secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -90f, 20f);
			Actions.setHotbarSlot(healer, 5);
		}, 80);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 81);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 7);
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Well 3/7 (Opened Chest)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.CHEST);
		}, 82);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 30f, 0f);
			Actions.setHotbarSlot(healer, 7);
		}, 82);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 83); // pearl to upper secret | lands in 13 ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 1);
			Actions.move(healer, "N", 2);
			Actions.turnHead(healer, 180f, 0f);
		}, 84);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 85); // reposition for wither essence + pearl
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 16f, 66f);
			Actions.setHotbarSlot(healer, 7);
		}, 86);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 87); // pearl to mini | lands in 16 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -93f, 12.5f);
			Actions.move(healer, "N", 4);
			Actions.setHotbarSlot(healer, 1);
		}, 88);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 89); // etherwarp into wither essence chamber
		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, -75f), 90);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 91); // etherwarp up to wither essence
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -117f, 31f);
			Actions.setHotbarSlot(healer, 1);
		}, 92);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Well 4/7 (Obtained Wither Essence)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.ESSENCE);
		}, 93);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 56f, 22f), 94);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Well 5/7 (Obtained Wither Essence)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.ESSENCE);
		}, 95);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -11f, 8f);
			Actions.move(healer, "N", 2);
		}, 96);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 97); // etherwarp to wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -90f, -50f);
			Actions.setHotbarSlot(healer, 5);
		}, 98);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 99);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 100);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 101); // stonk to secret
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 3);
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Well 6/7 (Opened Chest)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.CHEST);
		}, 102);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -45f, 0f), 103);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Well Cleared");
			Utils.broadcastBlessing(healer, Utils.BlessingType.LIFE, 5);
		}, 104);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -4, 1f);
			Actions.move(healer, "N", 2);
			Actions.setHotbarSlot(healer, 1);
		}, 105);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 106); // etherwarp to item
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 59f, 7f);
			Actions.setHotbarSlot(healer, 7);
		}, 107);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 108); // throw pearl to ice fill
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 68f);
			Actions.move(healer, "N", 0);
			Actions.setHotbarSlot(healer, 1);
		}, 109);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 110); // etherwarp down
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 180f, 90f);
			Actions.setHotbarSlot(healer, 5);
		}, 111);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 112); // stonk block
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 1), 113);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 114); // etherwarp down
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 1), 115);
		Utils.scheduleTask(() -> {
			Actions.move(healer, "N", 0);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Well 7/7 (Picked Up Item)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.ITEM);
		}, 116);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 106f, 6f), 117);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 118); // etherwarp into ice fill
		// Well: 53 ticks

		/*
		 * ██╗ ██████╗███████╗    ███████╗██╗██╗     ██╗
		 * ██║██╔════╝██╔════╝    ██╔════╝██║██║     ██║
		 * ██║██║     █████╗      █████╗  ██║██║     ██║
		 * ██║██║     ██╔══╝      ██╔══╝  ██║██║     ██║
		 * ██║╚██████╗███████╗    ██║     ██║███████╗███████╗
		 * ╚═╝ ╚═════╝╚══════╝    ╚═╝     ╚═╝╚══════╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 0f), 119);
		Utils.scheduleTask(() -> {
			startIceFillTask();
			Actions.move(healer, "WP", 0);
		}, 120);
		Utils.scheduleTask(() -> Actions.move(healer, "WN", 0), 123);
		Utils.scheduleTask(() -> Actions.move(healer, "A", 0), 124);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 129);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 130);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 134);
		Utils.scheduleTask(() -> playIceFillSounds(1), 135);
		Utils.scheduleTask(() -> Actions.move(healer, "A", 0), 138);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 142);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 150);
		Utils.scheduleTask(() -> Actions.move(healer, "S", 0), 152);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 162);
		Utils.scheduleTask(() -> Actions.move(healer, "DN", 0), 165);
		Utils.scheduleTask(() -> Actions.move(healer, "S", 0), 166);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 176);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 177);
		Utils.scheduleTask(() -> Actions.move(healer, "A", 0), 186);
		Utils.scheduleTask(() -> Actions.move(healer, "WPD", 0), 195);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 196);
		Utils.scheduleTask(() -> playIceFillSounds(2), 200);
		Utils.scheduleTask(() -> Actions.move(healer, "A", 0), 204);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 206);
		Utils.scheduleTask(() -> Actions.move(healer, "S", 0), 210);
		Utils.scheduleTask(() -> Actions.move(healer, "A", 0), 218);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 220);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 233);
		Utils.scheduleTask(() -> Actions.move(healer, "S", 0), 236);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 242);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 244);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 249);
		Utils.scheduleTask(() -> Actions.move(healer, "S", 0), 252);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 255);
		Utils.scheduleTask(() -> Actions.move(healer, "S", 0), 258);
		Utils.scheduleTask(() -> Actions.move(healer, "A", 0), 267);
		Utils.scheduleTask(() -> Actions.move(healer, "S", 0), 275);
		Utils.scheduleTask(() -> Actions.move(healer, "D", 0), 281);
		Utils.scheduleTask(() -> Actions.move(healer, "WDP", 0), 285);
		Utils.scheduleTask(() -> Actions.move(healer, "A", 0), 296);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 300);
		Utils.scheduleTask(() -> Actions.move(healer, "A", 0), 303);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 2), 307);
		Utils.scheduleTask(() -> {
			playIceFillSounds(3);
			Server.openIceFillRewards();
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Ice Fill Cleared");
		}, 309);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 68f, -33f), 310);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Utils.broadcastBlessing(healer, Utils.BlessingType.POWER, 5);
			Utils.playSecretFoundSound(healer, Utils.SecretType.BLESSING_CHEST);
		}, 329);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 112f, -33f), 330);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Utils.broadcastBlessing(healer, Utils.BlessingType.POWER, 5);
			Utils.playSecretFoundSound(healer, Utils.SecretType.BLESSING_CHEST);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Clear Finished in 331 Ticks (16.55 seconds)");
		}, 331);
		Utils.scheduleTask(Healer::stopIceFillTask, 400);
		// Ice Fill: 213 ticks
	}

	private static void playIceFillSounds(int level) {
		switch(level) {
			case 1 -> {
				Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.189446f);
				Utils.scheduleTask(() -> Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.3352f), 5);
				Utils.scheduleTask(() -> Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.41436f), 10);
			}
			case 2 -> {
				Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.4987f);
				Utils.scheduleTask(() -> Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.5878f), 5);
				Utils.scheduleTask(() -> Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.6821f), 10);
			}
			case 3 -> {
				Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.782f);
				Utils.scheduleTask(() -> Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.888f), 5);
				Utils.scheduleTask(() -> Utils.playLocalSound(healer, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 2.0f), 10);
			}
		}
	}

	private static BukkitTask iceFillTask;
	private static final Set<Block> frozenBlocks = new HashSet<>();

	private static void startIceFillTask() {
		if (iceFillTask != null) {
			iceFillTask.cancel();
		}

		frozenBlocks.clear();

		iceFillTask = new BukkitRunnable() {
			@Override
			public void run() {
				Block below = healer.getLocation().subtract(0, 1, 0).getBlock();
				if (below.getType() == Material.ICE) {
					below.setType(Material.PACKED_ICE);
					frozenBlocks.add(below);
					Utils.playGlobalSound(Sound.BLOCK_SNOW_BREAK, 2.0f, 1.0f);
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	public static void stopIceFillTask() {
		if (iceFillTask != null) {
			iceFillTask.cancel();
			iceFillTask = null;
		}

		for (Block block : frozenBlocks) {
			if (block.getType() == Material.PACKED_ICE) {
				block.setType(Material.ICE);
			}
		}
		frozenBlocks.clear();
	}


//		/*
//		 * ███╗   ███╗██╗   ██╗███████╗███████╗██╗   ██╗███╗   ███╗
//		 * ████╗ ████║██║   ██║██╔════╝██╔════╝██║   ██║████╗ ████║
//		 * ██╔████╔██║██║   ██║███████╗█████╗  ██║   ██║██╔████╔██║
//		 * ██║╚██╔╝██║██║   ██║╚════██║██╔══╝  ██║   ██║██║╚██╔╝██║
//		 * ██║ ╚═╝ ██║╚██████╔╝███████║███████╗╚██████╔╝██║ ╚═╝ ██║
//		 * ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚══════╝ ╚═════╝ ╚═╝     ╚═╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 66.6f, 7.8f), 160);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -133.5, 71, -17.5)), 161);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 56.7f, 43.4f), 162);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -136.5, 69, -15.5)), 163);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 0f, 0f);
//			Actions.setHotbarSlot(healer, 3);
//		}, 164);
//		Utils.scheduleTask(() -> Actions.superboom(healer, -138, 69, -15, -136, 74, -14), 165);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 1/5 (Opened Chest)");
//			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
//		}, 166);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -130.4f, -44.3f);
//			Actions.setHotbarSlot(healer, 1);
//		}, 167);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -128.5, 82, -22.5)), 168);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -64.5f, -61.9f), 169);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -124.5, 93, -20.5)), 170);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 29.1f, 31.7f), 171);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 2/5 (Obtained Wither Essence)");
//			world.playSound(healer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
//		}, 172);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -171f, 46.3f), 173);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -122.5, 82, -32.5)), 174);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 152.8f, 3.5f), 175);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -134.5, 82, -56.5)), 176);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -135f, 0f), 177);
//		Utils.scheduleTask(() -> Actions.AOTV(healer, new Location(world, -133.5, 82, -57.5)), 178);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -128f, 37.2f), 179);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -132.5, 82, -58.5)), 180);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 135f, 77.5f);
//			Actions.move(healer, new Vector(-0.183848, 0, -0.183848), 1);
//		}, 181);
//		Utils.scheduleTask(() -> Actions.swingHand(healer), 182);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(-133, 81, -59)), 183);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 157.9f, 20.1f), 184);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 3/5 (Opened Chest)");
//			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
//		}, 185);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 136.1f, 71.7f), 186);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -135.5, 70, -61.5)), 187);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 135f, 50f);
//			Actions.setHotbarSlot(healer, 4);
//		}, 188);
//		Utils.scheduleTask(() -> {
//			Actions.rightClickOld(healer);
//			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 4/5 (Mimic Chest)");
//			Actions.mimicChest(healer, world.getBlockAt(-137, 70, -63));
//		}, 189);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -89.1f, 7.6f);
//			Actions.setHotbarSlot(healer, 1);
//		}, 190);
//		Utils.scheduleTask(() -> {
//			Actions.etherwarp(healer, new Location(world, -128.5, 71, -61.5));
//			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Mimic Killed!");
//		}, 191);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -97.7f, 23.6f), 192);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -120.5, 69, -62.5)), 193);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 121.4f, 15.1f), 194);
//		Utils.scheduleTask(() -> Actions.AOTV(healer, new Location(world, -125.5, 69, -65.5)), 195);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -90f, 90f);
//			Actions.setHotbarSlot(healer, 3);
//		}, 196);
//		Utils.scheduleTask(() -> Actions.superboom(healer, -123, 68, -63, -126, 67, -70), 197);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 60f), 198);
//		Utils.scheduleTask(() -> Actions.etherwarp(healer, new Location(world, -120.5, 62, -65.5)), 199);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(healer);
//			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 5/5 (Opened Chest)");
//			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
//			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Clear Finished in 202 Ticks (10.10 seconds)");
//		}, 200);
//		Utils.scheduleTask(() -> {
//			Actions.swapItems(healer, 1, 28);
//			Actions.swapItems(healer, 3, 30);
//			Actions.swapItems(healer, 7, 34);
//		}, 201);
//		if(doContinue) {
//			Utils.scheduleTask(() -> {
//				Utils.teleport(healer, new Location(world, 73.5, 221, 13.5));
//				maxor(true);
//			}, 1025);
//		}
//	}
//
//	public static void maxor(boolean doContinue) {
//		// TODO predev with 500 speed
//		Actions.setHotbarSlot(healer, 5);
//		Actions.move(healer, new Vector(0.214, 0, 1.102), 17);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -11f, 0f), 1);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.0496, 0, 0.255), 4), 17);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 180f, 84.1f), 21);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(77, 220, 33)), 22);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(77, 220, 32)), 23);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -53.7f, 0f), 24);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.226, 0, 0.166), 29), 37);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.9046, 0, 0.6645), 4), 66);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.2095, 0, 0.1539), 2), 70);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -90.5f, -10.9f), 72);
//		Utils.scheduleTask(() -> {
//			Actions.move(healer, new Vector(1.12238, 0, -0.009795), 1);
//			Actions.ghostPick(healer, world.getBlockAt(91, 166, 41));
//		}, 73);
//		Utils.scheduleTask(() -> Actions.ghostPick(healer, world.getBlockAt(91, 167, 41)), 74);
//		Utils.scheduleTask(() -> Actions.ghostPick(healer, world.getBlockAt(91, 167, 40)), 75);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -90.8f, 17.4f), 76);
//		Utils.scheduleTask(() -> Actions.ghostPick(healer, world.getBlockAt(91, 166, 40)), 76);
//		Utils.scheduleTask(() -> Actions.ghostPick(healer, world.getBlockAt(91, 165, 40)), 77);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 30f), 78);
//		Utils.scheduleTask(() -> {
//			Actions.ghostPick(healer, world.getBlockAt(91, 165, 41));
//			Actions.jump(healer);
//			Actions.move(healer, new Vector(1.12242, 0, 0), 2);
//		}, 79);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.1984, 0, 0.1984), 2), 81);
//		Utils.scheduleTask(() -> {
//			Actions.move(healer, new Vector(0.8634, 0, 0), 9);
//			Actions.stonk(healer, world.getBlockAt(92, 166, 41));
//		}, 83);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(92, 165, 41)), 84);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(93, 166, 41)), 85);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(93, 165, 41)), 86);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(94, 166, 41)), 87);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(94, 165, 41)), 88);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(95, 166, 41)), 89);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(95, 165, 41)), 90);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, 0f, 0f);
//			Actions.setHotbarSlot(healer, 1);
//		}, 92); // 27-tick timesave!!!
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.1984, 0, 0.1984), 30), 93);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 0.2806), 9), 129);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.12242), 2), 130);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 15f, 82f), 131);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(-0.3948, 0.5, 1.4735)), 132);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 15f, 0f), 133);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 143);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.12242), 11), 144);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -4f, 82f), 154);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(0.1064, 0.5, 1.5218)), 155);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -4f, 0f), 156);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 165);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 1.12242), 5), 166);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 8.5f, 82f), 170);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(0.2255, 0.5, 1.5088)), 171);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 8.5f, 0f), 172);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 183);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.7937, 0, 0.7937), 3), 184);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -7f, 82f), 186);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(0.186, 0.5, 1.5142)), 187);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -7f, 0f), 188);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -5.8f, 0f), 197);
//		Utils.scheduleTask(() -> {
//			Server.resetGoldorCheese();
//			Actions.jump(healer);
//			Actions.move(healer, new Vector(0.1144, 0, 1.1166), 1);
//			Actions.setHotbarSlot(healer, 5);
//		}, 198);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0.0286, 0, 0.279), 7), 199);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 24.6f, 64.9f), 214);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(96, 120, 121)), 215);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 0f), 216);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.8634, 0, 0), 1), 217);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 26.1f), 218);
//		Utils.scheduleTask(() -> {
//			Actions.stonk(healer, world.getBlockAt(96, 121, 122));
//			Actions.move(healer, new Vector(0, 0, 1.12242), 5);
//		}, 219);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(96, 120, 122)), 220);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(96, 121, 123)), 221);
//		Utils.scheduleTask(() -> Actions.stonk(healer, world.getBlockAt(96, 120, 123)), 222);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, 0.2806), 10), 224);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 1), 225);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 83f, 0f), 234);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-1.114, 0, 0.1368), 8), 235);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 83f, 82f), 242);
//		Utils.scheduleTask(() -> Actions.bonzo(healer, new Vector(-1.514, 0.5, 0.186)), 243);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 83f, 0f), 244);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 49f, 0f), 268);
//		Utils.scheduleTask(() -> {
//			Actions.lavaJump(healer, true);
//			Actions.move(healer, new Vector(-0.2118, 0, 0.1841), 34);
//		}, 269);
//		Utils.scheduleTask(() -> Actions.move(healer, new Vector(-0.8471, 0, 0.7364), 2), 303);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 0f), 304);
//		Utils.scheduleTask(() -> {
//			Actions.jump(healer);
//			Actions.move(healer, new Vector(-1.12242, 0, 0), 2);
//		}, 305);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(healer, -49.8f, -52.1f);
//			Actions.setHotbarSlot(healer, 5);
//		}, 306);
//		Utils.scheduleTask(() -> Actions.rightClickLever(healer), 307);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 61.9f, -45.8f), 308);
//		Utils.scheduleTask(() -> Actions.rightClickLever(healer), 309);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 14.5f, -50.8f), 310);
//		Utils.scheduleTask(() -> Actions.rightClickLever(healer), 311);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 14.5f, -29f), 312);
//		Utils.scheduleTask(() -> Actions.rightClickLever(healer), 313);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, -47.8f, 4.3f), 314);
//		Utils.scheduleTask(() -> Actions.rightClickLever(healer), 315);
//		Utils.scheduleTask(() -> Actions.turnHead(healer, 60f, 4.3f), 316);
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
//			Utils.scheduleTask(() -> storm(true), 499);
//		}
//	}
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
//		Utils.scheduleTask(() -> {
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
