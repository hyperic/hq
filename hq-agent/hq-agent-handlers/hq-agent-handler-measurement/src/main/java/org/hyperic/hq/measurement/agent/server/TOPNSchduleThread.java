package org.hyperic.hq.measurement.agent.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.RtPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.encoding.Base64;
import org.hyperic.util.schedule.EmptyScheduleException;
import org.hyperic.util.schedule.Schedule;
import org.hyperic.util.schedule.ScheduleException;
import org.hyperic.util.schedule.UnscheduledItemException;

/**
 * The schedule thread which maintains the schedule, and dispatches on them.
 * After data is retrieved, it is sent to the SenderThread which handles
 * depositing the results on disk, and sending them to the bizapp.
 */

class TOPNScheduleThread implements Runnable {
    // How often we check schedules when we think they are empty.
    private static final  int  POLL_PERIOD      = 60000;
    private static final String PARSED_FILE_KEY = "rt:alreadyparsed:";

    private final          Schedule   schedule;    // Internal schedule of DSNs, etc
    private final          Hashtable  serverIDs;   // Hash of server IDs->schedule IDs
    private volatile boolean    shouldDie;   // Should I shut down?
    private volatile Thread     myThread;    // Thread the 'run' is running in
    private final          Object     interrupter; // Interrupt object

    private final AgentStorageProvider storage;       // Place to store data
    private final Log                  log;
    private final TOPNScheduleThread sender; // Guy handling the results

    TOPNScheduleThread(TOPNScheduleThread sender, AgentStorageProvider storage)
        throws AgentStartException
    {
        this.schedule        = new Schedule();
        this.serverIDs       = new Hashtable();
        this.shouldDie       = false;
        this.myThread        = null;
        this.interrupter     = new Object();
        this.log             = LogFactory.getLog(TOPNScheduleThread.class);
        this.sender          = sender;
        this.storage         = storage;
    }

    private void interruptMe(){
        synchronized(this.interrupter){
            this.interrupter.notify();
        }
    }

    /**
     * Unschedule a previously scheduled RT collection.
     *
     * @param ident Token previously passed to scheduleRt for
     *                 which to delete scheduled operations.
     *
     * @returns true if the item was sucessfully unscheduled
     */
    void unscheduleRt(String ident) throws UnscheduledItemException {
        // Synchronize on serverIDs to make sure someone isn't trying to
        // add to a service ID if we are trying to delete it
        synchronized(this.serverIDs){
            Long mID = (Long) this.serverIDs.get(ident);
            if(mID == null){
                throw new UnscheduledItemException("Client Rt ID, '" +
                                                   ident + "' was not " +
                                                   "found");
            }

            try {
                this.schedule.unscheduleItem(mID.longValue());
            } catch(UnscheduledItemException exc){
                throw new AgentAssertionException("Tried to unschedule " +
                                                  "something which wasn't" +
                                                  " scheduled");
            }
            this.serverIDs.remove(ident);
        }
    }

    /**
     * Schedule a response time collection to be taken at a given interval.
     */
    void scheduleRt(RtPlugin_args args){

        Integer svcID         = args.getID();
        ConfigResponse config = null;
        long oldNextTime, newNextTime = 0;

        // We don't have to worry about the exception.  By the time we get
        // here, we have already done this at least once, so we know we won't
        // trigger the exception.
        try {
            config = args.getConfigResponse();
        } catch (Exception e) { }

        Boolean endUser = args.getEndUser();
        long interval;
        if (endUser.booleanValue()) {
            interval = Long.parseLong(
                               config.getValue(RtPlugin.CONFIG_EUINTERVAL));
        } else {
            interval = Long.parseLong(
                               config.getValue(RtPlugin.CONFIG_INTERVAL));
        }

        synchronized(this.serverIDs){
            long    mID;

            try {
                oldNextTime = this.schedule.getTimeOfNext();
            } catch (EmptyScheduleException e) {
                oldNextTime = 0;
            }
            String ident = svcID.toString() + endUser.toString();

            try {
                mID = this.schedule.scheduleItem(args,
                                                 interval *
                                                 RtConstants.USEC_PER_SEC, true);
            } catch (ScheduleException e) {
                throw new AgentAssertionException(e.getMessage());
            }

            // If this is not repeatable, then we don't allow the
            // client to later unschedule it.

            this.serverIDs.put(ident, new Long(mID));

            try {
                newNextTime = this.schedule.getTimeOfNext();
            } catch (EmptyScheduleException e) {
                throw new AgentAssertionException("Schedule should have at " +
                                                  "least one entry: " +
                                                  e.getMessage());
            }
        }

        // Check to see if we scheduled something sooner than the
        // running thread is expecting
        if(newNextTime < oldNextTime){
            this.interruptMe();
        }
    }

