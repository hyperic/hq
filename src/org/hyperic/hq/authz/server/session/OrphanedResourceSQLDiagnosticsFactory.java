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
import java.util.Iterator;

import org.hyperic.hq.common.SQLDiagnostic;
import org.hyperic.hq.common.SQLDiagnosticsFactory;

public  class OrphanedResourceSQLDiagnosticsFactory implements SQLDiagnosticsFactory {

    private static final String SELECT_QUERY = "SELECT * ";
    
    private static final String DELETE_QUERY = "DELETE ";
    
    private static final String[] ORPHANED_RESOURCE_QUERY =  {
        
        "FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id "+
        "FROM eam_platform)",
        
        "FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_platform p " +
        "WHERE p.id = instance_id)",
        
        "FROM eam_platform " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 301)",
        
        "FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 301 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_platform))",
        
        "FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server)",
        
        "FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_server s " +
        "WHERE s.id = instance_id)",
        
        "FROM eam_server " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 303)",
        
        "FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 303 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_server))",
        
        "FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service)",
        
        "FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND NOT NAME = (SELECT NAME " +
        "FROM eam_service s " +
        "WHERE s.id = instance_id)",
        
        "FROM eam_service " +
        "WHERE id NOT IN (SELECT instance_id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 305)",
        
        "FROM eam_res_grp_res_map " +
        "WHERE resource_id IN (SELECT id " +
        "FROM eam_resource " +
        "WHERE resource_type_id = 305 " +
        "AND instance_id NOT IN (SELECT id " +
        "FROM eam_service))",
        
        "FROM eam_res_grp_res_map " +
        "WHERE resource_group_id NOT IN (SELECT id " +
        "FROM eam_resource_group)",
        
        "FROM eam_resource_group " +
        "WHERE id NOT IN (SELECT resource_group_id " +
        "FROM eam_res_grp_res_map)"
    
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

    public Iterator getSQLDiagnostics() {
        return new OrphanedResourceSQLDiagnostic();
    }
    
    private class OrphanedResourceSQLDiagnostic implements Iterator {
        
        private int pos = 0;

        public boolean hasNext() {
            return (pos <  ORPHANED_RESOURCE_QUERY.length);
        }

        public Object next() { 
            String sqlQuery = SELECT_QUERY + ORPHANED_RESOURCE_QUERY[pos];
            String sqlFix = DELETE_QUERY + ORPHANED_RESOURCE_QUERY[pos];
            pos++;
            return new SQLDiagnostic(sqlQuery, sqlFix);
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported");
            
        }
        
    }
}
