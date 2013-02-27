/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2013], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.api.services;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

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
	
	@POST
	@Path("/metrics/{rscId}")
    @Descriptions({ 
        @Description(value = "Get metric data/availability for measurements for a given time frame", target = DocTarget.METHOD),
        @Description(value = "Requested metric data per measuremet", target = DocTarget.RETURN),
        @Description(value = "ID of the Resource over which the measurements which are requested on the message body are defined, and the time frame for which the metric data is requested", target = DocTarget.REQUEST),
        @Description(value = "Requested metric data per measuremet per resource", target = DocTarget.RESPONSE),
        @Description(value = "Metric data", target = DocTarget.RESOURCE)
     })
	MeasurementResponse getMetrics(final MeasurementRequest measurementRequest,
	        @PathParam("rscId") final String rscId,
	        @QueryParam("begin") final Date begin,
			@QueryParam("end") final Date end) 
			        throws PermissionException, SessionNotFoundException, SessionTimeoutException, Throwable;
	
	@GET
    @Path("/metrics/poll")
	public MetricNotifications poll() throws SessionNotFoundException, SessionTimeoutException;
	
	@POST
    @Path("/metrics/aggregation")
    @Descriptions({ 
        @Description(value = "Get an aggregation of the metric data for measurements per resources for a given time frame", target = DocTarget.METHOD),
        @Description(value = "Aggregation of the requested metric data per measuremet per resource", target = DocTarget.RETURN),
        @Description(value = "The body contains a list of requests which contains a resource ID and the names of the measurements defined on it, for which the user want the metric data. The time frame for which the metric data is requested is given in the query parameters", target = DocTarget.REQUEST),
        @Description(value = "Aggregation of the requested metric data per measuremet per resource", target = DocTarget.RESPONSE),
        @Description(value = "Metric data aggregation", target = DocTarget.RESOURCE)
     })
    public ResourceMeasurementBatchResponse getAggregatedMetricData(final ResourceMeasurementRequests request, 
            @QueryParam("begin") final Date begin, 
            @QueryParam("end") final Date end) 
            throws ParseException, PermissionException, SessionNotFoundException, SessionTimeoutException, ObjectNotFoundException, UnsupportedOperationException, SQLException;
	
    @POST
    @Path("/metrics/register")
	public void register(final MetricFilterRequest metricFilterReq) throws SessionNotFoundException, SessionTimeoutException;
    
    @PUT
    @Path("/metrics/unregister")
    @Descriptions({ 
        @Description(value = "unregister user session and all assigned filters this user destination has", target = DocTarget.METHOD)
    })
    public void unregister() throws SessionNotFoundException, SessionTimeoutException;

    @POST
    @Path("/metrics/unregister")
    public void unregister(final MetricFilterRequest metricFilterReq) throws SessionNotFoundException, SessionTimeoutException;

    @POST
    @Path("/")
    public ResourceMeasurementBatchResponse getMeasurements(BulkResourceMeasurementRequest msmtMetaReq) throws SessionNotFoundException, SessionTimeoutException;
}
