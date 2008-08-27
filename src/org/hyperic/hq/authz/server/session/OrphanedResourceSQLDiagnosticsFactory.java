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
        
        "DELETE FROM eam_resource_group " +
        "WHERE id NOT IN (SELECT resource_group_id " +
        "FROM eam_res_grp_res_map)",
        
        "DELETE FROM eam_res_grp_res_map " +
        "WHERE resource_group_id NOT IN (SELECT id " +
        "FROM eam_resource_group)",
        
        "DELETE FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM eam_service " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 305)",
        
        "DELETE FROM EAM_ROLE " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM EAM_SUBJECT " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM EAM_RESOURCE_TYPE " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM EAM_RESOURCE_GROUP " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM EAM_AUDIT " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM EAM_ALERT_DEFINITION " +
        "WHERE resource_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE from_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE to_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM EAM_MEASUREMENT " +
        "WHERE resource_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "DELETE FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_service s " +
        "WHERE s.id = instance_id)",
        
        "DELETE FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service)",
        
        "DELETE FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM eam_server " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 303)",
        
        "DELETE FROM EAM_ROLE " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM EAM_SUBJECT " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM EAM_RESOURCE_TYPE " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM EAM_RESOURCE_GROUP " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM EAM_AUDIT " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM EAM_ALERT_DEFINITION " +
        "WHERE resource_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE from_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE to_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM EAM_MEASUREMENT " +
        "WHERE resource_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "DELETE FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_server s " +
        "WHERE s.id = instance_id)",
        
        "DELETE FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server)",
        
        "DELETE FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_platform))",
        
        "DELETE FROM eam_platform " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 301)",
        
        "DELETE FROM EAM_ROLE " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_SUBJECT " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE_TYPE " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE_GROUP " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_AUDIT " +
        "WHERE resource_id in (SELECT id " +
        "from EAM_RESOURCE " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_ALERT_DEFINITION " +
        "WHERE resource_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE from_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_RESOURCE_EDGE " +
        "WHERE to_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM EAM_MEASUREMENT " +
        "WHERE resource_id in (SELECT id from " +
        "EAM_RESOURCE WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM EAM_PLATFORM))",
        
        "DELETE FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_platform p " +
        "WHERE p.id = instance_id)",
        
        "DELETE FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id "+
        "FROM eam_platform)",
        
        "DELETE FROM EAM_RESOURCE_RELATION " +
        "WHERE id not in (SELECT rel_id from " +
        "EAM_RESOURCE_EDGE)"
        
    };
    
    private static final String[] ORPHANED_RESOURCE_TEST_QUERY =  {
        
        // platform checks
        "SELECT * FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id "+
        "FROM eam_platform)",
        
        "SELECT * FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_platform p " +
        "WHERE p.id = instance_id)",
        
        "SELECT * FROM eam_platform " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 301)",
        
        "SELECT * FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_platform))",
        
        // server checks
        "SELECT * FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server)",
        
        "SELECT * FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_server s " +
        "WHERE s.id = instance_id)",
        
        "SELECT * FROM eam_server " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 303)",
        
        "SELECT * FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        // service checks
        "SELECT * FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service)",
        
        "SELECT * FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_service s " +
        "WHERE s.id = instance_id)",
        
        "SELECT * FROM eam_service " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 305)",
        
        "SELECT * FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "SELECT * FROM eam_res_grp_res_map " +
        "WHERE resource_group_id NOT IN (SELECT id " +
        "FROM eam_resource_group)",
        
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
