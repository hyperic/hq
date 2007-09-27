/**
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
**/

package org.hyperic.hq.plugin.informix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricValue;

public class InformixMeasurementPlugin extends JDBCMeasurementPlugin
{
    private static final boolean DEBUG = InformixPluginUtil.DEBUG;

    static final String PROP_DBNAME    = InformixPluginUtil.PROP_DBNAME,
                        PROP_TABLENAME = InformixPluginUtil.PROP_TABLENAME,
                        PROP_CHUNK     = InformixPluginUtil.PROP_CHUNK,
                        JDBC_DRIVER    = InformixPluginUtil.JDBC_DRIVER,
                        LOCKREQUESTS   = "lockreqs",
                        LOCKWAITS      = "lockwts",
                        DEADLOCKS      = "deadlks",
                        IO_READS       = "bufreads",
                        IO_WRITES      = "bufwrites";

    private Map myQueries = new HashMap(),
                myFields = new HashMap();

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
        return "jdbc:informix-sqli://localhost:3500/sysmaster:informixserver=test_shm";
    }

    protected void initQueries() 
    {
        String sqlstmt = "select c.nfree*c.pagesize free_space "+
                         "from sysdbspaces d, syschunks c "+
                         "where d.dbsnum = c.dbsnum and d.name = '%chunk%' "+
                         "group by 1";
        myQueries.put("ChunkSpaceAvailable", sqlstmt);
        myQueries.put("TotNumCurrentSessionWrites", getCurrentSessionStatsSQL(IO_WRITES));
        myQueries.put("TotNumCurrentSessionReads", getCurrentSessionStatsSQL(IO_READS));
        myQueries.put("NumberOfLockWaits", getTableLockStatsSQL(LOCKWAITS));
        myQueries.put("NumberOfDeadLocks", getTableLockStatsSQL(DEADLOCKS));
        sqlstmt = "select count(*) from sysdatabases";
        myQueries.put("Availability", sqlstmt);
        sqlstmt = "select sum(lockwts) as numlockwaits from sysptprof";
        myQueries.put("TotNumLockWaits", sqlstmt);
        sqlstmt = "select sum(lockreqs) as numlockreqs from syssesprof, syssessions "+
                  "where syssesprof.sid = syssessions.sid";
        myQueries.put("TotNumUserWaitLocks", sqlstmt);
        sqlstmt = "select deadlks from sysptprof";
        myQueries.put("TotNumDeadLocks", sqlstmt);
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricUnreachableException,
               MetricInvalidException,
               MetricNotFoundException
    {
        String objectName = metric.getObjectName(),
               attr       = metric.getAttributeName();
        if (objectName.indexOf("DBSpace") == -1 || attr.indexOf("ChunkIO") == -1)
            return super.getValue(metric);

        try
        {
            String chunkName = metric.getObjectProperty(PROP_CHUNK);
            Connection conn = getCachedConnection(metric);
            long value = getChunkIOStats(attr, chunkName, conn).longValue();
            return new MetricValue(value, System.currentTimeMillis());
        }
        catch (SQLException e) {
            String msg = "Query failed for "+attr+": "+e.getMessage();
            throw new MetricNotFoundException(msg, e);
        }
    }

    private static Long getChunkIOStats(String attr, String chunkName, Connection conn)
        throws SQLException
    {
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            String field = "writes";
            if (attr.equals("ChunkIOPageWrites"))
                field = "pageswritten";
            else if (attr.equals("ChunkIOPageReads"))
                field = "pagesread";
            else if (attr.equals("ChunkIOReads"))
                field = "reads";
            else //attr.equals("ChunkIOWrites")
                field = "writes";
            stmt = conn.createStatement();
            List sql_list = getChunkIOStatsSQL(field, attr, chunkName, stmt);
            stmt.execute((String)sql_list.get(0));
            stmt.execute((String)sql_list.get(1));
            rs = stmt.executeQuery((String)sql_list.get(2));
            if (rs.next())
              return new Long(rs.getLong(field));
        }
        finally
        {
            try
            {
                if (rs != null) 
                    rs.close();
                if (stmt != null)
                {
                    stmt.execute("drop table A");
                    stmt.execute("drop table B");
                    stmt.close();
                }
            } catch (SQLException e) { }
        }
        throw new SQLException();
    }

    private static List getChunkIOStatsSQL(String field, String attr,
                                           String chunkName, Statement stmt)
    {
        List rtn = new ArrayList();

        String sql = //-- Collect chunk IO stats into temp table A
               "select name dbspace, chknum, \"Primary\" chktype, reads, "+
               "writes, pagesread, pageswritten "+
               "from syschktab c, sysdbstab d "+
               "where c.dbsnum = d.dbsnum "+
               "union all "+
               "select name[1,10] dbspace, chknum, \"Mirror\" chktype, reads,"+
               "writes, pagesread, pageswritten "+
               "from sysmchktab c, sysdbstab d "+
               "where c.dbsnum = d.dbsnum "+
               "into temp A\n\n";
        rtn.add(sql);
        sql =  //-- Collect total IO stats into temp table B
               "select sum(reads) total_reads, sum(writes) total_writes, "+
               "sum(pagesread) total_pgreads,sum(pageswritten) total_pgwrites "+
               "from A "+
               "into temp B\n\n";
        rtn.add(sql);
        sql =  //-- Report showing each chunks percent of total IO
               "select "+field+" from A, B "+
               "where dbspace = '"+chunkName+"'";
        rtn.add(sql);
        return rtn;
    }

    protected String getQuery(Metric metric)
    {
        String queryVal = metric.getAttributeName(),
               query    = (String)myQueries.get(queryVal),
               attr     = metric.getAttributeName(),
               objectName = metric.getObjectName();

        boolean isAvail = attr.equals(AVAIL_ATTR);

        if (objectName.indexOf("Type=Server") != -1)
            return (String)myQueries.get(attr);

        else if (objectName.indexOf("Type=Table") != -1)
        {
            String dbname = metric.getObjectProperty(PROP_DBNAME),
                   table  = metric.getObjectProperty(PROP_TABLE),
                   sql    = myQueries.get(attr).toString().
                                      replaceAll("%dbname%", dbname).
                                      replaceAll("%table%", table);
            return sql;
        }
        else if (objectName.indexOf("Type=DBSpace") != -1)
        {
            String chunk = metric.getObjectProperty(PROP_CHUNK),
                   sql   = myQueries.get(attr).toString().
                                     replaceAll("%chunk%", chunk);
            return sql;
        }
        return null;
    }

    private String getCurrentSessionStatsSQL(String field)
    {
        return "select sum("+field+") "+
               "from syssesprof, syssessions "+
               "where syssesprof.sid = syssessions.sid";
    }

    private String getTableLockStatsSQL(String field)
    {
        return "select "+field+" "+
               "from sysptprof "+
               "where tabname = '%table%' and dbsname = '%dbname%'";
    }
}
