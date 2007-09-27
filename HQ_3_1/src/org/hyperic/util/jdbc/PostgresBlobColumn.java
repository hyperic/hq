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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.file.FileUtil;

public class PostgresBlobColumn extends StdBlobColumn {

    private static final Log log
        = LogFactory.getLog(PostgresBlobColumn.class.getName());

    public PostgresBlobColumn (String dsName, String tableName, String idColName,
                               String blobColName) {
        super(dsName,tableName,idColName,blobColName);
    }

    protected void doSelect () throws SQLException {
        PreparedStatement    stmt = null;
        ResultSet            rs = null;
        Connection           conn = null;
        StringBuffer         sql = new StringBuffer();

        sql.append("SELECT ").append(getBlobColName()).append(" ")
           .append("FROM ").append(getTableName()).append(" ")
           .append("WHERE ").append(getIdColName()).append(" = ?");

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

    public static byte[] doSelect (ResultSet rs, int columnIndex) 
        throws SQLException {
        InputStream is = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            is = rs.getBinaryStream(columnIndex);
            if (is == null) return null;
            FileUtil.copyStream(is, os);
        } catch (IOException e) {
            log.error("Error reading blob: " + e, e);
            throw new SQLException(e.toString());
        } finally {
            if (is != null) try { is.close(); } catch (IOException e) {}
        }
        byte[] data = os.toByteArray();
        return data;
    }
}
