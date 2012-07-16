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

public class DataBaseMeasurementPlugin extends ResourceMeasurement {

    private static Log log = LogFactory.getLog(DataBaseMeasurementPlugin.class);

    @Override
    protected String getQuery(Metric metric) {

        String attributeName = metric.getAttributeName();
        String db = metric.getObjectProperty(PostgreSQL.PROP_DB);

        String serverQuery = null;
        // Check metrics that require joins across tables.
        if (attributeName.equals("DatabaseSize")) {
            serverQuery = "SELECT pg_database_size(d.oid) "
                    + "FROM pg_database d where datname='" + db + "'";
        } else if (attributeName.equals("DataSpaceUsed")) {
            // XXX assumes 8k page size. (which is the default)
            serverQuery = "SELECT SUM(relpages) * 8 FROM pg_class WHERE "
                    + "relname IN (SELECT relname from pg_stat_user_tables)";
        } else if (attributeName.equals("IndexSpaceUsed")) {
            serverQuery = "SELECT SUM(relpages) * 8 FROM pg_class WHERE "
                    + "relname IN (SELECT indexrelname from "
                    + "pg_stat_user_indexes)";
        }

        log.debug("[getQuery] serverQuery='" + serverQuery + "'");

        return serverQuery;
    }
}
