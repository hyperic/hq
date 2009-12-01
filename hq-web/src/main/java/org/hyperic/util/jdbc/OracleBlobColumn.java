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

package org.hyperic.util.jdbc;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.OutputStream;
import java.io.IOException;

import oracle.sql.BLOB;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * OracleBlobColum - a wrapper for Oracle Blob columns.
 */
public class OracleBlobColumn extends StdBlobColumn {

    private static final Log log
        = LogFactory.getLog(OracleBlobColumn.class.getName());

    public OracleBlobColumn (String dsName, String tableName, String idColName,
                             String blobColName) {
        super(dsName,tableName,idColName,blobColName);
    }

    protected void doSelect () throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ").append(getBlobColName()).append(" FROM ")
            .append(getTableName()).append(" WHERE ").append(getIdColName())
            .append(" = ?");
        Blob columnValue = null;
        ResultSet rs = null;

        try {
            conn = getDBConn();
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1,getId().intValue());
            rs = pstmt.executeQuery();
            
            if(rs.next()) {
                setBlobData(doSelect(rs, 1));
            }
            
        } finally {
            DBUtil.closeJDBCObjects(log, conn, pstmt, rs);
        }
    }

    public static byte[] doSelect (ResultSet rs, int columnIndex) 
        throws SQLException {

        Blob blob = rs.getBlob(columnIndex);
        if (blob == null) return null;
        int length = new Long(blob.length()).intValue();
        return blob.getBytes(1, length);
    }

    protected void doUpdate () throws SQLException {
        PreparedStatement pstmt = null;
        Connection conn = null;
        StringBuffer sql = new StringBuffer();
        sql.append("UPDATE ").append(getTableName()).append(" SET ")
            .append(getBlobColName()).append(" = EMPTY_BLOB() WHERE ")
            .append(getIdColName()).append(" = ? ");
        try {
            conn = getDBConn();
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, getId().intValue());
            pstmt.executeUpdate();
        } finally {
            DBUtil.closeJDBCObjects(log, conn, pstmt, null);
        }
        sql.setLength(0); 
        sql.append("SELECT ").append(getBlobColName()).append(" FROM ")
            .append(getTableName()).append(" WHERE ").append(getIdColName())
            .append(" = ? FOR UPDATE");
        ResultSet rs = null;

        log.debug(sql.toString());
        try {
            conn = getDBConn();
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1,getId().intValue());
            rs = pstmt.executeQuery();
            if(rs.next()) {

                // Get the Blob locator and open output stream for the Blob
                Blob columnValue = rs.getBlob(1);
                OutputStream bos = ((BLOB)columnValue).getBinaryOutputStream();
                bos.write(getBlobData()); // Write to Blob
                bos.close();
            }
        } catch (SQLException e) {
            log.error(e);
            throw e;
        } catch (IOException e) {
            log.error("Caught IO Exception closing config blob");
            throw new SQLException("Caught IO Exception closing blob");
        } finally {
           DBUtil.closeJDBCObjects(log, conn, pstmt, rs);
        }

    }

    protected void doInsert () throws SQLException {
        PreparedStatement pstmt = null;
        Connection conn = null;
        StringBuffer sql = new StringBuffer();
        
        sql.append("INSERT INTO ").append(getTableName())
            .append(" ( ").append(getIdColName()).append(", ")
            .append(getBlobColName()).append(") VALUES ( ")
            .append(" ? , EMPTY_BLOB() "); 
        
        log.debug(sql.toString());

        try {
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, getId().intValue());
            pstmt.executeUpdate();
        } finally {
            DBUtil.closeJDBCObjects(log, null, pstmt, null);
        }
            

        sql.setLength(0);
        sql.append("SELECT ").append(getBlobColName()). append(" FROM ")
            .append(getTableName()).append(" WHERE ").append(getIdColName())
            .append(" = ? FOR UPDATE"); 

        log.debug(sql.toString());

        ResultSet rs = null;
        BLOB columnValue = null;
        
        try {
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, getId().intValue());
            rs = pstmt.executeQuery();
            
            if(rs.next()) {
                columnValue = (BLOB)rs.getBlob(1);
                columnValue.putBytes(1, getBlobData());
            }
        } finally {
            DBUtil.closeJDBCObjects(log, null, pstmt, rs);
        }

        sql.setLength(0);
        sql.append("UPDATE ").append(getTableName())
            .append(" SET ").append(getBlobColName()).append(" = ?")
            .append(" WHERE ").append(getIdColName()).append(" = ?");
        log.debug(sql.toString());

        try {
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setBlob(1, columnValue);
            pstmt.setInt(2, getId().intValue());
        } finally {
            DBUtil.closeJDBCObjects(log,conn, pstmt, null);
        }
            
    }
}
