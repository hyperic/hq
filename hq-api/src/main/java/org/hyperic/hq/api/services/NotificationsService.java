package org.hyperic.hq.api.services;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;


@Path("/") 
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON}) 
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface NotificationsService {
    @GET
    @Path("/")
    public NotificationsReport poll(@QueryParam("registrationid") String id) throws SessionNotFoundException, SessionTimeoutException;

    @DELETE
    @Path("/")
    public void unregister(@QueryParam("registrationid") String id) throws SessionNotFoundException, SessionTimeoutException;

    @POST
    @Path("/post")
    public String post(String message) throws SessionNotFoundException, SessionTimeoutException;
}
