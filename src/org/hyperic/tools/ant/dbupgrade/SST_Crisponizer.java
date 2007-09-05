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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.hibernate.dialect.Dialect;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Converts an old-style serialized ConfigResponse into a Crispo
 */
public class SST_Crisponizer extends SchemaSpecTask {
    private static final Class LOGCTX = SST_Crisponizer.class;
    private static final String CRISPO_ID_SEQ     = "EAM_CRISPO_ID_SEQ";
    private static final String CRISPO_TABLE      = "EAM_CRISPO";
    private static final String CRISPO_OPT_ID_SEQ = "EAM_CRISPO_OPT_ID_SEQ";
    private static final String CRISPO_OPT_TABLE  = "EAM_CRISPO_OPT";
    
    private String _table;
    private String _column;
    private String _crispoColumn;
    
    public SST_Crisponizer() {}

    public void setTable(String table) {
        _table = table;
    }
    
    public void setColumn(String column) {
        _column = column;
    }
    
    public void setCrispoColumn(String c) {
        _crispoColumn = c;
    }
    
    private void createCrispoOpt(Dialect d, int crispoId, String key, 
                                 String val) 
        throws SQLException
    {
        PreparedStatement pstmt = null;
        Statement stmt = null;
        ResultSet optRs = null;
        int optId;
        
        try {
            String sql = d.getSequenceNextValString(CRISPO_OPT_ID_SEQ);
            stmt = getConnection().createStatement();
            optRs = stmt.executeQuery(sql);
            optRs.next();
            optId = optRs.getInt(1);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, optRs);
        }
        
        try {
            String sql = "insert into " + CRISPO_OPT_TABLE + 
                " (\"id\", \"version_col\", \"propkey\", \"val\", " + 
                "\"crispo_id\") VALUES (" + optId + ", 1, ?, ?, " + crispoId +
                ")";
                
            pstmt = getConnection().prepareStatement(sql);
            pstmt.setString(1, key);
            pstmt.setString(2, val);
            pstmt.execute();
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, pstmt, null);
        }
    }

    private int createCrispo(Dialect d, ConfigResponse cr) 
        throws SQLException
    {
        Statement stmt = null;
        ResultSet cidRs = null;
        int crispoId;
        
        try {
            String sql = d.getSequenceNextValString(CRISPO_ID_SEQ);
            stmt  = getConnection().createStatement();
            cidRs = stmt.executeQuery(sql);
            cidRs.next();
            crispoId = cidRs.getInt(1);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, cidRs);
        }

        try {
            stmt = getConnection().createStatement();
            String sql = "insert into " + CRISPO_TABLE +  
                         " (\"id\", \"version_col\") VALUES (" + crispoId + 
                         ", 1)";
            stmt.execute(sql);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, null);
        }
        
        for (Iterator i=cr.getKeys().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            String val = cr.getValue(key);
            
            createCrispoOpt(d, crispoId, key, val);
        }
        
        return crispoId;
    }
    
    private void updateRowWithCrispo(long fromId, long crispoId) 
        throws SQLException, BuildException
    {
        Statement stmt = null; 
        try {
            String sql = "update " + _table + " set " + _crispoColumn + 
                         "=" + crispoId + " where id=" + fromId;
            stmt = getConnection().createStatement();
            int updateRes = stmt.executeUpdate(sql); 
            if (updateRes != 1) {
                throw new BuildException("Update [" + sql + "] did not " + 
                                         "update 1 row (updated " + updateRes +
                                         ")");
            }
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, null);
        }
    }

    public void execute() throws BuildException {
        Statement stmt  = null;
        ResultSet rs    = null;

        if (_table == null || _column == null || _crispoColumn == null) {
            throw new BuildException("table, column, and crispoColumn " +
                                     "must be specified");
        }
        
        try {
            if (!DBUtil.checkColumnExists(LOGCTX.getName(), getConnection(), 
                                          _table, _column)) 
            {
                throw new BuildException("Column containing ConfigResponse (" + 
                                         _column + ") in table (" + _table + 
                                         ") does not exist");
            }
            if (!DBUtil.checkColumnExists(LOGCTX.getName(), getConnection(), 
                                          _table, _crispoColumn)) 
            {
                throw new BuildException("Column to place new crispo ID (" + 
                                         _crispoColumn + ") in table (" + 
                                         _table + ") does not exist"); 
            }
        } catch(SQLException e) {
            throw new BuildException(e.getMessage(), e);
        }
                        
        try {
            Connection conn = getConnection();
            Dialect d = DBUtil.getDialect(conn);
            stmt = conn.createStatement();
            
            rs = stmt.executeQuery("select id, " + _column + " from " + _table);
            while (rs.next()) {
                long fromId = rs.getLong(1);
                byte[] b = rs.getBytes(2);
                
                ConfigResponse cr = ConfigResponse.decode(b);

                long crispoId = createCrispo(d, cr);
                updateRowWithCrispo(fromId, crispoId);
                log(_table + " (id=" + fromId + ") now has " + _crispoColumn + 
                    "=" + crispoId);
            }
        } catch (EncodingException e) {
            throw new BuildException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(LOGCTX, null, stmt, rs);
        }
    }
}
