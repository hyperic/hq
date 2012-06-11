package org.hyperic.hq.api.transfer;

import org.hyperic.hq.api.model.measurements.ResourceMeasurementsRequestsCollection;
import org.hyperic.hq.api.model.measurements.ResourcesMeasurementsBatchResponse;

/**
 * 
 * @author yakarn
 *
 */
public interface MeasurementTransfer {
    public ResourcesMeasurementsBatchResponse getMetrics(final ResourceMeasurementsRequestsCollection resourceMeasurementsRequestsCollection/*,
			final Date samplingStartTime, final Date samplingEndTime*/);
}
