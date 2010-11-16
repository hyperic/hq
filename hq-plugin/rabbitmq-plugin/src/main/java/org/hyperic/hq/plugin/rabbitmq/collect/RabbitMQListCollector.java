/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.collect;

import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;

/**
 *
 * @author administrator
 */
public abstract class RabbitMQListCollector extends RabbitMQDefaultCollector {

    @Override
    public final MetricValue getValue(Metric metric, CollectorResult result) {
        MetricValue res = result.getMetricValue(metric.getAttributeName());
        if (metric.getAttributeName().endsWith(Metric.ATTR_AVAIL)) {
            if (res.getValue() != Metric.AVAIL_UP) {
                res = new MetricValue(Metric.AVAIL_DOWN, System.currentTimeMillis());
            }
            getLog().debug("[getValue] metric=" + metric.getAttributeName() + " res=" + res.getValue());
        }
        return res;
    }
}
