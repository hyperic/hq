package org.hyperic.hq.api.services.impl;

import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.services.MeasurementService;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.hyperic.hq.api.model.measurements.BulkResourceMeasurementRequest;


public class MeasurementServiceImpl extends RestApiService implements MeasurementService {
    @Autowired
    private MeasurementTransfer measurementTransfer;
    public ResourceMeasurementBatchResponse getMeasurements(BulkResourceMeasurementRequest msmtMetaReq) throws SessionNotFoundException, SessionTimeoutException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        return this.measurementTransfer.getMeasurements(apiMessageContext,msmtMetaReq);
    }
}
