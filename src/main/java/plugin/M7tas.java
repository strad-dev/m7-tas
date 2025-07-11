package plugin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.datafixers.util.Pair;
import instructions.*;
import instructions.Server;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.a;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EnumItemSlot;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R3.profile.CraftPlayerProfile;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class M7tas extends JavaPlugin implements CommandExecutor, Listener {
	/**
	 * A map that tracks spectator relationships between players.
	 * The key represents a player in spectator mode, and the value represents
	 * the player they are spectating. This structure is used to manage and
	 * retrieve the spectator-target associations dynamically within the plugin.
	 */
	private static final Map<Player, Player> spectatorMap = new HashMap<>();

	/**
	 * A mapping that tracks reverse spectator relationships in the game.
	 * Each key represents a player being spectated, and the value is a list
	 * of players who are currently spectating the key player.
	 * <br>
	 * The map is used to manage and evaluate the spectator system state,
	 * ensuring that the relationship between spectating players and their
	 * spectated target is properly maintained.
	 */
	private static final HashMap<Player, List<Player>> reverseSpectatorMap = new HashMap<>();

	/**
	 * A static map used to store fake player instances associated with their names.
	 * This map is primarily used for managing NPC-like entities (fake players)
	 * that are spawned and controlled programmatically within the server.
	 *<br>
	 * The keys in the map represent the unique names of the fake players, while
	 * the values are corresponding Player objects representing their in-game entities.
	 *<br>
	 * This map is immutable (final) and thread-safe due to its static nature.
	 */
	private static final Map<String, Player> fakePlayers = new HashMap<>();

	private static Plugin plugin;

	private boolean fakeTickerStarted = false;

	/*
	 * Archer - akc0303
	 * Berserk - AsapIcey
	 * Healer - Meepy_
	 * Mage - Beethoven_
	 * Tank - cookiethebald
	 */
	private static final Map<String, String> SKIN_DATA = Map.of("Archer", "0b0fa6bc-69ee-4f6c-a4f8-7cac79f1871a", "Berserk", "dff79c40-6aeb-458a-86cf-6789e1831317", "Healer", "6715b245-be6e-496c-87eb-1d2c19066403", "Mage", "cdb9e9c6-c096-4f58-9c49-35395d7b897c", "Tank", "5d142c3a-bdf1-418b-b907-797bbaaed188");

	/**
	 * Retrieves the player that the given player is currently spectating.
	 *
	 * @param player the player whose spectating target is being retrieved
	 * @return the player being spectated by the given player, or null if the player is not spectating anyone
	 */
	public static List<Player> getSpectatingPlayer(Player player) {
		return reverseSpectatorMap.getOrDefault(player, new ArrayList<>());
	}

	public static List<Player> getFakePlayers() {
		return new ArrayList<>(fakePlayers.values());
	}

	private ItemStack getCustomHead(String displayName, String identifier, String textureValue, String textureSignature) {
		GameProfile gp = new GameProfile(UUID.randomUUID(), identifier);
		gp.getProperties().put("textures", new Property("textures", textureValue, textureSignature));

		CraftPlayerProfile profile = new CraftPlayerProfile(gp);

		ItemStack helmet = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) helmet.getItemMeta();
		assert meta != null;
		meta.setDisplayName(displayName);
		meta.setOwnerProfile(profile);
		helmet.setItemMeta(meta);
		return helmet;
	}

	public ItemStack getDiamondHead() {
		return getCustomHead(ChatColor.RED + "Ancient Diamond Necron Head", "dNecronHead", "ewogICJ0aW1lc3RhbXAiIDogMTYxNzM5MDg5NDU4NiwKICAicHJvZmlsZUlkIiA6ICJjNWFhNTRhNmNmNTI0YmFmYmRiODUwNmUyMjRiNzViZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJOZWNyb25IYW5kbGVQTFMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGIxMTVjZGM0NWZkODRmMjFmYmE3YWMwZjJiYzc3YmMzYjYzMDJiZTY3MDg0MmY2ZTExZjY2ZWI1NTdmMTNlZSIKICAgIH0KICB9Cn0=", "hwaaE9h0QvFmtFvk2bNyAYlPo34NvaCjX3VWPv5zaRVM8KjfqQB5sB85vlFRVwCXK/HnQS2qMJ39ZuYplxT895lIShidIVdF5UP8T6cb1svhA9TmEVKFY4pKFyhUjIhD95HvO3OoNWPlCmb9Mho4XIo3K4AavnKPbuu3/I58gQmfKI71xDq7r+DRf9Dlxc8r3mcsUrdEwTfvC2/eFszHc/vqQXNm1smH2QJVfki+AgddNndFt7qumeicVFmsk2GmPNHxjlgH0xPL0hG8WGEmH5+Fnnj/eoYSutnpDRXVPY0H/KOMIa2Prga524stPC0gYmVU9y/wviXzDmKiiAa4uPVhwd/L/DgUSIGio6NlLMyA+Uvyy02HEr3TmzQ6bPqLphSttaDaVWW8Ltd1wvz/+Hhii5tYSSm3l5cAZQAO1O/JN/FKqA7tv0v0ZWp8AS1qw0QeLrRrLKlri2Zmzj5iYv7exfAVUiYB8f95ZZOWg1FLOufSJeFsQC5S7gsnsdJsWvnwvUNQI4RDfIc59a5Hvgzr90jgMTNoBGSXyrsXpeXJb+T9R8xfSEQY5V1XwFd+3lz8XRbBUQHubxN+b9AGj5FpQ2j5oaAz+BXY2+Iq20qVvkFMeJXdTRT0VIZM4r06ml0R3SZ1Jfui4xMH9OmhR+Hz3mmMvLN+BewhmtucN28=");
	}

	public ItemStack getStormHelmet() {
		return getCustomHead("§6Ancient Storm's Helmet", "stormHelmet", "ewogICJ0aW1lc3RhbXAiIDogMTc0NjgwNzcwMTU2MSwKICAicHJvZmlsZUlkIiA6ICJjNDIzYjQwMWZiOGU0ODc3YjMzMmVmMjhiZDdlZGZmZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFjdGlvbkJyaW5lWVQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTliYWVjNWZiNGNkOWRjNTk2ZDYxMGI2YzZiZDM4YWI5OTAxYjY4Yzk1OTQ5ZTJkNzFiOTI1MzE3MjcwZDAxMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "s8X+QmhjqwppG9pqW9SYQloIzPVTw3PBpMprwnx9pl9j2uNdBgJbpwhahgo3WjpXOV9aiewogO7HDqZ71fns/rkPLVBANO6mlnYS8J+J8rLkQFiinQERx4ucYtHM9atzZnG7dDv6QTK6Bvur8SwVhZIOYSj7YWN1ecrbm9RskNhiRSXVwFH/TcWdSv4z/c0zG2b+OXaD68NAwxTd8lszNl+JSWFU6dP/l8GP1EWDNz8WagfwzeOTaHU2rDztRCUXlNGeF16QdZBXgFUva3Kel6D0QSE492Q1vTt5f55xwk38Yjbw6wkv2se+arcd9sbInuxlJamev6J4FX0r1QhGpgxHDvu30O/htK7ni8Og4AWgESQg/ONo/R7GUYsysao3lV46cHGK9JBEQEG0Zlq+gQ9ajzLojLchfSMM03/V8FpyLKBsplMJuG3NNz4QLXlflWU3UpuXD7SDGgIcn4UVRlANhC/Nj2qO4DUVkMA6V3OSGFWdLe9ICMZfLPXQiGFkZd4SmJLp6dy/Z2C7DGZci7qSkTXBW8j1Zmz52dSvaNqQvb10nSS+EVG8yggniRMheW8s8d6fs4fwrXfj+so2ayTjtImr8eafK1CpIARWCDEXZhQEs/rFv4dpuRaziJw69eVpem0ZwMRe7V4bf98SA5+yxgdYMtxoWUi+uMvKC8U=");
	}

	public ItemStack getBonzoMask() {
		return getCustomHead(ChatColor.DARK_PURPLE + "Ancient Bonzo's Mask", "bonzoMask", "ewogICJ0aW1lc3RhbXAiIDogMTc0NjgxMDU3MDM4NCwKICAicHJvZmlsZUlkIiA6ICJhZTg3MzEyNjBmMzY0ZWE2YjU3YTRkYjI5Mjk1YTA1OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGdW50aW1lX0ZveHlfMTkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQ2MDFmNzM2YmQ0MmE3Zjg0YzU4ZGUzY2YxMjBhZTRhZTYwZmViODJiMDM3ZThjYjBkMjhhMWUyMTYxODc5ZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "MBSY1gYYDruisd3+61+sS9xWwBABXOkLgcNwhZOhSNMyAE4yyhEGomaMT5hNckff6KyKBerAMJkWBK8i6kmmEyYKcQfb2jVSFWzQOCZmreGr7n/PEs4hXsGTGXLI1NCEqRpyv2kxUjsnjpDsmJQicSXd2Q/z5NpuC9VwG1mnz7+nzzJxxIx5QtzoLDKrXjpfJtNwGgq+0k0m7lJYIeyjXOCvCgnZO1VyAvmYLIo1DD/4IXCVqErAlRouLhzjJrBNSz95rMr/sQ0T5qFsclzcMTydeti9Pb5j+OhXDavkGFkrsfEpiXzQnW82ZqQ+2ZL8FYVyIEV+0z4kbrbXf4bcLtxQZskKNe/8xN5+UE9KdBcFQ0nF1EEM8Ia+9ChpcGggMqJAq/Zs3Vd1L37/JA5ahZtZqyXS3azKw6Lfh0UWkh+c64svuJI0XJVNNG1cTdGg6CVV37D2UkfHk6dAIlP/7XybHj0ZB3Ew8hThCi48EK0RH37fQvbbujRBjuxFGvU8l5ON4iZRkV+7qyCgLmnhYXGDMsEoGcfAT3m0m2i0+CVH6jitRz6PlbWKhdhT3TW5lBF82TO2QF+muzDD9yTpT7v6YUTQOmISHm/svXnbCp2+du79iijYbW2iQaM8r8ahkC83Owbuhhkgd+SgbTY7JwyBlR7U9j4TBzL+h+Advqw=");
	}

	public ItemStack getSpiritMask() {
		ItemStack helmet = getCustomHead(ChatColor.LIGHT_PURPLE + "Ancient Spirit Mask", "spiritMask", "ewogICJ0aW1lc3RhbXAiIDogMTcwODQ5MDg0MTAzMywKICAicHJvZmlsZUlkIiA6ICJmZWY5ZDJmY2NkODE0MzJhYjA3NWRhMzBkNWZlMjdmMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJKb2VuVE5UIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M3Yzc1YzQ3MDI2YzI3YjlkY2Y5MTQyNzY2NmFiMWRhZTdjNDgzNTVlZDJkNGI5MTgyZWRmMjg0YzIzMmM1YzAiCiAgICB9CiAgfQp9", "TetuPlRZ+JwBiWh7uOqE0/394o1M5duQUMhdMDxf2MX5wCMW1nnRrMZpUG7dXiE6RXUt/pZNepD/JxKpwcWBVRXfcaaQWlT44oX6XRFuDhJsyrmbEjIXSmVM46CelVlI0oyyo0a2uEve7bkYyl20Cax/5tQ1zzRWOTSRzZVkR3X8BLlWhmmgCNKNVsrS3B1TSs4OTKL3Z4t1/OhmyXdCGBp8DLfNbBqhgpVAEkTvzNdGV3e6+/dwbRSs/DjOK/ZXBug589JPY4eiNU8GsmtURVvBa88gr7ug67MYtqY4d4If6147QK6oPoWzp4blRNxVWJHRJSWo+XTSwa4yp21jaPNBQHYyX4zy2MnyYHV7Yhyc64hrV/uO1+t6CA4W07vOqth5zFxan5SUmV/lE+mh2q2uGr3ipAZSkkk7vOoWZvPCG6bo3ZdCsjEx8cQxpw/BXDkpuanySt71oFk3C/NSJCvsifZB52WQkJMK2M+LQCwRhCiN1a5QFWjxAdNINXC87qJAzIOgNkyq69OPPASGC9Hau9lED8iawkRO+rAeCfhczxtCBNLQPN/Gs7zE0MMzChUyK1uWO8MK+Vd+98RmA3vb3VHeShAzQriNJMIDFarNKWt9U+S3DkevXn8lMUEpyKpVH+sQy3DZ3z952JNk8ByPEPd2Lxfc8g1QbkNyLBo=");
		SkullMeta meta = (SkullMeta) helmet.getItemMeta();
		assert meta != null;

		List<String> lore = new ArrayList<>();
		lore.add("I couldn't find the actual Spirit Mask skin");
		lore.add("online so this is the next best thing.");
		meta.setLore(lore);
		helmet.setItemMeta(meta);
		return helmet;
	}

	public ItemStack getSkyBlockItem(Material material, String name, String id) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		meta.setDisplayName(name);
		List<String> lore = new ArrayList<>();
		lore.add(id);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private void spawnAllFakes(World world) {
		// clear any old ones
		kickAllFakes();
		fakePlayers.clear();

		for(var entry : SKIN_DATA.entrySet()) {
			String role = entry.getKey();
			String skin = entry.getValue();

			Player fake = spawnFakePlayer(world, role, UUID.fromString(skin));
			fakePlayers.put(role, fake);
		}
		startFakePlayerTicker();
	}

	private void setInventories() {
		for(String entry : fakePlayers.keySet()) {
			Player p = fakePlayers.get(entry);

			p.getScoreboardTags().clear();
			Objects.requireNonNull(Objects.requireNonNull(plugin.getServer().getScoreboardManager()).getMainScoreboard().getObjective("Intelligence")).getScore(p.getName()).setScore(2500);
			p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 255, true, false));
			p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, -1, 255, true, false));
			p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 3, true, false));
			p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 255, true, false));
			Objects.requireNonNull(p.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).setBaseValue(1);
			Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(40);
			p.setHealth(40);
			p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, -1, 2, true, false));

			// Set Inventory
			ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
			pearls.setAmount(16);
			ItemStack pickaxe = getSkyBlockItem(Material.DIAMOND_PICKAXE, ChatColor.BLUE + "Scraped Diamond Pickaxe", "");
			pickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, 10);
			ItemStack treecap = getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.LIGHT_PURPLE + "Toil Treecapitator", "");
			treecap.addUnsafeEnchantment(Enchantment.EFFICIENCY, 5);
			PlayerInventory inventory = p.getInventory();
			inventory.clear();

			switch(entry) {
				case "Archer", "Berserk", "Healer", "Tank" -> {
					ItemStack chestplate = Utils.createLeatherArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(231, 65, 80), ChatColor.LIGHT_PURPLE + "Ancient Necron's Chestplate");
					ItemStack leggings = Utils.createLeatherArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(231, 92, 60), ChatColor.LIGHT_PURPLE + "Ancient Necron's Leggings");
					ItemStack boots = Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.fromRGB(231, 110, 60), ChatColor.LIGHT_PURPLE + "Ancient Necron's Boots");

					inventory.setHelmet(getDiamondHead());
					inventory.setChestplate(chestplate);
					inventory.setLeggings(leggings);
					inventory.setBoots(boots);
				}
				case "Mage" -> {
					ItemStack chestplate = Utils.createLeatherArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(23, 147, 196), ChatColor.LIGHT_PURPLE + "Ancient Storm's Chestplate");
					ItemStack leggings = Utils.createLeatherArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(23, 168, 196), ChatColor.LIGHT_PURPLE + "Ancient Storm's Leggings");
					ItemStack boots = Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.fromRGB(28, 212, 228), ChatColor.LIGHT_PURPLE + "Ancient Storm's Boots");

					inventory.setHelmet(getStormHelmet());
					inventory.setChestplate(chestplate);
					inventory.setLeggings(leggings);
					inventory.setBoots(boots);
				}
			}

			// Common items for all roles
			inventory.setItem(0, getSkyBlockItem(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "Heroic Hyperion", "skyblock/combat/scylla"));
			inventory.setItem(1, getSkyBlockItem(Material.DIAMOND_SHOVEL, ChatColor.GOLD + "Warped Aspect of the Void", "skyblock/combat/aspect_of_the_void"));
			inventory.setItem(5, pickaxe);
			inventory.setItem(6, getSkyBlockItem(Material.BLAZE_ROD, ChatColor.GOLD + "Gyrokinetic Wand", "skyblock/combat/gyro"));
			inventory.setItem(7, pearls);
			inventory.setItem(8, getSkyBlockItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Menu (Click)", ""));
			inventory.setItem(28, getSkyBlockItem(Material.BREEZE_ROD, ChatColor.DARK_PURPLE + "Bonzo Staff", "skyblock/combat/bonzo_staff"));
			inventory.setItem(29, getSkyBlockItem(Material.BLAZE_ROD, ChatColor.DARK_PURPLE + "Tactical Insertion", "skyblock/combat/tactical_insertion"));

			switch(entry) {
				case "Archer" -> {
					//noinspection DuplicatedCode
					inventory.setItem(2, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", ""));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", ""));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(9, getBonzoMask());
					inventory.setItem(10, getSpiritMask());
					inventory.setItem(30, getSkyBlockItem(Material.BOW, ChatColor.GOLD + "Rapid Death Bow", ""));
					inventory.setItem(32, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", ""));
					inventory.setItem(33, getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.DARK_PURPLE + "Withered Ragnarok Axe", ""));
					inventory.setItem(34, getSkyBlockItem(Material.HOPPER, ChatColor.GOLD + "Weirder Tuba", ""));
					inventory.setItem(35, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Pitchin' Rod of the Sea", ""));
				}
				case "Berserk" -> {
					//noinspection DuplicatedCode
					inventory.setItem(2, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", ""));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", ""));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(9, getBonzoMask());
					inventory.setItem(10, getSpiritMask());
					inventory.setItem(11, getSkyBlockItem(Material.CHAINMAIL_BOOTS, ChatColor.LIGHT_PURPLE + "Renowned Spring Boots", ""));
					inventory.setItem(33, getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.DARK_PURPLE + "Withered Ragnarok Axe", ""));
					inventory.setItem(34, getSkyBlockItem(Material.HOPPER, ChatColor.GOLD + "Weirder Tuba", ""));
					inventory.setItem(35, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Pitchin' Rod of the Sea", ""));
				}
				case "Healer" -> {
					inventory.setItem(2, getSkyBlockItem(Material.STICK, ChatColor.GOLD + "Heroic Ice Spray Wand", "skyblock/combat/ice_spray_wand"));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", ""));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(9, getBonzoMask());
					inventory.setItem(10, getSpiritMask());
					inventory.setItem(30, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", ""));
					inventory.setItem(32, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Withered Flaming Flay", "skyblock/combat/flaming_flay"));
					inventory.setItem(33, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", ""));
					inventory.setItem(34, treecap);
					inventory.setItem(35, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Pitchin' Rod of the Sea", ""));
				}
				case "Mage" -> {
					inventory.setItem(2, getSkyBlockItem(Material.STICK, ChatColor.GOLD + "Heroic Ice Spray Wand", "skyblock/combat/ice_spray_wand"));
					inventory.setItem(3, getSkyBlockItem(Material.GOLDEN_SWORD, ChatColor.LIGHT_PURPLE + "Gilded Midas Sword", ""));
					inventory.setItem(4, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", ""));
					inventory.setItem(9, getDiamondHead());
					inventory.setItem(10, getSkyBlockItem(Material.CHAINMAIL_BOOTS, ChatColor.LIGHT_PURPLE + "Renowned Spring Boots", ""));
					inventory.setItem(30, getSkyBlockItem(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "Withered Hyperion", "skyblock/combat/scylla"));
					inventory.setItem(32, getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.DARK_PURPLE + "Withered Ragnarok Axe", ""));
					inventory.setItem(33, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", ""));
				}
				case "Tank" -> {
					inventory.setItem(2, getSkyBlockItem(Material.STICK, ChatColor.GOLD + "Heroic Ice Spray Wand", "skyblock/combat/ice_spray_wand"));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", ""));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(30, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", ""));
					inventory.setItem(31, getSkyBlockItem(Material.DIAMOND_AXE, ChatColor.LIGHT_PURPLE + "Withered Axe of the Shredded", "skyblock/combat/axe_of_the_shredded"));
					inventory.setItem(32, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Withered Flaming Flay", "skyblock/combat/flaming_flay"));
					inventory.setItem(33, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", ""));
				}
			}

			CraftPlayer player = (CraftPlayer) p;
			EntityPlayer nmsPlayer = player.getHandle();

			net.minecraft.world.item.ItemStack helmet = CraftItemStack.asNMSCopy(Objects.requireNonNull(inventory).getHelmet());
			net.minecraft.world.item.ItemStack chestplate = CraftItemStack.asNMSCopy(Objects.requireNonNull(inventory).getChestplate());
			net.minecraft.world.item.ItemStack leggings = CraftItemStack.asNMSCopy(Objects.requireNonNull(inventory).getLeggings());
			net.minecraft.world.item.ItemStack boots = CraftItemStack.asNMSCopy(Objects.requireNonNull(inventory).getBoots());
			net.minecraft.world.item.ItemStack hand = CraftItemStack.asNMSCopy(inventory.getItemInMainHand());

			nmsPlayer.setItemSlot(EnumItemSlot.f, helmet, false);
			nmsPlayer.setItemSlot(EnumItemSlot.e, chestplate, false);
			nmsPlayer.setItemSlot(EnumItemSlot.d, leggings, false);
			nmsPlayer.setItemSlot(EnumItemSlot.c, boots, false);
			nmsPlayer.setItemSlot(EnumItemSlot.a, hand, false);

			List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> gear = List.of(Pair.of(EnumItemSlot.f, helmet), Pair.of(EnumItemSlot.e, chestplate), Pair.of(EnumItemSlot.d, leggings), Pair.of(EnumItemSlot.c, boots), Pair.of(EnumItemSlot.a, hand));

			PacketPlayOutEntityEquipment equipmentPacket = new PacketPlayOutEntityEquipment(nmsPlayer.ar(), gear);

			Utils.broadcastPacket(equipmentPacket);
		}
	}

	@Override
	public void onEnable() {
		plugin = this;

		// register ALL our commands on the same executor
		for(var cmd : List.of("spectate", "unspectate", "tas", "reset")) {
			Objects.requireNonNull(getCommand(cmd)).setExecutor(this);
		}
		getServer().getPluginManager().registerEvents(new JoinListener(), this);
	}

	@Override
	public void onDisable() {
		kickAllFakes();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=!player,type=!villager]");
	}

	private void kickAllFakes() {
		fakePlayers.values().forEach(p -> p.kickPlayer(""));
		fakePlayers.clear();
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this.");
			return true;
		}

		switch(cmd.getName().toLowerCase()) {
			/*
			 * Setup
			 * - Clears all NPCs and spawns new ones
			 * - Teleports all NPCs to their initial locations
			 * - Does an initial mob spawning (to test without running the full TAS)
			 */
			case "setup" -> {
				spawnAllFakes(p.getWorld());
				Server.serverSetup(p.getWorld());
				p.sendMessage("Cleared all NPCs and spawned new ones.");
				return true;
			}
			/*
			 * TAS
			 * - Gives all NPCs the appropriate inventory
			 * - Re-teleports all NPCs to their initial locations
			 * - Re-spawns all mobs
			 * - Runs the TAS script
			 */
			case "tas" -> {
				String section = "all";
				if(args.length >= 1) {
					section = args[0].toLowerCase();
					if(!section.equals("all") && !section.equals("clear") && !section.equals("maxor") && !section.equals("storm") && !section.equals("goldor") && !section.equals("necron") && !section.equals("witherking")) {
						p.sendMessage(ChatColor.RED + "Invalid section specified.  Valid sections: clear maxor storm goldor necron witherking");
						return true;
					}
				}
				runTAS(p.getWorld(), section);
				return true;
			}
			case "spectate" -> {
				if(args.length < 1) {
					p.sendMessage("Please specify a class to spectate.");
					return true;
				}

				if(spectatorMap.containsValue(p)) {
					p.sendMessage("You are already spectating a class. Use /unspectate first.");
					return true;
				}
				String role = args[0].toLowerCase();
				Player fakePlayer = fakePlayers.get(role);
				if(fakePlayer == null) {
					p.sendMessage("Invalid class specified.");
					return true;
				}
				spectatorMap.put(p, fakePlayer);
				reverseSpectatorMap.computeIfAbsent(fakePlayer, k -> new ArrayList<>()).add(p);
				p.sendMessage("You are now spectating " + role + ".");
				return true;
			}
			case "unspectate" -> {
				if(spectatorMap.containsValue(p)) {
					Player fakePlayer = spectatorMap.remove(p);
					if (fakePlayer != null) {
						List<Player> spectators = reverseSpectatorMap.get(fakePlayer);
						if (spectators != null) {
							spectators.remove(p);
							if (spectators.isEmpty()) {
								reverseSpectatorMap.remove(fakePlayer);
							}
						}
					}
					p.sendMessage("You are no longer spectating a class.");
					return true;
				}
				p.sendMessage("You are not spectating a class.");
				return true;
			}
			case "reset" -> {
				Location hide = new Location(Bukkit.getWorld("world"), -120.5, 100, -220.5);
				fakePlayers.values().forEach(npc -> npc.teleport(hide, PlayerTeleportEvent.TeleportCause.PLUGIN));
				p.sendMessage("Reset all NPC locations.");
				return true;
			}
		}

		return false;
	}

	/**
	 * Builds a fake GameProfile with textures copied from another GameProfile's skin.
	 * This method is used to create an NPC's GameProfile that mimics the skin of another Minecraft user.
	 *
	 * @param fakeUuid      the UUID of the NPC's fake GameProfile
	 * @param fakeName      the name of the NPC's fake GameProfile
	 * @param skinOwnerUuid the UUID of the player whose skin will be used for copying textures
	 * @return a GameProfile with the specified UUID, name, and the copied skin texture property
	 */
	public static GameProfile buildFakeProfileWithSkin(UUID fakeUuid, String fakeName, UUID skinOwnerUuid) {
		// 1) Create a GameProfile only for fetching textures
		MinecraftServer nms = ((CraftServer) Bukkit.getServer()).getServer();
		ProfileResult result = nms.aq().fetchProfile(skinOwnerUuid, /* requireSecure= */ true);

		if(result == null) {
			throw new IllegalStateException("Failed to fetch profile for " + skinOwnerUuid);
		}

		GameProfile populated = result.profile();
		Property tex = populated.getProperties()
				.get("textures")
				.iterator()
				.next();

		// 3) Create your own NPC profile and copy in that Property:
		GameProfile npcProfile = new GameProfile(fakeUuid, fakeName);
		npcProfile.getProperties().put("textures", tex);
		return npcProfile;
	}

	/**
	 * Spawns a “real” EntityPlayer into the world (no Citizens, no persistence).
	 *
	 * @param world          the World
	 * @param fakePlayerName the fake player’s username
	 * @param skinOwner      the owner of the skin being used
	 */
	public Player spawnFakePlayer(World world, String fakePlayerName, UUID skinOwner) {
		// 1) NMS server & world handles
		MinecraftServer nmsServer = ((CraftServer) getServer()).getServer();
		WorldServer nmsWorld = ((CraftWorld) world).getHandle();

		// 2) Build a GameProfile with skin properties
		GameProfile profile = buildFakeProfileWithSkin(UUID.randomUUID(), fakePlayerName, skinOwner);

		// 3) Create EntityPlayer with a dummy InteractManager
		ClientInformation clientInfo = ClientInformation.a();
		EntityPlayer nmsPlayer = new EntityPlayer(nmsServer, nmsWorld, profile, clientInfo);

		// 4) Fake-network channel & connection
		NetworkManager nm = new NetworkManager(EnumProtocolDirection.b);
		CommonListenerCookie cookie = CommonListenerCookie.a(profile, false);
		// EntityPlayer = ServerPlayer
		// EntityPlayer.f -> EntityPlayer.PlayerConnection
		nmsPlayer.f = new PlayerConnection(nmsServer, nm, nmsPlayer, cookie);

		// 5) Position & add to world
		// Entity.a_(double, double, double) -> Entity.setPos(...)
		switch(fakePlayerName) {
			case "Archer" -> nmsPlayer.a_(-120.5, 69, -202.5);
			case "Berserk" -> nmsPlayer.a_(-21.5, 70, -197.5);
			case "Healer" -> nmsPlayer.a_(-28.5, 69, -44.5);
			case "Mage" -> nmsPlayer.a_(-132.5, 69, -76.5);
			case "Tank" -> nmsPlayer.a_(-196.5, 68, -222.5);
		}
		nmsWorld.addFreshEntity(nmsPlayer, CreatureSpawnEvent.SpawnReason.CUSTOM);
		nmsPlayer.f(false);

		// 6) Register with the server’s player list (so Bukkit sees it as a Player)
		// MinecraftServer.ag() -> MinecraftServer.getPlayerList()
		nmsServer.ag().a(nmsPlayer);

		// 7) Send packets to Players about the new Entity
		ChunkProviderServer provider = nmsWorld.m();
		provider.a.a(nmsPlayer);

		int id = nmsPlayer.ar(); // in your mappings this is nmsEntity.ar()
		PlayerChunkMap.EntityTracker wrapper = provider.a.K.get(id);

		EntityTrackerEntry entry = wrapper.b;

		EnumSet<a> addAction = EnumSet.of(a.a);
		ClientboundPlayerInfoUpdatePacket add = new ClientboundPlayerInfoUpdatePacket(addAction, List.of(nmsPlayer));
		PacketPlayOutSpawnEntity spawn = new PacketPlayOutSpawnEntity(nmsPlayer, entry);

		DataWatcher synchedEntityData = nmsPlayer.au();
		DataWatcherObject<Byte> accessor = new DataWatcherObject<>(17, DataWatcherRegistry.a);
		synchedEntityData.a(accessor, (byte) 127);

		PacketPlayOutEntityMetadata entityMetadataPacket = new PacketPlayOutEntityMetadata(nmsPlayer.ar(), synchedEntityData.c());

		Utils.broadcastPacket(add);
		Utils.broadcastPacket(spawn);
		Utils.broadcastPacket(entityMetadataPacket);

		// 8) Return the Bukkit wrapper
		return nmsPlayer.getBukkitEntity();
	}

	private void runTAS(World world, String section) {
		if(fakePlayers.isEmpty()) {
			Bukkit.broadcastMessage(ChatColor.RED + "Could not run TAS!  There are no actors.");
			return;
		}

		setInventories();
		Server.serverSetup(world);

		Archer.archerInstructions(fakePlayers.get("Archer"), section);
		Berserk.berserkInstructions(fakePlayers.get("Berserk"), section);
		Healer.healerInstructions(fakePlayers.get("Healer"), section);
		Mage.mageInstructions(fakePlayers.get("Mage"), section);
		Tank.tankInstructions(fakePlayers.get("Tank"), section);

		Server.serverInstructions(world, section);
	}

	public static Plugin getInstance() {
		return plugin;
	}

	private void startFakePlayerTicker() {
		if(fakeTickerStarted) {
			return;
		}
		fakeTickerStarted = true;

		new BukkitRunnable() {
			@Override
			public void run() {
				for(Player fake : fakePlayers.values()) {
					if(!(fake instanceof CraftPlayer)) continue;
					EntityPlayer npc = ((CraftPlayer) fake).getHandle();
					npc.f(false);
					npc.d_();
				}
			}
		}.runTaskTimer(this, 0, 1);
	}
}