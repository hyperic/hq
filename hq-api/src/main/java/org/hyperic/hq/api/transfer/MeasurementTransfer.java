package org.hyperic.hq.api.transfer;

import java.text.ParseException;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
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
    public MeasurementResponse getMetrics(ApiMessageContext apiMessageContext, final MeasurementRequest measurementRequest,
			final String rscId, final String begin, final String end) throws ParseException, PermissionException;
    
    public ResourceMeasurementBatchResponse getAggregatedMetricData(ApiMessageContext apiMessageContext, final ResourceMeasurementRequests hqMsmtReqs, 
            final String begin, final String end) 
            throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException;
}
