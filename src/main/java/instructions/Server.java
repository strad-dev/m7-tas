package instructions;

import instructions.bosses.Watcher;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugin.M7tas;
import plugin.Utils;

import java.util.Objects;

public class Server {
	private static final Zombie[] archaeologists = new Zombie[10];
	private static Zombie yellowShadowAssassin = null;

	public static void serverInstructions(World world) {
		// Begin with 3 seconds of delay
		Bukkit.broadcastMessage("TAS starts in 3 seconds.");

		spawnMobs(world);

		// 5-second countdown
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 5 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 60);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 4 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 80);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 3 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 100);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 2 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 120);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 1 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 140);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Run started.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 160);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 1.0F), 160);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Watcher.watcherInstructions(world), 161);

//		Bukkit.broadcastMessage(ChatColor.RED + "The " + ChatColor.BOLD + "BLOOD DOOR" + ChatColor.RESET + ChatColor.RED + " has been opened!");
//		Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "A shiver runs down your spine...");
//		Utils.playGlobalSound(Sound.ENTITY_GHAST_HURT, 2.0F, 0.5F);
	}


	private static void spawnMobs(World world) {
		for(Zombie zombie : archaeologists) {
			if(zombie != null) {
				zombie.remove();
			}
		}

		Location[] locations = {
				new Location(world, -120.5, 69, -184.5, 90, 0), // Red Blue (I)
				new Location(world, -216.5, 69, -184.5, 0, 0), // Spider (II)
				new Location(world, -120.5, 67, -120.5, -90, 0), // Deathmite (II)
				new Location(world, -35.5, 69, -152.5, 90, 0), // Dino Dig Site (III)
				new Location(world, -88.5, 69.0625, -215.5, 0, 0), // Catwalk (IV)
				new Location(world, -156.5, 69, -120.5, 90, 0), // Well (III)
				new Location(world, -177.5, 69, -80.5, -90, 0), // Gravel (V)
				new Location(world, -136.5, 65.0625, -40.5, -180, 0), // Museum (VI)
				new Location(world, -211.5, 69, -56.5, -90, 0), // Market (VII)
				new Location(world, -88.5, 69, -56.5, -90, 0) // Melon (VIII)
		};

		double[] healthValues = {15, 16, 16, 17, 18, 17, 19, 20, 21, 22};

		for(int i = 0; i < locations.length; i++) {
			Zombie zombie = (Zombie) world.spawnEntity(locations[i], EntityType.ZOMBIE);
			zombie.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Angry Archaeologist " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + (int) healthValues[i] + "/" + (int) healthValues[i]);
			zombie.setCustomNameVisible(true);
			zombie.setAI(false);
			zombie.setSilent(true);
			Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-20);
			Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(healthValues[i]);
			zombie.setHealth(healthValues[i]);

			assert zombie.getEquipment() != null;
			zombie.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
			zombie.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
			zombie.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
			zombie.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
			zombie.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));

			archaeologists[i] = zombie;
		}

		if(yellowShadowAssassin != null) {
			yellowShadowAssassin.remove();
		}

		yellowShadowAssassin = (Zombie) world.spawnEntity(new Location(world, -216.5, 69, -24.5, -180, 0), EntityType.ZOMBIE);
		yellowShadowAssassin.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Shadow Assassin " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + 30 + "/" + 30);
		yellowShadowAssassin.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0));
		yellowShadowAssassin.setCustomNameVisible(true);
		yellowShadowAssassin.setAI(false);
		yellowShadowAssassin.setSilent(true);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.ARMOR)).setBaseValue(-3);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(30);
		yellowShadowAssassin.setHealth(30);

		ItemStack boots = Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.PURPLE, ChatColor.LIGHT_PURPLE + "Shadow Assassin Boots");
		assert yellowShadowAssassin.getEquipment() != null;
		yellowShadowAssassin.getEquipment().setBoots(boots);
		yellowShadowAssassin.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
	}
}