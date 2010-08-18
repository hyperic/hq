/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.events.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventLogStatus;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.events.server.session.EventLogDAO.ResourceEventLog;

/**
 * Local interface for EventLogManager.
 */
public interface EventLogManager {
    /**
     * Create a new vanilla log item.
     * @param event The event to log.
     * @param subject The log item subject.
     * @param status The log item status.
     * @param save <code>true</code> to persist the log item; <code>false</code>
     *        to create a transient log item only.
     */
    public EventLog createLog(AbstractEvent event, String subject, String status, boolean save)
        throws ResourceDeletedException;

    /**
     * Insert the event logs in batch.
     * @param eventLogs The event logs.
     */
    public void insertEventLogs(org.hyperic.hq.events.server.session.EventLog[] eventLogs);
    
    /**
     * Finds a unique log entry with the specified event type, instance ID, and timestamp.  Returns null if no such entry found.  
     * If multiple entries are found, returns first one found.
     * 
     */
    public EventLog findLog(String typeClass, int instanceId, long timestamp);

    /**
     * Find the last event logs of all the resources of a given prototype. (i.e.
     * 'Linux' or 'FileServer File')
     */
    public List<EventLog> findLastLogs(Resource proto);
    
    /**
     * Find the last unfixed AlertFiredEvents for each alert definition in the list
     * 
     * 
     * @return {@link Map} of alert definition id {@link Integer} to {@link AlertFiredEvent}
     * 
     */
    public Map<Integer,AlertFiredEvent> findLastUnfixedAlertFiredEvents();

    /**
     * Get a list of {@link ResourceEventLog}s in a given interval, with the
     * maximum specified status. If specified, typeClass dictates the full
     * classname of the rows to check (i.e. org.hyperic.hq.....ResourceLogEvent)
     * If specified, inGroups must be a collection of {@link ResourceGroup}s
     * which the resulting logs will be associated with.
     */
    public List<ResourceEventLog> findLogs(AuthzSubject subject, long begin, long end, PageInfo pInfo,
                                           EventLogStatus maxStatus, String typeClass,
                                           Collection<ResourceGroup> inGroups);

    /**
     * Get a list of log records based on resource, event type and time range.
     * All resources which are descendents of the passed resource will also have
     * their event logs included
     */
    public List<EventLog> findLogs(AppdefEntityID ent, AuthzSubject user, java.lang.String[] eventTypes, long begin,
                                   long end);

    /**
     * Get a list of log records based on resource, status and time range. All
     * resources which are descendants of the passed resource will also have
     * their event logs included
     */
    public List<EventLog> findLogs(AppdefEntityID ent, AuthzSubject user, String status, long begin, long end);

    /**
     * Retrieve the total number of event logs.
     * @return The total number of event logs.
     */
    public int getTotalNumberLogs();

    /**
     * Get an array of booleans, each element indicating whether or not there
     * are log records for that respective interval, for a particular entity
     * over a given time range. This method also takes descendents of the
     * passed-resource into consideration.
     * @param entityId The entity.
     * @param begin The begin timestamp for the time range.
     * @param end The end timestamp for the time range.
     * @param intervals The number of intervals.
     * @return The boolean array with length equal to the number of intervals
     *         specified.
     */
    public boolean[] logsExistPerInterval(AppdefEntityID entityId, AuthzSubject subject, long begin, long end,
                                          int intervals);

    /**
     * Delete event logs for the given resource TODO: Authz check.
     */
    public int deleteLogs(Resource r);

    /**
     * Purge old event logs.
     * @param from Delete all records starting from (and including) this time.
     *        If set to -1, then this method will delete all records from the
     *        earliest record forward.
     * @param to Delete all records up to (and including) this time. If set to
     *        -1, then this method will delete all records up to and including
     *        the most recent record.
     * @return The number of records removed.
     */
    public int deleteLogs(long from, long to);

}
