package org.hyperic.hq.api.services;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;

@Path("/")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface VMService {

    @POST
    @Path("/registerVC")
    public void registerVC(@QueryParam("url") String url,@QueryParam("user") String user, @QueryParam("password") String password) throws RemoteException, MalformedURLException, SessionNotFoundException, SessionTimeoutException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException;
    
    @POST
    @Path("/validateVCSettings")
    public boolean validateVCSettings(@QueryParam("url") String url,@QueryParam("user") String user, @QueryParam("password") String password) throws RemoteException, MalformedURLException, SessionNotFoundException, SessionTimeoutException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException;
}
