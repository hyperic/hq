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
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPluginManager;

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

    private boolean setCustomProperty(String type, ConfigResponse cprops,
                                      String key, String val) {

        //XXX code below does not work for the builtin
        //service type because the cprops are defined by
        //the SystemPlugin/ProductPlugin rather than hq-plugin.xml
        if (type.equals(SystemPlugin.SVC_NAME)) {
            cprops.setValue(key, val);
            return true;
        }

        ProductPluginManager manager =
            (ProductPluginManager)getManager().getParent();
        MeasurementPlugin plugin =
            manager.getMeasurementPlugin(type);

        if (plugin == null) {
            return false;
        }

        if (plugin.getCustomPropertiesSchema().getOption(key) == null) {
            return false;
        }
        else {
            cprops.setValue(key, val);
            return true;
        }
    }

    private ServiceConfig setServiceInventoryProperties(String serviceName,
                                                        AIServiceValue svc,
                                                        boolean exists) {

        ConfigResponse cprops = new ConfigResponse();     
        ServiceConfig config;
        Service service = null;
        int status;
        String statusString;

        try {
            service = new Service(serviceName);
            config = service.getConfig();
            status = service.getStatus();
            statusString = service.getStatusString();
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
            if (config.getStartType() != ServiceConfig.START_AUTO) {
                log.debug("Skipping " + serviceName +
                          ", start type=" + config.getStartTypeString());
                return null;
            }
            if (status != Service.SERVICE_RUNNING) {
                log.debug("Skipping " + serviceName +
                          ", status=" + statusString);
                return null;
            }
        }

        String desc = config.getDescription();
        if (desc == null) {
            desc = config.getDisplayName();
        }
        svc.setDescription(desc);

        String type = svc.getServiceTypeName();
        setCustomProperty(type, cprops,
                          "path", config.getPath());
        setCustomProperty(type, cprops,
                          "startupType", config.getStartTypeString());
        setCustomProperty(type, cprops,
                          "displayName", config.getDisplayName());
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
        svc.setName(getFullServiceName(config.getDisplayName()));
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

    private void discoverPluginServices(List services) {
        //auto-discovery of services (plugin defined types)
        Map plugins = getServiceInventoryPlugins();
        if (plugins == null) {
            return;
        }

        for (Iterator it = plugins.keySet().iterator(); it.hasNext();) {
            String type = (String)it.next();
            String name = getTypeProperty(type, SystemPlugin.PROP_SVC);
            if (name == null) {
                log.warn("Service type '" + type +
                         "' has autoinventory plugin " +
                         "without '" + SystemPlugin.PROP_SVC +
                         "' property defined.");
                continue;
            }
            log.debug("Looking for " + type + " service=" + name);

            AIServiceValue svc = findWindowsService(type, name);
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

        discoverPluginServices(services);

        //auto-discover of any generic windows service
        String windowsServices =
            getManager().getProperty("windows.services.discover");
        if (windowsServices != null) {
            discoverWindowsServices(windowsServices, services);
        }

        return services;
    }
}
