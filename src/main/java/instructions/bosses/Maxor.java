package instructions.bosses;

import instructions.Server;
import instructions.players.Tank;
import listeners.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.FakePlayerInventory;
import plugin.M7tas;
import plugin.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("DataFlowIssue")
public final class Maxor extends WitherLord {
	public static final Maxor INSTANCE = new Maxor();

	private static final int PRE_MAXOR_TICKS = 742;
	private static final String ENERGY_CRYSTAL_ID = "skyblock/game/energy_crystal";
	private static final String ENERGY_CRYSTAL_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Energy Crystal" + ChatColor.GOLD + ChatColor.BOLD + " ﴿";
	private static final String[] LASER_MESSAGE = {"YOU TRICKED ME!", "THAT BEAM!  IT HURTS!  IT HURTS!"};

	// Laser/stun cycle constants.
	private static final double LASER_CENTER_X = 73.5;
	private static final double LASER_CENTER_Z = 73.5;
	private static final double LASER_RADIUS_SQ = 2.5 * 2.5;
	private static final int CHARGE_DELAY_TICKS = 30;
	private static final int STUN_COOLDOWN_TICKS = 200;
	private static final int PLATE_GATE_TICKS = 160;
	private static final int CRYSTAL_RESPAWN_DELAY_TICKS = 40;

	private final Random random = new Random();

	// Top spawn crystals — pickupable. Nulled on pickUp.
	private EnderCrystal topLeftCrystal;
	private EnderCrystal topRightCrystal;
	// Plate-placed crystals — NOT pickupable. Committed once placed.
	private EnderCrystal plateLeftCrystal;
	private EnderCrystal plateRightCrystal;
	private final Map<UUID, ItemStack> previousSlot8 = new HashMap<>();
	private final WitherSkeleton[] miners = new WitherSkeleton[10];

	// Laser/stun cycle state.
	private BukkitTask laserScanTask;
	private BukkitTask stunEnrageTask;
	private boolean platesActive;
	private boolean stunCooldownActive;
	private boolean inStun;
	private double stunDamageDealt;
	private final List<Runnable> pendingPlateChecks = new ArrayList<>();

	private Maxor() {
		register(this);
	}

	/** Static facade for /tas, Watcher, and the boss-chain. */
	public static void maxorInstructions(World world, boolean doContinue) {
		INSTANCE.start(world, doContinue);
	}

	@Override protected String name() { return "Maxor"; }
	@Override protected String displayName() { return "Maxor"; }
	@Override protected Location spawnLocation() { return new Location(world, 73.5, 226, 53.5, 0f, 0f); }
	@Override protected double maxHealth() { return 300; }
	@Override protected String displayHealth() { return "800M"; }
	@Override protected int previousTicks() { return PRE_MAXOR_TICKS; }

	@Override
	protected void resetState() {
		cancelLaserScan();
		cancelStunEnrageTask();
		CustomBossBar.removeStunIndicator();
		inStun = false;
		stunDamageDealt = 0;
		stunCooldownActive = false;
		platesActive = false;
		pendingPlateChecks.clear();
		// EnderCrystal handles cleared by resetCrystals() inside onStart.
	}

