package com.nitnelave.CreeperHeal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Date;

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
import org.bukkit.inventory.ItemStack;
/*import org.bukkit.material.Attachable;
import org.bukkit.material.SimpleAttachableMaterialData;*/
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;



public class CreeperHeal extends JavaPlugin {
	Logger log;			//to output messages to the console/log
	Map<Date, List<BlockState>> map = Collections.synchronizedMap(new HashMap<Date, List<BlockState>>());		//hashmap storing the list of blocks destroyed in an explosion
	Map<Date, BlockState> map_burn = Collections.synchronizedMap(new HashMap<Date, BlockState>());				//same for burnt blocks
	Date interval = new Date(60000);													//interval defined in the config, with the default value in milisec
	private CreeperListener listener = new CreeperListener(this);						//listener for explosions
	private FireListener fire_listener = new FireListener(this);						//listener for fire
	private int log_level = 1;															//level of message output of the config, with default value
	HashMap<Location, ItemStack[]> chest_contents = new HashMap<Location, ItemStack[]>(); 		//stores the chests contents
	HashMap<Location, String[]> sign_text = new HashMap<Location, String[]>();					//stores the signs text
	HashMap<Location, Byte> note_block = new HashMap<Location, Byte>();
	HashMap<Location, String> mob_spawner = new HashMap<Location, String>();
	boolean creeper = true;						//whether to replace creeper explosions
	boolean tnt = false;						//same for tnt
	private ArrayList<Integer> whitelist_natural = new ArrayList<Integer>(Arrays.asList(1,2,3,9,11,12,13,14,15,16,17,18,21,24,31,32,37,38,39,40,48,49,56,73,79,81,82,86,87,88,89));  //default list of block_id for whitelist
	private ArrayList<Integer> blacklist_natural = new ArrayList<Integer>(Arrays.asList(0));														//same for blacklist
	private ArrayList<Integer> blocks_no_drop = new ArrayList<Integer>(Arrays.asList(0, 8, 9, 10, 11, 12, 13, 18, 51, 78, 79, 88));					//blocks that don't drop anything
	private ArrayList<Integer> blocks_physics = new ArrayList<Integer>(Arrays.asList(12,13,88));						//sand gravel, soulsand fall
	private ArrayList<Integer> blocks_last = new ArrayList<Integer>(Arrays.asList(6,18,26,27,28,31,32,37,38,39,40,50,55,59,63,64,65,66,68,69,70,71,72,75,76,77,81,83,93,94,96));  //blocks dependant on others. to put in last
	private ArrayList<Integer> blocks_non_solid = new ArrayList<Integer>(Arrays.asList(0,6,8,9,26,27,28,30,31,37,38,39,40, 50,55,59,63,64,65,66,68,69,70,71,72,75,76,77,78,83,90,93,94,96));   //the player can breathe
	//private ArrayList<Integer> attachable_blocks = new ArrayList<Integer>(Arrays.asList(50, 68, 69, 75, 76, 77));
	boolean drop_blocks_replaced = true;		//drop items when blocks are overwritten
	public static PermissionHandler Permissions = null;	//permission stuff
	int period = 20;		//frenquency to check for blocks to replace, in sec.
	int block_interval = 5;		//frequency for replacing blocks, in the case of block_per_block
	boolean block_per_block = false;	//as in the config
	boolean teleport_on_suffocate = true;	//teleport player in stuck in an explosion getting replaced
	int burn_interval = 45;			//interval after which burnt blocks are replaced. default value
	boolean replace_burn = true;	//replace burnt blocks?
	boolean replace_tnt = false;	//replace tnt blocks destroyed (primed) in an explosion (to allow for traps?)
	boolean replace_ghast = true;	//replace damage done by ghast fireballs
	boolean replace_other = false;	//replace damage done by other entities (see listener)
	String natural_only = "false";	//whitelist, blacklist or nothing
	boolean teleport_block_per_block = true;



	public void onEnable() {
		log = Logger.getLogger("Minecraft");

		if (!new File(getDataFolder().toString()).exists() ) {
			new File(getDataFolder().toString()).mkdir();
		}

		File yml = new File(getDataFolder()+"/config.yml");

		if (!yml.exists()) {
			config_write();		//write the config with the default values.
		}

		PluginManager pm = getServer().getPluginManager();		//registering the listeners
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, listener, Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_BURN, fire_listener, Event.Priority.Monitor, this);

		PluginDescriptionFile pdfFile = this.getDescription();
		setup_permissions();


