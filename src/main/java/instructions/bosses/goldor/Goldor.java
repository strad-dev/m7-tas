package instructions.bosses.goldor;

import instructions.Actions;
import instructions.bosses.WitherLord;
import instructions.bosses.necron.Necron;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

@SuppressWarnings("DataFlowIssue")
public final class Goldor extends WitherLord {
	public static final Goldor INSTANCE = new Goldor();

	private static final int PRE_GOLDOR_TICKS = 2416;

	private Goldor() {
		register(this);
	}

	/** Static facade for the boss-chain. */
	public static void goldorInstructions(World world, boolean doContinue) {
		INSTANCE.start(world, doContinue);
	}

	@Override protected String name() { return "Goldor"; }
	@Override protected String displayName() { return "Goldor"; }
	@Override protected Location spawnLocation() { return new Location(world, 80.5, 116, 40.5, -90f, 0f); }
	@Override protected double maxHealth() { return 700; }
	@Override protected String displayHealth() { return "1.2B"; }
	@Override protected int previousTicks() { return PRE_GOLDOR_TICKS; }

	@Override
	protected void resetState() {
		// Goldor has no per-fight state beyond what the base class handles.
	}

	@Override
	protected void onStart() {
		sendChatMessage("Who dares trespass into my domain?");
		Actions.forceMove(boss, new Vector(0.1, 0, 0), 200);
		Utils.scheduleTask(() -> sendChatMessage("Little ants, plotting and scheming, thinking they are invincibile..."), 60);
		Utils.scheduleTask(() -> sendChatMessage("I won't let you break the factory core, I gave my life to my Master."), 120);
		Utils.scheduleTask(() -> sendChatMessage("No one matches me in close quarters."), 180);
		Utils.scheduleTask(() -> Actions.turnHead(boss, 0f, 0f), 199);
		Utils.scheduleTask(() -> Actions.forceMove(boss, new Vector(0, 0, 0.1), 38), 200);
		Utils.scheduleTask(() -> {
			sendChatMessage("You have done it, you destroyed the factory...");
			Actions.turnHead(boss, 94.722f, 0f);
			Actions.forceMove(boss, new Vector(-0.7931, 0, -0.0655), 52);
			setArmor(false);
		}, 238);
		Utils.scheduleTask(() -> {
			sendChatMessage("...");
			Bukkit.broadcastMessage(ChatColor.GREEN + "Goldor killed in 54 ticks (2.70 seconds) | Terminals + Goldor: 290 ticks (14.50 seconds) | Overall: 2 706 ticks (135.30 seconds)");
		}, 290);
		Utils.scheduleTask(() -> sendChatMessage("But you have nowhere to hide anymore!"), 298);
		Utils.scheduleTask(() -> {
			sendChatMessage("Necron, forgive me.");
			Bukkit.broadcastMessage(ChatColor.GREEN + "Terminals + Goldor finished in 350 ticks (17.50 seconds) | Overall: 2 766 ticks (135.30 seconds)");
			chainNext(doContinue);
		}, 350);
		Utils.scheduleTask(() -> sendChatMessage("YOU ARE FACE TO FACE WITH GOLDOR!"), 358);
		Utils.scheduleTask(boss::remove, 450);
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			Necron.necronInstructions(world, true);
		}
	}

	public static void broadcastTerminalComplete(Player p, String type, int count, int total) {
		String message;
		if(type.equals("gate")) {
			message = ChatColor.GREEN + "The gate has been destroyed!";
		} else {
			message = ChatColor.GOLD + Utils.getRealName(p) + ChatColor.GREEN + " activated a " + type + "! (" + ChatColor.RED + count + ChatColor.GREEN + "/" + total + ")";
		}
		Bukkit.broadcastMessage(message);
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.sendTitle("", message, 0, 40, 0);
		}
		Utils.playGlobalSound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
	}
}
