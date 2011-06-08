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

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;


public class DispenserReFill extends JavaPlugin {
	private final Logger log = Logger.getLogger("Minecraft");
	public static PermissionHandler Permissions = null;

	boolean canUseCommand = true;
	boolean canUseAuto = true;
	boolean canByPassInventory = true;
	boolean logRefresh = true;
	String permissions_config = null;

	public void onEnable() {


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

				out.write("use-permissions: OP   #can be OP, permissions or false");
				out.newLine();
				out.write("refresh-frequency: 60      #in seconds");
				out.newLine();
				out.write("log-refresh: true");

				//Close the output stream
				out.close();
			}
			catch (Exception e) {
				log.warning("[DispenserReFill] Cannot write config file: "+e);
			}
		}
		
		logRefresh = getConfiguration().getBoolean("log-refresh", true);
		permissions_config = getConfiguration().getString("use-permissions", "OP").trim();

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

		int period = 0;
		try {
			period = getConfiguration().getInt("refresh-frequency", 60);
			log.info("[DispenserReFill] Refresh period set to " + period+" seconds.");
		}
		catch (Exception e) {
			log.warning("[DispenserReFill] Wrong value for refill-frequency. Defaulting to 600 seconds");
			period = 600;
		}

		if( getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			public void run() {
				refill_auto();
			}}, 200, period * 20) == -1)
			log.warning("[DispenserReFill] Impossible to schedule the re-filling task. Auto-refill will not work");

		setup_permissions();
		PluginDescriptionFile pdfFile = this.getDescription();

		log.info("[DispenserReFill] "+ pdfFile.getVersion() + " by nitnelave is enabled");


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




			if (permissions_config.equalsIgnoreCase("permissions") || permissions_config.equalsIgnoreCase("OP")) {
				if (permissions_config.equalsIgnoreCase("permissions")) {
					if (Permissions != null) {
						canUseCommand = Permissions.has(player, "DispenserReFill.fill");
						canByPassInventory = Permissions.has(player, "DispenserReFill.bypassinventory");
						canUseAuto = Permissions.has(player, "DispenserReFill.auto");
					}
				}
				else {
					canUseCommand = player.isOp();
					canByPassInventory = player.isOp();
					canUseAuto = player.isOp();
				}

			}
			else if(!permissions_config.equalsIgnoreCase("false")){
				log.warning("[DispenserReFill] : Bad option for use-permissions : "+permissions_config);
				return true;
			}
			if(canUseCommand) {
				if(command.equalsIgnoreCase("dfill")) {
					if(args.length > 0) {

						Block block = player.getTargetBlock(null, 1000);
						if(block.getTypeId() == 23 || block.getTypeId() == 54) {
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
								fill(block, fillid, durability, player);
							}
							return true;
						}
						else if(block.getTypeId() == 0) {
							player.sendMessage("No block in sight (or too far?)");
							return true;
						}
						else {
							player.sendMessage(block.getType().toString()+": The block is not a chest/dispenser. You must face a chest/dispenser.");
							return true;
						}
					}
					return false;
				}
				else if(command.equalsIgnoreCase("drefill")) {

					Block block = player.getTargetBlock(null, 1000);
					refill(block, player);
					return true;
				}
				else if(command.equalsIgnoreCase("dautofill")) {

					if(args.length > 0) {

						Block block = player.getTargetBlock(null, 1000);
						if(block.getTypeId() == 23 || (block.getTypeId() == 54 && canByPassInventory)) {
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
								refill_auto_create(canByPassInventory, player.getTargetBlock(null, 100), fillid, durability, false);
							}
						}
						else if(block.getTypeId() == 0) {
							player.sendMessage("No block in sight (or too far?)");
							return true;
						}
						else {
							player.sendMessage(block.getType().toString()+": The block is not a dispenser. You must face a dispenser.");
						}
						return true;
					}
					return false;

				}
				else if(command.equalsIgnoreCase("dautorefill")) {
					Block block = player.getTargetBlock(null, 1000);
					if(block == null)
						log.warning("null block");
					if(block.getTypeId() == 23 || (block.getTypeId() == 54 && canByPassInventory)) {
						refill_auto_create(canByPassInventory, block);
					}
					else {
						player.sendMessage(block.getType().toString()+": The block is not a chest/dispenser. You must face a chest/dispenser.");
					}
					
					return true;
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

	private int[] refill(Block block, Player player) {
		int[] return_values = new int[2];
		int fillid = 0;
		short durability = 0;
		if(block.getTypeId() == 23 || block.getTypeId() == 54) {
			ContainerBlock container = (ContainerBlock) block.getState();
			Inventory disp_inventory = container.getInventory();
			for(int k=0; k<disp_inventory.getSize(); k++) {
				try {
					fillid = disp_inventory.getContents()[k].getTypeId();
					durability = disp_inventory.getContents()[k].getDurability();
					break;
				}
				catch (NullPointerException e){

				}
			}
			if(container instanceof Chest && fillid == 0){
				ContainerBlock chest = (ContainerBlock) (scanForNeighborChest(block).getState());
				if(chest != null){
					Inventory chest_inventory = chest.getInventory();
					for(int k=0; k<chest_inventory.getSize(); k++) {
						try {
							fillid = chest_inventory.getContents()[k].getTypeId();
							durability = chest_inventory.getContents()[k].getDurability();
							break;
						}
						catch (NullPointerException e){

						}
					}
				}
			}
			if(fillid == 0) {
				if(player!=null) 
					player.sendMessage("The chest/dispenser is empty.");
				else {
					log.warning("The chest/dispenser at "+block.getX()+","+block.getY()+","+block.getZ()+" in world '"+block.getWorld().getName()+"' is empty.");
				}
				return_values[0]=fillid;
				return_values[1]=(int)durability;
				return return_values;
			}
			fill(block, fillid, durability, player);
			return_values[0]=fillid;
			return_values[1]=(int)durability;
			return return_values;
			}
		else if(block.getTypeId() == 0) {
			player.sendMessage("No block in sight (or too far?)");
			return return_values;
		}
		else {
			if(player!=null)
				player.sendMessage(block.getType().toString()+": The block is not a chest/dispenser. You must face a chest/dispenser.");
			else
				log.warning("The block at"+block.getX()+","+block.getY()+","+block.getZ()+" in "+block.getWorld().getName()+" is not a chest/dispenser.");
			return_values[0]=fillid;
			return_values[1]=(int)durability;
			return return_values;
		}
	}

	private void fill(Block block, int fillid, short durability, Player player){

		ContainerBlock container = (ContainerBlock) block.getState();
		fill_inventory(container, fillid, durability, player, block.getWorld(), block.getLocation());

		if(container instanceof Chest){
			Block tmp = scanForNeighborChest(block);
			ContainerBlock chest = null;
			if(tmp!=null)
				chest = (ContainerBlock) (tmp.getState());
			if(chest != null){
				fill_inventory(chest, fillid, durability, player, block.getWorld(), block.getLocation());

			}
		}

	}

	private void fill_inventory (ContainerBlock container, int fillid, short durability, Player player, World world, Location location){
		Inventory disp_inventory = container.getInventory();
		if(canByPassInventory) {
			disp_inventory.clear();
			if (fillid != 0) {
				for(int k=0; k<container.getInventory().getSize(); k++) {
					disp_inventory.addItem(new ItemStack(Material.getMaterial(fillid), Material.getMaterial(fillid).getMaxStackSize(), durability));
				}
			}
		}
		else {
			ContainerBlock[] chests = new ContainerBlock[12];
			if(container instanceof Dispenser)
				chests = scanNeighborsExtended(world, location);
			Inventory play_inventory = null;
			if(player!=null) {
				play_inventory = player.getInventory();
			}

			Inventory container_inv = container.getInventory();
			int play_amount_init = 0;
			int disp_amount_init = 0;
			int chest_amount_init = 0;
			//Chests to storage
			for(ContainerBlock chest : chests) {

				if(chest != null) {				
					int k = 0;
					for(ItemStack itemstack : chest.getInventory().getContents()){
						k++;

						if(itemstack != null) {
							//log.info(Integer.toString(k));
							if(itemstack.getTypeId() == fillid && itemstack.getDurability() == durability) {
								//log.info("ouais!");
								chest_amount_init += itemstack.getAmount();
								chest.getInventory().remove(itemstack);
							}
						}
					}
				}
				else 
					break;


			}
			//Player to storage
			if(play_inventory!=null) {

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
			for(ItemStack itemstack : container_inv.getContents()) {
				if(itemstack != null) {
					if(itemstack.getTypeId() == fillid && itemstack.getDurability() == durability) {
						disp_amount_init += itemstack.getAmount();
					}
					else {
						boolean added = false;
						for(ContainerBlock chest : chests) {
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
							world.dropItemNaturally(location, itemstack);
					}
					container_inv.removeItem(itemstack);
				}
			}
			container.getInventory().clear();
			((BlockState) container).update();
			container_inv = container.getInventory();

			int total_amount = play_amount_init + disp_amount_init + chest_amount_init;

			ReturnVariables var = stuff_inventory(container_inv, fillid, durability, total_amount);
			int amount_left = var.getAmount();
			disp_inventory = var.getInventory();
			((BlockState) container).update();

			if(play_inventory != null) {

				var = stuff_inventory(play_inventory, fillid, durability, Math.min(amount_left, play_amount_init));
				amount_left = amount_left - Math.min(amount_left, play_amount_init) + var.getAmount();
				play_inventory = var.getInventory();
			}

			for(ContainerBlock chest : chests) {
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
			/*
			//Dispenser in storage to dispenser
			ReturnVariables var = stuff_inventory(container_inv, fillid, durability, disp_amount_init);
			container_inv = var.getInventory();
			int amount_left = var.getAmount();
			//Excedent in chest
			for(ContainerBlock chest : chests) {
				if(chest != null) {

					if (amount_left < 0)
						break;
					Inventory chest_inv = chest.getInventory();
					var = stuff_inventory(chest_inv, fillid, durability, amount_left);
					chest_inv = var.getInventory();
					amount_left = var.getAmount();
					((BlockState) chest).update();
				}
				else
					break;

			}
			//Excedent dropped
			while(amount_left > 0){
				int stack_size = Math.min(Material.getMaterial(fillid).getMaxStackSize(), amount_left);
				world.dropItemNaturally(location, new ItemStack(fillid, stack_size, durability));
				amount_left -= stack_size;
			}


			//Chest in storage to Dispenser
			var = stuff_inventory(container_inv, fillid, durability, chest_amount_init);
			container_inv = var.getInventory();
			amount_left = var.getAmount();

			//Excedent in Chests

			for(ContainerBlock chest : chests) {
				if (amount_left < 0)
					break;
				Inventory chest_inv = chest.getInventory();
				var = stuff_inventory(chest_inv, fillid, durability, amount_left);
				chest_inv = var.getInventory();
				amount_left = var.getAmount();
				((BlockState) chest).update();

			}
			//Excedent dropped
			while(amount_left > 0){
				int stack_size = Math.min(Material.getMaterial(fillid).getMaxStackSize(), amount_left);
				world.dropItemNaturally(location, new ItemStack(fillid, stack_size, durability));
				amount_left -= stack_size;
			}

			//Player to Dispenser
			var = stuff_inventory(container_inv, fillid, durability, play_amount_init);
			container_inv = var.getInventory();
			amount_left = var.getAmount();

			//Excedent back to Player

			var = stuff_inventory(play_inventory, fillid, durability, amount_left);
			play_inventory = var.getInventory();
			amount_left = var.getAmount();

			//Excedent dropped
			while(amount_left > 0){
				int stack_size = Math.min(Material.getMaterial(fillid).getMaxStackSize(), amount_left);
				world.dropItemNaturally(location, new ItemStack(fillid, stack_size, durability));
				amount_left -= stack_size;
			}


			for(int k = 0; k<container_inv.getSize(); k++) {
				int stack_size = Math.min(Material.getMaterial(fillid).getMaxStackSize(), total_amount);
				if ( stack_size != 0)
					container_inv.addItem(new ItemStack(fillid, stack_size, durability));
				total_amount -= stack_size;
				if(total_amount == 0)
					break;
			}
			while (total_amount > 0 && player.getInventory().firstEmpty() != -1) {
				int stack_size = Math.min(Material.getMaterial(fillid).getMaxStackSize(), total_amount);
				if ( stack_size != 0)
					player.getInventory().addItem(new ItemStack(fillid, stack_size, durability));
				total_amount -= stack_size;
			}
			while(total_amount > 0){
				int stack_size = Math.min(Material.getMaterial(fillid).getMaxStackSize(), total_amount);
				world.dropItemNaturally(location, new ItemStack(fillid, stack_size, durability));
				total_amount -= stack_size;

			}*/
		}
		//((BlockState) container).update();
	}

	public static Block scanForNeighborChest(World world, int x, int y, int z)
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

	private ContainerBlock[] scanNeighborsExtended(World world, Location location) {
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
		return cont_chest;
	}

	private ReturnVariables stuff_inventory(Inventory inventory, int fillid, short durability, int amount) {
		if (amount != 0) {

			for(int k = 0; k<inventory.getSize(); k++) {
				int stack_size = Math.min(Material.getMaterial(fillid).getMaxStackSize(), amount);
				inventory.addItem(new ItemStack(fillid, stack_size, durability));
				amount -= stack_size;
				/*int max_stack_size = Material.getMaterial(fillid).getMaxStackSize();
				int stack_size = Math.min(max_stack_size, amount);
				if ( stack_size > 0) {
					if(inventory.getItem(k)!=null){
						ItemStack itmstck = inventory.getItem(k);
						if(itmstck.getAmount() < max_stack_size){
							int diff = max_stack_size - itmstck.getAmount();
							itmstck.setAmount(Math.min(itmstck.getAmount() + amount, max_stack_size));
							amount -= diff;
						}
					}
					else {
						inventory.addItem(new ItemStack(fillid, stack_size, durability));
						amount -= stack_size;
					}

				}*/
				if(amount <= 0)
					break;
			}
		}

		ReturnVariables var = new ReturnVariables();
		var.setAmount(amount);
		var.setInventory(inventory);
		return var;
	}

	public void refill_auto_create(boolean spawn_items, Block block) {
		refill_auto_create(spawn_items, block, 0, (short) 0, true);
	}
	public void refill_auto_create(boolean spawn_items, Block block, int fillid, short durability, boolean refill) {
		File refill_file = new File(getDataFolder()+"/refill.yml");
		try{
			Scanner scan = new Scanner(refill_file);
			if (!scan.hasNext(block.getX()+";"+block.getY()+";"+block.getZ()+";"+block.getWorld().getName())) {
				try {
					BufferedWriter out = new BufferedWriter(new FileWriter(refill_file, true));
					int[] result = new int[2];
					String str = block.getX()+";"+block.getY()+";"+block.getZ()+";"+block.getWorld().getName()+";"+spawn_items+";"+fillid+";"+durability+";"+refill;
					if((result = refill(block, null))[0] != 0)
						str = (block.getX()+";"+block.getY()+";"+block.getZ()+";"+block.getWorld().getName()+";"+spawn_items+";"+result[0]+";"+result[1]+";"+refill);
					
					out.write(str);
					out.newLine();

					//Close the output stream
					out.close();
				}
				catch (IOException e) {
					log.warning("[DispenserReFill] Cannot write refill file: "+e);
				}

			}
				
		}
		catch(Exception e) {
			log.warning("[DispenserReFill] Could not scan refill file: "+e);
		}


		
	}

	public void refill_auto() {
		File refill_file = new File(getDataFolder()+"/refill.yml");

		try {
			BufferedReader in = new BufferedReader(new FileReader(refill_file));
			String str;
			ArrayList<String> file_lines = new ArrayList<String>();
			while((str = in.readLine())!= null) {
				String[] args = str.split(";");
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
							if((result = refill(world.getBlockAt(x, y, z), null))[0] == 0){
								fill(world.getBlockAt(x, y, z), fillid, durability, null);
							}
							else {
								str = (x+";"+y+";"+z+";"+world.getName()+";"+canByPassInventory+";"+result[0]+";"+result[1]+";true");
							}
						}
						else {
							fill(world.getBlockAt(x, y, z), fillid, durability, null);
						}
						file_lines.add(str);
					}
				}
				if(!refill_file.delete()) {
					log.warning("[DispenserReFill] Could not rewrite file refill.yml");
				}
				try {
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
