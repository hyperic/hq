package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.product.MetricValue
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl
import org.hyperic.hq.measurement.server.session.Measurement

class MetricCategory {
    static String urlFor(Measurement d, String context) {
        "/resource/common/monitor/Visibility.do?m=${d.template.id}&eid=${d.entityId}&mode=chartSingleMetricSingleResource"
    }
    
    
    /**
     * Get the last data point for a collection of Measurements
     *
     * @param c          A collection of Measurement
     * @param timeWindow Time (in millis) that the resultant metric values
     *                   must be greater than.  (i.e. only return metric values
     *                   > this time value)                   
     *
     * @return a Map of the passed Measurements onto MetricValues
     */
    static Map getLastDataPoints(Collection c, long timeWindow) {
        Integer[] mids = new Integer[c.size()];
        def idToMetric = [:]
        
        int i = 0;
        for (Iterator it = c.iterator(); it.hasNext(); i++) {
            def m = it.next();
            mids[i] = m.id;
            idToMetric[m.id] = m
        }
        
        def vals = DataManagerEJBImpl.one.getLastDataPoints(mids, timeWindow)
        def res  = [:]
        mids.each { mid ->
            def metric = idToMetric[mid]
            res[metric] = vals[mid]
        }
        res
    }
    
    /**
     * Get the last data point for a derived measurement.
     *
     * @param timeWindow The minimum timestamp (in millis) that the result 
     *                   will have.
     */
    static MetricValue getLastDataPoint(Measurement m, long timeWindow) {
        [m].getLastDataPoints(timeWindow)[m]
    }
    
    /**
     * Get the last data point collected for a derived measurement.
     */
    static MetricValue getLastDataPoint(Measurement m) {
        m.getLastDataPoint(0)
    }
}
