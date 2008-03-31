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
 * 
 * <pre>
 * <h3>DBOverlay Example</h3>
 * create table eam_unitests
 * (
 *   id int,
 *   name varchar(32),
 *   version int,
 *   description varchar(255),
 *   primary key(id)
 * );
 * 
 * create table eam_unitest_runtime
 * (
 *   id int,
 *   unitest_id int references eam_unitests(id) DEFERRABLE,
 *   startime numeric(24,0),
 *   endtime numeric(24,0),
 *   datapoint numeric(9,5),
 *   primary key(id)
 * );
 * 
 * NOTE: notice the DEFERRABLE keyword on the constraint
 *       (doesn't work for MySQL, which is fine)
 * 
 * file contents ->
 * # NOTE:  order does *NOT* matter for constraints as long as it resolves
 * # before the commit
 * 
 * $ zcat $HQ_HOME/unittest/data/unittests.xml.gz
 * &lt;?xml version='1.0' encoding='UTF-8'?&gt;
 * &lt;dataset&gt;
 *   &lt;eam_unitest_runtime id="0" unitest_id="0" startime="1206553000000" endtime="1206559000000" datapoint="37.00001"/&gt;
 *   &lt;eam_unitests id="0" name="test1" version="1" description="testing import of data" /&gt;
 * &lt;/dataset&gt;
 * </pre>
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
     */
    public void testQueryAlertDefinitionManager() throws Exception {        
        AlertDefinitionManagerLocal adMan = 
            (AlertDefinitionManagerLocal)
                 _registry.getLocalInterface(AlertDefinitionManagerEJBImpl.class, 
                                             AlertDefinitionManagerLocal.class);
        
        
        Integer id = adMan.getIdFromTrigger(new Integer(-1));
        
        assertNull("shouldn't have found alert def id", id);
    }
    
    /**
     * This is an example of the DB Overlay framework.  The initial dbsetup
     * does not have any data in the made-up eam_unittest tables.  The overlay
     * populates it during setup(), verifies then deletes the data in tearDown().
     */
    public void testDBOverlay() throws Exception {
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
    
    /**
     * This test is merely here to test that multiple hq.ear deployments 
     * work within the same server instance, especially when the same 
     * EJB type is referenced between deployments.
     */
    public void testDeployAgain() throws Exception {
        AlertDefinitionManagerLocal adMan = 
            (AlertDefinitionManagerLocal)
                 _registry.getLocalInterface(AlertDefinitionManagerEJBImpl.class, 
                                             AlertDefinitionManagerLocal.class);
        
        
        Integer id = adMan.getIdFromTrigger(new Integer(-1));
        
        assertNull("shouldn't have found alert def id", id);
    }
    
}
