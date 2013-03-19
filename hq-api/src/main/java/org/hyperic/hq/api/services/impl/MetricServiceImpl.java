package org.hyperic.hq.api.services.impl;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.common.ExternalRegistrationStatus;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.MetricResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.services.MetricService;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.UnknownEndpointException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.TimeframeBoundriesException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.TimeframeSizeException;
import org.hyperic.hq.notifications.EndpointQueue;
import org.hyperic.hq.notifications.NotificationEndpoint;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

public class MetricServiceImpl extends RestApiService implements MetricService {
    @Autowired
    protected MeasurementTransfer measurementTransfer;
    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler;
    @Autowired
    private EndpointQueue endpointQueue;

    public MetricResponse getMetrics(final MeasurementRequest measurementRequest, final String resourceId,
                                     final Date begin, final Date end) throws Throwable {
        try {
            try {
                ApiMessageContext apiMessageContext = newApiMessageContext();
                return measurementTransfer.getMetrics(apiMessageContext, measurementRequest, resourceId, begin, end);
            } catch (Throwable t) {
                errorHandler.log(t);
                throw t;
            }
        } catch (TimeframeSizeException e) {
            throw errorHandler.newWebApplicationException(e, Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.WRONG_DATE_VALUES, e.getMessage());
        } catch (TimeframeBoundriesException e) {
            throw errorHandler.newWebApplicationException(e, Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.WRONG_DATE_VALUES, e.getMessage());
        } catch (UnsupportedOperationException e) {
            throw errorHandler.newWebApplicationException(e, Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.BAD_MEASUREMENT_REQ, e.getMessage());
        } catch (ObjectNotFoundException e) {
            String missingObj = e.getEntityName();
            if (MeasurementTemplate.class.getName().equals(missingObj)) {
                throw errorHandler.newWebApplicationException(e, Response.Status.NOT_FOUND,
                        ExceptionToErrorCodeMapper.ErrorCode.TEMPLATE_NOT_FOUND);
            }
            if (Measurement.class.getName().equals(missingObj)) {
                throw errorHandler.newWebApplicationException(e, Response.Status.NOT_FOUND,
                        ExceptionToErrorCodeMapper.ErrorCode.MEASUREMENT_NOT_FOUND);
            }
            throw errorHandler.newWebApplicationException(e, Response.Status.NOT_FOUND,
                    ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID);
        }
    }

    public ResourceMeasurementBatchResponse getAggregatedMetricData(ResourceMeasurementRequests hqMsmtReqs,
                                                                    Date begin, Date end) throws ParseException,
            PermissionException, SessionNotFoundException, SessionTimeoutException, ObjectNotFoundException,
            UnsupportedOperationException, SQLException {
        try {
            ApiMessageContext apiMessageContext = newApiMessageContext();
            return measurementTransfer.getAggregatedMetricData(apiMessageContext, hqMsmtReqs, begin, end);
        } catch (TimeframeBoundriesException e) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.WRONG_DATE_VALUES, e.getMessage());
        }
    }

    public RegistrationID register(final MetricFilterRequest request) throws SessionNotFoundException,
            SessionTimeoutException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        return measurementTransfer.register(request, apiMessageContext);
    }

    public ExternalRegistrationStatus getRegistrationStatus(final int registrationID) throws
            SessionNotFoundException, SessionTimeoutException, PermissionException, NotFoundException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        try {
            return measurementTransfer.getRegistrationStatus(apiMessageContext, registrationID);
        }catch(UnknownEndpointException e) {
            e.printStackTrace();
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.INTERNAL_SERVER_ERROR,
                    ExceptionToErrorCodeMapper.ErrorCode.UNKNOWN_ENDPOINT, e.getRegistrationID());
        }
    }

    public void unregister(final long registrationId) throws SessionNotFoundException, SessionTimeoutException {
        NotificationEndpoint endpoint = endpointQueue.unregister(registrationId);
        if (endpoint == null) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID);
        }
        measurementTransfer.unregister(endpoint);
    }
}