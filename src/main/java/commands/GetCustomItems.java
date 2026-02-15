package commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GetCustomItems implements CommandExecutor {
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}
		ItemStack stonk = new ItemStack(Material.DIAMOND_PICKAXE);
		stonk.addUnsafeEnchantment(Enchantment.EFFICIENCY, 255);
		ItemMeta meta = stonk.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.RED + "Dungeonbreaker");
		List<String> lore = new ArrayList<>();
		lore.add("skyblock/combat/stonk");
		meta.setLore(lore);
		stonk.setItemMeta(meta);

		ItemStack term = new ItemStack(Material.BOW);
		meta = term.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Precise Terminator");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/terminator");
		meta.setLore(lore);
		term.setItemMeta(meta);

		ItemStack gyro = new ItemStack(Material.BLAZE_ROD);
		meta = gyro.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Gyrokinetic Wand");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/gyro");
		meta.setLore(lore);
		gyro.setItemMeta(meta);

		ItemStack scylla = new ItemStack(Material.IRON_SWORD);
		meta = scylla.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Withered Hyperion");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/scylla");
		meta.setLore(lore);
		scylla.setItemMeta(meta);

		ItemStack aotv = new ItemStack(Material.DIAMOND_SHOVEL);
		meta = aotv.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Warped Aspect of the Void");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/aotv");
		meta.setLore(lore);
		aotv.setItemMeta(meta);

		ItemStack rag = new ItemStack(Material.GOLDEN_AXE);
		meta = rag.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.DARK_PURPLE + "Withered Ragnarok Axe");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/rag");
		meta.setLore(lore);
		rag.setItemMeta(meta);

		ItemStack aots = new ItemStack(Material.DIAMOND_AXE);
		meta = aots.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Withered Axe of the Shredded");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/aots");
		meta.setLore(lore);
		aots.setItemMeta(meta);

		ItemStack iceSpray = new ItemStack(Material.STICK);
		meta = iceSpray.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Heroic Ice Spray Wand");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/ice_spray");
		meta.setLore(lore);
		iceSpray.setItemMeta(meta);

		ItemStack flamingFlay = new ItemStack(Material.FISHING_ROD);
		meta = flamingFlay.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Withered Flaming Flay");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/flaming_flay");
		meta.setLore(lore);
		flamingFlay.setItemMeta(meta);

		ItemStack bonzo = new ItemStack(Material.BREEZE_ROD);
		meta = bonzo.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.DARK_PURPLE + "Heroic Bonzo Staff");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/bonzo");
		meta.setLore(lore);
		bonzo.setItemMeta(meta);

		ItemStack lb = new ItemStack(Material.BOW);
		meta = lb.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Precise Last Breath");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/last_breath");
		meta.setLore(lore);
		lb.setItemMeta(meta);

		ItemStack tac = new ItemStack(Material.BLAZE_ROD);
		meta = tac.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Tactical Insertion");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/tac");
		meta.setLore(lore);
		tac.setItemMeta(meta);

		ItemStack infinityboom = new ItemStack(Material.TNT);
		meta = tac.getItemMeta();
		meta.setUnbreakable(true);
		meta.setDisplayName(ChatColor.GOLD + "Infinityboom TNT");
		lore = new ArrayList<>();
		lore.add("skyblock/combat/infinityboom");
		meta.setLore(lore);
		tac.setItemMeta(meta);

		p.getInventory().addItem(scylla, aotv, iceSpray, bonzo, term, stonk, rag, lb, gyro, aots, tac, flamingFlay, infinityboom);
		p.sendMessage(ChatColor.GREEN + "Here you go!");
		return true;
	}
}
