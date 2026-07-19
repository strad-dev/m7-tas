package listeners;

import commands.Spectate;
import instructions.Server;
import instructions.bosses.Watcher;
import instructions.bosses.WitherActions;
import instructions.bosses.WitherLord;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.maxor.Maxor;
import instructions.bosses.necron.Necron;
import instructions.bosses.storm.Storm;
import instructions.bosses.witherking.WitherKing;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import plugin.FakePlayerManager;
import plugin.MovementAudit;
import plugin.Utils;

public class MiscListener implements Listener {
	@EventHandler
	public void onStoneButtonInArena(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if(e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.STONE_BUTTON) return;
		if(LavaJump.isInBossArena(e.getClickedBlock().getLocation())) {
			e.setCancelled(true);
		}
	}

	// Anvils must never open their repair/rename menu. Deny the block interaction on any anvil variant so the
	// UI never opens; item-use in hand is left untouched (only the container open is blocked).
	@EventHandler
	public void onAnvilInteract(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block b = e.getClickedBlock();
		if(b == null) return;
		Material m = b.getType();
		if(m == Material.ANVIL || m == Material.CHIPPED_ANVIL || m == Material.DAMAGED_ANVIL) {
			e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
		}
	}

	// The Watcher is a pre-boss you never damage (you kill its 19 blood mobs). It relies on RESISTANCE 255, but
	// genericKill (mage beam / hurtEntity) bypasses potion effects - so cancel ALL damage to it outright.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onWatcherDamage(EntityDamageEvent e) {
		// The Watcher is never damageable; a freshly-spawned blood mob is shielded for ~2 ticks (practice) so a
		// spawn-tick arrow can't prematurely kill it before it registers toward progress.
		if(e.getEntity().getScoreboardTags().contains("TASWatcher")
				|| e.getEntity().getScoreboardTags().contains("WatcherMobSpawning")) {
			e.setCancelled(true);
		}
	}

	// Killing the key archaeologists grants the global Wither / Blood keys (which gate the doors).
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onKeyMobDeath(EntityDeathEvent e) {
		boolean witherMob = e.getEntity().getScoreboardTags().contains("WitherKeyMob");
		boolean bloodMob = e.getEntity().getScoreboardTags().contains("BloodKeyMob");
		if(!witherMob && !bloodMob) return;
		// The key goes to (and the message names) the player closest to the mob when it died, excluding spectators.
		Player picker = null;
		double best = Double.MAX_VALUE;
		Location deathLoc = e.getEntity().getLocation();
		for(Player pl : Bukkit.getOnlinePlayers()) {
			if(pl.getGameMode() == GameMode.SPECTATOR || Spectate.isSpectating(pl)) continue;
			double d = pl.getLocation().distanceSquared(deathLoc);
			if(d < best) { best = d; picker = pl; }
		}
		if(witherMob) Server.grantWitherKey(picker);
		else Server.grantBloodKey(picker);
	}

	// Left/right-clicking a Wither/Blood door block opens it - if the matching key has been obtained. The click
	// is always cancelled within the door bounds so the block can't be broken; opening is a no-op without the key.
	@EventHandler
	public void onDoorClick(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
		Block b = e.getClickedBlock();
		if(b == null) return;
		if(Server.inWitherDoor(b)) {
			e.setCancelled(true);
			Server.tryOpenWitherDoor(e.getPlayer());
		} else if(Server.inBloodDoor(b)) {
			e.setCancelled(true);
			Server.tryOpenBloodDoor();
		}
	}

	// Vanilla ender pearls can't be thrown inside the boss arena (prevents pearl-skipping mechanics). The Infinileap
	// class ability (a named ender pearl) is exempt; fake players use simulated pearls + scripted teleports, so they
	// are excluded to avoid breaking choreography.
	@EventHandler
	public void onPearlInBossArena(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		ItemStack item = e.getItem();
		if(item == null || item.getType() != Material.ENDER_PEARL) return;
		if(CustomItems.getID(item).equals("skyblock/utility/infinileap")) return; // leap ability is allowed
		if(FakePlayerManager.getFakePlayers().containsValue(e.getPlayer())) return;
		if(LavaJump.isInBossArena(e.getPlayer().getLocation())) e.setCancelled(true);
	}

	// Chat is now owned by an external network chat plugin (when one is present), so the old per-server
	// chat handler was removed here to avoid double-broadcasting the same message.

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e) {
		if(e.getEntity() instanceof WindCharge windCharge && windCharge.getScoreboardTags().contains("Bonzo")) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent e) {
		// Every spawned entity joins the shared no-collision team so nothing push-collides with the players
		// (or each other). NOT setCollidable(false) - CraftBukkit makes canBeCollidedWith() return false when
		// collides=false, so vanilla's projectile sweep skips the entity and arrows phase through it. The
		// scoreboard team gives no-push collision while keeping the entity arrow-hittable.
		plugin.PlayerCollision.addEntityToNoCollisionTeam(e.getEntity());
	}

	// Prune the no-collision team when an entity leaves the world (death, despawn, chunk removal) - the symmetric
	// counterpart to onEntitySpawn's add, so the team's UUID entries don't accumulate unbounded over a run.
	@EventHandler
	public void onEntityRemove(EntityRemoveEvent e) {
		plugin.PlayerCollision.removeEntityFromNoCollisionTeam(e.getEntity());
	}

	// 26.2: migrated to Paper's unified io.papermc.paper.event.entity.EntityKnockbackEvent. The by-entity case
	// arrives as EntityPushedByEntityAttackEvent (a subclass sharing the same HandlerList), whose getPushedBy()
	// replaces the old EntityKnockbackByEntityEvent#getSourceEntity().
	@EventHandler
	public void onKnockback(EntityKnockbackEvent e) {
		// Cancel knockback on fake players (none in the practice fork - kept as a guard).
		if(e.getEntity() instanceof Player p && FakePlayerManager.getFakePlayers().containsValue(p)) {
			e.setCancelled(true);
			return;
		}
		// Bonzo's Staff wind charge must not knock back the player it hits.
		if(e instanceof EntityPushedByEntityAttackEvent pushed
				&& pushed.getPushedBy() instanceof WindCharge windCharge
				&& windCharge.getScoreboardTags().contains("Bonzo")) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		// Handle arrows hitting blocks - remove Terminator arrows
		if(e.getEntity() instanceof Arrow arrow) {
			// SUPER-verbose diagnostic: report when a Last Breath arrow hits a boss - a WITHER or the Wither King's
			// ENDER DRAGON (hits register on an EnderDragonPart, resolved to its parent) - so its hit timing can be
			// correlated against the boss vulnerability window, separate from ordinary Terminator arrows. Read-only;
			// uses the event's own hit reference so it's accurate even if the LOWEST-priority handler
			// (WithersNotImmuneToArrows) already processed/removed the arrow for the boss hit.
			if(Utils.isSuperVerbose() && arrow.getScoreboardTags().contains("LastBreathArrow")) {
				Entity rawHit = e.getHitEntity();
				Entity boss = rawHit instanceof Wither || rawHit instanceof EnderDragon ? rawHit
						: rawHit instanceof EnderDragonPart part ? part.getParent() : null;
				if(boss != null) {
					String shooter = arrow.getShooter() instanceof Player sp ? sp.getName() : "?";
					Utils.debug(Utils.DebugType.SERVER, "Last Breath arrow (" + shooter + ") hit " + boss.getName());
				}
			}
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

				// Phase through all real and fake players - fake-player arrows must never
				// damage a real player (would bypass Creative invulnerability via genericKill below).
				if(hitEntity instanceof Player) {
					e.setCancelled(true);
				}
				// Handle TerminatorArrow entity hits - cancel to preserve pierce, apply damage manually.
				// Wither hits are handled by WithersNotImmuneToArrows (which bypasses vanilla shield logic).
				else if(arrow.getScoreboardTags().contains("TerminatorArrow") && arrow.getShooter() instanceof Player p && !(hitEntity instanceof Wither)) {
					e.setCancelled(true);
					hitEntity.setNoDamageTicks(0);
					// Capture dead/dying state BEFORE applying damage: a killing-blow arrow SHOULD still ding (the
					// target was alive when hit), but an arrow striking an ALREADY dead/dying target should not. A
					// Wither-King dragon in its death animation keeps HP pinned to 1 (isDead()/getHealth() won't catch
					// it), so consult WitherKing's dying set too.
					boolean targetDead = hitEntity.isDead() || hitEntity.getHealth() <= 0
							|| hitEntity.getScoreboardTags().contains("TASDying") || WitherKing.isDyingDragon(hitEntity);
					// Berserk's per-mob damage ramp (+10%/hit, cap 3×); each pierced arrow counts as its own hit.
					Utils.hurtEntity(hitEntity, (float) CustomItems.scaleBerserkDamage(p, hitEntity, arrow.getDamage()), p);
					hitEntity.setNoDamageTicks(0);
					if(!targetDead) Utils.playLocalSound(p, Sound.ENTITY_ARROW_HIT_PLAYER, 0.75f, 0.79368752611448590621283707774885f);
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
				p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, windCharge.getLocation(), 128, 0, 0, 0, 0.75);
				p.getWorld().spawnParticle(Particle.CRIT, windCharge.getLocation(), 64, 0, 0, 0, 2);
				p.getWorld().playSound(windCharge.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0F, 1.0F);
				for(Player spectator : Spectate.getSpectatingPlayers(p)) spectator.playSound(spectator, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0F, 1.0F);

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

					// Fake players: queue the impulse so it's applied at the top of the next aiStep (see
					// FakePlayerManager.launch) - setting it here, inside the windcharge's entity tick, lands after
					// the fake ticker's aiStep already ran and the impulse gets clobbered before the next one, costing
					// the full first-tick rise. Real players run authoritative client physics, so set them directly.
					if(FakePlayerManager.getFakePlayers().containsValue(p)) {
						FakePlayerManager.launch(p, direction);
					} else {
						serverPlayer.setOnGround(false);
						p.setVelocity(direction);
						// Send the motion packet immediately rather than waiting for hurtMarked to be serviced
						// on the next aiStep - the deferral ships it a tick late and the client loses the full
						// first-tick rise (off-by-one). Immediate send matches Hypixel (full 0.5 on tick 1).
						serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
						serverPlayer.hurtMarked = false;
					}
					Integer fireTick = CustomItems.bonzoFireTick.remove(windCharge.getEntityId());
					int travelTicks = fireTick != null ? MinecraftServer.currentTick - fireTick : -1;
					Location loc = p.getLocation();
					Utils.debug(Utils.DebugType.SERVER, p.getName() + " bonzostaff launched from " + Utils.round(loc.getX(), 2) + " " + Utils.round(loc.getY(), 2) + " " + Utils.round(loc.getZ(), 2) + " with velocity " + Utils.round(direction.getX(), 4) + " " + Utils.round(direction.getY(), 4) + " " + Utils.round(direction.getZ(), 4) + " after " + travelTicks + " ticks");
					MovementAudit.startAirborneAudit(p, "bonzostaff");
				}
			}
		}
	}

	// Track game-mode changes during a run so the practice scoreboard shows golden names only for players who
	// stayed in Adventure Mode the whole time (a minor anti-cheat - any change disqualifies the gold name).
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent e) {
		WitherActions.noteGameModeChange(e.getPlayer().getUniqueId());
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if(e.getBlockPlaced().getType() != Material.SOUL_SAND) return;
		// Revert in survival AND adventure (the dungeon play modes) - only creative may place freely (setup/building).
		GameMode gm = e.getPlayer().getGameMode();
		if(gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) return;
		Location loc = e.getBlockPlaced().getLocation();
		double x = loc.getX(), y = loc.getY(), z = loc.getZ();
		if(x >= -8 && x <= 134 && y >= 0 && y <= 254 && z >= -8 && z <= 147) {
			org.bukkit.block.BlockState replacedState = e.getBlockReplacedState();
			Player player = e.getPlayer();
			ItemStack item = e.getItemInHand().clone();
			plugin.Utils.scheduleTask(() -> {
				replacedState.getBlock().setBlockData(replacedState.getBlockData());
				player.getInventory().addItem(item);
			}, 1);
		}
	}

	@EventHandler
	public void onEnderCrystalDamage(EntityDamageEvent e) {
		if(e.getEntity() instanceof EnderCrystal) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onWitherLordDamage(EntityDamageEvent e) {
		Maxor.INSTANCE.handleDamage(e);
		Storm.INSTANCE.handleDamage(e);
		Goldor.INSTANCE.handleDamage(e);
		Necron.INSTANCE.handleDamage(e);
	}

	// Remember whoever last damaged a TAS boss (fake or real) - bosses aggro that player. MONITOR (no
	// ignoreCancelled) so the damager is recorded even when the boss clamps/cancels the hit (immune window). This
	// catches melee / by-entity hits; mage-beam and terminator use no-source damage and note the damager directly.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBossDamager(EntityDamageByEntityEvent e) {
		if(!(e.getEntity() instanceof Wither w)) return;
		if(!w.getScoreboardTags().contains("TASWither")) return;
		Player damager = null;
		if(e.getDamager() instanceof Player pl) damager = pl;
		else if(e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player sp) damager = sp;
		if(damager != null) WitherActions.noteDamager(damager);
	}

	// Blood-Mob deaths drive the Watcher's kill lines + portal progression.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWatcherMobDeath(EntityDeathEvent e) {
		Watcher.INSTANCE.handleMobDeath(e);
	}

	// The Watcher's nether_portal is a teleport trigger, not real nether travel - never let vanilla relocate
	// the player to the Nether (single-world TAS server). Our portal-detection handles the intended teleport.
	@EventHandler(ignoreCancelled = true)
	public void onWatcherPortal(PlayerPortalEvent e) {
		if(e.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
			e.setCancelled(true);
		}
	}

	// Fire resistance stops fire/lava DAMAGE but the entity still visually catches fire and
	// accrues fire ticks - so cancel combustion outright for ALL players (walking through fire,
	// landing in lava on the Goldor lava-jump, etc.).
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerCombust(EntityCombustEvent e) {
		if(e.getEntity() instanceof Player) {
			e.setCancelled(true);
		}
	}

	// The wither bosses (and their skulls / any wither skeletons) apply the vanilla WITHER effect on hit -
	// a black damage-over-time that also clutters the screen. Players are never meant to be withered in
	// practice, so cancel it as it's applied. Only ADDED/CHANGED are blocked so removals aren't disturbed.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onWitherEffect(EntityPotionEffectEvent e) {
		if(e.getEntity() instanceof Player
				&& e.getModifiedType() == PotionEffectType.WITHER
				&& (e.getAction() == EntityPotionEffectEvent.Action.ADDED
					|| e.getAction() == EntityPotionEffectEvent.Action.CHANGED)) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onWitherSuffocation(EntityDamageEvent e) {
		if(e.getEntity() instanceof Wither && e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
			e.setCancelled(true);
		}
	}

	// Refresh nametag (which embeds current HP) after any damage event resolves -
	// MONITOR + 1-tick delay so we read the post-HP value, not the pre-event value.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWitherDamageNameRefresh(EntityDamageEvent e) {
		if(!(e.getEntity() instanceof Wither wither)) return;
		if(e.getFinalDamage() <= 0) return;
		Utils.scheduleTask(() -> { if(wither.isValid()) Utils.changeName(wither); }, 1);
	}

	// Runs before the WitherLord handleDamage HIGH-priority clamp so the original (pre-clamp)
	// damage is what we judge "did this hit do anything?" by. Otherwise hits that get
	// clamped to 0 (e.g. once Maxor's 75% / Storm's 55% stun cap is hit) would silently skip the sound.
	// Listens on the broader EntityDamageEvent so non-by-entity sources (e.g. mage beam's
	// Utils.hurtEntity, which uses genericKill) also trigger the sound.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onWitherHurtSound(EntityDamageEvent e) {
		if(!(e.getEntity() instanceof Wither wither)) return;
		if(e.getFinalDamage() <= 0) return;
		// Suffocation is cancelled by onWitherSuffocation but Bukkit doesn't guarantee
		// listener order within a class at the same priority - explicitly skip here.
		if(e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) return;
		// While dying, only the death noise plays - no hurt sound.
		WitherLord activeLord = WitherLord.activeFor(wither);
		if(activeLord != null && activeLord.isDying()) return;
		// Mage beam routes a constant-volume hurt sound to the beamer itself - skip the
		// at-location broadcast so beam hits aren't distance-attenuated (or doubled up close).
		if(CustomItems.beamDamageInProgress) return;

		Location loc = wither.getLocation();
		wither.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);

		Player damager = null;
		if(e instanceof EntityDamageByEntityEvent be) {
			damager = resolveDamager(be.getDamager());
		}
		if(damager == null) return;
		damager.playSound(loc, Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);

		// If a fake player dealt the damage, route the sound to its spectators too.
		if(FakePlayerManager.getFakePlayers().containsValue(damager)) {
			java.util.Set<Player> spectators = Spectate.getReverseSpectatorMap().get(damager);
			if(spectators != null) {
				for(Player spec : spectators) {
					spec.playSound(loc, Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
				}
			}
		}
	}

	private static Player resolveDamager(Entity damager) {
		if(damager instanceof Player p) return p;
		if(damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
		return null;
	}

	@EventHandler
	public void onEnergyCrystalRightClick(PlayerInteractAtEntityEvent e) {
		if(!(e.getRightClicked() instanceof EnderCrystal crystal)) return;
		if(Maxor.INSTANCE.notEnergyCrystal(crystal)) return;
		e.setCancelled(true);
		Maxor.INSTANCE.pickUp(e.getPlayer(), crystal);
	}

	@EventHandler
	public void onEnergyCrystalPlate(PlayerInteractEvent e) {
		if(e.getAction() != Action.PHYSICAL) return;
		Block b = e.getClickedBlock();
		if(b == null) return;
		int x = b.getX(), y = b.getY(), z = b.getZ();
		if(y == 224 && z == 41 && (x == 52 || x == 94)) {
			Maxor.INSTANCE.onPlateStep(e.getPlayer(), b.getLocation());
		}
	}

	// Mort and the Wizard are villagers - block right-clicks so the vanilla trade GUI doesn't open.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onVillagerInteract(PlayerInteractEntityEvent e) {
		if(e.getRightClicked() instanceof Villager) e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onVillagerInteractAt(PlayerInteractAtEntityEvent e) {
		if(e.getRightClicked() instanceof Villager) e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerPickupArrow(PlayerPickupArrowEvent e) {
		if(e.getArrow().getScoreboardTags().contains("TerminatorArrow")) {
			e.setCancelled(true);
		}
	}

}