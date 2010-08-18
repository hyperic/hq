/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.dialect.HQDialectUtil;
import org.hyperic.hq.install.InstallDBUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

public class SST_RoleDashboard extends CrispoTask {

    public static final Class LOGCTX = SST_RoleDashboard.class;
    
    public static ConfigResponse props;
    
    private static final String CRISPO_TABLE      = "EAM_CRISPO";
    private static final String CRISPO_OPT_TABLE  = "EAM_CRISPO_OPT";
    private static final String ROLE_TABLE        = "EAM_ROLE";
    private static final String DASH_CONFIG_TABLE = "EAM_DASH_CONFIG";
    private static final String USER_TABLE        = "EAM_SUBJECT";
    
    private static final String DASH_CONFIG_SEQ   = "EAM_DASH_CONFIG_ID_SEQ";
    
    private static final String RESOURCE_CREATOR_ROLE = "RESOURCE_CREATOR_ROLE";
    private static final String GUEST_ROLE_NAME = "Guest Role";
    
    static{
    	props = new ConfigResponse();
    	props.setValue(".dashContent.autoDiscovery.range",  "5");
    	props.setValue(".dashContent.problems.showIgnored", "false");
    	
    	props.setValue(".dashContent.controlActions.lastCompleted", "5");
    	props.setValue(".dashContent.controlActions.mostFrequent",  "5");
    	props.setValue(".dashContent.controlActions.nextScheduled", "5");
    	props.setValue(".dashContent.controlActions.useLastCompleted", "true");
    	props.setValue(".dashContent.controlActions.useMostFrequent",  "true");
    	props.setValue(".dashContent.controlActions.useNextScheduled", "true");
    	props.setValue(".dashContent.controlActions.past", "604800000");
    	
    	props.setValue(".dashContent.summaryCounts.application", "true");
    	props.setValue(".dashContent.summaryCounts.platform", "true");
    	props.setValue(".dashContent.summaryCounts.server", "true");
    	props.setValue(".dashContent.summaryCounts.service", "true");
    	
    	props.setValue(".dashContent.summaryCounts.group.cluster", "true");
    	props.setValue(".dashContent.summaryCounts.group.mixed", "true");
    	props.setValue(".dashContent.summaryCounts.group.groups", "false");
    	props.setValue(".dashContent.summaryCounts.group.plat.server.service",
    	               "false");
    	props.setValue(".dashContent.summaryCounts.group.application", "false");
    	
    	props.setValue(".dashContent.resourcehealth.availability", "true");
    	props.setValue(".dashContent.resourcehealth.throughput", "true");
    	props.setValue(".dashContent.resourcehealth.performance", "false");
    	props.setValue(".dashContent.resourcehealth.utilization", "true");
    	
    	props.setValue(".dashContent.recentlyApproved.range", "24");
    	
    	props.setValue(".dashContent.charts.range", "1d");
    	
    	props.setValue(".dashContent.criticalalerts.numberOfAlerts", "5");
    	props.setValue(".dashContent.criticalalerts.past", "86400000");
    	props.setValue(".dashContent.criticalalerts.priority", "2");
    	props.setValue(".dashContent.criticalalerts.selectedOrAll", "all");
    	
    	props.setValue(".dashcontent.portal.portlets.first", "|.dashContent.searchResources|.dashContent.savedCharts|.dashContent.recentlyApproved|.dashContent.availSummary");
    	props.setValue(".dashcontent.portal.portlets.second", "|.dashContent.autoDiscovery|.dashContent.resourceHealth|.dashContent.criticalAlerts|.dashContent.controlActions|.dashContent.problemResources");    	
    }
    
    public SST_RoleDashboard() {}
    
