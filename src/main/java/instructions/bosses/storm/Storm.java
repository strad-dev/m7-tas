package instructions.bosses.storm;

import instructions.Actions;
import instructions.Server;
import instructions.bosses.CustomBossBar;
import instructions.bosses.MobGroup;
import instructions.bosses.MobSpawnSpec;
import instructions.bosses.WitherLord;
import instructions.bosses.goldor.Goldor;
import instructions.players.Mage;
import commands.Spectate;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import plugin.BossScheduler;
import plugin.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SuppressWarnings("DataFlowIssue")
public final class Storm extends WitherLord {
	public static final Storm INSTANCE = new Storm();

	private static final int PRE_STORM_TICKS = 1235;

	// Intro ends at this tick; aggro + crush detection enable here.
	private static final int INTRO_END_TICK = 665;

	// Crush parameters.
	private static final double CRUSH_DAMAGE_FRACTION = 0.05;
	private static final double STUN_DAMAGE_CAP_FRACTION = 0.55;
	private static final int CRUSH_EXPLOSION_DELAY = 20;
	// Tuned for a wider visible blast radius. Each 0.3-block step through diorite decays
	// ray power by ~1.9, so the average destruction radius is roughly power/6 blocks.
	private static final float CRUSH_EXPLOSION_POWER = 50.0f;
	private static final int CRUSH_DETECTOR_WINDOW = 61;
	// Lowest Y Storm can be pushed down to by a descending pillar — at or below this
	// the push no-ops and the pillar is allowed to crush.
	private static final double STORM_FLOOR_Y = 169.0;
	private static final int STUN_AUTO_ENRAGE_TICKS = 160;

	// Aggro parameters (post-intro and post-enrage).
	private static final double AGGRO_STOP_DISTANCE = 6.0;
	private static final double AGGRO_Y_OFFSET = 2.0;
	private static final double AGGRO_MAX_SPEED = 0.66666;

	// Center the miners and sentries face toward.
	private static final Location FACING_CENTER = new Location(null, 73.5, 0, 53.5);

	// Deterministic Wither Guard locations — these are placed at exact pillar-sentry spots
	// rather than randomized within an AABB.
	private static final double[][] SENTRY_COORDS = {{114.5, 175, 35.5}, {114.5, 175, 45.5}, {114.5, 175, 61.5}, {114.5, 175, 71.5}, {86.5, 175, 35.5}, {86.5, 175, 45.5}, {86.5, 175, 61.5}, {86.5, 175, 71.5}, {60.5, 175, 35.5}, {60.5, 175, 45.5}, {60.5, 175, 61.5}, {60.5, 175, 71.5}, {32.5, 175, 35.5}, {32.5, 175, 45.5}, {32.5, 175, 61.5}, {32.5, 175, 71.5}, {79.5, 170, 104.5}, {77.5, 170, 103.5}, {75.5, 170, 103.5}, {73.5, 170, 103.5}, {71.5, 170, 103.5}, {69.5, 170, 103.5}, {67.5, 170, 104.5}, {22.5, 172, 59.5}, {23.5, 172, 57.5}, {23.5, 172, 55.5}, {23.5, 172, 53.5}, {23.5, 172, 51.5}, {23.5, 172, 49.5}, {22.5, 172, 47.5}, {67.5, 170, 2.5}, {69.5, 170, 3.5}, {71.5, 170, 3.5}, {73.5, 170, 3.5}, {75.5, 170, 3.5}, {77.5, 170, 3.5}, {79.5, 170, 2.5}, {124.5, 172, 47.5}, {123.5, 172, 49.5}, {123.5, 172, 51.5}, {123.5, 172, 53.5}, {123.5, 172, 55.5}, {123.5, 172, 57.5}, {124.5, 172, 59.5}};

	private static final Random random = new Random();
	private static final String[] LIGHTNING_MESSAGE = {"ENERGY HEED MY CALL!", "THUNDER LET ME BE YOUR CATALYST!"};
	private static final String[] CRUSHED_MESSAGE = {"Ouch, that hurt!", "Oof"};
	private static final String[] ENRAGE_MESSAGE = {"THAT WAS ONLY IN MY WAY!", "Slowing me down will be your greatest accomplishment!", "This factory is too small for me!", "BEGONE PILLAR!"};

