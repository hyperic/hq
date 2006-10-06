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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.Vector;

/**
 * A generic scheduler object which keeps track of events, when they should
 * be executed, deletion after invocation, etc.  The basetime used when
 * doing any arithmetic with times is the epoch.  The scheduler is 
 * synchronized.
 *
 * Scheduled events have an interval property -- how often the event should 
 * execute (in milliseconds).  
 */

public class Schedule {
    private long  _scheduleID;   // Used for assigning unique event IDs
    private List  _schedule;     /* The actual events being scheduled, sorted
                                    by ascending nextTime in the item */

    public Schedule(){
        _schedule   = new ArrayList();
        _scheduleID = 0;
    }

    /**
     * Get the next global schedule identifier, and internally increment it.
     *
     * @return a globally unique identifier, for the next scheduled item.
     */
    private long consumeNextGlobalID(){
        synchronized (_schedule) {
            return _scheduleID++;
        }
    }

    /**
     * Insert a pre-made ScheduledItem into the schedule.  The item should
     * already have all appropriate attributes assigned (including the ID).
     *
     * @param item The item to be inserted into the schedule
     */
    private void insertScheduledItem(ScheduledItem item){
        long nextTime = item.getNextTime();

        synchronized (_schedule) {
            int size = _schedule.size();
            
            for (int i=0; i < size; i++){
                ScheduledItem x = (ScheduledItem)_schedule.get(i);
                
                if (x.getNextTime() > nextTime) {
                    _schedule.add(i, item);
                    return;
                }
            }
            
            // Else add at the end of the vector (time greater than all others)
            _schedule.add(item);
        }
    }

    /**
     * Add an item to the internal schedule.  
     *
     * @param item the object to schedule
     * @param interval the number of seconds between invocations of the item
     * @param prev true if the item should be scheduled in the past to
     *        force immediate firing.
     * @param repeat true if the item should stay in the schedule even after
     *               its time has expired
     *
     * @return a global identifier for the scheduled item
     */
    public long scheduleItem(Object item, long interval, boolean prev, 
                             boolean repeat)
    {
        long itemId;
        
        if (interval <= 0) 
            throw new IllegalArgumentException("Interval must be >= 0");

        itemId  = consumeNextGlobalID();

        insertScheduledItem(new ScheduledItem(item, interval, prev, repeat, 
                                              itemId));
        return itemId;
    }
    
    /**
     * Add an item to the internal schedule.  
     *
     * @param item the object to schedule
     * @param interval the number of seconds between invocations of the item
     * @param repeat true if the item should stay in the schedule even after
     *               its time has expired
     *
     * @return a global identifier for the scheduled item
     */
    public long scheduleItem(Object item, long interval, boolean repeat) {
        return scheduleItem(item, interval, false, repeat);
    }

    /**
     * Add an item to the internal schedule, with the repeat flag set to true.
     * See the documentation for scheduleItem for more information.
     */
    public long scheduleItem(Object item, long interval) {
        return scheduleItem(item, interval, true);
    }

    /**
     * Remove an item from the schedule.  Scheduled items which have not
     * been consumed may be unscheduled by using this method.
     *
     * @param id ID returned by a call to scheduleItem of the item to remove
     *
     * @returns true if the id was found and removed, else false
     */
    public boolean unscheduleItem(long id) {
        int size = _schedule.size();

        synchronized (_schedule) {
            for (int i=0; i < size; i++) {
                ScheduledItem item = (ScheduledItem)_schedule.get(i);
                
                if(item.getId() == id){
                    _schedule.remove(i);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the time that the next scheduled item is to be executed.  The
     * returned time is in UTC since the epoch (similar to 
     * System.currentTimeMillis())
     *
     * @return the absolute time the next event is to be executed, or -1
     *         if the schedule is empty
     */
    public long getTimeOfNext() {
        synchronized (_schedule) {
            ScheduledItem item;
            
            if (_schedule.isEmpty()) 
                return -1;
            
            item = (ScheduledItem)_schedule.get(0);
            return item.getNextTime();
        }
    }

    /**
     * Get the next item (or items) to be executed.  If more than one item
     * is scheduled for a specific time, they are all returned.  Items 
     * returned by this function are re-inserted into the schedule if their
     * repeat flag is set to true -- otherwise they are removed.
     *
     * @return a list of items to execute
     */
    public List consumeNextItems() {
        List res = new ArrayList();
        List toReschedule = new ArrayList();

        synchronized (_schedule) {
            long baseNextTime;

            if (_schedule.isEmpty())
                return res;

            baseNextTime = -1;
            for (Iterator i=_schedule.iterator(); i.hasNext(); ) {
                ScheduledItem item = (ScheduledItem)i.next();
                
                if (baseNextTime == -1 || item.getNextTime() == baseNextTime) {
                    baseNextTime = item.getNextTime();
                    res.add(item.getObj());
                }

                i.remove();
                if (item.isRepeat()) {
                    toReschedule.add(item);
                }
            }
        }

        // Finally, add back in repeatable items which need to be rescheduled
        for (Iterator i=toReschedule.iterator(); i.hasNext(); ) {
            ScheduledItem item = (ScheduledItem)i.next();

            item.stepNextTime();
            insertScheduledItem(item);
        }
        return res;
    }
    
    /**
     * Get the number of items in the schedule.
     *
     * @return the number of items in the schedule.
     */
    public int getNumItems(){
        synchronized (_schedule) {
            return _schedule.size();
        }
    }

    public static void main(String args[]) throws Exception {
        Schedule s = new Schedule();

        s.scheduleItem(new Integer(30), 30 * 1000, true);
        s.scheduleItem(new Integer(5), 5 * 1000, true);
        long id_for_8 = s.scheduleItem(new Integer(8), 8 * 1000, true);
        s.scheduleItem(new Integer(10), 10 * 1000, false);
        int secondsRunning = 0;

        while(true){
            System.out.println(System.currentTimeMillis() / 1000);
            if(System.currentTimeMillis() > s.getTimeOfNext()){
                List items = s.consumeNextItems();

                System.out.println("Consuming");
                for(int i=0; i<items.size(); i++){
                    System.out.println("Consumed: " + (Integer) items.get(i));
                }
            }
            Thread.sleep(1000);
            secondsRunning++;
            if(secondsRunning > 10 && id_for_8 != -1){
                s.unscheduleItem(id_for_8);
                System.out.println("Removing 8");
                id_for_8 = -1;
            }
            if(secondsRunning > 20)
                System.exit(0);
        }
    }
}
