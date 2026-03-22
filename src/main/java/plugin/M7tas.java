/*
 * MIT License
 *
 * Copyright ©2025 Stradivarius Violin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package plugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.datafixers.util.Pair;
import commands.*;
import instructions.Server;
import instructions.players.*;
import io.netty.channel.embedded.EmbeddedChannel;
import listeners.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import nms.TASGamePacketListenerImpl;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.PluginCommand;
import org.bukkit.craftbukkit.v1_21_R7.CraftServer;
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R7.profile.CraftPlayerProfile;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;

public final class M7tas extends JavaPlugin {
	private static final Map<String, Player> fakePlayers = new HashMap<>();
	private static Plugin plugin;
	private static boolean fakeTickerStarted = false;
	private static final Map<Player, PlayerInventoryBackup> originalInventories = new HashMap<>();
	private static Team noCollisionTeam;

	public static void addPlayerInventoryBackup(Player player) {
		originalInventories.put(player, new PlayerInventoryBackup(player));
	}

	public static PlayerInventoryBackup removePlayerInventoryBackup(Player player) {
		return originalInventories.remove(player);
	}

	public static class PlayerInventoryBackup {
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
	}

	private static final Map<String, String> SKIN_DATA = Map.of("Archer", "0b0fa6bc-69ee-4f6c-a4f8-7cac79f1871a", "Mage3", "dff79c40-6aeb-458a-86cf-6789e1831317", "Healer", "f0b1d2e5-dfd2-4d33-9768-4d2b259a4743", "Mage1", "cdb9e9c6-c096-4f58-9c49-35395d7b897c", "Mage2", "5d142c3a-bdf1-418b-b907-797bbaaed188");



	public static Map<String, Player> getFakePlayers() {
		return fakePlayers;
	}

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
		return getCustomHead("§6Ancient Storm's Helmet", "stormHelmet", "ewogICJ0aW1lc3RhbXAiIDogMTc0NjgwNzcwMTU2MSwKICAicHJvZmlsZUlkIiA6ICJjNDIzYjQwMWZiOGU0ODc3YjMzMmVmMjhiZDdlZGZmZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFjdGlvbkJyaW5lWVQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTliYWVjNWZiNGNkOWRjNTk2ZDYxMGI2YzZiZDM4YWI5OTAxYjY4Yzk1OTQ5ZTJkNzFiOTI1MzE3MjcwZDAxMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "s8X+QmhjqwppG9pqW9SYQloIzPVTw3PBpMprwnx9pl9j2uNdBgJbpwhahgo3WjpXOV9aiewogO7HDqZ71fns/rkPLVBANO6mlnYS8J+J8rLkQFiinQERx4ucYtHM9atzZnG7dDv6QTK6Bvur8SwVhZIOYSj7YWN1ecrbm9RskNhiRSXVwFH/TcWdSv4z/c0zG2b+OXaD68NAwxTd8lszNl+JSWFU6dP/l8GP1EWDNz8WagfwzeOTaHU2rDztRCUXlNGeF16QdZBXgFUva3Kel6D0QSE492Q1vTt5f55xwk38Yjbw6wkv2se+arcd9sbInuxlJamev6J4FX0r1QhGpgxHDvu30O/htK7ni8Og4AWgESQg/ONo/R7GUYsysao3lV46cHGK9JBEQEG0Zlq+gQ9ajzLojLchfSMM03/V8FpyLKBsplMJuG3NNz4QLXlflWU3UpuXD7SDGgIcn4UVRlANhC/Nj2qO4DUVkMA6V3OSGFWdLe9ICMZfLPXQiGFkZd4SmJLp6dy/Z2C7DGZci7qSkTXBW8j1Zmz52dSvaNqQvb10nSS+EVG8yggniRMheW8s8d6fs4fwrXfj+so2ayTjtImr8eafK1CpIARWCDEXZhQEs/rFv4dpuRaziJw69eVpem0ZwMRe7V4bf98SA5+yxgdYMtxoWUi+uMvKC8U=");
	}

	public static ItemStack getBonzoMask() {
		return getCustomHead(ChatColor.DARK_PURPLE + "Ancient Bonzo's Mask", "bonzoMask", "ewogICJ0aW1lc3RhbXAiIDogMTc0NjgxMDU3MDM4NCwKICAicHJvZmlsZUlkIiA6ICJhZTg3MzEyNjBmMzY0ZWE2YjU3YTRkYjI5Mjk1YTA1OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGdW50aW1lX0ZveHlfMTkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQ2MDFmNzM2YmQ0MmE3Zjg0YzU4ZGUzY2YxMjBhZTRhZTYwZmViODJiMDM3ZThjYjBkMjhhMWUyMTYxODc5ZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "MBSY1gYYDruisd3+61+sS9xWwBABXOkLgcNwhZOhSNMyAE4yyhEGomaMT5hNckff6KyKBerAMJkWBK8i6kmmEyYKcQfb2jVSFWzQOCZmreGr7n/PEs4hXsGTGXLI1NCEqRpyv2kxUjsnjpDsmJQicSXd2Q/z5NpuC9VwG1mnz7+nzzJxxIx5QtzoLDKrXjpfJtNwGgq+0k0m7lJYIeyjXOCvCgnZO1VyAvmYLIo1DD/4IXCVqErAlRouLhzjJrBNSz95rMr/sQ0T5qFsclzcMTydeti9Pb5j+OhXDavkGFkrsfEpiXzQnW82ZqQ+2ZL8FYVyIEV+0z4kbrbXf4bcLtxQZskKNe/8xN5+UE9KdBcFQ0nF1EEM8Ia+9ChpcGggMqJAq/Zs3Vd1L37/JA5ahZtZqyXS3azKw6Lfh0UWkh+c64svuJI0XJVNNG1cTdGg6CVV37D2UkfHk6dAIlP/7XybHj0ZB3Ew8hThCi48EK0RH37fQvbbujRBjuxFGvU8l5ON4iZRkV+7qyCgLmnhYXGDMsEoGcfAT3m0m2i0+CVH6jitRz6PlbWKhdhT3TW5lBF82TO2QF+muzDD9yTpT7v6YUTQOmISHm/svXnbCp2+du79iijYbW2iQaM8r8ahkC83Owbuhhkgd+SgbTY7JwyBlR7U9j4TBzL+h+Advqw=");
	}

	public static ItemStack getSpiritMask() {
		return getCustomHead(ChatColor.LIGHT_PURPLE + "Ancient Spirit Mask", "spiritMask", "eyJ0aW1lc3RhbXAiOjE1MDUyMjI5OTg3MzQsInByb2ZpbGVJZCI6IjBiZTU2MmUxNzIyODQ3YmQ5MDY3MWYxNzNjNjA5NmNhIiwicHJvZmlsZU5hbWUiOiJ4Y29vbHgzIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsibWV0YWRhdGEiOnsibW9kZWwiOiJzbGltIn0sInVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWJiZTcyMWQ3YWQ4YWI5NjVmMDhjYmVjMGI4MzRmNzc5YjUxOTdmNzlkYTRhZWEzZDEzZDI1M2VjZTlkZWMyIn19fQ==", "BvTwl3pF3ab7hGRXD46RBSPXS0ue/I4F4mWOq40BUVZJGAplUlZWnXSm9WZhEiTlxSbza8xrwAawofgoCdRVEzLsNYdejYztdktGgqjOusAexXR2V85g4PXIPeoTtqvDlG97EVL0IJcRQsrlJ8WipdbUfeWHSzqy9Ibo689CblgGEgjQ8rNynwC+ncsRzQZ7t3uNviBpGNq1YllVLROt79uTmeI8EkwmkYOawXswIdFfMr5FXLZhWAaQ1c6zxPxTvPhkY9S5VxnSA0tFfyoqV6MD2SHkWvdQLT1VB3KCtsJi3hHXzgTH/24BrlkWUygmaG+Q89l7aG1v4bYoJMF9uUQe4e47JZKeWkLVSoadf+OUQv7K2h6aNJKYRmrdWd+Z9h3gfKFTTiIKF4VQ4kF5mDnlH8+FICRK6KcatQeEoC89WWHUcaHPmhQoih2X01mGUh+lusPc/l/v+ZjNKUuK+WDAu6HYZA3MGh60duLNrAHnoDcLaWbVJ8a4cyXQHVqHM43KJV8ljm9Zq1yElbAdExKW7F9WZ1jlPae6JJj9qJlFH+PZnMhlU9mxPMHttFz9jRmd5Bg//3g5jPb5PMZ/Y3mQ/YURJ2j5bYAO1+F4rYa6FE33UKLWbIZ8wY10HiK7d0hfr7DTy1NscQOOwQFeyxj4wfewMRoSmLa8uu9T7pc=");
	}

	public static ItemStack getCowHat() {
		return getCustomHead(ChatColor.GREEN + "Renowned Cow Hat", "cowHat", "eyJ0aW1lc3RhbXAiOjE0OTg2ODk0MDczOTQsInByb2ZpbGVJZCI6ImYxNTliMjc0YzIyZTQzNDBiN2MxNTJhYmRlMTQ3NzEzIiwicHJvZmlsZU5hbWUiOiJNSEZfQ293Iiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81ZDZjNmVkYTk0MmY3ZjVmNzFjMzE2MWM3MzA2ZjRhZWQzMDdkODI4OTVmOWQyYjA3YWI0NTI1NzE4ZWRjNSJ9fX0=", "OKbGpPO8wQDLCu/zeOrLBt9m6V29AZMNYR4gfumog1q5UvI/3if2IKdKi2X0UcJWyvgMlHW0y1OAGFsY5EZyL4Z+/+Ed2G5IFiMAQpq+sjqyiPgwIUixNLYdu7sMjxsuoRlzMe9y73tW2+Waxk4aWYbMCxGGQmgHv++L1gw+qiAzzFDs09l0GexVnLW8XO/baLAWD6DG7dpOieFVCAaap8cvw4ZVTF7jNBfr3dd3BkhkMbGrmS39WV5s1XsxkbomFRdHPCIh8ba/DEe5qYGhLNQe0mSHZS2k9Y15kMWiLEU+tTDa7UpD8ZYszvPbgbIEbVeKOIF0lGm7m/RIHHSTjSVzemTrYbXTkf54dcAdwsutd8mYm3yZDj8Iw/5y2X2qOalGVyDupFJcPJHabvct9uup7GaLzMBs1o3p4RjomtAdjQLlEX8ktY0YLExyKAhx8bfaI+67i1a7r2eh8eAMMY0A7WBnEJv9iO4vUtsOB0fKIiqbOIXGkUbaJk6Poj37CbNYQC83bSIzEcfXm4inytLwrTg3B6k1zrIL45SQ4XovItWfcWD+g1WzIEz7+LXBLaB8SRZT9EU7jA18GpazvhJBNlP4srIuE9AuVrNSNHg/LtA4FqFX06YhEH1/oxWOOO5uqS6ggUb0xiO6u6BbTLU/FQPh3GyytS76d2Dg0rk=");
	}

	public static ItemStack getSkyBlockItem(Material material, String name, String id) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		meta.setItemName(null);
		meta.setUnbreakable(true);
		meta.setDisplayName(name);
		List<String> lore = new ArrayList<>();
		lore.add(id);
		meta.setLore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
		return item;
	}

	public static void spawnAllFakes(World world) {
		// clear any old ones
		stopCustomConnection();
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

	private static void setInventories() {
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

			// Set Inventory
			ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
			pearls.setAmount(16);
			ItemStack pickaxe = getSkyBlockItem(Material.DIAMOND_PICKAXE, ChatColor.RED + "Dungeonbreaker", "skyblock/combat/stonk");
			pickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, 10);
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
				case "Mage", "Mage1", "Mage2", "Mage3" -> {
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
			inventory.setItem(1, getSkyBlockItem(Material.DIAMOND_SHOVEL, ChatColor.GOLD + "Warped Aspect of the Void", "skyblock/combat/aotv"));
			inventory.setItem(5, pickaxe);
			inventory.setItem(6, getSkyBlockItem(Material.BLAZE_ROD, ChatColor.GOLD + "Gyrokinetic Wand", "skyblock/combat/gyro"));
			inventory.setItem(7, pearls);
			inventory.setItem(8, getSkyBlockItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Menu (Click)", ""));
			inventory.setItem(28, getSkyBlockItem(Material.BREEZE_ROD, ChatColor.DARK_PURPLE + "Bonzo Staff", "skyblock/combat/bonzo"));
			inventory.setItem(29, getSkyBlockItem(Material.BLAZE_ROD, ChatColor.DARK_PURPLE + "Tactical Insertion", "skyblock/combat/tac"));

			switch(entry) {
				case "Archer" -> {
					//noinspection DuplicatedCode
					inventory.setItem(2, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", "skyblock/utility/infinileap"));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", "skyblock/combat/infinityboom"));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(9, getBonzoMask());
					inventory.setItem(10, getSpiritMask());
					inventory.setItem(11, getCowHat());
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
					inventory.setItem(9, getBonzoMask());
					inventory.setItem(10, getSpiritMask());
					inventory.setItem(11, getSkyBlockItem(Material.CHAINMAIL_BOOTS, ChatColor.LIGHT_PURPLE + "Renowned Spring Boots", ""));
					inventory.setItem(12, getCowHat());
					inventory.setItem(33, getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.DARK_PURPLE + "Withered Ragnarok Axe", "skyblock/combat/rag"));
					inventory.setItem(35, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Pitchin' Rod of the Sea", ""));
				}
				case "Healer" -> {
					inventory.setItem(2, getSkyBlockItem(Material.STICK, ChatColor.GOLD + "Heroic Ice Spray Wand", "skyblock/combat/ice_spray"));
					inventory.setItem(3, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", "skyblock/combat/infinityboom"));
					inventory.setItem(4, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Terminator", "skyblock/combat/terminator"));
					inventory.setItem(9, getBonzoMask());
					inventory.setItem(10, getSpiritMask());
					inventory.setItem(30, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", "skyblock/utility/infinileap"));
					inventory.setItem(32, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Withered Flaming Flay", "skyblock/combat/flaming_flay"));
					inventory.setItem(33, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", "skyblock/combat/last_breath"));
					inventory.setItem(35, getSkyBlockItem(Material.FISHING_ROD, ChatColor.LIGHT_PURPLE + "Pitchin' Rod of the Sea", ""));
				}
				case "Mage", "Mage1", "Mage2", "Mage3" -> {
					inventory.setItem(2, getSkyBlockItem(Material.STICK, ChatColor.GOLD + "Heroic Ice Spray Wand", "skyblock/combat/ice_spray"));
					inventory.setItem(3, getSkyBlockItem(Material.STONE_SWORD, ChatColor.LIGHT_PURPLE + "Withered Dark Claymore", "skyblock/combat/claymore"));
					inventory.setItem(4, getSkyBlockItem(Material.ENDER_PEARL, ChatColor.GOLD + "Infinileap", "skyblock/utility/infinileap"));
					inventory.setItem(9, getSkyBlockItem(Material.CHAINMAIL_BOOTS, ChatColor.LIGHT_PURPLE + "Renowned Spring Boots", ""));
					inventory.setItem(30, getSkyBlockItem(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "Withered Hyperion", "skyblock/combat/scylla"));
					inventory.setItem(31, getSkyBlockItem(Material.TNT, ChatColor.GOLD + "Infinityboom TNT", "skyblock/combat/infinityboom"));
					inventory.setItem(32, getSkyBlockItem(Material.GOLDEN_AXE, ChatColor.DARK_PURPLE + "Withered Ragnarok Axe", "skyblock/combat/rag"));
					inventory.setItem(33, getSkyBlockItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Precise Last Breath", "skyblock/combat/last_breath"));
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

			Utils.syncInventory(p);
			Utils.syncHand(p);
		}
	}

	@Override
	public void onEnable() {
		plugin = this;

		setupNoCollisionTeam();

		for(String cmd : List.of("setup", "spectate", "unspectate", "tas", "simulate", "reset", "getcustomitems", "verbose")) {
			PluginCommand command = getCommand(cmd);
			switch(cmd) {
				case "setup" -> command.setExecutor(new Setup());
				case "spectate", "unspectate" -> command.setExecutor(new Spectate());
				case "tas" -> command.setExecutor(new TAS());
				case "simulate" -> command.setExecutor(new Simulate());
				case "reset" -> command.setExecutor(new Reset());
				case "getcustomitems" -> command.setExecutor(new GetCustomItems());
				case "verbose" -> command.setExecutor(new Verbose());
			}
			command.setTabCompleter(new TabCompletor());
		}
		getServer().getPluginManager().registerEvents(new JoinListener(), this);
		getServer().getPluginManager().registerEvents(new SpectatorListener(), this);
		getServer().getPluginManager().registerEvents(new WithersNotImmuneToArrows(), this);
		getServer().getPluginManager().registerEvents(new PearlHelper(), this);
		getServer().getPluginManager().registerEvents(new MiscListener(), this);
		getServer().getPluginManager().registerEvents(new CustomItems(), this);
		getServer().getPluginManager().registerEvents(new AllMobsHaveNames(), this);

		Utils.startInventorySync();
		Spectate.startSpectatorSync();
	}

	@Override
	public void onDisable() {
		Utils.stopInventorySync();
		stopCustomConnection();
		Spectate.stopSpectatorSync();

		if(noCollisionTeam != null) {
			for(String entry : new HashSet<>(noCollisionTeam.getEntries())) {
				noCollisionTeam.removeEntry(entry);
			}
			noCollisionTeam.unregister();
		}

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tag @e[type=wither] remove TASWither");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=!item_frame,type=!player,type=!villager]");

		originalInventories.clear();
	}

	private static void kickAllFakes() {
		fakePlayers.values().forEach(p -> {
			if(p.isOnline()) {
				p.kickPlayer("");
			}
		});
		fakePlayers.clear();
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
		ProfileResult result = nms.services().sessionService().fetchProfile(skinOwnerUuid, /* requireSecure= */ true);

		if(result == null) {
			throw new IllegalStateException("Failed to fetch profile for " + skinOwnerUuid);
		}

		GameProfile populated = result.profile();
		Multimap<String, Property> properties = HashMultimap.create();
		for(Property tex : populated.properties().get("textures")) {
			properties.put("textures", tex);
		}

		PropertyMap propertyMap = new PropertyMap(properties);
		return new GameProfile(fakeUuid, fakeName, propertyMap);
	}

	/**
	 * Spawns a “real” ServerPlayer into the world (no Citizens, no persistence).
	 *
	 * @param world          the World
	 * @param fakePlayerName the fake player’s username
	 * @param skinOwner      the owner of the skin being used
	 */
	public static Player spawnFakePlayer(World world, String fakePlayerName, UUID skinOwner) {
		// 1) NMS server & world handles
		MinecraftServer nmsServer = ((CraftServer) getInstance().getServer()).getServer();
		ServerLevel nmsWorld = ((CraftWorld) world).getHandle();

		// 2) Build a GameProfile with skin properties
		GameProfile profile = buildFakeProfileWithSkin(UUID.randomUUID(), fakePlayerName, skinOwner);

		// 3) Create ServerPlayer with a dummy InteractManager
		ClientInformation clientInfo = ClientInformation.createDefault();
		ServerPlayer nmsPlayer = new ServerPlayer(nmsServer, nmsWorld, profile, clientInfo);

		// 4) Fake-network channel & connection
		Connection nm = new Connection(PacketFlow.SERVERBOUND) {
			{
				this.channel = new EmbeddedChannel();
				this.address = new InetSocketAddress("127.0.0.1", 0);
			}

			@Override
			public void send(Packet<?> packet) {

			}

			@Override
			public boolean isConnected() {
				return true;
			}
		};
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		// ServerPlayer = ServerPlayer
		// ServerPlayer.f -> ServerPlayer.PlayerConnection
		TASGamePacketListenerImpl connection = new TASGamePacketListenerImpl(nmsServer, nm, nmsPlayer, cookie);
		nmsPlayer.connection = connection;
		forceCustomConnection(nmsPlayer, connection);

		// 5) Position & add to world
		// Entity.a_(double, double, double) -> Entity.setPos(...)
		nmsPlayer.setPos(-120.5, 71, -183.5);
		nmsPlayer.setYRot(0.0f);
		nmsPlayer.setXRot(0.0f);
		nmsPlayer.setNoGravity(false);

		// 6) Register with the server’s player list (so Bukkit sees it as a Player)
		nmsServer.getPlayerList().placeNewPlayer(nm, nmsPlayer, cookie);

		// 7) Send packets to Players about the new Entity

		nmsPlayer.getEntityData().set(net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7F);

		ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(nmsPlayer.getId(), nmsPlayer.getEntityData().getNonDefaultValues());

		Utils.broadcastPacket(entityMetadataPacket);

		Player bukkitPlayer = nmsPlayer.getBukkitEntity();

		// Add fake player to no collision team
		addToNoCollisionTeam(bukkitPlayer);
		bukkitPlayer.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier(new NamespacedKey(M7tas.getInstance(), "speed"), 3, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));

		// 8) Return the Bukkit wrapper
		return bukkitPlayer;
	}

	private static void setupNoCollisionTeam() {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		if(manager != null) {
			Scoreboard scoreboard = manager.getMainScoreboard();

			// Remove existing team if it exists
			Team existingTeam = scoreboard.getTeam("nocollision");
			if(existingTeam != null) {
				existingTeam.unregister();
			}

			// Create new team with no collision
			noCollisionTeam = scoreboard.registerNewTeam("nocollision");
			noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
			noCollisionTeam.setCanSeeFriendlyInvisibles(false);
		}
	}

	public static void addToNoCollisionTeam(Player player) {
		if(noCollisionTeam != null) {
			noCollisionTeam.addEntry(player.getName());
		}
	}

	public static void removeFromNoCollisionTeam(Player player) {
		if(noCollisionTeam != null) {
			noCollisionTeam.removeEntry(player.getName());
		}
	}

	public static void addEntityToNoCollisionTeam(Entity entity) {
		if(noCollisionTeam != null) {
			noCollisionTeam.addEntry(entity.getUniqueId().toString());
		}
	}

	public static void removeEntityFromNoCollisionTeam(Entity entity) {
		if(noCollisionTeam != null) {
			noCollisionTeam.removeEntry(entity.getUniqueId().toString());
		}
	}

	public static void preventPlayerCollision(Player realPlayer, Player fakePlayer) {
		// Add both players to the no-collision team
		addToNoCollisionTeam(realPlayer);
		addToNoCollisionTeam(fakePlayer);

		// Also make the real player unable to be hit by projectiles while spectating
		realPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, true, false));
	}

	public static void runTAS(World world, String section) {
		if(fakePlayers.isEmpty()) {
			Bukkit.broadcastMessage(ChatColor.RED + "Could not run TAS!  There are no actors.");
			return;
		}

		setInventories();
		Server.serverSetup(world);

		Archer.archerInstructions(fakePlayers.get("Archer"), section);
		Berserk.berserkInstructions(fakePlayers.get("Mage3"), section);
		Healer.healerInstructions(fakePlayers.get("Healer"), section);
		Mage.mageInstructions(fakePlayers.get("Mage1"), section);
		Tank.tankInstructions(fakePlayers.get("Mage2"), section);

		Server.serverInstructions(world, section);

		// Restart spectator sync so it runs AFTER all instruction tasks in each tick
		Spectate.stopSpectatorSync();
		Spectate.startSpectatorSync();
	}

	public static Plugin getInstance() {
		return plugin;
	}

	private static void startFakePlayerTicker() {
		if(fakeTickerStarted) {
			return;
		}
		fakeTickerStarted = true;

		Method updatePlayerPose;
		try {
			updatePlayerPose = net.minecraft.world.entity.player.Player.class
					.getDeclaredMethod("updatePlayerPose");
			updatePlayerPose.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Failed to find updatePlayerPose", e);
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				for(Player fake : fakePlayers.values()) {
					if(!(fake instanceof CraftPlayer)) continue;
					ServerPlayer npc = ((CraftPlayer) fake).getHandle();
					npc.setNoGravity(false);
					try {
						updatePlayerPose.invoke(npc);
					} catch (Exception e) {
						e.printStackTrace();
					}
					npc.aiStep();
				}
			}
		}.runTaskTimer(getInstance(), 0, 1);
	}

	private static final Map<ServerPlayer, BukkitRunnable> customConnectionTask = new HashMap<>();

	private static void forceCustomConnection(ServerPlayer serverPlayer, TASGamePacketListenerImpl connection) {
		customConnectionTask.put(serverPlayer, new BukkitRunnable() {
			@Override
			public void run() {
				serverPlayer.connection = connection;
			}
		});
		customConnectionTask.get(serverPlayer).runTaskTimer(getInstance(), 0, 1);
	}

	private static void stopCustomConnection() {
		for(ServerPlayer serverPlayer : customConnectionTask.keySet()) {
			customConnectionTask.get(serverPlayer).cancel();
			MinecraftServer nmsServer = ((CraftServer) getInstance().getServer()).getServer();
			Connection nm = new Connection(PacketFlow.SERVERBOUND) {
				{
					this.channel = new EmbeddedChannel();
					this.address = new InetSocketAddress("127.0.0.1", 0);
				}

				@Override
				public void send(Packet<?> packet) {

				}

				@Override
				public boolean isConnected() {
					return true;
				}
			};
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(serverPlayer.getGameProfile(), false);
			serverPlayer.connection = new ServerGamePacketListenerImpl(nmsServer, nm, serverPlayer, cookie);
		}
	}
}