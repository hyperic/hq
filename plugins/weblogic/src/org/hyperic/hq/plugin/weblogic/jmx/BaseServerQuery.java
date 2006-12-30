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

package org.hyperic.hq.plugin.weblogic.jmx;

import java.util.HashMap;
import java.util.Properties;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.hyperic.hq.product.ServerControlPlugin;

import org.hyperic.hq.plugin.weblogic.WeblogicControlPlugin;

public class BaseServerQuery extends WeblogicQuery {

    private static HashMap addressMap = new HashMap();

    protected static final String ATTR_LISTEN_ADDR =
        "ListenAddress";

    protected static final String ATTR_LISTEN_PORT =
        "ListenPort";

    protected static final String ATTR_SSL_LISTEN_PORT =
        "SSLListenPort";

    public String getResourceName() {
        return getResourceType();
    }

    public String getResourceFullName() {
        return
            getResourceType() + " " + getFullName();
    }

    public String getInstallPath() {
        return null;
    }

    protected String getControlProgram() {
        return null;
    }

    public String getIdentifier() {
        return getInstallPath();
    }

    public Properties getControlConfig() {

        Properties props = new Properties();
        props.setProperty(ServerControlPlugin.PROP_PROGRAM,
                          getControlProgram());
        props.setProperty(WeblogicControlPlugin.PROP_TIMEOUT,
                          String.valueOf(WeblogicControlPlugin.DEFAULT_TIMEOUT));
        return props;
    }

    public boolean isAdmin() {
        return false;
    }

    public String getListenAddress() {
        String addr = getAttribute(ATTR_LISTEN_ADDR);
        if ((addr == null) || (addr.length() == 0)) {
            return "localhost";
        }
        else {
            return addr;
        }
    }

    public String getListenPort() {
        return getAttribute(ATTR_LISTEN_PORT);
    }

    public String getSSLListenPort() {
        return getAttribute(ATTR_SSL_LISTEN_PORT);
    }

    public String getFqdn() {
        String address = getListenAddress();

        //even if InetAddress is caching, this is likely faster.
        String fqdn = (String)addressMap.get(address);

        if (fqdn == null) {
            try {
                fqdn = InetAddress.getByName(address).getHostName();
            } catch (UnknownHostException e) {
                // fallback to regular address - but this might be a bad idea
                // because we might get duplicate platforms... hmmm....
                fqdn = address;
            }

            addressMap.put(address, fqdn);
        }

        return fqdn;
    }
}
