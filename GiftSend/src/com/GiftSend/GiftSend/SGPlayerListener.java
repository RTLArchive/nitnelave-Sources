

package com.GiftSend.GiftSend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;


public class SGPlayerListener extends PlayerListener {
	public static GiftSend plugin;

	public SGPlayerListener(GiftSend instance) {
		plugin = instance;
	}

	public void onPlayerJoin(PlayerJoinEvent event) {
		File offlineFile = new File(plugin.getDataFolder()+"/offline.txt");
		File tempFile = new File(plugin.getDataFolder() + "/offline.tmp");

		if (!tempFile.exists()) {
			try {
				tempFile.createNewFile();
			} catch (IOException e) {
				System.out.println("cannot create temp file "+tempFile.getPath()+"/"+tempFile.getName());
			}
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(offlineFile));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

			String line;
			String[] splittext;

			while((line = br.readLine()) != null) {
				splittext = line.split(":");
				if (splittext[0].equals(event.getPlayer().getName())) {
					if (event.getPlayer().getInventory().firstEmpty() >= 0) {
						int givetypeid = Integer.parseInt(splittext[1]);
						int giveamount = Integer.parseInt(splittext[2]);
						short givedurability = Short.valueOf(splittext[4]);

						event.getPlayer().getInventory().addItem(new ItemStack(givetypeid, giveamount, givedurability));
						//currentinventory.removeItem(new ItemStack(ID,AMOUNT));
						String materialname = Material.getMaterial(givetypeid).toString().toLowerCase().replace("_", " ");
						if (giveamount > 1)
							materialname = materialname+"s";

						event.getPlayer().sendMessage(ChatColor.GREEN+splittext[3]+ChatColor.GRAY+" gave you "+giveamount+" "+ChatColor.RED+materialname);
					}
					else {
						event.getPlayer().sendMessage(ChatColor.GRAY+"You have items sent to you, but your inventory is full.");
						event.getPlayer().sendMessage(ChatColor.GRAY+"Please make space and relog to get your items.");

						pw.println(line);
						pw.flush();
					}
				}
				else {
					pw.println(line);
					pw.flush();
				}
			}

			pw.close();
			br.close();

			offlineFile.delete();
			tempFile.renameTo(offlineFile);

		}
		catch(IOException e) {
			System.out.println("[GiftSend] Offline file read error: "+e);
		}

	}
}

