package instructions.players;

import instructions.Actions;
import instructions.Server;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import plugin.Utils;

import java.util.Objects;

public class Berserk {
	private static Player berserk;
	private static World world;

	// Berserk --> Mage3
	public static void berserkInstructions(Player p, String section) {
		berserk = p;
		world = berserk.getWorld();
		Objects.requireNonNull(berserk.getInventory().getItem(4)).addUnsafeEnchantment(Enchantment.POWER, 16);

		switch(section) {
			case "all", "clear" -> {
				Utils.teleport(berserk, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f));
				Utils.scheduleTask(() -> preClear(section.equals("all")), 60);
			}
			case "maxor", "boss" -> {
				Utils.teleport(berserk, new Location(world, 73.5, 221, 14.5, 0f, 0f));
				Actions.swapItems(berserk, 1, 28);
				Actions.swapItems(berserk, 3, 30);
				Actions.swapItems(berserk, 6, 33);
				Actions.swapItems(berserk, 7, 34);
				Actions.setHotbarSlot(berserk, 5);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
//			case "storm" -> {
//				Utils.teleport(berserk, new Location(world, 100.422, 169, 49.624, -1f, 23f));
//				Actions.swapItems(berserk, 1, 28);
//				Actions.swapItems(berserk, 7, 35);
//				Utils.scheduleTask(() -> storm(false), 60);
//			}
//			case "goldor" -> {
//				Utils.teleport(berserk, new Location(world, 89.565, 115.0625, 132.272, -128f, -19f));
//				Actions.swapItems(berserk, 1, 28);
//				Actions.swapItems(berserk, 7, 35);
//				Actions.swapItems(berserk, 9, 39);
//				Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 56);
//				Utils.scheduleTask(Berserk::shoot, 57);
//				Utils.scheduleTask(() -> Actions.turnHead(berserk, -15.4f, -1f), 58);
//				Utils.scheduleTask(() -> goldor(false), 60);
//			}
//			case "necron" -> {
//				Utils.teleport(berserk, new Location(world, 56.488, 64, 111.700, -180f, 0f));
//				Actions.swapItems(berserk, 1, 28);
//				Actions.swapItems(berserk, 7, 35);
//				Utils.scheduleTask(() -> necron(false), 60);
//			}
//			case "witherking" -> {
//				Utils.teleport(berserk, new Location(world, 90.7, 6, 56.581, -79.7f, 19.1f));
//				Actions.swapItems(berserk, 1, 28);
//				Actions.swapItems(berserk, 7, 35);
//				Actions.swapItems(berserk, 12, 39);
//				Utils.scheduleTask(Berserk::witherKing, 60);
//			}
		}
	}

