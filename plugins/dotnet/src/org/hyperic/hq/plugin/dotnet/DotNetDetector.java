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

package org.hyperic.hq.plugin.dotnet;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class DotNetDetector
    extends ServerDetector
    implements RegistryServerDetector {

    static final String SERVER_NAME = ".NET";
    
    static final String APP_NAME    = "Application";

    static final String PROP_APP    = "app.name";
    
    static Log log = LogFactory.getLog("DotNetDetector");

    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) 
        throws PluginException {

        if (!(path.indexOf(SERVER_NAME) > 0)) {
            return null;
        }

        log.debug("checking path=" + path);

        try {
            boolean found = false;
            String keyName = current.getSubKeyName() + "\\policy";
            RegistryKey key =
               RegistryKey.LocalMachine.openSubKey(keyName);

            String[] names = key.getSubKeyNames();
            String version = null;
            String thisVersion = getTypeInfo().getVersion();
            for (int i=0; i<names.length; i++) {
                if (names[i].charAt(0) == 'v') {
                    version = names[i].substring(1);
                    log.debug("found version=" + version);
                    if (version.equals(thisVersion)) {
                        log.debug("reporting version=" + version);
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                return null;
            }

            ServerResource server = createServerResource(path);
            
            server.setProductConfig();
            //server.setControlConfig(...); N/A
            server.setMeasurementConfig();

            ArrayList servers = new ArrayList();
            servers.add(server);
            return servers;
        } catch (Win32Exception e) {
            return null;
        }
    }

    protected List discoverServices(ConfigResponse serverConfig) 
        throws PluginException {
        
        List services = new ArrayList();
        String[] apps;

        try {
            apps = Pdh.getInstances(".NET CLR Loading");
        } catch (Win32Exception e) {
            throw new PluginException("Error getting pdh data: " + e, e);
        } 

        for (int i=0; i<apps.length; i++) {
            String name = apps[i];
            if (name.equals("_Global_")) {
                continue;
            }

            ServiceResource service = new ServiceResource();
            service.setType(this, APP_NAME);
            service.setServiceName(name);

            ConfigResponse config = new ConfigResponse();
            config.setValue(PROP_APP, name);
            service.setProductConfig(config);

            service.setMeasurementConfig();
            //service.setControlConfig(...); XXX?

            services.add(service);
        }

        return services;
    }
}
