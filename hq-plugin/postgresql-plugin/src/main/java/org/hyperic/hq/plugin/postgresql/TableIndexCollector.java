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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.jdbc.DBUtil;

public class TableIndexCollector extends Collector {

    private static Log log = LogFactory.getLog(TableIndexCollector.class);
    private List<String> schemas = new ArrayList<String>();
    private List<String> tables = new ArrayList<String>();
    private List<String> indexes = new ArrayList<String>();
    private String whereTable = "";
    private String whereIndex = "";
    private boolean firstCollect = true;

    /**
     * in the first collect call we collet metrics for all tables and indexes (where* are enpty)
     * afert that we onlu collect metrics for configured tables and indexes (where* have values)
     */
    @Override
    public void collect() {
        Properties p = getProperties();
        String user = p.getProperty(PostgreSQL.PROP_USER);
        String pass = p.getProperty(PostgreSQL.PROP_PASS);
        String db = p.getProperty(PostgreSQL.PROP_DB);

        String queryTable = "SELECT * FROM pg_stat_user_tables " + whereTable;
        String queryIndex = "SELECT * FROM pg_stat_user_indexes " + whereIndex;

        Connection conn = null;

        try {
            String url = PostgreSQL.prepareUrl(p, db);
            log.debug("[collect] url:'" + url + "'");
            conn = DriverManager.getConnection(url, user, pass);

            // TABLES
            if (firstCollect || (tables.size() > 0)) {
                log.debug("[collect] queryTable='" + queryTable + "'");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(queryTable);
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    String tableName = rs.getString("relname");
                    String schemaName = rs.getString("schemaname");
                    setValue("table." + schemaName + "." + tableName + "." + Metric.ATTR_AVAIL, Metric.AVAIL_UP);
                    for (int c = 1; c <= md.getColumnCount(); c++) {
                        setValue("table." + schemaName + "." + tableName + "." + md.getColumnLabel(c), rs.getString(c));
                    }
                }
                DBUtil.closeJDBCObjects(log, null, stmt, rs);
            }

            // INDXES
            if (firstCollect || (indexes.size() > 0)) {
                log.debug("[collect] queryInex='" + queryIndex + "'");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(queryIndex);
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    String indexrelName = rs.getString("indexrelname");
                    String schemaName = rs.getString("schemaname");
                    setValue("index." + schemaName + "." + indexrelName + "." + Metric.ATTR_AVAIL, Metric.AVAIL_UP);
                    for (int c = 1; c <= md.getColumnCount(); c++) {
                        setValue("index." + schemaName + "." + indexrelName + "." + md.getColumnLabel(c), rs.getString(c));
                    }
                }
            }
        } catch (Exception e) {
            final String msg = "Error getting metrics: " + e.getMessage();
            setErrorMessage(msg, e);
            log.debug("[collect] " + msg, e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, null, null);
        }
        firstCollect = false;
    }

    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        String table = metric.getProperties().getProperty(PostgreSQL.PROP_TABLE);
        String schema = metric.getProperties().getProperty(PostgreSQL.PROP_SCHEMA);
        String index = metric.getProperties().getProperty(PostgreSQL.PROP_INDEX);

        boolean updateWhere = false;
        if ((table != null) && (!tables.contains(table))) {
            tables.add(table);
            updateWhere = true;
        }

        if (!schemas.contains(schema)) {
            schemas.add(schema);
            updateWhere = true;
        }

        if ((index != null) && (!indexes.contains(index))) {
            indexes.add(index);
            updateWhere = true;
        }

        if (updateWhere) {
            whereTable = "where relname in(" + PostgreSQL.listToString(tables, ",") + ") and schemaname in(" + PostgreSQL.listToString(schemas, ",") + ")";
            whereIndex = "where indexrelname in(" + PostgreSQL.listToString(indexes, ",") + ") and schemaname in(" + PostgreSQL.listToString(schemas, ",") + ")";
        }

        MetricValue res = super.getValue(metric, getResult());
        if ((res == null) && (metric.getAttributeName().endsWith(Metric.ATTR_AVAIL))) {
            res = new MetricValue(Metric.AVAIL_DOWN);
        }
        return res;
    }
}
