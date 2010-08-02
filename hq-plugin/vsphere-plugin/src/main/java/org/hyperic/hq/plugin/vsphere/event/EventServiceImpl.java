/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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
package org.hyperic.hq.plugin.vsphere.event;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.EventHistoryCollector;
import com.vmware.vim25.mo.EventManager;
import com.vmware.vim25.mo.PropertyCollector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory; 

/**
 * EventServiceImpl
 * ToDo still investigating each of these methods and their design
 * 
 * @author Helena Edelson
 */
public class EventServiceImpl implements EventService {

    private static final Log logger = LogFactory.getLog(EventServiceImpl.class.getName());
 
    /* stateless handler */
    private EventHandler eventHandler = new DefaultEventHandler();

    /** Investigating this from EventInvoker vs DefaultEventListenerContainer */
    public void invoke(PropertyCollector propertyCollector, long duration) throws Exception { }



    /**
     * Trigger discoverPlatforms (and its call to super.discoverPlatforms).
     * If a vApp, ESX host, or VM is created, invoke the corresponding methods
     * to sync the resources and resource edges via hqapi, adding them to the inventory.
     * These will be named "discoverXXX" or "syncResourceEdges".
     *
     * If a vApp, ESX host or VM is deleted, invoke the removeFromInventory" methods
     * in the 2 VCenterPlatformDetectors.
     *
     * If a VM vMotions to another ESX host, update its relationship to its parent host
     * (and possibly some of its properties (this code should already exist).
    */
    public void queryNewEvents(){


    }
    


    /**
     * Creates a filter spec for querying events
     * Create an Entity Event Filter Spec to
     * specify the MoRef of the VM to get events filtered for
     *
     * @param eventManager
     * @param rootFolder
     * @throws Exception
     */
    public Event[] queryEvents(EventManager eventManager, ManagedObjectReference rootFolder) throws Exception {
        Event[] events = null;

        if (eventManager != null) {
            EventFilterSpec eventFilter = EventFilterBuilder.buildVMEventFilters(rootFolder);
            /* ToDo
            EventFilterSpecByTime eventTimeFilter = EventFilterBuilder.filterSinceLastQuery(..);
            eventFilter.setTime(eventTimeFilter); */

            events = eventManager.queryEvents(eventFilter);

            // ToDo behavior. Right now just discovery of what we have
            eventHandler.handleEvents(events);
        }

        return events;
    }

    public Event[] queryHistoricalEvents(EventManager eventManager, ManagedObjectReference rootFolder) throws Exception {
        Event[] events = null;

        if (eventManager != null) {
            EventFilterSpec eventFilter = EventFilterBuilder.buildVMEventFilters(rootFolder);
            /* ToDo
            EventFilterSpecByTime eventTimeFilter = EventFilterBuilder.filterSinceLastQuery(..);
            eventFilter.setTime(eventTimeFilter); */

            events = eventManager.queryEvents(eventFilter);

            // ToDo behavior. Right now just discovery of what we have
            EventHistoryCollector ehc = eventManager.createCollectorForEvents(eventFilter);
            eventHandler.handleEvents(ehc, 50);
        }

        return events;
    }
    
    /**
     * @param event
     * @param taskInfo
     * @throws Exception
     */
    public void publishEvent(Event event, TaskInfo taskInfo, EventManager eventManager) throws Exception {
        if (eventManager != null && event != null && taskInfo != null) {
            eventManager.postEvent(event, taskInfo);
        }
    }
    
     /**
     * Queries event manager for latest event
     */
    public Event getLatestEvent(EventManager eventManager) {
        Event latestEvent = eventManager.getLatestEvent();
        if(latestEvent != null) {
            eventHandler.handleEvent(latestEvent);
        }

        return latestEvent;
    }

}
