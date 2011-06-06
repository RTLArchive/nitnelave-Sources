package com.nitnelave.FreeSlot;


import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;

//import com.nijikokun.bukkit.Permissions.Permissions;
//import com.nijiko.permissions.PermissionHandler;
import java.util.logging.Logger;

public class FreeSlot extends JavaPlugin{
	private final Logger log = Logger.getLogger("Minecraft");
	//public static PermissionHandler Permissions = null;

	public void onEnable() {

		//setupPermissions();

		PluginDescriptionFile pdfFile = this.getDescription();

		log.info("[FreeSlot] "+ pdfFile.getVersion() + "by nitnelave is enabled");
	}

	public void onDisable() {
		log.info("[FreeSlot] disabled");
	}

	/*public void setupPermissions() {
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if(Permissions == null) {
			if(test != null) {
				Permissions = ((Permissions)test).getHandler();
			}
		}

	}*/

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		String errormsg = "";


		if (sender instanceof Player) {
			sender.sendMessage("Console command only");
			return true;
		}


		if( args.length > 0) {

			int itemid = 0;
			String playername = args[0];
			Player player = getServer().getPlayer(playername);
			if (player != null)
				playername = player.getName();
			int durability = 0;

			if(args.length > 2) {
				try {
					durability = Integer.parseInt(args[2]);
				}
				catch (NumberFormatException e) {
					errormsg = "Wrong durability number";
				}
			}

			if(args.length > 1) {
				String itemstring = args[1];
				try {
					itemid = Integer.parseInt(itemstring);
				}
				catch(NumberFormatException e) {

					itemstring = itemstring.toUpperCase();

					try {
						itemid = Material.getMaterial(itemstring).getId();
					}
					catch (NullPointerException n) {
						errormsg = "The item '"+itemstring+"' does not exist.";
					}
				}
			}

			if(errormsg.length()>0){
				log.info(errormsg);
				return true;
			}

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
					log.info("The player " + playername + " has " + freecounter + " free slots.");
					if(itemid != 0){
						stackcounter = stackcounter + Material.getMaterial(itemid).getMaxStackSize() * freecounter;
						String message = playername + " can get "+ stackcounter + " pieces of " + Material.getMaterial(itemid).toString();
						if (durability != 0)
							message = message + " with durability : " + durability;
						log.info(message);
					}



					return true;
				}
				else {
					log.warning("The player "+args[0]+" is not online.");
					return true;
				}
			}
			else {
				log.warning("The player "+args[0]+" is not online.");
				return true;
			}
		}
		else {
			return false;
		}




	}


}
