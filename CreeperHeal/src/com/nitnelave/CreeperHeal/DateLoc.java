package com.nitnelave.CreeperHeal;

import java.util.Date;

import org.bukkit.Location;



public class DateLoc {
    Date date;
    Location location;

    public DateLoc(Date time, Location loc) {
        date = time;
        location = loc;
    }

    public Date getTime() {
        return date;
    }

    public Location getLocation() {
        return location;
    }

}