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
        
        return (DatabaseRoutines[]) routines.toArray(new DatabaseRoutines[0]);
    }
    
    class CommonRoutines implements DatabaseRoutines {
        public void runRoutines(Connection conn) throws SQLException {
            final String METRIC_DATA_VIEW =
                "CREATE VIEW eam_measurement_data AS " +
                "SELECT * FROM metric_data_0d_0s UNION " +
                "SELECT * FROM metric_data_0d_1s UNION " +
                "SELECT * FROM metric_data_0d_2s UNION " +
                "SELECT * FROM metric_data_1d_0s UNION " +
                "SELECT * FROM metric_data_1d_1s UNION " +
                "SELECT * FROM metric_data_1d_2s UNION " +
                "SELECT * FROM metric_data_2d_0s UNION " +
                "SELECT * FROM metric_data_2d_1s UNION " +
                "SELECT * FROM metric_data_2d_2s UNION " +
                "SELECT * FROM metric_data_3d_0s UNION " +
                "SELECT * FROM metric_data_3d_1s UNION " +
                "SELECT * FROM metric_data_3d_2s UNION " +
                "SELECT * FROM metric_data_4d_0s UNION " +
                "SELECT * FROM metric_data_4d_1s UNION " +
                "SELECT * FROM metric_data_4d_2s UNION " +
                "SELECT * FROM metric_data_5d_0s UNION " +
                "SELECT * FROM metric_data_5d_1s UNION " +
                "SELECT * FROM metric_data_5d_2s UNION " +
                "SELECT * FROM metric_data_6d_0s UNION " +
                "SELECT * FROM metric_data_6d_1s UNION " +
                "SELECT * FROM metric_data_6d_2s UNION " +
                "SELECT * FROM metric_data_compat";

            Statement stmt = null;
            try {
                stmt = conn.createStatement();
                stmt.execute(METRIC_DATA_VIEW);
            } catch (SQLException e) {
                // View was pre-existing, contine
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
                "DELIMITER |" +
                "CREATE FUNCTION nextseqval (iname CHAR(50)) " +
                "RETURNS INT " +
                "DETERMINISTIC " +
                "BEGIN " +
                  "SET @new_seq_val = 0;" +
                  "UPDATE hq_sequence set seq_val = @new_seq_val:=seq_val+1 " +
                    "WHERE seq_name=iname; " +
                  "RETURN @new_seq_val;" +
                "END;" +
                "|";
    
            try {
                stmt = conn.createStatement();
                stmt.execute(function);
            } catch (SQLException e) {
                // Function already exists, contine
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }
        }
        
    }
}
