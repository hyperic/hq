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
import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
    
import com.ibm.icu.util.StringTokenizer;

public class HypervDetector extends   SystemServerDetector  {

    @Override
    protected String getServerType() {
        return SystemPlugin.HYPERV_SERVER_NAME;
    }
    
    protected ArrayList<AIServiceValue> getHyperVServices(String propertySet, String type, String namePrefix, String token, boolean toLower) {
        ArrayList<AIServiceValue> services = new ArrayList<AIServiceValue>();
        try {
            String[] instances = Pdh.getInstances(propertySet);
            log.debug("num of instances found=" + instances.length);
            
            Set<String> names = new HashSet<String>();
            for (int i = 0; i < instances.length; i++) {
                String instance = instances[i];
                log.debug("instance=<" + instance + ">");
                if ("_Total".equals(instance) || "<All instances>".equals(instance)) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(instance,token);
                String name = (String) st.nextElement();
                names.add(name);                
            }
            
            for (String name:names) {
                // add?"lfsdl;
                String info = namePrefix + name;
                AIServiceValue svc = 
                    createSystemService(type,
                                         getFullServiceName(info));

                try {
                    ConfigResponse cprops = new ConfigResponse();
                    svc.setCustomProperties(cprops.encode());
                    ConfigResponse conf = new ConfigResponse();                   
                    
                    conf.setValue("instance.name", name);
                    conf.setValue(propertySet, name);
                    svc.setProductConfig(conf.encode());
                    svc.setMeasurementConfig(ConfigResponse.EMPTY_CONFIG);
                    
                } catch (EncodingException e) {
                    
                }
                                       
                 services.add(svc);
            
            }
            if (services.isEmpty()) {
                log.debug("no servers found");
                return null;
            }
            return services;
        } catch (Win32Exception e) {
            log.debug("Error getting pdh data for " + propertySet + ": " + e, e);
            return null;
        }
    }

    @Override
    protected ArrayList<AIServiceValue> getSystemServiceValues(Sigar sigar, ConfigResponse config) throws SigarException {
        if (!OperatingSystemReflection.IS_HYPER_V()) {
            return null;
        }
        ArrayList<AIServiceValue>  services = new ArrayList<AIServiceValue>();
        
        ArrayList<AIServiceValue> netServices = getHyperVServices(SystemPlugin.PROP_HYPERV_NETWORK_INTERFACE, SystemPlugin.HYPERV_NETWORK_INTERFACE, "Network Interface - ","", false);
        if (netServices!=null&&!netServices.isEmpty()) {
            services.addAll(netServices);
        }

        
        ArrayList<AIServiceValue> diskServices = getHyperVServices(SystemPlugin.PROP_HYPERV_PHYSICAL_DISK, SystemPlugin.HYPERV_PHYSICAL_DISK, "PhysicalDisk - ","", true);
        if (diskServices!=null&&!diskServices.isEmpty()) {
            services.addAll(diskServices);
        }
        
        ArrayList<AIServiceValue> logicalProcessorServices = getHyperVServices(SystemPlugin.PROP_HYPERV_LOGICAL_PROCESSOR, SystemPlugin.HYPERV_LOGICAL_PROCESSOR, "Logical Processor - ","", false);
        if (logicalProcessorServices!=null&&!logicalProcessorServices.isEmpty()) {
            services.addAll(logicalProcessorServices);
        }
        
        AIServiceValue memoryServices = getHyperVMemoryService();
        if (memoryServices != null) {
            services.add(memoryServices);
        }

        return services;
    }

    private AIServiceValue getHyperVMemoryService() {
        String info =  SystemPlugin.HYPERV_MEMORY;
        AIServiceValue svc = 
            createSystemService(SystemPlugin.HYPERV_MEMORY,
                                 getFullServiceName(info));
        svc.setMeasurementConfig(ConfigResponse.EMPTY_CONFIG);
        svc.setProductConfig(ConfigResponse.EMPTY_CONFIG);
        return svc;
    }
    
 

}
