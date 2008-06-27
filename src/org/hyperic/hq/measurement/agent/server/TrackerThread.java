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

package org.hyperic.hq.measurement.agent.server;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.measurement.data.TrackEventReport;
import org.hyperic.hq.product.ConfigTrackPluginManager;
import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.product.TrackEventPluginManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class TrackerThread implements Runnable {
    
    private static final String PROP_MAXEVENTBATCHSIZE  = 
        "agent.eventReportBatchSize";
    
    private static final int    MAX_EVENT_BATCHSIZE = 100;
    private static final String CONFIGTRACK_LISTNAME = "configtrack_spool";
    private static final String LOGTRACK_LISTNAME    = "logtrack_spool";
    private static final int    LOGTRACK_RECSIZE     = 4096;

    private volatile boolean   shouldDie;
    private volatile Thread    myThread;

    private Object             interrupter;
    private long               waitTime;
    private int                maxEventBatchSize = MAX_EVENT_BATCHSIZE;
    
    private AgentStorageProvider      storage;
    private MeasurementCallbackClient client;

    private ConfigTrackPluginManager  ctManager;
    private LogTrackPluginManager     ltManager;

    private Log log;
        
    TrackerThread(ConfigTrackPluginManager ctManager, 
                  LogTrackPluginManager ltManager, 
                  AgentStorageProvider storage,
                  Properties bootProps)
        throws AgentStartException
    {
        this.ctManager   = ctManager;
        this.ltManager   = ltManager;
        this.storage     = storage;
        this.shouldDie   = false;
        this.myThread    = null;
        this.interrupter = new Object();
        this.client      = setupClient();
        this.waitTime    = TrackEventPluginManager.DEFAULT_INTERVAL;
        this.log         = LogFactory.getLog(TrackerThread.class);
        String info = bootProps.getProperty(CONFIGTRACK_LISTNAME);
        if (info != null) {
            storage.addOverloadedInfo(CONFIGTRACK_LISTNAME, info);
        }
        info = bootProps.getProperty(LOGTRACK_LISTNAME);
        if (info != null) {
            storage.addOverloadedInfo(LOGTRACK_LISTNAME, info);
        }

        // Create list early since we want a larger recordsize than the default of 1k.
        try {
            this.storage.createList(LOGTRACK_LISTNAME, LOGTRACK_RECSIZE);
        } catch (AgentStorageException ignore) {
            // Most likely an agent update where the existing spool
            // already exists.  Will fall back to the old 1k size.
        }

        String sMaxBatchSize = bootProps.getProperty(PROP_MAXEVENTBATCHSIZE);
        
        if(sMaxBatchSize != null){
            try {
                this.maxEventBatchSize = Integer.parseInt(sMaxBatchSize);
            } catch(NumberFormatException exc){
                throw new AgentStartException(PROP_MAXEVENTBATCHSIZE + " is not " +
                                              "a valid integer ('" + 
                                              sMaxBatchSize + "')");
            }

        }
        
        this.log.info("Event report batch size set to " + this.maxEventBatchSize);
    }

    private void interruptMe()
    {
        synchronized(this.interrupter) {
            this.interrupter.notify();
        }
    }
 
    private MeasurementCallbackClient setupClient()
        throws AgentStartException 
    {
        StorageProviderFetcher fetcher;

        fetcher = new StorageProviderFetcher(this.storage);
        return new MeasurementCallbackClient(fetcher);
    }

    private void flushEvents(LinkedList events, String dListName)
    {
        if (events.isEmpty()) {
            return;
        }

        for(Iterator i = events.iterator(); i.hasNext(); ) {
            TrackEvent event = (TrackEvent)i.next();

            try {
                String data = event.encode();
                this.storage.addToList(dListName, data);
                this.log.debug("Stored event (" + data.length() +
                               " bytes) " + event);
            } catch (Exception e) {
                this.log.error("Unable to store data", e);
            }
        }
    }

    private void flushLogTrackEvents()
    {
        LinkedList events = ltManager.getEvents();
        flushEvents(events, LOGTRACK_LISTNAME);
    }

    private void flushConfigTrackEvents()
    {
        LinkedList events = ctManager.getEvents();
        flushEvents(events, CONFIGTRACK_LISTNAME);
    }

    private void processDlist(String dListName)
    {
        boolean moreEventsToProcess = true;
        
        while (moreEventsToProcess) {
            moreEventsToProcess = 
                processNextEventReport(dListName, maxEventBatchSize);
        }
                
        try {
            this.storage.flush();
        } catch(AgentStorageException exc){
            this.log.error("Failed to flush agent storage", exc);
        }
    }

    /**
     * Process the next event report.
     * 
     * @param dListName
     * @param batchSize The event batch size per report.
     * @return <code>true</code> if there are more events to process.
     */
    private boolean processNextEventReport(String dListName, int batchSize) {
        boolean moreEventsToProcess = false;
                
        Iterator i = this.storage.getListIterator(dListName);
        
        if (i == null) {
            return false;
        }
        
        TrackEventReport report = new TrackEventReport();
        int numEventsProcessed = 0;
        
        while (numEventsProcessed <= batchSize && i.hasNext()) {
            TrackEvent event;

            try {
                event = TrackEvent.decode((String)i.next());
            } catch (Exception e) {
                this.log.error("Unable to decode record -- deleting: " + e);
                continue;
            } finally {
                numEventsProcessed++;
                moreEventsToProcess = i.hasNext();
            }

            this.log.debug("Adding event to report=" + event);

            report.addEvent(event);
        }
        
        if (!sendReportToServer(dListName, report)) {
            // If there is an error sending to the server, try again later.
            moreEventsToProcess = false;
        }

        removeProcessedEventsFromStorage(dListName, numEventsProcessed);
        
        return moreEventsToProcess;
    }

    private boolean sendReportToServer(String dListName,
                                       TrackEventReport report) {
        
        boolean succeeded = true;
        
        if (report.getEvents().length > 0) {
            this.log.debug("Sending report to server");
            
            try {
                if (dListName.equals(LOGTRACK_LISTNAME)) {
                    this.client.trackSendLog(report);
                } else if (dListName.equals(CONFIGTRACK_LISTNAME)) {
                    this.client.trackSendConfigChange(report);
                } else {
                    throw new IllegalArgumentException("Unknown DList name");
                }
                
                this.log.debug("Completed sending report to server");            
            } catch (AgentCallbackClientException e) {
                this.log.error("Error sending report to server: " + e);
                succeeded = false;
            }            
        }
        
        return succeeded;
    }

    private void removeProcessedEventsFromStorage(String dListName, 
                                                  int numEventsProcessed) {
        for(Iterator i= this.storage.getListIterator(dListName);
            numEventsProcessed > 0 && i != null && i.hasNext(); 
            numEventsProcessed--) {
            i.next();
            i.remove();
        }
    }

    public void die() 
    {
        // Before exiting, make sure we flush any outstanding events.
        flushLogTrackEvents();
        flushConfigTrackEvents();

        this.shouldDie = true;
        this.interruptMe();
    }

    public void run() 
    {
        this.myThread = Thread.currentThread();

        while(this.shouldDie == false) {
            // Flush log track events
            flushLogTrackEvents();

            // Flush config track events
            flushConfigTrackEvents();

            // Process the log track dlist
            processDlist(LOGTRACK_LISTNAME);

            // Process the config track dlist
            processDlist(CONFIGTRACK_LISTNAME);
            
            synchronized(this.interrupter) {
                try {
                    this.interrupter.wait(waitTime);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
