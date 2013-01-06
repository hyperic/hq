package org.hyperic.hq.api.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.hyperic.hq.api.model.measurements.BulkResourceMeasurementRequest;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;

@Path("/") 
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON}) 
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface MeasurementService {
    @POST
    @Path("/")
    public ResourceMeasurementBatchResponse getMeasurements(BulkResourceMeasurementRequest msmtMetaReq) throws SessionNotFoundException, SessionTimeoutException;
}