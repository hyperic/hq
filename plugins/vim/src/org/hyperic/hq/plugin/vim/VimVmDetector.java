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

package org.hyperic.hq.plugin.vim;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.Description;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.ToolsConfigInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class VimVmDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final Log _log =
        LogFactory.getLog(VimVmDetector.class.getName());

    static final String PROP_INSTANCE = "instance";

    private ServerResource discoverVM(ServiceInstance vim,
                                      VirtualMachine vm)
        throws Exception {

        VirtualMachineRuntimeInfo runtime = vm.getRuntime();

        VirtualMachineConfigInfo info = vm.getConfig();

        GuestInfo guest = vm.getGuest();
        ResourcePool pool = vm.getResourcePool();

        ServerResource server = createServerResource("/");
        VirtualMachineFileInfo files = info.getFiles();
        server.setInstallPath(files.getVmPathName());
        server.setIdentifier(info.getUuid());
        server.setName(info.getName());

        ConfigResponse config = new ConfigResponse();
        config.setValue(VimVmCollector.PROP_VM, info.getName());
        server.setProductConfig(config);
        //ConfigInfo
        ConfigResponse cprops = new ConfigResponse();
        cprops.setValue("guestOS", info.getGuestFullName());
        cprops.setValue("version", info.getVersion());
        //HardwareInfo
        VirtualHardware hw = info.getHardware();
        cprops.setValue("numvcpus", hw.getNumCPU());
        cprops.setValue("memsize", hw.getMemoryMB());
        //ToolsInfo
        ToolsConfigInfo tools = info.getTools();
        Integer toolsVersion = tools.getToolsVersion();
        if (toolsVersion != null) {
            cprops.setValue("toolsVersion", toolsVersion.toString());
        }
        //PoolInfo
        cprops.setValue("pool", (String)pool.getPropertyByPath("name"));                        

        String state = runtime.getPowerState().toString();
        if (state.equals("poweredOn")) {
            server.setMeasurementConfig();
            server.setControlConfig();
            String name;
            if ((name = guest.getHostName()) != null) {
                cprops.setValue("hostName", name);
            }
            //NetInfo
            GuestNicInfo[] nics = guest.getNet();
            if (nics != null) {
                for (int i=0; i<nics.length; i++) {
                    String mac = nics[i].getMacAddress();
                    String[] ips = nics[i].getIpAddress();
                    if ((mac != null) && (ips != null) && (ips.length != 0)) {
                        cprops.setValue("macAddress", mac);
                        cprops.setValue("ip", ips[0]);
                    }
                }
            }
        }
        else {
            _log.info(info.getName() + " powerState=" + state);
            return null;
        }

        server.setCustomProperties(cprops);
        return server;
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        String hostname = platformConfig.getValue(VimUtil.PROP_HOSTNAME);
        if (hostname == null) {
            return null;
        }
        VimUtil vim = null;
        List servers = new ArrayList();
        try {
            vim = VimUtil.getInstance(platformConfig.toProperties());
            ManagedEntity[] vms = vim.find(VimUtil.VM);

            for (int i=0; i<vms.length; i++) {
                if (! (vms[i] instanceof VirtualMachine)) {
                    _log.debug(vms[i] + " not a VirtualMachine, type=" +
                               vms[i].getMOR().getType());
                    continue;
                }
                VirtualMachine vm = (VirtualMachine)vms[i];
                if (vm.getConfig().isTemplate()) {
                    continue; //filter out template VMs
                }

                try {
                    ServerResource server = discoverVM(vim, vm);
                    if (server != null) {
                        servers.add(server);
                    }
                } catch (Exception e) {
                    _log.error(e.getMessage(), e);
                }
            }
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            VimUtil.dispose(vim);
        }

        return servers;
    }

    private ServiceResource createServiceResource(String type, String instance) {
        ServiceResource service = super.createServiceResource(type);
        ConfigResponse config = new ConfigResponse();
        config.setValue(PROP_INSTANCE, instance);
        service.setProductConfig(config);
        service.setMeasurementConfig();
        service.setServiceName(type + " " + instance);
        return service;
    }

    private ServiceResource discoverCPU(int num) {
        ServiceResource service =
            createServiceResource("CPU", String.valueOf(num));
        return service;
    }

    private ServiceResource discoverNIC(VirtualDevice device) {
        ServiceResource service =
            createServiceResource("NIC", String.valueOf(device.getKey()));
        Description info = device.getDeviceInfo();
        service.setServiceName(info.getLabel());
        service.setDescription(info.getSummary());
        return service;
    }

    protected List discoverServices(ConfigResponse config) throws PluginException {
        List services = new ArrayList();
        VimUtil vim = null;
        String name = config.getValue(VimVmCollector.PROP_VM);
        try {
            vim = VimUtil.getInstance(config.toProperties());
            VirtualMachine vm =
                (VirtualMachine)vim.find(VimUtil.VM, name);

            VirtualHardware hw = vm.getConfig().getHardware();
            for (int i=0; i<hw.getNumCPU(); i++) {
                services.add(discoverCPU(i));
            }

            VirtualDevice[] devices = hw.getDevice();
            for (int i=0; i<devices.length; i++) {
                VirtualDevice device = devices[i];
                Description info = device.getDeviceInfo();

                if (info.getLabel().startsWith("Network Adapter")) {
                    services.add(discoverNIC(device));
                }
            }
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            VimUtil.dispose(vim);
        }

        return services;
    }
}
