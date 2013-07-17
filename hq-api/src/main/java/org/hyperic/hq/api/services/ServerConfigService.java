package org.hyperic.hq.api.services;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.api.model.config.ServerConfig;

@Path("/")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface ServerConfigService {

    @GET
    @Path("/")
    List<ServerConfig> getServerConfigs(@QueryParam("type") String type);
    
    @POST
    @Path("/")
    void addServerConfig(final ServerConfig config);
    
    @POST
    @Path("/")
    void addServerConfig(final List<ServerConfig> configs);
    
    @PUT
    @Path("/")
    void updateServerConfig(final ServerConfig config);
    
    @PUT
    @Path("/")
    void updateServerConfig(final List<ServerConfig> configs);

    @GET
    @Path("/time")
    long getServerTime();
}
