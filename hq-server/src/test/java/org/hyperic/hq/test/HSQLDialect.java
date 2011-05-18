package org.hyperic.hq.test;

import java.sql.SQLException;
import java.sql.Statement;

import org.hyperic.hibernate.DialectAccessor;
import org.hyperic.hibernate.dialect.HQDialect;

/**
 * Implementation of @{link {@link HQDialect} to use with tests that use an
 * embedded HSQLDB. TODO Fill in methods as they are used by calling code (all
 * calling code will access this class through @{link {@link DialectAccessor})
 * @author jhickey
 * 
 */
public class HSQLDialect
    extends org.hibernate.dialect.HSQLDialect implements HQDialect {

    public String getOptimizeStmt(String table, int cost) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean useMetricUnion() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean useEamNumbers() {
        return false;
    }

    public int getMaxExpressions() {
        return -1;
    }

    public boolean supportsMultiInsertStmt() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getRegExSQL(String column, String regex, boolean ignoreCase, boolean invertMatch) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean supportsDuplicateInsertStmt() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean tableExists(Statement stmt, String tableName) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public String getLimitString(int num) {
        return "LIMIT "+num;
    }

    public boolean viewExists(Statement stmt, String viewName) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean usesSequenceGenerator() {
        return true;
    }

    public boolean supportsPLSQL() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getLimitBuf(String sql, int offset, int limit) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getMetricDataHint() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public boolean requiresCast() {
        return true;
    }

}
