package org.hyperic.hq.plugin.vmware;

import java.util.Properties;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.vmware.VM;
import org.hyperic.sigar.vmware.VMwareException;

public class VMwareControlPlugin
    extends ControlPlugin {

    private Properties props;

    private VMwareConnectParams params;
    private VM vm;

    public VMwareControlPlugin() {
        super();
        setName("vmware");
        //give waitForState enough time
        setTimeout(DEFAULT_TIMEOUT * 10);
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);
        this.props = config.toProperties();
    }

    protected boolean isRunning() { 
        boolean isConnected = (this.vm != null);
        try {
            if (!isConnected) {
                connectVM();
            }
            return this.vm.getExecutionState() == VM.EXECUTION_STATE_ON;
        } catch (VMwareException e) {
            return false;
        } finally {
            if (!isConnected) {
                disconnectVM();
            }
        } 
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
        String config =
            this.props.getProperty(VMwareDetector.PROP_VM_CONFIG);
        if (config == null) {
            throw new VMwareException(VMwareDetector.PROP_VM_CONFIG + "=null");
        }
        this.params = new VMwareConnectParams(this.props);
        this.vm = new VM();
        this.vm.connect(params, config, true);
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

    public void start() throws VMwareException {
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_OFF);
            this.vm.start();
            waitForState(STATE_STARTED);
        } finally {
            disconnectVM();
        }
    }

    public void stop() throws VMwareException {
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_ON);
            this.vm.stop();
            waitForState(STATE_STOPPED);
        } finally {
            disconnectVM();
        }
    }

    public void reset() throws VMwareException {
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_ON);
            this.vm.reset();
            waitForState(STATE_STARTED);
        } finally {
            disconnectVM();
        }
    }

    public void suspend() throws VMwareException {
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_ON);
            this.vm.suspend();
            waitForState(STATE_STOPPED);
        } finally {
            disconnectVM();
        }
    }

    public void resume() throws VMwareException {
        try {
            connectVM();
            checkState(VM.EXECUTION_STATE_SUSPENDED);
            this.vm.resume();
            waitForState(STATE_STARTED);
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
    
    public void revertToSnapshot() throws VMwareException {
        try {
            connectVM();
            this.vm.revertToSnapshot();
        } finally {
            disconnectVM();
        }
    }

    public void removeAllSnapshots() throws VMwareException {
        try {
            connectVM();
            this.vm.removeAllSnapshots();
        } finally {
            disconnectVM();
        }
    }
}
