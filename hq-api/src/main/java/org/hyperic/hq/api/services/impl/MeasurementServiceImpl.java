package org.hyperic.hq.api.services.impl;

import java.util.Calendar;

import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.api.services.MeasurementService;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.springframework.beans.factory.annotation.Autowired;

public class MeasurementServiceImpl implements MeasurementService {
    @Autowired
    private MeasurementTransfer measurementTransfer;
    
//    @Autowired
//    @Qualifier("restApiLogger")
//    private Log logger = Log;    
//    public Log getLogger() {
//        return logger;
//    }    
//    public void setLogger(Log logger) {
//        this.logger = logger;
//    }        
    
	public MeasurementResponse getMetrics(final MeasurementRequest measurementRequest,
			final Calendar begin, final Calendar end) {
        return measurementTransfer.getMetrics(measurementRequest,begin,end);
    }
}
