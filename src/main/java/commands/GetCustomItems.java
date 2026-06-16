package commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import plugin.FakePlayerInventory;
import plugin.M7tas;

import java.util.ArrayList;
import java.util.List;

public class GetCustomItems implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		// A class name sets the player's inventory to that class's exact loadout (shared with the fake-player
		// setup via FakePlayerInventory.applyClassLoadout). "all" or no argument falls through to the full
		// custom-item pile below.
		String section = args.length >= 1 ? args[0].toLowerCase() : "all";
		if(!section.equals("all")) {
			boolean isClass = section.equals("archer") || section.equals("berserk") || section.equals("healer") || section.equals("mage") || section.equals("tank");
			if(!isClass) {
				p.sendMessage(ChatColor.RED + "Invalid class. Valid: archer berserk healer mage tank all");
				return true;
			}
			String role = Character.toUpperCase(section.charAt(0)) + section.substring(1);
			FakePlayerInventory.applyClassLoadout(p, role);
			p.sendMessage(ChatColor.GREEN + "Set your inventory to the " + role + " loadout!");
			return true;
		}

		ItemStack stonk = new ItemStack(Material.DIAMOND_PICKAXE);
		stonk.addUnsafeEnchantment(Enchantment.EFFICIENCY, 255);
		ItemMeta meta = stonk.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.RED + "Dungeonbreaker");
		meta.setItemName(ChatColor.RED + "Dungeonbreaker");
		meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(new NamespacedKey(M7tas.getInstance(), "stonk"), 1024, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
		List<String> lore = new ArrayList<>();
		lore.add("skyblock/combat/stonk");
		meta.setLore(lore);
		stonk.setItemMeta(meta);
		// Lets Dungeonbreaker break any block while the holder is in adventure mode (matches Hypixel behaviour).
		stonk = plugin.Utils.breakAnyBlockInAdventure(stonk);

		ItemStack term = new ItemStack(Material.BOW);
		meta = term.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Precise Terminator");
		meta.setItemName(ChatColor.LIGHT_PURPLE + "Precise Terminator");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/terminator");
		meta.setLore(lore);
		term.setItemMeta(meta);

		ItemStack gyro = new ItemStack(Material.BLAZE_ROD);
		meta = gyro.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Gyrokinetic Wand");
		meta.setItemName(ChatColor.GOLD + "Gyrokinetic Wand");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/gyro");
		meta.setLore(lore);
		gyro.setItemMeta(meta);

		ItemStack scylla = new ItemStack(Material.IRON_SWORD);
		meta = scylla.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Withered Hyperion");
		meta.setItemName(ChatColor.LIGHT_PURPLE + "Withered Hyperion");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/scylla");
		meta.setLore(lore);
		scylla.setItemMeta(meta);

		ItemStack aotv = new ItemStack(Material.DIAMOND_SHOVEL);
		meta = aotv.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Warped Aspect of the Void");
		meta.setItemName(ChatColor.GOLD + "Warped Aspect of the Void");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/aotv");
		meta.setLore(lore);
		aotv.setItemMeta(meta);

		ItemStack rag = new ItemStack(Material.GOLDEN_AXE);
		meta = rag.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.DARK_PURPLE + "Withered Ragnarok Axe");
		meta.setItemName(ChatColor.DARK_PURPLE + "Withered Ragnarok Axe");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/rag");
		meta.setLore(lore);
		rag.setItemMeta(meta);

		ItemStack aots = new ItemStack(Material.DIAMOND_AXE);
		meta = aots.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Withered Axe of the Shredded");
		meta.setItemName(ChatColor.LIGHT_PURPLE + "Withered Axe of the Shredded");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/aots");
		meta.setLore(lore);
		aots.setItemMeta(meta);

		ItemStack iceSpray = new ItemStack(Material.STICK);
		meta = iceSpray.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Heroic Ice Spray Wand");
		meta.setItemName(ChatColor.GOLD + "Heroic Ice Spray Wand");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/ice_spray");
		meta.setLore(lore);
		iceSpray.setItemMeta(meta);

		ItemStack flamingFlay = new ItemStack(Material.FISHING_ROD);
		meta = flamingFlay.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Withered Flaming Flay");
		meta.setItemName(ChatColor.LIGHT_PURPLE + "Withered Flaming Flay");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/flaming_flay");
		meta.setLore(lore);
		flamingFlay.setItemMeta(meta);

		ItemStack bonzo = new ItemStack(Material.BREEZE_ROD);
		meta = bonzo.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.DARK_PURPLE + "Heroic Bonzo Staff");
		meta.setItemName(ChatColor.DARK_PURPLE + "Heroic Bonzo Staff");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/bonzo");
		meta.setLore(lore);
		bonzo.setItemMeta(meta);

		ItemStack lb = new ItemStack(Material.BOW);
		meta = lb.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Precise Last Breath");
		meta.setItemName(ChatColor.LIGHT_PURPLE + "Precise Last Breath");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/last_breath");
		meta.setLore(lore);
		lb.setItemMeta(meta);

		ItemStack explosiveBow = new ItemStack(Material.BOW);
		meta = explosiveBow.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Explosive Bow");
		meta.setItemName(ChatColor.GOLD + "Explosive Bow");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/explosive_bow");
		meta.setLore(lore);
		explosiveBow.setItemMeta(meta);

		ItemStack tac = new ItemStack(Material.BLAZE_ROD);
		meta = tac.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Tactical Insertion");
		meta.setItemName(ChatColor.GOLD + "Tactical Insertion");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/tac");
		meta.setLore(lore);
		tac.setItemMeta(meta);

		ItemStack infinityboom = new ItemStack(Material.TNT);
		meta = infinityboom.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Infinityboom TNT");
		meta.setItemName(ChatColor.GOLD + "Infinityboom TNT");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/infinityboom");
		meta.setLore(lore);
		infinityboom.setItemMeta(meta);

		ItemStack jerrychine = new ItemStack(Material.GOLDEN_HORSE_ARMOR);
		meta = jerrychine.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Heroic Jerry-chine Gun");
		meta.setItemName(ChatColor.GOLD + "Heroic Jerry-chine Gun");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/jerrychine");
		meta.setLore(lore);
		jerrychine.setItemMeta(meta);

		ItemStack springBoots = new ItemStack(Material.CHAINMAIL_BOOTS);
		meta = springBoots.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.DARK_PURPLE + "Renowned Spring Boots");
		meta.setItemName(ChatColor.DARK_PURPLE + "Renowned Spring Boots");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/spring_boots");
		meta.setLore(lore);
		springBoots.setItemMeta(meta);

		p.getInventory().addItem(scylla, aotv, iceSpray, bonzo, term, stonk, rag, lb, explosiveBow, gyro, aots, tac, flamingFlay, infinityboom, springBoots, jerrychine);
		p.sendMessage(ChatColor.GREEN + "Here you go!");
		return true;
	}
}
