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

package org.hyperic.hq.measurement.server.session;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
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
import org.hyperic.hq.measurement.data.AggregateObjectMeasurementValue;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.data.MeasurementDataSourceException;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerUtil;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.hq.measurement.shared.MeasRangeObj;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.StringUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;

/** The DataManagerEJB class is a stateless session bean that can be
 *  used to retrieve measurement data points
 *
 * @ejb:bean name="DataManager"
 *      jndi-name="ejb/measurement/DataManager"
 *      local-jndi-name="LocalDataManager"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class DataManagerEJBImpl extends SessionEJB implements SessionBean {
    private static final String logCtx = DataManagerEJBImpl.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);
    private Connection _conn;
    
    private static final BigDecimal MAX_DB_NUMBER =
        new BigDecimal("10000000000000000000000");

    private static final long MINUTE = 60 * 1000;
    
    // Table names
    private static final String TAB_DATA    = MeasurementConstants.TAB_DATA;
    private static final String TAB_DATA_1H = MeasurementConstants.TAB_DATA_1H;
    private static final String TAB_DATA_6H = MeasurementConstants.TAB_DATA_6H;
    private static final String TAB_DATA_1D = MeasurementConstants.TAB_DATA_1D;
    private static final String TAB_MEAS    = MeasurementConstants.TAB_MEAS;
    private static final String TAB_NUMS    = "EAM_NUMBERS";
    
    // Error strings
    private static final String ERR_DB    = "Cannot look up database instance";
    private static final String ERR_INTERVAL =
        "Interval cannot be larger than the time range";
    
    // Save some typing
    private static final int IND_MIN       = MeasurementConstants.IND_MIN;
    private static final int IND_AVG       = MeasurementConstants.IND_AVG;
    private static final int IND_MAX       = MeasurementConstants.IND_MAX;
    private static final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;
    private static final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;
    
    private SessionContext _ctx;
    
    // Pager class name
    private boolean confDefaultsLoaded = false;

    // Purge intervals, loaded once on first invocation.
    private long purgeRaw, purge1h, purge6h;

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
    
    private void mergeMetricValues(HighLowMetricValue existing,
                                   HighLowMetricValue additional) {
        if (Double.isNaN(additional.getValue()))
            return;
        
        if (Double.isNaN(existing.getValue())) {
            existing.setHighValue(additional.getHighValue());
            existing.setLowValue(additional.getLowValue());
            existing.setValue(additional.getValue());
            existing.setCount(additional.getCount());
            return;
        }
        
        existing.setHighValue(Math.max(existing.getHighValue(),
                                       additional.getHighValue()));
        existing.setLowValue(Math.min(existing.getLowValue(),
                                      additional.getLowValue()));
        
        // Average the two values
        double total = existing.getCount() + additional.getCount();
        existing.setValue(existing.getValue() / total * existing.getCount() +
                        additional.getValue() / total * additional.getCount());
        existing.setCount((int) total);
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
     * @ejb:interface-method
     */
    public void addData(Integer mid, MetricValue dp, boolean overwrite) {
        List pts = new ArrayList(1);
        pts.add(new DataPoint(mid, dp));

        addData(pts, overwrite);
    }
    
    /**
     * Write metric data points to the DB with transaction
     * 
     * @param data       a list of {@link DataPoint}s 
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public boolean addData(List data) {
        if (shouldAbortDataInsertion(data)) {
            return true;
        }

        data = enforceUnmodifiable(data);

        _log.debug("Attempting to insert data in a single transaction.");

        HQDialect dialect = Util.getHQDialect();
        boolean succeeded = false;

        _conn = safeGetConnection();
        if (_conn == null) {
            // We are in a bad state. Set txn rollback in case this txn was 
            // initiated by the client.
            _ctx.setRollbackOnly();
            return false;
        }

        boolean autocommit = false;
        try
        {
            autocommit = _conn.getAutoCommit();
            _conn.setAutoCommit(false);
            if (dialect.supportsMultiInsertStmt()) {
                succeeded = insertDataWithOneInsert(data, _conn);
            } else {
                succeeded = insertDataInBatch(data, _conn);
            }            

            if (succeeded) {
                _log.debug("Inserting data in a single transaction succeeded.");
                sendMetricEvents(data);
            } else {
                _log.debug("Inserting data in a single transaction failed." +
                           "  Rolling back transaction.");
                _conn.rollback();
                addData(data, true);
            }
            _conn.commit();
        }
        catch (SQLException e) {
            _log.debug("Rollback failed");
        }
        finally {
            try {
                if (_conn != null && !_conn.isClosed())
                    _conn.setAutoCommit(autocommit);
            } catch (SQLException e) {
                _log.debug("Error executing Connection method.", e);
            }
            DBUtil.closeConnection(logCtx, _conn);
        }

        return succeeded;        
    }

    /**
     * Write metric datapoints to the DB without transaction
     * 
     * @param data       a list of {@link DataPoint}s 
     * @param overwrite  If true, attempt to over-write values when an insert
     *                   of the data fails (i.e. it already exists)
     *                   XXX:  Why would you ever not want to overwrite?
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
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
        Set failedToSaveMetrics = new HashSet();
        List left = data;
        
        Connection conn = (_conn == null) ? safeGetConnection() : _conn;
        
        if (conn == null) {
            _log.debug("Inserting/Updating data outside a transaction failed.");
            return;
        }
        
        try {
            while (true && !left.isEmpty()) {
                int numLeft = left.size();
                _log.debug("Attempting to insert " + numLeft + " points");
                
                try {
                    left = insertData(conn, left, true);
                } catch (SQLException e) {
                    assert false : "The SQLException should not happen: "+e;
                }
                                
                _log.debug("Num left = " + left.size());
                
                if (left.isEmpty())
                    break;
                
                if (!overwrite) {
                    _log.debug("We are not updating the remaining "+
                                left.size()+" points");
                    failedToSaveMetrics.addAll(left);
                    break;
                }
                                    
                // The insert couldn't insert everything, so attempt to update
                // the things that are left
                _log.debug("Sending " + left.size() + " data points to update");
                                
                left = updateData(conn, left);                    
                
                if (left.isEmpty())
                    break;

                _log.debug("Update left " + left.size() + " points to process");
                
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
        } finally {
        }
        
        _log.debug("Inserting/Updating data outside a transaction finished.");
        
        sendMetricEvents(removeMetricsFromList(data, failedToSaveMetrics));
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
    private BigDecimal getDecimalInRange(BigDecimal val) {
        val = val.setScale(5, BigDecimal.ROUND_HALF_EVEN);
        if (val.compareTo(MAX_DB_NUMBER) == 1) {
            _log.warn("Value [" + val + "] is too big to put into the DB.  Truncating to [" + 
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
        analyzeMetricData(data);
        
        List cachedData = updateMetricDataCache(data);
        
        sendDataToEventHandlers(cachedData);        
    }  
    
    private void analyzeMetricData(List data) {
        if (analyzer != null) {
            for (Iterator i = data.iterator(); i.hasNext();) {
                DataPoint dp = (DataPoint) i.next();
                analyzer.analyzeMetricValue(dp.getMetricId(), dp.getMetricValue());
            }
        }
    }
    
    private List updateMetricDataCache(List data) {
        MetricDataCache cache = MetricDataCache.getInstance();
        return cache.bulkAdd(data);
    }
    
    private void sendDataToEventHandlers(List data) {
        ArrayList events  = new ArrayList();
        List zevents = new ArrayList();
        
        for (Iterator i = data.iterator(); i.hasNext();) {
            DataPoint dp = (DataPoint) i.next();
            Integer metricId = dp.getMetricId();
            MetricValue val = dp.getMetricValue();
            MeasurementEvent event = new MeasurementEvent(metricId, val);

            if (RegisteredTriggers.isTriggerInterested(event))
                events.add(event);

            zevents.add(new MeasurementZevent(metricId.intValue(), val));
        }
        
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
    
    private List getRemainingDataPoints(List data, int[] execInfo) {
        List res = new ArrayList();
        int idx = 0;

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

                StringBuffer values = new StringBuffer();
                int rowsToUpdate = 0;
                for (Iterator i=dpts.iterator(); i.hasNext(); ) {
                    DataPoint pt = (DataPoint)i.next();
                    Integer metricId  = pt.getMetricId();
                    MetricValue val   = pt.getMetricValue();
                    BigDecimal bigDec;
                    try {
                        bigDec = new BigDecimal(val.getValue());
                    } catch(NumberFormatException e) {  // infinite, or NaN
                        _log.warn("Unable to insert infinite or NaN for " +
                                  "metric id=" + metricId);
                        continue;
                    }
                    rowsToUpdate++;
                    values.append("(").append(val.getTimestamp()).append(", ")
                          .append(metricId.intValue()).append(", ")
                          .append(getDecimalInRange(bigDec)+"),");
                }
                String sql = "insert into "+table+" (timestamp, measurement_id, "+
                             "value) values "+values.substring(0, values.length()-1);
                stmt = conn.createStatement();
                int rows = stmt.executeUpdate(sql);
                _log.debug("Inserted "+rows+" rows into "+table+
                           " (attempted "+rowsToUpdate+" rows)");
                if (rows < rowsToUpdate)
                    return false;
            }   
        } catch (SQLException e) {
            // If there is a SQLException, then none of the data points 
            // should be inserted. Roll back the txn.
            _log.debug("Error while inserting data with one insert", e);
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
            // Oracle does not handle the batch insert in single transaction
            // so return right away
            if (DBUtil.isOracle(conn))
                return false;
            
            _log.debug("Attempting to insert " + left.size() + " points");
            left = insertData(conn, left, false);            
            _log.debug("Num left = " + left.size());
            
            if (!left.isEmpty()) {
                _log.debug("Need to update " + left.size() + " data points.");
                
                if (_log.isDebugEnabled())
                    _log.debug("Data points to update: " + left);                
                
                return false;
            }
        } catch (SQLException e) {
            // If there is a SQLException, then none of the data points 
            // should be inserted. Roll back the txn.
            _log.debug("Error while inserting data in batch", e);
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
        List left = new ArrayList();
        Map buckets = MeasRangeObj.getInstance().bucketData(data);
        
        for (Iterator it = buckets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String table = (String) entry.getKey();
            List dpts = (List) entry.getValue();

            try {

                stmt = conn.prepareStatement(
                    "INSERT /*+ APPEND */ INTO " + table + 
                    " (measurement_id, timestamp, value) VALUES (?, ?, ?)");

                for (Iterator i=dpts.iterator(); i.hasNext(); ) {
                    DataPoint pt = (DataPoint)i.next();
                    Integer metricId  = pt.getMetricId();
                    MetricValue val   = pt.getMetricValue();
                    BigDecimal bigDec;

                    try {
                        bigDec = new BigDecimal(val.getValue());
                    } catch(NumberFormatException e) {  // infinite, or NaN
                        _log.warn("Unable to insert infinite or NaN for " +
                                  "metric id=" + metricId);
                        continue;
                    }
                    stmt.setInt(1, metricId.intValue());
                    stmt.setLong(2, val.getTimestamp());
                    stmt.setBigDecimal(3, getDecimalInRange(bigDec));
                    stmt.addBatch();
                }

                int[] execInfo = stmt.executeBatch();
                left.addAll(getRemainingDataPoints(dpts, execInfo));
            } catch (BatchUpdateException e) {
                if (!continueOnSQLException) {
                    throw e;
                }
                
                left.addAll(
                    getRemainingDataPointsAfterBatchFail(dpts, 
                                                         e.getUpdateCounts()));
            } catch (SQLException e) {
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
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }            
        }

        return left;
    }

    private boolean dataPtExists(PreparedStatement stmt, int metricId,
                                 long timestamp)
    {
        ResultSet rs = null;
        try
        {
            stmt.setLong(1, timestamp);
            stmt.setInt(2, metricId);
            rs = stmt.executeQuery();
            if (rs.next())
                return true;
        }
        catch (SQLException e) {
            _log.debug("A general SQLException occurred during the check.", e);
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
        return false;
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

                    try {
                        bigDec = new BigDecimal(val.getValue());
                    } catch(NumberFormatException e) {  // infinite, or NaN
                        _log.warn("Unable to update infinite or NaN for " +
                                  "metric id=" + metricId);
                        continue;
                    }
                    stmt.setBigDecimal(1, getDecimalInRange(bigDec));
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

    /**
     * Based on the given start time, determine which measurement 
     * table we should query for measurement data.  If the slice is for the
     * detailed data segment, we return the UNION view of the required time
     * slices.
     * @param begin The beginning of the time range.
     * @param end The end of the time range
     */
    private String getDataTable(long begin, long end)
    {
        Integer[] empty = new Integer[0];
        return getDataTable(begin,end,empty);
    }

    private String getDataTable(long begin, long end, int measId)
    {
        Integer[] empty = new Integer[1];
        empty[0] = new Integer(measId);
        return getDataTable(begin,end,empty);
    }

    private boolean usesMetricUnion(long begin, long end)
    {
        long now = System.currentTimeMillis();
        if (MeasTabManagerUtil.getMeasTabStartTime(now - getPurgeRaw()) < begin)
            return true;
        return false;
    }

    private String getDataTable(long begin, long end, Object[] measIds)
    {
        long now = System.currentTimeMillis();

        if (!confDefaultsLoaded)
            loadConfigDefaults();

        if (usesMetricUnion(begin, end)) {
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
     * Fetch the list of historical data points given
     * a start and stop time range
     *
     * @param id the id of the Derived Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the list of data points
     * @ejb:interface-method
     */
    public PageList getHistoricalData(Integer id, long begin, long end,
                                      PageControl pc)
        throws DataNotAvailableException {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        // Push begin to at least current - 1 min, avoid row contention with
        // current writes
        long current = System.currentTimeMillis();
        begin = Math.min(begin, current - 60000);

        ArrayList history = new ArrayList();

        //Get the data points and add to the ArrayList
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;

        // The table to query from
        String table = getDataTable(begin, end, id.intValue());
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            int total =
                getMeasTableCount(conn, begin, end, id.intValue(), table);
            if (total == 0)
                return new PageList();

            try {
                // The index
                int i = 1;

                StringBuffer sqlBuf;
                // Now get the page that user wants
                boolean sizeLimit =
                    pc.getPagesize() != PageControl.SIZE_UNLIMITED;

                if (sizeLimit) {
                    // This query dynamically counts the number of rows to 
                    // return the correct paged ResultSet
                    sqlBuf = new StringBuffer(
                        "SELECT timestamp, value FROM " + table + " d1 " +
                        "WHERE measurement_id = ? AND" +
                             " timestamp BETWEEN ? AND ? AND" +
                             " ? <= (SELECT count(*) FROM " + table + " d2 "+
                               "WHERE d1.measurement_id = d2.measurement_id " +
                                     "AND d2.timestamp ")
                        .append(pc.isAscending() ? "<" : ">")
                        .append(" d1.timestamp) ORDER BY timestamp ")
                        .append(pc.isAscending() ? "" : "DESC");
                }
                else {
                    sqlBuf = new StringBuffer(
                        "SELECT value, timestamp FROM " + table +
                        " WHERE measurement_id=? AND " +
                              " timestamp BETWEEN ? AND ? ORDER BY timestamp ")
                        .append(pc.isAscending() ? "" : "DESC");
                }

                stmt = conn.prepareStatement(sqlBuf.toString());
                stmt.setInt(i++, id.intValue());
                stmt.setLong(i++, begin);
                stmt.setLong(i++, end - 1);

                if ( _log.isDebugEnabled() ) {
                    _log.debug("getHistoricalData(): " + sqlBuf);
                    _log.debug("arg1 = " + id);
                    _log.debug("arg2 = " + begin);
                    _log.debug("arg3 = " + end);
                }

                if (sizeLimit) {
                    stmt.setInt(i++, pc.getPageEntityIndex() + 1);
                    if ( _log.isDebugEnabled() )
                        _log.debug("arg4 = " + (pc.getPageEntityIndex() + 1));
                }

                StopWatch timer = new StopWatch(current);

                rs = stmt.executeQuery();

                if ( _log.isTraceEnabled() ) {
                    _log.trace("getHistoricalData() execute time: " +
                              timer.getElapsed());
                }

                for (i = 1; rs.next(); i++) {
                    history.add(getMetricValue(rs));

                    if (sizeLimit && (i == pc.getPagesize()))
                        break;
                }

                // Now return a PageList
                return new PageList(history, total);
            } catch (SQLException e) {
                throw new DataNotAvailableException(
                    "Can't lookup historical data for " + id, e);
            } finally {
                DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            }
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't open connection", e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    private int getMeasTableCount(Connection conn, long begin, long end,
                                  int measurementId, String measView)
        throws DataNotAvailableException
    {
        Statement stmt = null;
        ResultSet rs   = null;
        int total = 0;
        String sql;
        try {
            stmt = conn.createStatement();
            sql =  "SELECT count(*) FROM " + measView +
                   " WHERE timestamp BETWEEN "+begin+" AND "+end+
                   " AND measurement_id="+measurementId;
            rs = stmt.executeQuery(sql);
            if (rs.next())
                total = rs.getInt(1);
            if (total == 0) {
                return 0;
            }
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't count historical data for " + measurementId, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
        }
        return 0;
    }

    private String getSelectType(int type, long begin, long end)
    {
        switch(type)
        {
            case MeasurementConstants.COLL_TYPE_DYNAMIC:
                if (usesMetricUnion(begin, end))
                    return "AVG(value) AS value, " +
                           "MAX(value) AS peak, MIN(value) AS low";
                else
                    return "AVG(value) AS value, " +
                           "MAX(maxvalue) AS peak, MIN(minvalue) AS low";
            case MeasurementConstants.COLL_TYPE_TRENDSUP:
            case MeasurementConstants.COLL_TYPE_STATIC:
                return "MAX(value) AS value";
            case MeasurementConstants.COLL_TYPE_TRENDSDOWN:
                return "MIN(value) AS value";
            default:
                throw new IllegalArgumentException(
                    "No collection type specified in historical metric query.");
        }
    }

    /**
     * Fetch the list of historical data points given
     * a start and stop time range and interval
     *
     * @param ids The id's of the DerivedMeasurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @param interval Interval for the time range
     * @param type Collection type for the metric
     * @param returnNulls Specifies whether intervals with no data should be return as nulls
     * @return the list of data points
     * @ejb:interface-method
     */
    public PageList getHistoricalData(Integer[] ids, long begin, long end,
                                      long interval, int type,
                                      boolean returnNulls, PageControl pc)
        throws DataNotAvailableException {
        final int    MAX_IDS  = 30;
        final String REPL_IDS = "@@REPL_IDS@@";
        
        if (ids == null || ids.length < 1) {
            throw new DataNotAvailableException("No IDs were passed");
        }
        
        // Always return NULLs if there are more IDs than we can handle
        if (ids.length > MAX_IDS)
            returnNulls = true;
        
        // Check the begin and end times
        this.checkTimeArguments(begin, end, interval);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);
        
        // Push begin to at least current - 1 min, avoid row contention with
        // current writes
        long current = System.currentTimeMillis();
        begin = Math.min(begin, current - 60000);

        ArrayList history = new ArrayList();
    
        //Get the data points and add to the ArrayList
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs   = null;

        try {
            StopWatch timer = new StopWatch(current);
            
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            stmt = conn.createStatement();
    
            if(_log.isDebugEnabled())
            {
                _log.debug("GetHistoricalData: ID: " +
                          StringUtil.arrayToString(ids) + ", Begin: " +
                          TimeUtil.toString(begin) + ", End: " +
                          TimeUtil.toString(end) + ", Interval: " + interval +
                          '(' + (interval / 60000) + "m " + (interval % 60000) +
                          "s)" );
            }

            // Construct SQL command
            String selectType;

            // The table to query from
            String table = getDataTable(begin, end, ids);
            selectType = getSelectType(type, begin, end);

            final int pagesize =
                (int) Math.min(Math.max(pc.getPagesize(),
                                        (end - begin) / interval), 60);
            final long intervalWnd = interval * pagesize;

            int idsCnt = 0;
            
            for (int index = 0; index < ids.length; index += idsCnt)
            {
                if (idsCnt != Math.min(MAX_IDS, ids.length - index)) {
                    idsCnt = Math.min(MAX_IDS, ids.length - index);
                }

                long beginTrack = begin;
                do
                {
                    // Adjust the begin and end to query only the rows for the
                    // specified page.
                    long beginWnd;
                    long endWnd;

                    if(_log.isDebugEnabled())
                        _log.debug(pc.toString());
                    
                    if(pc.isDescending()) {
                        endWnd   = end - (pc.getPagenum() * intervalWnd);
                        beginWnd = Math.max(beginTrack, endWnd - intervalWnd);
                    } else {
                        beginWnd = beginTrack + (pc.getPagenum() * intervalWnd); 
                        endWnd   = Math.min(end, beginWnd + intervalWnd);
                    }

                    int ind = index;
                    int endIdx = ind + idsCnt;
                    Integer[] measids = new Integer[idsCnt];
                    int i=0;
                    for (; ind < endIdx; ind++) {
                        if (_log.isDebugEnabled())
                            _log.debug("arg " + i + " = " + ids[ind]);

                        measids[i++] = ids[ind];
                    }

                    String sql;
                    sql = getHistoricalSQL(selectType, begin, end, interval,
                                           beginWnd, endWnd, measids,
                                           pc.isDescending());
                    if(_log.isDebugEnabled())
                    {
                        _log.debug(
                            "Page Window: Begin: " + TimeUtil.toString(beginWnd)
                            + ", End: " + TimeUtil.toString(endWnd) );
                        _log.debug("SQL Command: " + sql);
                        _log.debug("arg 1 = " + beginWnd);
                        _log.debug("arg 2 = " + interval);
                        _log.debug("arg 3 = " + (endWnd - beginWnd) / interval);
                        _log.debug("arg 4 = " + interval);
                    }
                    rs = stmt.executeQuery(sql);
                    
                    long curTime = beginWnd;
                    
                    Iterator it = null;
                    if (history.size() > 0)
                        it = history.iterator();
                    
                    for (int row = 0; row < pagesize; row++)
                    {
                        long fillEnd = curTime;
                        
                        if (rs.next()) {
                            fillEnd = rs.getLong("timestamp");
                        }
                        else if (returnNulls) {
                            fillEnd = endWnd;
                        }
                        else {
                            break;
                        }
                        
                        if (returnNulls) {
                            for (; curTime < fillEnd; row++) {
                                if (it != null) {
                                    it.next();
                                }
                                else
                                    history.add(
                                        new HighLowMetricValue(Double.NaN,
                                                               curTime));
                                curTime += interval;
                            }
                        }
                        
                        if (row < pagesize) {
                            HighLowMetricValue val = this.getMetricValue(rs);
                            if (returnNulls || !Double.isNaN(val.getValue())) {
                                val.setCount(idsCnt);
                                
                                if (it != null) {
                                    HighLowMetricValue existing =
                                        (HighLowMetricValue) it.next();
                                    mergeMetricValues(existing, val);
                                }
                                else
                                    history.add(val);
                            }
                            curTime = val.getTimestamp() + interval;
                        }
                    }
                    
                    if (_log.isDebugEnabled()) {
                        _log.debug("getHistoricalData() for " + ids.length +
                                  " metric IDS: " +
                                  StringUtil.arrayToString(ids));
                    }

                    DBUtil.closeResultSet(logCtx, rs);

                    // If there was no result loop back, until we hit the end of
                    // the time range. Otherwise, break out of the loop.
                    if(history.size() >= pagesize)
                        break;
                        
                    // Move foward a page
                    pc.setPagenum(pc.getPagenum() + 1);
                    beginTrack += intervalWnd;
                } while(beginTrack < end);

                if(_log.isDebugEnabled()) {
                    _log.debug("GetHistoricalData: ElapsedTime: " + timer +
                              " seconds");
                }
            }

            // Now return a PageList
            return new PageList(history,
                                pc.getPageEntityIndex() + history.size());
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't lookup historical data for " +
                StringUtil.arrayToString(ids), e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    private String getHistoricalSQL(String selectType, long begin, long end,
                                    long interval, long beginWnd, long endWnd,
                                    Integer[] measids, boolean descending)
    {
        String metricUnion = getDataTable(begin, end, measids),
               measInStmt = MeasTabManagerUtil.getMeasInStmt(measids, true);
        StringBuffer sqlbuf = new StringBuffer()
            .append("SELECT begin AS timestamp, ")
            .append(selectType)
            .append(" FROM ")
             .append("(SELECT ").append(beginWnd)
             .append(" + (").append(interval).append(" * i) AS begin FROM ")
             .append(TAB_NUMS)
             .append(" WHERE i < ").append( ((endWnd - beginWnd) / interval) )
             .append(") n, ")
            .append(metricUnion)
            .append(" WHERE timestamp BETWEEN begin AND begin + ")
            .append(interval-1).append(" ")
            .append(measInStmt)
            .append(" GROUP BY begin ORDER BY begin");

        if (descending)
            sqlbuf.append(" DESC");

        return sqlbuf.toString();
    }

    private long getPurgeRaw()
    {
        if (!confDefaultsLoaded)
            loadConfigDefaults();
        return purgeRaw;
    }

    /**
     *
     * Fetch a list of historical data points of a specific size Note: There is
     * no guarantee that the list will be the size requested. It may be smaller.
     * 
     * @param id The id of the DerivedMeasurement
     * @param count The number of data points to return
     * @return the list of data points
     * @ejb:interface-method
     */
    public List getLastHistoricalData(Integer id, int count)
        throws DataNotAvailableException {
        // Make sure that client hasn't asked for more than we can provide
        if (count > 60)
            throw new DataNotAvailableException(
                "Cannot ask for more than 60 data points");
        
        ArrayList history = new ArrayList();
        
        if (count == 1) {
            // Check the cache
            MetricDataCache cache = MetricDataCache.getInstance();
            MetricValue mval = cache.get(id, 0);
            if (mval != null) {
                history.add(mval);
                return history;
            }
        }
        
        //Get the data points and add to the ArrayList
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs   = null;

        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
    
            /*
             * SELECT timestamp, value FROM EAM_MEASUREMENT_DATA, EAM_NUMBERS,
             * (SELECT id, interval, MAX(timestamp) AS maxt FROM
             * EAM_MEASUREMENT_data, EAM_MEASUREMENT WHERE measurement_id = id
             * AND id=10012 GROUP BY id,interval) mt WHERE MEASUREMENT_ID = id
             * AND TIMESTAMP = (maxt - i * interval);
             */
            String metricUnion = 
                MeasTabManagerUtil.getUnionStatement(getPurgeRaw(), id.intValue());
            StringBuffer sqlBuf = new StringBuffer(
                "SELECT timestamp, value FROM " + metricUnion +
                                              ", " + TAB_NUMS + ", " +
                    "(SELECT id, interval, MAX(timestamp) AS maxt" +
                    " FROM " + metricUnion + ", "+TAB_MEAS+
                    " WHERE measurement_id = id AND id = " + id +
                    " GROUP BY id, interval) mt " +
                "WHERE measurement_id = id AND" +
                    " timestamp = (maxt - i * interval)");

            stmt = conn.createStatement();
            
            if (_log.isDebugEnabled()) {
                _log.debug("getLastHistoricalData(): " + sqlBuf);
            }
    
            rs = stmt.executeQuery(sqlBuf.toString());
    
            for (int i = 0; rs.next() && i < count; i++) {
                history.add(getMetricValue(rs));
            }
    
            // Now return a PageList
            return history;
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't lookup historical data for " + id, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /**
     * Fetch an array of timestamps for which there is missing data
     *
     * @param id the id of the Derived Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the list of data points
     * @ejb:interface-method
     */
    public long[] getMissingDataTimestamps(Integer id, long interval,
                                           long begin, long end)
        throws DataNotAvailableException {
        this.checkTimeArguments(begin, end);
        
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs   = null;
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            
            stmt = conn.createStatement();
            // First, figure out how many i's we need
            int totalIntervals = (int) Math.min((end - begin) / interval, 60);
            // The SQL that we will use
            String metricUnion = MeasTabManagerUtil.getUnionStatement(getPurgeRaw(),
                                                                  id.intValue());
            String sql =
                "SELECT ("+begin+" + ("+interval+" * i)) FROM " + TAB_NUMS +
                " WHERE i < "+totalIntervals+" AND" +
                " NOT EXISTS (SELECT timestamp FROM " + metricUnion +
                " WHERE timestamp = ("+begin+" + ("+interval+" * i)) AND " +
                " measurement_id = "+id.intValue()+")";
            rs = stmt.executeQuery(sql);

            // Start with temporary array
            long[] temp = new long[totalIntervals];
            int i;
            for (i = 0; rs.next(); i++) {
                temp[i] = rs.getLong(1);
            }

            // Now shrink the array
            long[] missing = new long[i];
            for (i = 0; i < missing.length; i++) {
                missing[i] = temp[i];
            }
                    
            return missing;
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't lookup historical data for " + id, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /**
     * Fetch a set of data points for a single measurement that fall along a
     * given interval bounded by a range.
     *
     * @param id the id of the Derived Measurement
     * @param startTime the start time.
     * @param intervalInMs the interval length in ms (i.e. 60000 = 60 seconds)
     * @param ticks the the number of ticks desired.
     * @return an array object
     * @ejb:interface-method
     */
    public AggregateObjectMeasurementValue getTimedDataAggregate(
        Integer id, long startTime, long intervalInMs, int ticks)
        throws DataNotAvailableException {
        // jwescott -- this really is voodoo
        long[] bounds = TimingVoodoo.aggregate(startTime, intervalInMs, ticks);
    
        List data = this.getHistoricalData(id, bounds[0], bounds[1],
                                           PageControl.PAGE_ALL);
    
        AggregateObjectMeasurementValue mval = null;
    
        // zero record count returns null which forces datanotfound ex.
        if (data.size() > 0) {
            mval = new AggregateObjectMeasurementValue();
            // Create an array based on the values returned
            Double[] mvArray = new Double[data.size()];
            int i = 0;
            for (Iterator it = data.iterator(); it.hasNext(); i++) {
                MetricValue mv = (MetricValue) it.next();
                mvArray[i] = new Double(mv.getValue());
            }
            mval.setAggArray(mvArray);
        }
    
        return mval;
    }

    /**
     * Fetch a data point around a particular interval. If there
     * are more than one record that falls in range, then the closest
     * value to the interval will be used.
     *
     * @param id the id of the Derived Measurement
     * @param startTime start time.
     * @param intervalInMs the interval length in ms (i.e. 60000 = 60 seconds)
     * @param prev the measurement cycle relevent to now
     *             (i.e. 3 = three cycles ago)
     * @return the list of data points
     * @ejb:interface-method
     */
    public MetricValue getTimedData(Integer id, long startTime,
                                    long intervalInMs, int prev) 
        throws DataNotAvailableException {
        // jwescott -- this really is voodoo
        long[] bounds = TimingVoodoo.previous(startTime, intervalInMs, prev);

        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs   = null;

        // The table to query from
        String table = getDataTable(startTime, System.currentTimeMillis(),
                                    id.intValue());
        try
        {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            final String sqlString =
                "SELECT timestamp, value, "+
                " abs(timestamp - " + bounds[2] + ") AS diff" +
                " FROM " + table +
                " WHERE measurement_id = " + id.intValue()+
                " AND timestamp BETWEEN " + bounds[0] + " AND " + bounds[1] +
                " ORDER BY diff ASC";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sqlString);
            if(rs.next())
                return getMetricValue(rs);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } catch (SQLException e) {
            // Allow the DataNotAvailableException to be thrown
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
        throw new DataNotAvailableException(
            "No data available for " + id + " at " + startTime);
    }

    /**
     * Fetch the list of data points given a request time
     *
     * @param ids The id's of the Derived Measurement
     * @return the list of data points
     * @ejb:interface-method
     */
    public Map getTimedData(Integer[] ids, long reqTime, long interval) 
        throws DataNotAvailableException {
        HashMap values = new HashMap();

        // If we have no ID's, then return empty map
        if (ids.length == 0)
            return values;
        
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;

        // The table to query from
        String table = getDataTable(reqTime, System.currentTimeMillis(), ids);
        try
        {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code

            StringBuffer sqlBuf = new StringBuffer(
                "SELECT * FROM " + table + " WHERE ")
                .append(DBUtil.composeConjunctions("measurement_id", ids.length))
                .append(" AND timestamp BETWEEN ? AND ?");

            stmt = conn.prepareStatement(sqlBuf.toString());

            int i = this.setStatementArguments(stmt, 1, ids);

            // jwescott -- this really is voodoo
            long[] bounds = TimingVoodoo.current(reqTime, interval);

            // this is too verbose ... turn it off for now
            if ( false &&_log.isDebugEnabled() ) {
                _log.debug("sql: " + sqlBuf.toString());
                StringBuffer sb = new StringBuffer();
                sb.append(ids[0].intValue());
                for (int idx=1; idx<ids.length; ++idx) {
                    sb.append("," + ids[idx].intValue());
                }
                _log.debug("ids: " + sb.toString());
                _log.debug("ts1: " + bounds[0]);
                _log.debug("ts2: " + bounds[1]);
            }

            // Let's be generous and give a 5 second window
            stmt.setLong(i++, bounds[0]);
            stmt.setLong(i++, bounds[1]);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Integer mid = new Integer(rs.getInt("measurement_id"));
                values.put(mid, getMetricValue(rs));
            }

            return values;
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't get timed data for: " + StringUtil.arrayToString(ids),e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /**
     * Fetch the most recent data point for particular DerivedMeasurements.
     *
     * @param ids The id's of the DerivedMeasurements
     * @param timestamp Only use data points with collection times greater
     * than the given timestamp.
     * @return A Map of measurement ids to MetricValues.
     * @ejb:interface-method
     */
    public Map getLastDataPoints(Integer[] ids, long timestamp)
    {
        final int MAX_ID_LEN = 10;
        // The return map
        Map data = new HashMap();
        if (ids.length == 0)
            return data;

        // Try to get the values from the cache first
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

        if (nodata.size() == 0) {
            return data;
        } else {
            ids = (Integer[]) nodata.toArray(new Integer[0]);
        }
        
        Connection conn  = null;
        Statement  stmt  = null;
        ResultSet  rs    = null;
        StopWatch  timer = new StopWatch();
        
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            int length = Math.min(ids.length, MAX_ID_LEN);
            boolean constrain =
                (timestamp != MeasurementConstants.TIMERANGE_UNLIMITED);
            stmt = conn.createStatement();
            for (int ind = 0; ind < ids.length; )
            {
                length = Math.min(ids.length - ind, MAX_ID_LEN);
                // Create sub array
                Integer[] subids = new Integer[length];
                for (int j = 0; j < subids.length; j++) {
                    subids[j] = ids[ind++];
                    if (_log.isTraceEnabled())
                        _log.trace("arg" + (j+1) + ": " + subids[j]);
                }
                setDataPoints(data, length, timestamp, subids, stmt);
            }
        } catch (SQLException e) {         
            throw new SystemException("Cannot get last values", e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
            if (_log.isTraceEnabled()) {
                _log.trace("getLastDataPoints(): Statement query elapsed " +
                          "time: " + timer.getElapsed());
            }
        }
        List dataPoints = convertMetricId2MetricValueMapToDataPoints(data);
        updateMetricDataCache(dataPoints);
        return data;
    }

    private void setDataPoints(Map data, int length, long timestamp,
                               Integer[] measIds, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            int i = 1;
            StringBuffer sqlBuf = getLastDataPointsSQL(length, timestamp, measIds);
            if (_log.isTraceEnabled()) {
                _log.trace("getLastDataPoints(): " + sqlBuf);
            }
            rs = stmt.executeQuery(sqlBuf.toString());
            while (rs.next()) {
                Integer mid = new Integer(rs.getInt(1));
                if (!data.containsKey(mid)) {
                    MetricValue mval = getMetricValue(rs);
                    data.put(mid, mval);
                }
            }
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    private StringBuffer getLastDataPointsSQL(int len, long timestamp,
                                              Integer[] measIds)
    {
        String tables = MeasTabManagerUtil.getUnionStatement(getPurgeRaw(), measIds);
        StringBuffer sqlBuf = new StringBuffer(
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
    public double[] getBaselineData(Integer id, long begin, long end) {
        // Check the begin and end times
        super.checkTimeArguments(begin, end);
        
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs = null;

        // The table to query from
        String table = getDataTable(begin, end, id.intValue());
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            StringBuffer sqlBuf = new StringBuffer(
                "SELECT MIN(value), AVG(value), MAX(value) FROM ")
                .append(table)
                .append(" WHERE measurement_id = ").append(id.intValue())
                .append(" AND timestamp BETWEEN ").append(begin)
                .append(" AND ").append(end);

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
     * Fetch a map of aggregate data values keyed by metric templates given
     * a start and stop time range
     *
     * @param tids The id's of the Derived Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the map of data points
     * @ejb:interface-method
     */
    public Map getAggregateData(Integer[] tids, Integer[] iids,
                                long begin, long end)
    {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);

        // Result set
        HashMap resMap = new HashMap();

        if (tids.length == 0 || iids.length == 0)
            return resMap;
        
        // Get the data points and add to the ArrayList
        Connection conn = null;

        // Help database if previous query was cached
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        // Use the already calculated min, max and average on
        // compressed tables.
        String minMax;
        if (usesMetricUnion(begin, end)) {
            minMax = " MIN(value), AVG(value), MAX(value), ";
        } else {
            minMax = " MIN(minvalue), AVG(value), MAX(maxvalue), ";
        }

        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            HQDialect dialect = Util.getHQDialect();
            List measids = MeasTabManagerUtil.getMeasIds(conn, tids, iids);
            String table = getDataTable(begin, end, measids.toArray());
            Map lastMap = dialect.getAggData(conn, minMax, resMap, tids,
                                             iids, begin, end, table);
            return dialect.getLastData(conn, minMax, resMap, lastMap,
                                       iids, begin, end, table);
        } catch (SQLException e) {
            _log.warn("getAggregateData()", e);
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    /**
     * Aggregate data across the given metric IDs, returning max, min, avg, and
     * count of number of unique metric IDs
     *
     * @param mids The id's of the DerivedMeasurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @return the An array of aggregate values
     * @ejb:interface-method
     */
    public double[] getAggregateData(Integer[] mids, long begin, long end) {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        double[] result = new double[DataManagerEJBImpl.IND_CFG_COUNT + 1];
        if (mids.length == 0)
            return result;
        
        //Get the data points and add to the ArrayList
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        // The table to query from
        String table = getDataTable(begin, end, mids);
        StopWatch timer = new StopWatch();    
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
    
            StringBuffer mconj = new StringBuffer(
                DBUtil.composeConjunctions("measurement_id", mids.length));
            DBUtil.replacePlaceHolders(mconj, mids);

            // Use the already calculated min, max and average on
            // compressed tables.
            String minMax;
            if (usesMetricUnion(begin, end)) {
                minMax = " MIN(value), AVG(value), MAX(value), ";
            } else {
                minMax = " MIN(minvalue), AVG(value), MAX(maxvalue), ";
            }

            StringBuffer sqlBuf = new StringBuffer(
                "SELECT " + minMax +
                       "COUNT(DISTINCT(measurement_id)) FROM " + table +
                " WHERE ")
                .append(mconj)
                .append(" AND timestamp BETWEEN ? AND ? ");

            stmt = conn.prepareStatement(sqlBuf.toString());

            int i = 1;
            stmt.setLong(i++, begin);
            stmt.setLong(i++, end);

            if (_log.isTraceEnabled()) {
                DBUtil.replacePlaceHolder(sqlBuf, String.valueOf(begin));
                DBUtil.replacePlaceHolder(sqlBuf, String.valueOf(end));
                _log.trace("double[] getAggregateData(): " + sqlBuf);
            }
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                result[DataManagerEJBImpl.IND_MIN] = rs.getDouble(1);
                result[DataManagerEJBImpl.IND_AVG] = rs.getDouble(2);
                result[DataManagerEJBImpl.IND_MAX] = rs.getDouble(3);
                result[DataManagerEJBImpl.IND_CFG_COUNT] = rs.getDouble(4);
            }
            else {
                return result;    
            }
            
        } catch (SQLException e) {
            throw new SystemException("Can't get aggregate data for "+ 
                                      StringUtil.arrayToString(mids), e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            if (_log.isTraceEnabled()) {
                _log.trace("double[] getAggregateData(): query elapsed time: " +
                          timer.getElapsed());
            }
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
        return result;
    }

    /**
     * Fetch a map of aggregate data values keyed by metrics given
     * a start and stop time range
     *
     * @param tids The id's of the Derived Measurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @return the Map of data points
     * @ejb:interface-method
     */
    public Map getAggregateDataByMetric(Integer[] tids, Integer[] iids,
                                        long begin, long end)
        throws DataNotAvailableException {
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
        StringBuffer tconj = new StringBuffer(
            DBUtil.composeConjunctions("template_id", tids.length));

        try
        {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            // The table to query from
            List measids = MeasTabManagerUtil.getMeasIds(conn, tids, iids);
            String table = getDataTable(begin, end, measids.toArray());
            // Use the already calculated min, max and average on
            // compressed tables.
            String minMax;
            if (usesMetricUnion(begin, end)) {
                minMax = " MIN(value), AVG(value), MAX(value) ";
            } else {
                minMax = " MIN(minvalue), AVG(value), MAX(maxvalue) ";
            }
            
            final String aggregateSQL =
                "SELECT id, " + minMax + 
                " FROM " + table + "," + TAB_MEAS +
                " WHERE timestamp BETWEEN ? AND ? AND " + iconj +
                  " AND " + tconj + " AND measurement_id = id GROUP BY id";
        
            if (_log.isTraceEnabled())
                _log.trace("getAggregateDataByMetric(): " + aggregateSQL);
    
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
                    data[DataManagerEJBImpl.IND_MIN] = rs.getDouble(2);
                    data[DataManagerEJBImpl.IND_AVG] = rs.getDouble(3);
                    data[DataManagerEJBImpl.IND_MAX] = rs.getDouble(4);
    
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
            throw new DataNotAvailableException(e);
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
     * @param mids The id's of the Derived Measurement
     * @param begin The start of the time range
     * @param end The end of the time range
     * @return the map of data points
     * @ejb:interface-method
     */
    public Map getAggregateDataByMetric(Integer[] mids, long begin, long end)
        throws DataNotAvailableException {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        // The table to query from
        String table = getDataTable(begin, end, mids);
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
        if (usesMetricUnion(begin, end)) {
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
                    data[DataManagerEJBImpl.IND_MIN] = rs.getDouble(2);
                    data[DataManagerEJBImpl.IND_AVG] = rs.getDouble(3);
                    data[DataManagerEJBImpl.IND_MAX] = rs.getDouble(4);
                    data[DataManagerEJBImpl.IND_CFG_COUNT] = rs.getDouble(5);
    
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
            throw new DataNotAvailableException(e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
        }
    }

    /**
     * Fetch the list of instance ID's that have data in the given
     * start and stop time range and template IDs
     *
     * @param tids the template IDs
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the list of data points
     * @ejb:interface-method
     */
    public Integer[] getInstancesWithData(Integer[] tids, long begin, long end)
        throws DataNotAvailableException {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        if (tids.length == 0)
            return new Integer[0];

        //Get the valid ids
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;

        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            // The table to query from
            List measids = MeasTabManagerUtil
                .getMeasIdsFromTemplateIds(conn, tids);
            String table = getDataTable(begin, end, measids.toArray());
    
            StringBuffer sqlBuf = new StringBuffer(
                "SELECT DISTINCT(instance_id)" +
                " FROM " + TAB_MEAS + " m, " + table + " d" +
                " WHERE m.id = measurement_id AND ")
                .append(DBUtil.composeConjunctions("template_id", tids.length))
                .append(" AND timestamp BETWEEN ? AND ?");
    
            stmt = conn.prepareStatement(sqlBuf.toString());
    
            // Template ID's
            int i = this.setStatementArguments(stmt, 1, tids);
            
            // Time ranges
            stmt.setLong(i++, begin);
            stmt.setLong(i++, end);
            rs = stmt.executeQuery();
    
            ArrayList validList = new ArrayList();
            for (i = 1; rs.next(); i++) {
                validList.add(new Integer(rs.getInt(1)));
            }

            return (Integer[]) validList.toArray(new Integer[0]);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't get time data for " + StringUtil.arrayToString(tids) +
                " between " + begin + " " + end, e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /**
     * Fetch the list of measurement ID's that have no data in the given time
     * range
     *
     * @param current the current time
     * @param cycles the number of intervals to use as time buffer
     * @return the list of measurement IDs
     * @ejb:interface-method
     */
    public Integer[] getIdsWithoutData(long current, int cycles)
        throws DataNotAvailableException {
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            // select id from EAM_MEASUREMENT where enabled = true
            // and interval is not null and
            // and 0 = (SELECT COUNT(*) FROM EAM_MEASUREMENT_DATA WHERE
            // ID = measurement_id and timestamp > (105410357766 -3 * interval));
            String metricUnion =
                MeasTabManagerUtil.getUnionStatement(getPurgeRaw());
            stmt = conn.prepareStatement(
                "SELECT ID FROM " + TAB_MEAS +
                " WHERE enabled = ? AND NOT interval IS NULL AND " +
                      " NOT EXISTS (SELECT timestamp FROM " + metricUnion +
                                  " WHERE timestamp > (? - ? * interval) AND " +
                                  " WHERE id = measurement_id)");
    
            int i = 1;
            stmt.setBoolean(i++, true);
            stmt.setLong   (i++, current);
            stmt.setInt    (i++, cycles);
            
            rs = stmt.executeQuery();
    
            ArrayList validList = new ArrayList();
            for (i = 1; rs.next(); i++) {
                validList.add(new Integer(rs.getInt(1)));
            }
            return (Integer[]) validList.toArray(new Integer[0]);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't look up missing data", e);
        } catch (NamingException e) {
            throw new SystemException(ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
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
                analyze = Boolean.getBoolean(
                    conf.getProperty(HQConstants.OOBEnabled));
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
    public void setSessionContext(SessionContext ctx) {
        _ctx = ctx;
    }
}
