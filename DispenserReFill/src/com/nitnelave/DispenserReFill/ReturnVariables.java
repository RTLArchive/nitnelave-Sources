package com.nitnelave.DispenserReFill;

import org.bukkit.inventory.Inventory;

public class ReturnVariables {
    protected Inventory inventory;  
    protected int amount;  
   
    public Inventory getInventory(){  
        return inventory;  
    }  
   
    public int getAmount(){  
        return amount;  
    }  
   
    public void setInventory(Inventory inv){  
        this.inventory = inv;  
        return;  
    }  
   
    public void setAmount(int amo){  
        this.amount = amo;  
        return;  
    }  
}
