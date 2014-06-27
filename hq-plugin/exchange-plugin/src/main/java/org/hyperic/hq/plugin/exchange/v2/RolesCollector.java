package org.hyperic.hq.plugin.exchange.v2;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;

public class RolesCollector extends Collector {

    private static final Log log = LogFactory.getLog(RolesCollector.class.getName());

    @Override
    public void collect() {
        log.debug("[collect] getProperties: " + getProperties());
        String installPath = getProperties().getProperty("installPath");
        Map<String, Map<String, String>> roles = Detector.getServiceHealth(installPath);
        for (String roleName : roles.keySet()) {
            boolean running = roles.get(roleName).get("RequiredServicesRunning").equalsIgnoreCase("true");
            setValue(roleName + ".Availability", running ? Metric.AVAIL_UP : Metric.AVAIL_DOWN);
        }
    }

}
