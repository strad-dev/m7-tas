package plugin;

import commands.Spectate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.advancements.predicates.BlockPredicate; // 26.2: moved from advancements.criterion
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.component.TooltipDisplay;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

// import nms.TASGamePacketListenerImpl; // TAS-only fake-player connection, disabled in the practice fork

public class Utils {
	/**
	 * Every one-shot task handed to {@link #scheduleTask} that has NOT run yet, so {@link #cancelAllScheduled()}
	 * can clear a previous run's lingering choreography.  Repeating tasks (boss tickers, aggro, spectator sync) use
	 * runTaskTimer and are intentionally NOT tracked here.
	 *
	 * <p>Self-pruning: each task removes its own entry as it fires.  This is load-bearing, not tidiness:
	 * {@code CraftScheduler.cancelTask} walks the whole pending queue AND enqueues a cancellation task per call, so
	 * cancelling N tasks is O(N * queue). This map used to be an append-only list holding every task ever scheduled
	 * in the session (CustomItems schedules one per damage event, so hundreds/second), and cancelling ~100k dead
	 * entries froze the main thread past the 60s watchdog on the next {@code /m7practice}.
	 *
	 * <p>Main-thread only, so no synchronisation.
	 */
	private static final Map<Integer, org.bukkit.scheduler.BukkitTask> scheduledTasks = new LinkedHashMap<>();

	/**
	 * Bumped by {@link #cancelAllScheduled()}. A task captures the generation it was scheduled under and no-ops if
	 * it no longer matches, so choreography can never bleed into the next run even if its cancel didn't land.
	 */
	private static int scheduleGeneration = 0;

	// ===== Adventure item name/lore helpers (26.2: ItemMeta's String name/lore methods are @Deprecated) =====
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
	private static final MiniMessage MM = MiniMessage.miniMessage();

	/** Item display-name / lore line from a MiniMessage string, with the default item italic suppressed (vanilla look). */
	public static Component mm(String s) {
		return MM.deserialize("<!italic>" + s);
	}

	/** Chat message / entity custom-name component from a MiniMessage string (no forced italic). */
	public static Component msg(String s) {
		return MM.deserialize(s);
	}

	/** Chat component from a MiniMessage template with tag resolvers. Use Placeholder.unparsed(...) for untrusted or
	 *  arbitrary input (player names, chat text, debug payloads) so it is inserted literally and cannot inject tags. */
	public static Component msg(String template, TagResolver... resolvers) {
		return MM.deserialize(template, resolvers);
	}

	/** Plain (un-styled) text of a component, e.g. reading a custom name for a comparison. */

	public static String plain(Component c) {
		return c == null ? "" : PlainTextComponentSerializer.plainText().serialize(c);
	}

	/** Legacy §-coded string of a component, for Bukkit APIs that only accept a String (e.g. boss bar titles). */
	public static String legacyString(Component c) {
		return c == null ? "" : LEGACY.serialize(c);
	}

	/** MiniMessage string of a component, to round-trip a Component back through the MiniMessage helpers. */
	public static String mmString(Component c) {
		return c == null ? "" : MM.serialize(c);
	}

	/** Legacy §-string rendered from a MiniMessage string, for String-only Bukkit APIs (e.g. boss bar titles)
	*  while keeping the source free of deprecated ChatColor. */
	public static String mmLegacy(String miniMessage) {
		return LEGACY.serialize(MM.deserialize(miniMessage));
	}

	/** Component from a legacy §-string.  Replaces {@code meta.setDisplayName(s)} → {@code meta.displayName(Utils.nameComponent(s))}. */
	public static Component nameComponent(String legacy) {
		// Vanilla italicises custom item names; legacy setDisplayName did not.  Default to non-italic, keeping an explicit §o.
		return LEGACY.deserialize(legacy).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
	}

