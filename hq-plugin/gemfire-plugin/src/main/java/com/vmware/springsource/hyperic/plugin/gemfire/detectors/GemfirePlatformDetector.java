package com.vmware.springsource.hyperic.plugin.gemfire.detectors;

import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class GemfirePlatformDetector extends PlatformDetector {

    private static final String DEF_URL = "service:jmx:rmi://localhost/jndi/rmi://:1099/jmxconnector";
//
//    @Override
//    public PlatformResource getPlatformResource(ConfigResponse config) throws PluginException {
//        getLog().debug("[getPlatformResource] config=" + config);
//        PlatformResource res = super.getPlatformResource(config);
//
//        ConfigResponse c = new ConfigResponse();
//        c.setValue("jmx.url", DEF_URL);
//        c.setValue("jmx.username", "");
//        c.setValue("jmx.password", "");
//
//        config.merge(c, true);
//        res.setProductConfig(config);
//        res.setMeasurementConfig(new ConfigResponse());
//
//        getLog().debug("[getPlatformResource] res=" + res);
//
//        return res;
//    }
}
