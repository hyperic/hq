package org.hyperic.hq.measurement.agent.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_args;
import org.hyperic.hq.plugin.system.TopData;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.sigar.Humidor;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.schedule.EmptyScheduleException;
import org.hyperic.util.schedule.Schedule;
import org.hyperic.util.schedule.ScheduleException;
import org.hyperic.util.schedule.UnscheduledItemException;


class TOPNScheduleThread implements Runnable {


    private final          Schedule   schedule;    // Internal schedule of DSNs, etc
    private volatile boolean    shouldDie;   // Should I shut down?
    private volatile Thread     myThread;    // Thread the 'run' is running in
    private final          Object     interrupter; // Interrupt object

    private final AgentStorageProvider storage;       // Place to store data
    private final Log                  log;
    private final TOPNSenderThread sender; // Guy handling the results
    private Sigar _sigarImpl;
    private Humidor _humidor;

    TOPNScheduleThread(TOPNSenderThread sender, AgentStorageProvider storage) throws AgentStartException {
        this.schedule        = new Schedule();
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

    void unscheduleRt(ScheduleTopn_args args) throws UnscheduledItemException {

    }


    void scheduleRt(ScheduleTopn_args args) {

        long oldNextTime, newNextTime = 0;

        try {
            oldNextTime = this.schedule.getTimeOfNext();
        } catch (EmptyScheduleException e) {
            oldNextTime = 0;
        }
        try {
            this.schedule.scheduleItem(args, args.getInterval() * 60 * 1000);
        } catch (ScheduleException e) {
            throw new AgentAssertionException(e.getMessage());
        }
        try {
            newNextTime = this.schedule.getTimeOfNext();
        } catch (EmptyScheduleException e) {
            throw new AgentAssertionException("Schedule should have at " + "least one entry: " + e.getMessage());
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
        if (_sigarImpl != null) {
            _sigarImpl.close();
            _sigarImpl = null;
        }
    }

    /**
     * The main loop of the RtScheduleThread, which watches the schedule
     * waits the appropriate time, and executes scheduled operations.
     */

    public void run(){
        this.myThread = Thread.currentThread();

        while(this.shouldDie == false){
            long timeToNext = -1;
            try {
                timeToNext = this.schedule.getTimeOfNext() -
                        System.currentTimeMillis();

            } catch(EmptyScheduleException exc){
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
            if (this.schedule.getNumItems() == 0) {
                continue;
            }
            ScheduleTopn_args args = (ScheduleTopn_args) this.schedule.getScheduledItems()[0].getObj();
            ConfigResponse config = args.getConfig();
            if (null == config) {
                config = new ConfigResponse();
            }
            String filter = config.getValue(SigarMeasurementPlugin.PTQL_CONFIG);

            TopData data = null;
            try {
                data = TopData.gather(getSigar(), filter);
            } catch (SigarException e) {

            }
            if (null != data) {
                this.sender.processData(data);
            }

        }
    }

    private synchronized SigarProxy getSigar() {
        if (_humidor == null) {
            _sigarImpl = new Sigar();
            _humidor = new Humidor(_sigarImpl);
        }
        return _humidor.getSigar();
    }

}
