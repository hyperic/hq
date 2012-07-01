package org.hyperic.hq.api.transfer;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.authz.shared.PermissionException;

/**
 * 
 * @author yakarn
 *
 */
public interface MeasurementTransfer {
	/**
	 * 
	 * @param measurementsRequests	measurement requests which are null or have no resource IDs will be ignored
	 * 								no measurements will be calculated for measurementTemplates which are unknown in the system
	 * 								
	 * @return
	 * @throws ParseException 
	 * @throws PermissionException 
	 */
    public MeasurementResponse getMetrics(final MeasurementRequest measurementRequest,
			final String begin, final String end) throws ParseException, PermissionException;
}
