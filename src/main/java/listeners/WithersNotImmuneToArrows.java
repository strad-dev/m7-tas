package listeners;

import instructions.bosses.WitherActions;
import instructions.bosses.WitherLord;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import plugin.Utils;

public class WithersNotImmuneToArrows implements Listener {
	/**
	 * Vanilla blocks projectile damage on a "powered" wither (HP <= 50%) and while its
	 * invulnerability shield is up.  Arrows, including Terminator arrows, should
	 * damage a vulnerable wither at any HP. Cancel the event preemptively (LOWEST) so
	 * vanilla never gets to bounce/skip, then apply the damage manually.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onArrowHitWither(ProjectileHitEvent event) {
		if(!(event.getEntity() instanceof Arrow arrow)) return;
		if(!(event.getHitEntity() instanceof Wither wither)) return;
		if(!(arrow.getShooter() instanceof Player p)) return;

		// An arrow that actually takes health off the boss DOES set the aggro target, same as a swing; one that lands
		// for zero does not.  Damage.deal owns that call - see its javadoc, and note that the shield-up return below
		// means an arrow on an armoured boss never gets there.

		// Shield up (invulnerability ticks active) → bounce, no damage. EXCEPTION: a Terminator/Last Breath arrow
		// landing on a tick the boss was made vulnerable then re-armored within that same tick, since the live counter
		// already reads "shielded" because the arrow hit resolves after the start-of-tick boss scans, but the boss
		// WAS intended vulnerable this tick (a same-tick mage beam would connect). Honor that heartbeat-time intent.
		if(wither.getInvulnerableTicks() != 0
				&& !(arrow.getScoreboardTags().contains("TerminatorArrow") && WitherActions.wasMadeVulnerableThisTick(wither))) {
			return;
		}

		// Dying wither (any WitherLord): phase the arrow through silently, with no ding, no damage, no pierce loss.
		WitherLord activeLord = WitherLord.activeFor(wither);
		if(activeLord != null && activeLord.isDying()) {
			event.setCancelled(true);
			return;
		}

		event.setCancelled(true);
		// Clear the spawn-shield counter before damaging: vanilla WitherBoss.hurt() rejects all damage while
		// invulnerabilityTicks > 0, so on the same-tick-re-armored exception above the hit would otherwise no-op.
		// A re-armored boss's armorTask re-asserts the shield next tick, so this only lets THIS hit land.
		wither.setInvulnerableTicks(0);
		// One damage path (MAP.md §7): this used to be Bukkit's no-source wither.damage(), the only route
		// that reached the boss's clamps, which is exactly the split the unification removed.  The arrow carries
		// its own stat damage from fire time; the target half resolves here, and the deal Arrows.hit picks calls the
		// boss's clampDamage explicitly, and notes the aggro target if (and only if) health actually moves.
		damage.Arrows.hit(arrow, p, wither);
		Utils.playLocalSound(p, Sound.ENTITY_ARROW_HIT_PLAYER, 0.75f, 0.79368752611448590621283707774885f);

		int newPierce = arrow.getPierceLevel() - 1;
		if(newPierce <= 0) arrow.remove();
		else arrow.setPierceLevel(newPierce);
	}
}
