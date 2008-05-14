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

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceDeleteCallback;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.events.server.session.EventLogManagerEJBImpl;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.bizapp.shared.EventLogBossLocal;
import org.hyperic.hq.bizapp.shared.EventLogBossUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * The BizApp's interface to the Events/Logs
 *
 * @ejb:bean name="EventLogBoss"
 *      jndi-name="ejb/bizapp/EventLogBoss"
 *      local-jndi-name="LocalEventLogBoss"
 *      view-type="both"
 *      type="Stateless"
 * 
 * @ejb:transaction type="REQUIRED"
 */
public class EventLogBossEJBImpl extends BizappSessionEJB implements
        SessionBean {

    private Log _log = LogFactory.getLog(EventLogBossEJBImpl.class);
    
    private EventLogManagerLocal eventLogManager = null;

    private SessionManager manager;

    public EventLogBossEJBImpl() {
        manager = SessionManager.getInstance();
    }

    private EventLogManagerLocal getELM() {
        if (eventLogManager == null) {
            eventLogManager = EventLogManagerEJBImpl.getOne();
        }
        return eventLogManager;
    }

    /**
     * Find events based on event type and time range for a resource
     *
     * @param eventType Event classname (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if
     *         no events are found
     * 
     * @ejb:interface-method 
     */
    public List getEvents(int sessionId, String eventType, AppdefEntityID id, 
                          long beginTime, long endTime)
        throws SessionNotFoundException, SessionTimeoutException
    {
        // We ignore the subject for now
        manager.authenticate(sessionId);
        return getEvents(sessionId, eventType, new AppdefEntityID[] { id },
                         beginTime, endTime);
    }

    /**
     * Find events based on event type and time range for multiple
     * resources
     *
     * @param eventType Event classname (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if
     *         no events are found
     * 
     * @ejb:interface-method
     */
    public List getEvents(int sessionId, String eventType, 
                          AppdefEntityID ids[],
                          long beginTime, long endTime)
        throws SessionNotFoundException, SessionTimeoutException 
    {
        AuthzSubject subject = manager.getSubject(sessionId);
        List events = new ArrayList();
    
        for (int i=0; i <ids.length; i++) {
            events.addAll(getELM().findLogs(ids[i], subject, 
                                            new String[] { eventType },
                                            beginTime, endTime));
        }
    
        return events;
    }

    /**
     * Find events based on event type and time range for multiple
     * resources
     *
     * @param eventTypes Array of event class names. (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if
     *         no events are found
     * 
     * @ejb:interface-method
     */
    public List getEvents(int sessionId, AppdefEntityID aeid,
                          String[] eventTypes, long beginTime, long endTime)
        throws SessionNotFoundException, SessionTimeoutException 
    {
        AuthzSubject user = manager.getSubject(sessionId);
        return getELM().findLogs(aeid, user, eventTypes, beginTime, endTime);
    }

    /**
     * Find events based on status and time range for multiple
     * resources
     *
     * @return List of EventLogValue objects or an empty List if
     *         no events are found
     * 
     * @ejb:interface-method
     */
    public List getEvents(int sessionId, AppdefEntityID aeid,
                          String status, long beginTime, long endTime)
        throws SessionNotFoundException, SessionTimeoutException 
    {
        AuthzSubject subject = manager.getSubject(sessionId);
        return getELM().findLogs(aeid, subject, status, beginTime, endTime);
    }

    /** 
     * Get an array of boolean indicating if logs exist per interval, 
     * for an entity over a given time range.
     *
     * @param aeid the entity ID
     * @return boolean array indicating if logs exist per interval.
     * 
     * @ejb:interface-method
     */
    public boolean[] logsExistPerInterval(int sessionId, AppdefEntityID aeid,
                                          long beginTime, long endTime,
                                          int intervals)
        throws SessionNotFoundException, SessionTimeoutException 
    {
        // We ignore the subject for now.
        AuthzSubject subject = manager.getSubject(sessionId);
        return getELM().logsExistPerInterval(aeid, subject, beginTime, endTime,
                                             intervals);
    }

    /**
     * @ejb:interface-method
     */
    public void startup() {
        _log.info("Event Log Boss starting up!");

        HQApp app = HQApp.getInstance();

        app.registerCallbackListener(ResourceDeleteCallback.class,
            new ResourceDeleteCallback() {
                public void preResourceDelete(Resource r) throws VetoException {
                    EventLogManagerLocal elm = EventLogManagerEJBImpl.getOne();
                    elm.deleteLogs(r);
                }
            }
        );
    }

    public static EventLogBossLocal getOne() {
        try {
            return EventLogBossUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
