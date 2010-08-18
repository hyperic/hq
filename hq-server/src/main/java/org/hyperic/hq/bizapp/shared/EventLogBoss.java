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
package org.hyperic.hq.bizapp.shared;

import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.events.server.session.EventLog;

/**
 * Local interface for EventLogBoss.
 */
public interface EventLogBoss {
    /**
     * Find events based on event type and time range for a resource
     * @param eventType Event classname (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if no events are
     *         found
     */
    public List<EventLog> getEvents(int sessionId, String eventType, AppdefEntityID id,
                                    long beginTime, long endTime) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Find events based on event type and time range for multiple resources
     * @param eventType Event classname (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if no events are
     *         found
     */
    public List<EventLog> getEvents(int sessionId, String eventType,
                                    org.hyperic.hq.appdef.shared.AppdefEntityID[] ids,
                                    long beginTime, long endTime) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Find events based on event type and time range for multiple resources
     * @param eventTypes Array of event class names.
     *        (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if no events are
     *         found
     */
    public List<EventLog> getEvents(int sessionId, AppdefEntityID aeid,
                                    java.lang.String[] eventTypes, long beginTime, long endTime)
        throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Find events based on status and time range for multiple resources
     * @return List of EventLogValue objects or an empty List if no events are
     *         found
     */
    public List<EventLog> getEvents(int sessionId, AppdefEntityID aeid, String status,
                                    long beginTime, long endTime) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Get an array of boolean indicating if logs exist per interval, for an
     * entity over a given time range.
     * @param aeid the entity ID
     * @return boolean array indicating if logs exist per interval.
     */
    public boolean[] logsExistPerInterval(int sessionId, AppdefEntityID aeid, long beginTime,
                                          long endTime, int intervals)
        throws SessionNotFoundException, SessionTimeoutException;

}
