package com.nitnelave.CreeperHeal;



import org.bukkit.entity.Creeper;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;


public class CreeperListener extends EntityListener{

	private static CreeperHeal plugin;

	
	public CreeperListener(CreeperHeal instance)
	{
		plugin = instance;

	}
	
	public void onEntityExplode(EntityExplodeEvent event) {
		if(event.getEntity()!=null) {
			if( event.getEntity() instanceof Creeper && plugin.creeper) {
				plugin.recordBlocks(event);
				event.blockList().clear();
			}
			else if(event.getEntity() instanceof TNTPrimed && plugin.tnt) {
				plugin.recordBlocks(event);
				event.blockList().clear();
			}
		}
	}
	
}
