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
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBMath {

    private static boolean hasCeil = false;

    public static void defineCeilFunction (String ctx,
                                           Connection conn) 
        throws SQLException {

        if (hasCeil) return;

        int dbType = DBUtil.getDBType(conn);

        // Assume pointbase is the only one we need to define this for.
        if (dbType != DBUtil.DATABASE_POINTBASE_4) {
            hasCeil = true;
            return;
        }

        PreparedStatement ps = null;
        try {
            try {
                ps = conn.prepareStatement("SELECT COUNT(ceil(12.1)) "
                                           + "FROM SYSUSERS");
                ps.executeQuery();
            } catch (SQLException e) {
                DBUtil.closeStatement(ctx, ps);
                ps = conn.prepareStatement
                    ("CREATE FUNCTION ceil (IN CT FLOAT) "
                     + "RETURNS int "
                     + "LANGUAGE Java "
                     + "NO SQL "
                     + "EXTERNAL NAME \"java.lang.Math::ceil\" "
                     + "PARAMETER STYLE SQL");
                ps.executeUpdate();
            }
            hasCeil = true;
        } finally {
            DBUtil.closeStatement(ctx, ps);
        }
    }
}
