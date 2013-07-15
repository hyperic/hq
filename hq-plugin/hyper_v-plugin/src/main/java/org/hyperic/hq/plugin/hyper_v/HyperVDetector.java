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

package org.hyperic.hq.plugin.hyper_v;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.DetectionUtil;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

public class HyperVDetector
    extends  ServerDetector implements AutoServerDetector   {

    private static Log log =
        LogFactory.getLog(HyperVDetector.class);

    
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers= discoverServersWMI("Msvm_ComputerSystem","Caption-Virtual Machine","ElementName","Hyper-V VM","Hyper-V VM - ");

        return servers;
    }
    
    protected List<ServiceResource> discoverServices(String type, String namePrefix, String token, String instanceName) {
        List<ServiceResource> services = new ArrayList<ServiceResource>();
        try {
            String[] instances = Pdh.getInstances(type);            

            
            Set<String> names = new HashSet<String>();
            for (int i = 0; i < instances.length; i++) {
                String instance = instances[i];
                if ("_Total".equals(instance) || "<All instances>".equals(instance)) {
                    continue;
                }
                String instancePrefix = instanceName + token;
                if (!instance.startsWith(instancePrefix)) {
                    continue;
                }
                log.debug("find instance=" + instance + " of:" + instanceName);                
                names.add(instance);                
            }
            
            for (Iterator<String> it = names.iterator(); it.hasNext();) {
                String name = it.next();
                ConfigResponse conf = new ConfigResponse();
                conf.setValue("service.instance.name", name);
                conf.setValue("instance.name", instanceName);

                ServiceResource service = new ServiceResource();
                service.setType(this, type);
                service.setServiceName(namePrefix + name);

                ConfigResponse cprops = new ConfigResponse();
                service.setProductConfig(conf);
                service.setMeasurementConfig();
                service.setCustomProperties(cprops);
            	
                services.add(service);                
            }
    	    if (services.isEmpty()) {
    	        return null;
    	    }
            return services;
        } catch (Win32Exception e) {
            log.debug("Error getting pdh data for " + type + ": " + e, e);
            return null;
        }
    }

    protected List<ServerResource> discoverServersWMI(String wmiObjName, String filter, String col, String type, String namePrefix) throws PluginException {
        Set<String> wmiObjs = DetectionUtil.getWMIObj(wmiObjName, filter, col, "");
        List<ServerResource> servers = new ArrayList<ServerResource>();
        for(String name:wmiObjs) {
            ServerResource server = new ServerResource();
            
            ConfigResponse conf = new ConfigResponse();
            conf.setValue("instance.name", name);
            server.setProductConfig(conf);
                        
            ConfigResponse cprops = new ConfigResponse();
            server.setProductConfig(conf);
            server.setMeasurementConfig();
            server.setCustomProperties(cprops);           
            server.setName(getPlatformName() + " " +  " " +namePrefix + name);
            server.setDescription("");
            server.setInstallPath(name); //XXX
            server.setIdentifier(name);
            servers.add(server);
            server.setType(type);
            servers.add(server);
        }
        return servers;
    }

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        String instanceName = config.getValue("instance.name");
        log.debug("instance.name=" + instanceName);
        if (instanceName == null || instanceName.length() == 0) {
            log.error("discover services: - instance.name not found!" );
        }        
 
        List<ServiceResource>  services =  new LinkedList<ServiceResource>();
        
        List<ServiceResource> virtualProcessorServices  =  discoverServices("Hyper-V Hypervisor Virtual Processor", "Hyper-V Hypervisor Virtual Processor - ", ":", instanceName);  
        if (virtualProcessorServices != null) {
            services.addAll(virtualProcessorServices);
        }
        
        List<ServiceResource> virtualNetworkServices  =  discoverServices("Hyper-V Virtual Network Adapter", "Hyper-V Virtual Network Adapter - ", "_", instanceName);  
        if (virtualNetworkServices != null) {
            services.addAll(virtualNetworkServices);
        }
        
        List<ServiceResource> legacyNetworkServices  =  discoverServices("Hyper-V Legacy Network Adapter", "Hyper-V Legacy Network Adapter - ", "_", instanceName);  
        if (legacyNetworkServices != null) {
            services.addAll(legacyNetworkServices);
        }
        
        List<ServiceResource> virtualIdeControllers  =  discoverServices("Hyper-V Virtual IDE Controller (Emulated)", "Hyper-V Virtual IDE Controller - ", ":", instanceName);  
        if (virtualIdeControllers != null) {
            services.addAll(virtualIdeControllers);
        }



        if (services.isEmpty()) {
            return null;
        }
        return services;
    }


}
