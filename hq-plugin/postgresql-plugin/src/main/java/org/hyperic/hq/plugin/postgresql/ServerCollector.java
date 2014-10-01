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
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.util.jdbc.DBUtil;

/**
 *
 * @author laullon
 */
public class ServerCollector extends Collector {

    protected enum PGVersion {

        UNKNOW, PRE_92, POST_92
    }
    private PGVersion version = PGVersion.UNKNOW;

    private static Log log = LogFactory.getLog(ServerCollector.class);
    private String connectionQ = "SELECT COUNT(datid) AS connections, (SELECT setting AS mc FROM pg_settings WHERE name = 'max_connections') AS max_connections, d.datname FROM pg_database d LEFT JOIN pg_stat_activity s ON (s.datid = d.oid) GROUP BY 2,3";

    private String statsQ = "SELECT "
            + "SUM(xact_commit) as xact_commit, "
            + "SUM(xact_rollback) as xact_rollback, "
            + "SUM(blks_read) as blks_read, "
            + "SUM(blks_hit) as blks_hit, "
            + "SUM(tup_fetched) as tup_fetched, "
            + "SUM(tup_inserted) as tup_inserted, "
            + "SUM(tup_updated) as tup_updated, "
            + "SUM(tup_deleted) as tup_deleted "
            + "FROM pg_stat_database "
            + "where datname in (" + PostgreSQLServerDetector.DB_QUERY + ")";
    private final String[] queries_POST_92 = {"SELECT count(s.state) AS idle_backends FROM pg_database d LEFT JOIN pg_stat_activity s ON (s.datid = d.oid) where s.state = 'idle'"};
    private final String[] queries_PRE_92 = {"SELECT count(s.current_query) AS idle_backends FROM pg_database d LEFT JOIN pg_stat_activity s ON (s.datid = d.oid) where s.current_query = '<IDLE>'"};
//"select count(*) AS idle_backends from pg_stat_activity where current_query = '<IDLE>'"};

    @Override
    public void collect() {
        Properties p = getProperties();
        String user = p.getProperty(PostgreSQL.PROP_USER);
        String pass = p.getProperty(PostgreSQL.PROP_PASS);

        Connection conn = null;

        try {
            String url = PostgreSQL.prepareUrl(p, null);
            log.debug("[collect] url:'" + url + "'");
            conn = DriverManager.getConnection(url, user, pass);

            if (version == PGVersion.UNKNOW) {
                version = ServerCollector.checkVersion(conn);
            }

            String[] q = (version == PGVersion.POST_92) ? queries_POST_92 : queries_PRE_92;

            for (int j = 0; j < q.length; j++) {
                extartMetrics(q[j], conn);
            }

            getPGTOPStast(conn);
            getConnectionsMetrics(conn);
        } catch (Exception e) {
            final String msg = "Error getting metrics: " + e.getMessage();
            setErrorMessage(msg, e);
            log.debug("[collect] " + msg, e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, null, null);
        }
    }

    private void getPGTOPStast(Connection conn) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            log.debug("[getConnectionsMetrics] query:'" + connectionQ + "'");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(statsQ);
            while (rs.next()) {
                double blksRead = rs.getDouble("blks_read");
                double blksHit = rs.getDouble("blks_hit");
                double blksHitP = (blksRead + blksHit) > 0 ? (blksHit / (blksRead + blksHit)) : 0;
                double tupAltered = rs.getDouble("tup_inserted") + rs.getDouble("tup_updated") + rs.getDouble("tup_deleted");
                setValue("xact_commit", rs.getDouble("xact_commit"));
                setValue("xact_rollback", rs.getDouble("xact_rollback"));
                setValue("blks_read", blksRead);
                setValue("blks_hit", blksHit);
                setValue("blks_hit_p", blksHitP);
                setValue("tup_fetched", rs.getDouble("tup_fetched"));
                setValue("tup_altered", tupAltered);
            }
        } finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
    }

    private void getConnectionsMetrics(Connection conn) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            log.debug("[getConnectionsMetrics] query:'" + connectionQ + "'");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(connectionQ);
            double c = 0;
            double m = 0;
            while (rs.next()) {
                c += rs.getDouble("connections");
                m = rs.getDouble("max_connections");
            }
            setValue("connections", c);
            setValue("connections_usage", c / m);
        } finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
    }

    private void extartMetrics(String query, Connection conn) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            log.debug("[extartMetrics] query:'" + query + "'");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                for (int c = 1; c <= md.getColumnCount(); c++) {
                    setValue(md.getColumnLabel(c), rs.getString(c));
                }
            }
        } finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
    }

    /**
     * Check if the PG is 9.2+ or not by checking the exitance of
     * 'current_query' on 'pg_stat_activity'
     *
     * @param conn JDCB Connection to PG
     * @return PGVersion.PRE_92 or PGVersion.POST_92
     * @throws SQLException in case of a SQL problem not related to
     * 'current_query' column.
     */
    protected static PGVersion checkVersion(Connection conn) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        PGVersion version = PGVersion.PRE_92;

        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select current_query from pg_stat_activity");
        } catch (SQLException ex) {
            if (ex.getMessage().contains("current_query")) {
                log.debug("[checkVersion] " + ex.getMessage().trim());
                version = PGVersion.POST_92;
            } else {
                throw ex;
            }
        } finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }

        log.debug("[checkVersion] version: " + version);
        return version;
    }
}
