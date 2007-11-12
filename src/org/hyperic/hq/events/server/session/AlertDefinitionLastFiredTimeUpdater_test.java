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

import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.hyperic.hq.events.AlertDefinitionLastFiredUpdateEvent;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocalHome;
import org.mockejb.jndi.MockContextFactory;


/**
 * Tests the AlertDefinitionLastFiredTimeUpdater class.
 */
public class AlertDefinitionLastFiredTimeUpdater_test extends TestCase {
    
    private MockAlertDefinitionManagerEJBImpl alertDefMgr;

    /**
     * Creates an instance.
     *
     * @param name
     */
    public AlertDefinitionLastFiredTimeUpdater_test(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        
        alertDefMgr = new MockAlertDefinitionManagerEJBImpl();
        
        // set the initial context factory
        MockContextFactory.setAsInitial();
        
        // now register this EJB in the JNDI
        InitialContext context = new InitialContext();

        MockAlertDefinitionManagerLocalHome localHome = 
            new MockAlertDefinitionManagerLocalHome(alertDefMgr);
        
        context.rebind(AlertDefinitionManagerLocalHome.JNDI_NAME, localHome);
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
        
        MockContextFactory.revertSetAsInitial();
    }
    
    /**
     * Test enqueuing events on the alert definition last fired time updater. 
     * Events with old alert def last fired times will be thrown out.
     * 
     * @throws Exception
     */
    public void testEnqueuingEvents() throws Exception {
        Integer id1 = new Integer(1);
        Integer id2 = new Integer(2);
        
        long time1 = 1000;
        long time2 = 2000;
        
        AlertDefinitionLastFiredUpdateEvent event1 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time1);
        
        AlertDefinitionLastFiredUpdateEvent event2 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time1);
        
        AlertDefinitionLastFiredUpdateEvent event3 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time2);
        
        AlertDefinitionLastFiredUpdateEvent event4 = 
            new AlertDefinitionLastFiredUpdateEvent(id2, time1);
        
        AlertDefinitionLastFiredUpdateEvent event5 = 
            new AlertDefinitionLastFiredUpdateEvent(id2, time2);
        
        // Set the expected order in which the EJB will update the events.
        // Old alert def fired times for an alert def will be thrown out.
        AlertDefinitionLastFiredUpdateEvent[] expectedOrder = 
                new AlertDefinitionLastFiredUpdateEvent[2];
        expectedOrder[0] = event5;
        expectedOrder[1] = event3;
        
        alertDefMgr.setExpectedAlertDefEventsToUpdate(expectedOrder);
        
        List events = new ArrayList();
        events.add(event1);
        events.add(event2);
        events.add(event3);
        events.add(event4);
        events.add(event5);
        
        AlertDefinitionLastFiredTimeUpdater.getInstance().enqueueEvents(events);
        
        alertDefMgr.verify();
    }
    
}
