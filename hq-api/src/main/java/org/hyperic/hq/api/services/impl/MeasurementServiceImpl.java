package org.hyperic.hq.api.services.impl;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.hyperic.hq.api.services.MeasurementService;
import org.hyperic.hq.api.services.impl.MetricServiceImpl.IllegalFIQLStructure;
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
import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.measurements.BulkResourceMeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementAlias;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

public class MeasurementServiceImpl extends RestApiService implements MeasurementService {
    @Autowired
    protected MeasurementTransfer measurementTransfer;
    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler ; 
    @Context
    private SearchContext searchContext;

    public ResourceMeasurementBatchResponse getMeasurements() throws SessionNotFoundException, SessionTimeoutException {
        SearchCondition<ID> scRoot = this.searchContext.getCondition(ID.class);
        if (scRoot==null) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER, " (most likley that one of the supplied field names is not recognized, a needed field name is missing, the comparator type is unrecognized or that the value it is compared against is empty)");
        }
        if (!(ConditionType.OR.equals(scRoot.getConditionType()) || ConditionType.EQUALS.equals(scRoot.getConditionType()))) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER, " (only the following filter form is acceptable: id==<number>,id==<number>,...)");
        }
        List<ID> rscIDs = new ArrayList<ID>();
        List<SearchCondition<ID>> scs = scRoot.getSearchConditions();
        if (scs==null) {  // a list with only one element has been supplied
            ID id = scRoot.getCondition();
            ConditionType ct = scRoot.getConditionType();
            if (!ct.equals(ConditionType.EQUALS)) {
                throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                        ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER_COMPARATOR, ct.toString(), "id", id, ConditionType.EQUALS.toString());
            }
            rscIDs.add(id);
        } else {
            for(SearchCondition<ID> sc:scs) {
                ID id = sc.getCondition();
                ConditionType ct = sc.getConditionType();
                if (!ct.equals(ConditionType.EQUALS)) {
                    throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.BAD_REQUEST,
                            ExceptionToErrorCodeMapper.ErrorCode.ILLEGAL_FILTER_COMPARATOR, ct.toString(), "id", id, ConditionType.EQUALS.toString());
                }
                rscIDs.add(id);
            }
        }
        ApiMessageContext apiMessageContext = newApiMessageContext();
        return this.measurementTransfer.getMeasurements(apiMessageContext,rscIDs);
    }
}
