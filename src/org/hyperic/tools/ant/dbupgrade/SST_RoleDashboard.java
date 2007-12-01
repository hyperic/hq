package org.hyperic.tools.ant.dbupgrade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.HibernateUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

public class SST_RoleDashboard extends SchemaSpecTask {

    public static final Class LOGCTX = SST_RoleDashboard.class;
    
    private static final String CRISPO_TABLE      = "EAM_CRISPO";
    private static final String CRISPO_OPT_TABLE  = "EAM_CRISPO_OPT";
    private static final String ROLE_TABLE        = "EAM_ROLE";
    private static final String DASH_CONFIG_TABLE = "EAM_DASH_CONFIG";
    
    private static final String CRISPO_ID_SEQ     = "EAM_CRISPO_ID_SEQ";
    private static final String CRISPO_OPT_ID_SEQ = "EAM_CRISPO_OPT_ID_SEQ";
    private static final String DASH_CONFIG_SEQ   = "EAM_DASH_CONFIG_ID_SEQ";
    
    private static final String PROPERTIES_FILE_DEV   = "web/WEB-INF/DefaultUserDashboardPreferences.properties";
    private static final String PROPERTIES_FILE_PROD   = "../data/role-dashboard-preferences.properties";

    private static final String SUPER_USER_ROLE_NAME  = "Super User Role";
    private static final String RESOURCE_CREATOR_ROLE = "RESOURCE_CREATOR_ROLE";
    
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
        ConfigResponse properties = null;

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
                log("added ignore for: " + check_id_col);
            }
            checkRS.close();
            checkStatement.close();

            String sql = "select id,name from " + ROLE_TABLE;
            log("executed query: " + sql);
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
                log("creating roleid");
                roleId = roleRS.getInt(id_col);
                if (!ignores.containsKey(new Integer(roleId))) {
                    // create crispo
                    if (properties == null) {
                        properties = loadPropertiesFile();
                    }
                    long crispoId;
                    if (properties != null) {
                        crispoId = createCrispo(d, properties, conn);
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
                    log("executed query: " + insertSql);
                    int rows = dashStatement.executeUpdate(insertSql);
                    log("rows updated: " + rows);
                }
            }
            log("done");
        } catch (SQLException e) {
            throw new BuildException(e.getMessage(), e);
        } catch(IOException e){
            throw new BuildException(e.getMessage(), e);
        } finally {
            // don't close the connection, it is shared for all tasks
            DBUtil.closeJDBCObjects(LOGCTX, null, roleStatement, roleRS);
            DBUtil.closeJDBCObjects(LOGCTX, null, dashStatement, null);
        }
    }
    
    private ConfigResponse loadPropertiesFile() throws IOException{
        Properties props = new Properties();
        InputStream is = null;
        File file = null;
        try {
            file = new File(PROPERTIES_FILE_DEV);
            if (file.exists()) {
                is = new FileInputStream(file);
            } else
                is = new FileInputStream(new File(PROPERTIES_FILE_PROD));
        } catch (FileNotFoundException e) {
            throw new BuildException("Role dashboard properties file not found");
        }
        props.load(is);
        is.close();
        ConfigResponse prefs = null;
        if (props != null) {
            prefs = new ConfigResponse();
            Enumeration keys = props.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                prefs.setValue(key, props.getProperty(key));
            }
            log("added properties");
        }
        return prefs;
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

            log("executed query: " + sql);
            stmt.executeUpdate(sql);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, optRs);
        }
    }
}
