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

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VMGuestMetrics;
import com.xensource.xenapi.Types.VbdType;

public class XenVmDetector
    extends ServerDetector
    implements AutoServerDetector {

    private void setValue(ConfigResponse config, String key, String val) {
        if (val == null) {
            return;
        }
        config.setValue(key, val);
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        if (platformConfig.getValue(XenUtil.PROP_URL) == null) {
            return null;
        }

        List<ServerResource> servers = new ArrayList<ServerResource>();
        Properties props = platformConfig.toProperties();
        Connection conn = XenUtil.connect(props);

        Host host = XenUtil.getHost(conn, props);
        try {
            for (VM vm: host.getResidentVMs(conn)) {
                if (vm.getIsATemplate(conn)) {
                    continue;
                }
                if (vm.getIsControlDomain(conn)) {
                    continue;
                }
                ConfigResponse config = new ConfigResponse();

                //XXX should not have todo this
                final String[] keys = XenUtil.CONNECT_PROPS; 
                for (int i=0; i<keys.length; i++) {
                    config.setValue(keys[i], platformConfig.getValue(keys[i]));
                }

                ConfigResponse cprops = new ConfigResponse();
                String uuid = vm.getUuid(conn);
                String name = vm.getNameLabel(conn);

                VMGuestMetrics gmetrics = vm.getGuestMetrics(conn);
                Map<String,String> os = gmetrics.getOsVersion(conn);
                setValue(cprops, "os", os.get("name"));

                String type = getTypeInfo().getName();

                ServerResource server = new ServerResource();
                server.setType(type);
                server.setName(getPlatformName() + " " + type + " " + name);
                server.setDescription(vm.getNameDescription(conn));
                server.setInstallPath(uuid); //XXX
                server.setIdentifier(uuid);

                config.setValue(XenUtil.PROP_SERVER_UUID, uuid);
                server.setProductConfig(config);
                server.setMeasurementConfig();
                server.setControlConfig();
                server.setCustomProperties(cprops);
                servers.add(server);
            }
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
        return servers;
    }

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        List<ServiceResource> services = new ArrayList<ServiceResource>();
        Properties props = serverConfig.toProperties();
        String vmid = props.getProperty(XenUtil.PROP_SERVER_UUID);
        Connection conn = XenUtil.connect(props);
        VM vm;
        try {
            vm = VM.getByUuid(conn, vmid);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        try {
            for (VBD vbd: vm.getVBDs(conn)) {
                VbdType type = vbd.getType(conn);
                if (!type.equals(VbdType.DISK)) {
                    continue;
                }
                String uuid = vbd.getUuid(conn);
                String name = vbd.getDevice(conn);

                ConfigResponse config = new ConfigResponse();
                config.setValue(XenUtil.PROP_SERVICE_UUID, uuid);

                ConfigResponse cprops = new ConfigResponse();

                ServiceResource service =
                    createServiceResource(XenUtil.TYPE_STORAGE);
                service.setServiceName(XenUtil.TYPE_STORAGE + " " + name);
                service.setProductConfig(config);
                service.setMeasurementConfig();
                service.setCustomProperties(cprops);
                services.add(service);
            }
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        try {
            for (VIF vif: vm.getVIFs(conn)) {
                String uuid = vif.getUuid(conn);
                String name = vif.getDevice(conn);

                ConfigResponse config = new ConfigResponse();
                config.setValue(XenUtil.PROP_SERVICE_UUID, uuid);

                ConfigResponse cprops = new ConfigResponse();
                cprops.setValue(XenUtil.PROP_MAC, vif.getMAC(conn));
                long mtu = vif.getMTU(conn);
                if (mtu > 0) {
                    cprops.setValue(XenUtil.PROP_MTU, mtu);
                }

                ServiceResource service =
                    createServiceResource(XenUtil.TYPE_NIC);
                service.setServiceName(XenUtil.TYPE_NIC + " " + name);
                service.setProductConfig(config);
                service.setMeasurementConfig();
                service.setCustomProperties(cprops);
                services.add(service); 
            }
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        return services;
    }
}
