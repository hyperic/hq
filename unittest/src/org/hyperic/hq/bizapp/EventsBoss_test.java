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

package org.hyperic.hq.bizapp;

import org.hyperic.hq.bizapp.server.session.EventsBoss_testEJBImpl;
import org.hyperic.hq.bizapp.shared.EventsBoss_testLocal;
import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;

public class EventsBoss_test extends BaseServerTestCase {

    private LocalInterfaceRegistry _registry;
    private static final String FILENAME = "testUpdateAlertDefs.xml.gz";

    public EventsBoss_test(String name) {
        super(name, true);
    }

    public void setUp() throws Exception {
        super.setUp();
        super.insertSchemaData(FILENAME);
        _registry = deployHQ();
    }
    
    public void tearDown() throws Exception {
        super.deleteSchemaData(FILENAME);
        super.tearDown();
        undeployHQ();
    }

    public void testUpdateAlertDefinition() throws Exception {
        EventsBoss_testLocal eTest =
            (EventsBoss_testLocal)
                _registry.getLocalInterface(
                    EventsBoss_testEJBImpl.class,
                    EventsBoss_testLocal.class);
        eTest.testUpdateAlertDefinition();
    }

}
