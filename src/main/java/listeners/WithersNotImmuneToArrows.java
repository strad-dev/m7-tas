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
	 * Vanilla blocks projectiles on a powered wither (HP <= 50%) and while its shield is up. Arrows should hit a
	 * vulnerable wither at any HP, so cancel at LOWEST and apply the damage manually.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onArrowHitWither(ProjectileHitEvent event) {
		if(!(event.getEntity() instanceof Arrow arrow)) return;
		if(!(event.getHitEntity() instanceof Wither wither)) return;
		if(!(arrow.getShooter() instanceof Player p)) return;

		// An arrow that takes health off the boss sets the aggro target like a swing; one for zero doesn't. Damage.deal
		// owns that; an arrow on an armoured boss returns below before reaching it.

		// Shield up: bounce, no damage. EXCEPTION: a Terminator/Last Breath arrow on a tick the boss was made
		// vulnerable then re-armored. The hit resolves after the start-of-tick boss scans so the counter reads
		// shielded, but a same-tick mage beam would connect, so honour the heartbeat-time intent.
		if(wither.getInvulnerableTicks() != 0
				&& !(arrow.getScoreboardTags().contains("TerminatorArrow") && WitherActions.wasMadeVulnerableThisTick(wither))) {
			return;
		}

		// Dying wither (any WitherLord): arrow phases through, no ding, damage or pierce loss.
		WitherLord activeLord = WitherLord.activeFor(wither);
		if(activeLord != null && activeLord.isDying()) {
			event.setCancelled(true);
			return;
		}

		event.setCancelled(true);
		// Clear the shield counter first: WitherBoss.hurt() rejects all damage while invulnerabilityTicks > 0, so the
		// same-tick exception above would no-op. armorTask re-asserts the shield next tick; only THIS hit lands.
		wither.setInvulnerableTicks(0);
		// One damage path (MAP.md §7); this was Bukkit's no-source wither.damage(), once the only route to the
		// clamps. The arrow carries its stat damage from fire time; the target half resolves here, and Arrows.hit's
		// deal calls clampDamage and notes aggro only if health moves.
		damage.Arrows.hit(arrow, p, wither);
		Utils.playLocalSound(p, Sound.ENTITY_ARROW_HIT_PLAYER, 0.75f, 0.79368752611448590621283707774885f);

		int newPierce = arrow.getPierceLevel() - 1;
		if(newPierce <= 0) arrow.remove();
		else arrow.setPierceLevel(newPierce);
	}
}
