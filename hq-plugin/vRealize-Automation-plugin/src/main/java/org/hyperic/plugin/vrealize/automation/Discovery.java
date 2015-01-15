package org.hyperic.plugin.vrealize.automation;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import java.util.ArrayList;

public class Discovery extends DaemonDetector {

    private static final Log log = LogFactory.getLog(Discovery.class);

    /**
     * solve a weird bug with services on the whole plugin
     * @param config
     * @return
     * @throws PluginException 
     */
    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        return new ArrayList();
    }

}
