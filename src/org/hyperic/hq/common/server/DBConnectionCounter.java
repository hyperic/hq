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

package org.hyperic.hq.common.server;

import java.sql.Connection;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.hyperic.util.leak.RemoteResourceCounter;

public class DBConnectionCounter {

    private static RemoteResourceCounter rcService = null;
    private static boolean isEnabled = false;
    public static final String JDBC_CONNECTION = "dbConnection";

    static {
        try {
            InitialContext ic = new InitialContext();
            rcService = (RemoteResourceCounter) 
                ic.lookup(RemoteResourceCounter.JNDI_NAME);
            isEnabled = rcService.getEnabled();

        } catch (NamingException ne) {
            throw new IllegalStateException
                ("Error looking up " + RemoteResourceCounter.JNDI_NAME 
                 + " in JNDI: " + ne.toString());
        }
    }

    public static boolean getEnabled () { return isEnabled; }

    public static void openConnection ( Connection conn ) {
        if (!isEnabled) return;
        rcService.openResource(JDBC_CONNECTION, conn);
    }

    public static void closeConnection ( Connection conn ) {
        if (!isEnabled) return;
        rcService.closeResource(JDBC_CONNECTION, conn);
    }


}
