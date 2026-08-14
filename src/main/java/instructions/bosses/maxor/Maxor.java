package instructions.bosses.maxor;

import instructions.Server;
import instructions.bosses.CustomBossBar;
import instructions.bosses.WitherLord;
import instructions.bosses.storm.Storm;
import listeners.CustomItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import plugin.BossScheduler;
import plugin.FakePlayerInventory;
import plugin.Utils;

import java.time.Duration;
import java.util.*;

@SuppressWarnings("DataFlowIssue")
public final class Maxor extends WitherLord {
	public static final Maxor INSTANCE = new Maxor();

	private static final int PRE_MAXOR_TICKS = 738;
	private static final String ENERGY_CRYSTAL_ID = "skyblock/game/energy_crystal";
	private static final String ENERGY_CRYSTAL_NAME = "<gold><bold>﴾ <red>Energy Crystal<gold> ﴿";
	private static final String[] LASER_MESSAGE = {"YOU TRICKED ME!", "THAT BEAM!  IT HURTS!  IT HURTS!"};

	// Laser/stun cycle constants.
	private static final double LASER_CENTER_X = 73.5;
	private static final double LASER_CENTER_Z = 73.5;
	private static final double LASER_RADIUS_SQ = 2.5 * 2.5;
	private static final int CHARGE_DELAY_TICKS = 30;
	// Laser scan cadence: the scan only tests Maxor's position on phase ticks divisible by this (the real-Hypixel
	// 20-tick grid, same as Storm's pad poll).  Also the action bar's countdown period.
	private static final int LASER_CYCLE_TICKS = 20;
	// Phase tick Maxor gets his aggro and starts moving (see onStart).  Nothing can be in the middle of the arena
	// before it, so it's also where the action bar's laser countdown starts.
	private static final int AGGRO_TICK = 160;
	private static final int STUN_COOLDOWN_TICKS = 200;
	// Ticks from the stun to the automatic enrage.  The action bar counts this down, so it has to be the same
	// number the enrage is scheduled on - hence a constant rather than a literal at the schedule site.
	private static final int STUN_AUTO_ENRAGE_TICKS = 160;
	private static final int PLATE_GATE_TICKS = 160;
	private static final int CRYSTAL_RESPAWN_DELAY_TICKS = 40;

	// The two Energy Crystal pressure plates (see placeAtPlate / pickUp).  Stonk and break immune; see isProtected.
	private static final int PLATE_Y = 224, PLATE_Z = 41;
	private static final int PLATE_LEFT_X = 94, PLATE_RIGHT_X = 52;

	private final Random random = new Random();

	// Top spawn crystals, which are pickupable.  Nulled on pickUp.
	private EnderCrystal topLeftCrystal;
	private EnderCrystal topRightCrystal;
	// Plate-placed crystals, which are NOT pickupable.  Committed once placed.
	private EnderCrystal plateLeftCrystal;
	private EnderCrystal plateRightCrystal;
	private final Map<UUID, ItemStack> previousSlot8 = new HashMap<>();
	private final WitherSkeleton[] miners = new WitherSkeleton[10];

	// Laser/stun cycle state.
	// The laser scan runs as a boss ticker (BossScheduler.addTicker) so the stun is detected and applied every
	// tick BEFORE the players' beam choreography, which lets a beam hit on the stun tick read the post-stun state.
	private Runnable laserTicker;
	// Auto-enrage one-shot, run as a boss-lane task (BossScheduler.schedule) so it fires at the start of its tick.
	private Runnable stunEnrageTask;
	private boolean platesActive;
	private boolean stunCooldownActive;
	private boolean inStun;
	private double stunDamageDealt;
	// Latched true the moment the stun's 75% damage cap is reached.  Once set, handleDamage rejects ALL further
	// damage until the next stun.  That is what stops same-tick arrows landing AFTER the cap-enrage from
	// over-DPSing: enrage flips inStun=false mid-tick, which would re-open the uncapped path for the rest of the tick.
	private boolean stunCapReached;
	private final List<Runnable> pendingPlateChecks = new ArrayList<>();

