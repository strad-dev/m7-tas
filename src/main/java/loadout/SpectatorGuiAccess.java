package loadout;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import plugin.M7tas;

import java.lang.reflect.Field;

/**
 * Lets a player in SPECTATOR mode edit the loadout GUI WITHOUT ever leaving spectator mode.
 *
 * <p>Vanilla refuses container clicks from spectators: {@code ServerGamePacketListenerImpl#handleContainerClick}
 * calls {@code player.isSpectator()} and, when true, resyncs the menu and returns, and it does that BEFORE any
 * Bukkit {@code InventoryClickEvent} fires, so there is nothing a plugin can hook to allow it. Idle players on the
 * m7 server sit in spectator (the network plugin's {@code M7Bridge.makeSpectator}), which is why {@code /m7loadout}
 * would open there but nothing inside could be moved.
 *
 * <p>This netty handler consumes the container-click packet and re-runs vanilla's own
 * {@code handleContainerClick} on the main thread, wrapped in a SILENT server-side game-mode flip.
 * {@link ServerPlayerGameMode} {@code gameModeForPlayer} is written directly by reflection, never through
 * {@code setGameModeForPlayer} or {@code changeGameModeForPlayer}, which broadcast the change and fire the Bukkit
 * event.  So {@code isSpectator()} reads false for exactly that one call and the client is never told anything.
 * For every other tick of server logic the player is still a spectator: no gravity, no collision, no visibility
 * change, no {@code PlayerGameModeChangeEvent}.
 *
 * <p>The flip and the call MUST sit in one synchronous block. {@code handleContainerClick} opens with
 * {@code PacketUtils.ensureRunningOnSameThread}, so when vanilla reads the packet off the netty thread it
 * RE-QUEUES the work onto the main thread, meaning a "clear" queued before {@code super.channelRead} and a
 * "restore" queued after it do not reliably sandwich the handler. Invoking the handler ourselves from a
 * main-thread task sidesteps the ordering question entirely.
 *
 * <p>Consuming the packet is safe: nothing else on the pipeline wants container clicks, since my own
 * {@code tas_interceptor} only touches movement, use and attack packets, and a non-spectator still gets the click
 * processed normally, it just skips the flip.
 *
 * <p>Installed only while a spectator actually has the editor open ({@link #install}/{@link #uninstall} from
 * {@link LoadoutEditor}), so normal players never carry this handler.
 *
 * <p>NOTE: the network plugin has an identical copy ({@code loadout/SpectatorGuiAccess.java}) for the servers M7
 * isn't on, so keep them in sync.  The handler NAMES differ deliberately so the two can never collide on one pipeline.
 */
public final class SpectatorGuiAccess extends ChannelDuplexHandler {
	/** Deliberately not the network plugin's name ({@code straddev_spectator_gui}), since both may exist on m7. */
	private static final String HANDLER_NAME = "m7_spectator_gui";

	/** {@code ServerPlayerGameMode#gameModeForPlayer}, written directly so the flip never reaches the client. */
	private static Field gameModeField;
	private static boolean fieldUnavailable;

	private final Player player;

	private SpectatorGuiAccess(Player player) {
		this.player = player;
	}

	// ===== install / uninstall =====

	/** Arm the bypass for a spectator opening the editor. No-op for anyone not in spectator mode. */
	public static void install(Player p) {
		if(p.getGameMode() != GameMode.SPECTATOR) return;
		if(field() == null) {
			// Mappings moved: leave vanilla behaviour alone rather than break the GUI, but SAY so.  Silently
			// no-oping here looks identical to "the editor is broken", which is exactly the wrong thing to hide.
			M7tas.getInstance().getLogger().warning("Spectator GUI bypass unavailable: "
					+ "ServerPlayerGameMode#gameModeForPlayer not found. Spectators can't edit loadouts.");
			return;
		}
		try {
			Channel ch = channel(p);
			if(ch.pipeline().get(HANDLER_NAME) == null) {
				ch.pipeline().addBefore("packet_handler", HANDLER_NAME, new SpectatorGuiAccess(p));
			}
		} catch(Exception e) {
			M7tas.getInstance().getLogger().warning("Could not arm the spectator GUI bypass for "
					+ p.getName() + ": " + e);
		}
	}

	/** Always safe to call, whether or not the bypass was armed. */
	public static void uninstall(Player p) {
		try {
			Channel ch = channel(p);
			if(ch.pipeline().get(HANDLER_NAME) != null) ch.pipeline().remove(HANDLER_NAME);
		} catch(Exception ignored) {
			// channel already closed
		}
	}

	private static Channel channel(Player p) {
		return ((CraftPlayer) p).getHandle().connection.connection.channel;
	}

	// ===== interception =====

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		MinecraftServer server = MinecraftServer.getServer();
		if(msg instanceof ServerboundContainerClickPacket click && server != null) {
			// Consume the packet and run vanilla's handler ourselves, on the main thread, with the spectator flag
			// cleared for exactly that call. See the class comment for why this cannot be done by bracketing
			// super.channelRead with two queued tasks.
			server.execute(() -> handleAsNonSpectator(click));
			return;
		}
		super.channelRead(ctx, msg);
	}

	/** Main thread. Run the click as a non-spectator, then put the real mode straight back. */
	private void handleAsNonSpectator(ServerboundContainerClickPacket click) {
		ServerPlayer nms = handle();
		if(nms == null) return;
		GameType prev = nms.gameMode.getGameModeForPlayer();
		// Only flip if they're actually a spectator at this instant, since a practice pull may already have moved
		// them to adventure.  That way I can never wrongly force them back into spectator afterwards.
		boolean flipped = prev == GameType.SPECTATOR;
		if(flipped) write(nms, GameType.ADVENTURE);
		try {
			nms.connection.handleContainerClick(click);
		} finally {
			if(flipped) write(nms, prev);
		}
	}

	private ServerPlayer handle() {
		return player.isOnline() ? ((CraftPlayer) player).getHandle() : null;
	}

	private static void write(ServerPlayer nms, GameType type) {
		Field f = field();
		if(f == null) return;
		try {
			f.set(nms.gameMode, type);
		} catch(IllegalAccessException ignored) {
			// setAccessible succeeded at lookup time; nothing useful to do here
		}
	}

	private static Field field() {
		if(gameModeField == null && !fieldUnavailable) {
			try {
				gameModeField = ServerPlayerGameMode.class.getDeclaredField("gameModeForPlayer");
				gameModeField.setAccessible(true);
			} catch(NoSuchFieldException | RuntimeException e) {
				fieldUnavailable = true;
			}
		}
		return gameModeField;
	}
}
