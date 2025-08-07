package instructions.bosses;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_21_R3.profile.CraftPlayerProfile;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import plugin.M7tas;
import plugin.Utils;

import java.util.*;

public class Watcher {
	@SuppressWarnings("FieldCanBeLocal")
	private static Zombie watcher;
	private static World world;
	private static final Location ORIGINAL_POSITION = new Location(null, -120.5, 72.0, -88.5, -180, 0);
	private static final List<Location> MOB_SPAWN_LOCATIONS = new ArrayList<>();
	private static final List<String> MOB_NAMES = List.of("Diamante Giant", "Bonzo", "Meepy_", "Mallyanke", "valej", "nograssbro", "a6j3", "Nucleararmadillo", "lfgm7", "akc0303", "nil4k", "editqble", "JennAiel", "Stenoe", "aalatif_", "Deanvm", "uncheck", "EvilMerlyn", "Beethoven_");
	private static final List<String> SPAWN_LINES = List.of("This guy looks like a fighter.", "Hmmm... this one!", "You'll do.", "Go, fight!", "Go and live again!");
	private static final List<String> KILLED_LINES = List.of("Not bad.", "That one was weak anyway.", "I'm impressed.", "Very nice.", "Aw, I liked that one.");
	private static int mobCount = 0;
	private static int mobsKilled = 0;
	private static final Random random = new Random();
	private static final double MAX_SPEED = 0.64; // blocks per tick

	// Boss bar for the Watcher
	private static BossBar watcherBossBar;