	// Action-bar HUD (see updateActionBar).  Its own boss ticker rather than a job on the laser scan's, because two
	// of the three segments outlive that scan: it's removed the moment the stun triggers.
	private Runnable barTicker;
	// Phase ticks the two stun countdowns run out on, so the bar counts down the same clocks the mechanic uses
	// instead of a counter of its own.
	private int stunEndTick;
	private int laserImmuneUntilTick;
	// Whether the bar currently shows one of our segments, so an idle phase clears it once instead of broadcasting
	// an empty bar to everyone every tick.
	private boolean barShown;

	private Maxor() {
		register(this);
	}

	/**
	 * Static facade for /tas, Watcher, and the boss-chain.
	 */
	public static void maxorInstructions(World world, boolean doContinue) {
		INSTANCE.start(world, doContinue);
	}

	@Override
	protected String name() {
		return "Maxor";
	}

	@Override
	protected String displayName() {
		return "Maxor";
	}

	@Override
	protected Location spawnLocation() {
		return new Location(world, 73.5, 226, 53.5, 0f, 0f);
	}

	@Override
	protected double maxHealth() {
		return damage.MobStats.MAXOR.internalHealth();
	}

	@Override
	protected String displayHealth() {
		return "800M";
	}

	@Override
	protected int previousTicks() {
		return PRE_MAXOR_TICKS;
	}

	@Override
	protected void resetState() {
		cancelLaserScan();
		cancelStunEnrageTask();
		cancelBarTicker();
		CustomBossBar.removeStunIndicator();
		inStun = false;
		stunDamageDealt = 0;
		stunCapReached = false;
		stunCooldownActive = false;
		stunEndTick = 0;
		laserImmuneUntilTick = 0;
		platesActive = false;
		pendingPlateChecks.clear();
		// EnderCrystal handles cleared by resetCrystals() inside onStart.
	}

