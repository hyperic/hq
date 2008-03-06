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

package org.hyperic.hq.plugin.mssql;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.Win32ControlPlugin;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;

public class MsSQLDetector
    extends ServerDetector
    implements AutoServerDetector {

    static final String PROP_DB      = "db.name";
    private static final String DB_NAME      = "Database";
    private static final String DEFAULT_NAME = "MSSQL";
    static final String DEFAULT_SERVICE_NAME =
        DEFAULT_NAME + "SERVER";

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        String versionFile =
            getTypeProperty("mssql.version.file");

        List configs;
        try {
            configs =
                Service.getServiceConfigs("sqlservr.exe");
        } catch (Win32Exception e) {
            return null;
        }
        if (configs.size() == 0) {
            return null;
        }

        List servers = new ArrayList();
        for (int i=0; i<configs.size(); i++) {
            ServiceConfig serviceConfig =
                (ServiceConfig)configs.get(i);
            String name = serviceConfig.getName();
            File dir = new File(serviceConfig.getExe()).getParentFile();
            File dll = new File(dir, versionFile);
            if (!dll.exists()) {
                continue;
            }
            dir = dir.getParentFile(); //strip "Binn"

            ServerResource server =
                createServerResource(dir.getAbsolutePath(),
                                     name, false);
            servers.add(server);
        }

        return servers;
    }

    private ServerResource createServerResource(String installpath,
                                                String name,
                                                boolean isDefault) {
        ServerResource server =
            createServerResource(installpath);

        if (!isDefault) {
            String instance;
            if (name.startsWith("MSSQL$")) {
                instance = name.substring(6);
            }
            else {
                instance = name;
            }
            server.setName(server.getName() + " " + instance);
        }

        ConfigResponse config = new ConfigResponse();
        config.setValue(Win32ControlPlugin.PROP_SERVICENAME, name);
        server.setProductConfig(config);
        server.setMeasurementConfig();
        server.setControlConfig();

        return server;
    }

    protected List discoverServices(ConfigResponse serverConfig)
            throws PluginException {

        ArrayList services = new ArrayList();

        String serviceName =
            serverConfig.getValue(Win32ControlPlugin.PROP_SERVICENAME,
                                  DEFAULT_SERVICE_NAME);
        
        if (serviceName.equals(DEFAULT_SERVICE_NAME)) {
            // not sure why they drop the 'MS' from the service name
            // in the default case.
            serviceName = "SQLServer";
        }

        try {
            String[] instances =
                Pdh.getInstances(serviceName + ":Databases");

            for (int i=0; i<instances.length; i++) {
                String name = instances[i];
                if (name.equals("_Total")) {
                    continue;
                }
                
                ServiceResource service = new ServiceResource();
                service.setType(this, DB_NAME);
                service.setServiceName(name);

                ConfigResponse config = new ConfigResponse();
                config.setValue(MsSQLDetector.PROP_DB, name);
                service.setProductConfig(config);

                service.setMeasurementConfig();
                //service.setControlConfig(...); XXX?

                services.add(service);
            }
        } catch (Win32Exception e) {
            String msg =
                "Error getting pdh data for '" +
                serviceName + "': " + e.getMessage();
            throw new PluginException(msg, e);
        }

        return services;
    }
}
