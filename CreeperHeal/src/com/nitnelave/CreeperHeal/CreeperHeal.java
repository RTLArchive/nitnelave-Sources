package com.nitnelave.CreeperHeal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.Date;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;



public class CreeperHeal extends JavaPlugin {
	Logger log;
	HashMap<Date, List<BlockState>> map = new HashMap<Date, List<BlockState>>();
	Date interval = new Date(60000);
	private CreeperListener listener;
	private int log_level = 0;
	HashMap<Location, ItemStack[]> chest_contents = new HashMap<Location, ItemStack[]>();
	HashMap<Location, String[]> sign_text = new HashMap<Location, String[]>();
	boolean creeper = true;
	boolean tnt = false;
	private ArrayList<Integer> whitelist_natural = new ArrayList<Integer>(Arrays.asList(1,2,3,9,11,12,13,14,15,16,17,18,21,24,31,32,37,38,39,40,48,49,56,73,79,81,82,86,87,88,89));
	private ArrayList<Integer> blacklist_natural = new ArrayList<Integer>(Arrays.asList(0));
	private ArrayList<Integer> blocks_no_drop = new ArrayList<Integer>(Arrays.asList(0, 8, 9, 10, 11, 12, 13, 18, 51, 78, 79));
	private ArrayList<Integer> blocks_physics = new ArrayList<Integer>(Arrays.asList(12,13,88));
	private ArrayList<Integer> blocks_last = new ArrayList<Integer>(Arrays.asList(6,26,27,28,31,32,37,38,39,40,50,55,59,63,64,65,66,68,69,70,71,72,75,76,77,81,83,93,94,96));
	boolean drop_blocks_replaced = true;
	public static PermissionHandler Permissions = null;
	int period = 20;

	
	String natural_only;


	public void onEnable() {
		log = Logger.getLogger("Minecraft");

		if (!new File(getDataFolder().toString()).exists() ) {
			new File(getDataFolder().toString()).mkdir();
		}

		File yml = new File(getDataFolder()+"/config.yml");

		if (!yml.exists()) {
			config_write();
		}

		listener = new CreeperListener(this);
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, listener, Event.Priority.Monitor, this);

		PluginDescriptionFile pdfFile = this.getDescription();
		setup_permissions();


		try {
			period = getConfiguration().getInt("refresh-frequency", 20);
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for refill-frequency field. Defaulting to 20 seconds");
			period = 20;
		}
		
		loadConfig();
		
