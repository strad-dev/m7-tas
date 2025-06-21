package instructions.bosses;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R3.profile.CraftPlayerProfile;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.*;

public class Watcher {
	@SuppressWarnings("FieldCanBeLocal")
	private static Zombie watcher;
	private static World world;
	private static final Location ORIGINAL_POSITION = new Location(null, -120.5, 72.0, -88.5, -180, 0);
	private static final List<Location> MOB_SPAWN_LOCATIONS = new ArrayList<>();
	private static final List<String> MOB_NAMES = List.of("Diamante Giant", "Bonzo", "Meepy_", "Mallyanke", "valej", "Sillygreenj", "a6j3", "Nucleararmadillo", "ItzZeeka", "akc0303", "nil4k", "editqble", "uncheck", "EvilMerlyn", "JennAiel", "Stenoe", "aalatif_", "Deanvm", "Beethoven_");
	private static final List<String> SPAWN_LINES = List.of("This guy looks like a fighter.", "Hmmm... this one!", "You'll do.", "Go, fight!", "Go and live again!");
	private static final List<String> KILLED_LINES = List.of("Not bad.", "That one was weak anyway.", "I'm impressed.", "Very nice.", "Aw, I liked that one.");
	private static int mobCount = 0;
	private static final Random random = new Random();

	public static void watcherInstructions(World temp) {
		mobCount = 0;
		world = temp;
		ORIGINAL_POSITION.setWorld(world);
		if(watcher != null) {
			watcher.remove();
		}
		watcher = (Zombie) world.spawnEntity(ORIGINAL_POSITION, EntityType.ZOMBIE);
		watcher.setAI(false);
		watcher.setSilent(true);
		watcher.setPersistent(true);
		watcher.setRemoveWhenFarAway(false);

		GameProfile gp = new GameProfile(UUID.randomUUID(), "watcher");
		gp.getProperties().put("textures", new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTc0Njg0OTQ1NjQxOSwKICAicHJvZmlsZUlkIiA6ICIxZjk0OTQzN2RlYmQ0ODgyYTlhYzZhZmZmN2RhNDcxMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaWlra2FLYSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83YjBkNTI3OGNkZWUwNGM1MjBhOWY1ZDE1M2E1MmI0ZWZjNzBmMzAzMjM5MjY2OGQyMTExNjJkNWFkYzAxYjExIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "awvIfqy7f12hqzBY/BZrhqCpC3xl0zeb0xTOVERZlzXsmk+ZivSyC8ZAlsR1Kmam0aLNDlvO3Nrl8ZGg5n77H+aUkZsoGz4DsuV2GoFv71UxXpPgAVkiCw0kPmNr9O17JChNU2HrO2hd1X3kqPX9gbA/JZ4+kCpcmbEtr7+VAl7xScOEWvKZPimdijG6hkNrBnkcttk+TYdIenrKNrZf346l2nD9nRif+1istHv9ouxZ7GguZPFFTTqtuljhdjsDQ5lQnFN/Q0b4cENMErlAkzam4n2jwTBJPWz9BeIUdgpOr4qyp4bTOLrD3mVfdSEJ+Q4hMjQLZZeYLxMZLSCqm56ns+rzm7O0aj7/+sjxngWZuT8z4U+g2J5QOOA3n8R3Z+QvEHitb1RZdM8DccYb9VwSbGG2jZ8acInxSoIT5bFWWfp0Bh+rwfuNe+v2hFReyUz35BwKrYUOxqL4+A7/McSpik/C+9BVMYL5n78FMD+1+SlJniMwAoPlRpz87yGYivEH9aAlEnTLE+7Tpp6wsiFCaQp5WJ8vfJnV9HVxDYjFs7xB29Cw+FIQnYSsT5U7Uv6znjBMWRmHI9zeU7GzQ0eNQkThSbzX+dE/c1WyPXVuL/wTfefbgh6jm1i6rNGz/a3RdnWk8ItXu/pYQjSmKnc2FJH+x28VXkYl3qQr0gw="));

		CraftPlayerProfile profile = new CraftPlayerProfile(gp);