    public void execute() throws BuildException {
    	try {
            _execute();
        } catch(BuildException e) {
            e.printStackTrace();
            throw (BuildException)e;
        } catch(RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    private void _execute() throws BuildException
    {
        Statement roleStatement  = null;
        Statement dashStatement  = null;
        Statement checkStatement = null;
        ResultSet roleRS  = null;
        ResultSet checkRS = null;
        Connection conn   = null;

        try
        {
            conn = getConnection();
            if (!InstallDBUtil.checkTableExists(conn, CRISPO_OPT_TABLE)) {
                throw new BuildException("Table eam_crispo_opt doesn't exist");
            }
            if (!InstallDBUtil.checkTableExists(conn, DASH_CONFIG_TABLE)) {
                throw new BuildException("Table eam_dash_config doesn't exist");
            }
            if (!InstallDBUtil.checkTableExists(conn, CRISPO_TABLE)) {
                throw new BuildException("Table eam_crispo doesn't exist");
            }
            if (!InstallDBUtil.checkTableExists(conn, ROLE_TABLE)) {
                throw new BuildException("Table eam_role doesn't exist");
            }

            Dialect d = HQDialectUtil.getDialect(conn);
            String check_sql = "select id from " + ROLE_TABLE
                    + " where id in (select role_id from " + DASH_CONFIG_TABLE
                    + ")";
            checkStatement = conn.createStatement();
            log("executed query: " + check_sql);
            checkRS = checkStatement.executeQuery(check_sql);

            Map ignores = new HashMap();
            int check_id_col = checkRS.findColumn("id");
            while (checkRS.next()) {
                ignores.put(new Integer(checkRS.getInt(check_id_col)), "");
                System.out.println("added ignore for: " + check_id_col);
            }
            DBUtil.closeJDBCObjects(LOGCTX, null, checkStatement, checkRS);

            String sql = "select id,name from " + ROLE_TABLE;
            System.out.println("executed query: " + sql);
            roleStatement = conn.createStatement();
            roleRS = roleStatement.executeQuery(sql);

            int id_col = roleRS.findColumn("id");
            int name_col = roleRS.findColumn("name");
            int roleId;
            dashStatement = conn.createStatement();
            while (roleRS.next()) {
                
                String name = roleRS.getString(name_col);
                if (name.equalsIgnoreCase(RESOURCE_CREATOR_ROLE)) {
                    continue;
                }
                System.out.println("creating roleid");
                roleId = roleRS.getInt(id_col);
                if (!ignores.containsKey(new Integer(roleId))) {
                    // create crispo
                    long crispoId;
                    if (props != null) {
                        crispoId = createCrispo(d, props);
                    } else
                        throw new BuildException();

                    // insert role dash pref
                    String seq = d.getSequenceNextValString(DASH_CONFIG_SEQ);
                    
                    ResultSet rs = dashStatement.executeQuery(seq);
                    rs.next();
                    long id = rs.getInt(1);
                    DBUtil.closeResultSet(LOGCTX, rs);
                    
                    String insertSql = "insert into "
                            + DASH_CONFIG_TABLE
                            + " (id, config_type, version_col, name, crispo_id, role_id, user_id)"
                            + " values (" + id + ", 'ROLE', 0, '"
                            + name + " Dashboard', " + +crispoId + ", " + roleId
                            + ", null)";
                    System.out.println("executed query: " + insertSql);
                    int rows = dashStatement.executeUpdate(insertSql);
                    System.out.println("rows updated: " + rows);
                    
                    // Need this to prevent Guest Roles from having a default 
                    // selection dialog
                    if (name.equalsIgnoreCase(GUEST_ROLE_NAME)) {
                        // Need to get the dashboard config ID
                        
                        ConfigResponse guestRoleProps = new ConfigResponse();
                        guestRoleProps.setValue(".user.dashboard.default.id",
                                                id);
                        long prefCrispoId = createCrispo(d, guestRoleProps);
                        
                        // Set the crispo ID as the guest user's preference
                        dashStatement.executeUpdate("update " + USER_TABLE
                                                  + " set pref_crispo_id = "
                                                  + prefCrispoId
                                                  + " where id = 2");
                    }
                }
            }
            System.out.println("done");
        } catch (SQLException e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            // don't close the connection, it is shared for all tasks
            DBUtil.closeJDBCObjects(LOGCTX, null, roleStatement, roleRS);
            DBUtil.closeJDBCObjects(LOGCTX, null, dashStatement, null);
        }
    }
    
    
}
