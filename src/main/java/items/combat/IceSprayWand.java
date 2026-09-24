package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import plugin.Utils;

import java.util.List;

/**
 * The x1.1 debuff lands on EVERY enemy within 8 blocks BEFORE the damage, so the cast benefits from it (§7), and it
 * still lands on targets the damage can't reach (armoured wither).
 */
public final class IceSprayWand implements Weapon, AbilityItem {
	public static final IceSprayWand INSTANCE = new IceSprayWand();

	private IceSprayWand() {}

	@Override
	public String loreId() {
		return "skyblock/combat/ice_spray";
	}

	@Override
	public Material material() {
		return Material.STICK;
	}

	@Override
	public String baseName() {
		return "Ice Spray Wand";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.EPIC;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.HEROIC;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "STARRED_ICE_SPRAY_WAND");
	}

	@Override
	public boolean mageBeams() {
		return true;
	}

	@Override
	public boolean hasRightClick() {
		return true;
	}

	@Override
	public int cooldownTicks() {
		return 100; // 5s
	}

	@Override
	public boolean onRightClick(Cast cast) {
		iceSpray(cast.player());
		return true;
	}

	public static void iceSpray(Player p) {
		Location l = p.getEyeLocation();
		p.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 128);
		List<Entity> entities = (List<Entity>) p.getWorld().getNearbyEntities(l, 8, 8, 8);
		List<EntityType> doNotKill = ItemUtils.doNotKill();
		ItemStack wand = p.getInventory().getItemInMainHand();
		int debuffed = 0;
		int alreadyDebuffed = 0;
		for(Entity entity : entities) {
			if(!doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0) {
				// Counted BEFORE the apply, which refreshes the window. A refresh counts as "already debuffed" like
				// SkyBlock in Vanilla's wand, but here it still takes the damage.
				if(damage.TargetDebuffs.iceSprayed(entity1)) alreadyDebuffed++;
				else debuffed++;
				// 5s, lands FIRST (MAP.md §7), even on an armoured wither.
				damage.TargetDebuffs.applyIceSpray(entity1);
				if(entity instanceof Wither wither && wither.getInvulnerableTicks() != 0) continue;
				// 19,000 base, 0.1 Int scaling; the bigger base doesn't make up for Wither Impact's 0.3 (§7).
				double sbDamage = damage.Damage.ability(p, entity1, wand);
				damage.Damage.deal(entity1, sbDamage, damage.DamageKind.MAGIC, p, damage.DamagePath.ABILITY);
			}
		}
		if(debuffed > 0) {
			p.sendMessage(Utils.msg("<red>Your Ice Spray debuffed " + debuffed + " enemies."));
		}
		if(alreadyDebuffed > 0) {
			p.sendMessage(Utils.msg("<red>" + alreadyDebuffed + " enemies have already been debuffed."));
		}
		Utils.playLocalSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
	}
}
