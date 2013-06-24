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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.IntegerConfigOption;

import org.hyperic.hq.product.DetectionUtil;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class HyperVDetector
    extends ServerDetector
    implements RegistryServerDetector {

    private static Log log =
        LogFactory.getLog("HyperVDetector");

    public List getServerResources(ConfigResponse platformConfig,
                                   String path, RegistryKey current) throws PluginException {
        if (!new File(path).exists()) {
            log.debug(path + " does not exist");
            return null;
        }

        ConfigResponse cprops = new ConfigResponse();
        ServerResource server = createServerResource(path);
        server.setProductConfig();
        server.setMeasurementConfig();
        server.setCustomProperties(cprops);

        List servers = new ArrayList();
        servers.add(server);
        return servers;
    }
    
    protected List<ServiceResource> discoverServices(String propertySet, String type, String namePrefix) {
        List<ServiceResource> services = new ArrayList<ServiceResource>();

        try {
            String[] instances = Pdh.getInstances(propertySet);
            Set<String> names = new HashSet<String>();
            for (int i = 0; i < instances.length; i++) {
                String instance = instances[i];
                if ("_Total".equals(instance) || "<All instances>".equals(instance)) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(instance,":");
                String vmName = (String) st.nextElement();
                names.add(vmName);
            }
            for (Iterator<String> it = names.iterator(); it.hasNext();) {
                String name = it.next();
                ServiceResource service = new ServiceResource();
                service.setType(this, type);
                service.setServiceName(namePrefix + name);

                ConfigResponse conf = new ConfigResponse();
                conf.setValue("instance.name", name);
                service.setProductConfig(conf);
                service.setMeasurementConfig();
                services.add(service);
            }
            return services;
        } catch (Win32Exception e) {
            log.debug("Error getting pdh data for " + propertySet + ": " + e, e);
            return null;
        }
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
        List<ServiceResource> services = new ArrayList();

        List<ServiceResource> vmServices = discoverServices("Hyper-V Hypervisor Partition","Hyper-V VM","Hyper-V VM - ");
        if (vmServices!=null&&!vmServices.isEmpty()) {
            services.addAll(vmServices);
        }

        List<ServiceResource> netServices = discoverServices("Network Interface","Network Interface","Network Interface - ");
        if (netServices!=null&&!netServices.isEmpty()) {
            services.addAll(netServices);
        }

        List<ServiceResource> diskServices = discoverServices("PhysicalDisk","PhysicalDisk","PhysicalDisk - ");
        if (diskServices!=null&&!diskServices.isEmpty()) {
            services.addAll(diskServices);
        }


        return services;
    }
}
