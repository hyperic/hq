/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasRange;
import org.hyperic.hq.measurement.shared.MeasRangeObj;
import org.hyperic.util.jdbc.DBUtil;

public class SST_AvailRLEUpgrader extends SchemaSpecTask {

    public static final String logCtx = SST_AvailRLEUpgrader.class.getName();

    public static final String SCHEMA_MOD_IN_PROGRESS
        = " *** UPGRADE TASK: migrating availability data to" +
          " Run Length Encoded Availability";
    private static final List dataTables = new ArrayList();
    private static final String TAB_MEAS = MeasurementConstants.TAB_MEAS;
    private static final String AVAILABILITY =
        MeasurementConstants.CAT_AVAILABILITY.toUpperCase();
    private static final String TAB_MEAS_TEMPL = "EAM_MEASUREMENT_TEMPL";
    private static final String TAB_AVAIL_RLE = "HQ_AVAIL_DATA_RLE";
    private static final long ONE_HR = 1000*3600;
    private static final long SIX_HRS = 6*ONE_HR;
    private static final long ONE_DAY = 24*ONE_HR;
    private static final long MAX_TIMESTAMP = Long.MAX_VALUE;
    private static final long BATCH_SIZE = 1000;

    public SST_AvailRLEUpgrader () {}
    
    private void initDataTables() {
        // these must be ordered from the latest table to the oldest
        List ranges = MeasRangeObj.getInstance().getRanges();
        for (Iterator i=ranges.iterator(); i.hasNext(); ) {
            MeasRange range = (MeasRange)i.next();
            dataTables.add(new TableObj(range.getTable(), ONE_HR));
        }
        dataTables.add(
            new TableObj(MeasurementConstants.TAB_DATA_1H, ONE_HR*10));
        dataTables.add(
            new TableObj(MeasurementConstants.TAB_DATA_6H, SIX_HRS*10));
        dataTables.add(
            new TableObj(MeasurementConstants.TAB_DATA_1D, ONE_DAY*10));
    }

    public void execute() throws BuildException {
        initDataTables();
        Map avails = new HashMap();
        try
        {
            Connection conn = getConnection();
            log(SCHEMA_MOD_IN_PROGRESS);
            for (Iterator i=dataTables.iterator(); i.hasNext(); ) {
                TableObj table = (TableObj)i.next();
                setAvailData(avails, table, conn);
            }
            insertAvailData(avails, conn);
        }
        catch (SQLException e) {
            throw new BuildException(logCtx+": " + e.getMessage(), e);
        }
    }

    private void insertAvailData(Map avails, Connection conn)
        throws SQLException {
        String sql = "INSERT INTO " + TAB_AVAIL_RLE + " (measurement_id, " +
            "startime, endtime, availval) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        List debugList = new ArrayList((int)BATCH_SIZE);
        try {
            for (Iterator i=avails.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                int mid = ((Integer)entry.getKey()).intValue();
                //log("mid -> " + mid);
                List list = (List)entry.getValue();
                int ii=0;
                for (Iterator it=list.iterator(); it.hasNext(); ii++) {
                    if (0 == (ii % BATCH_SIZE) && ii != 0) {
                        int[] res = pstmt.executeBatch();
                        checkResult(res, debugList);
                        debugList.clear();
                        pstmt.clearBatch();
                    }
                    AvailData data = (AvailData)it.next();
                    //log("\tstartime -> " + data.getStartTime() + 
                    //" endtime -> " + data.getEndTime() +
                    //" availVal -> " + data.getAvailVal());
                    pstmt.clearParameters();
                    pstmt.setInt(1, mid);
                    pstmt.setLong(2, data.getStartTime());
                    pstmt.setLong(3, data.getEndTime());
                    pstmt.setDouble(4, data.getAvailVal());
                    debugList.add(data);
                    pstmt.addBatch();
                }
            }
            int[] res = pstmt.executeBatch();
            checkResult(res, debugList);
        } finally {
            DBUtil.closeStatement(logCtx, pstmt);
        }
    }
    
