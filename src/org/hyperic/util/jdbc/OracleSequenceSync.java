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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A class which scans all sequences for the specified user and resets them to begin
 * with a number that is guaranteed to be higher than the last number recorded in the 
 * table
 *
 */
public class OracleSequenceSync {

    private String jdbcUrl;
    private String user;
    private String password;
    private Connection conn;
    private boolean testMode = false;
    
    private static final String ctx = OracleSequenceSync.class.getName();
    
    private static final String GET_SEQUENCES =
        "SELECT SEQUENCE_NAME FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = ?";
            
    
    public static void main(String[] args) {
        boolean isTest = false;
        if(args.length < 3) {
            System.out.println("Usage: OracleSequenceSync jdbcUrl user pass");
            System.exit(-1);
        }
        if(args.length > 3) {
            isTest = new Boolean(args[3]).booleanValue();
        }
        try {
            OracleSequenceSync oss = 
                new OracleSequenceSync(args[0], args[1], args[2], isTest);
            oss.reinitSequences();    
        } catch (Exception e) {
            System.err.println("An error occured: " + e.getMessage());
        }
    }
    
    public OracleSequenceSync(String jdbcUrl, String user, String password, 
        boolean testMode) 
        throws SQLException {
        // validate that this is an oracle database we're looking at
        if(jdbcUrl.indexOf("oracle") == -1) {
            System.out.println("This tool can only be used against Oracle JDBC sources");
            System.exit(-1);
        }
        this.jdbcUrl = jdbcUrl;
        this.user = user.toUpperCase();
        this.password = password; 
        this.testMode = testMode;
        try {   
            JDBC.loadDriver(JDBC.ORACLE_NAME);
        } catch (ClassNotFoundException e) {
            System.out.println("Unable to load Oracle Driver: " + e.getMessage());
            System.out.println("Please check your classpath");
            System.exit(-1);
        }  
        System.out.println("Succesfully connected to: " + jdbcUrl);
    }
    
    public void reinitSequences() throws SQLException {
        try {
            conn = DriverManager.getConnection(jdbcUrl, user, password);
            // get a map of all the sequences and their tables
            Map allSequences = getSequenceList();
            dropAndRecreate(allSequences);
        } catch (SQLException e) {
            JDBC.printSQLException(e);
            throw e;
        } finally {
            DBUtil.closeConnection(ctx, conn);
        }
    }
    
    private void dropAndRecreate(Map seqMap) throws SQLException {
        PreparedStatement stmt = null;
        try {
            for(Iterator i = seqMap.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                // first get the current highest value of the key
                String seqName = (String)entry.getKey();
                String tableName = (String)(((Map)entry.getValue())).keySet().iterator().next();
                String keyName = (String)(((Map)entry.getValue())).values().iterator().next();
                System.out.println("Processing Sequence: " + seqName);
                
                int highestCurrKey = getHighestCurrKey(tableName, keyName);
                System.out.println("Current highest value of: " + keyName
                    + " for table: " + tableName + " is: " + highestCurrKey);
                // now drop the sequence and recreate it 
                int startingVal = highestCurrKey + 1000;
                if(!testMode) {
                    stmt = conn.prepareStatement("DROP SEQUENCE " + seqName);  
                    stmt.execute();
                    stmt = conn.prepareStatement("CREATE SEQUENCE " + seqName + 
                        " start with " + startingVal + " increment by 1 nocache nocycle");
                    stmt.execute();    
                } else {
                    System.out.println("TestMode specified... skipping");
                }
            }
        } finally {
            DBUtil.closeJDBCObjects(ctx, null, stmt, null);
        }
    }
    
    private int getHighestCurrKey(String table, String keyName) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT MAX(" + keyName + 
                ") FROM " + table);
            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);  
        } finally {
            DBUtil.closeJDBCObjects(ctx, null, stmt, rs);
        }
    }
    
    private Map getSequenceList() throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map seqMap;
        try {
            seqMap = new HashMap();
            stmt = conn.prepareStatement(GET_SEQUENCES);
            stmt.setString(1, user);
            rs = stmt.executeQuery();
            while(rs.next()) {
                String aSequence = rs.getString(1);
                seqMap.put(aSequence, getTableAndKeyFromSequence(aSequence));
            }
            return seqMap;
        } finally {
            DBUtil.closeJDBCObjects(ctx, null, stmt, rs);
        }
    }
    /**
     * Parse out the name of the table based on the name of its sequence.
     * This is based on the naming conventions used by DBSetup for sequence definition
     * which are:
     * SEQUENCE NAME: SOME_TABLE_NAME_KEY_SEQ
     * TABLE NAME: SOME_TABLE_NAME
     * KEY NAME: KEY
     * @param sequenceName
     * @return a map where the key is the name of the table and the value is the name of the
     * key the sequence is used by
     */
    private Map getTableAndKeyFromSequence(String sequenceName) {
        Map aMap = new HashMap();
        int idx = sequenceName.lastIndexOf("_");
        String tableAndKey = sequenceName.substring(0, idx);
        // now split the key and the table name
        idx = tableAndKey.lastIndexOf("_");
        String table = tableAndKey.substring(0, idx);
        String key = tableAndKey.substring(idx + 1);
        aMap.put(table, key);
        return aMap;
    }
}
