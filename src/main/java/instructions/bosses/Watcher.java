package instructions.bosses;

import instructions.bosses.maxor.Maxor;
import instructions.players.Mage;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_21_R7.profile.CraftPlayerProfile;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import plugin.BossScheduler;
import plugin.FakePlayerManager;
import plugin.M7tas;
import plugin.Utils;

import java.util.*;

/**
 * Blood Room pre-boss. Behaviour-driven (like the Wither bosses): a one-shot detection task armed at clear-tick 0
 * spawns the Watcher the first tick a player enters the Blood Room bounds; the spawn choreography is unchanged; kill
 * lines fire on real Blood-Mob deaths ({@link #handleMobDeath}); 80 ticks after the 19th death a nether portal opens
 * and stepping into it teleports the actors to the boss spawn (and hands off to Maxor). Per-class singleton reused
 * across runs; {@link #resetState()} clears all per-fight state.
 */
public class Watcher {
	public static final Watcher INSTANCE = new Watcher();

	private Watcher() {
	}

	private Zombie watcher;
	private World world;
	private static final Location ORIGINAL_POSITION = new Location(null, -120.5, 72.0, -56.5, -180, 0);
	private final List<Location> MOB_SPAWN_LOCATIONS = new ArrayList<>();
	private static final List<String> MOB_NAMES = List.of("Diamante Giant", "Bonzo", "Nucleararmadillo", "Jamie_2013", "JennAiel", "s3a3m3", "editqble", "valej", "Merlynade", "HenbotB", "Katsumi9877", "BananaBrigade", "derM0RITZZ", "TypeW", "aalatif_", "Cubpletionist", "akc0303", "AsapIcey", "Beethoven_");
	private static final List<String> SPAWN_LINES = List.of("This guy looks like a fighter.", "Hmmm... this one!", "You'll do.", "Go, fight!", "Go and live again!");
	private static final List<String> KILLED_LINES = List.of("Not bad.", "That one was weak anyway.", "I'm impressed.", "Very nice.", "Aw, I liked that one.");
	private int mobCount = 0;
	private int mobsKilled = 0;
	private static final Random random = new Random();
	private static final double MAX_SPEED = 0.64; // blocks per tick

	// Boss bar for the Watcher
	private BossBar watcherBossBar;

	// --- Behaviour-driven state ---
	private boolean active = false;            // encounter spawned & running (detection / death guard)
	private boolean tasActive = false;         // true iff the triggering player was a fake (TAS run)
	private boolean doContinue = false;        // chain into Maxor on portal entry (set by arm())
	private Runnable maxorHandoff = null;      // full Maxor handoff supplied by TAS.runTAS
	private int triggerPhaseTick = 0;          // Utils.phaseTick() captured at spawn — overall-column basis
	private BukkitTask detectTask;
	private BukkitTask portalDetectTask;

	// Blood Room bounds: (-136,66,-72) -> (-106,99,-42)
	private static final double BR_MIN_X = -136, BR_MAX_X = -106;
	private static final double BR_MIN_Y = 66, BR_MAX_Y = 99;
	private static final double BR_MIN_Z = -72, BR_MAX_Z = -42;

	// Boss spawn point
	private static final Location BOSS_SPAWN = new Location(null, 73.5, 221, 14.5, 0f, 0f);

	// ============================== Arming & detection ==============================

	/**
	 * Supply the run's continuation intent + Maxor handoff. Does NOT spawn or start detection — that's
	 * {@link #beginDetection(World)} at clear-tick 0. Called from TAS.runTAS for "all"/"clear".
	 */
	public void arm(World w, boolean doContinue, Runnable maxorHandoff) {
		this.world = w;
		this.doContinue = doContinue;
		this.maxorHandoff = maxorHandoff;
	}

