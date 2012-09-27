package org.hyperic.hq.plugin.multilogtrack;

import java.io.File;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class LogTrackMeasurementPlugin extends MeasurementPlugin {
    private final Sigar sigar = new Sigar();
    @Override
    public MetricValue getValue(Metric metric) throws MetricUnreachableException, PluginException {
        String logfile = metric.getObjectProperty("logfile");
        File file = new File(logfile);
        if (metric.isAvail()) {
            if (!file.exists()) {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
            return new MetricValue(Metric.AVAIL_UP);
        }
        if (!file.exists()) {
            throw new MetricUnreachableException("file=" + logfile + " does not exist");
        }
        try {
            return new MetricValue(sigar.getFileInfo(logfile).getSize());
        } catch (SigarException e) {
            throw new PluginException(e);
        }
    }
}
