package com.nitnelave.CreeperHeal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
/*import org.bukkit.material.Attachable;
import org.bukkit.material.SimpleAttachableMaterialData;*/
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.util.config.Configuration;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import com.garbagemule.MobArena.MobArenaHandler;



public class CreeperHeal extends JavaPlugin {
	private final Logger log = Logger.getLogger("Minecraft");            //to output messages to the console/log
	Map<Date, List<BlockState>> map = Collections.synchronizedMap(new HashMap<Date, List<BlockState>>());        //hashmap storing the list of blocks destroyed in an explosion
	Map<Date, BlockState> map_burn = Collections.synchronizedMap(new HashMap<Date, BlockState>());                //same for burnt blocks
	int interval = 60;                                                    //interval defined in the config, with the default value in milisec
	private CreeperListener listener = new CreeperListener(this);                        //listener for explosions
	private FireListener fire_listener = new FireListener(this);                        //listener for fire
	private TNTBreakListener block_listener = new TNTBreakListener(this);
	private EnderListener ender_listener = new EnderListener(this);
	private int log_level = 1;                                                            //level of message output of the config, with default value
	HashMap<Location, ItemStack[]> chest_contents = new HashMap<Location, ItemStack[]>();         //stores the chests contents
	HashMap<Location, String[]> sign_text = new HashMap<Location, String[]>();                    //stores the signs text
	HashMap<Location, Byte> note_block = new HashMap<Location, Byte>();
	HashMap<Location, String> mob_spawner = new HashMap<Location, String>();
	private ArrayList<Integer> blocks_physics = new ArrayList<Integer>(Arrays.asList(12,13,88));                        //sand gravel, soulsand fall
	private ArrayList<Integer> blocks_last = new ArrayList<Integer>(Arrays.asList(6,18,26,27,28,31,32,37,38,39,40,50,55,59,63,64,65,66,68,69,70,71,72,75,76,77,81,83,93,94,96));  //blocks dependent on others. to put in last
	private ArrayList<Integer> blocks_non_solid = new ArrayList<Integer>(Arrays.asList(0,6,8,9,26,27,28,30,31,37,38,39,40, 50,55,59,63,64,65,66,68,69,70,71,72,75,76,77,78,83,90,93,94,96));   //the player can breathe
	//private ArrayList<Integer> attachable_blocks = new ArrayList<Integer>(Arrays.asList(50, 68, 69, 75, 76, 77));
	private String[] world_config_nodes = {"Creepers", "TNT", "Ghast", "Magical", "Fire", "restrict-blocks", "restrict-list", "replace-tnt", "replace-above-only", "replace-limit", "block-enderman"};
	boolean drop_blocks_replaced = true;        //drop items when blocks are overwritten
	public static PermissionHandler Permissions = null;    //permission stuff
	int period = 20;        //frenquency to check for blocks to replace, in sec.
	int block_interval = 20;        //frequency for replacing blocks, in the case of block_per_block
	boolean block_per_block = true;    //as in the config
	boolean teleport_on_suffocate = true;    //teleport player in stuck in an explosion getting replaced
	int burn_interval = 45;            //interval after which burnt blocks are replaced. default value
	MobArenaHandler maHandler = null;
	boolean drop_not_replaced = true;
	int drop_chance = 100;

	Map<String, WorldConfig> world_config = Collections.synchronizedMap(new HashMap<String, WorldConfig>());
	Map<Block, Byte> torch_data = Collections.synchronizedMap(new HashMap<Block, Byte>());
	Map<Entity, DateLoc> tnt_location = Collections.synchronizedMap(new HashMap<Entity, DateLoc>());
	boolean called_set_torch = false;
	Map<String, String> trap_location = Collections.synchronizedMap(new HashMap<String, String>());
	Map<Date, Block> trapToAdd = Collections.synchronizedMap(new HashMap<Date, Block>());

	private static HashSet<Byte> transparent_blocks = null;

	private boolean opEnforce = true;
	private boolean cracked = false;


	/**
	 * Stores a list of dropped blocks for blocks.
	 */
	private static final Map<Integer,Integer> blockDrops = new HashMap<Integer,Integer>();

	/**
	 * Static constructor.
	 */
	static {
		blockDrops.put(1, 4);
		blockDrops.put(2, 3);
		blockDrops.put(3, 3);
		blockDrops.put(4, 4);
		blockDrops.put(5, 5);
		blockDrops.put(6, 6);
		blockDrops.put(12, 12);
		blockDrops.put(13, 13);
		blockDrops.put(14, 14);
		blockDrops.put(15, 15);
		blockDrops.put(16, 263);
		blockDrops.put(17, 17);
		blockDrops.put(19, 19);
		blockDrops.put(21, 351);
		blockDrops.put(22, 22);
		blockDrops.put(23, 23);
		blockDrops.put(24, 24);
		blockDrops.put(25, 25);
		blockDrops.put(26, 355);
		blockDrops.put(27, 27);
		blockDrops.put(28, 28);
		blockDrops.put(29, 29);
		blockDrops.put(30, 30);
		blockDrops.put(31, 295);
		blockDrops.put(35, 35);
		blockDrops.put(37, 37);
		blockDrops.put(38, 38);
		blockDrops.put(39, 39);
		blockDrops.put(40, 40);
		blockDrops.put(41, 41);
		blockDrops.put(42, 42);
		blockDrops.put(43, 44);
		blockDrops.put(44, 44);
		blockDrops.put(45, 45);
		blockDrops.put(47, 47);
		blockDrops.put(48, 48);
		blockDrops.put(49, 49);
		blockDrops.put(50, 50);
		blockDrops.put(53, 53);
		blockDrops.put(54, 54);
		blockDrops.put(55, 331);
		blockDrops.put(56, 264);
		blockDrops.put(57, 57);
		blockDrops.put(58, 58);
		blockDrops.put(59, 295);
		blockDrops.put(60, 3);
		blockDrops.put(61, 61);
		blockDrops.put(62, 61);
		blockDrops.put(63, 323);
		blockDrops.put(64, 324);
		blockDrops.put(65, 65);
		blockDrops.put(66, 66);
		blockDrops.put(67, 67);
		blockDrops.put(68, 323);
		blockDrops.put(69, 69);
		blockDrops.put(70, 70);
		blockDrops.put(71, 330);
		blockDrops.put(72, 72);
		blockDrops.put(73, 331);
		blockDrops.put(74, 331);
		blockDrops.put(75, 76);
		blockDrops.put(76, 76);
		blockDrops.put(77, 77);
		blockDrops.put(78, 332);
		blockDrops.put(80, 80);
		blockDrops.put(81, 81);
		blockDrops.put(82, 82);
		blockDrops.put(83, 338);
		blockDrops.put(84, 84);
		blockDrops.put(85, 85);
		blockDrops.put(86, 86);
		blockDrops.put(87, 87);
		blockDrops.put(88, 88);
		blockDrops.put(89, 348);
		blockDrops.put(91, 91);
		blockDrops.put(93, 356);
		blockDrops.put(94, 356);
		blockDrops.put(95, 95);
		blockDrops.put(96, 96);

		Byte[] elements = {0, 6, 8, 9, 10, 11, 18, 20, 26, 27, 28, 30, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 65, 66, 68, 69, 70, 72, 75, 76, 77, 78, 83, 93, 94, 96};
		transparent_blocks = new HashSet<Byte>(Arrays.asList(elements));
	}

