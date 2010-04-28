/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.util.Map;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;

/**
 *
 * @author laullon
 */
public class MemoryPoolMeasurement extends Measurement {

    protected String getQuery(Metric metric) {
        String query = super.getQuery(metric);
        query = query.replaceAll("POOL_SECONDARY_ID=''", "POOL_SECONDARY_ID is NULL");
        return query;
    }

    protected void postProcessResults(Map results) {
        results.put("POOL_CUR_SIZE_USED", new MetricValue(((MetricValue) results.get("POOL_CUR_SIZE")).getValue() / ((MetricValue) results.get("POOL_CONFIG_SIZE")).getValue()));
        results.put("POOL_WATERMARK_USED", new MetricValue(((MetricValue) results.get("POOL_WATERMARK")).getValue() / ((MetricValue) results.get("POOL_CONFIG_SIZE")).getValue()));
    }
}

