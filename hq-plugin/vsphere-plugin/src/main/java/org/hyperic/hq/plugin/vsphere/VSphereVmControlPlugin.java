/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], VMWare, Inc.
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

package org.hyperic.hq.plugin.vsphere;

import java.util.Properties;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VSphereVmControlPlugin extends ControlPlugin {

    private Properties _props;

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);
        _props = config.toProperties();
    }

    private String getType() {
        return VSphereUtil.VM;
    }

    private String getVmName() {
        return _props.getProperty(VSphereVmCollector.PROP_VM);
    }

    private String getUuid() {
        return _props.getProperty(VSphereCollector.PROP_UUID);
    }
    
    public void doAction(String action, String[] args)
        throws PluginException {

        setResult(ControlPlugin.RESULT_FAILURE);

        VSphereUtil vim = VSphereUtil.getInstance(getConfig());
        try {
            
            VirtualMachine vm = (VirtualMachine)vim.findByUuid(getType(), getUuid());
            Task task;

            if (action.equals("createSnapshot")) {
                if (args.length < 1 || args.length > 2) {
                    throw new PluginException("Usage: name, description");
                }
                String name = args[0];
                String description = "";
                if (args.length == 2) {
                    description = args[1];
                }
                task = vm.createSnapshot_Task(name, description,
                                              true, true);
            }
            else if (action.equals("removeAllSnapshots"))
            {
                task = vm.removeAllSnapshots_Task();
            }
            else if (action.equals("reset")) {
                task = vm.resetVM_Task();
            }
            else if (action.equals("revertToCurrentSnapshot")) {
                task = vm.revertToCurrentSnapshot_Task(null);
            }
            else if (action.equals("stop")) {
                task = vm.powerOffVM_Task();
            }
            else if (action.equals("start")) {
                task = vm.powerOnVM_Task(null);
            }
            else if (action.equals("suspend")) {
                task = vm.suspendVM_Task();
            }
            else if (action.equals("rebootGuest")) {
                vm.rebootGuest();
                setResult(ControlPlugin.RESULT_SUCCESS);
                return;
            }
            else if (action.equals("shutdownGuest")) {
                vm.shutdownGuest();
                setResult(ControlPlugin.RESULT_SUCCESS);
                return;
            }
            else if (action.equals("standbyGuest")) {
                vm.standbyGuest();
                setResult(ControlPlugin.RESULT_SUCCESS);
                return;
            }
            else if (action.equals("guestHeartbeatStatus")) {
                setMessage(vm.getGuestHeartbeatStatus().toString());
                setResult(ControlPlugin.RESULT_SUCCESS);
                return;
            }
            else {
                throw new PluginException("Unsupported action: " + action);
            }

            String result = task.waitForMe();
            if (Task.SUCCESS.equals(result)) {
                setResult(ControlPlugin.RESULT_SUCCESS);
            }
            else {
                setMessage(result);
            }
        } catch (PluginException e) {
            setMessage(e.getMessage());
            throw e;
        } catch (Exception e) {
            setMessage(e.getMessage());
            throw new PluginException(action + " " + getType() 
                                      + "[name=" + getVmName()
                                      + ", uuid=" + getUuid()
                                      + "]: " + e.getMessage(), e);
        } finally  {
            VSphereUtil.dispose(vim);
        }
    }
}
