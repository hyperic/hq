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

package org.hyperic.hq.events.server.session;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.shared.EventObjectDeserializer;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.EventTrackerUtil;

/**
 * Maintains the event state for each trigger. Event state changes are performed 
 * within a new Hibernate session so these changes are visible immediately to 
 * to other sessions.
 * 
 * @ejb:bean name="EventTracker"
 *      jndi-name="ejb/events/EventTracker"
 *      local-jndi-name="LocalEventTracker"
 *      view-type="local"
 *      type="Stateless"
 *      
 * @ejb:transaction type="REQUIRESNEW"
 */
public class EventTrackerEJBImpl extends SessionBase implements SessionBean {

    private final Log log = LogFactory.getLog(EventTrackerEJBImpl.class);    

    /** 
     * Add a reference from a trigger to an event. The event object id will be 
     * set to the id for the newly created trigger event.
     * 
     * @param tid The trigger id.
     * @param eventObject The event object.
     * @param expiration The number of milliseconds from now when the event will 
     *                   will expire (zero means no expiration).
     * @return The id for the newly created trigger event (also the eventObject id).                  
     * @ejb:interface-method
     */
    public Long addReference(Integer tid, AbstractEvent eventObject, long expiration) 
        throws SQLException {
        
        if (log.isDebugEnabled())
            log.debug("Add referenced event for trigger id: " + tid);
        
        long expire = 0;
        
        if (expiration == 0) {
            expire = Long.MAX_VALUE;
        } else {
            expire = expiration + System.currentTimeMillis();
        }
                
        TriggerEvent triggerEvent = new TriggerEvent(eventObject, 
                                                     tid, 
                                                     eventObject.getTimestamp(), 
                                                     expire);
        
        
        TriggerEventDAO triggerEventDAO = getTriggerEventDAO();
        
        Session session = triggerEventDAO.getNewSession();
        
        // NOTE: It appears that explicit transaction management is not necessary.
        // Effectively, the Hibernate transaction is flushing the session. Instead 
        // of setting the Hibernate transaction boundaries, we could just flush 
        // the session before closing it.
        Transaction txn = null;
        
        try {
            txn = session.beginTransaction();
            triggerEventDAO.save(triggerEvent, session);
            txn.commit();
        } catch (Exception e) {
            if (txn != null) {
                txn.rollback();
            }
            
            log.error("Failed to add referenced event for trigger id="+tid, e);     
            throw new SQLException("Failed to add referenced event for trigger id="+tid);
            
        } finally {
            session.close();
        }
                
        Long teid = triggerEvent.getId();
        eventObject.setId(teid);
        
        return teid;
    }

    /** 
     * Update the event object referenced by a trigger. The event object id will 
     * be set to the trigger event id.
     * 
     * NOTE: Since we don't use an optimistic locking strategy at the database 
     * level, it is important that all updates to referenced event streams are 
     * performed serially at the application level.
     * 
     * @param teid The id for the trigger event that should be updated with the 
     *             new event object.
     * @param eventObject The new event object.
     * @ejb:interface-method
     */
    public void updateReference(Long teid, AbstractEvent eventObject) 
        throws SQLException {        
        if (log.isDebugEnabled())
            log.debug("Updating the event object for trigger event id: " + teid);            

        TriggerEventDAO triggerEventDAO = getTriggerEventDAO();
        
        Session session = triggerEventDAO.getNewSession();
        
        // NOTE: It appears that explicit transaction management is not necessary.
        // Effectively, the Hibernate transaction is flushing the session. Instead 
        // of setting the Hibernate transaction boundaries, we could just flush 
        // the session before closing it.
        Transaction txn = null;
        
        try {
            txn = session.beginTransaction();
            TriggerEvent triggerEvent = triggerEventDAO.findById(teid, session);
            triggerEvent.setEventObject(eventObject);
            triggerEvent.setCtime(eventObject.getTimestamp());
            triggerEventDAO.save(triggerEvent, session);
            txn.commit();
        } catch (Exception e) {
            if (txn != null) {
                txn.rollback();
            }
            
            log.error("Failed to update event object for trigger event id="+teid, e);     
            throw new SQLException("Failed to update event object for trigger event id="+teid);
            
        } finally {
            session.close();
        }
        
        eventObject.setId(teid);
    }


