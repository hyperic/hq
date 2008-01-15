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

package org.hyperic.hq.plugin.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.JDBCControlPlugin;
import org.hyperic.util.config.ConfigResponse;

import org.hyperic.util.jdbc.DBUtil;

public class OracleControlPlugin extends JDBCControlPlugin
{
    private static final String _logCtx = OracleControlPlugin.class.getName();
    private String _segment;
    private String _tablespace;

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);
        _segment = config.getValue(OracleMeasurementPlugin.PROP_SEGMENT);
        _tablespace = config.getValue(OracleMeasurementPlugin.PROP_TABLESPACE);
    }

    protected Class getDriver()
        throws ClassNotFoundException {
        return Class.forName(OracleMeasurementPlugin.JDBC_DRIVER);
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

        Connection conn = null;
        try
        {
            // Currently only Tables support control
            conn = getConnection(url, user, password);
            if (action.equals("Analyze")) {
                query = "ANALYZE " +
                    ((isTable(conn, _segment, _tablespace)) ?
                    "TABLE " + _segment :
                    "INDEX " + _segment) + " COMPUTE STATISTICS";
            } else { 
                throw new PluginException("Action '" + action +
                                          "' not supported");
            }
            execute(query);
        }
        catch (SQLException e) {
            setMessage(e.getMessage());
            throw new PluginException(e.getMessage(), e);
        } finally {
            DBUtil.closeConnection(_logCtx, conn);
        }
    }

    static boolean isTable(Connection conn, String segment, String tablespace)
        throws SQLException
    {
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = conn.createStatement();
            String sql = "select segment_type from user_segments" +
                         " WHERE segment_name = '" + segment + "'" +
                         " AND tablespace_name = '" + tablespace + "'";
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String type = rs.getString("segment_type");
                if (type.equalsIgnoreCase("table")) {
                    return true;
                } else {
                    return false;
                }
            }
            throw new SQLException("ERROR: segment " + segment +
                                   " does not exist in tablespace " + tablespace);
        } finally {
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, rs);
        }
    }
}
