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
import java.util.Arrays;
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

    /**
     * service type, performance counter group, use service type on service name
     * (back compatibility)
     */
    private static final String[][] aspAppServices = {
        {"Application", ".NET CLR Loading", "false"},
        {"ASP.NET App", "ASP.NET Applications", "true"},
        {"ASP.NET App", "ASP.NET Apps v4.0.30319", "true"},
        {"ASP.NET App", "ASP.NET Apps v2.0.50727", "true"}
    };
    static final String PROP_APP = "app.name";
    static final String PROP_PATH = "app.path";
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
            key = getRegistryKey("SOFTWARE\\Microsoft\\NET Framework Setup\\NDP");
            String[] names = key.getSubKeyNames();

            for (int i = 0; i < names.length; i++) {
                log.debug("[getVersion] names["+i+"]->"+names[i]);
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
        return (String) versions.get(size - 1);
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
        if (!version.startsWith(thisVersion)) {
            return null;
        }

        String path = getInstallPath();
        if (path == null) {
            log.debug("Found .NET version=" + version
                    + ", path=" + path);
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

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
        List services = new ArrayList();
        String instancesToSkipStr = getProperties().getProperty("dotnet.instances.to.skip", "_Global_");
        List<String> instancesToSkip = Arrays.asList(instancesToSkipStr.toUpperCase().split(","));
        log.debug("dotnet.instances.to.skip = " + instancesToSkip);
        for (int i = 0; i < aspAppServices.length; i++) {
            String serviceType = aspAppServices[i][0];
            String counterName = aspAppServices[i][1];
            boolean useServiceType = aspAppServices[i][2].equals("true");

            try {
                String[] apps = Pdh.getInstances(counterName);

                for (int idx = 0; idx < apps.length; idx++) {
                    String name = apps[idx];
                    if (!instancesToSkip.contains(name.toUpperCase())) {
                        log.debug("instace '" + name + "' (" + counterName + ") valid.");
                        ServiceResource service = new ServiceResource();
                        service.setType(this, serviceType);
                        if (useServiceType) {
                            service.setServiceName(serviceType + " " + name);
                        } else {
                            service.setServiceName(name);
                        }

                        ConfigResponse pc = new ConfigResponse();
                        pc.setValue(PROP_APP, name);
                        pc.setValue(PROP_PATH, counterName);
                        service.setProductConfig(pc);

                        service.setMeasurementConfig();

                        services.add(service);
                    } else {
                        log.debug("instace '" + name + "' (" + counterName + ") skiped.");
                    }
                }
            } catch (Win32Exception e) {
                log.debug("Error getting pdh data for '" + serviceType + "': " + e, e);
            }
        }
        return services;
    }
}
