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
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim.ManagedObjectReference;
import com.vmware.vim.ToolsConfigInfo;
import com.vmware.vim.VirtualHardware;
import com.vmware.vim.VirtualMachineConfigInfo;
import com.vmware.vim.VirtualMachineFileInfo;
import com.vmware.vim.VirtualMachinePowerState;
import com.vmware.vim.VirtualMachineRuntimeInfo;

public class VimVmDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final Log _log =
        LogFactory.getLog(VimVmDetector.class.getName());

    private ServerResource discoverVM(VimUtil vim,
                                      ManagedObjectReference vm)
        throws Exception {

        VirtualMachineRuntimeInfo runtime =
            (VirtualMachineRuntimeInfo)vim.getUtil().getDynamicProperty(vm, "runtime");

        VirtualMachineConfigInfo info =
            (VirtualMachineConfigInfo)vim.getUtil().getDynamicProperty(vm, "config");

        ServerResource server = createServerResource("/");
        VirtualMachineFileInfo files = info.getFiles();
        server.setInstallPath(files.getVmPathName());
        server.setIdentifier(info.getUuid());
        server.setName(info.getName());
        ConfigResponse config = new ConfigResponse();
        config.setValue(VimVmCollector.PROP_VM, info.getName());
        server.setProductConfig(config);

        ConfigResponse cprops = new ConfigResponse();
        cprops.setValue("guestOS", info.getGuestFullName());
        cprops.setValue("version", info.getVersion());
        VirtualHardware hw = info.getHardware();
        cprops.setValue("numvcpus", hw.getNumCPU());
        cprops.setValue("memsize", hw.getMemoryMB());
        ToolsConfigInfo tools = info.getTools();
        Integer toolsVersion = tools.getToolsVersion();
        if (toolsVersion != null) {
            cprops.setValue("toolsVersion", toolsVersion.toString());
        }
        server.setCustomProperties(cprops);
        String state = runtime.getPowerState().getValue();
        if (state.equals(VirtualMachinePowerState.poweredOn)) {
            server.setMeasurementConfig();
        }
        else {
            _log.info(info.getName() + " powerState=" + state);
        }
        return server;
    }

    public List getServerResources(ConfigResponse platformConfig)
            throws PluginException {

        String hostname = platformConfig.getValue(VimUtil.PROP_HOSTNAME);
        if (hostname == null) {
            return null;
        }

        VimUtil vim = new VimUtil();
        List servers = new ArrayList();
        try {
            vim.init(platformConfig.toProperties());
            ManagedObjectReference mor = vim.getHost(hostname);
            ArrayList vms =
                vim.getUtil().getDecendentMoRefs(mor, VimVmCollector.TYPE, null);
            for (int i=0; i<vms.size(); i++) {
                try {
                    ServerResource server =
                        discoverVM(vim, (ManagedObjectReference)vms.get(i));
                    if (server != null) {
                        servers.add(server);
                    }
                } catch (Exception e) {
                    _log.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            vim.dispose();
        }

        return servers;
    }
}
