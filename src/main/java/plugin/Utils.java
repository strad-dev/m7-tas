package plugin;

import com.mojang.datafixers.util.Pair;
import commands.Spectate;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.inventory.AbstractContainerMenu;
import nms.TASGamePacketListenerImpl;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Utils {
	/**
	 * Wrapper for Bukkit.getScheduler().runTaskLater(Plugin, Runnable, long)
	 *
	 * @param task  The task to run later.
	 * @param delay In how many ticks this task should be run.
	 */
	public static void scheduleTask(Runnable task, long delay) {
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), task, delay);
	}

	public static void teleport(Player p, Location to) {
		if(!(p instanceof CraftPlayer cp)) {
			return;
		}
		ServerPlayer npc = cp.getHandle();

		// Update NMS position
		npc.setPos(to.getX(), to.getY(), to.getZ());
		npc.setYRot(to.getYaw());
		npc.setXRot(to.getPitch());
		npc.yHeadRot = to.getYaw();
		npc.yBodyRot = to.getYaw();

		// Send packets
		PositionMoveRotation pmr = PositionMoveRotation.of(npc);
		ClientboundTeleportEntityPacket tp = ClientboundTeleportEntityPacket.teleport(npc.getId(), pmr, EnumSet.noneOf(Relative.class), npc.onGround());

		broadcastPacket(tp);
	}

	/**
	 * Syncs fake player's inventory to spectators using packet-based approach
	 */
	public static void syncInventory(Player fakePlayer) {
		Set<Player> spectators = Spectate.getSpectatingPlayers(fakePlayer);
		if(!spectators.isEmpty()) {
			// Method 1: Use packet-based sync (more efficient)
			forceFullInventorySync(fakePlayer, spectators);

			// Method 2: Also sync held item slot specifically
			for(Player spectator : spectators) {
				spectator.getInventory().setHeldItemSlot(fakePlayer.getInventory().getHeldItemSlot());
			}
		}
	}

	/**
	 * Force a full resync of the given player's inventory to specified targets.
	 */
	public static void forceFullInventorySync(Player sourcePlayer, Set<Player> targets) {
		CraftPlayer cp = (CraftPlayer) sourcePlayer;
		ServerPlayer handle = cp.getHandle();
		AbstractContainerMenu menu = handle.containerMenu; // the open window (0 = player inv)

		// Build the packet with the current contents
		ClientboundContainerSetContentPacket pkt = new ClientboundContainerSetContentPacket(menu.containerId, menu.incrementStateId(), menu.getItems(), menu.getCarried());

		// Send to specified targets
		for(Player target : targets) {
			if(target instanceof CraftPlayer craftTarget) {
				craftTarget.getHandle().connection.send(pkt);
			}
		}
	}

	/**
	 * Updates the held item in an NPC's hand for all viewers AND spectators
	 */
	public static void syncHand(Player fake) {
		ServerPlayer npc = ((CraftPlayer) fake).getHandle();
		// get whatever ItemStack is in their selected slot
		net.minecraft.world.item.ItemStack handStack = npc.getInventory().getSelectedItem();

		// build an equipment packet for MAIN_HAND
		ClientboundSetEquipmentPacket equipPkt = new ClientboundSetEquipmentPacket(npc.getId(), Collections.singletonList(Pair.of(EquipmentSlot.MAINHAND, handStack)));

		// send it to every real viewer
		broadcastPacket(equipPkt);

		// Also update spectators' held items
		Set<Player> spectators = Spectate.getSpectatingPlayers(fake);
		for(Player spectator : spectators) {
			spectator.getInventory().setHeldItemSlot(fake.getInventory().getHeldItemSlot());
			spectator.getInventory().setItemInMainHand(fake.getInventory().getItemInMainHand());
			spectator.updateInventory();
		}
	}

	/**
	 * Simulates a packet sent from a Player to the server.
	 *
	 * @param player The player who sent the packet
	 * @param packet The packet to simulate
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void simulatePacket(Player player, Packet<?> packet) {
		if(!(player instanceof CraftPlayer craftPlayer)) return;

		Utils.debug(Utils.DebugType.CLIENT, player.getName() + " Sending Packet " + packet.getClass().getSimpleName());
		ServerPlayer serverPlayer = craftPlayer.getHandle();
		if(serverPlayer.connection instanceof TASGamePacketListenerImpl customConnection) {
			((Packet) packet).handle(customConnection);
		} else {
			((Packet) packet).handle(serverPlayer.connection);
		}
	}

	/**
	 * Sends a packet to every player on the server.
	 *
	 * @param pkt Packet to send
	 */
	public static void broadcastPacket(Packet<?> pkt) {
		for(Player p : Bukkit.getOnlinePlayers()) {
			((CraftPlayer) p).getHandle().connection.send(pkt);
		}
	}

	/**
	 * Creates a leather armor item with the specified material, color, and name.
	 *
	 * @param material The material type of the leather armor.
	 * @param color    The color to apply to the leather armor.
	 * @param name     The display name to set for the leather armor.
	 * @return A new ItemStack representing the customized leather armor.
	 */
	public static ItemStack createLeatherArmor(Material material, Color color, String name) {
		ItemStack item = new ItemStack(material);
		LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
		assert meta != null;
		meta.setColor(color);
		meta.setUnbreakable(true);
		meta.setDisplayName(name);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
		item.addUnsafeEnchantment(Enchantment.PROTECTION, 5);
		return item;
	}

	public static void backupInventory(Player player) {
		M7tas.addPlayerInventoryBackup(player);
	}

	public static void restoreInventory(Player player) {
		M7tas.PlayerInventoryBackup backup = M7tas.removePlayerInventoryBackup(player);
		if(backup != null) {
			backup.restore(player);
		}
	}

	private static BukkitTask inventorySyncTask;

	public static void startInventorySync() {
		if(inventorySyncTask != null) {
			inventorySyncTask.cancel();
		}

		inventorySyncTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), () -> {
			// Re-sync all spectators' inventories every 10 ticks
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

	/**
	 * Plays a sound for every player on the server
	 *
	 * @param s The sound to play
	 */
	@SuppressWarnings("unused")
	public static void playGlobalSound(Sound s) {
		Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, s, 1.0F, 1.0F));
	}

	/**
	 * Plays a sound for every player on the server
	 *
	 * @param s      The sound to play
	 * @param volume The volume of the sound
	 * @param pitch  The pitch of the sound
	 */
	public static void playGlobalSound(Sound s, float volume, float pitch) {
		Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, s, volume, pitch));
	}

	/**
	 * Plays a sound for all players spectating this player if applicable
	 *
	 * @param p The player causing the sound
	 * @param s The sound to play
	 */
	public static void playLocalSound(Player p, Sound s) {
		if(M7tas.getFakePlayers().containsValue(p) && Spectate.getReverseSpectatorMap().containsKey(p)) {
			for(Player spectator : Spectate.getReverseSpectatorMap().get(p)) {
				spectator.playSound(spectator, s, 1.0f, 1.0f);
			}
		} else {
			p.playSound(p, s, 1.0f, 1.0f);
		}
	}

	/**
	 * Plays a sound for all players spectating this player if applicable
	 *
	 * @param p      The player causing the sound
	 * @param s      The sound to play
	 * @param volume Volume
	 * @param pitch  Pitch
	 */
	public static void playLocalSound(Player p, Sound s, float volume, float pitch) {
		if(M7tas.getFakePlayers().containsValue(p) && Spectate.getReverseSpectatorMap().containsKey(p)) {
			for(Player spectator : Spectate.getReverseSpectatorMap().get(p)) {
				spectator.playSound(spectator, s, volume, pitch);
			}
		} else {
			p.playSound(p, s, volume, pitch);
		}
	}

	/**
	 * Gets the nearest real player at the given location
	 *
	 * @param l The location
	 * @return The nearest real player
	 */
	public static @Nullable Player getNearestPlayer(Location l) {
		ArrayList<Player> playersInWorld = new ArrayList<>(l.getWorld().getEntitiesByClass(Player.class));
		if(playersInWorld.isEmpty()) {
			return null;
		}
		for(int i = 0; i < playersInWorld.size(); i++) {
			Player p = playersInWorld.get(i);
			if(p.getGameMode().equals(GameMode.SPECTATOR) && playersInWorld.size() > 1) {
				playersInWorld.remove(i);
				i--;
			}
			if(M7tas.getFakePlayers().containsValue(p)) {
				playersInWorld.remove(i);
				i--;
			}
		}
		playersInWorld.sort(Comparator.comparingDouble(o -> o.getLocation().distanceSquared(l)));
		return playersInWorld.getFirst();
	}

	public enum DebugType {
		CLIENT, SERVER, BOSS
	}

	public static void debug(DebugType type, String message) {
		switch(type) {
			case CLIENT -> Bukkit.broadcastMessage(ChatColor.AQUA + "[Client] " + message);
			case SERVER -> Bukkit.broadcastMessage(ChatColor.YELLOW + "[Server] " + message);
			case BOSS -> Bukkit.broadcastMessage(ChatColor.GREEN + "[Game] " + message);
		}
	}

	public static void changeName(LivingEntity entity) {
		if(!(entity instanceof Player)) {
			String[] oldName;
			int health = (int) Math.ceil(entity.getHealth() + entity.getAbsorptionAmount());
			int maxHealth = (int) Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getValue();
			try {
				oldName = Objects.requireNonNull(entity.getCustomName()).split(" ");
			} catch(Exception exception) {
				oldName = (entity.getName() + " " + ChatColor.YELLOW + health + "/" + maxHealth).split(" ");
			}
			oldName[oldName.length - 1] = ChatColor.YELLOW + "" + health + "/" + maxHealth;
			StringBuilder newName = new StringBuilder(oldName[0]);
			for(int i = 1; i < oldName.length; i++) {
				newName.append(" ").append(oldName[i]);
			}
			entity.setCustomName(newName.toString());
		}
	}

	public enum BlessingType {
		LIFE, POWER, STONE, WISDOM, TIME
	}
	
	public static String getRealName(Player p) {
		switch(p.getName()) {
			case "Archer" -> {
				return "akc0303";
			}
			case "Berserk" -> {
				return "AsapIcey";
			}
			case "Healer", "Mage3" -> {
				return "Meepy_";
			}
			case "Mage", "Mage1" -> {
				return "Beethoven_";
			}
			case "Tank", "Mage2" -> {
				return "cookiethebald";
			}
			default -> {
				return p.getName();
			}
		}
	}

	public static String round(double value, int places) {
		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.toPlainString();
	}

	public static void broadcastBlessing(Player p, BlessingType type, int level) {
		String message1 = ChatColor.GOLD + "" + ChatColor.BOLD + "DUNGEON BUFF!" + ChatColor.RESET + ChatColor.GOLD + " " + getRealName(p) + ChatColor.WHITE + " found a ";
		String romanLevel;
		switch(level) {
			case 1 -> romanLevel = "I";
			case 2 -> romanLevel = "II";
			case 5 -> romanLevel = "V";
			default -> {
				Bukkit.broadcastMessage(ChatColor.RED + "Error: Invalid level " + level);
				return;
			}
		}
		String message2;
		switch(type) {
			case LIFE -> {
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Life " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(1 + (level * 5.445 / 100), 2) + "x" + ChatColor.RED + " ❤ Health" + ChatColor.GRAY +
						" and " + ChatColor.GREEN + "+" + round(1 + (level * 5.445 / 100), 2) + "x" + ChatColor.RED + " ❣ Health Regen";
			}
			case POWER -> {
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Power " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.RED + " ❁ Strength" + ChatColor.GRAY +
						" and " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.BLUE + " ☠ Crit Damage";
			}
			case STONE -> {
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Stone " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(level * 7.26, 2) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x ❈ Defense" + ChatColor.GRAY +
						" and " + ChatColor.GREEN + "+" + round(level * 10.89, 1) + ChatColor.RED + " ❁ Damage";
			}
			case WISDOM -> {
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Wisdom " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.AQUA + " ✎ Intelligence" + ChatColor.GRAY +
						" and " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.WHITE + " ✦ Speed";
			}
			case TIME -> {
				if(level != 5) {
					Bukkit.broadcastMessage(ChatColor.RED + "Error: Blessing of Time can only be level 5");
					return;
				}
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Time " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.RED + " ❤ Health" + ChatColor.GRAY +
						", " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.AQUA + " ✎ Intelligence" + ChatColor.GRAY +
						", " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x ❈ Defense" + ChatColor.GRAY +
						", and " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.RED + " ❁ Strength";
			}
			default -> {
				Bukkit.broadcastMessage(ChatColor.RED + "Error: Invalid blessing type " + type);
				return;
			}
		}
		Bukkit.broadcastMessage(message1);
		Bukkit.broadcastMessage(message2);
	}
}