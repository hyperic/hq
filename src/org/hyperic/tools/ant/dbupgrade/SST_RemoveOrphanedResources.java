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

package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.hyperic.util.jdbc.DBUtil;

public class SST_RemoveOrphanedResources extends SchemaSpecTask {

    public static final String _logCtx = SST_RemoveOrphanedResources.class
        .getName();
    public static final String SCHEMA_MOD_IN_PROGRESS =
        " *** UPGRADE TASK: Removing Oraphaned Resources ";
    private static String _oraDualTab = "";

    public SST_RemoveOrphanedResources() {
    }

    public void execute() throws BuildException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            if (DBUtil.isOracle(conn)) {
                _oraDualTab = " FROM DUAL";
            }
            stmt = conn.createStatement();
            log(SCHEMA_MOD_IN_PROGRESS);
            removeOrphanedServiceResources(stmt);
            removeOrphanedServerResources(stmt);
            removeOrphanedPlatformResources(stmt);
        } catch (SQLException e) {
            throw new BuildException(_logCtx + ": " + e.getMessage(), e);
        } finally {
            DBUtil.closeStatement(_logCtx, stmt);
        }
    }

    private void removeOrphanedPlatformResources(Statement stmt)
        throws SQLException {
        ResultSet rs = null;
        int resourceType = 301;
        try {
            String sql = "SELECT Abs((SELECT COUNT(*)" + " FROM EAM_RESOURCE"
                + " WHERE resource_type_id = " + resourceType
                + ") - (SELECT COUNT(*)" + " FROM EAM_PLATFORM)) as plat_count"
                + _oraDualTab;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                int count = rs.getInt("plat_count");
                if (count == 0) {
                    log("NOTE: No Orphaned Platforms To Remove.");
                    return;
                } else {
                    log("Attempting To Remove " + count + " Orphaned Platform"
                        + ((count == 1) ? "" : "s"));
                }
            } else {
                return;
            }
            sql = "DELETE FROM EAM_RES_GRP_RES_MAP"
                + " WHERE resource_id IN (SELECT id"
                + " FROM EAM_RESOURCE WHERE resource_type_id = " + resourceType
                + " AND instance_id NOT IN (SELECT id FROM EAM_PLATFORM))";
            int deleted = stmt.executeUpdate(sql);
            log("Removed " + deleted + " Orphaned Platform Group Map Row"
                + ((deleted == 1) ? "" : "s"));

            deleteResources(stmt, resourceType, "EAM_PLATFORM", "Platform");

            sql = "DELETE FROM EAM_PLATFORM"
                + " WHERE id NOT IN (SELECT instance_id" + " FROM EAM_RESOURCE"
                + " WHERE resource_type_id = " + resourceType + ")";
            deleted = stmt.executeUpdate(sql);
            log("Removed " + deleted + " Orphaned Platform Resource"
                + ((deleted == 1) ? "" : "s"));

        } finally {
            DBUtil.closeResultSet(_logCtx, rs);
        }
    }

    private void removeOrphanedServerResources(Statement stmt)
        throws SQLException {
        String sql;
        int resourceType = 303;
        ResultSet rs = null;
        try {
            sql = "SELECT Abs((SELECT COUNT(*)" + " FROM EAM_RESOURCE"
                + " WHERE resource_type_id = " + resourceType + ") -"
                + " (SELECT COUNT(*) FROM EAM_SERVER)) as server_count"
                + _oraDualTab;

            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                int count = rs.getInt("server_count");
                if (count == 0) {
                    log("NOTE: No Orphaned Servers To Remove.");
                    return;
                } else {
                    log("Attempting To Remove " + count + " Orphaned Server"
                        + ((count == 1) ? "" : "s"));
                }
            } else {
                return;
            }

            sql = "DELETE FROM EAM_RES_GRP_RES_MAP"
                + " WHERE resource_id IN (SELECT id FROM EAM_RESOURCE"
                + " WHERE resource_type_id = " + resourceType
                + " AND instance_id NOT IN (SELECT id FROM EAM_SERVER))";
            int deleted = stmt.executeUpdate(sql);
            log("Removed " + deleted + " Orphaned Server Group Map Row"
                + ((deleted == 1) ? "" : "s"));

            deleteResources(stmt, resourceType, "EAM_SERVER", "Server");

            sql = "DELETE FROM EAM_SERVER"
                + " WHERE  id NOT IN (SELECT instance_id"
                + " FROM   EAM_RESOURCE" + " WHERE  resource_type_id = "
                + resourceType + ")";
            deleted = stmt.executeUpdate(sql);
            log("Removed " + deleted + " Orphaned Server"
                + ((deleted == 1) ? "" : "s"));

        } finally {
            DBUtil.closeResultSet(_logCtx, rs);
        }
    }

    private void removeOrphanedServiceResources(Statement stmt)
        throws SQLException {
        int resourceType = 305;
        String sql;
        ResultSet rs = null;
        try {
            sql = "SELECT Abs((SELECT COUNT(*) FROM EAM_RESOURCE"
                + " WHERE resource_type_id = " + resourceType + ") -"
                + " (SELECT COUNT(*) FROM EAM_SERVICE)) as service_count"
                + _oraDualTab;
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                int count = rs.getInt("service_count");
                if (count == 0) {
                    log("NOTE: No Orphaned Services To Remove.");
                    return;
                } else {
                    log("Attempting To Remove " + count + " Orphaned Service"
                        + ((count == 1) ? "" : "s"));
                }
            } else {
                return;
            }
            sql = "DELETE FROM EAM_RES_GRP_RES_MAP"
                + " WHERE resource_id IN (SELECT id FROM"
                + " EAM_RESOURCE WHERE resource_type_id = " + resourceType
                + " AND instance_id NOT IN (SELECT id" + " FROM EAM_SERVICE))";
            int deleted = stmt.executeUpdate(sql);
            log("Removed " + deleted + " Orphaned Service Group Map Row"
                + ((deleted == 1) ? "" : "s"));

            deleteResources(stmt, resourceType, "EAM_SERVICE", "Service");

            sql = "DELETE FROM EAM_SERVICE"
                + " WHERE id NOT IN (SELECT instance_id FROM"
                + " EAM_RESOURCE WHERE resource_type_id = " + resourceType
                + ")";
            deleted = stmt.executeUpdate(sql);
            log("Removed " + deleted + " Orphaned Service"
                + ((deleted == 1) ? "" : "s"));
        } finally {
            DBUtil.closeResultSet(_logCtx, rs);
        }
    }

    private void deleteResources(Statement stmt, int typeId, String tableRel,
        String desc) throws SQLException {
        String baseSql = "from EAM_RESOURCE" + " WHERE resource_type_id = "
            + typeId + " AND instance_id NOT IN (SELECT id FROM " + tableRel
            + ")";
        String deleteSql = "DELETE " + baseSql;
        String resourceSql = "(SELECT id " + baseSql + ")";
        String sql;
        int deleted;
        sql = "DELETE FROM EAM_ROLE" + " WHERE resource_id in" + resourceSql;
        deleted = stmt.executeUpdate(sql);
        log("Removed " + deleted + " Orphaned Role"
            + ((deleted == 1) ? "" : "s"));

        sql = "DELETE FROM EAM_SUBJECT" + " WHERE resource_id in" + resourceSql;
        deleted = stmt.executeUpdate(sql);
        log("Removed " + deleted + " Orphaned Subject"
            + ((deleted == 1) ? "" : "s"));

        sql = "DELETE FROM EAM_RESOURCE_TYPE" + " WHERE resource_id in"
            + resourceSql;
        deleted = stmt.executeUpdate(sql);
        log("Removed " + deleted + " Orphaned Resource Type"
            + ((deleted == 1) ? "" : "s"));

        sql = "DELETE FROM EAM_RESOURCE_GROUP" + " WHERE resource_id in"
            + resourceSql;
        deleted = stmt.executeUpdate(sql);
        log("Removed " + deleted + " Orphaned Resource Group"
            + ((deleted == 1) ? "" : "s"));

        sql = "DELETE FROM EAM_AUDIT" + " WHERE resource_id in" + resourceSql;
        deleted = stmt.executeUpdate(sql);
        log("Removed " + deleted + " Orphaned Audit Row"
            + ((deleted == 1) ? "" : "s"));

        sql = "DELETE FROM EAM_ALERT_DEFINITION" + " WHERE resource_id in"
            + resourceSql;
        deleted = stmt.executeUpdate(sql);
        log("Removed " + deleted + " Orphaned Alert Definition"
            + ((deleted == 1) ? "" : "s"));

        deleted = stmt.executeUpdate(deleteSql);
        log("Removed " + deleted + " Orphaned " + desc + " Resource"
            + ((deleted == 1) ? "" : "s"));
    }
}