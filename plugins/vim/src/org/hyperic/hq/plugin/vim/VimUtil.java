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

import org.hyperic.hq.product.Collector;

public class VimUtil {

    public static String getURL(Properties props) {
        String host = props.getProperty(Collector.PROP_HOSTNAME);
        String port = props.getProperty(Collector.PROP_PORT);
        return "https://" + host + ":" + port + "/sdk";        
    }

    public static VimServiceConnection getServiceConnection(Properties props)
        throws Exception {

        String url = getURL(props);
        String username = props.getProperty(Collector.PROP_USERNAME);
        String password = props.getProperty(Collector.PROP_PASSWORD);
        VimServiceConnection conn = new VimServiceConnection();
        conn.connect(url, username, password);
        return conn;
    }
}
