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

package org.hyperic.hq.plugin.system;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.product.PluginException;

public class WindowsDetector
    extends SystemServerDetector {

    protected String getServerType() {
        return SystemPlugin.WINDOWS_SERVER_NAME;
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException {

        if (isWin32()) {
            return super.getServerResources(platformConfig);
        }

        //should not get invoked if !win32, but just incase.
        return null;
    }

    private ServiceConfig setServiceInventoryProperties(String serviceName,
                                                        AIServiceValue svc,
                                                        boolean exists) {

        ConfigResponse cprops = new ConfigResponse();     
        ServiceConfig config;
        Service service = null;
        try {
            service = new Service(serviceName);
            config = service.getConfig();
        } catch (Win32Exception e) {
            String msg =
                "Error getting config for service=" +
                serviceName + ": " + e.getMessage();
            if (exists) {
                log.error(msg);
            }
            else {
                log.debug(serviceName + " does not exist");
            }
            return null;
        } finally {
            if (service != null){
                service.close();
            }
        }

        if (!exists) {
            if (config.getStartType() == ServiceConfig.START_DISABLED) {
                log.debug("Skipping " + serviceName +
                          ", start type=" + config.getStartTypeString());
                return null;
            }
        }

        String desc = config.getDescription();
        if (desc == null) {
            desc = config.getDisplayName();
        }
        svc.setDescription(desc);
        cprops.setValue("path", config.getPath());
        cprops.setValue("startupType", config.getStartTypeString());
        cprops.setValue("displayName", config.getDisplayName());
        try {
            svc.setCustomProperties(cprops.encode());
        } catch (EncodingException e) {
            log.error("Error encoding cprops: " + e.getMessage());
            return null;
        }

        return config;
    }

    private AIServiceValue findWindowsService(String type, String serviceName) {
        AIServiceValue svc = createSystemService(type, serviceName);
        ServiceConfig config =
            setServiceInventoryProperties(serviceName, svc, false);
        if (config == null) {
            return null;
        }
        svc.setName(config.getDisplayName());
        log.debug("Found service " + svc.getName() +
                  " - " + svc.getDescription());
        ConfigResponse productConfig = new ConfigResponse();
        productConfig.setValue(SystemPlugin.PROP_SVC, serviceName);
        try {
            svc.setProductConfig(productConfig.encode());
            svc.setMeasurementConfig(new ConfigResponse().encode());
            //XXX control config
        } catch (EncodingException e) {
            log.error("Error encoding config: " + e.getMessage());
            return null;
        }
    
        return svc;
    }

    private void discoverWindowsServices(String names, ArrayList services)
        throws Win32Exception {

        List serviceNames;
        if ("true".equals(names)) {
            serviceNames = Service.getServiceNames();
        }
        else {
            serviceNames = StringUtil.explode(names, ",");
        }

        for (int i=0; i<serviceNames.size(); i++) {
            String name = (String)serviceNames.get(i);
            AIServiceValue svc = 
                findWindowsService(SystemPlugin.SVC_NAME, name);
            if (svc != null) {
                services.add(svc);
            }
        }
    }

    protected ArrayList getSystemServiceValues(Sigar sigar,
                                               ConfigResponse serverConfig)
        throws SigarException {

        String type = SystemPlugin.SVC_NAME;
        List serviceConfigs = getServiceConfigs(type);
    
        ArrayList services = new ArrayList();

        //set cprops for manually create resources
        for (int i=0; i<serviceConfigs.size(); i++) {
            ConfigResponse serviceConfig = 
                (ConfigResponse)serviceConfigs.get(i);

            String name =
                serviceConfig.getValue(SystemPlugin.PROP_RESOURCE_NAME);
            String serviceName =
                serviceConfig.getValue(SystemPlugin.PROP_SVC);
            AIServiceValue svc =
                createSystemService(type, name);

            setServiceInventoryProperties(serviceName, svc, true);

            services.add(svc);
        }

        //auto-discovery of services (plugin defined types)
        Map plugins = getServiceInventoryPlugins();
        for (Iterator it = plugins.keySet().iterator(); it.hasNext();) {
            type = (String)it.next();
            String name = getTypeProperty(type, SystemPlugin.PROP_SVC);
            log.debug("Looking for " + type + " service=" + name);

            AIServiceValue svc = findWindowsService(type, name);
            if (svc != null) {
                services.add(svc);
            }
        }

        //auto-discover of any generic windows service
        String windowsServices =
            getManager().getProperty("windows.services.discover");
        if (windowsServices != null) {
            discoverWindowsServices(windowsServices, services);
        }

        return services;
    }
}
