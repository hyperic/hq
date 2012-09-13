/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2007], Hyperic, Inc. 
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
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasRangeObj;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;

/**
 * HQ's version of MySQL5InnoDBDialect to create pseudo sequences.
 * 
 * This class must be public for Hibernate to access it.
 */
public class MySQL5InnoDBDialect
    extends org.hibernate.dialect.MySQL5InnoDBDialect
    implements HQDialect
{
    private static final String logCtx = MySQL5InnoDBDialect.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);
    private static final String TAB_MEAS   = MeasurementConstants.TAB_MEAS;
    private static final String TAB_DATA   = MeasurementConstants.TAB_DATA;
    private static final int IND_LAST_TIME = MeasurementConstants.IND_LAST_TIME;

    public MySQL5InnoDBDialect() {
        super();
        registerColumnType(Types.VARBINARY, 255, "blob");
    }

    public boolean supportsIdentityColumns() {
        return false;
    }
    
    public boolean supportsInsertSelectIdentity() {
        return false;
    }
    
    public String getOptimizeStmt(String table, int cost) {
        return "ANALYZE TABLE "+table.toUpperCase();
    }

    public boolean supportsDuplicateInsertStmt() {
        return true;
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
            String sql = "SELECT table_name from information_schema.views"+
                         " WHERE table_name = '"+viewName+"'"+
                         " AND table_schema = database()";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                return true;
            return false;
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    public boolean tableExists(Statement stmt, String tableName)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            String sql = "SELECT table_name from information_schema.tables"+
                         " WHERE table_name = '"+tableName+"'"+
                         " AND table_schema = database()";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                return true;
            return false;
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    public String getLimitBuf(String sql, int offset, int limit) {
        StringBuilder buf = new StringBuilder(sql);
        buf.append(" LIMIT ");
        if (offset > 0) {
            buf.append(offset).append(',');
        }
        buf.append(limit);
        return buf.toString();
    }

    public String getLimitString(int num) {
        return "LIMIT "+num;
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
    

    /**
     * Get the UNION statement from the detailed measurement tables based on
     * the beginning of the time range.
     * @param begin The beginning of the time range.
     * @param end The end of the time range
     * @param measId The array of measurement ids to set the where clause against
     * @return The UNION SQL statement.
     */
    private String getUnionStatement(Integer[] measIds, long timestamp) {
        StringBuilder sql = new StringBuilder();
        String measInStmt = getMeasInStmt(measIds, true);
        sql.append("(SELECT * FROM ").
            append(MeasRangeObj.getInstance().getTable(timestamp)).
            append(" WHERE timestamp = ").append(timestamp).
            append(measInStmt).
            append(") ").append(TAB_DATA);
        return sql.toString();
    }
    
    private String getMeasInStmt(Integer[] measIds, boolean prependAnd) {
        if (measIds.length == 0) {
            return "";
        }
        StringBuilder rtn = new StringBuilder();
        rtn.append(" "+((prependAnd) ? "AND" : "")+" measurement_id");
        // mysql gets a perf boost from using "=" as apposed to "in"
        if (measIds.length == 1) {
            rtn.append(" = "+measIds[0]);
            return rtn.toString();
        }
        rtn.append(" in (");
        for (int i=0; i<measIds.length; i++) {
            if (measIds[i] == null) {
                continue;
            }
            rtn.append(measIds[i]+",");
        }
        rtn.deleteCharAt(rtn.length()-1);
        rtn.append(")");
        return rtn.toString();
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
                Integer[] measIds =
                    (Integer[])obj.getList().toArray(new Integer[0]);
                if (table.endsWith(TAB_DATA))
                {
                    table = getUnionStatement(measIds,
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

    public String getAddForeignKeyConstraintString(String constraintName,
                                                   String[] foreignKey,
                                                   String referencedTable,
                                                   String[] primaryKey,
                                                   boolean referencesPrimaryKey)
    {
        String cols = StringUtil.implode(Arrays.asList(foreignKey), ", ");
        return new StringBuffer(64)
            .append(" add constraint ")
            .append(constraintName)
            .append(" foreign key (")
            .append(cols)
            .append(") references ")
            .append(referencedTable)
            .append(" (")
            .append( StringUtil.implode(Arrays.asList(primaryKey), ", ") )
            .append(')')
            .toString();
    }

    public boolean usesSequenceGenerator() {
        return false;
    }

    public String getRegExSQL(String column, String regex, boolean ignoreCase,
                              boolean invertMatch) {
        if (ignoreCase) {
            return new StringBuilder()
                .append("lower(").append(column).append(")")
                .append((invertMatch) ? " NOT " : " ")
                .append("REGEXP ")
                .append("lower(").append(regex).append(")")
                .toString();
        } else {
            return new StringBuilder()
                .append(column)
                .append((invertMatch) ? " NOT " : " ")
                .append("REGEXP ")
                .append(regex)
                .toString();
        }
    }

    public boolean useEamNumbers() {
        return false;
    }

    public int getMaxExpressions() {
        return -1;
    }

    public boolean supportsPLSQL() {
        return false;
    }

    public boolean useMetricUnion() {
        return false;
    }
    
    public String getMetricDataHint() {
        return "USE INDEX (PRIMARY)";
    }

	public Long getSchemaCreationTimestampInMillis(Statement stmt) throws SQLException {
        ResultSet rs = null;
        Date installDate = null;
    	
        try {
        	String[] sqls = new String[] {
        		"select CTIME from EAM_AGENT_TYPE where ID = 1",
        		"select CTIME from EAM_APPLICATION_TYPE where ID = 2",
        		"select CTIME from EAM_RESOURCE_GROUP where ID = 0",
        		"select CTIME from EAM_ALERT_DEFINITION where ID = 0",
        		"select CTIME from EAM_ESCALATION where ID = 100"
        	};
            
        	for (String sql : sqls) {
        		rs = stmt.executeQuery(sql);
            
	        	if (rs.next()) {
	        		Date date = new Date(rs.getLong(1));
	        		
	        		if (installDate == null) {
	        			installDate = date;
	        		} else {
	        			Calendar cal1 = Calendar.getInstance();
	        			Calendar cal2 = Calendar.getInstance();
	        			
	        			cal1.setTime(installDate);
	        			cal2.setTime(date);
	        			
	        			// Compare date with previous one (they should all be the same date)...
	        			if (cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) || cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR)) {
	        				// ...Something has been tampered with!...
	        				return null;
	        			}
	        		}
	        	}
        	}
        	
        	// Extra insurance, check the db schema creation timstamp...
        	String sql = "select min(CREATE_TIME) from information_schema.tables where table_schema = database()";
        	
        	rs = stmt.executeQuery(sql);
        	
        	if (rs.next()) {
        		Date date = rs.getDate(1);
        		Calendar cal1 = Calendar.getInstance();
    			Calendar cal2 = Calendar.getInstance();
    			
    			cal1.setTime(installDate);
    			cal2.setTime(date);
    			
    			// Compare date with previous one (they should all be the same date)...
    			if (cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) || cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR)) {
    				// ...Something has been tampered with!...
    				return null;
    			}
        	}
        } finally {
        	DBUtil.closeResultSet(logCtx, rs);
        }
        
        return installDate.getTime();
	}

    public boolean analyzeDb() {
        return true;
    }

    public boolean supportsAsyncCommit() {
        return false;
    }

    public String getSetAsyncCommitStmt(boolean on) {
        return null;
    }
}