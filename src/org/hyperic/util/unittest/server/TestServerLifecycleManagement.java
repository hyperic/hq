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



/**
 * A test case tasked with testing the server lifecycle management operations 
 * provided in the super class.
 */
public class TestServerLifecycleManagement extends BaseServerTestCase {

    public TestServerLifecycleManagement(String name) {
        super(name);
    }
                
    /**
     * Test starting the jboss server without deploying the HQ application.
     * 
     * @throws Exception
     */
    public void testStartServerNoHQDeployment() throws Exception {
        startServer();
    }
    
    /**
     * Test starting the jboss server and deploying the HQ application.
     * 
     * @throws Exception
     */
    public void testStartServerDeployHQ() throws Exception {
        startServer();
        deployHQ(false);
    }
    
    /**
     * Test starting the jboss server and deploying the HQEE application.
     * 
     * @throws Exception
     */
    public void testStartServerDeployHQEE() throws Exception {
        startServer();
        deployHQ(true);
    }
    
    /**
     * Test deploying the HQEE application without starting the jboss server. 
     * The server should be started before the deployment.
     * 
     * @throws Exception
     */
    public void testDeployHQEEWithoutStartingFirst() throws Exception {
        deployHQ(true);
    }
    
    /**
     * Test deploying then undeploying the HQ application.
     * 
     * @throws Exception
     */
    public void testDeployAndUndeployHQ() throws Exception {
        deployHQ(false);
        undeployHQ();
    }
    
    /**
     * Test deploying then undeploying the HQEE application.
     * 
     * @throws Exception
     */
    public void testDeployAndUndeployHQEE() throws Exception {
        deployHQ(true);
        undeployHQ();
    }

}
