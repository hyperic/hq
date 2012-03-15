/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
	
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventLogStatus;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.events.server.session.EventLogDAO.ResourceEventLog;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Stores Events to and deletes Events from storage
 * </p>
 * 
 */
@Service
@Transactional
public class EventLogManagerImpl implements EventLogManager {

    private static final int MSGMAX = TrackEvent.MESSAGE_MAXLEN;
    private static final int SRCMAX = TrackEvent.SOURCE_MAXLEN;

    private final Log log = LogFactory.getLog(EventLogManagerImpl.class.getName());
    
    private final Log traceLog = LogFactory.getLog(EventLogManagerImpl.class.getName() + "Trace");

    private EventLogDAO eventLogDAO;
    @Autowired
    private ResourceManager resourceManager;

    @Autowired
    public EventLogManagerImpl(EventLogDAO eventLogDAO) {
        this.eventLogDAO = eventLogDAO;
    }

    /**
     * Create a new vanilla log item.
     * 
     * @param event The event to log.
     * @param subject The log item subject.
     * @param status The log item status.
     * @param save <code>true</code> to persist the log item; <code>false</code>
     *        to create a transient log item only.
     * 
     * 
     */
    public EventLog createLog(AbstractEvent event, String subject, String status, boolean save)
        throws ResourceDeletedException {
        String detail = event.toString();
        if (detail.length() > MSGMAX) {
            detail = detail.substring(0, MSGMAX - 1);
        }

        if (subject != null) {
            if (subject.length() > SRCMAX) {
                subject = subject.substring(0, SRCMAX - 1);
            }
        }

        Resource r = null;
        if (event instanceof ResourceEventInterface) {
            AppdefEntityID aeId = ((ResourceEventInterface) event).getResource();
            r = resourceManager.findResource(aeId);
            if (r == null || r.isInAsyncDeleteState()) {
                final String m = aeId + " has already been deleted";
                throw new ResourceDeletedException(m);
            }
        }

        EventLog e = new EventLog(r, subject, event.getClass().getName(), detail, event.getTimestamp(), status, event.getInstanceId());
        if (save) {
            return eventLogDAO.create(e);
        } else {
            return e;
        }
    }

    /**
     * Insert the event logs in batch.
     * 
     * @param eventLogs The event logs.
     * 
     * 
     */
    public void insertEventLogs(EventLog[] eventLogs) {
        eventLogDAO.insertLogs(eventLogs);
    }
    
    /**
     * Finds a unique log entry with the specified event type, instance ID, and timestamp.  Returns null if no such entry found.  
     * If multiple entries are found, returns first one found.
     * 
     */
    @Transactional(readOnly=true)
    public EventLog findLog(String typeClass, int instanceId, long timestamp) {
        return eventLogDAO.findLog(typeClass, instanceId, timestamp);
    }

    /**
     * Find the last event logs of all the resources of a given prototype. (i.e.
     * 'Linux' or 'FileServer File')
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<EventLog> findLastLogs(Resource proto) {
        return eventLogDAO.findLastByType(proto);
    }
    
    /**
     * Find the last unfixed AlertFiredEvents for each alert definition in the list
     * 
     * 
     * @return {@link Map} of alert definition id {@link Integer} to {@link AlertFiredEvent}
     * 
     */
    @Transactional(readOnly=true)
    public Map<Integer,AlertFiredEvent> findLastUnfixedAlertFiredEvents() {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        if (debug) watch.markTimeBegin("findUnfixedAlertFiredEventLogs");
        Map<Integer,AlertFiredEvent> alertFiredMap = eventLogDAO.findUnfixedAlertFiredEventLogs();
        if (debug) {
            watch.markTimeEnd("findUnfixedAlertFiredEventLogs");
            if (traceLog.isDebugEnabled()) {
                watch.markTimeBegin("get mapping");
                for (Integer key: alertFiredMap.keySet()) {
                    AlertFiredEvent val = alertFiredMap.get(key);
                    traceLog.debug(
                            "alertFiredMap alertDefId=" + key
                                + ", alertFiredEvent=" + val
                                + ", alert id=" + val.getAlertId()
                                + ", timestamp=" + val.getTimestamp());
                }
                watch.markTimeEnd("get mapping");
            }
            log.debug("findLastUnfixedAlertFiredEvents[" + alertFiredMap.size() + "]: " + watch);
        }
        return alertFiredMap;
    }

