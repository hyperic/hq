/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.plugin.mysql_stats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.product.JDBCControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.jdbc.DBUtil;

public class MySqlStatsControlPlugin extends JDBCControlPlugin {

    public void doAction(String action) throws PluginException {
        final String query = getQuery(action);
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
            final Map columns = getColumnMap(rs);
            final StringBuffer result = new StringBuffer();
            if (rs != null && rs.next()) {
                for (Iterator it=columns.entrySet().iterator(); it.hasNext(); ) {
                    final Map.Entry entry = (Map.Entry)it.next();
                    final String name = (String)entry.getKey();
                    final Integer col = (Integer)entry.getValue();
                    result.append(name)
                        .append(": ")
                        .append(rs.getString(col.intValue()))
                        .append('\n');
                }
                setMessage("Result: " + result.toString());
                return;
            }
        } catch (SQLException e) {
            // Error running control command.
            setMessage(e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(getLog(), conn, stmt, rs);
        }
        throw new PluginException("Action '" + action +
            "' did not execute properly " + "");
    }

    private Map getColumnMap(ResultSet rs) throws SQLException {
        Map rtn = new HashMap();
        ResultSetMetaData rsmd = rs.getMetaData();
        final int count = rsmd.getColumnCount();
        for (int i=1; i<=count; i++) {
            final String name = rsmd.getColumnName(i);
            final int col = rs.findColumn(name);
            rtn.put(name, new Integer(col));
        }
        return rtn;
    }

    private String getQuery(final String action) throws PluginException {
        // Currently only Tables support control
        final String table = config.getValue("table"),
                     dbname = config.getValue("database");
        if (action.equals("Analyze")) {
            return "ANALYZE TABLE " + dbname + "." + table;
        } else if (action.equals("Check")) {
            return "CHECK TABLE " + dbname + "." + table;
        } else if (action.equals("Optimize")) {
            return "OPTIMIZE TABLE " + dbname + "." + table;
        } else if (action.equals("Repair")) {
            return "REPAIR TABLE " + dbname + "." + table;
        } else { 
            throw new PluginException("Action '" + action + "' not supported");
        }
    }

    protected Connection getConnection(String url, String user, String pass)
        throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);
        return DriverManager.getConnection(url, props);
    }

    protected Class getDriver() throws ClassNotFoundException {
        return Class.forName(MySqlStatsMeasurementPlugin.DEFAULT_DRIVER);
    }

}
