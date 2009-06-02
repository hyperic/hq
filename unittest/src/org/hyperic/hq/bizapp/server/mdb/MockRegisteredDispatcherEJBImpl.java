/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.mdb;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.jmock.core.Verifiable;
import org.jmock.expectation.ExpectationCounter;


/** A mock implementation of the RegisteredDispatcher Message-Drive Bean.
 *  This implementation is not thread-safe.
 * <p>
 */

public class MockRegisteredDispatcherEJBImpl
extends RegisteredDispatcherEJBImpl
implements Verifiable
{
	private Map triggerAssociations;
	private ExpectationCounter counter;
	
	public MockRegisteredDispatcherEJBImpl() {
		triggerAssociations = new HashMap();
		counter = new ExpectationCounter("MockRegisteredDispatcher failures");
		counter.setExpectNothing();
	}
	
	public void associateTrigger(AbstractEvent evt, TriggerInterface trigger) {
		Set associations = (Set) triggerAssociations.get(evt);
		if (associations == null) {
			associations = new HashSet();
			triggerAssociations.put(evt, associations);
		}
		
		associations.add(trigger);
	}
	
    protected void ensureSessionOpen() {
    }
    
	protected Collection getInterestedTriggers(AbstractEvent evt) {
		return (Collection) triggerAssociations.get(evt);
	}
	
	protected void dispatchEnqueuedEvents() {
		// Intentional no-op
	}
	
	public void verify() {
		counter.verify();
	}
}
