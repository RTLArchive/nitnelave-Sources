package com.nitnelave.CreeperHeal;

import java.util.ArrayList;
import java.util.Arrays;


public class WorldConfig {

	public boolean creepers, tnt, fire, ghast, magical, replace_tnt;
	public String restrict_blocks, name;
	public ArrayList<Integer> block_list = new ArrayList<Integer>();


	public WorldConfig(String world_name) {
		creepers = tnt = ghast = fire = true;
		magical = replace_tnt = false;
		restrict_blocks = "false";
		block_list = new ArrayList<Integer>(Arrays.asList(1,2,3,9,11,12,13,14,15,16,17,18,21,24,31,32,37,38,39,40,48,49,56,73,79,81,82,86,87,88,89));        //sample whitelist
		name = world_name;

	}

	public WorldConfig(String world_name, boolean tmp_creeper, boolean tmp_tnt, boolean tmp_ghast, boolean tmp_fire, boolean tmp_magical, boolean tmp_replace_tnt, String restrictBlocks, ArrayList<Integer> blockList) {

		creepers = tmp_creeper;

		tnt = tmp_tnt;

		ghast = tmp_ghast;

		fire = tmp_fire;

		magical = tmp_magical;

		replace_tnt = tmp_replace_tnt;

		restrict_blocks = restrictBlocks;

		block_list = new ArrayList<Integer>(blockList);

		name = world_name;

	}

	public ArrayList<Object> getConfig() {

		String blocklist = block_list.toString().substring(1, block_list.toString().length() - 1);

		ArrayList<Object> list = new ArrayList<Object>();
		list.add(creepers);
		list.add(tnt);
		list.add(ghast);
		list.add(magical);
		list.add(fire);
		list.add(restrict_blocks);
		list.add(blocklist);
		list.add(replace_tnt);
		
		return list;
		
	}

	public String getName() {
		return name;
	}

}