		if( getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			public void run() {
				check_replace();
			}}, 200, period * 20) == -1)
			log.warning("[CreeperHeal] Impossible to schedule the re-filling task. Auto-refill will not work");

		log.info("[CreeperHeal] version "+pdfFile.getVersion()+" by nitnelave is enabled");
	}
	
	
	public void onDisable() {
		force_replace(0);
		log.info("[CreeperHeal] Disabled");
	}
	
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		String cmd = command.getName();
		boolean canUseCommand = true;
		
		if(sender instanceof Player){
			if(Permissions!=null){
				canUseCommand = Permissions.has((Player)sender, "CreeperHeal.admin");
			}
			else {
				canUseCommand = ((Player)sender).isOp();
			}
		}
		
		if(canUseCommand){
			if(cmd.equalsIgnoreCase("CHreload")){
				loadConfig();
				sender.sendMessage("Config reloaded");
			}
			else if(cmd.equalsIgnoreCase("CHcreeper")) {
				if(args.length == 0)
					creeper = !creeper;
				else if(args[0].equalsIgnoreCase("on")) 
					creeper = true;
				else if(args[0].equalsIgnoreCase("off")) 
					creeper = false;
				else
					return false;
				sender.sendMessage("Creeper explosions replacement set to : "+Boolean.toString(creeper));
			}
			else if(cmd.equalsIgnoreCase("CHTNT")){
				if(args.length == 0)
					tnt = !tnt;
				else if(args[0].equalsIgnoreCase("on")) 
					tnt = true;
				else if(args[0].equalsIgnoreCase("off")) 
					tnt = false;
				else
					return false;
				sender.sendMessage("TNT explosions replacement set to : "+Boolean.toString(tnt));
			}
			else if(cmd.equalsIgnoreCase("CHdropReplaced")) {
				if(args.length == 0)
					drop_blocks_replaced = !drop_blocks_replaced;
				else if(args[0].equalsIgnoreCase("on")) 
					drop_blocks_replaced = true;
				else if(args[0].equalsIgnoreCase("off")) 
					drop_blocks_replaced = false;
				else
					return false;
				sender.sendMessage("Block replaced dropping items set to : "+Boolean.toString(drop_blocks_replaced));
			}
			else if(cmd.equalsIgnoreCase("CHinterval")) {
				if(args.length == 1){
					int interval_date = 0;
					try {
						interval_date = Integer.parseInt(args[0]);
					}
					catch (Exception e) {
						return false;
					}
					interval = new Date();
					interval.setTime(interval_date*1000);
					sender.sendMessage("New interval set to : "+interval_date);
				}
				else 
					return false;
			}
			else if(cmd.equalsIgnoreCase("CHforceHeal")){
				if(args.length == 1){
					try{
						long since = Long.parseLong(args[0]);
						force_replace(since * 1000);
					}
					catch (Exception e) {
						return false;
					}
				}
				else if(args.length == 0)
					force_replace(0);
				else
					return false;
				sender.sendMessage("Explosions healed");
			}
			else
				return false;
			
			new File(getDataFolder()+"/config.yml").delete();

			config_write();
		}
		else {
			sender.sendMessage("You don't have the Permission.");
		}
		
		
		
		
		return true;
	}
	
	

	public void recordBlocks(EntityExplodeEvent event) {
		event.setYield(0);
		List<Block> list = event.blockList();
		List<BlockState> list_state = new ArrayList<BlockState>();
		for(Block block : list){
			if(block.getTypeId() != 46)
				list_state.add(block.getState());
			if(block.getState() instanceof ContainerBlock) {
				chest_contents.put(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()), ((ContainerBlock) block.getState()).getInventory().getContents().clone());
				((ContainerBlock) block.getState()).getInventory().clear();
			}
			else if(block.getState() instanceof Sign) {
				sign_text.put(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()), ((Sign)block.getState()).getLines());
				log_info("sign registered", 2);
				
			}
			if(blocks_last.contains(block.getRelative(BlockFace.UP).getTypeId()))
				block.getRelative(BlockFace.UP).setTypeIdAndData(0, (byte)0, false);
		}
		map.put(new Date(), list_state);
		log_info("EXPLOSION!", 2);

	}
	public void check_replace() {
		Date now = new Date();

		log_info("Replacing blocks...", 2);
		Iterator<Date> iterator = map.keySet().iterator();
		while(iterator.hasNext()) {
			Date time = iterator.next();
			if(new Date(time.getTime() + interval.getTime()).before(now)) {
				if(map.get(time) == null) {
					log.warning("null map key?");
				}
				else {
					replace_blocks(map.get(time));
					iterator.remove();
					log_info("Blocks replaced!", 1);
				}


			}
		}
	}
	
	public void force_replace(long since) {
		Date now = new Date();

		log_info("Force_replace", 2);
		Iterator<Date> iterator = map.keySet().iterator();
		while(iterator.hasNext()) {
			Date time = iterator.next();
			if(new Date(time.getTime() + since).after(now) || since == 0) {
				if(map.get(time) == null) {
					log.warning("null map key?");
				}
				else {
					replace_blocks(map.get(time));
					iterator.remove();
					log_info("Blocks replaced!", 1);
				}


			}
		}
	}
	private void replace_blocks(List<BlockState> list) {
		while(!list.isEmpty()){
			Iterator<BlockState> iter = list.iterator();
			while (iter.hasNext()){
				BlockState block = iter.next();
				if(!blocks_physics.contains(block.getTypeId()) && !blocks_last.contains(block.getTypeId())){
					block_state_replace(block);
					iter.remove();
				}
			}
			iter = list.iterator();
			while (iter.hasNext()){
				BlockState block = iter.next();
				if(blocks_physics.contains(block.getTypeId())){
					block_state_replace(block);
					iter.remove();
				}
			}
			iter = list.iterator();
			while (iter.hasNext()){
				BlockState block = iter.next();
				if(blocks_last.contains(block.getTypeId())){
					//log_info("torch data : "+block.getRawData(),2);
					block_state_replace(block);
					iter.remove();
				}
			}
		}
		
	}
	public void block_state_replace(BlockState block){
		if ((block.getType() == Material.WOODEN_DOOR || block.getType() == Material.IRON_DOOR_BLOCK) && block.getRawData() < 8) {
			block_replace(block.getBlock(), block.getTypeId(), block.getRawData());
			block_replace(block.getBlock().getFace(BlockFace.UP), block.getTypeId(), (byte)(block.getRawData() + 8));
		}
		else if(block.getType() == Material.BED_BLOCK && block.getRawData() < 8) {
			byte data = block.getRawData();
			block.getBlock().setTypeIdAndData(block.getTypeId(), data, false);
			BlockFace face = null;
			if(data == 0)
				face = BlockFace.WEST;
			else if(data == 1)
				face = BlockFace.NORTH;
			else if(data == 2)
				face = BlockFace.EAST;
			else
				face = BlockFace.SOUTH;
			block_replace(block.getBlock().getFace(face), block.getTypeId(), (byte)(block.getRawData() + 8));
		}
		else if(block.getType() != Material.BED_BLOCK && block.getType() != Material.WOODEN_DOOR && block.getType() != Material.IRON_DOOR){
			block_replace(block.getBlock(), block.getTypeId(), block.getRawData());
		}
		
	}
	
	public void block_replace(Block block, int type_id, byte rawData) {
		int block_id = block.getTypeId();
		
		if(!blocks_no_drop.contains(block_id) && drop_blocks_replaced) {
			block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block_id, 1, block.getData()));
		}
		else if(blocks_physics.contains(block_id)) {
			//log_info("sand!", 2);
			for(int k = 1; block.getY() + k < 128; k++) {
				if(block.getRelative(0, k, 0).getTypeId() == 0) {
					block.getRelative(0, k, 0).setTypeIdAndData(block_id, (byte)0, false);
					break;
				}
			}
		}
		if((natural_only.equalsIgnoreCase("whitelist") && whitelist_natural.contains(type_id) 
				|| (natural_only.equalsIgnoreCase("blacklist") && !blacklist_natural.contains(type_id) 
						|| natural_only.equalsIgnoreCase("false")))){
			block.setTypeIdAndData(type_id, rawData, false);
			
			if(block.getState() instanceof ContainerBlock) {
				((ContainerBlock) block.getState()).getInventory().setContents(chest_contents.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ())));
				block.getState().update();
				chest_contents.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
			}
			if(block.getState() instanceof Sign) {
				log_info("replacing sign_text",2);
				int k = 0;
				for(String line : sign_text.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()))) {
					((Sign) block.getState()).setLine(k, line);
					k++;
				}
				sign_text.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
			}
		}
		
	}

	public void log_info(String msg, int min_level) {
		if(min_level<=log_level)
			log.info(msg);
	}

	
	
	private void loadConfig(){
		int interval_date = 0;
		try {
			interval_date = getConfiguration().getInt("interval", 60);
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for interval field. Defaulting to 1 minute");
			interval_date = 60;
		}
		interval = new Date(interval_date*1000);
		try {
			log_level = getConfiguration().getInt("log-level", 0);
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong values for log field. Defaulting to 0.");
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
		}
		if(!natural_only.equalsIgnoreCase("false") && !natural_only.equalsIgnoreCase("whitelist") && !natural_only.equalsIgnoreCase("blacklist"))
			log.warning("[CreeperHeal] Wrong values for replace-natural-only field. Defaulting to false.");

		whitelist_natural  = new ArrayList<Integer>();
		try{
			String tmp_str = getConfiguration().getString("natural-blocks-whitelist", "").trim();
			if(tmp_str.split(",")!=null){
				for(String elem : tmp_str.split(",")) {
					whitelist_natural.add(Integer.parseInt(elem));
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
					blacklist_natural.add(Integer.parseInt(elem));
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
	}
	
	public void setup_permissions() {

		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if(Permissions == null) {
			if(test != null) {
				Permissions = ((Permissions)test).getHandler();
			}
		}
	}
	
	public void config_write(){
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


			out.write("refresh-frequency: "+period+"      #in seconds");
			out.newLine();
			out.write("interval: "+(interval.getTime()/1000)+"      #in seconds, how long you have to wait before the damage is undone");
			out.newLine();
			out.write("log-level: "+log_level+"      #0-2 0:silent, 2:verbose");
			out.newLine();
			out.write("TNT: "+Boolean.toString(tnt)+"    #replaces TNT damage");
			out.newLine();
			out.write("Creepers: "+Boolean.toString(creeper)+"    #replaces Creeper damage");
			out.newLine();
			out.write("replace-natural-only: "+natural_only+"    #replace only natural blocks. Can be false, whitelist or blacklist");
			out.newLine();
			out.write("natural-blocks-whitelist: "+whitelist_natural.toString().substring(1, whitelist_natural.toString().length()-1)+"    #Blocks that will get replaced if replace-natural-only is set to whitelist");
			out.newLine();
			out.write("natural-blocks-blacklist: "+blacklist_natural.toString().substring(1, blacklist_natural.toString().length()-1)+"        #Blocks that will not get replaced if replace-natural-only is set to blacklist");
			out.newLine();
			out.write("drop-replaced-blocks: "+Boolean.toString(drop_blocks_replaced)+"      #gives back a drop when you place a block in an area to be healed");

			//Close the output stream
			out.close();
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Cannot write config file: "+e);
		}
	}

}
