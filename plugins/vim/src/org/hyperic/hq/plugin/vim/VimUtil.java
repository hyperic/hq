/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.vim;

import java.util.Properties;

public class VimUtil {

    VimServiceConnection _conn;
    VimServiceUtil _util;

    public VimUtil() {
        _conn = new VimServiceConnection();
        _util = new VimServiceUtil(_conn);
    }

    public void init(Properties props) throws Exception {
        initServiceConnection(_conn, props);
    }

    public void dispose() {
        if (_conn != null) {
            try {
                _conn.disconnect();
            } catch (Exception e) {}
        }
        _conn = null;
        _util = null;
    }

    public VimServiceConnection getConn() {
        return _conn;
    }

    public VimServiceUtil getUtil() {
        return _util;
    }

    public static String getURL(Properties props) {
        return props.getProperty(VimCollector.PROP_URL);
    }

    private static void initServiceConnection(VimServiceConnection conn,
                                              Properties props)
        throws Exception {

        String url = getURL(props);
        String username = props.getProperty(VimCollector.PROP_USERNAME);
        String password = props.getProperty(VimCollector.PROP_PASSWORD);        
        conn.connect(url, username, password);
    }

    public static VimServiceConnection getServiceConnection(Properties props)
        throws Exception {

        VimServiceConnection conn = new VimServiceConnection();
        initServiceConnection(conn, props);
        return conn;
    }
}