	public static void watcherInstructions(World temp, boolean doContinue) {
		cleanup(); // Clean up any previous instance

		mobCount = 0;
		mobsKilled = 0;
		world = temp;
		ORIGINAL_POSITION.setWorld(world);

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

		// Create the boss bar
		createWatcherBossBar();

		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 71, -88.5)); // Diamante Giant
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 71, -92.5)); // Bonzo
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 75, -92.5)); // Meepy_
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 75, -88.5)); // Mallyanke
		/* -------------------- "Let's see how you can handle this" -------------------- */
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 71, -88.5)); // valej
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 71, -84.5)); // nograssbro
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 71, -77.5)); // a6j3
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 75, -77.5)); // Nucleararmadillo
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 79, -77.5)); // lfgm7
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 79, -84.5)); // baldkc0303
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 75, -84.5)); // nil4k
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 75, -88.5)); // editqble
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 75, -92.5)); // JennAiel
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 71, -92.5)); // Stenoe
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 71, -99.5)); // aalatif_
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 75, -99.5)); // Deanvm
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 79, -99.5)); // uncheck
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 79, -92.5)); // EvilMerlyn
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 79, -88.5)); // Beethoven_

		// tick 0 = clear tick 1
		sendChatMessage("Things feel a little more roomy now, eh?");
		Utils.scheduleTask(() -> sendChatMessage("I've knocked down those pillars to go for a more... open concept."), 80);
		Utils.scheduleTask(() -> sendChatMessage("Plus I needed to give my new friends some space to roam..."), 160);
		Utils.scheduleTask(() -> travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.getFirst(), MOB_NAMES.getFirst()), 240);

		// Schedule kill messages and boss bar updates
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 437);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 442);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 447);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 452);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 494);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 527);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 568);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 598);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 631);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 660);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 689);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 718);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 751);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 783);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 824);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 854);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 887);
		Utils.scheduleTask(() -> { sendChatMessage(KILLED_LINES.get(random.nextInt(5))); mobsKilled++; updateWatcherBossBar(); }, 916);
		Utils.scheduleTask(() -> { mobsKilled++; updateWatcherBossBar(); sendChatMessage("You have proven yourself.  You may pass."); }, 946);
		Utils.scheduleTask(() -> {
			cleanup();
			world.spawnEntity(new Location(world, -120.5, 69, -74.5), EntityType.LIGHTNING_BOLT);
			if(doContinue) {
				Maxor.maxorInstructions(world, true);
			}
		}, 1026);
	}

	private static void createWatcherBossBar() {
		String title = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Watcher" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 19 + "/" + 19;

		watcherBossBar = Bukkit.createBossBar(title, BarColor.RED, BarStyle.SOLID);
		watcherBossBar.setProgress(1.0);

		// Add all online players
		for(Player player : Bukkit.getOnlinePlayers()) {
			watcherBossBar.addPlayer(player);
		}
	}

	private static void updateWatcherBossBar() {
		if(watcherBossBar == null) return;

		int mobsRemaining = 19 - mobsKilled;
		double progress = mobsRemaining / 19.0;

		String title = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Watcher" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + mobsRemaining + "/" + 19;

		watcherBossBar.setTitle(title);
		watcherBossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
	}

	private static void travelToAndSpawnMob(Location l, String mobName) {
		Location current = watcher.getLocation();
		moveEntitySmooth(watcher, current, l, MAX_SPEED, () -> {
			mobCount++;
			Location headStart = l.clone();
			Location endLoc = new Location(world, -120.5, 75, -88.5);

			// Create armor stand with zombie head
			ArmorStand stand = world.spawn(headStart, ArmorStand.class);
			stand.setGravity(false);
			stand.setVisible(false);
			stand.setCustomNameVisible(false);
			stand.setInvulnerable(true);
			stand.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 255));
			ItemStack zombieHead = new ItemStack(Material.ZOMBIE_HEAD);
			Objects.requireNonNull(stand.getEquipment()).setHelmet(zombieHead);

			moveEntitySmooth(stand, headStart.clone().add(0, 1, 0), endLoc, 0.4, () -> {
				spawnMob(endLoc, mobName);
				stand.remove(); // Remove armor stand after reaching destination
			});

			sendChatMessage(SPAWN_LINES.get(random.nextInt(5)));

			Utils.scheduleTask(() -> {
				if(mobCount == 4 || mobCount == 19) {
					returnToOriginalPosition();
				} else {
					travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.get(mobCount), MOB_NAMES.get(mobCount));
				}
			}, 16);
		});
	}

	private static void moveEntitySmooth(Entity entity, Location start, Location end, double maxSpeed, Runnable onComplete) {
		final double accel = 0.08;
		final Vector totalVector = end.toVector().subtract(start.toVector());
		final double totalDistance = totalVector.length();
		final Vector direction = totalVector.clone().normalize();

		final double accelDist = (maxSpeed * maxSpeed) / (2 * accel);

		int accelTicks, cruiseTicks, decelTicks;

		if(totalDistance < (accelDist + accelDist)) {
			// Triangular motion profile: peak speed is less than maxSpeed
			double peakSpeed = Math.sqrt(totalDistance * accel);
			accelTicks = (int) Math.ceil(peakSpeed / accel);
			decelTicks = accelTicks;
			cruiseTicks = 0;
		} else {
			accelTicks = (int) Math.ceil(maxSpeed / accel);
			decelTicks = accelTicks;
			double cruiseDistance = totalDistance - accelDist - accelDist;
			cruiseTicks = (int) Math.ceil(cruiseDistance / maxSpeed);
		}

		final int totalTicks = accelTicks + cruiseTicks + decelTicks;
		entity.teleport(start.clone());

		new BukkitRunnable() {
			int tick = 0;
			double currentSpeed = 0;
			final Location currentLoc = start.clone();

			@Override
			public void run() {
				if(!entity.isValid()) {
					cancel();
					return;
				}

				// Phase-based motion
				if(tick < accelTicks) {
					currentSpeed = accel * (tick + 1);
				} else if(tick < accelTicks + cruiseTicks) {
					currentSpeed = maxSpeed;
				} else {
					int decelTick = tick - accelTicks - cruiseTicks;
					currentSpeed = maxSpeed - accel * (decelTick + 1);
					if(currentSpeed < 0) currentSpeed = 0;
				}

				Vector moveVec = direction.clone().multiply(currentSpeed);
				currentLoc.add(moveVec);

				if(entity.equals(watcher)) {
					watcher.teleport(currentLoc.clone().setDirection(direction));
				} else {
					entity.teleport(currentLoc.clone());
				}

				if(entity instanceof ArmorStand armorStand) {
					world.spawnParticle(Particle.DUST, currentLoc.clone().add(0, 1.5, 0), 0, new Particle.DustOptions(Color.BLACK, 1));
					world.spawnParticle(Particle.DUST, currentLoc.clone().add(0, 1.5, 0), 0, new Particle.DustOptions(Color.PURPLE, 1));
					world.spawnParticle(Particle.DUST, currentLoc.clone().add(0, 1.5, 0), 0, new Particle.DustOptions(Color.WHITE, 1));
					EulerAngle pose = armorStand.getHeadPose();
					armorStand.setHeadPose(pose.add(0, Math.toRadians(25), 0));
				}

				tick++;
				if(tick >= totalTicks) {
					entity.teleport(end);

					if(entity.equals(watcher)) {
						Vector lookVec = ORIGINAL_POSITION.toVector().subtract(end.toVector());
						if(lookVec.lengthSquared() < 1e-6) {
							lookVec = new Vector(0, 0, 1);
						} else {
							lookVec.normalize();
						}
						Location finalLoc = end.clone();
						finalLoc.setDirection(lookVec);
						watcher.teleport(finalLoc);
					}

					if(onComplete != null) {
						onComplete.run();
					}
					cancel();
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private static void spawnMob(Location location, String mobName) {
		Zombie mob = (Zombie) world.spawnEntity(location, EntityType.ZOMBIE);
		mob.setCustomNameVisible(true);
		mob.setAI(true);
		Utils.scheduleTask(() -> mob.setAI(false), 20);
		mob.setGravity(true);
		mob.setSilent(true);
		mob.setAdult();
		mob.setPersistent(true);
		mob.setRemoveWhenFarAway(false);

		if(mobName.equals("Diamante Giant")) {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤ " + ChatColor.YELLOW + 40 + "/" + 40);
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-22);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(40);
			mob.setHealth(40);
			Objects.requireNonNull(mob.getAttribute(Attribute.SCALE)).setBaseValue(6);
			Objects.requireNonNull(mob.getEquipment()).setHelmet(new ItemStack(Material.DIAMOND_HELMET));
			mob.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
			mob.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
			mob.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
			mob.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
		} else if(mobName.equals("Bonzo")) {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤ " + ChatColor.YELLOW + 30 + "/" + 30);
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-2);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(30);
			mob.setHealth(30);
			Objects.requireNonNull(mob.getEquipment()).setItemInMainHand(new ItemStack(Material.BLAZE_ROD));
		} else {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤ " + ChatColor.YELLOW + 12 + "/" + 12);
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-12);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(12);
			mob.setHealth(12);
			Objects.requireNonNull(mob.getEquipment()).setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
			mob.getEquipment().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
			mob.getEquipment().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
			mob.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
		}
		Objects.requireNonNull(location.getWorld()).playSound(location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 2.0F);
	}

	private static void returnToOriginalPosition() {
		if(watcher != null && world != null) {
			if(mobCount != 19) {
				moveEntitySmooth(watcher, watcher.getLocation(), ORIGINAL_POSITION, MAX_SPEED, () -> sendChatMessage("Let's see how you can handle this."));
				Utils.scheduleTask(() -> travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.get(mobCount), MOB_NAMES.get(mobCount)), 60);
			} else {
				moveEntitySmooth(watcher, watcher.getLocation(), ORIGINAL_POSITION, MAX_SPEED, null);
			}
		}
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.RED + "[BOSS] The Watcher" + ChatColor.WHITE + ": " + message);
	}

	// Cleanup method
	private static void cleanup() {
		if(watcherBossBar != null) {
			watcherBossBar.removeAll();
			watcherBossBar = null;
		}
		if(watcher != null && !watcher.isDead()) {
			watcher.remove();
			watcher = null;
		}
		mobsKilled = 0;
	}

	// Force cleanup for /tas command
	public static void forceCleanup() {
		cleanup();
	}
}