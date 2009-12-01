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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.oro.text.perl.Perl5Util;

abstract class DataSet
{
    private DBSetup m_parent;
    private String  m_strTableName;
    private static Perl5Util regexp = new Perl5Util();
    
    protected DataSet(String tableName, DBSetup dbsetup)
    {
        this.m_strTableName = tableName;
        this.m_parent = dbsetup;
    }
    
    protected int create() throws SQLException
    {
        PreparedStatement stmt   = null;
        int               rowcnt = 0;
        
        try {
        for(;this.next();rowcnt ++)
        {
            // We only prepare the statement on the first row. We do this 
            // inside the loop because determining the number of rows may 
            // not be possible before iterating.
            if(rowcnt == 0)
            {
                String strCmd = this.getCreateCommand();
                stmt = (PreparedStatement)m_parent.doSQL(strCmd, true);
            }
        
            // For all rows we set the data in the prepared statement and execute it.
            if(this.setSqlData(stmt))
            {
                stmt.executeUpdate();
                this.m_parent.getConn().commit();
            }
        }
        } catch (SQLException e) {
            try { this.m_parent.getConn().rollback(); } 
            catch (Exception e2) {
                // Log this?
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
        }
        
        return rowcnt; // The number of rows created.
    }

    protected String getCreateCommand() throws SQLException
    {
        int iCols = this.getNumberColumns();
        
        StringBuffer strCmd = new StringBuffer("INSERT INTO ");
        strCmd.append(this.getTableName());
        strCmd.append(" (");
        
        for(int i = 0;i < iCols;i++)
        {
            Data data = this.getData(i);
            
            if(i > 0)
                strCmd.append(',');
                
            strCmd.append(data.getColumnName());
        }
        
        strCmd.append(") VALUES (");
     
        for(int i = 0;i < iCols;i ++)
        {
            if(i > 0)
                strCmd.append(',');
                
            strCmd.append('?');
        }
        
        strCmd.append(')');
        
        return strCmd.toString();
    }

    protected String getUpdateCommand(String strCol) throws SQLException
    {
        int iCols = this.getNumberColumns();
        
        StringBuffer strCmd = new StringBuffer("UPDATE ");
        strCmd.append(this.getTableName());
        strCmd.append(" SET ");
        
        for(int i = 0;i < iCols;i++)
        {
            Data data = this.getData(i);

            if(i > 0)
                strCmd.append(',');
                
            strCmd.append(data.getColumnName());
            strCmd.append(" = ?");
        }
        
        strCmd.append(" WHERE ").append(strCol).append(" = ?");

        
        return strCmd.toString();
    }
    
    protected boolean setSqlData(PreparedStatement stmt) throws SQLException
    {
        Statement   stmtQuery = null;
        boolean     bResult = true;
        ResultSet   results = null;
        String      strSelect;
        String      cid = "0";
        
        int iCols = this.getNumberColumns();
        // This may seem kludgy, but beats the alternative
        for(int i = 0; i < iCols; i++) {

            Data   data     = this.getData(i);
            
            //////////////////////////////////////////////////////////
            // Update the row if it already exists
            
            if(data.getColumnName().equalsIgnoreCase("CID") ||
               data.getColumnName().equalsIgnoreCase("ID"))
            {
                String strCol = data.getColumnName();
                String strValue = data.getValue();
                strSelect = "SELECT " + strCol + " FROM " + this.getTableName() +
							" WHERE " + strCol +" = " + strValue;
                
                try {
                    stmtQuery = m_parent.getConn().createStatement();
                    results   = stmtQuery.executeQuery(strSelect);
                
                    boolean bExists = results.next();

                    if(bExists) {           // If the row exists already, we update
                        bResult = false;    // Return false in the end
                        cid = strValue;
                        String strCmd = this.getUpdateCommand(strCol);
                        // before we overwrite stmt, close a previous statement
                        // if there was one
                        if (stmt != null) stmt.close();
                        stmt = (PreparedStatement)m_parent.doSQL(strCmd, true);
                    }
                } finally {
                    if (results != null) {
                        try { results.close(); } catch (SQLException e2) {}
                    }
                    if (stmtQuery != null) {
                        try { stmtQuery.close(); } catch (SQLException e2) {}
                    }
                }
                break;
            }                
        }

        for(int i = 0;i < iCols;i++)
        {
            Data   data     = this.getData(i);
            String strValue = data.getValue();
            
            //////////////////////////////////////////////////////////
            // Look for data references and translate them

            if((strValue.length() > 1) && 
                strValue.charAt(0) == '%' && strValue.charAt(1) != '%' &&
                regexp.match("m/^%[A-Z]+\\w+\\.[A-Z]+\\w+:\\d+$/i", strValue))
            {
                int    iColDelim  = strValue.indexOf('.');
                int    iDataDelim = strValue.indexOf(':');
                String strTab     = strValue.substring(1, iColDelim);
                String strCol     = strValue.substring(iColDelim + 1,
                                                       iDataDelim);
                String strSID     = strValue.substring(iDataDelim + 1);
            
                strSelect = "SELECT " + strCol + " FROM " + strTab +
							" WHERE " + strCol + " = "  + strSID;
                try {
                    stmtQuery = m_parent.getConn().createStatement();
                    results   = stmtQuery.executeQuery(strSelect);
            
                    if(results.next()) {
                        strValue = results.getString(1);
                    } else {
                        strValue = null;    // No foreign key
                    
                        if(m_parent.isQuiet() == false)
                            System.out.println("Error: Cannot find Foreign Key: \'" + strSelect + '\'');
                    }
                } finally {
                    if (results != null) {
                        try { results.close(); } catch (SQLException e2) {}
                    }
                    if (stmtQuery != null) {
                        try { stmtQuery.close(); } catch (SQLException e2) {}
                    }
                }
            }
        
            if (strValue != null) {
                if (strValue.equals("TRUE")) {
                    stmt.setBoolean(i + 1, true);
                    continue;
                }
                else if (strValue.equals("FALSE")) {
                    stmt.setBoolean(i + 1, false);
                    continue;
                }
                
                stmt.setString(i + 1, strValue);
            }
            else {
                // If the strVal is null, means that the foreign key is null
                stmt.setNull(i + 1, java.sql.Types.INTEGER);
            }
        }
        
        // If we don't want to return to caller, then we finish it right here
        if (!bResult) {
            stmt.setString(iCols + 1, cid);
            try {
                stmt.executeUpdate();
                this.m_parent.getConn().commit();
            } catch (SQLException e) {
                try { this.m_parent.getConn().rollback(); } 
                catch (Exception e2) {
                    // Log this?
                }
                throw e;
            }
        }

        return bResult;
    }
    
    protected int getNumberColumns()
    {
        return 0;
    }

    protected String getTableName()
    {
        return this.m_strTableName;
    }

    protected abstract Data    getData(int columnIndex);
    protected abstract boolean next();
}
