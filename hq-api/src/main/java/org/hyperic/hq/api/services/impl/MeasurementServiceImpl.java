/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.api.services.impl;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.MetricNotifications;
import org.hyperic.hq.api.model.measurements.RawMetric;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.services.MeasurementService;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.hyperic.hq.api.model.measurements.BulkResourceMeasurementRequest;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

public class MeasurementServiceImpl extends RestApiService implements MeasurementService {
    @Autowired
    private MeasurementTransfer measurementTransfer;
    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler ; 
    
    public void register(final MetricFilterRequest metricFilterReq) throws SessionNotFoundException, SessionTimeoutException {
//        ApiMessageContext apiMessageContext = newApiMessageContext();
//        Integer sessionId = apiMessageContext.getSessionId();
//        SearchCondition<RawMetric> a = searchContext.getCondition(org.hyperic.hq.api.model.measurements.RawMetric.class);
        measurementTransfer.register(/*sessionId, */metricFilterReq);
    }
    public void unregister() throws SessionNotFoundException, SessionTimeoutException {
//        ApiMessageContext apiMessageContext = newApiMessageContext();
//        Integer sessionId = apiMessageContext.getSessionId();
        measurementTransfer.unregister(/*sessionId*/);
    }
    public void unregister(final MetricFilterRequest metricFilterReq) throws SessionNotFoundException, SessionTimeoutException {
//        ApiMessageContext apiMessageContext = newApiMessageContext();
//        Integer sessionId = apiMessageContext.getSessionId();
        measurementTransfer.unregister(/*sessionId,*/metricFilterReq);
    }
    public MetricNotifications poll() throws SessionNotFoundException, SessionTimeoutException {
//        ApiMessageContext apiMessageContext = newApiMessageContext();
//        Integer sessionId = apiMessageContext.getSessionId();
        return measurementTransfer.poll(/*sessionId*/);
    }

    public MeasurementResponse getMetrics(final MeasurementRequest measurementRequest,
			final String rscId, final Date begin, final Date end)
			        throws Throwable {
	    try {
	        try {
	            ApiMessageContext apiMessageContext = newApiMessageContext();
	            return measurementTransfer.getMetrics(apiMessageContext, measurementRequest, rscId, begin, end);
	        } catch (Throwable t) {
	            errorHandler.log(t);
	            throw t;
	        }
        } catch (TimeframeSizeException e) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.WRONG_DATE_VALUES, e.getMessage());
        } catch (TimeframeBoundriesException e) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.WRONG_DATE_VALUES, e.getMessage());
        } catch (UnsupportedOperationException e) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.BAD_MEASUREMENT_REQ, e.getMessage());
        } catch (ObjectNotFoundException e) {
            String missingObj = e.getEntityName();
            if (MeasurementTemplate.class.getName().equals(missingObj)) {
                throw errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.TEMPLATE_NOT_FOUND);
            }
            if (Measurement.class.getName().equals(missingObj)) {
                throw errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.MEASUREMENT_NOT_FOUND);
            }
            throw errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID);
        }
    }

    public ResourceMeasurementBatchResponse getAggregatedMetricData(ResourceMeasurementRequests hqMsmtReqs, Date begin, Date end)
            throws ParseException, PermissionException, SessionNotFoundException, SessionTimeoutException, ObjectNotFoundException, UnsupportedOperationException, SQLException {
        try {
            ApiMessageContext apiMessageContext = newApiMessageContext();
            return measurementTransfer.getAggregatedMetricData(apiMessageContext, hqMsmtReqs, begin, end);
        } catch (TimeframeBoundriesException e) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.WRONG_DATE_VALUES, e.getMessage());
        }
    }
    
    public ResourceMeasurementBatchResponse getMeasurements(BulkResourceMeasurementRequest msmtMetaReq) throws SessionNotFoundException, SessionTimeoutException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        return this.measurementTransfer.getMeasurements(apiMessageContext,msmtMetaReq);
    }
}
