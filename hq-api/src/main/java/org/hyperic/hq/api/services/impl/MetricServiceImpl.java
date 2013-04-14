package org.hyperic.hq.api.services.impl;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.common.ExternalRegistrationStatus;
import org.hyperic.hq.api.model.measurements.MeasurementAlias;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.MetricResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurement;
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

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MetricServiceImpl extends RestApiService implements MetricService {
    public static class A implements Serializable {
        private static final long serialVersionUID = -3952973670560385315L;
        public int a;
        public int b;
        public int getA() {
            return a;
        }
        public void setA(int a) {
            this.a = a;
        }
        public int getB() {
            return b;
        }
        public void setB(int b) {
            this.b = b;
        }
    }
    @Autowired
    protected MeasurementTransfer measurementTransfer;
    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler;
    @Autowired
    private EndpointQueue endpointQueue;
    @Context
    private SearchContext searchContext;
    
    public void f() {
        SearchCondition<A> sc = this.searchContext.getCondition(MetricServiceImpl.A.class);
        System.out.println(sc);

    }
    
    protected String extractAliases(SearchCondition<MeasurementAlias> sc) {
        String ma = sc.getCondition().getMeasurementAlias();
        ConditionType ct = sc.getConditionType();
        if (!ct.equals(ConditionType.EQUALS)) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER_COMPARATOR, ct.toString(), "measurementalias", ma, ConditionType.EQUALS.toString());
        }
        return ma;
    }
    
    public MetricResponse getMetrics(final String resourceId,
                                     final Date begin, final Date end) throws Throwable {
        try {
            try {
                SearchCondition<MeasurementAlias> scRoot = this.searchContext.getCondition(MeasurementAlias.class);
                if (scRoot==null) {
                    throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                            ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER, " (most likley that the supplied field name is not measurementalias, the comparator tyepe is unrecognized or that the value it is compared against is empty)");
                }
                List<String> templateNames = new ArrayList<String>();
                List<SearchCondition<MeasurementAlias>> scs = scRoot.getSearchConditions();
                if (scs==null) {  // a list with only one element has been supplied
                    templateNames.add(extractAliases(scRoot));
                } else {
                    for(SearchCondition<MeasurementAlias> sc:scs) {
                        templateNames.add(extractAliases(sc));
                    }
                }
                ApiMessageContext apiMessageContext = newApiMessageContext();
                return measurementTransfer.getMetrics(apiMessageContext, templateNames, resourceId, begin, end);
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

    public ResourceMeasurementBatchResponse getAggregatedMetricData(Date begin, Date end) throws ParseException,
            PermissionException, SessionNotFoundException, SessionTimeoutException, ObjectNotFoundException,
            UnsupportedOperationException, SQLException {
        try {
            SearchCondition<ResourceMeasurement> scRoot = this.searchContext.getCondition(ResourceMeasurement.class);
            if (scRoot==null) {
                throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                        ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER, "");
            }
            create a visitor which returns a flat list of lists when it gets the tree:    (ResourceMeasurement.rscID0 & (ResourceMeasurement.alias0 | ResourceMeasurement.alias1 | ResourceMeasurement.alias2 | ...)) | (ResourceMeasurement.rscID1 & ...)
            ApiMessageContext apiMessageContext = newApiMessageContext();
            //XXX  
//            return measurementTransfer.getAggregatedMetricData(apiMessageContext, hqMsmtReqs, begin, end);
            return measurementTransfer.getAggregatedMetricData(apiMessageContext, null, begin, end);
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

    public ExternalRegistrationStatus getRegistrationStatus(final String registrationID) throws
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

    public void unregister(final String registrationId) throws SessionNotFoundException, SessionTimeoutException {
        NotificationEndpoint endpoint = endpointQueue.unregister(registrationId);
        if (endpoint == null) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID);
        }
        measurementTransfer.unregister(endpoint);
    }
}