		try {			//reading refresh-frequency and block-interval from config. Has to be in onEnable(), because we declare a bukkitscheduler task after that. 
			period = getConfiguration().getInt("refresh-frequency", 20);
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for refill-frequency field. Defaulting to 20 seconds");
			period = 20;
		}

		try {
			block_interval = getConfiguration().getInt("block-interval", 5);
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for block-interval field. Defaulting to 5 ticks");
			block_interval = 5;
		}

		loadConfig();		//read the rest of the config.

		int tmp_period = period * 20;		//register the task to go every "period" second if all at once
		if(block_per_block)					//or every "block_interval" ticks if block_per_block
			tmp_period = block_interval;
		if(tnt || creeper) {
			if( getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
				public void run() {
					check_replace(block_per_block);		//check to replace explosions/blocks
				}}, 200, tmp_period) == -1)
				log.warning("[CreeperHeal] Impossible to schedule the re-filling task. Auto-refill will not work");
		}


		if(replace_burn) {		//register burnt_blocks replacement task
			if( getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
				public void run() {
					replace_burnt(false);
				}}, 200, block_interval) == -1)
				log.warning("[CreeperHeal] Impossible to schedule the replace-burnt task. Burnt blocks replacement will not work");
		}


		log.info("[CreeperHeal] version "+pdfFile.getVersion()+" by nitnelave is enabled");
	}


	public void onDisable() {
		force_replace(0);		//replace blocks still in memory, so they are not lost
		replace_burnt(true);	//same for burnt_blocks
		log.info("[CreeperHeal] Disabled");
	}

	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

		boolean canUseCommand = true;

		if(sender instanceof Player){
			if(Permissions!=null){
				canUseCommand = Permissions.has((Player)sender, "CreeperHeal.admin");		//if permissions, checks for the node
			}
			else {
				canUseCommand = ((Player)sender).isOp();		//defaults to OP system
			}
		}

		if(canUseCommand){
			if(args.length != 0) {		//if it's just /ch, display help
				String cmd = args[0];
				if(cmd.equalsIgnoreCase("creeper")) {
					if(args.length == 1)		//toggle the state
						creeper = !creeper;
					else if(args[1].equalsIgnoreCase("on")) //force to true
						creeper = true;
					else if(args[1].equalsIgnoreCase("off")) //force to false
						creeper = false;
					else {		//invalid, send message with syntax
						sender.sendMessage("/"+command.getName()+" Creeper (on|off)");
						sender.sendMessage("Toggles Creeper's explosion replacement on/off");
						return true;
					}
					sender.sendMessage("Creeper explosions replacement set to : "+Boolean.toString(creeper));
				}
				else if(cmd.equalsIgnoreCase("TNT")){		//same as above
					if(args.length == 1)
						tnt = !tnt;
					else if(args[1].equalsIgnoreCase("on")) 
						tnt = true;
					else if(args[1].equalsIgnoreCase("off")) 
						tnt = false;
					else {
						sender.sendMessage("/"+command.getName()+" TNT (on|off)");
						sender.sendMessage("Toggles TNT's explosion replacement on/off");
						return true;
					}
					sender.sendMessage("TNT explosions replacement set to : "+Boolean.toString(tnt));
				}
				else if(cmd.equalsIgnoreCase("fire")){
					if(args.length == 1)
						replace_burn = !replace_burn;
					else if(args[1].equalsIgnoreCase("on")) 
						replace_burn = true;
					else if(args[1].equalsIgnoreCase("off")) 
						replace_burn = false;
					else {
						sender.sendMessage("/"+command.getName()+" fire (on|off)");
						sender.sendMessage("Toggles burnt blocks replacement on/off");
						return true;
					}
					sender.sendMessage("Burnt blocks replacement set to : "+Boolean.toString(tnt));
				}
				else if(cmd.equalsIgnoreCase("ghast")){
					if(args.length == 1)
						replace_ghast = !replace_ghast;
					else if(args[1].equalsIgnoreCase("on")) 
						replace_ghast = true;
					else if(args[1].equalsIgnoreCase("off")) 
						replace_ghast = false;
					else {
						sender.sendMessage("/"+command.getName()+" ghast (on|off)");
						sender.sendMessage("Toggles ghast explosions replacement on/off");
						return true;
					}
					sender.sendMessage("Ghast explosions replacement set to : "+Boolean.toString(tnt));
				}
				else if(cmd.equalsIgnoreCase("magical")){
					if(args.length == 1)
						replace_other = !replace_other;
					else if(args[1].equalsIgnoreCase("on")) 
						replace_other = true;
					else if(args[1].equalsIgnoreCase("off")) 
						replace_other = false;
					else {
						sender.sendMessage("/"+command.getName()+" magical (on|off)");
						sender.sendMessage("Toggles magical explosions replacement on/off");
						return true;
					}
					sender.sendMessage("Magical explosions replacement set to : "+Boolean.toString(tnt));
				}
				else if(cmd.equalsIgnoreCase("dropReplaced")) {
					if(args.length == 1)
						drop_blocks_replaced = !drop_blocks_replaced;
					else if(args[1].equalsIgnoreCase("on")) 
						drop_blocks_replaced = true;
					else if(args[1].equalsIgnoreCase("off")) 
						drop_blocks_replaced = false;
					else {
						sender.sendMessage("/"+command.getName()+" dropReplaced (on|off)");
						sender.sendMessage("If true, blocks overwritten as an explosion is healed will drop an item");
						return true;
					}
					sender.sendMessage("Block replaced dropping items set to : "+Boolean.toString(drop_blocks_replaced));
				}
				else if(cmd.equalsIgnoreCase("interval")) {	
					if(args.length == 2){
						int interval_date = 0;
						try {
							interval_date = Integer.parseInt(args[1]);
						}
						catch (Exception e) {
							sender.sendMessage("/"+command.getName()+" interval [seconds]");
							sender.sendMessage("Sets the interval before healing an explosion, in the case of all blocks at once");
							return true;
						}
						interval = new Date(interval_date*1000);
						sender.sendMessage("New interval set to : "+interval_date);
					}
					else {
						sender.sendMessage("/"+command.getName()+" interval [seconds]");
						sender.sendMessage("Sets the interval before healing an explosion, in the case of all blocks at once");
						return true;
					}
				}
				else if(cmd.equalsIgnoreCase("burnInterval")) {
					if(args.length == 2){
						int interval_date = 0;
						try {
							interval_date = Integer.parseInt(args[1]);
						}
						catch (Exception e) {
							sender.sendMessage("/"+command.getName()+" burnInterval [seconds]");
							sender.sendMessage("Sets the interval before healing a block burnt");
							return true;
						}
						burn_interval = interval_date;
						sender.sendMessage("New interval set to : "+interval_date);
					}
					else {
						sender.sendMessage("/"+command.getName()+" burn_interval [seconds]");
						sender.sendMessage("Sets the interval before healing a block burnt");
						return true;
					}
				}
				else if(cmd.equalsIgnoreCase("forceHeal") || cmd.equalsIgnoreCase("heal")){
					if(args.length == 2){
						try{
							long since = Long.parseLong(args[1]);
							force_replace(since * 1000);
						}
						catch (Exception e) {
							sender.sendMessage("/"+command.getName()+" forceHeal (seconds)");
							sender.sendMessage("If a time is specified, heals all explosions since x seconds ago. Otherwise, heals all.");
							return true;
						}
					}
					else if(args.length == 1)
						force_replace(0);
					else {
						sender.sendMessage("/"+command.getName()+" forceHeal (seconds)");
						sender.sendMessage("If a time is specified, heals all explosions since x seconds ago. Otherwise, heals all.");
						return true;
					}
					sender.sendMessage("Explosions healed");
				}
				else {		// /ch something gets back to the help
					sender.sendMessage("CreeperHeal -- heals Creepers's damage");
					sender.sendMessage("/ch [forceheal|Creeper|TNT|interval|dropReplaced|fire|burnInterval|ghast|magical]");
					return true;
				}

				new File(getDataFolder()+"/config.yml").delete();		//delete, then rewrite the config with the new settings.

				config_write();
			}
			else {
				sender.sendMessage("CreeperHeal -- heals Creepers's damage");
				sender.sendMessage("/ch [forceheal|Creeper|TNT|interval|dropReplaced|fire|burnInterval|ghast|magical]");
				return true;
			}
		}
		else {
			sender.sendMessage("You don't have the Permission.");
		}




		return true;
	}



	public void recordBlocks(EntityExplodeEvent event) {		//record the list of blocks of an explosion, from bottom to top
		List<Block> list = event.blockList();			//the list declared by the explosion
		List<BlockState> list_state = new ArrayList<BlockState>();		//the list of blockstate we'll be keeping afterward
		List<Location> list_loc = new ArrayList<Location>();			//to check for duplicates
		if(replace_tnt && event.getEntity() instanceof TNTPrimed) {			//to replace the tnt that just exploded
			int x = (int) Math.floor(event.getEntity().getLocation().getX());
			int y = (int) Math.floor(event.getEntity().getLocation().getY());
			int z = (int) Math.floor(event.getEntity().getLocation().getZ());
			World w = event.getEntity().getWorld();
			BlockState tmp_state = w.getBlockAt(x, y, z).getState();
			w.getBlockAt(x, y, z).setTypeId(46);							//set the block to tnt
			list_state.add(w.getBlockAt(x, y, z).getState());				//record it
			list_loc.add(new Location(w,x,y,z));
			w.getBlockAt(x, y, z).setTypeIdAndData(tmp_state.getTypeId(), tmp_state.getRawData(), false);		//set it back to what it was
		}
		for(Block block : list){
			if(block.getState() instanceof ContainerBlock) {		//save the inventory
				chest_contents.put(block.getLocation(), ((ContainerBlock) block.getState()).getInventory().getContents().clone());
				((ContainerBlock) block.getState()).getInventory().clear();
			}
			else if(block.getState() instanceof Sign) {				//save the text
				sign_text.put(block.getLocation(), ((Sign)block.getState()).getLines());
			}
			else if(block.getState() instanceof NoteBlock) {
				note_block.put(block.getLocation(), ((NoteBlock)(block.getState())).getRawNote());
			}
			else if(block.getState() instanceof CreatureSpawner) {
				mob_spawner.put(block.getLocation(), ((CreatureSpawner)(block.getState())).getCreatureTypeId());
			}
			switch (block.getType()) {		
			case IRON_DOOR :				//in case of a door or bed, only store one block to avoid dupes
			case WOODEN_DOOR :
				if(block.getData() < 8) {
					list_state.add(block.getState());
					list_loc.add(block.getLocation());
					list_loc.add(block.getRelative(0, 1, 0).getLocation());		//but store both locations
					block.setTypeIdAndData(0, (byte)0, false);
				}
				break;
			case BED_BLOCK :
				byte data = block.getData();
				if(data < 8) {
					list_state.add(block.getState());
					list_loc.add(block.getLocation());
					BlockFace face;
					if(data == 0)			//facing the right way
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
			case AIR :						//don't store air 
			case FIRE :						//or fire
				break;
			case TNT :						//allow for tnt to be stored if the setting is there
				if(replace_tnt) {
					list_state.add(block.getState());
					list_loc.add(block.getLocation());
					block.setTypeIdAndData(0, (byte)0, false);
				}
				break;
			default :						//store the rest
				list_state.add(block.getState());
				list_loc.add(block.getLocation());
				block.setTypeIdAndData(0, (byte)0, false);
				break;
			}
		}

		for(Block block : list) {			//go over a second time to check for torches, wire, or anything that would drop and store them
			Block block_up = block.getRelative(BlockFace.UP);
			if(blocks_last.contains(block_up.getTypeId()) && !list_loc.contains(block_up.getLocation())) {
				if(block.getState() instanceof Sign) {				//save the text
					sign_text.put(block.getLocation(), ((Sign)block.getState()).getLines());
				}
				switch (block.getType()) {		
				case IRON_DOOR :				//in case of a door or bed, only store one block to avoid dupes
				case WOODEN_DOOR :
				case BED_BLOCK :
					if(block.getData() < 8) {
						list_state.add(block.getState());
						block.setTypeIdAndData(0, (byte)0, false);
					}
					break;
				case AIR :						//don't store air 
					break;
				case TNT :						//allow for tnt to be stored if the setting is there
					if(replace_tnt) {
						list_state.add(block.getState());
						block.setTypeIdAndData(0, (byte)0, false);
					}
					break;
				default :						//store the rest
					list_state.add(block.getState());
					block.setTypeIdAndData(0, (byte)0, false);
					break;
				}
			}
		}

		BlockState[] tmp_array = list_state.toArray(new BlockState[list_state.size()]);		//sort through an array, then store back in the list
		Arrays.sort(tmp_array, new CreeperComparator());
		list_state.clear();
		for(BlockState block : tmp_array){
			list_state.add(block);
		}

		map.put(new Date(), list_state);		//store in the global hashmap, with the time it happened as a key

		log_info("EXPLOSION!", 3);



	}
	public void check_replace(boolean block_per_block) {		//check to see if any block has to be replaced
		Date now = new Date();

		log_info("Replacing blocks...", 3);
		Set<Date> keyset = map.keySet();
		Iterator<Date> iterator = keyset.iterator();		//goes over the hashmap containing the explosions
		while(iterator.hasNext()) {
			Date time = iterator.next();
			if(new Date(time.getTime() + interval.getTime()).before(now)) {		//if enough time went by
				if(!block_per_block){		//all blocks at once
					replace_blocks(map.get(time));		//replace the blocks
					map.remove(time);					//remove the explosion from the record
					log_info("Blocks replaced!", 2);
				}
				else {			//block per block
					if(!map.get(time).isEmpty())		//still some blocks left to be replaced
						replace_one_block(map.get(time));		//replace one
					if(map.get(time).isEmpty()) 		//if empty, remove from list
						map.remove(time);
					log_info("blocks replaced!", 3);
				}

			}
		}	


	}

private void replace_one_block(List<BlockState> list) {		//replace one block (block per block

	Iterator<BlockState> iter = list.iterator();
	while(iter.hasNext()){		//finds the first block that is not dependent
		BlockState block = iter.next();
		if(!blocks_last.contains(block.getTypeId())){
			replace_blocks(block);		//replace it
			check_player_one_block(block.getBlock().getLocation());
			log_info(block.getType().toString(), 3);
			iter.remove();		//remove it
			return;
		}
	}
	replace_blocks(list.get(0));		//only dependent blocks left, replace the first
	check_player_one_block(list.get(0).getBlock().getLocation());
	log_info(list.get(0).getType().toString(), 3);
	list.remove(0);
}

public void check_player_one_block(Location loc) {
	if(teleport_block_per_block) {
		Arrow entity = loc.getWorld().spawnArrow(loc, new Vector(0,0,0), 0, 0);
		List<Entity> play_list = entity.getNearbyEntities(10, 10, 10);
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
	int x = (int) Math.floor(player.getLocation().getX());		//get the player's coordinates in ints, to have the block he's standing on
	int y = (int) Math.floor(player.getLocation().getY());
	int z = (int) Math.floor(player.getLocation().getZ());
	World w = player.getWorld();
	if(!blocks_non_solid.contains(w.getBlockAt(x,y,z).getTypeId()) || !blocks_non_solid.contains(w.getBlockAt(x, y + 1, z).getTypeId())) {
		log_info("player suffocating",2);
		for(int k =1; k + y < 127; k++) {		//all the way to the sky, checks if there's some room up or around

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


public void force_replace(long since) {		//force replacement of all the explosions since x seconds
	Date now = new Date();

	Iterator<Date> iterator = map.keySet().iterator();
	while(iterator.hasNext()) {
		Date time = iterator.next();
		if(new Date(time.getTime() + since).after(now) || since == 0) {		//if the explosion happened since x seconds
			replace_blocks(map.get(time));
			iterator.remove();
			log_info("Blocks replaced!", 2);
		}
	}
	if(since == 0) 
		replace_burnt(true);
}


private void replace_blocks(List<BlockState> list) {	//replace all the blocks in the given list
	while(!list.isEmpty()){			//replace all non-physics non-dependent blocks
		Iterator<BlockState> iter = list.iterator();
		while (iter.hasNext()){
			BlockState block = iter.next();
			if(!blocks_physics.contains(block.getTypeId()) && !blocks_last.contains(block.getTypeId())){
				block_state_replace(block);
				iter.remove();
			}
		}
		iter = list.iterator();
		while (iter.hasNext()){		//then all physics
			BlockState block = iter.next();
			if(blocks_physics.contains(block.getTypeId())){
				block_state_replace(block);
				iter.remove();
			}
		}
		iter = list.iterator();
		while (iter.hasNext()){		//lastly all dependent
			BlockState block = iter.next();
			if(blocks_last.contains(block.getTypeId())){
				block_state_replace(block);
				iter.remove();
			}
		}
	}
	if(teleport_on_suffocate) {			//checks for players suffocating anywhere
		Player[] player_list = getServer().getOnlinePlayers();
		for(Player player : player_list) {
			check_player_suffocate(player);
		}
	}

}



private boolean check_free_horizontal(World w, double x, double y, double z, Player player) {		//checks one up and down, to broaden the scope
	for(int k = -1; k<2; k++){
		if(check_free(w, x, y+k, z, player))
			return true;  //found a spot
	}
	return false;
}

private boolean check_free(World w, double x, double y, double z, Player player) {
	if(blocks_non_solid.contains(w.getBlockAt(new Location(w, x, y, z)).getTypeId()) && blocks_non_solid.contains(w.getBlockAt(new Location(w, x, y + 1, z)).getTypeId()) && !blocks_non_solid.contains(w.getBlockAt(new Location(w, x, y-1, z)).getTypeId())) {
		Location loc = new Location(w, x+0.5, y, z+0.5);
		loc.setYaw(player.getLocation().getYaw());
		player.teleport(loc);			//if there's ground under and space to breathe, put the player there
		return true;
	}
	return false;
}

private void replace_blocks(BlockState block) {		//if there's just one block, no need to go over all this
	block_state_replace(block);
}


//replaces a single block, first sort
public void block_state_replace(BlockState block){
	if (block.getType() == Material.WOODEN_DOOR || block.getType() == Material.IRON_DOOR_BLOCK) {		//if it's a door, put the bottom then the top (which is unrecorded)
		block_replace(block.getBlock(), block.getTypeId(), block.getRawData());
		block_replace(block.getBlock().getFace(BlockFace.UP), block.getTypeId(), (byte)(block.getRawData() + 8));
	}
	else if(block.getType() == Material.BED_BLOCK) {		//put the head, then the feet
		byte data = block.getRawData();
		try{
			block.getBlock().setTypeIdAndData(block.getTypeId(), data, false);		//head
		}
		catch(IndexOutOfBoundsException e) {
			log_info(e.getLocalizedMessage(), 1);
		}
		BlockFace face;
		if(data == 0)			//facing the right way
			face = BlockFace.WEST;
		else if(data == 1)
			face = BlockFace.NORTH;
		else if(data == 2)
			face = BlockFace.EAST;
		else
			face = BlockFace.SOUTH;
		block_replace(block.getBlock().getFace(face), block.getTypeId(), (byte)(data + 8));    //feet
	}
	else if(block.getType() == Material.PISTON_MOVING_PIECE) {			//extended piston, you have to put a base instead (and it comes out unextended)
		log_info("Piston_moving_piece", 2);
		block_replace(block.getBlock(), (block.getRawData() >7)?Material.PISTON_STICKY_BASE.getId():Material.PISTON_BASE.getId(), (byte)(block.getRawData() + 8));

	}
	else {		//rest of it, just normal
		block_replace(block.getBlock(), block.getTypeId(), block.getRawData());
	}

}
//actual replacement method
public void block_replace(Block block, int type_id, byte rawData) {
	int block_id = block.getTypeId();		//id of the block in the world, before it is replaced

	if(!blocks_no_drop.contains(block_id) && drop_blocks_replaced) {		//drop an item in the spot
		block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block_id, 1, block.getData()));
	}
	if((natural_only.equalsIgnoreCase("whitelist") && whitelist_natural.contains(type_id) 
			|| (natural_only.equalsIgnoreCase("blacklist") && !blacklist_natural.contains(type_id) 
					|| natural_only.equalsIgnoreCase("false")))){			//if the block is to be replaced
		try {
			if(blocks_physics.contains(block_id)) {			//if the spot for the sand is occupied, put it in the first free spot above
				for(int k = 1; block.getY() + k < 127; k++) {
					if(block.getRelative(0,k,0) != null) {
						if(block.getRelative(0, k, 0).getTypeId() == 0) {
							block.getRelative(0, k, 0).setTypeIdAndData(block_id, (byte)0, false);
							break;
						}
					}
					else		//should not be thrown, maybe at the upper limit of the map?
						log.warning("block.getRelative(0,"+k+", 0) is null?? Y: "+(block.getY()+k));
				}
			}
			else
				block.setTypeIdAndData(type_id, rawData, false);		//replace the block
		}
		catch(IndexOutOfBoundsException e) {
			log_info(e.getLocalizedMessage(), 1);
		}

		if(block.getState() instanceof ContainerBlock) {			//if it's a chest, put the inventory back
			((ContainerBlock) block.getState()).getInventory().setContents(chest_contents.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ())));
			block.getState().update();
			chest_contents.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
		}
		else if(block.getState() instanceof Sign) {					//if it's a sign... no I'll let you guess
			log_info("replacing sign_text",2);
			int k = 0;

			for(String line : sign_text.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()))) {
				((Sign) block.getState()).setLine(k, line);
				k++;
			}
			sign_text.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));

		}
		else if(block.getState() instanceof NoteBlock) {					//if it's a sign... no I'll let you guess
			((NoteBlock)block.getState()).setRawNote(note_block.get(block.getLocation()));
			block.getState().update();
			note_block.remove(block.getLocation());
		}
		else if(block.getState() instanceof CreatureSpawner) {					//if it's a sign... no I'll let you guess
			((CreatureSpawner)block.getState()).setCreatureTypeId(mob_spawner.get(block.getLocation()));
			block.getState().update();
			mob_spawner.remove(block.getLocation());
		}

	}

}

