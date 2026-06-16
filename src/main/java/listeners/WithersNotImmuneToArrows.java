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

@SuppressWarnings("DataFlowIssue")
public class WithersNotImmuneToArrows implements Listener {
	/**
	 * Vanilla blocks projectile damage on a "powered" wither (HP <= 50%) and while its
	 * invulnerability shield is up. We want arrows — including Terminator arrows — to
	 * damage a vulnerable wither at any HP. Cancel the event preemptively (LOWEST) so
	 * vanilla never gets to bounce/skip, then apply the damage manually.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onArrowHitWither(ProjectileHitEvent event) {
		if(!(event.getEntity() instanceof Arrow arrow)) return;
		if(!(event.getHitEntity() instanceof Wither wither)) return;
		if(!(arrow.getShooter() instanceof Player p)) return;

		// Aggro the shooter on ANY attempted hit — bosses are armored most of the time, so waiting for damage to
		// actually land would leave them with no target. (Recorded before the shield-bounce early-return below.)
		WitherActions.noteDamager(p);

		// Shield up (invulnerability ticks active) → bounce, no damage. EXCEPTION: a Terminator/Last Breath arrow
		// landing on a tick the boss was made vulnerable then re-armored within that same tick — the live counter
		// already reads "shielded" because the arrow hit resolves after the start-of-tick boss scans, but the boss
		// WAS intended vulnerable this tick (a same-tick mage beam would connect). Honor that heartbeat-time intent.
		if(wither.getInvulnerabilityTicks() != 0
				&& !(arrow.getScoreboardTags().contains("TerminatorArrow") && WitherActions.wasMadeVulnerableThisTick(wither))) {
			return;
		}

		// Dying wither (any WitherLord): phase the arrow through silently — no ding, no damage, no pierce loss.
		WitherLord activeLord = WitherLord.activeFor(wither);
		if(activeLord != null && activeLord.isDying()) {
			event.setCancelled(true);
			return;
		}

		event.setCancelled(true);
		wither.setNoDamageTicks(0);
		// Clear the spawn-shield counter before damaging: vanilla WitherBoss.hurt() rejects all damage while
		// invulnerabilityTicks > 0, so on the same-tick-re-armored exception above the hit would otherwise no-op.
		// A re-armored boss's armorTask re-asserts the shield next tick, so this only lets THIS hit land.
		wither.setInvulnerabilityTicks(0);
		// Bukkit's no-source damage() uses a non-projectile cause, so the vanilla wither
		// "powered" projectile shield doesn't apply. The fired EntityDamageEvent still reaches
		// the WitherLord handleDamage dispatch (Maxor / Storm) for clamping.
		// Berserk's per-mob damage ramp (+10%/hit, cap 3×); each pierced arrow counts as its own hit.
		wither.damage(CustomItems.scaleBerserkDamage(p, wither, arrow.getDamage()));
		wither.setNoDamageTicks(0);
		Utils.playLocalSound(p, Sound.ENTITY_ARROW_HIT_PLAYER, 0.75f, 0.79368752611448590621283707774885f);
		Utils.changeName(wither);

		int newPierce = arrow.getPierceLevel() - 1;
		if(newPierce <= 0) arrow.remove();
		else arrow.setPierceLevel(newPierce);
	}
}
