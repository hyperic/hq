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
	public String translate(String template, ConfigResponse config) {
		
		// "Type=Table,table=%table%"  ==> "Type=Table,schema=%schema%,table=%table%"
		final String tableProperty = "table=%table%";		
		final String tableSchemaProperties = "schema=%schema%,table=%table%";

		template = StringUtil.replace(template, tableProperty, tableSchemaProperties);
		// parse the template-config
		template = super.translate(template, config);

		return template;

	}    

    @Override
	protected String getQuery(Metric metric)
    {
        String attributeName = metric.getAttributeName();
        String objectName = metric.getObjectName();
        boolean isAvailable = attributeName.equals(AVAIL_ATTR);

        String query = null;
        if (objectName.indexOf("Type=Server") != -1) {
            query = getServerQuery(metric, attributeName, isAvailable);
        } else if (objectName.indexOf("Type=Table") != -1) {
            query = getTableQuery(metric, attributeName, isAvailable);
        } else if (objectName.indexOf("Type=Index") != -1) {
            query = getIndexQuery(metric, attributeName, isAvailable);
        }

        // If query is still null, then it's most likely 
        // a hq-plugin.xml typo.  JDBCMeasurementPlugin
        // will pick this up.
        return query;
    }    

	private String getIndexQuery(Metric metric, String attributeName, boolean isAvailable) {
		String indexName = metric.getObjectProperties().getProperty(PROP_INDEX);
		String schemaName = metric.getObjectProperties().getProperty(PROP_SCHEMA);
		if (isAvailable) {
		    // Hardcode for availability.  Cannot have the same
		    // template else we get plugin dumper errors.
		    attributeName = "idx_scan";
		}      
		String indexQuery = null;
		if (schemaName == null) {
		    indexQuery = getIndexQueryNoSchema(attributeName, indexName);
		} else {
			// Else normal query from pg_stat_user_table
			indexQuery = "SELECT " + attributeName + " FROM pg_stat_user_indexes " +
			"WHERE indexrelname='" + indexName + "' " +
			"AND schemaname='" + schemaName + "'";
		}
		return indexQuery;
	}

	private String getTableQuery(Metric metric, String attributeName, boolean isAvailable) {
		String tableName = metric.getObjectProperties().getProperty(PROP_TABLE);            
		if (tableName == null) {
		    // Backwards compat
		    tableName = metric.getProperties().getProperty(PROP_TABLE);
		}
		if (isAvailable) {
		    // Hardcode for availability.  Cannot have the same
		    // template else we get plugin dumper errors.
		    attributeName = "seq_scan";
		}
		String schemaName = metric.getObjectProperties().getProperty(PROP_SCHEMA);
		String tableQuery = null;
		if (schemaName == null) {
		    tableQuery = getTableQueryNoSchema(attributeName, tableName);
		} else if (attributeName.equals("DataSpaceUsed")) {
		    tableQuery = "SELECT SUM(relpages) * 8 FROM pg_class " +
		    		"JOIN pg_catalog.pg_namespace n ON n.oid = pg_class.relnamespace " +
		    		"WHERE pg_class.relname = '" + tableName + "' " +
		    		"AND n.nspname ='" + schemaName + "'";
		} else if (attributeName.equals("IndexSpaceUsed")) {
		    tableQuery = "SELECT SUM(relpages) * 8 FROM pg_class " +
		    		"JOIN pg_catalog.pg_namespace n ON n.oid = pg_class.relnamespace " +
		    		"WHERE n.nspname = '" + schemaName + "' " + 
		    		"AND relname IN (SELECT indexrelname FROM " +
		    		"pg_stat_user_indexes WHERE relname='" +
		    		tableName + "' AND schemaname='" + schemaName + "')";
		} else {		
			// Else normal query from pg_stat_user_table
			tableQuery = "SELECT " + attributeName + " FROM pg_stat_user_tables " +
			"WHERE relname='" + tableName + "' " +
			"AND schemaname='" + schemaName + "'";
		}
		return tableQuery;
	}

	private String getServerQuery(Metric metric, String attributeName, boolean isAvailable) {
		if (isAvailable) {
		    // Hardcode for availability.  Cannot have the same
		    // template else we get plugin dumper errors.
		    attributeName = "numbackends";
		}

		String url = metric.getProperties().getProperty(PROP_URL);
		String db = url.substring(url.lastIndexOf("/") + 1,
		                          url.length());

		String serverQuery = null;
		// Check metrics that require joins across tables.
		if (attributeName.equals("LocksHeld")) {
		    serverQuery = "SELECT COUNT(*) FROM PG_STAT_DATABASE, PG_LOCKS " +
		        "WHERE PG_LOCKS.DATABASE = PG_STAT_DATABASE.DATID AND " +
		        "PG_STAT_DATABASE.DATNAME = '" + db + "'";
		} else if (attributeName.equals("DataSpaceUsed")) {
		    // XXX assumes 8k page size. (which is the default)
		    serverQuery = "SELECT SUM(relpages) * 8 FROM pg_class WHERE " +
		        "relname IN (SELECT relname from pg_stat_user_tables)";
		} else if (attributeName.equals("IndexSpaceUsed")) {
		    serverQuery = "SELECT SUM(relpages) * 8 FROM pg_class WHERE " +
		        "relname IN (SELECT indexrelname from " +
		        "pg_stat_user_indexes)";
		} else {
			// Else normal query from pg_stat_database
			serverQuery = "SELECT " + attributeName + " FROM pg_stat_database " +
			"WHERE datname='" + db + "'";
		}
		return serverQuery;
	}
    

	private String getTableQueryNoSchema(String attributeName, String tableName) {
		String tableQuery = null;
		if (attributeName.equals("DataSpaceUsed")) {
			tableQuery = "SELECT SUM(relpages) * 8 FROM pg_class WHERE " +
					"relname = '" + tableName + "'";
		} else if (attributeName.equals("IndexSpaceUsed")) {
			tableQuery = "SELECT SUM(relpages) * 8 FROM pg_class WHERE " +
					"relname IN (SELECT indexrelname FROM " +
					"pg_stat_user_indexes WHERE relname='" +
					tableName + "')";
		} else {        
			// Else normal query from pg_stat_user_table
			tableQuery = "SELECT " + attributeName + " FROM pg_stat_user_tables " +
					"WHERE relname='" + tableName + "'";
		}
		return tableQuery;
	}	

	private String getIndexQueryNoSchema(String attributeName, String indexName) {
		return "SELECT " + attributeName + " FROM pg_stat_user_indexes " +
				"WHERE indexrelname='" + indexName + "'";

	}	
	
}
