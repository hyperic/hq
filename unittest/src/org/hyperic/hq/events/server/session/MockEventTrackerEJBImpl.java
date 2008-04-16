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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.RemoveException;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.jmock.core.Verifiable;
import org.jmock.expectation.ExpectationCounter;

/**
 * A mock implementation of the event tracker EJB. This implementation is 
 * not thread safe.
 */
public class MockEventTrackerEJBImpl 
    implements EventTrackerLocal, Verifiable {

    private final Map _triggerEventId2triggerId = new HashMap();
    
    private final Map _triggerId2TriggerEvents = new HashMap();
    
    private final ExpectationCounter _expectedNumCalls = 
        new ExpectationCounter("number of event tracker invocations");
    
    private long _nextId = 1;

    private long _currentTimeMillis = System.currentTimeMillis();
    
    
    public MockEventTrackerEJBImpl() {
    }
    
    /**
     * Set the expectation that the event tracker interface methods will not 
     * be invoked.
     */
    public void setExpectNeverInvoked() {
        _expectedNumCalls.setExpectNothing();
    }
    
    public void setCurrentTimeMillis(long currentTime) {
        _currentTimeMillis = currentTime;
    }

    /**
     * @see org.hyperic.hq.events.shared.EventTrackerLocal#addReference(java.lang.Integer, org.hyperic.hq.events.AbstractEvent, long)
     */
    public Long addReference(Integer tid, AbstractEvent eventObject, long expiration) 
        throws SQLException {
        
        _expectedNumCalls.inc();
        
        long expire = 0;
        
        if (expiration == 0) {
            expire = Long.MAX_VALUE;
        } else {
            expire = expiration + _currentTimeMillis;
        }
                
        Long teid = getNextId();
        
        TriggerEvent triggerEvent = new TriggerEvent(teid,
                                                     eventObject, 
                                                     tid, 
                                                     eventObject.getTimestamp(), 
                                                     expire);
        
        _triggerEventId2triggerId.put(teid, tid);
        
        Set triggerEvents = (Set)_triggerId2TriggerEvents.get(tid);
        
        if (triggerEvents == null) {
            triggerEvents = new TreeSet(new Comparator() {

                public int compare(Object o1, Object o2) {
                    TriggerEvent event1 = (TriggerEvent)o1;
                    TriggerEvent event2 = (TriggerEvent)o2;
                    return (int)(event1.getCtime() - event2.getCtime());
                }
                
            });
            
            _triggerId2TriggerEvents.put(tid, triggerEvents);
        }
        
        triggerEvents.add(triggerEvent);
        
        eventObject.setId(teid);
        
        return teid;
    }

    /**
     * @see org.hyperic.hq.events.shared.EventTrackerLocal#deleteReference(java.lang.Integer)
     */
    public void deleteReference(Integer tid) throws SQLException {
        _expectedNumCalls.inc();
        // for now we don't care if we delete anything in the mock object
    }

    /**
     * @see org.hyperic.hq.events.shared.EventTrackerLocal#getReferencedEventStreams(java.lang.Integer)
     */
    public LinkedList getReferencedEventStreams(Integer tid)
            throws SQLException, IOException {
        
        _expectedNumCalls.inc();
        
        Set triggerEvents = (Set)_triggerId2TriggerEvents.get(tid);
        
        LinkedList eventStreams = new LinkedList();
        
        if (triggerEvents != null && !triggerEvents.isEmpty()) {            
            for (Iterator iter = triggerEvents.iterator(); iter.hasNext();) {
                TriggerEvent triggerEvent = (TriggerEvent) iter.next();
                
                if (!isExpired(triggerEvent)) {
                    eventStreams.add(new EventToTriggerEventLinker(triggerEvent));
                }
            }
        }
        
        return eventStreams;
    }

    /**
     * @see org.hyperic.hq.events.shared.EventTrackerLocal#updateReference(java.lang.Long, org.hyperic.hq.events.AbstractEvent)
     */
    public void updateReference(Long teid, AbstractEvent eventObject)
            throws SQLException {
        
        _expectedNumCalls.inc();
        
        Integer tid = (Integer)_triggerEventId2triggerId.get(teid);
        
        if (tid == null) {
            throw new SQLException("Can't update object that doesn't exist, teid="+teid);
        }
        
        TriggerEvent toUpdate = null;
        
        Set triggerEvents = (Set)_triggerId2TriggerEvents.get(tid);
        
        for (Iterator iter = triggerEvents.iterator(); iter.hasNext();) {
            TriggerEvent triggerEvent = (TriggerEvent) iter.next();
            
            if (triggerEvent.getId().equals(teid)) {
                iter.remove();
                toUpdate = triggerEvent;
            }            
        }
        
        if (toUpdate == null) {
            throw new SQLException("Can't update object that doesn't exist, teid="+teid);            
        }
        
        toUpdate.setEventObject(eventObject);
        toUpdate.setCtime(eventObject.getTimestamp());

        triggerEvents.add(toUpdate);
        
        eventObject.setId(teid);
    }

    /**
     * @see javax.ejb.EJBLocalObject#getEJBLocalHome()
     */
    public EJBLocalHome getEJBLocalHome() throws EJBException {
        return null;
    }

    /**
     * @see javax.ejb.EJBLocalObject#getPrimaryKey()
     */
    public Object getPrimaryKey() throws EJBException {
        return null;
    }

    /**
     * @see javax.ejb.EJBLocalObject#isIdentical(javax.ejb.EJBLocalObject)
     */
    public boolean isIdentical(EJBLocalObject arg0) throws EJBException {
        return false;
    }

    /**
     * @see javax.ejb.EJBLocalObject#remove()
     */
    public void remove() throws RemoveException, EJBException {
    }
    
    private Long getNextId() {
        return new Long(_nextId++);
    }
    
    private boolean isExpired(TriggerEvent event) {
        return event.getExpiration() < _currentTimeMillis;
    }


    /**
     * Verify the expectations.
     * 
     * @see org.jmock.core.Verifiable#verify()
     */
    public void verify() {
        if (_expectedNumCalls.hasExpectations()) {
            _expectedNumCalls.verify();
        }
    }


}
