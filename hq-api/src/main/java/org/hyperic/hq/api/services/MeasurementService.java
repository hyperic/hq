package org.hyperic.hq.api.services;

import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.api.model.measurements.ResourceMeasurementsRequestsCollection;
import org.hyperic.hq.api.model.measurements.ResourcesMeasurementsBatchResponse;


@Path("/") 
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON}) 
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface MeasurementService {
	
	@POST
	@Path("/metrics")
	ResourcesMeasurementsBatchResponse getMetrics(@QueryParam("resourceMeasurementsRequestsCollection") final ResourceMeasurementsRequestsCollection resourceMeasurementsRequestsCollection/*,
			@QueryParam("samplingStartTime") final Date samplingStartTime,
			@QueryParam("samplingEndTime") final Date samplingEndTime*/);
}