    private void checkResult(int[] res, List pts) {
        Iterator it = pts.iterator();
        for (int i=0; i<res.length; i++) {
            AvailData pt = (AvailData)it.next();
            if (res[i] == Statement.EXECUTE_FAILED) {
                log("ERROR inserting datapoint -> " + pt);
            }
        }
    }

    private long getMaxTimestamp(String table, Connection conn)
            throws SQLException {
        String sql = "SELECT max(timestamp) from " + table;
        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getLong(1);
            }
            return -1;
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
        }
    }

    private long getMinTimestamp(String table, Connection conn)
            throws SQLException {
        String sql = "SELECT min(timestamp) from " + table;
        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getLong(1);
            }
            return -1;
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
        }
    }

    private void setAvailData(Map avails, TableObj table, Connection conn)
        throws SQLException {
        long min = getMinTimestamp(table.getTable(), conn);
        long max = getMaxTimestamp(table.getTable(), conn);
        String sql = "SELECT timestamp, value, measurement_id" +
                     " FROM " + table.getTable() + " d, " +
                     TAB_MEAS + " m, " +
                     TAB_MEAS_TEMPL + " t" +
                     " WHERE d.timestamp between ? and ?" +
                     " AND d.measurement_id = m.id" +
                     " AND t.id = m.template_id AND upper(t.alias) = '" +
                     AVAILABILITY + "'" +
                     " ORDER BY d.timestamp, d.measurement_id";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = null;
        try {
            long interval = table.getInterval();
            for (long i=min; i<=max; i+=interval) {
                pstmt.setLong(1, i);
                pstmt.setLong(2, (i+interval));
                rs = pstmt.executeQuery();
                int timestamp_col = rs.findColumn("timestamp");
                int value_col     = rs.findColumn("value");
                int measId_col    = rs.findColumn("measurement_id");
                while (rs.next()) {
                    long timestamp = rs.getLong(timestamp_col);
                    double value   = rs.getDouble(value_col);
                    int measId     = rs.getInt(measId_col);
                    setAvail(avails, timestamp, value, measId);
                }
                DBUtil.closeResultSet(logCtx, rs);
            }
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, pstmt, rs);
        }
    }
    
    private void setAvail(Map avails, long timestamp, double value, int id) {
        List list;
        Integer mid = new Integer(id);
        if (null == (list = (List)avails.get(mid))) {
            AvailData data = new AvailData(timestamp, MAX_TIMESTAMP, id, value);
            list = new ArrayList();
            list.add(data);
            avails.put(mid, list);
        } else {
            AvailData last = (AvailData)list.get(list.size()-1);
            // the timestamps in the rollup tables may overlap the more recent
            // tables due to delays in deletion after compression.  If we see
            // overlapping just ignore the datapoint
            if (timestamp >= last.getStartTime()) {
                return;
            }
            if (value != last.getAvailVal()) {
                AvailData data =
                    new AvailData(timestamp, last.getStartTime(), id, value);
                list.add(data);
            } else {
                last.setStartTime(timestamp);
            }
        }
    }

    private class TableObj {
        private String _table;
        private long _interval;
        public TableObj(String table, long interval) {
            _table = table;
            _interval = interval;
        }
        public String getTable() {
            return _table;
        }
        public long getInterval() {
            return _interval;
        }
    }

    private class AvailData {
        long _startTime;
        long _endtime;
        int _metric_id;
        double _availval;
        
        public AvailData(long starttime, long endtime, int metric_id,
                         double availval) {
            super();
            _startTime = starttime;
            _endtime = endtime;
            _metric_id = metric_id;
            _availval = availval;
        }

        public long getStartTime() {
            return _startTime;
        }

        public void setStartTime(long startTime) {
            _startTime = startTime;
        }

        public long getEndTime() {
            return _endtime;
        }

        public int getMetric() {
            return _metric_id;
        }

        public double getAvailVal() {
            return _availval;
        }
    }
}