	/**
	 * Start the one-shot Blood-Room scan (clear-tick 0). Spawns the Watcher the first tick a player is in bounds,
	 * preferring a fake (→ TAS active); then cancels itself.
	 */
	public void beginDetection(World w) {
		this.world = w;
		if(detectTask != null && !detectTask.isCancelled()) {
			detectTask.cancel();
		}
		detectTask = new BukkitRunnable() {
			@Override
			public void run() {
				if(active) {
					cancel();
					return;
				}
				Player fake = firstInBounds(true);
				Player trigger = fake != null ? fake : firstInBounds(false);
				if(trigger != null) {
					tasActive = (fake != null);
					spawnEncounter(trigger);
					cancel();
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	/**
	 * Start the encounter the instant the Blood Door opens (faithful to F7 — the Watcher appears on door-open,
	 * not on walking into the room). Only fires if detection was armed this run ({@link #beginDetection}) and the
	 * Watcher hasn't already spawned; cancels the bounds scan and spawns immediately. {@code tasActive} follows
	 * whether any fake actor exists (TAS) vs real players (practice).
	 */
	public void startOnBloodDoor() {
		if(active) return;
		if(detectTask == null || detectTask.isCancelled()) return; // not armed/waiting this run
		tasActive = !FakePlayerManager.getFakePlayers().isEmpty();
		detectTask.cancel();
		spawnEncounter(null); // trigger param is unused by spawnEncounter (only sets tasActive, done above)
	}

	/** First in-bounds fake (wantFake=true) or genuine real non-spectator player (wantFake=false), else null. */
	private Player firstInBounds(boolean wantFake) {
		if(world == null) return null;
		for(Player p : world.getPlayers()) {
			boolean isFake = FakePlayerManager.getFakePlayers().containsValue(p);
			if(wantFake != isFake) continue;
			if(!wantFake && p.getGameMode() == GameMode.SPECTATOR) continue;
			if(inBloodRoom(p.getLocation())) return p;
		}
		return null;
	}

	private static boolean inBloodRoom(Location l) {
		return l.getX() >= BR_MIN_X && l.getX() <= BR_MAX_X
				&& l.getY() >= BR_MIN_Y && l.getY() <= BR_MAX_Y
				&& l.getZ() >= BR_MIN_Z && l.getZ() <= BR_MAX_Z;
	}

	// ============================== Encounter ==============================

	private void spawnEncounter(Player trigger) {
		resetState();
		active = true;
		triggerPhaseTick = Utils.phaseTick();
		ORIGINAL_POSITION.setWorld(world);

		Utils.timer(ChatColor.GREEN + "Watcher spawned on tick " + triggerPhaseTick);

		watcher = (Zombie) world.spawnEntity(ORIGINAL_POSITION, EntityType.ZOMBIE);
		watcher.addScoreboardTag("TASWatcher");
		watcher.setAI(false);
		watcher.setSilent(true);
		watcher.setPersistent(true);
		watcher.setRemoveWhenFarAway(false);

		Multimap<String, Property> props = HashMultimap.create();
		props.put("textures", new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTc0Njg0OTQ1NjQxOSwKICAicHJvZmlsZUlkIiA6ICIxZjk0OTQzN2RlYmQ0ODgyYTlhYzZhZmZmN2RhNDcxMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaWlra2FLYSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83YjBkNTI3OGNkZWUwNGM1MjBhOWY1ZDE1M2E1MmI0ZWZjNzBmMzAzMjM5MjY2OGQyMTExNjJkNWFkYzAxYjExIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "awvIfqy7f12hqzBY/BZrhqCpC3xl0zeb0xTOVERZlzXsmk+ZivSyC8ZAlsR1Kmam0aLNDlvO3Nrl8ZGg5n77H+aUkZsoGz4DsuV2GoFv71UxXpPgAVkiCw0kPmNr9O17JChNU2HrO2hd1X3kqPX9gbA/JZ4+kCpcmbEtr7+VAl7xScOEWvKZPimdijG6hkNrBnkcttk+TYdIenrKNrZf346l2nD9nRif+1istHv9ouxZ7GguZPFFTTqtuljhdjsDQ5lQnFN/Q0b4cENMErlAkzam4n2jwTBJPWz9BeIUdgpOr4qyp4bTOLrD3mVfdSEJ+Q4hMjQLZZeYLxMZLSCqm56ns+rzm7O0aj7/+sjxngWZuT8z4U+g2J5QOOA3n8R3Z+QvEHitb1RZdM8DccYb9VwSbGG2jZ8acInxSoIT5bFWWfp0Bh+rwfuNe+v2hFReyUz35BwKrYUOxqL4+A7/McSpik/C+9BVMYL5n78FMD+1+SlJniMwAoPlRpz87yGYivEH9aAlEnTLE+7Tpp6wsiFCaQp5WJ8vfJnV9HVxDYjFs7xB29Cw+FIQnYSsT5U7Uv6znjBMWRmHI9zeU7GzQ0eNQkThSbzX+dE/c1WyPXVuL/wTfefbgh6jm1i6rNGz/a3RdnWk8ItXu/pYQjSmKnc2FJH+x28VXkYl3qQr0gw="));
		PropertyMap propertyMap = new PropertyMap(props);
		GameProfile gp = new GameProfile(UUID.randomUUID(), "watcher", propertyMap);

		CraftPlayerProfile profile = new CraftPlayerProfile(gp);

		ItemStack helmet = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) helmet.getItemMeta();
		assert meta != null;
		meta.setOwnerProfile(profile);
		helmet.setItemMeta(meta);

		Objects.requireNonNull(watcher.getEquipment()).setHelmet(helmet);
		watcher.getEquipment().setChestplate(new ItemStack(Material.AIR));
		watcher.getEquipment().setLeggings(new ItemStack(Material.AIR));
		watcher.getEquipment().setBoots(new ItemStack(Material.AIR));
		watcher.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
		watcher.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
		watcher.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 255, false, false));
		watcher.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 255, false, false));
		Objects.requireNonNull(watcher.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(watcher.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
		Objects.requireNonNull(watcher.getAttribute(Attribute.SCALE)).setBaseValue(1.5);

		// Create the boss bar
		createWatcherBossBar();

		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 71, -56.5)); // Diamante Giant
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 71, -60.5)); // Bonzo
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 75, -60.5)); // Nucleararmadillo
		MOB_SPAWN_LOCATIONS.add(new Location(world, -131.5, 75, -56.5)); // Jamie_2013
		/* -------------------- "Let's see how you can handle this" -------------------- */
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 71, -56.5)); // JennAiel
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 71, -52.5)); // s3a3m3
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 71, -45.5)); // editqble
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 75, -45.5)); // valej
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 79, -45.5)); // Merlynade
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 79, -52.5)); // HenbotB
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 75, -52.5)); // Katsumi9877
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 75, -56.5)); // BananaBrigade
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 75, -60.5)); // derM0RITZZ
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 71, -60.5)); // TypeW
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 71, -67.5)); // aalatif_
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 75, -67.5)); // Cubpletionist
		MOB_SPAWN_LOCATIONS.add(new Location(world, -111.5, 79, -67.5)); // akc0303
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 79, -60.5)); // AsapIcey
		MOB_SPAWN_LOCATIONS.add(new Location(world, -109.5, 79, -56.5)); // Beethoven_

		// Choreography start (anchored to the real entry tick — replaces the old hardcoded 3-tick "time to bounds")
		sendChatMessage("Things feel a little more roomy now, eh?");
		Utils.scheduleTask(() -> sendChatMessage("I've knocked down those pillars to go for a more... open concept."), 80);
		Utils.scheduleTask(() -> sendChatMessage("Plus I needed to give my new friends some space to roam..."), 160);
		Utils.scheduleTask(() -> travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.getFirst(), MOB_NAMES.getFirst()), 240);
	}

	// ============================== Event-driven kills ==============================

	/** Dispatched from the EntityDeathEvent listener. Counts Blood-Mob deaths, drives kill lines + portal. */
	public void handleMobDeath(EntityDeathEvent e) {
		if(!active) return;
		if(!e.getEntity().getScoreboardTags().contains("WatcherMob")) return;

		mobsKilled++;
		updateWatcherBossBar();
		Utils.timer(ChatColor.GREEN + "Blood Mob " + mobsKilled + "/19 killed | " + formatTick(phaseRel()));

		if(mobsKilled < 19) {
			sendChatMessage(KILLED_LINES.get(random.nextInt(5)));
		} else {
			sendChatMessage("You have proven yourself.  You may pass.");
			bloodCampFinished();
			Utils.scheduleTask(this::openPortal, 80);
		}
	}

	/**
	 * "Blood Camp finished" milestone — owned here so it reports on the Watcher phase clock (like "Entered Boss"),
	 * but triggered by the Mage's final blood-camp left click (see the Mage blood-camp choreography).
	 */
	public void bloodCampFinished() {
		Utils.timer(ChatColor.GREEN + "Blood Camp finished in " + formatTick(phaseRel()));
	}

	// ============================== Portal sequence ==============================

	private void openPortal() {
		// The Watcher vanishes with the strike (boss bar + entity removed); keep counters until handoff.
		removeWatcherEntity();

		world.spawnEntity(new Location(world, -120.5, 69, -42.5), EntityType.LIGHTNING_BOLT);
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT);
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
		Utils.runCommand("fill -120 69 -43 -122 72 -43 minecraft:nether_portal[axis=x]");
		Utils.timer(ChatColor.GREEN + "Boss Portal Opened in " + formatTick(phaseRel()));
		Utils.debug(Utils.DebugType.BOSS, "Lightning struck, nether portal opened" + (Utils.isSuperVerbose() ? " at -120..-122 / 69..72 / -43" : ""));

		if(portalDetectTask != null && !portalDetectTask.isCancelled()) {
			portalDetectTask.cancel();
		}
		portalDetectTask = new BukkitRunnable() {
			@Override
			public void run() {
				for(Player p : world.getPlayers()) {
					if(inPortal(p.getLocation())) {
						enterPortal(p);
						cancel();
						return;
					}
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	/** Portal block region: x [-122,-120], y [69,72], z = -43. */
	private static boolean inPortal(Location l) {
		return l.getBlockX() >= -122 && l.getBlockX() <= -120
				&& l.getBlockY() >= 69 && l.getBlockY() <= 72
				&& l.getBlockZ() == -43;
	}

	private void enterPortal(Player p) {
		Location boss = BOSS_SPAWN.clone();
		boss.setWorld(world);
		Utils.debug(Utils.DebugType.BOSS, "Portal entered by " + Utils.getRealName(p) + " → teleporting " + (tasActive ? "fakes" : "all players"));

		// Teleport the actors THIS tick; the boss + player routines start together on the NEXT tick.
		if(tasActive) {
			FakePlayerManager.getFakePlayers().values().forEach(f -> Utils.teleport(f, boss));
			Utils.timer(ChatColor.GREEN + "Entered Boss in " + formatTick(phaseRel()));
			// Blood-room blessings: normally collected as item drops, but the TAS enters the boss immediately, so
			// there's no time to walk over them — broadcast them manually 200 ticks in. Owned here (the Watcher-driven
			// portal entry) rather than in the Mage routine.
			Utils.scheduleTask(() -> {
				Utils.broadcastBlessing(Mage.get(), Utils.BlessingType.POWER, 5);
				Utils.broadcastBlessing(Mage.get(), Utils.BlessingType.LIFE, 5);
			}, 200);
			if(doContinue && maxorHandoff != null) {
				Utils.scheduleTask(maxorHandoff, 1); // spawns Maxor + starts each player's maxor(true) together
			}
		} else {
			for(Player pl : world.getPlayers()) {
				if(FakePlayerManager.getFakePlayers().containsValue(pl)) continue;
				if(pl.getGameMode() == GameMode.SPECTATOR) continue;
				Utils.teleport(pl, boss);
			}
			Utils.scheduleTask(() -> Maxor.maxorInstructions(world, false), 1);
		}

		Utils.runCommand("fill -120 69 -43 -122 72 -43 minecraft:air");
		active = false;
	}

	// ============================== Boss bar ==============================

	private void createWatcherBossBar() {
		String title = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "The Watcher" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.YELLOW + 19 + ChatColor.RED + "❤";

		watcherBossBar = Bukkit.createBossBar(title, BarColor.RED, BarStyle.SOLID);
		watcherBossBar.setProgress(1.0);

		// Add all online players
		for(Player player : Bukkit.getOnlinePlayers()) {
			watcherBossBar.addPlayer(player);
		}
	}

	private void updateWatcherBossBar() {
		if(watcherBossBar == null) {
			return;
		}

		int mobsRemaining = 19 - mobsKilled;
		double progress = mobsRemaining / 19.0;

		String title = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "The Watcher" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.YELLOW + mobsRemaining + ChatColor.RED + "❤";

		watcherBossBar.setTitle(title);
		watcherBossBar.setProgress(Math.clamp(progress, 0.0, 1.0));
	}

	// ============================== Choreography (unchanged) ==============================

	private void travelToAndSpawnMob(Location l, String mobName) {
		Location current = watcher.getLocation();
		moveEntitySmooth(watcher, current, l, MAX_SPEED, () -> {
			mobCount++;
			final int idx = mobCount; // 1-based index of THIS mob (mobCount advances as spawns chain)
			Location headStart = l.clone();
			Location endLoc = new Location(world, -120.5, 75, -56.5);

			// Create armor stand with zombie head
			ArmorStand stand = world.spawn(headStart, ArmorStand.class);
			stand.setGravity(false);
			stand.setVisible(false);
			stand.setCustomNameVisible(false);
			stand.setInvulnerable(true);
			stand.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 255));
			ItemStack zombieHead = new ItemStack(Material.ZOMBIE_HEAD);
			Objects.requireNonNull(stand.getEquipment()).setHelmet(zombieHead);

			if(Utils.isSuperVerbose()) Utils.debug(Utils.DebugType.BOSS, "Begin spawning " + mobName + " (" + fmt(l) + ")");
			moveEntitySmooth(stand, headStart.clone().add(0, 1, 0), endLoc, 0.4, () -> {
				// Only the actual world-spawn is migrated onto the boss ticker — the travel/animation above stays on
				// the old raw schedule (unchanged Watcher movement). schedule(...,1) fires at the START of the next
				// tick (boss lane, before all player choreography), so the mob exists before the mage's same-tick
				// beam instead of after it (the old mid-tick spawn lost the task-id race to the run-start beam).
				BossScheduler.schedule(() -> {
					spawnMob(endLoc, mobName);
					Utils.timer(ChatColor.GREEN + "Blood Mob " + idx + "/19 spawned (" + mobName + ") | " + formatTick(phaseRel()));
					stand.remove(); // Remove armor stand after reaching destination
				}, 1);
			});

			sendChatMessage(SPAWN_LINES.get(random.nextInt(5)));

			if(mobCount == 4 || mobCount == 19) {
				returnToOriginalPosition();
			} else {
				travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.get(mobCount), MOB_NAMES.get(mobCount));
			}
		});
	}

	private void moveEntitySmooth(Entity entity, Location start, Location end, double maxSpeed, Runnable onComplete) {
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

		final int movementTicks = accelTicks + cruiseTicks + decelTicks;
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
				if(tick >= movementTicks) {
					entity.teleport(end);

					if(entity.equals(watcher)) {
						Vector lookVec = ORIGINAL_POSITION.toVector().subtract(end.toVector());
						if(lookVec.lengthSquared() < 1e-6) {
							// At ORIGINAL_POSITION: preserve its yaw/pitch
							Location finalLoc2 = end.clone();
							finalLoc2.setYaw(ORIGINAL_POSITION.getYaw());
							finalLoc2.setPitch(ORIGINAL_POSITION.getPitch());
							watcher.teleport(finalLoc2);
							if(onComplete != null) {
								onComplete.run();
							}
							cancel();
							return;
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

	private void spawnMob(Location location, String mobName) {
		Zombie mob = (Zombie) world.spawnEntity(location, EntityType.ZOMBIE);
		mob.setAdult();
		// Clear random armor and prevent chicken jockey from finalizeSpawn
		Objects.requireNonNull(mob.getEquipment()).setHelmet(null);
		mob.getEquipment().setChestplate(null);
		mob.getEquipment().setLeggings(null);
		mob.getEquipment().setBoots(null);
		if(mob.isInsideVehicle()) {
			Entity vehicle = mob.getVehicle();
			mob.leaveVehicle();
			if(vehicle != null) vehicle.remove();
		}
		mob.setCustomNameVisible(true);
		mob.addScoreboardTag("WatcherMob");
		mob.setAI(true);
		Utils.scheduleTask(() -> mob.setAI(false), 20);
		mob.setGravity(true);
		mob.setSilent(true);
		mob.setPersistent(true);
		mob.setRemoveWhenFarAway(false);

		if(mobName.equals("Diamante Giant")) {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤" + ChatColor.YELLOW + "80M");
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(40);
			mob.setHealth(40);
			Objects.requireNonNull(mob.getAttribute(Attribute.SCALE)).setBaseValue(6);
			Objects.requireNonNull(mob.getEquipment()).setHelmet(new ItemStack(Material.DIAMOND_HELMET));
			mob.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
			mob.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
			mob.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
			mob.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
		} else if(mobName.equals("Bonzo")) {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤" + ChatColor.YELLOW + "60M");
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(30);
			mob.setHealth(30);
			Objects.requireNonNull(mob.getEquipment()).setItemInMainHand(new ItemStack(Material.BLAZE_ROD));
		} else {
			mob.setCustomName(ChatColor.YELLOW + mobName + ChatColor.RESET + ChatColor.RED + " ❤" + ChatColor.YELLOW + "24M");
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
			Objects.requireNonNull(mob.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
			Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(12);
			mob.setHealth(12);
			Objects.requireNonNull(mob.getEquipment()).setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
			mob.getEquipment().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
			mob.getEquipment().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
			mob.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
		}
		Objects.requireNonNull(location.getWorld()).playSound(location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 2.0F);
	}

	private void returnToOriginalPosition() {
		if(watcher != null && world != null) {
			if(mobCount != 19) {
				moveEntitySmooth(watcher, watcher.getLocation(), ORIGINAL_POSITION, MAX_SPEED, () -> sendChatMessage("Let's see how you can handle this."));
				Utils.scheduleTask(() -> {
					Utils.debug(Utils.DebugType.BOSS, "Watcher moved");
					travelToAndSpawnMob(MOB_SPAWN_LOCATIONS.get(mobCount), MOB_NAMES.get(mobCount));
				}, 60);
			} else {
				moveEntitySmooth(watcher, watcher.getLocation(), ORIGINAL_POSITION, MAX_SPEED, null);
			}
		}
	}

	private void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.RED + "[BOSS] The Watcher" + ChatColor.WHITE + ": " + message);
	}

	// ============================== Cleanup / state ==============================

	/** Zero out per-fight flags, counters, scheduled tasks, and leftover mobs. */
	private void resetState() {
		mobCount = 0;
		mobsKilled = 0;
		MOB_SPAWN_LOCATIONS.clear();
		if(portalDetectTask != null && !portalDetectTask.isCancelled()) {
			portalDetectTask.cancel();
		}
		portalDetectTask = null;
		// Drop any stale Blood Mobs from an aborted previous fight so they can't miscount.
		if(world != null) {
			for(Entity ent : world.getEntitiesByClass(Zombie.class)) {
				if(ent.getScoreboardTags().contains("WatcherMob")) ent.remove();
			}
		}
	}

	private void removeWatcherEntity() {
		if(watcherBossBar != null) {
			watcherBossBar.removeAll();
			watcherBossBar = null;
		}
		if(watcher != null && !watcher.isDead()) {
			watcher.remove();
		}
		watcher = null;
	}

	private void cleanup() {
		removeWatcherEntity();
		if(detectTask != null && !detectTask.isCancelled()) {
			detectTask.cancel();
		}
		detectTask = null;
		if(portalDetectTask != null && !portalDetectTask.isCancelled()) {
			portalDetectTask.cancel();
		}
		portalDetectTask = null;
		active = false;
		mobsKilled = 0;
		mobCount = 0;
	}

	// Force cleanup for /setup and /tas reset
	public static void forceCleanup() {
		INSTANCE.cleanup();
	}

	public static BossBar getActiveBossBar() {
		return INSTANCE.watcherBossBar;
	}

	// ============================== Logging helpers (mimic WitherLord) ==============================

	private int phaseRel() {
		return Utils.phaseTick() - triggerPhaseTick;
	}

	/** Phase column = ticks since the Watcher engaged; overall = run clock (triggerPhaseTick + t). */
	private String formatTick(int t) {
		int overall = t + triggerPhaseTick;
		return ChatColor.GREEN + String.format("Watcher: %s ticks (%.2f seconds) | Overall: %s ticks (%.2f seconds)",
				formatWithSpaces(t), t / 20.0, formatWithSpaces(overall), overall / 20.0);
	}

	private static String formatWithSpaces(int n) {
		StringBuilder sb = new StringBuilder();
		String s = String.valueOf(n);
		for(int i = 0; i < s.length(); i++) {
			if(i > 0 && (s.length() - i) % 3 == 0) sb.append(' ');
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}

	private static String fmt(Location l) {
		return Utils.round(l.getX(), 1) + " " + Utils.round(l.getY(), 1) + " " + Utils.round(l.getZ(), 1);
	}
}
