package org.hyperic.hq.api.services;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.api.model.measurements.BulkResourceMeasurementRequest;
import org.hyperic.hq.api.model.measurements.MeasurementRequest;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.MetricNotifications;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementBatchResponse;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementRequests;
import org.hyperic.hq.api.model.measurements.MeasurementResponse;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;


@Path("/") 
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON}) 
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface MeasurementService {
    @GET
    @Path("/")
    public ResourceMeasurementBatchResponse getMeasurements() throws SessionNotFoundException, SessionTimeoutException;
}
