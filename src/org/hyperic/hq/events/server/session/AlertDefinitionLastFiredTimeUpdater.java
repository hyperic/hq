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

package org.hyperic.hq.events.server.session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.ShutdownCallback;
import org.hyperic.hq.events.AlertDefinitionLastFiredUpdateEvent;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.QueuedExecutor;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

/**
 * A singleton that queues and processes 
 * {@link AlertDefinitionLastFiredUpdateEvent AlertDefinitionLastFiredUpdateEvents} 
 * by updating each alert definition last fired time associated with each 
 * enqueued event.
 */
public class AlertDefinitionLastFiredTimeUpdater implements ShutdownCallback {

    private static final AlertDefinitionLastFiredTimeUpdater UPDATER = 
            new AlertDefinitionLastFiredTimeUpdater();
        
    // The LRU cache size - we don't need an aggressive cache so 100 is 
    // an ok default value
    private static final int MAX_ENTRIES = 
        Integer.getInteger("org.hq.alertdef.updater.max.cache.size", 100).intValue();
    
    private static final Log log = 
        LogFactory.getLog(AlertDefinitionLastFiredTimeUpdater.class);
    
    private final QueuedExecutor executor;
    
    private final Map alertDefId2LastFiredTimeCache;
    
    
    private AlertDefinitionLastFiredTimeUpdater() {
        executor = new QueuedExecutor(new LinkedQueue());
        
        executor.setThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "AlertDef LastFired Updater");
                thread.setDaemon(true);
                return thread;
            }});
                
        // warm up the executor
        executor.restart();
        
        alertDefId2LastFiredTimeCache = new LinkedHashMap(16, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }
    
    /**
     * @return The instance of this singleton.
     */
    public static AlertDefinitionLastFiredTimeUpdater getInstance() {
        return UPDATER;
    }
    
    /**
     * Enqueue the events for processing. We are making the optimizing assumption 
     * that all the events in the collection are of type 
     * {@link AlertDefinitionLastFiredUpdateEvent AlertDefinitionLastFiredUpdateEvent}. 
     * It is up to the client to enforce this precondition.
     * 
     * @param events The events to process.
     * @throws InterruptedException if event enqueuing has been interrupted.
     */
    public void enqueueEvents(final Collection events) throws InterruptedException {
        executor.execute(new Updater(events, alertDefId2LastFiredTimeCache));       
    }
    
    /**
     * The shutdown hook invokes this callback.
     */
    public void shutdown() {
        // Force the shutdown since we can't be guaranteed that the alert def
        // session bean will be operational on shutdown.
        shutdown(true);
    }
    
    /**
     * Shutdown the updater.
     * 
     * @param force <code>true</code> to force shutdown immediately; 
     *              <code>false</code> to process the events currently enqueued.
     */
    public void shutdown(boolean force) {        
        if (force) {
            executor.shutdownNow();
        } else {
            executor.shutdownAfterProcessingCurrentlyQueuedTasks();            
        }
    }
    
    /**
     * The class that does all the work!
     */
    private static class Updater implements Runnable {

        private final Collection _events;
        private final Map _alertDefId2LastFiredTimeCache;
        
        public Updater(Collection events, Map alertDefId2LastFiredTimeCache) {
            _events = events;
            _alertDefId2LastFiredTimeCache = alertDefId2LastFiredTimeCache;
        }
        
        public void run() {
            // throw out any events with old alert def last fired times
            AlertDefinitionLastFiredUpdateEvent[] updateEvents = getRelevantEvents();

            tryUpdateAlertDefsLastFiredTimes(updateEvents);
        }
        
        private AlertDefinitionLastFiredUpdateEvent[] getRelevantEvents() {
            Set toUpdate = new HashSet();
            
            final Map cache = _alertDefId2LastFiredTimeCache;
            
            // order the events, latest timestamp first
            Set orderedEvents = new TreeSet(Collections.reverseOrder());
            orderedEvents.addAll(_events);
            _events.clear();     
            
            for (Iterator iter = orderedEvents.iterator(); iter.hasNext();) {
                AlertDefinitionLastFiredUpdateEvent event = 
                    (AlertDefinitionLastFiredUpdateEvent) iter.next();
                
                Long currLastFiredTime = (Long)cache.get(event.getAlertDefinitionId());
                
                if (currLastFiredTime == null || 
                    currLastFiredTime.longValue() < event.getLastFiredTime()) {
                    
                    cache.put(event.getAlertDefinitionId(), 
                              new Long(event.getLastFiredTime()));
                    
                    toUpdate.add(event);
                }
            }
            
            return (AlertDefinitionLastFiredUpdateEvent[]) 
                    toUpdate.toArray(
                      new AlertDefinitionLastFiredUpdateEvent[toUpdate.size()]);
        }
        
        /**
         * Update the alert def last fired times within an EJB call to start a 
         * transactional context. Use exponential backoff for failed updates.
         * 
         * @param updateEvents The update events.
         */
        private void tryUpdateAlertDefsLastFiredTimes(
                AlertDefinitionLastFiredUpdateEvent[] updateEvents) {
            long sleep = 10;
            int numTries = 0;
            boolean succeeded = false;
            
            while (succeeded==false) {
                try {
                    AlertDefinitionManagerLocal alertDefMan = 
                        AlertDefinitionManagerEJBImpl.getOne();
                    
                    alertDefMan.updateAlertDefinitionsLastFiredTimes(updateEvents);
                    succeeded = true;
                } catch (Throwable e) {
                    if (++numTries == 5) {
                        log.error("Failed to update alert definition last " +
                                  "fired times: "+updateEvents, e);
                        break;
                    }

                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e1) {
                        // ignore
                    }

                    sleep = (sleep*3/2)+1;
                }                    
            }
        }
        
    }
        
}
