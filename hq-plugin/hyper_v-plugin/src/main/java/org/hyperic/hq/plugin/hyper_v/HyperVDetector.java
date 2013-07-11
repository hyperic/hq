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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.IntegerConfigOption;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.DetectionUtil;
import org.hyperic.hq.product.PlatformServiceDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class HyperVDetector
    extends  ServerDetector implements AutoServerDetector   {

    private static Log log =
        LogFactory.getLog("HyperVDetector");

    
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers= discoverServersWMI("Msvm_ComputerSystem","Caption-Virtual Machine","ElementName","Hyper-V VM","Hyper-V VM - ");

        return servers;
    }
  
    @Override
    protected void setProductConfig(ServerResource server, ConfigResponse config) {
        try {
            Set<String> guids = DetectionUtil.getWMIObj("Msvm_ComputerSystem", "ElementName-"+server.getIdentifier(), "Name", "");
            if (guids==null||guids.isEmpty()) {
                return;
            }
            String guid = guids.iterator().next();
            config.setValue(Collector.GUID, guid);
            Set<String> macs = DetectionUtil.getWMIObj("Msvm_SyntheticEthernetPort", "SystemName-"+guid, "PermanentAddress", "");
            if (macs==null||macs.isEmpty()) {
                return;
            }
            String mac = macs.iterator().next();
            config.setValue(Collector.MAC, mac);
        }catch(PluginException e) {
            log.error(e,new Throwable());
        }
    }

/*    protected List<ServerResource> discoverServers(String propertySet, String type, String namePrefix, String token) {
        List<ServerResource> servers = new ArrayList<ServerResource>();
        log.error("discoverServers");
        try {
            String[] instances = Pdh.getInstances(propertySet);
            log.error("num of instances found=" + instances.length);
            
            Set<String> names = new HashSet<String>();
            for (int i = 0; i < instances.length; i++) {
                log.error("instance=" +  instances[i]);
                String instance = instances[i];
                if ("_Total".equals(instance) || "<All instances>".equals(instance)) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(instance,token);
                String vmName = (String) st.nextElement();
                names.add(vmName);
                log.error("name=" + vmName);
            }
            
            for (Iterator<String> it = names.iterator(); it.hasNext();) {
                String name = it.next();
                ConfigResponse conf = new ConfigResponse();
                conf.setValue("instance.name", name);

                ServerResource server = new ServerResource();
                ConfigResponse cprops = new ConfigResponse();
                server.setProductConfig(conf);
    	        server.setMeasurementConfig();
            	server.setCustomProperties(cprops);
            	String type1 = getTypeInfo().getName();
            	//serddver.setType(type);
                server.setName(getPlatformName() + " " + type1 + " " +namePrefix + name);
                server.setDescription("");
                server.setInstallPath(name); //XXX
                server.setIdentifier(name);
                servers.add(server);
                server.setType(type1);
                //server.setName(getPlatformName() + " " + type + " " + name);

            }
    	    if (servers.isEmpty()) {
    	        log.error("no servers found");
    	        return null;
    	    }
            return servers;
        } catch (Win32Exception e) {
            log.debug("Error getting pdh data for " + propertySet + ": " + e, e);
            return null;
        }
    }
*/
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
        return null;
    }


}
