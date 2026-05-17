package instructions.bosses.necron;

import instructions.Server;
import instructions.bosses.WitherActions;
import instructions.bosses.WitherLord;
import instructions.bosses.witherking.WitherKing;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.Random;

/**
 * Necron on the real Hypixel server has the same live-chase aggro as Maxor
 * (see {@link WitherActions#setWitherAggro} with stopDistance≈3.0, yOffset≈1.0).
 * The TAS keeps him stationary for choreography; this is a simplification, not
 * a faithful behavior.
 */
@SuppressWarnings("DataFlowIssue")
public final class Necron extends WitherLord {
	public static final Necron INSTANCE = new Necron();

	private static final int PRE_NECRON_TICKS = 2766;
	private static final Random random = new Random();
	private static final String[] FRENZY_START_MESSAGES = {"Sometimes when you have a problem, you just need to destroy it all and start again.", "WITNESS MY RAW NUCLEAR POWER!"};
	private static final String[] FRENZY_END_MESSAGES = {"ARGH!", "Let's make some space!"};

	private Necron() {
		register(this);
	}

	/** Static facade for the boss-chain. */
	public static void necronInstructions(World world, boolean doContinue) {
		INSTANCE.start(world, doContinue);
	}

	@Override protected String name() { return "Necron"; }
	@Override protected String displayName() { return "Necron"; }
	@Override protected Location spawnLocation() { return new Location(world, 54.5, 66, 76.5, 0f, 0f); }
	@Override protected double maxHealth() { return 1000; }
	@Override protected String displayHealth() { return "1.4B"; }
	@Override protected int previousTicks() { return PRE_NECRON_TICKS; }

	@Override
	protected void resetState() {
		// Necron has no per-fight state beyond what the base class handles.
	}

	@Override
	protected void onStart() {
		sendChatMessage("You went further than any human before, congratulations.");
		Utils.scheduleTask(() -> {
			sendChatMessage("I'm afraid your journey ends now.");
			destroyPlatform();
		}, 60);
		Utils.scheduleTask(() -> sendChatMessage("Goodbye."), 120);
		Utils.scheduleTask(() -> setArmor(false), 160);
		// first frenzy
		Utils.scheduleTask(() -> {
			setArmor(true);
			frenzy();
		}, 161);
		Utils.scheduleTask(() -> boss.setHealth(560), 162);
		Utils.scheduleTask(() -> sendChatMessage("That's a very impressive trick.  I guess I'll have to handle this myself."), 180);
		// damageable on tick 302
		// blow up platform
		Utils.scheduleTask(() -> {
			setArmor(true);
			destroyPlatform();
		}, 307);
		Utils.scheduleTask(() -> boss.setHealth(175), 308);
		// damagable on tick 368 (mage one beam)
		Utils.scheduleTask(() -> {
			setArmor(false);
			sendChatMessage(FRENZY_END_MESSAGES[random.nextInt(FRENZY_END_MESSAGES.length)]);
		}, 367);
		// second frenzy
		Utils.scheduleTask(() -> {
			setArmor(true);
			frenzy();
		}, 368);
		Utils.scheduleTask(() -> boss.setHealth(35), 369);
		// damageable on tick 508
		// die on tick 509
		Utils.scheduleTask(() -> {
			sendChatMessage("All this, for nothing...");
			Server.playWitherDeathSound(boss);
			Bukkit.broadcastMessage(ChatColor.GREEN + "Necron killed in 509 ticks (25.45 seconds) | Overall: 3 275 ticks (163.75 seconds)");
		}, 509);
		Utils.scheduleTask(() -> sendChatMessage("I understand your words now, my master."), 569);
		Utils.scheduleTask(() -> {
			// note: in skytils, the Necron timer ends 2 seconds too early, thus making Wither King start 2 seconds too early.  The timing in this TAS fixes this.  To compare to Skytils time, subtract 2 seconds here and add 2 seconds to Wither King time.
			Bukkit.broadcastMessage(ChatColor.GREEN + "Necron finished in 609 ticks (30.45 seconds) | Overall: 3 375 ticks (168.75 seconds)");
			chainNext(doContinue);
		}, 609);
		Utils.scheduleTask(() -> sendChatMessage("The Catacombs... are no more."), 629);

		/*
		 * note: in normal floor 7, Wither EHP is ridiculously low to the point that true one-tick is possible for all partitions
		 * in addition to negating the need to debuff, given this information, the following timesaves will occur in normal floor 7
		 * maxor: none
		 * storm: 13 ticks (8 ticks from first crush, 5 ticks from second crush)
		 * goldor: approx. 35-40 ticks, due to not needing to debuff, as well as being able to be one-tapped the moment he is not obstructed by blocks
		 * necron: 5 ticks (between first frenzy and platform destroy)
		 * thus, a solution of ~3 260 ticks will be the perfect F7 (168.00 seconds | 2:48.00)
		 */
		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.GOLD + "Normal Floor 7 Finishes Here in 3 315 ticks (172.75 seconds | 2:52.75)"), 649);
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			WitherKing.witherKingInstructions(world);
		}
	}

	private void destroyPlatform() {
		shootFireball();
		Utils.scheduleTask(this::shootFireball, 10);
		Utils.scheduleTask(this::shootFireball, 20);
		Utils.scheduleTask(this::shootFireball, 30);
		Utils.scheduleTask(() -> {
			shootFireball();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 70 -10 120 38 -6 99 38 59 99");
		}, 40);
		Utils.scheduleTask(this::shootFireball, 50);
		Utils.scheduleTask(this::shootFireball, 60);
		Utils.scheduleTask(this::shootFireball, 70);
	}

	private void shootFireball() {
		Fireball fireball = (Fireball) world.spawnEntity(boss.getLocation().add(0, 3, 0), EntityType.FIREBALL);
		fireball.setVelocity(new Vector(0, -0.25, 1.25));
		Utils.scheduleTask(fireball::remove, 21);
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT);
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
	}

	private void frenzy() {
		sendChatMessage(FRENZY_START_MESSAGES[random.nextInt(FRENZY_START_MESSAGES.length)]);
		Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 20);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 40);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 60);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 80);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 100);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 120);
		Utils.scheduleTask(() -> {
			sendChatMessage(FRENZY_END_MESSAGES[random.nextInt(FRENZY_END_MESSAGES.length)]);
			setArmor(false);
		}, 140);
	}
}
