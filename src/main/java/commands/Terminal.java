package commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import plugin.Utils;

public class Terminal implements CommandExecutor {
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Utils.scheduleTask(() -> {
			if(sender instanceof BlockCommandSender commandBlock) {
				Location l = commandBlock.getBlock().getLocation();
				Block b;
				if(args.length < 2) {
					Bukkit.broadcastMessage(ChatColor.RED + "Badly configured terminal at " + l.getX() + " " + l.getY() + " " + l.getZ() + ": Not enough arguments");
					return;
				}
				Switch lever;
				String message = ChatColor.GOLD + Utils.getNearestPlayer(l).getName() + ChatColor.GREEN + " activated a ";
				switch(args[0]) {
					case "1" -> {
						switch(args[1]) {
							case "1" -> {
								b = l.getWorld().getBlockAt(110, 113, 73);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.WEST);
								message += "terminal!";
							}
							case "2" -> {
								b = l.getWorld().getBlockAt(110, 119, 79);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.WEST);
								message += "terminal!";
							}
							case "3" -> {
								b = l.getWorld().getBlockAt(90, 112, 92);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.EAST);
								message += "terminal!";
							}
							case "4" -> {
								b = l.getWorld().getBlockAt(90, 122, 101);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.EAST);
								message += "terminal!";
							}
							case "left" -> {
								b = l.getWorld().getBlockAt(110, 113, 73);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
								lever.setFacing(BlockFace.NORTH);
								message += "lever!";
							}
							case "right" -> {
								b = l.getWorld().getBlockAt(94, 124, 113);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
								lever.setFacing(BlockFace.NORTH);
								message += "lever!";
							}
							default -> {
								Bukkit.broadcastMessage(ChatColor.RED + "Badly configured terminal at " + l.getX() + " " + l.getY() + " " + l.getZ() + ": Invalid terminal provided.  Valid terminals for this section are 1 2 3 4 left right ss");
								return;
							}
						}
					}
					case "2" -> {
						switch(args[1]) {
							case "1" -> {
								b = l.getWorld().getBlockAt(68, 109, 122);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.SOUTH);
								message += "terminal!";
							}
							case "2" -> {
								b = l.getWorld().getBlockAt(59, 112, 123);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.SOUTH);
								message += "terminal!";
							}
							case "3" -> {
								b = l.getWorld().getBlockAt(47, 109, 122);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.SOUTH);
								message += "terminal!";
							}
							case "4" -> {
								b = l.getWorld().getBlockAt(39, 108, 142);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.NORTH);
								message += "terminal!";
							}
							case "5" -> {
								b = l.getWorld().getBlockAt(40, 124, 123);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.NORTH);
								message += "terminal!";
							}
							case "bottom" -> {
								b = l.getWorld().getBlockAt(27, 124, 127);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
								lever.setFacing(BlockFace.NORTH);
								message += "lever!";
							}
							case "top" -> {
								b = l.getWorld().getBlockAt(23, 132, 138);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
								lever.setFacing(BlockFace.NORTH);
								message += "lever!";
							}
							default -> {
								Bukkit.broadcastMessage(ChatColor.RED + "Badly configured terminal at " + l.getX() + " " + l.getY() + " " + l.getZ() + ": Invalid terminal provided.  Valid terminals for this section are 1 2 3 4 5 top bottom lights");
								return;
							}
						}
					}
					case "3" -> {
						switch(args[1]) {
							case "1" -> {
								b = l.getWorld().getBlockAt(-2, 109, 112);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.EAST);
								message += "terminal!";
							}
							case "2" -> {
								b = l.getWorld().getBlockAt(-2, 119, 93);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.EAST);
								message += "terminal!";
							}
							case "3" -> {
								b = l.getWorld().getBlockAt(18, 123, 93);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.WEST);
								message += "terminal!";
							}
							case "4" -> {
								b = l.getWorld().getBlockAt(-2, 109, 77);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.EAST);
								message += "terminal!";
							}
							case "left" -> {
								b = l.getWorld().getBlockAt(2, 122, 55);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
								lever.setFacing(BlockFace.NORTH);
								message += "lever!";
							}
							case "right" -> {
								b = l.getWorld().getBlockAt(14, 122, 55);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
								lever.setFacing(BlockFace.NORTH);
								message += "lever!";
							}
							default -> {
								Bukkit.broadcastMessage(ChatColor.RED + "Badly configured terminal at " + l.getX() + " " + l.getY() + " " + l.getZ() + ": Invalid terminal provided.  Valid terminals for this section are 1 2 3 4 left right arrows");
								return;
							}
						}
					}
					case "4" -> {
						switch(args[1]) {
							case "1" -> {
								b = l.getWorld().getBlockAt(41, 109, 30);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.SOUTH);
								message += "terminal!";
							}
							case "2" -> {
								b = l.getWorld().getBlockAt(44, 121, 30);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.SOUTH);
								message += "terminal!";
							}
							case "3" -> {
								b = l.getWorld().getBlockAt(67, 109, 30);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.SOUTH);
								message += "terminal!";
							}
							case "4" -> {
								b = l.getWorld().getBlockAt(72, 115, 47);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
								lever.setFacing(BlockFace.NORTH);
								message += "terminal!";
							}
							case "bottom" -> {
								b = l.getWorld().getBlockAt(84, 121, 34);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
								lever.setFacing(BlockFace.NORTH);
								message += "lever!";
							}
							case "top" -> {
								b = l.getWorld().getBlockAt(86, 128, 46);
								b.setType(Material.LEVER);
								lever = (Switch) b.getBlockData();
								lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
								lever.setFacing(BlockFace.NORTH);
								message += "lever!";
							}
							default -> {
								Bukkit.broadcastMessage(ChatColor.RED + "Badly configured terminal at " + l.getX() + " " + l.getY() + " " + l.getZ() + ": Invalid terminal provided.  Valid terminals for this section are 1 2 3 4 bottom top shooty");
								return;
							}
						}
					}
					default -> {
						Bukkit.broadcastMessage(ChatColor.RED + "Badly configured terminal at " + l.getX() + " " + l.getY() + " " + l.getZ() + ": Invalid section provided.  Valid sections are 1 2 3 4");
						return;
					}
				}
				lever.setPowered(false);
				Bukkit.broadcastMessage(message);
				String finalMessage = message;
				Bukkit.getOnlinePlayers().forEach(p1 -> p1.sendTitle("", finalMessage, 0, 40, 0));
			} else {
				sender.sendMessage(ChatColor.RED + "Only command blocks may run this.");
			}
		}, 1);
		return true;
	}
}