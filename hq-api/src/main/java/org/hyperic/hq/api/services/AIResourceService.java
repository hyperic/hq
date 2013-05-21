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
 *
 * **********************************************************************
 * 29 April 2012
 * Maya Anderson
 * *********************************************************************/
package org.hyperic.hq.api.services;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.util.config.ConfigOption;


/** 
 * Automatically discovered resource service declaration.
 * 
 * @since   4.5.0
 * @version 1.0 29 April 2012
 * @author Maya Anderson
 */
@Path("/")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface AIResourceService {
    
    @GET
    @Path("/{discovery-id}")
    @Descriptions({ 
           @Description(value = "Get discovered resource with its resource descendants", target = DocTarget.METHOD),
           @Description(value = "Requested discovered resource", target = DocTarget.RETURN),
           @Description(value = "Discovered resource's discovery identifier and resource type", target = DocTarget.REQUEST),
           @Description(value = "Requested discovered resource", target = DocTarget.RESPONSE),
           @Description(value = "Discovered resource", target = DocTarget.RESOURCE)
        })
    public AIResource getAIResource(@PathParam("discovery-id") String discoveryId, @QueryParam("type") ResourceTypeModel type);

    @POST
    @Path("/approve")
    @Descriptions({ 
       @Description(value = "Approve discovered resource", target = DocTarget.METHOD),
       @Description(value = "Approved Resources", target = DocTarget.RETURN),
       @Description(value = "Discovered resources' ids and their common resource type", target = DocTarget.REQUEST),
       @Description(value = "List of approved Resources", target = DocTarget.RESPONSE),
       @Description(value = "Discovered Resource", target = DocTarget.RESOURCE)
    })    
    public List<ResourceModel> approveAIResource(@QueryParam("id") List<String> ids, @QueryParam("type") ResourceTypeModel type);
    
    public List<ConfigOption> getConfigurationSchema();    
//    
//    @POST
//    @Path("{id}/configuration")
//    public void configureAIResource(@PathParam("id") String id, List<ConfigurationValue> resourceConfiguration);
}
