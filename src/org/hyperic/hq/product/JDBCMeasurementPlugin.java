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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

import org.hyperic.util.jdbc.DBUtil;

/**
 * Base class for JDBC measurement plugins.
 * Abstracts the JDBC connection and query functionality.
 */
public abstract class JDBCMeasurementPlugin extends MeasurementPlugin {

    protected static final String AVAIL_ATTR = "availability";

    public static final String PROP_URL      = "jdbcUrl";
    public static final String PROP_USER     = "jdbcUser";
    public static final String PROP_PASSWORD = "jdbcPassword";

    public static final String PROP_TABLE    = "table";
    public static final String PROP_INDEX    = "index";

    private static HashMap connectionCache = new HashMap();
    
    public static final int COL_INVALID = 0;

    /**
     * Config schema includes jdbc URL, database username and password.
     * These values will be used to obtain a connection from 
     * DriverManager.getConnection.
     */
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        ConfigSchema schema = super.getConfigSchema(info, config);
        if (schema.getOptions().size() > 0) {
            return schema; //from hq-plugin.xml
        }

        SchemaBuilder builder = new SchemaBuilder(config);
        builder.add(PROP_URL, "JDBC URL", getDefaultURL());
        builder.add(PROP_USER, "Database username", "username");
        builder.addSecret(PROP_PASSWORD, "Database password").setOptional(true);

        return builder.getSchema();
    }
    
    /**
     * Verifies that JDBC driver returned by the getDriver() method
     * can be loaded by the plugin.
     */
    public void init(PluginManager manager)
        throws PluginException
    {
        super.init(manager);

        try {
            getDriver();
        } catch (ClassNotFoundException e) {
            //driver is not loaded server-side
            //if the above fails client-side queries will fail with
            //"No suitable driver" so its okay to swallow this exception
            //throw new PluginException(e.getMessage(), e);
        }
    }

    /**
     * Close any cached connections.
     */
    public void shutdown()
        throws PluginException
    {
        super.shutdown();

        try {
            Iterator it = connectionCache.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                Connection conn = (Connection)entry.getValue();
                if (conn != null) {
                    conn.close();
                }
            }
        } catch (SQLException e) {
            throw new PluginException(e.getMessage(), e);
        }       

        connectionCache.clear();
    }

    /**
     * Dispatches to getQueryValue()
     */
    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricUnreachableException,
               MetricInvalidException,
               MetricNotFoundException
    {
        double value = getQueryValue(metric);

        MetricValue mValue =
            new MetricValue(value, System.currentTimeMillis());

        return mValue;
    }

    protected abstract void initQueries();

    protected abstract String getQuery(Metric jdsn);

    /**
     * The plugin must preform the Class.forName so its
     * ClassLoader is used to find the driver.
     */
    protected abstract void getDriver()
        throws ClassNotFoundException;

    /**
     * The plugin must preform the DriverManager.getConnection so its
     * ClassLoader is used to find the driver.
     */
    protected abstract Connection getConnection(String url,
                                                String user,
                                                String password)
        throws SQLException;

    protected abstract String getDefaultURL();

    /**
     * The column in the ResultSet that holds the measurement value.
     * For most plugins this will be 1, by some databases do not allow
     * a ResultSet with a single column to be returned (see MySQL)
     */
    protected int getColumn(Metric jdsn) {
        return 1;
    }

    protected String getColumnName(Metric jdsn) {
        return "";
    }

    protected Connection getCachedConnection(Metric metric)
        throws SQLException
    {
        Properties props = metric.getProperties();
        String url  = props.getProperty(PROP_URL),
               user = props.getProperty(PROP_USER),
               pass = props.getProperty(PROP_PASSWORD);
        return getCachedConnection(url, user, pass);
    }
    
    protected Connection getCachedConnection(String url, String user,
                                             String pass)
        throws SQLException
    {
        String cacheKey = url + user + pass;
        Connection conn = (Connection)connectionCache.get(cacheKey);

        if (conn == null) {
            conn = getConnection(url, user, pass);
            
            synchronized (connectionCache) {
                connectionCache.put(cacheKey, conn);
            }
        }
        return conn;
    }

    protected void removeCachedConnection(String url, String user,
                                          String pass)
    {
        synchronized(connectionCache) {
            connectionCache.remove(url + user + pass);
        }
    }

    /**
     * Do the database query returned by the getQuery() method
     * and return the result.  A cached connection will be used
     * if one exists, otherwise the created connection will be
     * cached for future use.
     */
    protected double getQueryValue(Metric jdsn)
        throws MetricNotFoundException, PluginException,
               MetricUnreachableException
    {
        initQueries();
        String query = getQuery(jdsn);
        String attr = jdsn.getAttributeName();

        if (query == null) {
            //plugin bug or hq-plugin.xml typo bug
            String msg = "No SQL query mapped to: " + attr;
            throw new PluginException(msg);
        }

        //ignore case to allow the stanard case "Availability"
        boolean isAvail = attr.equalsIgnoreCase(AVAIL_ATTR);
        Properties props = jdsn.getProperties();
        String
            url = props.getProperty(PROP_URL),
            user = props.getProperty(PROP_USER),
            pass = props.getProperty(PROP_PASSWORD);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getCachedConnection(url, user, pass);
            //XXX cache prepared statements
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();

            // If the query executed without error, we don't care if any 
            // results were returned.
            if (isAvail) {
                return Metric.AVAIL_UP;
            }

            if (rs != null && rs.next()) {
                int column = getColumn(jdsn);
                if (column != COL_INVALID)
                    return rs.getDouble(column);
                
                return rs.getDouble(getColumnName(jdsn));
            } else {
                throw new MetricNotFoundException(attr);
            }
        } catch (SQLException e) {

            if (isAvail) {
                getLog().debug("AVAIL_DOWN", e);
                return Metric.AVAIL_DOWN;
            }

            // Remove this connection from the cache.
            removeCachedConnection(url, user, pass);

            String msg = "Query failed for " + attr +
                ", while attempting to issue query " + query +
                ":" + e.getMessage();

            //XXX these two are oracle specific.
            // Catch divide by 0 errors and return 0
            if(e.getErrorCode() == DBUtil.ORACLE_ERROR_DIVIDE_BY_ZERO ||
               e.getErrorCode() == DBUtil.POSTGRES_ERROR_DIVIDE_BY_ZERO)
                return 0;
            if(e.getErrorCode() == DBUtil.ORACLE_ERROR_NOT_AVAILABLE    ||
               e.getErrorCode() == DBUtil.POSTGRES_CONNECTION_EXCEPTION ||
               e.getErrorCode() == DBUtil.POSTGRES_CONNECTION_FAILURE   ||
               e.getErrorCode() == DBUtil.POSTGRES_UNABLE_TO_CONNECT    ||
               e.getErrorCode() == DBUtil.MYSQL_LOCAL_CONN_ERROR        ||
               e.getErrorCode() == DBUtil.MYSQL_REMOTE_CONN_ERROR)
                throw new MetricUnreachableException(msg, e);
                
            throw new MetricNotFoundException(msg, e);
        } finally {
            DBUtil.closeJDBCObjects(getLog(), null, ps, rs);
        }
    }
}
