package com.nitnelave.CreeperHeal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;



public class CreeperHeal extends JavaPlugin {
	Logger log;
	HashMap<Date, List<BlockState>> map = new HashMap<Date, List<BlockState>>();
	Date interval;
	private CreeperListener listener;
	private int log_level = 0;
	HashMap<Location, ItemStack[]> chest_contents = new HashMap<Location, ItemStack[]>();
	HashMap<Location, String[]> sign_text = new HashMap<Location, String[]>();
	boolean creeper = true;
	boolean tnt = false;

	public void onEnable() {
		log = Logger.getLogger("Minecraft");

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
				log.warning("[CreeperHeal] Cannot create file "+yml.getPath());
			}

			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(yml, true));


				out.write("refresh-frequency: 20      #in seconds");
				out.newLine();
				out.write("interval: 60      #in seconds, how long you have to wait before the damage is undone");
				out.newLine();
				out.write("log-level: 0      #0-2 0:silent, 2:verbose");
				out.newLine();
				out.write("TNT: false    #replaces TNT damage");
				out.newLine();
				out.write("Creepers: true    #replaces Creeper damage");

				//Close the output stream
				out.close();
			}
			catch (Exception e) {
				log.warning("[CreeperHeal] Cannot write config file: "+e);
			}
		}

		listener = new CreeperListener(this);
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, listener, Event.Priority.Normal, this);

		PluginDescriptionFile pdfFile = this.getDescription();


		int period = 0;
		try {
			period = getConfiguration().getInt("refresh-frequency", 20);
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for refill-frequency field. Defaulting to 20 seconds");
			period = 20;
		}
		int interval_date = 0;
		try {
			interval_date = getConfiguration().getInt("interval", 60);
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for interval field. Defaulting to 1 minute");
			interval_date = 60;
		}
		interval = new Date();
		interval.setTime(interval_date*1000);
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

		if( getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			public void run() {
				check_replace();
			}}, 200, period * 20) == -1)
			log.warning("[CreeperHeal] Impossible to schedule the re-filling task. Auto-refill will not work");

		log.info("[CreeperHeal] version "+pdfFile.getVersion()+" by nitnelave is enabled");
	}
	public void onDisable() {
		log.info("[CreeperHeal] Disabled");
	}

	public void recordBlocks(EntityExplodeEvent event) {
		event.setYield(0);
		List<Block> list = event.blockList();
		List<BlockState> list_state = new ArrayList<BlockState>();
		for(Block block : list){
			list_state.add(block.getState());
			if(block.getState() instanceof ContainerBlock) {
				chest_contents.put(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()), ((ContainerBlock) block.getState()).getInventory().getContents().clone());
				((ContainerBlock) block.getState()).getInventory().clear();
			}
			else if(block.getState() instanceof Sign) {
				sign_text.put(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()), ((Sign)block.getState()).getLines());
			}
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
					for(BlockState block : map.get(time)){
						if (block.getTypeId() == 12 || block.getTypeId() == 13) {
							int k = 1;
							while(block.getY() + k < 128 && block.getWorld().getBlockAt(block.getX(), block.getY() + k, block.getZ()).getTypeId() != 0) {
								k++;
								block.getBlock().setTypeId(block.getTypeId());
							}
						}

						if(block.getType() != Material.WOODEN_DOOR && block.getType() != Material.IRON_DOOR_BLOCK && block.getType() != Material.BED_BLOCK) {
							block.getBlock().setTypeIdAndData(block.getTypeId(), block.getRawData(), true);
						}
						else if ((block.getType() == Material.WOODEN_DOOR || block.getType() == Material.IRON_DOOR_BLOCK) && block.getRawData() < 8) {
							block.getBlock().setTypeIdAndData(block.getTypeId(), block.getRawData(), false);
							block.getBlock().getFace(BlockFace.UP).setTypeIdAndData(block.getTypeId(), (byte)(block.getRawData() + 8), false);
						}
						else if(block.getType() == Material.BED_BLOCK && block.getRawData() < 8) {
							byte data = block.getRawData();
							block.getBlock().setTypeIdAndData(26, data, false);
							BlockFace face = null;
							if(data == 0)
								face = BlockFace.WEST;
							else if(data == 1)
								face = BlockFace.NORTH;
							else if(data == 2)
								face = BlockFace.EAST;
							else
								face = BlockFace.SOUTH;
							block.getBlock().getFace(face).setTypeIdAndData(26, (byte)(block.getRawData() + 8), false);
						}
						if(block instanceof ContainerBlock) {
							((ContainerBlock) block.getBlock().getState()).getInventory().setContents(chest_contents.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ())));
							block.update();
							chest_contents.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
						}
						if(block instanceof Sign) {
							int k = 0;
							for(String line : sign_text.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()))) {
								((Sign) block.getBlock().getState()).setLine(k, line);
								k++;
							}
							sign_text.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
						}


					}
					iterator.remove();
					log_info("Blocks replaced!", 1);
				}


			}
		}
	}

	public void log_info(String msg, int min_level) {
		if(min_level<=log_level)
			log.info(msg);
	}


}
