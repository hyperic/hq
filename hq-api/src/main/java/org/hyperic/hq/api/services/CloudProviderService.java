/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2013], VMware, Inc.
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hyperic.hq.api.model.cloud.CloudConfiguration;
import org.hyperic.hq.api.model.cloud.DataCenterList;
import org.hyperic.hq.api.model.cloud.HostsList;
import org.hyperic.hq.api.model.cloud.VirtualMachinesList;



@Path("/")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface CloudProviderService {
   
    @GET
    @Path("/dc")
    DataCenterList getDataCenters() throws Throwable;  
    
    @POST
    @Path("/configure")
    void configureCloudProvider(final CloudConfiguration cloudConfiguration) throws Throwable ; 
    
    @GET
    @Path("/dc/{dcName}")
    HostsList getHosts(@PathParam("dcName") final String dcName) throws Throwable ;  
    
    @GET
    @Path("/dc/{dcName}/host/{hostName}")
    VirtualMachinesList getVms(@PathParam("dcName") final String dcName, @PathParam("hostName") final String hostName) throws Throwable ; 
}
