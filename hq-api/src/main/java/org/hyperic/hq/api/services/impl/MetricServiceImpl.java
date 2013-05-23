package org.hyperic.hq.api.services.impl;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionVisitor;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.common.ExternalRegistrationStatus;
import org.hyperic.hq.api.model.measurements.MeasurementAlias;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.MetricResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurement;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequest;
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

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;

public class MetricServiceImpl extends RestApiService implements MetricService {
    @Autowired
    protected MeasurementTransfer measurementTransfer;
    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler;
    @Autowired
    private EndpointQueue endpointQueue;
    @Context
    private SearchContext searchContext;
    
    protected String extractAliases(SearchCondition<MeasurementAlias> sc) {
        String ma = sc.getCondition().getMeasurementAlias();
        ConditionType ct = sc.getConditionType();
        if (!ct.equals(ConditionType.EQUALS)) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER_COMPARATOR, ct.toString(), ResourceMeasurement.MEASUREMENT_ALIAS, ma, ConditionType.EQUALS.toString());
        }
        return ma;
    }
    
    public MetricResponse getMetrics(final Date begin, final Date end) throws Throwable {
        try {
            try {
                SearchCondition<ResourceMeasurement> scRoot = this.searchContext.getCondition(ResourceMeasurement.class);
                if (scRoot==null) {
                    throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                            ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER, " (most likley that the supplied field name is not " + ResourceMeasurement.MEASUREMENT_ALIAS + ", the comparator type is unrecognized or that the value it is compared against is empty)\n Only the following filter form is acceptable: " + ResourceMeasurement.MEASUREMENT_ALIAS + "=<string>," + ResourceMeasurement.MEASUREMENT_ALIAS + "=<string>,...");
                }
                ResourceMeasurementFIQLVisitor visitor = new ResourceMeasurementFIQLVisitor();
                try {
                    scRoot.accept(visitor);
                } catch (IllegalFIQLStructure e) {
                    throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                            ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER, e.getMessage());
                }
                ResourceMeasurementRequests rs = visitor.getResourceMeasurementRequests();
                if (rs==null) {
                    throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                            ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER, "");
                }
                ResourceMeasurementRequest hqMsmtReq = rs.getMeasurementRequests().get(0);
                ApiMessageContext apiMessageContext = newApiMessageContext();
                return measurementTransfer.getMetrics(apiMessageContext, hqMsmtReq.getMeasurementTemplateNames(), hqMsmtReq.getRscId(), begin, end);
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
                        ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_AGG_FILTER, " (most likley that one of the supplied field names is not recognized, a needed field name is missing, the comparator type is unrecognized or that the value it is compared against is empty)");
            }
            ResourceMeasurementFIQLVisitor visitor = new ResourceMeasurementFIQLVisitor();
            try {
                scRoot.accept(visitor);
            } catch (IllegalFIQLStructure e) {
                throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                        ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_AGG_FILTER, e.getMessage());
            }
            ResourceMeasurementRequests hqMsmtReqs = visitor.getResourceMeasurementRequests();
            if (hqMsmtReqs==null) {
                throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                        ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER, "");
            }
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
        try {
            return measurementTransfer.register(apiMessageContext, request, apiMessageContext);
        } catch (PermissionException e) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.UNAUTHORIZED,
                    ExceptionToErrorCodeMapper.ErrorCode.NON_ADMIN_ERR, "");
        }
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
        } catch (PermissionException e) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.UNAUTHORIZED,
                    ExceptionToErrorCodeMapper.ErrorCode.NON_ADMIN_ERR, "");
        }
    }

    public void unregister(final String registrationId) throws SessionNotFoundException, SessionTimeoutException {
        NotificationEndpoint endpoint = endpointQueue.unregister(registrationId);
        if (endpoint == null) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID);
        }
        try {
            ApiMessageContext apiMessageContext = newApiMessageContext();
            measurementTransfer.unregister(apiMessageContext,endpoint);
        } catch (PermissionException e) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.UNAUTHORIZED,
                    ExceptionToErrorCodeMapper.ErrorCode.NON_ADMIN_ERR, "");
        }
    }
    
    protected static class ResourceMeasurementFIQLVisitor implements SearchConditionVisitor<ResourceMeasurement> {
        protected Stack<Object> s = new Stack<Object>();
        
        
        @SuppressWarnings("unchecked")
        public void visit(SearchCondition<ResourceMeasurement> sc) {
            if (sc==null) {
                throw new IllegalFIQLStructure("no filter supplied");
            } 
            ConditionType ct = sc.getConditionType();

            if (ct.equals(ConditionType.OR)) {
                this.s.add(ConditionType.OR);
                List<SearchCondition<ResourceMeasurement>> scChildren = sc.getSearchConditions();
                for(SearchCondition<ResourceMeasurement> scChild:scChildren) {
                    scChild.accept(this);
                }

                Object o = this.s.pop();
                List l = new ArrayList<Object>();
                boolean childrenRepresentsAlias = false;
                while (!ConditionType.OR.equals(o)) {
                    if (o instanceof String[]) {
                        if (!l.isEmpty() && !childrenRepresentsAlias) {
                            throw new IllegalFIQLStructure("the following filter structure form is illegal: "+
                                    ResourceMeasurement.RESOURCE_ID+"==<string>," + ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>");
                        }
                        String[] kv = (String[]) o;
                        if (ResourceMeasurement.MEASUREMENT_ALIAS.equals(kv[0])) {
                            l.add((String)kv[1]);
                        } else {
                            throw new IllegalFIQLStructure("in a filter, only '" + ResourceMeasurement.MEASUREMENT_ALIAS +
                                    "' fields and elements of the form '("+ ResourceMeasurement.RESOURCE_ID+"==<string>;(" 
                                    + ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>," + ResourceMeasurement.MEASUREMENT_ALIAS +
                                    "==<string>,..))' may be seperated with ','");
                        }
                        childrenRepresentsAlias=true;
                    } else if (o instanceof ResourceMeasurementRequest) {
                        if (!l.isEmpty() && childrenRepresentsAlias) {
                            throw new IllegalFIQLStructure("the following filter structure form is illegal: "+
                                    ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>," + ResourceMeasurement.RESOURCE_ID + "==<string>");
                        }
                        l.add(o);
                        childrenRepresentsAlias=false;
                    } else {
                        throw new IllegalFIQLStructure("the following element was found next to a ',': " + o);
                    }
                    o = this.s.pop();
                }
                if (childrenRepresentsAlias) {
                    this.s.add(l);
                } else {
                    ResourceMeasurementRequests rs = new ResourceMeasurementRequests();
                    rs.setMeasurementRequests(l);
                    this.s.add(rs);
                }
            } else if (ct.equals(ConditionType.AND)) {
                this.s.add(ConditionType.AND);
                List<SearchCondition<ResourceMeasurement>> scChildren = sc.getSearchConditions();
                for(SearchCondition<ResourceMeasurement> scChild:scChildren) {
                    scChild.accept(this);
                }
                Object o = this.s.pop();
                ResourceMeasurementRequest r = new ResourceMeasurementRequest();
                boolean listMet = false;
                while (!ConditionType.AND.equals(o)) {
                    if (o instanceof List) {
                        if (listMet) {
                            throw new IllegalFIQLStructure("the following filter structure form is illegal: "+
                                    ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>;" + ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>");
                        }
                        r.setMeasurementTemplateNames((List<String>) o);
                        listMet = true;
                    } else if (o instanceof String[]){
                        String[] kv = (String[]) o;
                        if (ResourceMeasurement.RESOURCE_ID.equals(kv[0])) {
                            r.setRscId((String)kv[1]);
                        } else if (ResourceMeasurement.MEASUREMENT_ALIAS.equals(kv[0])) {
                            if (listMet) {
                                throw new IllegalFIQLStructure("the following filter structure form is illegal: "+
                                        ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>;" + ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>");
                            }
                            List<String> aliases = new ArrayList<String>();
                            aliases.add((String) kv[1]);
                            r.setMeasurementTemplateNames(aliases);
                        } else {
                            throw new IllegalFIQLStructure("illegal property name. Only "+ ResourceMeasurement.RESOURCE_ID+" and " +
                                    ResourceMeasurement.MEASUREMENT_ALIAS + " are allowed");
                        }
                    } else {
                        throw new IllegalFIQLStructure("in a filter, ';' can only come between '"+ ResourceMeasurement.RESOURCE_ID+"==<string>' and "+
                                "'" + ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>', or between '"+ ResourceMeasurement.RESOURCE_ID+"==<string>' and "+
                                "'(" + ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>," + ResourceMeasurement.MEASUREMENT_ALIAS + "==<string>,..)'");
                    }
                    o = this.s.pop();
                }
                this.s.add(r);
            } else if (ct.equals(ConditionType.EQUALS)) {
                PrimitiveStatement ps = sc.getStatement();
                String[] kv = new String[2];
                kv[0] = ps.getProperty();
                kv[1] = (String) ps.getValue();
                this.s.add(kv);
            } else {
                throw new IllegalFIQLStructure("unexpected filter element " + ct);
            }
        }
        public ResourceMeasurementRequests getResourceMeasurementRequests() {
            Object o = this.s.pop();
            if (o instanceof ResourceMeasurementRequests) {
                return (ResourceMeasurementRequests) o; 
            }
            if (o instanceof ResourceMeasurementRequest) {
                ResourceMeasurementRequests rs = new ResourceMeasurementRequests();
                rs.addMeasurementRequests((ResourceMeasurementRequest) o);
                return rs;
            }
            return null;
        }
        public String getResult() {
            return null;
        }
    }
    
    protected static class IllegalFIQLStructure extends RuntimeException {
        private static final long serialVersionUID = -2683108375142690198L;
        public IllegalFIQLStructure(String msg) {
            super(msg);
        }
    }
}