	@Override
	protected void onStart() {
		Utils.scheduleTask(() -> {
			platesActive = true;
			List<Runnable> snapshot = new ArrayList<>(pendingPlateChecks);
			pendingPlateChecks.clear();
			for(Runnable r : snapshot) r.run();
		}, PLATE_GATE_TICKS);

		resetCrystals();

		sendChatMessage("WELL WELL WELL, LOOK WHO'S HERE!");
		Utils.scheduleTask(() -> sendChatMessage("I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU."), 60);
		Utils.scheduleTask(() -> sendChatMessage("DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE."), 120);
		Utils.scheduleTask(() -> {
			setAggro(Tank.get(), 3.0, 1.0);
			spawnMiners();
			Utils.playGlobalSound(Sound.ENTITY_WITHER_SPAWN);
			Utils.playGlobalSound(Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 2.0F);
		}, 160);
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			Storm.stormInstructions(world, doContinue);
		}
	}

	public static ItemStack getEnergyCrystalItem() {
		return FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, ChatColor.RED + "Energy Crystal", ENERGY_CRYSTAL_ID);
	}

	public boolean notEnergyCrystal(Entity e) {
		// Only the top spawn crystals can be picked up — plate-placed ones are committed.
		return !(e instanceof EnderCrystal) || (!e.equals(topLeftCrystal) && !e.equals(topRightCrystal));
	}

	public void resetCrystals() {
		if(world == null) world = Bukkit.getWorlds().getFirst();

		if(topLeftCrystal != null) topLeftCrystal.remove();
		if(topRightCrystal != null) topRightCrystal.remove();
		if(plateLeftCrystal != null) { plateLeftCrystal.remove(); plateLeftCrystal = null; }
		if(plateRightCrystal != null) { plateRightCrystal.remove(); plateRightCrystal = null; }

		topLeftCrystal = spawnEnergyCrystal(new Location(world, 82.5, 238.48, 50.5));
		topRightCrystal = spawnEnergyCrystal(new Location(world, 64.5, 238.48, 50.5));
	}

	public void pickUp(Player p, EnderCrystal crystal) {
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

		Bukkit.broadcastMessage(ChatColor.GOLD + Utils.getRealName(p) + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!\n" + formatTick(tick));

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
	public void onPlateStep(Player p, Location plate) {
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

	public void placeAtPlate(Player p, Location plate) {
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
		String activeMsg = placedColor + String.valueOf(placed) + ChatColor.GREEN + "/2 Energy Crystals are now active!\n" + formatTick(tick);
		Bukkit.broadcastMessage(activeMsg);
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.sendTitle("", activeMsg.split("\n")[0], 0, 40, 0);
		}

		if(bothPlaced) {
			beginLaserCharge();
		}
	}

	private void beginLaserCharge() {
		cancelLaserScan();
		Utils.scheduleTask(() -> {
			if(boss == null || boss.isDead()) return;
			if(plateLeftCrystal == null || plateRightCrystal == null) return;
			String chargeMsg = ChatColor.GREEN + "The Energy Laser is charging up!\n" + formatTick(tick);
			Bukkit.broadcastMessage(chargeMsg);
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle("", chargeMsg.split("\n")[0], 0, 40, 0);
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:red_stained_glass");
			startLaserScan();
		}, CHARGE_DELAY_TICKS);
	}

	private void startLaserScan() {
		cancelLaserScan();
		laserScanTask = new BukkitRunnable() {
			@Override
			public void run() {
				if(boss == null || boss.isDead()) {
					cancel();
					laserScanTask = null;
					return;
				}
				if(stunCooldownActive) return;

				double dx = boss.getLocation().getX() - LASER_CENTER_X;
				double dz = boss.getLocation().getZ() - LASER_CENTER_Z;
				if(dx * dx + dz * dz <= LASER_RADIUS_SQ) {
					triggerStun();
					cancel();
					laserScanTask = null;
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private void cancelLaserScan() {
		if(laserScanTask != null && !laserScanTask.isCancelled()) {
			laserScanTask.cancel();
		}
		laserScanTask = null;
	}

	private void triggerStun() {
		double maxHp = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
		double laserDmg = maxHp * 0.05;
		double currentHp = boss.getHealth();
		double onePercent = maxHp * 0.01;

		// Killing-blow path: laser would drop Maxor to/below 0 HP — clamp to 1% + death sequence.
		if(laserDmg >= currentHp) {
			clearAggro();
			setArmor(false);
			sendChatMessage(LASER_MESSAGE[random.nextInt(LASER_MESSAGE.length)]);
			boss.setHealth(onePercent);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_HURT);
			enterDyingState();
			return;
		}

		stunCooldownActive = true;
		Utils.scheduleTask(() -> stunCooldownActive = false, STUN_COOLDOWN_TICKS);
		inStun = true;
		stunDamageDealt = 0;

		clearAggro();
		setArmor(false);
		sendChatMessage(LASER_MESSAGE[random.nextInt(LASER_MESSAGE.length)]);
		Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor stunned in " + formatTick(tick));

		// Laser hit: 5% max HP damage + wither hurt sound. Bypasses the damage event
		// (no event recursion) and counts toward the stun's 75% damage cap.
		boss.setHealth(Math.max(0.0, currentHp - laserDmg));
		stunDamageDealt += laserDmg;
		Utils.changeName(boss);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_HURT);

		CustomBossBar.spawnAnimatedStunnedIndicator(boss, Integer.MAX_VALUE);

		Utils.scheduleTask(() -> {
			resetCrystals();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setblock 73 224 73 minecraft:black_stained_glass");
		}, CRYSTAL_RESPAWN_DELAY_TICKS);

		// Auto-enrage 160 ticks after the stun, regardless of damage taken.
		cancelStunEnrageTask();
		stunEnrageTask = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), this::enrageMaxor, 160L);
	}

	private void enterDyingState() {
		dying = true;
		boss.addScoreboardTag("TASDying");
		cancelStunEnrageTask();
		cancelLaserScan();
		inStun = false;
		CustomBossBar.removeStunIndicator();
		// Pin internal HP to 1 — display already shows "1" via the TASDying tag in formatHealthM.
		// Deferred 1 tick so vanilla's post-event setHealth doesn't overwrite us.
		Utils.scheduleTask(() -> { if(boss != null && boss.isValid()) boss.setHealth(1.0); }, 1);
		Utils.changeName(boss);
		playDeathDialogue();
	}

	private void cancelStunEnrageTask() {
		if(stunEnrageTask != null && !stunEnrageTask.isCancelled()) {
			stunEnrageTask.cancel();
		}
		stunEnrageTask = null;
	}

	private void enrageMaxor() {
		if(!inStun) return;
		inStun = false;
		cancelStunEnrageTask();

		setArmor(true);
		Bukkit.broadcastMessage(ChatColor.RED + "⚠ Maxor is Enraged ⚠\n" + formatTick(tick));
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.sendTitle("", ChatColor.RED + "⚠ Maxor is Enraged ⚠", 0, 40, 0);
		}
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
		CustomBossBar.removeStunIndicator();
		setAggro(Tank.get(), 3.0, 1.0);
	}

	/**
	 * Damage interceptor for Maxor. Hook from a Bukkit listener.
	 * - If a hit would kill him, clamp it to leave him on 1% HP and play the death dialogue.
	 * - Otherwise during a stun, clamp cumulative damage to 75% of max HP and trigger enrage.
	 */
	public void handleDamage(EntityDamageEvent e) {
		if(boss == null || !boss.equals(e.getEntity())) return;
		if(e.isCancelled()) return;

		// Dying = completely immune. Cancel before any damage logic.
		if(dying) {
			e.setCancelled(true);
			return;
		}

		double finalDmg = e.getFinalDamage();
		if(finalDmg <= 0) return;

		double currentHp = boss.getHealth();
		double maxHp = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
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
			if(willEnrage) Bukkit.getScheduler().runTask(M7tas.getInstance(), this::enrageMaxor);
		}
	}

	private static void scaleEventDamage(EntityDamageEvent e, double currentFinal, double targetFinal) {
		if(currentFinal <= 0) return;
		double scale = Math.max(0, targetFinal) / currentFinal;
		e.setDamage(e.getDamage() * scale);
	}

	private void playDeathDialogue() {
		sendChatMessage("I'M TOO YOUNG TO DIE AGAIN!");
		Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor killed in " + formatTick(tick));
		Server.playWitherDeathSound(boss);
		Utils.scheduleTask(() -> sendChatMessage("I'LL MAKE YOU REMEMBER MY DEATH!"), 60);
		Utils.scheduleTask(() -> {
			// -1 because this is scheduled after the ticker, so there is an off-by-one without it
			Bukkit.broadcastMessage(ChatColor.GREEN + "Maxor finished in " + formatTick(tick - 1));
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
			chainNext(doContinue);
		}, 100);
	}

	public boolean isDyingWither(Wither w) {
		return dying && w != null && w.equals(boss);
	}

	private EnderCrystal spawnEnergyCrystal(Location loc) {
		EnderCrystal c = (EnderCrystal) world.spawnEntity(loc, EntityType.END_CRYSTAL);
		c.setCustomName(ENERGY_CRYSTAL_NAME);
		c.setCustomNameVisible(true);
		return c;
	}

	private void spawnMiners() {
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
			miner.setCustomName("Wither Miner " + ChatColor.YELLOW + "8M" + ChatColor.RED + "❤");
			miner.setCustomNameVisible(true);

			// Store in array
			miners[i] = miner;
		}
	}
}
