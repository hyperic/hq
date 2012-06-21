package org.hyperic.hq.api.transfer;

import java.util.Calendar;

import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;

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
    public MeasurementResponse getMetrics(final MeasurementRequest measurementRequest,
			final Calendar begin, final Calendar end);
}
