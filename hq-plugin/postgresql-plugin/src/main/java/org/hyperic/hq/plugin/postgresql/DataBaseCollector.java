/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2014], Hyperic, Inc.
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.postgresql.ServerCollector.PGVersion;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.util.jdbc.DBUtil;

/**
 *
 * @author laullon
 */
public class DataBaseCollector extends Collector {

    private static final Log log = LogFactory.getLog(DataBaseCollector.class);
    private final List<String> db_cache = new ArrayList<String>();
    private final String query[] = {
        "SELECT * FROM pg_stat_database where datname in (" + PostgreSQLServerDetector.DB_QUERY + ")",
        "SELECT COUNT(datid) AS connections, (SELECT setting AS mc FROM pg_settings WHERE name = 'max_connections') AS max_connections, d.datname FROM pg_database d LEFT JOIN pg_stat_activity s ON (s.datid = d.oid) GROUP BY 2,3"
    };
    private final String queryDB[] = {
        "SELECT (select current_database()) as datname, COALESCE(SUM(idx_scan),0) AS idx_scan, COALESCE(SUM(idx_tup_read),0) AS idx_tup_read, COALESCE(SUM(idx_tup_fetch),0) AS idx_tup_fetch from pg_stat_user_indexes",
        "SELECT (select current_database()) as datname, COALESCE(SUM(idx_blks_read),0) AS idx_blks_read, COALESCE(SUM(idx_blks_hit),0) AS idx_blks_hit from pg_statio_user_indexes",
        "SELECT (select current_database()) as datname, COALESCE(SUM(seq_scan),0) AS seq_scan, COALESCE(SUM(seq_tup_read),0) AS seq_tup_read FROM pg_stat_user_tables",
        "SELECT count(mode) granted_locks, datname FROM pg_locks l JOIN pg_database d ON (d.oid=l.database) WHERE l.granted=true GROUP BY 2",
        "SELECT count(mode) awaited_locks, datname FROM pg_locks l JOIN pg_database d ON (d.oid=l.database) WHERE l.granted=false GROUP BY 2"};
    private final String queryDB_PRE_92[] = {
        "SELECT datname, count(current_query) AS idle_backends FROM pg_stat_activity WHERE current_query != '<IDLE>' GROUP BY 1;",
        "SELECT datname, count(current_query) AS idle_backends FROM pg_stat_activity WHERE current_query = '<IDLE>' GROUP BY 1;"
    };
    private final String queryDB_POST_92[] = {
        "SELECT datname, 0 AS idle_backends FROM pg_stat_activity WHERE state != 'idle' GROUP BY 1;",
        "SELECT datname, count(state) AS idle_backends FROM pg_stat_activity WHERE state = 'idle' GROUP BY 1;"
    };
    private final String posibleNULLMetrics[] = {"granted_locks", "awaited_locks"};
    private PGVersion version = PGVersion.UNKNOW;

    @Override
    public void collect() {
        Properties p = getProperties();
        log.debug("[collect] p:" + p);
        db_cache.clear();
        String user = p.getProperty(PostgreSQL.PROP_USER);
        String pass = p.getProperty(PostgreSQL.PROP_PASS);

        Connection conn = null;

        try {
            String url = PostgreSQL.prepareUrl(p, null);
            log.debug("[collect] url:'" + url + "'");
            conn = DriverManager.getConnection(url, user, pass);
            for (int j = 0; j < query.length; j++) {
                String q = query[j];
                extartMetrics(q, conn, j == 0);
            }

            if (version == PGVersion.UNKNOW) {
                version = ServerCollector.checkVersion(conn);
            }
        } catch (Exception e) {
            final String msg = "Error getting metrics: " + e.getMessage();
            setErrorMessage(msg, e);
            log.debug("[collect] " + msg, e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, null, null);
        }

        log.debug("[collect] db_cache: " + db_cache);

        for (String db : db_cache) {
            try {
                String url = PostgreSQL.prepareUrl(p, db);
                log.debug("[collect] url:'" + url + "'");
                conn = DriverManager.getConnection(url, user, pass);
                for (String q : queryDB) {
                    extartMetrics(q, conn, false);
                }

                String[] extraQueryDB = (version == PGVersion.POST_92) ? queryDB_POST_92 : queryDB_PRE_92;
                for (String q : extraQueryDB) {
                    extartMetrics(q, conn, false);
                }
            } catch (Exception e) {
                final String msg = "Error getting metrics: " + e.getMessage();
                setErrorMessage(msg, e);
                log.debug("[collect] " + msg, e);
            } finally {
                DBUtil.closeJDBCObjects(log, conn, null, null);
            }
        }

        for (int i = 0; i < db_cache.size(); i++) {
            String db = db_cache.get(i);
            setValue(db + "." + Metric.ATTR_AVAIL, Metric.AVAIL_UP);

            double c = Double.parseDouble((String) getResult().getValues().get(db + ".connections"));
            double m = Double.parseDouble((String) getResult().getValues().get(db + ".max_connections"));
            setValue(db + ".connections_usage", c / m);

            double blksRead = Double.parseDouble((String) getResult().getValues().get(db + ".blks_read"));
            double blksHit = Double.parseDouble((String) getResult().getValues().get(db + ".blks_hit"));
            double blksHitP = (blksRead + blksHit) > 0 ? (blksHit / (blksRead + blksHit)) : 0;
            double tupAltered = Double.parseDouble((String) getResult().getValues().get(db + ".tup_inserted"))
                    + Double.parseDouble((String) getResult().getValues().get(db + ".tup_updated"))
                    + Double.parseDouble((String) getResult().getValues().get(db + ".tup_deleted"));
            setValue(db + ".blks_hit_p", blksHitP);
            setValue(db + ".tup_altered", tupAltered);
            for (String metric : posibleNULLMetrics) {
                if (getResult().getValues().get(db + "." + metric) == null) {
                    setValue(db + "." + metric, 0);
                }
            }
        }
    }

    private void extartMetrics(String query, Connection conn, boolean getDBNames) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            log.debug("[extartMetrics] query:'" + query + "'");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                String dbName = rs.getString("datname");
                if (getDBNames) {
                    db_cache.add(dbName);
                }
                for (int c = 1; c <= md.getColumnCount(); c++) {
                    setValue(dbName + "." + md.getColumnLabel(c), rs.getString(c));
                }
            }
        } finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
    }
}
