package org.hyperic.hq.plugin.exchange.v2;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.mssql.PDH;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.win32.Pdh;
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
                if (metric.getObjectProperties().get("services") != null) {
                    String[] services = ((String) metric.getObjectProperties().get("services")).split(",");
                    for (String service : services) {
                        boolean isOK = isServiceRunning(service.trim());
                        log.debug("[discoverServices] service:'" + service + ") runnig:" + (isOK ? "YES" : "NO"));
                        if (!isOK) {
                            res = new MetricValue(Metric.AVAIL_DOWN);
                        }
                    }
                } else {
                    res = new MetricValue(Metric.AVAIL_DOWN);
                }
            }
        } else if (metric.getDomainName().equals("pdh")) {
            if (metric.isAvail()) {
                return getPDHAvail(metric);
            }
            return getPDH(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("pdh_c")) {
            return Collector.getValue(this, metric);
        } else {
            res = super.getValue(metric);
        }
        return res;
    }

    private MetricValue getPDHAvail(Metric metric) {
        String obj = metric.getObjectPropString();
        MetricValue res;

        Pattern reg = Pattern.compile("([^\\(]*)\\(([^\\)]*)\\)");
        Matcher match = reg.matcher(obj);

        if (match.find()) {
            obj = match.group(1);
            String instance = match.group(2);
            log.debug("[getPDHAvail] obj='" + obj + "' instance='" + instance + "'");
            try {
                List<String> instaces = Arrays.asList(PDH.getInstances(obj));
                if (instaces.contains(instance)) {
                    res = new MetricValue(Metric.AVAIL_UP);
                } else {
                    res = new MetricValue(Metric.AVAIL_DOWN);
                }
            } catch (Throwable ex) {
                res = new MetricValue(Metric.AVAIL_DOWN);
                log.debug("[getPDHAvail] error on metric:'" + metric + "' (obj:" + obj + ") :" + ex.getLocalizedMessage(), ex);
            }
        } else {
            log.debug("[getPDHAvail] obj='" + obj + "'");
            try {
                String keys[] = Pdh.getKeys(obj);
                if (keys.length > 0) {
                    res = new MetricValue(Metric.AVAIL_UP);
                } else {
                    res = new MetricValue(Metric.AVAIL_DOWN);
                }
            } catch (Throwable ex) {
                res = new MetricValue(Metric.AVAIL_DOWN);
                log.debug("[getPDHAvail] error on metric:'" + metric + "' (obj:" + obj + ") :" + ex.getLocalizedMessage(), ex);
            }
        }
        return res;
    }

    private MetricValue getPDH(Metric metric) {
        String obj = "\\" + metric.getObjectPropString();

        if (!metric.isAvail()) {
            obj += "\\" + metric.getAttributeName();
        }

        MetricValue res;
        try {
            double val = PDH.getValue(obj);
            log.debug("[getPDH] obj:'" + obj + "' val:'" + val + "'");
            res = new MetricValue(val);
        } catch (Throwable ex) {
            res = MetricValue.NONE;
            log.debug("[getPDH] error on metric:'" + metric + "' (obj:" + obj + ") :" + ex.getLocalizedMessage(), ex);
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
