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

import org.hyperic.util.jdbc.DBUtil;
import java.util.Map;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * This class must be public for Hibernate to access it.
 */
public class PostgreSQLDialect 
    extends org.hibernate.dialect.PostgreSQLDialect
    implements HQDialect
{
    private static final String logCtx = PostgreSQLDialect.class.getName();

    public String getCascadeConstraintsString() {
        return " cascade ";
    }

    public boolean dropConstraints() {
        return false;
    }

	public String getCreateSequenceString(String sequenceName) {
        return new StringBuffer()
            .append("create sequence ")
            .append(sequenceName)
            .append(" start ")
            .append(HypericDialectConstants.SEQUENCE_START)
            .append(" increment 1 ")
            .toString();
    }

    public String getOptimizeStmt(String table, int cost)
    {
        return "ANALYZE "+table;
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
            String sql = "SELECT tablename from pg_tables"+
                         " WHERE lower(tablename) = lower('"+tableName+"')";
            rs = stmt.executeQuery(sql);
            if (rs.next())
                return true;
            return false;
        }
        finally {
            DBUtil.closeResultSet(logCtx, rs);
        }
    }

    public boolean viewExists(Statement stmt, String viewName)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            String sql = "SELECT viewname from pg_views"+
                         " WHERE lower(viewname) = lower('"+viewName+"')";
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
        return "LIMIT "+num;
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
}