    /** 
     * Delete all events referenced by a trigger.
     * 
     * @param tid The trigger id.
     * @ejb:interface-method
     */
    public void deleteReference(Integer tid) throws SQLException {
        if (log.isDebugEnabled())
            log.debug("Delete referenced events for trigger id: " + tid);
        
        TriggerEventDAO triggerEventDAO = getTriggerEventDAO();
        
        Session session = triggerEventDAO.getNewSession();
        
        // NOTE: It appears that explicit transaction management is not necessary.
        // Effectively, the Hibernate transaction is flushing the session. Instead 
        // of setting the Hibernate transaction boundaries, we could just flush 
        // the session before closing it.
        Transaction txn = null;
        
        try {
            txn = session.beginTransaction();
            triggerEventDAO.deleteByTriggerId(tid, session);        
            
            // To reduce contention on the trigger_event table we are going to reduce 
            // the number of times we try to delete expired trigger_events.
            if (ExpiredEventsDeletionScheduler.getInstance()
                        .shouldDeleteExpiredEvents()) {
                triggerEventDAO.deleteExpired(session);
            }
            
            txn.commit();
        } catch (Exception e) {
            if (txn != null) {
                txn.rollback();
            }
            
            log.error("Failed to delete expired and referenced events for trigger id="+tid, e);     
            throw new SQLException("Failed to delete expired and referenced events for trigger id="+tid);
            
        } finally {
            session.close();
        }        
        
    }


    /** 
     * Get the list of events that are referenced by a given trigger in order
     * of reference creation.
     * 
     * @param tid The trigger id.
     * @return The list of {@link EventObjectDeserializer EventObjectDeserializers} 
     *         containing the events referenced by the trigger. Each event will 
     *         have its id set to the trigger event id.
     *         
     * @ejb:transaction type="NOTSUPPORTED"
     * @ejb:interface-method
     */
    public LinkedList getReferencedEventStreams(Integer tid) 
        throws SQLException, IOException {
        
        boolean debug = log.isDebugEnabled();
        
        if (debug) {
            log.debug("Get referenced events for trigger id: " + tid);                
        }
        
        TriggerEventDAO triggerEventDAO = getTriggerEventDAO();
        
        Session session = triggerEventDAO.getNewSession();
        
        LinkedList eventObjectDeserializers = new LinkedList();
        
        try {
            List triggerEvents = getTriggerEventDAO()
                                    .findUnexpiredByTriggerId(tid, session);

            // When returning the events, we link the event to the trigger_event.
            for (Iterator it = triggerEvents.iterator(); it.hasNext();) {
                TriggerEvent triggerEvent = (TriggerEvent) it.next();
                eventObjectDeserializers.add(new EventToTriggerEventLinker(triggerEvent));
            }
        
        } catch (IOException e) {
            log.error("Failed to get referenced events for trigger id="+tid, e); 
            throw e;
        } catch (Exception e) {
            log.error("Failed to get referenced events for trigger id="+tid, e);     
            throw new SQLException("Failed to get referenced events for trigger id="+tid);            
        } finally {
            session.close();
        }
        
        if (debug) {
            log.debug("Retrieved " + eventObjectDeserializers.size() + 
                      " referenced events for trigger id: " + tid);            
        }
        
        return eventObjectDeserializers;
    }

    public static EventTrackerLocal getOne() {
        try {
            return EventTrackerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }
    
    /** @ejb:create-method */
    public void ejbCreate() {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {}
    
    private TriggerEventDAO getTriggerEventDAO() {
        return new TriggerEventDAO(DAOFactory.getDAOFactory());
    }
    
}



