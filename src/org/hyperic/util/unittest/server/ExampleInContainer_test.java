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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.util.jdbc.DBUtil;

/**
 * An example on how to start the container and execute a query against a 
 * managed object.
 */
public class ExampleInContainer_test extends BaseServerTestCase {
    
    private LocalInterfaceRegistry _registry;

    /**
     * Creates an instance.
     */
    public ExampleInContainer_test(String name) {
        super(name, true);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        super.insertSchemaData("example-unittest.xml.gz");
        _registry = deployHQ();
    }
    
    public void tearDown() throws Exception {
        super.deleteSchemaData("example-unittest.xml.gz");
        super.tearDown();
        undeployHQ();
    }
    
    /**
     * This is an example unit test demonstrating how the in-container unit test 
     * framework can be used to perform local invocations on EJBs.
     * 
     * This also is an example of the DB Overlay framework.  The initial dbsetup
     * does not have any data in the made-up eam_unittest tables.  The overlay
     * populates it during setup(), verifies then deletes the data in tearDown().
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
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = this.getConnectionToHQDatabase();
            stmt = conn.createStatement();
            String sql = "select count(*) from eam_unitests";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                assertTrue(rs.getInt(1) > 0);
            }
            rs.close();
            sql = "select count(*) from eam_unitest_runtime";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                assertTrue(rs.getInt(1) > 0);
            }
        } finally {
            DBUtil.closeJDBCObjects(getClass().getName(), conn, stmt, rs);
        }
    }
    
}
