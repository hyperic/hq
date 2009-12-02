package org.hyperic.hq.install;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hibernate.dialect.HQDialectUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.jdbc.DriverLoadException;
import org.hyperic.util.jdbc.JDBC;

public class InstallDBUtil {
    
    private static final Log log = LogFactory.getLog(InstallDBUtil.class);

    public static boolean checkTableExists(Connection conn, String table) throws SQLException {

        HQDialect dialect = HQDialectUtil.getHQDialect(conn);

        Statement stmt = null;
        boolean exists = false;

        try {
            stmt = conn.createStatement();
            exists = dialect.tableExists(stmt, table);
        } finally {
            DBUtil.closeStatement(log, stmt);
        }

        return exists;
    }

    public static boolean checkTableExists(String url, String user, String password, String table)
        throws DriverLoadException, SQLException {

        try {
            Class.forName(JDBC.getDriverString(url)).newInstance();
        } catch (Exception e) {
            throw new DriverLoadException(e);
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            return checkTableExists(conn, table);

        } finally {
            DBUtil.closeConnection(log, conn);
        }
    }

}
