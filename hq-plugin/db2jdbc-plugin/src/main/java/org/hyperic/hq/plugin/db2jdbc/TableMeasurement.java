/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.util.Map;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;

/**
 *
 * @author laullon
 */
public class TableMeasurement extends Measurement {

    public MetricValue getValue(Metric metric) throws MetricUnreachableException, MetricNotFoundException {
        MetricValue res;
        try {
            res = super.getValue(metric);
        } catch (MetricNotFoundException e) {
            if (metric.getObjectProperties().getProperty("func").equalsIgnoreCase("SNAP_GET_TAB_V91")) {
                res = new MetricValue(0);
            } else {
                throw e;
            }
        }
        return res;
    }

    protected void postProcessResults(Map results) {
        if (results.get("DATA_OBJECT_P_SIZE") != null) {
            results.put("TOTAL_SIZE", new MetricValue(((MetricValue) results.get("DATA_OBJECT_P_SIZE")).getValue() +
                    ((MetricValue) results.get("INDEX_OBJECT_P_SIZE")).getValue() +
                    ((MetricValue) results.get("LOB_OBJECT_P_SIZE")).getValue() +
                    ((MetricValue) results.get("LONG_OBJECT_P_SIZE")).getValue() +
                    ((MetricValue) results.get("XML_OBJECT_P_SIZE")).getValue()));
        }
    }
}
