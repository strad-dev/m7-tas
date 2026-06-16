package plugin;

import commands.Spectate;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.component.TooltipDisplay;
import nms.TASGamePacketListenerImpl;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
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
	/** Every one-shot task handed to {@link #scheduleTask} — so {@link #cancelAllScheduled()} can clear a previous
	 *  run's lingering choreography. Repeating tasks (boss tickers, aggro, spectator sync) use runTaskTimer and are
	 *  intentionally NOT tracked here. */
	private static final List<org.bukkit.scheduler.BukkitTask> scheduledTasks = new ArrayList<>();

	public static void scheduleTask(Runnable task, long delay) {
		if(!M7tas.getInstance().isEnabled()) return;
		scheduledTasks.add(Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), task, delay));
	}

	/** Cancel every pending one-shot task scheduled via {@link #scheduleTask}. Called at the start of a run so a
	 *  previous run's still-queued dialogue/choreography (e.g. a player routine's broadcasts) can't fire into it. */
	public static void cancelAllScheduled() {
		for(org.bukkit.scheduler.BukkitTask t : scheduledTasks) {
			if(t != null && !t.isCancelled()) t.cancel();
		}
		scheduledTasks.clear();
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
		Spectate.snapSpectatorsToFake(p);
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

		Utils.debug(Utils.DebugType.CLIENT, player.getName() + " Sending Packet " + packet.getClass().getSimpleName() + (Utils.isSuperVerbose() ? (" at " + round(player.getLocation().getX(), 3) + " " + round(player.getLocation().getY(), 5) + " " + round(player.getLocation().getZ(), 3) + " " + player.getLocation().getYaw() + " " + player.getLocation().getPitch()) : ""));
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
	 * Runs a server command without any output. Vanilla normally broadcasts a command's success
	 * to every op as "[Server: ...]" chat spam; {@code withSuppressedOutput()} sets the source
	 * silent so {@code sendSuccess}/{@code sendFailure} short-circuit before that broadcast (this
	 * works regardless of the logAdminCommands gamerule, which doesn't reliably suppress it).
	 * Use this in place of {@code Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ...)} for the
	 * plugin's own world edits (setblock/fill/clone) and entity commands (tag/kill).
	 *
	 * @param command Command to run, without a leading slash
	 */
	public static void runCommand(String command) {
		MinecraftServer server = MinecraftServer.getServer();
		CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
		server.getCommands().performPrefixedCommand(source, command);
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

	/** Return a copy of {@code item} that can break ANY block while its holder is in adventure mode. Stamps the
	 *  vanilla {@code minecraft:can_break} component with a single empty block-predicate — no block/state/nbt filter,
	 *  so it matches every block. Mirrors how SkyBlock's Dungeonbreaker bypasses adventure-mode block protection.
	 *  Apply LAST (after any setItemMeta), since this mutates the NMS copy directly. */
	public static ItemStack breakAnyBlockInAdventure(ItemStack item) {
		net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
		nms.set(DataComponents.CAN_BREAK, new AdventureModePredicate(List.of(BlockPredicate.Builder.block().build())));
		// The empty predicate has no concrete block to name, so the client renders a "Can Break: Unknown"
		// tooltip line. Hide the CAN_BREAK component from the tooltip to suppress it.
		nms.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CAN_BREAK, true));
		return CraftItemStack.asBukkitCopy(nms);
	}

	/** Return a copy of {@code item} that can be placed on Stone Bricks while its holder is in adventure mode
	 *  (the practice default). Stamps the vanilla {@code minecraft:can_place_on} component — renders a
	 *  "Can be placed on: Stone Bricks" tooltip. Apply LAST (after any setItemMeta), since it mutates the NMS copy. */
	public static ItemStack placeOnStoneBricksInAdventure(ItemStack item) {
		net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
		net.minecraft.core.HolderGetter<net.minecraft.world.level.block.Block> blocks =
				net.minecraft.server.MinecraftServer.getServer().registryAccess()
						.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK);
		BlockPredicate stoneBricks = BlockPredicate.Builder.block()
				.of(blocks, net.minecraft.world.level.block.Blocks.STONE_BRICKS).build();
		nms.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(List.of(stoneBricks)));
		return CraftItemStack.asBukkitCopy(nms);
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

	/**
	 * Plays a sound by namespaced key for all players spectating this player if applicable
	 *
	 * @param p      The player causing the sound
	 * @param s      The namespaced sound key (e.g. "minecraft:entity.wither.hurt")
	 * @param volume Volume
	 * @param pitch  Pitch
	 */
	public static void playLocalSound(Player p, String s, float volume, float pitch) {
		if(FakePlayerManager.getFakePlayers().containsValue(p) && Spectate.getReverseSpectatorMap().containsKey(p)) {
			for(Player spectator : Spectate.getReverseSpectatorMap().get(p)) {
				spectator.playSound(spectator.getLocation(), s, volume, pitch);
			}
		} else {
			p.playSound(p.getLocation(), s, volume, pitch);
		}
	}

	// LivingEntity#getHurtSound is protected — resolved lazily; Method.invoke dispatches
	// virtually, so subclass overrides (wither, dragon, zombie...) return their own sounds.
	private static java.lang.reflect.Method getHurtSoundMethod;

	/**
	 * Resolves the namespaced key of the sound an entity makes when hurt
	 * (e.g. "minecraft:entity.wither.hurt"), or null if it cannot be resolved.
	 */
	@Nullable
	public static String getHurtSoundKey(LivingEntity entity) {
		try {
			net.minecraft.world.entity.LivingEntity nmsEntity = ((CraftLivingEntity) entity).getHandle();
			if(getHurtSoundMethod == null) {
				getHurtSoundMethod = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("getHurtSound", net.minecraft.world.damagesource.DamageSource.class);
				getHurtSoundMethod.setAccessible(true);
			}
			Object soundEvent = getHurtSoundMethod.invoke(nmsEntity, nmsEntity.damageSources().genericKill());
			if(soundEvent == null) return null;
			// SoundEvent's ResourceLocation accessor is location() on record builds, getLocation() on older ones
			for(String name : new String[]{"location", "getLocation"}) {
				try {
					return soundEvent.getClass().getMethod(name).invoke(soundEvent).toString();
				} catch(NoSuchMethodException ignored) {
				}
			}
			return null;
		} catch(ReflectiveOperationException e) {
			return null;
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
		CLIENT, SERVER, BOSS, ERROR
	}

	/** Verbosity ladder, ascending. Each level is a superset of the one below:
	 *  OFF — silent; TIMER — only tick-timer announcements; ON — timers + full [Client]/[Server]/[Game]
	 *  debug + movement audit; SUPER — adds packet coordinates + movement residual tracking. */
	public enum VerboseLevel { OFF, TIMER, ON, SUPER }

	private static VerboseLevel verboseLevel = VerboseLevel.ON;

	public static VerboseLevel getVerboseLevel() {
		return verboseLevel;
	}

	public static void setVerboseLevel(VerboseLevel level) {
		verboseLevel = level;
	}

	/** Full debug stream ([Client]/[Server]/[Game] packet logging, movement audit): ON and SUPER. */
	public static boolean isVerbose() {
		return verboseLevel.ordinal() >= VerboseLevel.ON.ordinal();
	}

	/** Tick-timer announcements (section/boss timing lines): TIMER, ON, and SUPER. */
	public static boolean showTimers() {
		return verboseLevel.ordinal() >= VerboseLevel.TIMER.ordinal();
	}

	/** Packet coordinates + movement residual tracking: SUPER only. */
	public static boolean isSuperVerbose() {
		return verboseLevel == VerboseLevel.SUPER;
	}

	/** Server tick captured when the current phase began; basis for the {@code [tick: N]} prefix on verbose lines. */
	private static int phaseStartTick = 0;
	/** Server tick the live overall-run timer was anchored at (see {@link #markRunStart()}). */
	private static int runStartTick = 0;
	/** False once {@link #markRunStart()} arms the run timer, until the next {@link #markPhaseStart()} anchors it. */
	private static boolean runStarted = false;

	/** Mark the start of a new phase — resets the {@code [tick: N]} counter shown on every verbose line. The first
	 *  phase start after {@link #markRunStart()} also anchors the live overall-run timer. */
	public static void markPhaseStart() {
		int now = MinecraftServer.getServer().getTickCount();
		phaseStartTick = now;
		if(!runStarted) {
			runStartTick = now;
			runStarted = true;
		}
	}

	/** Arm a fresh live overall-run timer: the next {@link #markPhaseStart()} (the run's first phase) anchors it.
	 *  Used by /practice, whose "Overall" timer is live rather than the hardcoded per-phase cumulative offset. */
	public static void markRunStart() {
		runStarted = false;
	}

	/** Ticks elapsed since the live overall-run timer was anchored (see {@link #markRunStart()}). */
	public static int runTick() {
		return MinecraftServer.getServer().getTickCount() - runStartTick;
	}

	/** Ticks elapsed since the last {@link #markPhaseStart()} — the value rendered in the verbose-line prefix. */
	public static int phaseTick() {
		return MinecraftServer.getServer().getTickCount() - phaseStartTick;
	}

	/** Broadcast a tick-timer line — shown only at TIMER level and above (see {@link #showTimers()}). */
	public static void timer(String message) {
		if(showTimers()) Bukkit.broadcastMessage(message);
	}

	public static void debug(DebugType type, String message) {
		// ERROR always fires — it flags a misuse/bug, not routine debug output, so it ignores the verbosity gate.
		if(type == DebugType.ERROR) {
			// Errors always carry a [tick: #] stamp regardless of verbosity, so the misuse can be pinpointed.
			Bukkit.broadcastMessage(ChatColor.GRAY + "[tick: " + phaseTick() + "] " + ChatColor.RED + "[Error] " + message);
			return;
		}
		if(!isVerbose()) return;
		String prefix = isSuperVerbose() ? ChatColor.GRAY + "[tick: " + phaseTick() + "] " : "";
		switch(type) {
			case CLIENT -> Bukkit.broadcastMessage(prefix + ChatColor.DARK_AQUA + "[Client] " + message);
			case SERVER -> Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "[Server] " + message);
			case BOSS -> Bukkit.broadcastMessage(prefix + ChatColor.LIGHT_PURPLE + "[Game] " + message);
			case ERROR -> { /* handled above */ }
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
		// Withers flagged as dying always display "1" regardless of internal HP.
		if(entity.getScoreboardTags().contains("TASDying")) return "1";
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
		// Boss aggro: a mage beam / hurtEntity hit uses a no-source damage type (no EntityDamageByEntityEvent), so
		// record the attacker here for the last-damager aggro target.
		if(attacker != null && entity instanceof org.bukkit.entity.Wither && entity.getScoreboardTags().contains("TASWither")) {
			instructions.bosses.WitherActions.noteDamager(attacker);
		}
		if(nmsEntity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
			// EnderDragon.hurtServer() delegates to hurt() which rejects damage sources without
			// a Player entity or ALWAYS_HURTS_ENDER_DRAGONS tag. Using playerAttack() would pass
			// the check but causes infinite recursion via EntityDamageByEntityEvent → handleCustomItems.
			// Direct health manipulation avoids both issues.
			entity.setHealth(Math.max(0, (float)(entity.getHealth() - damage)));
			// Wither-King dragon kill chokepoint: dragon damage never fires an EntityDamageEvent (we setHealth
			// directly), so detect the kill here and hand off to the death/spawn-next logic (idempotent).
			if(entity instanceof org.bukkit.entity.EnderDragon dragon
					&& dragon.getScoreboardTags().contains("WitherKingDragon") && entity.getHealth() <= 0) {
				instructions.bosses.witherking.WitherKing.handleDragonKilled(dragon);
			}
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
				oldName = (entity.getName() + " " + ChatColor.YELLOW + healthStr + ChatColor.RED + "❤").split(" ");
			}
			oldName[oldName.length - 1] = ChatColor.YELLOW + healthStr + ChatColor.RED + "❤";
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
