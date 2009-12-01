/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.xen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PlatformServiceDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostCpu;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.SR;

public class XenHostServiceDetector extends PlatformServiceDetector {
    private static final Log _log =
        LogFactory.getLog(XenHostServiceDetector.class.getName());

    public List getServerResources(ConfigResponse config) {
        if (config.getValue(XenUtil.PROP_URL) == null) {
            return null;
        }
        return super.getServerResources(config);
    }

    protected ServerResource getServer(ConfigResponse config) {
        //XXX same as super.getServer, but w/o fqdn in identifier
        ServerResource server = createServerResource("/");
        String name = getPlatformName();
        String type = config.getValue(ProductPlugin.PROP_PLATFORM_TYPE);
        server.setName(name + " " + type);
        server.setIdentifier(type);
        server.setProductConfig();
        server.setMeasurementConfig();
        //server.setControlConfig();        
        _log.debug("Created server=" + server.getName());
        return server;
    }

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        List<ServiceResource> services = new ArrayList<ServiceResource>();
        Properties props = serverConfig.toProperties();
        Connection conn = XenUtil.connect(props);

        Host host = XenUtil.getHost(conn, props);
        try {
            for (PBD pbd: host.getPBDs(conn)) {
                Map<String,String> dconfig = pbd.getDeviceConfig(conn);
                String device = dconfig.get(XenUtil.PROP_DEVICE);
                if (device == null) {
                    continue;
                }
                if ("true".equals(dconfig.get("legacy_mode"))) {
                    continue;
                }
                String uuid = pbd.getUuid(conn);
                ConfigResponse config = new ConfigResponse();
                config.setValue(XenUtil.PROP_SERVICE_UUID, uuid);

                SR sr = pbd.getSR(conn);
                String type = sr.getType(conn);
                ConfigResponse cprops = new ConfigResponse();
                cprops.setValue(XenUtil.PROP_TYPE, type);

                ServiceResource service =
                    createServiceResource(XenUtil.TYPE_STORAGE);
                service.setServiceName(XenUtil.TYPE_STORAGE + " " + device);
                service.setProductConfig(config);
                service.setMeasurementConfig();
                service.setCustomProperties(cprops);
                service.setDescription(sr.getNameLabel(conn));
                services.add(service);
            }
        } catch (Exception e) {
            _log.error(e.getMessage(), e);
        }

        try {
            for (PIF pif: host.getPIFs(conn)) {
                ConfigResponse config = new ConfigResponse();
                String uuid = pif.getUuid(conn);
                config.setValue(XenUtil.PROP_SERVICE_UUID, uuid);
                String device = pif.getDevice(conn);
                ConfigResponse cprops = new ConfigResponse();
                cprops.setValue(XenUtil.PROP_MAC, pif.getMAC(conn));
                cprops.setValue(XenUtil.PROP_MTU, pif.getMTU(conn));
                //XXX 4.1 only
                //cprops.setValue(XenUtil.PROP_IP, pif.getIP(conn));
                //cprops.setValue(XenUtil.PROP_NETMASK, pif.getNetmask(conn));
                //cprops.setValue(XenUtil.PROP_GATEWAY, pif.getGateway(conn));
                //cprops.setValue(XenUtil.PROP_DNS, pif.getDNS(conn));

                ServiceResource service =
                    createServiceResource(XenUtil.TYPE_NIC);
                service.setServiceName(XenUtil.TYPE_NIC + " " + device);
                service.setProductConfig(config);
                service.setMeasurementConfig();
                service.setCustomProperties(cprops);
                services.add(service);
            }
        } catch (Exception e) {
            _log.error(e.getMessage(), e);
        }

        try {
            for (HostCpu cpu: host.getHostCPUs(conn)) {
                HostCpu.Record record = cpu.getRecord(conn);
                ConfigResponse config = new ConfigResponse();
                config.setValue(XenUtil.PROP_SERVICE_UUID, record.uuid);
                ConfigResponse cprops = new ConfigResponse();
                cprops.setValue("vendor", record.vendor);
                cprops.setValue("model", record.modelname);
                cprops.setValue("speed", record.speed + "Ghz");

                ServiceResource service =
                    createServiceResource(XenUtil.TYPE_CPU);
                service.setServiceName(XenUtil.TYPE_CPU + " " + record.number);
                service.setProductConfig(config);
                service.setMeasurementConfig();
                service.setCustomProperties(cprops);
                services.add(service);
            }
        } catch (Exception e) {
            _log.error(e.getMessage(), e);
        }

        return services;
    }
}
