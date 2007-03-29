package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.util.pager.PageControl
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl
import org.hyperic.hq.appdef.shared.AppdefEntityID

class MetricHelper 
    extends BaseHelper
{
    MetricHelper(user) {
        super(user)
    }

    private getDerivedMan() { DerivedMeasurementManagerEJBImpl.one }

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
}
