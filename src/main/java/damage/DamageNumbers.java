package damage;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
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
 * A {@link TextDisplay}, not an armour stand or a hologram plugin: it spawns at the hit with a small random offset
 * so simultaneous hits do not overlap, drifts upward, then is removed.  The drift is client-side interpolation, so
 * the whole thing costs one spawn and one remove packet.
 * <p>
 * <b>The number shown is what the target actually lost</b> - after the boss resistance and the defense divisor -
 * because that is the number that matches the health bar moving.  The pre-defense figure belongs in
 * {@code /verbose}'s breakdown.
 * <p>
 * <b>Never rounded, abbreviated or truncated.</b>  The full integer, every digit: {@code 726525143}, never
 * {@code 726.5M}.
 * <p>
 * Format is {@code ✧<digits>✧❤} for a crit, with each digit taking a random colour from white / orange / light
 * green / red - Hypixel's look, and what the template implies.  A damage KIND overrides the palette entirely, and
 * grey magic numbers drop the decoration and render as bare digits.
 */
public final class DamageNumbers {
	private DamageNumbers() {}

	/** How long one number lives.  Short and bounded: a Terminator volley plus Cleave is dozens of hits a tick. */
	private static final int LIFETIME_TICKS = 20;
	/** Hard cap on a player's concurrent displays, for the same reason. */
	private static final int MAX_PER_PLAYER = 24;
	/** How far the number drifts upward over its lifetime. */
	private static final float DRIFT = 0.75f;

	/**
	 * The random crit palette.  There is no named orange in the 16-colour set, so {@code gold} stands in, and
	 * {@code green} is the bright "light green" (dark_green is the deep one).
	 */
	private static final String[] CRIT_PALETTE = {"<white>", "<gold>", "<green>", "<red>"};

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
		Location at = target.getLocation().add(
				(RANDOM.nextDouble() - 0.5) * 1.2,
				target.getHeight() * 0.6 + (RANDOM.nextDouble() - 0.5) * 0.6,
				(RANDOM.nextDouble() - 0.5) * 1.2);

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

		// Interpolated upward drift: one transformation write, animated by the client.
		display.setInterpolationDelay(0);
		display.setInterpolationDuration(LIFETIME_TICKS);
		display.setTransformation(new Transformation(new Vector3f(0, DRIFT, 0), new AxisAngle4f(),
				new Vector3f(1, 1, 1), new AxisAngle4f()));

		// A fixed removal task per display.  A raw runTaskLater on purpose: Utils.scheduleTask is nuked by a run
		// reset, which would leave the display in the world forever.
		org.bukkit.Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			if(display.isValid()) display.remove();
			Deque<TextDisplay> live = LIVE.get(attacker.getUniqueId());
			if(live != null) live.remove(display);
		}, LIFETIME_TICKS);
	}

	/** The MiniMessage string for one number. */
	private static String format(double sbDamage, DamageKind kind) {
		String digits = Damage.integer(sbDamage);
		StringBuilder sb = new StringBuilder();
		if(kind.crit()) sb.append("<white>✧");
		String forced = kind.colour();
		for(int i = 0; i < digits.length(); i++) {
			// One roll PER DIGIT, which is the Hypixel look the format template implies.  A kind's colour
			// overrides the palette entirely, so a fire or venomous number is one solid colour.
			sb.append(forced != null ? forced : CRIT_PALETTE[RANDOM.nextInt(CRIT_PALETTE.length)]);
			sb.append(digits.charAt(i));
		}
		if(kind.crit()) sb.append("<white>✧<light_purple>❤");
		return sb.toString();
	}
}
