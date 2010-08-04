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

import java.util.Calendar;

/**
 * DefaultInventoryManager
 *
 * @author Helena Edelson
 */
public class DefaultInventoryManager implements InventoryManager, EventValidator {

    private static final Log logger = LogFactory.getLog(DefaultInventoryManager.class.getName());
 
    private Calendar lastQueryTime;

    /**
     * In doing the first query you may not get any new events
     * because the interval between the lastQueryTime and the
     * pending query will be seconds/milliseconds.
     *
     * @return
     */
    private Calendar getLastQueryTime() {
        return lastQueryTime == null ? lastQueryTime = Calendar.getInstance() : lastQueryTime;
    }

    public boolean updateInventory(EventManager eventManager, ManagedObjectReference rootFolder) throws Exception {

        if (eventManager != null && rootFolder != null) {
            Calendar pendingQueryTime = Calendar.getInstance();
            EventFilterSpec filter = EventFilterBuilder.buildEventFilters(rootFolder, getLastQueryTime(), pendingQueryTime);
            boolean foundEvents = queryEvents(eventManager, filter);
            lastQueryTime.setTime(pendingQueryTime.getTime());
        }

        return false;
    }


    /**
     * Creates a filter spec for querying events
     * Create an Entity Event Filter Spec to
     * specify the MoRef of the VM to get events filtered for
     *
     * @param eventManager
     * @param filter
     * @throws Exception
     */
    public boolean queryEvents(EventManager eventManager, EventFilterSpec filter) throws Exception {
        Event[] events = eventManager.queryEvents(filter);
        return hasValidEvents(events);
    }

     /**
     * @param events
     * @return
     */
    public boolean hasValidEvents(Event[] events) {
        if (events != null && events.length > 0) {
             
        }

        return false;
    }

}
