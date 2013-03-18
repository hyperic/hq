/* **********************************************************************
/*  
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.common.RegistrationStatus;
import org.hyperic.hq.api.model.resources.RegisteredResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceFilterRequest;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;


@Path("/")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface ResourceService {


    @GET
    @Path("/{platformNaturalID}/{resourceType}")
    Resource getResource(@PathParam("platformNaturalID") final String platformNaturalID,
                         @PathParam("resourceType") final ResourceType resourceType,
                         @QueryParam("status") final ResourceStatusType resourceStatusType,
                         @QueryParam("hierarchyDepth") final int hierarchyDepth,
                         @QueryParam("responseStructure") final ResourceDetailsType[] responseStructure)
            throws SessionNotFoundException, SessionTimeoutException;

    @GET
    @Path("/resourceUrl/{resourceID}")
    String getResourceUrl(@PathParam("resourceID") final int resourceID) throws SessionNotFoundException,
            SessionTimeoutException;

    @GET
    @Path("/{platformID}")
    Resource getResource(@PathParam("platformID") final String platformID,
                         @QueryParam("status") final ResourceStatusType resourceStatusType,
                         @QueryParam("hierarchyDepth") final int hierarchyDepth,
                         @QueryParam("responseStructure") final ResourceDetailsType[] responseStructure)
            throws SessionNotFoundException, SessionTimeoutException;

    @GET
    @Path("/")
    RegisteredResourceBatchResponse getResources(@QueryParam("responseStructure") final ResourceDetailsType[]
                                                         responseStructure,
                                                 @QueryParam("hierarchyDepth") final int hierarchyDepth) throws
            SessionNotFoundException, SessionTimeoutException, PermissionException, NotFoundException;

    @POST
    @Path("/registration")
    RegistrationID register(@QueryParam("responseStructure") final ResourceDetailsType responseMetadata,
                            final ResourceFilterRequest resourceFilterRequest) throws SessionNotFoundException,
            SessionTimeoutException, PermissionException, NotFoundException;

    @GET
    @Path("/registration/{registrationID}")
    RegistrationStatus getRegistrationStatus(@PathParam("registrationID") final int registrationID) throws
            SessionNotFoundException, SessionTimeoutException, PermissionException, NotFoundException;

    @POST
    @Path("/approve")
    ResourceBatchResponse approveResource(final Resources aiResources) throws SessionNotFoundException,
            SessionTimeoutException;

    @PUT
    ResourceBatchResponse updateResources(final Resources resources) throws SessionNotFoundException,
            SessionTimeoutException;

    @PUT
    @Path("/search")
    ResourceBatchResponse updateResourcesByCriteria(final Resource updateData) throws SessionNotFoundException,
            SessionTimeoutException;

    @DELETE
    @Path("/registration/{registrationID}")
    public void unregister(@PathParam("registrationID") final long registrationID) throws SessionNotFoundException,
            SessionTimeoutException;
}//EOC 
