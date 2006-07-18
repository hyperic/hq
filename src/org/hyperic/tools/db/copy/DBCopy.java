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

package org.hyperic.tools.db.copy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.jdbc.JDBC;

public class DBCopy
{
    private static final String VERSION   = "DBCopy, Version 1.0.0";
    private static final String COPYRIGHT =
        "Copyright (C) Covalent Technologies, Inc., All Rights Reserved.";
    private static final String SYNTAX =
        "DBCopy <Source JDBC> <Source User> <Source Password> <Source Table>\n" +
        "       <Destination JDBC> <Destination User> <Destionation Password>";
        
    private String     m_srcJdbc;
    private Connection m_srcConn;
    private String     m_srcUser;
    private String     m_srcPassword;
    private String     m_srcTable;
        
    private Connection m_destConn;
    private String     m_destJdbc;
    private String     m_destUser;
    private String     m_destPassword;
    private String     m_destTable;
        
	public static void main(String[] args) {
        System.out.println(VERSION + '\n' + COPYRIGHT + '\n');

        if(args.length < 7) {
            System.out.println(SYNTAX);
            System.exit(-1);
        }
                                
        String srcJdbc      = args[0];
        String srcUser      = args[1];
        String srcPassword  = args[2];
        String srcTable     = args[3];
        String destJdbc     = args[4];
        String destUser     = args[5];
        String destPassword = args[6];
        String destTable    = srcTable;
        
        if(args.length >= 8)
            destTable = args[7];
                
        DBCopy dbcopy = new DBCopy(srcJdbc, srcUser, srcPassword, srcTable, 
                                   destJdbc, destUser, destPassword, destTable);
        
        try {
            dbcopy.copy();
        } catch(SQLException e) {
            JDBC.printSQLException(e); 
        } catch(Exception e) {
            System.out.println(e); 
        }
	}
    
    public DBCopy(String srcJdbc, String srcUser, String srcPassword,
                  String srcTable,
                  String destJdbc, String destUser, String destPassword,
                  String destTable)
    {
        m_srcJdbc     = srcJdbc;
        m_srcUser     = srcUser;
        m_srcPassword = srcPassword;
        m_srcTable    = srcTable;
        
        m_destJdbc     = destJdbc;
        m_destUser     = destUser;
        m_destPassword = destPassword;
        m_destTable    = destTable;
    }
    
    public void copy() throws SQLException {
        this.openConnections();
        this.copyTables();             
        this.closeConnections();
    }

    public void copyTables() throws SQLException {
        // Build and execute the query for the src        
        StringBuffer sqlbuf =
            new StringBuffer("SELECT * FROM ").append(m_srcTable);
                    
        Statement stmt    = m_srcConn.createStatement();
        ResultSet results = stmt.executeQuery(sqlbuf.toString());

        this.copyResults(results);        
    }
    
    protected void copyResults(ResultSet rset) throws SQLException {
        // Look at the meta data for the src to know what to get
        ResultSetMetaData meta = rset.getMetaData();                   

        // Build the insert command for the destination
        StringBuffer sqlbuf = new StringBuffer("INSERT INTO ")
                                  .append(m_destTable).append(" VALUES(");
        for(int i = 0;i < meta.getColumnCount();i++) {
            if(i > 0)
                sqlbuf.append(',');
            sqlbuf.append('?');
        }
        sqlbuf.append(')');
        
        PreparedStatement prep = m_destConn.prepareStatement(sqlbuf.toString());
        
        for(int row = 1;rset.next() == true;row ++) {
            for(int col = 1;col <= meta.getColumnCount();col++) {
                switch(meta.getColumnType(col)) {
                case Types.ARRAY:
                    prep.setArray(col, rset.getArray(col));
                    break;
                case Types.BIGINT:
                    prep.setLong(col, rset.getLong(col));
                    break;
                case Types.DECIMAL:
                case Types.NUMERIC:
                    prep.setBigDecimal(col, rset.getBigDecimal(col));
                    break;
                case Types.BINARY:
                case Types.LONGVARBINARY:
                case Types.VARBINARY:
                    prep.setBytes(col, rset.getBytes(col));
                    break;
                case Types.BIT:
                case Types.BOOLEAN:
                    prep.setBoolean(col, rset.getBoolean(col));
                    break;
                case Types.BLOB:
                    prep.setBlob(col, rset.getBlob(col));
                    break;
                case Types.CLOB:
                    prep.setClob(col, rset.getClob(col));
                    break;
                case Types.DATE:
                    prep.setDate(col, rset.getDate(col));
                    break;
                case Types.DOUBLE:
                case Types.FLOAT:
                    prep.setDouble(col, rset.getDouble(col));
                    break;
                case Types.REAL:
                    prep.setFloat(col, rset.getFloat(col));
                    break;
                case Types.INTEGER:
                    prep.setInt(col, rset.getInt(col));
                    break;
                case Types.SMALLINT:
                    prep.setShort(col, rset.getShort(col));
                    break;
                case Types.CHAR:
                case Types.LONGVARCHAR:
                case Types.VARCHAR:
                    prep.setString(col, rset.getString(col));
                    break;
                case Types.TIME:
                    prep.setTime(col, rset.getTime(col));
                    break;
                case Types.TIMESTAMP:
                    prep.setTimestamp(col, rset.getTimestamp(col));
                    break;
                case Types.TINYINT:
                    prep.setByte(col, rset.getByte(col));
                    break;                    
                default:
                    throw new SQLException("Invalid Type");
                }
            }
            
            // Print the row that we're on
            String num = Integer.toString(row);
            System.out.print(num);
            for(int i = 0;i < num.length();i++)
                System.out.print('\b');

            // Execute the Insert                
            prep.execute();
        }
        
        m_destConn.commit();
        m_srcConn.commit();

        try { if(prep != null) prep.close(); } catch(SQLException e) {} 
    }
    
    protected void openConnections() throws SQLException {
        try {
            JDBC.loadDriver(m_srcJdbc);
            JDBC.loadDriver(m_destJdbc);
        } catch(ClassNotFoundException e) {
            throw new SQLException(e.toString());
        }
        
        m_srcConn  = DriverManager.getConnection(m_srcJdbc, m_srcUser,
                                                 m_srcPassword);
        m_srcConn.setAutoCommit(false);
        
        m_destConn = DriverManager.getConnection(m_destJdbc, m_destUser,
                                                 m_destPassword);                        
        m_destConn.setAutoCommit(false);
    }
    
    protected void closeConnections() {
        try { if(m_srcConn != null) m_srcConn.close(); }
        catch(SQLException e) {}
        
        try { if(m_destConn != null) m_srcConn.close(); }
        catch(SQLException e) {}
    }
}
