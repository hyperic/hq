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
public class TableSpaceMeasurement extends PoolMeasurement {

    protected void postProcessResults(Map results) {
        super.postProcessResults(results);
        if (results.get("TBSP_UTILIZATION_PERCENT") != null) {
            results.put("TBSP_UTILIZATION_PERCENT", new MetricValue(((MetricValue) results.get("TBSP_UTILIZATION_PERCENT")).getValue() / 100));
        }
    }
}

