package plugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import damage.ReforgeId;
import items.armor.*;
import items.bows.ExplosiveBow;
import items.bows.LastBreath;
import items.bows.Terminator;
import items.combat.*;
import items.tools.Dungeonbreaker;
import items.tools.GyrokineticWand;
import items.tools.InfinityboomTNT;
import items.tools.TacticalInsertion;
import items.util.Infinileap;
import items.util.PitchinRod;
import items.util.SkyblockMenu;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import nms.NBT;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.profile.CraftPlayerProfile;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

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

			applyClassLoadout(p, entry);

			PlayerInventory inventory = p.getInventory();
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

	/**
	 * The ONE definition of a class's kit, as a 41-slot array: [0..35] main inventory, [36] helmet, [37] chestplate,
	 * [38] leggings, [39] boots, [40] off-hand. Read by the catalog export ({@link Catalog}) and
	 * {@link #applyClassLoadout(Player, String)}.
	 */
	public static ItemStack[] classLoadoutContents(String role) {
		ItemStack[] arr = new ItemStack[41];

		// 16 vanilla ender pearls: the one kit stack that isn't an Item (no name, no lore ID).
		ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
		pearls.setAmount(16);

		switch(role) {
			case "Archer", "Berserk", "Healer", "Tank" -> {
				arr[36] = NecronHead.INSTANCE.build();
				arr[37] = NecronChestplate.INSTANCE.build();
				arr[38] = NecronLeggings.INSTANCE.build();
				arr[39] = NecronBoots.INSTANCE.build();
			}
			case "Mage", "Mage1", "Mage2", "Mage3", "Mage4" -> {
				arr[36] = StormHelmet.INSTANCE.build();
				arr[37] = StormChestplate.INSTANCE.build();
				arr[38] = StormLeggings.INSTANCE.build();
				arr[39] = StormBoots.INSTANCE.build();
			}
		}

		arr[0] = Hyperion.INSTANCE.build(ReforgeId.HEROIC);
		arr[1] = AspectOfTheVoid.INSTANCE.build();
		arr[5] = Dungeonbreaker.INSTANCE.build();
		arr[6] = GyrokineticWand.INSTANCE.build();
		arr[7] = pearls;
		arr[8] = SkyblockMenu.INSTANCE.build();
		arr[9] = SpiritMask.INSTANCE.build();
		arr[10] = BonzoMask.INSTANCE.build();
		arr[11] = SpringBootsItem.INSTANCE.build();
		arr[12] = RacingHelmet.INSTANCE.build();
		arr[13] = CowHat.INSTANCE.build();
		arr[28] = BonzoStaff.INSTANCE.build();
		arr[29] = TacticalInsertion.INSTANCE.build();
		arr[34] = JerrychineGun.INSTANCE.build();

		switch(role) {
			case "Archer" -> {
				arr[2] = Infinileap.INSTANCE.build();
				arr[3] = InfinityboomTNT.INSTANCE.build();
				arr[4] = Terminator.INSTANCE.build();
				arr[18] = ThermodynamicHelmet.INSTANCE.build();
				arr[19] = ThermodynamicChestplate.INSTANCE.build();
				arr[20] = ThermodynamicLeggings.INSTANCE.build();
				arr[21] = ThermodynamicBoots.INSTANCE.build();
				// Slot 30 was the Rapid Bonemerang; dropped, it had no ability.
				arr[32] = LastBreath.INSTANCE.build();
				arr[33] = RagnarockAxe.INSTANCE.build();
				arr[35] = PitchinRod.INSTANCE.build();
			}
			case "Berserk" -> {
				arr[2] = Infinileap.INSTANCE.build();
				arr[3] = InfinityboomTNT.INSTANCE.build();
				arr[4] = Terminator.INSTANCE.build();
				arr[33] = RagnarockAxe.INSTANCE.build();
				arr[35] = PitchinRod.INSTANCE.build();
			}
			case "Healer" -> {
				arr[2] = IceSprayWand.INSTANCE.build();
				arr[3] = InfinityboomTNT.INSTANCE.build();
				arr[4] = Terminator.INSTANCE.build();
				arr[30] = Infinileap.INSTANCE.build();
				arr[32] = FlamingFlay.INSTANCE.build();
				arr[33] = LastBreath.INSTANCE.build();
				arr[35] = PitchinRod.INSTANCE.build();
			}
			case "Mage", "Mage1", "Mage2", "Mage3", "Mage4" -> {
				arr[2] = IceSprayWand.INSTANCE.build();
				arr[3] = DarkClaymore.INSTANCE.build();
				arr[4] = Infinileap.INSTANCE.build();
				// Storage row 2, above the Infinityboom in slot 31. No Power: Catalog.defaultFor's class bake only
				// touches slot 4, and the Mage's Terminator Power is 0 anyway.
				arr[22] = Terminator.INSTANCE.build();
				arr[30] = Hyperion.INSTANCE.build(ReforgeId.FABLED);
				arr[31] = InfinityboomTNT.INSTANCE.build();
				arr[32] = RagnarockAxe.INSTANCE.build();
				arr[33] = LastBreath.INSTANCE.build();
				arr[35] = ExplosiveBow.INSTANCE.build();
			}
			case "Tank" -> {
				arr[2] = IceSprayWand.INSTANCE.build();
				arr[3] = InfinityboomTNT.INSTANCE.build();
				arr[4] = Terminator.INSTANCE.build();
				arr[30] = Infinileap.INSTANCE.build();
				arr[31] = AxeOfTheShredded.INSTANCE.build();
				arr[32] = FlamingFlay.INSTANCE.build();
				arr[33] = LastBreath.INSTANCE.build();
			}
		}
		return arr;
	}

	/**
	 * Clears {@code p}'s inventory and applies {@link #classLoadoutContents(String)}. {@code role} is a class name
	 * ({@code Archer}/{@code Berserk}/{@code Healer}/{@code Mage}/{@code Tank}) or a fake name ({@code Mage1}-
	 * {@code Mage4}, the Mage kit).
	 */
	public static void applyClassLoadout(Player p, String role) {
		ItemStack[] arr = classLoadoutContents(role);
		PlayerInventory inventory = p.getInventory();
		inventory.clear();

		// A kit sets the TAS speed (400).
		Utils.setSpeed(p, 400);

		for(int i = 0; i < 36; i++) inventory.setItem(i, arr[i]);
		inventory.setHelmet(arr[36]);
		inventory.setChestplate(arr[37]);
		inventory.setLeggings(arr[38]);
		inventory.setBoots(arr[39]);
		if(arr[40] != null) inventory.setItemInOffHand(arr[40]);
	}

	// --- Item Creators ---

	public static ItemStack getCustomHead(String displayName, String identifier, String textureValue, String textureSignature) {
		Multimap<String, Property> props = HashMultimap.create();
		props.put("textures", new Property("textures", textureValue, textureSignature));
		PropertyMap propertyMap = new PropertyMap(props);
		// Profile id from the identifier, NOT random: it's in the item's NBT, and a random one made every copy of a
		// head byte-different, so the palette listed shared heads (Spirit Mask, Bonzo Mask, ...) once per class.
		GameProfile gp = new GameProfile(UUID.nameUUIDFromBytes(identifier.getBytes(StandardCharsets.UTF_8)), identifier, propertyMap);

		CraftPlayerProfile profile = new CraftPlayerProfile(gp);

		ItemStack helmet = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) helmet.getItemMeta();
		assert meta != null;
		meta.displayName(Utils.mm(displayName));
		meta.setPlayerProfile(profile);
		helmet.setItemMeta(meta);
		helmet.addUnsafeEnchantment(Enchantment.PROTECTION, 5);
		return damage.StatLore.apply(helmet);
	}

	/** Wearables identified by sight (no lore ID), via the item registry so a rename can't make one match nothing. */
	public static boolean isRacingHelmet(ItemStack item) {
		return RacingHelmet.INSTANCE.matches(item);
	}

	public static boolean isCowHat(ItemStack item) {
		return CowHat.INSTANCE.matches(item);
	}

	public static boolean isSpiritMask(ItemStack item) {
		return SpiritMask.INSTANCE.matches(item);
	}

	public static boolean isBonzoMask(ItemStack item) {
		return BonzoMask.INSTANCE.matches(item);
	}

	/** The SkyBlock Menu (nether star in hotbar slot 8). */
	public static boolean isSkyblockMenu(ItemStack item) {
		return SkyblockMenu.INSTANCE.matches(item);
	}

	/**
	 * {@code id} becomes lore line 0 and must stay there: {@code items.ItemUtils.getID} and {@code Catalog.paletteKey}
	 * read it. {@code damage.StatLore} appends stat rows below from the damage math's own term lists (MAP.md §7b).
	 */
	public static ItemStack getSkyBlockItem(Material material, String name, String id, String sbId) {
        return getSkyBlockItem(material, name, id, nbt -> nbt.putString("id", sbId));
	}

    public static ItemStack getSkyBlockItem(Material material, String name, String id, Consumer<CompoundTag> nbtConsumer) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.itemName(Utils.mm(name));
        meta.setUnbreakable(true);
        meta.displayName(Utils.mm(name));
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.mm(id));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        return damage.StatLore.apply(NBT.modify(item, nbtConsumer));
    }
}
