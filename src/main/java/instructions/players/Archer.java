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
				Actions.swapItems(archer, 11, 36);
				Actions.setHotbarSlot(archer, 5);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
			case "storm" -> {
				Utils.teleport(archer, new Location(world, 65.087, 165, 69.329, 180f, -4f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 6, 33);
				Actions.swapItems(archer, 7, 34);
				Actions.setHotbarSlot(archer, 4);
				Utils.scheduleTask(() -> storm(false), 60);
			}
			case "goldor" -> {
				Utils.teleport(archer, new Location(world, 63.811, 127, 35.940, -74.5f, -46f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 5, 32);
				Actions.swapItems(archer, 6, 33);
				Actions.swapItems(archer, 7, 34);
				Actions.swapItems(archer, 12, 39);
				Actions.setHotbarSlot(archer, 5);
				Utils.scheduleTask(() -> Actions.dropItem(archer, false), 1);
				Utils.scheduleTask(() -> Actions.rightClick(archer), 19);
				Utils.scheduleTask(() -> Actions.stopRightClick(archer), 26);
				Utils.scheduleTask(() -> Actions.rightClick(archer), 27);
				Utils.scheduleTask(() -> Actions.stopRightClick(archer), 34);
				Utils.scheduleTask(() -> Actions.rightClick(archer), 35);
				Utils.scheduleTask(() -> Actions.stopRightClick(archer), 42);
				Utils.scheduleTask(() -> {
					Actions.turnHead(archer, -10.5f, -15.5f);
					Actions.setHotbarSlot(archer, 4);
				}, 45);
				Utils.scheduleTask(() -> {
					Actions.swapItems(archer, 18, 39);
					Actions.swapItems(archer, 19, 38);
					Actions.swapItems(archer, 20, 37);
					Actions.swapItems(archer, 21, 36);
				}, 46);
				Utils.scheduleTask(() -> Actions.dropItem(archer, true), 50);
				Utils.scheduleTask(() -> Actions.turnHead(archer, -3f, -4f), 51);
				Utils.scheduleTask(() -> Actions.turnHead(archer, -15f, -2.5f), 55);
				Utils.scheduleTask(() -> Actions.rightClick(archer), 56);
				Utils.scheduleTask(() -> Actions.turnHead(archer, -3f, 4f), 57);
				// rapid fire #2 at 58
				Utils.scheduleTask(() -> Actions.turnHead(archer, -14.5f, 5f), 59);
				Utils.scheduleTask(() -> goldor(false), 60);
			}
			case "necron" -> {
				Utils.teleport(archer, new Location(world, 54.524, 64, 100.707, 180f, -6f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 5, 32);
				Actions.swapItems(archer, 6, 33);
				Actions.swapItems(archer, 7, 34);
				Utils.scheduleTask(() -> necron(false), 60);
			}
			case "witherking" -> {
				Utils.teleport(archer, new Location(world, 22.3, 6, 59.361, 90f, 0f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 6, 33);
				Actions.swapItems(archer, 7, 35);
				Actions.swapItems(archer, 13, 39);
				Actions.setHotbarSlot(archer, 5);
				Utils.scheduleTask(Archer::witherKing, 60);
			}
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
		// Left-click the wither door to open it — requires the Wither Key from killing archaeologist I (handled by
		// Actions.leftClick's door detection; the old hard openWitherDoor() call is gone).
		Utils.scheduleTask(() -> Actions.leftClick(archer), 25); // opens tick 45
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Crypt 1/5");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Gravel 1/6 (Opened Chest)");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Gravel 2/6 (Killed Bat)");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Gravel Cleared");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Gravel 3/6 (Picked Up Item)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.ITEM);
		}, 83);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 84); // etherwarp to wither essence
		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 55f), 85);
		Utils.scheduleTask(() -> {
			Actions.leftClick(archer);
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Gravel 4/6 (Obtained Wither Essence)");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Gravel 5/6 (Picked Up Item)");
			Utils.playSecretFoundSound(archer, Utils.SecretType.ITEM);
		}, 98);
		Utils.scheduleTask(() -> {
			Actions.leftClick(archer);
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Gravel 6/6 (Obtained Wither Essence)");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Dino Dig Site Cleared");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Dino Dig Site 1/4 (Opened Chest)");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Dino Dig Site 2/4 (Picked Up Item)");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Dino Dig Site 3/4 (Opened Chest)");
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
			Utils.timer(ChatColor.DARK_GREEN + "Archer: Dino Dig Site 4/4 (Killed Bat)");
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
			Actions.swapItems(archer, 11, 36);
			Actions.setHotbarSlot(archer, 5);
		}, 619);
		// Boss handoff (teleport to boss spawn + maxor(true)) is now driven by the Watcher's portal entry — see
		// Watcher.enterPortal / the maxorHandoff armed in TAS.runTAS.
	}

	public static void maxor(boolean doContinue) {
		Utils.scheduleTask(() -> Actions.turnHead(archer, 13f, 0f), 1);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 2);
		Utils.scheduleTask(() -> Actions.move(archer, "WN", 0), 25); // spring boots to right crystal
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 49), 38);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 60);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 61);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 133f, 0f), 63);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -52f, 0f), 88); // land on pad
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 161); // spring boots again to right crystal
		Utils.scheduleTask(() -> Actions.move(archer, "WN", 0), 162);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 22), 173);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 11, 36), 200);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 240);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 128f, 0f), 241);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 24), 242);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 243);
		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 339);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -76f, 0f);
			Actions.setHotbarSlot(archer, 4);
		}, 340);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 9), 341); // move to storm clear spot
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, 0f), 350);
		// storm() is now started by Maxor.chainNext (player handoff armed in TAS.runTAS).
	}

	public static void storm(boolean doContinue) {
		for(int i = 1; i <= 79; i += 5) {
			Utils.scheduleTask(() -> Actions.aimTerminatorAtNearestEnemy(archer), i - 1);
			Utils.scheduleTask(() -> Actions.loopRightClick(archer), i);
		}
		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, -4f), 79);
		Utils.scheduleTask(() -> Actions.move(archer, "DN", 60), 80);
		for(int i = 80; i <= 140; i += 5) {
			Utils.scheduleTask(() -> Actions.rightClick(archer), i);
		} // clear mid
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 141);
		Utils.scheduleTask(() -> Actions.leap(archer, Tank.get()), 142); // leap to help clear green pad
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, 0f);
			Actions.setHotbarSlot(archer, 4);
		}, 143);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 144);
		Utils.scheduleTask(() -> Actions.move(archer, "WN", 3), 163);
		for(int i = 165; i <= 520; i += 5) {
			Utils.scheduleTask(() -> Actions.aimTerminatorAtNearestEnemy(archer), i - 1);
			Utils.scheduleTask(() -> Actions.loopRightClick(archer), i);
		}
		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 30f), 525);
		Utils.scheduleTask(() -> Actions.move(archer, "D", 0), 526);
		Utils.scheduleTask(() -> Actions.move(archer, "DN", 2), 536);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 5);
			Actions.swapItems(archer, 12, 39); // put on racing helmet -> speed auto-set to 650
		}, 545);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 95), 546); // go to goldor
		Utils.scheduleTask(() -> Actions.leftClick(archer), 576);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 577);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 578);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 579);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 580);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 581);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 582);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 583);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 584);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 585);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -90f, 0f);
			Actions.setHotbarSlot(archer, 1);
		}, 586);
		// tick 587: begin falling
		Utils.scheduleTask(() -> Actions.turnHead(archer, -109f, 0f), 602);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 603); // bonzo off opposite wall | lands in 34 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 71f, 0f);
			Actions.setHotbarSlot(archer, 5);
		}, 604);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 45f, 0f), 638);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 71f, 0f), 640);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 641);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 642);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 643);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 644);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 75f, 30f), 645);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 646);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 647);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 648);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 649);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 0f), 650);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 45f, 0f), 684);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 685);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 687);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 689);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 125f, 80f);
			Actions.setHotbarSlot(archer, 1);
		}, 700);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 702); // bonzo towards i4
		Utils.scheduleTask(() -> Actions.turnHead(archer, 86f, 0f), 717);
		Utils.scheduleTask(() -> Actions.move(archer, "WPJ", 7), 718);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -74.5f, -46f);
			Actions.swapItems(archer, 5, 32);
			Actions.setHotbarSlot(archer, 5);
		}, 726);
		if(doContinue) {
			Utils.scheduleTask(() -> Actions.dropItem(archer, false), 802);
			Utils.scheduleTask(() -> Actions.rightClick(archer), 820);
			Utils.scheduleTask(() -> Actions.stopRightClick(archer), 827);
			Utils.scheduleTask(() -> Actions.rightClick(archer), 828);
			Utils.scheduleTask(() -> Actions.stopRightClick(archer), 835);
			Utils.scheduleTask(() -> Actions.rightClick(archer), 836);
			Utils.scheduleTask(() -> Actions.stopRightClick(archer), 843);
			Utils.scheduleTask(() -> {
				Actions.turnHead(archer, -10.5f, -15.5f);
				Actions.setHotbarSlot(archer, 4);
			}, 844);
			Utils.scheduleTask(() -> {
				Actions.swapItems(archer, 18, 39);
				Actions.swapItems(archer, 19, 38);
				Actions.swapItems(archer, 20, 37);
				Actions.swapItems(archer, 21, 36);
			}, 845);
			Utils.scheduleTask(() -> Actions.dropItem(archer, true), 850);
			Utils.scheduleTask(() -> Actions.turnHead(archer, -3f, -4f), 851);
			Utils.scheduleTask(() -> Actions.turnHead(archer, -15f, -2.5f), 855);
			Utils.scheduleTask(() -> Actions.rightClick(archer), 856);
			Utils.scheduleTask(() -> Actions.turnHead(archer, -3f, 4f), 857);
			// rapid fire #2 at 858
			Utils.scheduleTask(() -> Actions.turnHead(archer, -14.5f, 5f), 859);
			// goldor() is now started by Storm.chainNext (player handoff armed in TAS.runTAS).
		}
	}

	public static void goldor(boolean doContinue) {
		/*
		 * ██╗██╗  ██╗
		 * ╚═╝██║  ██║
		 * ██╗███████║
		 * ██║╚════██║
		 * ██║     ██║
		 * ╚═╝     ╚═╝
		 */
		Actions.rightClick(archer);
		Utils.scheduleTask(() -> {
			Actions.swapItems(archer, 5, 32);
			Actions.swapItems(archer, 18, 39);
			Actions.swapItems(archer, 19, 38);
			Actions.swapItems(archer, 20, 37);
			Actions.swapItems(archer, 21, 36);
		}, 1);

		/*
		 * ███████╗███████╗██████╗
		 * ██╔════╝██╔════╝╚════██╗
		 * █████╗  █████╗   █████╔╝
		 * ██╔══╝  ██╔══╝   ╚═══██╗
		 * ███████╗███████╗██████╔╝
		 * ╚══════╝╚══════╝╚═════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, 81f, 0f), 2);
		// tick 5: device completes
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 6);
		Utils.scheduleTask(() -> Actions.move(archer, "WPJ", 0), 8);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 10);
		Utils.scheduleTask(() -> Actions.move(archer, "WPJ", 0), 23);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 25);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 81f, 80f);
			Actions.setHotbarSlot(archer, 1);
		}, 39);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 40);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 81f, 0f);
			Actions.setHotbarSlot(archer, 5);
		}, 41);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 9, 39), 60);
		// tick 60: death tick
		Utils.scheduleTask(() -> {
			Utils.playLocalSound(archer, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 2.0f, 2.0f);
			Bukkit.broadcastMessage(ChatColor.GOLD + Utils.getRealName(archer) + " used Spirit Mask!");
		}, 61);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 9, 39), 62);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 30f), 67);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 68);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 70);
		Utils.scheduleTask(() -> {
			Actions.leftClick(archer);
			Actions.move(archer, "WAP", 0);
		}, 71);
		Utils.scheduleTask(() -> {
			Actions.leftClick(archer);
			Actions.move(archer, "WP", 17);
		}, 72);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 73);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 74);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 75);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 55f, 0f), 77);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 120f, 0f), 91);
		// tick 110: tank leaps
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 1);
			Actions.turnHead(archer, 22.5f, 0f);
		}, 111);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 15), 112);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 22.5f, 80f), 114);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 115);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 22.5f, 0f), 116);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 10, 39), 119);
		// tick 120: death tick
		Utils.scheduleTask(() -> {
			Utils.playLocalSound(archer, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 2.0f, 2.0f);
			Bukkit.broadcastMessage(ChatColor.GOLD + Utils.getRealName(archer) + " used Bonzo Mask!");
		}, 120);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 10, 39), 121);

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 *  ╚═══██╗
		 * ██████╔╝
		 * ╚═════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 0f), 122);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -25f, 0f), 128);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 26), 129);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, -2f);
			Actions.setHotbarSlot(archer, 5);
		}, 132);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 133);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 134);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 135);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 80f);
			Actions.setHotbarSlot(archer, 1);
		}, 137);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 138);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 6f, 0f);
			Actions.setHotbarSlot(archer, 2);
		}, 139);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 55f, 2f), 155);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 156);
		// tick 157: terminal completes

		Utils.scheduleTask(() -> Actions.leap(archer, Mage.get()), 158);

		/*
		 * ██╗  ██╗
		 * ██║  ██║
		 * ███████║
		 * ╚════██║
		 *      ██║
		 *      ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, 130f, 0f), 159);
		Utils.scheduleTask(() -> {
			Actions.move(archer, "WP", 15);
			Actions.setHotbarSlot(archer, 5);
		}, 160);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 138f, 32f), 174);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 175); // s4 1
		// tick 176: terminal completes

		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, -4f), 177);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 5), 178);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 189);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 190); // s4 2
		// tick 191: terminal completes

		Utils.scheduleTask(() -> Actions.leap(archer, Mage.get()), 192);

		/*
		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
		 * █████╗  ██║██║  ███╗███████║   ██║
		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
		 * ██║     ██║╚██████╔╝██║  ██║   ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.move(archer, "WP", 35);
			Actions.setHotbarSlot(archer, 5);
		}, 193);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 42f), 227);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 228);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 0f), 229);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, -6f), 240);
		// lands at tick 271
		Utils.scheduleTask(() -> Actions.turnHead(archer, 42f, 66f), 272);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 273);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 274);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 123f, 55f), 275);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 276);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 277);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -160f, 52f), 278);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 279);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 281);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -73f, 58f), 282);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 283);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 284);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, 90f), 285);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 298);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 299);
		Utils.scheduleTask(() -> Actions.leap(archer, Mage.get()), 300);
		Utils.scheduleTask(() -> {
			Actions.swapItems(archer, 5, 32);
			Actions.swapItems(archer, 12, 39);
		}, 301);
	}

	public static void necron(boolean doContinue) {
		Actions.setHotbarSlot(archer, 5);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 1);
		Utils.scheduleTask(() -> Actions.stopRightClick(archer), 152);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 5, 32), 153);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, 62f);
			Actions.move(archer, "WP", 0);
			Actions.setHotbarSlot(archer, 5);
		}, 154);
		Utils.scheduleTask(() -> Actions.move(archer, "WPJ", 0), 155);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 59), 157);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 215);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 216);
		Utils.scheduleTask(() -> Actions.leftClick(archer), 217);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 0f), 218);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 13, 39), 219);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 121.3f, 0f), 503);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 0), 504);
		Utils.scheduleTask(() -> Actions.move(archer, "WPJ", 0), 522);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 9), 524);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 533);
	}

	private static void witherKing() {
		Actions.rightClick(archer);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -120f, -7f), 1);
		Utils.scheduleTask(() -> Actions.move(archer, "WP", 22), 2);
		Utils.scheduleTask(() -> Actions.rightClick(archer), 24);
	}

	public static Player get() {
		return archer;
	}
}
