package com.nitnelave.FreeSlot;


import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import info.somethingodd.bukkit.OddItem.OddItem;

import java.util.logging.Logger;

public class FreeSlot extends JavaPlugin{
	private final Logger log = Logger.getLogger("Minecraft");
	public static PermissionHandler Permissions = null;
	public static OddItem OI = null;


	public void onEnable() {

		setupPermissions();

		OI = (OddItem) getServer().getPluginManager().getPlugin("OddItem");
		if(OI != null) {
			log.info("[FreeSlot] Successfully connected with OddItem");
		}
		PluginDescriptionFile pdfFile = this.getDescription();

		log.info("[FreeSlot] "+ pdfFile.getVersion() + "by nitnelave is enabled");
	}

	public void onDisable() {
		log.info("[FreeSlot] disabled");
	}

	public void setupPermissions() {
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if(Permissions == null) {
			if(test != null) {
				Permissions = ((Permissions)test).getHandler();
				log.info("[FreeSlot] Successfully connected to Permissions");
			}
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		boolean canUseCommand = true;
		if(sender instanceof Player) {
			if(Permissions != null) {
				canUseCommand = Permissions.has((Player)sender, "FreeSlot.use");
			}
			else {
				canUseCommand = sender.isOp();
			}
		}
		return runCommand(sender, cmd, commandLabel, args, canUseCommand);
		
	}
	
	public boolean runCommand(CommandSender sender, Command cmd, String commandLabel, String[] args, boolean canUseCommand) {

		String errormsg = "";
		if(canUseCommand){
			if( args.length > 0) {

				int itemid = 0;
				short durability = 0;

				if(args.length > 2) {
					try {
						durability = Short.parseShort(args[2]);
					}
					catch (NumberFormatException e) {
						errormsg = "Wrong durability number";
					}
				}

				if(args.length > 1) {
					try {
						itemid = Integer.parseInt(args[1]);
					}
					catch (NumberFormatException e) {
						if(OI !=null) { //get the OddItem name
							try {
								itemid = OI.getItemStack(args[1]).getTypeId();
							}
							catch (IllegalArgumentException ex) {
								errormsg = "Did you mean : "+ex.getMessage()+" ?";
							}
						}
						else { //get the ENUM name

							String itemstring = args[1].toUpperCase();
							try {
								itemid = Material.getMaterial(itemstring).getId();
							}
							catch (NullPointerException n) {
								errormsg = "The item '"+itemstring+"' does not exist.";
							}
						}
					}
				}

				if(errormsg.length()>0){
					sender.sendMessage(errormsg);
					return true;
				}
				return getFreeSlot(args[0], itemid, durability, sender);

			}
			else {
				return false;
			}
		}
		else {
			sender.sendMessage("You do not have the permission to do this");
			return true;
		}
		



	}
	public boolean onCommand(ConsoleCommandSender sender, Command cmd, String commandLabel, String[] args) {

		return runCommand(sender, cmd, commandLabel, args, true);
	}

	private boolean getFreeSlot(String playername, int itemid, short durability, CommandSender sender) {
		Player player = getServer().getPlayer(playername);
		if (player != null) {
			if (player.isOnline()){
				ItemStack[] inventory = new ItemStack[36];
				inventory = player.getInventory().getContents();

				int freecounter = 0;
				int stackcounter = 0;
				for(ItemStack slot : inventory){
					if (slot != null) {
						if(slot.getTypeId() == itemid && slot.getDurability() == durability && itemid != 0)
						{
							stackcounter = stackcounter + slot.getMaxStackSize() - slot.getAmount();
						}

					}
					else {
						freecounter = freecounter+1;
					}

				}
				sender.sendMessage("The player " + playername + " has " + freecounter + " free slots.");
				if(itemid != 0){
					stackcounter = stackcounter + Material.getMaterial(itemid).getMaxStackSize() * freecounter;
					String message = playername + " can get "+ stackcounter + " pieces of " + Material.getMaterial(itemid).toString();
					if (durability != 0)
						message = message + " with durability : " + durability;
					sender.sendMessage(message);
				}



				return true;
			}
			else {
				sender.sendMessage("The player "+playername+" is not online.");
				return true;
			}
		}
		else {
			sender.sendMessage("The player "+playername+" is not online.");
			return true;
		}
	}
	

}
