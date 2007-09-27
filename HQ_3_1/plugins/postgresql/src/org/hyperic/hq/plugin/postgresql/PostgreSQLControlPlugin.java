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

package org.hyperic.hq.plugin.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.JDBCControlPlugin;

public class PostgreSQLControlPlugin extends JDBCControlPlugin {

    protected Class getDriver()
        throws ClassNotFoundException {
        return Class.forName(PostgreSQLMeasurementPlugin.JDBC_DRIVER);
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
        String query = null;

        if (this.index != null) {
            // Index actions
            if (action.equals("Reindex")) {
                query = "REINDEX INDEX " + this.index;
            }
        } else if (this.table != null) {
            // Table actions
            if (action.equals("Analyze")) {
                query = "ANALYZE " + this.table;
            } else if (action.equals("Vacuum")) {
                query = "VACUUM " + this.table;
            } else if (action.equals("VacuumAnalyze")) {
                query = "VACUUM ANALYZE " + this.table;
            } else if (action.equals("Reindex")) {
                query = "REINDEX TABLE " + this.table;
            }
        } else {
            // Server actions
            if (action.equals("Analyze")) {
                query = "ANALYZE";
            } else if (action.equals("Vacuum")) {
                query = "VACUUM";
            } else if (action.equals("VacuumAnalyze")) {
                query = "VACUUM ANALYZE";
            } else if (action.equals("ResetStatistics")) {
                // Special case for reset statistics
                executeQuery("SELECT pg_stat_reset()");
                return;
            }
        }

        if (query == null) {
            throw new PluginException("Action '" + action +
                                      "' not supported");
        }

        execute(query);
    }
}
