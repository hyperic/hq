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
import java.util.Properties;
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
    static final String MS_CLUSTER_DISCOVERY = "MS_CLUSTER_DISCOVERY";
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
            Service mssqlService = null;
            boolean serverIsRunning = false;
            try{
                mssqlService = new Service(name);
                if(mssqlService.getStatus() != Service.SERVICE_RUNNING) {
                    log.debug("[getServerResources] service '" + name + "' is not RUNNING (status='"
                            + mssqlService.getStatusString() + "')");
                } else {
                    serverIsRunning = true;
                }
            }catch(Win32Exception e) {
                log.debug("[getServerResources] Error getting '" + name + "' service information " + e, e);
            }finally {
                if(mssqlService != null) {
                    mssqlService.close();
                }
            }
            if (serverIsRunning){
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

        String discoverMsCluster = getTypeProperty(MS_CLUSTER_DISCOVERY);
        if (discoverMsCluster!=null){
            Properties mssqlClusterPropes = ClusterDetect.getMssqlClusterProps(instance);
            if(mssqlClusterPropes != null) {
                cfg.setValue("mssql-cluster-name",mssqlClusterPropes.getProperty(ClusterDetect.CLUSTER_NAME_PROP));
                cfg.setValue("virtual-platform-name",mssqlClusterPropes.getProperty(ClusterDetect.NETWORK_NAME_PROP));
                cfg.setValue("cluster-nodes",mssqlClusterPropes.getProperty(ClusterDetect.NODES_PROP));
                cfg.setValue("instance-name", instance);
                cfg.setValue("original-platform-name", getPlatformName());
            }
        }
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
            log.debug("[getServiceStatus] name='" + name + "' status='" + svc.getStatusString() + "'");
            return svc.getStatus();
        } catch (Win32Exception e) {
            log.debug("[getServiceStatus] name='" + name + "' " + e);
            return Service.SERVICE_STOPPED;
        } finally {
            if (svc != null) {
                svc.close();
            }
        }
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {

        ArrayList services = new ArrayList();

        String sqlServerServiceName =
                serverConfig.getValue(Win32ControlPlugin.PROP_SERVICENAME,
                        DEFAULT_SQLSERVER_SERVICE_NAME);

        List<ServiceInfo> servicesNames = new ArrayList<ServiceInfo>();
        String sqlServerMetricPrefix = "SQLServer"; //  metric prefix in case of default instance 

        String msrsPrefix = "MSRS 2011 Windows Service";
        String instaceName = DEFAULT_SQLSERVER_SERVICE_NAME;

        if (getTypeInfo().getVersion().equals("2008")) {
            msrsPrefix = "MSRS 2008 Windows Service";
        } else if (getTypeInfo().getVersion().equals("2008 R2")) {
            msrsPrefix = "MSRS 2008 R2 Windows Service";
        } else if (getTypeInfo().getVersion().equals("2005")) {
            msrsPrefix = "MSRS 2005 Windows Service";
        }

        if (sqlServerServiceName.equals(DEFAULT_SQLSERVER_SERVICE_NAME)) { // single instance
            String rpPrefix = "ReportServer";
            String olapPrefix = "MSAS11";
            if (getTypeInfo().getVersion().startsWith("2008")) {
                olapPrefix = "MSAS 2008";
            } else if (getTypeInfo().getVersion().equals("2005")) {
                olapPrefix = "MSAS 2005";
            }
            servicesNames.add(new ServiceInfo("SQLSERVERAGENT", "SQLAgent", "SQLAgent", "SQLAgent"));
            servicesNames.add(new ServiceInfo("ReportServer", "Report Server", rpPrefix, "Report Server"));
            servicesNames.add(new ServiceInfo("MSSQLServerOLAPService", "Analysis Services", olapPrefix, "Analysis Services"));
        } else {    // multiple instances
            instaceName = sqlServerServiceName.substring(sqlServerServiceName.indexOf("$") + 1);
            sqlServerMetricPrefix = sqlServerServiceName;
            servicesNames.add(new ServiceInfo("SQLAgent$" + instaceName, "SQLAgent", "SQLAgent$" + instaceName, "SQLAgent"));
            servicesNames.add(new ServiceInfo("ReportServer$" + instaceName, "Report Server", "ReportServer$" + instaceName, "Report Server"));
            servicesNames.add(new ServiceInfo("MSOLAP$" + instaceName, "Analysis Services", "MSOLAP$" + instaceName, "Analysis Services"));
        }

        for (int i = 0; i < servicesNames.size(); i++) {
            ServiceInfo s = servicesNames.get(i);
            if (getServiceStatus(s.winServiceName) == Service.SERVICE_RUNNING) {
                log.debug("[discoverServices] service='" + s.winServiceName + "' runnig");
                ServiceResource agentService = new ServiceResource();
                agentService.setType(this, s.type);
                agentService.setServiceName(s.serviceName);

                ConfigResponse cfg = new ConfigResponse();
                cfg.setValue(Win32ControlPlugin.PROP_SERVICENAME, s.winServiceName);
                cfg.setValue("pref_prefix", s.metricsPrefix);
                if (s.type.equals("Report Server")) {
                    cfg.setValue("MSRS", msrsPrefix);
                    cfg.setValue("instance", instaceName);
                }

                agentService.setProductConfig(cfg);
                agentService.setMeasurementConfig();
                agentService.setControlConfig();
                services.add(agentService);
            } else {
                log.debug("[discoverServices] service='" + s.winServiceName + "' NOT runnig");
            }
        }

        // creating Database services
        try {
            String obj = sqlServerMetricPrefix + ":Databases";
            log.debug("[discoverServices] obj='" + obj + "'");
            String[] instances = Pdh.getInstances(obj);
            log.debug("[discoverServices] instances=" + Arrays.asList(instances));
            for (int i = 0; i < instances.length; i++) {
                String dbName = instances[i];
                if (!dbName.equals("_Total")) {

                    ServiceResource service = new ServiceResource();
                    service.setType(this, DB_NAME);
                    service.setServiceName(dbName);

                    ConfigResponse cfg = new ConfigResponse();
                    cfg.setValue(MsSQLDetector.PROP_DB, dbName);
                    cfg.setValue("instance", instaceName);
                    service.setProductConfig(cfg);

                    service.setMeasurementConfig();
                    //service.setControlConfig(...); XXX?

                    services.add(service);
                }
            }
        } catch (Win32Exception e) {
            log.debug("[discoverServices] Error getting Databases pdh data for '" + sqlServerServiceName + "': " + e.getMessage(), e);
        }

        return services;
    }
    
    private class ServiceInfo {
        String winServiceName;
        String type;
        String metricsPrefix;
        String serviceName;

        public ServiceInfo(String winServiceName, String type, String metricsPrefix, String serviceName) {
            this.winServiceName = winServiceName;
            this.type = type;
            this.metricsPrefix = metricsPrefix;
            this.serviceName = serviceName;
        }
    }
}
