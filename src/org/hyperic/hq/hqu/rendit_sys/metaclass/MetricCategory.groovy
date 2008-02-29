package org.hyperic.hq.hqu.rendit.metaclass

import java.util.HashMap;
import java.util.Locale;

import org.hyperic.hq.product.MetricValue
import org.hyperic.hq.measurement.UnitsConvert
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl
import org.hyperic.hq.measurement.server.session.DerivedMeasurement
import org.hyperic.util.pager.PageControl


class MetricCategory {
    private static dataMan = DataManagerEJBImpl.one
    
    static String urlFor(DerivedMeasurement d, Map context) {
        def template = d.template
        if (context?.chart) {
            def units    = template.units
            def unitInt  = UnitsConvert.getUnitForUnit(units)
            def scale    = UnitsConvert.getScaleForUnit(units)
            def collType = template.collectionType
            def end      = context.get('end', System.currentTimeMillis())
            def start    = context.get('start', end - (8 * 60 * 60 * 1000))
            def showAvg  = context.get('showAverage', true)
            def showPeak = context.get('showPeak', true)
            def showLow  = context.get('showLow', true)
            def showEvents = context.get('showEvents', true)
            return "/resource/MetricChart" + 
                   "?unitUnits=$unitInt" + 
                   "&unitScale=$scale" +
                   "&showPeak=$showPeak" + 
                   "&showHighRange=true" + 
                   "&showValues=true" +
                   "&showAverage=$showAvg" +
                   "&showLowRange=true" + 
                   "&showLow=$showLow" +
                   "&collectionType=$collType" +
                   "&showEvents=$showEvents" + 
                   "&showBaseline=false" + 
                   "&baseline=" +
                   "&highRange=" + 
                   "&lowRange=" + 
                   "&start=$start" + 
                   "&end=$end" +
                   "&m=${d.template.id}" +
                   "&eid=${d.entityId}"
        }
        
        "/resource/common/monitor/Visibility.do?m=${template.id}&eid=${d.entityId}&mode=chartSingleMetricSingleResource"
    }
    
    
    /**
     * Get the last data point for a collection of DerivedMeasurements
     *
     * @param c          A collection of DerivedMeasurement
     * @param timeWindow Time (in millis) that the resultant metric values
     *                   must be greater than.  (i.e. only return metric values
     *                   > this time value)                   
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
        
        def vals = dataMan.getLastDataPoints(mids, timeWindow)
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
    static MetricValue getLastDataPoint(DerivedMeasurement m, long timeWindow) {
        [m].getLastDataPoints(timeWindow)[m]
    }
    
    /**
     * Get the last data point collected for a derived measurement.
     */
    static MetricValue getLastDataPoint(DerivedMeasurement m) {
        m.getLastDataPoint(0)
    }
    
    static List getData(DerivedMeasurement m, long start, long end) {
        dataMan.getHistoricalData(m.id, start, end, new PageControl()) 
    }
}
