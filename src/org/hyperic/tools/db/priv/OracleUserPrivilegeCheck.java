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

package org.hyperic.tools.db.priv;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.hyperic.util.jdbc.DBUtil;

public class OracleUserPrivilegeCheck implements PrivilegeCheck {

    private static final String logCtx
        = OracleUserPrivilegeCheck.class.getName();

    private static final String sessionRoles = "SELECT ROLE FROM SESSION_ROLES";
    private static final String sessionPrivs = "SELECT PRIVILEGE FROM SESSION_PRIVS";

    private static final List privQualifiers = new ArrayList();
    static {
        // privQualifiers.add("CREATE SESSION");
        // privQualifiers.add("ALTER SESSION");
        // privQualifiers.add("UNLIMITED TABLESPACE");
        privQualifiers.add("CREATE TABLE");
        // privQualifiers.add("CREATE CLUSTER");
        // privQualifiers.add("CREATE SYNONYM");
        privQualifiers.add("CREATE VIEW");
        privQualifiers.add("CREATE SEQUENCE");
        // privQualifiers.add("CREATE DATABASE LINK");
        // privQualifiers.add("CREATE PROCEDURE");
        // privQualifiers.add("CREATE TRIGGER");
        // privQualifiers.add("CREATE TYPE");
        // privQualifiers.add("CREATE OPERATOR");
        // privQualifiers.add("CREATE INDEXTYPE");
    }

    private Connection conn;
    
    private OracleUserPrivilegeCheck() {} // no empty constructor, I need a connection
    
    public OracleUserPrivilegeCheck(Connection conn) {
        this.conn = conn;
    }

    public String isPrivileged() {
        String role;
        boolean rolesOK = false;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // Do the role qualifiers first
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sessionRoles);
            while(rs.next()) {
                role = rs.getString(1);
                if (role.equals("CONNECT")
                    || role.equals("DBA")) {
                    rolesOK = true;
                    break;
                }
            }
            if (!rolesOK) return "CONNECT or DBA role is required.";
            DBUtil.closeResultSet(logCtx, rs);

            rs = stmt.executeQuery(sessionPrivs);
            List privs = new ArrayList();
            while (rs.next()) {
                privs.add(rs.getString(1));
            }
            for (int i=0; i<privQualifiers.size(); i++) {
                String qualifier = (String) privQualifiers.get(i);
                if (!privs.contains(qualifier)) {
                    return qualifier + " privilege is required";
                }
            }

            // finally just try to create a table and drop it.
            try {
                stmt.execute("CREATE TABLE HQ_TEMP_PRIVCHECK "
                             + "( TEMPID INTEGER NOT NULL )");
                stmt.execute("DROP TABLE HQ_TEMP_PRIVCHECK");
            } catch (SQLException e) {
                return e.getMessage();
            }
            // we successfully connected and found all of the qualifying
            // attributes in our connection, so we're all good
            return null;

        } catch (SQLException e) {
            return "An error occurred while checking privileges: "
                + e.getMessage();

        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, stmt);
        }
    }

    public void cleanup () {
        DBUtil.closeConnection(logCtx, conn);
    }
}
