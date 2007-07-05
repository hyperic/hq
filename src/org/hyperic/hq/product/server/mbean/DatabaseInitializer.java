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
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Initializer for HQ database, may contain database specific routines
 */
public class DatabaseInitializer {
    private String logCtx = DatabaseInitializer.class.getName();
    private Log log = LogFactory.getLog(logCtx);

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
                "CREATE VIEW HQ_METRIC_DATA AS " + UNION_BODY;
                        
            final String EAM_METRIC_DATA_VIEW =
                "CREATE VIEW EAM_MEASUREMENT_DATA AS " + UNION_BODY +
                " UNION ALL SELECT * FROM HQ_METRIC_DATA_COMPAT";

            Statement stmt = null;
            try {
                stmt = conn.createStatement();
                stmt.execute(HQ_METRIC_DATA_VIEW);
                stmt.execute(EAM_METRIC_DATA_VIEW);
            } catch (SQLException e) {
                // View was pre-existing, continue
                if (log.isDebugEnabled()) {
                    log.debug("Common Routines SQLException", e);
                }
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
            
            String function =
                "CREATE FUNCTION nextseqval (iname CHAR(50)) " +
                "RETURNS INT " +
                "DETERMINISTIC " +
                "BEGIN " +
                  "SET @new_seq_val = 0;" +
                  "UPDATE HQ_SEQUENCE set seq_val = @new_seq_val:=seq_val+1 " +
                    "WHERE seq_name=iname; " +
                  "RETURN @new_seq_val;" +
                "END;";
    
            try {
                stmt = conn.createStatement();
                stmt.execute(function);
            } catch (SQLException e) {
                // Function already exists, continue
                if (log.isDebugEnabled()) {
                    log.debug("MySQLRoutines SQLException", e);
                }
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }
        }
        
    }
}
