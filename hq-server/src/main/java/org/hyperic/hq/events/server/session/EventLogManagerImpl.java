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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.alert.data.AlertRepository;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.event.data.EventLogRepository;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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

    private EventLogRepository eventLogDAO;

    private ResourceManager resourceManager;
    
    private AlertRepository alertRepository;

    @Autowired
    public EventLogManagerImpl(EventLogRepository eventLogDAO, ResourceManager resourceManager,
                               AlertRepository alertRepository) {
        this.eventLogDAO = eventLogDAO;
        this.resourceManager = resourceManager;
        this.alertRepository = alertRepository;
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

        Integer r = null;
        if (event instanceof ResourceEventInterface) {
            r = ((ResourceEventInterface) event).getResource();
            if (r == null) {
                final String m = r + " has already been deleted";
                throw new ResourceDeletedException(m);
            }
        }

        EventLog e = new EventLog(r, subject, event.getClass().getName(), detail, event.getTimestamp(), status, event.getInstanceId());
        if (save) {
            return eventLogDAO.save(e);
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
        final Map<Integer,AlertFiredEvent> alertFiredMap = new HashMap<Integer,AlertFiredEvent>();
        final long ctime = alertRepository.getOldestUnfixedAlertTime();
        if (ctime == 0) {
            return new HashMap<Integer,AlertFiredEvent>(0,1);
        }
        final Map<Integer,Map<AlertInfo,Integer>> alerts = alertRepository.getUnfixedAlertInfoAfter(ctime);
        List<EventLog> list = eventLogDAO.findByTimestampGreaterThanOrEqualToAndType(ctime, AlertFiredEvent.class.getName());
        for (EventLog log  : list ) {
                if (log == null || log.getInstanceId() == null) {
                    continue;
                }
                final Map<AlertInfo,Integer> objs = alerts.get(log.getInstanceId());
                if (objs == null) {
                    continue;
                }
                final Integer alertDefId = log.getInstanceId();
                final long timestamp     = log.getTimestamp();
                final Integer alertId =
                    objs.get(new AlertInfo(alertDefId, timestamp));
                if (alertId == null) {
                    continue;
                }
                AlertFiredEvent alertFired = 
                    createAlertFiredEvent(alertDefId, alertId, log);
                alertFiredMap.put(alertDefId, alertFired);
            }
 
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
    
    private final AlertFiredEvent createAlertFiredEvent(Integer alertDefId,Integer alertId,EventLog eventLog) {
        return new AlertFiredEvent(alertId, alertDefId, eventLog.getResource(), eventLog.getSubject(),
            eventLog.getTimestamp(), eventLog.getDetail());
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

        if (r instanceof ResourceGroup) {
            if(eventTypes == null) {
                return eventLogDAO.findByTimestampBetweenAndResourcesOrderByTimestamp(begin,end,
                    ((ResourceGroup)r).getMemberIds());
            }else {
                return eventLogDAO.findByTimestampBetweenAndResourcesAndEventTypesOrderByTimestamp(begin,end,
                    ((ResourceGroup)r).getMemberIds(), Arrays.asList(eventTypes));
            }
        } else {
            if(eventTypes == null) {
                return eventLogDAO.findByTimestampBetweenAndResourceOrderByTimestampAsc(begin, end, r.getId());
            }else {
                return eventLogDAO.findByTimestampBetweenAndResourceAndEventTypesOrderByTimestamp(begin, end, r.getId(), 
                    Arrays.asList(eventTypes));
            }
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
        return eventLogDAO.findByTimestampBetweenAndStatusAndResourceOrderByTimestampAsc(begin, end, status, ent.getId());
    }

    /**
     * Retrieve the total number of event logs.
     * 
     * @return The total number of event logs.
     * 
     */
    @Transactional(readOnly=true)
    public int getTotalNumberLogs() {
        return eventLogDAO.count().intValue();
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
        return eventLogDAO.logsExistPerInterval(entityId.getId(), begin, end, intervals);
    }

    /**
     * Delete event logs for the given resource TODO: Authz check.
     * 
     */
    public void deleteLogs(Resource r) {
        eventLogDAO.deleteByResource(r.getId());
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

        return eventLogDAO.deleteLogsInTimeRange(from, to);
    }
}
