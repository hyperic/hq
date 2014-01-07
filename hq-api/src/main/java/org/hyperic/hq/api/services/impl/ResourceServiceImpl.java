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
package org.hyperic.hq.api.services.impl;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.api.model.ConfigurationTemplate;
import org.hyperic.hq.api.model.MetricTemplate;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.common.ExternalRegistrationStatus;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.resources.RegisteredResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceFilterRequest;
import org.hyperic.hq.api.services.ResourceService;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.UnknownEndpointException;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.notifications.EndpointQueue;
import org.hyperic.hq.notifications.NotificationEndpoint;
import org.springframework.beans.factory.annotation.Autowired;

public class ResourceServiceImpl extends RestApiService implements ResourceService {

	@Autowired
	private ResourceManager resourceManager;
	
	@Autowired
	private ResourceTransfer resourceTransfer;

	@Autowired
	private EndpointQueue endpointQueue;	
	
	protected Log log = LogFactory.getLog(ResourceServiceImpl.class.getName());
	
	
	public final ResourceModel getResource(final String platformNaturalID, final ResourceTypeModel resourceType, final ResourceStatusType resourceStatusType, 
			final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) throws SessionNotFoundException, SessionTimeoutException {
	    ApiMessageContext apiMessageContext = newApiMessageContext();
	    
	    ResourceModel resource = null;
	    try {
	        resource = this.resourceTransfer.getResource(apiMessageContext, platformNaturalID, resourceType, resourceStatusType, hierarchyDepth, responseMetadata);            
	    } catch (ObjectNotFoundException e) {
            WebApplicationException webApplicationException = createResourceNotFoundWAException(platformNaturalID, "natural");            
            throw webApplicationException;	        
	    }
		return resource;
	}//EOM 

