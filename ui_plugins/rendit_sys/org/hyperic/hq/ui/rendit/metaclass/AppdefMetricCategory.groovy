package org.hyperic.hq.ui.rendit.metaclass

import org.hyperic.hq.appdef.shared.PlatformValue
import org.hyperic.hq.appdef.shared.AppdefResourceValue
import org.hyperic.hq.ui.rendit.helpers.MetricHelper
import org.hyperic.hq.appdef.shared.AppdefEntityID

import groovy.lang.DelegatingMetaClass

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
}
