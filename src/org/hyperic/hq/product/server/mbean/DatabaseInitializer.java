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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Initializer for HQ database, may contain database specific routines
 *
 */
public class DatabaseInitializer {
    String logCtx = DatabaseInitializer.class.getName();
    Log log = LogFactory.getLog(logCtx);
    
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

        // Decide which database routines to run.  Interface and subclasses may
        // be broken out into own files in the future
        DatabaseRoutines dbr = null;
        Connection conn = null;
        
        try {
            conn = DBUtil.getConnByContext(ic, HQConstants.DATASOURCE);
            
            // We only do Postgres right now
            if (DBUtil.isPostgreSQL(conn))
                dbr = new PostgresRoutines(conn);

            if (dbr != null)
                dbr.runRoutines();
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
    
    public interface DatabaseRoutines {
        public void runRoutines() throws SQLException;
    }
    
    private class PostgresRoutines implements DatabaseRoutines {
        Connection conn = null;
        
        public PostgresRoutines(Connection conn) {
            this.conn = conn;
        }
        
        public void runRoutines() throws SQLException {
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
}
