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

package org.hyperic.hq.plugin.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.JDBCControlPlugin;

import org.hyperic.util.jdbc.DBUtil;

public class MySQLControlPlugin extends JDBCControlPlugin {

    protected Class getDriver()
        throws ClassNotFoundException {
        return Class.forName(MySQLMeasurementPlugin.JDBC_DRIVER);
    }
    
    protected Connection getConnection(String url,
                                       String user,
                                       String password)
        throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void doAction(String action)
        throws PluginException
    {
        String query;

        // Currently only Tables support control
        if (action.equals("Analyze")) {
            query = "ANALYZE TABLE " + table;
        } else if (action.equals("Check")) {
            query = "CHECK TABLE " + table;
        } else if (action.equals("Optimize")) {
            query = "OPTIMIZE TABLE " + table;
        } else if (action.equals("Repair")) {
            query = "REPAIR TABLE " + table;
        } else { 
            throw new PluginException("Action '" + action +
                                      "' not supported");
        }

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        setResult(RESULT_FAILURE);
        try {
            conn = getConnection(url, user, password);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            
            setResult(RESULT_SUCCESS);
            // Get result from the query
            if (rs != null && rs.next()) {
                String result = rs.getString("Msg_text");
                setMessage("Result: " + result);
            }
        } catch (SQLException e) {
            // Error running control command.
            setMessage(e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(getLog(), conn, stmt, rs);
        }
    }
}
