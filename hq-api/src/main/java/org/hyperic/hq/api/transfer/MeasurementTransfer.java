package org.hyperic.hq.api.transfer;

import org.hyperic.hq.api.model.measurements.ResourceMeasurementsRequestsCollection;
import org.hyperic.hq.api.model.measurements.ResourcesMeasurementsBatchResponse;

/**
 * 
 * @author yakarn
 *
 */
public interface MeasurementTransfer {
	/**
	 * 
	 * @param measurementsRequests	measurementsRequests which are null or have no resource IDs will be ignored
	 * 								no measurements will be calculated for measurementTemplates which are unknown in the system
	 * 								
	 * @return
	 */
    public ResourcesMeasurementsBatchResponse getMetrics(final ResourceMeasurementsRequestsCollection resourceMeasurementsRequestsCollection/*,
			final Date samplingStartTime, final Date samplingEndTime*/);
}
