package com.nitnelave.CreeperHeal;



import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;


public class FireListener extends BlockListener{

	private static CreeperHeal plugin;

	
	public FireListener(CreeperHeal instance)
	{
		plugin = instance;

	}
	
	public void onBlockBurn(BlockBurnEvent event) {		//no need to check for the setting, the listener only gets declared if it is set to true
		plugin.record_burn(event.getBlock());
	}
	
}
