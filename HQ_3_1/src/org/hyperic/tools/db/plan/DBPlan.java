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

package org.hyperic.tools.db.plan;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;
import org.hyperic.util.jdbc.JDBC;

public class DBPlan
{
    private static final String EMPTY     = "";
    private static final String VERSION   = "DBPlan, Version 1.0.0";
    private static final String COPYRIGHT =
        "Copyright (C) Covalent Technologies, Inc., All Rights Reserved.";
    private static final String SYNTAX =
        "DBPlan <JDBC> <User> <Password> <Command>";
        
    private String     m_jdbc;
    private Connection m_conn;
    private String     m_user;
    private String     m_password;
    private String     m_cmd;
        
    public static void main(String[] args) {
        System.out.println(VERSION + '\n' + COPYRIGHT + '\n');

        if(args.length < 4) {
            System.out.println(SYNTAX);
            System.exit(-1);
        }
                                
        String jdbc      = args[0];
        String user      = args[1];
        String password  = args[2];
        String cmd       = args[3];
        
        DBPlan dbplan = new DBPlan(jdbc, user, password, cmd); 
                                   
        try {
            dbplan.plan();
        } catch(SQLException e) {
            JDBC.printSQLException(e); 
        } catch(Exception e) {
            System.out.println(e); 
        }
    }

    public DBPlan(String jdbc, String user, String password, String cmd) {
        m_jdbc     = jdbc;
        m_user     = user;
        m_password = password;
        m_cmd      = cmd;
    }
    
    public void plan() throws SQLException {
        this.openConnections();
        this.printPlan();
        this.closeConnections();
    }
    
    protected void printPlan() throws SQLException{
        try {
            Plan.createPlan(m_jdbc).printPlan(m_conn, m_cmd);        
        } catch(ClassNotFoundException e) {
            throw new SQLException(e.toString());
        }
    }
    
    protected void openConnections() throws SQLException {
        try {
            JDBC.loadDriver(m_jdbc);
        } catch(ClassNotFoundException e) {
            throw new SQLException(e.toString());
        }
        
        m_conn = DriverManager.getConnection(m_jdbc, m_user, m_password);
        m_conn.setAutoCommit(false);
    }
    
    protected void closeConnections() {
        try { if(m_conn != null) m_conn.close(); } catch(SQLException e) {}
    }
    
    protected static void printResults(ResultSet result) throws SQLException {
        // Look at the meta data for the src to know what to get
        ResultSetMetaData meta = result.getMetaData();                   

        int[]    len  = new int[meta.getColumnCount()];
        Vector[] cols = new Vector[meta.getColumnCount()];
        for(int i = 0;i < cols.length;i++)
            cols[i] = new Vector();
        
        // Get the labels
        for(int col = 0;col < meta.getColumnCount();col++) {
            String label = meta.getColumnLabel(col+1);
            if(label == null)
                label = EMPTY;
                
            len[col] = Math.max(len[col], label.length());
            cols[col].add(label);
        }
        
        // Get the data
        for(int row = 1;result.next() == true;row ++) {
            for(int col = 0;col < meta.getColumnCount();col++) {
                String data = result.getString(col+1);
                if(data == null)
                    data = EMPTY;

                len[col] = Math.max(len[col], data.length());
                cols[col].add(data);
            }
        }
        
        // Print the results
        for(int row = 0;row < cols[0].size();row++) {
            for(int col = 0;col < cols.length;col++) {
                String padded =
                    pad((String)cols[col].get(row), len[col] + 1, ' ');
                System.out.print(padded);
            }
            
            System.out.println();
        }
    }

    private static String pad(String value, int length, char ch) {
        StringBuffer padder = new StringBuffer(value);
        if (value.length() < length) {
            for (int i=0; i < (length - value.length()); i++) {
                padder.append(ch);
            }
        }
        return padder.toString();
    }
}
