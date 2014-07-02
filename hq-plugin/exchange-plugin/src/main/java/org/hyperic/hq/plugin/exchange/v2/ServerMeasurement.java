package org.hyperic.hq.plugin.exchange.v2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;

public class ServerMeasurement extends Win32MeasurementPlugin {

    private static final Log log = LogFactory.getLog(ServerMeasurement.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        log.debug("[getValue] metric=" + metric);
        MetricValue res = MetricValue.NONE;

        if (metric.getDomainName().equals("exchange")) {
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_UP);
                String[] services = ((String) metric.getObjectProperties().get("services")).split(",");
                for (String service : services) {
                    boolean isOK = isServiceRunning(service.trim());
                    log.debug("[discoverServices] service:'" + service + ") runnig:" + (isOK ? "YES" : "NO"));
                    if (!isOK) {
                        res = new MetricValue(Metric.AVAIL_DOWN);
                    }
                }
            }
        } else {
            res = super.getValue(metric);
        }
        return res;
    }

    private static boolean isServiceRunning(String name) {
        Service svc = null;
        try {
            svc = new Service(name);
            return svc.getStatus() == Service.SERVICE_RUNNING;
        } catch (Win32Exception e) {
            log.debug("[isServiceRunning] service name = '" + name + "' - " + e, e);
            return false;
        } finally {
            if (svc != null) {
                svc.close();
            }
        }
    }

}
