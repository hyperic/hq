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

package org.hyperic.hq.plugin.bugzilla;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BugzillaServiceDetector 
    extends ServerDetector
{
    private static final String OWNERQUERY =
        "SELECT login_name, userid, COUNT(*) from profiles " +
        "LEFT OUTER JOIN bugs ON userid=assigned_to GROUP BY assigned_to";

    private static final String GROUPQUERY =
        "SELECT program, value FROM versions";
    
    private static final String SERVER_NAME = "Bugzilla";
    private static final String VERSION_2 = "2.x";

    private Log log = LogFactory.getLog("MySQLServiceDetector");

    /**
     * Discover all MySQL Table services
     */
    protected List discoverServices(ConfigResponse config)
        throws PluginException
    {
        String url = config.getValue(BugzillaMeasurementPlugin.PROP_URL);
        String user = config.getValue(BugzillaMeasurementPlugin.PROP_USER);
        String pass = config.getValue(BugzillaMeasurementPlugin.PROP_PASSWORD);
        
        try {
            Class.forName(BugzillaMeasurementPlugin.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            // No driver.  Should not happen.
            throw new PluginException("Unable to load JDBC " +
                                      "Driver: " + e.getMessage());
        }

        // Do Groups Discovery
        ArrayList services = new ArrayList();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        // Discover all MySQL tables.
        try {
            conn = DriverManager.getConnection(url, user, pass);
            stmt = conn.createStatement();
            
            try {
                rs = stmt.executeQuery(GROUPQUERY);
        
                while (rs.next()) {
                    String program = rs.getString(1);
                    String version = rs.getString(2);
        
                    ServiceResource service = new ServiceResource();
                    service.setType(this, BugzillaMeasurementPlugin.GROUP);
                    service.setServiceName(program + " " + version);
        
                    ConfigResponse productConfig = new ConfigResponse();
                    productConfig.setValue(
                            BugzillaMeasurementPlugin.PROP_VERSION, version);
                    productConfig.setValue(
                            BugzillaMeasurementPlugin.PROP_PROGRAM, program);
    
                    service.setProductConfig(productConfig);
                    service.setMeasurementConfig();
        
                    services.add(service);
                }
            } catch (SQLException e) {
                throw new PluginException("Error querying for Bugzilla " +
                                          "groups: " + e.getMessage());
            } finally {
                DBUtil.closeResultSet(this.log, rs);
            }
            
            try {
                rs = stmt.executeQuery(OWNERQUERY);
        
                while (rs.next()) {
                    String login  = rs.getString(1);
                    String userid = rs.getString(2);
        
                    ServiceResource service = new ServiceResource();
                    service.setType(this, BugzillaMeasurementPlugin.OWNER);
                    service.setServiceName(login);
        
                    ConfigResponse productConfig = new ConfigResponse();
                    ConfigResponse metricConfig = new ConfigResponse();
        
                    metricConfig.setValue(
                            BugzillaMeasurementPlugin.PROP_USERID, userid);
    
                    service.setProductConfig(productConfig);
                    service.setMeasurementConfig(metricConfig);
        
                    services.add(service);
                }
            } catch (SQLException e) {
                throw new PluginException("Error querying for Bugzilla " +
                                          "owners: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new PluginException("Error creating connection and stmt: " +
                                      e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }

        return services;
    }
}
