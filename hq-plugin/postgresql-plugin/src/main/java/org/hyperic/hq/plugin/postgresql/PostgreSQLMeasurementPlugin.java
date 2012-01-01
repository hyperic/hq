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
import java.sql.DriverManager;
import java.sql.SQLException;

import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class PostgreSQLMeasurementPlugin
    extends JDBCMeasurementPlugin {

	public static final String PROP_SCHEMA    = "schema";
	
    protected static final String JDBC_DRIVER = "org.postgresql.Driver";

    //XXX: Could default this to whatever HQ chooses as a db name
    protected static final String DEFAULT_URL = 
        "jdbc:postgresql://localhost/hq";

    @Override
	protected void getDriver()
        throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
    }

    @Override
	protected Connection getConnection(String url,
                                       String user,
                                       String password)
        throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
	protected String getDefaultURL() {
        // Not used since we override getConfigSchema().
        return DEFAULT_URL;
    }

    @Override
	protected void initQueries() {
    }

    @Override
	public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        // Override JDBCMeasurementPlugin.
        return new ConfigSchema();
    }

    @Override
	protected String getQuery(Metric metric)
    {
        String attr = metric.getAttributeName();
        String objectName = metric.getObjectName();
        boolean isAvail = attr.equals(AVAIL_ATTR);

        if (objectName.indexOf("Type=Server") != -1) {
            if (isAvail) {
                // Hardcode for availability.  Cannot have the same
                // template else we get plugin dumper errors.
                attr = "numbackends";
            }

            String url = metric.getProperties().getProperty(PROP_URL);
            String db = url.substring(url.lastIndexOf("/") + 1,
                                      url.length());

            // Check metrics that require joins across tables.
            if (attr.equals("LocksHeld")) {
                return "SELECT COUNT(*) FROM PG_STAT_DATABASE, PG_LOCKS " +
                    "WHERE PG_LOCKS.DATABASE = PG_STAT_DATABASE.DATID AND " +
                    "PG_STAT_DATABASE.DATNAME = '" + db + "'";
            } else if (attr.equals("DataSpaceUsed")) {
                // XXX assumes 8k page size. (which is the default)
                return "SELECT SUM(relpages) * 8 FROM pg_class WHERE " +
                    "relname IN (SELECT relname from pg_stat_user_tables)";
            } else if (attr.equals("IndexSpaceUsed")) {
                return "SELECT SUM(relpages) * 8 FROM pg_class WHERE " +
                    "relname IN (SELECT indexrelname from " +
                    "pg_stat_user_indexes)";
            }
            // Else normal query from pg_stat_database
            return "SELECT " + attr + " FROM pg_stat_database " +
                "WHERE datname='" + db + "'";
        } else if (objectName.indexOf("Type=Table") != -1) {

            String table = metric.getObjectProperties().getProperty(PROP_TABLE);            
            if (table == null) {
                // Backwards compat
                table = metric.getProperties().getProperty(PROP_TABLE);
            }
            if (isAvail) {
                // Hardcode for availability.  Cannot have the same
                // template else we get plugin dumper errors.
                attr = "seq_scan";
            }
            String schema = metric.getObjectProperties().getProperty(PROP_SCHEMA);
            if (schema == null) {
                return createTableQueryNoSchema(attr, table);
            }                      
            
            if (attr.equals("DataSpaceUsed")) {
                return "SELECT SUM(relpages) * 8 FROM pg_class " +
                		"JOIN pg_catalog.pg_namespace n ON n.oid = pg_class.relnamespace " +
                		"WHERE pg_class.relname = '" + table + "' " +
                		"AND n.nspname ='" + schema + "'";
            } else if (attr.equals("IndexSpaceUsed")) {
                return "SELECT SUM(relpages) * 8 FROM pg_class " +
                		"JOIN pg_catalog.pg_namespace n ON n.oid = pg_class.relnamespace " +
                		"WHERE n.nspname = '" + schema + "' " + 
                		"AND relname IN (SELECT indexrelname FROM " +
                		"pg_stat_user_indexes WHERE relname='" +
                		table + "' AND schemaname='" + schema + "')";
            }
            
            // Else normal query from pg_stat_user_table
            return "SELECT " + attr + " FROM pg_stat_user_tables " +
                "WHERE relname='" + table + "' " +
                "AND schemaname='" + schema + "'";
        } else if (objectName.indexOf("Type=Index") != -1) {

            String index = metric.getObjectProperties().getProperty(PROP_INDEX);
            String schema = metric.getObjectProperties().getProperty(PROP_SCHEMA);
            if (isAvail) {
                // Hardcode for availability.  Cannot have the same
                // template else we get plugin dumper errors.
                attr = "idx_scan";
            }            
            if (schema == null) {
                return createIndexQueryNoSchema(attr, index);
            }             

            // Else normal query from pg_stat_user_table
            return "SELECT " + attr + " FROM pg_stat_user_indexes " +
                "WHERE indexrelname='" + index + "' " +
                "AND schemaname='" + schema + "'";
        }

        // Most likely a hq-plugin.xml typo.  JDBCMeasurementPlugin
        // will pick this up.
        return null;
    }

	@Override
	public String translate(String template, ConfigResponse config) {
		
		// "Type=Table,table=%table%"  ==> "Type=Table,schema=%schema%,table=%table%"
		final String tableProperty = "table=%table%";		
		final String tableSchemaProperties = "schema=%schema%,table=%table%";
		
		template = StringUtil.replace(template, tableProperty, tableSchemaProperties);
        // parse the template-config
        template = super.translate(template, config);

        return template;

	}
    
	
	private String createTableQueryNoSchema(String attr, String table) {
        if (attr.equals("DataSpaceUsed")) {
            return "SELECT SUM(relpages) * 8 FROM pg_class WHERE " +
                    "relname = '" + table + "'";
        } else if (attr.equals("IndexSpaceUsed")) {
            return "SELECT SUM(relpages) * 8 FROM pg_class WHERE " +
                    "relname IN (SELECT indexrelname FROM " +
                    "pg_stat_user_indexes WHERE relname='" +
                    table + "')";
        }
        
        // Else normal query from pg_stat_user_table
        return "SELECT " + attr + " FROM pg_stat_user_tables " +
        "WHERE relname='" + table + "'";
	}	

	private String createIndexQueryNoSchema(String attr, String index) {
        return "SELECT " + attr + " FROM pg_stat_user_indexes " +
                "WHERE indexrelname='" + index + "'";

	}	
	
}
