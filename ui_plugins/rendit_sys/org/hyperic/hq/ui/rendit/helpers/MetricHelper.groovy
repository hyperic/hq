package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.util.pager.PageControl
import org.hyperic.hq.product.MetricValue
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl
import org.hyperic.hq.appdef.shared.AppdefEntityID

class MetricHelper 
    extends BaseHelper
{
    MetricHelper(user) {
        super(user)
    }

    private getDerivedMan() { DerivedMeasurementManagerEJBImpl.one }
    private getDataMan()    { DataManagerEJBImpl.one }

    /**
     * Returns a map of metric aliases onto the DerivedMeasurementValue
     * objects which are enabled for the specified resource
     */
    Map getEnabledMetrics(AppdefEntityID id) {
        def res = [:]    
        def metrics = derivedMan.findMeasurements(userVal, id, true, null, 
                                                  PageControl.PAGE_ALL)
                                                  
        for (m in metrics) {
			res[m.template.alias] = m            
        }
        res
    }

    MetricValue getLastDataPoint(DerivedMeasurementValue metric) {
        def timeStamp = System.currentTimeMillis() - (metric.interval * 3)
        def res = dataMan.getLastDataPoints(metric.id as Integer[],
                                            timeStamp)
        if (res.size() == 0)
            return null
		
        res[metric.id]
    }
}
