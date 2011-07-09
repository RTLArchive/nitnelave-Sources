package com.nitnelave.CreeperHeal;



import org.bukkit.entity.Creeper;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;


public class CreeperListener extends EntityListener{

	private static CreeperHeal plugin;

	
	public CreeperListener(CreeperHeal instance)		//declaration of the plugin dependence, or something like that
	{
		plugin = instance;

	}
	
	public void onEntityExplode(EntityExplodeEvent event) {//explosion
		
		if(event.getEntity()!=null && !event.isCancelled()) {		//if there actually is an explosion, and an entity causing the explosion
			if( event.getEntity() instanceof Creeper && plugin.creeper) 		//if it's a creeper, and creeper explosions are recorded
				plugin.recordBlocks(event);			//record the blocks destroyed
			else if(event.getEntity() instanceof TNTPrimed && plugin.tnt) 				//tnt
				plugin.recordBlocks(event);
			else if(event.getEntity() instanceof Fireball && plugin.replace_ghast) 		//fireballs (shot by ghasts)
				plugin.recordBlocks(event);
			else if(!(event.getEntity() instanceof Creeper) && !(event.getEntity() instanceof TNTPrimed) && !(event.getEntity() instanceof Fireball) && plugin.replace_other)		//none of it, another custom entity
				plugin.recordBlocks(event);
		}
	}
	
}
