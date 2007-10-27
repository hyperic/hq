/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.product.server.mbean;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Initializer for HQ database, may contain database specific routines
 */
public class DatabaseInitializer {
    private String logCtx = DatabaseInitializer.class.getName();
    private Log log = LogFactory.getLog(logCtx);
    private static final String TAB_DATA = MeasurementConstants.TAB_DATA,
                                MEAS_VIEW = MeasTabManagerUtil.MEAS_VIEW;

    public static void init() {
        new DatabaseInitializer();
    }
    
    private DatabaseInitializer() {
        InitialContext ic;
        try {
            ic = new InitialContext();
        } catch (NamingException e) {
            log.error("Could not get InitialContext", e);
            return;     // Can't do anything
        }

        Connection conn = null;
        
        try {
            conn = DBUtil.getConnByContext(ic, HQConstants.DATASOURCE);
            
            DatabaseRoutines[] dbrs = getDBRoutines(conn);
            
            for (int i = 0; i < dbrs.length; i++) {
                dbrs[i].runRoutines(conn);
            }
        } catch (SQLException e) {
            log.error("SQLException creating connection to " +
                      HQConstants.DATASOURCE, e);
        } catch (NamingException e) {
            log.error("NamingException creating connection to " +
                      HQConstants.DATASOURCE, e);
        } finally {
            DBUtil.closeConnection(DatabaseInitializer.class, conn);
        }        
    }
    
    interface DatabaseRoutines {
        public void runRoutines(Connection conn) throws SQLException;
    }

    private DatabaseRoutines[] getDBRoutines(Connection conn)
        throws SQLException {
        ArrayList routines = new ArrayList(2);
        
        routines.add(new CommonRoutines());
        
        if (DBUtil.isPostgreSQL(conn))
            routines.add(new PostgresRoutines());
        
        if (DBUtil.isMySQL(conn))
            routines.add(new MySQLRoutines());
        
        return (DatabaseRoutines[]) routines.toArray(new DatabaseRoutines[0]);
    }
    
    class CommonRoutines implements DatabaseRoutines {
        public void runRoutines(Connection conn) throws SQLException {
            final String UNION_BODY =
                "SELECT * FROM HQ_METRIC_DATA_0D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_0D_1S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_1D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_1D_1S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_2D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_2D_1S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_3D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_3D_1S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_4D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_4D_1S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_5D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_5D_1S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_6D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_6D_1S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_7D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_7D_1S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_8D_0S UNION ALL " +
                "SELECT * FROM HQ_METRIC_DATA_8D_1S";
            
            final String HQ_METRIC_DATA_VIEW =
                "CREATE VIEW "+MEAS_VIEW+" AS " + UNION_BODY;
                        
            final String EAM_METRIC_DATA_VIEW =
                "CREATE VIEW "+TAB_DATA+" AS " + UNION_BODY +
                " UNION ALL SELECT * FROM HQ_METRIC_DATA_COMPAT";

            Statement stmt = null;
            try {
                HQDialect dialect = Util.getHQDialect();
                stmt = conn.createStatement();
                if (!dialect.viewExists(stmt, TAB_DATA))
                    stmt.execute(EAM_METRIC_DATA_VIEW);
                if (!dialect.viewExists(stmt, MEAS_VIEW))
                    stmt.execute(HQ_METRIC_DATA_VIEW);
            } catch (SQLException e) {
                log.error("Error Creating Metric Data Views", e);
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }
        } 
    }
    
    class PostgresRoutines implements DatabaseRoutines {
        public void runRoutines(Connection conn) throws SQLException {
            Statement stmt = null;
            ResultSet rs = null;
            
            String function =
                "CREATE OR REPLACE FUNCTION add_data" +
                        "(in_id INT, in_time BIGINT, in_value NUMERIC) " + 
                "RETURNS VOID AS " +
                "$$ " +
                "BEGIN " +                
                "LOOP " +
                "UPDATE eam_measurement_data SET value = in_value " +
                "WHERE measurement_id = in_id AND timestamp = in_time; " +
                "IF found THEN RETURN; " +
                "END IF; " +
                "BEGIN " +
                "INSERT INTO eam_measurement_data" +
                        "(measurement_id,timestamp,value) VALUES " +
                        "(in_id, in_time, in_value); " +
                "RETURN; " +
                "EXCEPTION WHEN unique_violation THEN END; " +
                "END LOOP; " +
                "END; " +
                "$$ " +
                "LANGUAGE plpgsql;";

            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT * FROM pg_language " +
                                       "WHERE lanname = 'plpgsql'");
                
                if (!rs.next()) {
                    stmt.execute("CREATE LANGUAGE plpgsql");
                }
                
