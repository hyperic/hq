package org.hyperic.hq.api.transfer;

import java.util.Date;

import org.hyperic.hq.api.model.measurements.ResourceMeasurementsRequestsCollection;
import org.hyperic.hq.api.model.measurements.MeasurementsResponse;

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
    public MeasurementsResponse getMetrics(final ResourceMeasurementsRequestsCollection resourceMeasurementsRequestsCollection,
			final Date begin, final Date end);
}
