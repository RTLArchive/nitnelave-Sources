package com.nitnelave.DispenserReFill;

import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;


public class DispenserReFill extends JavaPlugin {
	private final Logger log = Logger.getLogger("Minecraft");
	public static PermissionHandler Permissions = null;

	public void onEnable() {


		if (!new File(getDataFolder().toString()).exists() ) {
			new File(getDataFolder().toString()).mkdir();
		}

		File yml = new File(getDataFolder()+"/config.yml");

		if (!yml.exists()) {
			new File(getDataFolder().toString()).mkdir();
			try {
				yml.createNewFile();
			}
			catch (IOException ex) {
				log.warning("[DispenserReFill] Cannot create file "+yml.getPath());
			}

			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(yml, true));

				out.write("use-permissions: OP   #can be OP, permissions or false");
				out.newLine();

				//Close the output stream
				out.close();
			}
			catch (Exception e) {
				log.warning("[DispenserReFill] Cannot write config file: "+e);
			}
		}

		
		setup_permissions();
		PluginDescriptionFile pdfFile = this.getDescription();

		log.info("[DispenserReFill] "+ pdfFile.getVersion() + "by nitnelave is enabled");
	}

	public void onDisable() {
		log.info("DispenserReFill disabled");
	}

	public void setup_permissions() {

		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if(Permissions == null) {
			if(test != null) {
				Permissions = ((Permissions)test).getHandler();
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		String errormsg = "";
		String command = cmd.getName();
		boolean canUseCommand = false;

		if(sender instanceof Player) {
			Player player = (Player) sender;


			String permissions_config = null;
			permissions_config = getConfiguration().getString("use-permissions", "OP").trim();


			if (permissions_config.equalsIgnoreCase("permissions") || permissions_config.equalsIgnoreCase("OP")) {
				if (permissions_config.equalsIgnoreCase("permissions")) {
					if (Permissions != null) {
						canUseCommand = Permissions.has(player, "DispenserReFill.fill");
					}
				}
				else {
					canUseCommand = player.isOp();
				}

			}
			else if(!permissions_config.equalsIgnoreCase("false")){
				log.warning("[DispenserReFill] : Bad option for use-permissions : "+permissions_config);
				return true;
			}
			if(canUseCommand) {
				if(command.equalsIgnoreCase("fill")) {
					if(args.length > 0) {

						Block block = player.getTargetBlock(null, 100);
						if(block.getTypeId() == 23) {
							short durability = 0;
							int fillid = 0;
							if(args.length > 1) {

								try {
									durability = Short.parseShort(args[1]);
								}
								catch (NumberFormatException e) {
									errormsg = "Wrong durability. Syntax : /fill [id] [durability]";
								}
							}
							
							try {
								fillid = Integer.parseInt(args[0]);
							}
							catch (NumberFormatException e) {
								String itemstring = args[0].toUpperCase();
								try {
									fillid = Material.getMaterial(itemstring).getId();
								}
								catch (NullPointerException n) {
									errormsg = "The item '"+itemstring+"' does not exist.";
								}
							}
							if(errormsg.length() > 0) {
								player.sendMessage(errormsg);
							}
							else {

								//ContainerBlock dispenser = (ContainerBlock) block;
								Dispenser dispenser = (Dispenser) block.getState();
								Inventory disp_inventory = dispenser.getInventory();
								disp_inventory.clear();
								if (fillid != 0) {
									for(int k=0; k<9; k++) {
										disp_inventory.addItem(new ItemStack(Material.getMaterial(fillid), Material.getMaterial(fillid).getMaxStackSize(), durability));
									}
								}
								dispenser.update();
							}
							return true;
						}
						else {
							player.sendMessage("The block is not a dispenser. You must face a dispenser.");
							return true;
						}
					}
					return false;
				}
				else if(command.equalsIgnoreCase("refill")) {

					Block block = player.getTargetBlock(null, 100);
					if(block.getTypeId() == 23) {

						int fillid = 0;
						short durability = 0;
						Dispenser dispenser = (Dispenser) block.getState();
						Inventory disp_inventory = dispenser.getInventory();
						for(int k=0; k<9; k++) {
							try {
								fillid = disp_inventory.getContents()[k].getTypeId();
								durability = disp_inventory.getContents()[k].getDurability();
								break;
							}
							catch (NullPointerException e){
								
							}
						}
						if(fillid == 0) {
							player.sendMessage("The dispenser is empty.");
							return true;
						}
						disp_inventory.clear();

						for(int k=0; k<9; k = k+1) {
							disp_inventory.addItem(new ItemStack(Material.getMaterial(fillid), Material.getMaterial(fillid).getMaxStackSize(), durability));
						}
						dispenser.update();
						return true;
						
					}
					else {
						player.sendMessage("The block is not a dispenser. You must face a dispenser.");
						return true;
					}
				}

			}
			else{
				player.sendMessage("You do not have permission to do this");
				return true;
			}

		}
		else {
			log.info("[DispenserReFill] This is a player only command.");
			return true;
		}



		return false;
	}


}
