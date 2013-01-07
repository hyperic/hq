package org.hyperic.hq.api.services.impl;

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
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

public class MeasurementServiceImpl extends RestApiService implements MeasurementService {
    @Autowired
    MeasurementTransfer measurementTransfer;
    @Autowired
    private ExceptionToErrorCodeMapper errorHandler ; 
    
    public ResourceMeasurementBatchResponse getMeasurements(BulkResourceMeasurementRequest msmtMetaReq) throws SessionNotFoundException, SessionTimeoutException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        return this.measurementTransfer.getMeasurements(apiMessageContext,msmtMetaReq);
    }
}
