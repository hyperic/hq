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

package org.hyperic.hq.plugin.vmware;

import java.util.Properties;

import org.hyperic.sigar.vmware.ConnectParams;

public class VMwareConnectParams extends ConnectParams {
    //vmware client is not thread safe
    static final Object LOCK = new Object();

    public static final String PROP_AUTHD_PORT = "authd.client.port";

    static final int DEFAULT_AUTHD_PORT_INT = 0;

    public static final String DEFAULT_AUTHD_PORT =
        String.valueOf(DEFAULT_AUTHD_PORT_INT);

    public VMwareConnectParams(Properties props) {
        super(getHost(props),
              getPort(props),
              getUser(props),
              getPass(props));
    }

    private static int getPort(Properties props) {
        String port =
            sanitize(props.getProperty(PROP_AUTHD_PORT));
        if (port == null) {
            return DEFAULT_AUTHD_PORT_INT;
        }
        return Integer.parseInt(port);
    }

    private static String sanitize(String prop) {
        if ((prop == null) || (prop.length() == 0)) {
            return null;
        }
        if ((prop.charAt(0) == '%') &&
            (prop.charAt(prop.length()-1) == '%'))
        {
            return null;
        }
        return prop;
    }

    private static String getHost(Properties props) {
        return sanitize(props.getProperty("host"));
    }

    private static String getUser(Properties props) {
        return sanitize(props.getProperty("user"));
    }

    private static String getPass(Properties props) {
        return sanitize(props.getProperty("pass"));
    }
}
