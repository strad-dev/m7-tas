package instructions.bosses;

import instructions.Server;
import instructions.players.Tank;
import listeners.CustomItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.FakePlayerInventory;
import plugin.M7tas;
import plugin.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("DataFlowIssue")
public class Maxor {
	private static Wither maxor;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;
	private static BossBar maxorBossBar;
	private static BukkitTask bossBarUpdateTask;
	// Top spawn crystals — pickupable. Nulled on pickUp.
	private static EnderCrystal topLeftCrystal;
	private static EnderCrystal topRightCrystal;
	// Plate-placed crystals — NOT pickupable. Committed once placed.
	private static EnderCrystal plateLeftCrystal;
	private static EnderCrystal plateRightCrystal;
	private static final Random random = new Random();
	private static final String[] laserMessage = {"YOU TRICKED ME!", "THAT BEAM!  IT HURTS!  IT HURTS!"};
	private static final WitherSkeleton[] miners = new WitherSkeleton[10];

	private static final int PRE_MAXOR_TICKS = 742;
	private static int maxorTick;
	private static BukkitTask tickerTask;

	private static final String ENERGY_CRYSTAL_ID = "skyblock/game/energy_crystal";
	private static final String ENERGY_CRYSTAL_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Energy Crystal" + ChatColor.GOLD + ChatColor.BOLD + " ﴿";
	private static final Map<UUID, ItemStack> previousSlot8 = new HashMap<>();

	// Laser/stun cycle state.
	private static final double LASER_CENTER_X = 73.5;
	private static final double LASER_CENTER_Z = 73.5;
	private static final double LASER_RADIUS_SQ = 2.5 * 2.5;
	private static final int CHARGE_DELAY_TICKS = 30;
	private static final int STUN_COOLDOWN_TICKS = 200;
	private static final int PLATE_GATE_TICKS = 160;
	private static final int CRYSTAL_RESPAWN_DELAY_TICKS = 40;
	private static BukkitTask laserScanTask;
	private static BukkitTask stunEnrageTask;
	private static boolean platesActive;
	private static boolean stunCooldownActive;
	private static boolean inStun;
	private static boolean dying;
	private static double stunDamageDealt;
	private static final java.util.List<Runnable> pendingPlateChecks = new java.util.ArrayList<>();

	public static void maxorInstructions(World temp, boolean doContinue) {
		world = temp;

		if(maxor != null) {
			maxor.remove();
		}

		if(maxorBossBar != null) {
			maxorBossBar.removeAll();
			maxorBossBar = null;
		}

		if(bossBarUpdateTask != null) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}

		// Reset tick counter and start ticker.
		maxorTick = 0;
		if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
		tickerTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), () -> maxorTick++, 0L, 1L);

		// Reset laser/stun state for this fight so subsequent TASes work.
		cancelLaserScan();
		cancelStunEnrageTask();
		CustomBossBar.removeStunIndicator();
		inStun = false;
		dying = false;
		stunDamageDealt = 0;
		stunCooldownActive = false;
		platesActive = false;
		pendingPlateChecks.clear();
		Utils.scheduleTask(() -> {
			platesActive = true;
			java.util.List<Runnable> snapshot = new java.util.ArrayList<>(pendingPlateChecks);
			pendingPlateChecks.clear();
			for(Runnable r : snapshot) r.run();
		}, PLATE_GATE_TICKS);

		maxor = (Wither) world.spawnEntity(new Location(world, 73.5, 226, 53.5, 0f, 0f), EntityType.WITHER);
		maxor.setAI(false);
		maxor.setSilent(true);
		maxor.setPersistent(true);
		maxor.setRemoveWhenFarAway(false);
		maxor.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Maxor" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.YELLOW + "800M" + ChatColor.RED + "❤");
		maxor.setCustomNameVisible(true);
		maxor.getAttribute(Attribute.MAX_HEALTH).setBaseValue(300);
		maxor.getAttribute(Attribute.ARMOR).setBaseValue(-30);
		maxor.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(-20);
		maxor.setHealth(300);
		maxor.addScoreboardTag("TASWither");
		maxor.addScoreboardTag("TASMaxor");
		maxor.removeScoreboardTag("TASDying");
		WitherActions.setWitherArmor(maxor, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(maxor, "Maxor"), 1);

		resetCrystals();

		sendChatMessage("WELL WELL WELL, LOOK WHO'S HERE!");
		Utils.scheduleTask(() -> sendChatMessage("I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU."), 60);
		Utils.scheduleTask(() -> sendChatMessage("DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE."), 120);
		Utils.scheduleTask(() -> {
			WitherActions.setWitherAggro(maxor, Tank.get(), 3.0, 1.0);
			spawnMiners();
			Utils.playGlobalSound(Sound.ENTITY_WITHER_SPAWN);
			Utils.playGlobalSound(Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 2.0F);
		}, 160);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.GREEN + "The Energy Laser is charging up!");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:red_stained_glass");
