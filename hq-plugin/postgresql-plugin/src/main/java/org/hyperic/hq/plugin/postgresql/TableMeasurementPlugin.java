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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Metric;

public class TableMeasurementPlugin extends ResourceMeasurement {

    private static Log log = LogFactory.getLog(TableMeasurementPlugin.class);

    @Override
    protected String getQuery(Metric metric) {
        String tableName = metric.getObjectProperties().getProperty(PostgreSQL.PROP_TABLE);
        String schemaName = metric.getObjectProperties().getProperty(PostgreSQL.PROP_SCHEMA);
        String attributeName = metric.getAttributeName();
        if (metric.isAvail()) {
            attributeName = "seq_scan";
        }
        String tableQuery = null;
        if (attributeName.equals("DataSpaceUsed")) {
            tableQuery = "SELECT SUM(relpages) * 8 FROM pg_class "
                    + "JOIN pg_catalog.pg_namespace n ON n.oid = pg_class.relnamespace "
                    + "WHERE pg_class.relname = '" + tableName + "' "
                    + "AND n.nspname ='" + schemaName + "'";
        } else if (attributeName.equals("IndexSpaceUsed")) {
            tableQuery = "SELECT SUM(relpages) * 8 FROM pg_class "
                    + "JOIN pg_catalog.pg_namespace n ON n.oid = pg_class.relnamespace "
                    + "WHERE n.nspname = '" + schemaName + "' "
                    + "AND relname IN (SELECT indexrelname FROM "
                    + "pg_stat_user_indexes WHERE relname='"
                    + tableName + "' AND schemaname='" + schemaName + "')";
        } else {
            // Else normal query from pg_stat_user_table
            tableQuery = "SELECT " + attributeName + " FROM pg_stat_user_tables "
                    + "WHERE relname='" + tableName + "' "
                    + "AND schemaname='" + schemaName + "'";
        }
        log.debug("[getQuery] tableQuery='" + tableQuery + "'");
        return tableQuery;
    }
}
