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

package org.hyperic.hibernate.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Dialect;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;

public class HQDialectUtil {
    
    private static final String logCtx = HQDialectUtil.class.getName();
    private static final Log _log = LogFactory.getLog(logCtx);
    private static final String TAB_MEAS = MeasurementConstants.TAB_MEAS;
    private static final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;
    private static final int IND_MIN       = MeasurementConstants.IND_MIN;
    private static final int IND_AVG       = MeasurementConstants.IND_AVG;
    private static final int IND_MAX       = MeasurementConstants.IND_MAX;
    private static final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;
    
    /**
     * Utility class should have a private constructor.
     */
    private HQDialectUtil() {
    }
    
    
    public static Dialect getDialect(Connection conn) throws SQLException {
        int t = DBUtil.getDBType(conn);
        
        if (DBUtil.isMySQL(t)) {
            return new MySQL5InnoDBDialect();
        } else if(DBUtil.isPostgreSQL(t)) {
            return new PostgreSQLDialect();
        } else if (DBUtil.isOracle(t)) {
            return new Oracle9Dialect();
        } else {
            throw new IllegalArgumentException("Unsupported DB");
        }
    }
    
    public static HQDialect getHQDialect(Connection conn) throws SQLException {
        return (HQDialect)HQDialectUtil.getDialect(conn);
    }
    
    public static Map getAggData(Connection conn, String minMax, Map resMap,
                                 long begin, long end, String table)
        throws SQLException {
        // Keep track of the "last" reported time
        HashMap lastMap = new HashMap();

        ResultSet rs            = null;
        PreparedStatement astmt = null;
        StopWatch timer         = new StopWatch();
        
        final String aggregateSQL =
            "SELECT template_id," + minMax + " MAX(timestamp) " +
            " FROM " + table + "," + TAB_MEAS +
            " WHERE timestamp BETWEEN ? AND ? AND measurement_id = id " +
            " GROUP BY template_id";
        
        try {
            // Prepare aggregate SQL
            astmt = conn.prepareStatement(aggregateSQL);
            // First set the time range
            int ind = 1;
            astmt.setLong(ind++, begin);
            astmt.setLong(ind++, end);

            if (_log.isTraceEnabled())
                _log.trace("getAggregateData() for begin=" + begin +
                           " end = " + end + ": " + aggregateSQL);
            timer.markTimeBegin("aggregateSQL");
            // First get the min, max, average
            rs = astmt.executeQuery();
            while (rs.next()) {
                Integer tid = new Integer(rs.getInt(1));
                // data[0] = min, data[1] = avg, data[2] = max,
                // data[3] = last, data[4] = count of measurement ID's
                double[] data = (double[]) resMap.get(tid);
                if (data == null) {
                    data = new double[IND_LAST_TIME + 1];
                    resMap.put(tid, data);
                }
                
                data[IND_MIN] = rs.getDouble(2);
                data[IND_AVG] = rs.getDouble(3);
                data[IND_MAX] = rs.getDouble(4);
                // Put it into the last map
                lastMap.put(tid, new Long(rs.getLong(5)));
            }
            timer.markTimeEnd("aggregateSQL");
            if (_log.isDebugEnabled())
                _log.debug("getAggData(): Statement query elapsed: " + timer);
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, astmt);
        }
        return lastMap;
    }

    public static Map getCountData(Connection conn, String minMax, Map resMap,
                                   long begin, long end, String table)
        throws SQLException {
        ResultSet rs            = null;
        PreparedStatement astmt = null;
        StopWatch timer         = new StopWatch();
    
        final String countSQL =
            "SELECT template_id, COUNT(id) FROM " + TAB_MEAS +
            " WHERE EXISTS (SELECT 1 FROM " + table  +
            " WHERE timestamp BETWEEN ? AND ? AND measurement_id = id) " +
            " GROUP BY template_id";
        try {
            astmt = conn.prepareStatement(countSQL);
            int ind = 1;
            astmt.setLong(ind++, begin);
            astmt.setLong(ind++, end);
            timer.markTimeBegin("countSQL");
            rs = astmt.executeQuery();
            while (rs.next()) {
                Integer tid = new Integer(rs.getInt(1));
                int count = rs.getInt(2);
                if (count > 0) {
                    // Put it into the result map
                    double[] data = (double[]) resMap.get(tid);
                    if (data == null) {
                        data = new double[IND_LAST_TIME + 1];
                        resMap.put(tid, data);
                    }
                    data[IND_CFG_COUNT] = count;
                }
                else {
                    resMap.remove(tid);
                }
            }
            timer.markTimeEnd("countSQL");
    
            if (_log.isDebugEnabled())
                _log.debug("countSQL(): Statement query elapsed: " + timer);
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, astmt);
        }
        return resMap;
    }


    public static Map getLastData(Connection conn, String minMax,
                                  Map resMap, Map lastMap, Integer[] iids,
                                  long begin, long end, String table)
        throws SQLException {

        ResultSet rs            = null;
        PreparedStatement lstmt = null;
        StopWatch timer         = new StopWatch();

        StringBuffer iidsConj = new StringBuffer(
                DBUtil.composeConjunctions("instance_id", iids.length));
        DBUtil.replacePlaceHolders(iidsConj, iids);

        final String lastSQL =
            "SELECT value FROM " + table + ", " + TAB_MEAS +
            " WHERE template_id = ? AND timestamp = ? AND id = measurement_id";

        for (Iterator it = lastMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();

            Integer tid = (Integer) entry.getKey();
            Long lastTime = (Long) entry.getValue();

            // Now get the last timestamp
            if (_log.isDebugEnabled()) {
                _log.debug("getAggregateData() for tid=" + tid + " lastTime=" +
                           lastTime + ": " + lastSQL);
            }

            try {
                // Prepare last value SQL
                lstmt = conn.prepareStatement(lastSQL);

                // Reset the index
                int ind = 1;
                lstmt.setInt(ind++, tid.intValue());
                lstmt.setLong(ind++, lastTime.longValue());

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
                DBUtil.closeStatement(logCtx, lstmt);
            }
            if (_log.isDebugEnabled()) {
                _log.debug("getAggregateData(): Statement query elapsed " +
                           "time: " + timer);
            }
        }
        return resMap;
    }  

}
