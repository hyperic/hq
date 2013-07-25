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
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.win32.*;
import org.hyperic.util.config.ConfigResponse;

public class MsSQLDetector extends ServerDetector implements AutoServerDetector {

    static final String PROP_DB = "db.name";
    private static final String DB_NAME = "Database";
    static final String DEFAULT_SQLSERVER_SERVICE_NAME = "MSSQLSERVER";
    static final String DEFAULT_SQLAGENT_SERVICE_NAME = "SQLSERVERAGENT";
    private static final Log log = LogFactory.getLog(MsSQLDetector.class);

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List cfgs;
        try {
            cfgs = Service.getServiceConfigs("sqlservr.exe");
        } catch (Win32Exception e) {
            log.debug("[getServerResources] Error: " + e.getMessage(), e);
            return null;
        }

        log.debug("[getServerResources] MSSQL Server found:'" + cfgs.size() + "'");

        if (cfgs.size() == 0) {
            return null;
        }

        List servers = new ArrayList();
        for (int i = 0; i < cfgs.size(); i++) {
            ServiceConfig serviceConfig = (ServiceConfig) cfgs.get(i);
            String name = serviceConfig.getName();
            String instance = instaceName(name);
            File dir = new File(serviceConfig.getExe()).getParentFile();

            boolean correctVersion = false;
            String regKey = getTypeProperty("regKey");

            if (regKey != null) {
                try {
                    regKey = regKey.replace("%NAME%", instance);
                    log.debug("[getServerResources] regKey:'" + regKey + "'");
                    RegistryKey key = RegistryKey.LocalMachine.openSubKey(regKey);
                    String version = key.getStringValue("CurrentVersion");
                    String expectedVersion = getTypeProperty("version");
                    correctVersion = Pattern.compile(expectedVersion).matcher(version).find();
                    log.debug("[getServerResources] server:'" + instance + "' version:'" + version + "' expectedVersion:'" + expectedVersion + "' correctVersion:'" + correctVersion + "'");
                } catch (Win32Exception ex) {
                    log.debug("[getServerResources] Error accesing to windows registry to get '" + instance + "' version. " + ex.getMessage());
                }
            } else {
                correctVersion = checkVersionOldStyle(dir);
            }

            if (correctVersion) {
                dir = dir.getParentFile(); //strip "Binn"
                ServerResource server = createServerResource(dir.getAbsolutePath(), name);
                servers.add(server);
            }
        }

        return servers;
    }

    private boolean checkVersionOldStyle(File dir) {
        String versionFile = getTypeProperty("mssql.version.file");
        File dll = new File(dir, versionFile);
        boolean correctVersion = dll.exists();
        getLog().debug("[checkVersionOldStyle] dll:'" + dll + "' correctVersion='" + correctVersion + "'");
        return correctVersion;
    }

    private ServerResource createServerResource(String installpath, String name) {
        ServerResource server = createServerResource(installpath);

        String instance = instaceName(name);
        server.setName(server.getName() + " " + instance);

        ConfigResponse cfg = new ConfigResponse();
        cfg.setValue(Win32ControlPlugin.PROP_SERVICENAME, name);
        server.setProductConfig(cfg);
        server.setMeasurementConfig();
        server.setControlConfig();

        return server;
    }

    private static String instaceName(String name) {
        String instance = name;
        if (name.startsWith("MSSQL$")) {
            instance = name.substring(6);
        }
        return instance;
    }

    private static int getServiceStatus(String name) {
        Service svc = null;
        try {
            svc = new Service(name);
            log.debug("[getServiceStatus] name='"+name+"' status='"+svc.getStatusString()+"'");
            return svc.getStatus();
        } catch (Win32Exception e) {
            log.debug("[getServiceStatus] name='"+name+"' "+e);
            return Service.SERVICE_STOPPED;
        } finally {
            if (svc != null) {
                svc.close();
            }
        }
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig)
            throws PluginException {

        ArrayList services = new ArrayList();
        
        String sqlServerServiceName =
                serverConfig.getValue(Win32ControlPlugin.PROP_SERVICENAME,
                DEFAULT_SQLSERVER_SERVICE_NAME);

        String sqlServerMetricPrefix = "SQLServer"; //  metric prefix in case of default instance 
        String sqlAgentServiceName = MsSQLDetector.DEFAULT_SQLAGENT_SERVICE_NAME; //agent name in case of default instance

        if (!sqlServerServiceName.equals(DEFAULT_SQLSERVER_SERVICE_NAME)) {
            sqlServerMetricPrefix = sqlServerServiceName;
            sqlAgentServiceName = sqlServerServiceName.replaceFirst("MSSQL", "SQLAgent");
        }
        
        // creating agent service
        if (getServiceStatus(sqlAgentServiceName) != Service.SERVICE_STOPPED) {
            ServiceResource agentService = new ServiceResource();
            agentService.setType(this, "SQLAgent");
            agentService.setServiceName(sqlAgentServiceName);

            ConfigResponse cfg = new ConfigResponse();
            cfg.setValue(Win32ControlPlugin.PROP_SERVICENAME, sqlAgentServiceName);

            agentService.setProductConfig(cfg);
            agentService.setMeasurementConfig();
            agentService.setControlConfig();
            services.add(agentService);
        }

        String instaceName = sqlServerServiceName.substring(sqlServerServiceName.indexOf("$") + 1);

        // ReportServer
        String reportServiceName = "ReportServer$" + instaceName;
        if (getServiceStatus(reportServiceName) == Service.SERVICE_RUNNING) {
            ServiceResource agentService = new ServiceResource();
            agentService.setType(this, "Report Server");
            agentService.setServiceName("Report Server");

            ConfigResponse cfg = new ConfigResponse();
            cfg.setValue(Win32ControlPlugin.PROP_SERVICENAME, reportServiceName);

            agentService.setProductConfig(cfg);
            agentService.setMeasurementConfig();
            agentService.setControlConfig();
            services.add(agentService);
        }

        // MSOLAP
        String olapServiceName = "MSOLAP$" + instaceName;
        if (getServiceStatus(olapServiceName) == Service.SERVICE_RUNNING) {
            ServiceResource agentService = new ServiceResource();
            agentService.setType(this, "Analysis Services");
            agentService.setServiceName("Analysis Services");

            ConfigResponse cfg = new ConfigResponse();
            cfg.setValue(Win32ControlPlugin.PROP_SERVICENAME, olapServiceName);

            agentService.setProductConfig(cfg);
            agentService.setMeasurementConfig();
            agentService.setControlConfig();
            services.add(agentService);
        }
        
        // creating Database services
        try {
            String[] instances = Pdh.getInstances(sqlServerMetricPrefix + ":Databases");
            for (int i = 0; i < instances.length; i++) {
                String name = instances[i];
                if (name.equals("_Total")) {
                    continue;
                }

                ServiceResource service = new ServiceResource();
                service.setType(this, DB_NAME);
                service.setServiceName(name);

                ConfigResponse cfg = new ConfigResponse();
                cfg.setValue(MsSQLDetector.PROP_DB, name);
                service.setProductConfig(cfg);

                service.setMeasurementConfig();
                //service.setControlConfig(...); XXX?

                services.add(service);

            }
        } catch (Win32Exception e) {
            throw new PluginException("Error getting pdh data for '" + sqlServerServiceName + "': " + e.getMessage(), e);
        }

        return services;
    }
}
