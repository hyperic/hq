package com.vmware.springsource.hyperic.plugin.gemfire;

import org.apache.commons.logging.Log;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxMeasurementPlugin;
import org.hyperic.util.config.ConfigResponse;

public class MemberMeasurement extends MxMeasurementPlugin {

    Log log = getLog();

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        log.debug("[getValue] metric=" + metric);
        if (metric.getDomainName().equals("collector")) {
            return Collector.getValue(this, metric);
        }

        return super.getValue(metric);
    }

    @Override
    public String translate(String template, ConfigResponse config) {
        template = super.translate(template, config);
        log.debug("-------> " + template);
        if (template.contains("GemFire.Statistic")) {
            template = template.replaceAll("(\\w*)\\((\\d*)\\)<(\\w*)>%3A(\\d*)/(\\d*)", "$1($2)<$3>-$4/$5"); // %3A == :
            log.debug("-------> " + template);
        }
        return template;
    }
}
