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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.jdbc.IDGeneratorFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * StdBlobColum - a wrapper for Blob columns of different databses.
 * Blobs represent a point of divergence for many vendors and
 * so this class is intended to serve as a base class for more
 * specialized blob handling versions.
 *
 * XXX - to do, support sibling (non-blob) columns
 */
public class StdBlobColumn implements BlobColumn {
    private String tableName;
    private String idColName;
    private String blobColName;
    private String seqName;
    private String ctxName;
    private String dsName;
    private Integer id;
    private byte[] blobData;
    private DataSource dataSource;

    private static final Log log
        = LogFactory.getLog(StdBlobColumn.class.getName());

    public StdBlobColumn (String dsName, String tableName, String idColName, 
        String blobColName) {
        this.dsName = dsName;
        this.tableName = tableName;
        this.idColName = idColName;
        this.blobColName = blobColName;
    }

    public Integer getId() { return this.id; }
    public void setId(Integer id) { this.id = id; }
    public byte[] getBlobData() { return this.blobData; }
    public void setBlobData( byte[] ba ) { this.blobData = ba; }
    public void setTableName(String s) { this.tableName = s; }
    public String getTableName() { return this.tableName; }
    public void setIdColName(String s) { this.idColName= s; }
    public String getIdColName() { return this.idColName; }
    public void setBlobColName(String s) { this.blobColName= s; }
    public String getBlobColName() { return this.blobColName; }
    public String getSeqName () { return this.seqName; }
    public String getCtxName () { return this.ctxName; }
    public void setSequenceInfo(String ctxName, String seqName) { 
        this.seqName = seqName; 
        this.ctxName = ctxName;
    }

    /** select the blob data */
    public void select () throws SQLException {
        checkIdSet();
        doSelect();
    }

    /** update the blob data */
    public void update () throws SQLException {
        checkIdSet();
        doUpdate();
    }

    /** insert the blob */
    public void insert () 
        throws SQLException, NamingException, ConfigPropertyException {

        checkForInsert();

        if (this.seqName != null) {
            setId(fetchNextId());
        }
        doInsert();
    }

    /** delete the blob row */
    public void delete () throws SQLException {
        checkIdSet();
        doDelete();
    }
    
    protected void checkForInsert() 
        throws SQLException, NamingException, ConfigPropertyException {
        if(getId() == null && getSeqName() == null)
            throw new IllegalArgumentException("Can't insert without valid id "+
                                               "or sequence name set");
        if(this.seqName != null)
            setId(fetchNextId());
    }

    protected void doSelect () throws SQLException {
        PreparedStatement    stmt = null;
        ResultSet            rs = null;
        Connection           conn = null;
        StringBuffer         sql = new StringBuffer();

        sql.append("SELECT ").append(this.blobColName).append(" ")
           .append("FROM ").append(this.tableName).append(" ")
           .append("WHERE ").append(this.idColName).append(" = ?");

        try {
            conn = getDBConn();
            stmt = conn.prepareStatement(sql.toString());
            log.debug(sql.toString());
            stmt.setInt(1, getId().intValue());
            rs = stmt.executeQuery();
            if (rs.next()) {
                setBlobData(doSelect(rs, 1));
            }
        } finally {
            DBUtil.closeJDBCObjects(log, conn, stmt, rs);
        }
    }

    protected static byte[] doSelect (ResultSet rs, int columnIndex) 
        throws SQLException {
        Blob blob = null;
        try {
            blob = rs.getBlob(columnIndex);
        } catch (SQLException e) {
            log.error("Error reading blob: " + e, e);
            throw e;
        }
        if (blob == null) return null;
        int blen = new Long(blob.length()).intValue();
        return blob.getBytes(1, blen);
    }

    protected void doUpdate () throws SQLException {
        PreparedStatement    stmt = null;
        Connection           conn = null;
        StringBuffer         sql = new StringBuffer();

        sql.append("UPDATE ").append(this.tableName).append(" ")
           .append("SET ").append(this.blobColName).append(" = ? ")
           .append("WHERE ").append(this.idColName).append(" = ? ");

        try {
            conn = getDBConn();
            stmt = conn.prepareStatement(sql.toString());
            log.debug(sql.toString());
            stmt.setBytes(1, getBlobData());
            stmt.setInt(2, getId().intValue());
            stmt.executeUpdate();
        } finally {
            DBUtil.closeJDBCObjects(log, conn, stmt, null);
        }
    }

    protected void doInsert () throws SQLException {
        PreparedStatement    stmt = null;
        Connection           conn = null;
        StringBuffer         sql = new StringBuffer();

        sql.append("INSERT INTO ").append(this.tableName).append(" ")
           .append("(").append(this.idColName).append(",")
           .append(this.blobColName).append(") VALUES ( ?,? ) ");

        try {
            conn = getDBConn();
            stmt = conn.prepareStatement(sql.toString());
            log.debug(sql.toString());
            stmt.setInt(1, getId().intValue());
            stmt.setBytes(2, getBlobData());
            stmt.executeUpdate();
        } finally {
            DBUtil.closeJDBCObjects(log, conn, stmt, null);
        }
    }

    protected void doDelete () throws SQLException {
        PreparedStatement    stmt = null;
        Connection           conn = null;
        StringBuffer         sql = new StringBuffer();

        sql.append("DELETE FROM ").append(this.tableName).append(" ")
           .append("WHERE ").append(this.idColName).append(" = ? ");

        try {
            conn = getDBConn();
            stmt = conn.prepareStatement(sql.toString());
            log.debug(sql.toString());
            stmt.setInt(1, getId().intValue());
            stmt.executeUpdate();
        } finally {
            DBUtil.closeJDBCObjects(log, conn, stmt, null);
        }
    }

    protected Integer fetchNextId () 
        throws SQLException, NamingException, ConfigPropertyException {
        Integer retVal;
        try {
            // first we get a new id from the id generator
            retVal = new Integer((int) 
                IDGeneratorFactory.getNextId(this.ctxName, this.seqName, 
                                             this.dsName));
            this.setId(id);
        } catch (NamingException e) {
            log.error("Naming Exception occured in " +
                      "StdBlobColumn.fetchNextId(): " + e.getMessage());
            throw e;
        } catch (SQLException e) {
            log.error("SQL Exception occured in " +
                      "StdBlobColumn.fetchNextId(): " + e.getMessage());
            throw e;
        } catch (ConfigPropertyException e) {
            log.error("Config Property Exception occured in " +
                      "StdBlobColumn.fetchNextId: " + e.getMessage());
            throw e;
        }
        return retVal;
    }

    protected void checkIdSet () {
        if (null == getId()) {
            throw new IllegalArgumentException("Id not set.");
        }
    }

    protected Connection getDBConn() throws SQLException {
        try {
            if (this.dataSource == null) {
                this.dataSource = 
                    (DataSource) getInitialContext().lookup(dsName);
            }
            return dataSource.getConnection();
        } catch (NamingException e) {
//            throw new SystemException(e);
            log.error("Errog getting DB connection: " + e, e);
            throw new SQLException(e.toString());
        }
    }

    protected InitialContext getInitialContext() {
        try {
            return new InitialContext();
        } catch (NamingException e) {
//            throw new SystemException(e);
            log.error("Error getting initial context: " + e, e);
            return null;
        }
    }

}
