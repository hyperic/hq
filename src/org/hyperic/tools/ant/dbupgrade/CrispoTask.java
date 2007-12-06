package org.hyperic.tools.ant.dbupgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

public class CrispoTask extends SchemaSpecTask {

    public static final Class LOGCTX = CrispoTask.class;

    private static final String CRISPO_TABLE      = "EAM_CRISPO";
    private static final String CRISPO_OPT_TABLE  = "EAM_CRISPO_OPT";
    
    private static final String CRISPO_ID_SEQ     = "EAM_CRISPO_ID_SEQ";
    private static final String CRISPO_OPT_ID_SEQ = "EAM_CRISPO_OPT_ID_SEQ";
    
    public int createCrispo(Dialect d, ConfigResponse cr) throws SQLException {
        Statement stmt = null;
        ResultSet cidRs = null;
        int crispoId;

        try {
            String sql = d.getSequenceNextValString(CRISPO_ID_SEQ);
            stmt = getConnection().createStatement();
            cidRs = stmt.executeQuery(sql);
            cidRs.next();
            crispoId = cidRs.getInt(1);

            sql = "insert into " + CRISPO_TABLE + " (id, version_col) VALUES ("
                    + crispoId + ", 1)";
            stmt.execute(sql);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, cidRs);
        }

        for (Iterator i = cr.getKeys().iterator(); i.hasNext();) {
            String key = (String) i.next();
            String val = cr.getValue(key);
            createCrispoOpt(d, crispoId, key, val, stmt);
        }

        return crispoId;
    }

    public void createCrispoOpt(Dialect d, int crispoId, String key,
            String val, Statement stmt) throws SQLException {
        if (val == null || val.trim().equals(""))
            return;

        ResultSet optRs = null;
        int optId;

        try {
            String sql = d.getSequenceNextValString(CRISPO_OPT_ID_SEQ);
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
