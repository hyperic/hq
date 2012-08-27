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

package org.hyperic.hq.plugin.sybase;

import java.util.HashMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;

import org.hyperic.hq.product.PluginManager;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginInfo;


public class SybaseMeasurementPlugin 
    extends JDBCMeasurementPlugin
{
    private final Log log = getLog();
    private static final String JDBC_DRIVER = 
        "com.sybase.jdbc3.jdbc.SybDriver";

    private static final String DEFAULT_URL = SybasePluginUtil.DEFAULT_URL;

    private static final String PROP_INSTANCE = "instance",
                                TYPE_SP_MONITOR_CONFIG =
                                    SybasePluginUtil.TYPE_SP_MONITOR_CONFIG,
                                TYPE_STORAGE = SybasePluginUtil.TYPE_STORAGE,
                                PROP_DATABASE = SybasePluginUtil.PROP_DATABASE,
                                PROP_SEGMENT  = SybasePluginUtil.PROP_SEGMENT,
                                PROP_PAGESIZE = SybasePluginUtil.PROP_PAGESIZE,
                                PROP_CONFIG_OPTION = SybasePluginUtil.PROP_CONFIG_OPTION,
                                PERCENT_ACTIVE     = SybasePluginUtil.PERCENT_ACTIVE,
                                NUM_ACTIVE = SybasePluginUtil.NUM_ACTIVE,
                                MAX_USED   = SybasePluginUtil.MAX_USED,
                                NUM_FREE   = SybasePluginUtil.NUM_FREE,
                                NUM_REUSED = SybasePluginUtil.NUM_REUSED;

    private static HashMap syb12Queries    = null;  // Sybase 12.5 only
    private static HashMap genericQueries  = null;  // Any
    private static HashMap connectionCache = new HashMap();


    protected void getDriver()
        throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
    }

    protected Connection getConnection(String url,
                                       String user,
                                       String password)
        throws SQLException
    {
        String pass = (password == null) ? "" : password;
        pass = (pass.matches("^\\s*$")) ? "" : pass;
        java.util.Properties props = new java.util.Properties();
        props.put("CHARSET_CONVERTER_CLASS",
            "com.sybase.jdbc3.utils.TruncationConverter");
        props.put("user", user);
        props.put("password", pass);
        return DriverManager.getConnection(url, props);
    }

    protected String getDefaultURL() {
        return DEFAULT_URL;
    }

    protected void initQueries()
    {
        if (genericQueries != null)
            return;

        syb12Queries = new HashMap();
        genericQueries = new HashMap();

        String baseQuery = "SELECT ";
        String baseTxQuery = "SELECT COUNT(*) FROM systransactions ";
        String baseIndQuery = "SELECT COUNT(*) FROM sysindexes ";

        genericQueries.put("NumUserTables", baseQuery + 
                           "COUNT(*) FROM sysobjects WHERE type='U'");
        genericQueries.put("NumServers", baseQuery + 
                           "COUNT(*) FROM sysservers");

        // Transactions
        genericQueries.put("NumTx", baseTxQuery);

        // Transactions by type
        genericQueries.put("NumLocalTransactions", baseTxQuery + 
                           "WHERE type=1");
        genericQueries.put("NumExternalTransactions", baseTxQuery + 
                           "WHERE type=3");
        genericQueries.put("NumRemoteTransactions", baseTxQuery + 
                           "WHERE type=98");
        genericQueries.put("NumDtxTransactions", baseTxQuery + 
                           "WHERE type=99");

        // Uptime
        genericQueries.put("UpTime",
                           "SELECT MAX(datediff(ss, " + 
                           "loggedindatetime, getdate()) * 1000) " +
                           "FROM sysprocesses");

        // Instance total, used and free space
        genericQueries.put("InstanceUsedSpace",
                           "select sum(u.size) * 1024 " +
                           "from sysusages u, sysdevices d " +
                           "where u.vstart between d.low and d.high " +
                           "and UPPER(d.name) = UPPER('%instance%')");
        genericQueries.put("InstanceFreeSpace",
                           "select ((d.high - d.low) + 1 - sum(u.size)) " +
                           "* 1024 " +
                           "from sysusages u, sysdevices d " +
                           "where u.vstart between d.low and d.high " +
                           "and UPPER(d.name) = UPPER('%instance%')");
        genericQueries.put("InstanceTotalSpace",
                           "select ((d.high - d.low) + 1) * 1024 " +
                           "from sysusages u, sysdevices d " +
                           "where u.vstart between d.low and d.high " +
                           "and UPPER(d.name) = UPPER('%instance%')");

        // Page locks (table and page)
        genericQueries.put("NumActiveLocks", baseQuery + 
                           "COUNT(*) FROM syslocks");

        genericQueries.put("NumActivePageLocks", baseQuery +
                           "COUNT(*) FROM syslocks WHERE type = 1 OR " +
                           "type = 2");
        genericQueries.put("NumActiveTableLocks", baseQuery +
                           "COUNT(*) FROM syslocks WHERE type = 4 OR " +
                           "type = 5 OR type = 6");

        // Number of active users
        genericQueries.put("ActiveUsers", baseQuery + 
                           "COUNT(*) FROM sysprocesses WHERE suid > 0");

        // Transaction by connection type
        genericQueries.put("NumAttachedTransactions", baseTxQuery + 
                           "WHERE connection=1");
        genericQueries.put("NumDetachedTransactions", baseTxQuery + 
                           "WHERE connection=2");

        // Transaction by state
        genericQueries.put("NumTxInBegun", baseTxQuery + 
                           "WHERE state=1");
        genericQueries.put("NumTxInDoneCmd", 
                           baseTxQuery + "WHERE state=2");
        genericQueries.put("NumTxInDone", baseTxQuery + 
                           "WHERE state=3");
        genericQueries.put("NumTxInPrepared", baseTxQuery + 
                           "WHERE state=4");
        genericQueries.put("NumTxInInCmd", baseTxQuery + 
                           "WHERE state=5");
        genericQueries.put("NumTxInInAbortCmd",
                           baseTxQuery + "WHERE state=6");
        genericQueries.put("NumTxInCommitted", baseTxQuery + 
                           "WHERE state=7");
        genericQueries.put("NumTxInInPostCommit", 
                           baseTxQuery + "WHERE state=8");
        genericQueries.put("NumTxInInAbortTran", 
                           baseTxQuery + "WHERE state=9");
        genericQueries.put("NumTxInInAbortSavept",
                           baseTxQuery + "WHERE state=10");
        genericQueries.put("NumTxInBegunDetached",
                           baseTxQuery + "WHERE state=65537");
        genericQueries.put("NumTxInDoneCmdDetached",
                           baseTxQuery + "WHERE state=65538");
        genericQueries.put("NumTxInDoneDetached",
                           baseTxQuery + "WHERE state=65539");
        genericQueries.put("NumTxInPrepareDetached",
                           baseTxQuery + "WHERE state=65540");
        genericQueries.put("NumTxInHeurCommitted",
                           baseTxQuery + "WHERE state=65548");
        genericQueries.put("NumTxInHeurRolledBack",
                           baseTxQuery + "WHERE state=65549");

        // Indices
        genericQueries.put("NumIndexes", baseIndQuery +
                           "WHERE NOT indid=0");
        genericQueries.put("NumLobIndexes", baseIndQuery +
                           "WHERE indid = 255");
        genericQueries.put("NumLargeRowSize", baseQuery +
                           "MAX(exp_rowsize) FROM sysindexes");

        // Transaction log
        genericQueries.put("NumTxLogs", baseQuery +
                           "COUNT(*) FROM syslogs");
        genericQueries.put("LargestUpdateCountOfAnyLog", baseQuery +
                           "MAX(op) FROM syslogs");

        //alias for avail.
        //if we can fetch any metric, consider the server available
        //XXX this check can be more robust
        genericQueries.put("Availability",
                           genericQueries.get("NumServers"));
    }

    protected String getQuery(Metric metric)
    {
        String queryVal = metric.getAttributeName();
        String query = (String)genericQueries.get(queryVal);
        
        if (query == null) {
            // Not in the generic queries, check the version specific table
            // XXX: grab the version from the Metric
            query = (String)syb12Queries.get(queryVal);
        }

        // Do substituion on the user name in the SQL query
        String instance = metric.getObjectProperties().getProperty(PROP_INSTANCE);
        if (instance == null) {
            // Backwards compat
            instance = metric.getProperties().getProperty(PROP_INSTANCE);
        }

        query = StringUtil.replace(query, "%instance%", instance);
        return query;
    }

    public MetricValue getValue(Metric metric)
            throws PluginException,
            MetricUnreachableException,
            MetricInvalidException,
               MetricNotFoundException
    {
        if(log.isDebugEnabled())
           log.debug("[getValue] metric="+metric);
        initQueries();
        String objectName = metric.getObjectName(),
               alias      = metric.getAttributeName();
        if (objectName.indexOf(TYPE_SP_MONITOR_CONFIG) == -1
            && objectName.indexOf(TYPE_STORAGE) == -1
            && !metric.isAvail()) {
            return super.getValue(metric);
        }

        Properties props = metric.getProperties();
        String url = props.getProperty(PROP_URL);
        String user = props.getProperty(PROP_USER);
        String pass = props.getProperty(PROP_PASSWORD);
        if (url == null) {
            throw new MetricUnreachableException("URL = null");
        }

        MetricValue res = null;
        Connection conn = null;
        try {
            // do not close cached connection
            conn = getCachedConnection(url, user, pass);
        } catch (SQLException e) {
            removeCachedConnection(url, user, pass);
            DBUtil.closeConnection("[getValue]", conn);
            String msg = "Commection failed for '" + alias + "': " + e.getMessage();
            throw new MetricNotFoundException(msg, e);
        }

        try {
            if (objectName.indexOf(TYPE_SP_MONITOR_CONFIG) != -1) {
                res = getSP_MonitorConfigValue(metric, alias, conn);
            } else if (objectName.indexOf(TYPE_STORAGE) != -1) {
                res = getStorageValue(metric, alias, conn);
            } else if (metric.isAvail()) {
                res = getAvailability(conn);
            }
        } catch (SQLException e) {
            removeCachedConnection(url, user, pass);
            DBUtil.closeConnection(null, conn);
            String msg = "Query failed for '" + alias + "': " + e.getMessage();
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_DOWN);
            } else {
                throw new MetricNotFoundException(msg, e);
            }
        }
        if (res == null) {
            throw new MetricNotFoundException("cannot find metric " + metric);
        }
        if (log.isDebugEnabled()) {
            log.debug("[getValue] alias='" + alias + "' res='" + res + "' metric="+metric);
        }
        return res;
    }

    private MetricValue getAvailability(Connection conn)
    {
        Statement stmt = null;
        ResultSet rs = null;
        double res=Metric.AVAIL_DOWN;
        try
        {
            stmt = conn.createStatement();
            String sql = (String)genericQueries.get("NumServers");
            rs = stmt.executeQuery(sql);
            res=Metric.AVAIL_UP;
        }
        catch (SQLException e) {
            log.debug("Query failed for Availability "+e.getMessage(),e);
        }
        finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
        return new MetricValue(res, System.currentTimeMillis());
    }

    private MetricValue getStorageValue(Metric metric,
                                        String attr,
                                        Connection conn)
        throws SQLException, MetricNotFoundException
    {
        String database = metric.getObjectProperty(PROP_DATABASE),
                segment = metric.getObjectProperty(PROP_SEGMENT);
        int pagesize = Integer.parseInt(metric.getObjectProperty(PROP_PAGESIZE));
        Statement stmt = null;
        ResultSet rs = null;
        MetricValue res = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("use " + database);
            stmt.execute("sp_helpsegment '" + segment + "'");
            rs = getResultSet(stmt, "total_pages");
            if (rs.next()) {
                long total_pages = rs.getLong("total_pages"),
                        free_pages = rs.getLong("free_pages"),
                        used_pages = rs.getLong("used_pages");
                if (attr.equals("PercentUsed")) {
                    float percent_used = (getSegmentSize(used_pages, pagesize) / getSegmentSize(total_pages, pagesize));
                    res = new MetricValue(percent_used, System.currentTimeMillis());
                } else if (attr.equals("StorageUsed")) {
                    float storage_used = getSegmentSize(used_pages, pagesize);
                    res = new MetricValue(storage_used, System.currentTimeMillis());
                } else if (metric.isAvail()) {
                    res = new MetricValue(Metric.AVAIL_UP, System.currentTimeMillis());
                } else {
                    throw new MetricNotFoundException("[getStorageValue] Metric => '" + attr + "'");
                }
            } else {
                res = new MetricValue(Metric.AVAIL_DOWN, System.currentTimeMillis());
            }
        } catch (SQLException e) {
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_DOWN, System.currentTimeMillis());
            } else {
                throw e;
            }
        } finally {
            if(stmt!=null)
                stmt.execute("use master"); // XXX why?
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
        return res;
    }

    private ResultSet getResultSet(Statement stmt, String col) throws SQLException
    {
        do
        {
            ResultSet rs = null;
            try
            {
                rs = stmt.getResultSet();
                if (rs == null)
                    break;
                rs.findColumn(col);
                return rs;
            }
            catch (SQLException e) {
                //don't close the resultset!!!
            }
        }
        while (stmt.getMoreResults() == true && stmt.getUpdateCount() != -1);
        throw new SQLException();
    }

    private void printMetaCols(ResultSetMetaData md) throws SQLException
    {
        for (int i=1; i<=md.getColumnCount(); i++)
        {
            System.out.println(md.getColumnName(i));
        }
    }

    private float getSegmentSize(long pages, int pagesize)
    {
        return pages/1024*pagesize/1024;
    }

    private MetricValue getSP_MonitorConfigValue(Metric metric,
                                                 String alias,
                                                 Connection conn)
        throws SQLException, MetricNotFoundException, MetricUnreachableException
    {
        String configOpt = metric.getObjectProperty(PROP_CONFIG_OPTION);
        float value = -1;
        if (alias.equalsIgnoreCase(MAX_USED))
            value = getMaxUsed(conn, configOpt);
        else if (alias.equalsIgnoreCase(NUM_REUSED))
            value = getNumReuse(conn, configOpt);
        else if (alias.equalsIgnoreCase(NUM_FREE))
            value = getNumFree(conn, configOpt);
        else if (alias.equalsIgnoreCase(NUM_ACTIVE))
            value = getNumActive(conn, configOpt);
        else if (alias.equalsIgnoreCase(PERCENT_ACTIVE))
            value = getPercentActive(conn, configOpt);
        else if (metric.isAvail())
            value = (float) getAvail(conn, configOpt);
        else
            throw new MetricNotFoundException(alias);

        return new MetricValue(value, System.currentTimeMillis());
    }

    private float getNumActive(Connection conn, String configOpt)throws MetricUnreachableException, MetricNotFoundException
    {
        return getMonitorConfigValue(conn,configOpt,"Num_active");
    }

    private double getAvail(Connection conn, String configOpt) {
        double res = Metric.AVAIL_DOWN;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("sp_monitorconfig '" + configOpt + "'");
            if (rs.next()) {
                res = Metric.AVAIL_UP;
            }
        } catch (SQLException e) {
            log.debug("[getAvail] configOpt='" + configOpt + "' -> " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
        return res;
    }

    private float getNumFree(Connection conn, String configOpt)
        throws MetricUnreachableException, MetricNotFoundException
    {
        return getMonitorConfigValue(conn,configOpt,"Num_free");
    }

    private float getNumReuse(Connection conn, String configOpt)
        throws SQLException
    {
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("sp_monitorconfig '"+configOpt+"'");
            ResultSetMetaData rsmd = rs.getMetaData();
            // I have seen Num_Reuse, Reuse and Reuse_cnt so far for this
            String reuseCol = "Num_Reuse";
            for (int i=1; i<=rsmd.getColumnCount(); i++) {
                String name = rsmd.getColumnName(i);
                if (name.indexOf("Reuse") != -1) {
                    reuseCol = name;
                }
            }
            int col = rs.findColumn(reuseCol);
            if (rs.next()) {
                return rs.getFloat(col);
            }
        }
        finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
        throw new SQLException();
    }

    private float getMaxUsed(Connection conn, String configOpt)
        throws MetricUnreachableException, MetricNotFoundException
    {
        return getMonitorConfigValue(conn,configOpt,"Max_Used");
    }

    private float getPercentActive(Connection conn, String configOpt)
        throws MetricUnreachableException, MetricNotFoundException
    {
        return getMonitorConfigValue(conn,configOpt,"Pct_act");
    }

    private float getMonitorConfigValue(Connection conn, String configOpt, String prop) throws MetricUnreachableException, MetricNotFoundException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("sp_monitorconfig '" + configOpt + "'");
            if (rs.next()) {
                return rs.getFloat(prop);
            }else{
                throw new MetricNotFoundException(prop);
            }
        } catch (SQLException e) {
            throw new MetricUnreachableException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
    }
}
