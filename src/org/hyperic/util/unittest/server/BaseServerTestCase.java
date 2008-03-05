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

import java.io.File;

import junit.framework.TestCase;

/**
 * The test case that all server-side unit tests should extend. Before starting 
 * the server, the user must set the <code>UNITTEST_JBOSS_HOME</code> environment 
 * variable to the path where the jboss deployment that will be used for testing 
 * is deployed. That deployed jboss instance must contain a "unittest" configuration 
 * that has already had the Ant <code>prepare-jboss</code> target run against it.
 */
public abstract class BaseServerTestCase extends TestCase {
    
    /**
     * The environment variable specifying the path to the jboss deployment 
     * that will be used for unit testing.
     */
    public static final String JBOSS_HOME_DIR_ENV_VAR = "UNITTEST_JBOSS_HOME";
    
    /**
     * The "unittest" configuration that the jboss deployment must have installed 
     * and prepared using the Ant "prepare-jboss" target.
     */
    public static final String JBOSS_UNIT_TEST_CONFIGURATION = "unittest";
    
    private static ServerLifecycle server;
    

    public BaseServerTestCase(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        super.setUp();
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    protected final void startServer() throws Exception {        
        if (server == null || !server.isStarted()) {
            String jbossHomeDir = System.getenv(JBOSS_HOME_DIR_ENV_VAR);
            
            if (jbossHomeDir == null) {
                throw new IllegalStateException("The "+JBOSS_HOME_DIR_ENV_VAR+
                                " environment variable was not set");
            }
            
            server = new ServerLifecycle(new File(jbossHomeDir), 
                                         JBOSS_UNIT_TEST_CONFIGURATION);
            server.startServer();
        }
        
        assertTrue(server.isStarted());
    }
    
    protected final void stopServer() {
        if (server != null) {
            server.stopServer();
            assertFalse(server.isStarted());
            server = null;
        }
    }
    
}
