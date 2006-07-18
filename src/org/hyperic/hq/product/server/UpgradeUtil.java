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

package org.hyperic.hq.product.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.naming.InitialContext;

import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.DBUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Upgrade utility to handle tasks that cannot be done easily by DBSetup
 */
public class UpgradeUtil {

    private static Log log = LogFactory.getLog(UpgradeUtil.class.getName());
    
    /**
     * This method removes the old 2.1 HQ JBoss and HQ Tomcat resources.  These
     * components have been upgraded in HQ 2.5.
     */
    public static void removeOldResources() 
    {
        Connection conn = null;
        Statement  stmt = null;
        ResultSet  rs   = null;
        try {
            String sql = "SELECT id, name FROM EAM_SERVER WHERE " +
                "AUTOINVENTORYIDENTIFIER='HQ JBoss' OR " +
                "AUTOINVENTORYIDENTIFIER='HQ Tomcat'";

            conn = DBUtil.getConnByContext(new InitialContext(), 
                                           HQConstants.DATASOURCE);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            AuthzSubjectValue overlord = AuthzSubjectManagerUtil.
                getLocalHome().create().findOverlord();
            ServerManagerLocal serverMgr = 
                ServerManagerUtil.getLocalHome().create();

            log.info("Removing old resources");
            while (rs.next()) {
                Integer serverId = new Integer(rs.getInt(1));
                serverMgr.removeServer(overlord, serverId, true);
                log.info("Removed " + rs.getString(2));
            }

        } catch (Exception e) {
            log.error("Unable to remove old resources: " + e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, stmt, rs);
        }
    }
}
