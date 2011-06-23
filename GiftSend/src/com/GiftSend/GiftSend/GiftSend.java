package com.GiftSend.GiftSend;

//java stuff
import info.somethingodd.bukkit.OddItem.OddItem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
//bukkit stuff

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.ItemStack;
//import org.bukkit.material.Wool;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
//permissions stuff
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

public class GiftSend extends JavaPlugin{
	private static Logger log = Logger.getLogger("Minecraft");
	public static PermissionHandler Permissions = null;
	private final SGPlayerListener playerListener = new SGPlayerListener(this);
	private int maxradius = 0;
	private String allowoffline = null;
	public static OddItem OI = null;
	private static GiftSend plugin;
	private static File dataFolder;


	public void onDisable() {
		log.info("[GiftSend] Disabled");
	}

	public void onEnable() {
		dataFolder = getDataFolder();
		if (!new File(dataFolder.toString()).exists() ) {
			new File(dataFolder.toString()).mkdir();
		}
		plugin = this;

		File yml = new File(dataFolder+"/config.yml");
		File offlinesends = new File(dataFolder+"/offline.txt");

		if (!yml.exists()) {
			new File(dataFolder.toString()).mkdir();
			try {
				yml.createNewFile();
			}
			catch (IOException ex) {
				System.out.println("cannot create file "+yml.getPath());
			}

			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(yml, true));

				out.write("max-range: 100");
				out.newLine();
				out.write("allow-offline: 'true'");
				out.newLine();
				out.write("use-permissions: 'permissions'  #permissions, OP or false");
				out.newLine();

				//Close the output stream
				out.close();
			}
			catch (Exception e) {
				System.out.println("cannot write config file: "+e);
			}
		}

		if (!offlinesends.exists()) {
			try {
				offlinesends.createNewFile();
			} catch (IOException e) {
				System.out.println("cannot create file "+offlinesends.getPath()+"/"+offlinesends.getName());
			}
		}

		maxradius = getConfiguration().getInt("max-range", 0);
		allowoffline = getConfiguration().getString("allow-offline", "false");

		PluginManager pm = getServer().getPluginManager();

		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);

		//Get the information from the plugin.yml file.
		PluginDescriptionFile pdfFile = this.getDescription();

		setupPermissions();

		OI = (OddItem) getServer().getPluginManager().getPlugin("OddItem");
		if(OI != null) {
			log.info("[GiftSend] Successfully connected with OddItem");
		}
		//Print that the plugin has been enabled!
		log.info("[GiftSend] version " + pdfFile.getVersion() + " by nitnelave is enabled!");
	}

	public void setupPermissions() {
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if(Permissions == null) {
			if(test != null) {
				Permissions = ((Permissions)test).getHandler();
				log.info("[GiftSend] Successfully connected with Permissions.");
			}
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		boolean canUseCommand = true;

		HashMap<Integer, ? extends ItemStack> itemsarray = new HashMap<Integer, ItemStack>();

		if (sender instanceof Player) {
			Player player = (Player)sender;
			
			String permissions_config = null;
			permissions_config = getConfiguration().getString("use-permissions", "OP").trim();


			if (permissions_config.equalsIgnoreCase("permissions") || permissions_config.equalsIgnoreCase("OP")) {
				if (permissions_config.equalsIgnoreCase("permissions")) {
					if (Permissions != null) {
						canUseCommand = Permissions.has(player, "GiftSend.send");
					}
				}
				else {
					canUseCommand = player.isOp();
				}
				
			}
			

			if (canUseCommand) {

				if (args.length > 1) {
					String playername = args[0];
					String itemamount = args[1];
					String tmpdurability = null;
					String itemstring = null;
					if(args.length>2)
						itemstring = args[2];

					if (args.length > 3) 
						tmpdurability = args[3];
					

					String errormsg = "";
					int giveamount = 0;
					try {
						giveamount = Integer.parseInt(itemamount);
					}
					catch (NumberFormatException e) {
						return false;
					}
					int givetypeid = 0;
					
					short durability = 0;
					
					if (tmpdurability != null){
						try {
							durability = Short.parseShort(tmpdurability);
						}
						catch (NumberFormatException e) {
							return false;
						}
					}
					//checks to see if the item works
					if(itemstring == null){
						if(player.getItemInHand().getTypeId() != 0){
							itemstring = Integer.toString(player.getItemInHand().getTypeId());
						}
						else {
							player.sendMessage("Hold an item in your hand, or use this syntax :");
							return false;
						}
					}
					try {
						givetypeid = Integer.parseInt(itemstring);
					}
					catch (NumberFormatException e) {
						if(OI !=null) { //get the OddItem name
							try {
								givetypeid = OI.getItemStack(itemstring).getTypeId();
							}
							catch (IllegalArgumentException ex) {
								errormsg = "Did you mean : "+ex.getMessage()+" ?";
							}
						}
						else { //get the ENUM name

							try {
								givetypeid = Material.getMaterial(itemstring.toUpperCase()).getId();
							}
							catch (NullPointerException n) {
								errormsg = "The item '"+itemstring.toUpperCase()+"' does not exist.";
							}
						}
					}

					//allows offline transfers
					Player testplayer = getServer().getPlayer(playername);
					String materialname;

					//Checks to see if you have enough
					itemsarray = player.getInventory().all(Material.getMaterial(givetypeid));
					int playerHasInInventory = 0;

					for (Entry<Integer, ? extends ItemStack> entry : itemsarray.entrySet()) {
						ItemStack value = entry.getValue();
						


							if (value.getDurability() == durability && value.getAmount() > 0){

								playerHasInInventory = playerHasInInventory + value.getAmount(); 
							}
						
					}

					//Checks to see if players are close enough
					if (testplayer != null) {
						if (maxradius > 0) {
							int totaldistance =0;

							int x1 = player.getLocation().getBlockX();
							int y1 = player.getLocation().getBlockY();
							int z1 = player.getLocation().getBlockZ();
							int x2 = testplayer.getLocation().getBlockX();
							int y2 = testplayer.getLocation().getBlockY();
							int z2 = testplayer.getLocation().getBlockZ();

							totaldistance = ((x1 - x2)^2 + (y1 - y2)^2 +(z1 - z2)^2);
							if (totaldistance >= (maxradius^2)) {
								errormsg = "That player too far away.";
							}
						}
					}
					else {
						if (allowoffline.matches("false")) {
							errormsg = "That player is not online.";
						}
					}


					//outputs error
					if (giveamount > playerHasInInventory) {
						errormsg = "You do not have that item with that amount.";
					}
					if (errormsg.length() > 0) {
						player.sendMessage(ChatColor.GRAY+errormsg);
					}

					//start the transfer
					else {
						short tmp_durability = durability;
						int tmp_amount = giveamount;
						for (Entry<Integer, ? extends ItemStack> entry : itemsarray.entrySet()) {
							ItemStack value = entry.getValue();
							if (value.getDurability() == tmp_durability) {
								if (value.getAmount() <= tmp_amount) {
									tmp_amount = tmp_amount - value.getAmount();
									player.getInventory().removeItem(value);
								}
								else if (value.getAmount() > tmp_amount){
									player.getInventory().removeItem(value);
									player.getInventory().addItem(new ItemStack(givetypeid, (value.getAmount() - tmp_amount), durability));
									tmp_amount = 0;
								}
							}
							
						}

						//player is not online, store in offline.txt
						if (testplayer == null || !testplayer.isOnline()) {	
							writeOffline(player, playername, givetypeid, durability, giveamount, false);
							

						}

						//both online, do in real time
						else {
							//make sure that the receiving player's inventory isn't full
							if (testplayer.getInventory().firstEmpty() >= 0) {

								//remove the item
								int tmpamount = giveamount;
								int stack_size = Material.getMaterial(givetypeid).getMaxStackSize();
								if(givetypeid == 357) 
									stack_size = 8;
								while(tmpamount > 0) {
									if(testplayer.getInventory().firstEmpty() == -1)
										break;
									testplayer.getInventory().addItem(new ItemStack(givetypeid, Math.min(tmpamount, stack_size), durability));
									tmpamount -= Math.min(tmpamount, stack_size);
								}
								
								materialname = Material.getMaterial(givetypeid).toString().toLowerCase().replace("_", " ");
								if (giveamount > 1) {
									if (materialname.endsWith("s") || materialname.endsWith("z"))
										materialname = materialname+"es";
									else
										materialname = materialname+"s";
								}

								player.sendMessage(ChatColor.GRAY+"You gave "+ChatColor.GREEN+testplayer.getName()+" "+ChatColor.GRAY+itemamount+" "+ ChatColor.RED+materialname);
								testplayer.sendMessage(ChatColor.GREEN+player.getName()+ChatColor.GRAY+" gave you "+itemamount+" "+ChatColor.RED+materialname);
								if(tmpamount > 0) {
									writeOffline(player, testplayer.getName(), givetypeid, durability, tmpamount, false);
									player.sendMessage(ChatColor.GREEN+testplayer.getName()+"'s "+ChatColor.GRAY+" inventory is full. Only part of the items were sent.");
									testplayer.sendMessage(ChatColor.GREEN+player.getName()+ChatColor.GRAY+" tried to send you something, but you have no space left. Try to reconnect with some space.");
								}
							}
							else {
								player.sendMessage(ChatColor.GREEN+testplayer.getName()+"'s "+ChatColor.GRAY+" inventory is full. Try again later.");
								testplayer.sendMessage(ChatColor.GREEN+player.getName()+ChatColor.GRAY+" tried to send you something, but you have no space.");
							}
						}
					}
				}
				else {
					return false;
				}
			}
			else {
				sender.sendMessage("You don't have Permissions to do that");
			}
		}
		else {
			sender.sendMessage("This is a player only command.");
		}
		return true;
	}
	static void writeOffline(Player sender, String recipient, int givetypeid, short durability, int giveamount, boolean listener) {
		File offlineFile = new File(dataFolder+"/offline.txt");
		//Write the send to file
		try {

			BufferedWriter out = new BufferedWriter(new FileWriter(offlineFile, true));

			String textToWrite = recipient+":"+givetypeid+":"+giveamount+":"+sender.getName()+":"+durability;

			out.write(textToWrite);
			out.newLine();

			//Close the output stream
			out.close();


			String materialname = Material.getMaterial(givetypeid).toString().toLowerCase().replace("_", " ");
			if (giveamount > 1) {
				if (materialname.endsWith("s") || materialname.endsWith("z"))
					materialname = materialname+"es";
				else
					materialname = materialname+"s";
			}


			if(!listener){
				sender.sendMessage(ChatColor.GRAY+"You gave "+ChatColor.GREEN+recipient+" "+ChatColor.GRAY+giveamount+" "+ ChatColor.RED+materialname);
				sender.sendMessage(ChatColor.GRAY+"They will receive it when they log in.");
			}


		}
		catch (Exception e) {
			log.info("[GiftSend] Offline transfer to "+recipient+" failed: " + e);
		}
	}
	public static GiftSend getPlugin() {
		return plugin;
	}
}