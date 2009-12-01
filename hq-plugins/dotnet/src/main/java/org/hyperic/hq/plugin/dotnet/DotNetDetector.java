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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class DotNetDetector
    extends ServerDetector
    implements AutoServerDetector {

    static final String PROP_APP = "app.name";

    private static final String APP_NAME = "Application";

    private static final String REG_KEY =
        "SOFTWARE\\Microsoft\\.NETFramework";

    private static Log log =
        LogFactory.getLog(DotNetDetector.class.getName());

    private RegistryKey getRegistryKey(String path)
        throws Win32Exception {

        return RegistryKey.LocalMachine.openSubKey(path);
    }

    private String getVersion() {
        RegistryKey key = null;
        List versions = new ArrayList();

        try {
            key = getRegistryKey(REG_KEY + "\\policy");
            String[] names = key.getSubKeyNames();

            for (int i=0; i<names.length; i++) {
                if (names[i].charAt(0) == 'v') {
                    String version = names[i].substring(1);
                    versions.add(version);
                }
            }
        } catch (Win32Exception e) {
            return null;            
        } finally {
            if (key != null) {
                key.close();
            }
        }

        int size = versions.size();
        if (size == 0) {
            return null;
        }

        Collections.sort(versions);

        log.debug("Found .NET versions=" + versions);

        //all runtime versions have the same metrics,
        //so just discover the highest version
        return (String)versions.get(size-1);
    }

    private String getInstallPath() {
        RegistryKey key = null;
        try {
            key = getRegistryKey(REG_KEY);
            return key.getStringValue("InstallRoot").trim();
        } catch (Win32Exception e) {
            return null;
        } finally {
            if (key != null) {
                key.close();
            }
        }
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        String thisVersion = getTypeInfo().getVersion();
        String version = getVersion();
        if (version == null) {
            return null;
        }
        if (!thisVersion.equals(version)) {
            return null;
        }

        String path = getInstallPath();
        if (path == null) {
            log.debug("Found .NET version=" + version +
                      ", path=" + path);
            return null;
        }

        ServerResource server = createServerResource(path);
            
        server.setProductConfig();
        //server.setControlConfig(...); N/A
        server.setMeasurementConfig();

        List servers = new ArrayList();
        servers.add(server);
        return servers;
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
