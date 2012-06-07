package org.hyperic.hq.api.services.impl;

import javax.ws.rs.QueryParam;

import org.hyperic.hq.api.model.measurements.ResourceMeasurementsRequestsCollection;
import org.hyperic.hq.api.model.measurements.ResourcesMeasurementsBatchResponse;
import org.hyperic.hq.api.services.MeasurementService;
import org.apache.commons.logging.Log;

public class MeasurementServiceImpl implements MeasurementService {
    @Autowired
    private MeasurementTransfer measurementTransfer;
    
    @Autowired
    @Qualifier("restApiLogger")
    private Log logger = Log;    
//    public Log getLogger() {
//        return logger;
//    }    
//    public void setLogger(Log logger) {
//        this.logger = logger;
//    }        
    
	public ResourcesMeasurementsBatchResponse getMetrics(final ResourceMeasurementsRequestsCollection resourceMeasurementsRequestsCollection/*,
			final Date samplingStartTime, final Date samplingEndTime*/) {
        return measurementTransfer.getMetrics(discoveryId, type);
    }
}
