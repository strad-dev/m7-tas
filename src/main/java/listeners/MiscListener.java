package listeners;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.util.Vector;
import plugin.FakePlayerManager;
import plugin.Utils;

public class MiscListener implements Listener {
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		String player = e.getPlayer().getName();
		String message = e.getMessage();
		String sentMessage = "";
		if(player.equals("Beethoven_")) {
			sentMessage += ChatColor.BLUE;
		} else {
			sentMessage += ChatColor.GREEN;
		}
		sentMessage += player + ChatColor.WHITE + ": " + message;
		Bukkit.broadcastMessage(sentMessage);
		e.setCancelled(true);
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e) {
		if(e.getEntity() instanceof WindCharge windCharge && windCharge.getScoreboardTags().contains("Bonzo")) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityKnockbackByEntity(EntityKnockbackByEntityEvent e) {
		if(e.getSourceEntity() instanceof WindCharge windCharge && windCharge.getScoreboardTags().contains("Bonzo")) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		// Handle arrows hitting blocks - remove Terminator arrows
		if(e.getEntity() instanceof Arrow arrow) {
			if(e.getHitBlock() != null) {
				if(arrow.getScoreboardTags().contains("TerminatorArrow")) {
					arrow.remove();
				}
			} else if(e.getHitEntity() != null) {
				// Resolve EnderDragonPart to its parent EnderDragon (EnderDragonPart is not a LivingEntity)
				Entity rawHit = e.getHitEntity();
				LivingEntity hitEntity = rawHit instanceof LivingEntity le ? le
						: rawHit instanceof EnderDragonPart part ? part.getParent() : null;
				if(hitEntity == null) return;

				// Phase through fake players and self
				if(hitEntity instanceof Player player && (FakePlayerManager.getFakePlayers().containsValue(player) || (arrow.getShooter() instanceof Player shooter && player.equals(shooter)))) {
					e.setCancelled(true);
				}
				// Handle TerminatorArrow entity hits - cancel to preserve pierce, apply damage manually
				else if(arrow.getScoreboardTags().contains("TerminatorArrow") && arrow.getShooter() instanceof Player p) {
					e.setCancelled(true);
					hitEntity.setNoDamageTicks(0);
					Utils.hurtEntity(hitEntity, (float) arrow.getDamage(), p);
					hitEntity.setNoDamageTicks(0);
					Utils.playLocalSound(p, Sound.ENTITY_ARROW_HIT_PLAYER, 0.75f, 0.79368752611448590621283707774885f);
					Utils.changeName(hitEntity);
					int newPierce = arrow.getPierceLevel() - 1;
					if(newPierce <= 0) arrow.remove();
					else arrow.setPierceLevel(newPierce);
				}
			}
		} else if(e.getEntity() instanceof WindCharge windCharge && windCharge.getScoreboardTags().contains("Bonzo")) {
			e.setCancelled(true);
			windCharge.remove();

			if(windCharge.getShooter() instanceof Player p) {
				double distance = p.getLocation().distanceSquared(windCharge.getLocation());
				p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, windCharge.getLocation(), 350, 0, 0, 0, 0.75);
				p.getWorld().spawnParticle(Particle.CRIT, windCharge.getLocation(), 150, 0, 0, 0, 2);
				p.getWorld().playSound(windCharge.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0F, 1.0F);

				if(distance <= 16) {
					if(!(p instanceof CraftPlayer craftPlayer)) return;
					ServerPlayer serverPlayer = craftPlayer.getHandle();

					Vector direction = p.getLocation().toVector().subtract(windCharge.getLocation().toVector()).normalize();
					direction.setY(0);
					direction.normalize();
					direction.multiply(1.52552);
					direction.setY(0.5);

					if(!Double.isFinite(direction.getX())) {
						direction.setX(0);
					}
					if(!Double.isFinite(direction.getZ())) {
						direction.setZ(0);
					}

					serverPlayer.setOnGround(false);
					p.setVelocity(direction);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerPickupArrow(PlayerPickupArrowEvent e) {
		if(e.getArrow().getScoreboardTags().contains("TerminatorArrow")) {
			e.setCancelled(true);
		}
	}

}