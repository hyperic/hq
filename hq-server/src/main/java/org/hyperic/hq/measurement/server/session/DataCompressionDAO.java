/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.measurement.server.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

/**
 * @author jhickey
 * 
 */
@Repository
public class DataCompressionDAO {
    private JdbcTemplate jdbcTemplate;
    private SessionFactory sessionFactory;
    private final Log log = LogFactory.getLog(DataCompressionDAO.class);
    private static final String MEAS_VIEW = MeasTabManagerUtil.MEAS_VIEW;
    private static final String TAB_DATA = MeasurementConstants.TAB_DATA;

    @Autowired
    public DataCompressionDAO(JdbcTemplate jdbcTemplate, SessionFactory sessionFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.sessionFactory = sessionFactory;
    }

    public void createMetricDataViews() {
        final String UNION_BODY = "SELECT * FROM HQ_METRIC_DATA_0D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_0D_1S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_1D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_1D_1S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_2D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_2D_1S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_3D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_3D_1S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_4D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_4D_1S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_5D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_5D_1S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_6D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_6D_1S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_7D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_7D_1S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_8D_0S UNION ALL "
                                  + "SELECT * FROM HQ_METRIC_DATA_8D_1S";

        final String HQ_METRIC_DATA_VIEW = "CREATE VIEW " + MEAS_VIEW + " AS " + UNION_BODY;

        final String EAM_METRIC_DATA_VIEW = "CREATE VIEW " + TAB_DATA + " AS " + UNION_BODY +
                                            " UNION ALL SELECT * FROM HQ_METRIC_DATA_COMPAT";

        try {
            HQDialect dialect = (HQDialect) ((SessionFactoryImplementor) sessionFactory)
                .getDialect();
            Statement stmt = jdbcTemplate.getDataSource().getConnection().createStatement();
            if (!dialect.viewExists(stmt, TAB_DATA)) {
                jdbcTemplate.execute(EAM_METRIC_DATA_VIEW);
            }
            if (!dialect.viewExists(stmt, MEAS_VIEW)) {
                jdbcTemplate.execute(HQ_METRIC_DATA_VIEW);
            }
        } catch (DataAccessException e) {
            log.debug("Error Creating Metric Data Views", e);
        } catch (SQLException e) {
            log.debug("Error Creating Metric Data Views", e);
        }
    }

    public void truncateMeasurementData(long truncateBefore) {
        // we can't get any accurate metric tablenames if truncateBefore
        // is less than the base point in time which is used for the
        // tablename calculations
        if (truncateBefore < MeasTabManagerUtil.getBaseTime()) {
            return;
        }
        long currtime = System.currentTimeMillis();
        String currTable = MeasTabManagerUtil.getMeasTabname(currtime);
        long currTruncTime = truncateBefore;
        // just in case truncateBefore is in the middle of a table
        currTruncTime = MeasTabManagerUtil.getPrevMeasTabTime(currTruncTime);
        String delTable = MeasTabManagerUtil.getMeasTabname(currTruncTime);
        if (delTable.equals(currTable)) {
            currTruncTime = MeasTabManagerUtil.getPrevMeasTabTime(currTruncTime);
            delTable = MeasTabManagerUtil.getMeasTabname(currTruncTime);
        }
        log.debug("Truncating tables, starting with -> " + delTable + " (currTable -> " +
                  currTable + ")\n");
        HQDialect dialect = (HQDialect) ((SessionFactoryImplementor) sessionFactory).getDialect();
        while (!currTable.equals(delTable) && truncateBefore > currTruncTime) {
            try {
                log.debug("Truncating table " + delTable);
                jdbcTemplate.execute("truncate table " + delTable);
                String sql = dialect.getOptimizeStmt(delTable, 0);
                jdbcTemplate.execute(sql);
            } catch (DataAccessException e) {
                log.error(e.getMessage(), e);
            } finally {
                currTruncTime = MeasTabManagerUtil.getPrevMeasTabTime(currTruncTime);
                delTable = MeasTabManagerUtil.getMeasTabname(currTruncTime);
            }
        }
    }

    public void purgeMeasurements(String tableName, final long startWindow, final long endWindow) {
        log.debug("Purging data between " + TimeUtil.toString(startWindow) + " and " +
                  TimeUtil.toString(endWindow) + " in " + tableName);
        final String sql = "DELETE FROM " + tableName + " WHERE timestamp BETWEEN ? AND ?";

        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement stmt = con.prepareStatement(sql);
                stmt.setLong(1, startWindow);
                stmt.setLong(2, endWindow);
                return stmt;
            }
        });
    }

    /**
     * Get the oldest timestamp in the database.
     */
    public long getMinTimestamp(String dataTable) {
        return jdbcTemplate.queryForLong("SELECT MIN(timestamp) FROM " + dataTable);
    }

    public void compactData(final String fromTable, final String toTable, final long begin,
                            final long end) {
        log.info("Compressing from: " + fromTable + " to " + toTable);
        try {
            jdbcTemplate.update(new PreparedStatementCreator() {

                public PreparedStatement createPreparedStatement(Connection con)
                    throws SQLException {
                    String minMax;
                    if (fromTable.endsWith(TAB_DATA)) {
                        minMax = "AVG(value), MIN(value), MAX(value) ";
                    } else {
                        minMax = "AVG(value), MIN(minvalue), MAX(maxvalue) ";
                    }

                    PreparedStatement insStmt = con
                        .prepareStatement("INSERT INTO " +
                                          toTable +
                                          " (measurement_id, timestamp, value, minvalue, maxvalue)" +
                                          " (SELECT measurement_id, ? AS timestamp, " + minMax +
                                          "FROM " + fromTable +
                                          " WHERE timestamp >= ? AND timestamp < ? " +
                                          "GROUP BY measurement_id)");
                    insStmt.setLong(1, begin);
                    insStmt.setLong(2, begin);
                    insStmt.setLong(3, end);
                    return insStmt;
                }
            });
        } catch (DataAccessException e) {
            // Just log the error and continue
            log.debug("SQL exception when inserting data " + " at " + TimeUtil.toString(begin), e);
        }
    }

    /**
     * Get the most recent measurement.
     */
    public long getMaxTimestamp(String dataTable) {
        Connection connection;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
        } catch (SQLException e) {
            throw jdbcTemplate.getExceptionTranslator().translate(
                "Obtaining connection from DataSource", null, e);
        }
        String sql;
        try {
            if (DBUtil.isPostgreSQL(connection)) {
                // Postgres handles this much better
                sql = "SELECT timestamp FROM " + dataTable + " ORDER BY timestamp DESC LIMIT 1";
            } else {
                sql = "SELECT MAX(timestamp) FROM " + dataTable;
            }
        } catch (SQLException e) {
            throw jdbcTemplate.getExceptionTranslator().translate(
                "Determining if the database is PostGres", null, e);
        }
        return jdbcTemplate.query(sql, new ResultSetExtractor<Long>() {
            public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    // New installation
                    return 0l;
                }
            }
        });
    }

    public String getMeasurementUnionStatement(long begin) {
        return MeasurementUnionStatementBuilder.getUnionStatement(
            (begin - MeasurementConstants.HOUR), begin,
            (HQDialect) ((SessionFactoryImplementor) sessionFactory).getDialect());
    }
}
