package org.hyperic.hq.plugin.vmware;

import java.io.File;
import java.util.Map;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.vmware.VMwareException;

public class VMCollector extends Collector {

    private String config;

    public void collect() {
        try {
            Map values =
                VMwareMetrics.getInstance(getProperties(), this.config);

            addValues(values);
        } catch (VMwareException e) {
            setErrorMessage(e.getMessage());
        }
    }

    protected void init() throws PluginException {

        this.config = getProperty("Config");
        setSource(new File(this.config).getName());
    }
}
