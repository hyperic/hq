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
package org.hyperic.hq.api.services.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.hyperic.hq.api.model.ConfigurationOption;
import org.hyperic.hq.api.model.ConfigurationTemplate;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.resources.RegisteredResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceFilterRequest;
import org.hyperic.hq.api.services.ResourceService;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

//@Component
public class ResourceServiceImpl extends RestApiService implements ResourceService{
	
	@Autowired
	private ResourceTransfer resourceTransfer;
	
//	@Autowired
//	private ConfigurationTransfer configurationTransfer;
	
	
	public final Resource getResource(final String platformNaturalID, final ResourceType resourceType, final ResourceStatusType resourceStatusType, 
			final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) throws SessionNotFoundException, SessionTimeoutException {
	    ApiMessageContext apiMessageContext = newApiMessageContext();
	    
	    Resource resource = null;
	    try {
	        resource = this.resourceTransfer.getResource(apiMessageContext, platformNaturalID, resourceType, resourceStatusType, hierarchyDepth, responseMetadata);            
	    } catch (ObjectNotFoundException e) {
            WebApplicationException webApplicationException = createResourceNotFoundWAException(platformNaturalID, "natural");            
            throw webApplicationException;	        
	    }
		return resource;
	}//EOM 

    public final Resource getResource(final String platformID, final ResourceStatusType resourceStatusType, final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) throws SessionNotFoundException, SessionTimeoutException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        Resource resource = null;
        try {
            resource =  this.resourceTransfer.getResource(apiMessageContext, platformID, resourceStatusType, hierarchyDepth, responseMetadata) ;
        } catch (ObjectNotFoundException e) {
            WebApplicationException webApplicationException = createResourceNotFoundWAException(platformID, "");            
            throw webApplicationException;
        } 
        return resource;
	}//EOM 
	
	public final RegisteredResourceBatchResponse getResources(final ResourceDetailsType responseStructure, final int hierarchyDepth, final boolean register,
	        final ResourceFilterRequest resourceFilterRequest) throws SessionNotFoundException, SessionTimeoutException, PermissionException, NotFoundException { 
        ApiMessageContext apiMessageContext = newApiMessageContext();
        return this.resourceTransfer.getResources(apiMessageContext, responseStructure, hierarchyDepth,register,resourceFilterRequest) ;
	}//EOM 
	
	public final ResourceBatchResponse approveResource(final Resources aiResources) throws SessionNotFoundException, SessionTimeoutException {
	    ApiMessageContext apiMessageContext = newApiMessageContext();
		return this.resourceTransfer.approveResource(apiMessageContext, aiResources) ; 
	}//EOM 
	
	public final ResourceBatchResponse updateResources(final Resources resources) throws SessionNotFoundException, SessionTimeoutException { 
	    ApiMessageContext apiMessageContext = newApiMessageContext();
		return this.resourceTransfer.updateResources(apiMessageContext, resources) ; 
	}//EOM
	
	public final ResourceBatchResponse updateResourcesByCriteria(final Resource updateData) throws SessionNotFoundException, SessionTimeoutException {
	    ApiMessageContext apiMessageContext = newApiMessageContext();
		//TODO: NYI 
		//return this.resourceTransfer.approveResource(cirteria, updateData) ;
		throw new UnsupportedOperationException() ; 
	}//EOM 
	
	public void unregister() throws SessionNotFoundException, SessionTimeoutException {
	    this.resourceTransfer.unregister();
	}

	// TODO: Implement
    public ConfigurationTemplate getConfigurationTemplate(final String resourceID) throws SessionNotFoundException, SessionTimeoutException {
        
        // TODO: implement error handling!!!
        ApiMessageContext apiMessageContext = newApiMessageContext();
        try {
            return this.resourceTransfer.getConfigurationTemplate(apiMessageContext, resourceID);
        }catch(AppdefEntityNotFoundException e) {
            WebApplicationException webApplicationException = createResourceNotFoundWAException(resourceID, "");            
            throw webApplicationException;
        }catch(ConfigFetchException e) {
            // TODO Auto-generated catch block
            throw new WebApplicationException(e);
        }catch(PermissionException e) {
            // TODO Auto-generated catch block
            throw new WebApplicationException(e);
        }catch(PluginException e) {
            // TODO Auto-generated catch block
            throw new WebApplicationException(e);
        }catch(EncodingException e) {
            // TODO Auto-generated catch block
            throw new WebApplicationException(e);
        }
        
//        ConfigurationTemplate configTemplate = new ConfigurationTemplate();
//        List<ConfigurationOption> configOptions = new ArrayList<ConfigurationOption>();
//        configTemplate.setConfigurationOptions(configOptions);
//        
//        // int, double, boolean, long, string, ip, enum, secret, hidden, port, macaddress, stringarray
//        configOptions.add(new ConfigurationOption("optionalInt", "an int", "measurement", "0", null, true, "int", null));
//        configOptions.add(new ConfigurationOption("mandatoryIp", "an IP", "measurement", "0.0.0.0", null, false, "int", null));
//        String[] enumValues = {"ONE", "TWO", "THREE"};
//        configOptions.add(new ConfigurationOption("anEnum", "an enum", "measurement", "ONE", null, false, "enum", Arrays.asList(enumValues)));
//        
//            
//        
//        return configTemplate;
    }//EOM
    
    
    private WebApplicationException createResourceNotFoundWAException(final String resourceID, final String idType) {
        logger.warn("Resource with the " + idType + " ID " + resourceID + " not found.");
        WebApplicationException webApplicationException = 
                errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID, resourceID);
        return webApplicationException;
    }    
}//EOC 
