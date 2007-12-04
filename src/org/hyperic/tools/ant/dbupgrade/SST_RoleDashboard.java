package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.HibernateUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

public class SST_RoleDashboard extends SchemaSpecTask {

    public static final Class LOGCTX = SST_RoleDashboard.class;
    
    public static ConfigResponse props;
    
    private static final String CRISPO_TABLE      = "EAM_CRISPO";
    private static final String CRISPO_OPT_TABLE  = "EAM_CRISPO_OPT";
    private static final String ROLE_TABLE        = "EAM_ROLE";
    private static final String DASH_CONFIG_TABLE = "EAM_DASH_CONFIG";
    
    private static final String CRISPO_ID_SEQ     = "EAM_CRISPO_ID_SEQ";
    private static final String CRISPO_OPT_ID_SEQ = "EAM_CRISPO_OPT_ID_SEQ";
    private static final String DASH_CONFIG_SEQ   = "EAM_DASH_CONFIG_ID_SEQ";
    
    private static final String SUPER_USER_ROLE_NAME  = "Super User Role";
    private static final String RESOURCE_CREATOR_ROLE = "RESOURCE_CREATOR_ROLE";
    
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
    	props.setValue(".dashContent.summaryCounts.group.plat.server.service", "false");
    	props.setValue(".dashContent.summaryCounts.group.application", "false");
    	
    	props.setValue(".dashContent.resourcehealth.availability", "true");
    	props.setValue(".dashContent.resourcehealth.throughput", "true");
    	props.setValue(".dashContent.resourcehealth.performance", "false");
    	props.setValue(".dashContent.resourcehealth.utilization", "true");
    	
    	props.setValue(".dashContent.recentlyApproved.range", "24");
    	
    	props.setValue(".dashContent.criticalalerts.numberOfAlerts", "5");
    	props.setValue(".dashContent.criticalalerts.past", "86400000");
    	props.setValue(".dashContent.criticalalerts.priority", "2");
    	props.setValue(".dashContent.criticalalerts.selectedOrAll", "all");
    	
    	props.setValue(".dashcontent.portal.portlets.first", "|.dashContent.searchResources|.dashContent.savedCharts|.dashContent.recentlyApproved|.dashContent.availSummary");
    	props.setValue(".dashcontent.portal.portlets.second", "|.dashContent.autoDiscovery|.dashContent.resourceHealth|.dashContent.criticalAlerts|.dashContent.controlActions");
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
            if (!DBUtil.checkTableExists(conn, CRISPO_OPT_TABLE)) {
                throw new BuildException("Table eam_crispo_opt doesn't exist");
            }
            if (!DBUtil.checkTableExists(conn, DASH_CONFIG_TABLE)) {
                throw new BuildException("Table eam_dash_config doesn't exist");
            }
            if (!DBUtil.checkTableExists(conn, CRISPO_TABLE)) {
                throw new BuildException("Table eam_crispo doesn't exist");
            }
            if (!DBUtil.checkTableExists(conn, ROLE_TABLE)) {
                throw new BuildException("Table eam_role doesn't exist");
            }

            Dialect d = HibernateUtil.getDialect(conn);
            roleStatement = conn.createStatement();
            dashStatement = conn.createStatement();
            String check_sql = "select id from " + ROLE_TABLE
                    + " where id in (select role_id from " + DASH_CONFIG_TABLE + ")";
            checkStatement = conn.createStatement();
            log("executed query: " + check_sql);
            checkRS = checkStatement.executeQuery(check_sql);

            Map ignores = new HashMap();
            int check_id_col = checkRS.findColumn("id");
            while (checkRS.next()) {
                ignores.put(new Integer(checkRS.getInt(check_id_col)), "");
                System.out.println("added ignore for: " + check_id_col);
            }
            checkRS.close();
            checkStatement.close();

            String sql = "select id,name from " + ROLE_TABLE;
            System.out.println("executed query: " + sql);
            roleRS = roleStatement.executeQuery(sql);

            int id_col = roleRS.findColumn("id");
            int name_col = roleRS.findColumn("name");
            int roleId;
            while (roleRS.next()) {
                
                String name = roleRS.getString(name_col);
                if (name.equalsIgnoreCase(SUPER_USER_ROLE_NAME)
                        || name.equalsIgnoreCase(RESOURCE_CREATOR_ROLE)) {
                    continue;
                }
                System.out.println("creating roleid");
                roleId = roleRS.getInt(id_col);
                if (!ignores.containsKey(new Integer(roleId))) {
                    // create crispo
                    long crispoId;
                    if (props != null) {
                        crispoId = createCrispo(d, props, conn);
                    } else
                        throw new BuildException();

                    // insert role dash pref
                    String seq = d.getSelectSequenceNextValString(DASH_CONFIG_SEQ);
                    
                    String insertSql = "insert into "
                            + DASH_CONFIG_TABLE
                            + " (id, config_type, version_col, name, crispo_id, role_id, user_id)"
                            + " values (" + seq + ", 'ROLE', 0, '"
                            + name + " Dashboard', " + +crispoId + ", " + roleId
                            + ", null)";
                    System.out.println("executed query: " + insertSql);
                    int rows = dashStatement.executeUpdate(insertSql);
                    System.out.println("rows updated: " + rows);
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
    
    private int createCrispo(Dialect d, ConfigResponse cr, Connection conn) 
        throws SQLException {
        Statement stmt = null;
        ResultSet cidRs = null;
        int crispoId;

        try {
            String sql = d.getSequenceNextValString(CRISPO_ID_SEQ);
            stmt = conn.createStatement();
            cidRs = stmt.executeQuery(sql);
            cidRs.next();
            crispoId = cidRs.getInt(1);

            sql = "insert into " + CRISPO_TABLE +
                  " (id, version_col) VALUES (" + crispoId + ", 1)";
            stmt.execute(sql);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, cidRs);
        }

        for (Iterator i = cr.getKeys().iterator(); i.hasNext();) {
            String key = (String) i.next();
            String val = cr.getValue(key);
            createCrispoOpt(d, crispoId, key, val, conn);
        }

        return crispoId;
    }

    private void createCrispoOpt(Dialect d, int crispoId, String key, 
            String val, Connection conn)
            throws SQLException {
        if (val == null || val.trim().equals(""))
            return;

        Statement stmt = null;
        ResultSet optRs = null;
        int optId;

        try {
            String sql = d.getSequenceNextValString(CRISPO_OPT_ID_SEQ);
            stmt = conn.createStatement();
            optRs = stmt.executeQuery(sql);
            optRs.next();
            optId = optRs.getInt(1);

            sql = "insert into " + CRISPO_OPT_TABLE
                    + " (id, version_col, propkey, val, "
                    + "crispo_id) VALUES (" + optId + ", 1, '" + key + "', "
                    + "'" + val + "', " + crispoId + ")";

            System.out.println("executed query: " + sql);
            stmt.executeUpdate(sql);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, optRs);
        }
    }
}
