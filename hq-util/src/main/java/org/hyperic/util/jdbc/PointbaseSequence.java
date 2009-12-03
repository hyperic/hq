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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PointbaseSequence
{
	private static final String	CREATE_SEQUENCE_TABLE
		= "CREATE TABLE COV_SEQUENCE (NAME VARCHAR(128) PRIMARY KEY, " +
		  "NEXTVAL INTEGER NOT NULL, INCREMENT INTEGER NOT NULL)";
		
	private static final String DROP_SEQUENCE_TABLE
		= "DROP TABLE COV_SEQUENCE";
		
	private static final String SELECT_SEQUENCE_COUNT
		= "SELECT COUNT(*) FROM COV_SEQUENCE";
	
	public static void cleanupSequences(Connection conn) throws SQLException
	{
		ResultSet rset = null;
		Statement stmt = null;
		Statement stmtQuery = conn.createStatement();
		
		try {
			rset = stmtQuery.executeQuery(PointbaseSequence.SELECT_SEQUENCE_COUNT);
			
			if(rset.next() == true)
			{
				int cnt = rset.getInt(1);
				rset.close();
		
				if(cnt == 0)
				{
					stmt = conn.createStatement();
					stmt.executeUpdate(PointbaseSequence.DROP_SEQUENCE_TABLE);
				}
			}
			
		}
		catch(SQLException e) {
			// We'll eat the exception which means COV_SEQUENCE
			// doesn't exist. That's our goal.
		}
		finally {
			if(rset != null)
				rset.close();
			
			if(stmt != null)
				stmt.close();
				
			if(stmtQuery != null)
				stmtQuery.close();
		}
	}
	
	public static void create(Connection conn, String name, int startVal, int increment) throws SQLException
	{
		PointbaseSequence.initSequences(conn);

		Statement stmt = null;
				
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate( PointbaseSequence.getCreateCommand(name, startVal, increment) );
		}
		catch(SQLException e) {
			throw e;
		}
		finally {
			if(stmt != null)
				stmt.close();
		}
	}

	public static void drop(Connection conn, String name) throws SQLException
	{
		Statement stmt = null;
		
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM COV_SEQUENCE WHERE NAME = \'" + name.toUpperCase() + '\'');
		}
		catch(SQLException e) {
			throw e;
		}
		finally {
			if(stmt != null)
				stmt.close();
		}
		
		PointbaseSequence.cleanupSequences(conn);
	}

	public static String getCreateCommand(String name, int startVal, int increment) {
		return "INSERT INTO COV_SEQUENCE VALUES (\'"
			+ name.toUpperCase() + "\', "
			+ startVal + ", "
			+ increment + ')';
	}

	public static String getDropCommand(String name) {
		return "DELETE FROM COV_SEQUENCE WHERE NAME = \'"
			+ name.toUpperCase() + '\'';
	}
	
	public static String getIncrementCommand(String name, int increment) {
		return "UPDATE COV_SEQUENCE SET NEXTVAL = NEXTVAL + " + increment
			+ " WHERE NAME = \'" + name.toUpperCase() + '\'';
	}

	public static String getSelectNextValCommand(String name) {
		return "SELECT NEXTVAL FROM COV_SEQUENCE WHERE NAME = \'"
			+ name.toUpperCase() + '\'';
	}
			
	public static void initSequences(Connection conn) throws SQLException
	{
		Statement stmt = conn.createStatement();
		
		try
		{
			stmt.executeUpdate(PointbaseSequence.CREATE_SEQUENCE_TABLE);
		}
		catch(SQLException e) {
			// We'll eat the exception which means COV_SEQUENCE
			// already exist. That's our goal.
		}
		finally
		{
			stmt.close();
		}
	}
}
