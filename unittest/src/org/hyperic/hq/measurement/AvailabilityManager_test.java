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

package org.hyperic.hq.measurement;

import org.hyperic.hq.measurement.server.session.AvailabilityManager_testEJBImpl;
import org.hyperic.hq.measurement.shared.AvailabilityManager_testLocal;
import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;

public class AvailabilityManager_test extends BaseServerTestCase {
    
    private LocalInterfaceRegistry _registry;
    private static final String FILENAME = "availabilityTests.xml.gz";

    public AvailabilityManager_test(String name) {
        super(name, true);
    }
    
    public void setUp() throws Exception {
        // Disable DB dump and restore, it does not work on Oracle.
        //super.setUp();
        super.setUp();
        super.insertSchemaData(FILENAME);
        _registry = deployHQ();
    }

    public void tearDown() throws Exception {
        // Disable DB dump and restore, it does not work on Oracle.
        //super.tearDown();
        super.deleteSchemaData(FILENAME);
        super.tearDown();
        undeployHQ();
    }
    
    public void testRLE() throws Exception {
        AvailabilityManager_testLocal aTest =
            (AvailabilityManager_testLocal)
                _registry.getLocalInterface(
                    AvailabilityManager_testEJBImpl.class,
                    AvailabilityManager_testLocal.class);
        aTest.testInsertScenarios();
    }
    
}
