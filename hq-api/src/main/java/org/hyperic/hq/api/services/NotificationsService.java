package org.hyperic.hq.api.services;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;


@Path("/") 
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON}) 
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface NotificationsService {
    @PUT
    @Path("/{regID}")
    public void refresh(@PathParam("regID") final Integer regID) throws SessionNotFoundException, SessionTimeoutException;
    
    @GET
    @Path("/{regID}")
    public NotificationsReport poll(@PathParam("regID") Integer regID) throws SessionNotFoundException, SessionTimeoutException;

    @DELETE
    @Path("/{regID}")
    public void unregister(@PathParam("regID") Integer regID) throws SessionNotFoundException, SessionTimeoutException;
}
