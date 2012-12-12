/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
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
package org.hyperic.hq.api.transfer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.ResourceMapper;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

//@Component
public class ResourceTransferImpl implements ResourceTransfer{

    private AIQueueManager aiQueueManager;
	private ResourceManager resourceManager ; 
    private AuthzSubjectManager authzSubjectManager;
    private ResourceMapper resourceMapper;
    private ProductBoss productBoss ;
	private CPropManager cpropManager ;
	private AppdefBoss appdepBoss ;
	private PlatformManager platformManager ; 
	private ExceptionToErrorCodeMapper errorHandler ;
	private Log log ;
	
	@Autowired  
    public ResourceTransferImpl(final AIQueueManager aiQueueManager, final ResourceManager resourceManager, 
    		final AuthzSubjectManager authzSubjectManager, final ResourceMapper resourceMapper, 
    		final ProductBoss productBoss, final CPropManager cpropManager, final AppdefBoss appdepBoss, 
    		final PlatformManager platformManager, final ExceptionToErrorCodeMapper errorHandler, @Qualifier("restApiLogger")Log log) { 
    	this.aiQueueManager = aiQueueManager ; 
    	this.resourceManager = resourceManager ; 
    	this.authzSubjectManager = authzSubjectManager ; 
    	this.resourceMapper = resourceMapper ; 
    	this.productBoss = productBoss ; 
    	this.cpropManager = cpropManager ; 
    	this.appdepBoss = appdepBoss ;
    	this.platformManager = platformManager ; 
    	this.errorHandler = errorHandler ; 
    	this.log = log ;
    }//EOM 
	    
	public final Resource getResource(ApiMessageContext messageContext, final String platformNaturalID, final ResourceType resourceType, 
			final ResourceStatusType resourceStatusType, final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) throws SessionNotFoundException, SessionTimeoutException, ObjectNotFoundException {
	    AuthzSubject authzSubject = messageContext.getAuthzSubject();
		if(resourceStatusType == ResourceStatusType.AUTO_DISCOVERED) { 
			return this.getAIResource(platformNaturalID, resourceType, hierarchyDepth, responseMetadata) ; 
		}else { 			
            return this.getResourceInner(new Context(authzSubject, platformNaturalID, resourceType, responseMetadata, this), hierarchyDepth) ;  
		}//EO else if approved resource 
	}//EOM
	
	/**
	 * Note: AI resources are unsupported 
	 * @param platformID
	 * @param resourceStatusType
	 * @param hierarchyDepth
	 * @param responseMetadata
	 * @return
	 * @throws ObjectNotFoundException 
	 */
	public final Resource getResource(ApiMessageContext messageContext, final String platformID, final ResourceStatusType resourceStatusType, final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) throws ObjectNotFoundException {
	    AuthzSubject authzSubject = messageContext.getAuthzSubject();
		if(resourceStatusType == ResourceStatusType.AUTO_DISCOVERED) { 
			throw new UnsupportedOperationException("AI Resource load by internal ID is unsupported") ; 
		}else { 
			return this.getResourceInner(new Context(authzSubject, platformID, responseMetadata, this), hierarchyDepth) ;
		}//EO else if approved resource type 
		 
	}//EOM 
	

