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
	 * Build the full 41-slot loadout array for a class/role WITHOUT a live player: indices [0..35] are the main
	 * inventory slots, [36] helmet, [37] chestplate, [38] leggings, [39] boots, [40] off-hand. Used by the
	 * cross-server item-catalog export ({@link Catalog}).  IMPORTANT: this mirrors
	 * {@link #applyClassLoadout(Player, String)} item-for-item, so if you change a class's items in one, change
	 * the other to match.
	 */
	public static ItemStack[] classLoadoutContents(String role) {
		ItemStack[] arr = new ItemStack[41];

		// Sixteen vanilla ender pearls: the one stack in a kit that is not an Item, since it has no name, no
		// lore ID and nothing custom about it at all.
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
				// Slot 30 was the Rapid Bonemerang; dropped because it had no ability behind it at all.
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
				// Storage row 2, middle, directly above the Infinityboom in slot 31.  No Power enchant: the class
				// bake in Catalog.defaultFor only touches slot 4, and the Mage's Terminator Power is 0 anyway.
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
	 * Populate {@code p}'s inventory with the full loadout for a class/role: armor set, shared hotbar/utility items,
	 * and class-specific weapons in their fixed slots. Clears the inventory first. {@code role} accepts a class name
	 * ({@code Archer}/{@code Berserk}/{@code Healer}/{@code Mage}/{@code Tank}) or a fake-player name
	 * ({@code Mage1}-{@code Mage4}, mapped to the Mage loadout).
	 * <br>
	 * The kit itself comes from {@link #classLoadoutContents(String)}, the ONE definition of what a class carries.
	 * This used to be a hand-copied second listing of the same items, which had to be kept in step item-for-item;
	 * it was only separate because {@code /getcustomitems} applied it to a live player while the catalog export
	 * needed an array. That command is gone (players get their kit from {@code /class} + {@code /m7loadout} +
	 * {@code /m7practice}), so this is now just "the array, applied to a player".
	 */
	public static void applyClassLoadout(Player p, String role) {
		ItemStack[] arr = classLoadoutContents(role);
		PlayerInventory inventory = p.getInventory();
		inventory.clear();

		// Applying a kit gives the player the TAS movement speed (400).
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
		// Profile id derived from the identifier, NOT random: the profile id is part of the item's NBT, so a random
		// one made every copy of the same head a byte-different ItemStack.  That is why the catalog palette used to
		// list the shared heads (Spirit Mask, Bonzo Mask, Racing Helmet, Cow Hat, ...) once per class.
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

	/**
	 * The wearables the rest of the plugin identifies by SIGHT rather than by a lore ID, since none of them
	 * carries one.  Each of these used to compare a hardcoded legacy display-name constant; they ask the item
	 * registry now, so a rename or a recolour cannot leave one of them silently matching nothing.
	 */
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

	/** True if the given item is the SkyBlock Menu (the nether star kept in hotbar slot 8). */
	public static boolean isSkyblockMenu(ItemStack item) {
		return SkyblockMenu.INSTANCE.matches(item);
	}

	/**
	 * Build a custom item.  {@code id} becomes lore line 0, which is where {@code items.ItemUtils.getID} and
	 * {@code Catalog.paletteKey} both read it from - it must stay there.  Stat rows are APPENDED below it by
	 * {@code damage.StatLore}, generated from the same term lists the damage math reads so the two cannot drift
	 * (MAP.md §7b).
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
