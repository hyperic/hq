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

package org.hyperic.hq.product;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.util.jdbc.DBUtil;

public abstract class JDBCControlPlugin extends ControlPlugin {

    // Plugin config
    protected String url;
    protected String user;
    protected String password;
    protected String table = null;
    protected String index = null;

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);

        // Initialize configuration.
        this.url = config.getValue(JDBCMeasurementPlugin.PROP_URL);
        this.user = config.getValue(JDBCMeasurementPlugin.PROP_USER);
        this.password = 
            config.getValue(JDBCMeasurementPlugin.PROP_PASSWORD);
        // Optional
        this.table = 
            config.getValue(JDBCMeasurementPlugin.PROP_TABLE);
        this.index =
            config.getValue(JDBCMeasurementPlugin.PROP_INDEX);

        // Validate.
        Connection conn = null;

        try {
            getDriver();
            conn = getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            // No driver.  Should not happen.
            throw new PluginException("Unable to load JDBC " +
                                      "Driver: " + e.getMessage());
        } catch (SQLException e) {
            // Invalid configuration.
            throw new PluginException("Unable to obtain JDBC " +
                                      "Connection: " + 
                                      e.getMessage());
        } finally {
            if (conn != null) {
                try { 
                    conn.close();
                } catch (Exception e) {
                    getLog().warn("Error closing connection: " +
                                  e.getMessage());
                }
            }
        }
    }

    public boolean isRunning() {

        Connection conn = null;
        try {
            conn = getConnection(url, user, password);
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            if (conn != null) {
                try { 
                    conn.close();
                } catch (Exception e) {
                    getLog().warn("Error closing connection: " +
                                  e.getMessage());
                }
            }
        }
    }

    /**
     * The plugin must preform the Class.forName so its
     * ClassLoader is used to find the driver.
     */
    protected abstract Class getDriver()
        throws ClassNotFoundException;

    /**
     * The plugin must preform the DriverManager.getConnection so its
     * ClassLoader is used to find the driver.
     */
    protected abstract Connection getConnection(String url,
                                                String user,
                                                String password)
        throws SQLException;

    public abstract void doAction(String action)
        throws PluginException;

    public void execute(String query)
        throws PluginException
    {
        Connection conn = null;
        Statement stmt = null;

        setResult(RESULT_FAILURE);

        try {
            conn = getConnection(url, user, password);
            stmt = conn.createStatement();
            stmt.execute(query);

            setResult(RESULT_SUCCESS);
        } catch (SQLException e) {
            // Error running control command.
            setMessage(e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(getLog(), conn, stmt, null);
        }
    }

    public void executeQuery(String query)
        throws PluginException
    {
        Connection conn = null;
        Statement stmt = null;

        setResult(RESULT_FAILURE);

        try {
            conn = getConnection(url, user, password);
            stmt = conn.createStatement();
            stmt.executeQuery(query);

            conn.commit();
            setResult(RESULT_SUCCESS);
        } catch (SQLException e) {
            // Error running control command.
            setMessage(e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(getLog(), conn, stmt, null);
        }
    }
}
