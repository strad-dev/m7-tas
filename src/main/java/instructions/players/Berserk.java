package instructions.players;

import instructions.Actions;
import instructions.Server;
import instructions.bosses.storm.Storm;
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
			case "storm" -> {
				Utils.teleport(berserk, new Location(world, 114.145, 170, 93.962, 154f, 0f));
				Actions.swapItems(berserk, 1, 28);
				Actions.swapItems(berserk, 3, 30);
				Actions.swapItems(berserk, 6, 33);
				Actions.swapItems(berserk, 7, 34);
				Actions.setHotbarSlot(berserk, 3);
				Utils.scheduleTask(() -> storm(false), 60);
			}
			case "goldor" -> {
				Utils.teleport(berserk, new Location(world, 91.351, 115, 133.781, -145f, -5f));
				Actions.swapItems(berserk, 1, 28);
				Actions.swapItems(berserk, 3, 30);
				Actions.swapItems(berserk, 6, 33);
				Actions.swapItems(berserk, 7, 34);
				Actions.swapItems(berserk, 12, 39);
				Actions.setHotbarSlot(berserk, 1);
				Utils.scheduleTask(() -> Actions.swapItems(berserk, 7, 35), 1);
				Utils.scheduleTask(() -> Actions.dropItem(berserk, true), 49);
				Utils.scheduleTask(() -> goldor(false), 60);
			}
			case "necron" -> {
				Utils.teleport(berserk, new Location(world, 54.524, 64, 100.707, 180f, -6f));
				Actions.swapItems(berserk, 1, 28);
				Actions.swapItems(berserk, 3, 30);
				Actions.swapItems(berserk, 6, 33);
				Actions.swapItems(berserk, 7, 34);
				Utils.scheduleTask(() -> Actions.swapItems(berserk, 7, 35), 1);
				Utils.scheduleTask(() -> necron(false), 60);
			}
			case "witherking" -> {
				Utils.teleport(berserk, new Location(world, 90.7, 6, 56.507, -90f, 0f));
				Actions.swapItems(berserk, 1, 28);
				Actions.swapItems(berserk, 6, 33);
				Actions.swapItems(berserk, 7, 35);
				Actions.swapItems(berserk, 13, 39);
				Actions.setHotbarSlot(berserk, 5);
				Utils.scheduleTask(() -> Actions.swapItems(berserk, 7, 35), 1);
				Utils.scheduleTask(Berserk::witherKing, 60);
			}
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
			Utils.timer(ChatColor.RED + "Berserk found a Special Crystal!");
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
			Utils.timer(ChatColor.RED + "Berserk: Wizard 1/4 (Opened Chest)");
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
			Utils.timer(ChatColor.RED + "Berserk: Wizard 2/4 (Killed Bat)");
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
			Utils.timer(ChatColor.RED + "Berserk: Wizard 3/4 (Picked Up Item)");
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
			Utils.timer(ChatColor.RED + "Berserk: Wizard 4/4 (Opened Chest)");
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
			Utils.timer(ChatColor.RED + "Berserk: Well 1/7 (Opened Chest)");
			Utils.playLocalSound(berserk, Sound.BLOCK_CHEST_OPEN);
			Utils.playSecretFoundSound(berserk, Utils.SecretType.CHEST);
		}, 71);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -67f, -18f), 72);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 73); // reposition for next secret
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 85f, 17.5f), 74);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 75); // etherwarp to secret
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -96.3f, 3.1f);
			Utils.timer(ChatColor.RED + "Berserk: Well 2/7 (Picked Up Item)");
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
			Utils.timer(ChatColor.RED + "Berserk: Well 3/7 (Opened Chest)");
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
			Utils.timer(ChatColor.RED + "Berserk: Well 4/7 (Obtained Wither Essence)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.ESSENCE);
		}, 95);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 56f, 22f), 96);
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Utils.timer(ChatColor.RED + "Berserk: Well 5/7 (Obtained Wither Essence)");
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
			Utils.timer(ChatColor.RED + "Berserk: Well 6/7 (Opened Chest)");
			Utils.playSecretFoundSound(berserk, Utils.SecretType.CHEST);
		}, 104);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -45f, 0f), 105); // pearl lands
		Utils.scheduleTask(() -> {
			Actions.leftClick(berserk);
			Utils.timer(ChatColor.RED + "Berserk: Well Cleared");
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
			Utils.timer(ChatColor.RED + "Berserk: Well 7/7 (Picked Up Item)");
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
			Actions.swapItems(berserk, 4, 31);
			Actions.swapItems(berserk, 6, 33);
			Actions.swapItems(berserk, 7, 34);
			Actions.setHotbarSlot(berserk, 5);
		}, 248);
		Utils.scheduleTask(Server.IceFill::stopIceFillTask, 742);
		// Boss handoff (teleport to boss spawn + maxor(true)) is now driven by the Watcher's portal entry — see
		// Watcher.enterPortal / the maxorHandoff armed in TAS.runTAS.
	}

	public static void maxor(boolean doContinue) {
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 30f);
			Actions.move(berserk, "WP", 17);
		}, 3);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 18);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 19);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 34f, 0f), 21);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 70), 22); // move to leap spot for healer
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -145f, 0f), 287);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 288); // move to leap spot for mage1
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 0f), 297);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -32f, 0f), 337);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -32f, 80f);
			Actions.move(berserk, "WPJ", 0);
			Actions.setHotbarSlot(berserk, 1);
		}, 354);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 355); // bonzo to purple pad
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -32f, 0f);
			Actions.move(berserk, "WP", 21);
		}, 356);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 154f, 0f);
			Actions.setHotbarSlot(berserk, 3);
		}, 377);
		// storm() is now started by Maxor.chainNext (player handoff armed in TAS.runTAS).
	}

	public static void storm(boolean doContinue) {
		for(int i = 0; i <= 80; i += 5) {
			Utils.scheduleTask(() -> Actions.snapHeadToNearestEnemy(berserk), i - 1);
			Utils.scheduleTask(() -> Actions.loopLeftClick(berserk), i);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 0), 81);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 82);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 85);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 88);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 91);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 3), 92);
		for(int i = 95; i <= 150; i += 3) {
			Utils.scheduleTask(() -> Actions.snapHeadToNearestEnemy(berserk), i - 1);
			Utils.scheduleTask(() -> Actions.loopLeftClick(berserk), i);
		} // clear pad, including shadow assassin
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 154f, 0f), 151);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, "WP", 31);
			Actions.setHotbarSlot(berserk, 1);
		}, 152); // move off the pad early enough
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 154f, 80f), 159);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 160); // bonzo back to pillar
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 154f, 0f);
			Actions.setHotbarSlot(berserk, 3);
		}, 161);
		for(int i = 187; i <= 532; i += 5) {
			Utils.scheduleTask(() -> Actions.snapHeadToNearestEnemy(berserk), i - 1);
			Utils.scheduleTask(() -> Actions.loopLeftClick(berserk), i);
		} // kill outstanding wither skeletons
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -155f, -90f);
			Actions.setHotbarSlot(berserk, 5);
			Actions.swapItems(berserk, 5, 32);
		}, 533);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 534);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 2), 546);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 6);
			Actions.swapItems(berserk, 5, 32);
		}, 595); // last breath
		int lbFor = 8;
		for(int i = 597; i <= 678 - lbFor - 1; i += lbFor + 1) {
			Utils.scheduleTask(() -> Actions.rightClick(berserk), i);
			Utils.scheduleTask(() -> Actions.stopRightClick(berserk), i + lbFor);
		}
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 3);
			Actions.snapHeadAtEntity(berserk, Storm.INSTANCE.getBoss());
		}, 679);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 680);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(berserk, 4);
			Actions.swapItems(berserk, 12, 39); // equip racing helmet (speed auto-set to 650), black cat, and prepare explosive bow
			Actions.swapItems(berserk, 7, 35);
		}, 681);
		Utils.scheduleTask(() -> Actions.leap(berserk, Archer.get()), 682);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, "AWP", 0);
			Actions.setHotbarSlot(berserk, 1);
		}, 683);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 685);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 12f, 80f), 707);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 708); // bonzo staff here because there is less time to build up momentum | lands in 12 ticks
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 0f), 709);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, "WPJ", 1);
			Actions.turnHead(berserk, -10f, 80f);
		}, 729);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 730);
		// pause to allow time to leap
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 0f), 745);
		Utils.scheduleTask(() -> Actions.move(berserk, "SN", 0), 746);
		Utils.scheduleTask(() -> Actions.move(berserk, "N", 10), 754);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -47f, 80f), 755);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 80f), 784);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 785);
		Utils.scheduleTask(() -> Actions.move(berserk, "WPJ", 0), 786);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 787);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 788);
		Utils.scheduleTask(() -> Actions.move(berserk, "WPJ", 0), 802);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 803);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 3f, 0f);
			Actions.setHotbarSlot(berserk, 5);
			Actions.move(berserk, "WP", 39);
		}, 805);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 45f), 821);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 822);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 823);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 824);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 825);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 826);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 827);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 111f, 0f), 836);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -145f, -5);
			Actions.setHotbarSlot(berserk, 1);
		}, 845);
		if(doContinue) {
			Utils.scheduleTask(() -> Actions.dropItem(berserk, true), 849);
			// goldor() is now started by Storm.chainNext (player handoff armed in TAS.runTAS).
		}
	}

	public static void goldor(boolean doContinue) {
		/*
		 *  ██╗
		 * ███║
		 * ╚██║
		 *  ██║
		 *  ██║
		 *  ╚═╝
		 */
		Actions.turnHead(berserk, 110f, 80f);
		Actions.setHotbarSlot(berserk, 1);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 25), 1);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 2);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 110f, 0f);
			Actions.setHotbarSlot(berserk, 5);
		}, 3);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 170f, 5f), 26);
		// tick 37: tank leaps
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 65f, 0f), 38);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 39);
		Utils.scheduleTask(() -> Actions.move(berserk, "WPJ", 10), 41);

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 * ██╔═══╝
		 * ███████╗
		 * ╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 58f, 80f), 51);
		// tick 52: tank leaps
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, -30f), 53);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, "WP", 8);
			Actions.setHotbarSlot(berserk, 7);
		}, 54);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 58);
		// holy shit nosethe clutch with the explosive bow idea
		Utils.scheduleTask(() -> Actions.stopRightClick(berserk), 71);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -150f, 10f);
			Actions.setHotbarSlot(berserk, 5);
		}, 72);
		Utils.scheduleTask(() -> Actions.move(berserk, "WPD", 0), 73);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 26), 74);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 180f, 10f), 96);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 97); // s2 5
		// tick 98: terminal completes

		Utils.scheduleTask(() -> Actions.turnHead(berserk, -95f, 10f), 99);
		Utils.scheduleTask(() -> Actions.move(berserk, "WAP", 0), 100);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 6), 102);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 5), 117);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 121); // s2 3
		// tick 122: terminal completes

		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 123);
		Utils.scheduleTask(() -> Actions.leap(berserk, Archer.get()), 128); // better positioning

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 *  ╚═══██╗
		 * ██████╔╝
		 * ╚═════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -47.5f, 90f);
			Actions.setHotbarSlot(berserk, 1);
		}, 129);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 130);
		Utils.scheduleTask(() -> Actions.move(berserk, "WPJ", 0), 131);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 133);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -47.5f, 28f);
			Actions.setHotbarSlot(berserk, 4);
		}, 134);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 16), 135);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -30f, -45f), 149);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 169);
		// tick 170: terminal activates

		Utils.scheduleTask(() -> Actions.leap(berserk, Mage.get()), 171);

		/*
		 * ██╗  ██╗
		 * ██║  ██║
		 * ███████║
		 * ╚════██║
		 *      ██║
		 *      ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 15), 172);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 187); // s4 3
		// tick 188: terminal completes

		Utils.scheduleTask(() -> Actions.leap(berserk, Mage.get()), 191);

		/*
		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
		 * █████╗  ██║██║  ███╗███████║   ██║
		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
		 * ██║     ██║╚██████╔╝██║  ██║   ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 180f, 0f);
			Actions.setHotbarSlot(berserk, 3);
		}, 200);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 5), 219);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -111f, -2f);
			Actions.swapItems(berserk, 12, 39);
		}, 223);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 224);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 225);
		Utils.scheduleTask(() -> Actions.leap(berserk, Archer.get()), 245);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 246);
		Utils.scheduleTask(() -> Actions.move(berserk, "WN", 0), 287);
		Utils.scheduleTask(() -> Actions.move(berserk, "N", 10), 297);
	}

	public static void necron(boolean doContinue) {
		Actions.setHotbarSlot(berserk, 6);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 1);
		Utils.scheduleTask(() -> Actions.stopRightClick(berserk), 152);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 3), 153);
		Utils.scheduleTask(() -> Actions.leftClick(berserk), 160);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 6), 161);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 162);
		Utils.scheduleTask(() -> Actions.stopRightClick(berserk), 183);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 3), 184);
		for(int i = 185; i <= 360; i += 5) {
			Utils.scheduleTask(() -> Actions.leftClick(berserk), i);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 6), 361);
		Utils.scheduleTask(() -> Actions.rightClick(berserk), 362);
		Utils.scheduleTask(() -> Actions.stopRightClick(berserk), 383);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 3), 384);
		for(int i = 385; i <= 500; i += 5) {
			Utils.scheduleTask(() -> Actions.leftClick(berserk), i);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(berserk, 4), 501);
		Utils.scheduleTask(() -> Actions.leap(berserk, Archer.get()), 502);
		Utils.scheduleTask(() -> Actions.swapItems(berserk, 13, 39), 503);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -122f, 0f);
			Actions.setHotbarSlot(berserk, 5);
		}, 504);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 0), 505);
		Utils.scheduleTask(() -> Actions.move(berserk, "WPJ", 0), 523);
		Utils.scheduleTask(() -> Actions.move(berserk, "WP", 12), 525);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 0f), 537);
		Utils.scheduleTask(() -> Actions.swapItems(berserk, 3, 30), 538);
	}

	private static void witherKing() {
		Actions.rightClick(berserk);
	}

	@SuppressWarnings("unused")
	public static Player get() {
		return berserk;
	}
}