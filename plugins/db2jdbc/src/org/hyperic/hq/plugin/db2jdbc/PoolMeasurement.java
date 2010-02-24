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
public class PoolMeasurement extends Measurement {

    /**
     * 'TOTAL_LOGICAL_READS' and 'TOTAL_PHYSICAL_READS' Metrics.
     * http://publib.boulder.ibm.com/infocenter/db2luw/v9/topic/com.ibm.db2.udb.admin.doc/doc/r0021988.htm
     *
     * PAGE_HIT_RATIO
     * http://publib.boulder.ibm.com/infocenter/db2luw/v9/topic/com.ibm.db2.udb.admin.doc/doc/r0001235.htm
     *
     * INDEX_PAGE_HIT_RATIO
     * http://publib.boulder.ibm.com/infocenter/db2luw/v9/topic/com.ibm.db2.udb.admin.doc/doc/r0001238.htm
     */
    protected void postProcessResults(Map results) {
        if (results.get("POOL_DATA_L_READS") != null) {
            results.put("TOTAL_LOGICAL_READS", new MetricValue(((MetricValue) results.get("POOL_DATA_L_READS")).getValue() + ((MetricValue) results.get("POOL_INDEX_L_READS")).getValue()));
            results.put("TOTAL_PHYSICAL_READS", new MetricValue(((MetricValue) results.get("POOL_DATA_P_READS")).getValue() + ((MetricValue) results.get("POOL_INDEX_P_READS")).getValue()));
            results.put("DATA_PAGE_HIT_RATIO", new MetricValue(((MetricValue) results.get("POOL_DATA_P_READS")).getValue() / ((MetricValue) results.get("POOL_DATA_L_READS")).getValue()));
            results.put("INDEX_PAGE_HIT_RATIO", new MetricValue(((MetricValue) results.get("POOL_INDEX_P_READS")).getValue() / ((MetricValue) results.get("POOL_INDEX_L_READS")).getValue()));
        }
    }
}
