package instructions.players;

import instructions.Actions;
import instructions.bosses.storm.Storm;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import plugin.Utils;

public class Healer {
	private static Player healer;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;

	// Healer --> Mage4
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
				Actions.swapItems(healer, 12, 39);
				Actions.setHotbarSlot(healer, 5);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
			case "storm" -> {
				Utils.teleport(healer, new Location(world, 32.702, 170, 95.151, -155f, 0f));
				Actions.swapItems(healer, 1, 28);
				Actions.swapItems(healer, 3, 30);
				Actions.swapItems(healer, 6, 33);
				Actions.swapItems(healer, 7, 34);
				Actions.setHotbarSlot(healer, 3);
				Utils.scheduleTask(() -> storm(false), 60);
			}
			case "goldor" -> {
				Utils.teleport(healer, new Location(world, 107.622, 120, 93.618, -123f, 1f));
				Actions.swapItems(healer, 1, 28);
				Actions.swapItems(healer, 3, 30);
				Actions.swapItems(healer, 6, 33);
				Actions.swapItems(healer, 7, 34);
				Actions.swapItems(healer, 12, 39);
				Actions.setHotbarSlot(healer, 5);
				Utils.scheduleTask(() -> goldor(false), 60);
			}
			case "necron" -> {
				Utils.teleport(healer, new Location(world, 54.524, 64, 100.707, 180f, -6f));
				Actions.swapItems(healer, 1, 28);
				Actions.swapItems(healer, 3, 30);
				Actions.swapItems(healer, 6, 33);
				Actions.swapItems(healer, 7, 34);
				Utils.scheduleTask(() -> necron(false), 60);
			}
			case "witherking" -> {
				Utils.teleport(healer, new Location(world, 56.321, 8, 130.7, 0f, 0f));
				Actions.swapItems(healer, 1, 28);
				Actions.swapItems(healer, 6, 33);
				Actions.swapItems(healer, 7, 34);
				Actions.swapItems(healer, 13, 39);
				Actions.setHotbarSlot(healer, 5);
				Utils.scheduleTask(Healer::witherKing, 60);
			}
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
			Utils.timer(ChatColor.YELLOW + "Healer: Trap 1/3 (Opened Chest)");
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
			Utils.timer(ChatColor.YELLOW + "Healer: Trap 2/3 (Killed Bat)");
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
			Utils.timer(ChatColor.YELLOW + "Healer: Trap 3/3 (Opened Chest)");
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
			Utils.timer(ChatColor.YELLOW + "Healer: Red Blue 1/4 (Opened Chest)");
			Utils.playSecretFoundSound(healer, Utils.SecretType.CHEST);
		}, 145);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, -90f);
			Actions.move(healer, "N", 30);
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
			Utils.timer(ChatColor.YELLOW + "Healer: Prince Killed | Crypt 5/5");
		}, 153); // etherwarp across
		Utils.scheduleTask(() -> Actions.turnHead(healer, 100f, 10f), 154);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 155); // etherwarp to item
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -97f, -4f);
			Utils.timer(ChatColor.YELLOW + "Healer: Red Blue 2/4 (Picked Up Item)");
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
			Utils.timer(ChatColor.YELLOW + "Healer: Red Blue 3/4 (Opened Chest)");
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
			Utils.timer(ChatColor.YELLOW + "Healer: Red Blue 4/4 (Opened Wither Essence)");
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
		Utils.scheduleTask(() -> {
			Utils.timer(ChatColor.YELLOW + "Healer: Yellow Cleared");
			Utils.timer(ChatColor.YELLOW + "Healer: Clear Finished in 174 ticks (8.70d seconds)");
			Utils.broadcastBlessing(healer, Utils.BlessingType.WISDOM, 5);
		}, 174);
		// Yellow: 10 ticks

		Utils.scheduleTask(() -> {
			Actions.swapItems(healer, 1, 28);
			Actions.swapItems(healer, 3, 30);
			Actions.swapItems(healer, 6, 33);
			Actions.swapItems(healer, 7, 34);
			Actions.swapItems(healer, 12, 39);
			Actions.setHotbarSlot(healer, 5);
		}, 175);
		// Boss handoff (teleport to boss spawn + maxor(true)) is now driven by the Watcher's portal entry — see
		// Watcher.enterPortal / the maxorHandoff armed in TAS.runTAS.
	}

	public static void maxor(boolean doContinue) {
		Utils.scheduleTask(() -> Actions.turnHead(healer, -14.5f, 37.5f), 1);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 11), 2);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 12);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 13); // stonk maxor floor
		Utils.scheduleTask(() -> Actions.turnHead(healer, -55f, 0f), 14);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 16); // fall to storm floor
		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 30f), 62);
		Utils.scheduleTask(() -> {
			Actions.move(healer, "W", 11);
			Actions.leftClick(healer);
		}, 63); // walk forward + fall to goldor floor
		Utils.scheduleTask(() -> Actions.leftClick(healer), 64);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 65);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 66);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 67);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 68);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 69);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 70);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 71);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 72); // stonk into goldor
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 15f, 0f); // adjust movement angle
			Actions.setHotbarSlot(healer, 1);
		}, 74);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 75);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 116);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 118);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 128);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -4f, 80f), 137);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 138); // bonzo 1 of 3
		// tick 139: propelled forward for 10 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 80f), 140);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 150); // jump to gain slightly more distance
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 151);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 152); // bonzo 2 of 3
		// tick 154: propelled forward for 11 ticks
		Utils.scheduleTask(() -> Actions.rightClick(healer), 166); // bonzo 3 of 3
		// tick 167: propelled forward for 10 ticks
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 45f);
			Actions.setHotbarSlot(healer, 5);
		}, 168);
		// tick 177: walk forward and fall for 7 ticks
		Utils.scheduleTask(() -> Actions.leftClick(healer), 184);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 185);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 186);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 187);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 188);
		Utils.scheduleTask(() -> {
			Actions.leftClick(healer);
			Actions.move(healer, "W", 0);
		}, 189);
		// tick 191: start falling for 13 ticks
		Utils.scheduleTask(() -> Actions.turnHead(healer, 75f, 90f), 193);
		// tick 204: reach ground
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 205);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, 90f);
			Actions.setHotbarSlot(healer, 7);
		}, 220);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 222);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 49), 224);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 237); // soul sand to lava jump higher
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 16f, 0f);
			Actions.setHotbarSlot(healer, 5);
		}, 238);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 85f, 0f), 271);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -12f, -16f), 273);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 274);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -34f, -63f), 275);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 276);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 34f, -30f), 277);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 278);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 33f, -53f), 279);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 280);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 66f, 3f), 281);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 282);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 66f, -43f), 283);
		Utils.scheduleTask(() -> {
			Actions.rightClick(healer);
			Utils.timer(ChatColor.YELLOW + "Healer: Predev Finished in 284 Ticks (14.20 seconds) | Overall: 1 026 ticks (51.30 seconds)");
		}, 284);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 4), 285);
		Utils.scheduleTask(() -> Actions.leap(healer, Berserk.get()), 286); // leap to bers
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 1);
			Actions.move(healer, "WP", 0);
		}, 403);
		Utils.scheduleTask(() -> {
			Actions.move(healer, "WPJ", 0);
			Actions.turnHead(healer, 34f, 80f);
		}, 408);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 410); // bonzo to yellow pad
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 34f, 0f);
			Actions.move(healer, "WP", 19);
			Actions.setHotbarSlot(healer, 0);
		}, 411);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -155f, 0f);
			Actions.swapItems(healer, 12, 39); // rod swap off of black cat, remove racing helmet -> speed auto-set to 400
			Actions.setHotbarSlot(healer, 3);
		}, 431);
		// storm() is now started by Maxor.chainNext (player handoff armed in TAS.runTAS).
	}

	public static void storm(boolean doContinue) {
		for(int i = 0; i <= 80; i += 5) {
			Utils.scheduleTask(() -> Actions.snapHeadToNearestEnemy(healer), i - 1);
			Utils.scheduleTask(() -> Actions.loopLeftClick(healer), i);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 0), 81);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 82);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 85);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 88);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 91);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 92);
		for(int i = 95; i <= 170; i += 3) {
			Utils.scheduleTask(() -> Actions.snapHeadToNearestEnemy(healer), i - 1);
			Utils.scheduleTask(() -> Actions.loopLeftClick(healer), i);
		} // clear pad, including shadow assassin
		Utils.scheduleTask(() -> Actions.turnHead(healer, -155f, 0f), 171);
		Utils.scheduleTask(() -> {
			Actions.move(healer, "WP", 32);
			Actions.setHotbarSlot(healer, 1);
		}, 172); // move off the pad early enough
		Utils.scheduleTask(() -> Actions.turnHead(healer, -155f, 80f), 179);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 180); // bonzo back to pillar
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -155f, 0f);
			Actions.setHotbarSlot(healer, 3);
		}, 181);
		for(int i = 207; i <= 532; i += 5) {
			Utils.scheduleTask(() -> Actions.snapHeadToNearestEnemy(healer), i - 1);
			Utils.scheduleTask(() -> Actions.loopLeftClick(healer), i);
		} // kill outstanding wither skeletons
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -85f, 0f);
			Actions.swapItems(healer, 5, 32);
			Actions.setHotbarSlot(healer, 6);
		}, 533);
		Utils.scheduleTask(() -> {
			Actions.move(healer, "WP", 0);
			Actions.rightClick(healer); // rag buff
		}, 546);
		Utils.scheduleTask(() -> Actions.move(healer, "WN", 8), 555); // go to checkpoint spot
		Utils.scheduleTask(() -> Actions.turnHead(healer, -94.5f, -18.5f), 565);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 6);
			Actions.swapItems(healer, 5, 32);
		}, 607);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 608);
		Utils.scheduleTask(() -> Actions.stopRightClick(healer), 665);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 0f), 684);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 685);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 36f, 0f), 691);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 36f, 80f);
			Actions.move(healer, "WPJ", 0);
			Actions.setHotbarSlot(healer, 1);
		}, 699);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 702);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 36f, 0f);
			Actions.move(healer, "WP", 20);
			Actions.setHotbarSlot(healer, 3);
		}, 703);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -144f, 0f), 723);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 4), 741);
		Utils.scheduleTask(() -> Actions.snapHeadAtEntity(healer, Storm.INSTANCE.getBoss()), 759);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 760);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 4);
			Actions.swapItems(healer, 12, 39); // put on racing helmet -> speed auto-set to 650
		}, 761);
		Utils.scheduleTask(() -> Actions.leap(healer, Berserk.get()), 782);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 1);
			Actions.move(healer, "WP", 12);
		}, 783);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 784);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -47f, 0f);
			Actions.setHotbarSlot(healer, 5);
		}, 785);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -141f, 1f), 800);
		Utils.scheduleTask(() -> Actions.move(healer, "AN", 3), 812);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -123f, 1f), 814);
		// goldor() is now started by Storm.chainNext (player handoff armed in TAS.runTAS).
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
		Actions.rightClick(healer);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 1);
		// tick 2: body-blocked by tank
		// tick 3: body-blocked by tank
		Utils.scheduleTask(() -> Actions.rightClick(healer), 4);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 5);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 6);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 7); // s1 dev

		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 80f);
			Actions.setHotbarSlot(healer, 1);
		}, 8);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 9);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 10);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 12), 12);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 14);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 0f);
			Actions.setHotbarSlot(healer, 5);
		}, 15);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 18f, 1f), 24);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 25); // s1 left

		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 75f, 90f);
			Actions.setHotbarSlot(healer, 1);
		}, 26);
		Utils.scheduleTask(() -> Actions.move(healer, "J", 0), 27);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 10), 29);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 31);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 83f, 35f);
			Actions.setHotbarSlot(healer, 5);
		}, 40);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 41); //s1 right

		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 4), 42);
		Utils.scheduleTask(() -> Actions.leap(healer, Berserk.get()), 51); // delay for better positioning

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 * ██╔═══╝
		 * ███████╗
		 * ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 90f);
			Actions.setHotbarSlot(healer, 7);
		}, 52);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 44), 53);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 59);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, -10f);
			Actions.setHotbarSlot(healer, 5);
		}, 60);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 99); // s2 dev

		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, 80f);
			Actions.setHotbarSlot(healer, 1);
		}, 100);
		Utils.scheduleTask(() -> Actions.move(healer, "W", 0), 101);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 27), 102);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 108);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, 0f);
			Actions.setHotbarSlot(healer, 4);
		}, 109);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 96f, 0f), 123);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 90f, 6f), 128);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 129); // s2 top

		Utils.scheduleTask(() -> Actions.leap(healer, Archer.get()), 130);

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 *  ╚═══██╗
		 * ██████╔╝
		 * ╚═════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, 5f);
			Actions.setHotbarSlot(healer, 5);
		}, 131);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 132); // s3 4
		// tick 133: terminal completes

		Utils.scheduleTask(() -> Actions.turnHead(healer, -8f, 0f), 134);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 10), 135);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 55f, 0f), 142);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 53f, -8f);
			Actions.setHotbarSlot(healer, 4);
		}, 157);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 158); // s3 2
		// tick 159: terminal completes

		Utils.scheduleTask(() -> Actions.leap(healer, Mage.get()), 160);

		/*
		 * ██╗  ██╗
		 * ██║  ██║
		 * ███████║
		 * ╚════██║
		 *      ██║
		 *      ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.move(healer, "WP", 0);
			Actions.setHotbarSlot(healer, 1);
		}, 161);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 12), 170);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -100f, 80f), 172);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 173);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -100f, 0f);
			Actions.setHotbarSlot(healer, 4);
		}, 174);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -151f, -22f), 180);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 181); // s4 bottom

		Utils.scheduleTask(() -> Actions.leap(healer, Mage.get()), 191);

		/*
		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
		 * █████╗  ██║██║  ███╗███████║   ██║
		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
		 * ██║     ██║╚██████╔╝██║  ██║   ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 180f, 0f);
			Actions.setHotbarSlot(healer, 3);
		}, 200);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 5), 219);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -111f, -2f);
			Actions.swapItems(healer, 12, 39);
		}, 223);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 224);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 4), 225);
		Utils.scheduleTask(() -> Actions.leap(healer, Archer.get()), 245);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 246);
		Utils.scheduleTask(() -> Actions.move(healer, "WN", 0), 287);
		Utils.scheduleTask(() -> Actions.move(healer, "N", 10), 297);
	}

	public static void necron(boolean doContinue) {
		Actions.setHotbarSlot(healer, 6);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 1);
		Utils.scheduleTask(() -> Actions.stopRightClick(healer), 152);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 153);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 160);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 6), 161);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 162);
		Utils.scheduleTask(() -> Actions.stopRightClick(healer), 183);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 184);
		for(int i = 185; i <= 360; i += 5) {
			Utils.scheduleTask(() -> Actions.leftClick(healer), i);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 6), 361);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 362);
		Utils.scheduleTask(() -> Actions.stopRightClick(healer), 383);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 384);
		for(int i = 385; i <= 500; i += 5) {
			Utils.scheduleTask(() -> Actions.leftClick(healer), i);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 4), 501);
		Utils.scheduleTask(() -> Actions.leap(healer, Archer.get()), 502);
		Utils.scheduleTask(() -> Actions.swapItems(healer, 13, 39), 503);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -2f, 0f);
			Actions.setHotbarSlot(healer, 5);
		}, 504);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 505);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 515);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 517);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 527);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 529);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 539);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 10), 541);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 551);
		Utils.scheduleTask(() -> Actions.swapItems(healer, 3, 30), 552);
	}

	public static void witherKing() {
		Actions.rightClick(healer);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 4), 1);
		Utils.scheduleTask(() -> Actions.leap(healer, Berserk.get()), 25);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 90f, 0f);
			Actions.move(healer, "WDP", 0);
			Actions.setHotbarSlot(healer, 8);
		}, 26);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 1), 27);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 28);
		Utils.scheduleTask(() -> {
			Actions.swapItems(healer, 5, 32);
			Actions.swapItems(healer, 13, 39);
		}, 29);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 4), 30);
		Utils.scheduleTask(() -> Actions.leap(healer, Mage.get()), 100);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(healer, 5);
			Actions.turnHead(healer, 180f, -90f);
		}, 101);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 200);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 6), 261);
		int lbFor = 10;
		for(int i = 290; i <= 386 - lbFor - 1; i += lbFor + 1) {
			Utils.scheduleTask(() -> Actions.rightClick(healer), i);
			Utils.scheduleTask(() -> Actions.stopRightClick(healer), i + lbFor);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 387);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 388);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 393);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -177.5f, -90f);
			Actions.setHotbarSlot(healer, 5);
		}, 394);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 395);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 411);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 21), 413);
		Utils.scheduleTask(() -> Actions.rightClick(healer), 529);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 6), 590);
		for(int i = 630; i <= 726 - lbFor - 1; i += lbFor + 1) {
			Utils.scheduleTask(() -> Actions.rightClick(healer), i);
			Utils.scheduleTask(() -> Actions.stopRightClick(healer), i + lbFor);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 727);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 728);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 92.5f, -90f);
			Actions.setHotbarSlot(healer, 5);
		}, 729);
		Utils.scheduleTask(() -> {
			Actions.rightClick(healer);
			Actions.move(healer, "WP", 0);
		}, 730);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 2), 780);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 6), 791);
		for(int i = 792; i <= 826 - lbFor - 1; i += lbFor + 1) {
			Utils.scheduleTask(() -> Actions.rightClick(healer), i);
			Utils.scheduleTask(() -> Actions.stopRightClick(healer), i + lbFor);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 827);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 828);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -3f, -90f);
			Actions.setHotbarSlot(healer, 6);
		}, 829);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 0), 830);
		Utils.scheduleTask(() -> Actions.move(healer, "WPJ", 0), 851);
		Utils.scheduleTask(() -> Actions.move(healer, "WP", 14), 853);
		for(int i = 858; i <= 926 - lbFor - 1; i += lbFor + 1) {
			Utils.scheduleTask(() -> Actions.rightClick(healer), i);
			Utils.scheduleTask(() -> Actions.stopRightClick(healer), i + lbFor);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(healer, 3), 927);
		Utils.scheduleTask(() -> Actions.leftClick(healer), 928);
	}

	@SuppressWarnings("unused")
	public static Player get() {
		return healer;
	}
}
