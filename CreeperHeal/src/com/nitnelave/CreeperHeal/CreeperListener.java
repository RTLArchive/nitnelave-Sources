package com.nitnelave.CreeperHeal;



import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.ExplosionPrimeEvent;


public class CreeperListener extends EntityListener{

	private static CreeperHeal plugin;


	public CreeperListener(CreeperHeal instance)        //declaration of the plugin dependence, or something like that
	{
		plugin = instance;

	}

	public void onEntityExplode(EntityExplodeEvent event) {//explosion
		WorldConfig world = getWorld(event.getLocation().getWorld());
		if(!event.isCancelled()) {        //if there actually is an explosion
			Entity entity = event.getEntity();
			if(entity != null) {

				if( entity instanceof Creeper && world.creepers)         //if it's a creeper, and creeper explosions are recorded

					recordBlocks(event, world);            //record the blocks destroyed

				else if(entity instanceof TNTPrimed && world.tnt)                 //tnt

					recordBlocks(event, world);

				else if(entity instanceof Fireball && world.ghast)         //fireballs (shot by ghasts)

					recordBlocks(event, world);

				else if(!(entity instanceof Creeper) && !(entity instanceof TNTPrimed) && !(entity instanceof Fireball) && world.magical)        //none of it, another custom entity

					recordBlocks(event, world);

			}

			else if(world.magical) {

				recordBlocks(event, world);

			}           
		}
	}

	private void recordBlocks(EntityExplodeEvent event, WorldConfig world) {
		plugin.recordBlocks(event, world);
	}

	private WorldConfig getWorld(World w) {
		return plugin.world_config.get(w.getName());
	}


	public void onExplosionPrime( ExplosionPrimeEvent event) {
		if(event.getEntity() instanceof TNTPrimed)
			plugin.storeTNT(event);
	}

}
