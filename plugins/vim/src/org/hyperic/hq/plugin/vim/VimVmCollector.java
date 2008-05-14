package org.hyperic.hq.plugin.vim;

import com.vmware.vim.ManagedObjectReference;

public class VimVmCollector extends VimHostCollector {

    protected String getName() {
        return getProperties().getProperty("vm");
    }

    protected ManagedObjectReference getRoot() {
        return null;
    }

    protected String getType() {
        return "VirtualMachine";
    }

    protected void collect(VimUtil vim) throws Exception {
        super.collect(vim);
    }
}
