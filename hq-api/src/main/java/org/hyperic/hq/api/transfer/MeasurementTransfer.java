package org.hyperic.hq.api.transfer;

import java.text.ParseException;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;

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
	 * @throws TimeframeBoundriesException 
	 * @throws TimeframeSizeException 
	 * @throws IllegalArgumentException 
	 * @throws ObjectNotFoundException 
	 * @throws UnsupportedOperationException 
	 */
    public MeasurementResponse getMetrics(ApiMessageContext apiMessageContext, final MeasurementRequest measurementRequest,
			final String rscId, final String begin, final String end) throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException, IllegalArgumentException, TimeframeSizeException, TimeframeBoundriesException;
}
