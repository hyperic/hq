/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.tomcat;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
public class JBossWeb20MeasurementPlugin extends JBossWebMeasurementPlugin {

    @Override
    public MetricValue getValue(Metric metric)
            throws PluginException,
            MetricNotFoundException,
            MetricUnreachableException {

        if (metric.getDomainName().equals("java.lang")) {
            return MetricValue.NONE;
        }
        return super.getValue(metric);
    }
}
