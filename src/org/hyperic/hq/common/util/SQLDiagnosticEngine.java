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

package org.hyperic.hq.common.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SQLDiagnosticsFactory;
import org.hyperic.util.jdbc.DBUtil;

public class SQLDiagnosticEngine {

    private List diagnostics = new ArrayList();
    private Connection connection;
    
    private static Log _log =
        LogFactory.getLog(SQLDiagnosticEngine.class);
    
    private static final String SELECT_COUNT_SQL = "SELECT COUNT(*) FROM (";
    
    private static final String SELECT_COUNT_ALIAS_SQL = ") INNERSELECT";
      
    private final String logCtx =
        "org.hyperic.hq.common.util.SQLDiagnosticEngine";
    
    private SQLDiagnosticEngine(Connection conn) {
        connection = conn;
    }
    
    private int errorCount = 0;
    private List fixSql = new ArrayList();
    
    
    public static SQLDiagnosticEngine getInstance(Connection conn) {
        return new SQLDiagnosticEngine(conn);
    }
    
    /**
     * Executes the test SQL for the given diagnosticFactory. Each new invocation of
     * execute resets the internal error count and list of SQL fixes returned by the
     * getErrorCount and getFixSql methods respectively.
     * 
     * @param diagnosticFactory the SQLDiagnosticsFactory used to iterate through
     * the list of test SQLs
     * @throws SQLException 
     */
    public void execute(SQLDiagnosticsFactory diagnosticFactory) throws SQLException {
        errorCount = 0;
        fixSql = new ArrayList();
        List testSql = diagnosticFactory.getTestQueries();
        
        for (Iterator it = testSql.iterator(); it.hasNext();) {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            String query = (String) it.next();
            
            final String sql = SELECT_COUNT_SQL + query
                    + SELECT_COUNT_ALIAS_SQL;
            try {
                stmt = connection.prepareStatement(sql);
                if (_log.isDebugEnabled()) {
                    _log.debug("Executing query: " + sql);
                }
                rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt(1);
                    errorCount += count;
                }
            }
            catch (SQLException e) {
                _log.error("Failed to execute statement " + sql, e);
            }
            finally {
                DBUtil.closeResultSet(logCtx, rs);
                DBUtil.closeStatement(logCtx, stmt);
            }
        }
    }
    
    /**
     * Returns the number of SQL rows that matched the test SQL criteria provided in the
     * last invocation of the execute method.
     * 
     * @return the number of matching SQL rows
     */
    public int getMatchCount() {
        return errorCount;
    }

}
