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

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceDeleteRequestedEvent;
import org.hyperic.hq.bizapp.shared.EventLogBoss;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.events.shared.EventLogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BizApp's interface to the Events/Logs
 * 
 * 
 */
@Service
@Transactional
public class EventLogBossImpl implements EventLogBoss,
    ApplicationListener<ResourceDeleteRequestedEvent> {

    private EventLogManager eventLogManager;

    private SessionManager sessionManager;

    @Autowired
    public EventLogBossImpl(EventLogManager eventLogManager, SessionManager sessionManager) {
        this.eventLogManager = eventLogManager;
        this.sessionManager = sessionManager;
    }

    /**
     * Find events based on event type and time range for a resource
     * 
     * @param eventType Event classname (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if no events are
     *         found
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<EventLog> getEvents(int sessionId, String eventType, AppdefEntityID id,
                                    long beginTime, long endTime) throws SessionNotFoundException,
        SessionTimeoutException {
        // We ignore the subject for now
        sessionManager.authenticate(sessionId);
        return getEvents(sessionId, eventType, new AppdefEntityID[] { id }, beginTime, endTime);
    }

    /**
     * Find events based on event type and time range for multiple resources
     * 
     * @param eventType Event classname (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if no events are
     *         found
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<EventLog> getEvents(int sessionId, String eventType, AppdefEntityID ids[],
                                    long beginTime, long endTime) throws SessionNotFoundException,
        SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        List<EventLog> events = new ArrayList<EventLog>();

        for (int i = 0; i < ids.length; i++) {
            events.addAll(eventLogManager.findLogs(ids[i], subject, new String[] { eventType },
                beginTime, endTime));
        }

        return events;
    }

    /**
     * Find events based on event type and time range for multiple resources
     * 
     * @param eventTypes Array of event class names.
     *        (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if no events are
     *         found
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<EventLog> getEvents(int sessionId, AppdefEntityID aeid, String[] eventTypes,
                                    long beginTime, long endTime) throws SessionNotFoundException,
        SessionTimeoutException {
        AuthzSubject user = sessionManager.getSubject(sessionId);
        return eventLogManager.findLogs(aeid, user, eventTypes, beginTime, endTime);
    }

    /**
     * Find events based on status and time range for multiple resources
     * 
     * @return List of EventLogValue objects or an empty List if no events are
     *         found
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<EventLog> getEvents(int sessionId, AppdefEntityID aeid, String status,
                                    long beginTime, long endTime) throws SessionNotFoundException,
        SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return eventLogManager.findLogs(aeid, subject, status, beginTime, endTime);
    }

    /**
     * Get an array of boolean indicating if logs exist per interval, for an
     * entity over a given time range.
     * 
     * @param aeid the entity ID
     * @return boolean array indicating if logs exist per interval.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public boolean[] logsExistPerInterval(int sessionId, AppdefEntityID aeid, long beginTime,
                                          long endTime, int intervals)
        throws SessionNotFoundException, SessionTimeoutException {
        // We ignore the subject for now.
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return eventLogManager.logsExistPerInterval(aeid, subject, beginTime, endTime, intervals);
    }

    public void onApplicationEvent(ResourceDeleteRequestedEvent event) {
        eventLogManager.deleteLogs(event.getResource());
    }
}
