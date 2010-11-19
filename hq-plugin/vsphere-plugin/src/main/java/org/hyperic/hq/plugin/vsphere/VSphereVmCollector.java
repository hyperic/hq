/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

import org.hyperic.hq.product.Metric;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;

public class VSphereVmCollector extends VSphereHostCollector {

    static final String TYPE = VSphereUtil.VM;
    static final String PROP_VM = "vm";

    protected String getName() {
        return getProperties().getProperty(PROP_VM);
    }

    protected String getType() {
        return TYPE;
    }

    protected void setAvailability(ManagedEntity entity) {
        
        double avail;
        if (entity == null) {
            setValue(Metric.ATTR_AVAIL, Metric.AVAIL_UNKNOWN);
            return;
        }
        VirtualMachine vm = (VirtualMachine) entity;
        VirtualMachineRuntimeInfo runtime = vm.getRuntime();
        if (runtime == null) {
            setValue(Metric.ATTR_AVAIL, Metric.AVAIL_UNKNOWN);
            return;
        }

        VirtualMachinePowerState state = runtime.getPowerState();
        if (state == VirtualMachinePowerState.poweredOn) {
            avail = Metric.AVAIL_UP;
        } else if (state == VirtualMachinePowerState.poweredOff) {
            avail = Metric.AVAIL_POWERED_OFF;
        } else if (state == VirtualMachinePowerState.suspended) {
            avail = Metric.AVAIL_PAUSED;
        } else {
            avail = Metric.AVAIL_UNKNOWN;
        }
        setValue(Metric.ATTR_AVAIL, avail);
    }
}
