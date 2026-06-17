package instructions.bosses.witherking;

import instructions.bosses.CustomBossBar;
import instructions.bosses.WitherActions;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonDeathPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftEnderDragon;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import plugin.BossScheduler;
import plugin.ChatFont;
import plugin.FakePlayerInventory;
import plugin.M7tas;
import plugin.Utils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * The Wither King — the final encounter, run AFTER Necron (chained via {@code Necron#chainNext})
 * or standalone via {@code /tas witherking} / {@code /practice witherking}.
 *
 * <p>Two stages:
 * <ol>
 *   <li><b>Summon phase</b> — five relics spawn (ItemDisplay + Interaction) at the dragon statues. A player
 *       right-clicks a relic's Interaction entity to pick it up (→ hotbar slot 8), then right-clicks the
 *       matching altar block (Y 6/7) to place it (wool ItemDisplay appears at y=8). When all five are placed
 *       the Wither King intro fires.</li>
 *   <li><b>Dragon phase</b> — the Wither King (5 HP) spawns; five dragons must be killed (each removes 1 HP).
 *       The first three spawn on timers (Soul/Ice together, then Flame); the last two (Power, then Apex) begin
 *       their spawn animation the tick the last living dragon is killed. Dragon kills are detected
 *       automatically via {@link #handleDragonKilled} (called from {@code Utils.hurtEntity}'s EnderDragon
 *       chokepoint) and fire {@link #instaKillDragon} + {@link #playDragonDeathSound}.</li>
 * </ol>
 *
 * <p>Kept standalone (not a {@code WitherLord} — its 5-HP scale, MAGIC name and dragon-driven HP don't fit),
 * but it reuses the same tick machinery: {@link Utils#markPhaseStart()} / {@link Utils#phaseTick()} /
 * {@link Utils#runTick()} + {@link WitherActions#isPracticeMode()}.
 */
@SuppressWarnings({"unused", "DataFlowIssue"})
public class WitherKing {
	private static World world;
	private static Wither witherKing;
	/** True only when reached via {@code /tas|/practice witherking} (Server case); the Necron→WK chain passes false. */
	private static boolean standalone;
	private static final Random random = new Random();
	private static final String[] dragonDieMessage = {"Oh, this one hurts!", "I have more of those.", "My soul is disposable."};

	/** Cumulative ticks before the Wither-King phase, for the TAS overall column
	 *  (Clear 738 + Maxor 500 + Storm 860 + Goldor 304 + Necron 600). */
	private static final int PRE_WITHERKING_TICKS = 3002;
	/** Ticks after the final dragon dies before the congratulation prints (WK split 1029 − Apex kill 959). */
	private static final int END_DELAY_TICKS = 70;

	// --- Summon-phase relics ---
	/** Each relic: wool material + chat color + label, its dragon-statue spawn point, and its altar block (X,Z). */
	private enum Relic {
		RED(Material.RED_WOOL, ChatColor.RED, "Red", 20.5, 6.8125, 59.5, 51, 42),
		GREEN(Material.GREEN_WOOL, ChatColor.DARK_GREEN, "Green", 20.5, 6.8125, 94.5, 49, 44),
		PURPLE(Material.PURPLE_WOOL, ChatColor.LIGHT_PURPLE, "Purple", 56.5, 8.8125, 132.5, 54, 41),
		BLUE(Material.LIGHT_BLUE_WOOL, ChatColor.AQUA, "Blue", 91.5, 6.8125, 94.5, 59, 44),
		ORANGE(Material.ORANGE_WOOL, ChatColor.GOLD, "Orange", 92.5, 6.8125, 56.5, 57, 42);

		final Material wool;
		final ChatColor chatColor;
		final String label;
		final double x, y, z;     // statue: center X/Z, bottom Y
		final int altarX, altarZ; // altar pillar (Y 6 & 7)

		Relic(Material wool, ChatColor chatColor, String label, double x, double y, double z, int altarX, int altarZ) {
			this.wool = wool;
			this.chatColor = chatColor;
			this.label = label;
			this.x = x; this.y = y; this.z = z;
			this.altarX = altarX; this.altarZ = altarZ;
		}

		String itemName() { return chatColor + label + " Relic"; }

		/** Center Y of the floating wool ItemDisplay above the statue (Purple's statue sits 2 blocks higher). */
		double displayY() { return this == PURPLE ? 9.5 : 7.5; }

		static Relic fromWool(Material m) {
			for(Relic r : values()) if(r.wool == m) return r;
			return null;
		}

		static Relic altarAt(int x, int z) {
			for(Relic r : values()) if(r.altarX == x && r.altarZ == z) return r;
			return null;
		}
	}

	private static final Map<Relic, ItemDisplay> statueDisplays = new HashMap<>();
	private static final Map<Relic, Interaction> statueInteractions = new HashMap<>();
	private static final Map<UUID, Relic> interactionRelic = new HashMap<>();
	private static final List<ItemDisplay> altarWoolDisplays = new ArrayList<>();
	private static final Set<Relic> placedRelics = new HashSet<>();

	// Wither-King scale-up: grows 0.1 scale/tick from 0.1 to its full scale of 4 (rises into existence).
	private static BukkitTask growthTask;
	/** Full scale the Wither King grows to. */
	private static final double WITHER_KING_SCALE = 4.0;

	// Slow Y-axis spin shared by every relic + altar wool display.
	private static BukkitTask rotationTask;
	private static float rotationAngle = 0f;
	private static final float ROTATION_STEP = (float) (Math.PI / 40); // full turn every 80 ticks (4s)
	private static final Vector3f DISPLAY_SCALE = new Vector3f(0.66666f, 0.66666f, 0.66666f); // 0.66666³ wool cube

	// --- Dragon phase ---
	private static final Map<String, EnderDragon> dragons = new HashMap<>();
	private static final Set<UUID> dyingDragons = new HashSet<>();
	/** Phase tick at which each dragon's spawn was announced — used to time its kill relative to its own spawn. */
	private static final Map<String, Integer> dragonSpawnTick = new HashMap<>();
	private static int aliveCount = 0;
	/** True once the last TIMER dragon (Flame) has spawned — gates the event-driven Power/Apex spawns so the
	 *  early death of Soul/Ice (before Flame appears) can't trigger them. */
	private static boolean flameSpawned = false;
	/** Event-spawned dragons, in order: Power then Apex. Spawned when the last living dragon is killed. */
	private static final Deque<String> eventQueue = new ArrayDeque<>();

	// ============================== Entry / summon phase ==============================

	public static void witherKingInstructions(World temp, boolean isStandalone) {
		world = temp;
		standalone = isStandalone;

		forceCleanup(temp);
		Utils.markPhaseStart();
		// Record the Necron section's end for the practice scoreboard (overall tick at WK phase start).
		WitherActions.recordSplit("Necron", Utils.runTick());

		eventQueue.clear();
		eventQueue.add("red");   // Power
		eventQueue.add("green"); // Apex
		aliveCount = 0;
		flameSpawned = false;

		spawnRelics();
	}

	/** Spawn the five relics as an ItemDisplay (0.66666³ wool) + an Interaction (1 × 1.1875 × 1 hitbox) per statue. */
	private static void spawnRelics() {
		for(Relic relic : Relic.values()) {
			// Floating wool sits at displayY (7.25, or 9.25 for Purple); the Interaction hitbox stays on the statue.
			ItemDisplay display = world.spawn(new Location(world, relic.x, relic.displayY(), relic.z), ItemDisplay.class, d -> {
				d.setItemStack(new ItemStack(relic.wool));
				d.setTransformation(rotationTransform(0f)); // 0.66666³ cube; the rotation task animates the spin (per-tick snap)
				d.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
				d.setPersistent(true);
				d.addScoreboardTag("TASWitherKingRelic");
				d.addScoreboardTag("TASNoName");
			});

			Interaction interaction = world.spawn(new Location(world, relic.x, relic.y, relic.z), Interaction.class, i -> {
				i.setInteractionWidth(1.0f);
				i.setInteractionHeight(1.1875f);
				i.setResponsive(true);
				i.setPersistent(true);
				i.addScoreboardTag("TASWitherKingRelic");
				i.addScoreboardTag("TASNoName");
			});

			statueDisplays.put(relic, display);
			statueInteractions.put(relic, interaction);
			interactionRelic.put(interaction.getUniqueId(), relic);
		}
		startRotation();
	}

	/** A 0.66666³-scale transform rotated {@code angle} radians about the Y axis (translation zero — spins in place). */
	private static Transformation rotationTransform(float angle) {
		return new Transformation(
				new Vector3f(0f, 0f, 0f),
				new AxisAngle4f(angle, 0f, 1f, 0f),
				DISPLAY_SCALE,
				new AxisAngle4f(0f, 0f, 0f, 1f));
	}

	/** Slowly spin every relic + altar wool display about the Y axis. Runs until {@link #forceCleanup}. */
	private static void startRotation() {
		if(rotationTask != null && !rotationTask.isCancelled()) return;
		rotationTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), () -> {
			rotationAngle += ROTATION_STEP;
			if(rotationAngle > (float) (2 * Math.PI)) rotationAngle -= (float) (2 * Math.PI);
			Transformation t = rotationTransform(rotationAngle);
			for(ItemDisplay d : statueDisplays.values()) if(d != null && d.isValid()) d.setTransformation(t);
			for(ItemDisplay d : altarWoolDisplays) if(d != null && d.isValid()) d.setTransformation(t);
		}, 1L, 1L);
	}

	/** Grow the Wither King 0.1 scale/tick from its spawn scale (0.1) up to {@link #WITHER_KING_SCALE}, then stop. */
	private static void startWitherKingGrowth() {
		if(growthTask != null && !growthTask.isCancelled()) growthTask.cancel();
		growthTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), () -> {
			if(witherKing == null || !witherKing.isValid()) {
				if(growthTask != null) growthTask.cancel();
				growthTask = null;
				return;
			}
			double scale = witherKing.getAttribute(Attribute.SCALE).getBaseValue();
			if(scale >= WITHER_KING_SCALE) {
				witherKing.getAttribute(Attribute.SCALE).setBaseValue(WITHER_KING_SCALE);
				growthTask.cancel();
				growthTask = null;
				return;
			}
			witherKing.getAttribute(Attribute.SCALE).setBaseValue(Math.min(WITHER_KING_SCALE, scale + 0.1));
		}, 1L, 1L);
	}

	/** The relic this Interaction entity represents, or null if it's not a (still-present) relic interaction. */
	public static String relicColorForInteraction(Interaction interaction) {
		Relic r = interactionRelic.get(interaction.getUniqueId());
		return r == null ? null : r.name();
	}

	/** The relic placed by right-clicking the altar block at (x,z), or null if not an altar block. */
	public static String altarColorAt(int x, int z) {
		Relic r = Relic.altarAt(x, z);
		return r == null ? null : r.name();
	}

	/** The relic color of an item that is genuinely a relic (correct wool AND "[Color] Relic" name), else null. */
	public static String relicColorOfItem(ItemStack item) {
		if(item == null) return null;
		Relic r = Relic.fromWool(item.getType());
		if(r == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
		return r.itemName().equals(item.getItemMeta().getDisplayName()) ? r.name() : null;
	}

	/** True if the player is already carrying a relic in slot 8 — used to block picking up a second. */
	public static boolean isHoldingRelic(Player p) {
		return relicColorOfItem(p.getInventory().getItem(8)) != null;
	}

	/** Picks up the given relic (by {@link Relic} name) — called from the relic Interaction right-click. */
	public static void pickUpRelic(Player p, String color) {
		// One relic at a time — ignore if this player is already carrying one (and hasn't placed it).
		if(isHoldingRelic(p)) return;
		Relic relic = Relic.valueOf(color);

		ItemStack itemStack = new ItemStack(relic.wool);
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(relic.itemName());
		itemStack.setItemMeta(meta);

		// Remove this relic's display + interaction so it can't be picked up twice.
		removeRelicEntities(relic);

		Bukkit.broadcastMessage(ChatColor.GOLD + Utils.getRealName(p) + ChatColor.GREEN + " picked up the " + relic.itemName() + ChatColor.GREEN + "!");
		Utils.timer(ChatColor.GREEN + "Picked up in " + formatTick());
		p.getInventory().setItem(8, itemStack);
		instructions.Actions.setHotbarSlot(p, 8);
		Utils.playGlobalSound(Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);
	}

	/** Places the held relic on its altar (by {@link Relic} name) — called from the altar block right-click. */
	public static void placeRelic(Player p, String color) {
		Relic relic = Relic.valueOf(color);
		if(placedRelics.contains(relic)) return;
		placedRelics.add(relic);

		// Wool ItemDisplay centered on the altar at y=8.5 — same 0.66666³ cube, same spin as the statue relics.
		Location woolLoc = new Location(world, relic.altarX + 0.5, 8.5, relic.altarZ + 0.5);
		ItemDisplay wool = world.spawn(woolLoc, ItemDisplay.class, d -> {
			d.setItemStack(new ItemStack(relic.wool));
			d.setTransformation(rotationTransform(rotationAngle));
			d.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
			d.setPersistent(true);
			d.addScoreboardTag("TASWitherKingRelic");
			d.addScoreboardTag("TASNoName");
		});
		altarWoolDisplays.add(wool);

		// Clear the relic out of hand (back to the SkyBlock-menu nether star).
		p.getInventory().setItem(8, FakePlayerInventory.getSkyBlockItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Menu (Click)", ""));
		instructions.Actions.setHotbarSlot(p, 8);
		Utils.playGlobalSound(Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);

		if(placedRelics.size() == 5) {
			Utils.timer(ChatColor.GOLD + "" + ChatColor.BOLD + "All Relics Placed " + ChatColor.RESET + "in " + formatTick());
			// Delay the intro 10 ticks after the last relic is placed.
			Utils.scheduleTask(WitherKing::startWitherKingIntro, 10);
		} else {
			Utils.timer(relic.chatColor + relic.label + " Relic placed (" + placedRelics.size() + "/5) in " + formatTick());
		}
	}

	// ============================== Wither King intro ==============================

	private static void startWitherKingIntro() {
		for(int i = 20; i <= 101; i += 20) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_IRON_GOLEM_REPAIR, 2.0f, 0.5f), i);
		}
		for(int i = 20; i <= 261; i += 20) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f), i);
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f), i);
		}
		Utils.scheduleTask(() -> sendChatMessage("You... again?"), 100);
		// Spawn the Wither King 20 ticks before its "I no longer wish to fight" line, at scale 0.1, and grow it
		// 0.1 scale/tick up to its full scale of 4 — it rises into existence over the lead-up to the line.
		Utils.scheduleTask(() -> {
			witherKing = (Wither) world.spawnEntity(new Location(world, 54.5, 6, 32.5, 0f, 0f), EntityType.WITHER);
			witherKing.setAI(false);
			witherKing.setSilent(true);
			witherKing.setPersistent(true);
			witherKing.setRemoveWhenFarAway(false);
			witherKing.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + ChatColor.MAGIC + "Wither-King" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.YELLOW + 5 + ChatColor.RED + "❤");
			witherKing.setCustomNameVisible(true);
			witherKing.getAttribute(Attribute.MAX_HEALTH).setBaseValue(5);
			witherKing.getAttribute(Attribute.ARMOR).setBaseValue(-30);
			witherKing.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(-20);
			witherKing.getAttribute(Attribute.SCALE).setBaseValue(0.1);
			witherKing.setHealth(5);
			witherKing.addScoreboardTag("TASWither");
			witherKing.addScoreboardTag("TASWitherKing");
			WitherActions.setWitherArmor(witherKing, true);
			startWitherKingGrowth();

			Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(witherKing, ChatColor.MAGIC + "Wither-King"), 1);
		}, 140);
		Utils.scheduleTask(() -> {
			sendChatMessage("I no longer wish to fight, but I know that will not stop you.");
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 160);
		Utils.scheduleTask(() -> sendChatMessage("We will decide it all, here, now."), 220);
		// First three dragons spawn on predetermined timers (Soul + Ice together, then Flame).
		// Power and Apex are NOT timer-spawned — they fire from handleDragonKilled when the last living dragon dies.
		Utils.scheduleTask(() -> spawnDragon("purple"), 260); // Soul
		Utils.scheduleTask(() -> spawnDragon("blue"), 260);   // Ice
		Utils.scheduleTask(() -> spawnDragon("orange"), 600); // Flame (last timer dragon)
	}

	/** Colored bold display name for a dragon color key (e.g. "orange" → gold-bold "Flame Dragon"). */
	private static String dragonName(String color) {
		return switch(color) {
			case "orange" -> ChatColor.GOLD + "" + ChatColor.BOLD + "Flame Dragon";
			case "green" -> ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Apex Dragon";
			case "red" -> ChatColor.RED + "" + ChatColor.BOLD + "Power Dragon";
			case "blue" -> ChatColor.AQUA + "" + ChatColor.BOLD + "Ice Dragon";
			case "purple" -> ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Soul Dragon";
			default -> ChatColor.GRAY + "" + ChatColor.BOLD + "Unknown Dragon";
		};
	}

	/** The color key for a spawned dragon entity (reverse lookup of {@link #dragons}), or "" if unknown. */
	private static String colorOf(EnderDragon dragon) {
		for(Map.Entry<String, EnderDragon> en : dragons.entrySet()) {
			if(en.getValue() != null && en.getValue().getUniqueId().equals(dragon.getUniqueId())) return en.getKey();
		}
		return "";
	}

	public static void spawnDragon(String color) {
		for(int i = 0; i < 81; i += 20) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f), i);
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f), i);
		}
		String dragonName = dragonName(color);
		Location spawnLocation = switch(color) {
			case "orange" -> new Location(world, 86.5, 15, 56.5, 180f, 0f);
			case "green" -> new Location(world, 26.5, 15, 94.5, 0f, 0f);
			case "red" -> new Location(world, 26.5, 15, 59.5, 45f, 0f);
			case "blue" -> new Location(world, 85.5, 15, 94.5, 180f, 0f);
			case "purple" -> new Location(world, 56.5, 14, 126.5, 0f, 0f);
			default -> new Location(world, 54.5, 15, 76.5);
		};

		Bukkit.broadcastMessage(ChatColor.YELLOW + "The " + dragonName + ChatColor.RESET + ChatColor.YELLOW + " is spawning!");
		Utils.timer(ChatColor.YELLOW + "Triggered in " + formatTick());

		// Spawn on the boss lane (start of the target tick, before player choreography) — NOT a raw scheduleTask,
		// which fires mid-tick AFTER the players' beams, eating the spawn-tick of damage. This lets a beam on the
		// same tick the dragon appears actually hit it.
		BossScheduler.schedule(() -> {
			// Time the dragon from when it actually spawns in, not when the spawn animation began.
			dragonSpawnTick.put(color, Utils.phaseTick());
			Utils.timer(ChatColor.YELLOW + dragonName + ChatColor.RESET + ChatColor.YELLOW + " spawned in " + formatTick());
			EnderDragon dragon = (EnderDragon) world.spawnEntity(spawnLocation, EntityType.ENDER_DRAGON);
			dragons.put(color, dragon);
			dragon.setSilent(true);
			dragon.setPersistent(true);
			dragon.setRemoveWhenFarAway(false);
			dragon.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + dragonName + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.YELLOW + "1B" + ChatColor.RED + "❤");
			dragon.setCustomNameVisible(true);
			dragon.getAttribute(Attribute.MAX_HEALTH).setBaseValue(800);
			dragon.getAttribute(Attribute.ARMOR).setBaseValue(0);
			dragon.setHealth(800);
			dragon.addScoreboardTag("WitherKingDragon");
			aliveCount++;
			if(color.equals("orange")) flameSpawned = true; // last timer dragon is now alive
			Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 1.0f);
		}, 100);
	}

	/**
	 * Called the tick a Wither-King dragon's HP reaches 0 (from {@code Utils.hurtEntity}'s EnderDragon chokepoint).
	 * Forces the death animation in place, decrements the Wither King's HP, and — once all timer dragons have
	 * spawned and the arena is clear — begins the next event dragon's spawn animation (Power, then Apex). When the
	 * final dragon (Apex) dies, kicks off the death sequence.
	 */
	public static void handleDragonKilled(EnderDragon dragon) {
		if(dragon == null || dyingDragons.contains(dragon.getUniqueId())) return;
		dyingDragons.add(dragon.getUniqueId());
		String color = colorOf(dragon);
		int elapsed = Utils.phaseTick() - dragonSpawnTick.getOrDefault(color, Utils.phaseTick());
		Utils.timer(ChatColor.YELLOW + dragonName(color) + ChatColor.RESET + ChatColor.YELLOW + " killed in " + formatDragonKillTick(elapsed));
		instaKillDragon(dragon);
		aliveCount--;

		boolean isFinalDragon = eventQueue.isEmpty() && aliveCount <= 0;
		if(isFinalDragon) {
			playDragonDeathSound(false);
			deathSequence();
		} else {
			playDragonDeathSound(true);
			// Begin the next event dragon the tick the last living dragon is killed (guarded on all timer
			// dragons having spawned, so an early Soul/Ice death before Flame appears can't trigger it).
			if(flameSpawned && aliveCount <= 0 && !eventQueue.isEmpty()) {
				spawnDragon(eventQueue.poll());
			}
		}
	}

	/**
	 * Forces an Ender Dragon to play its death animation in place instead of flying
	 * to 0,0,0 (the default portal target of DragonDeathPhase). Reflects
	 * DragonDeathPhase.targetLocation to the dragon's current position and kicks
	 * dragonDeathTime to 1 so the death animation starts this tick.
	 * true if {@code e} is a Wither-King dragon currently playing its death animation. instaKillDragon pins the
	 * dragon's HP to 1 for the animation, so isDead()/getHealth() can't detect this — the UUID set is authoritative. */
	public static boolean isDyingDragon(Entity e) {
		return e != null && dyingDragons.contains(e.getUniqueId());
	}

	public static void instaKillDragon(EnderDragon dragon) {
		if(!(dragon instanceof CraftEnderDragon craftDragon)) return;
		net.minecraft.world.entity.boss.enderdragon.EnderDragon nmsDragon = craftDragon.getHandle();
		nmsDragon.getPhaseManager().setPhase(EnderDragonPhase.DYING);
		DragonPhaseInstance phase = nmsDragon.getPhaseManager().getCurrentPhase();
		if(phase instanceof DragonDeathPhase deathPhase) {
			try {
				Field targetField = DragonDeathPhase.class.getDeclaredField("targetLocation");
				targetField.setAccessible(true);
				Location l = dragon.getLocation();
				targetField.set(deathPhase, new Vec3(l.getX(), l.getY(), l.getZ()));
			} catch(ReflectiveOperationException e) {
				Bukkit.getLogger().warning("Failed to force dragon death targetLocation: " + e.getMessage());
			}
		}
		nmsDragon.setDeltaMovement(Vec3.ZERO);
		nmsDragon.setHealth(1.0F);
		try {
			Field deathTimeField = nmsDragon.getClass().getDeclaredField("dragonDeathTime");
			deathTimeField.setAccessible(true);
			deathTimeField.setInt(nmsDragon, 1);
		} catch(ReflectiveOperationException e) {
			Bukkit.getLogger().warning("Failed to force dragon dragonDeathTime: " + e.getMessage());
		}
	}

	public static void playDragonDeathSound(boolean sendMessage) {
		for(int i = 0; i < 181; i += 10) {
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f), i);
		}
		Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f), 190);
		if(witherKing.getHealth() == 1) {
			witherKing.setHealth(0.001);
		} else {
			witherKing.setHealth(witherKing.getHealth() - 1);
		}
		if(sendMessage) {
			sendChatMessage(dragonDieMessage[random.nextInt(dragonDieMessage.length)]);
		}
	}

	// ============================== Death / end ==============================

	public static void deathSequence() {
		sendChatMessage("Incredible.  You did what I couldn't do myself.");
		Utils.scheduleTask(() -> sendChatMessage("In a way, I should thank you.  I lost all hope centuries ago that it would ever end."), 60);
		Utils.scheduleTask(() -> sendChatMessage("I hope you'll become the Heroes I could never be."), 120);
		Utils.scheduleTask(() -> sendChatMessage("So long champions of this mad world!"), 180);
		Utils.scheduleTask(() -> sendChatMessage("My strengths are depleting.  This... this is it."), 240);
		Utils.scheduleTask(() -> { if(witherKing != null && witherKing.isValid()) witherKing.remove(); }, 300);

		Utils.scheduleTask(WitherKing::printFinalMessage, END_DELAY_TICKS);
	}

	/** Final congratulation: hardcoded splits for a TAS run, live ticks for a practice run, a short line for
	 *  a standalone Wither-King practice. */
	private static void printFinalMessage() {
		WitherActions.recordSplit("WitherKing", Utils.runTick());

		if(!WitherActions.isPracticeMode()) {
			printTasScoreboard();
		} else if(standalone) {
			Bukkit.broadcastMessage(ChatColor.GREEN + "You defeated the " + ChatColor.RED + ChatColor.BOLD + "Wither King" + ChatColor.RESET + ChatColor.GREEN
					+ " in " + formatWithSpaces(Utils.phaseTick()) + " ticks!  Try doing the full run to see how you fare.");
			Utils.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP);
			Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f), 1);
		} else {
			printPracticeScoreboard();
		}
	}

	/** The hardcoded TAS victory screen. */
	private static void printTasScoreboard() {
		Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
		Bukkit.broadcastMessage("                " + ChatColor.RED + "Master Mode The Catacombs " + ChatColor.DARK_GRAY + "- " + ChatColor.YELLOW + "Floor VII");
		Bukkit.broadcastMessage("");
		Bukkit.broadcastMessage("                           " + ChatColor.WHITE + "Team Score: " + ChatColor.GREEN + "308 " + ChatColor.WHITE + "(" + ChatColor.AQUA + ChatColor.BOLD + "S+" + ChatColor.RESET + ChatColor.WHITE + ")");
		Bukkit.broadcastMessage(" " + ChatColor.RED + "☠ " + ChatColor.YELLOW + "Defeated " + ChatColor.RED + "Maxor, Storm, Goldor, and Necron " + ChatColor.YELLOW + "in " + ChatColor.GREEN + "4404 ticks");
		Bukkit.broadcastMessage("                         " + ChatColor.GREEN + "220.20 seconds | 3:40.20");
		Bukkit.broadcastMessage("");
		Bukkit.broadcastMessage("                              " + ChatColor.GOLD + "> " + ChatColor.YELLOW + ChatColor.BOLD + "EXTRA INFO " + ChatColor.RESET + ChatColor.GOLD + "<");
		Bukkit.broadcastMessage("                                   " + ChatColor.GREEN + ChatColor.BOLD + "SPLITS");
		Bukkit.broadcastMessage("    " + ChatColor.BLUE + ChatColor.BOLD + "Clear" + ChatColor.RESET + ChatColor.WHITE + ": 738 ticks | " + ChatColor.AQUA + ChatColor.BOLD + "Maxor" + ChatColor.RESET + ChatColor.WHITE + ": 500 ticks | " + ChatColor.RED + ChatColor.BOLD + "Storm" + ChatColor.RESET + ChatColor.WHITE + ": 860 ticks");
		Bukkit.broadcastMessage(" " + ChatColor.YELLOW + ChatColor.BOLD + "Terminals" + ChatColor.RESET + ChatColor.WHITE + ": 200 ticks | " + ChatColor.GOLD + ChatColor.BOLD + "Goldor" + ChatColor.RESET + ChatColor.WHITE + ": 104 ticks | " + ChatColor.DARK_RED + ChatColor.BOLD + "Necron" + ChatColor.RESET + ChatColor.WHITE + ": 600 ticks");
		Bukkit.broadcastMessage("                         " + ChatColor.GRAY + ChatColor.BOLD + "Wither King" + ChatColor.RESET + ChatColor.WHITE + ": 1029 ticks");
		Bukkit.broadcastMessage("");
		Bukkit.broadcastMessage("     " + ChatColor.GREEN + ChatColor.BOLD + "TAS by " + ChatColor.RESET + ChatColor.AQUA + "Stradivarius Violin" + ChatColor.GREEN + ", also known as " + ChatColor.AQUA + "Beethoven_");
		Bukkit.broadcastMessage("    " + ChatColor.RED + ChatColor.BOLD + "YOUTUBE" + ChatColor.AQUA + ": https://www.youtube.com/@Stradivarius_Violin");
		Bukkit.broadcastMessage("               " + ChatColor.BLUE + ChatColor.BOLD + "DISCORD" + ChatColor.AQUA + ": https://discord.gg/gNfPwa8");
		Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

		Utils.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP);
		Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f), 1);
	}

	/** The victory screen for a chained practice run (full boss / full run): real splits from this run, in the same
	 *  layout as the TAS scoreboard. Full run → Clear/Maxor/Storm, Terminals/Goldor/Necron, Wither King. Boss-only →
	 *  Maxor/Storm/Terminals, Goldor/Necron/Wither King, then "Clear: Skipped". */
	private static void printPracticeScoreboard() {
		int overall = Utils.runTick();
		boolean clearRan = WitherActions.getSplitEnd("Clear") != null;

		// Compute each section's split (end − previous end) in run order.
		Map<String, Integer> sp = new HashMap<>();
		int prev = 0;
		for(String s : new String[]{"Clear", "Maxor", "Storm", "Terminals", "Goldor", "Necron", "WitherKing"}) {
			Integer end = WitherActions.getSplitEnd(s);
			if(end == null) continue;
			sp.put(s, end - prev);
			prev = end;
		}

		Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
		Bukkit.broadcastMessage("                " + ChatColor.RED + "Master Mode The Catacombs " + ChatColor.DARK_GRAY + "- " + ChatColor.YELLOW + "Floor VII");
		Bukkit.broadcastMessage("");
		Bukkit.broadcastMessage(" " + ChatColor.RED + "☠ " + ChatColor.YELLOW + "Defeated " + ChatColor.RED + "Maxor, Storm, Goldor, and Necron " + ChatColor.YELLOW + "in " + ChatColor.GREEN + overall + " ticks");
		Bukkit.broadcastMessage("                         " + ChatColor.GREEN + formatTime(overall));
		Bukkit.broadcastMessage("");
		Bukkit.broadcastMessage("                              " + ChatColor.GOLD + "> " + ChatColor.YELLOW + ChatColor.BOLD + "EXTRA INFO " + ChatColor.RESET + ChatColor.GOLD + "<");
		Bukkit.broadcastMessage("                                   " + ChatColor.GREEN + ChatColor.BOLD + "SPLITS");
		// Build the ordered split segments, then pack them onto as many centered lines as the chat width allows.
		List<String> segs = new ArrayList<>();
		if(clearRan) {
			segs.add(seg(ChatColor.BLUE, "Clear", sp.get("Clear")));
			segs.add(seg(ChatColor.AQUA, "Maxor", sp.get("Maxor")));
			segs.add(seg(ChatColor.RED, "Storm", sp.get("Storm")));
			segs.add(seg(ChatColor.YELLOW, "Terminals", sp.get("Terminals")));
			segs.add(seg(ChatColor.GOLD, "Goldor", sp.get("Goldor")));
			segs.add(seg(ChatColor.DARK_RED, "Necron", sp.get("Necron")));
			segs.add(seg(ChatColor.GRAY, "Wither King", sp.get("WitherKing")));
		} else {
			segs.add(seg(ChatColor.AQUA, "Maxor", sp.get("Maxor")));
			segs.add(seg(ChatColor.RED, "Storm", sp.get("Storm")));
			segs.add(seg(ChatColor.YELLOW, "Terminals", sp.get("Terminals")));
			segs.add(seg(ChatColor.GOLD, "Goldor", sp.get("Goldor")));
			segs.add(seg(ChatColor.DARK_RED, "Necron", sp.get("Necron")));
			segs.add(seg(ChatColor.GRAY, "Wither King", sp.get("WitherKing")));
			segs.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Clear" + ChatColor.RESET + ChatColor.WHITE + ": Skipped");
		}
		for(String line : packLines(segs)) {
			Bukkit.broadcastMessage(ChatFont.centerPad(line));
		}
		Bukkit.broadcastMessage("");
		Bukkit.broadcastMessage(ChatFont.centerPad(ChatColor.GREEN + "" + ChatColor.BOLD + "PLAYERS"));
		for(String line : packPlayerLines()) {
			Bukkit.broadcastMessage(ChatFont.centerPad(line));
		}
		Bukkit.broadcastMessage("");
		Bukkit.broadcastMessage("   " + ChatColor.GREEN + ChatColor.BOLD + "Plugin by " + ChatColor.RESET + ChatColor.AQUA + "Stradivarius Violin" + ChatColor.GREEN + ", also known as " + ChatColor.AQUA + "Beethoven_");
		Bukkit.broadcastMessage("    " + ChatColor.RED + ChatColor.BOLD + "YOUTUBE" + ChatColor.AQUA + ": https://www.youtube.com/@Stradivarius_Violin");
		Bukkit.broadcastMessage("               " + ChatColor.BLUE + ChatColor.BOLD + "DISCORD" + ChatColor.AQUA + ": https://discord.gg/gNfPwa8");
		Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

		Utils.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP);
		Utils.scheduleTask(() -> Utils.playGlobalSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f), 1);
	}

	/** "<color><b>Label</b></color>: N ticks" split segment for the practice scoreboard. */
	private static String seg(ChatColor color, String label, Integer ticks) {
		return color + "" + ChatColor.BOLD + label + ChatColor.RESET + ChatColor.WHITE + ": " + (ticks == null ? "—" : formatWithSpaces(ticks)) + " ticks";
	}

	/** White " | " separator between scoreboard segments. */
	private static final String SEG_SEP = ChatColor.WHITE + " | ";

	/** Greedily pack segments onto centered lines: as many per line as fit {@link ChatFont#MAX_WIDTH}, then wrap. */
	private static List<String> packLines(List<String> segments) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for(String s : segments) {
			if(line.isEmpty()) {
				line.append(s);
				continue;
			}
			String candidate = line + SEG_SEP + s;
			if(ChatFont.width(candidate) > ChatFont.WRAP_WIDTH) {
				lines.add(line.toString());
				line = new StringBuilder(s);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if(!line.isEmpty()) lines.add(line.toString());
		return lines;
	}

	/** One name per online player (spectators excluded) — gold only if they stayed in Adventure Mode the whole run
	 *  (a minor anti-cheat), white if they changed game mode at any point. */
	private static List<String> packPlayerLines() {
		List<String> names = new ArrayList<>();
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.getGameMode() == GameMode.SPECTATOR || commands.Spectate.isSpectating(p)) continue;
			ChatColor color = WitherActions.stayedAdventure(p) ? ChatColor.GOLD : ChatColor.WHITE;
			names.add(color + Utils.getRealName(p));
		}
		if(names.isEmpty()) names.add(ChatColor.GRAY + "(none)");
		return packLines(names);
	}

	// ============================== Cleanup / helpers ==============================

	/** Remove all relic/altar entities + any stray Wither-King / dragon entities, and reset all per-fight state. */
	public static void forceCleanup(World w) {
		world = w;
		if(rotationTask != null && !rotationTask.isCancelled()) {
			rotationTask.cancel();
			rotationTask = null;
		}
		rotationAngle = 0f;
		if(growthTask != null && !growthTask.isCancelled()) {
			growthTask.cancel();
			growthTask = null;
		}
		if(witherKing != null) {
			witherKing.remove();
			witherKing = null;
		}
		for(EnderDragon dragon : dragons.values()) {
			if(dragon != null && dragon.isValid()) dragon.remove();
		}
		dragons.clear();
		dyingDragons.clear();
		dragonSpawnTick.clear();

		for(ItemDisplay d : statueDisplays.values()) if(d != null && d.isValid()) d.remove();
		for(Interaction i : statueInteractions.values()) if(i != null && i.isValid()) i.remove();
		for(ItemDisplay d : altarWoolDisplays) if(d != null && d.isValid()) d.remove();
		statueDisplays.clear();
		statueInteractions.clear();
		altarWoolDisplays.clear();
		interactionRelic.clear();
		placedRelics.clear();

		// Sweep any orphaned relic/dragon entities a prior run may have left behind.
		if(world != null) {
			for(org.bukkit.entity.Entity e : world.getEntities()) {
				if(e.getScoreboardTags().contains("TASWitherKingRelic") || e.getScoreboardTags().contains("WitherKingDragon")) {
					e.remove();
				}
			}
		}

		aliveCount = 0;
		flameSpawned = false;
		eventQueue.clear();
	}

	private static void removeRelicEntities(Relic relic) {
		ItemDisplay display = statueDisplays.remove(relic);
		if(display != null && display.isValid()) display.remove();
		Interaction interaction = statueInteractions.remove(relic);
		if(interaction != null) {
			interactionRelic.remove(interaction.getUniqueId());
			if(interaction.isValid()) interaction.remove();
		}
	}

	private static String formatTime(int ticks) {
		double secs = ticks / 20.0;
		int mins = (int) (secs / 60);
		double rem = secs - mins * 60.0;
		return String.format("%.2f seconds | %d:%05.2f", secs, mins, rem);
	}

	/** Render an int with space thousands separators, e.g. 4404 → "4 404". */
	private static String formatWithSpaces(int n) {
		StringBuilder sb = new StringBuilder();
		String s = String.valueOf(n);
		for(int i = 0; i < s.length(); i++) {
			if(i > 0 && (s.length() - i) % 3 == 0) sb.append(' ');
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}

	/** Timer line tick suffix: phase ticks since the WK phase start + the overall-run column (live in practice). */
	private static String formatTick() {
		int t = Utils.phaseTick();
		int overall = WitherActions.isPracticeMode() ? Utils.runTick() : PRE_WITHERKING_TICKS + t;
		return ChatColor.GREEN + String.format("%s ticks (%.2f seconds) | Overall: %s ticks (%.2f seconds)",
				formatWithSpaces(t), t / 20.0, formatWithSpaces(overall), overall / 20.0);
	}

	/** Dragon-kill tick suffix: ticks since that dragon spawned, then the Wither-King section clock, then overall. */
	private static String formatDragonKillTick(int dragonElapsed) {
		int phase = Utils.phaseTick();
		int overall = WitherActions.isPracticeMode() ? Utils.runTick() : PRE_WITHERKING_TICKS + phase;
		return ChatColor.GREEN + String.format("%s ticks (%.2f seconds) | Wither King: %s ticks (%.2f seconds) | Overall: %s ticks (%.2f seconds)",
				formatWithSpaces(dragonElapsed), dragonElapsed / 20.0,
				formatWithSpaces(phase), phase / 20.0,
				formatWithSpaces(overall), overall / 20.0);
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] " + ChatColor.MAGIC + "Wither-King" + ChatColor.RESET + ChatColor.RED + ": " + message);
	}
}
