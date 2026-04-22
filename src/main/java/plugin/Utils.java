package plugin;

import commands.Spectate;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import nms.TASGamePacketListenerImpl;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

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

	public static void setSpeed(Player p, int speed) {
		var instance = p.getAttribute(Attribute.MOVEMENT_SPEED);
		NamespacedKey key = new NamespacedKey(M7tas.getInstance(), "speed");
		instance.removeModifier(new AttributeModifier(key, 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
		double modifier = (speed - 100) / 100.0;
		instance.addModifier(new AttributeModifier(key, modifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
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
	 * Simulates a packet sent from a Player to the server.
	 *
	 * @param player The player who sent the packet
	 * @param packet The packet to simulate
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void simulatePacket(Player player, Packet<?> packet) {
		if(!(player instanceof CraftPlayer craftPlayer)) return;

		Utils.debug(Utils.DebugType.CLIENT, player.getName() + " Sending Packet " + packet.getClass().getSimpleName() + (Utils.superVerbose ? (" at " + round(player.getLocation().getX(), 3) + " " + round(player.getLocation().getY(), 5) + " " + round(player.getLocation().getZ(), 3) + " " + player.getLocation().getYaw() + " " + player.getLocation().getPitch()) : ""));
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
		if(FakePlayerManager.getFakePlayers().containsValue(p) && Spectate.getReverseSpectatorMap().containsKey(p)) {
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
		if(FakePlayerManager.getFakePlayers().containsValue(p) && Spectate.getReverseSpectatorMap().containsKey(p)) {
			for(Player spectator : Spectate.getReverseSpectatorMap().get(p)) {
				spectator.playSound(spectator, s, volume, pitch);
			}
		} else {
			p.playSound(p, s, volume, pitch);
		}
	}

	public enum SecretType {
		CHEST, BLESSING_CHEST, ITEM, BAT, ESSENCE
	}

	public static void playSecretFoundSound(Player p, SecretType type) {
		Sound sound;
		switch(type) {
			case CHEST, BLESSING_CHEST -> sound = Sound.BLOCK_CHEST_OPEN;
			case ITEM -> sound = Sound.ENTITY_ITEM_PICKUP;
			case BAT -> sound = Sound.ENTITY_BAT_DEATH;
			case ESSENCE -> sound = Sound.BLOCK_NOTE_BLOCK_PLING;
			default -> {
				Bukkit.broadcastMessage(ChatColor.RED + "Error: Invalid secret type " + type);
				return;
			}
		}
		playLocalSound(p, sound, 2.0f, type == SecretType.ESSENCE ? 2.0f : 1.0f);
		if(type == SecretType.BLESSING_CHEST || type == SecretType.ESSENCE) {
			playRewardSequence(p);
		}
	}

	public static void playRewardSequence(Player p) {
		playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 2.0f, 0.793685f);
		scheduleTask(() -> playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.891f), 5);
		scheduleTask(() -> playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1f), 10);
		scheduleTask(() -> playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.12284f), 15);
		scheduleTask(() -> playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.18945f), 20);
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
			if(FakePlayerManager.getFakePlayers().containsValue(p)) {
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

	private static boolean verbose = true;
	private static boolean superVerbose = false;

	public static boolean isVerbose() {
		return verbose;
	}

	public static void setVerbose(boolean verbose) {
		Utils.verbose = verbose;
		if(!verbose) superVerbose = false;
	}

	public static boolean isSuperVerbose() {
		return superVerbose;
	}

	public static void setSuperVerbose(boolean superVerbose) {
		Utils.superVerbose = superVerbose;
		if(superVerbose) Utils.verbose = true;
	}

	public static void debug(DebugType type, String message) {
		if(!verbose) return;
		switch(type) {
			case CLIENT -> Bukkit.broadcastMessage(ChatColor.DARK_AQUA + "[Client] " + message);
			case SERVER -> Bukkit.broadcastMessage(ChatColor.GREEN + "[Server] " + message);
			case BOSS -> Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Game] " + message);
		}
	}

	// Scoreboard-tag → display-max in M for bosses whose displayed HP is decoupled from internal HP
	private static final Map<String, Double> BOSS_DISPLAY_MAX_M = Map.of(
			"TASMaxor", 800.0,
			"TASStorm", 1000.0,
			"TASGoldor", 1200.0,
			"TASNecron", 1400.0,
			"WitherKingDragon", 1000.0
	);

	public static String formatHealthM(LivingEntity entity) {
		double current = entity.getHealth() + entity.getAbsorptionAmount();
		for(String tag : entity.getScoreboardTags()) {
			Double displayMax = BOSS_DISPLAY_MAX_M.get(tag);
			if(displayMax == null) continue;
			double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
			double ratio = maxHealth > 0 ? Math.clamp(current / maxHealth, 0.0, 1.0) : 0.0;
			return formatDisplayM(displayMax * ratio);
		}
		return formatHealthM(current);
	}

	public static String formatHealthM(double rawHealth) {
		return formatDisplayM(rawHealth * 2);
	}

	private static String formatDisplayM(double displayM) {
		if(displayM >= 1000) {
			int tenths = (int) Math.round(displayM / 100.0); // round to nearest 0.1B
			if(tenths % 10 == 0) return (tenths / 10) + "B";
			return (tenths / 10) + "." + (tenths % 10) + "B";
		}
		return (int) Math.round(displayM) + "M"; // round to nearest 1M
	}

	public static void hurtEntity(LivingEntity entity, float damage, Player attacker) {
		CraftLivingEntity craftEntity = (CraftLivingEntity) entity;
		net.minecraft.world.entity.LivingEntity nmsEntity = craftEntity.getHandle();
		ServerLevel level = (ServerLevel) nmsEntity.level();
		if(nmsEntity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
			// EnderDragon.hurtServer() delegates to hurt() which rejects damage sources without
			// a Player entity or ALWAYS_HURTS_ENDER_DRAGONS tag. Using playerAttack() would pass
			// the check but causes infinite recursion via EntityDamageByEntityEvent → handleCustomItems.
			// Direct health manipulation avoids both issues.
			entity.setHealth(Math.max(0, (float)(entity.getHealth() - damage)));
		} else {
			nmsEntity.hurtServer(level, nmsEntity.damageSources().genericKill(), damage);
		}
	}

	public static void changeName(LivingEntity entity) {
		if(!(entity instanceof Player)) {
			String[] oldName;
			double health = entity.getHealth() + entity.getAbsorptionAmount();
			boolean exempt = entity.getScoreboardTags().stream().anyMatch(t -> t.equals("TASWitherKing") || t.equals("TASWatcher"));
			String healthStr = exempt ? String.valueOf(health) : formatHealthM(entity);
			try {
				oldName = Objects.requireNonNull(entity.getCustomName()).split(" ");
			} catch(Exception exception) {
				oldName = (entity.getName() + " " + ChatColor.RED + "❤" + ChatColor.YELLOW + healthStr).split(" ");
			}
			oldName[oldName.length - 1] = ChatColor.RED + "❤" + ChatColor.YELLOW + healthStr;
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
			case "Berserk", "Mage3" -> {
				return "Cubpletionist";
			}
			case "Mage4" -> {
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
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(1 + (level * 5.445 / 100), 2) + "x" + ChatColor.RED + " ❤ Health" + ChatColor.GRAY + " and " + ChatColor.GREEN + "+" + round(1 + (level * 5.445 / 100), 2) + "x" + ChatColor.RED + " ❣ Health Regen";
			}
			case POWER -> {
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Power " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.RED + " ❁ Strength" + ChatColor.GRAY + " and " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.BLUE + " ☠ Crit Damage";
			}
			case STONE -> {
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Stone " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(level * 7.26, 2) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x ❈ Defense" + ChatColor.GRAY + " and " + ChatColor.GREEN + "+" + round(level * 10.89, 1) + ChatColor.RED + " ❁ Damage";
			}
			case WISDOM -> {
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Wisdom " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.AQUA + " ✎ Intelligence" + ChatColor.GRAY + " and " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.WHITE + " ✦ Speed";
			}
			case TIME -> {
				if(level != 5) {
					Bukkit.broadcastMessage(ChatColor.RED + "Error: Blessing of Time can only be level 5");
					return;
				}
				message1 += ChatColor.LIGHT_PURPLE + "Blessing of Time " + romanLevel + ChatColor.WHITE + "!";
				message2 = ChatColor.GRAY + "     Granted you " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.RED + " ❤ Health" + ChatColor.GRAY + ", " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.AQUA + " ✎ Intelligence" + ChatColor.GRAY + ", " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x ❈ Defense" + ChatColor.GRAY + ", and " + ChatColor.GREEN + "+" + round(level * 7.26, 1) + ChatColor.GRAY + " & " + ChatColor.GREEN + "+" + round(1 + (level * 3.63 / 100), 2) + "x" + ChatColor.RED + " ❁ Strength";
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