public void record_burn(Block block) {			//record a burnt block
	if(block.getType() != Material.TNT) {		//unless it's TNT triggered by fire
		Date now = new Date();
		map_burn.put(now, block.getState());
		BlockState block_up = block.getFace(BlockFace.UP).getState();
		if(blocks_last.contains(block_up.getTypeId())) {		//the block above is a dependent block, store it, but one interval after
			map_burn.put(new Date(now.getTime() + burn_interval*1000), block_up);
			if(block_up instanceof Sign) {				//as a side note, chests don't burn, but signs are dependent
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

public void replace_burnt(boolean force) {		//checks for burnt blocks to replace, with an override for onDisable()
	Iterator<Date> iter = map_burn.keySet().iterator();
	Date now = new Date();
	HashMap<Date, BlockState> to_add = new HashMap<Date, BlockState>();
	while(iter.hasNext()) {
		Date time = iter.next();
		if((new Date(time.getTime() + burn_interval * 1000).before(now)) || force) {		//if enough time went by
			BlockState block = map_burn.get(time);
			if(blocks_last.contains(block.getTypeId())) {
				if(blocks_non_solid.contains(block.getBlock().getFace(BlockFace.DOWN).getTypeId()) && !force) {		//if it's a dependent, and there's nothing under, store it for later
					iter.remove();
					to_add.put(new Date(time.getTime() + burn_interval*1000), block);
				}
				else {
					replace_blocks(block);		//replace the block as there's something under
					iter.remove();
				}
			}
			else {
				replace_blocks(block);		//replace the non-dependent block
				iter.remove();
			}
		}
	}
	map_burn.putAll(to_add);		//stores the non-dependent to be re-checked later
}


public void log_info(String msg, int min_level) {		//logs a message, according to the log_level
	if(min_level<=log_level)
		log.info("[CreeperHeal] "+msg);
}



private void loadConfig(){			//reds the config
	int interval_date;
	try {
		interval_date = getConfiguration().getInt("interval", 60);		//tries to read the value directly from the config
	}
	catch (Exception e) {		//wrong value, not an integer
		log.warning("[CreeperHeal] Wrong value for interval field. Defaulting to 1 minute");
		interval_date = 60;
	}
	interval = new Date(interval_date*1000);

	try {
		log_level = getConfiguration().getInt("log-level", 0);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for log-level field. Defaulting to 0.");
	}

	try {
		creeper = getConfiguration().getBoolean("Creepers", true);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for Creepers field. Defaulting to true.");
	}

	try {
		tnt = getConfiguration().getBoolean("TNT", true);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for TNT field. Defaulting to false.");
	}

	try{
		natural_only = getConfiguration().getString("replace-natural-only", "false").trim();
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for replace-natural-only field. Defaulting to false.");
		log_info(e.getLocalizedMessage(), 1);
		natural_only = "false";
	}		//if not a valid value
	if(!natural_only.equalsIgnoreCase("false") && !natural_only.equalsIgnoreCase("whitelist") && !natural_only.equalsIgnoreCase("blacklist")) {
		log.warning("[CreeperHeal] Wrong values for replace-natural-only field. Defaulting to false.");
		natural_only = "false";
	}

	whitelist_natural  = new ArrayList<Integer>();
	try{
		String tmp_str = getConfiguration().getString("natural-blocks-whitelist", "").trim();
		if(tmp_str.split(",")!=null){		//split the list into single strings of integer
			for(String elem : tmp_str.split(",")) {
				whitelist_natural.add(Integer.parseInt(elem.trim()));
			}
		}
		else
			log_info("[CreeperHeal] Empty white-list", 1);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for natural-blocks-whitelist field.");
	}

	blacklist_natural = new ArrayList<Integer>();
	try{
		String tmp_str = getConfiguration().getString("natural-blocks-blacklist", "").trim();
		if(tmp_str.split(",")!=null){
			for(String elem : tmp_str.split(",")) {
				blacklist_natural.add(Integer.parseInt(elem.trim()));
			}
		}
		else
			log_info("[CreeperHeal] Empty black-list", 1);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for natural-blocks-blacklist field.");
	}

	try{
		drop_blocks_replaced = getConfiguration().getBoolean("drop-replaced-blocks", true);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for drop-replaced-blocks field. Defaulting to true.");
	}

	try{
		block_per_block = getConfiguration().getBoolean("block-per-block", true);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for block-per-block field. Defaulting to true.");
	}

	try{
		teleport_on_suffocate = getConfiguration().getBoolean("teleport-on-suffocate", true);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for teleport-on-suffocate field. Defaulting to true.");
	}

	try{
		teleport_block_per_block = getConfiguration().getBoolean("teleport-on-suffocate-block-per-block", true);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for teleport-on-suffocate-block-per-block field. Defaulting to true.");
	}

	try{
		replace_burn = getConfiguration().getBoolean("replace-burnt-blocks", true);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for replace-burnt-blocks field. Defaulting to true.");
	}

	try {
		burn_interval = getConfiguration().getInt("burn-interval", 45);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for burn-interval field. Defaulting to 45.");
	}

	try{
		replace_tnt = getConfiguration().getBoolean("replace-tnt", false);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for replace-tnt field. Defaulting to false.");
	}

	try{
		replace_ghast = getConfiguration().getBoolean("replace-ghast-fireballs", true);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for replace-ghast-fireballs field. Defaulting to true.");
	}

	try{
		replace_other = getConfiguration().getBoolean("replace-magic-explosions", false);
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Wrong values for replace-magic-explosions field. Defaulting to false.");
	}
}

public void setup_permissions() {		//permissions stuff

	Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

	if(Permissions == null) {
		if(test != null) {
			Permissions = ((Permissions)test).getHandler();
		}
	}
}

public void config_write(){			//write the config to a file, with the values used, or the default ones
	File yml = new File(getDataFolder()+"/config.yml");

	new File(getDataFolder().toString()).mkdir();
	try {
		yml.createNewFile();
	}
	catch (IOException ex) {
		log.warning("[CreeperHeal] Cannot create file "+yml.getPath());
	}
	try {
		BufferedWriter out = new BufferedWriter(new FileWriter(yml, true));


		out.write("Creepers: "+Boolean.toString(creeper)+"    #replaces Creeper damage");
		out.newLine();
		out.write("TNT: "+Boolean.toString(tnt)+"    #replaces TNT damage");
		out.newLine();
		out.write("replace-ghast-fireballs: "+Boolean.toString(replace_ghast)+"       #replace damage done by ghast fireballs");
		out.newLine();
		out.write("replace-magic-explosions: "+Boolean.toString(replace_other)+"        #replace damage done by other causes");
		out.newLine();
		out.write("interval: "+(interval.getTime()/1000)+"      #in seconds, how long you have to wait before the damage starts getting repaired");
		out.newLine();
		out.write("block-per-block: "+Boolean.toString(block_per_block)+"        #Replaces one block at a time given the block-interval, or the whole explosion after the interval");
		out.newLine();
		out.write("block-interval: "+block_interval+"     #in ticks, 1/20th of a second, rate of replacement for explosions. Also frequency of check for fire block replacement");
		out.newLine();
		out.write("replace-burnt-blocks: "+Boolean.toString(replace_burn)+"       #If true, replaces the blocks burnt after burnt_interval");
		out.newLine();
		out.write("burn-interval: "+burn_interval+"        #in seconds, how long you have to wait before the blocks burnt are replaced");
		out.newLine();
		out.write("replace-natural-only: "+natural_only+"    #replace only natural blocks. Can be false, whitelist or blacklist");
		out.newLine();
		out.write("natural-blocks-whitelist: "+whitelist_natural.toString().substring(1, whitelist_natural.toString().length()-1)+"    #Blocks that will get replaced if replace-natural-only is set to whitelist");
		out.newLine();
		out.write("natural-blocks-blacklist: "+blacklist_natural.toString().substring(1, blacklist_natural.toString().length()-1)+"        #Blocks that will not get replaced if replace-natural-only is set to blacklist");
		out.newLine();
		out.write("refresh-frequency: "+period+"      #in seconds, how often it should check for explosions to be replaced, if block-per-block is set to false");
		out.newLine();
		out.write("drop-replaced-blocks: "+Boolean.toString(drop_blocks_replaced)+"      #gives back a drop when you place a block in an area to be healed");
		out.newLine();
		out.write("teleport-on-suffocate: "+Boolean.toString(teleport_on_suffocate)+"     #Teleport players out of explosions being healed if they suffocate (not for block_per_block)");
		out.newLine();
		out.write("teleport-on-suffocate-block-per-block: "+Boolean.toString(teleport_block_per_block)+"     #Teleport players out of explosions being healed if they suffocate (for block_per_block)");
		out.newLine();
		out.write("replace-tnt: "+Boolean.toString(replace_tnt)+"     #whether exploding tnt should be replaced or not");
		out.newLine();
		out.write("log-level: "+log_level+"      #0-3 0:silent, 3:verbose. Recommended:1");
		//Close the output stream
		out.close();
	}
	catch (Exception e) {
		log.warning("[CreeperHeal] Cannot write config file: "+e);
	}
}

}
