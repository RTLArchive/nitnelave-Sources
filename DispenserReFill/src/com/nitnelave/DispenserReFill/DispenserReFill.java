package com.nitnelave.DispenserReFill;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Dispenser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import info.somethingodd.bukkit.OddItem.OddItem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.logging.Logger;


public class DispenserReFill extends JavaPlugin {
	private final Logger log = Logger.getLogger("Minecraft");
	public static PermissionHandler Permissions = null;
	public static LWC lwc = null;
	public static OddItem OI = null;
	HashSet<Byte> transparent_blocks = null;

	boolean canUseCommand = true; //can use /dfill and /drefill, necessary for auto commands
	boolean canUseAuto = true; //can use /dautofill and /dautorefill, requires canUseCommand
	boolean canByPassInventory = true; //spawn items
	boolean logRefresh = true; //display Refreshed finished in the console
	boolean logEmpty = true; //warns when an auto-refill chest is empty
	String permissions_config = null; //config value of permissions: op, permissions of false

	public void onEnable() {

		//create folder if it does not exist
		if (!new File(getDataFolder().toString()).exists() ) {
			new File(getDataFolder().toString()).mkdir();
		}

		File yml = new File(getDataFolder()+"/config.yml");
		File refill = new File(getDataFolder()+"/refill.yml");

		//create the config
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

				out.write("use-permissions: OP   #can be OP, permissions, bukkitPerms or false");
				out.newLine();
				out.write("refresh-frequency: 60      #in seconds");
				out.newLine();
				out.write("log-refresh: true");
				out.newLine();
				out.write("log-empty-auto-refill: true #warns when an auto-refill chest is empty");
				out.newLine();

				//Close the output stream
				out.close();
			}
			catch (Exception e) {
				log.warning("[DispenserReFill] Cannot write config file: "+e.getLocalizedMessage());
			}
		}
		//read config
		logRefresh = getConfiguration().getBoolean("log-refresh", true);
		permissions_config = getConfiguration().getString("use-permissions", "OP").trim();
		logEmpty = getConfiguration().getBoolean("log-empty-auto-refill", true);


		//create the list of containers to refill
		if(!refill.exists()) {
			new File(getDataFolder().toString()).mkdir();
			try {
				refill.createNewFile();
			}
			catch (IOException ex) {
				log.warning("[DispenserReFill] Cannot create file "+refill.getPath());
			}
		}
		//Initialize plugins
		Plugin lwcPlugin = getServer().getPluginManager().getPlugin("LWC");
		if(lwcPlugin != null) {
			lwc = ((LWCPlugin) lwcPlugin).getLWC();
			log.info("[DispenserReFill] Connected with LWC");
		}

		OI = (OddItem) getServer().getPluginManager().getPlugin("OddItem");
		if(OI != null) {
			log.info("[DispenserReFill] Connected with OddItem");
		}


		int period = 0;
		try {
			period = getConfiguration().getInt("refresh-frequency", 60);
			log.info("[DispenserReFill] Refresh period set to " + period+" seconds.");
		}
		catch (Exception e) {
			log.warning("[DispenserReFill] Wrong value for refill-frequency. Defaulting to 600 seconds");
			period = 600;
		}
		//start repeating refresh task
		if( getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			public void run() {
				refill_auto();
			}}, 200, period * 20) == -1)
			log.warning("[DispenserReFill] Impossible to schedule the re-filling task. Auto-refill will not work");

		setup_permissions();
		PluginDescriptionFile pdfFile = this.getDescription();

		Byte[] elements = {0, 6, 8, 9, 10, 11, 18, 20, 26, 27, 28, 30, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59,
				63, 65, 66, 68, 69, 70, 72, 75, 76, 77, 78, 83, 93, 94, 96};  //Block IDs considered transparent

		transparent_blocks = new HashSet<Byte>(Arrays.asList(elements));

		log.info("[DispenserReFill] Version "+ pdfFile.getVersion() + " by nitnelave is enabled");


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

		if(sender instanceof Player) {
			Player player = (Player) sender;
			//get the permissions for the player
			if (!permissions_config.equalsIgnoreCase("false")) {
				if (permissions_config.equalsIgnoreCase("permissions")) {
					if (Permissions != null) {
						canUseCommand = Permissions.has(player, "DispenserReFill.fill");
						canByPassInventory = Permissions.has(player, "DispenserReFill.bypassinventory");
						canUseAuto = Permissions.has(player, "DispenserReFill.auto");
					}
					else {
						log.info("[DispenserReFill] Could not access Permissions.");
						canUseCommand = canByPassInventory = canUseAuto = false;
					}
				}
				else if(permissions_config.equalsIgnoreCase("OP"))   //permissions config == OP
					canUseCommand = canByPassInventory = canUseAuto = player.isOp();
				else if(permissions_config.equalsIgnoreCase("bukkitPerms")) {
					canUseCommand = player.hasPermission("DispenserReFill.fill");
					canByPassInventory = player.hasPermission("DispenserReFill.bypassinventory");
					canUseAuto = player.hasPermission("DispenserReFill.auto");
				}


			}
			else if(permissions_config.equalsIgnoreCase("false")){
				canUseCommand = canByPassInventory = canUseAuto = true;
			}
			else {
				log.warning("[DispenserReFill] : Bad option for use-permissions : "+permissions_config);
				return true;
			}

			if(canUseCommand) { //if he can use the commands
				if(command.equalsIgnoreCase("dfill")) {
					if(args.length > 0) {

						Block block = player.getTargetBlock(transparent_blocks, 100); //get the block the player is facing, ignoring transparent block (wire...)
						boolean protect = false;
						//get the block protection
						if(lwc != null) {
							Protection protection = lwc.findProtection(block);
							if(protection != null) {
								protect = !lwc.canAccessProtection(player, block);
								// the protection was found in the database
							}
						}
						if((block.getTypeId() == 23 || block.getTypeId() == 54) && !protect) { //if it is a chest and not protected
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
								if(OI !=null) { //get the OddItem name
									try {
										fillid = OI.getItemStack(args[0]).getTypeId();
									}
									catch (IllegalArgumentException ex) {
										errormsg = "Did you mean : "+ex.getMessage()+" ?";
									}
								}
								else { //get the ENUM name

									String itemstring = args[0].toUpperCase();
									try {
										fillid = Material.getMaterial(itemstring).getId();
									}
									catch (NullPointerException n) {
										errormsg = "The item '"+itemstring+"' does not exist.";
									}
								}
							}
							if(errormsg.length() > 0) {
								player.sendMessage(errormsg);
							}
							else {
								fill(block, fillid, durability, player); //fill the chest/dispenser
							}
							return true;
						}
						else if(block.getTypeId() == 0) {
							player.sendMessage("No block in sight (or too far?)");
							return true;
						}
						else if(protect) {
							player.sendMessage("The block is protected by another player, you cannot access it");
						}
						else {
							player.sendMessage("The block is not a chest/dispenser. You must face a chest/dispenser.");
							return true;
						}
					}
					return false; //syntax error
				}
				else if(command.equalsIgnoreCase("drefill")) { //refill command, mostly same stuff

					Block block = player.getTargetBlock(transparent_blocks, 100);
					boolean protect = false;
					if(lwc != null) {
						Protection protection = lwc.findProtection(block);

						if(protection != null) {
							protect = !lwc.canAccessProtection(player, block);
							// the protection was found in the database
						}
					}
					if(!protect) {
						refill(block, player); //refill the chest/dispenser
					}
					else
						player.sendMessage("The block is protected by another player, you cannot access it");
					return true;
				}
				else if(command.equalsIgnoreCase("dautofill")) {

					if(args.length > 0) {


						Block block = player.getTargetBlock(transparent_blocks, 100);
						boolean protect = false;
						if(lwc != null) {
							Protection protection = lwc.findProtection(block);

							if(protection != null) {
								protect = !lwc.canAccessProtection(player, block);
								// the protection was found in the database
							}
						}
						if((block.getTypeId() == 23 || (block.getTypeId() == 54 && canByPassInventory)) && !protect) { //only users that can spawn items can set auto-refill to a chest
							short durability = 0;
							int fillid = 0;
							if(args.length > 1) {

								try {
									durability = Short.parseShort(args[1]);
								}
								catch (NumberFormatException e) {
									errormsg = "Wrong durability. Syntax : /dautofill [id] [durability]";
								}
							}

							try {
								fillid = Integer.parseInt(args[0]);
							}
							catch (NumberFormatException e) {
								if(OI !=null) {
									try {
										fillid = OI.getItemStack(args[0]).getTypeId();
									}
									catch (IllegalArgumentException ex) {
										errormsg = "Did you mean : "+ex.getMessage()+" ?";
									}
								}
								else {

									String itemstring = args[0].toUpperCase();
									try {
										fillid = Material.getMaterial(itemstring).getId();
									}
									catch (NullPointerException n) {
										errormsg = "The item '"+itemstring+"' does not exist.";
									}
								}
							}
							if(errormsg.length() > 0) {
								player.sendMessage(errormsg);
							}
							else {
								refill_auto_create(canByPassInventory, player.getTargetBlock(null, 100), fillid, durability, false); //create the entry in the file
							}
						}
						else if(block.getTypeId() == 0) {
							player.sendMessage("No block in sight (or too far?)");
							return true;
						}
						else if(protect)
							player.sendMessage("The block is protected by another player, you cannot access it");
						else {
							player.sendMessage("The block is not a dispenser. You must face a dispenser.");
						}
						return true;
					}
					return false; //syntax error

				}
				else if(command.equalsIgnoreCase("dautorefill")) {
					Block block = player.getTargetBlock(transparent_blocks, 100);
					boolean protect = false;
					if(lwc != null) {
						Protection protection = lwc.findProtection(block);

						if(protection != null) {
							protect = !lwc.canAccessProtection(player, block);
							// the protection was found in the database
						}
					}
					if((block.getTypeId() == 23 || (block.getTypeId() == 54 && canByPassInventory)) && !protect) {
						refill_auto_create(canByPassInventory, block);
					}
					else if(block.getTypeId() == 0)
						player.sendMessage("No block in sight (or too far?)");
					else if(protect)
						player.sendMessage("The block is protected by another player, you cannot access it");
					else {
						player.sendMessage("The block is not a dispenser. You must face a dispenser.");
					}

					return true;
				}
			}
			else{
				player.sendMessage("You do not have permission to do this");
				return true;
			}

		}
		else { //if typed from console
			log.info("[DispenserReFill] This is a player only command.");
			return true;
		}

		return false;
	}

	private int[] refill(Block block, Player player) { //refill the container from the chests around, and then from the player's inventory
		int[] return_values = new int[2];
		int fillid = 0;
		short durability = 0;
		if(block.getTypeId() == 23 || block.getTypeId() == 54) {
			ContainerBlock container = (ContainerBlock) block.getState();
			Inventory disp_inventory = container.getInventory();


			for(ItemStack itemstack : disp_inventory.getContents()) { //get the first item in the container
				if(itemstack!=null) {
					fillid = itemstack.getTypeId();
					durability = itemstack.getDurability();
					break;
				}
			}


			if(container instanceof Chest && fillid == 0){ //if it's a chest, but there was no item inside, look for a double chest
				Block chest_block = (scanForNeighborChest(block));
				if(chest_block != null){
					ContainerBlock chest = (ContainerBlock) (chest_block.getState());
					for(ItemStack itemstack : chest.getInventory().getContents()) { //get the first item in the container
						if(itemstack!=null) {
							fillid = itemstack.getTypeId();
							durability = itemstack.getDurability();
							break;
						}
					}
				}
			}
			if(fillid == 0) { //if no item were found
				if(player!=null)
					player.sendMessage("The chest/dispenser is empty.");
				else { //empty on refresh
					if(logEmpty)
						log.warning("The chest/dispenser at "+block.getX()+","+block.getY()+","+block.getZ()+" in world '"+block.getWorld().getName()+"' is empty.");
				}
				return_values[0]=0;
				return_values[1]=0;
				return return_values; //return 0, the values that were there before (last item inside) are kept
			}
			fill(block, fillid, durability, player); //fill the container with the item detected
			return_values[0]=fillid;
			return_values[1]=(int)durability;
			return return_values; //return the values of the item detected
		}
		else if(block.getTypeId() == 0) {
			player.sendMessage("No block in sight (or too far?)");
			return return_values;
		}
		else {
			player.sendMessage(block.getType().toString()+": The block is not a chest/dispenser. You must face a chest/dispenser."); //can only be player, for the refresh, the block is checked before
			return_values[0]=fillid;
			return_values[1]=(int)durability;
			return return_values;
		}
	}

	private void fill(Block block, int fillid, short durability, Player player){ //fill a container with first the chest around (in case of a dispenser) then the player's inventory
		//in case of an OP, just spawns the items

		ContainerBlock container = (ContainerBlock) block.getState();
		fill_inventory(container, fillid, durability, player, block.getWorld(), block.getLocation()); //fills the inventory

		if(container instanceof Chest){
			Block tmp = scanForNeighborChest(block);
			ContainerBlock chest = null;
			if(tmp!=null)
				chest = (ContainerBlock) (tmp.getState());
			if(chest != null){ //in case of a double chest, fills the second one
				fill_inventory(chest, fillid, durability, player, block.getWorld(), block.getLocation());

			}
		}

	}

	private void fill_inventory (ContainerBlock container, int fillid, short durability, Player player, World world, Location location){ //Actual filling algorithm
		Inventory disp_inventory = container.getInventory();
		if(canByPassInventory) { //spawn the items
			disp_inventory.clear();
			if (fillid != 0) {
				int stack_size = Material.getMaterial(fillid).getMaxStackSize();
				if (fillid == 357)
					stack_size = 8;
				for(int k=0; k<container.getInventory().getSize(); k++) {
					disp_inventory.addItem(new ItemStack(Material.getMaterial(fillid), stack_size, durability));
				}
			}
		}
		else { //fetch the items from around
			ContainerBlock[] chests = new ContainerBlock[12];
			if(container instanceof Dispenser)
				chests = scanNeighborsExtended(world, location); //get the nearby chests in case of a dispenser
			if(fillid != 0){


				Inventory play_inventory = null;
				if(player!=null) {
					play_inventory = player.getInventory();
				}

				Inventory container_inv = container.getInventory();
				int play_amount_init = 0; //amount of the item initially in the player's inventory
				int disp_amount_init = 0;  //amount of the item initially in the target's inventory
				int chest_amount_init = 0;  //amount of the item initially in the nearby chests' inventory
				//Chests to storage
				for(ContainerBlock chest : chests) { //empty the chests of the item, store it in chest_amount_init

					if(chest != null) {               
						int k = 0;
						for(ItemStack itemstack : chest.getInventory().getContents()){
							k++;

							if(itemstack != null) {
								if(itemstack.getTypeId() == fillid && itemstack.getDurability() == durability) {
									chest_amount_init += itemstack.getAmount();
									chest.getInventory().remove(itemstack);
								}
							}
						}
					}
					else //null chest, end of list
						break;


				}
				//Player to storage
				if(play_inventory!=null) { //empty the player's inventory of the item, store it

					for(ItemStack itemstack : play_inventory.getContents()) {
						if(itemstack != null) {
							if(itemstack.getTypeId() == fillid && itemstack.getDurability() == durability) {
								play_amount_init += itemstack.getAmount();
								play_inventory.remove(itemstack);
							}
						}
					}
				}
				//Dispenser to storage or chest or drop
				for(ItemStack itemstack : container_inv.getContents()) { //empty the target
					if(itemstack != null) {
						if(itemstack.getTypeId() == fillid && itemstack.getDurability() == durability) { //store the item
							disp_amount_init += itemstack.getAmount();
						}
						else {
							boolean added = false;
							for(ContainerBlock chest : chests) { //store in nearby chest, if any
								if(chest != null) {
									if (chest.getInventory().firstEmpty() > -1) {
										chest.getInventory().addItem(itemstack);
										added = true;
										break;
									}
								}
								else
									break;

							}
							if(!added)
								world.dropItemNaturally(location, itemstack); //couldn't fit it anywhere, drop it
						}
					}
				}
				container.getInventory().clear();
				((BlockState) container).update();

				int total_amount = play_amount_init + disp_amount_init + chest_amount_init; //total stock

				ReturnVariables var = stuff_inventory(container_inv, fillid, durability, total_amount); //fill the target
				int amount_left = var.getAmount();
				disp_inventory = var.getInventory();
				((BlockState) container).update();

				if(play_inventory != null) { //if there is a player, give him back his stuff (because we take from the chests first)

					var = stuff_inventory(play_inventory, fillid, durability, Math.min(amount_left, play_amount_init));
					amount_left = amount_left - Math.min(amount_left, play_amount_init) + var.getAmount();
					play_inventory = var.getInventory();
				}

				for(ContainerBlock chest : chests) { //put whatever's left in the chests
					if(chest != null && amount_left > 0) {
						Inventory inv = chest.getInventory();
						var = stuff_inventory(inv, fillid, durability, amount_left);
						amount_left = var.getAmount();
						inv = var.getInventory();
						((BlockState) chest).update();
					}
					else
						break;
				}
				while( amount_left>0) { //if there are still items left, drop them
					int max_size = Math.min(amount_left, Material.getMaterial(fillid).getMaxStackSize());
					if(fillid == 357)
						max_size = Math.min(amount_left, 8);
					world.dropItemNaturally(location, new ItemStack(fillid, max_size, durability));
				}

			}
			else {
				for(ItemStack itemstack : container.getInventory().getContents()) {
					world.dropItemNaturally(location, itemstack);
				}
				container.getInventory().clear();
				((BlockState)container).update();

			}

		}
	}

	public static Block scanForNeighborChest(World world, int x, int y, int z) //given a chest, scan for double, return the Chest
	{
		if ((world.getBlockAt(x - 1, y, z)).getType().equals(Material.CHEST)) {
			return world.getBlockAt(x - 1, y, z);
		}
		if ((world.getBlockAt(x + 1, y, z)).getType().equals(Material.CHEST)) {
			return world.getBlockAt(x + 1, y, z);
		}
		if ((world.getBlockAt(x, y, z - 1)).getType().equals(Material.CHEST)) {
			return world.getBlockAt(x, y, z - 1);
		}
		if ((world.getBlockAt(x, y, z + 1)).getType().equals(Material.CHEST)) {
			return world.getBlockAt(x, y, z + 1);
		}
		return null;
	}

	public static Block scanForNeighborChest(Block block)
	{
		return scanForNeighborChest(block.getWorld(), block.getX(), block.getY(), block.getZ());
	}

	private ContainerBlock[] scanNeighborsExtended(World world, Location location) {   //scan for adjacent chest in any direction
		Block[] chest =  new Block[12];
		int list_length = 0;
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();
		if (world.getBlockAt(x - 1, y, z).getType().equals(Material.CHEST)) {
			chest[list_length] = world.getBlockAt(x - 1, y, z);
			list_length++;
			Block tmp = scanForNeighborChest(world, x - 1, y, z);
			if(tmp!=null){
				chest[list_length] = world.getBlockAt(tmp.getLocation());
				list_length++;
			}
		}
		if (world.getBlockAt(x + 1, y, z).getType().equals(Material.CHEST)) {
			chest[list_length] = world.getBlockAt(x + 1, y, z);
			list_length++;
			Block tmp = scanForNeighborChest(world, x + 1, y, z);
			if(tmp!=null){
				chest[list_length] = world.getBlockAt(tmp.getLocation());
				list_length++;
			}
		}
		if (world.getBlockAt(x, y + 1, z).getType().equals(Material.CHEST)) {
			chest[list_length] = world.getBlockAt(x, y + 1, z);
			list_length++;
			Block tmp = scanForNeighborChest(world, x, y + 1, z);
			if(tmp!=null){
				chest[list_length] = world.getBlockAt(tmp.getLocation());
				list_length++;
			}
		}
		if (world.getBlockAt(x, y - 1, z).getType().equals(Material.CHEST)) {
			chest[list_length] = world.getBlockAt(x, y - 1, z);
			list_length++;
			Block tmp = scanForNeighborChest(world, x, y - 1, z);
			if(tmp!=null){
				chest[list_length] = world.getBlockAt(tmp.getLocation());
				list_length++;
			}
		}
		if (world.getBlockAt(x, y, z - 1).getType().equals(Material.CHEST)) {
			chest[list_length] = world.getBlockAt(x, y, z - 1);
			list_length++;
			Block tmp = scanForNeighborChest(world, x, y, z - 1);
			if(tmp!=null){
				chest[list_length] = world.getBlockAt(tmp.getLocation());
				list_length++;
			}
		}
		if (world.getBlockAt(x, y, z + 1).getType().equals(Material.CHEST)) {
			chest[list_length] = world.getBlockAt(x, y, z + 1);
			list_length++;
			Block tmp = scanForNeighborChest(world, x, y, z + 1);
			if(tmp!=null){
				chest[list_length] = world.getBlockAt(tmp.getLocation());
				list_length++;
			}
		}
		ContainerBlock[] cont_chest = new ContainerBlock[12];
		for(int k = 0; chest[k]!=null; k++)
			cont_chest[k] = (ContainerBlock) chest[k].getState();
		return cont_chest; //return a ContainerBlock array
	}

	private ReturnVariables stuff_inventory(Inventory inventory, int fillid, short durability, int amount) { //fill the given inventory with <amount> of the item
		if (amount != 0) {

			for(int k = 0; k<inventory.getSize(); k++) {
				int stack_size = Math.min(Material.getMaterial(fillid).getMaxStackSize(), amount);
				if(fillid == 357)
					stack_size = Math.min(amount, 8);
				inventory.addItem(new ItemStack(fillid, stack_size, durability));
				amount -= stack_size;
				if(amount <= 0)
					break;
			}
		}

		ReturnVariables var = new ReturnVariables();
		var.setAmount(amount);
		var.setInventory(inventory);
		return var; //return the amount left and the final state of the inventory
	}

	public void refill_auto_create(boolean spawn_items, Block block) {
		refill_auto_create(spawn_items, block, 0, (short) 0, true);
	}

	public void refill_auto_create(boolean spawn_items, Block block, int fillid, short durability, boolean refill) {  //store a auto refill in the file
		File refill_file = new File(getDataFolder()+"/refill.yml");
		try{
			Scanner scan = new Scanner(refill_file);
			if (!scan.hasNext(block.getX()+";"+block.getY()+";"+block.getZ()+";"+block.getWorld().getName())) { //check for duplicate, abort
				try {
					BufferedWriter out = new BufferedWriter(new FileWriter(refill_file, true));
					int[] result = new int[2];
					String str = block.getX()+";"+block.getY()+";"+block.getZ()+";"+block.getWorld().getName()+";"+spawn_items+";"+fillid+";"+durability+";"+refill;
					if(refill) { //if it is a refill, and not a fill
						if((result = refill(block, null))[0] != 0) //refills the block. If not empty, update the last item
							str = (block.getX()+";"+block.getY()+";"+block.getZ()+";"+block.getWorld().getName()+";"+spawn_items+";"+result[0]+";"+result[1]+";"+refill);
					}

					out.write(str);
					out.newLine();

					//Close the output stream
					out.close();
				}
				catch (IOException e) {
					log.warning("[DispenserReFill] Cannot write refill file: "+e.getStackTrace());
				}

			}

		}
		catch(Exception e) {
			log.warning("[DispenserReFill] Could not scan refill file: "+e.getStackTrace());
		}



	}

	public void refill_auto() { //refill task
		File refill_file = new File(getDataFolder()+"/refill.yml");

		try {
			BufferedReader in = new BufferedReader(new FileReader(refill_file));
			boolean rewrite = false;
			String str;
			ArrayList<String> file_lines = new ArrayList<String>();
			while((str = in.readLine())!= null) {
				String[] args = str.split(";"); //get the arguments
				int x = Integer.parseInt(args[0]);
				int y = Integer.parseInt(args[1]);
				int z = Integer.parseInt(args[2]);
				World world = getServer().getWorld(args[3]);
				canByPassInventory = Boolean.parseBoolean(args[4]);
				int fillid = Integer.parseInt(args[5]);
				short durability = Short.parseShort(args[6]);
				boolean refill = Boolean.parseBoolean(args[7]);
				Block block = world.getBlockAt(x, y, z);
				if(block != null) {
					if(block.getTypeId() == 23 || block.getTypeId() == 54) {
						if(refill) {
							int[] result = new int[2];
							if((result = refill(world.getBlockAt(x, y, z), null))[0] == 0){ //if container is empty
								fill(world.getBlockAt(x, y, z), fillid, durability, null); //fill with last item
							}
							else { //update last item
								str = x+";"+y+";"+z+";"+world.getName()+";"+canByPassInventory+";"+result[0]+";"+result[1]+";true";
							}
						}
						else { //fill task (not refill)
							fill(world.getBlockAt(x, y, z), fillid, durability, null);
						}
						file_lines.add(str);
					}
					else
						rewrite = true;
				}
				else
					rewrite = true;



			}

			if(rewrite) {
				if (!refill_file.delete()) { //delete file to update
					log.warning("[DispenserReFill] Could not rewrite file refill.yml");
				}
				try { //rewrite the file
					BufferedWriter out = new BufferedWriter(new FileWriter(refill_file));
					for(String tmp_str : file_lines) {
						out.write(tmp_str);
						out.newLine();
					}
					out.close();
				}
				catch (IOException e) {
					log.warning("[DispenserReFill] Could not write to file refill.yml: "+e);
				}
			}

		}
		catch (IOException e) {
			log.warning("[DispenserReFill] Cannot read refill file refill.yml: "+e);
		}

		if(logRefresh)
			log.info("[DispenserReFill] Refresh finished");
	}


}
