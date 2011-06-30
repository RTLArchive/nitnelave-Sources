package com.nitnelave.CreeperHeal;

import java.util.*;

import org.bukkit.block.Block;

public class CreeperComparator implements Comparator<Block>{
	
	public int compare(Block b1, Block b2) {
		
		int pos1 = b1.getY();
		int pos2 = b2.getY();
		
		if(pos1 > pos2)
			return 1;
		else if(pos1<pos2)
			return -1;
		else
			return 0;
		
		
	}

}