	@Override
	protected void onStart() {
		startBarTicker();

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
			setAggro(3.0, 1.0, 0.5);
			spawnMiners();
			Utils.playGlobalSound(Sound.ENTITY_WITHER_SPAWN);
			Utils.playGlobalSound(Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 2.0F);
		}, AGGRO_TICK);
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			Storm.stormInstructions(world, true);
			runPlayerHandoff(); // start each player's storm() routine the same tick Storm spawns
		} else {
			instructions.bosses.WitherActions.signalRunComplete(); // Maxor was the last boss of this practice
		}
	}

	public static ItemStack getEnergyCrystalItem() {
		return FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, "<red>Energy Crystal", ENERGY_CRYSTAL_ID);
	}

	public boolean notEnergyCrystal(Entity e) {
		// Only the top spawn crystals can be picked up; plate-placed ones are committed.
		return !(e instanceof EnderCrystal) || (!e.equals(topLeftCrystal) && !e.equals(topRightCrystal));
	}

	/** True if this block is one of the two Energy Crystal pressure plates OR the support block directly beneath
	*  it.  Both are stonk and break immune so the plate can't be knocked out from under a crystal placement;
	*  breaking the support pops the plate off too.  This is a pure positional test, phase-independent and immune
	*  in EVERY phase including the pre-run prep window, mirroring
	*  {@link instructions.bosses.goldor.Goldor#isProtected}. */
	public boolean isProtected(Block b) {
		return (b.getY() == PLATE_Y || b.getY() == PLATE_Y - 1) && b.getZ() == PLATE_Z
				&& (b.getX() == PLATE_LEFT_X || b.getX() == PLATE_RIGHT_X);
	}

	public void resetCrystals() {
		if(world == null) world = Bukkit.getWorlds().getFirst();

		if(topLeftCrystal != null) topLeftCrystal.remove();
		if(topRightCrystal != null) topRightCrystal.remove();
		if(plateLeftCrystal != null) {
			plateLeftCrystal.remove();
			plateLeftCrystal = null;
		}
		if(plateRightCrystal != null) {
			plateRightCrystal.remove();
			plateRightCrystal = null;
		}

		topLeftCrystal = spawnEnergyCrystal(new Location(world, 82.5, 238.48, 50.5));
		topRightCrystal = spawnEnergyCrystal(new Location(world, 64.5, 238.48, 50.5));
	}

	/**
	 * Collect a top Energy Crystal (right-click or left-click, both routed here by {@code MiscListener}).
	 * <br>
	 * <b>Spectators are refused here, at the chokepoint</b>, not in the two listeners, so every route in gets the
	 * check.  A pickup DELETES the crystal from the arena and hands the item to that player, and only the holder
	 * can walk it onto a plate, so an idle m7 spectator taking one used to leave the running party unable to
	 * finish the mechanic at all, with the crystal sitting in an inventory they can never reach.  Vanilla is no
	 * help: the interact event fires for a spectator's click on an entity hitbox exactly as it does for a runner's.
	 */
	public void pickUp(Player p, EnderCrystal crystal) {
		if(Utils.isSpectator(p)) return;
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

		Bukkit.broadcast(Utils.msg("<gold>" + Utils.getRealName(p) + "<green> picked up an <aqua>Energy Crystal<green>!"));
		Utils.timer(formatTick(displayTick()));

		// If the player is already standing on a plate, place immediately,
		// no PHYSICAL interact fires until they re-step on the plate.
		Location feet = p.getLocation();
		int fx = feet.getBlockX(), fy = feet.getBlockY(), fz = feet.getBlockZ();
		if(fy == 224 && fz == 41 && (fx == 52 || fx == 94)) {
			onPlateStep(p, feet);
		}
	}

	/**
	 * Plate-step entry point.  If plates are active, place immediately.
	 * Otherwise schedule a recheck for when the gate opens: if the player is still
	 * standing on the same plate at that moment and still holds a crystal, place then.
	 */
	public void onPlateStep(Player p, Location plate) {
		if(platesActive) {
			placeAtPlate(p, plate);
			return;
		}
		// Gate not open yet, so queue a recheck.  The gate-open task drains and runs these.
		final int px = plate.getBlockX(), py = plate.getBlockY(), pz = plate.getBlockZ();
		pendingPlateChecks.add(() -> {
			Location feet = p.getLocation();
			if(feet.getBlockX() == px && feet.getBlockY() == py && feet.getBlockZ() == pz) {
				placeAtPlate(p, plate);
			}
		});
	}

	public void placeAtPlate(Player p, Location plate) {
		// Re-checked here as well as at pickUp, because onPlateStep can DEFER this until the gate opens: the same
		// "re-check all state on the deferred path" rule the other mechanics follow.  Also covers a crystal carried
		// into spectator rather than picked up there.
		if(Utils.isSpectator(p)) return;
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
		p.getInventory().setItem(8, restore != null ? restore : FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, "<green>SkyBlock Menu (Click)", ""));

		boolean bothPlaced = plateLeftCrystal != null && plateRightCrystal != null;
		int placed = bothPlaced ? 2 : 1;
		String placedColor = bothPlaced ? "<green>" : "<red>";
		String activeGame = placedColor + placed + "<green>/2 Energy Crystals are now active!";
		Bukkit.broadcast(Utils.msg(activeGame));
		Utils.timer(formatTick(displayTick()));
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.showTitle(Title.title(Utils.msg(""), Utils.msg(activeGame),
					Title.Times.times(Duration.ofMillis(0L), Duration.ofMillis(40 * 50L), Duration.ofMillis(0L))));
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
			String chargeMsg = "<green>The Energy Laser is charging up!\n" + formatTick(displayTick());
			Utils.timer(chargeMsg);
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.showTitle(Title.title(Utils.msg(""), Utils.msg(chargeMsg.split("\n")[0]),
						Title.Times.times(Duration.ofMillis(0L), Duration.ofMillis(40 * 50L), Duration.ofMillis(0L))));
			}
			Utils.runCommand("setblock 73 224 73 minecraft:red_stained_glass");
			startLaserScan();
		}, CHARGE_DELAY_TICKS);
	}

	private void startLaserScan() {
		cancelLaserScan();
		laserTicker = new Runnable() {
			@Override
			public void run() {
				if(boss == null || boss.isDead()) {
					BossScheduler.removeTicker(this);
					laserTicker = null;
					return;
				}
				// On real Hypixel the laser check is a 20-tick cycle (like Storm's crush detection), not every tick.
				// Anchor to phase ticks divisible by 20 so the stun can only trigger on the 20-tick grid.
				if(displayTick() % LASER_CYCLE_TICKS != 0) return;
				if(stunCooldownActive) return;

				double dx = boss.getLocation().getX() - LASER_CENTER_X;
				double dz = boss.getLocation().getZ() - LASER_CENTER_Z;
				if(dx * dx + dz * dz <= LASER_RADIUS_SQ) {
					triggerStun();
					BossScheduler.removeTicker(this);
					laserTicker = null;
				}
			}
		};
		BossScheduler.addTicker(laserTicker);
	}

	private void cancelLaserScan() {
		if(laserTicker != null) {
			BossScheduler.removeTicker(laserTicker);
			laserTicker = null;
		}
	}

	/**
	 * Per-tick action-bar QoL for the Maxor phase, in the same slot as Storm's pad/crush bar.  One segment at a
	 * time, checked in that order because the later states overlap the earlier ones:
	 * <ul>
	 *   <li><b>Laser</b>: ticks until the next laser check, i.e. how long Maxor can sit in the middle of the arena
	 *       before it stuns him.  Counts {@link #LASER_CYCLE_TICKS} → 1t on the same grid {@link #startLaserScan}
	 *       polls and resets on the poll tick itself.  Shown from {@link #AGGRO_TICK}, when Maxor starts moving,
	 *       NOT from the moment the scan arms: the grid is absolute (it's phase tick mod 20, the same clock the
	 *       scan gates on), so the countdown is already the right answer to "when does the next check land" while
	 *       the crystals are still being walked over.  Before he moves he can't be in the middle at all.</li>
	 *   <li><b>Stunned</b>: ticks until the stun's auto-enrage.  Replaced early if the 75% damage cap ends the stun
	 *       first, since {@link #enrageMaxor} re-renders the bar itself.</li>
	 *   <li><b>Immune</b>: ticks left on {@link #STUN_COOLDOWN_TICKS}, the window the laser cannot stun him in.  That
	 *       window runs from the STUN, not from the enrage, so this is normally the 40t remainder - but after a
	 *       cap-enrage it shows however much is really left, which is the point of counting the real clock.</li>
	 * </ul>
	 * Both stun counters run off phase ticks stamped at {@link #triggerStun} rather than counters of their own, so
	 * the bar can't drift from the mechanic.  Sent to every real player, spectators included, and the fakes are
	 * skipped.  It doesn't collide with {@code ClearManager}'s bar: that one bails out for anyone outside the
	 * dungeon room grid, which the arena is.
	 */
	private void updateActionBar() {
		int t = displayTick();
		String bar;
		if(inStun) {
			bar = "<yellow>Stunned <white>" + Math.max(0, stunEndTick - t) + "t";
		} else if(stunCooldownActive) {
			bar = "<yellow>Immune <white>" + Math.max(0, laserImmuneUntilTick - t) + "t";
		} else if(t >= AGGRO_TICK) {
			bar = "<red>Laser <white>" + (LASER_CYCLE_TICKS - Math.floorMod(t, LASER_CYCLE_TICKS)) + "t";
		} else {
			// Nothing to count (the opening dialogue, before Maxor moves): clear once on the way in rather than
			// broadcasting an empty bar to everyone every tick.
			if(barShown) {
				barShown = false;
				Utils.broadcastActionBar(Component.empty());
			}
			return;
		}
		barShown = true;
		Utils.broadcastActionBar(Utils.msg(bar));
	}

	/** The HUD's own boss ticker.  Registered for the whole phase (from {@link #onStart}), because the laser scan
	 *  it started life on is removed the moment the stun triggers - which is exactly when two of the three
	 *  segments apply. */
	private void startBarTicker() {
		cancelBarTicker();
		barTicker = new Runnable() {
			@Override
			public void run() {
				if(boss == null || boss.isDead()) {
					BossScheduler.removeTicker(this);
					barTicker = null;
					barShown = false;
					Utils.broadcastActionBar(Component.empty());
					return;
				}
				updateActionBar();
			}
		};
		BossScheduler.addTicker(barTicker);
	}

	private void cancelBarTicker() {
		if(barTicker != null) {
			BossScheduler.removeTicker(barTicker);
			barTicker = null;
		}
		// Wipe the HUD instead of letting the last "Immune 1t" sit on screen for its fade-out.
		barShown = false;
		Utils.broadcastActionBar(Component.empty());
	}

	private void triggerStun() {
		double maxHp = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
		double laserDmg = maxHp * 0.05;
		double currentHp = boss.getHealth();

		// Killing-blow path: the laser would drop Maxor to 0 HP or below, so leave DYING_SLIVER and run the death
		// sequence.  This used to leave 1% of max, which is a visible chunk of the health bar at these HP values.
		if(laserDmg >= currentHp) {
			clearAggro();
			setArmor(false);
			sendChatMessage(LASER_MESSAGE[random.nextInt(LASER_MESSAGE.length)]);
			boss.setHealth(DYING_SLIVER);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_HURT);
			enterDyingState();
			return;
		}

		stunCooldownActive = true;
		// Boss-lane: the cooldown must lift at the START of its tick so the laser ticker (also start-of-tick) sees
		// it cleared the same tick, not a tick late.
		BossScheduler.schedule(() -> {
			stunCooldownActive = false;
			// Swap "Immune" back to "Laser" on the tick the window really closes: the HUD ticker registers first, so
			// it already drew this tick's bar from the pre-clear state.
			updateActionBar();
		}, STUN_COOLDOWN_TICKS);
		inStun = true;
		stunDamageDealt = 0;
		stunCapReached = false;
		// Action-bar anchors for the "Stunned"/"Immune" counters.  Both windows open HERE, so the immune one is the
		// stun's 160t plus a 40t tail, not a fresh 200t after the enrage.
		stunEndTick = displayTick() + STUN_AUTO_ENRAGE_TICKS;
		laserImmuneUntilTick = displayTick() + STUN_COOLDOWN_TICKS;

		clearAggro();
		setArmor(false);
		sendChatMessage(LASER_MESSAGE[random.nextInt(LASER_MESSAGE.length)]);
		Utils.timer("<green>Maxor stunned in " + formatTick(displayTick()));

		// Laser hit: 5% max HP damage + wither hurt sound. Bypasses the damage event
		// (no event recursion) and counts toward the stun's 75% damage cap.
		boss.setHealth(Math.max(0.0, currentHp - laserDmg));
		stunDamageDealt += laserDmg;
		Utils.changeName(boss);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_HURT);

		CustomBossBar.spawnAnimatedStunnedIndicator(boss, Integer.MAX_VALUE);

		// Boss-lane: respawn the crystals at the START of their tick (before player choreography) so a right-click
		// on that same tick sees the crystal entity. A raw scheduleTask here gets a higher task-id than the run-start
		// right-click and would run AFTER it that tick, leaving nothing to pick up.
		BossScheduler.schedule(() -> {
			resetCrystals();
			Utils.runCommand("setblock 73 224 73 minecraft:black_stained_glass");
		}, CRYSTAL_RESPAWN_DELAY_TICKS);

		// Auto-enrage exactly 160 ticks after the stun (start of tick), regardless of damage taken: stun at the start
		// of tick T → enrage at the start of tick T+160, so a beam on the enrage tick sees the re-armored boss.
		cancelStunEnrageTask();
		stunEnrageTask = BossScheduler.schedule(this::enrageMaxor, STUN_AUTO_ENRAGE_TICKS);

		// Re-render now: the HUD ticker registers before the laser scan does, so it already drew this tick's bar
		// from the pre-stun state.
		updateActionBar();
	}

	private void enterDyingState() {
		dying = true;
		boss.addScoreboardTag("TASDying");
		cancelStunEnrageTask();
		cancelLaserScan();
		cancelBarTicker();
		inStun = false;
		CustomBossBar.removeStunIndicator();
		// Pin internal HP to DYING_SLIVER.  The display already shows "1" via the TASDying tag in formatHealthM.
		// Deferred 1 tick so vanilla's post-event setHealth doesn't overwrite us.
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) boss.setHealth(DYING_SLIVER);
		}, 1);
		Utils.changeName(boss);
		playDeathDialogue();
	}

	private void cancelStunEnrageTask() {
		if(stunEnrageTask != null) {
			BossScheduler.removeTicker(stunEnrageTask);
		}
		stunEnrageTask = null;
	}

	private void enrageMaxor() {
		if(!inStun) return;
		inStun = false;
		cancelStunEnrageTask();

		setArmor(true);
		Bukkit.broadcast(Utils.msg("<red>⚠ Maxor is Enraged ⚠"));
		Utils.timer(formatTick(displayTick()));
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.showTitle(Title.title(Utils.msg(""), Utils.msg("<red>⚠ Maxor is Enraged ⚠"),
					Title.Times.times(Duration.ofMillis(0L), Duration.ofMillis(40 * 50L), Duration.ofMillis(0L))));
		}
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
		CustomBossBar.removeStunIndicator();
		setAggro(3.0, 1.0, 0.5);

		// Flip "Stunned" to "Immune" on the tick the stun really ends, which a cap-enrage (called from the damage
		// path, mid-tick) reaches long after the HUD ticker ran.
		updateActionBar();
	}

	/**
	 * Damage clamp for Maxor, called from {@code damage.Damage.deal}.
	 * If a hit would kill him, clamp it to leave him on 1% HP and play the death dialogue.
	 * Otherwise during a stun, clamp cumulative damage to 75% of max HP and trigger enrage.
	 */
	@Override
	public double clampDamage(double incoming) {
		if(boss == null) return incoming;

		// Dying = completely immune.  Blocked before any damage logic.
		if(dying) return 0;

		// Stun cap already hit this stun → Maxor has enraged (or is enraging this very tick). Reject everything,
		// including same-tick arrows that resolved after the cap-hitting hit, so the 75% cap can't be exceeded.
		if(stunCapReached) return 0;

		if(incoming <= 0) return 0;

		double currentHp = boss.getHealth();
		double maxHp = boss.getAttribute(Attribute.MAX_HEALTH).getValue();

		// Apply stun cap FIRST so that subsequent killing-blow check sees the already-capped value.
		// Otherwise a single huge hit during stun bypasses the cap by clamping straight to the sliver via the kill clamp.
		double cappedDmg = incoming;
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
			cappedDmg = Math.max(0, currentHp - DYING_SLIVER);
			willDie = true;
			willEnrage = false; // dying overrides, so no enrage
		}

		if(willDie) {
			enterDyingState();
		} else {
			if(inStun) stunDamageDealt = Math.min(maxHp * 0.75, stunDamageDealt + cappedDmg);
			if(willEnrage) {
				// Latch BEFORE enraging so any further same-tick hit is rejected at the top of clampDamage.
				stunCapReached = true;
				enrageMaxor();
			}
		}
		return cappedDmg;
	}

	private void playDeathDialogue() {
		sendChatMessage("I'M TOO YOUNG TO DIE AGAIN!");
		Utils.timer("<green>Maxor killed in " + formatTick(displayTick()));
		Server.playWitherDeathSound(boss);
		// Open the wall to Storm's arena 100t after the killing blow (restored on the next /reset).
		Utils.scheduleTask(instructions.bosses.BossTransition::openMaxorToStorm, 100);
		Utils.scheduleTask(() -> sendChatMessage("I'LL MAKE YOU REMEMBER MY DEATH!"), 60);
		Utils.scheduleTask(() -> {
			Utils.timer("<green>Maxor finished in " + formatTick(displayTick()));
			// Stamp the leaderboard duration at the phase's real end (this tick), not the killing blow.  It must
			// come before chainNext, which spawns Storm and re-anchors the phase clock.
			instructions.bosses.WitherActions.recordPhaseDuration("Maxor", displayTick());
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
			chainNext(doContinue);
		}, 100);
	}

	public boolean isDyingWither(Wither w) {
		return dying && w != null && w.equals(boss);
	}

	private EnderCrystal spawnEnergyCrystal(Location loc) {
		EnderCrystal c = (EnderCrystal) world.spawnEntity(loc, EntityType.END_CRYSTAL);
		c.customName(Utils.msg(ENERGY_CRYSTAL_NAME));
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

			// Real stats (MAP.md §5): 300M, Wither + Undead, so a Hyperion hits it for x1.5 on top of
			// Smite and Undead Ruler.  It gets no Skeletal Ruler - Skeletal is Normal-mode only, and this is
			// Master Mode.  This replaces the old 4 HP flat-kill value and the negative armour attributes.
			damage.MobStats.apply(miner, damage.MobStats.WITHER_MINER);
			miner.setAI(false);

			// Give stone pickaxe
			miner.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_PICKAXE));

			// The name is also how damage/MobStats identifies it, so the leading text has to stay.
			miner.customName(Utils.msg("Wither Miner <yellow>" + Utils.formatHealthM(miner) + "<red>❤"));
			miner.setCustomNameVisible(true);

			// Store in array
			miners[i] = miner;
		}
	}
}
