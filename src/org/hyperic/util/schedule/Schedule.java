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
import java.util.List;
import java.util.Vector;

import java.text.DateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private long   scheduleID;   // Used for assigning unique event IDs
    private Vector schedule;     // The actual events being scheduled, sorted
                                 // by ascending nextTime in the item

    private Log log = LogFactory.getLog(Schedule.class);
    private Log traceLog = LogFactory.getLog(Schedule.class.getName()+"Trace");

    public Schedule(){
        this.schedule   = new Vector();
        this.scheduleID = 0;
    }

    /**
     * Get the next global schedule identifier, and internally increment it.
     *
     * @return a globally unique identifier, for the next scheduled item.
     */

    private long consumeNextGlobalID(){
        return this.scheduleID++;
    }

    /**
     * Insert a pre-made ScheduledItem into the schedule.  The item should
     * already have all appropriate attributes assigned (including the ID).
     *
     * @param item The item to be inserted into the schedule
     */

    private void insertScheduledItem(ScheduledItem item){
        int i, size = this.schedule.size();
        long nextTime = item.getNextTime();
        ScheduledItem x;

        for(i=0; i<size; i++){
            x = (ScheduledItem) this.schedule.get(i);
            if(x.getNextTime() > nextTime){
                this.schedule.add(i, item);
                return;
            }
        }
        // Else add at the end of the vector (time greater than all others)
        this.schedule.add(item);
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
     * @throws UnscheduledItemException If the given schedule interval is <= 0
     *
     * @return a global identifier for the scheduled item
     */

    public synchronized long scheduleItem(Object item, long interval, 
                                          boolean prev, boolean repeat)
        throws ScheduleException
    {
        long itemId;
        ScheduledItem newItem;
        
        if (interval <= 0) {
            throw new ScheduleException("Invalid schedule interval given (" +
                                        interval + ")");
        }

        itemId      = this.consumeNextGlobalID();
        newItem     = new ScheduledItem(item, interval, prev,
                                        repeat, itemId);
        this.insertScheduledItem(newItem);
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

    public synchronized long scheduleItem(Object item, long interval, 
                                          boolean repeat)
        throws ScheduleException
    {
        return this.scheduleItem(item, interval, false, repeat);
    }

    /**
     * Add an item to the internal schedule, with the repeat flag set to true.
     * See the documentation for scheduleItem for more information.
     */

    public long scheduleItem(Object item, long interval)
        throws ScheduleException
    {
        return this.scheduleItem(item, interval, true);
    }

    /**
     * Remove an item from the schedule.  Scheduled items which have not
     * been consumed may be unscheduled by using this method.
     *
     * @param id ID returned by a call to scheduleItem of the item to remove
     *
     * @throws UnscheduledItemException indicating the ID was not found.
     */

    public synchronized ScheduledItem unscheduleItem(long id) 
        throws UnscheduledItemException 
    {
        int i, size = this.schedule.size();

        for(i=0; i<size; i++){
            ScheduledItem item = (ScheduledItem) this.schedule.get(i);

            if(item.getId() == id){
                if (log.isDebugEnabled()) {
                    log.debug("unscheduling "+item.getObj()+" getNextTime "+
                    getDateStr(item.getNextTime()));
                }
                return (ScheduledItem)this.schedule.remove(i);
            }
        }

        throw new UnscheduledItemException("id '" + id + "' not found");
    }

    /**
     * Get the time that the next scheduled item is to be executed.  The
     * returned time is in UTC since the epoch (similar to 
     * System.currentTimeMillis())
     *
     * @return the absolute the the next event is to be executed
     * 
     * @throws EmptyScheduleException indicating there is no next item for
     *                                which the time can be retrieved.
     */

    public synchronized long getTimeOfNext() 
        throws EmptyScheduleException 
    {
        int size = this.schedule.size();
        ScheduledItem item;

        if(size == 0)
            throw new EmptyScheduleException();

        item = (ScheduledItem) this.schedule.get(0);
        return item.getNextTime();
    }

    /**
     * Get the next item (or items) to be executed.  If more than one item
     * is scheduled for a specific time, they are all returned.  Items 
     * returned by this function are re-inserted into the schedule, if their
     * repeat flag is set to true -- otherwise they are removed.
     *
     * @return a list of items to execute
     *
     * @throws EmptyScheduleException indicating there was no 'next item'
     */

    public synchronized List consumeNextItems() 
        throws EmptyScheduleException 
    {
        int size = this.schedule.size();
        ScheduledItem base;
        ArrayList res;
        long baseNextTime;

        if(size == 0)
            throw new EmptyScheduleException();

        res = new ArrayList(1);

        // We always add the first item to the list of returned objects
        base = (ScheduledItem) this.schedule.get(0);
        baseNextTime = System.currentTimeMillis();
        res.add(base);

        boolean debug = traceLog.isDebugEnabled();
        // Now add other items if they occur at the same time 
        for(int i=1; i<size; i++){
            ScheduledItem other = (ScheduledItem) this.schedule.get(i);
            if (debug) {
                traceLog.debug("checking "+other.getObj()+" baseNextTime: "+
                    getDateStr(baseNextTime)+", getNextTime: "+
                    getDateStr(other.getNextTime()));
            }

            if(other.getNextTime() <= baseNextTime){
                res.add(other);
            } else {
                break;
            }
        }

        // Finally, loop through the objects we are about to return, so
        // we can re-order our innards, and return the actual objects
        // stored instead of the ScheduledItem
        // XXX -- This could be MUCH more efficient, especially with respect
        //        to re-inserting into the list, since all of the returned
        //        objects are of the same size.
        for(int i=0; i<res.size(); i++){
            ScheduledItem other = (ScheduledItem) res.get(i);

            if (debug) {
                traceLog.debug("removing "+other.getObj());
            }
            this.schedule.remove(other);
            if(other.isRepeat()){
                other.stepNextTime();
                if (debug) {
                    traceLog.debug("adding "+other.getObj()+" getNextTime "+
                        getDateStr(other.getNextTime()));
                }
                this.insertScheduledItem(other);
            }

            res.set(i, other.getObj());
        }
        return res;
    }

    private static String getDateStr(long timems) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                              DateFormat.SHORT).
            format(new java.util.Date(timems));
    }
    
    /**
     * Get the number of items in the schedule.
     *
     * @return the number of items in the schedule.
     */

    public int getNumItems(){
        return this.schedule.size();
    }

    /**
     * Get a list of all the currently scheduled items.
     *
     * @return the list of scheduled items.
     */

    public ScheduledItem[] getScheduledItems(){
        return (ScheduledItem[]) this.schedule.toArray(new ScheduledItem[0]);
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