    public final ResourceModel getResource(final String platformID,
                                      final ResourceStatusType resourceStatusType,
                                      final int hierarchyDepth,
                                      final ResourceDetailsType[] responseMetadata)
    throws SessionNotFoundException, SessionTimeoutException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        ResourceModel resource = null;
        try {
            resource =  this.resourceTransfer.getResource(apiMessageContext, platformID, resourceStatusType, hierarchyDepth, responseMetadata) ;
        } catch (ObjectNotFoundException e) {
            WebApplicationException webApplicationException = createResourceNotFoundWAException(platformID, "");            
            throw webApplicationException;
        } 
        return resource;
	}//EOM

    public final RegisteredResourceBatchResponse getResources(final ResourceDetailsType[] responseMetaData, final int hierarchyDepth) throws SessionNotFoundException, SessionTimeoutException, PermissionException, NotFoundException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        return this.resourceTransfer.getResources(apiMessageContext, responseMetaData, hierarchyDepth) ;
	}//EOM 

    public final RegistrationID register(final ResourceDetailsType responseMetadata, final ResourceFilterRequest resourceFilterRequest) throws SessionNotFoundException, SessionTimeoutException, PermissionException, NotFoundException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        try {
            return this.resourceTransfer.register(apiMessageContext, responseMetadata, resourceFilterRequest) ;
        } catch (PermissionException e) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.UNAUTHORIZED,
                    ExceptionToErrorCodeMapper.ErrorCode.NON_ADMIN_ERR, "");
        }
    }//EOM

    public final ExternalRegistrationStatus getRegistrationStatus(final String registrationID)  throws SessionNotFoundException, SessionTimeoutException, PermissionException, NotFoundException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        try {
            return this.resourceTransfer.getRegistrationStatus(apiMessageContext, registrationID);
        }catch(UnknownEndpointException e) {
            e.printStackTrace();
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.INTERNAL_SERVER_ERROR,
                    ExceptionToErrorCodeMapper.ErrorCode.UNKNOWN_ENDPOINT, e.getRegistrationID());
        } catch (PermissionException e) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.UNAUTHORIZED,
                    ExceptionToErrorCodeMapper.ErrorCode.NON_ADMIN_ERR, "");
        }
    }//EOM

	public final ResourceBatchResponse approveResource(final Resources aiResources) throws SessionNotFoundException, SessionTimeoutException {
	    ApiMessageContext apiMessageContext = newApiMessageContext();
		return this.resourceTransfer.approveResource(apiMessageContext, aiResources) ; 
	}//EOM 
	
	public final ResourceBatchResponse updateResources(final Resources resources) throws SessionNotFoundException, SessionTimeoutException { 
	    ApiMessageContext apiMessageContext = newApiMessageContext();
		return this.resourceTransfer.updateResources(apiMessageContext, resources) ; 
	}//EOM
	
	public final ResourceBatchResponse updateResourcesByCriteria(final ResourceModel updateData) throws SessionNotFoundException, SessionTimeoutException {
	    ApiMessageContext apiMessageContext = newApiMessageContext();
		//TODO: NYI 
		//return this.resourceTransfer.approveResource(cirteria, updateData) ;
		throw new UnsupportedOperationException() ; 
	}//EOM 
	
	public void unregister(final String registrationId) throws SessionNotFoundException, SessionTimeoutException {
        NotificationEndpoint endpoint = endpointQueue.unregister(registrationId);
        if (endpoint == null) {
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST,
                    ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID);
        }
        try {
            ApiMessageContext apiMessageContext = newApiMessageContext();
            resourceTransfer.unregister(apiMessageContext,endpoint);
        } catch (PermissionException e) {
            throw errorHandler.newWebApplicationException(new Throwable(), Response.Status.UNAUTHORIZED,
                    ExceptionToErrorCodeMapper.ErrorCode.NON_ADMIN_ERR, "");
        }
	}

    public ConfigurationTemplate getConfigurationTemplateByName(final String protoTypeName)
    throws SessionNotFoundException, SessionTimeoutException {
        Resource protoType = resourceManager.findResourcePrototypeByName(protoTypeName);
        if (protoType == null) {
            log.error("Resource Prototype" + protoTypeName + " not found.");
            final WebApplicationException webApplicationException = createResourceNotFoundWAException(protoTypeName, "prototype");
            throw webApplicationException;
        }
        return getConfigurationTemplate(protoType.getId().toString());
    }

    public ConfigurationTemplate getConfigurationTemplate(final String resourceID)
    throws SessionNotFoundException, SessionTimeoutException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        try {
            return this.resourceTransfer.getConfigurationTemplate(apiMessageContext, resourceID);
        } catch(WebApplicationException e) {
            throw e;
        } catch(AppdefEntityNotFoundException e) {
            log.error("Resource " + resourceID + " not found.", e);
            final WebApplicationException webApplicationException = createResourceNotFoundWAException(resourceID, "");
            throw webApplicationException;
        } catch(PermissionException e) {
            log.error("Insufficient permissions for the action", e);
            final WebApplicationException webApplicationException = new WebApplicationException(e,
                    Response.Status.FORBIDDEN);
            throw webApplicationException;
        } catch(ConfigFetchException e) {
            log.error("Failed to fetch exception", e);
            final WebApplicationException webApplicationException = errorHandler.newWebApplicationException(e,
                    Response.Status.INTERNAL_SERVER_ERROR, ExceptionToErrorCodeMapper.ErrorCode.FAILED_TO_FETCH_CONFIGURATION);
            throw webApplicationException;
        }
    }// EOM getConfigurationTemplate

    @GET
    @Path("/measurement-name-by-prototype")
    public List<MetricTemplate> getMeasurementNamesByProtoType(@QueryParam("protoTypeName") String protoTypeName)
    throws SessionNotFoundException, SessionTimeoutException {
        final Resource proto = resourceManager.findResourcePrototypeByName(protoTypeName);
        return getMetricTemplate(proto.getId().toString());
    }

    public List<MetricTemplate> getMetricTemplate(final String resourceID) throws SessionNotFoundException,
    SessionTimeoutException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        try {
            return this.resourceTransfer.getMetricTemplates(apiMessageContext, resourceID);
        } catch(ObjectNotFoundException e) {
            log.error("Resource " + resourceID + " not found.", e);
            final WebApplicationException webApplicationException = createResourceNotFoundWAException(resourceID, "");
            throw webApplicationException;
        } catch (WebApplicationException e) {
            throw e;
        } catch(PermissionException e) {
            log.error("Insufficient permissions for the action", e);
            final WebApplicationException webApplicationException = new WebApplicationException(e,
                    Response.Status.FORBIDDEN);
            throw webApplicationException;
        }
    }//EOM getMetricTemplate
    
    private WebApplicationException createResourceNotFoundWAException(final String resourceID, final String idType) {
        logger.warn("Resource with the " + idType + " ID " + resourceID + " not found.");
        final WebApplicationException webApplicationException = 
                errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID, resourceID);
        return webApplicationException;
    }    
}//EOC 
