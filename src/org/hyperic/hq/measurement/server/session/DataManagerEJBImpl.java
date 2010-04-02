/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hibernate.dialect.HQDialectUtil;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.MeasurementDataSourceException;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerUtil;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.hq.measurement.shared.MeasRangeObj;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.stats.ConcurrentStatsCollector;
import org.hyperic.util.timer.StopWatch;

/** The DataManagerEJB class is a stateless session bean that can be
 *  used to retrieve measurement data points
 *
 * @ejb:bean name="DataManager"
 *      jndi-name="ejb/measurement/DataManager"
 *      local-jndi-name="LocalDataManager"
 *      view-type="local"
 *      type="Stateless"
 *      transaction-type="Bean"
 */
public class DataManagerEJBImpl extends SessionEJB implements SessionBean {
    private final String logCtx = DataManagerEJBImpl.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);

    // The boolean system property that makes all events interesting. This 
    // property is provided as a testing hook so we can flood the event 
    // bus on demand.
    public final String ALL_EVENTS_INTERESTING_PROP = 
        "org.hq.triggers.all.events.interesting";
    
    private final BigDecimal MAX_DB_NUMBER =
        new BigDecimal("10000000000000000000000");
    
    private final long MINUTE = 60 * 1000,
                       HOUR = 60 * MINUTE;
        
    // Table names
    private final String TAB_DATA_1H = MeasurementConstants.TAB_DATA_1H;
    private final String TAB_DATA_6H = MeasurementConstants.TAB_DATA_6H;
    private final String TAB_DATA_1D = MeasurementConstants.TAB_DATA_1D;
    private final String TAB_DATA    = MeasurementConstants.TAB_DATA;
    private final String TAB_MEAS    = MeasurementConstants.TAB_MEAS;
    private final String TAB_NUMS    = "EAM_NUMBERS";
    private static final String DATA_MANAGER_INSERT_TIME =
        ConcurrentStatsCollector.DATA_MANAGER_INSERT_TIME;
    
    // Error strings
    private final String ERR_DB    = "Cannot look up database instance";
    private final String ERR_INTERVAL =
        "Interval cannot be larger than the time range";
    
    // Save some typing
    private final int IND_MIN       = MeasurementConstants.IND_MIN;
    private final int IND_AVG       = MeasurementConstants.IND_AVG;
    private final int IND_MAX       = MeasurementConstants.IND_MAX;
    private final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;
    
    // Pager class name
    private boolean confDefaultsLoaded = false;

    // Purge intervals, loaded once on first invocation.
    private long purgeRaw, purge1h, purge6h;

    private final long HOURS_PER_MEAS_TAB =
        MeasTabManagerUtil.NUMBER_OF_TABLES_PER_DAY;

    private Analyzer analyzer = null;

    private double getValue(ResultSet rs) throws SQLException {
        double val = rs.getDouble("value");

        if(rs.wasNull())
            val = Double.NaN;

        return val;
    }
    
    private HighLowMetricValue getMetricValue(ResultSet rs)
        throws SQLException 
    {
        long timestamp = rs.getLong("timestamp");
        double value = this.getValue(rs);
        
        if (!Double.isNaN(value)) {
            try {
                double high = rs.getDouble("peak");
                double low = rs.getDouble("low");
                return new HighLowMetricValue(value, high, low,
                                              timestamp);
            } catch (SQLException e) {
                // Peak and low columns do not exist
            }
        }
        return new HighLowMetricValue(value, timestamp);
    }

    // Returns the next index to be used
    private int setStatementArguments(PreparedStatement stmt, int start,
                                      Integer[] ids)
        throws SQLException 
    {
        // Set ID's
        int i = start;
        for (int ind = 0; ind < ids.length; ind++) {
            stmt.setInt(i++, ids[ind].intValue());
        }
        
        return i;
    }

    private void checkTimeArguments(long begin, long end, long interval)
        throws IllegalArgumentException {
        
        checkTimeArguments(begin, end);
        
        if(interval > (end - begin) )
            throw new IllegalArgumentException(ERR_INTERVAL);
    }
    
    /**
     * Save the new MetricValue to the database
     *
     * @param dp the new MetricValue
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     * @ejb:transaction-type="Required"
     * @ejb:interface-method
     */
    public void addData(Integer mid, MetricValue mv, boolean overwrite) {
        MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        Measurement meas = mMan.getMeasurement(mid);
        List pts = Collections.singletonList(new DataPoint(meas.getId(), mv));

        addData(pts, overwrite);
    }
    
    /**
     * Write metric data points to the DB with transaction
     * 
     * @param data       a list of {@link DataPoint}s 
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     *
     * @ejb:interface-method
     */
    public boolean addData(List data) {
        if (shouldAbortDataInsertion(data)) {
            return true;
        }

        data = enforceUnmodifiable(data);

        _log.debug("Attempting to insert data in a single transaction.");

        HQDialect dialect = Util.getHQDialect();
        boolean succeeded = false;
        final boolean debug = _log.isDebugEnabled();

        Connection conn = safeGetConnection();
        if (conn == null) {
            return false;
        }

        try
        {
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
                        _log.debug("Inserting data in a single transaction " +
                            "succeeded");
                        _log.debug("Data Insertion process took " +
                            (end-start) + " ms");
                    }
                    ConcurrentStatsCollector.getInstance().addStat(
                        end-start, DATA_MANAGER_INSERT_TIME);
                    sendMetricEvents(data);
                } else {
                    if (debug) {
                        _log.debug("Inserting data in a single transaction failed." +
                                   "  Rolling back transaction.");
                    }
                    conn.rollback();
                    conn.setAutoCommit(true);
                    List processed = addDataWithCommits(data, true, conn);
                    final long end = System.currentTimeMillis();
                    ConcurrentStatsCollector.getInstance().addStat(
                        end-start, DATA_MANAGER_INSERT_TIME);
                    sendMetricEvents(processed);
                    if (debug) {
                        _log.debug("Data Insertion process took " +
                            (end-start) + " ms");
                    }
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autocommit);
            }
        }
        catch (SQLException e) {
            _log.debug("Transaction failed around inserting metric data.", e);
        }
        finally {
            DBUtil.closeConnection(logCtx, conn);
        }
        return succeeded;        
    }

    /**
     * Write metric datapoints to the DB without transaction
     * 
     * @param data       a list of {@link DataPoint}s 
     * @param overwrite  If true, attempt to over-write values when an insert
     *                   of the data fails (i.e. it already exists). You may
     *                   not want to over-write values when, for instance, the 
     *                   back filler is inserting data.
     * @throws NumberFormatException if the value from the
     *         DataPoint.getMetricValue() cannot instantiate a BigDecimal
     *
     * @ejb:interface-method
     */
    public void addData(List data, boolean overwrite) {
        /**
         * We have to account for 2 types of metric data insertion here:
         *  1 - New data, using 'insert'
         *  2 - Old data, using 'update'
         *  
         * We optimize the amount of DB roundtrips here by executing in batch,
         * however there are some serious gotchas:
         * 
         *  1 - If the 'insert' batch update fails, a BatchUpdateException can
         *      be thrown instead of just returning an error within the
         *      executeBatch() array.  
         *  2 - This is further complicated by the fact that some drivers will
         *      throw the exception at the first instance of an error, and some
         *      will continue with the rest of the batch.
         */
        if (shouldAbortDataInsertion(data)) {
            return;
        }

        _log.debug("Attempting to insert/update data outside a transaction.");

        data = enforceUnmodifiable(data);

        Connection conn = safeGetConnection();

        if (conn == null) {
            _log.debug("Inserting/Updating data outside a transaction failed.");
            return;
        }
                
        try {
            boolean autocommit = conn.getAutoCommit();
            
            try {
                conn.setAutoCommit(true);
                addDataWithCommits(data, overwrite, conn);                
            }
            finally {
                conn.setAutoCommit(autocommit);
            }
        }
        catch (SQLException e) {
            _log.debug("Inserting/Updating data outside a transaction failed " +
                       "because autocommit management failed.", e);          
        }
        finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    private List addDataWithCommits(List data, boolean overwrite,
                                    Connection conn)
    {
        Set failedToSaveMetrics = new HashSet();
        List left = data;
        while (!left.isEmpty())
        {
            int numLeft = left.size();
            if (_log.isDebugEnabled()) {
                _log.debug("Attempting to insert " + numLeft + " points");
            }
            
            try {
                left = insertData(conn, left, true);
            } catch (SQLException e) {
                assert false : "The SQLException should not happen: "+e;
            }
                            
            if (_log.isDebugEnabled()) {
                _log.debug("Num left = " + left.size());
            }
            
            if (left.isEmpty())
                break;
            
            if (!overwrite) {
                if (_log.isDebugEnabled()) {
                    _log.debug("We are not updating the remaining "+
                                left.size()+" points");
                }
                failedToSaveMetrics.addAll(left);
                break;
            }
                                
            // The insert couldn't insert everything, so attempt to update
            // the things that are left
            if (_log.isDebugEnabled()) {
                _log.debug("Sending " + left.size() + " data points to update");
            }
                            
            left = updateData(conn, left);                    
            
            if (left.isEmpty())
                break;

            if (_log.isDebugEnabled()) {
                _log.debug("Update left " + left.size() + " points to process");
            }
            
            if (numLeft == left.size()) {
                DataPoint remPt = (DataPoint)left.remove(0);
                failedToSaveMetrics.add(remPt);
                // There are some entries that we weren't able to do
                // anything about ... that sucks.
                _log.warn("Unable to do anything about " + numLeft + 
                          " data points.  Sorry.");
                _log.warn("Throwing away data point " + remPt);
            }
        }

        _log.debug("Inserting/Updating data outside a transaction finished.");
        
        return removeMetricsFromList(data, failedToSaveMetrics);
    }
    
    private boolean shouldAbortDataInsertion(List data) {
        if (data.isEmpty()) {
            _log.debug("Aborting data insertion since data list is empty. This is ok.");
            return true;
        } else {
            return false;
        }
    }
    
    private List enforceUnmodifiable(List aList) {
        return Collections.unmodifiableList(aList);
    }
    
    private List removeMetricsFromList(List data, Set metricsToRemove) {
        if (metricsToRemove.isEmpty()) {
            return data;
        }
        
        Set allMetrics = new HashSet(data);
        allMetrics.removeAll(metricsToRemove);
        return new ArrayList(allMetrics);
    }

    /**
     * Convert a decimal value to something suitable for being thrown into the database
     * with a NUMERIC(24,5) definition
     */
    private BigDecimal getDecimalInRange(BigDecimal val, Integer metricId) {
        val = val.setScale(5, BigDecimal.ROUND_HALF_EVEN);
        if (val.compareTo(MAX_DB_NUMBER) == 1) {
            _log.warn("Value [" + val + "] for metric id=" + metricId +
                      "is too big to put into the DB.  Truncating to [" +
                      MAX_DB_NUMBER+ "]");
            return MAX_DB_NUMBER;
        }
        
        return val;
    }
        
    private void sendMetricEvents(List data) {
        if (data.isEmpty()) {
            return;
        }
        
        // Finally, for all the data which we put into the system, make sure
        // we update our internal cache, kick off the events, etc.
        final boolean debug = _log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        if (debug) watch.markTimeBegin("analyzeMetricData");
        analyzeMetricData(data);
        if (debug) watch.markTimeEnd("analyzeMetricData");
        
        Collection cachedData = updateMetricDataCache(data);
        sendDataToEventHandlers(cachedData);        
        if (debug) {
            _log.debug(watch);
        }
    }  

    private void analyzeMetricData(List data)
    {
        if (analyzer != null)
        {
            for (Iterator i = data.iterator(); i.hasNext();) {
                DataPoint dp = (DataPoint) i.next();
                analyzer.analyzeMetricValue(dp.getMetricId(), dp.getMetricValue());
            }
        }
    }

    private Collection updateMetricDataCache(List data) {
        MetricDataCache cache = MetricDataCache.getInstance();
        return cache.bulkAdd(data);
    }

    private void sendDataToEventHandlers(Collection data) {
        ArrayList events  = new ArrayList();
        List zevents = new ArrayList();
        MeasurementManagerLocal measMan = MeasurementManagerEJBImpl.getOne();
        
        final boolean allEventsInteresting = 
            Boolean.getBoolean(ALL_EVENTS_INTERESTING_PROP);
        
        final StopWatch watch = new StopWatch();
        final boolean debug = _log.isDebugEnabled();
        for (Iterator i = data.iterator(); i.hasNext();) {
            DataPoint dp = (DataPoint) i.next();
            Integer metricId = dp.getMetricId();
            MetricValue val = dp.getMetricValue();
            MeasurementEvent event = new MeasurementEvent(metricId, val);

            if (debug) watch.markTimeBegin("isTriggerInterested");
            boolean isEventInteresting =
                allEventsInteresting || RegisteredTriggers.isTriggerInterested(event);
            if (debug) watch.markTimeEnd("isTriggerInterested");
            if (isEventInteresting) {
                measMan.buildMeasurementEvent(event);
                events.add(event);
            }

            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }
        if (debug) _log.debug(watch);
        
        if (!events.isEmpty()) {
            Messenger sender = new Messenger();
            sender.publishMessage(EventConstants.EVENTS_TOPIC, events);
        }
        
        if (!zevents.isEmpty()) {
            try {
                // XXX:  Shouldn't this be a transactional queueing?
                ZeventManager.getInstance().enqueueEvents(zevents);
            } catch(InterruptedException e) {
                _log.warn("Interrupted while sending events.  Some data may " +
                          "be lost");
            }
        }
    }
    
    private List getRemainingDataPoints(List data, int[] execInfo)
    {
        List res = new ArrayList();
        int idx = 0;

        // this is the case for mysql
        if (execInfo.length == 0)
            return res;
            
        for (Iterator i=data.iterator(); i.hasNext(); idx++) {
            DataPoint pt = (DataPoint)i.next();

            if (execInfo[idx] == Statement.EXECUTE_FAILED)
                res.add(pt);
        }
        
        if (_log.isDebugEnabled()) {
            _log.debug("Need to deal with " + res.size() + " unhandled " + 
                       "data points (out of " + execInfo.length + ")");
        }
        return res;
    }
    
    private List getRemainingDataPointsAfterBatchFail(List data, int[] counts) {  
        List res = new ArrayList();
        Iterator i=data.iterator();
        int idx;
    
        for (idx=0; idx < counts.length; idx++) { 
            DataPoint pt = (DataPoint)i.next();
        
            if (counts[idx] == Statement.EXECUTE_FAILED) {
                res.add(pt);
            }
        }
    
        if (_log.isDebugEnabled()) {
            _log.debug("Need to deal with " + res.size() + " unhandled " + 
                       "data points (out of " + counts.length + ").  " +
                       "datasize=" + data.size());
        }
        
        // It's also possible that counts[] is not as long as the list
        // of data points, so we have to return all the un-processed points
        if (data.size() != counts.length)
            res.addAll(data.subList(idx, data.size()));
        return res;
    }

    /**
     * Insert the metric data points to the DB with one insert statement. This 
     * should only be invoked when the DB supports multi-insert statements.
     * 
     * @param data a list of {@link DataPoint}s 
     * @return <code>true</code> if the multi-insert succeeded; <code>false</code> 
     *         otherwise.
     */
    private boolean insertDataWithOneInsert(List data, Connection conn) {
        Statement stmt = null;
        ResultSet rs = null;
        Map buckets = MeasRangeObj.getInstance().bucketData(data);
        
        try {
            for (Iterator it = buckets.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                String table = (String) entry.getKey();
                List dpts = (List) entry.getValue();

                StringBuilder values = new StringBuilder();
                int rowsToUpdate = 0;
                for (Iterator i=dpts.iterator(); i.hasNext(); ) {
                    DataPoint pt = (DataPoint)i.next();
                    Integer metricId  = pt.getMetricId();
                    MetricValue val   = pt.getMetricValue();
                    BigDecimal bigDec;
                    bigDec = new BigDecimal(val.getValue());
                    rowsToUpdate++;
                    values.append("(").append(val.getTimestamp()).append(", ")
                          .append(metricId.intValue()).append(", ")
                          .append(getDecimalInRange(bigDec, metricId))
                          .append("),");
                }
                String sql = "insert into "+table+" (timestamp, measurement_id, "+
                             "value) values "+values.substring(0, values.length()-1);
                stmt = conn.createStatement();
                int rows = stmt.executeUpdate(sql);
                if (_log.isDebugEnabled()) {
                    _log.debug("Inserted "+rows+" rows into "+table+
                               " (attempted "+rowsToUpdate+" rows)");
                }
                if (rows < rowsToUpdate)
                    return false;
            }   
        } catch (SQLException e) {
            // If there is a SQLException, then none of the data points 
            // should be inserted. Roll back the txn.
            if (_log.isDebugEnabled()) {
                _log.debug("Error inserting data with one insert stmt: " +
                    e.getMessage() + " (this is ok)");
            }
            return false;            
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
        }
        return true;
    }
    
    /**
     * Insert the metric data points to the DB in batch.
     * 
     * @param data a list of {@link DataPoint}s 
     * @return <code>true</code> if the batch insert succeeded; <code>false</code> 
     *         otherwise.       
     */
    private boolean insertDataInBatch(List data, Connection conn) {
        List left = data;
        
        try {            
            if (_log.isDebugEnabled()) {
                _log.debug("Attempting to insert " + left.size() + " points");
            }
            left = insertData(conn, left, false);            
            if (_log.isDebugEnabled()) {
                _log.debug("Num left = " + left.size());
            }
            
            if (!left.isEmpty()) {
                if (_log.isDebugEnabled()) {
                    _log.debug("Need to update " + left.size() + " data points.");
                    _log.debug("Data points to update: " + left);                
                }
                
                return false;
            }
        } catch (SQLException e) {
            // If there is a SQLException, then none of the data points 
            // should be inserted. Roll back the txn.
            if (_log.isDebugEnabled()) {
                _log.debug("Error while inserting data in batch (this is ok) " +
                    e.getMessage());
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
            // XXX:  Get a better connection here - directly from Hibernate
            // XXX scottmf, may not make a difference transactionally since
            // we use ConnectionReleaseMode.AFTER_STATEMENT
            conn = DBUtil.getConnByContext(getInitialContext(), 
                        DATASOURCE_NAME);              
        } catch (NamingException e) {
            _log.error("Failed to retrieve data source", e);
        } catch (SQLException e) {
            _log.error("Failed to retrieve connection from data source", e);
        }
        
        return conn;
    }

    /**
     * This method inserts data into the data table.  If any data points in the
     * list fail to get added (e.g. because of a constraint violation), it will
     * be returned in the result list.
     * 
     * @param conn The connection.
     * @param data The data points to insert.
     * @param continueOnSQLException <code>true</code> to continue inserting the 
     *                               rest of the data points even after a 
     *                               <code>SQLException</code> occurs; 
     *                               <code>false</code> to throw the 
     *                               <code>SQLException</code>.
     * @return The list of data points that were not inserted.
     * @throws SQLException only if there is an exception for one of the data 
     *                      point batch inserts and <code>continueOnSQLException</code> 
     *                      is set to <code>false</code>.
     */
    private List insertData(Connection conn, 
                            List data, 
                            boolean continueOnSQLException) 
        throws SQLException {
        PreparedStatement stmt = null;
        final List left = new ArrayList();
        final Map buckets = MeasRangeObj.getInstance().bucketData(data);
        final HQDialect dialect = Util.getHQDialect();
        final boolean supportsDupInsStmt = dialect.supportsDuplicateInsertStmt();
        final boolean supportsPLSQL = dialect.supportsPLSQL();
        final String plSQL = 
            "BEGIN " +
            "INSERT INTO :table (measurement_id, timestamp, value) " +
            "VALUES(?, ?, ?); " +
            "EXCEPTION WHEN DUP_VAL_ON_INDEX THEN " +
                "UPDATE :table SET VALUE = ? " +
                "WHERE timestamp = ? and measurement_id = ?; " +
            "END; ";

        final StringBuilder buf = new StringBuilder();
        for (Iterator it = buckets.entrySet().iterator(); it.hasNext(); )
        {
            buf.setLength(0);
            Map.Entry entry = (Map.Entry) it.next();
            final String table = (String) entry.getKey();
            final List dpts = (List) entry.getValue();

            try
            {
                if (supportsDupInsStmt) {
                    stmt = conn.prepareStatement(
                        buf.append("INSERT INTO ").append(table)
                           .append(" (measurement_id, timestamp, value) VALUES (?, ?, ?)")
                           .append(" ON DUPLICATE KEY UPDATE value = ?")
                           .toString());
                }
                else if (supportsPLSQL) {
                    final String sql =  plSQL.replaceAll(":table", table);
                    stmt = conn.prepareStatement(sql);
                }
                else {
                    stmt = conn.prepareStatement(
                        buf.append("INSERT INTO ").append(table)
                           .append(" (measurement_id, timestamp, value) VALUES (?, ?, ?)")
                           .toString());
                }

                for (Iterator i=dpts.iterator(); i.hasNext(); )
                {
                    DataPoint pt = (DataPoint)i.next();
                    Integer metricId  = pt.getMetricId();
                    MetricValue val   = pt.getMetricValue();
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
            }
            catch (BatchUpdateException e) {
                if (!continueOnSQLException) {
                    throw e;
                }
                
                left.addAll(
                    getRemainingDataPointsAfterBatchFail(dpts, 
                                                         e.getUpdateCounts()));
            }
            catch (SQLException e) {
                if (!continueOnSQLException) {
                    throw e;
                }
                
                // If the batch insert is not within a transaction, then we 
                // don't know which of the inserts completed successfully. 
                // Assume they all failed.
                left.addAll(dpts);
                
                if (_log.isDebugEnabled()) {
                    _log.debug("A general SQLException occurred during the insert. " +
                               "Assuming that none of the "+dpts.size()+
                               " data points were inserted.", e);
                }
            }
            finally {
                DBUtil.closeStatement(logCtx, stmt);
            }            
        }

        return left;
    }

    /**
     * This method is called to perform 'updates' for any inserts that failed. 
     * 
     * @return The data insert result containing the data points that were 
     *         not updated.
     */
    private List updateData(Connection conn, List data) {
        PreparedStatement stmt = null;
        List left = new ArrayList();
        Map buckets = MeasRangeObj.getInstance().bucketData(data);
        
        for (Iterator it = buckets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String table = (String) entry.getKey();
            List dpts = (List) entry.getValue();

            try {
                stmt = conn.prepareStatement(
                    "UPDATE " + table + 
                    " SET value = ? WHERE timestamp = ? AND measurement_id = ?");

                for (Iterator i = dpts.iterator(); i.hasNext();) {
                    DataPoint pt = (DataPoint) i.next();
                    Integer metricId  = pt.getMetricId();
                    MetricValue val   = pt.getMetricValue();
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
                left.addAll(
                    getRemainingDataPointsAfterBatchFail(dpts, 
                                                         e.getUpdateCounts()));
            } catch (SQLException e) {
                // If the batch update is not within a transaction, then we 
                // don't know which of the updates completed successfully. 
                // Assume they all failed.
                left.addAll(dpts);
                
                if (_log.isDebugEnabled()) {
                    _log.debug("A general SQLException occurred during the update. " +
                               "Assuming that none of the "+dpts.size()+
                               " data points were updated.", e);
                }
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }
        }
        return left;
    }

    /**
     * Get the server purge configuration and storage option, loaded on startup.
     */
    private void loadConfigDefaults() { 

        _log.debug("Loading default purge intervals");
        Properties conf;
        try {
            conf = ServerConfigManagerEJBImpl.getOne().getConfig();
        } catch (Exception e) {
            throw new SystemException(e);
        }

        String purgeRawString = conf.getProperty(HQConstants.DataPurgeRaw);
        String purge1hString  = conf.getProperty(HQConstants.DataPurge1Hour);
        String purge6hString  = conf.getProperty(HQConstants.DataPurge6Hour);

        try {
            purgeRaw = Long.parseLong(purgeRawString);
            purge1h  = Long.parseLong(purge1hString);
            purge6h  = Long.parseLong(purge6hString);
            confDefaultsLoaded = true;
        } catch (NumberFormatException e) {
            // Shouldn't happen unless manual edit of config table
            throw new IllegalArgumentException("Invalid purge interval: " + e);
        }
    }

    private String getDataTable(long begin, long end, int measId)
    {
        Integer[] empty = new Integer[1];
        empty[0] = new Integer(measId);
        return getDataTable(begin,end,empty);
    }

    private boolean usesMetricUnion(long begin)
    {
        long now = System.currentTimeMillis();
        if (MeasTabManagerUtil.getMeasTabStartTime(now - getPurgeRaw()) < begin) {
            return true;
        }
        return false;
    }

    private boolean usesMetricUnion(long begin, long end,
                                    boolean useAggressiveRollup)
    {
        if (!useAggressiveRollup && usesMetricUnion(begin) ||
            (useAggressiveRollup && ((end-begin)/HOUR) < HOURS_PER_MEAS_TAB)) {
            return true;
        }
        return false;
    }

    private String getDataTable(long begin, long end, Integer[] measIds)
    {
        return getDataTable(begin, end, measIds, false);
    }

    /**
     * @param begin beginning of the time range
     * @param end end of the time range
     * @param measIds the measurement_ids associated with the query.
     *      This is only used for 'UNION ALL' queries
     * @param useAggressiveRollup will use the rollup tables if
     *      the timerange represents the same timerange as one
     *      metric data table
     */
    private String getDataTable(long begin, long end, Integer[] measIds,
                                boolean useAggressiveRollup) {
        long now = System.currentTimeMillis();

        if (!confDefaultsLoaded) {
            loadConfigDefaults();
        }

        if (usesMetricUnion(begin, end, useAggressiveRollup)) {
            return MeasTabManagerUtil.getUnionStatement(begin, end, measIds);
        } else if (now - this.purge1h < begin) {
            return TAB_DATA_1H;
        } else if (now - this.purge6h < begin) {
            return TAB_DATA_6H;
        } else {
            return TAB_DATA_1D;
        }
    }

    /**
     * Fetch the list of historical data points given a begin and end time range.
     * Returns a PageList of DataPoints without begin rolled into time windows.
     *
     * @param m The Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @param prependUnknowns determines whether to prepend AVAIL_UNKNOWN if the
     * corresponding time window is not accounted for in the database.  Since
     * availability is contiguous this will not occur unless the time range
     * precedes the first availability point.
     * @return the list of data points
     * @ejb:interface-method
     */
    public PageList getHistoricalData(Measurement m, long begin, long end,
                                      PageControl pc,
                                      boolean prependAvailUnknowns) {
        if (m.getTemplate().isAvailability()) {
            return getAvailMan().getHistoricalAvailData(m, begin, end, pc,
                prependAvailUnknowns);
        } else {
            return getHistData(m, begin, end, pc);
        }
    }

    /**
     * Fetch the list of historical data points given a begin and end time range.
     * Returns a PageList of DataPoints without begin rolled into time windows.
     *
     * @param m The Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the list of data points
     * @ejb:interface-method
     */
    public PageList getHistoricalData(Measurement m, long begin, long end,
                                      PageControl pc) {
        return getHistoricalData(m, begin, end, pc, false);
    }

    private PageList getHistData(final Measurement m,
                                 long begin, long end,
                                 final PageControl pc) {
        checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);
        final ArrayList history = new ArrayList();
        // Get the data points and add to the ArrayList
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        // The table to query from
        final String table = getDataTable(begin, end, m.getId().intValue());
        try {
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            stmt = conn.createStatement();
            try {
                final boolean sizeLimit =
                    (pc.getPagesize() != PageControl.SIZE_UNLIMITED);
                final StringBuilder sqlBuf = new StringBuilder()
                    .append("SELECT :fields FROM ").append(table)
                    .append(" WHERE timestamp BETWEEN ")
                    .append(begin).append(" AND ").append(end)
                    .append(" AND measurement_id=").append(m.getId())
                    .append(" ORDER BY timestamp ")
                    .append(pc.isAscending() ? "ASC" : "DESC");
                Integer total = null;
                if (sizeLimit) {
                    // need to get the total count if there is a limit on the
                    // size.  Otherwise we can just take the size of the
                    // resultset
                    rs = stmt.executeQuery(
                        sqlBuf.toString().replace(":fields", "count(*)"));
                    total = (rs.next()) ?
                        new Integer(rs.getInt(1)) : new Integer(1);
                    rs.close();
                    if (total.intValue() == 0) {
                        return new PageList();
                    }
                    if (_log.isDebugEnabled()) {
                        _log.debug("paging: offset= " + pc.getPageEntityIndex() +
                            ", pagesize=" + pc.getPagesize());
                    }
                }
                final HQDialect dialect = Util.getHQDialect();
                final int offset = pc.getPageEntityIndex();
                final int limit = pc.getPagesize();
                final String sql = (sizeLimit) ?
                    dialect.getLimitBuf(sqlBuf.toString(), offset, limit) :
                    sqlBuf.toString();
                if (_log.isDebugEnabled()) {
                    _log.debug(sql);
                }
                final StopWatch timer = new StopWatch();
                rs = stmt.executeQuery(
                    sql.replace(":fields", "value, timestamp"));
                if (_log.isTraceEnabled()) {
                    _log.trace("getHistoricalData() execute time: "
                        + timer.getElapsed());
                }
                while (rs.next()) {
                    history.add(getMetricValue(rs));
                }
                total = (total == null) ? new Integer(history.size()) : total;
                return new PageList(history, total.intValue());
            } catch (SQLException e) {
                throw new SystemException(
                    "Can't lookup historical data for " + m, e);
            }
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } catch (SQLException e) {
            throw new SystemException("Can't open connection", e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    private String getSelectType(int type, long begin)
    {
        switch(type)
        {
            case MeasurementConstants.COLL_TYPE_DYNAMIC:
                if (usesMetricUnion(begin))
                    return "AVG(value) AS value, MAX(value) AS peak, " +
                        "MIN(value) AS low, count(*) as points";
                else
                    return "AVG(value) AS value, MAX(maxvalue) AS peak, " +
                        "MIN(minvalue) AS low, count(*) as points";
            case MeasurementConstants.COLL_TYPE_TRENDSUP:
            case MeasurementConstants.COLL_TYPE_STATIC:
                return "MAX(value) AS value, count(*) as points";
            case MeasurementConstants.COLL_TYPE_TRENDSDOWN:
                return "MIN(value) AS value, count(*) as points";
            default:
                throw new IllegalArgumentException(
                    "No collection type specified in historical metric query.");
        }
    }

    private class MeasurementComparator implements Comparator {
        public int compare(Object arg0, Object arg1) {
            if (arg0 == arg1) {
                return 0;
            }
            if (!(arg0 instanceof Measurement) ||
                !(arg1 instanceof Measurement)) {
                throw new ClassCastException();
            }
            Measurement meas0 = (Measurement)arg0,
                        meas1 = (Measurement)arg0;
            return meas0.getId().compareTo(meas1.getId());
        }
    }

    /**
     * Aggregate data across the given metric IDs, returning max, min, avg, and
     * count of number of unique metric IDs
     *
     * @param measurements {@link List} of {@link Measurement}s
     * @param begin The start of the time range
     * @param end The end of the time range
     * @return the An array of aggregate values
     * @ejb:interface-method
     */
    public double[] getAggregateData(final List measurements,
                                     final long begin,
                                     final long end) {
        checkTimeArguments(begin, end);
        long interval = end-begin;
        interval = (interval == 0) ? 1 : interval;
        final List pts = getHistoricalData(measurements, begin, end, interval,
            MeasurementConstants.COLL_TYPE_DYNAMIC, false, PageControl.PAGE_ALL);
        return getAggData(pts);
    }
    
    /**
     * Fetch the list of historical data points, grouped by template, given
     * a begin and end time range.  Does not return an entry for templates
     * with no associated data.
     * PLEASE NOTE:  The {@link MeasurementConstants.IND_LAST_TIME} index in the
     * {@link double[]} part of the returned map does not contain the real last
     * value.  Instead it is an averaged value calculated from the last 1/60 of 
     * the specified time range.  If this becomes an issue the best way I can
     * think of to solve is to pass in a boolean "getRealLastTime" and issue
     * another query to get the last value if this is set.  It is much
     * better than the alternative of always querying the last metric time
     * because not all pages require this value.
     *
     * @param measurements The List of {@link Measurement}s to query
     * @param begin The start of the time range
     * @param end The end of the time range
     * @see org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl#getHistoricalData()
     * @return the {@link Map} of {@link Integer} to {@link double[]} which
     * represents templateId to data points
     * @ejb:interface-method
     */
    public Map getAggregateDataByTemplate(final List measurements,
                                          final long begin,
                                          final long end) {
        // the idea here is to try and match the exact query executed by
        // getHistoricalData() when viewing the metric indicators page.
        // By issuing the same query we are hoping that the db's query
        // cache will optimize performance.
        checkTimeArguments(begin, end);
        final long interval = (end - begin)/60;
        final List availIds = new ArrayList();
        final Map measIdsByTempl = new HashMap();
        setMeasurementObjects(measurements, availIds, measIdsByTempl);
        final Integer[] avIds = (Integer[])availIds.toArray(new Integer[0]);
        final Map rtn =
            getAvailMan().getAggregateDataByTemplate(avIds, begin, end);
        rtn.putAll(getAggDataByTempl(measIdsByTempl, begin, end, interval));
        return rtn;
    }

    private final Map getAggDataByTempl(final Map measIdsByTempl,
                                        final long begin,
                                        final long end,
                                        final long interval) {
        final HashMap rtn = new HashMap(measIdsByTempl.size());
        for (Iterator it=measIdsByTempl.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)it.next();
            final Integer tid = (Integer)entry.getKey();
            final List meas = (List)entry.getValue();
            final List pts = getHistoricalData(meas, begin, end, interval,
                MeasurementConstants.COLL_TYPE_DYNAMIC, false,
                PageControl.PAGE_ALL);
            final double[] aggData = getAggData(pts);
            if (aggData == null) {
                continue;
            }
            rtn.put(tid, aggData);
        }
        return rtn;
    }

    private final void setMeasurementObjects(final List measurements,
                                             final List availIds,
                                             final Map measIdsByTempl) {
        for (Iterator i = measurements.iterator(); i.hasNext(); ) {
            final Measurement m = (Measurement)i.next();
            final MeasurementTemplate t = m.getTemplate();
            if (m.getTemplate().isAvailability()) {
                availIds.add(m.getId());
            } else {
                final Integer tid = t.getId();
                List list;
                if (null == (list = (List)measIdsByTempl.get(tid))) {
                    list = new ArrayList();
                    measIdsByTempl.put(tid, list);
                }
                list.add(m);
            }
        }
    }

    private final double[] getAggData(final List historicalData) {
        if (historicalData.size() == 0) {
            return null;
        }
        double high  = Double.MIN_VALUE;
        double low   = Double.MAX_VALUE;
        double total = 0;
        Double lastVal = null;
        int count = 0;
        long last = Long.MIN_VALUE;
        for (Iterator it=historicalData.iterator(); it.hasNext(); ) {
            final HighLowMetricValue mv = (HighLowMetricValue)it.next();
            low  = Math.min(mv.getLowValue(), low);
            high = Math.max(mv.getHighValue(), high);
            if (mv.getTimestamp() > last) {
                lastVal = new Double(mv.getValue());
            }
            final int c = mv.getCount();
            count = count + c;
            total = (double)((mv.getValue() * c) + total);
        }
        final double[] data = new double[MeasurementConstants.IND_LAST_TIME + 1];
        data[MeasurementConstants.IND_MIN] = low;
        data[MeasurementConstants.IND_AVG] = (double)total/count;
        data[MeasurementConstants.IND_MAX] = high;
        data[MeasurementConstants.IND_CFG_COUNT] = count;
        if (lastVal != null) {
            data[MeasurementConstants.IND_LAST_TIME] = lastVal.doubleValue();
        }
        return data;
    }

    /**
     * Fetch the list of historical data points given
     * a start and stop time range and interval
     *
     * @param measurements The List of Measurements to query
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param interval Interval for the time range
     * @param type Collection type for the metric
     * @param returnMetricNulls Specifies whether intervals with no data should
     * be return as nulls
     * @see org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl#getHistoricalData()
     * @return the list of data points
     * @ejb:interface-method
     */
    public PageList getHistoricalData(List measurements, long begin, long end,
                                      long interval, int type,
                                      boolean returnMetricNulls,
                                      PageControl pc) {
        final List availIds = new ArrayList();
        final List measIds = new ArrayList();
        checkTimeArguments(begin, end, interval);
        interval = (interval == 0) ? 1 : interval;
        // Want to sort in an attempt to make use of db query cache
        final Comparator comparator = new MeasurementComparator();
        // ensure List may be modified
        measurements = new ArrayList(measurements);
        Collections.sort(measurements, comparator);
        for (Iterator i = measurements.iterator(); i.hasNext(); ) {
            Measurement m = (Measurement)i.next();
            if (m.getTemplate().isAvailability()) {
                availIds.add(m.getId());
            } else {
                measIds.add(m.getId());
            }
        }
        final Integer[] avIds = (Integer[])availIds.toArray(new Integer[0]);
        final PageList rtn = getAvailMan().getHistoricalAvailData(
            avIds, begin, end, interval, pc, true);
        final HQDialect dialect = Util.getHQDialect();
        final int maxExprs = (dialect.getMaxExpressions() == -1) ?
            Integer.MAX_VALUE : dialect.getMaxExpressions();
        for (int i=0; i<measIds.size(); i+=maxExprs) {
            final int last = Math.min(i + maxExprs, measIds.size());
            final List sublist = measIds.subList(i, last);
            final Integer[] mids = (Integer[])sublist.toArray(new Integer[0]);
            final PageList pList = getHistData(mids, begin, end, interval, type,
                                               returnMetricNulls, pc);
            merge(rtn, pList);
        }
        return rtn;
    }

    private void merge(PageList master, PageList toMerge) {
        if (master.size() == 0) {
            master.addAll(toMerge);
            return;
        }
        for (int i=0; i<master.size(); i++) {
            if (toMerge.size() < (i+1)) {
                break;
            }
            final HighLowMetricValue val = (HighLowMetricValue)master.get(i);
            final HighLowMetricValue mval = (HighLowMetricValue)toMerge.get(i);
            final int mcount = mval.getCount();
            final int count = val.getCount();
            final int tot = count+mcount;
            final double high = 
                ((val.getHighValue()*count) + (mval.getHighValue()*mcount))/tot;
            val.setHighValue(high);
            final double low = 
                ((val.getLowValue()*count) + (mval.getLowValue()*mcount))/tot;
            val.setLowValue(low);
            final double value = 
                ((val.getValue()*count) + (mval.getValue()*mcount))/tot;
            val.setValue(value);
        }
    }

    private PageList getHistData(Integer[] ids, long start, long finish,
                                 long interval, int type, boolean returnNulls,
                                 PageControl pc) {
        if (ids == null || ids.length < 1) {
            return new PageList();
        }
        final boolean debug = _log.isDebugEnabled();
        // Check the begin and end times
        checkTimeArguments(start, finish, interval);
        final long begin = TimingVoodoo.roundDownTime(start, MINUTE);
        final long end = TimingVoodoo.roundDownTime(finish, MINUTE);
        final long current = System.currentTimeMillis();
        final ArrayList rtn = new ArrayList();
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs   = null;
        try {
            final StopWatch watch = new StopWatch(current);
            final String selectType = getSelectType(type, begin);
            final long wndSize = (interval > 0) ?
                ((end - begin) / interval) : (end - begin);
            final int pagesize = (int) Math.min(
                Math.max(pc.getPagesize(), wndSize),
                60);
            final long intervalWnd = interval * pagesize;
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            stmt = conn.createStatement();
            long endWnd, beginWnd;
            if(pc.isDescending()) {
                endWnd   = end - (pc.getPagenum() * intervalWnd);
                beginWnd = Math.max(begin, endWnd - intervalWnd);
            } else {
                beginWnd = begin + (pc.getPagenum() * intervalWnd); 
                endWnd   = Math.min(end, beginWnd + intervalWnd);
            }
            final String sql = getHistoricalSQL(selectType, begin, end, interval,
                                                beginWnd, endWnd, ids,
                                                pc.isDescending());
            if (debug) watch.markTimeBegin("getHistoricalSQL");
            rs = stmt.executeQuery(sql);
            if (debug) watch.markTimeEnd("getHistoricalSQL");
            List vals = processHistoricalRS(rs);
            rtn.addAll(vals);
            DBUtil.closeResultSet(logCtx, rs);
            if (debug) _log.debug(watch);
            return getPageList(begin, end, interval, rtn, returnNulls, pc);
        } catch (SQLException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    private PageList getPageList(long begin, long end, long interval,
                                 List points, boolean returnNulls,
                                 PageControl pc) {
        List rtn = new ArrayList();
        Iterator it=points.iterator();
        HighLowMetricValue curr = null;
        for (long i=begin; i<end; i+=interval) {
            if (curr == null && !it.hasNext()) {
                if (returnNulls) {
                    rtn.add(new HighLowMetricValue(Double.NaN, i));
                }
                continue;
            }
            if (curr == null) {
                curr = (HighLowMetricValue)it.next();
            }
            final long currTimestamp = curr.getTimestamp();
            if (currTimestamp == i) {
                rtn.add(curr);
                curr = (it.hasNext()) ? (HighLowMetricValue)it.next() : null;
            } else if (returnNulls) {
                rtn.add(new HighLowMetricValue(Double.NaN, i));
            }
        }
        return new PageList(rtn, pc.getPageEntityIndex() + rtn.size());
    }

    private List processHistoricalRS(ResultSet rs) throws SQLException {
        List rtn = new ArrayList();
        boolean tmp = true;
        int peakCol = -1,
            lowCol  = -1;
        try {
            peakCol = rs.findColumn("peak");
            lowCol  = rs.findColumn("low");
        } catch (SQLException e) {
            // peak and low may not exist
            tmp = false;
        }
        final boolean peakLowExist = tmp;
        final int timeCol = rs.findColumn("timestamp"),
                  valCol  = rs.findColumn("value"),
                  ptsCol  = rs.findColumn("points");
        while (rs.next()) {
            final int count = rs.getInt(ptsCol);
            final long timestamp = rs.getLong(timeCol);
            double value = rs.getDouble(valCol);
            if(rs.wasNull()) {
                value = Double.NaN;
            }
            HighLowMetricValue val;
            if (peakLowExist) {
                final double high = rs.getDouble(peakCol),
            	             low  = rs.getDouble(lowCol);
                val = new HighLowMetricValue(value, high, low, timestamp);
            } else {
                val = new HighLowMetricValue(value, timestamp);
            }
            val.setCount(count);
            rtn.add(val);
        }
        return rtn;
    }

    private String getHistoricalSQL(String selectType, long begin, long end,
                                    long interval, long beginWnd, long endWnd,
                                    Integer[] measids, boolean descending) {
        final long wndSize = (interval > 0) ?
            ((endWnd - beginWnd) / interval) : (endWnd - beginWnd);
        final String metricUnion = getDataTable(begin, end, measids);
        // measInStmt is not needed for the dynamic EAM_MEASUREMENT_DATA view,
        // but is needed for the rollup tables
        final String measInStmt = (metricUnion.endsWith(TAB_DATA)) ?
            "" :
            MeasTabManagerUtil.getMeasInStmt(measids, true);
        final StringBuilder sqlbuf = new StringBuilder()
            .append("SELECT begin AS timestamp, ")
            .append(selectType)
            .append(" FROM ")
             .append("(SELECT ").append(beginWnd)
             .append(" + (").append(interval).append(" * i) AS begin FROM ")
             .append(TAB_NUMS)
             .append(" WHERE i < ").append(wndSize).append(") n, ")
            .append(metricUnion)
            .append(" WHERE timestamp BETWEEN begin AND begin + ")
            .append(interval-1).append(" ")
            .append(measInStmt)
            .append(" GROUP BY begin ORDER BY begin");
        if (descending) {
            sqlbuf.append(" DESC");
        }
        return sqlbuf.toString();
    }

    private long getPurgeRaw()
    {
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
     * @ejb:interface-method
     */
    public MetricValue getLastHistoricalData(Measurement m)
    {
        if (m.getTemplate().isAvailability()) {
            return getAvailMan().getLastAvail(m);
        } else {
            return getLastHistData(m);
        }
    }

    private MetricValue getLastHistData(Measurement m) {
        // Check the cache
        MetricDataCache cache = MetricDataCache.getInstance();
        MetricValue mval = cache.get(m.getId(), 0);
        if (mval != null) {
            return mval;
        }
        //Get the data points and add to the ArrayList
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs   = null;

        try {
            conn = DBUtil.getConnByContext(getInitialContext(),
                                           DATASOURCE_NAME);

            final String metricUnion = MeasTabManagerUtil.getUnionStatement(
                8*HOUR, m.getId().intValue());
            StringBuilder sqlBuf = new StringBuilder()
                .append("SELECT timestamp, value FROM ").append(metricUnion)
                .append(", (SELECT MAX(timestamp) AS maxt")
                .append(" FROM ").append(metricUnion).append(") mt ")
                .append("WHERE measurement_id = ").append(m.getId())
                .append(" AND timestamp = maxt");

            stmt = conn.createStatement();
            
            if (_log.isDebugEnabled()) {
                _log.debug("getLastHistoricalData(): " + sqlBuf);
            }
    
            rs = stmt.executeQuery(sqlBuf.toString());

            if (rs.next()) {
                MetricValue mv = getMetricValue(rs);
                cache.add(m.getId(), mv);
                return mv;
            } else {
                // No cached value, nothing in the database
                return null;
            }
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } catch (SQLException e) {
            _log.error("Unable to look up historical data for " + m, e);
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /**
     * Fetch the most recent data point for particular Measurements.
     *
     * @param measurements The List of Measurements to query.  In the list of
     * Measurements null values are allowed as placeholders.
     * @param timestamp Only use data points with collection times greater
     * than the given timestamp.
     * @return A Map of measurement ids to MetricValues.
     * TODO: We should change this method to now allow NULL values.  This is
     * legacy and only used by the Metric viewer and Availabilty Summary
     * portlets.
     * @ejb:interface-method
     */
    public Map getLastDataPoints(List measurements, long timestamp)
    {
        List availIds = new ArrayList();
        List measurementIds = new ArrayList();

        for (Iterator i = measurements.iterator(); i.hasNext(); ) {
            Measurement m = (Measurement)i.next();
            if (m == null) {
                //XXX: See above.
                measurementIds.add(null);
            } else if (m.getTemplate().isAvailability()) {
                availIds.add(m.getId());
            } else {
                measurementIds.add(m.getId());
            }
        }

        Integer[] sepMids =
            (Integer[])measurementIds.toArray(new Integer[measurementIds.size()]);
        Integer[] avIds =
            (Integer[])availIds.toArray(new Integer[availIds.size()]);

        Map data = getLastDataPts(sepMids, timestamp);

        if (availIds.size() > 0)
            data.putAll(getAvailMan().getLastAvail(avIds));
        
        return data;
    }

    private Map getLastDataPts(Integer[] ids, long timestamp)
    {
        final int MAX_ID_LEN = 10;
        // The return map
        Map data = new HashMap();
        if (ids.length == 0)
            return data;
    
        // Try to get the values from the cache first
        ArrayList nodata = getCachedDataPoints(ids, data, timestamp);
    
        if (nodata.size() == 0) {
            return data;
        } else {
            ids = (Integer[]) nodata.toArray(new Integer[nodata.size()]);
        }
        
        Connection conn  = null;
        Statement  stmt  = null;
        StopWatch  timer = new StopWatch();
        
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
    
            int length;
            stmt = conn.createStatement();
            for (int ind = 0; ind < ids.length; )
            {
                length = Math.min(ids.length - ind, MAX_ID_LEN);
                // Create sub array
                Integer[] subids = new Integer[length];
                for (int j = 0; j < subids.length; j++) {
                    subids[j] = ids[ind++];
                    if (_log.isDebugEnabled())
                        _log.debug("arg" + (j+1) + ": " + subids[j]);
                }
                setDataPoints(data, timestamp, subids, stmt);
            }
        } catch (SQLException e) {         
            throw new SystemException("Cannot get last values", e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
            if (_log.isDebugEnabled()) {
                _log.debug("getLastDataPoints(): Statement query elapsed " +
                          "time: " + timer.getElapsed());
            }
        }
        List dataPoints = convertMetricId2MetricValueMapToDataPoints(data);
        updateMetricDataCache(dataPoints);
        return data;
    }

    /**
     * Get data points from cache only
     * 
     * @ejb:interface-method
     */
    public ArrayList getCachedDataPoints(Integer[] ids, Map data,
                                         long timestamp) {
        MetricDataCache cache = MetricDataCache.getInstance();
        ArrayList nodata = new ArrayList();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == null) {
                continue;
            }
    
            MetricValue mval = cache.get(ids[i], timestamp);
            if (mval != null) {
                data.put(ids[i], mval);
            } else {
                nodata.add(ids[i]);
            }
        }
        return nodata;
    }

    private void setDataPoints(Map data, long timestamp,
                               Integer[] measIds, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            StringBuilder sqlBuf = getLastDataPointsSQL(timestamp, measIds);
            if (_log.isTraceEnabled()) {
                _log.trace("getLastDataPoints(): " + sqlBuf);
            }
            rs = stmt.executeQuery(sqlBuf.toString());
            
            MetricDataCache cache = MetricDataCache.getInstance();
            while (rs.next()) {
                Integer mid = new Integer(rs.getInt(1));
                if (!data.containsKey(mid)) {
                    MetricValue mval = getMetricValue(rs);
                    data.put(mid, mval);
                    cache.add(mid, mval);       // Add to cache to avoid lookup
                }
            }
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    private StringBuilder getLastDataPointsSQL(long timestamp, Integer[] measIds)
    {
        String tables = (timestamp != MeasurementConstants.TIMERANGE_UNLIMITED) ?
            MeasTabManagerUtil.getUnionStatement(
                timestamp, System.currentTimeMillis(), measIds)
            : MeasTabManagerUtil.getUnionStatement(getPurgeRaw(), measIds);

        StringBuilder sqlBuf = new StringBuilder(
            "SELECT measurement_id, value, timestamp" +
            " FROM " + tables + ", " +
            "(SELECT measurement_id AS id, MAX(timestamp) AS maxt" +
            " FROM " + tables +
            " WHERE ").
            append(MeasTabManagerUtil.getMeasInStmt(measIds, false));

        if (timestamp != MeasurementConstants.TIMERANGE_UNLIMITED);
            sqlBuf.append(" AND timestamp >= ").append(timestamp);

        sqlBuf.append(" GROUP BY measurement_id) mt")
              .append(" WHERE timestamp = maxt AND measurement_id = id");
        return sqlBuf;
    }

    /**
     * Convert the MetricId->MetricValue map to a list of DataPoints.
     * 
     * @param metricId2MetricValueMap The map to convert.
     * @return The list of DataPoints.
     */
    private List convertMetricId2MetricValueMapToDataPoints(
            Map metricId2MetricValueMap) {
        
        List dataPoints = new ArrayList(metricId2MetricValueMap.size());
        
        for (Iterator i = metricId2MetricValueMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            Integer mid = (Integer)entry.getKey();
            MetricValue mval = (MetricValue)entry.getValue();
            dataPoints.add(new DataPoint(mid, mval));
        }
        
        return dataPoints;
    }

    /**
     * Get a Baseline data.
     *
     * @ejb:interface-method
     */
    public double[] getBaselineData(Measurement meas, long begin, long end) {
        if (meas.getTemplate().getAlias().equalsIgnoreCase("availability")) {
            Integer[] mids = new Integer[1];
            Integer id = meas.getId();
            mids[0] = id;
            return (double[])getAvailMan()
                .getAggregateData(mids, begin, end)
                .get(id);
        } else {
            return getBaselineMeasData(meas, begin, end);
        }
    }

    private double[] getBaselineMeasData(Measurement meas, long begin, long end) {
        // Check the begin and end times
        super.checkTimeArguments(begin, end);
        Integer id = meas.getId();
        
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs = null;

        // The table to query from
        String table = getDataTable(begin, end, id.intValue());
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            StringBuilder sqlBuf = new StringBuilder(
                "SELECT MIN(value), AVG(value), MAX(value) FROM ")
                .append(table)
                .append(" WHERE timestamp BETWEEN ").append(begin)
                .append(" AND ").append(end)
                .append(" AND measurement_id = ").append(id.intValue());

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sqlBuf.toString());

            rs.next();          // Better have some result
            double[] data = new double[MeasurementConstants.IND_MAX + 1];
            data[MeasurementConstants.IND_MIN] = rs.getDouble(1);
            data[MeasurementConstants.IND_AVG] = rs.getDouble(2);
            data[MeasurementConstants.IND_MAX] = rs.getDouble(3);
                
            return data;
        } catch (SQLException e) {
            throw new MeasurementDataSourceException
                ("Can't get baseline data for: " + id, e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /**
     * Fetch a map of aggregate data values keyed by metrics given
     * a start and stop time range
     *
     * @param tids The template id's of the Measurement
     * @param iids The instance id's of the Measurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param useAggressiveRollup uses a measurement rollup table to fetch the
     *      data if the time range spans more than one data table's max timerange
     * @return the Map of data points
     * @ejb:interface-method
     */
    public Map getAggregateDataByMetric(Integer[] tids, Integer[] iids,
                                        long begin, long end,
                                        boolean useAggressiveRollup) {
        checkTimeArguments(begin, end);
        Map rtn =
            getAggDataByMetric(tids, iids, begin, end, useAggressiveRollup);
        Collection metrics =
            getMeasurementDAO().findAvailMeasurements(tids, iids);
        if (metrics.size() > 0) {
            Integer[] mids = new Integer[metrics.size()];
            Iterator it = metrics.iterator();
            for (int i = 0; i < mids.length; i++) {
                Measurement m = (Measurement) it.next();
                mids[i] = m.getId();
            }
            rtn.putAll(getAvailMan().getAggregateData(mids, begin, end));
        }
        return rtn;
    }

    private Map getAggDataByMetric(Integer[] tids, Integer[] iids,
                                   long begin, long end,
                                   boolean useAggressiveRollup) {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);
    
        // Result set
        HashMap resMap = new HashMap();
    
        if (tids.length == 0 || iids.length == 0)
            return resMap;
        
        //Get the data points and add to the ArrayList
        Connection conn        = null;
        PreparedStatement stmt = null;
        ResultSet rs           = null;
        StopWatch timer        = new StopWatch();

        StringBuffer iconj = new StringBuffer(
            DBUtil.composeConjunctions("instance_id", iids.length));

        DBUtil.replacePlaceHolders(iconj, iids);
        StringBuilder tconj = new StringBuilder(
            DBUtil.composeConjunctions("template_id", tids.length));

        try {
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // The table to query from
            List measids = MeasTabManagerUtil.getMeasIds(conn, tids, iids);
            String table = getDataTable(begin, end,
                (Integer[])measids.toArray(new Integer[0]), useAggressiveRollup);
            // Use the already calculated min, max and average on
            // compressed tables.
            String minMax;
            if (usesMetricUnion(begin, end, useAggressiveRollup)) {
                minMax = " MIN(value), AVG(value), MAX(value) ";
            } else {
                minMax = " MIN(minvalue), AVG(value), MAX(maxvalue) ";
            }
            final String aggregateSQL =
                "SELECT id, " + minMax + 
                " FROM " + table + "," + TAB_MEAS +
                " WHERE timestamp BETWEEN ? AND ? AND measurement_id = id " +
                " AND " + iconj + " AND " + tconj + " GROUP BY id";
            if (_log.isTraceEnabled()) {
                _log.trace("getAggregateDataByMetric(): " + aggregateSQL);
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
                DBUtil.closeResultSet(logCtx, rs);
            }
            if (_log.isTraceEnabled()) {
                _log.trace("getAggregateDataByMetric(): Statement query elapsed "
                          + "time: " + timer.getElapsed());
            }
            return resMap;
        } catch (SQLException e) {
            _log.debug("getAggregateDataByMetric()", e);
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
        }
    }

    /**
     * Fetch a map of aggregate data values keyed by metrics given
     * a start and stop time range
     *
     * @param measurements The id's of the Measurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param useAggressiveRollup uses a measurement rollup table to fetch the 
     *      data if the time range spans more than one data table's max timerange
     * @return the map of data points
     * @ejb:interface-method
     */
    public Map getAggregateDataByMetric(List measurements, long begin,
                                        long end, boolean useAggressiveRollup)
    {
        checkTimeArguments(begin, end);
        List avids = new ArrayList();
        List mids = new ArrayList();
        for (Iterator i=measurements.iterator(); i.hasNext(); ) {
            Measurement meas = (Measurement)i.next();
            MeasurementTemplate t = meas.getTemplate();
            if (t.isAvailability()) {
                avids.add(meas.getId());
            } else {
                mids.add(meas.getId());
            }
        }
        Map rtn = getAggDataByMetric(
            (Integer[]) mids.toArray(new Integer[0]), begin, end,
            useAggressiveRollup);
        rtn.putAll(getAvailMan().getAggregateData(
            (Integer[]) avids.toArray(new Integer[0]), begin, end));
        return rtn;
    }

    private AvailabilityManagerLocal getAvailMan() {
        return AvailabilityManagerEJBImpl.getOne();
    }

    private Map getAggDataByMetric(Integer[] mids, long begin,
                                   long end, boolean useAggressiveRollup)
    {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        // The table to query from
        String table = getDataTable(begin, end, mids, useAggressiveRollup);
        // Result set
        HashMap resMap = new HashMap();
    
        if (mids.length == 0)
            return resMap;
        
        //Get the data points and add to the ArrayList
        Connection conn        = null;
        PreparedStatement stmt = null;
        ResultSet rs           = null;
        StopWatch timer        = new StopWatch();
        
        StringBuffer mconj = new StringBuffer(
            DBUtil.composeConjunctions("measurement_id", mids.length));
        DBUtil.replacePlaceHolders(mconj, mids);

        // Use the already calculated min, max and average on
        // compressed tables.
        String minMax;
        if (usesMetricUnion(begin, end, useAggressiveRollup)) {
            minMax = " MIN(value), AVG(value), MAX(value), ";
        } else {
            minMax = " MIN(minvalue), AVG(value), MAX(maxvalue), ";
        }

        final String aggregateSQL =
            "SELECT measurement_id, " + minMax + " count(*) " +
            " FROM " + table +
            " WHERE timestamp BETWEEN ? AND ? AND " + mconj +
            " GROUP BY measurement_id";
        
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            if (_log.isTraceEnabled())
                _log.trace("getAggregateDataByMetric(): " + aggregateSQL);
    
            stmt = conn.prepareStatement(aggregateSQL);
            
            int i = 1;
            stmt.setLong(i++, begin);
            stmt.setLong(i++, end);
    
            try {
                rs = stmt.executeQuery();
    
                while (rs.next())
                {
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
                DBUtil.closeResultSet(logCtx, rs);
            }
    
            if (_log.isTraceEnabled())
                _log.trace("getAggregateDataByMetric(): Statement query elapsed "
                          + "time: " + timer.getElapsed());
    
            return resMap;
        } catch (SQLException e) {
            _log.debug("getAggregateDataByMetric()", e);
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
        }
    }

    public static DataManagerLocal getOne() { 
        try {
            return DataManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {
        boolean analyze = true;
        try {
            Properties conf = ServerConfigManagerEJBImpl.getOne().getConfig();
            if (conf.containsKey(HQConstants.OOBEnabled)) {
                analyze = Boolean.valueOf(
                    conf.getProperty(HQConstants.OOBEnabled)).booleanValue();
            }
        } catch (Exception e) {
            _log.debug("Error looking up server configs", e);
        } finally {
            if (analyze) {
                analyzer = (Analyzer) ProductProperties
                    .getPropertyInstance("hyperic.hq.measurement.analyzer");    
            }
        }
    }

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}
