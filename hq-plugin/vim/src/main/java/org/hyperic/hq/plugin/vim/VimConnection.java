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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

public class VimConnection {
    private static final Log _log =
        LogFactory.getLog(VimConnection.class.getName());
    private static final Map _connections = new HashMap();

    final Object LOCK = new Object();
    VimUtil vim;
    private String _user = "", _pass = "";

    private static String address(Object obj) {
        return "@" + Integer.toHexString(obj.hashCode());
    }

    private static String mask(String val) {
        return val.replaceAll(".", "*");
    }

    private static String diff(String old, String cur) {
        return "'" + old + "'->'" + cur + "'";  
    }

    static synchronized VimConnection getInstance(Properties props)
        throws PluginException {

        String url = VimUtil.getURL(props);
        String username = props.getProperty(VimUtil.PROP_USERNAME, "");
        String password = props.getProperty(VimUtil.PROP_PASSWORD, "");

        VimConnection conn = (VimConnection)_connections.get(url);
        if (conn == null) {
            conn = new VimConnection();
            _connections.put(url, conn);
            _log.debug("Creating cache for: " + url);
        }
        else {
            boolean requiresReconnect = false;
            boolean usernameChanged = !username.equals(conn._user);
            boolean passwordChanged = !password.equals(conn._pass);
            if (usernameChanged || passwordChanged) {
                requiresReconnect = true;
                if (_log.isDebugEnabled()) {
                    String diff = "";
                    if (usernameChanged) {
                        diff += "user:" + diff(conn._user, username);
                    }
                    if (passwordChanged) {
                        if (diff.length() != 0) {
                            diff += ",";
                        }
                        diff += "pass:" + diff(mask(conn._pass), mask(password));
                    }
                    _log.debug("Credentials changed (" + diff +
                               ") reconnecting cached connection for: " + url);                    
                }
            }
            else {
                if (conn.vim != null) {
                    requiresReconnect = !conn.vim.isSessionValid();
                }
                // Else, some previous error must have happened -- expect it to be already logged.
            }
            if (requiresReconnect) {
                VimUtil.dispose(conn.vim);
                _log.debug("Closed previous connection (" +
                           address(conn.vim) + ") for: " + url); 
                conn.vim = null;
            }
        }
        if (conn.vim == null) {
            conn.vim = VimUtil.getInstance(props);
            _log.debug("Opened new connection (" +
                       address(conn.vim) + "/" +
                       address(conn.LOCK) + ") for: " + url);
            conn._user = username;
            conn._pass = password;
        }
        return conn;
    }
}
