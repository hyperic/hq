package org.hyperic.hq.api.services;

import java.util.Calendar;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;


@Path("/") 
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON}) 
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface MeasurementService {
	
	@POST
	@Path("/metrics")
	MeasurementResponse getMetrics(@QueryParam("MeasurementRequest") final MeasurementRequest measurementRequest,
			@QueryParam("begin") final Calendar begin,
			@QueryParam("end") final Calendar end);
}