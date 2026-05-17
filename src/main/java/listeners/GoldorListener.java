package listeners;

import instructions.Actions;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.goldor.GoldorLever;
import instructions.bosses.goldor.GoldorSection;
import instructions.bosses.goldor.GoldorTerminal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import plugin.Utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GoldorListener implements Listener {

	// ------ Per-device runtime state (cleared on each phase via Goldor's resetState by reference) ------

	// Simon Says: per-player click timestamps (millis)
	private final Map<UUID, Deque<Long>> simonClicks = new HashMap<>();
	// Sharp Shooter: hit state on the 9 target blocks
	private final boolean[][] sharpHits = new boolean[3][3]; // [xIdx 0..2 → 68/66/64][yIdx 0..2 → 130/128/126]
	private int sharpHitCount = 0;

	// Simon Says button coord
	private static final int SIMON_BX = 110, SIMON_BY = 121, SIMON_BZ = 91;
	// Sharp Shooter plate coord
	private static final int PLATE_X = 63, PLATE_Y = 127, PLATE_Z = 35;
	// Sharp Shooter target Z
	private static final int TARGET_Z = 50;
	// Sharp Shooter target X values
	private static final int[] TARGET_XS = {68, 66, 64};
	// Sharp Shooter target Y values
	private static final int[] TARGET_YS = {130, 128, 126};
	// Lights levers bounding box (S2 device)
	private static final int LIGHTS_X1 = 58, LIGHTS_X2 = 62;
	private static final int LIGHTS_Y1 = 133, LIGHTS_Y2 = 136;
	private static final int LIGHTS_Z = 142;
	// Arrow Align item frame block coord (S3 device)
	private static final int ARROW_X = -2, ARROW_Y = 122, ARROW_Z = 77;

	// =================== Terminal click ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onInteractAt(PlayerInteractAtEntityEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		Entity ent = e.getRightClicked();
		String tagPrefix = GoldorTerminal.TAG_PREFIX;
		for(String tag : ent.getScoreboardTags()) {
			if(tag.startsWith(tagPrefix)) {
				int[] idx = GoldorTerminal.parseTag(tag);
				if(idx == null) return;
				GoldorSection sec = Goldor.INSTANCE.getSection(idx[0]);
				if(sec == null) return;
				if(idx[0] != Goldor.INSTANCE.getCurrentSectionIdx()) return; // section-gated
				if(idx[1] < 0 || idx[1] >= sec.terminals.size()) return;
				GoldorTerminal term = sec.terminals.get(idx[1]);
				if(term.isActivated() || term.isPending()) return;

				Player p = e.getPlayer();
				term.setPending();
				Actions.clearMovementInput(p);
				Utils.scheduleTask(() -> {
					if(term.isActivated()) return;
					term.markActivated();
					Goldor.INSTANCE.onActivation(p, sec, "terminal");
				}, 1L);
				return;
			}
		}
	}

	// =================== Lever flip + Simon Says button + Lights levers ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onInteract(PlayerInteractEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block b = e.getClickedBlock();
		if(b == null) return;
		int bx = b.getX(), by = b.getY(), bz = b.getZ();
		Player p = e.getPlayer();

		// Simon Says button
		if(bx == SIMON_BX && by == SIMON_BY && bz == SIMON_BZ) {
			GoldorSection s1 = Goldor.INSTANCE.getSection(0);
			if(s1 != null && !s1.device.isActivated()) {
				long now = System.currentTimeMillis();
				Deque<Long> clicks = simonClicks.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
				clicks.addLast(now);
				while(!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
				if(clicks.size() >= 15) {
					s1.device.markActivated();
					Goldor.INSTANCE.onActivation(p, s1, "device");
					clicks.clear();
				}
			}
			return;
		}

		// Lights levers (S2 device)
		if(b.getType() == Material.LEVER
				&& bx >= LIGHTS_X1 && bx <= LIGHTS_X2
				&& by >= LIGHTS_Y1 && by <= LIGHTS_Y2
				&& bz == LIGHTS_Z) {
			GoldorSection s2 = Goldor.INSTANCE.getSection(1);
			if(s2 != null && !s2.device.isActivated()) {
				s2.device.markActivated();
				Goldor.INSTANCE.onActivation(p, s2, "device");
			}
			return;
		}

		// Section levers
		if(b.getType() == Material.LEVER) {
			for(GoldorSection sec : new GoldorSection[]{
					Goldor.INSTANCE.getSection(0),
					Goldor.INSTANCE.getSection(1),
					Goldor.INSTANCE.getSection(2),
					Goldor.INSTANCE.getSection(3)}) {
				if(sec == null) continue;
				for(GoldorLever lev : sec.levers) {
					if(lev.isLeverBlock(bx, by, bz)) {
						if(sec.idx != Goldor.INSTANCE.getCurrentSectionIdx()) return; // section-gated
						if(lev.isActivated()) return;
						lev.markActivated();
						Goldor.INSTANCE.onActivation(p, sec, "lever");
						return;
					}
				}
			}
		}
	}

	// =================== Arrow Align frame rotation ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(!(e.getRightClicked() instanceof ItemFrame frame)) return;
		Location floc = frame.getLocation();
		double dx = floc.getX() - (ARROW_X + 0.5);
		double dy = floc.getY() - (ARROW_Y + 0.5);
		double dz = floc.getZ() - (ARROW_Z + 0.5);
		// Frame entity sits on a block face; allow up to ~1 block of slack.
		if(Math.abs(dx) > 1.0 || Math.abs(dy) > 1.0 || Math.abs(dz) > 1.0) return;
		GoldorSection s3 = Goldor.INSTANCE.getSection(2);
		if(s3 == null || s3.device.isActivated()) return;
		s3.device.markActivated();
		Goldor.INSTANCE.onActivation(e.getPlayer(), s3, "device");
	}

	// =================== Sharp Shooter arrows ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onProjectileHit(ProjectileHitEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		Block hit = e.getHitBlock();
		if(hit == null) return;
		if(hit.getZ() != TARGET_Z) return;
		GoldorSection s4 = Goldor.INSTANCE.getSection(3);
		if(s4 == null || s4.device.isActivated()) return;

		int xIdx = -1, yIdx = -1;
		for(int i = 0; i < TARGET_XS.length; i++) if(hit.getX() == TARGET_XS[i]) { xIdx = i; break; }
		for(int i = 0; i < TARGET_YS.length; i++) if(hit.getY() == TARGET_YS[i]) { yIdx = i; break; }
		if(xIdx < 0 || yIdx < 0) return;

		// Must have a player on the plate
		if(!isPlayerOnPlate()) return;

		if(!sharpHits[xIdx][yIdx]) {
			sharpHits[xIdx][yIdx] = true;
			sharpHitCount++;
			if(sharpHitCount >= 9) {
				Player shooter = (e.getEntity().getShooter() instanceof Player pl) ? pl : null;
				if(shooter == null) {
					for(Player pl : Bukkit.getOnlinePlayers()) { shooter = pl; break; }
				}
				s4.device.markActivated();
				if(shooter != null) Goldor.INSTANCE.onActivation(shooter, s4, "device");
				resetSharpHits();
			}
		}
	}

	private boolean isPlayerOnPlate() {
		Location plate = new Location(Bukkit.getWorlds().getFirst(), PLATE_X + 0.5, PLATE_Y, PLATE_Z + 0.5);
		for(Player p : Bukkit.getOnlinePlayers()) {
			Location pl = p.getLocation();
			if(Math.abs(pl.getX() - (PLATE_X + 0.5)) <= 0.6
					&& Math.abs(pl.getZ() - (PLATE_Z + 0.5)) <= 0.6
					&& Math.abs(pl.getY() - PLATE_Y) <= 1.5) {
				return true;
			}
		}
		return false;
	}

	private void resetSharpHits() {
		for(int i = 0; i < 3; i++) for(int j = 0; j < 3; j++) sharpHits[i][j] = false;
		sharpHitCount = 0;
	}

	// =================== Item-frame indestructibility ===================
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(e.getEntity() instanceof ItemFrame) {
			e.setCancelled(true);
		}
	}

	// =================== Gate explosion (real EntityExplodeEvent fallback) ===================
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		Location loc = e.getEntity().getLocation();
		Goldor.INSTANCE.notifyExplosionAt(loc);
	}
}
