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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.Win32ControlPlugin;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;

public class MsSQLDetector
    extends ServerDetector
    implements AutoServerDetector {

    static Log log = LogFactory.getLog("MsSQLDetector");
    
    static final String PROP_DB      = "db.name";
    static final String DB_NAME      = "Database";
    static final String DEFAULT_NAME = "MSSQL";
    static final String DEFAULT_SERVICE_NAME =
        DEFAULT_NAME + "SERVER";

    private static final String MSSQL_DEFAULT_NAME = "MSSQLServer"; 
    private static final String MSKEY = "SOFTWARE\\Microsoft\\";
    private static final String MSSQL_DEFAULT = 
        MSKEY + MSSQL_DEFAULT_NAME;

    private static final String MSSQL_INSTANCE =
        MSKEY + "Microsoft SQL Server";

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        String wantedVersion =
            getTypeProperty("mssql.version") + ".";
        ArrayList servers = new ArrayList();
        List found =
            getServerResources(platformConfig,
                               MSSQL_INSTANCE,
                               wantedVersion);
        if (found != null) {
            servers.addAll(found);
        }
        RegistryKey root;

        try {
            root = RegistryKey.LocalMachine.openSubKey(MSSQL_DEFAULT);
            ServerResource server =
                detectServer(root, MSSQL_DEFAULT_NAME, wantedVersion);
            if (server != null) {
                servers.add(server);
            }
            root.close();
        } catch (Win32Exception e) {
            log.debug(MSSQL_DEFAULT + " regkey does not exist");
        }

        found = findWindowsServices();
        if (found != null){
            servers.addAll(found);
        }

        return servers;
    }

    //XXX introduced for detection of 64-bit SQL server by 32-bit process,
    //but could do this by default.
    private List findWindowsServices() {
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

    private ServerResource detectServer(RegistryKey key,
                                        String name,
                                        String wantedVersion) {
        RegistryKey setup;
        File dir;

        try {
            setup = key.openSubKey("Setup");            
        } catch (Win32Exception e) {
            return null;
        }

        try {
            dir = new File(setup.getStringValue("SQLPath"));
        } catch (Win32Exception e) {
            setup.close();
            return null;
        }

        setup.close();

        log.debug("checking path=" + dir);

        boolean isDefault = false;
        
        //the default install
        if (name.equals(DEFAULT_NAME) ||
            name.equals(MSSQL_DEFAULT_NAME))
        {
            name = DEFAULT_SERVICE_NAME;
            dir = dir.getParentFile();
            isDefault = true;
        }
        else {
            //non-default install (multiple instances)
            name = DEFAULT_NAME + "$" + name;
        }

        RegistryKey versionKey;
        String productVersion;

        try {
            versionKey =
                key.openSubKey("MSSQLServer\\CurrentVersion");            
        } catch (Win32Exception e) {
            return null;
        }

        try {
            productVersion =
                versionKey.getStringValue("CurrentVersion").trim();
        } catch (Win32Exception e) {
            versionKey.close();
            return null;
        }

        versionKey.close();

        if (!productVersion.startsWith(wantedVersion)) {
            return null;
        }

        ServerResource server =
            createServerResource(dir.getAbsolutePath(),
                                 name, isDefault);

        ConfigResponse cprops = new ConfigResponse();
        cprops.setValue("version", productVersion);
        server.setCustomProperties(cprops);

        return server;
    }

    private List getServerResources(ConfigResponse platformConfig,
                                    String rootName,
                                    String wantedVersion)
        throws PluginException {


        RegistryKey root;

        try {
            root = RegistryKey.LocalMachine.openSubKey(rootName);
        } catch (Win32Exception e) {
            log.debug(rootName + " regkey does not exist");
            return null;
        }

        String[] keys = root.getSubKeyNames();
        ArrayList servers = new ArrayList();

        if (log.isDebugEnabled()) {
            log.debug("Scanning keys=" + rootName + "\\" +
                      Arrays.asList(keys));
        }
        
        for (int i=0; i<keys.length; i++) {
            String name = keys[i];
            RegistryKey key;

            try {
                key = root.openSubKey(name);
            } catch (Win32Exception e) {
                continue;
            }

            ServerResource server =
                detectServer(key, name, wantedVersion);
            if (server != null) {
                servers.add(server);
            }

            key.close();
        }

        return servers;
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
