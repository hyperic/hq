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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.DetectionUtil;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

import java.util.Collections;



public class HyperVDetector
    extends  ServerDetector implements AutoServerDetector   {

    private static Log log =
        LogFactory.getLog(HyperVDetector.class);

    
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers= discoverServersWMI("Msvm_ComputerSystem","Caption-Virtual Machine","Name","Hyper-V VM","Hyper-V VM - ");

        return servers;
    }
    
    private ServiceResource createService(String type, String name, String serviceInstanceName, String instanceName) {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue("service.instance.name", serviceInstanceName);
        conf.setValue("instance.name", instanceName);

        ServiceResource service = new ServiceResource();
        service.setType(this, type);
        service.setServiceName( name);

        ConfigResponse cprops = new ConfigResponse();
        service.setProductConfig(conf);
        service.setMeasurementConfig();
        service.setCustomProperties(cprops);
        return service;

    }

    
    protected List<ServiceResource> discoverServices(String propertySet, String type, String namePrefix, String token, String instanceName) {
        List<ServiceResource> services = new ArrayList<ServiceResource>();
        try {
            String[] instances = Pdh.getInstances(propertySet);            

            
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
                services.add(createService(type, namePrefix + name, name, instanceName));                
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
        Set<String> wmiObjs = DetectionUtil.getWMIObj("root\\virtualization",wmiObjName, Collections.singletonMap(filter,"="), col, "");
        List<ServerResource> servers = new ArrayList<ServerResource>();
        if (wmiObjs==null||wmiObjs.isEmpty()) {
            return null;
        }
        for(String guid:wmiObjs) {
            Set<String> vmNames = DetectionUtil.getWMIObj("root\\virtualization","Msvm_ComputerSystem", Collections.singletonMap("Name-"+guid,"="), "ElementName", "");
            if (vmNames==null||vmNames.isEmpty()) {
                continue;
            }
            ServerResource server = new ServerResource();
            
            ConfigResponse conf = new ConfigResponse();
            String name = vmNames.iterator().next();
            conf.setValue("instance.name", name);

            conf.setValue(Collector.GUID, guid);
            Set<String> macs = DetectionUtil.getWMIObj("root\\virtualization","Msvm_SyntheticEthernetPort", Collections.singletonMap("SystemName-"+guid, "="), "PermanentAddress", "");
            if (macs!=null&&!macs.isEmpty()) {
                String mac = macs.iterator().next();
                conf.setValue(Collector.MAC, mac);
            }
            server.setProductConfig(conf);
            server.setMeasurementConfig();
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
        
        List<ServiceResource> virtualProcessorServices  =  discoverServices("Hyper-V Hypervisor Virtual Processor", "Hyper-V Hypervisor Virtual Processor", "Hyper-V Hypervisor Virtual Processor - ", ":", instanceName);  
        if (virtualProcessorServices != null) {
            services.addAll(virtualProcessorServices);
        }
        
        List<ServiceResource> virtualNetworkServices  =  discoverServices("Hyper-V Virtual Network Adapter", "Hyper-V Virtual Network Adapter", "Hyper-V Virtual Network Adapter - ", "_", instanceName);  
        if (virtualNetworkServices != null) {
            services.addAll(virtualNetworkServices);
        }
        
        List<ServiceResource> legacyNetworkServices  =  discoverServices("Hyper-V Legacy Network Adapter","Hyper-V Legacy Network Adapter",  "Hyper-V Legacy Network Adapter - ", "_", instanceName);  
        if (legacyNetworkServices != null) {
            services.addAll(legacyNetworkServices);
        }
        
        List<ServiceResource> virtualIdeControllers  =  discoverServices("Hyper-V Virtual IDE Controller (Emulated)","Hyper-V Virtual IDE Controller", "Hyper-V Virtual IDE Controller - ", ":", instanceName);  
        if (virtualIdeControllers != null) {
            services.addAll(virtualIdeControllers);
        }

        // discover storage services:
        String vmGuid = config.getValue(Collector.GUID);
        if (vmGuid != null) {
            log.debug("instanceNAME=" + instanceName + " guid=" + vmGuid);
            List<ServiceResource> storageServices  =  discoverStorageServices(vmGuid, instanceName);  
            if (storageServices != null) {
                services.addAll(storageServices);
            }
        }
        else{
            log.error("null guid for:" + instanceName);
        }
        if (services.isEmpty()) {
            return null;
        }
        return services;
    }
    
    private List<ServiceResource> discoverStorageServices(String vmGuid, String instanceName)  {
        /*
         *
         *Get-WmiObject -Namespace root\virtualization  -Query "Select Connection From Msvm_ResourceAllocationSettingData where ElementName='Hard Disk Image' and InstanceID LIKE '%3D546820-BB6B-4333-A808-057503C0DE13%'"
         *
         */
        List<ServiceResource> services = new ArrayList<ServiceResource>();
        Map<String, String> filters =  new HashMap<String,String>(2);
        filters.put("ElementName-Hard Disk Image", "=");
        filters.put("InstanceID-" + "%"+ vmGuid+"%", "like");
        try {
            Set<String> wmiObjs = DetectionUtil.getWMIObj("root\\virtualization","Msvm_ResourceAllocationSettingData", filters, "Connection", "");
            log.debug("storage for instance=" + instanceName + " guid=" + vmGuid + " is:" + wmiObjs);
 
            if (wmiObjs == null) {
                log.debug("no storage were found for instance=" + instanceName + " guid=" + vmGuid);
                return null;
            }
            for (String obj:wmiObjs) {
                // remove \"{\"} the str is of the form[{"fsdfsdfsd"}
                log.debug("storage obj=<" + obj + ">");
                
                String serviceInstanceName = obj.substring(2, obj.length()-2);
                String serviceStr  = serviceInstanceName;
                serviceInstanceName = serviceInstanceName.replace("\\", "-");
                log.debug("service instance name=<" + serviceInstanceName + ">");
                ServiceResource service = createService("Hyper-V Virtual Storage Device", "Hyper-V Virtual Storage Device - " + serviceStr, serviceInstanceName, instanceName);                    
                services.add(service);
                
            }
            if (services.isEmpty()) {
                return null;
            }
            return services;

            // create service for each res
        }catch(PluginException e) {
            // TODO Auto-generated catch block
            log.info("faled to get Msvm_ResourceAllocationSettingData: for:" +  vmGuid + " " + e.getMessage());
            return null;
        }
    }


}
