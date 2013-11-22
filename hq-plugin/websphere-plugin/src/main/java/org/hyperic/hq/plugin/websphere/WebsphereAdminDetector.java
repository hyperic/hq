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
package org.hyperic.hq.plugin.websphere;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.hyperic.hq.plugin.websphere.jmx.WebsphereRuntimeDiscoverer;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class WebsphereAdminDetector extends WebsphereDetector {

    @Override
    protected List discoverServers(ConfigResponse config)
            throws PluginException {
        if (!WebsphereProductPlugin.VALID_JVM) {
            return new ArrayList();
        }

        if (this.discoverer == null) {
            String version = getTypeInfo().getVersion();
            this.discoverer = new WebsphereRuntimeDiscoverer(version, this);
        }

        //for use w/ -jar hq-product.jar or agent.properties
        Properties props = getManager().getProperties();
        String[] credProps = {
            WebsphereProductPlugin.PROP_USERNAME,
            WebsphereProductPlugin.PROP_PASSWORD,
            WebsphereProductPlugin.PROP_SERVER_NODE
        };
        for (int i = 0; i < credProps.length; i++) {
            String name = credProps[i];
            String value =
                    props.getProperty(name, config.getValue(name));
            if (value == null) {
                //prevent NPE since user/pass is not required
                value = "";
            }
            config.setValue(name, value);
        }
        return this.discoverer.discoverServers(config);
    }

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List servers = new ArrayList();
        List processes = getServerProcessList(getProcessQuery());

        for (int i = 0; i < processes.size(); i++) {
            WebSphereProcess p = (WebSphereProcess) processes.get(i);
            if (p.getServer().equals("nodeagent")) {
                List found = getServerList(new File(p.getServerRoot()), null, p);
                if (found != null) {
                    servers.addAll(found);
                }
            }
        }
        return servers;
    }
}
