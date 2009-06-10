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
import java.util.HashSet;
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
    
    public void setFailOnVerify() {
    	_expectedNumCalls.setFailOnVerify();
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
        
        synchronized (_triggerId2TriggerEvents) {

        	LinkedList triggerEvents = (LinkedList)_triggerId2TriggerEvents.get(tid);

        	if (triggerEvents == null) {
        		triggerEvents = new LinkedList();
        		_triggerId2TriggerEvents.put(tid, triggerEvents);
        	}

        	addEvent(triggerEvents, triggerEvent);
        }
        
        eventObject.setId(teid);
        
        return teid;
    }
    
    private void addEvent(LinkedList list, TriggerEvent te) {
    	if (list.size() == 0) {
    		list.add(te);
    	} else {
    		int addAt = 0;
    		int index = 0;
    		do {
    			TriggerEvent evtAt = (TriggerEvent) list.get(index);
    			if (evtAt.getCtime() > te.getCtime()) {
    				addAt = index;
    				break;
    			}
    		} while (++index < list.size());

    		list.add(addAt, te);
    	}
    }

    /**
     * @see org.hyperic.hq.events.shared.EventTrackerLocal#deleteReference(java.lang.Integer)
     */
    public void deleteReference(Integer tid) throws SQLException {
        _expectedNumCalls.inc();

        synchronized (_triggerId2TriggerEvents) {
        	// remove all events for this trigger
        	_triggerId2TriggerEvents.remove(tid);
        }
    }
    
    /**
     * @see org.hyperic.hq.events.shared.EventTrackerLocal#getReferencedEventStreams(java.lang.Integer)
     */
    public LinkedList getReferencedEventStreams(Integer tid)
            throws SQLException, IOException {
        
        _expectedNumCalls.inc();
        
        LinkedList eventStreams = new LinkedList();

        synchronized (_triggerId2TriggerEvents) {

        	LinkedList triggerEvents = (LinkedList)_triggerId2TriggerEvents.get(tid);

        	if (triggerEvents != null && !triggerEvents.isEmpty()) {            
        		for (Iterator iter = triggerEvents.iterator(); iter.hasNext();) {
        			TriggerEvent triggerEvent = (TriggerEvent) iter.next();

        			if (!isExpired(triggerEvent)) {
        				eventStreams.add(new EventToTriggerEventLinker(triggerEvent));
        			}
        		}
        	}
        }
        
        return eventStreams;
    }

    public int getEventsCount(Integer tid)
        throws SQLException {
        _expectedNumCalls.inc();
        
        int count = 0;
        
        synchronized (_triggerId2TriggerEvents) {

        	LinkedList triggerEvents = (LinkedList)_triggerId2TriggerEvents.get(tid);

        	LinkedList eventStreams = new LinkedList();

        	if (triggerEvents != null && !triggerEvents.isEmpty()) {            
        		for (Iterator iter = triggerEvents.iterator(); iter.hasNext();) {
        			TriggerEvent triggerEvent = (TriggerEvent) iter.next();

        			if (!isExpired(triggerEvent)) {
        				count++;
        			}
        		}
        	}
        }
        
        return count;
    }

    /**
     * @see org.hyperic.hq.events.shared.EventTrackerLocal#updateReference(java.lang.Long, org.hyperic.hq.events.AbstractEvent)
     */
    public void updateReference(Integer tid, Long teid,
                                AbstractEvent eventObject,
                                long expiration)
            throws SQLException {
        
        _expectedNumCalls.inc();
        
        if (tid == null) {
            throw new SQLException("Can't update object that doesn't exist, teid="+teid);
        }
        
        TriggerEvent toUpdate = null;
        
        synchronized (_triggerId2TriggerEvents) {

        	LinkedList triggerEvents = (LinkedList)_triggerId2TriggerEvents.get(tid);
        	if (triggerEvents == null) {
        		triggerEvents = new LinkedList();
        		_triggerId2TriggerEvents.put(tid, triggerEvents);
        	} else {
        		for (Iterator iter = triggerEvents.iterator(); iter.hasNext();) {
        			TriggerEvent triggerEvent = (TriggerEvent) iter.next();

        			if (triggerEvent.getId().equals(teid)) {
        				iter.remove();
        				toUpdate = triggerEvent;
        			}            
        		}
        	}

        	if (toUpdate == null) {
                toUpdate = new TriggerEvent(teid,
                						    eventObject, 
                						    tid, 
                						    eventObject.getTimestamp(), 
                						    expiration);
        	} else {
        		toUpdate.setEventObject(eventObject);
        		toUpdate.setCtime(eventObject.getTimestamp());
        	}
        	
        	addEvent(triggerEvents, toUpdate);

        	eventObject.setId(teid);
        }
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
