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

package org.hyperic.hq.plugin.bugzilla;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

public class BugzillaMeasurementPlugin extends JDBCMeasurementPlugin {

    // Driver defaults
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DEFAULT_URL = "jdbc:mysql://localhost/bugs";

    // Config properties
    protected static final String PROP_PROGRAM = "program";
    protected static final String PROP_VERSION = "version";
    protected static final String PROP_USERID  = "userid";

    static final String GROUP = "Group";
    static final String OWNER = "Owner";

    // Queries
    private static final String STATUSQUERY = "SHOW STATUS LIKE ";

    private static final String AVAILGROUPQUERY  =
        "SELECT * FROM versions WHERE program='%program%' AND value='%version%'";

    private static final String GROUPQUERY  =
        "SELECT count(*) FROM bugs " +
        "WHERE product='%program%' AND version='%version%' AND %cond%";

    private static final String AVAILOWNERQUERY  =
        "SELECT DISTINCT(assigned_to) FROM bugs WHERE assigned_to='%userid%'";

    private static final String OWNERQUERY  =
        "SELECT count(*) FROM bugs WHERE assigned_to='%userid%' AND %cond%";

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

    protected void initQueries() {}

    protected int getColumn(Metric metric) {
        Properties objectProps = metric.getObjectProperties();
        String type = objectProps.getProperty("Type");
        if (type.equals(GROUP) || type.equals(OWNER))
            return 1;
        
        return 2;
    }

    protected String getQuery(Metric metric)
    {
        Properties objectProps = metric.getObjectProperties();
        Properties props = metric.getProperties();
        String queryVal = metric.getAttributeName();
        
        String type = objectProps.getProperty("Type");
        if (type.equals(GROUP)) {
            String prog = objectProps.getProperty(PROP_PROGRAM);
            if (prog == null) {
                // Backwards compat
                prog = props.getProperty(PROP_PROGRAM);
            }

            String ver  = objectProps.getProperty(PROP_VERSION);
            if (ver == null) {
                // Backwards compat
                ver = props.getProperty(PROP_VERSION);
            }

            String query =
                queryVal.equals(AVAIL_ATTR) ? AVAILGROUPQUERY : GROUPQUERY;
            
            query = StringUtil.replace(query, "%program%", prog);
            query = StringUtil.replace(query, "%version%", ver);
            query = StringUtil.replace(query, "%cond%", queryVal);
            return query;
        } else if (type.equals(OWNER)) {
            String userid = objectProps.getProperty(PROP_USERID);
            if (userid == null) {
                // Backwards compat
                userid = props.getProperty(PROP_USERID);
            }

            String query =
                queryVal.equals(AVAIL_ATTR) ? AVAILOWNERQUERY : OWNERQUERY;

            query = StringUtil.replace(query, "%userid%", userid);
            query = StringUtil.replace(query, "%cond%", queryVal);
            return query;
        }

        if (queryVal.equals(AVAIL_ATTR)) {
            return STATUSQUERY + "'" + "Uptime" + "'";
        }
        
        return STATUSQUERY + "'" + queryVal + "'";
    }

    /**
     * Override of JDBCMeasurementPlugin's getConfigSchema to support
     * MySQL Table services.
     */
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        if (info.getType() == TypeInfo.TYPE_SERVICE) {
            SchemaBuilder builder = new SchemaBuilder(config);
            builder.add(PROP_PROGRAM, "Table to monitor", getDefaultURL());
            return builder.getSchema();
        }
        
        return super.getConfigSchema(info, config);
    }
}
