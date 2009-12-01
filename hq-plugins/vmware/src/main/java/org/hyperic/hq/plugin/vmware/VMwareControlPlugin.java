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

package org.hyperic.hq.plugin.vmware;

import java.io.File;
import java.util.Properties;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerControlPlugin;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.vmware.VM;
import org.hyperic.sigar.vmware.VMwareException;

public class VMwareControlPlugin
    extends ServerControlPlugin {

    private Properties props;

    private VMwareConnectParams params;
    private VM vm;
    private boolean hasVIX;
    private String vmx;

    private boolean useVIX() {
        return this.hasVIX;
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        VMwareProductPlugin.checkIsLoaded();

        super.configure(config);
        this.props = config.toProperties();
        String vmrun;
        if (isWin32()) {
            vmrun = "C:/Program Files/VMware/VMware VIX/vmrun.exe";
        }
        else {
            vmrun = "/usr/bin/vmrun";
        }
        this.hasVIX = new File(vmrun).exists();

        this.vmx =
            this.props.getProperty(VMwareDetector.PROP_VM_CONFIG);
        if (this.vmx == null) {
            throw new PluginException(VMwareDetector.PROP_VM_CONFIG + "=null");
        }

        setControlProgram(vmrun);
        setTimeout(10 * 60 * 1000); //10 minutes
    }

    private void checkState(int state) throws VMwareException {
        int exState = vm.getExecutionState();
        if (exState != state) {
            throw new VMwareException("VM state is " +
                                      VM.EXECUTION_STATES[exState]); 
        }
    }

    private void connectVM() throws VMwareException {
        if (this.params != null) {
            return;
        }

        this.params = new VMwareConnectParams(this.props);
        this.vm = new VM();
        this.vm.connect(params, this.vmx, true);
    }

    private void disconnectVM() {
        if (this.params != null) {
            this.params.dispose(); 
            this.params = null;
        }
        if (this.vm != null) {
            this.vm.disconnect();
            this.vm.dispose();
            this.vm = null;
        }
    }

    private boolean vmRun(String cmd) {
        if (useVIX()) {
            String[] args = { cmd, this.vmx };      
            doCommand(args);
            return true;
        }
        else {
            return false;
        }
    }

    public void start() throws VMwareException {
        if (vmRun("start")) {
            return;
        }
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_OFF);
            this.vm.start();
        } finally {
            disconnectVM();
        }
    }

    public void stop() throws VMwareException {
        if (vmRun("stop")) {
            return;
        }
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_ON);
            this.vm.stop();
        } finally {
            disconnectVM();
        }
    }

    public void reset() throws VMwareException {
        if (vmRun("reset")) {
            return;
        }
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_ON);
            this.vm.reset();
        } finally {
            disconnectVM();
        }
    }

    public void suspend() throws VMwareException {
        if (vmRun("suspend")) {
            return;
        }
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_ON);
            this.vm.suspend();
        } finally {
            disconnectVM();
        }
    }

    public void resume() throws VMwareException {
        if (vmRun("start")) {
            return;
        }
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_SUSPENDED);
            this.vm.resume();
        } finally {
            disconnectVM();
        }
    }

    public void createSnapshot(String[] args) throws VMwareException {
        String name=null, description=null;
        if (args.length >= 1) {
            name = args[0];
        }
        if (args.length >= 2) {
            description = args[1];
        }
        if (vmRun("snapshot")) {
            return;
        }
        try {
            connectVM();
            this.vm.createSnapshot(name, description, true, true);
        } finally {
            disconnectVM();
        }
    }

    //XXX not too useful at the moment need to be able
    //to display in HQ
    public void saveScreenshot(String[] args) throws VMwareException {
        try {
            connectVM();
            String name =
                (args.length == 0) ?
                this.vm.getDisplayName() + ".png" :
                args[0];
            getLog().debug("saveScreenshot(" + name + ")");
            this.vm.saveScreenshot(name);
        } finally {
            disconnectVM();
        }
    }

    //VMware server
    public void revertSnapshot() throws VMwareException {
        revertToSnapshot();
    }

    //ESX
    public void revertToSnapshot() throws VMwareException {
        if (vmRun("revertToSnapshot")) {
            return;
        }
        try {
            connectVM();
            this.vm.revertToSnapshot();
        } finally {
            disconnectVM();
        }
    }

    //VMware server
    public void deleteSnapshot() throws VMwareException {
        removeAllSnapshots();
    }

    //ESX
    public void removeAllSnapshots() throws VMwareException {
        if (vmRun("deleteSnapshot")) {
            return;
        }
        try {
            connectVM();
            this.vm.removeAllSnapshots();
        } finally {
            disconnectVM();
        }
    }
}
