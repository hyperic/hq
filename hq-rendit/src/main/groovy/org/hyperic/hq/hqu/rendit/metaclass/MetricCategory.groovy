/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.bizapp.shared.MeasurementBoss
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary
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

class MetricCategory {
    private static dataMan = Bootstrap.getBean(DataManager.class)
    private static tmplMan = Bootstrap.getBean(TemplateManager.class)
    private static measMan = Bootstrap.getBean(MeasurementManager.class)
    private static measBoss = Bootstrap.getBean(MeasurementBoss.class)

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
    static Map getLastDataPoints(Collection<Measurement> c, long timeWindow) {
        def idToMetric = [:]
        
        int i = 0;
        for (Iterator it = c.iterator(); it.hasNext(); i++) {
            def m = it.next();
            idToMetric[m.id] = m
        }
        
        def vals = dataMan.getLastDataPoints(c.collect { m -> m.id }, timeWindow)
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
    
    static MetricDisplaySummary getSummary(Measurement m, AuthzSubject user, long start, long end) {
       def mgr = SessionManager.instance
       def sessionId = mgr.put(user)

    	measBoss.findMetric(sessionId, [m.resource.entityId], m.template.id, start, end)
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
