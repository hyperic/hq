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

package org.hyperic.tools.ant.dbupgrade;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.hyperic.util.jdbc.DBUtil;


/**
 * Migrates the data in the EAM_EVENT and EAM_TRIGGER_EVENT tables into the 
 * new EAM_TRIGGER_EVENT table.
 */
public class SST_MigrateTriggerEvents extends HibernateSchemaSpecTask {
    
    private static final String OLD_EAM_TRIGGER_EVENT = "EAM_TRIGGER_EVENT";
    private static final String EAM_EVENT = "EAM_EVENT";
    
    private final String _logCtx;
    
    private String  _table;
    private String  _sequence;
    
    public SST_MigrateTriggerEvents() {
        super(SST_MigrateTriggerEvents.class.getName());
        _logCtx = SST_MigrateTriggerEvents.class.getName();
    }
    
    public void setTable(String table) {
        _table = table;
    }
    
    public void setSequence(String sequence) {
        _sequence = sequence;
    }

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

    private void _execute() throws BuildException {
        if (_table == null) {
            throw new BuildException("The new EAM_TRIGGER_EVENT table name must be specified.");
        }
        
        if (_sequence == null) {
            throw new BuildException("The EAM_TRIGGER_EVENT sequence name must be specified.");
        }

        try {
            if (!DBUtil.checkTableExists(getConnection(), OLD_EAM_TRIGGER_EVENT)) {
                throw new BuildException("The old EAM_TRIGGER_EVENT table does not exist.");
            }

            if (!DBUtil.checkTableExists(getConnection(), EAM_EVENT)) {
                throw new BuildException("The EAM_EVENT table does not exist.");
            }            
        } catch (SQLException e) {
            throw new BuildException(e.getMessage(), e);
        }
        
        log(">>>>> Migrating events to new EAM_TRIGGER_EVENT table");

        Connection conn = null; 
        Statement stmt  = null;
        PreparedStatement seqStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs    = null;
                
        try {
            conn = getConnection();
            
            String selectSQL = "select e.EVENT_OBJECT, te.TRIGGER_ID, e.CTIME, " +
            		           "te.EXPIRATION from " + OLD_EAM_TRIGGER_EVENT + 
            		           " te join " + EAM_EVENT + " e on te.EVENT_ID = e.ID " +
            		           "order by te.TRIGGER_ID, e.CTIME";
            
            stmt = conn.createStatement();
            rs = stmt.executeQuery(selectSQL);
        
            String seqNextVal = getDialect().getSequenceNextValString(_sequence);
            seqStmt = conn.prepareStatement(seqNextVal);
                        
            String insertSQL = "insert into " + _table + 
                               " (ID, EVENT_OBJECT, TRIGGER_ID, CTIME, EXPIRATION) " +
                               "values (?,?,?,?,?)";
            
            insertStmt = conn.prepareStatement(insertSQL);
            
            while (rs.next()) {
                byte[] eventObject = rs.getBytes(1);
                int triggerId = rs.getInt(2); 
                long ctime = rs.getLong(3);
                long expiration = rs.getLong(4);
                
                migrateTriggerEventResults(seqStmt, insertStmt, 
                                           eventObject, triggerId, 
                                           ctime, expiration);
            }

        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeStatement(_logCtx, seqStmt);
            DBUtil.closeStatement(_logCtx, insertStmt);
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, rs);
        }
        
        log(">>>>> Finished migrating events to new EAM_TRIGGER_EVENT table");
    }
    
    private void migrateTriggerEventResults(PreparedStatement seqStmt,
                                            PreparedStatement insertStmt, 
                                            byte[] eventObject, 
                                            int triggerId, 
                                            long ctime, 
                                            long expiration) throws SQLException {
        
        long id;
        ResultSet seqRs = null;
        
        try {
            seqRs = seqStmt.executeQuery();
            seqRs.next();
            id = seqRs.getLong(1);            
        } finally {
            DBUtil.closeResultSet(_logCtx, seqRs);
        }
        
        insertStmt.setLong(1, id);
        insertStmt.setBytes(2, eventObject);
        insertStmt.setInt(3, triggerId);
        insertStmt.setLong(4, ctime);
        insertStmt.setLong(5, expiration);
        insertStmt.executeUpdate();
    }

}
