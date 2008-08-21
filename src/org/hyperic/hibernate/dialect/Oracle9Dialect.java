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
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

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

	public String getCreateSequenceString(String sequenceName) {
        return new StringBuffer()
            .append("create sequence ")
            .append(sequenceName)
            .append(" start with ")
            .append(HypericDialectConstants.SEQUENCE_START)
            .append(" increment by 1 ")
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

    public Map getLastData(Connection conn, String minMax,
                           Map resMap, Map lastMap, Integer[] iids,
                           long begin, long end, String table) 
        throws SQLException
    {
        return HQDialectUtil.getLastData(conn, minMax, resMap, lastMap,
                                         iids, begin, end, table);
    }

    public Map getAggData(Connection conn, String minMax, Map resMap,
                          Integer[] tids, Integer[] iids,
                          long begin, long end, String table) 
        throws SQLException
    {   
        return HQDialectUtil.getAggData(conn, minMax, resMap, tids,
                                        iids, begin, end, table);
    }
    
    public boolean usesSequenceGenerator() {
        return true;
    }

    public boolean useEamNumbers() {
        return true;
    }
    
}
