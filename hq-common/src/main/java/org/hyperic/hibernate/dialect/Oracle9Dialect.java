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

package org.hyperic.hibernate.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

import org.hyperic.util.jdbc.DBUtil;

/**
 * HQ customized Oracle dialect to (re)define default
 * JDBC sql types to native db column type mapping
 * for backwards compatibility, :(
 * 
 * This class must be public for Hibernate to access it.
 */
public class Oracle9Dialect 
    extends org.hibernate.dialect.Oracle9Dialect
    implements HQDialect
{
    private static final String logCtx = Oracle9Dialect.class.getName();

    public Oracle9Dialect() {
        registerColumnType(Types.VARBINARY, 2000, "blob");
    }
    
    public boolean supportsDeferrableConstraints() {
        return true;
    }

	public String getCreateSequenceString(String sequenceName) {
        return new StringBuffer()
            .append("create sequence ")
            .append(sequenceName)
            .append(" start with ")
            .append(HypericDialectConstants.SEQUENCE_START)
            .append(" increment by 1 ")
            .append(" cache 100 ")
            .toString();
    }

    public String getOptimizeStmt(String table, int tablePercent)
    {
        return "ANALYZE TABLE "+table+
               ((tablePercent <= 0 || tablePercent > 100) ? 
                   " COMPUTE STATISTICS" :
                   " ESTIMATE STATISTICS SAMPLE "+tablePercent+" PERCENT");
    }

    public boolean supportsDuplicateInsertStmt() {
        return false;
    }

    public boolean supportsMultiInsertStmt()
    {
        return false;
    }

    public boolean tableExists(Statement stmt, String tableName)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            String sql = "SELECT table_name from all_tables"+
                         " WHERE lower(table_name) = lower('"+tableName+"')";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                return true;
            return false;
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    public String getLimitString(int num) {
        return "AND ROWNUM <= "+num;
    }

    public boolean viewExists(Statement stmt, String viewName)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            String sql = "SELECT view_name from all_views"+
                         " WHERE lower(view_name) = lower('"+viewName+"')";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                return true;
            return false;
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    public boolean usesSequenceGenerator() {
        return true;
    }

    public String getRegExSQL(String column, String regex, boolean ignoreCase,
                              boolean invertMatch) {
        return new StringBuffer()
            .append((invertMatch) ? "NOT " : "")
            .append("REGEXP_LIKE(")
            .append(column)
            .append(", ").append(regex)
            .append((ignoreCase) ? ", 'i')" : ")")
            .toString();
    }

    public boolean useEamNumbers() {
        return true;
    }

    public int getMaxExpressions() {
        // oracle limit is 1000, but leave room for others
        return 900;
    }

    public boolean supportsPLSQL() {
        return true;
    }

    public boolean useMetricUnion() {
        return false;
    }

    public String getLimitBuf(String sql, int offset, int limit) {
        sql = sql.trim();
        final boolean hasOffset = offset > 0;
        boolean isForUpdate = false;
        if ( sql.toLowerCase().endsWith(" for update") ) {
            sql = sql.substring( 0, sql.length()-11 );
            isForUpdate = true;
        }
        final StringBuilder rtn = new StringBuilder();
        if (hasOffset) {
            rtn.append(
                "select * from ( select row_.*, rownum rownum_ from ( ");
        } else {
            rtn.append("select * from ( ");
        }
        rtn.append(sql);
        if (hasOffset) {
            rtn.append(" ) row_ where rownum <= ")
               .append(limit).append(") where rownum_ > ")
               .append(offset);
        } else {
            rtn.append(" ) where rownum <= ").append(limit);
        }
        if ( isForUpdate ) {
            rtn.append( " for update" );
        }
        return rtn.toString();
    }
    
    public String getMetricDataHint() {
        return "";
    }

	public Long getSchemaCreationTimestampInMillis(Statement stmt)
			throws SQLException {
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
        	String sql = "select min(created) from user_objects where object_name like 'EAM_%'";
        	
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
