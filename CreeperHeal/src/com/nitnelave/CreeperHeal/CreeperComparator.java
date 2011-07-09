package com.nitnelave.CreeperHeal;

import java.util.*;

import org.bukkit.block.BlockState;

public class CreeperComparator implements Comparator<BlockState>{		//used to sort blocks from bottom to top
	
	public int compare(BlockState b1, BlockState b2) {
		
		int pos1 = b1.getY();		//altitude of block one
		int pos2 = b2.getY();		//altitude of block two
		
		if(pos1 > pos2)
			return 1;
		else if(pos1<pos2)
			return -1;
		else
			return 0;
		
		
	}

}
