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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Properties;

import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

public class MySQLMeasurementPlugin
    extends JDBCMeasurementPlugin {

    // Driver defaults
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    // Measurement Queries
    private static final String STATUSQUERY = "SHOW /*!50002 GLOBAL */ STATUS LIKE ";
    private static final String TABLEQUERY  = "SHOW TABLE STATUS LIKE %table%";
    private static final String INDEXQUERY  = "SHOW INDEX FROM %table%";
    private static final String DBQUERY     = "SHOW TABLE STATUS";
    private static final String NUMDATABASES = "SHOW DATABASES";
    private static HashMap columnMap = null;

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
        // defined in hq-plugin.xml
        return getProperty("DEFAULT_URL");
    }

    protected void initQueries()
    {
        if (columnMap != null)
            return;

        columnMap = new HashMap();
        // Table metrics
        columnMap.put("availability", new Integer(4));
        columnMap.put("Rows", new Integer(4));
        columnMap.put("Avg_row_length", new Integer(5));
        columnMap.put("Data_length", new Integer(6));
        columnMap.put("Max_data_length", new Integer(7));
        columnMap.put("Index_length", new Integer(8));
        columnMap.put("Data_free", new Integer(9));
    }

    /**
     * Mysql results are in different columns depending on what metric
     * is being collected.  Server metrics are always in the second column,
     * table metrics come from a lookup map.
     */
    protected int getColumn(Metric metric)
    {
        String queryVal = metric.getAttributeName();
        Integer index = (Integer)columnMap.get(queryVal);

        if (index == null)
            return 2;
        
        return COL_INVALID;     // Force parent to call getColumnName()
    }
    
    protected String getColumnName(Metric metric) {
        return metric.getAttributeName();
    }

    private boolean isCumulativeMetric(String attr) {
        return
            attr.equals("Data_length") || 
            attr.equals("Rows") ||
            attr.equals("Index_length");
    }
    
    private boolean isIndexMetric(String attr) {
        return 
            attr.equals("NumIndicies") ||
            attr.equals("AvgCardinality");
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricUnreachableException,
               MetricInvalidException,
               MetricNotFoundException
    {
        String objectName = metric.getObjectName(),
               alias      = metric.getAttributeName();
        if (alias.indexOf("NumberOfDatabases") == -1 &&
            !alias.equalsIgnoreCase(AVAIL_ATTR))
            return super.getValue(metric);

        int value = 0;
        Statement stmt = null;
        ResultSet rs   = null;
        try
        {
            Connection conn = getCachedConnection(metric);
            stmt = conn.createStatement();
            rs   = stmt.executeQuery(NUMDATABASES);

            if (alias.equalsIgnoreCase(AVAIL_ATTR))
                return new MetricValue(Metric.AVAIL_UP,
                                       System.currentTimeMillis());

            while (rs.next())
                value++;

            return new MetricValue(value, System.currentTimeMillis());
        }
        catch (SQLException e)
        {
            String msg = "Query failed for "+alias+": "+e.getMessage();
            throw new MetricUnreachableException(msg, e);
        }
        finally {
            DBUtil.closeJDBCObjects(getLog(), null, stmt, rs);
        }
    }

    protected String getQuery(Metric metric)
    {
        Properties objectProps = metric.getObjectProperties();
        Properties props = metric.getProperties();
        String queryVal = metric.getAttributeName();

        if (isIndexMetric(queryVal)) {
            String table = objectProps.getProperty(PROP_TABLE);
            if (table == null) {
                // Backward compat
                table = props.getProperty(PROP_TABLE);
            }
            return StringUtil.replace(INDEXQUERY, "%table%", table);
        }
        
        if (objectProps.getProperty("Type").
            equals(MySQLServerDetector.TABLE)) {
            String table = objectProps.getProperty(PROP_TABLE);
            if (table == null) {
                // Backwards compat
                table = props.getProperty(PROP_TABLE);
            }
            return StringUtil.replace(TABLEQUERY, "%table%", "'" + table + "'");
        }

        // Cumulative metrics
        if (isCumulativeMetric(queryVal)) {
            return DBQUERY;
        }

        return STATUSQUERY + "'" + queryVal + "'";
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        // Override JDBCMeasurementPlugin.
        return new ConfigSchema();
    }
    
    /**
     * Since there is no way to retrieve index metric information on a 
     * per-index basis we need to duplicate getQueryValue from the JDBC
     * measurement plugin so that we can iterate the results looking for
     * the index in question.
     */
    protected double getQueryValue(Metric jdsn)
        throws MetricNotFoundException, PluginException,
               MetricUnreachableException
    {
        initQueries();
        String query = getQuery(jdsn);
        String attr = jdsn.getAttributeName();

        boolean isIndex = isIndexMetric(attr);
        boolean isCumulative = isCumulativeMetric(attr);
        // Cumulative and index metrics need to be computed by hand by
        // iterating over the results given by the 'SHOW xxx STATUS command.
        if (!(isIndex || isCumulative)) {
            return super.getQueryValue(jdsn);
        }

        if (query == null) {
            //plugin bug or hq-plugin.xml typo bug
            String msg = "No SQL query mapped to: " + attr;
            throw new PluginException(msg);
        }

        Properties props = jdsn.getProperties();
        String
            url = props.getProperty(PROP_URL),
            user = props.getProperty(PROP_USER),
            pass = props.getProperty(PROP_PASSWORD);
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        double numIndicies = 0;
        double totCardinality = 0;
        double dataLength = 0;
        double indexLength = 0;
        double totalRows = 0;

        try {
            conn = getCachedConnection(url, user, pass);
            //XXX cache prepared statements
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();

            while (rs != null && rs.next()) {
                if (isIndex) {
                    double cardinality = rs.getDouble(7);
                    numIndicies++;
                    totCardinality += cardinality;
                } else if (isCumulative) {
                    dataLength += rs.getDouble(attr);
                    indexLength += rs.getDouble(attr);
                    totalRows += rs.getDouble(attr);
                }
            }

            if (attr.equals("NumIndicies")) {
                return numIndicies;
            } else if (attr.equals("AvgCardinality")) {
                return totCardinality/numIndicies;
            } else if (attr.equals("Data_length")) {
                return dataLength;
            } else if (attr.equals("Index_length")) {
                return indexLength;
            } else if (attr.equals("Rows")) {
                return totalRows;
            } else {
                //Shouldn't happen
                throw new MetricNotFoundException(attr);
            }
        } catch (SQLException e) {
            // Remove this connection from the cache.
            removeCachedConnection(url, user, pass);
            
            String msg = "Query failed for " + attr +
                ": " + e.getMessage();

            throw new MetricNotFoundException(msg, e);
        } finally {
            DBUtil.closeJDBCObjects(getLog(), null, ps, rs);
        }
    }
}
