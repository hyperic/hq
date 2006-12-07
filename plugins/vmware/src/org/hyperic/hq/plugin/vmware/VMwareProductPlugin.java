package org.hyperic.hq.plugin.vmware;

import java.io.File;
import java.io.IOException;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.sigar.vmware.VMControlLibrary;

public class VMwareProductPlugin extends ProductPlugin {

    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);

        File dir = getWorkDir("lib");

        if (dir != null) {
            try {
                VMControlLibrary.link(dir.getPath());
            } catch (IOException e) {
                getLog().error(e.getMessage());
            }

            if (VMControlLibrary.isLoaded()) {
                getLog().info("Using vmcontrol library=" +
                              VMControlLibrary.getSharedLibrary());
            }
            else {
                getLog().debug("vmcontrol library not available");
            }
        }
    }
}