	/**
	 * 
	 * @param platformID
	 * @param resourceID if null assumed to be the internal id 
	 * @param hierarchyDepth
	 * @param responseMetadata
	 * @return
	 * @throws ObjectNotFoundException 
	 */
	private final Resource getResourceInner(final Context flowContext, int hierarcyDepth) throws ObjectNotFoundException { 
		
		Resource currentResource =  null ; 
		try{ 
			//derive the resource type load strategy using the resource type enum 
			//Note: of the resourceType is null, then the generic resource resource type 
			//would be used 
			final ResourceTypeStrategy resourceTypeStrategy = ResourceTypeStrategy.valueOf(flowContext.resourceType) ; 

			//if the resource Type is null then find by internal id else natural id  
			flowContext.setBackendResource(resourceTypeStrategy.getResource(flowContext)) ;  
				
			//populate the response resource (excluding the children)
			for(ResourceDetailsTypeStrategy resourceDetailType : flowContext.resourceDetails) { 
				resourceDetailType.populateResource(flowContext) ; 
			}//EO while there are more resource details 
			
			//if resource root was not yet initialized do so now 
			//if(flowContext.resourceRoot == null) flowContext.resourceRoot = flowContext.currResource ; 
			currentResource = flowContext.currResource ; 
			
			//starts from 1 
			if(--hierarcyDepth > 0) {
				//populate the resource's children 
				final List<org.hyperic.hq.authz.server.session.Resource> listBackendResourceChildren =
								this.resourceManager.findChildren(flowContext.subject, flowContext.backendResource) ; 
				
				Resource resourceChild = null ; 
				for(org.hyperic.hq.authz.server.session.Resource backendResourceChild : listBackendResourceChildren) { 
					flowContext.reset() ; 
					
					//set the child in the flow's backend resource 
					flowContext.setBackendResource(backendResourceChild) ; 
					flowContext.resourceType = ResourceType.valueOf(backendResourceChild.getResourceType().getAppdefType())  ; 
					resourceChild = this.getResourceInner(flowContext, hierarcyDepth) ;
					currentResource.addSubResource(resourceChild) ;
					
				}//EO while there are more children 
				
			}//EOM 
			
		} catch (ObjectNotFoundException e) {
		    throw e;
		}catch(Throwable t) { 
			throw (t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t)) ; 
		}//EO catch block 
		
		return currentResource ;  
		
	}//EOM
	
	
	public final ResourceBatchResponse approveResource(ApiMessageContext messageContext, final Resources aiResources) {
		//NYI 
		return null; 
	}//EOM 
	
