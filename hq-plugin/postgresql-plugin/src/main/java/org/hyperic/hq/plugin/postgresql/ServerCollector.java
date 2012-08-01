/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.postgresql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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

    private static Log log = LogFactory.getLog(ServerCollector.class);
    private String connectionQ = "select "
            + "count(datid) as connections,"
            + "(SELECT setting AS mc FROM pg_settings WHERE name = 'max_connections') as max_connections "
            + "from pg_stat_activity";
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
                setValue("blks_hit",blksHit);
                setValue("blks_hit_p",blksHitP);
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
            while (rs.next()) {
                double c = rs.getDouble("connections");
                double m = rs.getDouble("max_connections");
                setValue("connections", c);
                setValue("connections_usage", c / m);
            }
        } finally {
            DBUtil.closeJDBCObjects(log, null, stmt, rs);
        }
    }
}
