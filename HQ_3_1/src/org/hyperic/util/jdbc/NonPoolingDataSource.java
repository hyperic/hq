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

import java.io.PrintWriter;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A DataSource that does not do connection pooling.  Useful for when
 * your connection pool sucks and you just want things to work.
 */
public class NonPoolingDataSource implements DataSource {

    private String driver   = null;
    private String url      = null;
    private String user     = null;
    private String password = null;

    private PrintWriter writer = null;
    private Integer loginTimeout = null;

    public NonPoolingDataSource () {}

    public void init (String driver,
                      String url,
                      String user,
                      String password) {
        this.driver   = driver;
        this.url      = url;
        this.user     = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
    public Connection getConnection(String otherUser,
                                    String otherPass) throws SQLException {
        return DriverManager.getConnection(url, otherUser, otherPass);
    }

    public PrintWriter getLogWriter() throws SQLException {
        if (writer == null) {
            writer = new PrintWriter(System.out);
        }
        return writer;
    }
    public void setLogWriter(PrintWriter out) throws SQLException {
        writer = out;
    }

    public int getLoginTimeout() throws SQLException {
        if (loginTimeout == null) return 5;
        return loginTimeout.intValue();
    }
    public void setLoginTimeout(int seconds) throws SQLException {
        loginTimeout = new Integer(seconds);
    }
}
