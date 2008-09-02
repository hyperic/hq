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
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.common.SQLDiagnosticsFactory;

public class TypeAlertdefTriggersSQLDiagnosticsFactory
    implements SQLDiagnosticsFactory {

    private static final String SELECT_QUERY =
        "SELECT * FROM EAM_ALERT_DEFINITION ";
    
    private static final String RESOURCE_TYPE_ALERT_QUERY = 
        "WHERE PARENT_ID = 0 AND ACT_ON_TRIGGER_ID IS NOT NULL";
    
    private static final String ALERT_DEF_FIX = "UPDATE EAM_ALERT_DEFINITION " +
        "SET ACT_ON_TRIGGER_ID = NULL";
    
    private static final String ALERT_CONDITION_FIX =
        "UPDATE EAM_ALERT_CONDITION SET TRIGGER_ID = NULL " +
        "WHERE ALERT_DEFINITION_ID IN (" + SELECT_QUERY +
        RESOURCE_TYPE_ALERT_QUERY + ")";
    
    private static final String TRIGGER_FIX =
        "DELETE FROM EAM_REGISTERED_TRIGGER WHERE ALERT_DEFINITION_ID IN (" +
        SELECT_QUERY + RESOURCE_TYPE_ALERT_QUERY + ")";
    
    private Connection connection;
    
    private TypeAlertdefTriggersSQLDiagnosticsFactory(Connection c) {
        connection = c;
    }
    
    public static SQLDiagnosticsFactory getInstance(Connection c) {
        return new TypeAlertdefTriggersSQLDiagnosticsFactory(c);
    }
    
    public String getName() {
        return "Resource Type Alert Definition Triggers Diagnostics";
    }

    public List getFixQueries() throws SQLException {
        List fixes = new ArrayList(3);
        String sqlFix = ALERT_DEF_FIX + RESOURCE_TYPE_ALERT_QUERY;
        fixes.add(sqlFix);
        fixes.add(ALERT_CONDITION_FIX);
        fixes.add(TRIGGER_FIX);
        return fixes;
    }

    public List getTestQueries() throws SQLException {
        List tests = new ArrayList(1);
        tests.add(SELECT_QUERY + RESOURCE_TYPE_ALERT_QUERY);
        return tests;
    }
}