	private final List<PillarOscillator> pillars = new ArrayList<>();
	private final List<MobGroup> mobGroups = new ArrayList<>();
	private final List<WitherSkeleton> sentries = new ArrayList<>();

	// The pad/crush poll runs as a boss ticker (BossScheduler.addTicker) so crush detection + its state mutation
	// happen at the start of the tick, before the players' beam choreography — see BossScheduler.
	private Runnable cycleTicker;
	// Auto-enrage one-shot, run as a boss-lane task (BossScheduler.schedule) so it fires at the start of its tick.
	private Runnable stunEnrageTask;
	private boolean crushEnabled;
	private boolean inStun;
	private double stunDamageDealt;
	private PadAndPillar currentCrushPillar;
	private boolean crushExplosionActive;

	private Storm() {
		register(this);
	}

	/**
	 * Static facade for /tas and the boss-chain.
	 */
	public static void stormInstructions(World world, boolean doContinue) {
		INSTANCE.start(world, doContinue);
	}

	@Override
	protected String name() {
		return "Storm";
	}

	@Override
	protected String displayName() {
		return "Storm";
	}

	@Override
	protected Location spawnLocation() {
		return new Location(world, 102.5, 182, 53.5, 90f, 0f);
	}

	@Override
	protected double maxHealth() {
		return 600;
	}

	@Override
	protected String displayHealth() {
		return "1B";
	}

	@Override
	protected int previousTicks() {
		return PRE_STORM_TICKS;
	}

	@Override
	protected void resetState() {
		cancelCycleTask();
		cancelStunEnrageTask();
		CustomBossBar.removeStunIndicator();
		crushEnabled = false;
		inStun = false;
		stunDamageDealt = 0;
		currentCrushPillar = null;
		cleanupMobs();
		pillars.clear();
		for(PadAndPillar p : PadAndPillar.ACTIVE) {
			pillars.add(new PillarOscillator(p));
		}
	}

