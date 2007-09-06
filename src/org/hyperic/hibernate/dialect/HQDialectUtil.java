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

package org.hyperic.hibernate.dialect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HQDialectUtil
{
    private final String logCtx = MySQL5InnoDBDialect.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);
    private static final String TAB_MEAS = MeasurementConstants.TAB_MEAS;
    private static final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;
    private static final int IND_MIN       = MeasurementConstants.IND_MIN;
    private static final int IND_AVG       = MeasurementConstants.IND_AVG;
    private static final int IND_MAX       = MeasurementConstants.IND_MAX;
    private static final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;

    public HQDialectUtil() {
    }

    public Map getAggData(Connection conn, String minMax, Map resMap,
                          Integer[] tids, Integer[] iids,
                          long begin, long end, String table)
        throws SQLException
    {
        // Keep track of the "last" reported time
        HashMap lastMap = new HashMap();

        ResultSet rs            = null;
        PreparedStatement astmt = null;
        StopWatch timer         = new StopWatch();

        StringBuffer iidsConj = new StringBuffer(
                DBUtil.composeConjunctions("instance_id", iids.length));
        DBUtil.replacePlaceHolders(iidsConj, iids);

        StringBuffer tidsConj = new StringBuffer(
                DBUtil.composeConjunctions("template_id", tids.length));
        DBUtil.replacePlaceHolders(tidsConj, tids);
        final String aggregateSQL =
            "SELECT COUNT(DISTINCT id)," + minMax +
                   "MAX(timestamp), template_id " +
            " FROM " + table + "," + TAB_MEAS +
            " WHERE timestamp BETWEEN ? AND ? AND measurement_id = id AND " +
                    iidsConj + " AND " + tidsConj + " GROUP BY template_id";
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
            // First get the min, max, average
            rs = astmt.executeQuery();
            while (rs.next()) {
                Integer tid = new Integer(rs.getInt("template_id"));
                double[] data =
                    new double[IND_LAST_TIME + 1];
                // data[0] = min, data[1] = avg, data[2] = max,
                // data[3] = last, data[4] = count of measurement ID's
                data[IND_CFG_COUNT] = rs.getInt(1);
                // If there are no metrics, then forget it
                if (data[IND_CFG_COUNT] == 0)
                    continue;
                data[IND_MIN] = rs.getDouble(2);
                data[IND_AVG] = rs.getDouble(3);
                data[IND_MAX] = rs.getDouble(4);
                // Put it into the result map
                resMap.put(tid, data);
                // Get the time
                Long lastTime = new Long(rs.getLong(5));
                // Put it into the last map
                lastMap.put(tid, lastTime);
            }
            if (_log.isTraceEnabled())
                _log.trace("getAggregateData(): Statement query elapsed: " +
                          timer.reset());
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, astmt);
        }
        return lastMap;
    }

    public Map getLastData(Connection conn, String minMax,
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
            "SELECT value FROM " + table + ", " +
                "(SELECT id FROM " + TAB_MEAS +
                    " WHERE template_id = ? AND " + iidsConj + ") ids " +
            "WHERE id = measurement_id AND timestamp = ?";

        for (Iterator it = lastMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();

            Integer tid = (Integer) entry.getKey();
            Long lastTime = (Long) entry.getValue();

            // Now get the last timestamp
            if (_log.isTraceEnabled()) {
                _log.trace("getAggregateData() for tid=" + tid +
                          " lastTime=" + lastTime + ": " + lastSQL);
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
            if (_log.isTraceEnabled()) {
                _log.trace("getAggregateData(): Statement query elapsed " +
                          "time: " + timer.reset());
            }
        }
        return resMap;
    }
}
