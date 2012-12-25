package org.hyperic.hq.api.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.api.model.cloud.CloudConfiguration;

@Path("/")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface VMService {

    @POST
    @Path("/collect")
    public void collect(final CloudConfiguration cloudConfiguration);
}
