package instructions;

import instructions.bosses.Goldor;
import instructions.bosses.Maxor;
import instructions.bosses.WitherKing;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Mage {
	private static Player mage;
	private static World world;

	public static void mageInstructions(Player p, String section) {
		mage = p;
		world = Mage.mage.getWorld();

		switch(section) {
			case "all", "clear" -> {
				Actions.teleport(Mage.mage, new Location(world, -132.5, 69, -76.5, -180f, 0f));
				Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 2, 29), 60);
				Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 2), 61);
				Utils.scheduleTask(() -> Actions.rightClickWithSpectators(mage), 101);
				Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 0.8634), 5), 102);
				Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 121);
				Utils.scheduleTask(() -> {
					Actions.teleport(mage, new Location(mage.getWorld(), -120.5, 75, -220.5));
					Actions.swapFakePlayerInventorySlots(mage, 2, 29);
				}, 141);
				// Tick 160 (clear tick 0: run begins)
				// Tick 161 (clear tick 1: teleport back) - watcher sequence begins
				Utils.scheduleTask(() -> clear(section.equals("all")), 162);
			}
			case "maxor", "boss" -> {
				Actions.teleport(mage, new Location(world, 73.5, 221, 13.5, 0f, 0f));
				Actions.swapFakePlayerInventorySlots(mage, 9, 36);
				Actions.swapFakePlayerInventorySlots(mage, 1, 28);
				Actions.swapFakePlayerInventorySlots(mage, 3, 30);
				Actions.swapFakePlayerInventorySlots(mage, 5, 32);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
			case "storm" -> {
				Actions.teleport(mage, new Location(world, 46.576, 169, 49.503, 1.4f, 22.4f));
				Actions.swapFakePlayerInventorySlots(mage, 1, 28);
				Actions.swapFakePlayerInventorySlots(mage, 3, 30);
				Actions.swapFakePlayerInventorySlots(mage, 5, 32);
				Utils.scheduleTask(() -> storm(false), 60);
			}
			case "goldor" -> {
				Actions.teleport(mage, new Location(world, 108.308, 120, 94.675, -139.3f, 1.6f));
				Actions.swapFakePlayerInventorySlots(mage, 1, 28);
				Actions.swapFakePlayerInventorySlots(mage, 3, 30);
				Actions.swapFakePlayerInventorySlots(mage, 6, 33);
				Utils.scheduleTask(() -> goldor(false), 60);
			}
			case "necron" -> {
				Actions.teleport(mage, new Location(world, 56.488, 64, 111.700, -180f, 0f));
				Actions.swapFakePlayerInventorySlots(mage, 1, 28);
				Actions.swapFakePlayerInventorySlots(mage, 3, 30);
				Actions.swapFakePlayerInventorySlots(mage, 5, 32);
				Actions.swapFakePlayerInventorySlots(mage, 6, 33);
				Utils.scheduleTask(() -> necron(false), 60);
			}
			case "witherking" -> {
				Actions.teleport(mage, new Location(world, 89.7, 6, 94.406, -75.6f, 18.8f));
				Actions.swapFakePlayerInventorySlots(mage, 1, 28);
				Actions.swapFakePlayerInventorySlots(mage, 5, 32);
				Actions.swapFakePlayerInventorySlots(mage, 6, 33);
				Utils.scheduleTask(() -> necron(false), 60);
			}
		}
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
		// Tick 162 (clear tick 5, delay = 3)
		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 3);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 3.4f, -3.4f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 4);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -120.5, 74, -154.5)), 5);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 13.4f), 6);
		Utils.scheduleTask(() -> Actions.AOTV(mage, new Location(world, -120.5, 71.21667, -142.83)), 7);
		Utils.scheduleTask(() -> Actions.AOTV(mage, new Location(world, -120.5, 71, -138.5)), 8);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -6.9f, 8.6f);
			Actions.swingHand(mage);
			Server.openWitherDoor();
		}, 9);
		// tick 10: open inventory
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 4, 31), 11);
		// tick 12: close inventory
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -119.5, 69, -127.5)), 29);

		/*
		 * ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗███╗   ███╗██╗████████╗███████╗
		 * ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║████╗ ████║██║╚══██╔══╝██╔════╝
		 * ██║  ██║█████╗  ███████║   ██║   ███████║██╔████╔██║██║   ██║   █████╗
		 * ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║██║╚██╔╝██║██║   ██║   ██╔══╝
		 * ██████╔╝███████╗██║  ██║   ██║   ██║  ██║██║ ╚═╝ ██║██║   ██║   ███████╗
		 * ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -31, 15);
			Actions.setFakePlayerHotbarSlot(mage, 7);
		}, 30);
		Utils.scheduleTask(() -> Actions.throwPearl(mage), 31);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 45.1f, 6f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 32);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -130.5, 69, -116.5)), 33);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 0f);
			Actions.setFakePlayerHotbarSlot(mage, 3);
		}, 34);
		Utils.scheduleTask(() -> Actions.superboom(mage, -131, 69, -116, -130, 72, -115), 35);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 1), 36);
		Utils.scheduleTask(() -> Actions.AOTV(mage, new Location(world, -130.5, 69, -111.5)), 37);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 1/6 (Obtained Item)");
			world.playSound(mage.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 38);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -124f, -78f);
			Actions.setFakePlayerHotbarSlot(mage, 7);
			Actions.AOTV(mage, new Location(world, -114.535, 67, -119.218));
		}, 39);
		Utils.scheduleTask(() -> Actions.throwPearl(mage), 40);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -15.3f, -5.2f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 41);
		Utils.scheduleTask(() -> Actions.AOTV(mage, new Location(world, -111.387, 69, -107.693)), 42);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 76.4f), 43);
		Utils.scheduleTask(() -> Actions.AOTV(mage, new Location(world, -109.5, 60, -107.5)), 44);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 171f, 18f), 45);
		Utils.scheduleTask(() -> Actions.AOTV(mage, new Location(world, -110.5, 60, -112.5)), 46);
		Utils.scheduleTask(() -> {
			Actions.swingHand(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 2/6 (Opened Chest)");
			world.playSound(mage.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 47);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, -90f), 48);
		Utils.scheduleTask(() -> Actions.AOTV(mage, new Location(world, -112.3, 78.2, -120.7)), 50);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -112.5, 81, -120.5)), 51);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 52);
		Utils.scheduleTask(() -> {
			Actions.swingHand(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 3/6 (Opened Chest)");
			world.playSound(mage.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 53);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 100.8f, 3f), 54);
		Utils.scheduleTask(() -> Actions.AOTV(mage, new Location(world, -124.5, 82, -122.5)), 55);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -164.2f, 12.9f), 56);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -122.5, 82, -129.5)), 57);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -90f, 4f);
			Actions.setFakePlayerHotbarSlot(mage, 7);
		}, 58);
		Utils.scheduleTask(() -> Actions.throwPearl(mage), 59);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 124.1f, -63.5f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 60);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -125.5, 92, -131.5)), 61);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -133.7f, 39.3f), 62);
		Utils.scheduleTask(() -> {
			Actions.swingHand(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 4/6 (Opened Chest)");
			world.playSound(mage.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 63);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -90f, 60f);
			Actions.setFakePlayerHotbarSlot(mage, 0);
		}, 64);
		Utils.scheduleTask(() -> {
			Actions.witherImpact(mage, new Location(world, -124.5, 92, -131.5));
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmte 5/6 (Killed Bat)");
			world.playSound(mage.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 65);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -84.6f, -4.9f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
			Actions.AOTV(mage, new Location(world, -113.923, 82, -129.565));
		}, 66);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -92.5, 86, -127.5)), 67);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -96.4f, 6.7f), 68);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -78.5, 86, -129.5)), 69);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 171f, 18f);
			Actions.setFakePlayerHotbarSlot(mage, 4);
		}, 70);
		Utils.scheduleTask(() -> {
			Actions.crypt(mage, -81, 87, -133, -83, 86, -130);
			Actions.crypt(mage, -76, 87, -133, -78, 86, -130);
		}, 71);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 124f, 0f);
			Actions.setFakePlayerHotbarSlot(mage, 3);
		}, 72);
		Utils.scheduleTask(() -> {
			mageBeam();
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Crypt 1/5");
		}, 73);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -135f, 0f), 74);
		Utils.scheduleTask(() -> {
			mageBeam();
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Crypt 2/5");
		}, 78);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 17.8f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 79);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -78.5, 86, -124.5)), 80);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 45f, 39.4f), 81);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -94.5, 69, -108.5)), 82);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 126.5f, 3.7f), 83);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -119.5, 69, -127.5)), 84);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 2.3f, 4.4f), 85);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -120.5, 69, -106.5)), 86);
		// tick 87: open inventory
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 5, 32), 88);
		// tick 89: close inventory
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 5), 90);

		/*
		 * ██████╗ ██╗      ██████╗  ██████╗ ██████╗      ██████╗ █████╗ ███╗   ███╗██████╗
		 * ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔════╝██╔══██╗████╗ ████║██╔══██╗
		 * ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██║     ███████║██╔████╔██║██████╔╝
		 * ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██║     ██╔══██║██║╚██╔╝██║██╔═══╝
		 * ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ╚██████╗██║  ██║██║ ╚═╝ ██║██║
		 * ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝      ╚═════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝
		 */
		Utils.scheduleTask(() -> Actions.rag(mage), 374);
		Utils.scheduleTask(Server::openBloodDoor, 415);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, -12f), 416);
		// rag axe activates on tick 434
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 3), 435);
		Utils.scheduleTask(() -> {
			mageBeam();
			Actions.move(mage, new Vector(0, 0, 1.12242), 10);
		}, 436);
		Utils.scheduleTask(() -> snapHead("Bonzo"), 437);
		Utils.scheduleTask(Mage::mageBeam, 441);
		Utils.scheduleTask(() -> snapHead("Meepy_"), 442);
		Utils.scheduleTask(Mage::mageBeam, 446);
		Utils.scheduleTask(() -> snapHead("Mallyanke"), 447);
		Utils.scheduleTask(Mage::mageBeam, 451);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, -35f), 452);
		Utils.scheduleTask(Mage::mageBeam, 493);
		Utils.scheduleTask(Mage::mageBeam, 526);
		Utils.scheduleTask(Mage::mageBeam, 567);
		Utils.scheduleTask(Mage::mageBeam, 597);
		Utils.scheduleTask(Mage::mageBeam, 630);
		Utils.scheduleTask(Mage::mageBeam, 659);
		Utils.scheduleTask(Mage::mageBeam, 688);
		Utils.scheduleTask(Mage::mageBeam, 717);
		Utils.scheduleTask(Mage::mageBeam, 750);
		Utils.scheduleTask(Mage::mageBeam, 782);
		Utils.scheduleTask(Mage::mageBeam, 823);
		Utils.scheduleTask(Mage::mageBeam, 853);
		Utils.scheduleTask(Mage::mageBeam, 886);
		Utils.scheduleTask(Mage::mageBeam, 915);
		Utils.scheduleTask(() -> {
			mageBeam();
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Blood Camp Finished in 947 Ticks (47.35 seconds)");
		}, 945);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 4.5f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 946);
		Utils.scheduleTask(() -> Actions.etherwarp(mage, new Location(world, -120.5, 69, -74.5)), 947);
		Utils.scheduleTask(() -> {
			Actions.swapFakePlayerInventorySlots(mage, 9, 36);
			Actions.swapFakePlayerInventorySlots(mage, 1, 28);
			Actions.swapFakePlayerInventorySlots(mage, 3, 30);
			Actions.swapFakePlayerInventorySlots(mage, 4, 31);
		}, 948);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Entered Boss in 1027 Ticks (51.35 seconds)");
			if(doContinue) {
				Actions.teleport(mage, new Location(world, 73.5, 221, 13.5));
				maxor(true);
			}
		}, 1025);
	}

	public static void maxor(boolean doContinue) {
		Actions.setFakePlayerHotbarSlot(mage, 5);
		Actions.move(mage, new Vector(0.22, 0, 1.1), 28);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -11.31f, 0f), 1);
		Utils.scheduleTask(() -> {
			Actions.move(mage, new Vector(0.051, 0, 0.255), 16);
			Actions.springBoots(mage);
		}, 28);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -33f, 0f), 45);
		Utils.scheduleTask(() -> {
			Maxor.pickUpCrystal(mage);
			Bukkit.broadcastMessage(ChatColor.GOLD + "Beethoven_" + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!");
		}, 56);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1403, 0, 0.243), 2), 57);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 58);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2806, 0, 0), 16), 59);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -131.2925f, 0f), 75);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 79);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2108, 0, -0.1852), 19), 81);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 100);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1954, 0, -0.1716), 1), 101);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 48.7075f, 0f), 102);
		Utils.scheduleTask(() -> {
			Maxor.placeCrystal(mage);
			Actions.move(mage, new Vector(-0.1954, 0, 0.1716), 16);
			Actions.springBoots(mage);
		}, 160);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.2108, 0, 0.1852), 27), 177);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.0488, 0, 0.0429), 1), 206);
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 9, 36), 207);
		Utils.scheduleTask(() -> {
			Maxor.pickUpCrystal(mage);
			Bukkit.broadcastMessage(ChatColor.GOLD + "Beethoven_" + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!");
		}, 239);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -131.2925f, 0f), 240);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 241);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2108, 0, -0.1852), 19), 242);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 261);
		Utils.scheduleTask(() -> Maxor.placeCrystal(mage), 262);
		Utils.scheduleTask(() -> Actions.rag(mage), 263);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 324);
		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 325);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 3), 326);
		Utils.scheduleTask(Mage::mageBeam, 399);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 400);
		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 401);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, 0f), 402);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-1.12242, 0, 0), 11), 403);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 1.4f, 22.4f);
			Actions.setFakePlayerHotbarSlot(mage, 6);
		}, 414);
		if(doContinue) {
			Utils.scheduleTask(() -> storm(true), 499);
		}
	}

	public static void storm(boolean doContinue) {
		Actions.setFakePlayerHotbarSlot(mage, 6);
		Utils.scheduleTask(() -> Actions.gyro(mage, new Location(world, 46.5, 169, 53.5)), 1); // gyro will be up in 7.5 seconds (150 ticks)
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -105.7f, -19.8f);
			Actions.setFakePlayerHotbarSlot(mage, 3);
		}, 2);
		Utils.scheduleTask(Mage::mageBeam, 8);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -135.4f, -18.2f), 9);
		Utils.scheduleTask(Mage::mageBeam, 13);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 135f, -18.5f), 14);
		Utils.scheduleTask(Mage::mageBeam, 18);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 106.3f, -20.8f), 19);
		Utils.scheduleTask(Mage::mageBeam, 23);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 50f, -17.2f), 24);
		Utils.scheduleTask(Mage::mageBeam, 28);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 32.5f, -14.2f), 29);
		Utils.scheduleTask(Mage::mageBeam, 33);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -32.1f, -14.2f), 34);
		Utils.scheduleTask(Mage::mageBeam, 38);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -49.2f, -17.8f), 39);
		Utils.scheduleTask(Mage::mageBeam, 43);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 44);
		Utils.scheduleTask(() -> Actions.leap(mage, Berserk.get()), 45);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -106.4f, -21.8f);
			Actions.setFakePlayerHotbarSlot(mage, 3);
		}, 46);
		Utils.scheduleTask(Mage::mageBeam, 48);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -135.1f, -18.2f), 49);
		Utils.scheduleTask(Mage::mageBeam, 53);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 135.6f, -18.2f), 54);
		Utils.scheduleTask(Mage::mageBeam, 58);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 106f, -22f), 59);
		Utils.scheduleTask(Mage::mageBeam, 63);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 50f, -17.8f), 64);
		Utils.scheduleTask(Mage::mageBeam, 68);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 32.4f, -14.2f), 69);
		Utils.scheduleTask(Mage::mageBeam, 73);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -32.5f, -13.9f), 74);
		Utils.scheduleTask(Mage::mageBeam, 78);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -49.9f, -17.5f), 79);
		Utils.scheduleTask(Mage::mageBeam, 83);
		for(int tick = 85; tick <= 200; tick += 5) {
			Utils.scheduleTask(() -> {
				List<Entity> nearbyEntities = mage.getNearbyEntities(6, 6, 6);

				for(Entity entity : nearbyEntities) {
					if(entity instanceof WitherSkeleton) {
						Location healerLoc = mage.getLocation();
						Location witherLoc = entity.getLocation();

						double deltaX = witherLoc.getX() - healerLoc.getX();
						double deltaY = witherLoc.getY() - healerLoc.getY();
						double deltaZ = witherLoc.getZ() - healerLoc.getZ();

						float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
						float pitch = (float) -(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * 180.0 / Math.PI);

						Actions.turnHead(mage, yaw, pitch);

						Utils.scheduleTask(Mage::mageBeam, 1);

						break;
					}
				}
			}, tick);
		}
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 202);
		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 203);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 5), 204);
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 6, 33), 205);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -172.9f, -42.5f), 544);
		Utils.scheduleTask(() -> {
			Mage.mageBeam();
			Actions.move(mage, new Vector(-0.1067, 0, 0.8568), 7);
		}, 545);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -172.9f, -35.6f), 549);
		Utils.scheduleTask(Mage::mageBeam, 550);
		Utils.scheduleTask(Mage::mageBeam, 555);
		Utils.scheduleTask(Mage::mageBeam, 560);
		Utils.scheduleTask(Mage::mageBeam, 565);
		Utils.scheduleTask(Mage::mageBeam, 570);
		Utils.scheduleTask(Mage::mageBeam, 575);
		Utils.scheduleTask(Mage::mageBeam, 580);
		Utils.scheduleTask(() -> {
			Mage.mageBeam();
			Actions.rag(mage);
		}, 585);
		Utils.scheduleTask(Mage::mageBeam, 590);
		Utils.scheduleTask(Mage::mageBeam, 595);
		Utils.scheduleTask(Mage::mageBeam, 600);
		Utils.scheduleTask(Mage::mageBeam, 605);
		Utils.scheduleTask(Mage::mageBeam, 610);
		Utils.scheduleTask(Mage::mageBeam, 615);
		Utils.scheduleTask(Mage::mageBeam, 620);
		Utils.scheduleTask(Mage::mageBeam, 625);
		Utils.scheduleTask(Mage::mageBeam, 630);
		Utils.scheduleTask(Mage::mageBeam, 635);
		Utils.scheduleTask(Mage::mageBeam, 640);
		Utils.scheduleTask(Mage::mageBeam, 645);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 6), 650);
		Utils.scheduleTask(() -> Actions.lastBreath(mage, 33), 651);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 2), 685);
		Utils.scheduleTask(() -> Actions.iceSpray(mage), 686);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 3), 687);
		Utils.scheduleTask(Mage::mageBeam, 688);
		Utils.scheduleTask(Mage::mageBeam, 693);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 694);
		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 695);
		Utils.scheduleTask(() -> {
			Actions.move(mage, new Vector(-0.8634, 0, 0), 6);
			Actions.setFakePlayerHotbarSlot(mage, 3);
			mageBeam();
		}, 696);
		Utils.scheduleTask(Mage::mageBeam, 701);
		Utils.scheduleTask(() -> Actions.jump(mage), 702);
		Utils.scheduleTask(Mage::mageBeam, 706);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -7f), 707);
		Utils.scheduleTask(Mage::mageBeam, 711);
		Utils.scheduleTask(() -> Actions.jump(mage), 714);
		Utils.scheduleTask(Mage::mageBeam, 716);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -8f), 717);
		Utils.scheduleTask(Mage::mageBeam, 721);
		Utils.scheduleTask(() -> {
			mageBeam();
			Actions.jump(mage);
		}, 726);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -9f), 727);
		Utils.scheduleTask(Mage::mageBeam, 731);
		Utils.scheduleTask(Mage::mageBeam, 736);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -11f), 737);
		Utils.scheduleTask(() -> Actions.jump(mage), 738);
		Utils.scheduleTask(Mage::mageBeam, 741);
		Utils.scheduleTask(Mage::mageBeam, 746);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -13f), 747);
		Utils.scheduleTask(() -> Actions.jump(mage), 750);
		Utils.scheduleTask(Mage::mageBeam, 751);
		Utils.scheduleTask(Mage::mageBeam, 756);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -15f), 757);
		Utils.scheduleTask(Mage::mageBeam, 761);
		Utils.scheduleTask(() -> Actions.jump(mage), 762);
		Utils.scheduleTask(Mage::mageBeam, 766);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -18f), 767);
		Utils.scheduleTask(Mage::mageBeam, 771);
		Utils.scheduleTask(() -> Actions.jump(mage), 774);
		Utils.scheduleTask(Mage::mageBeam, 776);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -21f), 777);
		Utils.scheduleTask(Mage::mageBeam, 781);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, -24f), 783);
		Utils.scheduleTask(() -> Actions.jump(mage), 784);
		Utils.scheduleTask(Mage::mageBeam, 785);
		Utils.scheduleTask(Mage::mageBeam, 790);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 791);
		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 792);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 0f), 793);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 0.26), 3), 794);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -139.3f, 1.6f), 797);
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 5, 32), 798);
		if(doContinue) {
			Utils.scheduleTask(() -> goldor(true), 890);
		}
	}

	private static void goldor(boolean doContinue) {
		/*
		 *  ██╗
		 * ███║
		 * ╚██║
		 *  ██║
		 *  ██║
		 *  ╚═╝
		 */
		Actions.setFakePlayerHotbarSlot(mage, 5);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 1);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 2);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 3);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 4);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 5);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 6);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 7);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 8);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 9);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 10);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 11);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 12);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 13);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 14);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 15);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 67.5f, 0f), 16);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-1.282, 0, 0.5707), 2), 17);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 67.5f, 82f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 18);
		Utils.scheduleTask(() -> Actions.bonzo(mage, new Vector(-1.3936, 0.5, 0.6205)), 19);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 67.5f, 0f), 20);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, 0f), 29);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-1.08, 0, 0), 3), 30);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 65.8f, 15.8f), 33);
		Utils.scheduleTask(() -> {
			Actions.swingHand(mage);
			Actions.move(mage, new Vector(0, 0, -1.08), 1);
		}, 34);
		Utils.scheduleTask(() -> {
			Goldor.broadcastTerminalComplete(mage, "terminal", 3, 7);
			// note: in real hypixel, momentum carries over even after a terminal is opened; as such this is a valid move sequence
			Actions.move(mage, new Vector(0, 0, -0.2806), 9);
		}, 35);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 180f, 0f);
		}, 36);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, 17.1f), 45);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 50);
		Utils.scheduleTask(() -> {
			Goldor.broadcastTerminalComplete(mage, "terminal", 7, 7);
			Bukkit.broadcastMessage(ChatColor.GREEN + "S1 finished in 51 ticks (2.55 seconds) | Overall: 2 367 ticks (118.35 seconds)");
		}, 51);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 52);
		Utils.scheduleTask(() -> Actions.leap(mage, Archer.get()), 53);

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 * ██╔═══╝
		 * ███████╗
		 * ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -174f, 0f);
			Actions.setFakePlayerHotbarSlot(mage, 5);
		}, 58);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1467, 0, -1.395), 1), 59);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.022, 0, -0.2797), 4), 60);
		Utils.scheduleTask(() -> Actions.lavaJump(mage, false), 67);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.022, 0, -0.2797), 6), 68);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -160f, -10.5f), 73);
		Utils.scheduleTask(() -> Actions.swingHand(mage), 74);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(mage, "terminal", 2, 8), 75);

		/*
		 *  ██████╗ ██████╗ ██████╗ ███████╗
		 * ██╔════╝██╔═══██╗██╔══██╗██╔════╝
		 * ██║     ██║   ██║██████╔╝█████╗
		 * ██║     ██║   ██║██╔══██╗██╔══╝
		 * ╚██████╗╚██████╔╝██║  ██║███████╗
		 *  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(mage, -174f, 0f), 76);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.022, 0, -0.2797), 9), 77);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -180f, 90f), 93);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 123, 122)), 94);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -180f, 35f);
			Actions.move(mage, new Vector(0, 0, -1.12242), 5);
		}, 95);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 124, 121)), 96);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 123, 121)), 97);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 124, 120)), 98);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(58, 123, 120)), 99);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 134.99f, 55.7f); // 135f gets the wrong blockface
			Actions.stonk(mage, world.getBlockAt(58, 124, 119));
		}, 100);
		Utils.scheduleTask(() -> {
			Actions.stonk(mage, world.getBlockAt(58, 123, 119));
			Actions.move(mage, new Vector(-0.9921, 0, -0.9921), 1);
		}, 101);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(57, 124, 119)), 102);
		Utils.scheduleTask(() -> {
			Actions.stonk(mage, world.getBlockAt(57, 123, 119));
			Actions.move(mage, new Vector(-0.9921, 0, -0.9921), 1);
		}, 103);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 177.4f, 35f);
			Actions.stonk(mage, world.getBlockAt(57, 124, 118));
		}, 104);
		Utils.scheduleTask(() -> {
			Actions.stonk(mage, world.getBlockAt(57, 123, 118));
			Actions.move(mage, new Vector(-0.0636, 0, -1.4016), 2);
		}, 105);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 177.4f, 0f), 106);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.01273, 0, -0.2803), 16), 107);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.0636, 0, -1.4016), 33), 123);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 180f, 58f), 156);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 58)), 157);
		Utils.scheduleTask(() -> {
			Actions.move(mage, new Vector(0, 0, -1.08), 8);
			Actions.stonk(mage, world.getBlockAt(54, 114, 57));
		}, 158);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 56)), 159);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 55)), 160);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 115, 54)), 161);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 54)), 162);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 53)), 163);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 52)), 164);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(54, 114, 51)), 165);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 180f, 0f);
			Actions.jump(mage);
		}, 166);

		/*
		 * ██╗  ██╗
		 * ██║  ██║
		 * ███████║
		 * ╚════██║
		 *      ██║
		 *      ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(mage, 66.5f, 0f), 198);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.99, 0, 0.4307), 2), 199);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 60f), 200);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(52, 114, 52)), 201);
		Utils.scheduleTask(() -> {
			Actions.move(mage, new Vector(0, 0, 1.08), 5);
			Actions.stonk(mage, world.getBlockAt(52, 114, 53));
		}, 202);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(52, 115, 54)), 203);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(52, 114, 54)), 204);
		Utils.scheduleTask(() -> Actions.stonk(mage, world.getBlockAt(52, 114, 55)), 205);
		Utils.scheduleTask(() -> {
			Actions.jump(mage);
			Actions.turnHead(mage, -90f, 0f);
		}, 207);
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 5, 32), 208);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 5), 209);
		Utils.scheduleTask(() -> Actions.rag(mage), 210);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(1.08, 0, 0), 2), 213);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 180f, 0f), 215);
		// swap to gdrag

		/*
		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
		 * █████╗  ██║██║  ███╗███████║   ██║
		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
		 * ██║     ██║╚██████╔╝██║  ██║   ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, -1.403), 11), 256);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 3), 261);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -82.5f, -5f), 262);
		Utils.scheduleTask(Mage::mageBeam, 280); // wait for debuff
		Utils.scheduleTask(Mage::mageBeam, 285);
		Utils.scheduleTask(Mage::mageBeam, 290);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 291);
		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 292);
		if(doContinue) {
			Utils.scheduleTask(() -> necron(true), 350);
		}
	}

	private static void necron(boolean doContinue) {
		Actions.setFakePlayerHotbarSlot(mage, 5);
		Utils.scheduleTask(() -> Actions.rag(mage), 59);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 120);
		Utils.scheduleTask(() -> Actions.leap(mage, Tank.get()), 121);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 6), 122);
		Utils.scheduleTask(() -> Actions.lastBreath(mage, 36), 123);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 3), 160);
		for(int i = 161; i < 295; i += 5) {
			Utils.scheduleTask(Mage::mageBeam, i);
		}
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 5), 239);
		Utils.scheduleTask(() -> Actions.rag(mage), 240);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 3), 301);
		for(int i = 302; i < 360; i += 5) {
			Utils.scheduleTask(Mage::mageBeam, i);
		}
		for(int i = 368; i < 500; i += 5) {
			Utils.scheduleTask(Mage::mageBeam, i);
		}
		Utils.scheduleTask(Mage::mageBeam, 509);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 510);
		Utils.scheduleTask(() -> Actions.leap(mage, Healer.get()), 511);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -67f, 0f), 512);
		// tick 513: equip black cat
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(1.2915, 0, 0.5482), 19), 514);
		Utils.scheduleTask(() -> Actions.jump(mage), 532);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2583, 0, 0.1096), 9), 533);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(1.2915, 0, 0.5482), 3), 542);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -75.6f, 18.8f), 544);
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 3, 30), 545);
		if(doContinue) {
			Utils.scheduleTask(Mage::witherKing, 609);
		}
	}

	private static void witherKing() {
		Utils.scheduleTask(() -> WitherKing.pickUpRelic(mage), 1);
	}

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

	private static void mageBeam() {
		Actions.swingHand(mage);

		Location l = mage.getLocation();

		// Get player's yaw in radians
		double yaw = Math.toRadians(l.getYaw());

		// Calculate perpendicular vector (90 degrees to the right)
		// Adding 90 degrees to get the right-hand direction
		double rightYaw = yaw + Math.toRadians(90);

		// Calculate offsets (16 pixels = 1 block)
		double offsetX = -Math.sin(rightYaw) * (5.0 / 16.0);   // 5 pixels = 0.3125 blocks to the right
		double offsetZ = Math.cos(rightYaw) * (5.0 / 16.0);    // 5 pixels = 0.3125 blocks to the right
		double offsetY = 1.62 - (13.0 / 16.0);                 // 13 pixels down from eye level = 0.8125 blocks down

		// Apply offsets
		l.add(offsetX, offsetY, offsetZ);

		// Get the eye location and direction
		Location eyeLocation = mage.getEyeLocation();
		Vector eyeDirection = eyeLocation.getDirection();

		// Calculate where the eye is looking at 35 blocks away
		Vector targetPoint = eyeLocation.toVector().add(eyeDirection.multiply(35));

		// Calculate the direction from hand to the target point
		Vector handToTarget = targetPoint.subtract(l.toVector());
		handToTarget.normalize();

		// Scale down the vector for per-iteration movement
		Vector v = handToTarget.multiply(0.2); // Equivalent to dividing by 5

		for(int i = 0; i < 175; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			boolean shouldBreak = false;
			ArrayList<Entity> entities = (ArrayList<Entity>) world.getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				//noinspection DataFlowIssue
				if(entity instanceof LivingEntity temp && !temp.equals(mage) && !(temp instanceof Player) && !entity.isDead() && !entity.isInvulnerable() && !(temp.hasPotionEffect(PotionEffectType.RESISTANCE) && temp.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255)) {
					double damage = mage.getScoreboardTags().contains("RagBuff") ? (temp instanceof Wither ? 145 : 85) : (temp instanceof Wither ? 120 : 70);
					Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(mage, temp, EntityDamageByEntityEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), damage));
//					if(temp.getHurtSound() != null) {
//						world.playSound(l, temp.getHurtSound(), 1.0F, 1.0F);
//					}
					shouldBreak = true;
					break;
				}
			}
			spawnParticle(l);
			l.add(v);
			if(shouldBreak) {
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				break;
			}
		}
	}

	private static void spawnParticle(Location l) {
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.FIREWORK,  // ParticleParam
				false,                   // overrideLimiter
				false,                   // longDistance
				l.getX(),        // x position
				l.getY(),        // y position
				l.getZ(),        // z position
				0.0f,                   // xDist (no spread)
				0.0f,                   // yDist (no spread)
				0.0f,                   // zDist (no spread)
				0.0f,                   // speed (no velocity)
				1                       // count (single particle)
		);

		for(Player player : Objects.requireNonNull(l.getWorld()).getPlayers()) {
			if(player instanceof CraftPlayer craftPlayer) {
				ServerPlayer nmsPlayer = craftPlayer.getHandle();
				nmsPlayer.connection.send(packet);
			}
		}
	}

	@SuppressWarnings("unused")
	public static Player get() {
		return mage;
	}
}
