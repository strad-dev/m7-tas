package instructions;

import instructions.bosses.CustomBossBar;
import instructions.bosses.Watcher;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.maxor.Maxor;
import instructions.bosses.necron.Necron;
import instructions.bosses.storm.Storm;
import instructions.bosses.witherking.WitherKing;
import listeners.CustomItems;
import listeners.GoldorListener;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import plugin.M7tas;
import plugin.Utils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Server {
	private static final Zombie[] archaeologists = new Zombie[10];
	private static Zombie yellowShadowAssassin = null;
	private static final LivingEntity[] trashMobs = new LivingEntity[18]; // each 1x1 has 6 mobs spawned

	// --- Clear-phase keys & doors ---
	// Global keys: killing the matching Angry Archaeologist grants the key, which lets ANY player open the
	// corresponding door by left/right-clicking a block within its bounds (detection + open in MiscListener).
	private static boolean hasWitherKey = false;
	private static boolean hasBloodKey = false;
	private static boolean witherDoorOpened = false;
	private static boolean bloodDoorOpened = false;

	// Door bounds as {minX, minY, minZ, maxX, maxY, maxZ} (inclusive). Match the openXxxDoor() fill regions.
	private static final int[] WITHER_DOOR = {-122, 69, -106, -120, 72, -104};
	private static final int[] BLOOD_DOOR = {-122, 69, -74, -120, 72, -72};

	public static void resetClearState() {
		hasWitherKey = false;
		hasBloodKey = false;
		witherDoorOpened = false;
		bloodDoorOpened = false;
	}

	public static boolean hasWitherKey() {
		return hasWitherKey;
	}

	public static boolean hasBloodKey() {
		return hasBloodKey;
	}

	public static void grantWitherKey(Player picker) {
		if(hasWitherKey) return;
		hasWitherKey = true;
		String name = picker != null ? Utils.getRealName(picker) : "Someone";
		Bukkit.broadcast(Utils.msg("<gold>[MVP<dark_blue>++<gold>] <name> <green>has obtained <dark_gray>Wither Key<green>!", Placeholder.unparsed("name", name)));
		if(picker != null) Utils.playLocalSound(picker, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
	}

	public static void grantBloodKey(Player picker) {
		if(hasBloodKey) return;
		hasBloodKey = true;
		String name = picker != null ? Utils.getRealName(picker) : "Someone";
		Bukkit.broadcast(Utils.msg("<gold>[MVP<dark_blue>++<gold>] <name> <green>has obtained <red>Blood Key<green>!", Placeholder.unparsed("name", name)));
		if(picker != null) Utils.playLocalSound(picker, Sound.ENTITY_ITEM_PICKUP, 2.0f, 1.0f);
	}

	private static boolean inBounds(Block b, int[] d) {
		return b.getX() >= d[0] && b.getX() <= d[3] && b.getY() >= d[1] && b.getY() <= d[4] && b.getZ() >= d[2] && b.getZ() <= d[5];
	}

	public static boolean inWitherDoor(Block b) {
		return inBounds(b, WITHER_DOOR);
	}

	public static boolean inBloodDoor(Block b) {
		return inBounds(b, BLOOD_DOOR);
	}

	/**
	 * Open the Wither Door if the player has the key and it isn't already open (one-shot). Called on a door click.
	 */
	public static void tryOpenWitherDoor(Player p) {
		if(!hasWitherKey || witherDoorOpened) return;
		witherDoorOpened = true;
		openWitherDoor(p);
	}

	/**
	 * Open the Blood Door if the player has the key and it isn't already open (one-shot). Called on a door click.
	 */
	public static void tryOpenBloodDoor() {
		if(!hasBloodKey || bloodDoorOpened) return;
		bloodDoorOpened = true;
		openBloodDoor();
	}

	public static void serverInstructions(World world, String section) {
		serverInstructions(world, section, 60);
	}

	/**
	 * @param delayTicks how long to wait before the section's instructions begin — the pre-run "get into
	 *   position" window, followed by a shared 5-second countdown. Defaults to 60 (3s) via the two-arg overload;
	 *   the network plugin passes a longer delay (e.g. 400 = 20s) when it warps a whole party in so everyone has
	 *   time to get into position. EXCEPTION: the boss/maxor sections ignore this — Maxor is the dungeon-boss
	 *   entry and on real Hypixel it starts the moment you enter, so those get only a 20-tick load grace and no
	 *   countdown (see {@link #MAXOR_GRACE_TICKS}).
	 */
	public static void serverInstructions(World world, String section, int delayTicks) {
		// Tear down any lingering Goldor phase from a previous run immediately, before this run's pre-fired
		// arrows/abilities can interact with stale phase state (e.g. an already-activated S4 device). Without
		// this, a re-run's first sharpshooter arrows land into the old still-active phase and are rejected.
		Goldor.INSTANCE.forceEndPhase();

		// boss/maxor: no prep window and no countdown — just a short load grace, then Maxor starts.
		if(section.equals("boss") || section.equals("maxor")) {
			Utils.scheduleTask(() -> startSection(world, section), MAXOR_GRACE_TICKS);
			return;
		}

		// Every other section: the pre-run "get into position" window, then a 5-second countdown, then start.
		Bukkit.broadcast(Utils.msg((instructions.bosses.WitherActions.isPracticeMode() ? "Run" : "TAS") + " starts in " + Math.max(1, delayTicks / 20) + " seconds"));
		Utils.scheduleTask(() -> countdownThenStart(world, section), delayTicks);
	}

	/** Load grace before Maxor (boss/maxor sections) — just long enough for warped-in clients to finish loading. */
	private static final int MAXOR_GRACE_TICKS = 20;

	/**
	 * The shared 5-second "Starting in N seconds" countdown (the messages the clear section has always used), then
	 * the section's actual start. Used for every section except boss/maxor.
	 */
	private static void countdownThenStart(World world, String section) {
		for(int i = 5; i >= 1; i--) {
			int secs = i;
			Utils.scheduleTask(() -> {
				Bukkit.broadcast(Utils.msg("<green>Starting in " + secs + " second" + (secs == 1 ? "" : "s")));
				Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
			}, (5 - i) * 20L);
		}
		Utils.scheduleTask(() -> {
			Bukkit.broadcast(Utils.msg("<green>Run started"));
			startSection(world, section);
		}, 100);
	}

	/** Runs the section-specific start actions. Callers own the pre-run delay/countdown; this is just the start. */
	private static void startSection(World world, String section) {
		switch(section) {
			case "all", "clear" -> {
				Utils.markPhaseStart();
				Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 1.0F);
				// Arm the one-shot Blood-Room detection at clear-tick 0; the Watcher spawns the first tick a
				// player enters the bounds (continuation intent + Maxor handoff were armed in TAS.runTAS).
				Watcher.INSTANCE.beginDetection(world);
				openFirstDoor();
			}
			case "boss" -> Maxor.maxorInstructions(world, true);
			case "maxor" -> Maxor.maxorInstructions(world, false);
			case "storm" -> Storm.stormInstructions(world, false);
			case "goldor" -> {
				Utils.runCommand("fill 62 136 142 58 133 142 minecraft:lever[face=wall,facing=north,powered=true]");
				Utils.runCommand("fill 58 136 143 62 133 143 minecraft:redstone_lamp[lit=true]");
				Goldor.goldorInstructions(world, false);
			}
			case "necron" -> {
				Utils.scheduleTask(() -> Utils.runCommand("fill 53 63 113 55 63 115 minecraft:air"), 1);
				Necron.necronInstructions(world, false);
			}
			case "witherking" -> WitherKing.witherKingInstructions(world, true);
		}
	}

	public static void serverSetup(World world) {
		CustomItems.flushStonkRestorations();
		// Replace every superboomed wall / crypt still set to AIR and despawn active crypt mobs, then clear ender pearl
		// cooldowns so a fresh run/setup starts from a clean state.
		CustomItems.flushBlockRestorations();
		for(Player pl : Bukkit.getOnlinePlayers()) {
			pl.setCooldown(Material.ENDER_PEARL, 0);
		}
		spawnMinibosses(world);
		Utils.runCommand("fill -122 69 -170 -120 72 -168 minecraft:chiseled_stone_bricks");
		Utils.runCommand("fill -69 82 -155 -69 74 -151 minecraft:iron_bars replace minecraft:air");
		Utils.runCommand("fill -120 69 -106 -122 72 -104 minecraft:coal_block");
		Utils.runCommand("fill -122 69 -74 -120 72 -72 minecraft:red_terracotta");
		Utils.runCommand("setblock 73 224 73 minecraft:black_stained_glass");
		Utils.runCommand("fill 62 136 142 58 133 142 minecraft:lever[face=wall,facing=north,powered=false]");
		Utils.runCommand("fill 58 136 143 62 133 143 minecraft:redstone_lamp[lit=false]");
		Utils.runCommand("fill 56 121 54 52 115 54 minecraft:gold_block");
		Utils.runCommand("clone 70 -5 120 38 -1 99 38 59 99");
		// Restore every boss-to-boss transition wall opened during a previous run.
		instructions.bosses.BossTransition.resetAll();
//		Utils.runCommand("setblock 54 64 79 minecraft:jungle_planks");
//		Utils.runCommand("setblock 54 64 80 minecraft:jungle_stairs");
//		Utils.runCommand("setblock 54 63 79 minecraft:stone_brick_slab[type=top]");
		CustomBossBar.forceCleanup();
		Watcher.forceCleanup();
		WitherKing.forceCleanup(world);
		Storm.INSTANCE.cleanupMobs();
		instructions.bosses.WitherSpawn.restoreStormPillars(world);
		Goldor.resetS3Device(world);
		Goldor.resetSectionLevers(world);
		if(GoldorListener.INSTANCE != null) {
			GoldorListener.INSTANCE.resetSharpShooter(world);
			GoldorListener.INSTANCE.resetSimon();
		}
//		turnArrow(world, false);
	}

	private static void spawnMinibosses(World world) {
		resetClearState(); // fresh run — keys not yet obtained, doors closed
		for(Zombie zombie : archaeologists) {
			if(zombie != null) {
				zombie.remove();
			}
		}

		Location[] locations = {new Location(world, -120.5, 69, -152.5, 90f, 0f), // Red Blue (I)
				new Location(world, -120.5, 67, -88.5, -90f, 0f), // Deathmite (II)
				new Location(world, -24.5, 69, -184.5, 0f, 0f), // Well (III)
				new Location(world, -56.5, 69, -48.5, -90f, 0f), // Gravel (III)
				new Location(world, -168.5, 65.0625, -104.5f, -180, 0f), // Museum (III)
				new Location(world, -35.5, 69, -120.5, 90f, 0f), // Dino Dig Site (IV)
				new Location(world, -184.5, 69, -28.5, -90f, 0f), // Market (IV)
				new Location(world, -152.5, 69, -24.5, -90f, 0f), // Hallway (V)
		};

		double[] healthValues = {15, 16, 17, 17, 17, 18, 18, 19};

		for(int i = 0; i < locations.length; i++) {
			Zombie zombie = (Zombie) world.spawnEntity(locations[i], EntityType.ZOMBIE);
			zombie.customName(Utils.msg("<light_purple><bold>Angry Archaeologist </bold><yellow>" + ((int) healthValues[i] * 2) + "M<red>❤"));
			zombie.setCustomNameVisible(true);
			zombie.setAI(false);
			zombie.setSilent(true);
			zombie.setAdult();
			zombie.setPersistent(true);
			zombie.setRemoveWhenFarAway(false);
			Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
			Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
			Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(healthValues[i]);
			zombie.setHealth(healthValues[i]);

			assert zombie.getEquipment() != null;
			zombie.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
			zombie.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
			zombie.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
			zombie.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
			zombie.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));

			// The first archaeologist (-120.5,69,-152.5) drops the Wither Key; the next one the Blood Key.
			if(i == 0) zombie.addScoreboardTag("WitherKeyMob");
			else if(i == 1) zombie.addScoreboardTag("BloodKeyMob");

			archaeologists[i] = zombie;
		}

		if(yellowShadowAssassin != null) {
			yellowShadowAssassin.remove();
		}

		yellowShadowAssassin = (Zombie) world.spawnEntity(new Location(world, -184.5, 69, -184.5, 0f, 0f), EntityType.ZOMBIE);
		yellowShadowAssassin.customName(Utils.msg("<light_purple><bold>Shadow Assassin </bold><yellow>60M<red>❤"));
		yellowShadowAssassin.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0));
		yellowShadowAssassin.setCustomNameVisible(true);
		yellowShadowAssassin.setAI(false);
		yellowShadowAssassin.setSilent(true);
		yellowShadowAssassin.setAdult();
		yellowShadowAssassin.setPersistent(true);
		yellowShadowAssassin.setRemoveWhenFarAway(false);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
		Objects.requireNonNull(yellowShadowAssassin.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(30);
		yellowShadowAssassin.setHealth(30);

		ItemStack boots = Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.PURPLE, Utils.mmLegacy("<light_purple>Shadow Assassin Boots"));
		assert yellowShadowAssassin.getEquipment() != null;
		yellowShadowAssassin.getEquipment().setBoots(boots);
		yellowShadowAssassin.getEquipment().setLeggings(new ItemStack(Material.AIR));
		yellowShadowAssassin.getEquipment().setChestplate(new ItemStack(Material.AIR));
		yellowShadowAssassin.getEquipment().setHelmet(new ItemStack(Material.AIR));
		yellowShadowAssassin.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
	}

	public static void openFirstDoor() {
		Utils.runCommand("fill -122 69 -170 -120 72 -168 minecraft:glass");
		Utils.scheduleTask(() -> Utils.runCommand("fill -122 69 -170 -120 72 -168 minecraft:air"), 20);
	}

	public static void openWitherDoor(Player p) {
		Utils.runCommand("fill -120 69 -106 -122 72 -104 minecraft:glass");
		Utils.scheduleTask(() -> Utils.runCommand("fill -120 69 -106 -122 72 -104 minecraft:air"), 20);
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		Bukkit.broadcast(Utils.msg("<gold><name><green> opened a <dark_gray><bold>WITHER </bold><green>door!", Placeholder.unparsed("name", Utils.getRealName(p))));
	}

	public static void openBloodDoor() {
		Utils.runCommand("fill -122 69 -74 -120 72 -72 minecraft:glass");
		Utils.scheduleTask(() -> Utils.runCommand("fill -122 69 -74 -120 72 -72 minecraft:air"), 20);
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		Utils.playGlobalSound(Sound.ENTITY_GHAST_HURT, 1.0F, 0.5F);
		Bukkit.broadcast(Utils.msg("<red>The <bold>BLOOD DOOR</bold> has been opened!"));
		Bukkit.broadcast(Utils.msg("<dark_purple>A shiver runs down your spine..."));
		// The Watcher encounter begins the moment the Blood Door opens (if it was armed this run and hasn't
		// already spawned from a player walking into the Blood Room).
		Watcher.INSTANCE.startOnBloodDoor();
	}

	public static void openIceFillRewards() {
		Utils.runCommand("fill -69 82 -155 -69 74 -151 minecraft:air replace minecraft:iron_bars");
	}

	public static void playWitherDeathSound(Wither wither) {
		Utils.playGlobalSound(Sound.ENTITY_WITHER_DEATH);
		wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 4);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 10);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 14);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 20);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 24);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 30);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 34);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 40);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 44);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 50);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F), 54);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 60);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 70);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 80);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 90);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 100);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 110);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 120);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 130);
		Utils.scheduleTask(() -> wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F), 140);
		Utils.scheduleTask(wither::remove, 160);
	}

	public static Zombie spawnCryptLurker(Location loc, boolean isPrince) {
		Zombie zombie = (Zombie) Objects.requireNonNull(loc.getWorld()).spawnEntity(loc, EntityType.ZOMBIE);
		zombie.setAdult();
		// Clear random armor and prevent chicken jockey from finalizeSpawn
		assert zombie.getEquipment() != null;
		zombie.getEquipment().setHelmet(null);
		zombie.getEquipment().setChestplate(null);
		zombie.getEquipment().setLeggings(null);
		zombie.getEquipment().setBoots(null);
		if(zombie.isInsideVehicle()) {
			Entity vehicle = zombie.getVehicle();
			zombie.leaveVehicle();
			if(vehicle != null) vehicle.remove();
		}
		zombie.setAI(false);
		zombie.setSilent(true);
		zombie.setPersistent(true);
		zombie.setRemoveWhenFarAway(false);
		zombie.setCustomNameVisible(true);
		Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(1);
		zombie.setHealth(1);
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-30);
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(-20);
		zombie.getEquipment().setItemInMainHand(new ItemStack(Material.BONE));
		String mobName = isPrince ? "Prince" : "Crypt Lurker";
		zombie.customName(Utils.msg("<red>" + mobName + " ❤<yellow>2M"));
		return zombie;
	}

	public static class Quiz {
		private static final Location PARTICLE_START = new Location(null, -24.5, 85.5, -23.0);
		private static final double[][] OPTION_COORDS = {{-19.5, 71.5, -33.5}, {-24.5, 71.5, -30.5}, {-29.5, 71.5, -33.5}};
		private static final String[] OPTION_LABELS = {"ⓐ", "ⓑ", "ⓒ"};
		private static final float[] OPTION_PITCHES = {0.6f, 0.7f, 0.8f};
		private static final String ORUO = "<dark_red>[STATUE] Oruo the Omniscient<white>: ";

		public static void oruoMessage(String message) {
			Bukkit.broadcast(Utils.msg(ORUO + message));
			Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
		}

		private static TextDisplay spawnOption(World world, Location loc, String text) {
			TextDisplay td = (TextDisplay) world.spawnEntity(loc, EntityType.TEXT_DISPLAY);
			td.text(Utils.nameComponent(text));
			td.setAlignment(TextDisplay.TextAlignment.CENTER);
			td.setBillboard(Display.Billboard.CENTER);
			return td;
		}

		private static void spawnParticleTrail(World world, Location start, Location end) {
			int duration = 40;
			double dx = (end.getX() - start.getX()) / duration;
			double dy = (end.getY() - start.getY()) / duration;
			double dz = (end.getZ() - start.getZ()) / duration;
			for(int i = 0; i <= duration; i++) {
				final int tick = i;
				Utils.scheduleTask(() -> world.spawnParticle(Particle.HAPPY_VILLAGER, start.getX() + dx * tick, start.getY() + dy * tick, start.getZ() + dz * tick, 1, 0, 0, 0, 0), i);
			}
		}

		private static void spawnQuestion(int questionNum, String questionText, String[] answers, TextDisplay[] options, Location particleStart, Player player, World world) {
			Bukkit.broadcast(Utils.msg(""));
			Bukkit.broadcast(Utils.msg("<gold>                                <bold>Question #" + questionNum));
			Bukkit.broadcast(Utils.msg("<gold>" + questionText));
			Bukkit.broadcast(Utils.msg(""));
			for(int i = 0; i < 3; i++) {
				Bukkit.broadcast(Utils.msg("<gold>     " + OPTION_LABELS[i] + " <green>" + answers[i]));
			}
			Bukkit.broadcast(Utils.msg(""));
			Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
			for(int i = 0; i < 3; i++) {
				final int idx = i;
				Utils.scheduleTask(() -> spawnParticleTrail(world, particleStart, new Location(world, OPTION_COORDS[idx][0], OPTION_COORDS[idx][1], OPTION_COORDS[idx][2])), idx * 10);
			}
			for(int i = 0; i < 3; i++) {
				final int idx = i;
				Utils.scheduleTask(() -> {
					options[idx] = spawnOption(world, new Location(world, OPTION_COORDS[idx][0], OPTION_COORDS[idx][1], OPTION_COORDS[idx][2]), Utils.mmLegacy("<gold>" + OPTION_LABELS[idx] + " <green>" + answers[idx]));
					Utils.playLocalSound(player, Sound.ENTITY_ITEM_PICKUP, 2.0f, OPTION_PITCHES[idx]);
				}, 40 + idx * 10);
			}
			Utils.scheduleTask(() -> {
				answeredCorrectly(questionNum, options);
				Actions.rightClick(player);
			}, 61);
		}

		private static void answeredCorrectly(int questionNum, TextDisplay[] options) {
			Bukkit.broadcast(Utils.msg(ORUO + "<gold>akc0303 <green>answered <gold>Question #" + questionNum + "<green> correctly!"));
			Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
			Utils.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 0.75f);
			options[0].remove();
			options[1].remove();
			options[2].remove();
		}

		public static void run(Player player, World world) {
			Utils.scheduleTask(() -> {
				Actions.turnHead(player, 0f, 5f);
				Actions.setHotbarSlot(player, 5);
			}, 137);
			Utils.scheduleTask(() -> oruoMessage("Prove your knowledge by answering 3 questions and I shall reward you in ways that transcend time!"), 155);
			Utils.scheduleTask(() -> oruoMessage("Answer incorrectly, and your moment of ineptitude will live on for generations."), 195);
			final TextDisplay[] options = new TextDisplay[3];
			Location particleStart = PARTICLE_START.clone();
			particleStart.setWorld(world);
			Utils.scheduleTask(() -> spawnQuestion(1, "                      How is the run going so far?", new String[]{"Alright", "Trash", "Literally tick-perfect"}, options, particleStart, player, world), 235);
			Utils.scheduleTask(() -> oruoMessage("2 question left... then you will have proven your worth to me!"), 336);
			Utils.scheduleTask(() -> spawnQuestion(2, "Did you know that you can sub scribe to Stradivarius Violin to                         see more content like this?!", new String[]{"Oh wow, I should sub scribe!", "Oh wow, I should sub scribe!!", "Oh wow, I should sub scribe!!!"}, options, particleStart, player, world), 376);
			Utils.scheduleTask(() -> oruoMessage("One more question!"), 477);
			Utils.scheduleTask(() -> spawnQuestion(3, "                             Is akc0303 bald?", new String[]{"No", "Yes", "Decline to Answer"}, options, particleStart, player, world), 517);
			Utils.scheduleTask(() -> {
				Bukkit.broadcast(Utils.msg("<dark_green>Archer: Quiz Cleared"));
				Bukkit.broadcast(Utils.msg("<dark_green>Archer: Clear Finished in 578 Ticks (28.90 seconds)"));
			}, 578);
			Utils.scheduleTask(() -> oruoMessage("I bestow upon you all the power of a hundred years!"), 598);
			Utils.scheduleTask(() -> Utils.broadcastBlessing(player, Utils.BlessingType.TIME, 5), 618);
		}
	}

	public static class IceFill {
		private static BukkitTask iceFillTask;
		private static final Set<Block> frozenBlocks = new HashSet<>();

		private static void playIceFillSounds(int level, Player player) {
			switch(level) {
				case 1 -> {
					Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.189446f);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.3352f), 5);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.41436f), 10);
				}
				case 2 -> {
					Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.4987f);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.5878f), 5);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.6821f), 10);
				}
				case 3 -> {
					Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.782f);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 1.888f), 5);
					Utils.scheduleTask(() -> Utils.playLocalSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 2.0f), 10);
				}
			}
		}

		private static void startIceFillTask(Player player) {
			if(iceFillTask != null) {
				iceFillTask.cancel();
			}

			frozenBlocks.clear();

			iceFillTask = new BukkitRunnable() {
				@Override
				public void run() {
					Block below = player.getLocation().subtract(0, 1, 0).getBlock();
					if(below.getType() == Material.ICE) {
						below.setType(Material.PACKED_ICE);
						frozenBlocks.add(below);
						Utils.playGlobalSound(Sound.BLOCK_SNOW_BREAK, 2.0f, 1.0f);
					}
				}
			}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
		}

		public static void stopIceFillTask() {
			if(iceFillTask != null) {
				iceFillTask.cancel();
				iceFillTask = null;
			}

			for(Block block : frozenBlocks) {
				if(block.getType() == Material.PACKED_ICE) {
					block.setType(Material.ICE);
				}
			}
			frozenBlocks.clear();
		}

		public static void run(Player player, World world) {
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 121);
			Utils.scheduleTask(() -> {
				startIceFillTask(player);
				Actions.rightClick(player);
			}, 122);
			Utils.scheduleTask(() -> Actions.rightClick(player), 123);
			Utils.scheduleTask(() -> Actions.rightClick(player), 124);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 125);
			Utils.scheduleTask(() -> Actions.rightClick(player), 126);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 127);
			Utils.scheduleTask(() -> Actions.rightClick(player), 128);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 129);
			Utils.scheduleTask(() -> Actions.rightClick(player), 130);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 131);
			Utils.scheduleTask(() -> {
				Actions.rightClick(player);
				playIceFillSounds(1, player);
			}, 132);
			Utils.scheduleTask(() -> Actions.rightClick(player), 133);
			Utils.scheduleTask(() -> Actions.rightClick(player), 134);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 135);
			Utils.scheduleTask(() -> Actions.rightClick(player), 136);
			Utils.scheduleTask(() -> Actions.rightClick(player), 137);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 138);
			Utils.scheduleTask(() -> Actions.rightClick(player), 139);
			Utils.scheduleTask(() -> Actions.rightClick(player), 140);
			Utils.scheduleTask(() -> Actions.rightClick(player), 141);
			Utils.scheduleTask(() -> Actions.rightClick(player), 142);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 143);
			Utils.scheduleTask(() -> Actions.rightClick(player), 144);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 145);
			Utils.scheduleTask(() -> Actions.rightClick(player), 146);
			Utils.scheduleTask(() -> Actions.rightClick(player), 147);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 148);
			Utils.scheduleTask(() -> Actions.rightClick(player), 149);
			Utils.scheduleTask(() -> Actions.rightClick(player), 150);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 151);
			Utils.scheduleTask(() -> Actions.rightClick(player), 152);
			Utils.scheduleTask(() -> Actions.rightClick(player), 153);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 154);
			Utils.scheduleTask(() -> Actions.rightClick(player), 155);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 156);
			Utils.scheduleTask(() -> Actions.rightClick(player), 157);
			Utils.scheduleTask(() -> Actions.rightClick(player), 158);
			Utils.scheduleTask(() -> Actions.rightClick(player), 159);
			Utils.scheduleTask(() -> Actions.rightClick(player), 160);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 161);
			Utils.scheduleTask(() -> Actions.rightClick(player), 162);
			Utils.scheduleTask(() -> Actions.rightClick(player), 163);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 164);
			Utils.scheduleTask(() -> {
				Actions.rightClick(player);
				playIceFillSounds(2, player);
			}, 165);
			Utils.scheduleTask(() -> Actions.rightClick(player), 166);
			Utils.scheduleTask(() -> Actions.rightClick(player), 167);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 168);
			Utils.scheduleTask(() -> Actions.rightClick(player), 169);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 170);
			Utils.scheduleTask(() -> Actions.rightClick(player), 171);
			Utils.scheduleTask(() -> Actions.rightClick(player), 172);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 173);
			Utils.scheduleTask(() -> Actions.rightClick(player), 174);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 175);
			Utils.scheduleTask(() -> Actions.rightClick(player), 176);
			Utils.scheduleTask(() -> Actions.rightClick(player), 177);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 178);
			Utils.scheduleTask(() -> Actions.rightClick(player), 179);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 180);
			Utils.scheduleTask(() -> Actions.rightClick(player), 181);
			Utils.scheduleTask(() -> Actions.rightClick(player), 182);
			Utils.scheduleTask(() -> Actions.rightClick(player), 183);
			Utils.scheduleTask(() -> Actions.rightClick(player), 184);
			Utils.scheduleTask(() -> Actions.rightClick(player), 185);
			Utils.scheduleTask(() -> Actions.rightClick(player), 186);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 187);
			Utils.scheduleTask(() -> Actions.rightClick(player), 188);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 189);
			Utils.scheduleTask(() -> Actions.rightClick(player), 190);
			Utils.scheduleTask(() -> Actions.rightClick(player), 191);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 192);
			Utils.scheduleTask(() -> Actions.rightClick(player), 193);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 194);
			Utils.scheduleTask(() -> Actions.rightClick(player), 195);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 196);
			Utils.scheduleTask(() -> Actions.rightClick(player), 197);
			Utils.scheduleTask(() -> Actions.rightClick(player), 198);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 199);
			Utils.scheduleTask(() -> Actions.rightClick(player), 200);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 201);
			Utils.scheduleTask(() -> Actions.rightClick(player), 202);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 203);
			Utils.scheduleTask(() -> Actions.rightClick(player), 204);
			Utils.scheduleTask(() -> Actions.rightClick(player), 205);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 206);
			Utils.scheduleTask(() -> Actions.rightClick(player), 207);
			Utils.scheduleTask(() -> Actions.turnHead(player, -90f, 60f), 208);
			Utils.scheduleTask(() -> Actions.rightClick(player), 209);
			Utils.scheduleTask(() -> Actions.rightClick(player), 210);
			Utils.scheduleTask(() -> Actions.turnHead(player, 180f, 60f), 211);
			Utils.scheduleTask(() -> Actions.rightClick(player), 212);
			Utils.scheduleTask(() -> Actions.rightClick(player), 213);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 214);
			Utils.scheduleTask(() -> Actions.rightClick(player), 215);
			Utils.scheduleTask(() -> Actions.rightClick(player), 216);
			Utils.scheduleTask(() -> Actions.rightClick(player), 217);
			Utils.scheduleTask(() -> Actions.rightClick(player), 218);
			Utils.scheduleTask(() -> Actions.rightClick(player), 219);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 220);
			Utils.scheduleTask(() -> Actions.rightClick(player), 221);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 222);
			Utils.scheduleTask(() -> Actions.rightClick(player), 223);
			Utils.scheduleTask(() -> Actions.turnHead(player, 0f, 60f), 224);
			Utils.scheduleTask(() -> Actions.rightClick(player), 225);
			Utils.scheduleTask(() -> Actions.rightClick(player), 226);
			Utils.scheduleTask(() -> Actions.turnHead(player, 90f, 60f), 227);
			Utils.scheduleTask(() -> {
				Actions.rightClick(player);
				playIceFillSounds(3, player);
				Server.openIceFillRewards();
				Bukkit.broadcast(Utils.msg("<red>Berserk: Ice Fill Cleared"));
			}, 228);
			Utils.scheduleTask(() -> Actions.move(player, "WP", 1), 229);
			Utils.scheduleTask(() -> Actions.turnHead(player, 68f, -33f), 230);
			Utils.scheduleTask(() -> {
				Actions.leftClick(player);
				Utils.broadcastBlessing(player, Utils.BlessingType.POWER, 5);
				Utils.playSecretFoundSound(player, Utils.SecretType.BLESSING_CHEST);
			}, 248);
			Utils.scheduleTask(() -> Actions.turnHead(player, 112f, -33f), 249);
			Utils.scheduleTask(() -> {
				Actions.leftClick(player);
				Utils.broadcastBlessing(player, Utils.BlessingType.POWER, 5);
				Utils.playSecretFoundSound(player, Utils.SecretType.BLESSING_CHEST);
				Bukkit.broadcast(Utils.msg("<red>Berserk: Clear Finished in 250 Ticks (12.50 seconds)"));
			}, 250);
		}
	}
}