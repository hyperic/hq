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

package org.hyperic.hq.plugin.informix;

import java.util.HashMap;
import java.util.Map;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

public class InformixMeasurementPlugin extends JDBCMeasurementPlugin
{
    private static final boolean DEBUG = true;
    private static final String JDBC_DRIVER = "com.informix.jdbc.IfxDriver";

    private static final String PROP_DBNAME = "dbname",
                                PROP_TABLENAME = "tablename";

    private Map myQueries = new HashMap();

    public InformixMeasurementPlugin()
    {
        setName(InformixProductPlugin.NAME);
    }

    protected void getDriver() throws ClassNotFoundException
    {
        Class.forName(JDBC_DRIVER);
    }

    protected Connection getConnection(String url, String user, String password)
        throws SQLException
    {
        return DriverManager.getConnection(url, user, password);
    }

    protected String getDefaultURL()
    {
        return "jdbc:informix-sqli://"+getProperty("hostname")+":"+getProperty("port")+
               "/sysmaster:informixserver="+getProperty("servername");
    }

    protected void initQueries() 
    {
        String sqlstmt = "";
/*
        myQueries.put("SpaceAvailable", sqlstmt);
        sqlstmt = "";
        myQueries.put("ChunkIOWrites", sqlstmt);
        sqlstmt = "";
        myQueries.put("ChunkIOReads", sqlstmt);
        sqlstmt = "";
        myQueries.put("Availability", sqlstmt);
        sqlstmt = "";
        myQueries.put("NumberOfLockWaits", sqlstmt);
        sqlstmt = "";
        myQueries.put("NumberOfUsersWaitingForLocks", sqlstmt);
        sqlstmt = "";
        myQueries.put("NumberOfActiveLocks", sqlstmt);
*/

        sqlstmt = "select count(*) from sysdatabases";
        myQueries.put("Availability", sqlstmt);
        sqlstmt = "select sum(lockwts) as numlockwaits from sysptprof";
        myQueries.put("TotNumLocksWaits", sqlstmt);
        sqlstmt = "select sum(lockreqs) as numlockreqs from syssesprof, syssessions "+
                  "where syssesprof.sid = syssessions.sid";
        myQueries.put("TotNumUserWaitLocks", sqlstmt);
        sqlstmt = "select deadlks from sysptprof";
        myQueries.put("TotNumDeadLocks", sqlstmt);
    }

    protected String getQuery(Metric metric)
    {
        String queryVal = metric.getAttributeName();
        String query = (String)myQueries.get(queryVal);
        String inst = metric.getObjectProperties().getProperty(PROP_DBNAME);

        // Backwards compat
        if (inst == null)
            inst = metric.getProperties().getProperty(PROP_TABLENAME);

        return query;
    }
}
