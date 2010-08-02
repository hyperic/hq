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
 * EventFilter
 * Builder methods which create event filters
 * to partition event results from querying.
 * <p/>
 * To further filter results you can add a
 * String[] categories:
 * new String[]{"error", "warning"}
 * eventFilter.setCategory(categoryTypes);
 *
 * @author Helena Edelson
 */
public class EventFilterBuilder {

    /** 
     * Creates a filter for VM events for Sonoma.
     * Refer to API Data Object vmEvent and see the
     * extends class list for elaborate list of vmEvents.
     * ToDo - determine which vm events we need
     *
     * @param rootFolder
     * @return
     */
    public static EventFilterSpec buildVMEventFilters(ManagedObjectReference rootFolder) {
        EventFilterSpec filter = new EventFilterSpec();
        filter.setEntity(filterByChildrenOfRootFolder(rootFolder));

        filter.setType(EventChannel.DEFAULT_SUBSCRIPTION);

        return filter;
    }

    /**
     * Creates a filter for Host events for Sonoma.
     * Refer to API Data Object vmEvent and see the
     * extends class list for elaborate list of vmEvents.
     *
     * @param rootFolder
     * @return
     */
    public static EventFilterSpec buildHostEventFilters(ManagedObjectReference rootFolder) {
        EventFilterSpec filter = new EventFilterSpec();
        filter.setEntity(filterByChildrenOfRootFolder(rootFolder));
        /* ToDo add to list */
        filter.setType(new String[]
                {EventChannel.Host.HostAddedEvent}
        );

        return filter;
    }


    /**
     * Limits to the events happened in the past month
     *
     * @param currentTime
     * @return
     */
    public static EventFilterSpecByTime filterByTime(Calendar currentTime, Calendar timeLastChecked) throws RemoteException {
        EventFilterSpecByTime filter = new EventFilterSpecByTime();
        Calendar startTime = currentTime;
        startTime.roll(Calendar.MINUTE, true);
        /** This will collect only those events that have occurred after the specified time. */
        filter.setBeginTime(startTime);
        // ToDo refactor to do efs.setTime(filter);

        return filter;
    }

    public static EventFilterSpecByTime filterSinceLastQuery(Calendar currentTime, Calendar lastQuery) throws RemoteException {
        EventFilterSpecByTime filter = new EventFilterSpecByTime();
        Calendar startTime = lastQuery;
        startTime.roll(Calendar.MILLISECOND, true);

        filter.setBeginTime(startTime);
        filter.setEndTime(currentTime);
        // ToDo refactor to do efs.setTime(filter);

        return filter;
    }

    /**
     * Limits to the children of root folder
     *
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
     *
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
