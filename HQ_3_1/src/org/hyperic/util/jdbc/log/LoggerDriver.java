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

package org.hyperic.util.jdbc.log;

import java.util.Properties;
import java.sql.*;

public class LoggerDriver implements Driver {

    public static final String JDBC_URL_PREFIX  = "jdbc:covalent-log:";
    public static final String PROP_RESTRACKER = "jdbcResourceTracker";

    static {
        try {
            DriverManager.registerDriver(new LoggerDriver());
        } catch (Exception e) {
            System.err.println("LoggerDriver: Could not register "
                               + "with DriverManager: " + e);
        }
    }

    public boolean acceptsURL(String url) throws SQLException {
        // System.err.println("acceptsURL: '" + url + "'");
        return url.startsWith(JDBC_URL_PREFIX);
    }

    public DriverPropertyInfo[] getPropertyInfo(String url,
                                                Properties info)
        throws SQLException {

        return null;
    }

    public int getMajorVersion() {
        return 1;
    }

    public int getMinorVersion() {
        return 0;
    }

    public boolean jdbcCompliant() {
        return false;
    }

    public Connection connect(String url,
                              Properties info)
        throws SQLException {

        String url2 = url;

		// Make sure we are being asked for a connection on a logger connect
		// string. If a url is passed to the jdbc driver manager that doesn't
		// match any jdbc driver, all drivers are asked to connect with it.
		// We need to filter this behavior.
		if(!url2.startsWith(JDBC_URL_PREFIX)) {
            return null;
        }
		 
        // Strip leading prefix
        if ( url2.length() > JDBC_URL_PREFIX.length() ) {
            url2 = url2.substring(JDBC_URL_PREFIX.length());

            int colonPos = url2.indexOf(":");
            if ( colonPos == -1 ) { 
                throw new SQLException("Unrecognized url: " + url);
            }

            String driver = url2.substring(0,colonPos);
            url2 = url2.substring(colonPos+1);
            try {
                Class.forName(driver).newInstance();
            } catch ( Exception e ) {
                throw new SQLException("Error loading logger sub-driver: "
                                       + driver + ": " + e);
            }

            String user     = info.getProperty("user");
            String password = info.getProperty("password");
            Connection conn = DriverManager.getConnection(url2, user, password);

            return new LoggerConnection(this, conn);
        } else {
            String msg = ( new StringBuffer("Cannot strip off ") ).
                append(JDBC_URL_PREFIX.length()).
                append(" characters of url because \n").
                append("it is too short: ").
                append(url).
                toString();
            throw new SQLException(msg);
        }
    }
}
