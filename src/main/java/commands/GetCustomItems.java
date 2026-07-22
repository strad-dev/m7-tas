package commands;

import net.kyori.adventure.text.Component;
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
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;

public class GetCustomItems implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage(Utils.msg("Only players can run this"));
			return true;
		}

		// A class name sets the player's inventory to that class's exact loadout (shared with the fake-player
		// setup via FakePlayerInventory.applyClassLoadout). "all" or no argument falls through to the full
		// custom-item pile below.
		String section = args.length >= 1 ? args[0].toLowerCase() : "all";
		if(!section.equals("all")) {
			boolean isClass = section.equals("archer") || section.equals("berserk") || section.equals("healer") || section.equals("mage") || section.equals("tank");
			if(!isClass) {
				p.sendMessage(Utils.msg("<red>Invalid class. Valid: archer berserk healer mage tank all"));
				return true;
			}
			String role = Character.toUpperCase(section.charAt(0)) + section.substring(1);
			FakePlayerInventory.applyClassLoadout(p, role);

			// Apply the class scoreboard tag (removing any other class tag) so class-gated behaviour (mage beam,
			// archer/berserk damage paths, etc.) treats this real player as that class — mirrors how the named
			// fake players are identified.
			for(String tag : new String[]{"Archer", "Berserk", "Healer", "Mage", "Tank"}) p.removeScoreboardTag(tag);
			p.addScoreboardTag(role);

			// Pre-enchant the terminator (slot 4) with the same Power level the TAS uses for that class, so a real
			// player testing as that class has a correctly-powered bow without running the full TAS.
			int power = terminatorPower(role);
			if(power > 0) {
				ItemStack term = p.getInventory().getItem(4);
				if(term != null && term.getType() == Material.BOW) term.addUnsafeEnchantment(Enchantment.POWER, power);
			}

			p.sendMessage(Utils.msg("<green>Set your inventory to the " + role + " loadout!"));
			return true;
		}

		ItemStack stonk = new ItemStack(Material.DIAMOND_PICKAXE);
		stonk.addUnsafeEnchantment(Enchantment.EFFICIENCY, 255);
		ItemMeta meta = stonk.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<red>Dungeonbreaker"));
		meta.itemName(Utils.mm("<red>Dungeonbreaker"));
		meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(new NamespacedKey(M7tas.getInstance(), "stonk"), 1024, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
		List<Component> lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/stonk"));
		meta.lore(lore);
		stonk.setItemMeta(meta);
		// Lets Dungeonbreaker break any block while the holder is in adventure mode (matches Hypixel behaviour).
		stonk = plugin.Utils.breakAnyBlockInAdventure(stonk);

		ItemStack term = new ItemStack(Material.BOW);
		meta = term.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<light_purple>Precise Terminator"));
		meta.itemName(Utils.mm("<light_purple>Precise Terminator"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/terminator"));
		meta.lore(lore);
		term.setItemMeta(meta);

		ItemStack gyro = new ItemStack(Material.BLAZE_ROD);
		meta = gyro.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<gold>Gyrokinetic Wand"));
		meta.itemName(Utils.mm("<gold>Gyrokinetic Wand"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/gyro"));
		meta.lore(lore);
		gyro.setItemMeta(meta);

		ItemStack scylla = new ItemStack(Material.IRON_SWORD);
		meta = scylla.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<light_purple>Withered Hyperion"));
		meta.itemName(Utils.mm("<light_purple>Withered Hyperion"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/scylla"));
		meta.lore(lore);
		scylla.setItemMeta(meta);

		ItemStack aotv = new ItemStack(Material.DIAMOND_SHOVEL);
		meta = aotv.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<gold>Warped Aspect of the Void"));
		meta.itemName(Utils.mm("<gold>Warped Aspect of the Void"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/aotv"));
		meta.lore(lore);
		aotv.setItemMeta(meta);

		ItemStack rag = new ItemStack(Material.GOLDEN_AXE);
		meta = rag.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<dark_purple>Withered Ragnarok Axe"));
		meta.itemName(Utils.mm("<dark_purple>Withered Ragnarok Axe"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/rag"));
		meta.lore(lore);
		rag.setItemMeta(meta);

		ItemStack aots = new ItemStack(Material.DIAMOND_AXE);
		meta = aots.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<light_purple>Withered Axe of the Shredded"));
		meta.itemName(Utils.mm("<light_purple>Withered Axe of the Shredded"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/aots"));
		meta.lore(lore);
		aots.setItemMeta(meta);

		ItemStack iceSpray = new ItemStack(Material.STICK);
		meta = iceSpray.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<gold>Heroic Ice Spray Wand"));
		meta.itemName(Utils.mm("<gold>Heroic Ice Spray Wand"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/ice_spray"));
		meta.lore(lore);
		iceSpray.setItemMeta(meta);

		ItemStack flamingFlay = new ItemStack(Material.FISHING_ROD);
		meta = flamingFlay.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<light_purple>Withered Flaming Flay"));
		meta.itemName(Utils.mm("<light_purple>Withered Flaming Flay"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/flaming_flay"));
		meta.lore(lore);
		flamingFlay.setItemMeta(meta);

		ItemStack bonzo = new ItemStack(Material.BREEZE_ROD);
		meta = bonzo.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<dark_purple>Heroic Bonzo Staff"));
		meta.itemName(Utils.mm("<dark_purple>Heroic Bonzo Staff"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/bonzo"));
		meta.lore(lore);
		bonzo.setItemMeta(meta);

		ItemStack lb = new ItemStack(Material.BOW);
		meta = lb.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<light_purple>Precise Last Breath"));
		meta.itemName(Utils.mm("<light_purple>Precise Last Breath"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/last_breath"));
		meta.lore(lore);
		lb.setItemMeta(meta);

		ItemStack explosiveBow = new ItemStack(Material.BOW);
		meta = explosiveBow.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<gold>Explosive Bow"));
		meta.itemName(Utils.mm("<gold>Explosive Bow"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/explosive_bow"));
		meta.lore(lore);
		explosiveBow.setItemMeta(meta);

		ItemStack tac = new ItemStack(Material.BLAZE_ROD);
		meta = tac.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<gold>Tactical Insertion"));
		meta.itemName(Utils.mm("<gold>Tactical Insertion"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/tac"));
		meta.lore(lore);
		tac.setItemMeta(meta);

		ItemStack infinityboom = new ItemStack(Material.TNT);
		meta = infinityboom.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<gold>Infinityboom TNT"));
		meta.itemName(Utils.mm("<gold>Infinityboom TNT"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/infinityboom"));
		meta.lore(lore);
		infinityboom.setItemMeta(meta);
		// Placeable on any block in adventure mode (the practice default), like the fake-player loadout.
		infinityboom = plugin.Utils.placeOnAnythingInAdventure(infinityboom);

		ItemStack jerrychine = new ItemStack(Material.GOLDEN_HORSE_ARMOR);
		meta = jerrychine.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<gold>Heroic Jerry-chine Gun"));
		meta.itemName(Utils.mm("<gold>Heroic Jerry-chine Gun"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/jerrychine"));
		meta.lore(lore);
		jerrychine.setItemMeta(meta);

		ItemStack springBoots = new ItemStack(Material.CHAINMAIL_BOOTS);
		meta = springBoots.getItemMeta();
		meta.setUnbreakable(true);
		meta.displayName(Utils.mm("<dark_purple>Renowned Spring Boots"));
		meta.itemName(Utils.mm("<dark_purple>Renowned Spring Boots"));
		lore = new ArrayList<>();
		lore.add(Utils.mm("skyblock/combat/spring_boots"));
		meta.lore(lore);
		springBoots.setItemMeta(meta);

		p.getInventory().addItem(scylla, aotv, iceSpray, bonzo, term, stonk, rag, lb, explosiveBow, gyro, aots, tac, flamingFlay, infinityboom, springBoots, jerrychine);
		p.sendMessage(Utils.msg("<green>Here you go!"));
		return true;
	}

	/** Terminator Power enchant level per class, matching the TAS (Archer 66, Berserk/Healer/Tank 16). Mage uses
	 *  no terminator → 0. */
	private static int terminatorPower(String role) {
		return switch(role) {
			case "Archer" -> 70;
			case "Berserk", "Healer", "Tank" -> 17;
			default -> 0;
		};
	}
}