	private static void preClear(boolean doContinue) {
		Actions.turnHead(berserk, 180f, -89f);
		Actions.swapItems(berserk, 2, 29);
		Actions.swapItems(berserk, 4, 31);
		Actions.setHotbarSlot(berserk, 7);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 35), 1); // move to pearl spot
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 37); // lands in 10 ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 1);
			Actions.move(berserk, "N", 0);
		}, 38);
		// dodge tick 40 teleport
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 47); // etherwarp to top
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -16f, 0.26f), 48);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 49); // etherwarp to Wizard middle
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 2.2f), 50);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 51); // etherwarp to correct X
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 90f), 52);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 1), 53); // walk off edge and unsneak
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 54);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 55);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 56);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 57);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 58); // teleport down
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 4f), 59);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 60); // aotv into position
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 7);
			Actions.turnHead(berserk, -90f, -55f);
		}, 61);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 62); // throw pearl | lands in 6 ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 2);
			Actions.turnHead(berserk, 125f, -5f);
		}, 63);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 68); // activate tac, procs in 60 ticks (128)
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 1), 69);
		// tick 80: get teleported back
		Utils.scheduleTask(() -> Utils.teleport(berserk, new Location(world, -120.5, 71, -183.5, 0.0f, 0.0f)), 80);
		// tick 120: dodged
		// tick 126: run starts
		Utils.scheduleTask(() -> clear(doContinue), 126);
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
		Utils.scheduleTask(() -> Actions.move(berserk, "N", 2), 2);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 3);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 4);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 110f, -72f);
			Actions.setHotbarSlot(berserk, 7);
		}, 5);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 6); // throw pearl towards wizard crystal | lands in 8 ticks
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 159.5f, -15.5f), 7);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 8); // throw pearl towards lower part | lands in 19 ticks
		Utils.scheduleTask(() -> {
			Actions.move(berserk, "N", 1);
			Actions.setHotbarSlot(berserk, 1);
		}, 13);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 14); // pearl lands, etherwarp into compartment with crystal
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 28f, 7f), 15);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 16); // teleport to crystal
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 45f, 45f), 17);
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk found a Special Crystal!");
		}, 18); // pick up crystal
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 180f, -25f);
			Actions.move(berserk, "N", 0);
		}, 19);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 20); // etherwarp up
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -161f, -72f), 21);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 22); // etherwarp to secret compartment
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, -32f), 23);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 24); // etherwarp to secret
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 0f), 25);
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Wizard 1/4 (Opened Chest)");
			Utils.broadcastBlessing(berserk, Utils.BlessingType.WISDOM, 2);
			Utils.playSecretFoundSound(berserk, Utils.SecretType.BLESSING_CHEST);
		}, 26); // obtain secret 1/4
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -139f, 0f), 27); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 28); // etherwarp towards bat
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -149.7f, 17.5f);
			Actions.setHotbarSlot(berserk, 1);
		}, 29);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 30); // etherwarp into bat area
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 145f, 6f);
			Actions.setHotbarSlot(berserk, 0);
		}, 31);
		Utils.scheduleTask(() -> {
			Actions.rightClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Wizard 2/4 (Killed Bat)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.BAT);
		}, 32); // kill bat
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -23f, -20f);
			Actions.setHotbarSlot(berserk, 1);
		}, 33);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 34); // etherwarp out
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 50f, -25f), 35);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 36); // etherwarp up
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 7);
			Actions.turnHead(berserk, -180f, -15.5f);
			Actions.move(berserk, "S", 1);
		}, 37);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 38); // pearl to correct spot for well | lands in 28 ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 1);
			Actions.turnHead(berserk, 180f, 0.97f);
			Actions.move(berserk, "N", 0);
		}, 39);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 40); // etherwarp to position
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 91f, -68f);
			Actions.setHotbarSlot(berserk, 7);
		}, 41);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 42); // pearl to dig spot | lands in 4 ticks
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 90f), 43);
		// tick 46: pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 47); // pearl to reposition | lands in 1 tick
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 90f, 0f);
			Actions.setHotbarSlot(berserk, 5);
		}, 48); // pearl lands
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Actions.move(berserk, "WP", 0);
		}, 49);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 50); // stonk through wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, -90f);
			Actions.setHotbarSlot(berserk, 1);
			Actions.move(berserk, "N", 0);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Wizard 3/4 (Picked Up Item)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.ITEM);
		}, 51);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 52); // etherwarp up
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -76f, -21f), 53);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 54); // etherwarp to wizard
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 180f, 0f), 55);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 1), 56); // walk forward 1 tick to be in range
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Utils.playLocalSound(berserk, Sound.ENTITY_VILLAGER_YES);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "[NPC] Wizard" + ChatColor.WHITE + ": Oh my lovely crystal ball, mi so happy");
			Utils.scheduleTask(() -> {
				Utils.playLocalSound(berserk, Sound.ENTITY_VILLAGER_YES);
				Bukkit.broadcastMessage(ChatColor.YELLOW + "[NPC] Wizard" + ChatColor.WHITE + ": You deserve a reward young gobelin");
			}, 20);
			Utils.scheduleTask(() -> {
				Utils.playLocalSound(berserk, Sound.ENTITY_VILLAGER_YES);
				Bukkit.broadcastMessage(ChatColor.YELLOW + "[NPC] Wizard" + ChatColor.WHITE + ": Granted your team a " + ChatColor.LIGHT_PURPLE + "Blessing of Wisdom I");
				Utils.broadcastBlessing(berserk, Utils.BlessingType.WISDOM, 1);
			}, 60);
		}, 57);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 102f, 1f);
			Actions.move(berserk, "N", 24);
		}, 58);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 59); // etherwarp to wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 41f, 1.21f);
			Actions.setHotbarSlot(berserk, 4);
		}, 60);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 61); // blow up wall
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 1), 62);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 63); // etherwarp to secret
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -10f, 13f), 64);
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Wizard 4/4 (Opened Chest)");
			Utils.broadcastBlessing(berserk, Utils.BlessingType.WISDOM, 1);
			Utils.playSecretFoundSound(berserk, Utils.SecretType.BLESSING_CHEST);
		}, 65);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 1f), 66); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 67); // etherwarp into well
		// Wizard: 67 ticks

		/*
		 * ██╗    ██╗███████╗██╗     ██╗
		 * ██║    ██║██╔════╝██║     ██║
		 * ██║ █╗ ██║█████╗  ██║     ██║
		 * ██║███╗██║██╔══╝  ██║     ██║
		 * ╚███╔███╔╝███████╗███████╗███████╗
		 *  ╚══╝╚══╝ ╚══════╝╚══════╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, -60.1f), 68);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 69); // etherwarp up to secret
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, -36f), 70);
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Well 1/7 (Opened Chest)");
			Utils.playLocalSound(berserk, Sound.BLOCK_CHEST_OPEN);
			Utils.playSecretFoundSound(berserk, Utils.SecretType.CHEST);
		}, 71);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -67f, -18f), 72);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 73); // reposition for next secret
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 85f, 17.5f), 74);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 75); // etherwarp to secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -96.3f, 3.1f);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Well 2/7 (Picked Up Item)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.ITEM);
		}, 76);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 77); // etherwarp out
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -158f, 24f), 78);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 79); // etherwarp down
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -98f, 1.5f), 80);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 81); // etherwarp to secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 20f);
			Actions.setHotbarSlot(berserk, 5);
		}, 82);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 83);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 7);
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Well 3/7 (Opened Chest)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.CHEST);
		}, 84);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 30f, 0f);
			Actions.setHotbarSlot(berserk, 7);
		}, 84);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 85); // pearl to upper secret | lands in 13 ticks
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 1);
			Actions.move(berserk, "N", 2);
			Actions.turnHead(berserk, 180f, 0f);
		}, 86);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 87); // reposition for wither essence + pearl
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 16f, 66f);
			Actions.setHotbarSlot(berserk, 7);
		}, 88);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 89); // pearl to mini | lands in 16 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -93f, 12.5f);
			Actions.move(berserk, "N", 4);
			Actions.setHotbarSlot(berserk, 1);
		}, 90);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 91); // etherwarp into wither essence chamber
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 180f, -75f), 92);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 93); // etherwarp up to wither essence
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -117f, 31f);
			Actions.setHotbarSlot(berserk, 1);
		}, 94);
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Well 4/7 (Obtained Wither Essence)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.ESSENCE);
		}, 95);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 56f, 22f), 96);
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Well 5/7 (Obtained Wither Essence)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.ESSENCE);
		}, 97);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -11f, 8f);
			Actions.move(berserk, "N", 2);
		}, 98); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 99); // etherwarp to wall
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, -50f);
			Actions.setHotbarSlot(berserk, 5);
		}, 100);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 101);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 102);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 103); // stonk to secret
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 3);
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Well 6/7 (Opened Chest)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.CHEST);
		}, 104);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -45f, 0f), 105); // pearl lands
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Well Cleared");
		}, 106);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -4, 1f);
			Actions.move(berserk, "N", 2);
			Actions.setHotbarSlot(berserk, 1);
			Utils.playLocalSound(berserk, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
			Utils.broadcastBlessing(berserk, Utils.BlessingType.LIFE, 5);
		}, 107);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 108); // etherwarp to item
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 59f, 7f);
			Actions.setHotbarSlot(berserk, 7);
		}, 109);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 110); // throw pearl to ice fill | lands in 9 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 68f);
			Actions.move(berserk, "N", 0);
			Actions.setHotbarSlot(berserk, 1);
		}, 111);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 112); // etherwarp down
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 180f, 90f);
			Actions.setHotbarSlot(berserk, 5);
		}, 113);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 114); // stonk block
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 1), 115);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 116); // etherwarp down
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 117);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, "N", 3);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Well 7/7 (Picked Up Item)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.ITEM);
		}, 118);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 105.5f, 5f), 119); // pearl lands
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 120); // etherwarp into ice fill
		// Well: 53 ticks

		/*
		 * ██╗ ██████╗███████╗    ███████╗██╗██╗     ██╗
		 * ██║██╔════╝██╔════╝    ██╔════╝██║██║     ██║
		 * ██║██║     █████╗      █████╗  ██║██║     ██║
		 * ██║██║     ██╔══╝      ██╔══╝  ██║██║     ██║
		 * ██║╚██████╗███████╗    ██║     ██║███████╗███████╗
		 * ╚═╝ ╚═════╝╚══════╝    ╚═╝     ╚═╝╚══════╝╚══════╝
		 */
		Server.IceFill.run(berserk, world);
		// Ice Fill: 127 ticks

		Utils.scheduleTask(() -> {
			Actions.swapItems(berserk, 1, 28);
			Actions.swapItems(berserk, 3, 30);
			Actions.swapItems(berserk, 6, 33);
			Actions.swapItems(berserk, 7, 34);
			Actions.setHotbarSlot(berserk, 5);
		}, 248);
		Utils.scheduleTask(() -> {
			Server.IceFill.stopIceFillTask();
			if(doContinue) {
				Utils.teleport(berserk, new Location(world, 73.5, 221, 14.5));
				maxor(true);
			}
		}, 742);
	}

	public static void maxor(boolean doContinue) {
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 30f);
			Actions.move(berserk, "WP", 17);
		}, 3);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 18);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 19);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 35f, 0f), 21);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 70), 22);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -145f, 0f), 287);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 60), 288);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 0f), 298);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -35f, 0f), 338);
		if(doContinue) {
//			Utils.scheduleTask(() -> storm(true), 499);
		}
	}