//		}, 190);
//		Utils.scheduleTask(() -> {
//			sendChatMessage(laserMessage[random.nextInt(2)]);
//			WitherActions.setWitherArmor(maxor, false);
//			CustomBossBar.spawnAnimatedStunnedIndicator(maxor, 160); "
//		}, 198);
//		Utils.scheduleTask(() -> maxor.setHealth(124), 230);
//		Utils.scheduleTask(() -> {
//			Maxor.spawnCrystals();
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
//		}, 238);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.GREEN + "The Energy Laser is charging up!");
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:red_stained_glass");
//		}, 292);
//		Utils.scheduleTask(() -> {
//			WitherActions.setWitherArmor(maxor, true);
//			Bukkit.broadcastMessage(ChatColor.RED + "⚠ Maxor is enraged! ⚠");
//			for(Player player : Bukkit.getOnlinePlayers()) {
//				player.sendTitle("", ChatColor.RED + "⚠ Maxor is enraged! ⚠", 0, 40, 0);
//			}
//			Actions.turnHead(maxor, 0f, 30f);
//			Actions.forceMove(maxor, new Vector(0, 0, 0.5), 4);
//			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
//		}, 358);
//		Utils.scheduleTask(() -> {
//			sendChatMessage(laserMessage[random.nextInt(2)]);
//			WitherActions.setWitherArmor(maxor, false);
//			CustomBossBar.spawnAnimatedStunnedIndicator(maxor, 2);
//		}, 398);
//		Utils.scheduleTask(() -> {
//			sendChatMessage("I’M TOO YOUNG TO DIE AGAIN!");
//			Server.playWitherDeathSound(maxor);
//			Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor killed in 399 ticks (19.95 seconds) | Overall: 1 426 ticks (71.30 seconds)");
//		}, 399);
//		Utils.scheduleTask(() -> {
//			Maxor.spawnCrystals();
//			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
//		}, 438);
//		Utils.scheduleTask(() -> sendChatMessage("I’LL MAKE YOU REMEMBER MY DEATH!"), 459);
//		Utils.scheduleTask(() -> {
//			Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor finished in 499 ticks (24.95 seconds) | Overall: 1 526 ticks (76.30 seconds)");
//			if(doContinue) {
//				Storm.stormInstructions(world, true);
//			}
//		}, 499);
	}

	public static ItemStack getEnergyCrystalItem() {
		return FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, ChatColor.RED + "Energy Crystal", ENERGY_CRYSTAL_ID);
	}

	public static boolean notEnergyCrystal(Entity e) {
		// Only the top spawn crystals can be picked up — plate-placed ones are committed.
		return !(e instanceof EnderCrystal) || (!e.equals(topLeftCrystal) && !e.equals(topRightCrystal));
	}

	public static void resetCrystals() {
		if(world == null) world = Bukkit.getWorlds().getFirst();

		if(topLeftCrystal != null) topLeftCrystal.remove();
		if(topRightCrystal != null) topRightCrystal.remove();
		if(plateLeftCrystal != null) { plateLeftCrystal.remove(); plateLeftCrystal = null; }
		if(plateRightCrystal != null) { plateRightCrystal.remove(); plateRightCrystal = null; }

		topLeftCrystal = spawnEnergyCrystal(new Location(world, 82.5, 238.48, 50.5));
		topRightCrystal = spawnEnergyCrystal(new Location(world, 64.5, 238.48, 50.5));
	}

	public static void pickUp(Player p, EnderCrystal crystal) {
		if(notEnergyCrystal(crystal)) return;

		// Already holding an Energy Crystal anywhere in inventory? Reject.
		for(ItemStack item : p.getInventory().getContents()) {
			if(item != null && ENERGY_CRYSTAL_ID.equals(CustomItems.getID(item))) return;
		}

		ItemStack prev = p.getInventory().getItem(8);
		previousSlot8.put(p.getUniqueId(), prev == null ? null : prev.clone());
		p.getInventory().setItem(8, getEnergyCrystalItem());

		crystal.remove();
		if(crystal.equals(topLeftCrystal)) topLeftCrystal = null;
		else if(crystal.equals(topRightCrystal)) topRightCrystal = null;

		Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!\n" + formatTick(maxorTick));

		// If the player is already standing on a plate, place immediately —
		// no PHYSICAL interact fires until they re-step on the plate.
		Location feet = p.getLocation();
		int fx = feet.getBlockX(), fy = feet.getBlockY(), fz = feet.getBlockZ();
		if(fy == 224 && fz == 41 && (fx == 52 || fx == 94)) {
			onPlateStep(p, feet);
		}
	}

	/**
	 * Plate-step entry point. If plates are active, place immediately.
	 * Otherwise schedule a recheck for when the gate opens — if the player is still
	 * standing on the same plate at that moment (and still holds a crystal), place then.
	 */
	public static void onPlateStep(Player p, Location plate) {
		if(platesActive) {
			placeAtPlate(p, plate);
			return;
		}
		// Gate not open yet — queue a recheck. The gate-open task drains and runs these.
		final int px = plate.getBlockX(), py = plate.getBlockY(), pz = plate.getBlockZ();
		pendingPlateChecks.add(() -> {
			Location feet = p.getLocation();
			if(feet.getBlockX() == px && feet.getBlockY() == py && feet.getBlockZ() == pz) {
				placeAtPlate(p, plate);
			}
		});
	}

	public static void placeAtPlate(Player p, Location plate) {
		ItemStack slot8 = p.getInventory().getItem(8);
		if(slot8 == null || !ENERGY_CRYSTAL_ID.equals(CustomItems.getID(slot8))) return;

		if(world == null) world = Bukkit.getWorlds().getFirst();
		// Pressure-plate placement is disabled until 160 ticks into the fight.
		if(!platesActive) return;
		int px = plate.getBlockX(), py = plate.getBlockY(), pz = plate.getBlockZ();

		if(px == 94 && py == 224 && pz == 41) {
			if(plateLeftCrystal != null) return;
			plateLeftCrystal = spawnEnergyCrystal(new Location(world, 94.5, 224.48, 41.5));
		} else if(px == 52 && py == 224 && pz == 41) {
			if(plateRightCrystal != null) return;
			plateRightCrystal = spawnEnergyCrystal(new Location(world, 52.5, 224.48, 41.5));
		} else {
			return;
		}

		ItemStack restore = previousSlot8.remove(p.getUniqueId());
		p.getInventory().setItem(8, restore != null ? restore : FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Menu (Click)", ""));

		boolean bothPlaced = plateLeftCrystal != null && plateRightCrystal != null;
		int placed = bothPlaced ? 2 : 1;
		ChatColor placedColor = bothPlaced ? ChatColor.GREEN : ChatColor.RED;
		String activeMsg = placedColor + String.valueOf(placed) + ChatColor.GREEN + "/2 Energy Crystals are now active!\n" + formatTick(maxorTick);
		Bukkit.broadcastMessage(activeMsg);
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.sendTitle("", activeMsg.split("\n")[0], 0, 40, 0);
		}

		if(bothPlaced) {
			beginLaserCharge();
		}
	}

	private static void beginLaserCharge() {
		cancelLaserScan();
		Utils.scheduleTask(() -> {
			if(maxor == null || maxor.isDead()) return;
			if(plateLeftCrystal == null || plateRightCrystal == null) return;
			String chargeMsg = ChatColor.GREEN + "The Energy Laser is charging up!\n" + formatTick(maxorTick);
			Bukkit.broadcastMessage(chargeMsg);
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle("", chargeMsg.split("\n")[0], 0, 40, 0);
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:red_stained_glass");
			startLaserScan();
		}, CHARGE_DELAY_TICKS);
	}

	private static void startLaserScan() {
		cancelLaserScan();
		laserScanTask = new BukkitRunnable() {
			@Override
			public void run() {
				if(maxor == null || maxor.isDead()) {
					cancel();
					laserScanTask = null;
					return;
				}
				if(stunCooldownActive) return;

				double dx = maxor.getLocation().getX() - LASER_CENTER_X;
				double dz = maxor.getLocation().getZ() - LASER_CENTER_Z;
				if(dx * dx + dz * dz <= LASER_RADIUS_SQ) {
					triggerStun();
					cancel();
					laserScanTask = null;
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private static void cancelLaserScan() {
		if(laserScanTask != null && !laserScanTask.isCancelled()) {
			laserScanTask.cancel();
		}
		laserScanTask = null;
	}

	private static void triggerStun() {
		double maxHp = maxor.getAttribute(Attribute.MAX_HEALTH).getValue();
		double laserDmg = maxHp * 0.05;
		double currentHp = maxor.getHealth();
		double onePercent = maxHp * 0.01;

		// Killing-blow path: laser would drop Maxor to/below 0 HP — clamp to 1% + death sequence.
		if(laserDmg >= currentHp) {
			WitherActions.clearWitherAggro(maxor);
			WitherActions.setWitherArmor(maxor, false);
			sendChatMessage(laserMessage[random.nextInt(laserMessage.length)]);
			maxor.setHealth(onePercent);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_HURT);
			enterDyingState();
			return;
		}

		stunCooldownActive = true;
		Utils.scheduleTask(() -> stunCooldownActive = false, STUN_COOLDOWN_TICKS);
		inStun = true;
		stunDamageDealt = 0;

		WitherActions.clearWitherAggro(maxor);
		WitherActions.setWitherArmor(maxor, false);
		sendChatMessage(laserMessage[random.nextInt(laserMessage.length)]);
		Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor stunned in " + formatTick(maxorTick));

		// Laser hit: 5% max HP damage + wither hurt sound. Bypasses the damage event
		// (no event recursion) and counts toward the stun's 75% damage cap.
		maxor.setHealth(Math.max(0.0, currentHp - laserDmg));
		stunDamageDealt += laserDmg;
		Utils.changeName(maxor);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_HURT);

		CustomBossBar.spawnAnimatedStunnedIndicator(maxor, Integer.MAX_VALUE);

		Utils.scheduleTask(() -> {
			resetCrystals();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
		}, CRYSTAL_RESPAWN_DELAY_TICKS);

		// Auto-enrage 160 ticks after the stun, regardless of damage taken.
		cancelStunEnrageTask();
		stunEnrageTask = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), Maxor::enrageMaxor, 160L);
	}

	private static void enterDyingState() {
		dying = true;
		maxor.addScoreboardTag("TASDying");
		cancelStunEnrageTask();
		cancelLaserScan();
		inStun = false;
		CustomBossBar.removeStunIndicator();
		// Pin internal HP to 1 — display already shows "1" via the TASDying tag in formatHealthM.
		// Deferred 1 tick so vanilla's post-event setHealth doesn't overwrite us.
		Utils.scheduleTask(() -> { if(maxor != null && maxor.isValid()) maxor.setHealth(1.0); }, 1);
		Utils.changeName(maxor);
		playDeathDialogue();
	}

	private static void cancelStunEnrageTask() {
		if(stunEnrageTask != null && !stunEnrageTask.isCancelled()) {
			stunEnrageTask.cancel();
		}
		stunEnrageTask = null;
	}

	private static void enrageMaxor() {
		if(!inStun) return;
		inStun = false;
		cancelStunEnrageTask();

		WitherActions.setWitherArmor(maxor, true);
		Bukkit.broadcastMessage(ChatColor.RED + "⚠ Maxor is Enraged ⚠\n" + formatTick(maxorTick));
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.sendTitle("", ChatColor.RED + "⚠ Maxor is Enraged ⚠", 0, 40, 0);
		}
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
		CustomBossBar.removeStunIndicator();
		WitherActions.setWitherAggro(maxor, Tank.get(), 3.0, 1.0);
	}

	/**
	 * Damage interceptor for Maxor. Hook from a Bukkit listener.
	 * - If a hit would kill him, clamp it to leave him on 1% HP and play the death dialogue.
	 * - Otherwise during a stun, clamp cumulative damage to 75% of max HP and trigger enrage.
	 */
	public static void handleDamage(EntityDamageEvent e) {
		if(maxor == null || !maxor.equals(e.getEntity())) return;
		if(e.isCancelled()) return;

		// Dying = completely immune. Cancel before any damage logic.
		if(dying) {
			e.setCancelled(true);
			return;
		}

		double finalDmg = e.getFinalDamage();
		if(finalDmg <= 0) return;

		double currentHp = maxor.getHealth();
		double maxHp = maxor.getAttribute(Attribute.MAX_HEALTH).getValue();
		double onePercent = maxHp * 0.01;

		// Apply stun cap FIRST so that subsequent killing-blow check sees the already-capped value.
		// Otherwise a single huge hit during stun bypasses the cap by clamping straight to "1% HP" via the kill clamp.
		double cappedDmg = finalDmg;
		boolean willEnrage = false;
		if(inStun) {
			double damageCap = maxHp * 0.75;
			double remaining = Math.max(0, damageCap - stunDamageDealt);
			if(cappedDmg >= remaining) {
				cappedDmg = remaining;
				willEnrage = true;
			}
		}

		// Killing-blow check on the (possibly cap-clamped) damage.
		boolean willDie = false;
		if(cappedDmg >= currentHp) {
			cappedDmg = Math.max(0, currentHp - onePercent);
			willDie = true;
			willEnrage = false; // dying overrides — no enrage
		}

		if(cappedDmg < finalDmg) {
			scaleEventDamage(e, finalDmg, cappedDmg);
		}

		if(willDie) {
			enterDyingState();
		} else {
			if(inStun) stunDamageDealt = Math.min(maxHp * 0.75, stunDamageDealt + cappedDmg);
			if(willEnrage) Bukkit.getScheduler().runTask(M7tas.getInstance(), Maxor::enrageMaxor);
		}
	}

	private static void scaleEventDamage(EntityDamageEvent e, double currentFinal, double targetFinal) {
		if(currentFinal <= 0) return;
		double scale = Math.max(0, targetFinal) / currentFinal;
		e.setDamage(e.getDamage() * scale);
	}

	private static void playDeathDialogue() {
		// Bypass sendChatMessage so we don't emit ENTITY_WITHER_AMBIENT — the death noise
		// is the only sound permitted while dying.
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] Maxor" + ChatColor.RED + ": I'M TOO YOUNG TO DIE AGAIN!");
		Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor killed in " + formatTick(maxorTick));
		// Inlined Server.playWitherDeathSound — sounds + remove at +160.
		Utils.playGlobalSound(Sound.ENTITY_WITHER_DEATH);
		maxor.getWorld().playSound(maxor.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F);
		int[] hurtTicks = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140};
		int[] shootTicks = {4, 14, 24, 34, 44, 54};
		for(int t : hurtTicks) Utils.scheduleTask(() -> { if(maxor.isValid()) maxor.getWorld().playSound(maxor.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0F, 1.0F); }, t);
		for(int t : shootTicks) Utils.scheduleTask(() -> { if(maxor.isValid()) maxor.getWorld().playSound(maxor.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0F, 1.0F); }, t);
		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] Maxor" + ChatColor.RED + ": I'LL MAKE YOU REMEMBER MY DEATH!"), 60);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor finished in " + formatTick(maxorTick));
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
		}, 100);
		Utils.scheduleTask(() -> { if(maxor != null && maxor.isValid()) maxor.remove(); }, 160);
	}

	public static boolean isDyingWither(Wither w) {
		return dying && w != null && w.equals(maxor);
	}

	private static EnderCrystal spawnEnergyCrystal(Location loc) {
		EnderCrystal c = (EnderCrystal) world.spawnEntity(loc, EntityType.END_CRYSTAL);
		c.setCustomName(ENERGY_CRYSTAL_NAME);
		c.setCustomNameVisible(false);
		return c;
	}

	private static String formatTick(int tick) {
		int overall = tick + PRE_MAXOR_TICKS;
		return ChatColor.GREEN + String.format("%s ticks (%.2f seconds) | Overall: %s ticks (%.2f seconds)",
				formatWithSpaces(tick), tick / 20.0, formatWithSpaces(overall), overall / 20.0);
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

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] Maxor" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}

	private static void spawnMiners() {
		// Remove any old Wither Skeletons
		for(WitherSkeleton witherSkeleton : miners) {
			if(witherSkeleton != null && witherSkeleton.isValid()) {
				witherSkeleton.remove();
			}
		}

		for(int i = 0; i < 10; i++) {
			// Random location within 3 blocks of center (73.5, 225, 73.5)
			double x = 73.5 + (random.nextDouble() * 6 - 3); // -3 to +3
			double z = 73.5 + (random.nextDouble() * 6 - 3); // -3 to +3
			Location spawnLoc = new Location(world, x, 225, z);

			WitherSkeleton miner = (WitherSkeleton) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);

			// Set health to 4 HP
			miner.getAttribute(Attribute.MAX_HEALTH).setBaseValue(4.0);
			miner.setHealth(4.0);
			miner.getAttribute(Attribute.ARMOR).setBaseValue(-30);
			miner.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(-20);
			miner.setAI(false);

			// Give stone pickaxe
			miner.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_PICKAXE));

			// Optional: Set custom name
			miner.setCustomName("Wither Miner " + ChatColor.YELLOW + "6M" + ChatColor.RED + "❤");
			miner.setCustomNameVisible(true);

			// Store in array
			miners[i] = miner;
		}
	}
}
