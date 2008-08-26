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

package org.hyperic.hq.authz.server.session;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.hyperic.hq.common.SQLDiagnostic;
import org.hyperic.hq.common.SQLDiagnosticsFactory;
import org.hyperic.util.jdbc.DBUtil;

public class OrphanedAlertdefSQLDiagnosticsFactory implements SQLDiagnosticsFactory {

    private static final String SELECT_QUERY = "SELECT * ";
    
    private static final String UPDATE_QUERY_POSTGRES = "UPDATE EAM_ALERT_DEFINITION " +
        "SET DELETED = TRUE, PARENT_ID = NULL ";
    
    private static final String UPDATE_QUERY_MYSQL = "UPDATE EAM_ALERT_DEFINITION " +
    "SET DELETED = 1, PARENT_ID = NULL ";
    
    private static final String ORPHANED_ALERTDEF_QUERY = 
        "FROM EAM_ALERT_DEFINITION " +
        "WHERE RESOURCE_ID IS NULL " +
        "AND PARENT_ID IS NOT NULL " +
        "AND APPDEF_ID IS NOT NULL " +
        "AND NOT PARENT_ID = 0";
    
    private Connection connection;
    
    private OrphanedAlertdefSQLDiagnosticsFactory(Connection c) {
        connection = c;
    }
    
    public static SQLDiagnosticsFactory getInstance(Connection c) {
        return new OrphanedAlertdefSQLDiagnosticsFactory(c);
    }
    
    public String getName() {
        return "Orphaned Alert Definition Diagnostics";
    }

    public Iterator getSQLDiagnostics() {
        return new OprhanedAlertdefSQLDiagnostics();
    }
    
    private class OprhanedAlertdefSQLDiagnostics implements Iterator {
        
        private int pos = 0;

        public boolean hasNext() {
            return (pos <  1);
        }

        public Object next() {
            String sqlQuery = SELECT_QUERY + ORPHANED_ALERTDEF_QUERY;
            boolean isPostgresSQL = true;
            try {
                isPostgresSQL = DBUtil.isPostgreSQL(connection);
            }
            catch (SQLException e) {
                // swallow -- assume we are running postgres
            }
            String sqlFix;
            if (isPostgresSQL) {
                sqlFix = UPDATE_QUERY_POSTGRES + ORPHANED_ALERTDEF_QUERY;
            }
            else {
                sqlFix = UPDATE_QUERY_MYSQL + ORPHANED_ALERTDEF_QUERY;
            }
            pos++;
            return new SQLDiagnostic(sqlQuery, sqlFix);
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported");
            
        }
        
    }
}
