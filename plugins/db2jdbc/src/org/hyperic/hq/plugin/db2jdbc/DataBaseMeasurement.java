/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.util.Map;
import org.hyperic.hq.product.MetricValue;

/**
 *
 * @author laullon
 */
public class DataBaseMeasurement extends PoolMeasurement {

    protected void postProcessResults(Map results) {
        super.postProcessResults(results);
        results.put("LOCK_LIST_IN_USE", new MetricValue(((MetricValue) results.get("LOCK_LIST_IN_USE")).getValue() * 4));

        results.put("DIRECT_READS_RATIO", new MetricValue(((MetricValue) results.get("DIRECT_READS")).getValue() / ((MetricValue) results.get("DIRECT_READ_REQS")).getValue()));
        results.put("DIRECT_READ_TIME_AVE", new MetricValue(((MetricValue) results.get("DIRECT_READ_TIME")).getValue() / ((MetricValue) results.get("DIRECT_READS")).getValue()));

        results.put("DIRECT_WRITE_RATIO", new MetricValue(((MetricValue) results.get("DIRECT_WRITES")).getValue() / ((MetricValue) results.get("DIRECT_WRITE_REQS")).getValue()));
        results.put("DIRECT_WRITE_TIME_AVE", new MetricValue(((MetricValue) results.get("DIRECT_WRITE_TIME")).getValue() / ((MetricValue) results.get("DIRECT_WRITES")).getValue()));

    }
}
