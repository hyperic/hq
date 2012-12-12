package org.hyperic.hq.api.transfer;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.model.measurements.BulkMeasurementMetaDataRequest;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;
import org.hyperic.hq.notifications.filtering.IMetricFilter;
import org.hyperic.hq.notifications.filtering.IMetricFilterByResource;

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
	 * @throws ObjectNotFoundException 
	 * @throws UnsupportedOperationException 
	 * @throws TimeframeSizeException 
	 * @throws IllegalArgumentException 
	 */
    public MeasurementResponse getMetrics(ApiMessageContext apiMessageContext, final MeasurementRequest measurementRequest,
            final String rscId, final Date begin, final Date end) throws ParseException, PermissionException, UnsupportedOperationException, ObjectNotFoundException, TimeframeBoundriesException, TimeframeSizeException;
    
    public void register(Integer sessionId, IMetricFilterByResource rscFilter, IMetricFilter metricFilter);

    public void unregister(Integer sessionId, IMetricFilterByResource rscFilter, IMetricFilter metricFilter);

    public ResourceMeasurementBatchResponse poll(Integer sessionId);
    
    public ResourceMeasurementBatchResponse getAggregatedMetricData(ApiMessageContext apiMessageContext, final ResourceMeasurementRequests hqMsmtReqs, 
            final Date begin, final Date end) 
            throws PermissionException, UnsupportedOperationException, ObjectNotFoundException, TimeframeBoundriesException, SQLException;
    public MeasurementResponse getMeasurementMetaData(BulkMeasurementMetaDataRequest msmtMetaReq);

}
