/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.util.schedule;


public class ScheduledItem {
    private Object  obj;      // Scheduled object
    private long    interval; // Execution interval
    private long    nextTime; // Absolute time the object should be invoked
    private boolean repeat;   // Should this event be repeated?
    private long    id;       // Global schedule ID
    
    ScheduledItem(Object obj, long interval, boolean prev,
                  boolean repeat, long id) 
    {
        this.obj      = obj;
        this.interval = interval;
        this.repeat   = repeat;
        this.id       = id;
        if (prev)
            this.nextTime = ScheduledItem.getScheduledTimePrev(interval);
        else
            this.nextTime = ScheduledItem.getScheduledTime(interval);
    }

    /**
     * The the previous fire time for an item that scheduled
     *
     * @param interval The millisecond interval
     *
     * @return The previous fire time this item should be executed
     */
    public static long getScheduledTimePrev(long interval){
        long currentTime = System.currentTimeMillis();
        return currentTime - (currentTime % interval);
    }
    
    /** 
     * Get the time when something scheduled right now would be
     * executed.
     *
     * @param interval The millisecond interval that would ordinarily be
     *                 passed to a ScheduledItem constructor.
     *
     * @return the time when a scheduled item with the passed interval would
     *         be executed.
     */
    public static long getScheduledTime(long interval){
        long currentTime = System.currentTimeMillis();
        return currentTime + interval - (currentTime % interval);
    }

    public Object getObj() {
        return obj;
    }
    
    public long getInterval() {
        return interval;
    }
    
    public long getNextTime() {
        return nextTime;
    }
        
    public boolean isRepeat() {
        return repeat;
    }
    
    public long getId() {
        return id;
    }
    
    /**
     * Step the nextTime attribute to the current nextTime plus the 
     * interval for which the object should repeat.
     */
    public void stepNextTime(){
        long curTime = System.currentTimeMillis();
        
        this.nextTime += this.interval;
        // Somehow the clock jumped (laptop was suspended?), or we got really 
        // far behind.  Jump up to the next slot  
        if (this.nextTime < curTime) {
            this.nextTime = getScheduledTime(this.interval);
        }
    }
}
