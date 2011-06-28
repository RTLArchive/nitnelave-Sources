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
		if(event.getEntity()!=null && !event.isCancelled()) {
			if( event.getEntity() instanceof Creeper && plugin.creeper) {
				plugin.recordBlocks(event);
			}
			else if(event.getEntity() instanceof TNTPrimed && plugin.tnt) {
				plugin.recordBlocks(event);
			}
		}
		event.setYield(0);
	}
	
}
