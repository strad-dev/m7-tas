package plugin;

import com.mojang.datafixers.util.Pair;
import commands.Spectate;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PlayerInventoryBackup {
	private static final Map<Player, PlayerInventoryBackup> originalInventories = new HashMap<>();

	private final ItemStack[] contents;
	private final ItemStack[] armorContents;
	private final ItemStack offHand;
	private final int heldItemSlot;

	public PlayerInventoryBackup(Player player) {
		PlayerInventory inv = player.getInventory();
		this.contents = inv.getContents().clone();
		this.armorContents = inv.getArmorContents().clone();
		this.offHand = inv.getItemInOffHand().clone();
		this.heldItemSlot = inv.getHeldItemSlot();
	}

	public void restore(Player player) {
		PlayerInventory inv = player.getInventory();
		inv.setContents(contents);
		inv.setArmorContents(armorContents);
		inv.setItemInOffHand(offHand);
		inv.setHeldItemSlot(heldItemSlot);
		player.updateInventory();
	}

	public static void backup(Player player) {
		originalInventories.put(player, new PlayerInventoryBackup(player));
	}

	public static void restoreAndRemove(Player player) {
		PlayerInventoryBackup backup = originalInventories.remove(player);
		if(backup != null) {
			backup.restore(player);
		}
	}

	public static void clearAll() {
		originalInventories.clear();
	}

	// --- Inventory Sync ---

	private static BukkitTask inventorySyncTask;

	public static void startInventorySync() {
		if(inventorySyncTask != null) {
			inventorySyncTask.cancel();
		}

		inventorySyncTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), () -> {
			for(Player spectator : Spectate.getSpectatorMap().keySet()) {
				Player fakePlayer = Spectate.getSpectatorMap().get(spectator);
				if(fakePlayer != null) {
					syncInventory(fakePlayer);
				}
			}
		}, 0L, 1L);
	}

	public static void stopInventorySync() {
		if(inventorySyncTask != null) {
			inventorySyncTask.cancel();
			inventorySyncTask = null;
		}
	}

	public static void syncInventory(Player fakePlayer) {
		Set<Player> spectators = Spectate.getSpectatingPlayers(fakePlayer);
		if(!spectators.isEmpty()) {
			forceFullInventorySync(fakePlayer, spectators);

			for(Player spectator : spectators) {
				spectator.getInventory().setHeldItemSlot(fakePlayer.getInventory().getHeldItemSlot());
			}
		}
	}

	public static void forceFullInventorySync(Player sourcePlayer, Set<Player> targets) {
		CraftPlayer cp = (CraftPlayer) sourcePlayer;
		ServerPlayer handle = cp.getHandle();
		AbstractContainerMenu menu = handle.containerMenu;

		ClientboundContainerSetContentPacket pkt = new ClientboundContainerSetContentPacket(menu.containerId, menu.incrementStateId(), menu.getItems(), menu.getCarried());

		for(Player target : targets) {
			if(target instanceof CraftPlayer craftTarget) {
				craftTarget.getHandle().connection.send(pkt);
			}
		}
	}

	public static void syncHand(Player fake) {
		ServerPlayer npc = ((CraftPlayer) fake).getHandle();
		net.minecraft.world.item.ItemStack handStack = npc.getInventory().getSelectedItem();

		ClientboundSetEquipmentPacket equipPkt = new ClientboundSetEquipmentPacket(npc.getId(), Collections.singletonList(Pair.of(EquipmentSlot.MAINHAND, handStack)));

		Utils.broadcastPacket(equipPkt);

		Set<Player> spectators = Spectate.getSpectatingPlayers(fake);
		for(Player spectator : spectators) {
			spectator.getInventory().setHeldItemSlot(fake.getInventory().getHeldItemSlot());
			spectator.getInventory().setItemInMainHand(fake.getInventory().getItemInMainHand());
			spectator.updateInventory();
		}
	}
}
