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

import java.rmi.RemoteException;
import java.util.Calendar;

/**
 * EventFilterBuilder
 * Builder methods which create event filters
 * to partition event results from querying. 
 *
 * @author Helena Edelson
 */
public class EventFilterBuilder {

    /** 
     * Creates a filter for events.
     * @param rootFolder
     * @return
     */
    public static EventFilterSpec buildEventFilters(ManagedObjectReference rootFolder, Calendar lastQueriedTime, Calendar pendingQueryTime) {
        EventFilterSpec filter = new EventFilterSpec();
        filter.setEntity(filterByChildrenOfRootFolder(rootFolder));
        filter.setType(EventTypes.getSubscription());

        EventFilterSpecByTime eventTimeFilter = EventFilterBuilder.filterSinceLastQuery(lastQueriedTime, pendingQueryTime);
        filter.setTime(eventTimeFilter);

        return filter;
    } 

    /**
     * Filters events to only those that occurred
     * between the time last queried and the pending query time.
     * @param lastQueriedTime
     * @param pendingQueryTime
     * @return
     */
    public static EventFilterSpecByTime filterSinceLastQuery(Calendar lastQueriedTime, Calendar pendingQueryTime) {
        EventFilterSpecByTime filter = new EventFilterSpecByTime();
        filter.setBeginTime(lastQueriedTime);
        filter.setEndTime(pendingQueryTime);

        return filter;
    }

    /**
     * Filters to children of root folder
     * @return
     */
    public static EventFilterSpecByEntity filterByChildrenOfRootFolder(ManagedObjectReference rootFolder) {
        EventFilterSpecByEntity filter = new EventFilterSpecByEntity();
        filter.setEntity(rootFolder);
        filter.setRecursion(EventFilterSpecRecursionOption.children);

        return filter;
    }

    /**
     * Limits to the user names passed in.
     * @param users
     * @return
     */
    public static EventFilterSpecByUsername filterByUser(String[] users) {
        EventFilterSpecByUsername filter = new EventFilterSpecByUsername();
        filter.setSystemUser(false);
        filter.setUserList(users);

        return filter;
    }

}
