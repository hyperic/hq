package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.events.shared.EventLogManagerUtil;

/** 
 * The BizApp's interface to the Events/Logs
 *
 * @ejb:bean name="EventLogBoss"
 *      jndi-name="ejb/bizapp/EventLogBoss"
 *      local-jndi-name="LocalEventLogBoss"
 *      view-type="both"
 *      type="Stateless"
 * 
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class EventLogBossEJBImpl extends BizappSessionEJB implements
        SessionBean {

    private EventLogManagerLocal eventLogManager = null;

    private SessionManager manager;

    public EventLogBossEJBImpl() {
        this.manager = SessionManager.getInstance();
    }

    private EventLogManagerLocal getELM() {
        if (eventLogManager == null) {
            try {
                eventLogManager = 
                    EventLogManagerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
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
        AuthzSubjectValue subject = this.manager.getSubject(sessionId);
        
        // We ignore the subject for now
        return this.getEvents(sessionId, eventType, new AppdefEntityID[] { id },
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
        throws SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionId);
        List events = new ArrayList();
    
        // We ignore the subject for now.
        for (int i=0; i <ids.length; i++) {
            events.addAll(getELM().findLogs(ids[i].getType(), ids[i].getID(),
                                            new String[] { eventType },
                                            beginTime, endTime));
        }
    
        return events;
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
    public List getEvents(int sessionId, AppdefEntityID aeid,
                          String[] eventTypes, long beginTime, long endTime)
        throws SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionId);
        // We ignore the subject for now.
        List events = getELM().findLogs(aeid.getType(), aeid.getID(),
                                        eventTypes, beginTime, endTime);
        return events;
    }

    /**
     * Find events based on status and time range for multiple
     * resources
     *
     * @param eventType Event classname (ControlEvent.class.getName())
     * @return List of EventLogValue objects or an empty List if
     *         no events are found
     * 
     * @ejb:interface-method
     */
    public List getEvents(int sessionId, AppdefEntityID aeid,
                          String status, long beginTime, long endTime)
        throws SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionId);
        // We ignore the subject for now.
        List events = getELM().findLogs(aeid.getType(), aeid.getID(),
                                        status, beginTime, endTime);
        return events;
    }

    /** Get an array of log record counts based on entity ID and time range
     *
     * @param aeid the entity ID
     * @return array of log record counts
     * 
     * @ejb:interface-method
     */
    public int[] getLogsCount(int sessionId, AppdefEntityID aeid,
                              long beginTime, long endTime, int intervals)
        throws SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionId);
        return getELM().getLogsCount(aeid, beginTime, endTime, intervals);
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
