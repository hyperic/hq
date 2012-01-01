/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.install;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hibernate.dialect.HQDialectUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.jdbc.DriverLoadException;
import org.hyperic.util.jdbc.JDBC;

public class InstallDBUtil {

	private static final Log log = LogFactory.getLog(InstallDBUtil.class);

	public static boolean checkTableExists(Connection conn, String table) throws SQLException {

		HQDialect dialect = HQDialectUtil.getHQDialect(conn);

		Statement stmt = null;
		boolean exists = false;

		try {
			stmt = conn.createStatement();
			exists = dialect.tableExists(stmt, table);
		} finally {
			DBUtil.closeStatement(log, stmt);
		}

		return exists;
	}

	public static boolean checkTableExists(String url, String user, String password, String table)
			throws DriverLoadException, SQLException {

		try {
			Class.forName(JDBC.getDriverString(url)).newInstance();
		} catch (Exception e) {
			throw new DriverLoadException(e);
		}
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, user, password);
			return checkTableExists(conn, table);

		} finally {
			DBUtil.closeConnection(log, conn);
		}
	}

	public static boolean checkConnectionExists(String url, String user, String password)  {

		String	driver = JDBC.getDriverString(url);
		try {
			JDBC.loadDriver(driver);
			DriverManager.getConnection(url, user, password);
		} 
		catch (Exception e) {
			return false;
		}
		return true;
	}
	
}
