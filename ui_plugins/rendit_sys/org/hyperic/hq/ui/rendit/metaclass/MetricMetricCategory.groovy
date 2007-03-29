package org.hyperic.hq.ui.rendit.metaclass

import org.hyperic.hq.product.MetricValue
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue
import org.hyperic.hq.ui.rendit.helpers.MetricHelper

class MetricMetricCategory {
    static MetricValue getLastDataPoint(DerivedMeasurementValue metric) {
        (new MetricHelper(CategoryInfo.user)).getLastDataPoint(metric)
    }
}