	public void onEnable() {

		if (!new File(getDataFolder().toString()).exists() ) {
			new File(getDataFolder().toString()).mkdir();
		}

		File yml = new File(getDataFolder()+"/config.yml");

		if (!yml.exists()) {
			log.warning("[CreeperHeal] Config file not found, creating default.");
			config_write();        //write the config with the default values.
		}

		File trapFile = new File(getDataFolder() + "/trap.yml");

		if(!trapFile.exists()) {
			try {
				trapFile.createNewFile();
			}
			catch (IOException ex) {
				log.warning("[CreeperHeal] Cannot create file "+trapFile.getPath());
			}
		}

		loadTraps(trapFile);


		PluginManager pm = getServer().getPluginManager();        //registering the listeners

		PluginDescriptionFile pdfFile = this.getDescription();
		setup_permissions();

		Plugin mobArena = pm.getPlugin("MobArena");
		if(mobArena != null) {
			maHandler = new MobArenaHandler();
		}


		period = configInt("refresh-frequency", 20); //reading refresh-frequency and block-interval from config. Has to be in onEnable(), because we declare a bukkitscheduler task after that.

		block_interval = configInt("block-per-block-interval", 20);

		loadConfig();        //read the rest of the config.
		new File(getDataFolder()+"/config.yml").delete();        //delete, then rewrite the config with the new settings.

		config_write();         //regenerates the config, allowing for some update.

		pm.registerEvent(Event.Type.BLOCK_BREAK, block_listener, Event.Priority.Normal, this);

		pm.registerEvent(Event.Type.ENTITY_EXPLODE, listener, Event.Priority.High, this);

		pm.registerEvent(Event.Type.EXPLOSION_PRIME, listener, Event.Priority.High, this);
		int tmp_period = period * 20;        //register the task to go every "period" second if all at once
		if(block_per_block)                    //or every "block_interval" ticks if block_per_block
			tmp_period = block_interval;
		if( getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				check_replace(block_per_block);        //check to replace explosions/blocks
			}}, 200, tmp_period) == -1)
			log.warning("[CreeperHeal] Impossible to schedule the re-filling task. Auto-refill will not work");

		pm.registerEvent(Event.Type.BLOCK_BURN, fire_listener, Event.Priority.Monitor, this);

		pm.registerEvent(Event.Type.ENDERMAN_PICKUP, ender_listener, Event.Priority.High, this);

		if( getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				replace_burnt();
			}}, 200, block_interval) == -1)
			log.warning("[CreeperHeal] Impossible to schedule the replace-burnt task. Burnt blocks replacement will not work");




		log.info("[CreeperHeal] version "+pdfFile.getVersion()+" by nitnelave is enabled");
	}


	public void onDisable() {
		for(WorldConfig w : world_config.values()) {
			force_replace(0, w);        //replace blocks still in memory, so they are not lost
			force_replace_burnt(0, w);    //same for burnt_blocks
		}
		log.info("[CreeperHeal] Disabled");
		saveTraps();
	}




	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {



		if(args.length != 0) {        //if it's just /ch, display help
			WorldConfig current_world =     loadWorldConfig(args[args.length - 1]);   

			if(current_world == null) {

				if(sender instanceof Player){
					current_world = loadWorldConfig( ((Player)sender).getWorld());
				}
				else {
					current_world = loadWorldConfig(getServer().getWorlds().get(0));
					sender.sendMessage("No world specified, defaulting to " + current_world.getName());
				}
			}

			String cmd = args[0];
			if(cmd.equalsIgnoreCase("creeper"))
				current_world.creepers = booleanCmd(current_world.creepers, args, "Creepers explosions", sender);
			else if(cmd.equalsIgnoreCase("TNT"))        //same as above
				current_world.tnt = booleanCmd(current_world.tnt, args, "TNT explosions", sender);
			else if(cmd.equalsIgnoreCase("fire"))
				current_world.fire = booleanCmd(current_world.fire, args, "Burnt blocks", sender);
			else if(cmd.equalsIgnoreCase("ghast"))
				current_world.ghast = booleanCmd(current_world.ghast, args, "Ghast fireballs explosions", sender);
			else if(cmd.equalsIgnoreCase("magical"))
				current_world.magical = booleanCmd(current_world.magical, args, "Magical explosions", sender);
			else if(cmd.equalsIgnoreCase("interval"))
				interval = integerCmd(interval, args, "block destroyed in an explosion", sender);
			else if(cmd.equalsIgnoreCase("burnInterval"))
				burn_interval = integerCmd(burn_interval, args, "burnt block", sender);
			else if(cmd.equalsIgnoreCase("forceHeal") || cmd.equalsIgnoreCase("heal"))
				forceCmd(args, "explosions", sender, current_world);
			else if(cmd.equalsIgnoreCase("healBurnt"))
				forceCmd(args, "burnt blocks", sender, current_world);
			else if(cmd.equalsIgnoreCase("trap")) {
				if(args.length == 2 && sender instanceof Player) {
					if(args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("make"))
						createTrap((Player)sender);
					else if(args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("delete"))
						deleteTrap((Player)sender);
					else
						sender.sendMessage("/ch trap (create|remove)");
				}
				else if(args.length != 2)
					sender.sendMessage("/ch trap (create|remove)");
				else if(!(sender instanceof Player))
					sender.sendMessage("Player only command");
			}
			else if(cmd.equalsIgnoreCase("reload"))
				loadConfig();
			else if(cmd.equalsIgnoreCase("help"))
				sendHelp(sender);
			else {        // /ch something gets back to the help
				sender.sendMessage("/ch help");
				return true;
			}

			new File(getDataFolder()+"/config.yml").delete();        //delete, then rewrite the config with the new settings.

			config_write();
		}
		else {
			sender.sendMessage("/ch help");
			return true;
		}

		return true;
	}



	private void sendHelp(CommandSender sender) {
		sender.sendMessage("CreeperHeal -- Repair explosions damage and make traps");
		sender.sendMessage("--------------------------------------------");
		String green = ChatColor.GREEN.toString();
		String purple = ChatColor.DARK_PURPLE.toString();
		boolean admin = true;
		boolean heal = true;
		boolean trap = true;

		if(sender instanceof Player){
			Player player = (Player) sender;
			admin = checkPermissions("admin", player);
			heal = checkPermissions("heal", player);
			trap = checkPermissions("trap.create", player) || checkPermissions("trap.*", player);
		}

		if(!(admin || heal || trap))
			sender.sendMessage(purple + "You do not have access to any of the CreeperHeal commands");

		if(admin){
			sender.sendMessage(green + "/ch reload :" + purple + " reloads the config from the file.");
			sender.sendMessage(green + "/ch creeper (on|off) (world) :" + purple + " toggles creeper explosion replacement");
			sender.sendMessage(green + "/ch TNT (on|off) (world) :" + purple + " same for TNT");
			sender.sendMessage(green + "/ch Ghast (on|off) (world) :" + purple + " same for Ghast fireballs");
			sender.sendMessage(green + "/ch magical (on|off) :" + purple + " same for \"magical\" explosions.");
			sender.sendMessage(green + "/ch fire (on|off) (world) :" + purple + " same for fire");
			sender.sendMessage(green + "/ch interval [seconds] :" + purple + " Sets the interval before an explosion is replaced to x seconds");
			sender.sendMessage(green + "/ch burnInterval [seconds] :" + purple + " Sets the interval before a block burnt is replaced to x seconds");
		}

		if(heal || admin){
			sender.sendMessage(green + "/ch heal (seconds) (world) :" + purple + " Heals all explosions in the last x seconds, or all if x is not specified.");
			sender.sendMessage(green + "/ch healBurnt (seconds) (world) :" + purple + " Heal all burnt blocks since x seconds, or all if x is not specified.");
		}

		if(trap){
			sender.sendMessage(green + "/ch trap (create|delete) :" + purple + " creates/removes a trap from the tnt block in front of you.");
		}


	}


	private boolean booleanCmd(boolean current, String[] args, String msg, CommandSender sender) {
		if(sender instanceof Player) {
			if(!checkPermissions("admin", (Player)sender)) {
				sender.sendMessage(ChatColor.RED + "You don’t have the permission");
				return current;
			}
		}
		boolean return_value = false;

		if(args.length == 1)
			return_value = !current;
		else if(args[1].equalsIgnoreCase("on"))
			return_value = true;
		else if(args[1].equalsIgnoreCase("off"))
			return_value = false;
		else {
			sender.sendMessage("/ch " + args[0] + " (on|off)");
			sender.sendMessage("Toggles " + msg + " replacement on/off");
			return current;
		}
		sender.sendMessage(ChatColor.GREEN + msg + " replacement set to : "+Boolean.toString(return_value));
		return return_value;

	}

	private int integerCmd(int current, String[] args, String msg, CommandSender sender) {
		if(sender instanceof Player) {
			if(!checkPermissions("admin", (Player) sender)) {
				sender.sendMessage(ChatColor.RED + "You don’t have the permission");
				return current;
			}
		}
		if(args.length == 2){
			int interval = 0;
			try {
				interval = Integer.parseInt(args[1]);
			}
			catch (Exception e) {
				sender.sendMessage("/ch " + args[0] + " [seconds]");
				sender.sendMessage("Sets the interval before replacing a " + msg);
				return current;
			}
			sender.sendMessage(ChatColor.GREEN+ "New interval set to : "+interval + "seconds");

			return interval;
		}
		else {
			sender.sendMessage("/ch " + args[0] + " [seconds]");
			sender.sendMessage("Sets the interval before replacing a " + msg);
			return current;
		}
	}

	public void forceCmd(String[] args, String msg, CommandSender sender, WorldConfig current_world) {
		String cmd = args[0];

		if(sender instanceof Player) {
			if(!checkPermissions("heal", (Player)sender) && !checkPermissions("admin", (Player)sender)) {
				sender.sendMessage(ChatColor.RED + "You don’t have the permission");
				return;
			}
		}   

		long since = 0;               
		if(args.length > 1){
			try{
				since = Long.parseLong(args[1]);
			}
			catch (Exception e) {
				sender.sendMessage("/ch " + cmd + " (seconds) (world_name | all)");
				sender.sendMessage("If a time is specified, heals all " + msg + " since x seconds ago. Otherwise, heals all.");
				return;
			}
		}
		boolean burnt = cmd.equalsIgnoreCase("healBurnt");
		if(args.length >2) {
			if(args[2].equalsIgnoreCase("all")) {
				for(WorldConfig w : world_config.values()) {
					if(burnt)
						force_replace_burnt(since, w);
					else
						force_replace(since, w);
				}
			}
			else {
				if(burnt)
					force_replace_burnt(since, current_world);
				else
					force_replace(since, current_world);
			}
		}
		else {
			if(burnt)
				force_replace_burnt(since, current_world);
			else
				force_replace(since, current_world);
		}

		sender.sendMessage(ChatColor.GREEN + "Explosions healed");
	}




	public void recordBlocks(EntityExplodeEvent event, WorldConfig world) {        //record the list of blocks of an explosion, from bottom to top
		Date now = new Date();
		event.setYield(0);
		List<Block> list = event.blockList();            //the list declared by the explosion
		List<BlockState> list_state = new ArrayList<BlockState>();        //the list of blockstate we'll be keeping afterward
		List<Location> list_loc = new ArrayList<Location>();            //to check for duplicates
		if(maHandler != null) {
			if (maHandler.inRegion(event.getLocation())) {
				log_info("Explosion ignored, inside a mob arena", 2);
				return;
			}

		}
		if(event.getEntity() instanceof TNTPrimed) {            //to replace the tnt that just exploded
			Entity entity = event.getEntity();
			Block block = tnt_location.get(entity).getLocation().getBlock();

			log_info("explosion at " + block.getX() + ";" + block.getY() + ";" + block.getZ(), 1);
			if(world.replace_tnt || isTrap(block)) {
				log_info("trap exploded", 1);

				trapToAdd.put(now, block);

				getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()

				{

					public void run() {

						addTrap();

					}});


			}
		}

		for(Block block : list){
			int type_id = block.getTypeId();
			byte data = block.getData();
			if((world.restrict_blocks.equalsIgnoreCase("whitelist") && world.block_list.contains(new BlockId(type_id, data))
					|| (world.restrict_blocks.equalsIgnoreCase("blacklist") && !world.block_list.contains(new BlockId(type_id, data))
							|| world.restrict_blocks.equalsIgnoreCase("false")))) {

				if(block.getState() instanceof ContainerBlock) {        //save the inventory
					chest_contents.put(block.getLocation(), ((ContainerBlock) block.getState()).getInventory().getContents().clone());
					((ContainerBlock) block.getState()).getInventory().clear();
				}
				else if(block.getState() instanceof Sign) {                //save the text
					sign_text.put(block.getLocation(), ((Sign)block.getState()).getLines());
				}
				else if(block.getState() instanceof NoteBlock) {
					note_block.put(block.getLocation(), ((NoteBlock)(block.getState())).getRawNote());
				}
				else if(block.getState() instanceof CreatureSpawner) {
					mob_spawner.put(block.getLocation(), ((CreatureSpawner)(block.getState())).getCreatureTypeId());
				}
				switch (block.getType()) {       
				case IRON_DOOR :                //in case of a door or bed, only store one block to avoid dupes
				case WOODEN_DOOR :
					if(block.getData() < 8) {
						list_state.add(block.getState());
						list_loc.add(block.getLocation());
						list_loc.add(block.getRelative(0, 1, 0).getLocation());        //but store both locations
						block.setTypeIdAndData(0, (byte)0, false);
					}
					break;
				case BED_BLOCK :
					if(data < 8) {
						list_state.add(block.getState());
						list_loc.add(block.getLocation());
						BlockFace face;
						if(data == 0)            //facing the right way
							face = BlockFace.WEST;
						else if(data == 1)
							face = BlockFace.NORTH;
						else if(data == 2)
							face = BlockFace.EAST;
						else
							face = BlockFace.SOUTH;
						list_loc.add(block.getRelative(face).getLocation());
						block.setTypeIdAndData(0, (byte)0, false);
					}
					break;
				case AIR :                        //don't store air
				case FIRE :                        //or fire
				case PISTON_EXTENSION :
					break;
				case TNT :
					if(isTrap(block) || loadWorldConfig(block.getWorld()).replace_tnt){
						list_state.add(block.getState());
						list_loc.add(block.getLocation());
					}
					break;
				case SMOOTH_BRICK :
				case BRICK_STAIRS :
					if(cracked  && block.getData() == (byte)0)
						block.setData((byte) 2);
				default :                        //store the rest
					list_state.add(block.getState());
					list_loc.add(block.getLocation());
					block.setTypeIdAndData(0, (byte)0, false);
					break;
				}
			}
			else if(drop_not_replaced){
				Random generator = new Random();
				if(generator.nextInt(100) < drop_chance)
					dropBlock(type_id, block.getData(), block.getLocation());

			}
		}

		for(Block block : list) {            //go over a second time to check for torches, wire, or anything that would drop and store them
			Block block_up = block.getRelative(BlockFace.UP);

			int type_id = block_up.getTypeId();
			byte data = block.getData();
			if(blocks_last.contains(type_id)) {
				if((world.restrict_blocks.equalsIgnoreCase("whitelist") && world.block_list.contains(new BlockId(type_id, data))
						|| (world.restrict_blocks.equalsIgnoreCase("blacklist") && !world.block_list.contains(new BlockId(type_id, data))
								|| world.restrict_blocks.equalsIgnoreCase("false")))) {
					if(block_up.getState() instanceof Sign) {                //save the text
						sign_text.put(block_up.getLocation(), ((Sign)block_up.getState()).getLines());
					}
					switch (block_up.getType()) {       
					case IRON_DOOR :                //in case of a door or bed, only store one block to avoid dupes
					case WOODEN_DOOR :
					case BED_BLOCK :
						if(block_up.getData() < 8) {
							list_state.add(block_up.getState());
							block_up.setTypeIdAndData(0, (byte)0, false);
						}
						break;
					default :                        //store the rest
						list_state.add(block_up.getState());
						block_up.setTypeIdAndData(0, (byte)0, false);
						break;
					}
				}
				else if(drop_not_replaced){
					Random generator = new Random();
					if(generator.nextInt(100) < drop_chance)
						dropBlock(type_id, block.getData(), block.getLocation());

				}
			}

		}




		BlockState[] tmp_array = list_state.toArray(new BlockState[list_state.size()]);        //sort through an array, then store back in the list
		Arrays.sort(tmp_array, new CreeperComparator());
		list_state.clear();
		for(BlockState block : tmp_array){
			list_state.add(block);
		}

		map.put(now, list_state);        //store in the global hashmap, with the time it happened as a key

		log_info("EXPLOSION!", 3);



	}


	protected void addTrap() {
		Iterator<Date> iter = trapToAdd.keySet().iterator();
		while(iter.hasNext()) {
			Date date = iter.next();
			Block block = trapToAdd.get(date);
			BlockState tmp_state = block.getState();

			block.setType(Material.TNT);                            //set the block to tnt

			map.get(date).add(block.getState());                //record it


			tmp_state.update(true);        //set it back to what it was

			iter.remove();
		}


	}


	public void check_replace(boolean block_per_block) {        //check to see if any block has to be replaced
		Date now = new Date();

		log_info("Replacing blocks...", 3);
		Date[] keyset = map.keySet().toArray(new Date[map.keySet().size()]);
		for(Date time : keyset) {
			if(new Date(time.getTime() + interval * 1000).before(now)) {        //if enough time went by
				if(!block_per_block){        //all blocks at once
					replace_blocks(map.get(time));        //replace the blocks
					map.remove(time);                    //remove the explosion from the record
					log_info("Blocks replaced!", 2);
				}
				else {            //block per block
					if(!map.get(time).isEmpty())        //still some blocks left to be replaced
						replace_one_block(map.get(time));        //replace one
					if(map.get(time).isEmpty())         //if empty, remove from list
						map.remove(time);
					log_info("blocks replaced!", 3);
				}

			}
		}   


	}

	private void replace_one_block(List<BlockState> list) {        //replace one block (block per block)

		Iterator<BlockState> iter = list.iterator();
		while(iter.hasNext()){        //finds the first block that is not dependent
			BlockState block = iter.next();
			if(!blocks_last.contains(block.getTypeId())){
				replace_blocks(block);        //replace it
				check_player_one_block(block.getBlock().getLocation());
				log_info(block.getType().toString(), 3);
				iter.remove();        //remove it
				return;

			}
		}
		replace_blocks(list.get(0));        //only dependent blocks left, replace the first
		check_player_one_block(list.get(0).getBlock().getLocation());
		log_info(list.get(0).getType().toString(), 3);
		list.remove(0);

	}

	public void check_player_one_block(Location loc) {
		if(teleport_on_suffocate) {
			Arrow entity = loc.getWorld().spawnArrow(loc, new Vector(0,0,0), 0, 0);
			List<Entity> play_list = Collections.synchronizedList(entity.getNearbyEntities(10, 10, 10));
			entity.remove();
			if(!play_list.isEmpty()) {
				for(Entity en : play_list) {
					if(en instanceof Player) {
						check_player_suffocate((Player)en);
					}
				}
			}
		}
	}
	public void check_player_suffocate(Player player) {
		log_info("checking player "+player.getName(),3);
		Location loc = player.getLocation();
		int x =loc.getBlockX();        //get the player's coordinates in ints, to have the block he's standing on
		int y =loc.getBlockY();
		int z =loc.getBlockZ();
		World w = player.getWorld();
		if(!blocks_non_solid.contains(loc.getBlock().getTypeId()) || !blocks_non_solid.contains(loc.getBlock().getRelative(0, 1, 0).getTypeId())) {
			log_info("player suffocating",2);
			for(int k =1; k + y < 127; k++) {        //all the way to the sky, checks if there's some room up or around

				if(check_free(w, x, y+k, z, player))
					break;

				if(check_free_horizontal(w, x+k, y, z, player))
					break;

				if(check_free_horizontal(w, x-k, y, z, player))
					break;

				if(check_free_horizontal(w, x, y, z+k, player))
					break;

				if(check_free_horizontal(w, x, y, z-k, player))
					break;

			}

		}

	}


	public void force_replace(long since, WorldConfig world) {        //force replacement of all the explosions since x seconds
		Date now = new Date();

		Iterator<Date> iterator = map.keySet().iterator();
		while(iterator.hasNext()) {
			Date time = iterator.next();
			if(new Date(time.getTime() + since).after(now) || since == 0) {        //if the explosion happened since x seconds
				if(map.get(time).get(0).getWorld().getName().equals( world.getName())) {

					replace_blocks(map.get(time));

					iterator.remove();

					log_info("Blocks replaced!", 2);

				}
			}
		}
		if(since == 0)
			force_replace_burnt(0L, world);
	}


	private void replace_blocks(List<BlockState> list) {    //replace all the blocks in the given list
		while(!list.isEmpty()){            //replace all non-physics non-dependent blocks
			Iterator<BlockState> iter = list.iterator();
			while (iter.hasNext()){
				BlockState block = iter.next();
				if(!blocks_physics.contains(block.getTypeId()) && !blocks_last.contains(block.getTypeId())){
					block_state_replace(block);
					iter.remove();
				}
			}
			iter = list.iterator();
			while (iter.hasNext()){        //then all physics
				BlockState block = iter.next();
				if(blocks_physics.contains(block.getTypeId())){
					block_state_replace(block);
					iter.remove();
				}
			}
			iter = list.iterator();
			while (iter.hasNext()){        //lastly all dependent
				BlockState block = iter.next();
				if(blocks_last.contains(block.getTypeId())){
					block_state_replace(block);
					iter.remove();
				}
			}
		}
		if(teleport_on_suffocate) {            //checks for players suffocating anywhere
			Player[] player_list = getServer().getOnlinePlayers();
			for(Player player : player_list) {
				check_player_suffocate(player);
			}
		}

	}



	private boolean check_free_horizontal(World w, int x, int y, int z, Player player) {        //checks one up and down, to broaden the scope
		for(int k = -1; k<2; k++){
			if(check_free(w, x, y+k, z, player))
				return true;  //found a spot
		}
		return false;
	}

	private boolean check_free(World w, int x, int y, int z, Player player) {
		Block block = w.getBlockAt(x, y, z);
		if(blocks_non_solid.contains(block.getTypeId()) && blocks_non_solid.contains(block.getRelative(0, 1, 0).getTypeId()) && !blocks_non_solid.contains(block.getRelative(0, -1, 0).getTypeId())) {
			Location loc = new Location(w, x+0.5, y, z+0.5);
			loc.setYaw(player.getLocation().getYaw());
			loc.setPitch(player.getLocation().getPitch());
			player.teleport(loc);            //if there's ground under and space to breathe, put the player there
			return true;
		}
		return false;
	}

	private void replace_blocks(BlockState block) {        //if there's just one block, no need to go over all this
		block_state_replace(block);
	}


	//replaces a single block, first sort
	public void block_state_replace(BlockState block){
		/*BlockFace dependentFace = getDependentBlock(block);
    if(dependentFace != BlockFace.SELF && blocks_non_solid.contains(block.getBlock().getRelative(dependentFace).getTypeId())){
        return false;
    }*/
		if (block.getType() == Material.WOODEN_DOOR || block.getType() == Material.IRON_DOOR_BLOCK) {        //if it's a door, put the bottom then the top (which is unrecorded)
			block_replace(block.getBlock(), block.getTypeId(), block.getRawData());
			block_replace(block.getBlock().getRelative(BlockFace.UP), block.getTypeId(), (byte)(block.getRawData() + 8));
		}
		else if(block.getType() == Material.BED_BLOCK) {        //put the head, then the feet
			byte data = block.getRawData();
			block.getBlock().setTypeIdAndData(block.getTypeId(), data, false);        //head
			BlockFace face;
			if(data == 0)            //facing the right way
				face = BlockFace.WEST;
			else if(data == 1)
				face = BlockFace.NORTH;
			else if(data == 2)
				face = BlockFace.EAST;
			else
				face = BlockFace.SOUTH;
			block_replace(block.getBlock().getRelative(face), block.getTypeId(), (byte)(data + 8));    //feet
		}
		else if(block.getType() == Material.PISTON_MOVING_PIECE) {            //extended piston, you have to put a base instead (and it comes out unextended)
			//log_info("Piston_moving_piece", 2);
			//block_replace(block.getBlock(), (block.getRawData() >7)?Material.PISTON_STICKY_BASE.getId():Material.PISTON_BASE.getId(), (byte)(block.getRawData() + 8));

		}
		else {        //rest of it, just normal
			block_replace(block.getBlock(), block.getTypeId(), block.getRawData());
		}

		log_info(block.getType().toString(), 3);
		//return true;

	}
	//actual replacement method
	public void block_replace(Block block, int type_id, byte rawData) {
		int block_id = block.getTypeId();        //id of the block in the world, before it is replaced

		if(drop_blocks_replaced && block_id != 0) {        //drop an item in the spot
			dropBlock(block_id, block.getData(), block.getLocation());
		}

		if(blocks_physics.contains(block_id)) {            //if the spot for the sand is occupied, put it in the first free spot above
			for(int k = 1; block.getY() + k < 128; k++) {
				if(block.getRelative(0,k,0) != null) {
					if(block.getRelative(0, k, 0).getTypeId() == 0) {
						block.getRelative(0, k, 0).setTypeIdAndData(block_id, (byte)0, false);
						break;
					}
				}
			}
		}
		else
			block.setTypeIdAndData(type_id, rawData, false);        //replace the block

		if(block.getState() instanceof ContainerBlock) {            //if it's a chest, put the inventory back
			((ContainerBlock) block.getState()).getInventory().setContents( chest_contents.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ())));
			block.getState().update();
			chest_contents.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
		}
		else if(block.getState() instanceof Sign) {                    //if it's a sign... no I'll let you guess
			log_info("replacing sign_text",2);
			int k = 0;

			for(String line : sign_text.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()))) {
				((Sign) block.getState()).setLine(k++, line);
			}
			sign_text.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));

		}
		else if(block.getState() instanceof NoteBlock) {
			((NoteBlock)block.getState()).setRawNote( note_block.get(block.getLocation()));
			block.getState().update();
			note_block.remove(block.getLocation());
		}
		else if(block.getState() instanceof CreatureSpawner) {
			((CreatureSpawner)block.getState()).setCreatureTypeId( mob_spawner.get(block.getLocation()));
			block.getState().update();
			mob_spawner.remove(block.getLocation());
		}
		else if(block.getType() == Material.TORCH || block.getType() == Material.REDSTONE_TORCH_ON || block.getType() ==  Material.REDSTONE_TORCH_OFF ) {
			torch_data.put(block, rawData);
			if(!called_set_torch) {

				getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()

				{

					public void run() {

						setTorchData();

					}

				}

				);

				called_set_torch = true;

			}

		}

	}

	private void setTorchData() {
		Iterator<Block> iter = torch_data.keySet().iterator();
		while(iter.hasNext()) {
			Block block = iter.next();
			block.setData(torch_data.get(block));
			iter.remove();
		}
		called_set_torch = false;
	}

	public void record_burn(Block block) {            //record a burnt block
		if(block.getType() != Material.TNT) {        //unless it's TNT triggered by fire
			Date now = new Date();
			map_burn.put(now, block.getState());
			BlockState block_up = block.getRelative(BlockFace.UP).getState();
			if(blocks_last.contains(block_up.getTypeId())) {        //the block above is a dependent block, store it, but one interval after
				map_burn.put(new Date(now.getTime() + burn_interval*1000), block_up);
				if(block_up instanceof Sign) {                //as a side note, chests don't burn, but signs are dependent
					sign_text.put(new Location(block_up.getWorld(), block_up.getX(), block_up.getY(), block_up.getZ()), ((Sign)block_up).getLines());
				}
				try{
					block_up.getBlock().setTypeIdAndData(0, (byte)0, false);
				}
				catch(IndexOutOfBoundsException e) {
					log_info(e.getLocalizedMessage(), 1);
				}
			}
		}
	}

	public void replace_burnt() {        //checks for burnt blocks to replace, with an override for onDisable()
		Date[] keyset = map_burn.keySet().toArray(new Date[map_burn.keySet().size()]);

		Date now = new Date();
		for(Date time : keyset) {
			if((new Date(time.getTime() + burn_interval * 1000).before(now))) {        //if enough time went by
				BlockState block = map_burn.get(time);
				if(blocks_last.contains(block.getTypeId())) {
					if(blocks_non_solid.contains(block.getBlock().getRelative(BlockFace.DOWN).getTypeId())) {        //if it's a dependent, and there's nothing under, store it for later
						map_burn.remove(time);
						map_burn.put(new Date(time.getTime() + burn_interval*1000), block);
					}
					else {
						replace_blocks(block);         //replace the block as there's something under
						map_burn.remove(time);
					}
				}
				else {
					replace_blocks(block);        //replace the non-dependent block
					map_burn.remove(time);
				}
			}
		}
	}

	public void force_replace_burnt(long since, WorldConfig world_config) {
		boolean force = false;
		if(since == 0)
			force = true;
		World world = getServer().getWorld(world_config.getName());

		Date[] keyset = map_burn.keySet().toArray(new Date[map_burn.keySet().size()]);

		Date now = new Date();
		for(Date time : keyset) {
			BlockState block = map_burn.get(time);
			if(block.getWorld() == world && (new Date(time.getTime() + since * 1000).after(now) || force)) {        //if enough time went by

				replace_blocks(block);        //replace the non-dependent block
				map_burn.remove(time);
			}
		}
	}



	public void log_info(String msg, int min_level) {        //logs a message, according to the log_level
		if(min_level<=log_level)
			log.info("[CreeperHeal] "+msg);
	}

	private void dropBlock(int type_id, short data, Location loc) {
		if(blockDrops.containsKey(type_id)){
			int type_drop = blockDrops.get(type_id);
			int number_drops = 1;
			Random generator = new Random();
			if(type_id == 21)
				number_drops = generator.nextInt(5) + 4;
			else if(type_drop == 331)
				number_drops = generator.nextInt(2) + 4;
			else if(type_drop == 337)
				number_drops = 4;
			else if(type_drop == 348)
				number_drops = generator.nextInt(3) + 2;

			loc.getWorld().dropItemNaturally(loc, new ItemStack(type_drop, number_drops, data));
		}

	}


	private void loadConfig(){            //reads the config
		log.info("Loading config");

		interval = configInt("wait-before-heal", 60);        //tries to read the value directly from the config
		log_level = configInt("log-level", 1);

		drop_blocks_replaced = configBoolean("drop-replaced-blocks", true);


		String tmp_str;

		try{
			tmp_str = getConfiguration().getString("replacement-method", "block-per-block").trim();
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for replacement method field. Defaulting to block-per-block.");
			log_info(e.getLocalizedMessage(), 1);
			tmp_str = "block-per-block";
		}
		if(!tmp_str.equalsIgnoreCase("all-at-once") && !tmp_str.equalsIgnoreCase("block-per-block"))
			log.warning("[CreeperHeal] Wrong value for replacement method field. Defaulting to block-per-block.");
		block_per_block = (tmp_str.equalsIgnoreCase("all-at-once"))?false:true;

		teleport_on_suffocate = configBoolean("teleport-on-suffocate", true);

		burn_interval = configInt("wait-after-fire", 45);

		drop_not_replaced = configBoolean("drop-not-replaced-block", true);

		drop_chance = configInt("drop-chance", 100);

		opEnforce = configBoolean("op-permissions", true);

		cracked = configBoolean("crack-bricks", false);

		world_config.clear();
		for(World w : getServer().getWorlds()) {
			String name = w.getName();
			log_info("Loading world : "+name, 1);

			loadWorldConfig(name);
		}

	}

	public boolean configBoolean(String path, boolean def) {
		boolean tmp;
		try {
			tmp = getConfiguration().getBoolean(path, def);
		}
		catch(Exception e) {
			log.warning("[CreeperHeal] Wrong value for " + path + " field. Defaulting to " + Boolean.toString(def));
			tmp = def;
		}
		return tmp;
	}

	public int configInt(String path, int def) {
		int tmp;
		try {
			tmp = getConfiguration().getInt(path, def);
		}
		catch(Exception e) {
			log.warning("[CreeperHeal] Wrong value for " + path + " field. Defaulting to " + Integer.toString(def));
			tmp = def;
		}
		return tmp;
	}



	public void setup_permissions() {        //permissions stuff

		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if(Permissions == null) {
			if(test != null) {
				Permissions = ((Permissions)test).getHandler();
			}
		}
	}




	public void config_write(){            //write the config to a file, with the values used, or the default ones
		log_info("Writing config...", 2);
		File yml = new File(getDataFolder()+"/config.yml");

		new File(getDataFolder().toString()).mkdir();
		try {
			yml.createNewFile();
		}
		catch (IOException ex) {
			log.warning("[CreeperHeal] Cannot create file "+yml.getPath());
		}

		Configuration config = new Configuration(yml);

		config.load();

		config.setProperty("wait-before-heal", (int) interval);
		config.setProperty("replacement-method", block_per_block ? "block-per-block" : "all-at-once");
		config.setProperty("block-per-block-interval", block_interval);
		config.setProperty("wait-after-fire", burn_interval);
		config.setProperty("refresh-frequency", period);
		config.setProperty("drop-replaced-block", drop_blocks_replaced);
		config.setProperty("drop-not-replaced-block", drop_not_replaced);
		config.setProperty("drop-chance", drop_chance);
		config.setProperty("teleport-on-suffocate", teleport_on_suffocate);
		config.setProperty("log-level", log_level);
		config.setProperty("op-permissions", opEnforce);
		config.setProperty("crack-bricks", cracked);


		for(WorldConfig w : world_config.values()) {
			String name = w.getName();

			int k = 0;

			ArrayList<Object> node_list = w.getConfig();

			for(String property : world_config_nodes)
				config.setProperty( name + "." + property, node_list.get(k++));
		}

		config.save();

	}

	public boolean checkPermissions(String node, Player player) {
		boolean tmp_bool;
		node = "CreeperHeal." + node;
		if (Permissions != null) {
			tmp_bool =  Permissions.has(player, node) || Permissions.has(player, "CreeperHeal.*");

			if(!tmp_bool && opEnforce)

				return player.isOp();

			return tmp_bool;
		} else {
			tmp_bool = (player.hasPermission(node) || player.hasPermission("CreeperHeal.*"));
			if(!tmp_bool && opEnforce)
				return player.isOp();
			return tmp_bool;
		}
	}




	public void storeTNT(ExplosionPrimeEvent event) {
		log_info("tnt primed", 1);
		tnt_location.put(event.getEntity(), new DateLoc(new Date(), event.getEntity().getLocation().getBlock().getLocation()));

		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()

		{

			public void run() {

				cleanTNTLocation();

			}

		}, 200

		);

	}

	private void cleanTNTLocation() {
		Iterator<DateLoc> iter = tnt_location.values().iterator();
		Date now = new Date();
		while( iter.hasNext() ) {
			Date time = iter.next().getTime();
			if(new Date(time.getTime() + 10000).before(now))
				iter.remove();
		}
	}


	public boolean isTrap(Location loc) {
		return (getTrapOwner(loc) != null);
	}

	public boolean isTrap(Block block) {
		return (getTrapOwner(block) != null);
	}

	public boolean isTrap(Entity en){
		return (getTrapOwner(tnt_location.get(en).getLocation().getBlock().getLocation()) != null);
	}


	private void loadTraps(File file) {
		Scanner scanner = null;
		int count = 0;
		try {
			scanner = new Scanner(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] args = line.split(":");
			if(!(args.length == 2))
				continue;
			trap_location.put(args[0], args[1]);
			count++;
		}
		log.info("[CreeperHeal] Loaded " + count + " traps");
	}

	public String locToString(Location loc) {
		return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
	}

	public String locToString(Block block) {
		return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();

	}

	public Location stringToLoc(String str) {
		String[] args = str.split(";");
		World w = getServer().getWorld(args[0]);
		int x = Integer.parseInt(args[1]);
		int y = Integer.parseInt(args[2]);
		int z = Integer.parseInt(args[3]);
		return new Location(w, x, y, z);
	}

	public String getTrapOwner(Location loc) {
		return trap_location.get(locToString(loc));
	}

	public String getTrapOwner(Block block) {
		return trap_location.get(locToString(block));
	}

	private void createTrap(Player player) {
		if(checkPermissions("trap.create", player) || checkPermissions("trap.*", player)) {

			Block block = player.getTargetBlock(transparent_blocks, 10);
			if(block.getType() == Material.TNT) {
				String owner = getTrapOwner(block);
				if(owner == null) {
					trap_location.put(locToString(block), player.getName());
					player.sendMessage(ChatColor.GREEN + "Trap registered");
				}
				else if(owner.equalsIgnoreCase(player.getName()))
					player.sendMessage(ChatColor.RED + "You already registered this trap");
				else    
					player.sendMessage(ChatColor.RED + "Trap belongs to "+ owner);
			}
			else
				player.sendMessage(ChatColor.RED + "You must point to a block of TNT");
		}
		else
			player.sendMessage(ChatColor.RED + "You do not have permission");
	}

	public boolean deleteTrap(Player player) {
		boolean delete_own, delete_all = checkPermissions("trap.remove.all", player) || checkPermissions("trap.*", player);
		delete_own = delete_all;
		if(!delete_own)
			delete_own = checkPermissions("trap.remove.own", player);
		if(delete_own) {

			Block block = player.getTargetBlock(transparent_blocks, 10);

			if(block.getType() == Material.TNT) {

				String owner = getTrapOwner(block.getLocation());

				if(owner == null) {

					player.sendMessage(ChatColor.RED + "Target is not a trap");
					return false;
				}

				else if(owner.equalsIgnoreCase(player.getName())){

					trap_location.remove(locToString(block.getLocation()));

					player.sendMessage(ChatColor.GREEN + "Trap removed");
					return true;

				}

				else {

					if(delete_all) {

						trap_location.remove(locToString( block.getLocation()));

						player.sendMessage(ChatColor.GREEN + "Trap removed");
						return true;
					}

					else {
						player.sendMessage(ChatColor.RED + "Trap belongs to " + owner + ", you cannot remove it.");
						return false;
					}

				}

			}

			else {
				player.sendMessage(ChatColor.RED + "You must target a TNT block.");
				return false;
			}

		}

		else {
			player.sendMessage(ChatColor.RED + "This TNT block is protected");
			return false;
		}

	}

	public void deleteTrap(Location loc){
		trap_location.remove(locToString(loc));
	}

	private void saveTraps() {
		File trapFile = new File(getDataFolder() + "/trap.yml");
		BufferedWriter out;

		trapFile.delete();

		try {
			trapFile.createNewFile();
			out = new BufferedWriter(new FileWriter(trapFile));
			for(String loc : trap_location.keySet()) {
				out.write(loc + ":" + trap_location.get(loc));
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}


	public boolean isAbove(Entity entity, int replaceLimit) {
		return tnt_location.get(entity).getLocation().getBlockY()>= replaceLimit;
	}


	public WorldConfig loadWorldConfig(World world) {

		return loadWorldConfig(world.getName());
	}


	private WorldConfig loadWorldConfig(String name) {
		WorldConfig returnValue = world_config.get(name);
		
		if(returnValue == null){
			boolean creeper = configBoolean(name + ".Creepers", true);
			boolean tnt = configBoolean(name + ".TNT", true);
			boolean fire = configBoolean(name + ".Fire", true);

			boolean ghast = configBoolean(name + ".Ghast", true);

			boolean magical = configBoolean(name + ".Magical", false );

			boolean replace_tnt = configBoolean(name + ".replace-tnt", false);

			boolean replaceAbove = configBoolean(name + ".replace-above-only", false);

			int replaceLimit = configInt(name + ".replace-limit", 64);
			
			boolean enderman = configBoolean(name + ".block-enderman", false);

			String restrict_blocks;

			try{

				restrict_blocks = getConfiguration().getString(name + ".restrict-blocks", "false").trim();

			}

			catch (Exception e) {

				log.warning("[CreeperHeal] Wrong value for " + name + ".restrict-blocks field. Defaulting to false.");

				log_info(e.getLocalizedMessage(), 1);

				restrict_blocks = "false";

			}        //if not a valid value

			if(!restrict_blocks.equalsIgnoreCase("false") && !restrict_blocks.equalsIgnoreCase("whitelist") && !restrict_blocks.equalsIgnoreCase("blacklist")) {

				log.warning("[CreeperHeal] Wrong value for " + name + ".restrict-blocks field. Defaulting to false.");

				restrict_blocks = "false";

			}

			ArrayList<BlockId> restrict_list  = new ArrayList<BlockId>();

			try{

				String tmp_str1 = getConfiguration().getString(name + ".restrict-list", "").trim();

				String[] split = tmp_str1.split(",");

				if(split!=null){        //split the list into single strings of integer

					for(String elem : split) {

						restrict_list.add(new BlockId(elem));

					}

				}

				else

					log_info("[CreeperHeal] Empty restrict-list for world " + name, 1);

			}

			catch (Exception e) {

				log.warning("[CreeperHeal] Wrong values for restrict-list field for world " + name);

				restrict_list.clear();

				restrict_list.add(new BlockId(0));

			}
			
			returnValue = new WorldConfig(name, creeper, tnt, ghast, fire, magical, replace_tnt, restrict_blocks, restrict_list, replaceAbove, replaceLimit, enderman);
			
			world_config.put(name, returnValue);
			return returnValue;
		}

		return returnValue;
	}
}