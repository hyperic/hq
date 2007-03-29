package org.hyperic.hq.ui.rendit.metaclass

import org.hyperic.hq.appdef.shared.AppdefResourceValue
import org.hyperic.hq.ui.rendit.helpers.MetricHelper
import org.hyperic.hq.appdef.shared.AppdefEntityID

/**
 * This category adds measurement methods to appdef types
 */
class AppdefMetricCategory {
    /**
     * AppdefEntityID.enabledMetrics
     * See also:  MetricHelper.enabledMetrics
     */
    static Map getEnabledMetrics(AppdefEntityID id) {
        (new MetricHelper(CategoryInfo.user)).getEnabledMetrics(id)
    }

    static Map getEnabledMetrics(AppdefResourceValue resource) {
		getEnabledMetrics(resource.entityId)
    }
}
