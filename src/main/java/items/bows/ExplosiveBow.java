package items.bows;

import damage.Rarity;
import damage.ReforgeId;
import items.*;
import net.minecraft.world.InteractionHand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Drawn bow whose arrows detonate: every mob within 3 blocks takes the weapon's FULL damage (§1.9), and the blast
 * goes through the Superboom radius, so it opens crypts and walls like the TNT.
 * <p>
 * The directly-hit entity is EXCLUDED: it already took arrow damage on the normal path
 * ({@code WithersNotImmuneToArrows} for a vulnerable wither).
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

		// Entity hit: normal arrow damage and ding via the normal path. Entity or block hit: then it explodes.
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

		// Every mob within 3 blocks takes FULL damage (MAP.md §1.9); the directly-hit one is skipped, not hit twice.
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