//	private final AuthzSubject getAuthzSubject() {
//		//TODO: replace with actual subject once security layer is implemented 
//		//return authzSubjectManager.getOverlordPojo();
//		AuthzSubject subject = authzSubjectManager.findSubjectByName("hqadmin") ;
//		return (subject != null ? subject : authzSubjectManager.getOverlordPojo()) ; 
//	}//EOM
	
	
	@Transactional(readOnly=false)
	public final ResourceBatchResponse updateResources(ApiMessageContext messageContext, final Resources resources) {

		final ResourceBatchResponse response = new ResourceBatchResponse(this.errorHandler) ; 
		
		//iterate over the resources and for each resource, update the following metadata if exists and changed:
		//TODO: if no resources where provided (null or empty) should an excpetion be thrown? 
		final List<Resource> resourcesList = resources.getResources() ; 
		if(resourcesList == null) return response ; 
		String resourceID = null ;
		final int noOfInputResources = resourcesList.size() ; 
		Resource inputResource = null ; 
		ResourceConfig resourceConfig = null ; 
		
		AuthzSubject authzSubject = messageContext.getAuthzSubject();
		final Context flowContext = new Context(authzSubject, this) ; 
		
		for(int i=0; i < noOfInputResources; i++) { 
			
			try{ 
				inputResource = resourcesList.get(i) ;
				flowContext.setInputResource(inputResource) ; 
				
				//find the backend resource using the resource Type (if null then find by internal id else natural id)
				//not need to check for null backendResource as exception should have been thrown if the resource 
				//could not have been loaded
				final ResourceTypeStrategy resourceTypeStrategy = ResourceTypeStrategy.valueOf(inputResource.getResourceType()) ;
				
				flowContext.setBackendResource(resourceTypeStrategy.getResource(flowContext)) ;  
				
				//map the input into the backend resource (resource metadata update
				this.resourceMapper.mergeResource(inputResource, flowContext.backendResource) ; 
				
				//if there are properties to update do so now
				resourceConfig = inputResource.getResourceConfig() ; 
				if(resourceConfig !=  null) this.updateResourceConfig(flowContext, resourceConfig) ; 
				
			}catch(Throwable t) { 
				this.errorHandler.log(t) ; 

				resourceID = ResourceTypeStrategy.getResourceIdentifier(flowContext.currResource) ; 
				
				String description = resourceID, additionalDescription = null ; 
				if(t instanceof NumberFormatException) {
					description = "Resource ID " + resourceID ; 
				}//EO if number format 
				else if(t instanceof InvalidConfigException) { 
				    additionalDescription = t.getMessage() ; 
				}//EO else if InvalidConfigException
				
				response.addFailedResource(t,resourceID, additionalDescription /*additional Description*/, description) ;
				resourceID = null  ;
			} finally {
	             flowContext.reset() ; 
			}
			
		}//EO while there are more resources
		
		return response ; 
	}//EOM 
	
	/**
	 * 
	 * Note: AI resource updates are unsupported 
	 * Note: Currently all-or-nothing.
	 * 
	 * @param flowContext
	 * @param resourceConfig
	 * @throws EncodingException
	 * @throws PluginNotFoundException
	 * @throws PluginException
	 * @throws ScheduleWillNeverFireException
	 * @throws ApplicationException
	 * @throws AutoinventoryException
	 * @throws AgentConnectionException
	 */
	private final void updateResourceConfig(final Context flowContext, final ResourceConfig resourceConfig) throws 
		EncodingException, PluginNotFoundException, PluginException, ScheduleWillNeverFireException, ApplicationException, AutoinventoryException, AgentConnectionException { 
		
		final org.hyperic.hq.authz.server.session.Resource resource = flowContext.backendResource  ; 
		final AppdefEntityID entityID = flowContext.entityID = AppdefUtil.newAppdefEntityId(resource) ;  
		
		//load existing resource config data 
		this.initResourceConfig(flowContext) ; 
		
		//merge cprops 
		final org.hyperic.hq.authz.server.session.Resource prototype = resource.getPrototype() ; 
		final int appdefTypeID = resource.getResourceType().getAppdefType() ; 
		
		Object oldValue = null ; 
		String sKey = null, sNewValue = null ;
		
		//iterate over the new config properties and search for a match in the following config sub systems 
		// - cprops 
		// - config response 
		//if not found in the above, store the value so that a user response may contain a list of unset (new) properties 
		final Map<String,String> configValues = resourceConfig.getMapProps() ; 
		final List<String> listUnmatchedConfigProperties = new ArrayList<String>(configValues.size()); 
		
		//config resource modifications 
		final AllConfigResponses allConfigs = new AllConfigResponses(), rollbackConfigs = new AllConfigResponses() ;
		rollbackConfigs.setResource(allConfigs.setResource(entityID)) ;
		
		final int iNoOfConfigurableTypes = ProductPlugin.CONFIGURABLE_TYPES.length ;
		final ConfigResponse[] newConfigResponses = new ConfigResponse[iNoOfConfigurableTypes] ; 

		ConfigSchemaAndBaseResponse configResponse = null ; 
		ConfigSchema configSchema = null ; 
		ConfigResponse currConfigData = null, newConfigData = null ;  
		boolean bKeySupported = false, bOverallKeySupported = false, bHadConfigResponsesChanged = false ; 
		  
		for(Map.Entry<String,String> entry : configValues.entrySet()) { 
			
			sKey = entry.getKey() ; 
			sNewValue = entry.getValue() ; 
			
			if(flowContext.cprops != null) {
				oldValue = flowContext.cprops.get(sKey) ; 
				
				if((bKeySupported = flowContext.cprops.containsKey(sKey)) && (oldValue == null || !oldValue.equals(sNewValue))) { 
					this.cpropManager.setValue(entityID, appdefTypeID, sKey, sNewValue) ; 
				}//EO if the property exists and the value is different 
			}//EO if there were cprops associated with the resource 
			
			for(int i=0; i < iNoOfConfigurableTypes; i++) { 
				
				configResponse = flowContext.configResponses[i] ; 
				
				if(configResponse == null) { 
					rollbackConfigs.setSupports(i, false) ; 
				}else { 
					allConfigs.setSupports(i, true) ; 
					rollbackConfigs.setSupports(i, true) ;
					rollbackConfigs.setConfig(i, configResponse.getResponse()) ; 
					
					newConfigData = newConfigResponses[i] ;  
					if(newConfigData == null) { 
						newConfigData = newConfigResponses[i] = new ConfigResponse() ;
						allConfigs.setConfig(i, newConfigData)  ;
					}//EO if not yet initialized 
 
					currConfigData = configResponse.getResponse() ;
					oldValue = currConfigData.getValue(sKey) ; 
					 	
					 //if the current configResponse contains the key and the value is different, 
					 //override with the new value else ignore
						 
					 if( (bKeySupported = (oldValue != null || currConfigData.supportsOption(sKey))) && (oldValue == null || !oldValue.equals(sNewValue))) { 
							newConfigData.setValue(sKey, sNewValue) ; 
							bHadConfigResponsesChanged = true ; 
							allConfigs.setShouldConfig(i, true) ; 
					 }//EO if the config option was provided and was different than the current 
					 
					 bOverallKeySupported = (bOverallKeySupported || bKeySupported) ;  
					 
				}//EO else if config data was defined 
				
			}//EO while there are more config resources
			
			//if !bConfigChanged then the key was not supported and should be discarded 
			if(!bOverallKeySupported) listUnmatchedConfigProperties.add(sKey) ;
			
			//reset the overall key support flag 
			bOverallKeySupported = false ; 
			
		}//EO while there are more new entries 
		
		//only store the configResponse if modifications were made 
		if(bHadConfigResponsesChanged) {
		    //allConfigs.setEnableRuntimeAIScan(true) ;  
		    //rollbackConfigs.setEnableRuntimeAIScan(true); 
		    this.appdepBoss.setAllConfigResponses(flowContext.subject, allConfigs, rollbackConfigs, false /*isUserManaged*/) ;
		}//EO if there was a change 

		//TODO: pojo fields modifications 
	}//EOM 
	
	private final Object initResourceConfig(final Context flowContext)  
		throws ConfigFetchException, EncodingException, PluginNotFoundException, PluginException, PermissionException, AppdefEntityNotFoundException {
		
		final int iNoOfConfigTypes = ProductPlugin.CONFIGURABLE_TYPES.length ;
		ConfigSchemaAndBaseResponse configMetadata = null ; 
		
		//load all resource related config metadata&data resources
		String configurableType = null ; 
		for(int i=0; i < iNoOfConfigTypes; i++) { 
			configurableType = ProductPlugin.CONFIGURABLE_TYPES[i] ; 
			try{ 
				flowContext.configResponses[i] = configMetadata = this.productBoss.getConfigSchemaAndBaseResponse(flowContext.subject, flowContext.entityID, configurableType, false/*validateFlow*/) ; 
				//init the schema map in the config response so as to support key recognition validation 
				configMetadata.getResponse().setSchema(configMetadata.getSchema()) ; 
			}catch(PluginNotFoundException pnfe) { 
				log.debug("Plugin Config Schema of type: " + configurableType + " was not defined for resource " + flowContext.entityID) ;	
			}//EO catch block 
			catch(NullPointerException npe) { 
				npe.printStackTrace() ; 
			}
		}//EO while there are more configurable types
		
		//load all cprop metadata
		final org.hyperic.hq.authz.server.session.Resource prototype = flowContext.backendResource.getPrototype() ; 
		
		flowContext.cprops = cpropManager.getEntries(flowContext.entityID) ; 
		
		//TODO: pojo members data 
		
		return null ; 
	}//EOM 
		
	
	private enum ResourceDetailsTypeStrategy { 
		BASIC{ 
			@Override
			final Resource populateResource(final Context flowContext) throws Throwable{ 
				return flowContext.currResource = flowContext.visitor.resourceMapper.toResource(flowContext.backendResource) ;
				
			}//EOM 
		},//EO BASIC 
		PROPERTIES{ 
			/**
			 * @throws PluginException 
			 * @throws EncodingException 
			 * @throws PermissionException 
			 * @throws PluginNotFoundException 
			 * @throws ConfigFetchException 
			 * @throws AppdefEntityNotFoundException 
			 */
			@Override
			final Resource populateResource(final Context flowContext) throws Throwable { 
				Resource resource = flowContext.currResource ; 
				if(resource == null) { 
					resource = flowContext.currResource = new Resource(flowContext.internalID) ; 
				}//EO if resource was not initialized yet 
				//init the response config metadata 
				
				flowContext.entityID = AppdefUtil.newAppdefEntityId(flowContext.backendResource) ;
				flowContext.visitor.initResourceConfig(flowContext) ;
				return flowContext.visitor.resourceMapper.mergeConfig(flowContext.resourceType, flowContext.backendResource ,resource, flowContext.configResponses) ; 
			}//EOM 
		},//EO PROPERTIES
		ALL{ 
			@Override
			final Resource populateResource(final Context flowContext) throws Throwable{ 
				BASIC.populateResource(flowContext) ;
				return PROPERTIES.populateResource(flowContext) ;
			}//EOM
			
			@Override 
			protected final SortedSet<ResourceDetailsTypeStrategy> addToSuperset(SortedSet<ResourceDetailsTypeStrategy> setUniqueResourceDetails) { 
				setUniqueResourceDetails.clear() ;
				return super.addToSuperset(setUniqueResourceDetails) ;  
			}//EOM
		};//EO ALL 
		
		protected SortedSet<ResourceDetailsTypeStrategy> addToSuperset(SortedSet<ResourceDetailsTypeStrategy> setUniqueResourceDetails) { 
			setUniqueResourceDetails.add(this) ; 
			return setUniqueResourceDetails ; 
		}//EOM 
				
		abstract Resource populateResource(final Context flowContext) throws Throwable;  
		
		static final Set<ResourceDetailsTypeStrategy> valueOf(final ResourceDetailsType[] arrResourceDetailsTypes) { 
			final SortedSet<ResourceDetailsTypeStrategy> setUniqueResourceDetails = new TreeSet<ResourceDetailsTypeStrategy>() ; 

			if(arrResourceDetailsTypes == null || arrResourceDetailsTypes.length == 0) { 
				setUniqueResourceDetails.add(ALL) ; 
				return setUniqueResourceDetails ; 
			}//EO if all 

			ResourceDetailsTypeStrategy enumResourceDetailsTypeStrategy = null ; 
			
			for(ResourceDetailsType enumResourceDetailsType : arrResourceDetailsTypes) { 
				try{ 
					enumResourceDetailsTypeStrategy = ResourceDetailsTypeStrategy.valueOf(enumResourceDetailsType.name()) ; 
					enumResourceDetailsTypeStrategy.addToSuperset(setUniqueResourceDetails) ;  
				}catch(Throwable t) { 
					t.printStackTrace() ; 
				}
			}//EO while there are more arrResourceDetailsTypes
			
			return setUniqueResourceDetails ; 
		}//EOM 
	}//EOE ResourceDetailsTypeStrategy
	
	private enum ResourceTypeStrategy { 
		
		PLATFORM(AppdefEntityConstants.APPDEF_TYPE_PLATFORM) { 
			
			@Override
			final org.hyperic.hq.authz.server.session.Resource getResourceByNaturalID(final Context flowContext) throws PlatformNotFoundException, PermissionException {
				final Platform platform = flowContext.visitor.platformManager.findPlatformByFqdn(flowContext.subject, flowContext.naturalID);  
				return platform.getResource() ;  
			}//EOM 
			
		},//EO PLATFORM
		SERVER(AppdefEntityConstants.APPDEF_TYPE_SERVER) {
			
		},//EO SERVER
		SERVICE(AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
			
		},//EO SERVER
		RESOURCE(-999){
			
		};//EO RESOURCE
		
		/**
		 * 
		 * @param resourceID naturalID/internal ID 
		 * @param resourceManager
		 * @return
		 */
		org.hyperic.hq.authz.server.session.Resource getResourceByNaturalID(final Context flowContext) throws Exception{ 
			throw new UnsupportedOperationException("Resource Of Type " + this.name() + " does not support find by natural ID") ; 
		}//EOM 
		
		org.hyperic.hq.authz.server.session.Resource getResourceByInternalID(final Context flowContext) { 
			final int iInternalResourceID = Integer.parseInt(flowContext.internalID) ; 
			return flowContext.visitor.resourceManager.findResourceById(iInternalResourceID) ; 
		}//EOM
		
		org.hyperic.hq.authz.server.session.Resource getResource(final Context flowContext) throws Exception{ 
			org.hyperic.hq.authz.server.session.Resource resource = null ;  
			
			if(flowContext.backendResource != null) { 
				resource = flowContext.backendResource ; 
			}else if(flowContext.internalID != null) { 
				 resource = this.getResourceByInternalID(flowContext) ; 
			}else { 
				resource = this.getResourceByNaturalID(flowContext);   
			}//EO else if internal id was not provided
				
			return resource ;
		}//EOM
		
		private static final ResourceTypeStrategy[] cachedValues ; 
		private static final int iNoOfStrategies ;
		
		private int appdefEntityType ;  
		
		static{ 
			cachedValues = values() ; 
			iNoOfStrategies = cachedValues.length ; 
		}//EO static block
		
		private ResourceTypeStrategy(final int appdefEntityType) { 
			this.appdefEntityType = appdefEntityType ; 
		}//EOM 
		
		static final ResourceTypeStrategy valueOf(final int iStrategyType) { 
			return (iStrategyType >= iNoOfStrategies ? RESOURCE : cachedValues[iStrategyType]) ; 
		}//EOM 
		
		static final ResourceTypeStrategy valueOf(final ResourceType enumResourceType) {
			return (enumResourceType == null ? RESOURCE : valueOf(enumResourceType.name()) ) ; 
		}//EOM 
		
		static final String getResourceIdentifier(final Resource resource) {
			final String resourceIdentifier = resource.getId() ; 
			return (resourceIdentifier == null || resourceIdentifier.isEmpty() ? resource.getNaturalID() : resourceIdentifier) ; 
		}//EOM 
	
		
	}//EOE 
	
	
	private final Resource getAIResource(final String platformNaturalID, final ResourceType resourceType, 
			final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) { 
		//TODO: NYI 
		return null ; 
	}//EOM 
	
	
	final static class Context  { 
		org.hyperic.hq.authz.server.session.Resource backendResource ; 
		AppdefEntityID entityID ; 
		AuthzSubject subject ;
		ConfigSchemaAndBaseResponse[] configResponses ; 
		Properties cprops ; 
		
		ResourceTransferImpl visitor ; 
		String internalID ;  
		String naturalID ; 
		ResourceType resourceType ;  
		Set<ResourceDetailsTypeStrategy> resourceDetails ;  
		Resource currResource ;
		//Resource resourceRoot ; 
		
		Context(final AuthzSubject subject, final String naturalID, final ResourceType resourceType, final ResourceDetailsType[] responseMetadata, final ResourceTransferImpl visitor)  { 
			this(subject, null/*internalID*/,responseMetadata, visitor) ;  
			this.naturalID = naturalID ; 
			this.resourceType = resourceType ; 
		}//EOM
		
		Context(final AuthzSubject subject, final String internalID, final ResourceDetailsType[] responseMetadata, final ResourceTransferImpl visitor)  {
			this(subject, visitor) ;
			this.internalID  = internalID;  
			this.resourceDetails = ResourceDetailsTypeStrategy.valueOf(responseMetadata) ; 
		}//EOM 
		
		Context(final AuthzSubject subject, final ResourceTransferImpl visitor) {
			this.subject = subject ; 
			this.visitor = visitor ; 
			this.configResponses = new ConfigSchemaAndBaseResponse[ProductPlugin.CONFIGURABLE_TYPES.length] ; 
		}//EOM
		
		public final void setBackendResource(final org.hyperic.hq.authz.server.session.Resource backendResource ) {
			this.backendResource  = backendResource  ;
			this.internalID = backendResource.getId() + "" ; 
		}//EOM 
		
		public final void setInputResource(final Resource inputResource)  {
			this.currResource = inputResource ; 
			this.internalID = inputResource.getId() ; 
			this.naturalID = inputResource.getNaturalID() ; 
		}//EOM 
		
		public final void reset() { 
			this.backendResource  = null ;
			this.internalID = null ;  
			this.entityID = null ;
			this.cprops = null ; 
			this.configResponses = new ConfigSchemaAndBaseResponse[ProductPlugin.CONFIGURABLE_TYPES.length] ;
			this.resourceType = null ; 
			this.naturalID = null ; 
			this.currResource = null  ;
		}//EOM 
		
	}//EO inner class Context 

	
}//EOC 
