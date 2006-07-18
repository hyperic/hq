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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.hyperic.util.jdbc.JDBC;

public class SpeedTestThread extends Thread
{
    private final String m_strDB;
    private final String m_strUsername;
    private final String m_strPassword;
    private final String m_strTableName;

    private final int    m_iStart;
    private final int    m_iEnd;

    private boolean      m_bResult;

    protected SpeedTestThread(String database, String username, String password, String tableName, int start, int end)
    {
        this.m_strDB        = database;
        this.m_strUsername  = username;
        this.m_strPassword  = password;
        this.m_strTableName = tableName;
        this.m_iStart       = start;
        this.m_iEnd         = end;
    }

    public void run()
    {
        String     strCmd;
        Connection conn = null;
        Statement  stmt = null;

        try
        {
            conn = DriverManager.getConnection(this.m_strDB, this.m_strUsername, this.m_strPassword);
            conn.setAutoCommit(false);

            /////////////////////////////////////////////////////////
            // INSERT ROWS

            stmt = conn.createStatement();

            for(int i = this.m_iStart;i <= this.m_iEnd;i++)
                stmt.executeUpdate("INSERT INTO " + this.m_strTableName + " VALUES(" + i + ", 'Mark Douglas', '645 Howard Street', 'San Francisco', 'CA', '94105')");

            conn.commit();


            /////////////////////////////////////////////////////////
            // DELETE THE ROWS

            for(int i = this.m_iStart;i <= this.m_iEnd;i++)
                stmt.executeUpdate("DELETE FROM " + this.m_strTableName + " WHERE ID = " + i);

            conn.commit();

            this.m_bResult = true;
        }
        catch(SQLException e)
        {
            JDBC.printSQLException(e);
        }
        catch(Exception e)
        {
            System.out.println("Error: " + e);
        }
        finally
        {
            // Clean-up the statement object
            if(stmt != null)
            {
                try
                {
                    stmt.close();
                    conn.close();
                }
                catch(Exception e)
                {
                }
            }
        }
    }

    public boolean getResult()
    {
        return this.m_bResult;
    }
}