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

package org.hyperic.tools.db;

import java.sql.*;
import java.io.*;
import java.util.*;
import org.hyperic.util.*;
import org.hyperic.util.jdbc.*;

public class DBSQL {

    public static final String ctx = DBSQL.class.getName();

    private String _driver = null;
    private String _url = null;
    private String _user = null;
    private String _password = null;

    public static final void main ( String[] args ) {
        DBSQL d = new DBSQL();
        try {
            if ( d.init(args) ) {
                d.run();
            }
        } catch ( Exception e ) {
            System.err.println("BONK! " + e.toString());
            e.printStackTrace();
        }
    }

    public DBSQL () {}

    public boolean init (String[] args) throws Exception {
        if ( args.length != 2 && args.length != 4 ) {
            printUsage();
            return false;
        }
        _driver = args[0];
        _url = args[1];
        if ( args.length == 4 ) {
            _user = args[2];
            _password = args[3];
        }
        Class.forName(_driver).newInstance();
        return true;
    }

    private String stripSQLComments ( String sql ) {
        if ( sql == null ) return null;
        String rstr = "";
        StringTokenizer st = new StringTokenizer(sql, "\n");
        String line = null;
        while ( st.hasMoreTokens() ) {
            line = st.nextToken().trim();
            if ( !line.startsWith("--") ) rstr += line + " ";
        }
        return rstr;
    }

    public void run () throws Exception {

        String anErr = null;
        Connection conn = null;
        String sql = "";

        String results = "";
        List sqlList = new Vector();
        String line = null;
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        try {
            // Read everything from stdin
            line = r.readLine();
            while ( line != null ) {
                sql += line + " ";
                line = r.readLine();
            }

            // Only supply user/password to DriverManager if at least one 
            // was given by the user
            if ( (_user == null || _user.length()     == 0) &&
                 (_password == null || _password.length() == 0) ) {
                conn = DriverManager.getConnection(_url);
            } else {
                conn = DriverManager.getConnection(_url, _user, _password);
            }
            
            String fullsql = sql;
            StringTokenizer st = new StringTokenizer(fullsql, ";");
            while ( st.hasMoreTokens() ) {
                sql = stripSQLComments(st.nextToken()).trim();
                if ( sql.endsWith(";") ) sql = sql.substring(0,sql.length()-1);
                if ( sql.length() > 0 ) {
                    sqlList.add(sql);
                }
            }
            
            int numStatements = sqlList.size();
            for ( int i=0; i<numStatements; i++ ) {
                sql = sqlList.get(i).toString();
                results += processSQL(conn, sql, i, numStatements) + "<hr>";
            }
            
        } catch ( Exception e ) {
            anErr = "Error: " + e.toString() + "<br>"
                + "SQL was:" + sql + "<br>"
                + "StackTrace: " + StringUtil.getStackTrace(e);

        } finally {
            DBUtil.closeConnection(ctx, conn);
            try { r.close(); } catch ( Exception e ) {}
        }

        System.out.println("<HTML>" + results + "</HTML>");
    }

    public void printUsage () {
        System.err.println( "\n\n"
                            + "java " + getClass().getName() 
                            + " <driver> <url> [<user> <password>] "
                            + "\n\nSQL commands are read from stdin."
                            + "\n\nHTML results are written to stdout."
                            + "\n\n");
    }

    private String processSQL ( Connection conn, String sql, int index, int numStatements ) throws SQLException {
        
        String rstr = "";
        PreparedStatement statement = null;
        ResultSet rs = null;
        int columnCount = -1;
        int i;
        String aValue = null;
        
        
        try {
            if ( sql == null ) return rstr;
            
            if ( sql.length() > 0 ) {
                String LCsql = sql.toLowerCase();
                
                statement = conn.prepareStatement(sql);
                
                if ( LCsql.startsWith("select") || 
                     LCsql.startsWith("values") ) {
                    rs = statement.executeQuery();
                    ResultSetMetaData rsMD = rs.getMetaData();
                    columnCount = rsMD.getColumnCount();
                    
                    // Generate title row
                    String aHeaderRow = "<tr>";
                    for ( i=1; i<=columnCount; i++ ) {
                        aHeaderRow += "<th><font face=\"Verdana,Arial,Helvetica\" size=\"-2\">"
                            + rsMD.getColumnName(i)
                            + "</font></th>"
                            ;
                    }
                    aHeaderRow += "</tr>";
                    
                    rstr += "Results for statement " + (index+1) + " of " + numStatements + ":"
                        + "<br>" + sql + " "
                        + "<table border=1>"
                        + aHeaderRow;
                
                    // Output data rows
                    boolean hasResults = false;
                    while ( rs.next() ) {
                        hasResults = true;
                        rstr += "<tr>";
                        for ( i=1; i<=columnCount; i++ ) {
                            aValue = rs.getString(i);
                            if ( aValue == null || rs.wasNull() ) aValue = "NULL";
                            rstr += "<td valign=\"top\"><font face=\"Verdana,Arial,Helvetica\" size=\"-2\">"
                                + aValue + "</font></td>";
                        }
                        rstr += "</tr>";
                    }
                    if ( !hasResults ) {
                        rstr += "<tr>"
                            + "<td colspan=" + columnCount + "><b><font face=\"Verdana,Arial,Helvetica\">Query returned empty set.</font></b></td>"
                            + "</tr>";
                    }
                    
                    rstr += aHeaderRow + "</table>";
                    
                } else if ( LCsql.startsWith("update") || 
                            LCsql.startsWith("insert") || 
                            LCsql.startsWith("delete") ||
                            LCsql.startsWith("create") ||
                            LCsql.startsWith("drop")   ||
                            LCsql.startsWith("set") ) {
                    statement.executeUpdate();
                    rstr = "Command (statement " + (index+1) + " of " + numStatements + ") "
                        + "Executed Successufully:<br>" + sql + "<br>";
                    
                } else {
                    rstr = "Invalid SQL Command: " + sql + "<br>"
                        + "The first word must be "
                        + "SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, or SET.";
                }
            }
        } finally {
            DBUtil.closeStatement(ctx, statement);
            DBUtil.closeResultSet(ctx, rs);
        }

        return rstr;
    }
}