//	public static void storm(boolean doContinue) {
//		// move continues for 2 more ticks from previous instruction
//		Actions.setHotbarSlot(berserk, 6);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -1f, 23f), 2);
//		Utils.scheduleTask(() -> Actions.gyro(berserk, new Location(world, 100.5, 169, 53.5)), 3);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, -1f, 0f);
//			Actions.setHotbarSlot(berserk, 4);
//		}, 4);
//		Utils.scheduleTask(Berserk::shoot, 5);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 6);
//		Utils.scheduleTask(Berserk::shoot, 10);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 11);
//		Utils.scheduleTask(Berserk::shoot, 15);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 16);
//		Utils.scheduleTask(Berserk::shoot, 20);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 21);
//		Utils.scheduleTask(Berserk::shoot, 25);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 26);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -163.5f, -4f), 27);
//		Utils.scheduleTask(Berserk::shoot, 30);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 31);
//		Utils.scheduleTask(Berserk::shoot, 35);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 36);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -155.2f, -4f), 37);
//		Utils.scheduleTask(Berserk::shoot, 40);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 41);
//		Utils.scheduleTask(Berserk::shoot, 45);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 46);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -149f, -4f), 47);
//		Utils.scheduleTask(Berserk::shoot, 50);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 51);
//		Utils.scheduleTask(Berserk::shoot, 55);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 56);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90.6f, -10f), 57);
//		Utils.scheduleTask(Berserk::shoot, 60);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 61);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -81.4f, -10f), 62);
//		Utils.scheduleTask(Berserk::shoot, 65);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 66);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -70.9f, -10f), 67);
//		Utils.scheduleTask(Berserk::shoot, 70);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 71);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 151.5f, -5f), 72);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.4991, 0, -1.0054), 17), 73);
//		Utils.scheduleTask(Berserk::shoot, 75);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 76);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 151.5f, 6f), 79);
//		Utils.scheduleTask(Berserk::shoot, 80);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 81);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 151.5f, 9f), 84);
//		Utils.scheduleTask(Berserk::shoot, 85);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 86);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 167f, 11f), 89);
//		Utils.scheduleTask(Berserk::shoot, 90);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 91);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 148f, -5.2f), 92);
//		Utils.scheduleTask(Berserk::shoot, 95);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 96);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 143f, -5.2f), 97);
//		Utils.scheduleTask(Berserk::shoot, 100);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 101);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 135f, 8f), 102);
//		Utils.scheduleTask(Berserk::shoot, 105);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 106);
//		Utils.scheduleTask(Berserk::shoot, 110);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 111);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 2), 112);
//		Utils.scheduleTask(() -> Actions.leap(berserk, Archer.get()), 113);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 114);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 120f, 0f), 115);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.972, 0, -0.5612), 12), 116);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 128);
//		for(int tick = 130; tick <= 545; tick += 5) {
//			Utils.scheduleTask(() -> {
//				List<Entity> nearbyEntities = berserk.getNearbyEntities(10, 10, 10);
//
//				for(Entity entity : nearbyEntities) {
//					if(entity instanceof WitherSkeleton) {
//						Location healerLoc = berserk.getLocation();
//						Location witherLoc = entity.getLocation();
//
//						double deltaX = witherLoc.getX() - healerLoc.getX();
//						double deltaY = witherLoc.getY() - healerLoc.getY();
//						double deltaZ = witherLoc.getZ() - healerLoc.getZ();
//
//						float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
//						float pitch = (float) -(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * 180.0 / Math.PI);
//
//						Actions.turnHead(berserk, yaw, pitch);
//
//						Utils.scheduleTask(Berserk::shoot, 1);
//
//						break;
//					}
//				}
//			}, tick);
//		}
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, -91.5f, 0f);
//			Actions.setHotbarSlot(berserk, 1);
//		}, 544);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.12204, 0, -0.0294), 11), 545);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.2805, 0, -0.00735), 8), 556);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.12204, 0, -0.0294), 20), 564);
//		Utils.scheduleTask(() -> {
//			Actions.jump(berserk);
//			Actions.turnHead(berserk, -85f, 0f);
//		}, 583);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.118, 0, 0.0978), 8), 584);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.28, 0, -0.0245), 17), 592);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -101f, 0f), 608);
//		Utils.scheduleTask(() -> Actions.bonzo(berserk, new Vector(-1.4975, 0.5, 0.2911)), 609);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, 79f, 0f);
//			Actions.setHotbarSlot(berserk, 5);
//		}, 610);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 65f), 643);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0, 0, -0.8634), 1), 644);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(92, 131, 45)), 645);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 45f, 0f), 646);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.7937, 0, 0.7937), 2), 646);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -74.6f, 6.9f), 648);
//		Utils.scheduleTask(() -> Actions.ghostPick(berserk, world.getBlockAt(91, 132, 45)), 649);
//		Utils.scheduleTask(() -> Actions.ghostPick(berserk, world.getBlockAt(91, 132, 46)), 650);
//		Utils.scheduleTask(() -> Actions.ghostPick(berserk, world.getBlockAt(90, 132, 46)), 651);
//		Utils.scheduleTask(() -> Actions.ghostPick(berserk, world.getBlockAt(89, 132, 46)), 652);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 76.6f, 26.7f), 653);
//		Utils.scheduleTask(() -> Actions.ghostPick(berserk, world.getBlockAt(91, 131, 45)), 654);
//		Utils.scheduleTask(() -> Actions.ghostPick(berserk, world.getBlockAt(91, 131, 46)), 655);
//		Utils.scheduleTask(() -> Actions.ghostPick(berserk, world.getBlockAt(90, 131, 46)), 656);
//		Utils.scheduleTask(() -> Actions.ghostPick(berserk, world.getBlockAt(89, 131, 46)), 657);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 0f), 658);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 70f, 0f), 666);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.055, 0, 0.384), 2), 667);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 0f), 668);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.12242, 0, 0), 3), 669);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.2806, 0, 0), 8), 672);
//		Utils.scheduleTask(() -> {
//			Actions.jump(berserk);
//			Actions.move(berserk, new Vector(-1.12242, 0, 0), 1);
//		}, 682);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.2806, 0, 0), 3), 683);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, 138f, 82f);
//			Actions.setHotbarSlot(berserk, 1);
//		}, 693);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.751, 0, -0.834), 1), 694);
//		Utils.scheduleTask(() -> Actions.bonzo(berserk, new Vector(-1.0201, 0.5, -1.1337)), 695);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 138f, 0f), 696);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 97.6f, 0f), 709);
//		Utils.scheduleTask(() -> {
//			Actions.jump(berserk);
//			Actions.move(berserk, new Vector(-1.1126, 0, -0.1485), 1);
//		}, 710);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.278, 0, -0.037), 12), 711);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, -15.4f, -7.6f);
//			Actions.setHotbarSlot(berserk, 4);
//		}, 723);
//		Utils.scheduleTask(() -> Actions.swapItems(berserk, 9, 39), 724);
//		if(doContinue) {
//			Utils.scheduleTask(Berserk::shoot, 887);
//			Utils.scheduleTask(() -> Actions.turnHead(berserk, -15.4f, -1f), 888);
//			Utils.scheduleTask(() -> goldor(true), 890);
//		}
//	}
//
//	private static void goldor(boolean doContinue) {
//		/*
//		 * ██╗██╗  ██╗
//		 * ╚═╝██║  ██║
//		 * ██╗███████║
//		 * ██║╚════██║
//		 * ██║     ██║
//		 * ╚═╝     ╚═╝
//		 */
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 130 50 emerald_block");
//		Utils.scheduleTask(() -> {
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 130 50 blue_terracotta");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 128 50 emerald_block");
//		}, 1);
//		Utils.scheduleTask(Berserk::shoot, 2);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -15.4f, 6.6f), 3);
//		Utils.scheduleTask(() -> {
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 128 50 blue_terracotta");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 126 50 emerald_block");
//		}, 6);
//		Utils.scheduleTask(Berserk::shoot, 7);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -4.5f, -7.2f), 8);
//		Utils.scheduleTask(() -> {
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 68 126 50 blue_terracotta");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 64 130 50 emerald_block");
//		}, 11);
//		Utils.scheduleTask(Berserk::shoot, 12);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -4.2f, -0.3f), 13);
//		Utils.scheduleTask(() -> {
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 64 130 50 blue_terracotta");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 64 128 50 emerald_block");
//		}, 16);
//		Utils.scheduleTask(Berserk::shoot, 17);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -4.2f, 7.3f), 18);
//		Utils.scheduleTask(() -> {
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 64 128 50 blue_terracotta");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 64 126 50 emerald_block");
//		}, 21);
//		Utils.scheduleTask(Berserk::shoot, 22);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, 79f, 0f);
//			Actions.setHotbarSlot(berserk, 1);
//		}, 23);
//		Utils.scheduleTask(() -> {
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 64 126 50 blue_terracotta");
//			Goldor.broadcastTerminalComplete(berserk, "device", 2, 7);
//		}, 26);
//
//		/*
//		 * ███████╗███████╗██████╗
//		 * ██╔════╝██╔════╝╚════██╗
//		 * █████╗  █████╗   █████╔╝
//		 * ██╔══╝  ██╔══╝   ╚═══██╗
//		 * ███████╗███████╗██████╔╝
//		 * ╚══════╝╚══════╝╚═════╝
//		 */
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, 79f, 82f);
//			Actions.move(berserk, new Vector(-0.8475, 0, 0.1674), 1);
//		}, 27);
//		Utils.scheduleTask(() -> Actions.bonzo(berserk, new Vector(-1.4975, 0.5, 0.2911)), 28);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 79f, 0f), 29);
//		// swap pet to black cat
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 63.5f, 82f), 42);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.256, 0, 0.626), 1), 43);
//		Utils.scheduleTask(() -> Actions.bonzo(berserk, new Vector(-1.365, 0.5, 0.5809)), 44);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 63.5f, 0f), 45);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Bonzo Procced!");
//			world.playSound(berserk.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 2f);
//		}, 60);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 89f, 82f), 65);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.4028, 0, 0.0245), 1), 66);
//		Utils.scheduleTask(() -> Actions.bonzo(berserk, new Vector(-1.5253, 0.5, 0.0266)), 67);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 89f, 0f), 68);
//		Utils.scheduleTask(() -> world.playSound(berserk.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f), 80);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, 90f, 60f);
//			Actions.setHotbarSlot(berserk, 5);
//		}, 87);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 114, 44)), 88);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 35f), 89);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 115, 45)), 90);
//		Utils.scheduleTask(() -> {
//			Actions.move(berserk, new Vector(-0.7637, 0, 0.7637), 1);
//			Actions.stonk(berserk, world.getBlockAt(18, 114, 45));
//		}, 91);
//		Utils.scheduleTask(() -> {
//			Actions.move(berserk, new Vector(0, 0, 1.08), 13);
//			Actions.stonk(berserk, world.getBlockAt(18, 115, 46));
//		}, 92);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 114, 46)), 93);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 115, 47)), 94);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 114, 47)), 95);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 115, 48)), 96);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 114, 48)), 97);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 115, 49)), 98);
//		Utils.scheduleTask(() -> {
//			Actions.stonk(berserk, world.getBlockAt(18, 114, 49));
//			world.playSound(berserk.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
//		}, 99);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 115, 50)), 100);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 114, 50)), 101);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 115, 51)), 102);
//		Utils.scheduleTask(() -> Actions.stonk(berserk, world.getBlockAt(18, 114, 51)), 103);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, -135f, 0f);
//			Actions.setHotbarSlot(berserk, 1);
//		}, 108);
//		Utils.scheduleTask(() -> Actions.bonzo(berserk, new Vector(-1.0787, 0.5, 1.0787)), 109);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 45f, 0f), 110);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 7), 111);
//		Utils.scheduleTask(() -> Actions.swingHand(berserk), 112); // equip phoenix
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 1), 113);
//		Utils.scheduleTask(() -> Actions.swapItems(berserk, 9, 39), 114);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Phoenix Procced!");
//			world.playSound(berserk.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1f, 1.6f);
//			Actions.setHotbarSlot(berserk, 7);
//		}, 120);
//		Utils.scheduleTask(() -> Actions.swingHand(berserk), 121); // equip black cat
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 1), 122);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 30f, 0f), 123);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.7015, 0, 1.215), 2), 124);
//		Utils.scheduleTask(() -> Actions.jump(berserk), 125);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.1403, 0, 0.243), 12), 126);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 11f, 0f), 137);
//
//		/*
//		 * ██████╗
//		 * ╚════██╗
//		 *  █████╔╝
//		 *  ╚═══██╗
//		 * ██████╔╝
//		 * ╚═════╝
//		 */
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 0f), 140);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0, 0, 1.403), 1), 141);
//		Utils.scheduleTask(() -> Actions.lavaJump(berserk, false), 148);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0, 0, 0.2806), 14), 150);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, -10.2f), 163);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.2806, 0, 0), 6), 164);
//		Utils.scheduleTask(() -> {
//			Actions.swingHand(berserk);
//			Server.turnArrow(world, true);
//			Goldor.broadcastTerminalComplete(berserk, "device", 4, 7);
//			Actions.move(berserk, new Vector(1.08, 0, 0), 1);
//		}, 170);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -5f, 0f), 171);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.1223, 0, 1.3977), 2), 172);
//		Utils.scheduleTask(() -> Actions.jump(berserk), 173);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.02446, 0, -0.2795), 12), 174);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 22f, 0f), 185);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.5256, 0, 1.301), 2), 186);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.1051, 0, 0.2602), 2), 188);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 66.5f, 29f), 189);
//		Utils.scheduleTask(() -> Actions.swingHand(berserk), 190);
//		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(berserk, "terminal", 6, 7), 191);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 2), 192);
//		Utils.scheduleTask(() -> Actions.leap(berserk, Mage.get()), 193);
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
//			Actions.turnHead(berserk, 153f, 0f);
//			Actions.setHotbarSlot(berserk, 1);
//		}, 194);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.637, 0, -1.25), 3), 195);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 153f, 82f), 197);
//		Utils.scheduleTask(() -> Actions.bonzo(berserk, new Vector(-0.6926, 0.5, -1.359)), 198);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 153f, 0f), 199);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 180f, 0f), 219);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0, 0, -1.403), 1), 220);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0, 0, -0.2806), 3), 221);
//		Utils.scheduleTask(() -> Actions.lavaJump(berserk, false), 227);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 0f), 228);
//		Utils.scheduleTask(() -> Actions.swingHand(berserk), 235);
//		Utils.scheduleTask(() -> {
//			Goldor.broadcastTerminalComplete(berserk, "terminal", 7, 7);
//			Bukkit.broadcastMessage(ChatColor.GREEN + "S4 finished in 41 ticks (2.05 seconds) | Terminals: 236 ticks (11.80 seconds) | Overall: 2 652 ticks (132.60 seconds)");
//			Server.openCore();
//		}, 236);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 2), 237);
//		Utils.scheduleTask(() -> Actions.leap(berserk, Mage.get()), 238);
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
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 239);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0, 0, -1.403), 11), 256);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -82.5f, -5f), 266);
//		// tick 267: swap to gdrag
//		Utils.scheduleTask(Berserk::shoot, 268);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 272);
//		Utils.scheduleTask(Berserk::shoot, 273);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 277);
//		Utils.scheduleTask(Berserk::shoot, 278);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 282);
//		Utils.scheduleTask(Berserk::shoot, 283);
//		Utils.scheduleTask(() -> Actions.salvation(berserk), 287);
//		Utils.scheduleTask(Berserk::shoot, 288);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 2), 289);
//		Utils.scheduleTask(() -> Actions.leap(berserk, Healer.get()), 290);
//		if(doContinue) {
//			Utils.scheduleTask(() -> necron(true), 350);
//		}
//	}
//
//	private static void necron(boolean doContinue) {
//		Actions.setHotbarSlot(berserk, 2);
//		Utils.scheduleTask(() -> Actions.leap(berserk, Tank.get()), 121);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 122);
//		for(int i = 161; i < 510; i += 5) {
//			Utils.scheduleTask(Berserk::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(berserk), i + 4);
//		}
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 2), 507);
//		Utils.scheduleTask(() -> Actions.leap(berserk, Healer.get()), 508);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -122f, 0f), 509);
//		// tick 510: equip black cat
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.1898, 0, -0.7435), 20), 511);
//		Utils.scheduleTask(() -> Actions.jump(berserk), 530);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.238, 0, -0.1487), 9), 531);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.1898, 0, -0.7435), 5), 540);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -79.7f, 19.1f), 544);
//		Utils.scheduleTask(() -> Actions.swapItems(berserk, 12, 39), 545);
//		if(doContinue) {
//			Utils.scheduleTask(Berserk::witherKing, 609);
//		}
//	}
//
//	private static void witherKing() {
//		Utils.scheduleTask(() -> WitherKing.pickUpRelic(berserk), 1);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 115f, 0f), 2);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.2716, 0, -0.5929), 23), 3);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 0f), 25);
//		Utils.scheduleTask(() -> WitherKing.placeRelic(berserk), 26);
//		// tick 27: equip greg
//		Utils.scheduleTask(() -> {
//			Actions.swapItems(berserk, 6, 33);
//			Actions.swapItems(berserk, 12, 39);
//		}, 28);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, 18.9f, 0f);
//			Actions.setHotbarSlot(berserk, 6);
//		}, 29);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.3636, 0, 1.062), 4), 30);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.0909, 0, 0.2655), 5), 34);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.3636, 0, 1.062), 40), 39);
//		Utils.scheduleTask(() -> Actions.jump(berserk), 78);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.0909, 0, 0.2655), 9), 79);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.3636, 0, 1.062), 13), 88);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -109.3f, -14f), 100);
//		Utils.scheduleTask(() -> Actions.rag(berserk), 150);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 211);
//		// arrows take 13 ticks to reach the Dragon
//		// rag on tick 150
//		// rag is activated on tick 210
//		// start shooting tick 388
//		// begin moving tick 388
//		// Dragon spawns tick 401
//		// last arrow fired tick 408
//		// rag wears off tick 410
//		// rag is back tick 550
//		for(int i = 383; i < 410; i += 5) {
//			Utils.scheduleTask(Berserk::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(berserk), i + 1);
//		}
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.0593, 0, -0.371), 20), 388);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -109.3f, -17f), 395);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -109.3f, -20f), 405);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, 146.5f, 0f);
//			WitherKing.playDragonDeathSound(true);
//			Bukkit.broadcastMessage(ChatColor.BLUE + "Ice Dragon " + ChatColor.GREEN + "killed in 9 ticks (0.45 seconds) | Wither King: 410 ticks (20.50 seconds) | Overall: 3 785 ticks (189.25 seconds)");
//		}, 410);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.6195, 0, -0.936), 9), 411);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.155, 0, -0.234), 5), 420);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.6195, 0, -0.936), 2), 425);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -134.1f, -17f), 426);
//		// arrows take 13 ticks to reach the Dragon
//		// start shooting tick 728
//		// begin moving tick 728
//		// last arrow fired tick 738
//		// Dragon spawns tick 741
//		for(int i = 723; i < 740; i += 5) {
//			Utils.scheduleTask(Berserk::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(berserk), i + 1);
//		}
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.806, 0, -0.781), 10), 728);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -134.1f, -20f), 735);
//		Utils.scheduleTask(() -> {
//			Actions.turnHead(berserk, -151f, 0f);
//			Actions.setHotbarSlot(berserk, 6);
//		}, 739);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.5442, 0, -0.9817), 35), 740);
//		Utils.scheduleTask(() -> Actions.rag(berserk), 750);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 79.6f, -17f), 775);
//		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 811);
//		// arrows take 13 ticks to reach the Dragon
//		// start shooting tick 834
//		// begin moving tick 834
//		// last arrow fired tick 844
//		// Dragon spawns tick 847
//		for(int i = 829; i < 850; i += 5) {
//			Utils.scheduleTask(Berserk::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(berserk), i + 1);
//		}
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.104, 0, 0.2026), 10), 834);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 79.6f, -20f), 840);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, -17.2f, 0f), 851);
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.3319, 0, 1.072), 23), 852);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 70.6f, -17f), 875);
//		// arrows take 13 ticks to reach the Dragon
//		// start shooting tick 940
//		// begin moving tick 940
//		// last arrow fired tick 950
//		// Dragon spawns tick 953
//		for(int i = 935; i < 955; i += 5) {
//			Utils.scheduleTask(Berserk::shoot, i);
//			Utils.scheduleTask(() -> Actions.salvation(berserk), i + 1);
//		}
//		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.0587, 0, 0.373), 10), 940);
//		Utils.scheduleTask(() -> Actions.turnHead(berserk, 70.6f, -20f), 950);
//	}
//
//	private static void shoot() {
//		Actions.rightClickOld(berserk);
//		Location l = berserk.getLocation();
//		l.add(l.getDirection());
//		l.setY(l.getY() + 1.62);
//
//		// Duplex Arrow
//		Utils.scheduleTask(() -> {
//
//			double powerBonus;
//			try {
//				int power = berserk.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.POWER);
//				powerBonus = power * 0.05;
//				if(power == 7) {
//					powerBonus += 0.05;
//				}
//			} catch(Exception exception) {
//				powerBonus = 0;
//			}
//
//			double strengthBonus;
//			try {
//				strengthBonus = 0.15 + 0.15 * Objects.requireNonNull(berserk.getPotionEffect(PotionEffectType.STRENGTH)).getAmplifier();
//			} catch(Exception exception) {
//				strengthBonus = 0;
//			}
//
//			double add = powerBonus + strengthBonus;
//			Arrow arrow = world.spawnArrow(l, l.getDirection(), 4, 0);
//			arrow.setDamage(0.5 + add);
//			arrow.setPierceLevel(4);
//			arrow.setShooter(berserk);
//			arrow.setWeapon(berserk.getInventory().getItemInMainHand());
//			arrow.addScoreboardTag("TerminatorArrow");
//		}, 3);
//
//		PotionEffect strength = berserk.getPotionEffect(PotionEffectType.STRENGTH);
//		boolean hasRagBuff = berserk.getScoreboardTags().contains("RagBuff");
//		int maxAmplifier = hasRagBuff ? 12 : 10;
//		int baseAmplifier = hasRagBuff ? 2 : 0;
//
//		int newAmplifier;
//		if(strength == null) {
//			newAmplifier = baseAmplifier;
//		} else {
//			newAmplifier = Math.min(strength.getAmplifier() + 1, maxAmplifier);
//		}
//		berserk.removePotionEffect(PotionEffectType.STRENGTH);
//		berserk.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, newAmplifier));
//	}

	@SuppressWarnings("unused")
	public static Player get() {
		return berserk;
	}
}