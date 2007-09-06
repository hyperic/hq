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

import org.hibernate.MappingException;
import org.hyperic.hq.common.SystemException;
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
 * HQ's version of MySQL5InnoDBDialect to create pseudo sequences
 */
public class MySQL5InnoDBDialect
    extends org.hibernate.dialect.MySQL5InnoDBDialect
    implements HQDialect
{
    private static final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;
    private static final String logCtx = MySQL5InnoDBDialect.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);
    private static final String TAB_MEAS = MeasurementConstants.TAB_MEAS;
    private static final String TAB_DATA = MeasurementConstants.TAB_DATA;
    private static final int IND_MIN       = MeasurementConstants.IND_MIN;
    private static final int IND_AVG       = MeasurementConstants.IND_AVG;
    private static final int IND_MAX       = MeasurementConstants.IND_MAX;
    private static final int IND_CFG_COUNT = MeasurementConstants.IND_CFG_COUNT;

    /*
     * Database table and function to support sequences.  It is assumed that
     * the database has already been prepped by running the following SQL.

        CREATE TABLE `HQ_SEQUENCE` (
            `seq_name` char(50) NOT NULL PRIMARY KEY,
            `seq_val` int(11) DEFAULT NULL
        );
    
        DELIMITER |
        
        CREATE FUNCTION nextseqval (iname CHAR(50))
         RETURNS INT
         DETERMINISTIC
         BEGIN
          SET @new_seq_val = 0;
          UPDATE HQ_SEQUENCE set seq_val = @new_seq_val:=seq_val+1
           WHERE seq_name=iname;
          RETURN @new_seq_val;
         END;

        |
    
     */

    public String getOptimizeStmt(String table, int cost)
    {
        return "ANALYZE TABLE "+table.toUpperCase();
    }

    public String getDeleteJoinStmt(String deleteTable,
                                    String commonKey,
                                    String joinTables,
                                    String joinKeys,
                                    String condition,
                                    int limit)
    {
        String cond = (condition.matches("^\\s*$")) ? "" : " and "+condition;
        String limitCond = (limit <= 0) ? "" : " LIMIT "+limit;
        return ("DELETE FROM "+deleteTable+" WHERE EXISTS"+
               " (SELECT "+commonKey+" FROM "+joinTables+
               " WHERE "+joinKeys+cond+")").toUpperCase();
    }

    public boolean supportsSequences() {
        return true;
    }
    private final static String SEQUENCE_TABLE = "HQ_SEQUENCE";
    private final static String SEQUENCE_NAME  = "seq_name";
    private final static String SEQUENCE_VALUE = "seq_val";

    protected String getCreateSequenceString(String sequenceName)
        throws MappingException {
        return "INSERT INTO " + SEQUENCE_TABLE +
               " (" + SEQUENCE_NAME + "," + SEQUENCE_VALUE + ") VALUES ('" +
               sequenceName + "', " + HypericDialectConstants.SEQUENCE_START +
               ")";
    }

    protected String getDropSequenceString(String sequenceName)
        throws MappingException {
        return "DELETE FROM " + SEQUENCE_TABLE + " WHERE " +
               SEQUENCE_NAME + " = '" + sequenceName + "'";
    }

    public String getSequenceNextValString(String sequenceName)
        throws MappingException {
        return "SELECT " + getSelectSequenceNextValString(sequenceName);
    }

    public String getSelectSequenceNextValString(String sequenceName)
        throws MappingException {
        return "nextseqval('" + sequenceName + "')";
    }

    public String getQuerySequencesString() {
        return "SELECT " + SEQUENCE_TABLE + " FROM " + SEQUENCE_TABLE;
    }

    public boolean supportsMultiInsertStmt() {
        return true;
    }

    public boolean viewExists(Statement stmt, String viewName)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            //no need to lower case here
            String sql = "SHOW TABLES";
            rs = stmt.executeQuery(sql);
            while (rs.next())
            {
                String objName = rs.getString(1);
                if (objName.equalsIgnoreCase(viewName))
                    return true;
            }
            return false;
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    private Map getMeasIds(Connection conn, Map lastMap, Integer[] iids)
        throws SQLException
    {
        StringBuffer iidsConj = new StringBuffer(
                DBUtil.composeConjunctions("instance_id", iids.length));
        DBUtil.replacePlaceHolders(iidsConj, iids);
        Map rtn = new HashMap();
        PreparedStatement pstmt = null;
        ResultSet rs            = null;
        try
        {
            String sql = "SELECT id FROM " + TAB_MEAS +
                         " WHERE template_id = ? AND " + iidsConj;
            pstmt = conn.prepareStatement(sql);
            for (Iterator it = lastMap.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                Integer tid = (Integer) entry.getKey();
                Long lastTime = (Long) entry.getValue();

                // Reset the index
                int ind = 1;
                pstmt.setInt(ind++, tid.intValue());

                rs = pstmt.executeQuery();
                List list = new ArrayList(); 
                while (rs.next()) {
                    list.add(new Integer(rs.getInt(1)));
                }
                LongListObj longListObj;
                if (null == (longListObj = (LongListObj)rtn.get(tid))) {
                    rtn.put(tid, new LongListObj(lastTime, list));
                } else {
                    List timelist = longListObj.getList();
                    timelist.addAll(list);
                }
            }
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, pstmt);
        }
        return rtn;
    }

    public Map getLastData(Connection conn, String minMax,
                           Map resMap, Map lastMap, Integer[] iids,
                           long begin, long end, String table)
        throws SQLException {

        ResultSet rs    = null;
        Statement stmt  = null;
        StopWatch timer = new StopWatch();

        try {
            Map timeMeasIDMap = getMeasIds(conn, lastMap, iids);
            stmt = conn.createStatement();

            for (Iterator it = timeMeasIDMap.entrySet().iterator();
                it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                Integer tid = (Integer) entry.getKey();
                LongListObj obj = (LongListObj) entry.getValue();
                Long lastTime = obj.getLong();
                Object[] measIds = obj.getList().toArray();
                if (table.endsWith(TAB_DATA))
                {
                    table = MeasTabManagerUtil.getUnionStatement(measIds,
                                                    lastTime.longValue());
                }

                String sql = "SELECT value FROM " + table +
                             " WHERE timestamp = " + lastTime;
                if (_log.isTraceEnabled()) {
                    _log.trace("getAggregateData() for measids=" + measIds +
                               " lastTime=" + lastTime + ": " + sql);
                }
                rs = stmt.executeQuery(sql);

                if (rs.next())
                {
                    // Get the double[] value from results
                    double[] data = (double[]) resMap.get(tid);
                    // Now set the the last reported value
                    data[IND_LAST_TIME] = rs.getDouble(1);
                }

            }
            if (_log.isTraceEnabled()) {
                _log.trace("getAggregateData(): Statement query elapsed " +
                          "time: " + timer.reset());
            }
        }
        finally {
            // Close ResultSet
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, stmt);
        }
        return resMap;
    }

    private class LongListObj
    {
        private Long longVal;
        private List listVal;
        LongListObj(Long longVal, List listVal) {
            this.longVal = longVal;
            this.listVal = listVal;
        }
        List getList() {
            return listVal;
        }
        Long getLong() {
            return longVal;
        }
    }

    private List getMeasIds(Connection conn, Integer[] tids, Integer[] iids)
        throws SQLException
    {
        List rtn = new ArrayList();
        StringBuffer iidsConj = new StringBuffer(
                DBUtil.composeConjunctions("instance_id", iids.length));
        DBUtil.replacePlaceHolders(iidsConj, iids);
        StringBuffer tidsConj = new StringBuffer(
                DBUtil.composeConjunctions("template_id", tids.length));
        DBUtil.replacePlaceHolders(tidsConj, tids);
        final String sql = "SELECT distinct id FROM " + TAB_MEAS +
                           " WHERE " + iidsConj + " AND " + tidsConj;
        Statement stmt = null;
        ResultSet rs   = null;
        try
        {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                rtn.add(new Integer(rs.getInt(1)));
            }
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, stmt);
        }
        return rtn;
    }

    public Map getAggData(Connection conn, String minMax, Map resMap,
                          Integer[] tids, Integer[] iids,
                          long begin, long end, String table)
        throws SQLException
    {
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

        if (table.endsWith(TAB_DATA))
        {
            List measIds = getMeasIds(conn, tids, iids);
            table = MeasTabManagerUtil.getUnionStatement(begin, end,
                                                measIds.toArray());
            //if there are 0 measurement ids there is no need to go forward
            if (measIds.size() == 0)
                return lastMap;
        }

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
}
