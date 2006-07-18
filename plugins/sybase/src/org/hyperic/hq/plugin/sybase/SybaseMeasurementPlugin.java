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

package org.hyperic.hq.plugin.sybase;

import java.util.HashMap;

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

public class SybaseMeasurementPlugin 
    extends JDBCMeasurementPlugin {

    private static final String JDBC_DRIVER = 
        "com.sybase.jdbc2.jdbc.SybDriver";

    private static final String DEFAULT_URL = 
        "jdbc:sybase:Tds:localhost:4100/master";

    private static final String PROP_INSTANCE = "instance";

    private static HashMap syb12Queries    = null;  // Sybase 12.5 only
    private static HashMap genericQueries  = null;  // Any
    private static HashMap connectionCache = new HashMap();

    public SybaseMeasurementPlugin() {
        setName(SybaseProductPlugin.NAME);
    }

    protected void getDriver()
        throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
    }

    protected Connection getConnection(String url,
                                       String user,
                                       String password)
        throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    protected String getDefaultURL() {
        return DEFAULT_URL;
    }

    protected void initQueries() {
        if (genericQueries != null) {
            return;
        }

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
        genericQueries.put(AVAIL_ATTR,
                           genericQueries.get("NumServers"));
    }

    /**
     * Override the JDBCMeasurementPlugin getConfigSchema so that we only
     * generate config schema questions for the server types.  The service
     * types will use server config
     */
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        if (info.getType() == TypeInfo.TYPE_SERVICE) {
            SchemaBuilder builder = new SchemaBuilder(config);
            // User instances require an additional user argument
            builder.add(PROP_INSTANCE, "Database instance to monitor", "");
            return builder.getSchema();
        }

        return super.getConfigSchema(info, config);
    }

    protected String getQuery(Metric metric) {
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
}
