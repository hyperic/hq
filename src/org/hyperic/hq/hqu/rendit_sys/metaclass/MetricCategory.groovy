package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl
import org.hyperic.hq.measurement.server.session.DerivedMeasurement

class MetricCategory {
    /**
     * Get the last data point for a collection of DerivedMeasurements
     *
     * @param c          A collection of DerivedMeasurement
     * @param timeWindow Time (in millis) prior to 'now' to search for data
     *
     * @return a Map of the passed DerivedMeasurements onto MetricValues
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
}