                stmt.execute(function);
            } finally {
                DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            }
        }
        
    }

    class MySQLRoutines implements DatabaseRoutines {
        public void runRoutines(Connection conn) throws SQLException {
            Statement stmt = null;

            final int batch = 1000;
            final String createHqSeqMemTable =
                "CREATE TABLE HQ_MEM_SEQUENCE ( " +
                   "seq_name varchar(50) NOT NULL, " +
                   "seq_val int(11) default NULL, " +
                   "PRIMARY KEY  (seq_name, seq_val) " +
                ") ENGINE=MEMORY;";

            final String getNextHqSeqval =
                "CREATE FUNCTION getNextHqSeqval(iname CHAR(50)) " +
                "RETURNS INT " +
                "DETERMINISTIC " +
                "BEGIN " +
                  "SET @new_seq_val = 0;" +
                  "UPDATE HQ_SEQUENCE set seq_val = @new_seq_val:=seq_val+1 " +
                    "WHERE seq_name=iname; " +
                  "RETURN @new_seq_val;" +
                "END;";

            final String getNextMemSeqval =
                "CREATE PROCEDURE getNextMemSeqVal(iname VARCHAR(50), " +
                                         "OUT rtn_val INT)" +
                "BEGIN "+
                "DECLARE seq_count INT; " +
                "DECLARE idx INT; " +
                "DECLARE next_seq INT; " +
                "DECLARE seq_max INT; " +
                "DECLARE mem_seq_max INT; " +
                "DECLARE tmp INT; " +
                "SELECT get_lock(\"seq_lock\", 60) into tmp; " +
                "SELECT count(*) into seq_count from HQ_MEM_SEQUENCE " +
                    "WHERE seq_name = iname; " +
                "IF seq_count <= 1 THEN " +
                     "SELECT seq_val into seq_max from HQ_SEQUENCE " +
                        "WHERE seq_name = iname; " +
                     "SELECT max(seq_val) into mem_seq_max from HQ_MEM_SEQUENCE " +
                        "WHERE seq_name = iname; " +
                     "IF seq_max != mem_seq_max THEN " +
                          "UPDATE HQ_SEQUENCE set seq_val = mem_seq_max " +
                            "WHERE seq_name = iname; " +
                     "END IF; " +
                     "select getNextHqSeqval(iname) into next_seq; " +
                     "SET idx = next_seq + 1; " +
                     "SET tmp = next_seq + "+(batch-1)+"; " +
                     "insert into HQ_MEM_SEQUENCE (seq_name, seq_val) " +
                        "values (iname, next_seq); " +
                     "UPDATE HQ_SEQUENCE set seq_val = seq_val+"+(batch-1)+" " +
                        "WHERE seq_name=iname; " +
                     "populate: LOOP " +
                         "IF idx >= tmp THEN " +
                                "LEAVE populate; " +
                         "END IF; " +
                         "insert into HQ_MEM_SEQUENCE (seq_name, seq_val) " +
                                "values (iname, idx); " +
                         "SET idx = idx + 1; " +
                     "END LOOP populate; " +
                "END IF; " +
                "select min(seq_val) into rtn_val from HQ_MEM_SEQUENCE" +
                " WHERE seq_name = iname; " +
                "delete from HQ_MEM_SEQUENCE where seq_name = iname" +
                " AND seq_val = rtn_val; " +
                "SELECT release_lock(\"seq_lock\") into tmp; " +
                "END;";

            final String nextseqval =
                "CREATE FUNCTION nextseqval(iname VARCHAR(50)) " +
                "RETURNS INT " +
                "READS SQL DATA " +
                "BEGIN " +
                     "DECLARE rtn_val INT; " +
                     "set @tmp = 0; " +
                     "call getNextMemSeqval(iname, @tmp); " +
                     "select name_const('seq_val', @tmp) into rtn_val; " +
                     "return (rtn_val); " +
                "END;";

            try {
                // To see the reason for this refer to JIRA HHQ-1158
                incrementSequenceIDs(conn, batch);
                stmt = conn.createStatement();
                stmt.execute(getNextHqSeqval);
                stmt.execute(getNextMemSeqval);
                stmt.execute(nextseqval);
                stmt.execute(createHqSeqMemTable);
            } catch (SQLException e) {
                // Function + Procedure already exist, continue
                if (log.isDebugEnabled()) {
                    log.debug("MySQLRoutines SQLException", e);
                }
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }
        }
    }

    private void incrementSequenceIDs(Connection conn, int batchSize)
    {
        if (!memTableExists(conn))
            return;
        Statement stmt = null;
        try
        {
            String sql = "update HQ_SEQUENCE set seq_val = seq_val+"+batchSize;
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
        } finally {
            DBUtil.closeStatement(logCtx, stmt);
        }
    }

    private boolean memTableExists(Connection conn)
    {
        Statement stmt = null;
        try
        {
            stmt = conn.createStatement();
            HQDialect dialect = Util.getHQDialect();
            return dialect.tableExists(stmt, "HQ_MEM_SEQUENCE");
        } catch (SQLException e) {
        } finally {
            DBUtil.closeStatement(logCtx, stmt);
        }
        return false;
    }
}
