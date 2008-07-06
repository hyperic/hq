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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EnumerationConfigOption;

public class SQLQueryMeasurementPlugin
    extends JDBCMeasurementPlugin {

    private static final String DOMAIN = "sql";
    private static final String PROP_DRIVER = "jdbcDriver";
    private static final String EXEC_TIME_ATTR = "QueryExecTime";
    private String config;
    private boolean isProxy = false;

    private static Map loadedDrivers = new HashMap();
    private static Map availableDrivers;
    
    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);

        if (getName().equals(DOMAIN)) {
            this.isProxy = true;
            this.config =
                ":" + getPluginProperty(PROP_TEMPLATE_CONFIG);
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

    //<config> defined in plugin.xml..
    //overriding jdbcDriver w/ *.class from plugin.properties
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        ConfigSchema schema = super.getConfigSchema(info, config);
        ConfigOption defopt =
            schema.getOption(SQLQueryMeasurementPlugin.PROP_DRIVER);

        EnumerationConfigOption option =
            new EnumerationConfigOption(defopt.getName(),
                                        defopt.getDescription(),
                                        defopt.getDefault(),
                                        getDriverNames());
        schema.addOption(option);

        return schema;
    }

    private Map getAvailableDrivers() {
        if (availableDrivers != null) {
            return availableDrivers;
        }
        Map drivers = new HashMap();
        for (Iterator it = getProperties().entrySet().iterator();
             it.hasNext();)
        {
            Map driver;
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            String val = (String)entry.getValue();
            int ix = key.indexOf('.');
            if (ix == -1) {
                continue;
            }
            String name = key.substring(0, ix);
            String prop = key.substring(ix+1);
            driver = (Map)drivers.get(name);
            if (driver == null) {
                driver = new HashMap();
                drivers.put(name, driver);
            }
            driver.put(prop, val);
        }
        availableDrivers = new HashMap(drivers.size());
        for (Iterator it = drivers.values().iterator();
             it.hasNext();)
        {
            Map driver = (Map)it.next();
            String name = (String)driver.get(PROP_DRIVER);
            availableDrivers.put(name, driver);
        }
        return availableDrivers;
    }

    private String[] getDriverNames() {
        Map drivers = getAvailableDrivers();

        String[] names =
            (String[])drivers.keySet().toArray(new String[0]);
        Arrays.sort(names);
        return names;
    }

    private String encodeHTML(String val) {
        val = StringUtil.replace(val, "<", "&lt;");
        val = StringUtil.replace(val, ">", "&gt;");
        return val;
    }

    private void driverItem(StringBuffer help,
                            String key, String val) {
        help.append("<p><b>").append(key).append("</b><br>");
        help.append(val).append("</p>\n");
    }

    private String href(String url) {
        return "<a href=\"" + url + "\">" + url + "</a>";
    }

    private String included(Map driver) {
        if ("true".equals(driver.get("included"))) {
            return
                "<img src=\"/images/icon_available_green.gif\"> " +
                "This driver is included with HQ";
        }
        else {
            return
                "<img src=\"/images/icon_available_yellow.gif\"> " +
                "This driver is not included with HQ.  Copy required files to the " +
                "agent pdk/lib/jdbc/ directory";
        }
    }

    public String getHelp(TypeInfo info, Map props) {
        StringBuffer help = new StringBuffer();
        Map drivers = getAvailableDrivers();
        String[] names = getDriverNames();
        for (int i=0; i<names.length; i++) {
            String name = names[i];
            Map driver = (Map)drivers.get(name);
            String driverName = (String)driver.get(PROP_DRIVER);
            if (driverName == null) {
                continue;
            }
            String title = (String)driver.get("name");
            help.append("<hr><p>\n");
            help.append("<h3>").append(title).append("</h3>\n");
            help.append(included(driver));
            driverItem(help, "URL Format",
                       encodeHTML((String)driver.get(PROP_URL)));
            driverItem(help, "Required File(s)",
                       (String)driver.get("files"));
            driverItem(help, "Driver Class",
                       driverName);
            driverItem(help, "More information and download",
                       href((String)driver.get("download")));
            help.append("</p>\n");
        }
        return help.toString();
    }

    protected void getDriver()
        throws ClassNotFoundException {}

    private static synchronized boolean loadDriver(String driver) {
        if (loadedDrivers.get(driver) != null) {
            return true;
        }
        try {
            loadedDrivers.put(driver,
                              Class.forName(driver));
            return true;
        } catch (ClassNotFoundException e) {
            // Ignore, will fail in getConnection()
            return false;
        }
    }

    private static boolean loadDriver(Properties props) {
        return loadDriver(props.getProperty(PROP_DRIVER));
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
        Properties props = metric.getProperties();
        if (!loadDriver(props)) {
            String name = props.getProperty(PROP_DRIVER);
            Map driver =
                (Map)getAvailableDrivers().get(name);
            if (driver != null) {
                String msg =
                    "Failed to load " + PROP_DRIVER + "=" + name +
                    " (missing " + driver.get("files") + "?)";
                throw new PluginException(msg);
            }
        }
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
