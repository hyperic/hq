package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.product.MetricValue
import org.hyperic.hq.measurement.UnitsConvert
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.measurement.server.session.Measurement
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.util.pager.PageControl
import org.hyperic.hq.measurement.server.session.MeasurementTemplate
import org.hyperic.util.units.UnitNumber
import org.hyperic.util.units.UnitsFormat
import org.hyperic.hq.authz.server.session.AuthzSubject

class MetricCategory {
    private static dataMan = Bootstrap.getBean(DataManager.class)
    private static tmplMan = Bootstrap.getBean(TemplateManager.class)
    private static measMan = Bootstrap.getBean(MeasurementManager.class)

    static String urlFor(Measurement d, Map context) {
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
        def idToMetric = [:]
        
        int i = 0;
        for (Iterator it = c.iterator(); it.hasNext(); i++) {
            def m = it.next();
            idToMetric[m.id] = m
        }
        
        def vals = dataMan.getLastDataPoints(c, timeWindow)
        def res  = [:]
        c.each { m ->
            res[m] = vals[m.id]
        }
        res
    }
    
    /**
     * Get the last data point for a measurement.
     *
     * @param timeWindow The minimum timestamp (in millis) that the result 
     *                   will have.
     */
    static MetricValue getLastDataPoint(Measurement m, long timeWindow) {
        [m].getLastDataPoints(timeWindow)[m]
    }
    
    /**
     * Get the last data point collected for a measurement.
     */
    static MetricValue getLastDataPoint(Measurement m) {
        m.getLastDataPoint(-1)
    }

    /**
     * Get the measurement data for the given measurement and range.
     */
    static List getData(Measurement m, long start, long end) {
        boolean prependAvailUnknowns = false;
        dataMan.getHistoricalData(m, start, end, new PageControl(),
                                  prependAvailUnknowns)
    }
    
    /**
     * Use the 'units' specified by the measurement template to get a 
     * UnitNumber with the passed value.
     */
    static UnitNumber getUnitOf(MeasurementTemplate t, double value) {
        def units = UnitsConvert.getUnitForUnit(t.units)
        def scale = UnitsConvert.getScaleForUnit(t.units)
        new UnitNumber(value, units, scale)
    }

    /**
     * Format a data point using the template's units.
     */
    static String renderWithUnits(MeasurementTemplate t, double value) {
        UnitsFormat.format(getUnitOf(t, value)).toString()
    }

    /**
     * Set the default interval for a template.
     */
    static void setDefaultInterval(MeasurementTemplate t, AuthzSubject user,
                                   long interval) {
        tmplMan.updateTemplateDefaultInterval(user, [t.getId()] as Integer[],
                                              interval)
    }

    /**
     * Set the indicator flag for a template.
     */
    static void setDefaultIndicator(MeasurementTemplate t, AuthzSubject user,
                                    boolean on) {
        tmplMan.setDesignated(t, on);
    }
    
    /**
     * Set the default on flag for a template.
     */
    static void setDefaultOn(MeasurementTemplate t, AuthzSubject user,
                             boolean on) {
        tmplMan.setTemplateEnabledByDefault(user, [t.getId()] as Integer[], on)
    }

    /**
     * Disable the specified measurement.
     */
    static void disableMeasurement(Measurement m, AuthzSubject user) {
        measMan.disableMeasurements(user, m.getEntityId(), [m.getTemplate().getId()] as Integer[])
    }

    /**
     * Enable the specified measurement.
     */
    static void enableMeasurement(Measurement m, AuthzSubject user,
                                  long interval) {
        measMan.enableMeasurement(user, m.getId(), interval)
    }

    /**
     * Update the specified measurement interval.
     */
    static void updateMeasurementInterval(Measurement m, AuthzSubject user,
                                          long interval) {
        measMan.updateMeasurementInterval(user, m.getId(), interval)
    }
}
