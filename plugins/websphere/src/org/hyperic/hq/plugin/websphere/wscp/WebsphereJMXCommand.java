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

package org.hyperic.hq.plugin.websphere.wscp;

import java.util.Properties;
import java.util.HashMap;

/**
 * Convert JMX property string to WebsphereCommand.
 */
public class WebsphereJMXCommand {

    private static HashMap cache = new HashMap();

    public static WebsphereCommand convert(Properties props) {
        WebsphereCommand cmd = (WebsphereCommand)cache.get(props);

        if (cmd == null) {
            //every command needs the Node
            String node = props.getProperty("Node");
            String leaf;

            if ((leaf = props.getProperty("Module")) != null) {
                //Node=%server.node%,AppServer=%server.name%,Module=%webapp%
                cmd = new ModuleCommand(node,
                                        props.getProperty("Application"),
                                        leaf);
            }
            else if ((leaf = props.getProperty("Application")) != null) {
                //Node=%server.node%,Application=%app%
                cmd = new ApplicationCommand(node, leaf);
            }
            else if ((leaf = props.getProperty("AppServer")) != null) {
                //Node=%server.node%,AppServer=%server.name%
                cmd = new AppServerCommand(node, leaf);
            }
            else if ((leaf = props.getProperty("DataSource")) != null) {
                //DataSource=%ds%
                cmd = new DataSourceCommand(leaf);
            }
            else if (node != null) {
                //Node=%server.node%
                cmd = new NodeCommand(node);
            }
            else {
                throw new IllegalArgumentException();
            }

            cache.put(props, cmd);
        }

        return cmd;
    }
}
