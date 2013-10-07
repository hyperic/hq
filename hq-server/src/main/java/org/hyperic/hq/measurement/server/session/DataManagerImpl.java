/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.MeasurementDataSourceException;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasRange;
import org.hyperic.hq.measurement.shared.MeasRangeObj;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TopNManager;
import org.hyperic.hq.plugin.system.TopReport;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The DataManagerImpl can be used to retrieve measurement data points
 * 
 */
@Service
@Transactional

public class DataManagerImpl implements DataManager {
    private static final String ERR_START = "Begin and end times must be positive";
    private static final String ERR_END = "Start time must be earlier than end time";
    private static final String LOG_CTX = DataManagerImpl.class.getName();
    private final Log log = LogFactory.getLog(LOG_CTX);

    // The boolean system property that makes all events interesting. This
    // property is provided as a testing hook so we can flood the event
    // bus on demand.
    public static final String ALL_EVENTS_INTERESTING_PROP = "org.hq.triggers.all.events.interesting";

    private static final BigDecimal MAX_DB_NUMBER = new BigDecimal("10000000000000000000000");

    private static final long MINUTE = 60 * 1000, HOUR = 60 * MINUTE;
    protected final static long HOUR_IN_MILLI = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.HOURS);
    protected final static long SIX_HOURS_IN_MILLI = TimeUnit.MILLISECONDS.convert(6L, TimeUnit.HOURS);
    protected final static long DAY_IN_MILLI = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS);

    // Table names
    private static final String TAB_DATA_1H = MeasurementConstants.TAB_DATA_1H;
    private static final String TAB_DATA_6H = MeasurementConstants.TAB_DATA_6H;
    private static final String TAB_DATA_1D = MeasurementConstants.TAB_DATA_1D;
    private static final String TAB_MEAS = MeasurementConstants.TAB_MEAS;
    private static final String DATA_MANAGER_INSERT_TIME = ConcurrentStatsCollector.DATA_MANAGER_INSERT_TIME;

    // Error strings
    private static final String ERR_INTERVAL = "Interval cannot be larger than the time range";
    
    private static final String PLSQL =
        "BEGIN " +
        "INSERT INTO :table (measurement_id, timestamp, value) " +
        "VALUES(?, ?, ?); " +
        "EXCEPTION WHEN DUP_VAL_ON_INDEX THEN " +
        "UPDATE :table SET VALUE = ? " +
        "WHERE timestamp = ? and measurement_id = ?; " + "END; ";

    // Save some typing
    private static final int IND_MIN = MeasurementConstants.IND_MIN;
    private static final int IND_AVG = MeasurementConstants.IND_AVG;
    private static final int IND_MAX = MeasurementConstants.IND_MAX;
    private static final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;

    private final DBUtil dbUtil;

    // Pager class name
    private boolean confDefaultsLoaded = false;

    // Purge intervals, loaded once on first invocation.
    private long purgeRaw, purge1h, purge6h, purge1d;

    private static final long HOURS_PER_MEAS_TAB = MeasTabManagerUtil.NUMBER_OF_TABLES_PER_DAY;
    private static final String DATA_MANAGER_RETRIES_TIME = ConcurrentStatsCollector.DATA_MANAGER_RETRIES_TIME;

    private final MeasurementDAO measurementDAO;
    private final MeasurementManager measurementManager;
    private final ServerConfigManager serverConfigManager;
    private final AvailabilityManager availabilityManager;
    private final MetricDataCache metricDataCache;
    private final ZeventEnqueuer zeventManager;
    private final MessagePublisher messagePublisher;
    private final RegisteredTriggers registeredTriggers;
    private final ConcurrentStatsCollector concurrentStatsCollector;
    private final int transactionTimeout;
    private final TopNManager topNManager;

    
    @Autowired
    public DataManagerImpl(DBUtil dbUtil, MeasurementDAO measurementDAO,
                           MeasurementManager measurementManager,
                           ServerConfigManager serverConfigManager,
                           AvailabilityManager availabilityManager,
                           MetricDataCache metricDataCache, ZeventEnqueuer zeventManager,
                           MessagePublisher messagePublisher, RegisteredTriggers registeredTriggers,
                           ConcurrentStatsCollector concurrentStatsCollector,
                           HibernateTransactionManager transactionManager,
                           TopNManager topNManager) {
        this.dbUtil = dbUtil;
        this.measurementDAO = measurementDAO;
        this.measurementManager = measurementManager;
        this.serverConfigManager = serverConfigManager;
        this.availabilityManager = availabilityManager;
        this.metricDataCache = metricDataCache;
        this.zeventManager = zeventManager;
        this.messagePublisher = messagePublisher;
        this.registeredTriggers = registeredTriggers;
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.transactionTimeout = transactionManager.getDefaultTimeout();
        this.topNManager = topNManager;
    }

    @PostConstruct
    public void initStatsCollector() {
    	concurrentStatsCollector.register(DATA_MANAGER_INSERT_TIME);
    	concurrentStatsCollector.register(DATA_MANAGER_RETRIES_TIME);
    }

    private double getValue(ResultSet rs) throws SQLException {
        double val = rs.getDouble("value");

        if (rs.wasNull()) {
            val = Double.NaN;
        }

        return val;
    }

    private void checkTimeArguments(long begin, long end) throws IllegalArgumentException {
        if (begin > end) {
            throw new IllegalArgumentException(ERR_END);
        }

        if (begin < 0) {
            throw new IllegalArgumentException(ERR_START);
        }
    }

    private HighLowMetricValue getMetricValue(ResultSet rs) throws SQLException {
        long timestamp = rs.getLong("timestamp");
        double value = this.getValue(rs);

        if (!Double.isNaN(value)) {
            try {
                double high = rs.getDouble("peak");
                double low = rs.getDouble("low");
                return new HighLowMetricValue(value, high, low, timestamp);
            } catch (SQLException e) {
                // Peak and low columns do not exist
            }
        }
        return new HighLowMetricValue(value, timestamp);
    }

    // Returns the next index to be used
    private int setStatementArguments(PreparedStatement stmt, int start, Integer[] ids)
        throws SQLException {
        // Set ID's
        int i = start;
        for (Integer id : ids) {
            stmt.setInt(i++, id.intValue());
        }

        return i;
    }

    private void checkTimeArguments(long begin, long end, long interval)
        throws IllegalArgumentException {

        checkTimeArguments(begin, end);

        if (interval > (end - begin)) {
            throw new IllegalArgumentException(ERR_INTERVAL);
        }
    }

    /**
     * Save the new MetricValue to the database
     * 
     * @param mv the new MetricValue
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     * 
     * 
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addData(Integer mid, MetricValue mv, boolean overwrite) {

        Measurement meas = measurementManager.getMeasurement(mid);
        List<DataPoint> pts = Collections.singletonList(new DataPoint(meas.getId(), mv));

        addData(pts, overwrite);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addData(List<DataPoint> data, String aggTable, Connection conn) throws Exception {
        try {
            insertDataWithOneInsert(data, aggTable, conn);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            DBUtil.closeConnection(LOG_CTX, conn);
        }
    }
         
    private void insertDataWithOneInsert(List<DataPoint> dpts, String table, Connection conn) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            StringBuilder values = new StringBuilder();
            for (DataPoint pt : dpts) {
                Integer metricId = pt.getMeasurementId();
                HighLowMetricValue metricVal = (HighLowMetricValue) pt.getMetricValue();
                BigDecimal val = new BigDecimal(metricVal.getValue());
                BigDecimal highVal = new BigDecimal(metricVal.getHighValue());
                BigDecimal lowVal = new BigDecimal(metricVal.getLowValue());
                values.append("(").append(metricId.intValue()).append(", ").append(
                        metricVal.getTimestamp()).append(", ").append(
                                getDecimalInRange(val, metricId)).append(", ").append(
                                        getDecimalInRange(lowVal, metricId)).append(", ").append(
                                                getDecimalInRange(highVal, metricId)).append("),");
            }
            String sql = "insert into " + table + " (measurement_id, timestamp, value, minvalue, maxvalue)" + 
                    " values " + values.substring(0, values.length() - 1);
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            // If there is a SQLException, then none of the data points
            // should be inserted. Roll back the txn.
            log.error("an unexpected SQL exception has occured inserting datapoints to the " + table + " table:\n",e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, null, stmt, rs);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean addData(List<DataPoint> data) {
        return this._addData(data,safeGetConnection());
    }

    public boolean addData(List<DataPoint> data, Connection conn) {
        return this._addData(data,safeGetConnection());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean addTopData(List<TopNData> topNData) {
        return this._addTopData(topNData, safeGetConnection());
    }


    
    /**
     * Write metric data points to the DB with transaction
     * 
     * @param data a list of {@link DataPoint}s
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     * 
     * 
     */
    protected boolean _addData(List<DataPoint> data, Connection conn) {
        if (shouldAbortDataInsertion(data)) {
            return true;
        }

        data = enforceUnmodifiable(data);

        log.debug("Attempting to insert data in a single transaction.");

        HQDialect dialect = measurementDAO.getHQDialect();
        boolean succeeded = false;
        final boolean debug = log.isDebugEnabled();

        if (conn == null) {
            return false;
        }

        try {
            boolean autocommit = conn.getAutoCommit();

            try {
                final long start = System.currentTimeMillis();
                conn.setAutoCommit(false);
                if (dialect.supportsMultiInsertStmt()) {
                    succeeded = insertDataWithOneInsert(data, conn);
                } else {
                    succeeded = insertDataInBatch(data, conn);
                }

                if (succeeded) {
                    conn.commit();
                    final long end = System.currentTimeMillis();
                    if (debug) {
                        log.debug("Inserting data in a single transaction " + "succeeded");
                        log.debug("Data Insertion process took " + (end - start) + " ms");
                    }
                    
                    concurrentStatsCollector.addStat(end - start, DATA_MANAGER_INSERT_TIME);
                    sendMetricEvents(data);
                } else {
                    if (debug) {
                        log.debug("Inserting data in a single transaction failed."
                                  + "  Rolling back transaction.");
                    }
                    conn.rollback();
                    conn.setAutoCommit(true);
                    List<DataPoint> processed = addDataWithCommits(data, true, conn);
                    final long end = System.currentTimeMillis();
                    
                    concurrentStatsCollector.addStat(end - start, DATA_MANAGER_INSERT_TIME);
                    sendMetricEvents(processed);
                    if (debug) {
                        log.debug("Data Insertion process took " + (end - start) + " ms");
                    }
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autocommit);
            }
        } catch (SQLException e) {
            log.debug("Transaction failed around inserting metric data.", e);
        } finally {
            DBUtil.closeConnection(LOG_CTX, conn);
        }
        return succeeded;
    }


    protected boolean _addTopData(List<TopNData> topNData, Connection conn) {

        if (log.isDebugEnabled()) {
            log.debug("Attempting to insert topN data in a single transaction.");
        }
        boolean succeeded = false;
        final boolean debug = log.isDebugEnabled();

        if (conn == null) {
            return false;
        }

        try {
            boolean autocommit = conn.getAutoCommit();

            try {
                final long start = System.currentTimeMillis();
                conn.setAutoCommit(false);

                succeeded = insertTopData(conn, topNData, false);

                if (succeeded) {
                    conn.commit();
                    final long end = System.currentTimeMillis();
                    if (debug) {
                        log.debug("Inserting TopN data in a single transaction " + "succeeded");
                        log.debug("TopN Data Insertion process took " + (end - start) + " ms");
                    }

                    concurrentStatsCollector.addStat(end - start, DATA_MANAGER_INSERT_TIME);
                     } else {
                    if (debug) {
                        log.debug("Inserting TopN data in a single transaction failed." + "  Rolling back transaction" +
                                ".");
                    }
                    conn.rollback();
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autocommit);
            }
        } catch (SQLException e) {
            log.debug("Transaction failed around inserting TopN data.", e);
        } finally {
            DBUtil.closeConnection(LOG_CTX, conn);
        }
        return succeeded;
    }


    /**
     * Write metric datapoints to the DB without transaction
     * 
     * @param data a list of {@link DataPoint}s
     * @param overwrite If true, attempt to over-write values when an insert of
     *        the data fails (i.e. it already exists). You may not want to
     *        over-write values when, for instance, the back filler is inserting
     *        data.
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     * 
     * 
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addData(List<DataPoint> data, boolean overwrite) {
        /**
         * We have to account for 2 types of metric data insertion here: 1 - New
         * data, using 'insert' 2 - Old data, using 'update'
         * 
         * We optimize the amount of DB roundtrips here by executing in batch,
         * however there are some serious gotchas:
         * 
         * 1 - If the 'insert' batch update fails, a BatchUpdateException can be
         * thrown instead of just returning an error within the executeBatch()
         * array. 2 - This is further complicated by the fact that some drivers
         * will throw the exception at the first instance of an error, and some
         * will continue with the rest of the batch.
         */
        if (shouldAbortDataInsertion(data)) {
            return;
        }

        log.debug("Attempting to insert/update data outside a transaction.");

        data = enforceUnmodifiable(data);

        Connection conn = safeGetConnection();

        if (conn == null) {
            log.debug("Inserting/Updating data outside a transaction failed.");
            return;
        }

        try {
            boolean autocommit = conn.getAutoCommit();

            try {
                conn.setAutoCommit(true);
                addDataWithCommits(data, overwrite, conn);
            } finally {
                conn.setAutoCommit(autocommit);
            }
        } catch (SQLException e) {
            log.debug("Inserting/Updating data outside a transaction failed "
                      + "because autocommit management failed.", e);
        } finally {
            DBUtil.closeConnection(LOG_CTX, conn);
        }
    }

    private List<DataPoint> addDataWithCommits(List<DataPoint> data, boolean overwrite, Connection conn) {
        final StopWatch watch = new StopWatch();
        try {
            return _addDataWithCommits(data, overwrite, conn);
        } finally {
            concurrentStatsCollector.addStat(watch.getElapsed(), DATA_MANAGER_RETRIES_TIME);
        }
    }

    private List<DataPoint> _addDataWithCommits(List<DataPoint> data, boolean overwrite, Connection conn) {
        Set<DataPoint> failedToSaveMetrics = new HashSet<DataPoint>();
        List<DataPoint> left = data;
        while (!left.isEmpty()) {
            int numLeft = left.size();
            if (log.isDebugEnabled()) {
                log.debug("Attempting to insert " + numLeft + " points");
            }

            try {
                left = insertData(conn, left, true);
            } catch (SQLException e) {
                assert false : "The SQLException should not happen: " + e;
            }

            if (log.isDebugEnabled()) {
                log.debug("Num left = " + left.size());
            }

            if (left.isEmpty()) {
                break;
            }

            if (!overwrite) {
                if (log.isDebugEnabled()) {
                    log.debug("We are not updating the remaining " + left.size() + " points");
                }
                failedToSaveMetrics.addAll(left);
                break;
            }

            // The insert couldn't insert everything, so attempt to update
            // the things that are left
            if (log.isDebugEnabled()) {
                log.debug("Sending " + left.size() + " data points to update");
            }

            left = updateData(conn, left);

            if (left.isEmpty()) {
                break;
            }

            if (log.isDebugEnabled()) {
                log.debug("Update left " + left.size() + " points to process");
            }

            if (numLeft == left.size()) {
                DataPoint remPt = left.remove(0);
                failedToSaveMetrics.add(remPt);
                // There are some entries that we weren't able to do
                // anything about ... that sucks.
                log.warn("Unable to do anything about " + numLeft + " data points.  Sorry.");
                log.warn("Throwing away data point " + remPt);
            }
        }

        log.debug("Inserting/Updating data outside a transaction finished.");

        return removeMetricsFromList(data, failedToSaveMetrics);
    }

    private boolean shouldAbortDataInsertion(List<?> data) {
        if (data.isEmpty()) {
            log.debug("Aborting data insertion since data list is empty. This is ok.");
            return true;
        } else {
            return false;
        }
    }

    private <T> List<T> enforceUnmodifiable(List<T> aList) {
        return Collections.unmodifiableList(aList);
    }

    private List<DataPoint> removeMetricsFromList(List<DataPoint> data,
                                                  Set<DataPoint> metricsToRemove) {
        if (metricsToRemove.isEmpty()) {
            return data;
        }

        Set<DataPoint> allMetrics = new HashSet<DataPoint>(data);
        allMetrics.removeAll(metricsToRemove);
        return new ArrayList<DataPoint>(allMetrics);
    }

    /**
     * Convert a decimal value to something suitable for being thrown into the
     * database with a NUMERIC(24,5) definition
     */
    private BigDecimal getDecimalInRange(BigDecimal val, Integer metricId) {
        val = val.setScale(5, BigDecimal.ROUND_HALF_EVEN);
        if (val.compareTo(MAX_DB_NUMBER) == 1) {
            log.warn("Value [" + val + "] for metric id=" + metricId +
                     "is too big to put into the DB.  Truncating to [" + MAX_DB_NUMBER + "]");
            return MAX_DB_NUMBER;
        }

        return val;
    }

    private void sendMetricEvents(List<DataPoint> data) {
        if (data.isEmpty()) {
            return;
        }

        // Finally, for all the data which we put into the system, make sure
        // we update our internal cache, kick off the events, etc.
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        if (debug) {
            watch.markTimeBegin("analyzeMetricData");
        }
        analyzeMetricData(data);
        if (debug) {
            watch.markTimeEnd("analyzeMetricData");
        }

        Collection<DataPoint> cachedData = updateMetricDataCache(data);
        sendDataToEventHandlers(cachedData);
        if (debug) {
            log.debug(watch);
        }
    }

    private void analyzeMetricData(List<DataPoint> data) {
        Analyzer analyzer = getAnalyzer();
        if (analyzer != null) {
            for (DataPoint dp : data) {
                analyzer.analyzeMetricValue(dp.getMeasurementId(), dp.getMetricValue());
            }
        }
    }

    private Collection<DataPoint> updateMetricDataCache(List<DataPoint> data) {

        return metricDataCache.bulkAdd(data);
    }

    private void sendDataToEventHandlers(Collection<DataPoint> data) {
        ArrayList<MeasurementEvent> events = new ArrayList<MeasurementEvent>();
        List<MeasurementZevent> zevents = new ArrayList<MeasurementZevent>();

        boolean allEventsInteresting = Boolean.getBoolean(ALL_EVENTS_INTERESTING_PROP);

        for (DataPoint dp : data) {

            Integer metricId = dp.getMeasurementId();
            MetricValue val = dp.getMetricValue();
            MeasurementEvent event = new MeasurementEvent(metricId, val);

            if (registeredTriggers.isTriggerInterested(event) || allEventsInteresting) {
                measurementManager.buildMeasurementEvent(event);
                events.add(event);
            }

            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }

        if (!events.isEmpty()) {
            messagePublisher.publishMessage(EventConstants.EVENTS_TOPIC, events);
        }

        if (!zevents.isEmpty()) {
            try {
                // XXX: Shouldn't this be a transactional queueing?
                zeventManager.enqueueEvents(zevents);
            } catch (InterruptedException e) {
                log.warn("Interrupted while sending events.  Some data may " + "be lost");
            }
        }
    }

    private List<DataPoint> getRemainingDataPoints(List<DataPoint> data, int[] execInfo) {
        List<DataPoint> res = new ArrayList<DataPoint>();
        int idx = 0;

        // this is the case for mysql
        if (execInfo.length == 0) {
            return res;
        }

        for (Iterator<DataPoint> i = data.iterator(); i.hasNext(); idx++) {
            DataPoint pt = i.next();

            if (execInfo[idx] == Statement.EXECUTE_FAILED) {
                res.add(pt);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Need to deal with " + res.size() + " unhandled " + "data points (out of " +
                      execInfo.length + ")");
        }
        return res;
    }

    private List<DataPoint> getRemainingDataPointsAfterBatchFail(List<DataPoint> data, int[] counts) {
        List<DataPoint> res = new ArrayList<DataPoint>();
        Iterator<DataPoint> i = data.iterator();
        int idx;

        for (idx = 0; idx < counts.length; idx++) {
            DataPoint pt = i.next();

            if (counts[idx] == Statement.EXECUTE_FAILED) {
                res.add(pt);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Need to deal with " + res.size() + " unhandled " + "data points (out of " +
                      counts.length + ").  " + "datasize=" + data.size());
        }

        // It's also possible that counts[] is not as long as the list
        // of data points, so we have to return all the un-processed points
        if (data.size() != counts.length) {
            res.addAll(data.subList(idx, data.size()));
        }
        return res;
    }

    /**
     * Insert the metric data points to the DB with one insert statement. This
     * should only be invoked when the DB supports multi-insert statements.
     * 
     * @param data a list of {@link DataPoint}s
     * @return <code>true</code> if the multi-insert succeeded;
     *         <code>false</code> otherwise.
     */
    private boolean insertDataWithOneInsert(List<DataPoint> data, Connection conn) {
        Statement stmt = null;
        final Map<String, Set<DataPoint>> buckets = MeasRangeObj.getInstance().bucketDataEliminateDups(data);
        final boolean debug = log.isDebugEnabled();
        String sql = "";
        final HQDialect dialect = measurementDAO.getHQDialect();
        final boolean supportsAsyncCommit = dialect.supportsAsyncCommit();
        try {
            for (Entry<String, Set<DataPoint>> entry : buckets.entrySet()) {
                final String table = entry.getKey();
                final Set<DataPoint> dpts = entry.getValue();
                final int dptsSize = dpts.size();
                final StringBuilder values = new StringBuilder(dptsSize*15);
                int rows = 0;
                for (final DataPoint pt : dpts) {
                    final Integer metricId = pt.getMeasurementId();
                    final MetricValue val = pt.getMetricValue();
                    final BigDecimal bigDec = new BigDecimal(val.getValue());
                    rows++;
                    values.append("(").append(val.getTimestamp()).append(",")
                          .append(metricId.intValue()).append(",")
                          .append(getDecimalInRange(bigDec, metricId)).append(")");
                    if (rows < dptsSize) {
                        values.append(",");
                    }
                }
                sql = "insert into " + table + " (timestamp, measurement_id, value) values " + values;
                stmt = conn.createStatement();
                if (supportsAsyncCommit) {
                    stmt.execute(dialect.getSetAsyncCommitStmt(false));
                }
                final int rowsUpdated = stmt.executeUpdate(sql);
                if (supportsAsyncCommit) {
                    stmt.execute(dialect.getSetAsyncCommitStmt(true));
                }
                stmt.close();
                stmt = null;
                if (debug) {
                    log.debug("Inserted " + rowsUpdated + " rows into " + table + " (attempted " + rows + " rows)");
                }
                if (rowsUpdated < rows) {
                    return false;
                }
            }
        } catch (SQLException e) {
            // If there is a SQLException, then none of the data points
            // should be inserted. Roll back the txn.
            if (debug) {
                log.debug("Error inserting data with one insert stmt: " + e +
                          ".  Server will retry the insert in degraded mode.  sql=\n" + sql);
            }
            return false;
        } finally {
            DBUtil.closeStatement(LOG_CTX, stmt);
        }
        return true;
    }

    /**
     * Insert the metric data points to the DB in batch.
     * 
     * @param data a list of {@link DataPoint}s
     * @return <code>true</code> if the batch insert succeeded;
     *         <code>false</code> otherwise.
     */
    private boolean insertDataInBatch(List<DataPoint> data, Connection conn) {
        List<DataPoint> left = data;

        try {
            if (log.isDebugEnabled()) {
                log.debug("Attempting to insert " + left.size() + " points");
            }
            left = insertData(conn, left, false);
            if (log.isDebugEnabled()) {
                log.debug("Num left = " + left.size());
            }

            if (!left.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Need to update " + left.size() + " data points.");
                    log.debug("Data points to update: " + left);
                }

                return false;
            }
        } catch (SQLException e) {
            // If there is a SQLException, then none of the data points
            // should be inserted. Roll back the txn.
            if (log.isDebugEnabled()) {
                log.debug("Error while inserting data in batch (this is ok) " + e.getMessage());
            }
            return false;
        }

        return true;
    }

    /**
     * Retrieve a DB connection.
     * 
     * @return The connection or <code>null</code>.
     */
    private Connection safeGetConnection() {
        Connection conn = null;

        try {
            // XXX: may want to explore grabbing a connection directly from the transactionManager
            conn = dbUtil.getConnection();
        } catch (SQLException e) {
            log.error("Failed to retrieve connection from data source", e);
        }

        return conn;
    }

    /**
     * This method inserts data into the data table. If any data points in the
     * list fail to get added (e.g. because of a constraint violation), it will
     * be returned in the result list.
     * 
     * @param conn The connection.
     * @param data The data points to insert.
     * @param continueOnSQLException <code>true</code> to continue inserting the
     *        rest of the data points even after a <code>SQLException</code>
     *        occurs; <code>false</code> to throw the <code>SQLException</code>.
     * @return The list of data points that were not inserted.
     * @throws SQLException only if there is an exception for one of the data
     *         point batch inserts and <code>continueOnSQLException</code> is
     *         set to <code>false</code>.
     */
    private List<DataPoint> insertData(Connection conn, List<DataPoint> data,
                                       boolean continueOnSQLException)
    throws SQLException {
        PreparedStatement stmt = null;
        final List<DataPoint> left = new ArrayList<DataPoint>();
        final Map<String, List<DataPoint>> buckets = MeasRangeObj.getInstance().bucketData(data);
        final HQDialect dialect = measurementDAO.getHQDialect();
        final boolean supportsDupInsStmt = dialect.supportsDuplicateInsertStmt();
        final boolean supportsPLSQL = dialect.supportsPLSQL();
        final StringBuilder buf = new StringBuilder();
        for (final Entry<String, List<DataPoint>> entry : buckets.entrySet()) {
            buf.setLength(0);
            final String table = entry.getKey();
            final List<DataPoint> dpts = entry.getValue();
            try {
                if (supportsDupInsStmt) {
                    stmt = conn.prepareStatement(
                        buf.append("INSERT INTO ").append(table)
                           .append(" (measurement_id, timestamp, value) VALUES (?, ?, ?)")
                           .append(" ON DUPLICATE KEY UPDATE value = ?")
                           .toString());
                } else if (supportsPLSQL) {
                    final String sql = PLSQL.replaceAll(":table", table);
                    stmt = conn.prepareStatement(sql);
                } else {
                    stmt = conn.prepareStatement(
                        buf.append("INSERT INTO ")
                           .append(table)
                           .append(" (measurement_id, timestamp, value) VALUES (?, ?, ?)")
                           .toString());
                }
                // TODO need to set synchronous commit to off
                for (DataPoint pt : dpts) {
                    Integer metricId = pt.getMeasurementId();
                    MetricValue val = pt.getMetricValue();
                    BigDecimal bigDec;
                    bigDec = new BigDecimal(val.getValue());
                    stmt.setInt(1, metricId.intValue());
                    stmt.setLong(2, val.getTimestamp());
                    stmt.setBigDecimal(3, getDecimalInRange(bigDec, metricId));
                    if (supportsDupInsStmt) {
                        stmt.setBigDecimal(4, getDecimalInRange(bigDec, metricId));
                    } else if (supportsPLSQL) {
                        stmt.setBigDecimal(4, getDecimalInRange(bigDec, metricId));
                        stmt.setLong(5, val.getTimestamp());
                        stmt.setInt(6, metricId.intValue());
                    }
                    stmt.addBatch();
                }
                int[] execInfo = stmt.executeBatch();
                left.addAll(getRemainingDataPoints(dpts, execInfo));
            } catch (BatchUpdateException e) {
                if (!continueOnSQLException) {
                    throw e;
                }
                left.addAll(getRemainingDataPointsAfterBatchFail(dpts, e.getUpdateCounts()));
            } catch (SQLException e) {
                if (!continueOnSQLException) {
                    throw e;
                }
                // If the batch insert is not within a transaction, then we
                // don't know which of the inserts completed successfully.
                // Assume they all failed.
                left.addAll(dpts);
                if (log.isDebugEnabled()) {
                    log.debug("A general SQLException occurred during the insert. " +
                              "Assuming that none of the " + dpts.size() +
                              " data points were inserted.", e);
                }
            } finally {
                DBUtil.closeStatement(LOG_CTX, stmt);
            }
        }
        return left;
    }

    public int purgeTopNData(Date timeToKeep) {
        PreparedStatement ps = null;
        int topNDeleted = -1;
        Connection conn = safeGetConnection();
        try {
            ps = conn.prepareStatement("select drop_old_partitions(?, ?);");
            ps.setString(1, TopNData.class.getSimpleName());
            ps.setTimestamp(2, new Timestamp(timeToKeep.getTime()));
            ResultSet rs = ps.executeQuery();
            rs.next();
            topNDeleted = rs.getInt(1);
        } catch (SQLException e) {
            log.error("Problem purging old TopN Data", e);
        } finally {
            DBUtil.closeStatement(LOG_CTX, ps);
        }
        return topNDeleted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hyperic.hq.measurement.shared.DataManager#getTopNData(int, long)
     */
    public TopNData getTopNData(int resourceId, long time) {
        Statement stmt = null;
        Connection conn = safeGetConnection();
        StringBuilder builder = new StringBuilder();
        try {
            builder.append("SELECT data FROM TOPNDATA TOP WHERE TOP.resourceid='").append(resourceId)
                    .append("' AND TOP.time = '").append(new java.sql.Timestamp(time)).append("'");

            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(builder.toString());
            while (rs.next()) {
                TopNData data = new TopNData();
                data.setData(rs.getBytes("data"));
                data.setResourceId(resourceId);
                data.setTime(new Date(time));
                return data;
            }

        } catch (SQLException e) {
            log.error("Problem fetching TOP data", e);
        } finally {
            DBUtil.closeStatement(LOG_CTX, stmt);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.hyperic.hq.measurement.shared.DataManager#getTopNDataAsString(int,
     * long)
     */
    @Transactional(readOnly = true)
    public TopReport getTopReport(int resourceId, long time) {
        TopNData data = getTopNData(resourceId, time);
        if(data == null){
            return null;
        }
        try {
            byte[] unCompressedData = topNManager.uncompressData(data.getData());
            return TopReport.fromSerializedForm(unCompressedData);
        } catch (Exception e) {
            log.error("Error un serializing TopN data", e);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.hyperic.hq.measurement.shared.DataManager#getLatestAvailableTopDataTimes
     * (int, int)
     */
    @Transactional(readOnly = true)
    public List<Long> getLatestAvailableTopDataTimes(int resourceId, int count) {
        List<Long> times = new ArrayList<Long>();
        Statement stmt = null;
        Connection conn = safeGetConnection();
        StringBuilder builder = new StringBuilder();
        try {
            builder.append("SELECT TIME FROM TOPNDATA TOP WHERE TOP.resourceid='").append(resourceId)
                    .append("' ORDER BY time DESC LIMIT ").append(count);

            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(builder.toString());
            while (rs.next()) {
                times.add(rs.getTimestamp("time").getTime());
            }

        } catch (SQLException e) {
            log.error("Problem fetching TOP data", e);
            return times;
        } finally {
            DBUtil.closeStatement(LOG_CTX, stmt);
        }
        return times;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.hyperic.hq.measurement.shared.DataManager#getAvailableTopDataTimes
     * (int, long, long)
     */
    @Transactional(readOnly=true)
    public List<Long> getAvailableTopDataTimes(int resourceId, long from, long to) {
        List<Long> times = new ArrayList<Long>();
        Statement stmt = null;
        Connection conn = safeGetConnection();
        StringBuilder builder = new StringBuilder();
        try {
            builder.append("SELECT TIME FROM TOPNDATA TOP WHERE TOP.resourceid='").append(resourceId)
                    .append("' AND TOP.time BETWEEN '").append(new java.sql.Timestamp(from)).append("' AND '")
                    .append(new java.sql.Timestamp(to)).append("'");

            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(builder.toString());
            while (rs.next()) {
                times.add(rs.getTimestamp("time").getTime());
            }

        } catch (SQLException e) {
            log.error("Problem fetching TOP data", e);
            return times;
        } finally {
            DBUtil.closeStatement(LOG_CTX, stmt);
        }
        return times;
        
    }

    private boolean insertTopData(Connection conn, List<TopNData> topNData, boolean continueOnSQLException) throws
            SQLException {
        if (log.isDebugEnabled()) {
            log.debug("Inserting Top Processes data - '" + topNData + "'");
        }
        PreparedStatement stmt = null;
        final StringBuilder buf = new StringBuilder();

        Map<Date, List<TopNData>> dayToDataMap = mapDayToData(topNData);

        for (Entry<Date, List<TopNData>> entry : dayToDataMap.entrySet()) {
            try {

                String partitionName = getAndCreatePartition(TopNData.class.getSimpleName(), entry.getKey(), conn);
                stmt = conn.prepareStatement(buf.append("INSERT INTO ").append(partitionName).
                        append(" (resourceId, time, data) VALUES (?, ?, ?)").toString());

                for (TopNData data : entry.getValue()) {
                    stmt.setInt(1, data.getResourceId());
                    stmt.setTimestamp(2, new java.sql.Timestamp(data.getTime().getTime()));
                    stmt.setBytes(3, data.getData());
                    stmt.addBatch();
                }
                int[] execInfo = stmt.executeBatch();

            } catch (BatchUpdateException e) {
                if (!continueOnSQLException) {
                    throw e;
                }
            } catch (SQLException e) {
                if (!continueOnSQLException) {
                    throw e;
                }
                if (log.isDebugEnabled()) {
                    log.debug("A general SQLException occurred during the insert of TopN. ", e);
                }


            } finally {
                DBUtil.closeStatement(LOG_CTX, stmt);
            }

        }
        return true;
    }

    private Map<Date, List<TopNData>> mapDayToData(List<TopNData> topNData) {
        Map<Date, List<TopNData>> dayToDataMap = new HashMap<Date, List<TopNData>>();
        for (TopNData data : topNData) {
            Date day = DateUtils.truncate(data.getTime(), Calendar.DAY_OF_MONTH);
            List<TopNData> datas = dayToDataMap.get(day);
            if (datas == null) {
                dayToDataMap.put(day, new LinkedList<TopNData>());
            }
            dayToDataMap.get(day).add(data);
        }
        return dayToDataMap;
    }

    private String getAndCreatePartition(final String tableName, final Date time,
                                         final Connection conn) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("select get_and_create_partition(?,?);");
            ps.setString(1, tableName);
            ps.setTimestamp(2, new Timestamp(time.getTime()));
            final ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getString(1);
        } finally {
            DBUtil.closeStatement(LOG_CTX, ps);
        }
    }



    /**
     * This method is called to perform 'updates' for any inserts that failed.
     * 
     * @return The data insert result containing the data points that were not
     *         updated.
     */
    private List<DataPoint> updateData(Connection conn, List<DataPoint> data) {
        PreparedStatement stmt = null;
        List<DataPoint> left = new ArrayList<DataPoint>();
        Map<String, List<DataPoint>> buckets = MeasRangeObj.getInstance().bucketData(data);

        for (Entry<String, List<DataPoint>> entry : buckets.entrySet()) {
        String table = entry.getKey();
        List<DataPoint> dpts = entry.getValue();

        try {
            // TODO need to set synchronous commit to off
            stmt = conn.prepareStatement("UPDATE " + table + 
                                         " SET value = ? WHERE timestamp = ? AND measurement_id = ?");
            for (DataPoint pt : dpts) {
                Integer metricId = pt.getMeasurementId();
                MetricValue val = pt.getMetricValue();
                BigDecimal bigDec;
                bigDec = new BigDecimal(val.getValue());
                stmt.setBigDecimal(1, getDecimalInRange(bigDec, metricId));
                stmt.setLong(2, val.getTimestamp());
                stmt.setInt(3, metricId.intValue());
                stmt.addBatch();
            }

            int[] execInfo = stmt.executeBatch();
            left.addAll(getRemainingDataPoints(dpts, execInfo));
        } catch (BatchUpdateException e) {
            left.addAll(getRemainingDataPointsAfterBatchFail(dpts, e.getUpdateCounts()));
        } catch (SQLException e) {
            // If the batch update is not within a transaction, then we
            // don't know which of the updates completed successfully.
            // Assume they all failed.
            left.addAll(dpts);

            if (log.isDebugEnabled()) {
                log.debug("A general SQLException occurred during the update. " +
                          "Assuming that none of the " + dpts.size() +
                          " data points were updated.", e);
            }
        } finally {
            DBUtil.closeStatement(LOG_CTX, stmt);
        }
      }
        return left;
    }

    /**
     * Get the server purge configuration and storage option, loaded on startup.
     */
    private void loadConfigDefaults() {

        log.debug("Loading default purge intervals");
        Properties conf;
        try {
            conf = serverConfigManager.getConfig();
        } catch (Exception e) {
            throw new SystemException(e);
        }

        String purgeRawString = conf.getProperty(HQConstants.DataPurgeRaw);
        String purge1hString = conf.getProperty(HQConstants.DataPurge1Hour);
        String purge6hString = conf.getProperty(HQConstants.DataPurge6Hour);
        String purge1dString = conf.getProperty(HQConstants.DataPurge1Day);

        try {
            purgeRaw = Long.parseLong(purgeRawString);
            purge1h = Long.parseLong(purge1hString);
            purge6h = Long.parseLong(purge6hString);
            purge1d = Long.parseLong(purge1dString);
            
            confDefaultsLoaded = true;
        } catch (NumberFormatException e) {
            // Shouldn't happen unless manual edit of config table
            throw new IllegalArgumentException("Invalid purge interval: " + e);
        }
    }

    /**
     *  find the aggregation table with the greatest interval which is smaller than the requested time frame
     *  and which the time frame doesn't cover a range which is even partially after its purge has been done
     *  and in which no more than maxDTPs fit in the requested time frame
     * @throws TimeframeSizeException 
     * @throws TimeframeBoundriesException 
     */
    protected String getDataTable(long begin, long end, Measurement msmt, int maxDTPs) throws TimeframeSizeException, TimeframeBoundriesException {
        long tf = end - begin;  // the time frame
        long maxInterval = tf / maxDTPs; // the max interval for which maxDTPs DTPs still fit in the time frame
        long msmtInterval = msmt.getInterval();
        if (tf<msmtInterval) { 
            MeasurementTemplate tmp = msmt.getTemplate();
            throw new TimeframeSizeException("The requested time frame is of size " + tf + " milliseconds, which is smaller than the time interval of the measurement" + (tmp!=null?(" "+tmp.getName()):"") + " which is " + msmtInterval + " milliseconds");
        }
        long now = System.currentTimeMillis();
        if ((end>now) || (end<begin)) {
            throw new TimeframeBoundriesException("The requested time frame"
                    + (end>now?" ends in the future":(end<begin?" and":""))
                    + (end<begin?" ends before it starts":""));
        }
        if (!confDefaultsLoaded) {
            loadConfigDefaults();
        }
        if ((msmtInterval>=maxInterval) && (begin>=(now-DataManagerImpl.this.purgeRaw))) {
            return MeasurementUnionStatementBuilder.getUnionStatement(begin, end, msmt.getId().intValue(), measurementDAO.getHQDialect());
        }
        if (tf<HOUR_IN_MILLI) {
            MeasurementTemplate tmp = msmt.getTemplate();
            throw new TimeframeSizeException("The requested time frame is of size " + tf + " milliseconds, which is smaller than the hourly aggregated time interval of the measurement " + (tmp!=null?tmp.getName():""));
        }
        if ((HOUR_IN_MILLI>=maxInterval) && (begin>=(now-DataManagerImpl.this.purge1h))) {
            return MeasurementConstants.TAB_DATA_1H;
        }
        if (tf<SIX_HOURS_IN_MILLI) { 
            MeasurementTemplate tmp = msmt.getTemplate();
            throw new TimeframeSizeException("The requested time frame is of size " + tf + " milliseconds, which is smaller than the 6-hourly aggregated time interval of the measurement " + (tmp!=null?tmp.getName():""));
        }
        if ((SIX_HOURS_IN_MILLI>=maxInterval) && (begin>=(now-DataManagerImpl.this.purge6h))) {
            return MeasurementConstants.TAB_DATA_6H;
        }
        if (tf<DAY_IN_MILLI) { 
            MeasurementTemplate tmp = msmt.getTemplate();
            throw new TimeframeSizeException("The requested time frame is of size " + tf + " milliseconds, which is smaller than the daily aggregated time interval of the measurement " + (tmp!=null?tmp.getName():""));
        }
        // return daily aggregated data even if the time frame is beyond the purge time or contains more than 400 DTPs
        return MeasurementConstants.TAB_DATA_1D;
    }
    
    public String getDataTable(long begin, long end, int measId) {
        Integer[] empty = new Integer[1];
        empty[0] = new Integer(measId);
        return getDataTable(begin, end, empty);
    }
    


    private boolean usesMetricUnion(long begin) {
        long now = System.currentTimeMillis();
        if (MeasTabManagerUtil.getMeasTabStartTime(now - getPurgeRaw()) < begin) {
            return true;
        }
        return false;
    }

    private boolean usesMetricUnion(long begin, long end, boolean useAggressiveRollup) {
        if ((!useAggressiveRollup && usesMetricUnion(begin)) ||
            (useAggressiveRollup && (((end - begin) / HOUR) < HOURS_PER_MEAS_TAB))) {
            return true;
        }
        return false;
    }

    private String getDataTable(long begin, long end, Integer[] measIds) {
        return getDataTable(begin, end, measIds, false);
    }

    /**
     * @param begin beginning of the time range
     * @param end end of the time range
     * @param useAggressiveRollup will use the rollup tables if
     *      the timerange represents the same timerange as one
     *      metric data table
     */
    private String[] getDataTables(long begin, long end, boolean useAggressiveRollup) {
        long now = System.currentTimeMillis();
        if (!confDefaultsLoaded) {
            loadConfigDefaults();
        }
        if (usesMetricUnion(begin, end, useAggressiveRollup)) {
            return MeasTabManagerUtil.getMetricTables(begin, end);
        } else if ((now - this.purge1h) < begin) {
            return new String[] { TAB_DATA_1H };
        } else if ((now - this.purge6h) < begin) {
            return new String[] { TAB_DATA_6H };
        } else {
            return new String[] { TAB_DATA_1D };
        }
    }

    /**
     * @param begin beginning of the time range
     * @param end end of the time range
     * @param measIds the measurement_ids associated with the query. This is
     *        only used for 'UNION ALL' queries
     * @param useAggressiveRollup will use the rollup tables if the timerange
     *        represents the same timerange as one metric data table
     */
    private String getDataTable(long begin, long end, Integer[] measIds, boolean useAggressiveRollup) {
        long now = System.currentTimeMillis();

        if (!confDefaultsLoaded) {
            loadConfigDefaults();
        }

        if (usesMetricUnion(begin, end, useAggressiveRollup)) {
            return MeasurementUnionStatementBuilder.getUnionStatement(begin, end, measIds,
                measurementDAO.getHQDialect());
        } else if ((now - this.purge1h) < begin) {
            return TAB_DATA_1H;
        } else if ((now - this.purge6h) < begin) {
            return TAB_DATA_6H;
        } else {
            return TAB_DATA_1D;
        }
    }


    @Transactional(readOnly = true)
    public List<HighLowMetricValue> getHistoricalData(Measurement m, long begin, long end,
                                                          boolean prependAvailUnknowns, int maxDTPs) throws TimeframeSizeException, TimeframeBoundriesException {
        if (m.getTemplate().isAvailability()) {
            return availabilityManager.getHistoricalAvailData(m, begin, end, PageControl.PAGE_ALL, prependAvailUnknowns);
        } else {
            return getNonAvailabilityMetricData(m, begin, end, maxDTPs);
        }
    }
    
    /**
     * Fetch the list of historical data points given a begin and end time
     * range. Returns a PageList of DataPoints without begin rolled into time
     * windows.
     * 
     * @param m The Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @param prependAvailUnknowns determines whether to prepend AVAIL_UNKNOWN if the
     *        corresponding time window is not accounted for in the database.
     *        Since availability is contiguous this will not occur unless the
     *        time range precedes the first availability point.
     * @return the list of data points
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> getHistoricalData(Measurement m, long begin, long end,
                                                          PageControl pc,
                                                          boolean prependAvailUnknowns) {
        if (m.getTemplate().isAvailability()) {
            return availabilityManager.getHistoricalAvailData(m, begin, end, pc,
                prependAvailUnknowns);
        } else {
            return getHistData(m, begin, end, pc);
        }
    }

    /**
     * Fetch the list of historical data points given a begin and end time
     * range. Returns a PageList of DataPoints without begin rolled into time
     * windows.
     * 
     * @param m The Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the list of data points
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> getHistoricalData(Measurement m, long begin, long end,
                                                          PageControl pc) {
        return getHistoricalData(m, begin, end, pc, false);
    }

    private PageList<HighLowMetricValue> getHistData(final Measurement m, long begin, long end,
                                                     final PageControl pc) {
        checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);
        final ArrayList<HighLowMetricValue> history = new ArrayList<HighLowMetricValue>();
        // Get the data points and add to the ArrayList
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        // The table to query from
        final String table = getDataTable(begin, end, m.getId().intValue());
        final HQDialect dialect = measurementDAO.getHQDialect();
        try {
            conn = dbUtil.getConnection();
            stmt = conn.createStatement();
            try {
                final boolean sizeLimit = (pc.getPagesize() != PageControl.SIZE_UNLIMITED);
                final StringBuilder sqlBuf = new StringBuilder()
                    .append("SELECT :fields FROM ")
                    .append(table).append(" WHERE timestamp BETWEEN ")
                    .append(begin).append(" AND ").append(end)
                    .append(" AND measurement_id=").append(m.getId())
                    .append(" ORDER BY timestamp ").append(pc.isAscending() ? "ASC" : "DESC");
                Integer total = null;
                if (sizeLimit) {
                    // need to get the total count if there is a limit on the
                    // size. Otherwise we can just take the size of the
                    // resultset
                    rs = stmt.executeQuery(sqlBuf.toString().replace(":fields", "count(*)"));
                    total = (rs.next()) ? new Integer(rs.getInt(1)) : new Integer(1);
                    rs.close();
                    if (total.intValue() == 0) {
                        return new PageList<HighLowMetricValue>();
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("paging: offset= " + pc.getPageEntityIndex() + ", pagesize=" +
                                  pc.getPagesize());
                    }
                }
                final int offset = pc.getPageEntityIndex();
                final int limit = pc.getPagesize();
                final String sql = (sizeLimit) ?
                    dialect.getLimitBuf(sqlBuf.toString(), offset, limit) : sqlBuf.toString();
                if (log.isDebugEnabled()) {
                    log.debug(sql);
                }
                final StopWatch timer = new StopWatch();
                rs = stmt.executeQuery(sql.replace(":fields", "value, timestamp"));
                if (log.isTraceEnabled()) {
                    log.trace("getHistoricalData() execute time: " + timer.getElapsed());
                }
                while (rs.next()) {
                    history.add(getMetricValue(rs));
                }
                total = (total == null) ? new Integer(history.size()) : total;
                return new PageList<HighLowMetricValue>(history, total.intValue());
            } catch (SQLException e) {
                throw new SystemException("Can't lookup historical data for " + m, e);
            }
        } catch (SQLException e) {
            throw new SystemException("Can't open connection", e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, conn, stmt, rs);
        }
    }

    private List<HighLowMetricValue> getNonAvailabilityMetricData(final Measurement m, long begin, long end, int maxDTPs) throws TimeframeSizeException, TimeframeBoundriesException {
        checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);
        final ArrayList<HighLowMetricValue> history = new ArrayList<HighLowMetricValue>();
        // Get the data points and add to the ArrayList
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        // The table to query from
        final String table = getDataTable(begin, end, m,maxDTPs);
        try {
            conn = dbUtil.getConnection();
            stmt = conn.createStatement();
            try {
                final StringBuilder sqlBuf = new StringBuilder()
                .append("SELECT ")
                .append((table.equals(TAB_DATA_1H) || table.equals(TAB_DATA_6H) || table.equals(TAB_DATA_1D))?
                        ("maxvalue as peak, minvalue as low, "):"")
                .append("value, timestamp FROM ")
                .append(table).append(" WHERE timestamp BETWEEN ")
                .append(begin).append(" AND ").append(end)
                .append(" AND measurement_id=").append(m.getId())
                .append(" ORDER BY timestamp ").append("ASC");
                final String sql = sqlBuf.toString();
                if (log.isDebugEnabled()) {
                    log.debug(sql);
                }
                rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    history.add(getMetricValue(rs));
                }
                return new ArrayList<HighLowMetricValue>(history);
            } catch (SQLException e) {
                throw new SystemException("Can't lookup historical data for " + m, e);
            }
        } catch (SQLException e) {
            throw new SystemException("Can't open connection", e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, conn, stmt, rs);
        }
    }


    public Collection<HighLowMetricValue> getRawData(Measurement m, long begin, long end,
                                                     AtomicLong publishedInterval) {
        final long interval = m.getInterval();
        begin = TimingVoodoo.roundDownTime(begin, interval);
        end = TimingVoodoo.roundDownTime(end, interval);
        Collection<HighLowMetricValue> points;
        if (m.getTemplate().isAvailability()) {
            points = availabilityManager.getHistoricalAvailData(
                new Integer[] { m.getId() }, begin, end, interval, PageControl.PAGE_ALL, true);
            publishedInterval.set(interval);
        } else {
            points = getRawDataPoints(m, begin, end, publishedInterval);
        }
        return points;
    }

    private TreeSet<HighLowMetricValue> getRawDataPoints(Measurement m, long begin, long end,
                                                         AtomicLong publishedInterval) {
        final StringBuilder sqlBuf = getRawDataSql(m, begin, end, publishedInterval);
        final TreeSet<HighLowMetricValue> rtn =
            new TreeSet<HighLowMetricValue>(getTimestampComparator());
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = safeGetConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sqlBuf.toString());
            final int valCol = rs.findColumn("value");
            final int timestampCol = rs.findColumn("timestamp");
            while (rs.next()) {
                final double val = rs.getDouble(valCol);
                final long timestamp = rs.getLong(timestampCol);
                rtn.add(new HighLowMetricValue(val, timestamp));
            }
        } catch (SQLException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, conn, stmt, rs);
        }
        return rtn;
    }

    private StringBuilder getRawDataSql(Measurement m, long begin, long end,
                                        AtomicLong publishedInterval) {
        final String sql = new StringBuilder(128).append("SELECT value, timestamp FROM :table")
            .append(" WHERE timestamp BETWEEN ").append(begin).append(" AND ").append(end).append(
                " AND measurement_id=").append(m.getId()).toString();
        final String[] tables = getDataTables(begin, end, false);
        if (tables.length == 1) {
            if (tables[0].equals(TAB_DATA_1H)) {
                publishedInterval.set(HOUR);
            } else if (tables[0].equals(TAB_DATA_6H)) {
                publishedInterval.set(HOUR * 6);
            } else if (tables[0].equals(TAB_DATA_1D)) {
                publishedInterval.set(HOUR * 24);
            }
        }
        final StringBuilder sqlBuf = new StringBuilder(128 * tables.length);
        for (int i = 0; i < tables.length; i++) {
            sqlBuf.append(sql.replace(":table", tables[i]));
            if (i < (tables.length - 1)) {
                sqlBuf.append(" UNION ALL ");
            }
        }
        return sqlBuf;
    }

    private Comparator<MetricValue> getTimestampComparator() {
        return new Comparator<MetricValue>() {
            public int compare(MetricValue arg0, MetricValue arg1) {
                Long point0 = arg0.getTimestamp();
                Long point1 = arg1.getTimestamp();
                return point0.compareTo(point1);
            }
        };
    }

    /**
     * Aggregate data across the given metric IDs, returning max, min, avg, and
     * count of number of unique metric IDs
     * 
     * @param measurements {@link List} of {@link Measurement}s
     * @param begin The start of the time range
     * @param end The end of the time range
     * @return the An array of aggregate values
     * 
     */
    @Transactional(readOnly = true)
    public double[] getAggregateData(final List<Measurement> measurements, final long begin,
                                     final long end) {
        checkTimeArguments(begin, end);
        long interval = end - begin;
        interval = (interval == 0) ? 1 : interval;
        final List<HighLowMetricValue> pts = getHistoricalData(measurements, begin, end, interval,
            MeasurementConstants.COLL_TYPE_DYNAMIC, false, PageControl.PAGE_ALL);
        return getAggData(pts);
    }
    
    @Transactional(readOnly = true)
    public Map<Integer, double[]> getAggregateDataAndAvailUpByMetric(final List<Measurement> measurements,
            final long begin, final long end) throws SQLException {
        List<Integer> avids = new ArrayList<Integer>();
        List<Integer> mids = new ArrayList<Integer>();
        for (Measurement meas : measurements) {

            MeasurementTemplate t = meas.getTemplate();
            if (t.isAvailability()) {
                avids.add(meas.getId());
            } else {
                mids.add(meas.getId());
            }
        }
        Map<Integer, double[]> rtn = getAggDataByMetric(mids.toArray(new Integer[0]),begin, end, false);
        rtn.putAll(availabilityManager.getAggregateDataAndAvailUpByMetric(avids,begin, end));
        return rtn;
    }

    
    /**
     * Fetch the list of historical data points, grouped by template, given a
     * begin and end time range. Does not return an entry for templates with no
     * associated data. PLEASE NOTE: The
     * {@link MeasurementConstants.IND_LAST_TIME} index in the {@link double[]}
     * part of the returned map does not contain the real last value. Instead it
     * is an averaged value calculated from the last 1/60 of the specified time
     * range. If this becomes an issue the best way I can think of to solve is
     * to pass in a boolean "getRealLastTime" and issue another query to get the
     * last value if this is set. It is much better than the alternative of
     * always querying the last metric time because not all pages require this
     * value.
     * 
     * @param measurements The List of {@link Measurement}s to query
     * @param begin The start of the time range
     * @param end The end of the time range
     * @see org.hyperic.hq.measurement.server.session.AvailabilityManagerImpl#getHistoricalData()
     * @return the {@link Map} of {@link Integer} to {@link double[]} which
     *         represents templateId to data points
     * 
     */
    @Transactional(readOnly = true)
    public Map<Integer, double[]> getAggregateDataByTemplate(final List<Measurement> measurements,
                                                             final long begin, final long end) {
        // the idea here is to try and match the exact query executed by
        // getHistoricalData() when viewing the metric indicators page.
        // By issuing the same query we are hoping that the db's query
        // cache will optimize performance.
        checkTimeArguments(begin, end);
        final long interval = (end - begin) / 60;
        final List<Integer> availIds = new ArrayList<Integer>();
        final Map<Integer, List<Measurement>> measIdsByTempl = new HashMap<Integer, List<Measurement>>();
        setMeasurementObjects(measurements, availIds, measIdsByTempl);
        final Integer[] avIds = availIds.toArray(new Integer[0]);
        final Map<Integer, double[]> rtn = availabilityManager.getAggregateDataByTemplate(avIds,
            begin, end);
        rtn.putAll(getAggDataByTempl(measIdsByTempl, begin, end, interval));
        return rtn;
    }

    private Map<Integer, double[]> getAggDataByTempl(final Map<Integer,List<Measurement>> measIdsByTempl,
                                                     final long begin,
                                                     final long end,
                                                     final long interval) {
        final HashMap<Integer, double[]> rtn = new HashMap<Integer, double[]>(measIdsByTempl.size());
        for (final Map.Entry<Integer, List<Measurement>> entry : measIdsByTempl.entrySet()) {
            final Integer tid = entry.getKey();
            final List<Measurement> meas = entry.getValue();
            final List<HighLowMetricValue> pts = getHistoricalData(meas, begin, end, interval,
                MeasurementConstants.COLL_TYPE_DYNAMIC, false, PageControl.PAGE_ALL);
            final double[] aggData = getAggData(pts);
            if (aggData == null) {
                continue;
            }
            rtn.put(tid, aggData);
        }
        return rtn;
    }

    /**
     * @param measurements      source measurements list
     * @param availIds          measurements from the source list of type availability
     * @param measIdsByTempl    measurements from the source list which are not of type availability, sorted as per their type
     */
    private final void setMeasurementObjects(final List<Measurement> measurements,
                                             final List<Integer> availIds,
                                             final Map<Integer, List<Measurement>> measIdsByTempl) {
        for (Measurement m : measurements) {

            final MeasurementTemplate t = m.getTemplate();
            if (m.getTemplate().isAvailability()) {
                availIds.add(m.getId());
            } else {
                final Integer tid = t.getId();
                List<Measurement> list;
                if (null == (list = measIdsByTempl.get(tid))) {
                    list = new ArrayList<Measurement>();
                    measIdsByTempl.put(tid, list);
                }
                list.add(m);
            }
        }
    }

    private final double[] getAggData(final List<HighLowMetricValue> historicalData) {
        if (historicalData.size() == 0) {
            return null;
        }
        double high = Double.MIN_VALUE;
        double low = Double.MAX_VALUE;
        double total = 0;
        Double lastVal = null;
        int count = 0;
        long last = Long.MIN_VALUE;
        for (HighLowMetricValue mv : historicalData) {
            low = Math.min(mv.getLowValue(), low);
            high = Math.max(mv.getHighValue(), high);
            if (mv.getTimestamp() > last) {
                lastVal = new Double(mv.getValue());
            }
            final int c = mv.getCount();
            count = count + c;
            total = ((mv.getValue() * c) + total);
        }
        final double[] data = new double[MeasurementConstants.IND_LAST_TIME + 1];
        data[MeasurementConstants.IND_MIN] = low;
        data[MeasurementConstants.IND_AVG] = total / count;
        data[MeasurementConstants.IND_MAX] = high;
        data[MeasurementConstants.IND_CFG_COUNT] = count;
        if (lastVal != null) {
            data[MeasurementConstants.IND_LAST_TIME] = lastVal.doubleValue();
        }
        return data;
    }

    /**
     * Fetch the list of historical data points given a start and stop time
     * range and interval
     * 
     * @param measurements The List of Measurements to query
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param interval Interval for the time range
     * @param type Collection type for the metric
     * @param returnMetricNulls Specifies whether intervals with no data should
     *        be return as {@link HighLowMetricValue} with the value set as
     *        Double.NaN
     * @see org.hyperic.hq.measurement.server.session.AvailabilityManagerImpl#getHistoricalData()
     * @return the list of data points
     * 
     */
    @Transactional(readOnly = true)
    public PageList<HighLowMetricValue> getHistoricalData(final List<Measurement> measurements,
                                                          long begin, long end, long interval,
                                                          int type, boolean returnMetricNulls,
                                                          PageControl pc) {
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundUpTime(end, MINUTE);

        final List<Integer> availIds = new ArrayList<Integer>();
        final List<Integer> measIds = new ArrayList<Integer>();
        checkTimeArguments(begin, end, interval);
        interval = (interval == 0) ? 1 : interval;
        for (final Measurement m : measurements) {
            if (m.getTemplate().isAvailability()) {
                availIds.add(m.getId());
            } else {
                measIds.add(m.getId());
            }
        }
        final Integer[] avIds = availIds.toArray(new Integer[0]);
        final PageList<HighLowMetricValue> rtn =
            availabilityManager.getHistoricalAvailData(avIds, begin, end, interval, pc, true);
        final HQDialect dialect = measurementDAO.getHQDialect();
        final int points = (int) ((begin-end)/interval);
        final int maxExprs = (dialect.getMaxExpressions() == -1) ?
            Integer.MAX_VALUE : dialect.getMaxExpressions();
        for (int i = 0; i < measIds.size(); i += maxExprs) {
            final int last = Math.min(i + maxExprs, measIds.size());
            final List<Integer> sublist = measIds.subList(i, last);
            final Integer[] mids = sublist.toArray(new Integer[0]);
            final Collection<HighLowMetricValue> coll =
                getHistData(mids, begin, end, interval, returnMetricNulls, null);
            final PageList<HighLowMetricValue> pList =
                new PageList<HighLowMetricValue>(coll, points);
            merge(rtn, pList);
        }
        return rtn;
    }
    
    private void merge(int bucket, AggMetricValue[] rtn, AggMetricValue val, long timestamp) {
        AggMetricValue amv = null;
       
        try {
            amv = rtn[bucket];
            if (amv == null) {
                rtn[bucket] = val;
            } else {
                amv.merge(val);
            }
        } catch (NullPointerException e) {
            log.error("Error has occured in Merge function, bucket size[" + bucket + "] " +
                    "but AggMetricValue array is null, " + e, e); 
        } catch (ArrayIndexOutOfBoundsException e){
            log.error("Error has occured in Merge function, bucket size[" + bucket + "] " +
                    "but AggMetricValue array size is [" + rtn.length +"] ," + e, e);
        }
    }

    private Collection<HighLowMetricValue> getHistData(Integer[] mids, long start, long finish,
                                                       long windowSize, final boolean returnNulls,
                                                       AtomicLong publishedInterval) {
        final int buckets = (int) ((finish - start) / windowSize);
        final Collection<HighLowMetricValue> rtn = new ArrayList<HighLowMetricValue>(buckets);
        long tmp = start;
        final AggMetricValue[] values = getAggValueSets(mids, start, finish,
                windowSize, returnNulls, publishedInterval);
        for (int ii = 0; ii < values.length; ii++) {
            final AggMetricValue val = values[ii];
            if ((null == val) && returnNulls) {
                rtn.add(new HighLowMetricValue(Double.NaN, tmp + (ii * windowSize)));
                continue;
            } else if (null == val) {
                continue;
            } else {
                rtn.add(val.getHighLowMetricValue());
            }
        }
        return rtn;
    }

    private class AggMetricValue {
        private int count;
        private double max;
        private double min;
        private double sum;
        private final long timestamp;
        private AggMetricValue(long timestamp, double sum, double max, double min, int count) {
            this.timestamp = timestamp;
            this.sum = sum;
            this.max = max;
            this.min = min;
            this.count = count;
        }
        private void merge(AggMetricValue val) {
                count += val.count;
                max = (val.max > max) ? val.max : max;
                min  = (val.min < min)  ? val.min : min;
                sum += val.sum;
        }
        @SuppressWarnings("unused")
        private void set(double val) {
                count++;
                max = (val > max) ? val : max;
                min  = (val < min)  ? val : min;
                sum += val;
        }
        private double getAvg() {
                return sum/count;
        }
        private HighLowMetricValue getHighLowMetricValue() {
                HighLowMetricValue rtn = new HighLowMetricValue(getAvg(), max, min, timestamp);
                rtn.setCount(count);
                return rtn;
        }
    }
    
    private CharSequence getRawDataSql(Integer[] mids, long begin, long end,
                                       AtomicLong publishedInterval) {
        if ((mids == null) || (mids.length == 0)) {
            return "";
        }
        if (log.isDebugEnabled()) {
            log.debug("gathering data from begin=" + TimeUtil.toString(begin) + 
                      ", end=" + TimeUtil.toString(end));
        }
        final HQDialect dialect = measurementDAO.getHQDialect();
        // XXX I don't like adding the sql hint, when we start testing against mysql 5.5 we should
        //     re-evaluate if this is necessary
        // 1) we shouldn't have to tell the db to explicitly use the Primary key for these
        //    queries, it should just know because we update stats every few hours
        // 2) we only want to use the primary key for bigger queries.  Our tests show
        //    that the primary key performance is very consistent for large queries and smaller
        //    queries.  But for smaller queries the measurement_id index is more effective
        final String hint = (dialect.getMetricDataHint().isEmpty() || (mids.length < 1000)) ?
            "" : " " + dialect.getMetricDataHint();
        final String sql = new StringBuilder(1024 + (mids.length * 5))
            .append("SELECT count(*) as cnt, sum(value) as sumvalue, ")
            .append("min(value) as minvalue, max(value) as maxvalue, timestamp")
            .append(" FROM :table").append(hint)
            .append(" WHERE timestamp BETWEEN ").append(begin).append(" AND ").append(end)
            .append(MeasTabManagerUtil.getMeasInStmt(mids, true))
            .append(" GROUP BY timestamp")
            .toString();
        final String[] tables = getDataTables(begin, end, false);
        if ((publishedInterval != null) && (tables.length == 1)) {
            if (tables[0].equals(TAB_DATA_1H)) {
                publishedInterval.set(HOUR);
            } else if (tables[0].equals(TAB_DATA_6H)) {
                publishedInterval.set(HOUR * 6);
            } else if (tables[0].equals(TAB_DATA_1D)) {
                publishedInterval.set(HOUR * 24);
            }
        }
        final StringBuilder sqlBuf = new StringBuilder(128 * tables.length);
        for (int i = 0; i < tables.length; i++) {
            sqlBuf.append(sql.replace(":table", tables[i]));
            if (i < (tables.length - 1)) {
                sqlBuf.append(" UNION ALL ");
            }
        }
        return sqlBuf;
    }
    
    private AggMetricValue[] getAggValueSets(final Integer[] mids,
                                             final long start, final long finish,
                                             final long windowSize, final boolean returnNulls,
                                             final AtomicLong publishedInterval) {
        final String[] tables = getDataTables(start, finish, false);
        if (tables.length <= 0) {
            throw new SystemException(
                "ERROR: no data tables represent range " + TimeUtil.toString(start) +
                " - " + TimeUtil.toString(finish));
        }
        final MeasRange[] ranges = (tables.length > 1) ?
            MeasTabManagerUtil.getMetricRanges(start, finish) :
            new MeasRange[] {new MeasRange(tables[0], start, finish)};
        final String threadName = Thread.currentThread().getName();
        final List<Thread> threads = new ArrayList<Thread>(ranges.length);
        final Collection<AggMetricValue[]> data = new ArrayList<AggMetricValue[]>(ranges.length);
        final int maxThreads = 4;
        // The result encapsulates the timeframe start -> finish.  The results are gathered
        // via sub-queries.  Each sub-query is from begin -> end
        // start                                                                    finish
        // <----------------------------------------------------------------------------->
        // (begin-end)(begin-end)(begin-end)(begin-end)(begin-end)(begin-end)(begin-end)..
        for (final MeasRange range : ranges) {
            final long min = range.getMinTimestamp();
            final long max = range.getMaxTimestamp();
            final long begin = (min < start) ? start : min;
            final long end = (max > finish) ? finish : max;
            waitForThreads(threads, maxThreads);
            // XXX may want to add a thread pool or a static Executor here so that these
            // queries don't overwhelm the DB
            Thread thread = getNewDataWorkerThread(mids, start, finish, begin, end, windowSize,
                                                   returnNulls, publishedInterval, threadName,
                                                   data);
            thread.start();
            threads.add(thread);
        }
        waitForThreads(threads);
        return mergeThreadData(start, finish, windowSize, data);
    }
    
    /**
     * @param begin - the begin time of the sub window
     * @param end - the end time of the sub window
     * @param start - the start time of the user specified window
     * @param finish - the finish time of the user specified window
     */
    private Thread getNewDataWorkerThread(final Integer[] mids, final long start,
                                          final long finish, final long begin, final long end,
                                          final long windowSize, final boolean returnNulls,
                                          final AtomicLong publishedInterval,
                                          final String threadName,
                                          final Collection<AggMetricValue[]> data) {
        final boolean debug = log.isDebugEnabled();
        final Thread thread = new Thread() {
            @Override
            public void run() {
                final StopWatch watch = new StopWatch();
                if (debug) {
                    watch.markTimeBegin("data gatherer begin=" + TimeUtil.toString(begin) + 
                                        ", end=" + TimeUtil.toString(end));
                }
                final AggMetricValue[] array = getHistDataSet(mids, start, finish, begin, end,
                                                              windowSize, returnNulls,
                                                              publishedInterval, threadName);
                if (debug) {
                    watch.markTimeEnd("data gatherer begin=" + TimeUtil.toString(begin) + 
                                      ", end=" + TimeUtil.toString(end));
                    log.debug(watch);
                }
                synchronized (data) {
                    data.add(array);
                }
            }
        };
        return thread;
    }

    private AggMetricValue[] getHistDataSet(Integer[] mids, long start, long finish,
                                            long rangeBegin, long rangeEnd,
                                            long windowSize, final boolean returnNulls,
                                            AtomicLong publishedInterval, String threadName) {
        final CharSequence sqlBuf = getRawDataSql(mids, rangeBegin, rangeEnd, publishedInterval);
        final int buckets = (int) ((finish - start) / windowSize);
        final AggMetricValue[] array = new AggMetricValue[buckets];
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = safeGetConnection();
            stmt = conn.createStatement();
            int timeout = stmt.getQueryTimeout();
            if (timeout == 0) {
                stmt.setQueryTimeout(transactionTimeout);
            }
            rs = stmt.executeQuery("/* " + threadName + " */ " + sqlBuf.toString());
            final int sumValCol = rs.findColumn("sumvalue");
            final int countValCol = rs.findColumn("cnt");
            final int minValCol = rs.findColumn("minvalue");
            final int maxValCol = rs.findColumn("maxvalue");
            final int timestampCol = rs.findColumn("timestamp");
            while (rs.next()) {
                final double sum = rs.getDouble(sumValCol);
                final double min = rs.getDouble(minValCol);
                final double max = rs.getDouble(maxValCol);
                final int count = rs.getInt(countValCol);
                final long timestamp = rs.getLong(timestampCol);
                final AggMetricValue val = new AggMetricValue(timestamp, sum,
                        max, min, count);
                if ((timestamp < start) || (timestamp > finish)) {
                    continue;
                }
                final int bucket = (int) (buckets - ((finish - timestamp) / (float) windowSize));
                if (bucket < 0) {
                    continue;
                }
                merge(bucket, array, val, timestamp);
            }
        } catch (SQLException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(getClass().getName(), conn, stmt, rs);
        }
        return array;
    }
    
    private AggMetricValue[] mergeThreadData(long start, long finish, long windowSize,
                                             Collection<AggMetricValue[]> data) {
        final int buckets = (int) ((finish - start) / windowSize);
        final AggMetricValue[] rtn = new AggMetricValue[buckets];
        for (final AggMetricValue[] vals : data) {
            for (int ii = 0; ii < vals.length; ii++) {
                final AggMetricValue val = vals[ii];
                if (val == null) {
                    continue;
                }
                if (rtn[ii] == null) {
                    rtn[ii] = val;
                } else {
                    rtn[ii].merge(val);
                }
            }
        }
        return rtn;
    }

    private void waitForThreads(List<Thread> threads) {
        for (final Thread thread : threads) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    log.debug(e, e);
                }
            }
        }
    }

    private void waitForThreads(List<Thread> threads, int maxThreads) {
        if (threads.isEmpty() || (threads.size() < maxThreads)) {
            return;
        }
        int i=0;
        while (threads.size() >= maxThreads) {
            i = ((i >= threads.size()) || (i < 0)) ? 0 : i;
            final Thread thread = threads.get(i);
            try {
                if (thread.isAlive()) {
                    thread.join(100);
                }
                if (!thread.isAlive()) {
                    threads.remove(i);
                } else {
                    i++;
                }
            } catch (InterruptedException e) {
                log.debug(e,e);
            }
        }
    }

    private void merge(PageList<HighLowMetricValue> master, PageList<HighLowMetricValue> toMerge) {
        if (master.size() == 0) {
            master.addAll(toMerge);
            return;
        }
        for (int i = 0; i < master.size(); i++) {
            if (toMerge.size() < (i + 1)) {
                break;
            }
            final HighLowMetricValue val = master.get(i);
            final HighLowMetricValue mval = toMerge.get(i);
            final int mcount = mval.getCount();
            final int count = val.getCount();
            final int tot = count + mcount;
            final double high = ((val.getHighValue() * count) + (mval.getHighValue() * mcount)) /
                                tot;
            val.setHighValue(high);
            final double low = ((val.getLowValue() * count) + (mval.getLowValue() * mcount)) / tot;
            val.setLowValue(low);
            final double value = ((val.getValue() * count) + (mval.getValue() * mcount)) / tot;
            val.setValue(value);
        }
    }

    private long getPurgeRaw() {
        if (!confDefaultsLoaded) {
            loadConfigDefaults();
        }
        return purgeRaw;
    }

    /**
     * 
     * Get the last MetricValue for the given Measurement.
     * 
     * @param m The Measurement
     * @return The MetricValue or null if one does not exist.
     * 
     */
    @Transactional(readOnly = true)
    public MetricValue getLastHistoricalData(Measurement m) {
        if (m.getTemplate().isAvailability()) {
            return availabilityManager.getLastAvail(m);
        } else {
            return getLastHistData(m);
        }
    }

    private MetricValue getLastHistData(Measurement m) {
        // Check the cache

        MetricValue mval = metricDataCache.get(m.getId(), 0);
        if (mval != null) {
            return mval;
        }
        // Get the data points and add to the ArrayList
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbUtil.getConnection();

            final String metricUnion =
                MeasurementUnionStatementBuilder.getUnionStatement(8 * HOUR, m.getId(), measurementDAO.getHQDialect());
            final StringBuilder sqlBuf = new StringBuilder()
                .append("SELECT timestamp, value FROM ").append(metricUnion)
                    .append(", (SELECT MAX(timestamp) AS maxt").append(" FROM ").append(metricUnion)
                    .append(") mt ")
                .append("WHERE measurement_id = ").append(m.getId()).append(" AND timestamp = maxt");

            stmt = conn.createStatement();

            if (log.isDebugEnabled()) {
                log.debug("getLastHistoricalData(): " + sqlBuf);
            }

            rs = stmt.executeQuery(sqlBuf.toString());

            if (rs.next()) {
                MetricValue mv = getMetricValue(rs);
                metricDataCache.add(m.getId(), mv);
                return mv;
            } else {
                // No cached value, nothing in the database
                return null;
            }
        } catch (SQLException e) {
            log.error("Unable to look up historical data for " + m, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, conn, stmt, rs);
        }
    }

    /**
     * Fetch the most recent data point for particular Measurements.
     * 
     * @param mids The List of Measurements to query. In the list of
     *        Measurements null values are allowed as placeholders.
     * @param timestamp Only use data points with collection times greater than
     *        the given timestamp.
     * @return A Map of measurement ids to MetricValues. TODO: We should change
     *         this method to now allow NULL values. This is legacy and only
     *         used by the Metric viewer and Availabilty Summary portlets.
     * 
     */
    @Transactional(readOnly = true)
    public Map<Integer, MetricValue> getLastDataPoints(List<Integer> mids, long timestamp) {
        final List<Integer> availIds = new ArrayList<Integer>(mids.size());
        final List<Integer> measurementIds = new ArrayList<Integer>(mids.size());
        for (Integer measId : mids) {
            if (measId == null) {
                // See above.
                measurementIds.add(null);
                continue;
            }
            final Measurement m = measurementDAO.get(measId);
            if (m == null) {
                // See above.
                measurementIds.add(null);
                continue;
            } else if (m.getTemplate().isAvailability()) {
                availIds.add(m.getId());
            } else {
                measurementIds.add(measId);
            }
        }
        final Integer[] avIds = availIds.toArray(new Integer[0]);
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        if (debug) {
            watch.markTimeBegin("getLastDataPts");
        }
        final Map<Integer, MetricValue> data = getLastDataPts(measurementIds, timestamp);
        if (debug) {
            watch.markTimeEnd("getLastDataPts");
        }
        if (availIds.size() > 0) {
            if (debug) {
                watch.markTimeBegin("getLastAvail");
            }
            data.putAll(availabilityManager.getLastAvail(avIds));
            if (debug) {
                watch.markTimeEnd("getLastAvail");
            }
        }
        if (debug) {
            log.debug(watch);
        }
        return data;
    }

    private Map<Integer, MetricValue> getLastDataPts(Collection<Integer> mids, long timestamp) {
        final int BATCH_SIZE = 500;
        final Map<Integer, MetricValue> rtn = new HashMap<Integer, MetricValue>(mids.size());
        if ((mids == null) || mids.isEmpty()) {
            return rtn;
        }
        // all cached values are inserted into rtn
        // nodata represents values that are not cached
        final Collection<Integer> nodata = getCachedDataPoints(mids, rtn, timestamp);
        ArrayList<Integer> ids = null;
        final boolean debug = log.isDebugEnabled();
        if (nodata.isEmpty()) {
            if (debug) {
                log.debug("got data from cache");
            }
            // since we have all the data from cache (nodata is empty), just return it
            return rtn;
        } else {
            ids = new ArrayList<Integer>(nodata);
        }
        Connection conn = null;
        Statement stmt = null;
        final StopWatch watch = new StopWatch();
        try {
            conn = dbUtil.getConnection();
            stmt = conn.createStatement();
            for (int i=0; i<ids.size(); i+=BATCH_SIZE) {
                final int max = Math.min(ids.size(), i+BATCH_SIZE);
                // Create sub array
                Collection<Integer> subids = ids.subList(i, max);
                if (debug) {
                    watch.markTimeBegin("setDataPoints");
                }
                setDataPoints(rtn, timestamp, subids, stmt);
                if (debug) {
                    watch.markTimeEnd("setDataPoints");
                }
            }
        } catch (SQLException e) {
            throw new SystemException("Cannot get last values", e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, conn, stmt, null);
            if (debug) {
                log.debug(watch);
            }
        }
        List<DataPoint> dataPoints = convertMetricId2MetricValueMapToDataPoints(rtn);
        updateMetricDataCache(dataPoints);
        return rtn;
    }

    /**
     * Get data points from cache only
     */
    @Transactional(readOnly = true)
    public Collection<Integer> getCachedDataPoints(Collection<Integer> mids,
                                                   Map<Integer, MetricValue> data,
                                                   long timestamp) {
        ArrayList<Integer> nodata = new ArrayList<Integer>();
        for (Integer mid : mids) {
            if (mid == null) {
                continue;
            }
            final MetricValue mval = metricDataCache.get(mid, timestamp);
            if (mval != null) {
                data.put(mid, mval);
            } else {
                nodata.add(mid);
            }
        }
        return nodata;
    }

    private void setDataPoints(Map<Integer, MetricValue> data, long timestamp, Collection<Integer> measIds,
                               Statement stmt) throws SQLException {
        ResultSet rs = null;
        try {
            StringBuilder sqlBuf = getLastDataPointsSQL(timestamp, measIds);
            if (log.isTraceEnabled()) {
                log.trace("getLastDataPoints(): " + sqlBuf);
            }
            rs = stmt.executeQuery(sqlBuf.toString());

            while (rs.next()) {
                Integer mid = new Integer(rs.getInt(1));
                if (!data.containsKey(mid)) {
                    MetricValue mval = getMetricValue(rs);
                    data.put(mid, mval);
                    metricDataCache.add(mid, mval); // Add to cache to avoid
                    // lookup
                }
            }
        } finally {
            DBUtil.closeResultSet(LOG_CTX, rs);
        }
    }

    private StringBuilder getLastDataPointsSQL(long timestamp, Collection<Integer> measIds) {
        String tables = (timestamp != MeasurementConstants.TIMERANGE_UNLIMITED) ?
            MeasurementUnionStatementBuilder.getUnionStatement(timestamp, now(), measIds,
                                                               measurementDAO.getHQDialect()) :
            MeasurementUnionStatementBuilder.getUnionStatement(getPurgeRaw(), measIds,
                                                               measurementDAO.getHQDialect());

        StringBuilder sqlBuf = new StringBuilder(
            "SELECT measurement_id, value, timestamp" + " FROM " + tables + ", " +
                "(SELECT measurement_id AS id, MAX(timestamp) AS maxt" + " FROM " + tables +
                " WHERE ").append(MeasTabManagerUtil.getMeasInStmt(measIds, false));

        if (timestamp != MeasurementConstants.TIMERANGE_UNLIMITED) {
            ;
        }
        sqlBuf.append(" AND timestamp >= ").append(timestamp);

        sqlBuf.append(" GROUP BY measurement_id) mt").append(
            " WHERE timestamp = maxt AND measurement_id = id");
        return sqlBuf;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    /**
     * Convert the MetricId->MetricValue map to a list of DataPoints.
     * 
     * @param metricId2MetricValueMap The map to convert.
     * @return The list of DataPoints.
     */
    private List<DataPoint> convertMetricId2MetricValueMapToDataPoints(Map<Integer, MetricValue> metricId2MetricValueMap) {

        List<DataPoint> dataPoints = new ArrayList<DataPoint>(metricId2MetricValueMap.size());

        for (Entry<Integer, MetricValue> entry : metricId2MetricValueMap.entrySet()) {
        Integer mid = entry.getKey();
        MetricValue mval = entry.getValue();
        dataPoints.add(new DataPoint(mid, mval));
      }

        return dataPoints;
    }

    /**
     * Get a Baseline data.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public double[] getBaselineData(Measurement meas, long begin, long end) {
        if (meas.getTemplate().getAlias().equalsIgnoreCase("availability")) {
            Integer[] mids = new Integer[1];
            Integer id = meas.getId();
            mids[0] = id;
            return availabilityManager.getAggregateData(mids, begin, end).get(id);
        } else {
            return getBaselineMeasData(meas, begin, end);
        }
    }

    private double[] getBaselineMeasData(Measurement meas, long begin, long end) {
        // Check the begin and end times
        checkTimeArguments(begin, end);
        Integer id = meas.getId();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        // The table to query from
        String table = getDataTable(begin, end, id.intValue());
        try {
            conn = dbUtil.getConnection();

            StringBuilder sqlBuf = new StringBuilder(
                "SELECT MIN(value), AVG(value), MAX(value) FROM ").append(table).append(
                " WHERE timestamp BETWEEN ").append(begin).append(" AND ").append(end).append(
                " AND measurement_id = ").append(id.intValue());

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sqlBuf.toString());

            rs.next(); // Better have some result
            double[] data = new double[MeasurementConstants.IND_MAX + 1];
            data[MeasurementConstants.IND_MIN] = rs.getDouble(1);
            data[MeasurementConstants.IND_AVG] = rs.getDouble(2);
            data[MeasurementConstants.IND_MAX] = rs.getDouble(3);

            return data;
        } catch (SQLException e) {
            throw new MeasurementDataSourceException("Can't get baseline data for: " + id, e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, conn, stmt, rs);
        }
    }

    /**
     * Fetch a map of aggregate data values keyed by metrics given a start and
     * stop time range
     * 
     * @param tids The template id's of the Measurement
     * @param iids The instance id's of the Measurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param useAggressiveRollup uses a measurement rollup table to fetch the
     *        data if the time range spans more than one data table's max
     *        timerange
     * @return the Map of data points
     * 
     */
    @Transactional(readOnly = true)
    public Map<Integer, double[]> getAggregateDataByMetric(Integer[] tids, Integer[] iids,
                                                           long begin, long end,
                                                           boolean useAggressiveRollup) {
        checkTimeArguments(begin, end);
        Map<Integer, double[]> rtn = getAggDataByMetric(tids, iids, begin, end, useAggressiveRollup);
        Collection<Measurement> metrics = measurementDAO.findAvailMeasurements(tids, iids);
        if (metrics.size() > 0) {
            Integer[] mids = new Integer[metrics.size()];
            Iterator<Measurement> it = metrics.iterator();
            for (int i = 0; i < mids.length; i++) {
                Measurement m = it.next();
                mids[i] = m.getId();
            }
            rtn.putAll(availabilityManager.getAggregateData(mids, begin, end));
        }
        return rtn;
    }

    private Map<Integer, double[]> getAggDataByMetric(Integer[] tids, Integer[] iids, long begin,
                                                      long end, boolean useAggressiveRollup) {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        // Result set
        HashMap<Integer, double[]> resMap = new HashMap<Integer, double[]>();

        if ((tids.length == 0) || (iids.length == 0)) {
            return resMap;
        }

        // Get the data points and add to the ArrayList
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        StopWatch timer = new StopWatch();

        StringBuffer iconj = new StringBuffer(DBUtil
            .composeConjunctions("instance_id", iids.length));

        DBUtil.replacePlaceHolders(iconj, iids);
        StringBuilder tconj = new StringBuilder(DBUtil.composeConjunctions("template_id",
            tids.length));

        try {
            conn = dbUtil.getConnection();
            // The table to query from
            List<Integer> measids = MeasTabManagerUtil.getMeasIds(conn, tids, iids);
            String table = getDataTable(begin, end, measids.toArray(new Integer[0]),
                useAggressiveRollup);
            // Use the already calculated min, max and average on
            // compressed tables.
            String minMax;
            if (usesMetricUnion(begin, end, useAggressiveRollup)) {
                minMax = " MIN(value), AVG(value), MAX(value) ";
            } else {
                minMax = " MIN(minvalue), AVG(value), MAX(maxvalue) ";
            }
            final String aggregateSQL = "SELECT id, " +
                                        minMax +
                                        " FROM " +
                                        table +
                                        "," +
                                        TAB_MEAS +
                                        " WHERE timestamp BETWEEN ? AND ? AND measurement_id = id " +
                                        " AND " + iconj + " AND " + tconj + " GROUP BY id";
            if (log.isTraceEnabled()) {
                log.trace("getAggregateDataByMetric(): " + aggregateSQL);
            }
            stmt = conn.prepareStatement(aggregateSQL);
            int i = 1;
            stmt.setLong(i++, begin);
            stmt.setLong(i++, end);

            i = this.setStatementArguments(stmt, i, tids);

            try {
                rs = stmt.executeQuery();

                while (rs.next()) {
                    double[] data = new double[IND_MAX + 1];

                    Integer mid = new Integer(rs.getInt(1));
                    data[IND_MIN] = rs.getDouble(2);
                    data[IND_AVG] = rs.getDouble(3);
                    data[IND_MAX] = rs.getDouble(4);

                    // Put it into the result map
                    resMap.put(mid, data);
                }
            } finally {
                DBUtil.closeResultSet(LOG_CTX, rs);
            }
            if (log.isTraceEnabled()) {
                log.trace("getAggregateDataByMetric(): Statement query elapsed " + "time: " +
                          timer.getElapsed());
            }
            return resMap;
        } catch (SQLException e) {
            log.debug("getAggregateDataByMetric()", e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, conn, stmt, null);
        }
    }

    /**
     * Fetch a map of aggregate data values keyed by metrics given a start and
     * stop time range
     * 
     * @param measurements The id's of the Measurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param useAggressiveRollup uses a measurement rollup table to fetch the
     *        data if the time range spans more than one data table's max
     *        timerange
     * @return the map of data points
     * 
     */
    @Transactional(readOnly = true)
    public Map<Integer, double[]> getAggregateDataByMetric(List<Measurement> measurements,
                                                           long begin, long end,
                                                           boolean useAggressiveRollup) {
        checkTimeArguments(begin, end);
        List<Integer> avids = new ArrayList<Integer>();
        List<Integer> mids = new ArrayList<Integer>();
        for (Measurement meas : measurements) {

            MeasurementTemplate t = meas.getTemplate();
            if (t.isAvailability()) {
                avids.add(meas.getId());
            } else {
                mids.add(meas.getId());
            }
        }
        Map<Integer, double[]> rtn = getAggDataByMetric(mids.toArray(new Integer[0]),
            begin, end, useAggressiveRollup);
        rtn.putAll(availabilityManager.getAggregateData(avids.toArray(new Integer[0]),
            begin, end));
        return rtn;
    }

    private Map<Integer, double[]> getAggDataByMetric(Integer[] mids, long begin, long end,
                                                      boolean useAggressiveRollup) {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        // The table to query from
        String table = getDataTable(begin, end, mids, useAggressiveRollup);
        // Result set
        HashMap<Integer, double[]> resMap = new HashMap<Integer, double[]>();

        if (mids.length == 0) {
            return resMap;
        }

        // Get the data points and add to the ArrayList
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        StopWatch timer = new StopWatch();

        StringBuffer mconj = new StringBuffer(DBUtil.composeConjunctions("measurement_id",
            mids.length));
        DBUtil.replacePlaceHolders(mconj, mids);

        // Use the already calculated min, max and average on
        // compressed tables.
        String minMax;
        if (usesMetricUnion(begin, end, useAggressiveRollup)) {
            minMax = " MIN(value), AVG(value), MAX(value), ";
        } else {
            minMax = " MIN(minvalue), AVG(value), MAX(maxvalue), ";
        }

        final String aggregateSQL = "SELECT measurement_id, " + minMax + " count(*) " + " FROM " +
                                    table + " WHERE timestamp BETWEEN ? AND ? AND " + mconj +
                                    " GROUP BY measurement_id";

        try {
            conn = dbUtil.getConnection();

            if (log.isTraceEnabled()) {
                log.trace("getAggregateDataByMetric(): " + aggregateSQL);
            }

            stmt = conn.prepareStatement(aggregateSQL);

            int i = 1;
            stmt.setLong(i++, begin);
            stmt.setLong(i++, end);

            try {
                rs = stmt.executeQuery();

                while (rs.next()) {
                    double[] data = new double[IND_CFG_COUNT + 1];
                    Integer mid = new Integer(rs.getInt(1));
                    data[IND_MIN] = rs.getDouble(2);
                    data[IND_AVG] = rs.getDouble(3);
                    data[IND_MAX] = rs.getDouble(4);
                    data[IND_CFG_COUNT] = rs.getDouble(5);

                    // Put it into the result map
                    resMap.put(mid, data);
                }
            } finally {
                DBUtil.closeResultSet(LOG_CTX, rs);
            }

            if (log.isTraceEnabled()) {
                log.trace("getAggregateDataByMetric(): Statement query elapsed " + "time: " +
                          timer.getElapsed());
            }

            return resMap;
        } catch (SQLException e) {
            log.debug("getAggregateDataByMetric()", e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(LOG_CTX, conn, stmt, null);
        }
    }

    // TODO remove after HE-54 allows injection
    @Transactional(readOnly = true)
    public Analyzer getAnalyzer() {
        boolean analyze = false;
        try {
            Properties conf = serverConfigManager.getConfig();
            if (conf.containsKey(HQConstants.OOBEnabled)) {
                analyze = Boolean.valueOf(conf.getProperty(HQConstants.OOBEnabled)).booleanValue();
            }
        } catch (Exception e) {
            log.debug("Error looking up server configs", e);
        } finally {
            if (analyze) {
                return (Analyzer) ProductProperties
                    .getPropertyInstance("hyperic.hq.measurement.analyzer");
            }
        }
        return null;
    }

}