	@Override
	protected void onStart() {
		spawnMobGroups();
		initialMovement();
		scheduleIntroDialogue();

		Utils.scheduleTask(() -> {
			crushEnabled = true;
			setAggro(Mage.get(), AGGRO_STOP_DISTANCE, AGGRO_Y_OFFSET, AGGRO_MAX_SPEED);
		}, INTRO_END_TICK);

		startCycleTask();
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			Goldor.goldorInstructions(world, true);
			runPlayerHandoff(); // start each player's goldor() routine the same tick Goldor spawns
		}
	}

	private void scheduleIntroDialogue() {
		sendChatMessage("Pathetic Maxor, just like expected.");
		Utils.scheduleTask(() -> sendChatMessage("Don't boast about beating this simple-minded Wither."), 60);
		Utils.scheduleTask(() -> sendChatMessage("My abilities are unparalleled, in may ways I am the last bastion."), 120);
		Utils.scheduleTask(() -> sendChatMessage("The memory of your death will be your fondest, focus up!"), 180);
		Utils.scheduleTask(() -> {
			sendChatMessage("The power of lightning is quite phenomenal.  A single strike can vaporize a person whole.");
			Actions.turnHead(boss, 90f, 0f);
		}, 400);
		Utils.scheduleTask(() -> {
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle(ChatColor.DARK_RED + "4", "", 0, 25, 0);
			}
		}, 440);
		Utils.scheduleTask(() -> sendChatMessage("I'd be happy to show you what that's like!"), 460);
		Utils.scheduleTask(() -> {
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle(ChatColor.DARK_RED + "3", "", 0, 25, 0);
			}
		}, 465);
		Utils.scheduleTask(() -> {
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle(ChatColor.DARK_RED + "2", "", 0, 25, 0);
			}
		}, 490);
		Utils.scheduleTask(() -> {
			for(Player player : Bukkit.getOnlinePlayers()) {
				player.sendTitle(ChatColor.DARK_RED + "1", "", 0, 25, 0);
			}
		}, 515);
		Utils.scheduleTask(() -> sendChatMessage(LIGHTNING_MESSAGE[random.nextInt(LIGHTNING_MESSAGE.length)]), 525);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 1.0F);
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0F, 1.0F);
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
			spamLightning();
		}, 535);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 1.0F);
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0F, 1.0F);
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
			spamLightning();
		}, 545);
	}

	private void initialMovement() {
		Actions.turnHead(boss, 45f, 0f);
		Actions.forceMove(boss, new Vector(-0.29, 0, 0.29), 100);
		Utils.scheduleTask(() -> {
			Actions.turnHead(boss, 135f, 0f);
			Actions.forceMove(boss, new Vector(-0.29, 0, -0.29), 100);
		}, 100);
		Utils.scheduleTask(() -> {
			Actions.turnHead(boss, -135f, 0f);
			Actions.forceMove(boss, new Vector(0.29, 0, -0.29), 100);
		}, 200);
		Utils.scheduleTask(() -> {
			Actions.turnHead(boss, -45f, 0f);
			Actions.forceMove(boss, new Vector(0.29, 0, 0.29), 100);
		}, 300);
	}

	// --- 20-tick poll loop: pad gating + crush detection ---

	private void startCycleTask() {
		cancelCycleTask();
		// Runs every tick in the boss heartbeat, but the poll body only fires on phase ticks divisible by 20
		// (0, 20, 40, ...). Gating on the absolute phase tick — not a registration-relative counter — keeps the
		// cadence locked to the 20-grid regardless of when the ticker was registered or first ran.
		cycleTicker = () -> {
			if(boss == null || boss.isDead()) {
				cancelCycleTask();
				return;
			}
			if(displayTick() % 20 != 0) return;

			// Pad-gated pillar advance: per pillar, if any player stands on its pad, run a cycle.
			// Used (already-crushed) pillars are skipped — their pad is dead.
			for(PillarOscillator osc : pillars) {
				if(osc.isUsed()) continue;
				if(padOccupied(osc.getPillar().padBox())) {
					osc.runCycle(tick);
				}
			}

			// Crush detection — only after intro ends, only while not already stunned, and only
			// within the 60-tick window after any pillar's most recent movement.
			if(crushEnabled && !inStun && !dying && anyPillarMovedRecently() && stormInDiorite()) {
				triggerCrush();
			}
		};
		BossScheduler.addTicker(cycleTicker);
	}

	private void cancelCycleTask() {
		if(cycleTicker != null) {
			BossScheduler.removeTicker(cycleTicker);
			cycleTicker = null;
		}
	}

	private boolean padOccupied(BoundingBox padBox) {
		for(Player p : world.getPlayers()) {
			if(p.getGameMode() == GameMode.SPECTATOR) continue;
			// A player spectating a fake player is teleported onto that fake's position every tick, so their
			// location isn't their own — ignore them or they'd falsely register as standing on the pad.
			if(Spectate.isSpectating(p)) continue;
			int bx = p.getLocation().getBlockX();
			int by = p.getLocation().getBlockY();
			int bz = p.getLocation().getBlockZ();
			if(bx >= padBox.getMinX() && bx <= padBox.getMaxX() && by >= padBox.getMinY() && by <= padBox.getMaxY() && bz >= padBox.getMinZ() && bz <= padBox.getMaxZ()) {
				return true;
			}
		}
		return false;
	}

	private boolean anyPillarMovedRecently() {
		for(PillarOscillator osc : pillars) {
			if(osc.movedRecently(tick, CRUSH_DETECTOR_WINDOW)) return true;
		}
		return false;
	}

	private boolean stormInDiorite() {
		BoundingBox box = boss.getBoundingBox();
		int minX = (int) Math.floor(box.getMinX());
		int maxX = (int) Math.floor(box.getMaxX());
		int minY = (int) Math.floor(box.getMinY());
		int maxY = (int) Math.floor(box.getMaxY());
		int minZ = (int) Math.floor(box.getMinZ());
		int maxZ = (int) Math.floor(box.getMaxZ());
		for(int x = minX; x <= maxX; x++) {
			for(int y = minY; y <= maxY; y++) {
				for(int z = minZ; z <= maxZ; z++) {
					Material m = world.getBlockAt(x, y, z).getType();
					if(m == Material.DIORITE || m == Material.POLISHED_DIORITE) return true;
				}
			}
		}
		return false;
	}

	// --- Crush mechanic (mirror Maxor.triggerStun / handleDamage with 0.55 cap and 20t-delayed explosion) ---

	private void triggerCrush() {
		// Consume the pillar Storm is currently inside so its pad goes dead, and
		// record it so the T+20 explosion listener can scope block destruction
		// to that pillar's column only (no collateral damage to other pillars).
		PillarOscillator crushed = findPillarStormIsIn();
		currentCrushPillar = crushed != null ? crushed.getPillar() : null;
		if(crushed != null) crushed.markUsed();

		double maxHp = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
		double crushDmg = maxHp * CRUSH_DAMAGE_FRACTION;
		double currentHp = boss.getHealth();
		double onePercent = maxHp * 0.01;

		// Killing-blow path: crush would drop Storm to/below 0 HP — clamp to 1% + death sequence.
		if(crushDmg >= currentHp) {
			clearAggro();
			setArmor(false);
			sendChatMessage(CRUSHED_MESSAGE[random.nextInt(CRUSHED_MESSAGE.length)]);
			boss.setHealth(onePercent);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_HURT);
			enterDyingState();
			Utils.scheduleTask(this::fireCrushExplosion, CRUSH_EXPLOSION_DELAY);
			return;
		}

		inStun = true;
		stunDamageDealt = 0;

		clearAggro();
		setArmor(false);
		sendChatMessage(CRUSHED_MESSAGE[random.nextInt(CRUSHED_MESSAGE.length)]);
		Utils.timer(ChatColor.GREEN + "Storm crushed in " + formatTick(displayTick()));

		// Crush damage: 5% max HP, bypasses event (no recursion) — counted toward the 55% stun cap.
		boss.setHealth(Math.max(0.0, currentHp - crushDmg));
		stunDamageDealt += crushDmg;
		Utils.changeName(boss);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_HURT);

		CustomBossBar.spawnAnimatedStunnedIndicator(boss, Integer.MAX_VALUE);

		// Auto-enrage N ticks after the crush (start of tick), so a beam on the enrage tick sees the re-armored boss.
		cancelStunEnrageTask();
		stunEnrageTask = BossScheduler.schedule(this::enrageStorm, STUN_AUTO_ENRAGE_TICKS);

		// Pillar destruction explosion fires 20 ticks after Storm is damaged.
		Utils.scheduleTask(this::fireCrushExplosion, CRUSH_EXPLOSION_DELAY);
	}

	/**
	 * @return the PillarOscillator whose column Storm's hitbox currently overlaps horizontally.
	 * If no overlap (e.g. Storm is between columns at trigger time), falls back to the closest
	 * unused active pillar by horizontal distance — so the crush always has a valid pillar to
	 * scope explosion destruction to. Returns null only if every active pillar has been used.
	 */
	private PillarOscillator findPillarStormIsIn() {
		BoundingBox box = boss.getBoundingBox();
		double sx1 = box.getMinX(), sx2 = box.getMaxX();
		double sz1 = box.getMinZ(), sz2 = box.getMaxZ();
		PillarOscillator closest = null;
		double closestDistSq = Double.POSITIVE_INFINITY;
		double sxMid = (sx1 + sx2) * 0.5;
		double szMid = (sz1 + sz2) * 0.5;
		for(PillarOscillator osc : pillars) {
			if(osc.isUsed()) continue;
			PadAndPillar p = osc.getPillar();
			// Pillar block columns occupy x in [pillarX1, pillarX2+1) and z in [pillarZ1, pillarZ2+1).
			if(sx2 >= p.pillarX1() && sx1 <= p.pillarX2() + 1
					&& sz2 >= p.pillarZ1() && sz1 <= p.pillarZ2() + 1) {
				return osc;
			}
			double pxMid = (p.pillarX1() + p.pillarX2() + 1) * 0.5;
			double pzMid = (p.pillarZ1() + p.pillarZ2() + 1) * 0.5;
			double ddx = sxMid - pxMid;
			double ddz = szMid - pzMid;
			double d2 = ddx * ddx + ddz * ddz;
			if(d2 < closestDistSq) {
				closestDistSq = d2;
				closest = osc;
			}
		}
		return closest;
	}

	private void fireCrushExplosion() {
		if(boss == null || !boss.isValid()) return;
		Location loc = boss.getLocation();
		// Power=7 mirrors the vanilla Wither spawn explosion. StormCrushExplosion listener
		// filters the resulting block list to keep only diorite/polished_diorite with y<196.
		// Vanilla's Level.explode() force-disables block-breaking when the source is a Mob and
		// mobGriefing is false, regardless of the breakBlocks parameter. Toggle the gamerule
		// for the duration of this call so the explosion can destroy pillar blocks.
		Boolean prevMobGriefing = world.getGameRuleValue(GameRule.MOB_GRIEFING);
		try {
			world.setGameRule(GameRule.MOB_GRIEFING, true);
			crushExplosionActive = true;
			world.createExplosion(loc.getX(), loc.getY(), loc.getZ(), CRUSH_EXPLOSION_POWER, false, true, boss);
		} finally {
			crushExplosionActive = false;
			world.setGameRule(GameRule.MOB_GRIEFING, prevMobGriefing != null && prevMobGriefing);
		}
	}

	private void enrageStorm() {
		if(!inStun) return;
		inStun = false;
		cancelStunEnrageTask();

		setArmor(true);
		sendChatMessage(ENRAGE_MESSAGE[random.nextInt(ENRAGE_MESSAGE.length)]);
		Utils.timer(ChatColor.RED + "⚠ Storm is enraged! ⚠\n" + formatTick(displayTick()));
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.sendTitle("", ChatColor.RED + "⚠ Storm is enraged! ⚠", 0, 40, 0);
		}
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
		CustomBossBar.removeStunIndicator();
		setAggro(Mage.get(), AGGRO_STOP_DISTANCE, AGGRO_Y_OFFSET, AGGRO_MAX_SPEED);
	}

	private void cancelStunEnrageTask() {
		if(stunEnrageTask != null) BossScheduler.removeTicker(stunEnrageTask);
		stunEnrageTask = null;
	}

	/**
	 * Damage interceptor for Storm. Hooked from MiscListener. Identical shape to
	 * Maxor.handleDamage but with the 0.55 stun cap.
	 */
	public void handleDamage(EntityDamageEvent e) {
		if(boss == null || !boss.equals(e.getEntity())) return;
		if(e.isCancelled()) return;

		if(dying) {
			e.setCancelled(true);
			return;
		}

		double finalDmg = e.getFinalDamage();
		if(finalDmg <= 0) return;

		double currentHp = boss.getHealth();
		double maxHp = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
		double onePercent = maxHp * 0.01;

		double cappedDmg = finalDmg;
		boolean willEnrage = false;
		if(inStun) {
			double damageCap = maxHp * STUN_DAMAGE_CAP_FRACTION;
			double remaining = Math.max(0, damageCap - stunDamageDealt);
			if(cappedDmg >= remaining) {
				cappedDmg = remaining;
				willEnrage = true;
			}
		}

		boolean willDie = false;
		if(cappedDmg >= currentHp) {
			cappedDmg = Math.max(0, currentHp - onePercent);
			willDie = true;
			willEnrage = false;
		}

		if(cappedDmg < finalDmg) {
			scaleEventDamage(e, finalDmg, cappedDmg);
		}

		if(willDie) {
			enterDyingState();
		} else {
			if(inStun) stunDamageDealt = Math.min(maxHp * STUN_DAMAGE_CAP_FRACTION, stunDamageDealt + cappedDmg);
			if(willEnrage) enrageStorm();
		}
	}

	private static void scaleEventDamage(EntityDamageEvent e, double currentFinal, double targetFinal) {
		if(currentFinal <= 0) return;
		double scale = Math.max(0, targetFinal) / currentFinal;
		e.setDamage(e.getDamage() * scale);
	}

	private void enterDyingState() {
		dying = true;
		boss.addScoreboardTag("TASDying");
		cancelStunEnrageTask();
		cancelCycleTask();
		inStun = false;
		CustomBossBar.removeStunIndicator();
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) boss.setHealth(1.0);
		}, 1);
		Utils.changeName(boss);
		playDeathDialogue();
	}

	private void playDeathDialogue() {
		sendChatMessage("I should have known that I stand no chance.");
		Server.playWitherDeathSound(boss);
		Utils.timer(ChatColor.GREEN + "Storm killed in " + formatTick(displayTick()));
		Utils.scheduleTask(() -> sendChatMessage("At least my son died by your hands."), 60);
		Utils.scheduleTask(() -> {
			Utils.timer(ChatColor.GREEN + "Storm finished in " + formatTick(displayTick()));
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
			chainNext(doContinue);
		}, 100);
	}

	public boolean isDyingWither(Wither w) {
		return dying && w != null && w.equals(boss);
	}

	/**
	 * The pillar currently being destroyed by a crush explosion (set in {@link #triggerCrush}
	 * and consumed by {@link listeners.StormCrushExplosion}). null between crushes.
	 */
	public PadAndPillar getCurrentCrushPillar() {
		return currentCrushPillar;
	}

	/**
	 * True only during the synchronous call to {@code world.createExplosion(...)} inside
	 * {@code fireCrushExplosion}. Used by {@link listeners.StormCrushExplosion} to identify
	 * damage/knockback events sourced by Storm's crush even when the event doesn't carry
	 * the wither entity explicitly.
	 */
	public boolean crushExplosionNotActive() {
		return !crushExplosionActive;
	}

	/**
	 * Called by {@link PillarOscillator} immediately before each DOWN clone op. If
	 * Storm's hitbox horizontally overlaps {@code pillar} and the new pillar bottom
	 * row at {@code newBottomY} would dip into Storm's vertical extent, shove Storm
	 * down by one block to keep him below the descending pillar.
	 * <br>
	 * No-ops if Storm has reached {@link #STORM_FLOOR_Y} — at that point the descending
	 * pillar is allowed to crush. This preserves the rule that a stationary or upward-
	 * moving pillar Storm flies into horizontally still crushes via the 20-tick poll;
	 * push only happens during active downward motion.
	 */
	public void tryPushBelowDescendingPillar(PadAndPillar pillar, int newBottomY) {
		if(boss == null || !boss.isValid()) return;
		BoundingBox box = boss.getBoundingBox();

		// Horizontal overlap with pillar column [pillarX1, pillarX2+1) × [pillarZ1, pillarZ2+1).
		if(box.getMaxX() <= pillar.pillarX1() || box.getMinX() >= pillar.pillarX2() + 1) return;
		if(box.getMaxZ() <= pillar.pillarZ1() || box.getMinZ() >= pillar.pillarZ2() + 1) return;

		// New bottom block occupies y in [newBottomY, newBottomY+1). No vertical overlap
		// means the new clone wouldn't touch Storm — nothing to push out of.
		if(box.getMaxY() <= newBottomY) return;

		if(boss.getLocation().getY() <= STORM_FLOOR_Y) return;

		Location loc = boss.getLocation();
		loc.setY(loc.getY() - 1);
		boss.teleport(loc);
	}

	// --- Mob spawning ---

	private void spawnMobGroups() {
		mobGroups.clear();

		// Static name + equipment shared by every miner.
		String minerName = "Wither Miner " + ChatColor.YELLOW + "8M" + ChatColor.RED + "❤";
		ItemStack stonePickaxe = new ItemStack(Material.STONE_PICKAXE);
		Location facingCenter = FACING_CENTER.clone();
		facingCenter.setWorld(world);

		// 0t: Pillar A (south pillar zone) + Pillar B (north pillar zone)
		mobGroups.add(new MobGroup(minerSpec("Pillar A", 20, MobSpawnSpec.uniformIn(36, 169, 37, 56, 169, 69), minerName, stonePickaxe, facingCenter, 0)));
		mobGroups.add(new MobGroup(minerSpec("Pillar B", 20, MobSpawnSpec.uniformIn(90, 169, 37, 110, 169, 69), minerName, stonePickaxe, facingCenter, 0)));

		// 40t: lava bridge groups
		mobGroups.add(new MobGroup(minerSpec("Lava bridge SW", 5, MobSpawnSpec.uniformIn(72, 168, 29, 74, 168, 31), minerName, stonePickaxe, facingCenter, 40)));
		mobGroups.add(new MobGroup(minerSpec("Lava bridge SE", 5, MobSpawnSpec.uniformIn(72, 168, 21, 74, 168, 23), minerName, stonePickaxe, facingCenter, 40)));
		mobGroups.add(new MobGroup(minerSpec("Lava bridge NW", 5, MobSpawnSpec.uniformIn(72, 168, 75, 74, 168, 77), minerName, stonePickaxe, facingCenter, 40)));
		mobGroups.add(new MobGroup(minerSpec("Lava bridge NE", 5, MobSpawnSpec.uniformIn(72, 168, 83, 74, 168, 86), minerName, stonePickaxe, facingCenter, 40)));

		// 40t: center-edge groups
		mobGroups.add(new MobGroup(minerSpec("Center edge N", 20, MobSpawnSpec.uniformIn(58, 163, 92, 88, 163, 100), minerName, stonePickaxe, facingCenter, 40)));
		mobGroups.add(new MobGroup(minerSpec("Center edge W", 20, MobSpawnSpec.uniformIn(26, 165, 37, 30, 165, 69), minerName, stonePickaxe, facingCenter, 40)));
		mobGroups.add(new MobGroup(minerSpec("Center edge S", 20, MobSpawnSpec.uniformIn(58, 163, 6, 88, 163, 14), minerName, stonePickaxe, facingCenter, 40)));
		mobGroups.add(new MobGroup(minerSpec("Center edge E", 20, MobSpawnSpec.uniformIn(116, 165, 37, 120, 165, 69), minerName, stonePickaxe, facingCenter, 40)));

		// 80t: center + 4 pad groups (each with one shadow assassin at a corner)
		mobGroups.add(new MobGroup(minerSpec("Center", 40, MobSpawnSpec.uniformIn(65, 165, 41, 81, 165, 65), minerName, stonePickaxe, facingCenter, 80)));

		mobGroups.add(new MobGroup(minerSpec("Pad NE", 10, MobSpawnSpec.uniformIn(108, 170, 88, 120, 170, 100), minerName, stonePickaxe, facingCenter, 80)));
		mobGroups.add(new MobGroup(shadowAssassinSpec("Shadow NE", 120.5, 100.5)));

		mobGroups.add(new MobGroup(minerSpec("Pad NW", 10, MobSpawnSpec.uniformIn(26, 170, 88, 38, 170, 100), minerName, stonePickaxe, facingCenter, 80)));
		mobGroups.add(new MobGroup(shadowAssassinSpec("Shadow NW", 26.5, 100.5)));

		mobGroups.add(new MobGroup(minerSpec("Pad SW", 10, MobSpawnSpec.uniformIn(26, 170, 6, 38, 170, 18), minerName, stonePickaxe, facingCenter, 80)));
		mobGroups.add(new MobGroup(shadowAssassinSpec("Shadow SW", 26.5, 6.5)));

		mobGroups.add(new MobGroup(minerSpec("Pad SE", 10, MobSpawnSpec.uniformIn(108, 170, 6, 120, 170, 18), minerName, stonePickaxe, facingCenter, 80)));
		mobGroups.add(new MobGroup(shadowAssassinSpec("Shadow SE", 120.5, 6.5)));

		// Sentries — deterministic locations, spawned inline (44 separate fixed-location entities).
		spawnSentries();

		// Schedule all groups
		for(MobGroup g : mobGroups) {
			g.spawn(world, random);
		}
	}

	private static MobSpawnSpec minerSpec(String groupName, int count, java.util.function.Function<Random, Vector> provider, String customName, ItemStack mainHand, Location facingCenter, int startTick) {
		return new MobSpawnSpec(groupName, EntityType.WITHER_SKELETON, count, provider, 4.0, -30, -20, customName, mainHand,
				/* aiEnabled */ true, /* aggressive */ true, /* adult */ false,
				/* silent */ false, /* persistent */ true, facingCenter, null, null, startTick,
				Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 2.0f);
	}

	private static MobSpawnSpec shadowAssassinSpec(String groupName, double x, double z) {
		ItemStack boots = Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.PURPLE, ChatColor.LIGHT_PURPLE + "Shadow Assassin Boots");
		List<ItemStack> armor = Arrays.asList(new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), boots);
		List<PotionEffect> effects = List.of(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0));
		return new MobSpawnSpec(groupName, EntityType.ZOMBIE, 1, MobSpawnSpec.fixed(x, 170, z), 15.0, -30, -20, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Shadow Assassin " + ChatColor.RESET + ChatColor.YELLOW + "30M" + ChatColor.RED + "❤", new ItemStack(Material.STONE_SWORD),
				/* aiEnabled */ false, /* aggressive */ false, /* adult */ true,
				/* silent */ true, /* persistent */ true,
				/* facingTarget */ null, effects, armor, 80,
				null, 1.0f);
	}

	private void spawnSentries() {
		sentries.clear();
		Location center = FACING_CENTER.clone();
		center.setWorld(world);

		for(double[] coords : SENTRY_COORDS) {
			Location loc = new Location(world, coords[0], coords[1], coords[2]);
			WitherSkeleton sentry = (WitherSkeleton) world.spawnEntity(loc, EntityType.WITHER_SKELETON);
			sentry.getAttribute(Attribute.MAX_HEALTH).setBaseValue(4.0);
			sentry.setHealth(4.0);
			sentry.getAttribute(Attribute.ARMOR).setBaseValue(-30);
			sentry.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(-20);
			sentry.setAI(false);
			sentry.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
			sentry.setCustomName("Wither Guard " + ChatColor.YELLOW + "8M" + ChatColor.RED + "❤");
			sentry.setCustomNameVisible(true);

			Location targetLoc = new Location(world, center.getX(), loc.getY(), center.getZ());
			Vector direction = targetLoc.toVector().subtract(loc.toVector()).normalize();
			float yaw = (float) (Math.atan2(-direction.getX(), direction.getZ()) * 180.0 / Math.PI);
			float pitch = (float) (Math.asin(-direction.getY()) * 180.0 / Math.PI);
			Location facingLoc = loc.clone();
			facingLoc.setYaw(yaw);
			facingLoc.setPitch(pitch);
			sentry.teleport(facingLoc);

			net.minecraft.world.entity.monster.skeleton.WitherSkeleton nmsWs = (net.minecraft.world.entity.monster.skeleton.WitherSkeleton) ((org.bukkit.craftbukkit.v1_21_R7.entity.CraftWitherSkeleton) sentry).getHandle();
			nmsWs.setAggressive(true);

			sentries.add(sentry);
		}
	}

	public void cleanupMobs() {
		for(MobGroup g : mobGroups) g.cleanup();
		mobGroups.clear();
		for(WitherSkeleton ws : sentries) {
			if(ws != null && ws.isValid()) ws.remove();
		}
		sentries.clear();
	}

	private void spamLightning() {
		for(MobGroup g : mobGroups) {
			for(LivingEntity mob : g.getSpawned()) {
				if(mob == null || !mob.isValid()) continue;
				if(mob instanceof Zombie) continue; // Shadow assassins don't get the lightning effect on them.
				Location l = mob.getLocation();
				world.strikeLightning(l);
				Utils.scheduleTask(() -> world.strikeLightning(l), 10);
			}
		}
		for(WitherSkeleton ws : sentries) {
			if(ws == null || !ws.isValid()) continue;
			Location l = ws.getLocation();
			world.strikeLightning(l);
			Utils.scheduleTask(() -> world.strikeLightning(l), 10);
		}
	}
}
