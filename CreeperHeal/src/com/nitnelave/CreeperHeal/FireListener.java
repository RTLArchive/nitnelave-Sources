package com.nitnelave.CreeperHeal;



import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;


public class FireListener extends BlockListener{

	private static CreeperHeal plugin;

	
	public FireListener(CreeperHeal instance)
	{
		plugin = instance;

	}
	
	public void onBlockBurn(BlockBurnEvent event) {
		plugin.record_burn(event.getBlock());
	}
	
}
