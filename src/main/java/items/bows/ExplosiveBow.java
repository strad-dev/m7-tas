package items.bows;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Bow;
import items.Cast;
import items.ItemFactory;
import items.ItemUtils;
import items.ProjectileItem;
import net.minecraft.world.InteractionHand;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import plugin.*;

import java.util.*;

/**
 * The Explosive Bow.  A drawn bow whose arrows detonate on impact: every mob within 3 blocks takes the weapon's
 * FULL damage (§1.9), and the blast also routes through the shared Superboom radius, so it opens crypts and
 * cracked-brick walls exactly as the TNT does.
 * <p>
 * The directly-hit entity is EXCLUDED from the blast: it already took its arrow damage on the normal path
 * (through {@code WithersNotImmuneToArrows} for a vulnerable wither), so including it would hit it twice.
 */
public final class ExplosiveBow implements Bow, AbilityItem, ProjectileItem {
	public static final ExplosiveBow INSTANCE = new ExplosiveBow();

	private ExplosiveBow() {}

	@Override
	public String loreId() {
		return "skyblock/combat/explosive_bow";
	}

	@Override
	public Material material() {
		return Material.BOW;
	}

	@Override
	public String baseName() {
		return "Explosive Bow";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.EPIC;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.PRECISE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "EXPLOSIVE_BOW");
	}

	@Override
	public boolean holdToDraw() {
		return true;
	}

	@Override
	public List<String> arrowTags() {
		return List.of("ExplosiveBowArrow");
	}

	@Override
	public boolean allowsEntityInteract() {
		return true;
	}

	@Override
	public boolean hasRightClick() {
		return true;
	}

	@Override
	public boolean onRightClick(Cast cast) {
		((CraftPlayer) cast.player()).getHandle().startUsingItem(InteractionHand.MAIN_HAND);
		return true;
	}

	@Override
	public String projectileTag() {
		return "ExplosiveBowArrow";
	}

	@Override
	public void onProjectileHit(ProjectileHitEvent e, Player shooter) {
		handleHit(e);
	}

	private static void handleHit(ProjectileHitEvent e) {
		if(!(e.getEntity() instanceof Arrow arrow)) return;
		if(!arrow.getScoreboardTags().contains("ExplosiveBowArrow")) return;
		if(!(arrow.getShooter() instanceof Player p)) return;

		// On entity contact the arrow behaves like a normal arrow: its arrow damage and the hit ding are applied
		// by the normal damage path (WithersNotImmuneToArrows for a vulnerable wither).  On EITHER an entity or a
		// block hit it then detonates an added explosion bonus at the point of impact.
		Location impact;
		if(e.getHitEntity() != null) {
			impact = e.getHitEntity().getLocation().add(0, e.getHitEntity().getHeight() / 2.0, 0);
		} else if(e.getHitBlock() != null) {
			impact = e.getHitBlock().getLocation();
		} else {
			return;
		}
		impact.getWorld().spawnParticle(Particle.EXPLOSION, impact.clone().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0);
		impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);

		// The Explosive Bow's own ability: every mob within 3 blocks takes the weapon's FULL damage
		// (MAP.md §1.9).  The directly-hit entity already took its arrow damage on the normal path, so it
		// is excluded here rather than hit twice.
		for(Entity nearby : impact.getWorld().getNearbyEntities(impact, 3, 3, 3)) {
			if(!(nearby instanceof LivingEntity mob) || nearby instanceof Player) continue;
			if(nearby.equals(e.getHitEntity()) || mob.isDead() || mob.getHealth() <= 0) continue;
			if(nearby instanceof Wither wither && wither.getInvulnerableTicks() != 0) continue;
			damage.Arrows.hit(arrow, p, mob, false);
		}

		ItemUtils.triggerSuperboomRadius(impact, p);
		arrow.remove();
	}
}
