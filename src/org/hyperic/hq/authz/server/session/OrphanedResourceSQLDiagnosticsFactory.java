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

public  class OrphanedResourceSQLDiagnosticsFactory implements SQLDiagnosticsFactory {
    
    private static final String[] ORPHANED_RESOURCE_FIX_QUERY =  {
        
        "DELETE FROM EAM_RESOURCE_GROUP " +
        "WHERE ID NOT IN (SELECT RESOURCE_GROUP_ID " +
        "FROM EAM_RES_GRP_RES_MAP)",
        
        "DELETE FROM EAM_RES_GRP_RES_MAP " +
        "WHERE RESOURCE_GROUP_ID NOT IN (SELECT ID " +
        "FROM EAM_RESOURCE_GROUP)",
        
        "DELETE FROM EAM_RES_GRP_RES_MAP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_SERVICE " +
        "WHERE ID NOT IN (SELECT INSTANCE_ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305)",
        
        "DELETE FROM EAM_ROLE " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_SUBJECT " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_RESOURCE_TYPE " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_RESOURCE_GROUP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_AUDIT " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_ALERT_DEFINITION " +
        "WHERE RESOURCE_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE FROM_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE TO_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_MEASUREMENT " +
        "WHERE RESOURCE_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "DELETE FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM EAM_SERVICE S " +
        "WHERE S.ID = INSTANCE_ID)",
        
        "DELETE FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE)",
        
        "DELETE FROM EAM_RES_GRP_RES_MAP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_SERVER " +
        "WHERE ID NOT IN (SELECT INSTANCE_ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303)",
        
        "DELETE FROM EAM_ROLE " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_SUBJECT " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_RESOURCE_TYPE " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_RESOURCE_GROUP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_AUDIT " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_ALERT_DEFINITION " +
        "WHERE RESOURCE_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE FROM_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE TO_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_MEASUREMENT " +
        "WHERE RESOURCE_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        "DELETE FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM EAM_SERVER S " +
        "WHERE S.ID = INSTANCE_ID)",
        
        "DELETE FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER)",
        
        "DELETE FROM EAM_RES_GRP_RES_MAP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_PLATFORM " +
        "WHERE ID NOT IN (SELECT INSTANCE_ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301)",
        
        "DELETE FROM EAM_ROLE " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_SUBJECT " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE_TYPE " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE_GROUP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_AUDIT " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_ALERT_DEFINITION " +
        "WHERE RESOURCE_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE FROM_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE TO_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_MEASUREMENT " +
        "WHERE RESOURCE_ID IN (SELECT ID FROM " +
        "EAM_RESOURCE WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM EAM_PLATFORM P " +
        "WHERE P.ID = INSTANCE_ID)",
        
        "DELETE FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID "+
        "FROM EAM_PLATFORM)",
        
        "DELETE FROM EAM_RESOURCE_RELATION " +
        "WHERE ID NOT IN (SELECT REL_ID FROM " +
        "EAM_RESOURCE_EDGE)"
        
    };
    
    private static final String[] ORPHANED_RESOURCE_TEST_QUERY =  {
        
        // platform checks
        "SELECT * FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID "+
        "FROM EAM_PLATFORM)",
        
        "SELECT * FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM EAM_PLATFORM P " +
        "WHERE P.ID = INSTANCE_ID)",
        
        "SELECT * FROM EAM_PLATFORM " +
        "WHERE ID NOT IN (SELECT INSTANCE_ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301)",
        
        "SELECT * FROM EAM_RES_GRP_RES_MAP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 301 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_PLATFORM))",
        
        // server checks
        "SELECT * FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER)",
        
        "SELECT * FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM EAM_SERVER S " +
        "WHERE S.ID = INSTANCE_ID)",
        
        "SELECT * FROM EAM_SERVER " +
        "WHERE ID NOT IN (SELECT INSTANCE_ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303)",
        
        "SELECT * FROM EAM_RES_GRP_RES_MAP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 303 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVER))",
        
        // service checks
        "SELECT * FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE)",
        
        "SELECT * FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM EAM_SERVICE S " +
        "WHERE S.ID = INSTANCE_ID)",
        
        "SELECT * FROM EAM_SERVICE " +
        "WHERE ID NOT IN (SELECT INSTANCE_ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305)",
        
        "SELECT * FROM EAM_RES_GRP_RES_MAP " +
        "WHERE RESOURCE_ID IN (SELECT ID " +
        "FROM EAM_RESOURCE " +
        "WHERE RESOURCE_TYPE_ID = 305 " +
        "AND INSTANCE_ID NOT IN (SELECT ID " +
        "FROM EAM_SERVICE))",
        
        "SELECT * FROM EAM_RES_GRP_RES_MAP " +
        "WHERE RESOURCE_GROUP_ID NOT IN (SELECT ID " +
        "FROM EAM_RESOURCE_GROUP)"
        
    };
    
    private Connection connection;
    
    private OrphanedResourceSQLDiagnosticsFactory(Connection c) {
        connection = c;
    }
    
    public static SQLDiagnosticsFactory getInstance(Connection c) {
        return new OrphanedResourceSQLDiagnosticsFactory(c);
    }
    
    public String getName() {
        return "Orphaned Resource Diagnostics";
    }

    public List getFixQueries() throws SQLException {
        List fixes = new ArrayList(ORPHANED_RESOURCE_FIX_QUERY.length);
        for (int i=0; i < ORPHANED_RESOURCE_FIX_QUERY.length; i++) {
            fixes.add(ORPHANED_RESOURCE_FIX_QUERY[i]);
        }
        return fixes;
    }

    public List getTestQueries() throws SQLException {
        List tests = new ArrayList(ORPHANED_RESOURCE_TEST_QUERY.length);
        for (int i=0; i < ORPHANED_RESOURCE_TEST_QUERY.length; i++) {
            tests.add(ORPHANED_RESOURCE_TEST_QUERY[i]);
        }
        return tests;
    }
}
