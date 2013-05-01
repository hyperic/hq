package org.hyperic.hq.api.transfer;
 
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.common.ExternalRegistrationStatus;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.MetricResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.mapping.UnknownEndpointException;
import org.hyperic.hq.api.model.measurements.BulkResourceMeasurementRequest;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;
import org.hyperic.hq.notifications.NotificationEndpoint;

/**
 * 
 * @author yakarn
 *
 */
public interface MeasurementTransfer {
	/**
	 * 
	 * @param measurementRequest	measurement requests which are null or have no resource IDs will be ignored
	 * 								no measurements will be calculated for measurementTemplates which are unknown in the system
	 * 								
	 * @return
	 * @throws ParseException 
	 * @throws PermissionException 
	 * @throws TimeframeBoundriesException 
	 * @throws ObjectNotFoundException 
	 * @throws UnsupportedOperationException 
	 * @throws TimeframeSizeException 
	 * @throws IllegalArgumentException 
	 */
    MetricResponse getMetrics(ApiMessageContext apiMessageContext, final List<String> templateNames,
            final String rscId, final Date begin, final Date end) throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException, TimeframeBoundriesException, TimeframeSizeException;
    
    RegistrationID register(final ApiMessageContext messageContext, final MetricFilterRequest metricFilterReq, ApiMessageContext apiMessageContext) throws PermissionException;

    ExternalRegistrationStatus getRegistrationStatus(final ApiMessageContext messageContext,
                                             final String registrationID) throws PermissionException,NotFoundException, UnknownEndpointException;

    ResourceMeasurementBatchResponse getAggregatedMetricData(ApiMessageContext apiMessageContext, final ResourceMeasurementRequests hqMsmtReqs,
            final Date begin, final Date end) 
            throws PermissionException, UnsupportedOperationException, ObjectNotFoundException, TimeframeBoundriesException, SQLException;

    ResourceMeasurementBatchResponse getMeasurements(ApiMessageContext apiMessageContext,List<ID> ids);

    void unregister(final ApiMessageContext messageContext,NotificationEndpoint endpoint) throws PermissionException;

}
