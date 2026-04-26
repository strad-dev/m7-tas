package listeners;

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
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onArrowHitWither(ProjectileHitEvent event) {
		if(!(event.getEntity() instanceof Arrow arrow)) return;
		if(!(event.getHitEntity() instanceof Wither wither)) return;
		if(!(arrow.getShooter() instanceof Player p)) return;

		// Shield up (invulnerability ticks active) → bounce, no damage.
		if(wither.getInvulnerabilityTicks() != 0) return;

		event.setCancelled(true);
		wither.setNoDamageTicks(0);
		Utils.hurtEntity(wither, (float) arrow.getDamage(), p);
		wither.setNoDamageTicks(0);
		Utils.playLocalSound(p, Sound.ENTITY_ARROW_HIT_PLAYER, 0.75f, 0.79368752611448590621283707774885f);
		Utils.changeName(wither);

		int newPierce = arrow.getPierceLevel() - 1;
		if(newPierce <= 0) arrow.remove();
		else arrow.setPierceLevel(newPierce);
	}
}
