package listeners;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.util.Vector;
import plugin.FakePlayerManager;
import plugin.MovementAudit;
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
				else if(arrow.getScoreboardTags().contains("TerminatorArrow") && arrow.getShooter() instanceof Player p && !(hitEntity instanceof Wither wither && wither.getInvulnerabilityTicks() != 0)) {
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

				if(distance <= 12.25) {
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
					Utils.debug(Utils.DebugType.SERVER, p.getName() + " bonzostaff launched with velocity " + Utils.round(direction.getX(), 4) + " " + Utils.round(direction.getY(), 4) + " " + Utils.round(direction.getZ(), 4));
					MovementAudit.startAirborneAudit(p, "bonzostaff");
				}
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if(e.getBlockPlaced().getType() != Material.SOUL_SAND) return;
		if(e.getPlayer().getGameMode() != GameMode.SURVIVAL) return;
		Location loc = e.getBlockPlaced().getLocation();
		double x = loc.getX(), y = loc.getY(), z = loc.getZ();
		if(x >= -8 && x <= 134 && y >= 0 && y <= 254 && z >= -8 && z <= 147) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onEnderCrystalDamage(EntityDamageEvent e) {
		if(e.getEntity() instanceof EnderCrystal) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerPickupArrow(PlayerPickupArrowEvent e) {
		if(e.getArrow().getScoreboardTags().contains("TerminatorArrow")) {
			e.setCancelled(true);
		}
	}

}