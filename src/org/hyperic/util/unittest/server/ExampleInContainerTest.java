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

package org.hyperic.util.unittest.server;

import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;

/**
 * An example on how to start the container and execute a query against a 
 * managed object.
 */
public class ExampleInContainerTest extends BaseServerTestCase {
    
    private LocalInterfaceRegistry _registry;

    /**
     * Creates an instance.
     */
    public ExampleInContainerTest(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        _registry = deployHQ(false);
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
        undeployHQ();
    }
    
    /**
     * This is an example unit test demonstrating how the in-container unit test 
     * framework can be used to perform local invocations on EJBs. Note that this 
     * example DOES NOT show how to use the database overlay framework.
     * 
     * @throws Exception
     */
    public void testQueryAlertDefinitionManager() throws Exception {        
        AlertDefinitionManagerLocal adMan = 
            (AlertDefinitionManagerLocal)
                 _registry.getLocalInterface(AlertDefinitionManagerEJBImpl.class, 
                                             AlertDefinitionManagerLocal.class);
        
        
        Integer id = adMan.getIdFromTrigger(new Integer(-1));
        
        assertNull("shouldn't have found alert def id", id);        
    }
    
}
