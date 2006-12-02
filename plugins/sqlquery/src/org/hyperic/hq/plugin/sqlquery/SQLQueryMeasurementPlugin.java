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

package org.hyperic.hq.plugin.sqlquery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Properties;

import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;

import org.hyperic.util.config.ConfigResponse;

public class SQLQueryMeasurementPlugin
    extends JDBCMeasurementPlugin {

    private static final String DOMAIN = "sql";
    private static final String PROP_DRIVER = "jdbcDriver";
    private static final String EXEC_TIME_ATTR = "QueryExecTime";
    private String config;
    private boolean isProxy = false;

    private static HashMap loadedDrivers = new HashMap();

    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);

        if (getName().equals(DOMAIN)) {
            this.isProxy = true;
            this.config =
                ":" + getProperty(PROP_TEMPLATE_CONFIG);
        }
        else if (!getManager().isRegistered(DOMAIN)) {
            getLog().info("Registered proxy for domain: " + DOMAIN);
            getManager().createPlugin(DOMAIN, this);
        }
    }

    public String translate(String template, ConfigResponse config) {
        if (this.isProxy) {
            if (!template.endsWith(this.config)) {
                template += this.config;
            }
        }
        return super.translate(template, config);
    }

    protected void getDriver()
        throws ClassNotFoundException {}

    private static synchronized void loadDriver(String driver) {
        if (loadedDrivers.get(driver) != null) {
            return;
        }
        try {
            loadedDrivers.put(driver,
                              Class.forName(driver));
        } catch (ClassNotFoundException e) {
            // Ignore, will fail in getConnection()
        }
    }

    static void loadDriver(Properties props) {
        loadDriver(props.getProperty(PROP_DRIVER));
    }

    static Connection getConnection(Properties props)
        throws SQLException {

        loadDriver(props);

        String
            url = props.getProperty(PROP_URL),
            user = props.getProperty(PROP_USER),
            pass = props.getProperty(PROP_PASSWORD);

        return DriverManager.getConnection(url, user, pass);
    }

    protected Connection getConnection(String url,
                                       String user,
                                       String password)
        throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    protected String getDefaultURL() {
        return "";
    }

    protected void initQueries() {}

    protected String getQuery(Metric metric)
    {
        //The ObjectProperties portion is pulled directly from the plugin 
        //without being parsed since the query may include a ','.
        String query = Metric.decode(metric.getObjectPropString());

        loadDriver(metric.getProperties());

        return query;
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricUnreachableException,
               MetricInvalidException,
               MetricNotFoundException

    {
        String attr = metric.getAttributeName();
        MetricValue val;

        long startTime = System.currentTimeMillis();
        val = super.getValue(metric);
        // Not exact, but close enough
        long totalTime = System.currentTimeMillis() - startTime;

        if (attr.equals(EXEC_TIME_ATTR)) {
            return new MetricValue(totalTime);
        } else {
            return val;
        }
    }
}
