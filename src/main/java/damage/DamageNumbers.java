package damage;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import plugin.M7tas;
import plugin.Utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Floating damage numbers (DAMAGE_PLAN.md §7a).
 * <p>
 * A {@link TextDisplay}, not an armour stand or a hologram plugin: it spawns somewhere around the hit, holds
 * perfectly still for its lifetime, then is removed.  <b>No drift, no interpolation, no transformation</b> - a
 * number that slides upward while a dozen others spawn around it reads as smearing rather than as separate hits, so
 * the spread is spatial instead: a random point anywhere within {@value #SPAWN_RADIUS} blocks of the target's eyes.
 * <p>
 * <b>The number shown is what the target actually lost</b> - after the boss resistance and the defense divisor -
 * because that is the number that matches the health bar moving.  The pre-defense figure belongs in
 * {@code /verbose}'s breakdown.  The one exception is a boss in a feedback-only window (Goldor on patrol, Necron
 * mid-interlude), which displays the hit it would have taken while losing no health at all; see
 * {@code Damage.deal}.
 * <p>
 * <b>Never rounded, abbreviated or truncated.</b>  The full integer, every digit, thousands-separated:
 * {@code 726,525,143}, never {@code 726.5M}.
 * <p>
 * Format is {@code ✧<digits>✧❤} for a crit, with the digits taking their colours from a FIXED cadence rather than a
 * random roll, so two identical hits render identically.  A damage KIND overrides the cadence entirely, and grey
 * magic numbers drop the decoration and render as bare digits.
 */
public final class DamageNumbers {
	private DamageNumbers() {}

	/** How long one number lives.  Short and bounded: a Terminator volley plus Cleave is dozens of hits a tick. */
	private static final int LIFETIME_TICKS = 20;
	/**
	 * Hard cap on a player's concurrent displays.  Every damage instance gets its own number - an Archer's Terminator
	 * volley is three arrows, each resolving separately, and a Berserk's swing is one hit plus a Cleave sweep plus
	 * ten Fire Aspect and Venomous ticks - so with a 20-tick life the honest steady state is dozens, and the old cap
	 * of 24 was evicting a volley's own earlier numbers before anyone could read them.  Still a cap, because this is
	 * an entity count: nothing here may grow without a bound.
	 */
	private static final int MAX_PER_PLAYER = 100;
	/** How far from the target's EYES a number may spawn, in any direction. */
	private static final double SPAWN_RADIUS = 1.5;
	/** Random points to try before falling back to the eye location itself. */
	private static final int SPAWN_ATTEMPTS = 12;

	/**
	 * The crit digit cadence, walked one entry per DIGIT and wrapping once it runs out.  There is no named orange in
	 * the 16-colour set, so {@code gold} stands in, and {@code green} is the bright "light green" (dark_green is the
	 * deep one).
	 */
	private static final String[] CRIT_CADENCE = {"<white>", "<green>", "<gold>", "<red>", "<red>", "<gold>",
			"<green>", "<white>", "<green>", "<gold>", "<red>"};

	private static final Random RANDOM = new Random();
	private static final Map<UUID, Deque<TextDisplay>> LIVE = new HashMap<>();

	/** Remove every live number.  Called at run start so a previous run's stragglers cannot linger. */
	public static void reset() {
		for(Deque<TextDisplay> q : LIVE.values()) {
			for(TextDisplay d : q) if(d.isValid()) d.remove();
		}
		LIVE.clear();
	}

	/**
	 * Show one number.
	 *
	 * @param sbDamage the damage the target ACTUALLY lost, in SkyBlock units (post-resistance, post-defense)
	 */
	public static void show(LivingEntity target, double sbDamage, DamageKind kind, Player attacker) {
		if(target == null || sbDamage <= 0 || attacker == null) return;
		Location at = spawnPoint(target);

		Deque<TextDisplay> q = LIVE.computeIfAbsent(attacker.getUniqueId(), k -> new ArrayDeque<>());
		while(q.size() >= MAX_PER_PLAYER) {
			TextDisplay oldest = q.pollFirst();
			if(oldest != null && oldest.isValid()) oldest.remove();
		}

		TextDisplay display = target.getWorld().spawn(at, TextDisplay.class, d -> {
			d.text(Utils.msg(format(sbDamage, kind)));
			d.setBillboard(Display.Billboard.CENTER);
			d.setSeeThrough(false);
			// A fully transparent background, so the number reads as text rather than as a chat bubble.
			d.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
			d.setShadowed(true);
			d.setViewRange(0.4f);
			d.addScoreboardTag("TASNoName");
			d.addScoreboardTag("TASDamageNumber");
		});
		q.addLast(display);

		// A fixed removal task per display.  A raw runTaskLater on purpose: Utils.scheduleTask is nuked by a run
		// reset, which would leave the display in the world forever.
		org.bukkit.Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			if(display.isValid()) display.remove();
			Deque<TextDisplay> live = LIVE.get(attacker.getUniqueId());
			if(live != null) live.remove(display);
		}, LIFETIME_TICKS);
	}

	/**
	 * Where one number goes: any point within {@link #SPAWN_RADIUS} of the target's eyes, in any direction, as long
	 * as it is not inside a block.  Numbers no longer move, so the whole spread has to come from the spawn point,
	 * and a point buried in the arena floor or a wall would render a number nobody can read.
	 */
	private static Location spawnPoint(LivingEntity target) {
		Location eyes = target.getEyeLocation();
		for(int i = 0; i < SPAWN_ATTEMPTS; i++) {
			// A uniform point in the sphere: a random direction, with the radius taken through a cube root so the
			// points do not bunch up around the centre.
			double theta = RANDOM.nextDouble() * Math.PI * 2;
			double y = RANDOM.nextDouble() * 2 - 1;
			double ring = Math.sqrt(1 - y * y);
			double r = SPAWN_RADIUS * Math.cbrt(RANDOM.nextDouble());
			Location at = eyes.clone().add(r * ring * Math.cos(theta), r * y, r * ring * Math.sin(theta));
			if(at.getBlock().isPassable()) return at;
		}
		return eyes;
	}

	/** The MiniMessage string for one number. */
	private static String format(double sbDamage, DamageKind kind) {
		String digits = Damage.integer(sbDamage);
		StringBuilder sb = new StringBuilder();
		if(kind.crit()) sb.append("<white>✧");
		String forced = kind.colour();
		int step = 0;
		for(int i = 0; i < digits.length(); i++) {
			char c = digits.charAt(i);
			// The cadence advances on DIGITS only: a thousands separator carries the colour of the digit before it,
			// so inserting commas cannot shift the pattern the digits themselves are drawn in.
			if(c != ',') {
				sb.append(forced != null ? forced : CRIT_CADENCE[step % CRIT_CADENCE.length]);
				step++;
			}
			sb.append(c);
		}
		if(kind.crit()) sb.append("<white>✧<light_purple>❤");
		return sb.toString();
	}
}