    /**
     * Shut down the schedule thread.
     */

    void die(){
        this.shouldDie = true;
        this.interruptMe();
    }

    /**
     * The main loop of the RtScheduleThread, which watches the schedule
     * waits the appropriate time, and executes scheduled operations.
     */

    public void run(){
        this.myThread = Thread.currentThread();

        while(this.shouldDie == false){
            Collection data = null;
            List items;
            long timeToNext;

            try {
                synchronized(this.serverIDs){
                    timeToNext = this.schedule.getTimeOfNext() -
                        System.currentTimeMillis();
                }
            } catch(EmptyScheduleException exc){
                timeToNext = RtScheduleThread.POLL_PERIOD;
            }

            if(timeToNext > 0){
                this.log.debug("Sleeping " + timeToNext +" to next batch");
                synchronized(this.interrupter){
                    try {
                        this.interrupter.wait(timeToNext);
                    } catch(InterruptedException exc){
                        this.log.debug("Schedule thread kicked");
                    }
                }
            }

            synchronized(this.serverIDs){
                if(this.schedule.getNumItems() == 0) {
                    continue;
                }

                try {
                    items = this.schedule.consumeNextItems();
                } catch(EmptyScheduleException exc){
                    continue;
                }
            }

            for(int i=0; i<items.size(); i++){
                RtPlugin_args scan = (RtPlugin_args)items.get(i);
                boolean success = false;

                try {
                    Properties alreadyParsed = readParsed(scan.getID());
                    boolean collectIPs = loadCollectIPs();
                    data = 
                        this.manager.getTimes(scan.getType(),
                                              scan.getConfigResponse(),
                                              scan.getID(),
                                              alreadyParsed,
                                              scan.getEndUser().booleanValue(),
                                              collectIPs);

                    writeParsed(scan.getID(), alreadyParsed);
                    success = true;
                } catch(PluginNotFoundException exc){
                    exc.printStackTrace();
                } catch(Exception exc){
                    // Don't even let the plugin manager mess with us
                    exc.printStackTrace();
                }
                
                if(success){
                    this.sender.processData(data);
                }
            } 
        }
    }
    
    private Properties readParsed(Integer serviceId)
    {
        String key = PARSED_FILE_KEY + serviceId;
        String serialized = storage.getValue(key);
        Properties alreadyParsed = new Properties();

        if (serialized == null) {
            this.log.warn("No previous log information for service " +
                          "id: " + serviceId);
            return alreadyParsed;
        }

        ByteArrayInputStream bIs = 
            new ByteArrayInputStream(Base64.decode(serialized));

        try {
            alreadyParsed.load(bIs);
        } catch (IOException e) {
            this.log.error("Unable to load already parsed files: " +
                           e.getMessage());
        }

        return alreadyParsed;
    }

    private void writeParsed(Integer serviceId, Properties alreadyParsed)
    {
        String key = PARSED_FILE_KEY + serviceId;
        ByteArrayOutputStream bOs = new ByteArrayOutputStream();

        try {
            alreadyParsed.store(bOs, null);
        } catch (IOException e) { 
            this.log.error("Unable to store already parsed files: " +
                           e.getMessage());
        }
        
        String serialized = Base64.encode(bOs.toByteArray());
        storage.setValue(key, serialized);
    }

    private boolean loadCollectIPs () {
        String val = storage.getValue(RtCommandsService.COLLECTIPS);
        if (val == null) {
            log.warn("Parsing RT data and COLLECTIPS setting was not set."
                     + " Defaulting to true");
            return true;
        }
        return Boolean.valueOf(val).booleanValue();
    }
}
