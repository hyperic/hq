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
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.AggregateObjectMeasurementValue;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.data.MeasurementDataSourceException;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;

/** The DataManagerEJB class is a stateless session bean that can be
 *  used to retrieve measurement data points
 * <p>
 *
 * </p>
 * @ejb:bean name="DataManager"
 *      jndi-name="ejb/measurement/DataManager"
 *      local-jndi-name="LocalDataManager"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class DataManagerEJBImpl extends SessionEJB implements SessionBean {
    private final String logCtx = DataManagerEJBImpl.class.getName();
    private final Log    log    = LogFactory.getLog(logCtx);

    private final long MINUTE   = 60000;
    
    // Table names
    private final String TAB_DATA    = MeasurementConstants.TAB_DATA;
    private final String TAB_DATA_1H = MeasurementConstants.TAB_DATA_1H;
    private final String TAB_DATA_6H = MeasurementConstants.TAB_DATA_6H;
    private final String TAB_DATA_1D = MeasurementConstants.TAB_DATA_1D;
    private final String TAB_MEAS    = MeasurementConstants.TAB_MEAS;
    private final String TAB_NUMS    = "EAM_NUMBERS";
    
    // Error strings
    private final String ERR_DB    = "Cannot look up database instance";
    private final String ERR_INTERVAL =
        "Interval cannot be larger than the time range";
    
    // Save some typing
    private final int IND_MIN       = MeasurementConstants.IND_MIN;
    private final int IND_AVG       = MeasurementConstants.IND_AVG;
    private final int IND_MAX       = MeasurementConstants.IND_MAX;
    private final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;
    private final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;
    
    // Pager class name
    private boolean confDefaultsLoaded = false;

    // Purge intervals, loaded once on first invocation.
    private long purgeRaw, purge1h, purge6h;
    
    // Whether or not we should store all data points
    private boolean storeAll = true;

    ///////////////////////////////////////
    // operations

    // This method is required to avoid a problem that occurs when
    // the agent and server are disconnected AND the server starts
    // back up AND the agent sends data before its ready.

    private double getValue(ResultSet rs) throws SQLException {
        double val = rs.getDouble("value");

        if(rs.wasNull() == true)
            val = Double.NaN;

        return val;
    }
    
    private HighLowMetricValue getMetricValue(ResultSet rs)
        throws SQLException {
        long timestamp = rs.getLong("timestamp");
        double value = this.getValue(rs);
        
        if (value != Double.NaN) {
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
        if (additional.getValue() == Double.NaN)
            return;
        
        if (existing.getValue() == Double.NaN) {
            existing.setHighValue(additional.getHighValue());
            existing.setLowValue(additional.getLowValue());
            existing.setValue(additional.getValue());
            existing.setCount(additional.getCount());
            return;
        }
        
        existing.setHighValue(
            Math.max(existing.getHighValue(), additional.getHighValue()));
        existing.setLowValue(
            Math.min(existing.getLowValue(), additional.getLowValue()));
        
        // Average the two values
        double total = existing.getCount() + additional.getCount();
        existing.setValue(existing.getValue() / total * existing.getCount() +
                        additional.getValue() / total * additional.getCount());
        existing.setCount((int) total);
    }

    // Returns the next index to be used
    private int setStatementArguments(PreparedStatement stmt, int start,
                                      Integer[] ids)
        throws SQLException {
        // Set ID's
        int i = start;
        for (int ind = 0; ind < ids.length; ind++) {
            stmt.setInt(i++, ids[ind].intValue());
        }
        
        return i;
    }

    private void replacePlaceHolder(StringBuffer buf, String repl) {
        int index = buf.indexOf("?");
        if (index >= 0)
            buf.replace(index, index + 1, repl);
    }

    private void replacePlaceHolders(StringBuffer buf, Object[] objs) {
        for (int i = 0; i < objs.length; i++)
            replacePlaceHolder(buf, objs[i].toString());
    }
    
    private void checkTimeArguments(long begin, long end, long interval)
        throws IllegalArgumentException {
        
        this.checkTimeArguments(begin, end);
        
        if(interval > (end - begin) )
            throw new IllegalArgumentException(this.ERR_INTERVAL);
    }
    
    /** <p>
     * Save the new MetricValue to the database
     * </p>
     * @param dp the new MetricValue
     * @ejb:interface-method
     */
    public void addData(Integer mid, MetricValue dp, boolean overwrite) {
        this.addData(mid, new MetricValue[] { dp }, overwrite);
    }

    /** <p>
     * Save the new MetricValue to the database
     * </p>
     * @param dp the new MetricValue
     * @ejb:interface-method
     * @ejb:transaction type="NEVER"
     */
    public void addData(Integer mid, MetricValue[] dpts, boolean overwrite)
    {
        // Be ready to send a measurement event
        ArrayList events = new ArrayList();
        MetricDataCache cache = MetricDataCache.getInstance();
        
        // Save the data point
        Connection conn = null;
        PreparedStatement istmt = null;
        PreparedStatement ustmt = null;

        Analyzer analyzer = (Analyzer) ProductProperties
                .getPropertyInstance("hyperic.hq.measurement.analyzer");

        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            
            conn.setAutoCommit(false);

            if (overwrite && DBUtil.isPostgreSQL(conn)) {
                istmt = conn.prepareStatement(
                    "SELECT add_data(?, ?, ?)");

            }
            else {
                istmt = conn.prepareStatement(
                    "INSERT  /*+ APPEND */ INTO " + TAB_DATA +
                    " (measurement_id, timestamp, value) VALUES (?, ?, ?)");
            }

            for (int ind = 0; ind < dpts.length; ind++) {
                BigDecimal bigDec;
                
                try {
                    bigDec = new BigDecimal(dpts[ind].getValue());
                } catch (NumberFormatException e) {
                    // Don't try to insert if it's not a number
                    log.debug("addData() encountered " + dpts[ind].getValue() +
                              " for ID: " + mid);
                    continue;
                }

                // See if we even need to insert this data
                if (getStoreAll() == false &&
                    cache.hasChanged(mid, dpts[ind]) == false) {
                    cache.add(mid, dpts[ind]);
                    continue;
                }
                
                try {
                    int i = 1;
                    istmt.setInt       (i++, mid.intValue());
                    istmt.setLong      (i++, dpts[ind].getTimestamp());
                    istmt.setBigDecimal(i++, bigDec);                    
                    istmt.execute();
                    
                    conn.commit();
                } catch (SQLException e) {
                    if (e.getMessage().toUpperCase()
                         .indexOf("MEASUREMENT_DATA_ID_TIME") == -1) {
                        log.error("addData() SQL Error: " + e.getMessage() +
                                  " Code: " + e.getErrorCode());
                        continue;
                    }

                    if (!overwrite) {
                        // Do a little verbose logging here
                        log.debug("addData() value already exists for ID: "
                                  + mid + " at: " + dpts[ind].getTimestamp()
                                  + " value: " + dpts[ind]);
                        continue;
                    }

                    // Expect a SQLException to be thrown if the data is not
                    // unique; this is okay, since we only need one data point
                    try {
                        if (log.isTraceEnabled())
                            log.trace("Overwrite data point for ID: " + mid +
                                      " at: " + dpts[ind].getTimestamp() +
                                      " Value: " + dpts[ind]/*, e*/);

                        // Roll back previous transaction
                        conn.rollback();

                        // Overwrite the data
                        if (ustmt == null) {
                            ustmt = conn.prepareStatement(
                                "UPDATE " + TAB_DATA + " SET value = ? " +
                                "WHERE measurement_id = ? AND timestamp = ?"
                                );
                        }

                        int i = 1;
                        ustmt.setBigDecimal(i++,bigDec);
                        ustmt.setInt       (i++,mid.intValue());
                        ustmt.setLong      (i++,dpts[ind].getTimestamp());
                        ustmt.executeUpdate();
                        conn.commit();
                    } catch (SQLException se) {
                        log.error("Update duplicate data in addData(" +
                                  mid + ", " + dpts[ind] + ") failed at: " +
                                  dpts[ind].getTimestamp(), se);
                    }
                }

                // Analyze the value
                if (analyzer != null) {
                    analyzer.analyzeMetricValue(mid, dpts[ind]);
                }

                // Save value in "last" cache
                if (cache.add(mid, dpts[ind])) {
                    MeasurementEvent event =
                        new MeasurementEvent(mid, dpts[ind]);
                    
                    // See if we need to send the measurement event                
                    if (RegisteredTriggers.isTriggerInterested(event))
                        events.add(event);                    
                }
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (SQLException e) {
            throw new SystemException(
                "addData() unable to obtain connection or statement", e);
        } finally {
            DBUtil.closeStatement(logCtx, istmt);
            DBUtil.closeStatement(logCtx, ustmt);
            DBUtil.closeConnection(logCtx, conn);
            // DEBUG: connection tracking code
            decrement(conn);
        }
        
        // Now send the events
        if (events.size() > 0) {
            Messenger sender = new Messenger();
            sender.publishMessage(EventConstants.EVENTS_TOPIC, events);
        }
    }

    /**
     * Get the server purge configuration and storage option, loaded on startup.
     */
    private void loadConfigDefaults() 
    {
        this.log.debug("Loading default purge intervals");
        Properties conf;
        try {
            conf = ServerConfigManagerUtil.getLocalHome().create().getConfig();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (ConfigPropertyException e) {
            // Not gonna happen
            throw new SystemException(e);
        }

        String purgeRawString = conf.getProperty(HQConstants.DataPurgeRaw);
        String purge1hString  = conf.getProperty(HQConstants.DataPurge1Hour);
        String purge6hString  = conf.getProperty(HQConstants.DataPurge6Hour);
        String storeAllString = conf.getProperty(HQConstants.DataStoreAll);

        try {
            this.purgeRaw = Long.parseLong(purgeRawString);
            this.purge1h = Long.parseLong(purge1hString);
            this.purge6h = Long.parseLong(purge6hString);
            this.confDefaultsLoaded = true;
        } catch (NumberFormatException e) {
            // Shouldn't happen unless manual edit of config table
            throw new IllegalArgumentException("Invalid purge interval: " + e);
        }
        
        if (storeAllString != null)
            this.storeAll = Boolean.valueOf(storeAllString).booleanValue();
    }

    /**
     * Based on the given start time, determine which measurement 
     * table we should query for measurement data.
     */
    private String getDataTable(long begin)
    {
        long now = System.currentTimeMillis();

        if (!this.confDefaultsLoaded)
            loadConfigDefaults();

        if (now - this.purgeRaw < begin) {
            return TAB_DATA;
        } else if (now - this.purge1h < begin) {
            return TAB_DATA_1H;
        } else if (now - this.purge6h < begin) {
            return TAB_DATA_6H;
        } else {
            return TAB_DATA_1D;
        }
    }
    
    private boolean getStoreAll() {
        if (!this.confDefaultsLoaded)
            loadConfigDefaults();
        
        return storeAll;
    }
    
    /** <p>
     * Fetch the list of historical data points given
     * a start and stop time range
     * </p>
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
        
        // The total count
        int total = 0;

        // The table to query from
        String table = getDataTable(begin);
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            try {
                // DEBUG: connection tracking code
                increment(conn);
    
                stmt = conn.prepareStatement(
                    "SELECT count(*) FROM " + table +
                    " WHERE measurement_id=? AND timestamp BETWEEN ? AND ?");
    
                int i = 1;
                stmt.setInt (i++, id.intValue());
                stmt.setLong(i++, begin);
                stmt.setLong(i++, end);
                rs = stmt.executeQuery();
    
                if (rs.next())
                    total = rs.getInt(1);

                if (total == 0) {
                    // Nothing to return
                    return new PageList();
                }
            } catch (SQLException e) {
                throw new DataNotAvailableException(
                    "Can't count historical data for " + id, e);
            } finally {
                DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            }

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

                if ( log.isDebugEnabled() ) {
                    log.debug("getHistoricalData(): " + sqlBuf);
                    log.debug("arg1 = " + id);
                    log.debug("arg2 = " + begin);
                    log.debug("arg3 = " + end);
                }

                if (sizeLimit) {
                    stmt.setInt(i++, pc.getPageEntityIndex() + 1);
                    if ( log.isDebugEnabled() )
                        log.debug("arg4 = " + (pc.getPageEntityIndex() + 1));
                }
                
                StopWatch timer = new StopWatch(current);
                
                rs = stmt.executeQuery();

                if ( log.isTraceEnabled() ) {
                    log.trace("getHistoricalData() execute time: " +
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
            throw new SystemException(this.ERR_DB, e);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't open connection", e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /** <p>
     * Fetch the list of historical data points given
     * a start and stop time range and interval
     * </p>
     * @param id the id of the Derived Measurement
     * @param beginTrack the start of the time range
     * @param end the end of the time range
     * @param interval for the time range
     * @param type (collection) for the metric
     * @param wantNull specifies whether intervals with no data should be return as nulls
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
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;

        // The table to query from
        String table = getDataTable(begin);
    
        try {
            StopWatch timer = new StopWatch(current);
            
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);

            // DEBUG: connection tracking code
            increment(conn);
    
            if(log.isDebugEnabled()) {
                log.debug("GetHistoricalData: ID: " +
                          StringUtil.arrayToString(ids) + ", Begin: " +
                          TimeUtil.toString(begin) + ", End: " +
                          TimeUtil.toString(end) + ", Interval: " + interval +
                          '(' + (interval / 60000) + "m " + (interval % 60000) +
                          "s)" );
            }

            // Construct SQL command
            String selectType;
            
            switch(type) {
            case MeasurementConstants.COLL_TYPE_DYNAMIC:
                if (table.equals(TAB_DATA))
                    selectType = "AVG(value) AS value, " +
                                 "MAX(value) AS peak, MIN(value) AS low";
                else
                    selectType = "AVG(value) AS value, " +
                                 "MAX(maxvalue) AS peak, MIN(minvalue) AS low";
                break; 
            case MeasurementConstants.COLL_TYPE_TRENDSUP:
            case MeasurementConstants.COLL_TYPE_STATIC:
                selectType = "MAX(value) AS value";
                break;
            case MeasurementConstants.COLL_TYPE_TRENDSDOWN:
                selectType = "MIN(value) AS value";
                break;
            default:
                throw new IllegalArgumentException(
                    "No collection type specified in historical metric query.");
            }
            
            StringBuffer sqlbuf = new StringBuffer()
                .append("SELECT begin AS timestamp, ")
                .append(selectType)
                .append(" FROM ")
                .append("(SELECT ? + (? * i) AS begin FROM ")
                .append(TAB_NUMS)
                .append(" WHERE i < ?) n, ")
                .append(table)
                .append(" WHERE timestamp BETWEEN begin AND begin + ? AND ")
                .append(REPL_IDS)
                .append(" GROUP BY begin ORDER BY begin");
            
            if (pc.isDescending())
                sqlbuf.append(" DESC");

            final int pagesize =
                (int) Math.min(Math.max(pc.getPagesize(),
                                        (end - begin) / interval), 60);
            final long intervalWnd = interval * pagesize;

            int idsCnt = 0;
            
            for (int index = 0; index < ids.length; index += idsCnt) {
                String sql = sqlbuf.toString();
                
                // Prepare the statement correctly
                if (idsCnt != Math.min(MAX_IDS, ids.length - index)) {
                    idsCnt = Math.min(MAX_IDS, ids.length - index);
                    sql = StringUtil.replace(sql, REPL_IDS,
                        DBUtil.composeConjunctions("measurement_id", idsCnt));

                    // Prepare the command and bind the variables
                    DBUtil.closeStatement(logCtx, stmt);
                    stmt = conn.prepareStatement(sql);
                }
                
                long beginTrack = begin;
                do {
                    // Adjust the begin and end to query only the rows for the
                    // specified page.
                    long beginWnd;
                    long endWnd;

                    if(log.isDebugEnabled())
                        log.debug(pc.toString());
                    
                    if(pc.isDescending()) {
                        endWnd   = end - (pc.getPagenum() * intervalWnd);
                        beginWnd = Math.max(beginTrack, endWnd - intervalWnd);
                    } else {
                        beginWnd = beginTrack + (pc.getPagenum() * intervalWnd); 
                        endWnd   = Math.min(end, beginWnd + intervalWnd);
                    }
                    
                    if(log.isDebugEnabled()) {
                        log.debug(
                            "Page Window: Begin: " + TimeUtil.toString(beginWnd)
                            + ", End: " + TimeUtil.toString(endWnd) );

                        log.debug("SQL Command: " + sqlbuf.toString());
                    }
                    
                    int i = 1;
                    stmt.setLong(i++, beginWnd);
                    stmt.setLong(i++, interval);
                    stmt.setLong(i++, (endWnd - beginWnd) / interval);
                    stmt.setLong(i++, interval - 1);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("arg 1 = " + beginWnd);
                        log.debug("arg 2 = " + interval);
                        log.debug("arg 3 = " + (endWnd - beginWnd) / interval);
                        log.debug("arg 4 = " + interval);
                    }

                    int ind = index;
                    int endIdx = ind + idsCnt;
                    for (; ind < endIdx; ind++) {
                        if (log.isDebugEnabled())
                            log.debug("arg " + i + " = " + ids[ind]);

                        stmt.setInt(i++, ids[ind].intValue());
                    }

                    rs = stmt.executeQuery();
                    
                    long curTime = beginWnd;
                    
                    Iterator it = null;
                    if (history.size() > 0)
                        it = history.iterator();
                    
                    for (int row = 0; row < pagesize; row++) {
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
                            if (returnNulls || val.getValue() != Double.NaN) {
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
                    
                    if (log.isDebugEnabled()) {
                        log.debug("getHistoricalData() for " + ids.length +
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

                if(log.isDebugEnabled()) {
                    log.debug("GetHistoricalData: ElapsedTime: " + timer +
                              " seconds");
                }
            }

            // Now return a PageList
            return new PageList(history,
                                pc.getPageEntityIndex() + history.size());
        } catch (NamingException e) {
            throw new SystemException(this.ERR_DB, e);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't lookup historical data for " +
                StringUtil.arrayToString(ids), e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /**
     * <p>
     * Fetch a list of historical data points of a specific size Note: There is
     * no guarantee that the list will be the size requested. It may be smaller.
     * </p>
     * 
     * @param id
     *            the id of the Derived Measurement
     * @param begin
     *            the start of the time range
     * @param end
     *            the end of the time range
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
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;

        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);
    
            /*
             * SELECT timestamp, value FROM EAM_MEASUREMENT_DATA, EAM_NUMBERS,
             * (SELECT id, interval, MAX(timestamp) AS maxt FROM
             * EAM_MEASUREMENT_data, EAM_MEASUREMENT WHERE measurement_id = id
             * AND id=10012 GROUP BY id,interval) mt WHERE MEASUREMENT_ID = id
             * AND TIMESTAMP = (maxt - i * interval);
             */
            StringBuffer sqlBuf = new StringBuffer(
                "SELECT timestamp, value FROM " + TAB_DATA + ", " +
                                                  TAB_NUMS + ", " +
                    "(SELECT id, interval, MAX(timestamp) AS maxt" +
                    " FROM " + TAB_DATA + ", " + TAB_MEAS +
                    " WHERE measurement_id = id AND id = ?" +
                    " GROUP BY id, interval) mt " +
                "WHERE measurement_id = id AND" +
                     " timestamp = (maxt - i * interval)");

            stmt = conn.prepareStatement(sqlBuf.toString());
            
            if (log.isDebugEnabled()) {
                log.debug("getLastHistoricalData(): " + sqlBuf);
            }
    
            stmt.setInt (1, id.intValue());
            rs = stmt.executeQuery();
    
            for (int i = 0; rs.next() && i < count; i++) {
                history.add(getMetricValue(rs));
            }
    
            // Now return a PageList
            return history;
        } catch (NamingException e) {
            throw new SystemException(this.ERR_DB, e);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't lookup historical data for " + id, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /** <p>
     * Fetch an array of timestamps for which there is missing data
     * </p>
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
        
        // The SQL that we will use
        final String SQL =
            "SELECT (? + (? * i)) FROM " + TAB_NUMS +
            " WHERE i < ? AND" +
                  " NOT EXISTS (SELECT timestamp FROM " + TAB_DATA +
                               " WHERE measurement_id = ? AND" +
                                     " timestamp = (? + (? * i)))";
       
        //Get the data points and add to the ArrayList
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            
            // First, figure out how many i's we need
            int totalIntervals = (int) Math.min((end - begin) / interval, 60);
    
            stmt = conn.prepareStatement(SQL);
            
            int i = 1;
            stmt.setLong(i++, begin);
            stmt.setLong(i++, interval);
            stmt.setInt (i++, totalIntervals);
            stmt.setInt (i++, id.intValue());
            stmt.setLong(i++, begin);
            stmt.setLong(i++, interval);

            rs = stmt.executeQuery();

            // Start with temporary array
            long[] temp = new long[totalIntervals];
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
            throw new SystemException(this.ERR_DB, e);
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't lookup historical data for " + id, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
    }

    /** <p>
     * Fetch a set of data points for a single measurement that fall along a
     * given interval bounded by a range.
     * </p>
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
     * <p>Fetch a data point around a particular interval. If there
     * are more than one record that falls in range, then the closest
     * value to the interval will be used.</p>
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

        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;

        // The table to query from
        String table = getDataTable(startTime);
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);
    
            final String sqlString =
                "SELECT timestamp, value, abs(timestamp - ?) AS diff" +
                " FROM " + table +
                " WHERE measurement_id = ? AND timestamp BETWEEN ? AND ?" +
                " ORDER BY diff ASC";
    
            stmt = conn.prepareStatement(sqlString);
    
            int i = 1;
            stmt.setLong(i++, bounds[2]);
            stmt.setInt (i++, id.intValue());
            stmt.setLong(i++, bounds[0]);
            stmt.setLong(i++, bounds[1]);
            rs = stmt.executeQuery();

            if(rs.next())
                return getMetricValue(rs);
        } catch (NamingException e) {
            throw new SystemException(this.ERR_DB, e);
        } catch (SQLException e) {
            // Allow the DataNotAvailableException to be thrown
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
            // DEBUG: connection tracking code
            decrement(conn);
        }
 
        throw new DataNotAvailableException(
            "No data available for " + id + " at " + startTime);
    }

    /** <p>
     * Fetch the list of data points given a request time
     * </p>
     * @param id the id of the Derived Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
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
        String table = getDataTable(reqTime);

        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);

            StringBuffer sqlBuf = new StringBuffer(
                "SELECT * FROM " + table + " WHERE ")
                .append(DBUtil.composeConjunctions("measurement_id", ids.length))
                .append(" AND timestamp BETWEEN ? AND ?");

            stmt = conn.prepareStatement(sqlBuf.toString());

            int i = this.setStatementArguments(stmt, 1, ids);

            // jwescott -- this really is voodoo
            long[] bounds = TimingVoodoo.current(reqTime, interval);

            // this is too verbose ... turn it off for now
            if ( false &&log.isDebugEnabled() ) {
                log.debug("sql: " + sqlBuf.toString());
                StringBuffer sb = new StringBuffer();
                sb.append(ids[0].intValue());
                for (int idx=1; idx<ids.length; ++idx) {
                    sb.append("," + ids[idx].intValue());
                }
                log.debug("ids: " + sb.toString());
                log.debug("ts1: " + bounds[0]);
                log.debug("ts2: " + bounds[1]);
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
            throw new SystemException(this.ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    private StringBuffer getLastDataPointsSQL(int len, boolean constrain) {
        StringBuffer sqlBuf = new StringBuffer(
            "SELECT measurement_id, value, timestamp" +
            " FROM " + TAB_DATA + ", " +
                     "(SELECT measurement_id AS id, MAX(timestamp) AS maxt" +
                     " FROM " + TAB_DATA + " WHERE ")
                     .append(DBUtil.composeConjunctions("measurement_id", len));
        
        if (constrain)
            sqlBuf.append(" AND timestamp >= ? ");
        
        sqlBuf.append(" GROUP BY measurement_id) mt")
              .append(" WHERE measurement_id = id AND timestamp = maxt");
        return sqlBuf;
    }
    
    /** <p>
     * Fetch the most recent data point for particular DerivedMeasurements.
     * </p>
     * @param id the id of the Derived Measurement
     * @return long value timestamp of last record.
     * @ejb:interface-method
     */
    public Map getLastDataPoints(Integer[] ids, long timerange)
        throws DataNotAvailableException {
        final int MAX_ID_LEN = 10;
        
        // The return map
        Map data = new HashMap();
        if (ids.length == 0)
            return data;

        // Try to get the values from the cache first
        MetricDataCache cache = MetricDataCache.getInstance();
        ArrayList nodata = new ArrayList();
        for (int i = 0; i < ids.length; i++) {
            MetricValue mval = cache.get(ids[i], timerange);
            if (mval != null)
                data.put(ids[i], mval);
            else
                nodata.add(ids[i]);
        }
        if (nodata.size() == 0) {
            return data;
        }
        else {
            ids = (Integer[]) nodata.toArray(new Integer[0]);
        }
        
        Connection        conn  = null;
        PreparedStatement stmt  = null;
        ResultSet         rs    = null;
        StopWatch         timer = new StopWatch();
        
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);

            int length = Math.min(ids.length, MAX_ID_LEN);
            boolean constrain =
                (timerange != MeasurementConstants.TIMERANGE_UNLIMITED);
            StringBuffer sqlBuf = this.getLastDataPointsSQL(length, constrain);
            
            // No binding of id's due to bug in Pointbase
            //this.replacePlaceHolder(sqlBuf, ids);

            stmt = conn.prepareStatement(sqlBuf.toString());

            for (int ind = 0; ind < ids.length; ) {
                length = Math.min(ids.length - ind, MAX_ID_LEN);
                
                if (length != MAX_ID_LEN) {
                    // Close the statement
                    DBUtil.closeStatement(logCtx,stmt);
                    
                    // Prepare new statement, length has changed
                    sqlBuf = this.getLastDataPointsSQL(length, constrain);
                    stmt = conn.prepareStatement(sqlBuf.toString());
                }
                
                if (log.isTraceEnabled()) {
                    log.trace("getLastDataPoints(): " + sqlBuf);
                }

                // Create sub array
                Integer[] subids = new Integer[length];
                for (int j = 0; j < subids.length; j++) {
                    subids[j] = ids[ind++];

                    if (log.isTraceEnabled())
                        log.trace("arg" + (j+1) + ": " + subids[j]);
                }
                
                // Set the ID's
                int i = 1;
                i = this.setStatementArguments(stmt, i, subids);
                
                if (constrain) {
                    stmt.setLong(i++, timerange);

                    if (log.isTraceEnabled())
                        log.trace("arg" + (i-1) + ": " + timerange);
                }

                rs = stmt.executeQuery();

                while (rs.next()) {
                    Integer mid = new Integer(rs.getInt(1));
                    if (!data.containsKey(mid)) {
                        MetricValue mval = getMetricValue(rs);
                        data.put(mid, mval);
                        cache.add(mid, mval);
                    }
                }
                
                DBUtil.closeResultSet(logCtx, rs);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            if (log.isTraceEnabled())
                log.trace("failed to get measurement id: ", e);            
            throw new DataNotAvailableException("Cannot get last values", e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
            // DEBUG: connection tracking code
            decrement(conn);

            if (log.isTraceEnabled()) {
                log.trace("getLastDataPoints(): Statement query elapsed " +
                          "time: " + timer.getElapsed());
            }
        }
    
        return data;
    }

    /**
     * Get a List of <code>{@link
     * org.hyperic.util.data.IComparableDataPoint}</code> objects in
     * order to compute a baseline.
     *
     * @ejb:interface-method
     */
    public double[] getBaselineData(Integer id, long begin, long end) {
        // Check the begin and end times
        super.checkTimeArguments(begin, end);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        // The table to query from
        String table = getDataTable(begin);
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);

            StringBuffer sqlBuf = new StringBuffer(
                "SELECT MIN(value), AVG(value), MAX(value) FROM ")
                .append(table)
                .append(" WHERE measurement_id = ?")
                .append(" AND timestamp BETWEEN ? AND ?");

            stmt = conn.prepareStatement( sqlBuf.toString() );
    
            int i = 1;
            stmt.setInt (i++, id.intValue());
            stmt.setLong(i++, begin);
            stmt.setLong(i++, end);
            rs = stmt.executeQuery();

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
            throw new SystemException(this.ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /** <p>
     * Fetch a map of aggregate data values keyed by metric templates given
     * a start and stop time range
     * </p>
     * @param id the id of the Derived Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the map of data points
     * @ejb:interface-method
     */
    public Map getAggregateData(Integer[] tids, Integer[] iids,
                                long begin, long end)
        throws DataNotAvailableException {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);

        // Result set
        HashMap resMap = new HashMap();

        if (tids.length == 0 || iids.length == 0)
            return resMap;
        
        // Get the data points and add to the ArrayList
        Connection conn         = null;
        PreparedStatement astmt = null;
        PreparedStatement lstmt = null;
        ResultSet rs            = null;
        StopWatch timer         = new StopWatch();

        // Keep track of the "last" reported time
        HashMap lastMap = new HashMap();
        
        // Help database if previous query was cached
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        // The table to query from
        String table = getDataTable(begin);

        StringBuffer iidsConj = new StringBuffer(
                DBUtil.composeConjunctions("instance_id", iids.length));
        replacePlaceHolders(iidsConj, iids);
            
        StringBuffer tidsConj = new StringBuffer(
                DBUtil.composeConjunctions("template_id", tids.length));
        replacePlaceHolders(tidsConj, tids);

        // Use the already calculated min, max and average on
        // compressed tables.
        String minMax;
        if (table.equals(TAB_DATA)) {
            minMax = " MIN(value), AVG(value), MAX(value), ";
        } else {
            minMax = " MIN(minvalue), AVG(value), MAX(maxvalue), ";
        }

        final String aggregateSQL =
            "SELECT COUNT(DISTINCT id)," + minMax +
                   "MAX(timestamp), template_id " +
            " FROM " + table + "," + TAB_MEAS +
            " WHERE timestamp BETWEEN ? AND ? AND measurement_id = id AND " +
                    iidsConj + " AND " + tidsConj + " GROUP BY template_id";
        
        final String lastSQL =
            "SELECT /*+ RULE */ value FROM " + table + ", " +
                "(SELECT id FROM " + TAB_MEAS +
                    " WHERE template_id = ? AND " + iidsConj + ") ids " +
            "WHERE id = measurement_id AND timestamp = ?";
        
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);

            // Prepare aggregate SQL
            astmt = conn.prepareStatement(aggregateSQL);
            
            // First set the time range
            int ind = 1;
            astmt.setLong(ind++, begin);
            astmt.setLong(ind++, end);
            
            try {
                if (log.isTraceEnabled())
                    log.trace("getAggregateData() for begin=" + begin +
                              " end = " + end + ": " + aggregateSQL);
                
                // First get the min, max, average
                rs = astmt.executeQuery();

                while (rs.next()) {
                    Integer tid = new Integer(rs.getInt("template_id"));
                    
                    double[] data = new double[this.IND_LAST_TIME + 1];

                    // data[0] = min, data[1] = avg, data[2] = max,
                    // data[3] = last, data[4] = count of measurement ID's
                    data[this.IND_CFG_COUNT] = rs.getInt(1);

                    // If there are no metrics, then forget it
                    if (data[this.IND_CFG_COUNT] == 0)
                        continue;

                    data[this.IND_MIN] = rs.getDouble(2);
                    data[this.IND_AVG] = rs.getDouble(3);
                    data[this.IND_MAX] = rs.getDouble(4);
    
                    // Put it into the result map
                    resMap.put(tid, data);

                    // Get the time
                    Long lastTime = new Long(rs.getLong(5));
                    
                    // Put it into the last map
                    lastMap.put(tid, lastTime);
                }
                
                if (log.isTraceEnabled())
                    log.trace("getAggregateData(): Statement query elapsed: " +
                              timer.reset());
            } finally {
                DBUtil.closeResultSet(logCtx, rs);
            }

            // Prepare last value SQL
            lstmt = conn.prepareStatement(lastSQL);

            for (Iterator it = lastMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                
                Integer tid = (Integer) entry.getKey();
                Long lastTime = (Long) entry.getValue();
                
                // First set the instance ID
                astmt.setInt(1, tid.intValue());

                // Now get the last timestamp
                if (log.isTraceEnabled()) {
                    log.trace("getAggregateData() for tid=" + tid +
                              " lastTime=" + lastTime + ": " + lastSQL);
                }

                // Reset the index
                ind = 1;
                lstmt.setInt(ind++, tid.intValue());
                lstmt.setLong(ind++, lastTime.longValue());
               
                try {
                    rs = lstmt.executeQuery();
                    
                    // Assume data exists
                    rs.next();
                    
                    // Get the double[] value from results
                    double[] data = (double[]) resMap.get(tid);

                    // Now set the the last reported value
                    data[IND_LAST_TIME] = rs.getDouble(1);
                } finally {
                    // Close ResultSet
                    DBUtil.closeResultSet(logCtx, rs);
                }

                if (log.isTraceEnabled()) {
                    log.trace("getAggregateData(): Statement query elapsed " +
                              "time: " + timer.reset());
                }
            }

            return resMap;
        } catch (SQLException e) {
            log.debug("getAggregateData()", e);
            throw new DataNotAvailableException(e);
        } catch (NamingException e) {
            throw new SystemException(this.ERR_DB, e);
        } finally {
            DBUtil.closeStatement(logCtx, astmt);
            DBUtil.closeStatement(logCtx, lstmt);
            DBUtil.closeConnection(logCtx, conn);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /** <p>
     * Aggregate data across the given metric IDs, returning max, min, avg, and
     * count of number of unique metric IDs
     * </p>
     * @param id the id of the Derived Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the double array of aggregate values
     * @ejb:interface-method
     */
    public double[] getAggregateData(Integer[] mids, long begin, long end)
        throws DataNotAvailableException {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        double[] result = new double[this.IND_CFG_COUNT + 1];
        if (mids.length == 0)
            return result;
        
        //Get the data points and add to the ArrayList
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        // The table to query from
        String table = getDataTable(begin);

        StopWatch timer = new StopWatch();    
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);
    
            StringBuffer mconj = new StringBuffer(
                DBUtil.composeConjunctions("measurement_id", mids.length));
            this.replacePlaceHolders(mconj, mids);

            // Use the already calculated min, max and average on
            // compressed tables.
            String minMax;
            if (table.equals(TAB_DATA)) {
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

            if (log.isTraceEnabled()) {
                this.replacePlaceHolder(sqlBuf, String.valueOf(begin));
                this.replacePlaceHolder(sqlBuf, String.valueOf(end));
                log.trace("double[] getAggregateData(): " + sqlBuf);
            }
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                result[this.IND_MIN] = rs.getDouble(1);
                result[this.IND_AVG] = rs.getDouble(2);
                result[this.IND_MAX] = rs.getDouble(3);
                result[this.IND_CFG_COUNT] = rs.getDouble(4);
            }
            else {
                return result;    
            }
            
        } catch (SQLException e) {
            throw new DataNotAvailableException(
                "Can't get aggregate data for "+ StringUtil.arrayToString(mids),
                e);
        } catch (NamingException e) {
            throw new SystemException(this.ERR_DB, e);
        } finally {
            if (log.isTraceEnabled()) {
                log.trace("double[] getAggregateData(): query elapsed time: " +
                          timer.getElapsed());
            }
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
            // DEBUG: connection tracking code
            decrement(conn);
        }
        return result;
    }

    /** <p>
     * Fetch a map of aggregate data values keyed by metrics given
     * a start and stop time range
     * </p>
     * @param id the id of the Derived Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
     * @return the map of data points
     * @ejb:interface-method
     */
    public Map getAggregateDataByMetric(Integer[] tids, Integer[] iids,
                                        long begin, long end)
        throws DataNotAvailableException {
        // Check the begin and end times
        this.checkTimeArguments(begin, end);
        begin = TimingVoodoo.roundDownTime(begin, MINUTE);
        end = TimingVoodoo.roundDownTime(end, MINUTE);

        // The table to query from
        String table = getDataTable(begin);
    
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
        replacePlaceHolders(iconj, iids);
        
        StringBuffer tconj = new StringBuffer(
            DBUtil.composeConjunctions("template_id", tids.length));

        // Use the already calculated min, max and average on
        // compressed tables.
        String minMax;
        if (table.equals(TAB_DATA)) {
            minMax = " MIN(value), AVG(value), MAX(value) ";
        } else {
            minMax = " MIN(minvalue), AVG(value), MAX(maxvalue) ";
        }
            
        final String aggregateSQL =
            "SELECT id, " + minMax + 
            " FROM " + table + "," + TAB_MEAS +
            " WHERE timestamp BETWEEN ? AND ? AND " + iconj +
              " AND " + tconj + " AND measurement_id = id GROUP BY id";
        
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);
            
            if (log.isTraceEnabled())
                log.trace("getAggregateDataByMetric(): " + aggregateSQL);
    
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
                    data[this.IND_MIN] = rs.getDouble(2);
                    data[this.IND_AVG] = rs.getDouble(3);
                    data[this.IND_MAX] = rs.getDouble(4);
    
                    // Put it into the result map
                    resMap.put(mid, data);
                }
            } finally {
                DBUtil.closeResultSet(logCtx, rs);
            }
    
            if (log.isTraceEnabled())
                log.trace("getAggregateDataByMetric(): Statement query elapsed "
                          + "time: " + timer.getElapsed());
    
            return resMap;
        } catch (SQLException e) {
            log.debug("getAggregateDataByMetric()", e);
            throw new DataNotAvailableException(e);
        } catch (NamingException e) {
            throw new SystemException(this.ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /** <p>
     * Fetch a map of aggregate data values keyed by metrics given
     * a start and stop time range
     * </p>
     * @param id the id of the Derived Measurement
     * @param begin the start of the time range
     * @param end the end of the time range
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
        String table = getDataTable(begin);
    
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
        replacePlaceHolders(mconj, mids);

        // Use the already calculated min, max and average on
        // compressed tables.
        String minMax;
        if (table.equals(TAB_DATA)) {
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
            // DEBUG: connection tracking code
            increment(conn);
            
            if (log.isTraceEnabled())
                log.trace("getAggregateDataByMetric(): " + aggregateSQL);
    
            stmt = conn.prepareStatement(aggregateSQL);
            
            int i = 1;
            stmt.setLong(i++, begin);
            stmt.setLong(i++, end);
    
            try {
                rs = stmt.executeQuery();
    
                while (rs.next()) {
                    double[] data = new double[IND_CFG_COUNT + 1];
    
                    Integer mid = new Integer(rs.getInt(1));
                    data[this.IND_MIN] = rs.getDouble(2);
                    data[this.IND_AVG] = rs.getDouble(3);
                    data[this.IND_MAX] = rs.getDouble(4);
                    data[this.IND_CFG_COUNT] = rs.getDouble(5);
    
                    // Put it into the result map
                    resMap.put(mid, data);
                }
            } finally {
                DBUtil.closeResultSet(logCtx, rs);
            }
    
            if (log.isTraceEnabled())
                log.trace("getAggregateDataByMetric(): Statement query elapsed "
                          + "time: " + timer.getElapsed());
    
            return resMap;
        } catch (SQLException e) {
            log.debug("getAggregateDataByMetric()", e);
            throw new DataNotAvailableException(e);
        } catch (NamingException e) {
            throw new SystemException(this.ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, null);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /** <p>
     * Fetch the list of instance ID's that have data in the given
     * start and stop time range and template IDs
     * </p>
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

        // The table to query from
        String table = getDataTable(begin);

        if (tids.length == 0)
            return new Integer[0];

        //Get the valid ids
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);
    
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
            throw new SystemException(this.ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /** <p>
     * Fetch the list of measurement ID's that have no data in the given time
     * range
     * </p>
     * @param current the current time
     * @param cycles the number of intervals to use as time buffer
     * @return the list of measurement IDs
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public Integer[] getIdsWithoutData(long current, int cycles)
        throws DataNotAvailableException {
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;
    
        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
            // DEBUG: connection tracking code
            increment(conn);

            // select id from EAM_MEASUREMENTR_DATA where enabled = true
            // and interval is not null and
            // and 0 = (SELECT COUNT(*) FROM EAM_MEASUREMENT_DATA WHERE
            // ID = measurement_id and timestamp > (105410357766 -3 * interval));
            stmt = conn.prepareStatement(
                "SELECT ID FROM " + TAB_MEAS +
                " WHERE enabled = ? AND NOT interval IS NULL AND " +
                      " NOT EXISTS (SELECT timestamp FROM " + TAB_DATA +
                                  " WHERE id = measurement_id AND " +
                                        " timestamp > (? - ? * interval))");
    
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
            throw new SystemException(this.ERR_DB, e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
            // DEBUG: connection tracking code
            decrement(conn);
        }
    }

    /**
     * Used for debugging leaking connections
     * 
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void leakConnection () throws SQLException, NamingException {
        DBUtil.getConnByContext(getInitialContext(), DATASOURCE_NAME);
    }

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.SessionBean#ejbCreate()
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {}

    /**
     * @see javax.ejb.SessionBean#ejbPostCreate()
     */
    public void ejbPostCreate() {}

    /**
     * @see javax.ejb.SessionBean#ejbActivate()
     */
    public void ejbActivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbPassivate()
     */
    public void ejbPassivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbRemove()
     */
    public void ejbRemove() {
        this.ctx = null;
    }

    /**
     * @see javax.ejb.SessionBean#setSessionContext()
     */
    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }

    // DEBUG: connection tracking code
    private void increment (Connection conn) {
        // disable while this is broken
        // org.hyperic.hq.common.server.DBConnectionCounter.openConnection(conn);
    }
    private void decrement (Connection conn) {
        // disable while this is broken
        // org.hyperic.hq.common.server.DBConnectionCounter.closeConnection(conn);
    }

} // end DataManager