    /**
     * Get a list of {@link ResourceEventLog}s in a given interval, with the
     * maximum specified status.
     * 
     * If specified, typeClass dictates the full classname of the rows to check
     * (i.e. org.hyperic.hq.....ResourceLogEvent)
     * 
     * If specified, inGroups must be a collection of {@link ResourceGroup}s
     * which the resulting logs will be associated with.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<ResourceEventLog> findLogs(AuthzSubject subject, long begin, long end, PageInfo pInfo,
                                           EventLogStatus maxStatus, String typeClass,
                                           Collection<ResourceGroup> inGroups) {
        return eventLogDAO.findLogs(subject, begin, end, pInfo, maxStatus, typeClass, inGroups);
    }

    /**
     * Get a list of log records based on resource, event type and time range.
     * All resources which are descendents of the passed resource will also have
     * their event logs included
     * 
     * 
     */
    @Transactional(readOnly=true)
    @SuppressWarnings("unchecked")
    public List<EventLog> findLogs(AppdefEntityID ent, AuthzSubject user, String[] eventTypes, long begin, long end) {
        Resource r = resourceManager.findResource(ent);

        if (r == null || r.isInAsyncDeleteState()) {
            return new ArrayList<EventLog>(0);
        }

        Collection<String> eTypes;

        if (eventTypes == null) {
            eTypes = Collections.EMPTY_LIST;
        } else {
            eTypes = Arrays.asList(eventTypes);
        }
        if (r.getResourceType().getId().equals(AuthzConstants.authzGroup)) {
            return eventLogDAO.findByGroup(r, begin, end, eTypes);
        } else {
            return eventLogDAO.findByEntity(user, r, begin, end, eTypes);
        }
    }

    /**
     * Get a list of log records based on resource, status and time range. All
     * resources which are descendants of the passed resource will also have
     * their event logs included
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<EventLog> findLogs(AppdefEntityID ent, AuthzSubject user, String status, long begin, long end) {
        Resource r = resourceManager.findResource(ent);
        return eventLogDAO.findByEntityAndStatus(r, user, begin, end, status);
    }

    /**
     * Retrieve the total number of event logs.
     * 
     * @return The total number of event logs.
     * 
     */
    @Transactional(readOnly=true)
    public int getTotalNumberLogs() {
        return eventLogDAO.getTotalNumberLogs();
    }

    /**
     * Get an array of booleans, each element indicating whether or not there
     * are log records for that respective interval, for a particular entity
     * over a given time range.
     * 
     * This method also takes descendents of the passed-resource into
     * consideration.
     * 
     * @param entityId The entity.
     * @param begin The begin timestamp for the time range.
     * @param end The end timestamp for the time range.
     * @param intervals The number of intervals.
     * @return The boolean array with length equal to the number of intervals
     *         specified.
     * 
     */
    @Transactional(readOnly=true)
    public boolean[] logsExistPerInterval(AppdefEntityID entityId, AuthzSubject subject, long begin, long end,
                                          int intervals) {
        Resource r = resourceManager.findResource(entityId);
        return eventLogDAO.logsExistPerInterval(r, subject, begin, end, intervals);
    }

    /**
     * Delete event logs for the given resource TODO: Authz check.
     * 
     */
    public int deleteLogs(Resource r) {
        return eventLogDAO.deleteLogs(r);
    }

    /**
     * Purge old event logs.
     * 
     * @param from Delete all records starting from (and including) this time.
     *        If set to -1, then this method will delete all records from the
     *        earliest record forward.
     * @param to Delete all records up to (and including) this time. If set to
     *        -1, then this method will delete all records up to and including
     *        the most recent record.
     * @return The number of records removed.
     * 
     */
    public int deleteLogs(long from, long to) {
        if (log.isDebugEnabled()) {
            log.debug("deleteLogs(" + from + ", " + to + ")");
        }

        if (from == -1) {
            from = eventLogDAO.getMinimumTimeStamp();

            if (from == -1) {
                return 0;
            }
        }

        if (to == -1) {
            to = System.currentTimeMillis();
        }

        if (log.isDebugEnabled()) {
            log.debug("updated deleteLogs(" + from + ", " + to + ")");
        }

        if (from > to) {
            log.debug("deleteLogs range has (from > to). There are no rows to delete.");
            return 0;
        }

        // Now that we have valid from/to values, figure out what the
        // interval is (don't loop more than 60 times)
        long interval = Math.max(MeasurementConstants.DAY, (to - from) / 60);

        return eventLogDAO.deleteLogs(from, to, interval);
    }

	@PreDestroy 
    public final void destroy() { 
        this.eventLogDAO = null ; 
    }//EOM 
}