	/** Components from legacy §-strings.  Replaces {@code meta.setLore(list)} → {@code meta.lore(Utils.loreComponents(list))}. */
	public static List<Component> loreComponents(List<String> legacy) {
		List<Component> out = new ArrayList<>(legacy.size());
		for(String s : legacy) out.add(LEGACY.deserialize(s).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
		return out;
	}

	/** Legacy §-string of an item's display name, or "" if none.  Replaces {@code meta.getDisplayName()}. */
	public static String displayName(ItemMeta meta) {
		Component c = meta.displayName();
		return c == null ? "" : LEGACY.serialize(c);
	}

	/** Legacy §-strings of an item's lore, or an empty list if none.  Replaces {@code meta.getLore()}. */
	public static List<String> lore(ItemMeta meta) {
		List<Component> l = meta.lore();
		if(l == null) return new ArrayList<>();
		List<String> out = new ArrayList<>(l.size());
		for(Component c : l) out.add(LEGACY.serialize(c));
		return out;
	}

	/** Plain (un-styled) first lore line, which is the custom-item ID.  It is styling-independent, so the ID lookup
	*  can't be broken by lore formatting such as the non-italic default.  Replaces the old getLore().getFirst(). */
	public static String firstLorePlain(ItemMeta meta) {
		List<Component> l = meta.lore();
		return l == null || l.isEmpty() ? "" : PlainTextComponentSerializer.plainText().serialize(l.getFirst());
	}


	/**
	 * Wrapper for Bukkit.getScheduler().runTaskLater(Plugin, Runnable, long)
	 *
	 * @param task  The task to run later.
	 * @param delay In how many ticks this task should be run.
	 */
	public static void scheduleTask(Runnable task, long delay) {
		if(!M7tas.getInstance().isEnabled()) return;
		final int generation = scheduleGeneration;
		final int[] id = new int[1]; // filled in below; runTaskLater always defers at least a tick, so it's set by then
		org.bukkit.scheduler.BukkitTask handle = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			scheduledTasks.remove(id[0]);
			if(generation != scheduleGeneration) return; // a run started or ended after this was queued, so drop it

			task.run();
		}, delay);
		id[0] = handle.getTaskId();
		scheduledTasks.put(id[0], handle);
	}

	/**
	 * Cancel every *pending* one-shot task scheduled via {@link #scheduleTask}. Called at the start of a run so a
	 * previous run's still-queued dialogue/choreography (e.g. a player routine's broadcasts) can't fire into it.
	 * Bumping the generation is what actually guarantees that; the cancels are just to keep the scheduler queue
	 * from carrying dead weight.
	 */
	public static void cancelAllScheduled() {
		scheduleGeneration++;
		List<org.bukkit.scheduler.BukkitTask> live = new ArrayList<>(scheduledTasks.values());
		scheduledTasks.clear(); // before cancelling, so a task firing mid-loop can't mutate the map we copied from
		for(org.bukkit.scheduler.BukkitTask t : live) {
			if(t != null && !t.isCancelled()) t.cancel();
		}
	}

	public static void setSpeed(Player p, int speed) {
		var instance = p.getAttribute(Attribute.MOVEMENT_SPEED);
		NamespacedKey key = new NamespacedKey(M7tas.getInstance(), "speed");
		instance.removeModifier(new AttributeModifier(key, 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
		double modifier = (speed - 100) / 100.0;
		instance.addModifier(new AttributeModifier(key, modifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
	}

	/** Toggle a 50% movement-speed debuff (a separate modifier that composes with {@link #setSpeed}). Used while a
	 *  player carries a Wither-King relic without a Cow Hat equipped. */
	public static void setRelicDebuff(Player p, boolean active) {
		var instance = p.getAttribute(Attribute.MOVEMENT_SPEED);
		if(instance == null) return;
		NamespacedKey key = new NamespacedKey(M7tas.getInstance(), "relic_debuff");
		instance.removeModifier(new AttributeModifier(key, 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
		if(active) {
			instance.addModifier(new AttributeModifier(key, -0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
		}
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
		// TAS fake-player custom-connection branch removed (no fake players in the practice fork);
		// real players always use the normal connection.
		((Packet) packet).handle(serverPlayer.connection);
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
	 * Sends an action bar to every real player, spectators included; the fakes are skipped (they have no client).
	 * <p>
	 * The home of the boss-phase tick-timer HUDs (Storm's pad/crush counters, Maxor's laser/stun, Necron's
	 * interludes), which is why it lives here rather than as a private copy per boss: there is exactly one
	 * action-bar slot, so anything writing it every tick has to agree about who it writes to.
	 * Pass {@link Component#empty()} to clear it.
	 *
	 * @param bar The action bar to send
	 */
	public static void broadcastActionBar(Component bar) {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(FakePlayerManager.getFakePlayers().containsValue(p)) continue;
			p.sendActionBar(bar);
		}
	}

	/** The NMS server via the non-deprecated CraftServer bridge ({@code MinecraftServer.getServer()} is deprecated).
	 *  Returns the same instance, so callers (tick counters, command source) are behaviour-identical. */
	private static MinecraftServer nmsServer() {
		return ((org.bukkit.craftbukkit.CraftServer) Bukkit.getServer()).getServer();
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
		MinecraftServer server = nmsServer();
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
		meta.displayName(Utils.nameComponent(name));
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
		item.addUnsafeEnchantment(Enchantment.PROTECTION, 5);
		// Armour has no lore ID, so StatLore keeps a blank lore line 0 and appends its stat rows below it - which
		// is what leaves Catalog.paletteKey's third component the empty string it has always been (§7b).
		return damage.StatLore.apply(item);
	}

	/**
	 * Return a copy of {@code item} that can break ANY block while its holder is in adventure mode.  Stamps the
	 * vanilla {@code minecraft:can_break} component with a single empty block-predicate: no block, state or nbt
	 * filter, so it matches every block.  This mirrors how SkyBlock's Dungeonbreaker bypasses adventure-mode block
	 * protection.  Apply LAST, after any setItemMeta, since this mutates the NMS copy directly.
	 */
	public static ItemStack breakAnyBlockInAdventure(ItemStack item) {
		net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
		nms.set(DataComponents.CAN_BREAK, new AdventureModePredicate(List.of(BlockPredicate.Builder.block().build())));
		// The empty predicate has no concrete block to name, so the client renders a "Can Break: Unknown"
		// tooltip line. Hide the CAN_BREAK component from the tooltip to suppress it.
		nms.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CAN_BREAK, true));
		return CraftItemStack.asBukkitCopy(nms);
	}

	/**
	 * Return a copy of {@code item} that can be placed on Stone Bricks while its holder is in adventure mode,
	 * the practice default.  Stamps the vanilla {@code minecraft:can_place_on} component, which renders a
	 * "Can be placed on: Stone Bricks" tooltip.  Apply LAST, after any setItemMeta, since it mutates the NMS copy.
	 */
	public static ItemStack placeOnStoneBricksInAdventure(ItemStack item) {
		net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
		// Use the static built-in block registry as the HolderGetter.  That avoids RegistryAccess.lookupOrThrow,
		// whose signature differs between the Spigot-mojang compile target and the Paper runtime (NoSuchMethodError).
		BlockPredicate stoneBricks = BlockPredicate.Builder.block()
				.of(net.minecraft.core.registries.BuiltInRegistries.BLOCK, net.minecraft.world.level.block.Blocks.STONE_BRICKS)
				.build();
		nms.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(List.of(stoneBricks)));
		return CraftItemStack.asBukkitCopy(nms);
	}

	/**
	 * Return a copy of {@code item} that can be placed on ANY block while its holder is in adventure mode. Stamps the
	 * vanilla {@code minecraft:can_place_on} component with a single empty block-predicate (no filter → matches every
	 * block), the same trick as {@link #breakAnyBlockInAdventure}. Apply LAST (after any setItemMeta), since it mutates
	 * the NMS copy directly.
	 */
	public static ItemStack placeOnAnythingInAdventure(ItemStack item) {
		net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
		nms.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(List.of(BlockPredicate.Builder.block().build())));
		// The empty predicate has no concrete block to name, so the client renders a "Can be placed on: Unknown"
		// tooltip line. Hide the CAN_PLACE_ON component from the tooltip to suppress it.
		nms.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CAN_PLACE_ON, true));
		return CraftItemStack.asBukkitCopy(nms);
	}

	/**
	 * Return a copy of {@code item} that can be BOTH placed on and "broken" against any block while its holder is in
	 * adventure mode, i.e. {@link #placeOnAnythingInAdventure} and {@link #breakAnyBlockInAdventure} in a single
	 * stamp.  It has to be one call: each of those writes its own {@code TOOLTIP_DISPLAY}, so chaining them would
	 * clobber the first one's entry and un-hide its "Unknown" tooltip line.
	 * <p>
	 * The can_break half is NOT about breaking blocks.  Superboom TNT's left-click never breaks anything, since the
	 * interact event is always cancelled.  It exists so the CLIENT will tell us WHICH block was left-clicked: verified in the
	 * 26.2 client, {@code MultiPlayerGameMode.startDestroyBlock} returns early without sending
	 * {@code ServerboundPlayerActionPacket} when {@code Player.blockActionRestricted} is true, and in adventure mode
	 * that's true unless the held stack's can_break predicate matches the block. Without the stamp a left-click sends
	 * only a swing packet (no block position) and the server would have to ray-trace the target itself.
	 * <p>
	 * Apply LAST (after any setItemMeta), since it mutates the NMS copy directly.
	 */
	public static ItemStack placeAndBreakAnythingInAdventure(ItemStack item) {
		ItemStack copy = withoutBlockBreakSpeed(item);
		net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(copy);
		nms.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(List.of(BlockPredicate.Builder.block().build())));
		nms.set(DataComponents.CAN_BREAK, new AdventureModePredicate(List.of(BlockPredicate.Builder.block().build())));
		// Empty predicates name no concrete block, so the client would render "Can be placed on: Unknown" and
		// "Can Break: Unknown".  Hide both components from the tooltip.  The break-speed modifier below would render
		// its own "-1024 Block Break Speed" line, so hide the attribute block too.
		nms.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT
				.withHidden(DataComponents.CAN_PLACE_ON, true)
				.withHidden(DataComponents.CAN_BREAK, true)
				.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true));
		return CraftItemStack.asBukkitCopy(nms);
	}

	/**
	 * Return a copy of {@code item} that cannot break blocks at all, by cancelling out the base BLOCK_BREAK_SPEED
	 * every player joins with. {@code JoinListener} sets that base to 1024 (destroy speed is MULTIPLIED by it, so
	 * everything breaks in one tick with anything); this hangs a matching −1024 on the item for the MAINHAND, so
	 * while it is held the attribute is 1024 − 1024 = 0 and the multiplier zeroes the destroy speed outright.
	 * <p>
	 * Applied by {@link #placeAndBreakAnythingInAdventure} to every can_break item EXCEPT Dungeonbreaker (which
	 * uses {@link #breakAnyBlockInAdventure} and adds its own +1024 on top instead).  Those items only carry
	 * can_break so the CLIENT reports which block was left-clicked, and that packet is gated on the predicate, not
	 * on destroy speed.  Zeroing the speed keeps Superboom's targeting while making the break itself impossible
	 * client-side too, instead of relying on {@code CustomItems.onBlockBreak} to refuse it after the fact.
	 * <p>
	 * NOTE: setting any explicit attribute modifier drops the item's DEFAULT modifiers (vanilla replaces the whole
	 * component), so don't route a weapon through this without re-adding its damage.
	 */
	public static ItemStack withoutBlockBreakSpeed(ItemStack item) {
		ItemStack copy = item.clone();
		ItemMeta meta = copy.getItemMeta();
		if(meta == null) return copy;
		meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(
				new NamespacedKey(M7tas.getInstance(), "no_break_speed"), -1024,
				AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
		copy.setItemMeta(meta);
		return copy;
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

	// LivingEntity#getHurtSound is protected, so it is resolved lazily.  Method.invoke dispatches
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
				Bukkit.broadcast(msg("<red>Error: Invalid secret type " + type));
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

	/**
	 * True if {@code p} is WATCHING rather than running: vanilla spectator mode, which is the idle state on m7
	 * (the network plugin parks everyone who isn't in the current party there), or a real player spectating a fake.
	 * <br>
	 * <b>Every player-driven dungeon mechanic must check this, and vanilla will not do it for you.</b>  Bukkit
	 * still fires the interact events for a spectator's clicks, and a click on a block with a {@code MenuProvider}
	 * (a chest) or on an ENTITY hitbox is not even pre-cancelled, so a spectator could otherwise take a secret,
	 * solve a Goldor device, pocket a Wither-King relic or walk off with a Maxor Energy Crystal - the last two
	 * being permanent griefs, since the entity is removed from the arena and the item lands in a spectator's
	 * inventory where the running party can never reach it.  M7's own packet interceptor bypasses vanilla
	 * entirely, so the check cannot live in vanilla either.  Put it at the mechanic's chokepoint where there is
	 * one ({@code Maxor.pickUp}, {@code WitherKing.pickUpRelic}), so a future caller inherits it.
	 */
	public static boolean isSpectator(Player p) {
		return p == null || p.getGameMode() == GameMode.SPECTATOR || commands.Spectate.isSpectating(p);
	}

	public enum DebugType {
		CLIENT, SERVER, BOSS, ERROR
	}

	/**
	 * Verbosity ladder, ascending.  Each level is a superset of the one below.
	 * OFF: silent.  TIMER: only tick-timer announcements.  ON: timers plus full [Client]/[Server]/[Game]
	 * debug and the movement audit.  SUPER: adds packet coordinates and movement residual tracking.
	 */
	public enum VerboseLevel {OFF, TIMER, ON, SUPER}

	private static VerboseLevel verboseLevel = VerboseLevel.TIMER;

	public static VerboseLevel getVerboseLevel() {
		return verboseLevel;
	}

	public static void setVerboseLevel(VerboseLevel level) {
		verboseLevel = level;
	}

	/**
	 * Full debug stream ([Client]/[Server]/[Game] packet logging, movement audit): ON and SUPER.
	 */
	public static boolean isVerbose() {
		return verboseLevel.ordinal() >= VerboseLevel.ON.ordinal();
	}

	/**
	 * Tick-timer announcements (section/boss timing lines): TIMER, ON, and SUPER.
	 */
	public static boolean showTimers() {
		return verboseLevel.ordinal() >= VerboseLevel.TIMER.ordinal();
	}

	/**
	 * Packet coordinates + movement residual tracking: SUPER only.
	 */
	public static boolean isSuperVerbose() {
		return verboseLevel == VerboseLevel.SUPER;
	}

	/**
	 * Server tick captured when the current phase began; basis for the {@code [tick: N]} prefix on verbose lines.
	 */
	private static int phaseStartTick = 0;
	/**
	 * Server tick the live overall-run timer was anchored at (see {@link #markRunStart()}).
	 */
	private static int runStartTick = 0;
	/**
	 * False once {@link #markRunStart()} arms the run timer, until the next {@link #markPhaseStart()} anchors it.
	 */
	private static boolean runStarted = false;

	/**
	 * Mark the start of a new phase.  This resets the {@code [tick: N]} counter shown on every verbose line.  The
	 * first phase start after {@link #markRunStart()} also anchors the live overall-run timer.
	 */
	public static void markPhaseStart() {
		int now = nmsServer().getTickCount();
		phaseStartTick = now;
		if(!runStarted) {
			runStartTick = now;
			runStarted = true;
		}
	}

	/**
	 * Arm a fresh live overall-run timer: the next {@link #markPhaseStart()} (the run's first phase) anchors it.
	 * Used by /m7practice, whose "Overall" timer is live rather than the hardcoded per-phase cumulative offset.
	 */
	public static void markRunStart() {
		runStarted = false;
	}

	/**
	 * Ticks elapsed since the live overall-run timer was anchored (see {@link #markRunStart()}).
	 */
	public static int runTick() {
		return nmsServer().getTickCount() - runStartTick;
	}

	/**
	 * Ticks elapsed since the last {@link #markPhaseStart()}, the value rendered in the verbose-line prefix.
	 */
	public static int phaseTick() {
		return nmsServer().getTickCount() - phaseStartTick;
	}

	/**
	 * Broadcast a tick-timer line.  Shown only at TIMER level and above (see {@link #showTimers()}).
	 */
	public static void timer(String message) {
		// `message` is a MiniMessage string built by callers (timer lines carry their own colors).
		if(showTimers()) Bukkit.broadcast(msg(message));
	}

	public static void debug(DebugType type, String message) {
		// The debug payload is inserted as an UNPARSED placeholder so arbitrary content (entity names, coords, a
		// stray '<') is shown literally and can never break MiniMessage parsing or inject tags.
		TagResolver m = Placeholder.unparsed("m", message);
		// ERROR always fires.  It flags a misuse or a bug, not routine debug output, so it ignores the verbosity gate.
		if(type == DebugType.ERROR) {
			// Errors always carry a [tick: #] stamp regardless of verbosity, so the misuse can be pinpointed.
			Bukkit.broadcast(msg("<gray>[tick: " + phaseTick() + "] <red>[Error] <m>", m));
			return;
		}
		if(!isVerbose()) return;
		String prefix = isSuperVerbose() ? "<gray>[tick: " + phaseTick() + "] " : "";
		switch(type) {
			case CLIENT -> Bukkit.broadcast(msg(prefix + "<dark_aqua>[Client] <m>", m));
			case SERVER -> Bukkit.broadcast(msg(prefix + "<green>[Server] <m>", m));
			case BOSS -> Bukkit.broadcast(msg(prefix + "<light_purple>[Game] <m>", m));
			case ERROR -> { /* handled above */ }
		}
	}

	/**
	 * Displayed HP for a mob's nameplate / boss bar, e.g. {@code "800M"} or {@code "1.4B"}.
	 * <p>
	 * Internal HP IS the SkyBlock value divided by {@link damage.Scale#SB_PER_MC_HP}, so display is just
	 * {@code internal x 1M} and there is nothing to decouple.  This used to carry a per-boss display-max table plus
	 * a x2 fudge, because internal HP was hand-tuned (Maxor sat at 300 while displaying 800M) and the two numbers
	 * had no relationship; MAP.md §8 retires all of that.
	 */
	public static String formatHealthM(LivingEntity entity) {
		// Withers flagged as dying always display "1" regardless of internal HP.
		if(entity.getScoreboardTags().contains("TASDying")) return "1";
		return formatHealthM(entity.getHealth() + entity.getAbsorptionAmount());
	}

	/** Displayed HP for a raw internal health value.  Internal is SB/1e6, so one point of health is one million. */
	public static String formatHealthM(double rawHealth) {
		return formatDisplayM(rawHealth);
	}

	private static String formatDisplayM(double displayM) {
		if(displayM >= 1000) {
			int tenths = (int) Math.round(displayM / 100.0); // round to nearest 0.1B
			if(tenths % 10 == 0) return (tenths / 10) + "B";
			return (tenths / 10) + "." + (tenths % 10) + "B";
		}
		return (int) Math.round(displayM) + "M"; // round to nearest 1M
	}

	// hurtEntity lived here.  It is now damage/Damage.deal - one path for every damage instance, applying damage
	// by reading health, subtracting and setting it (MAP.md §7).  Three paths used to exist
	// (hurtServer(genericKill), setHealth for dragons, wither.damage() for arrows-on-withers) with different
	// i-frame, armor, event and aggro behaviour, and the split was already visible as workarounds here.
	// The worn-head damage multiplier that used to sit alongside it (Cow Hat x0.70, masks x0.85) is deleted, not
	// re-bucketed: helmet-slot exclusivity in the stat layer models the same thing properly (§1.10, §8).

	public static void changeName(LivingEntity entity) {
		if(!(entity instanceof Player)) {
			double health = entity.getHealth() + entity.getAbsorptionAmount();
			boolean exempt = entity.getScoreboardTags().stream().anyMatch(t -> t.equals("TASWitherKing") || t.equals("TASWatcher"));
			String healthStr = exempt ? String.valueOf(health) : formatHealthM(entity);
			// <!bold> so the health suffix isn't bold (legacy §e/§c reset bold; MiniMessage color tags do not).
			String healthTag = "<!bold><yellow>" + healthStr + "<red>❤";
			Component current = entity.customName();
			if(current == null) {
				entity.customName(msg(entity.getName() + " " + healthTag));
				return;
			}
			// Replace the last space-delimited token (the health suffix) in place, preserving the colored base name.
			String[] parts = MM.serialize(current).split(" ");
			parts[parts.length - 1] = healthTag;
			entity.customName(msg(String.join(" ", parts)));
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

	/**
	 * {@link #round(double, int)} with thousands separators on the integer part - {@code "3,729.6"} rather than
	 * {@code "3729.6"}.  Used by the stat readouts (item lore, {@code /eq}), where a dungeon-scaled stat is routinely
	 * five or six digits.
	 * <p>
	 * Grouped by hand rather than with a {@code DecimalFormat}, so it inherits {@code round}'s exact
	 * {@code BigDecimal} half-up behaviour instead of introducing a second rounding rule.
	 */
	public static String roundCommas(double value, int places) {
		String s = round(value, places);
		boolean negative = s.startsWith("-");
		if(negative) s = s.substring(1);
		int dot = s.indexOf('.');
		String whole = dot < 0 ? s : s.substring(0, dot);
		String frac = dot < 0 ? "" : s.substring(dot);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < whole.length(); i++) {
			if(i > 0 && (whole.length() - i) % 3 == 0) sb.append(',');
			sb.append(whole.charAt(i));
		}
		return (negative ? "-" : "") + sb + frac;
	}

	public static void broadcastBlessing(Player p, BlessingType type, int level) {
		String message1 = "<gold><bold>DUNGEON BUFF!<reset><gold> " + getRealName(p) + "<white> found a ";
		String romanLevel;
		switch(level) {
			case 1 -> romanLevel = "I";
			case 2 -> romanLevel = "II";
			case 5 -> romanLevel = "V";
			default -> {
				Bukkit.broadcast(msg("<red>Error: Invalid level " + level));
				return;
			}
		}
		String message2;
		switch(type) {
			case LIFE -> {
				message1 += "<light_purple>Blessing of Life " + romanLevel + "<white>!";
				message2 = "<gray>     Granted you <green>+" + round(1 + (level * 5.445 / 100), 2) + "x<red> ❤ Health<gray> and <green>+" + round(1 + (level * 5.445 / 100), 2) + "x<red> ❣ Health Regen";
			}
			case POWER -> {
				message1 += "<light_purple>Blessing of Power " + romanLevel + "<white>!";
				message2 = "<gray>     Granted you <green>+" + round(level * 7.26, 1) + "<gray> & <green>+" + round(1 + (level * 3.63 / 100), 2) + "x<red> ❁ Strength<gray> and <green>+" + round(level * 7.26, 1) + "<gray> & <green>+" + round(1 + (level * 3.63 / 100), 2) + "x<blue> ☠ Crit Damage";
			}
			case STONE -> {
				message1 += "<light_purple>Blessing of Stone " + romanLevel + "<white>!";
				message2 = "<gray>     Granted you <green>+" + round(level * 7.26, 2) + "<gray> & <green>+" + round(1 + (level * 3.63 / 100), 2) + "x ❈ Defense<gray> and <green>+" + round(level * 10.89, 1) + "<red> ❁ Damage";
			}
			case WISDOM -> {
				message1 += "<light_purple>Blessing of Wisdom " + romanLevel + "<white>!";
				message2 = "<gray>     Granted you <green>+" + round(level * 7.26, 1) + "<gray> & <green>+" + round(1 + (level * 3.63 / 100), 2) + "x<aqua> ✎ Intelligence<gray> and <green>+" + round(level * 7.26, 1) + "<white> ✦ Speed";
			}
			case TIME -> {
				if(level != 5) {
					Bukkit.broadcast(msg("<red>Error: Blessing of Time can only be level 5"));
					return;
				}
				message1 += "<light_purple>Blessing of Time " + romanLevel + "<white>!";
				message2 = "<gray>     Granted you <green>+" + round(level * 7.26, 1) + "<gray> & <green>+" + round(1 + (level * 3.63 / 100), 2) + "x<red> ❤ Health<gray>, <green>+" + round(level * 7.26, 1) + "<gray> & <green>+" + round(1 + (level * 3.63 / 100), 2) + "x<aqua> ✎ Intelligence<gray>, <green>+" + round(level * 7.26, 1) + "<gray> & <green>+" + round(1 + (level * 3.63 / 100), 2) + "x ❈ Defense<gray>, and <green>+" + round(level * 7.26, 1) + "<gray> & <green>+" + round(1 + (level * 3.63 / 100), 2) + "x<red> ❁ Strength";
			}
			default -> {
				Bukkit.broadcast(msg("<red>Error: Invalid blessing type " + type));
				return;
			}
		}
		Bukkit.broadcast(msg(message1));
		Bukkit.broadcast(msg(message2));
	}
}
