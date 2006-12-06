package org.hyperic.hq.plugin.vmware;

import java.util.Map;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.vmware.VMwareException;

public class ServerCollector extends Collector {

    public void collect() {
        try {
            Map values =
                VMwareMetrics.getInstance(getProperties());

            addValues(values);
            setAvailability(true);
        } catch (VMwareException e) {
            setAvailability(true);
            setErrorMessage(e.getMessage());
        }
    }

    protected void init() throws PluginException {
        setSource(getPlugin().getName());
    }
}
