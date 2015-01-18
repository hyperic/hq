package org.hyperic.plugin.vrealize.automation;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class Discovery extends DaemonDetector {


    /**
     * solve a weird bug with services on the whole plugin
     * @param config
     * @return
     * @throws PluginException
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        return new ArrayList();
    }

}
