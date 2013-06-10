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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * service type, performance counter group, use service type on service
     * name, name prefix (back compatibility)
     */
    private static final String[][] aspAppServices = {
        {"Application", ".NET CLR Loading", "false", ""},
        {"ASP.NET App", "ASP.NET Applications", "true", ""},
        {"ASP.NET App", "ASP.NET Apps v4.0.30319", "true", "V4 "},
        {"ASP.NET App", "ASP.NET Apps v2.0.50727", "true", "V2 "}
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
    
    protected final static String ORACLE_PROVIDER_STR = "Oracle Data Provider for .NET";
    protected final static String SQL_SERVER_PROVIDER_STR = ".NET Data Provider for SqlServer";
    protected final static String SQL_SERVER_PROVIDER_TYPE_STR = "Data Provider for SqlServer";
    protected final static String ORACLE_SERVER_PROVIDER_TYPE_STR = "Oracle Data Provider for .Net";
    

    private String getVersion() {
        RegistryKey key = null;
        List versions = new ArrayList();

        Pattern regex = Pattern.compile("v(\\d*\\.\\d*).*");
        try {
            key = getRegistryKey("SOFTWARE\\Microsoft\\NET Framework Setup\\NDP");
            String[] names = key.getSubKeyNames();

            for (int i = 0; i < names.length; i++) {
                log.debug("[getVersion] names[" + i + "]->" + names[i]);
                Matcher m = regex.matcher(names[i] + ".0"); // for v4
                if (m.find()) {
                    versions.add(m.group(1));
                }
            }
        } catch (Win32Exception e) {
            log.debug(e,e);
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

        String expectedVersion = getTypeInfo().getVersion();
        String version = getVersion();
        if (version == null) {
            return null;
        }

        String path = getInstallPath();
        boolean valid = ((version.startsWith(expectedVersion)) && (path != null));

        log.debug("Found .NET version=" + version + " (expectedVersion=" + expectedVersion + "), path=" + path + ", valid=" + valid);

        if (!valid) {
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
    
    private Collection<ServiceResource>  addDataProvidersServices(String dataProviderStr, String dataProviderTypeStr) {
        try {            
            String[] instances = Pdh.getInstances(dataProviderStr);
            Pattern regex = Pattern.compile("([^\\[]*).*");                                        
            Set<String> names = new HashSet<String>();
            for (int i = 0; i < instances.length; i++) {
                String instance = instances[i];
                Matcher m = regex.matcher(instance);
                if (m.find()) {                    
                    String n = m.group(1).trim();
                    log.debug("[discoverServices] instance = " + instance + " (" + n + ")");                    
                    if (n.length() > 0) {
                        log.debug("[discoverServices] instance = " + instance + " (" + n + ") valid.");
                        names.add(n);
                    }
                } else {
                    log.debug("[discoverServices] instance = " + instance + " skiped.");
                }
            }
            List<ServiceResource> services = new ArrayList<ServiceResource>();
            for (Iterator<String> it = names.iterator(); it.hasNext();) {
                String name = it.next();
                ServiceResource service = new ServiceResource();
                service.setType(this, dataProviderTypeStr);
                service.setServiceName(dataProviderTypeStr + " " + name);

                ConfigResponse pc = new ConfigResponse();
                pc.setValue(PROP_APP, name);
                service.setProductConfig(pc);
                service.setMeasurementConfig();
                services.add(service);
            }
            return services;
        } catch (Win32Exception e) {
            log.debug("Error getting pdh data for '.NET Data Provider for:" + dataProviderStr + ":"  + e, e);
            return Collections.emptyList();
        }
        
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
        List<ServiceResource> services = new ArrayList<ServiceResource>();

        //Data providers.
        services.addAll(addDataProvidersServices(SQL_SERVER_PROVIDER_STR, SQL_SERVER_PROVIDER_TYPE_STR));
        services.addAll(addDataProvidersServices(ORACLE_PROVIDER_STR, ORACLE_SERVER_PROVIDER_TYPE_STR));

        // apps
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
                            service.setServiceName(serviceType + " " + aspAppServices[i][3] + name);
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
