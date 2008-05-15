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

package org.hyperic.hq.plugin.xen;

import java.util.Properties;
import java.util.StringTokenizer;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;

public class XenUtil {
    static final String PROP_URL      = "url";
    static final String PROP_USERNAME = Collector.PROP_USERNAME;
    static final String PROP_PASSWORD = Collector.PROP_PASSWORD;
    static final String[] CONNECT_PROPS = { PROP_URL, PROP_USERNAME, PROP_PASSWORD };
    static final String PROP_PLATFORM_UUID = "platform.uuid";
    static final String PROP_SERVER_UUID = "server.uuid";
    static final String PROP_SERVICE_UUID = "service.uuid";
    static final String PROP_LOCATION = "location";
    static final String PROP_DEVICE = "device";
    static final String PROP_TYPE = "type";
    static final String PROP_MAC = "mac";
    static final String PROP_MTU = "mtu";
    static final String PROP_IP = "address";
    static final String PROP_NETMASK = "netmask";
    static final String PROP_GATEWAY = "gateway";
    static final String PROP_DNS = "dns";

    static final String TYPE_STORAGE = "Storage";
    static final String TYPE_NIC = "NIC";
    static final String TYPE_CPU = "CPU";

    static String getProperty(Properties props, String key)
        throws PluginException {

        String val = props.getProperty(key);
        if (val == null) {
            throw new PluginException("Missing property '" + key + "'");
        }
        return val;
    }

    static Connection connect(Properties props) throws PluginException {
        String url = getProperty(props, PROP_URL);
        String username = getProperty(props, PROP_USERNAME);
        String password = getProperty(props, PROP_PASSWORD);
        try {
            return new Connection(url, username, password);
        } catch (Exception e) {
            throw new PluginException("Session setup error: " + e.getMessage(), e);
        }
    }

    static Host getHost(Connection conn, Properties props) throws PluginException {
        return getHost(conn, getProperty(props, PROP_PLATFORM_UUID));
    }

    static boolean isUUID(String uuid) {
        return
            (uuid.length() == 36) &&
            ((new StringTokenizer(uuid, "-")).countTokens() == 5);
    }

    static Host getHost(Connection conn, String uuid) throws PluginException {
        if (isUUID(uuid)) {
            try {
                return Host.getByUuid(conn, uuid);
            } catch (Exception e) {
                throw new PluginException("Host.getByUuid(" + uuid + "): " +
                                          e.getMessage(), e);
            }
        }

        try {
            return (Host)Host.getByNameLabel(conn, uuid).iterator().next();    
        } catch (Exception e) {
            throw new PluginException("Host.getByNameLabel(" + uuid + "): " +
                                      e.getMessage(), e);
        }
    }
}
