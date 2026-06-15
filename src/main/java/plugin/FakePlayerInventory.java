package plugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R7.profile.CraftPlayerProfile;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class FakePlayerInventory {

	public static void setInventories() {
		Map<String, Player> fakePlayers = FakePlayerManager.getFakePlayers();
		for(String entry : fakePlayers.keySet()) {
			Player p = fakePlayers.get(entry);

			p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 255, true, false));
			p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, -1, 255, true, false));
			p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 3, true, false));
			p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 255, true, false));
			p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, -1, 2, true, false));
			Objects.requireNonNull(p.getAttribute(Attribute.ARMOR)).setBaseValue(20.0);
			Objects.requireNonNull(p.getAttribute(Attribute.ARMOR_TOUGHNESS)).setBaseValue(20.0);
			Objects.requireNonNull(p.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).setBaseValue(1);
			Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(40);
			p.setHealth(40);

			ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
			pearls.setAmount(16);
			ItemStack pickaxe = getSkyBlockItem(Material.DIAMOND_PICKAXE, ChatColor.RED + "Dungeonbreaker", "skyblock/combat/stonk");
			pickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, 255);
			ItemMeta meta = pickaxe.getItemMeta();
			meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(new NamespacedKey(M7tas.getInstance(), "stonk"), 1024, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
			pickaxe.setItemMeta(meta);
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
				case "Mage", "Mage1", "Mage2", "Mage3", "Mage4" -> {
					ItemStack chestplate = Utils.createLeatherArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(23, 147, 196), ChatColor.LIGHT_PURPLE + "Ancient Storm's Chestplate");
					ItemStack leggings = Utils.createLeatherArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(23, 168, 196), ChatColor.LIGHT_PURPLE + "Ancient Storm's Leggings");
					ItemStack boots = Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.fromRGB(28, 212, 228), ChatColor.LIGHT_PURPLE + "Ancient Storm's Boots");

					inventory.setHelmet(getStormHelmet());
					inventory.setChestplate(chestplate);
					inventory.setLeggings(leggings);
					inventory.setBoots(boots);
				}
			}

			inventory.setItem(0, getSkyBlockItem(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "Heroic Hyperion", "skyblock/combat/scylla"));
			inventory.setItem(1, getSkyBlockItem(Material.DIAMOND_SHOVEL, ChatColor.GOLD + "Warped Aspect of the Void", "skyblock/combat/aotv"));
			inventory.setItem(5, pickaxe);
			inventory.setItem(6, getSkyBlockItem(Material.BLAZE_ROD, ChatColor.GOLD + "Gyrokinetic Wand", "skyblock/combat/gyro"));
			inventory.setItem(7, pearls);
			inventory.setItem(8, getSkyBlockItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Menu (Click)", ""));
			inventory.setItem(9, getSkyBlockItem(Material.CHAINMAIL_BOOTS, ChatColor.LIGHT_PURPLE + "Renowned Spring Boots", "skyblock/combat/spring_boots"));
			inventory.setItem(10, getBonzoMask());
			inventory.setItem(11, getSpiritMask());
			inventory.setItem(12, getRacingHelmet());
			inventory.setItem(13, getCowHat());
			inventory.setItem(28, getSkyBlockItem(Material.BREEZE_ROD, ChatColor.DARK_PURPLE + "Bonzo Staff", "skyblock/combat/bonzo"));
			inventory.setItem(29, getSkyBlockItem(Material.BLAZE_ROD, ChatColor.DARK_PURPLE + "Tactical Insertion", "skyblock/combat/tac"));
			inventory.setItem(34, new ItemStack(Material.SOUL_SAND));

			switch(entry) {
				case "Archer" -> {
					//noinspection DuplicatedCode
					inventory.setItem(2, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", "skyblock/utility/infinileap"));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", "skyblock/combat/infinityboom"));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(18, getThermodynamicHelmet());
					inventory.setItem(19, Utils.createLeatherArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(255, 112, 10),  ChatColor.LIGHT_PURPLE + "Renowned Thermodynamic Chestplate"));
					inventory.setItem(20, Utils.createLeatherArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(255, 112, 10),  ChatColor.LIGHT_PURPLE + "Renowned Thermodynamic Leggings"));
					inventory.setItem(21, Utils.createLeatherArmor(Material.LEATHER_BOOTS, Color.fromRGB(255, 112, 10),  ChatColor.LIGHT_PURPLE + "Renowned Thermodynamic Boots"));
					inventory.setItem(30, getSkyBlockItem(Material.BONE, ChatColor.LIGHT_PURPLE + "Rapid Bonemerang", ""));
					inventory.setItem(32, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", "skyblock/combat/last_breath"));
					inventory.setItem(33, getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.DARK_PURPLE + "Withered Ragnarok Axe", "skyblock/combat/rag"));
					inventory.setItem(35, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Pitchin' Rod of the Sea", ""));
				}
				case "Berserk" -> {
					//noinspection DuplicatedCode
					inventory.setItem(2, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", "skyblock/utility/infinileap"));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", "skyblock/combat/infinityboom"));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(33, getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.DARK_PURPLE + "Withered Ragnarok Axe", "skyblock/combat/rag"));
					inventory.setItem(35, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Pitchin' Rod of the Sea", ""));
				}
				case "Healer" -> {
					inventory.setItem(2, getSkyBlockItem(Material.STICK, ChatColor.GOLD + "Heroic Ice Spray Wand", "skyblock/combat/ice_spray"));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", "skyblock/combat/infinityboom"));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(30, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", "skyblock/utility/infinileap"));
					inventory.setItem(32, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Withered Flaming Flay", "skyblock/combat/flaming_flay"));
					inventory.setItem(33, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", "skyblock/combat/last_breath"));
					inventory.setItem(35, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Pitchin' Rod of the Sea", ""));
				}
				case "Mage", "Mage1", "Mage2", "Mage3", "Mage4" -> {
					inventory.setItem(2, getSkyBlockItem(Material.STICK, ChatColor.GOLD + "Heroic Ice Spray Wand", "skyblock/combat/ice_spray"));
					inventory.setItem(3, getSkyBlockItem(Material.STONE_SWORD, ChatColor.LIGHT_PURPLE + "Withered Dark Claymore", "skyblock/combat/claymore"));
					inventory.setItem(4, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", "skyblock/utility/infinileap"));
					inventory.setItem(30, getSkyBlockItem(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "Withered Hyperion", "skyblock/combat/scylla"));
					inventory.setItem(31, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", "skyblock/combat/infinityboom"));
					inventory.setItem(32, getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.DARK_PURPLE + "Withered Ragnarok Axe", "skyblock/combat/rag"));
					inventory.setItem(33, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", "skyblock/combat/last_breath"));
					inventory.setItem(35, getSkyBlockItem(Material.BOW, ChatColor.GOLD + "Precise Explosive Bow", "skyblock/combat/explosive_bow"));
				}
				case "Tank" -> {
					inventory.setItem(2, getSkyBlockItem(Material.STICK, ChatColor.GOLD + "Heroic Ice Spray Wand", "skyblock/combat/ice_spray"));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", "skyblock/combat/infinityboom"));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(30, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", "skyblock/utility/infinileap"));
					inventory.setItem(31, getSkyBlockItem(Material.DIAMOND_AXE, ChatColor.LIGHT_PURPLE + "Withered Axe of the Shredded", "skyblock/combat/aots"));
					inventory.setItem(32, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Withered Flaming Flay", "skyblock/combat/flaming_flay"));
					inventory.setItem(33, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", "skyblock/combat/last_breath"));
				}
			}

			CraftPlayer player = (CraftPlayer) p;
			ServerPlayer nmsPlayer = player.getHandle();

			net.minecraft.world.item.ItemStack helmet = CraftItemStack.asNMSCopy(Objects.requireNonNull(inventory).getHelmet());
			net.minecraft.world.item.ItemStack chestplate = CraftItemStack.asNMSCopy(Objects.requireNonNull(inventory).getChestplate());
			net.minecraft.world.item.ItemStack leggings = CraftItemStack.asNMSCopy(Objects.requireNonNull(inventory).getLeggings());
			net.minecraft.world.item.ItemStack boots = CraftItemStack.asNMSCopy(Objects.requireNonNull(inventory).getBoots());
			net.minecraft.world.item.ItemStack hand = CraftItemStack.asNMSCopy(inventory.getItemInMainHand());

			nmsPlayer.setItemSlot(EquipmentSlot.HEAD, helmet, false);
			nmsPlayer.setItemSlot(EquipmentSlot.CHEST, chestplate, false);
			nmsPlayer.setItemSlot(EquipmentSlot.LEGS, leggings, false);
			nmsPlayer.setItemSlot(EquipmentSlot.FEET, boots, false);
			nmsPlayer.setItemSlot(EquipmentSlot.MAINHAND, hand, false);

			List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> gear = List.of(Pair.of(EquipmentSlot.HEAD, helmet), Pair.of(EquipmentSlot.CHEST, chestplate), Pair.of(EquipmentSlot.LEGS, leggings), Pair.of(EquipmentSlot.FEET, boots), Pair.of(EquipmentSlot.MAINHAND, hand));

			ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(nmsPlayer.getId(), gear);

			Utils.broadcastPacket(equipmentPacket);

			PlayerInventoryBackup.syncInventory(p);
			PlayerInventoryBackup.syncHand(p);
		}
	}

	// --- Item Creators ---

	private static ItemStack getCustomHead(String displayName, String identifier, String textureValue, String textureSignature) {
		Multimap<String, Property> props = HashMultimap.create();
		props.put("textures", new Property("textures", textureValue, textureSignature));
		PropertyMap propertyMap = new PropertyMap(props);
		GameProfile gp = new GameProfile(UUID.randomUUID(), identifier, propertyMap);

		CraftPlayerProfile profile = new CraftPlayerProfile(gp);

		ItemStack helmet = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) helmet.getItemMeta();
		assert meta != null;
		meta.setDisplayName(displayName);
		meta.setOwnerProfile(profile);
		helmet.setItemMeta(meta);
		helmet.addUnsafeEnchantment(Enchantment.PROTECTION, 5);
		return helmet;
	}

	public static ItemStack getDiamondHead() {
		return getCustomHead(ChatColor.RED + "Ancient Diamond Necron Head", "dmndNecronHead", "ewogICJ0aW1lc3RhbXAiIDogMTYxNzM5MDg5NDU4NiwKICAicHJvZmlsZUlkIiA6ICJjNWFhNTRhNmNmNTI0YmFmYmRiODUwNmUyMjRiNzViZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJOZWNyb25IYW5kbGVQTFMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGIxMTVjZGM0NWZkODRmMjFmYmE3YWMwZjJiYzc3YmMzYjYzMDJiZTY3MDg0MmY2ZTExZjY2ZWI1NTdmMTNlZSIKICAgIH0KICB9Cn0=", "hwaaE9h0QvFmtFvk2bNyAYlPo34NvaCjX3VWPv5zaRVM8KjfqQB5sB85vlFRVwCXK/HnQS2qMJ39ZuYplxT895lIShidIVdF5UP8T6cb1svhA9TmEVKFY4pKFyhUjIhD95HvO3OoNWPlCmb9Mho4XIo3K4AavnKPbuu3/I58gQmfKI71xDq7r+DRf9Dlxc8r3mcsUrdEwTfvC2/eFszHc/vqQXNm1smH2QJVfki+AgddNndFt7qumeicVFmsk2GmPNHxjlgH0xPL0hG8WGEmH5+Fnnj/eoYSutnpDRXVPY0H/KOMIa2Prga524stPC0gYmVU9y/wviXzDmKiiAa4uPVhwd/L/DgUSIGio6NlLMyA+Uvyy02HEr3TmzQ6bPqLphSttaDaVWW8Ltd1wvz/+Hhii5tYSSm3l5cAZQAO1O/JN/FKqA7tv0v0ZWp8AS1qw0QeLrRrLKlri2Zmzj5iYv7exfAVUiYB8f95ZZOWg1FLOufSJeFsQC5S7gsnsdJsWvnwvUNQI4RDfIc59a5Hvgzr90jgMTNoBGSXyrsXpeXJb+T9R8xfSEQY5V1XwFd+3lz8XRbBUQHubxN+b9AGj5FpQ2j5oaAz+BXY2+Iq20qVvkFMeJXdTRT0VIZM4r06ml0R3SZ1Jfui4xMH9OmhR+Hz3mmMvLN+BewhmtucN28=");
	}

	public static ItemStack getStormHelmet() {
		return getCustomHead(ChatColor.LIGHT_PURPLE + "Ancient Storm's Helmet", "stormHelmet", "ewogICJ0aW1lc3RhbXAiIDogMTc0NjgwNzcwMTU2MSwKICAicHJvZmlsZUlkIiA6ICJjNDIzYjQwMWZiOGU0ODc3YjMzMmVmMjhiZDdlZGZmZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFjdGlvbkJyaW5lWVQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTliYWVjNWZiNGNkOWRjNTk2ZDYxMGI2YzZiZDM4YWI5OTAxYjY4Yzk1OTQ5ZTJkNzFiOTI1MzE3MjcwZDAxMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "s8X+QmhjqwppG9pqW9SYQloIzPVTw3PBpMprwnx9pl9j2uNdBgJbpwhahgo3WjpXOV9aiewogO7HDqZ71fns/rkPLVBANO6mlnYS8J+J8rLkQFiinQERx4ucYtHM9atzZnG7dDv6QTK6Bvur8SwVhZIOYSj7YWN1ecrbm9RskNhiRSXVwFH/TcWdSv4z/c0zG2b+OXaD68NAwxTd8lszNl+JSWFU6dP/l8GP1EWDNz8WagfwzeOTaHU2rDztRCUXlNGeF16QdZBXgFUva3Kel6D0QSE492Q1vTt5f55xwk38Yjbw6wkv2se+arcd9sbInuxlJamev6J4FX0r1QhGpgxHDvu30O/htK7ni8Og4AWgESQg/ONo/R7GUYsysao3lV46cHGK9JBEQEG0Zlq+gQ9ajzLojLchfSMM03/V8FpyLKBsplMJuG3NNz4QLXlflWU3UpuXD7SDGgIcn4UVRlANhC/Nj2qO4DUVkMA6V3OSGFWdLe9ICMZfLPXQiGFkZd4SmJLp6dy/Z2C7DGZci7qSkTXBW8j1Zmz52dSvaNqQvb10nSS+EVG8yggniRMheW8s8d6fs4fwrXfj+so2ayTjtImr8eafK1CpIARWCDEXZhQEs/rFv4dpuRaziJw69eVpem0ZwMRe7V4bf98SA5+yxgdYMtxoWUi+uMvKC8U=");
	}

	public static final String RACING_HELMET_NAME = ChatColor.LIGHT_PURPLE + "Renowned Racing Helmet";

	/** True if the given item is the Renowned Racing Helmet (identified by display name — it carries no lore ID). */
	public static boolean isRacingHelmet(ItemStack item) {
		if(item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		return meta.hasDisplayName() && RACING_HELMET_NAME.equals(meta.getDisplayName());
	}

	public static final String COW_HAT_NAME = ChatColor.GREEN + "Renowned Cow Hat";

	/** True if the given item is the Renowned Cow Hat (identified by display name — it carries no lore ID). */
	public static boolean isCowHat(ItemStack item) {
		if(item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		return meta.hasDisplayName() && COW_HAT_NAME.equals(meta.getDisplayName());
	}

	public static ItemStack getRacingHelmet() {
		return getCustomHead(RACING_HELMET_NAME, "racingHelmet", "ewogICJ0aW1lc3RhbXAiIDogMTY1NTg2ODcxMjQwMCwKICAicHJvZmlsZUlkIiA6ICJmZTYxY2RiMjUyMTA0ODYzYTljY2E2ODAwZDRiMzgzZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNeVNoYWRvd3MiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmNlMDc0NmIxMmVlNDA1Mzk1OGUxNDBiYTI5NTkzMjcyYmQ4NGNhMzRiYWY1MGQwZDgwYjViYzNjNjE1ZTljNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "nXxrmNM9KqMjDMg9Lw6gZRooMCm/GnNIn0beDi/2SCyg6aeTCpKe4//cgxWMcg83qEdunAu7z0YICeL/z+K9ynAuviN0AnbwFdwfMf9/Wucs2KXfV9OPPqLnGc8qkht+qbm6d97QvSVh/Ldq4gh19beWPkEY+TP9bBdVGIn2KMY/VukvKGr9PU4aYT2R/5ntQT+iYMPUryQg48+a6EthLcBPhvy7htew2QtebXUKtEfxbnQ7jbj7WLKRoOTlpBCaT9F/TE4Zc2eJJiNwLPh0Cojce2zBCbmDt69wFKlzwJjjcpogaUNqNuVJLRUuAA/r/N82Skc+uoDHaYg0P9vHRhqmSRQNQ85lLcWBu6wmpl5Y+p/NTrqDglQFp919Bk7gsPu2S3gu7+Mx1O0SkSiw13mui67Y31M+9QYXD5yBn5HBt0aao48rOVkL8eM+DHv9cCrefWWIdFXNUpRKqvYsd/2wYgWqjHkE6CvHq0PHMR6IYC7o8zc99A25Ps3wAJwTM3SUyraiEuHxDcJdZLVS9pS43TXaWV8AnEUbBvhdnG1rLDlIi+O5aGIDuKLyQuJo/iaj2mHgHeIg1wbBSQxdIfPTCncPCAITVHouXSds46vnoMNsDmVRW1cpf7T1xwfsEwlWv+hornChME+KxAzQZOY1OqduagUvruLZi/0UPxo=");
	}

	public static ItemStack getBonzoMask() {
		return getCustomHead(ChatColor.DARK_PURPLE + "Ancient Bonzo's Mask", "bonzoMask", "ewogICJ0aW1lc3RhbXAiIDogMTc0NjgxMDU3MDM4NCwKICAicHJvZmlsZUlkIiA6ICJhZTg3MzEyNjBmMzY0ZWE2YjU3YTRkYjI5Mjk1YTA1OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGdW50aW1lX0ZveHlfMTkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQ2MDFmNzM2YmQ0MmE3Zjg0YzU4ZGUzY2YxMjBhZTRhZTYwZmViODJiMDM3ZThjYjBkMjhhMWUyMTYxODc5ZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "MBSY1gYYDruisd3+61+sS9xWwBABXOkLgcNwhZOhSNMyAE4yyhEGomaMT5hNckff6KyKBerAMJkWBK8i6kmmEyYKcQfb2jVSFWzQOCZmreGr7n/PEs4hXsGTGXLI1NCEqRpyv2kxUjsnjpDsmJQicSXd2Q/z5NpuC9VwG1mnz7+nzzJxxIx5QtzoLDKrXjpfJtNwGgq+0k0m7lJYIeyjXOCvCgnZO1VyAvmYLIo1DD/4IXCVqErAlRouLhzjJrBNSz95rMr/sQ0T5qFsclzcMTydeti9Pb5j+OhXDavkGFkrsfEpiXzQnW82ZqQ+2ZL8FYVyIEV+0z4kbrbXf4bcLtxQZskKNe/8xN5+UE9KdBcFQ0nF1EEM8Ia+9ChpcGggMqJAq/Zs3Vd1L37/JA5ahZtZqyXS3azKw6Lfh0UWkh+c64svuJI0XJVNNG1cTdGg6CVV37D2UkfHk6dAIlP/7XybHj0ZB3Ew8hThCi48EK0RH37fQvbbujRBjuxFGvU8l5ON4iZRkV+7qyCgLmnhYXGDMsEoGcfAT3m0m2i0+CVH6jitRz6PlbWKhdhT3TW5lBF82TO2QF+muzDD9yTpT7v6YUTQOmISHm/svXnbCp2+du79iijYbW2iQaM8r8ahkC83Owbuhhkgd+SgbTY7JwyBlR7U9j4TBzL+h+Advqw=");
	}

	public static ItemStack getSpiritMask() {
		return getCustomHead(ChatColor.LIGHT_PURPLE + "Ancient Spirit Mask", "spiritMask", "ewogICJ0aW1lc3RhbXAiIDogMTYxNDYyNDQzODQxMywKICAicHJvZmlsZUlkIiA6ICIzMmQ0YzJmN2NlODg0OTAxOGIyZjA3OWM5ZmFiODQxYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJQbHV0byIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83Y2JhOTAyYzhiZGE0NTA2MDlmZGU0OTE4ODgzNDc2MmE3ODA5ZjY1ZjlkZjI2ODQ1ZTM1MWU4MGUxMmJlODMxIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "v3AvlOspp/7rzGxKdoNj56ZldCLMP0ntaKeHNwjAZA1Ai0WtSsM0j8PIf3YgYIGDs+To5WetlTtgxCp2Mj6fSY6OWeGW2n3Jz+08FkXFUterwfAuqguF90ktROcMG4sEZTOESItehng/LSVShHSwtg5IekQgi8mr07mntH6CGy/xhVu1SMuTIthLBBUbOI6NwfsqZe60BvBPv899C8k5zGzRAVYsDD4cdjXC6ALbMVfIIspyHM6vUuqo7MlNmOJxrr1HfhNXqlFbwDdP9CaL3DmGbHBfTLk8dhLxE89+SNI9HGbvn3YUP5M4f5K4mnire6kIwgXzVNrjXwNR0wkD86dC5ridVRFP6f5VUKLoNSbdQqnAhWhpS2PyV653609dCWR7ES5T2GNxqGv+XufDBqSNSDQc0w/Bhavd0SA3evDX4tc33t3ho6z/XUUYYP5lF8lXtAZmq64MvJB3NOj9XQMBr46b0Zjf986nZKpfmi5hhL/ddXsigl5HszR3EvAQj0M/OHZ0IAu3LRWm9bqwNdPS+soF5n+hOuDAWv0/fh5Dzy8O8PqCUWE0sOkwzB3on3ih7qt8nduCS0kkkCZRvd1eE0Xh1KxnHlKR0QdIIYkM2R2Y3RNDM1mI45NcCTxoIg1I6F/FmXI+aKWDibTHNCK6ix9Nc5We1z6S6vccRHw=");
	}

	public static ItemStack getCowHat() {
		return getCustomHead(COW_HAT_NAME, "cowHat", "eyJ0aW1lc3RhbXAiOjE0OTg2ODk0MDczOTQsInByb2ZpbGVJZCI6ImYxNTliMjc0YzIyZTQzNDBiN2MxNTJhYmRlMTQ3NzEzIiwicHJvZmlsZU5hbWUiOiJNSEZfQ293Iiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81ZDZjNmVkYTk0MmY3ZjVmNzFjMzE2MWM3MzA2ZjRhZWQzMDdkODI4OTVmOWQyYjA3YWI0NTI1NzE4ZWRjNSJ9fX0=", "OKbGpPO8wQDLCu/zeOrLBt9m6V29AZMNYR4gfumog1q5UvI/3if2IKdKi2X0UcJWyvgMlHW0y1OAGFsY5EZyL4Z+/+Ed2G5IFiMAQpq+sjqyiPgwIUixNLYdu7sMjxsuoRlzMe9y73tW2+Waxk4aWYbMCxGGQmgHv++L1gw+qiAzzFDs09l0GexVnLW8XO/baLAWD6DG7dpOieFVCAaap8cvw4ZVTF7jNBfr3dd3BkhkMbGrmS39WV5s1XsxkbomFRdHPCIh8ba/DEe5qYGhLNQe0mSHZS2k9Y15kMWiLEU+tTDa7UpD8ZYszvPbgbIEbVeKOIF0lGm7m/RIHHSTjSVzemTrYbXTkf54dcAdwsutd8mYm3yZDj8Iw/5y2X2qOalGVyDupFJcPJHabvct9uup7GaLzMBs1o3p4RjomtAdjQLlEX8ktY0YLExyKAhx8bfaI+67i1a7r2eh8eAMMY0A7WBnEJv9iO4vUtsOB0fKIiqbOIXGkUbaJk6Poj37CbNYQC83bSIzEcfXm4inytLwrTg3B6k1zrIL45SQ4XovItWfcWD+g1WzIEz7+LXBLaB8SRZT9EU7jA18GpazvhJBNlP4srIuE9AuVrNSNHg/LtA4FqFX06YhEH1/oxWOOO5uqS6ggUb0xiO6u6BbTLU/FQPh3GyytS76d2Dg0rk=");
	}

	public static ItemStack getThermodynamicHelmet() {
		return getCustomHead(ChatColor.LIGHT_PURPLE + "Renowned Thermodynamic Helmet", "thermoHelmet", "ewogICJ0aW1lc3RhbXAiIDogMTc3MjE1OTIwMzEzMywKICAicHJvZmlsZUlkIiA6ICJkZGRhNjc4ZmYyN2M0NjFhOWUyMjRiMTU1NjI4NDZmYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJSeWxlZTc1NDMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2FkNjdjZjMzMTg4YTU0YjZmNTVkMmVmNTI0OGNkM2I3MjE3Njk5NGUxZDIwMzczMWU4NmIzNjVhODU4MTcyNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "erdSAba2Gk9jBaa6fd87ZzBdUbuKFtDuo4m2+H9s3WISKq+i+VMi/IUa5WvN2dy7i2sFXA8jqtXh5LG87+I3bTSZKfvxlEgQfwBg0aG7HEsqn1OAAsT/4ZFWE7Flt79D22G8kZZe5IGkFj5T8pZpEC6NBKWX+k666Bd+G68TmGFcaSzKgKs/AGRtR6iwJKMp1U9CD9+jr7WAC5j/jDtwHLzYTv+zxEt8ufNv5ewSrGtnXelBkAQmAo8dhvrfik0G/rpB5RhM8FZOfhV+fqjLvtcBd0vOVrDMLHaZJ+2TpjDnjScA+GIS30EEwnx8TpsoJM5PLJUL7b9xNZEECQAklnZt59gSg2e0PF6rI2Q2Fb9HIVDETPKoNK/9X7MfWC9uWr/GEA4G61tLYq5NnMEAqj3+AT6YTWb3Vy5xDauTHftbAogQEWa/2S5GyHUVQ0zh850aOy9AQMCbUaN5hTk/x6AFJIY9bvFEGVJ3Wr8HZPIh/WAqgRqgnLRx6RxSqymBsh/I2SYLaWtM027hkUlcAfj3HbYMkBdD8UXx3AzPvXAwhDojUPMWwK74La9MwiDRDO+fxFdhOIhrbh4ib1MaH7dqrcnDX6OjdQfZ0QR3lNRYet8wKlxVL3xy3ppRfLys8Wrvhdi3kBplKJxW5CyoI4fEJLtyvC2NM3olt9ZvVbM=");
	}

	public static ItemStack getSkyBlockItem(Material material, String name, String id) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		meta.setItemName(name);
		meta.setUnbreakable(true);
		meta.setDisplayName(name);
		List<String> lore = new ArrayList<>();
		lore.add(id);
		meta.setLore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
		return item;
	}
}