		ItemStack helmet = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) helmet.getItemMeta();
		assert meta != null;
		meta.setOwnerProfile(profile);
		helmet.setItemMeta(meta);

		Objects.requireNonNull(watcher.getEquipment()).setHelmet(helmet);
		watcher.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 255, false, false));
		watcher.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 255, false, false));
		Objects.requireNonNull(watcher.getAttribute(Attribute.SCALE)).setBaseValue(1.5);

		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 72, -88.5)); // Diamante Giant
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 72, -92.5)); // Bonzo
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 76, -92.5)); // Meepy_
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 76, -88.5)); // Mallyanke
		/* -------------------- "Let's see how you can handle this" -------------------- */
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 72, -88.5)); // valej
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 72, -84.5)); // sillygreenj
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 72, -77.5)); // a6j3
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 76, -77.5)); // Nucleararmadillo
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 80, -77.5)); // TankForM7
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 80, -84.5)); // baldkc0303
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 76, -84.5)); // nil4k
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 76, -88.5)); // editqble
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 80, -88.5)); // uncheck
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 80, -92.5)); // EvilMerlyn
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 76, -92.5)); // JennAiel
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 72, -92.5)); // Stenoe
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 72, -99.5)); // aalatif_
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 76, -99.5)); // Deanvm
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 80, -99.5)); // Beethoven_

		sendChatMessage("Things feel a little more roomy now, eh?");
		Utils.scheduleTask(() -> sendChatMessage("I've knocked down those pillars to go for a more... open concept."), 80);
		Utils.scheduleTask(() -> sendChatMessage("Plus I needed to give my new friends some space to roam..."), 160);
		Utils.scheduleTask(() -> travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.getFirst(), MOB_NAMES.getFirst()), 240);
	}

	private static void travelToAndSpawnMob(Location l, String mobName) {
		Location current = watcher.getLocation();
		double speed = 0.6; // blocks per tick

		moveEntitySmooth(current, l, speed, () -> {
			Location headStart = l.clone();
			Location endLoc = new Location(world, -120.5, 75, -88.5);

			moveEntitySmooth(headStart, endLoc, 0.3, () -> spawnMob(endLoc, mobName));

			sendChatMessage(SPAWN_LINES.get(random.nextInt(5)));

			Utils.scheduleTask(() -> {
				mobCount++;
				if(mobCount == 4 || mobCount == 19) {
					returnToOriginalPosition();
				} else {
					travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.get(mobCount), MOB_NAMES.get(mobCount));
				}
			}, 10);
		});
	}

	private static void moveEntitySmooth(Location start, Location end, double speed, Runnable onComplete) {
		Vector direction = end.clone().subtract(start).toVector().normalize().multiply(speed);
		int steps = (int) (start.distance(end) / speed);
		for(int i = 0; i < steps; i++) {
			int finalI = i;
			Utils.scheduleTask(() -> {
				Location newLoc = start.clone().add(direction.clone().multiply(finalI));
				watcher.teleport(newLoc);
				if(finalI == steps - 1 && onComplete != null) {
					onComplete.run();
				}
			}, i);
		}
	}

	private static void spawnMob(Location location, String mobName) {
		Zombie mob = (Zombie) world.spawnEntity(location, EntityType.ZOMBIE);
		mob.setCustomNameVisible(true);
		mob.setAI(false);
		mob.setSilent(true);
		mob.setAdult();
		mob.setPersistent(true);
		mob.setRemoveWhenFarAway(false);
		Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-2);
		if(mobName.equals("Diamante Giant")) {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤ " + ChatColor.YELLOW + 80 + "/" + 80);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(80);
			mob.setHealth(80);
			Objects.requireNonNull(mob.getAttribute(Attribute.SCALE)).setBaseValue(16);
		} else if(mobName.equals("Bonzo")) {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤ " + ChatColor.YELLOW + 50 + "/" + 50);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(50);
			mob.setHealth(50);
		} else {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤ " + ChatColor.YELLOW + 24 + "/" + 24);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(24);
			mob.setHealth(24);
		}

		if(mobCount == 19) {
			Utils.scheduleTask(() -> sendChatMessage("You have proven yourself.  You may pass."), 1);
		} else {
			Utils.scheduleTask(() -> sendChatMessage(KILLED_LINES.get(random.nextInt(5))), 1);
		}
	}

	private static void returnToOriginalPosition() {
		if(watcher != null && world != null) {
			if(mobCount != 19) {
				moveEntitySmooth(watcher.getLocation(), ORIGINAL_POSITION, 0.6, () -> sendChatMessage("Let's see how you can handle this."));
				Utils.scheduleTask(() -> travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.get(mobCount - 1), MOB_NAMES.get(mobCount - 1)), 40);
			} else {
				moveEntitySmooth(watcher.getLocation(), ORIGINAL_POSITION, 0.6, null);
			}
		}
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.RED + "[BOSS] The Watcher" + ChatColor.WHITE + ": " + message);
	}
}
