/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc. This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify it under the terms
 * version 2 of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.authz.server.session;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.SQLDiagnosticsFactory;
import org.hyperic.util.jdbc.DBUtil;

import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.util.SQLDiagnosticEngine;

/**
 * A diagnostic object for event tracker operations.
 */
class ResourceDiagnostic implements DiagnosticObject {

    private static final ResourceDiagnostic INSTANCE = new ResourceDiagnostic();
    private static InitialContext _ic = null;
    private final String logCtx =
        "org.hyperic.hq.authz.server.session.ResourceDiagnostic";
    
    private static Log _log =
        LogFactory.getLog(ResourceDiagnostic.class);
    /**
     * @return The singleton instance.
     */
    public static ResourceDiagnostic getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor for a singleton.
     */
    private ResourceDiagnostic() {
    }

    /**
     * @see org.hyperic.hq.common.DiagnosticObject#getName()
     */
    public String getName() {
        return "Data Integrity";
    }

    /**
     * @see org.hyperic.hq.common.DiagnosticObject#getShortName()
     */
    public String getShortName() {
        return "DataIntegrity";
    }

    public String getStatus() {
        Connection conn = null;
        StringBuffer rslt = new StringBuffer();
        try {
            conn = getConnection();
            SQLDiagnosticEngine engine = SQLDiagnosticEngine.getInstance(conn);
            rslt.append(getDetailedStatus(
                OrphanedResourceSQLDiagnosticsFactory.getInstance(conn),
                conn, engine));
            rslt.append(getDetailedStatus(
                OrphanedAlertdefSQLDiagnosticsFactory.getInstance(conn),
                conn, engine));
            rslt.append(getDetailedStatus(
                TypeAlertdefTriggersSQLDiagnosticsFactory.getInstance(conn),
                conn, engine));
        } catch (SQLException e) {
            _log.error("Failed to get SQL diagnostics: ", e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
        return rslt.toString();
    }
    
    private String getDetailedStatus(SQLDiagnosticsFactory factory,
            Connection c, SQLDiagnosticEngine engine) throws SQLException {

        engine.execute(factory);
        int count = engine.getMatchCount();
        StringBuffer fix = new StringBuffer();
        if (count == 0) {
            fix.append("N/A\n");
        }
        else {
            for (Iterator it = factory.getFixQueries().iterator(); it.hasNext();) {
                fix.append((String) it.next() + ";\n");
            }
        }

        StringBuffer rslt = new StringBuffer();
        rslt.append(factory.getName() + ":\n").append(
                count + " resources matched the criteria.\n\n").append(
                "Suggested fix:\n" + fix.toString() + "\n");

        return rslt.toString();
    }
    
    private Connection getConnection() throws SQLException {
        try {
            return DBUtil.getConnByContext(getInitialContext(), HQConstants.DATASOURCE);            
        } catch (NamingException e) {
            throw new SQLException("Failed to retrieve datasource: "+e);
        }
    }
    
    private InitialContext getInitialContext() throws NamingException {
        if (_ic == null)
            _ic = new InitialContext();
        return _ic;
    }

}