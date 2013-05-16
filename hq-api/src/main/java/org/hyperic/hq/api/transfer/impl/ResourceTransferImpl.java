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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.jms.Destination;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.api.model.PropertyList;
import org.hyperic.hq.api.model.ConfigurationTemplate;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.resources.RegisteredResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceFilterRequest;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.NotificationsTransfer;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.ConfigurationTemplateMapper;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.ResourceDetailsTypeStrategy;
import org.hyperic.hq.api.transfer.mapping.ResourceMapper;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.Property;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.Q;
import org.hyperic.hq.notifications.filtering.AgnosticFilter;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.filtering.ResourceContentFilter;
import org.hyperic.hq.notifications.filtering.ResourceDestinationEvaluator;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.hyperic.hq.notifications.model.InventoryNotification;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.transaction.annotation.Transactional;

public class ResourceTransferImpl implements ResourceTransfer{

    private static final String IP_MAC_ADDRESS_KEY = "IP_MAC_ADDRESS" ; 

    private AIQueueManager aiQueueManager;
	private ResourceManager resourceManager ; 
    private AuthzSubjectManager authzSubjectManager;
    private ResourceMapper resourceMapper;
    private ProductBoss productBoss ;
	private CPropManager cpropManager ;
	private AppdefBoss appdepBoss ;
	private PlatformManager platformManager ; 
	private ConfigurationTemplateMapper configTemplateMapper;
	private ExceptionToErrorCodeMapper errorHandler ;
	private Log log ;
    private ResourceDestinationEvaluator evaluator;
    private Q q;
    protected NotificationsTransfer notificationsTransfer;
    protected boolean isRegistered = false;
	@Autowired  
    public ResourceTransferImpl(final AIQueueManager aiQueueManager, final ResourceManager resourceManager, 
    		final AuthzSubjectManager authzSubjectManager, final ResourceMapper resourceMapper, 
    		final ProductBoss productBoss, final CPropManager cpropManager, final AppdefBoss appdepBoss, 
    		final PlatformManager platformManager, 
    		final ConfigurationTemplateMapper configTemplateMapper, 
            final ExceptionToErrorCodeMapper errorHandler, ResourceDestinationEvaluator evaluator, Q q, @Qualifier("restApiLogger")Log log) { 
    	this.aiQueueManager = aiQueueManager ; 
    	this.resourceManager = resourceManager ; 
    	this.authzSubjectManager = authzSubjectManager ; 
    	this.resourceMapper = resourceMapper ; 
    	this.productBoss = productBoss ; 
    	this.cpropManager = cpropManager ; 
    	this.appdepBoss = appdepBoss ;
    	this.platformManager = platformManager ; 
    	this.configTemplateMapper = configTemplateMapper;
    	this.errorHandler = errorHandler ; 
    	this.evaluator = evaluator;
    	this.q=q;
    	this.log = log ;
    }//EOM 
    @PostConstruct
    public void init() {
        this.notificationsTransfer = (NotificationsTransfer) Bootstrap.getBean("notificationsTransfer");
    }
    
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
		
		int failureCounter =  0 ; 
		for(int i=0; i < noOfInputResources; i++) { 
			
			try{ 
				inputResource = resourcesList.get(i) ;
				flowContext.setInputResource(inputResource) ; 
				
				//find the backend resource using the resource Type (if null then find by internal id else natural id)
				//not need to check for null backendResource as exception should have been thrown if the resource 
				//could not have been loaded
				final ResourceTypeStrategy resourceTypeStrategy = ResourceTypeStrategy.valueOf(inputResource.getResourceType()) ;
				
				flowContext.setBackendResource(resourceTypeStrategy.getResource(flowContext)) ;
				flowContext.resourceTypeStrategy = resourceTypeStrategy ; 
				
				//map the input into the backend resource (resource metadata update
				this.resourceMapper.mergeResource(inputResource, flowContext.backendResource, flowContext) ; 
				
				//if there are properties to update do so now
				resourceConfig = inputResource.getResourceConfig() ; 
				if(resourceConfig !=  null && resourceConfig.getMapProps() != null) this.updateResourceConfig(flowContext, resourceConfig) ; 
				
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
				failureCounter++ ; 
			} finally { //EO catch block
                flowContext.reset() ; 
			}//EO catch block 
			
		}//EO while there are more resources
		
		//if the failure counter == no of input resources, raise an error rather than returning success with failed resources section 
		if(failureCounter == noOfInputResources) { 
		    throw this.errorHandler.newWebApplicationException(new Throwable(), Response.Status.INTERNAL_SERVER_ERROR,
                    ExceptionToErrorCodeMapper.ErrorCode.UPDATE_FAILURE, ". Failed to update all " + noOfInputResources + " input resources.");
		}//EO if overall update failuer 
		
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
		    this.appdepBoss.setAllConfigResponses(flowContext.subject, allConfigs, rollbackConfigs, true) ;
		}//EO if there was a change 

		//TODO: pojo fields modifications 
	}//EOM 
    public final void initResourceVirtualData(final Context flowContext)   
            throws ConfigFetchException, EncodingException, PluginNotFoundException, PluginException, PermissionException, AppdefEntityNotFoundException {
        flowContext.cprops = cpropManager.getEntries(flowContext.entityID) ;
    }

	public final Object initResourceConfig(final Context flowContext)  
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
		
    public ConfigurationTemplate getConfigurationTemplate(ApiMessageContext apiMessageContext, String resourceID) throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, ConfigFetchException, PermissionException, EncodingException, PluginException {
        
        final ResourceDetailsType[] detailsType =  { ResourceDetailsType.BASIC };
        
        final AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();
        final Integer sessionId = apiMessageContext.getSessionId();
                    
        final org.hyperic.hq.authz.server.session.Resource resource = 
                ResourceTypeStrategy.RESOURCE.getResourceByInternalID(new Context(authzSubject, resourceID, detailsType , this));
                
        final int numConfigTypes = ProductPlugin.CONFIGURABLE_TYPES.length;         
                
        ConfigurationTemplate configTemplate = null;
      //load all prototype related config metadata
        String configurableType = null; 
        for(int i=0; i < numConfigTypes; i++) { 
            configurableType = ProductPlugin.CONFIGURABLE_TYPES[i]; 
            try {
                if(resourceIsPrototype(resource)) {
                    final String protototypeName = resource.getName();

                    final Map<String, ConfigSchema> configurations = this.productBoss.getConfigSchemas(protototypeName,
                            configurableType);
                    // Add to the existing configTemplate
                    configTemplate = this.configTemplateMapper.toConfigurationTemplate(configurations, configurableType,
                            configTemplate);
                } else {//if resource is not a prototype
                    
                    final AppdefEntityID entityID = AppdefUtil.newAppdefEntityId(resource);
                    
                    final ConfigSchema config = this.productBoss.getConfigSchema(sessionId, entityID, configurableType);
                    // Add to the existing configTemplate
                    configTemplate = this.configTemplateMapper.toConfigurationTemplate(config, configurableType,
                            configTemplate);

                }// EO if resource is not a prototype
            }catch(PluginException e) {
                if (ProductPlugin.TYPE_CONTROL.equals(configurableType)) {
                    log.debug("Plugin config not found for config type " + configurableType, e);                    
                } else {// not all plugins have control config
                    throw e;
                }               
                
            }
        }// EO while there are more configTypes              
        return configTemplate;                
    }//EOM
    
    private boolean resourceIsPrototype(final org.hyperic.hq.authz.server.session.Resource resource) {

            String name = resource.getResourceType().getName();

            return AuthzConstants.platformPrototypeTypeName.equals(name) ||
                AuthzConstants.serverPrototypeTypeName.equals(name) ||
                AuthzConstants.servicePrototypeTypeName.equals(name);

    }//EOM
	
	public enum ResourceTypeStrategy { 
		
		PLATFORM(AppdefEntityConstants.APPDEF_TYPE_PLATFORM, PlatformValue.class) { 
			
			@Override
			final org.hyperic.hq.authz.server.session.Resource getResourceByNaturalID(final Context flowContext) throws PlatformNotFoundException, PermissionException {
				final Platform platform = flowContext.visitor.getPlatformManager().findPlatformByFqdn(flowContext.subject, flowContext.naturalID);  
				flowContext.resourceInstance = platform  ;
				return platform.getResource() ;  
			}//EOM 

			@Override
			protected final AppdefResourceValue loadExistingDTO(final AppdefBoss appdefBoss, final Context flowContext) throws Exception{
			    Platform platform = (Platform) flowContext.resourceInstance ; 
			    
			    if(platform == null) {  
			        flowContext.resourceInstance = platform = flowContext.getVisitor().getPlatformManager().
			                                findPlatformById(flowContext.backendResource.getInstanceId()) ; 
			    }//EO else if platform was not yet loaded
			    
			    final PlatformValue platformDTO = new PlatformValue() ; 
			    platformDTO.setId(platform.getId()) ; 
			    platformDTO.setFqdn(platform.getFqdn()) ;
			    platformDTO.setName(platform.getName()) ;  
			    return platformDTO ; 
			}//EOM 
	        
			@Override
	        protected final AppdefResourceValue mergeResource(final Map<String,String> inputProps,  Map<String,PropertyList> oneToManyProps, 
	                AppdefResourceValue existingDTO, final Context flowContext) throws Exception { 
	            
	            PropertyList requetsIps = null ;
                
                if( (requetsIps = oneToManyProps.get(IP_MAC_ADDRESS_KEY)) != null) {
                    if(requetsIps != null) { 
                        //else determine whether the ips have changed
                        Platform platform =  (Platform) flowContext.resourceInstance ; 
                                               
                        existingDTO = flowContext.visitor.getResourceMapper().mergePlatformIPs(existingDTO, requetsIps.getProperties(), platform) ; 
                        
                    }//EO if ips were provided 
                }//EO if propertyList was defined 
                
                return existingDTO ; 
	        }//EOM 
	        
	        @Override
	        protected final void updateResourceInstance(final AppdefResourceValue newDTO, 
	                    final AppdefBoss appdefBoss, final AuthzSubject subject) throws Exception{ 
	            appdefBoss.updatePlatform(subject, (PlatformValue) newDTO) ; 
	        }//EOM 
	        
			
		},//EO PLATFORM
		SERVER(AppdefEntityConstants.APPDEF_TYPE_SERVER, ServerValue.class) {
		   
		    @Override
		    protected final AppdefResourceValue loadExistingDTO(final AppdefBoss appdefBoss, final Context flowContext) throws Exception {
		        AppdefResourceValue existingDTO = null ; 
		        
		        if(flowContext.resourceInstance == null) { 
		            final Integer serverId = flowContext.backendResource.getInstanceId() ; 
		            existingDTO = appdefBoss.findById(flowContext.subject, AppdefEntityID.newServerID(serverId));
		        }else { 
		            existingDTO = ((Server)flowContext.resourceInstance).getServerValue() ; 
		        }//EO else if the backend resource was loaded 
		        
		        return existingDTO ; 
		    }//EOM 
	        
	        protected final void updateResourceInstance(final AppdefResourceValue newDTO, final AppdefBoss appdefBoss, final AuthzSubject subject) throws Exception { 
	            appdefBoss.updateServer(subject, (ServerValue)newDTO, null/*cprops*/) ;  
	        }//EOM 

		    
		},//EO SERVER 
		SERVICE(AppdefEntityConstants.APPDEF_TYPE_SERVICE, ServiceValue.class) {
		    protected AppdefResourceValue loadExistingDTO(final AppdefBoss appdefBoss, final Context flowContext) throws Exception {  
		        final Integer serviceId = flowContext.backendResource.getInstanceId() ; 
                return appdefBoss.findById(flowContext.subject, AppdefEntityID.newServiceID(serviceId)) ;  
	        }//EOM 
		    
		    protected final void updateResourceInstance(final AppdefResourceValue newDTO, final AppdefBoss appdefBoss, final AuthzSubject subject) throws Exception { 
                appdefBoss.updateService(subject, (ServiceValue)newDTO, null/*cprops*/) ;  
            }//EOM
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
			return flowContext.visitor.getResourceManager().findResourceById(iInternalResourceID) ; 
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
		
		public void mergeResource(final Context flowContext) throws Exception {
		    final ResourceConfig resourceConfig = flowContext.currResource.getResourceConfig() ; 
		    if(this.clsAppdefResourceValue == null || resourceConfig == null) return ;
		    
		    final AppdefBoss appdefBoss = flowContext.getVisitor().getAppdefBoss() ; 
		    
		    //else if the resource config section was provided in the request
		    AppdefResourceValue existingDTO = null ;  
		    final Map<String,String> inputProps = resourceConfig.getMapProps() ;
		    
		    if(inputProps != null) { 

		        //load the existing AppdefResourceValue 
		        existingDTO = this.loadExistingDTO(appdefBoss, flowContext) ;
		                        
                String value = null ; 
                Object oValue = null ;;
                Object[] metadata = null ;  
                Method method = null ; 
                
                for(Map.Entry<String,Object[]> entry : this.cachedPropertiesMutators.entrySet()) {
                    
                    if((value = inputProps.get(entry.getKey()))  != null ) {
                        metadata = entry.getValue() ; 
                        method = (Method) metadata[0] ; 
                        
                        //convert the value into the target type 
                        oValue = conversionService.convert(value, STRING_DESCRIPTOR, (TypeDescriptor)metadata[1]); 
                        method.invoke(existingDTO, oValue) ;
                        
                    }//EO if input was provided for the given attribute  
                }//EO while there are more attributes to check 
                
		    }//EO if the inputProps was not null 
		    	    
		    //now check whether there are multi-property section in the request and if 
		    //so delegate to sub-classes 
		    final Map<String,PropertyList> oneToManyProps = resourceConfig.getMapListProps() ; 
		    if(oneToManyProps != null) {
		        
		        //if the existingDTO was not yet loaded (i.e. no one-one properties 
		        //do so now 
		        if(existingDTO == null) { 
		            existingDTO = this.loadExistingDTO(appdefBoss, flowContext) ;
		        }//EO the dtos were not yet initialized 
		        
		        this.mergeResource(inputProps, oneToManyProps, existingDTO, flowContext) ; 
		    }//EO if there were oneToManyProps for the given resource in the request
		    
		    
		    //finally, if a DTO was created (i.e. potential modifications were found) 
		    //invoke the respective udpateXXX 
		    if(existingDTO != null) this.updateResourceInstance(existingDTO, appdefBoss, flowContext.subject); 
		    
		}//EOM
		
		protected AppdefResourceValue loadExistingDTO(final AppdefBoss appdefBoss, final Context flowContext) throws Exception {  
		    throw new UnsupportedOperationException() ; 
        }//EOM 
		
		protected AppdefResourceValue mergeResource(final Map<String,String> inputProps,  Map<String,PropertyList> oneToManyProps, 
		        final AppdefResourceValue existingDTO, final Context flowContext) throws Exception { 
		    return existingDTO ; 
		}//EOM 
		
		protected void updateResourceInstance(final AppdefResourceValue newDTO, final AppdefBoss appdefBoss, final AuthzSubject subject) throws Exception { 
		   throw new UnsupportedOperationException() ; 
		}//EOM 
		
		
		private final static GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService() ;
		private final static TypeDescriptor STRING_DESCRIPTOR = TypeDescriptor.valueOf(String.class) ; 
		private static final ResourceTypeStrategy[] cachedValues ; 
		private static final int iNoOfStrategies ;
		
		private int appdefEntityType ;  
		private Class<? extends AppdefResourceValue> clsAppdefResourceValue ;
		private Map<String,Object[]> cachedPropertiesMutators ; 
		
		
		static{ 
			cachedValues = values() ; 
			iNoOfStrategies = cachedValues.length ;
		}//EO static block
		
		private final void initCachedPropertiesMutators() { 
		    final Method[] methods = this.clsAppdefResourceValue.getDeclaredMethods() ;
		    Property propertyMetadata = null ; 
		    
		    try{ 
    		    for(Method method : methods) { 
    		        propertyMetadata = method.getAnnotation(Property.class) ;
    		        if(propertyMetadata != null) { 
    		            method.setAccessible(true) ;
    		            this.cachedPropertiesMutators.put(propertyMetadata.value(), new Object[]{ method, TypeDescriptor.valueOf(method.getParameterTypes()[0]) }) ;  
    		        }//EO if defined 
    		    }//EO while there are more declared methods
		    }catch(Throwable t) { 
		        t.printStackTrace() ; 
		        throw new Error("Failed to initialize cachedPropertiesMutators for type " + this.name() + " from class " + this.clsAppdefResourceValue, t) ; 
		    }//EO catch block 
		}//EOM 
		
		private ResourceTypeStrategy(final int appdefEntityType, final Class<? extends AppdefResourceValue> clsAppdefResourceValue) { 
		    this(appdefEntityType) ; 
		    this.clsAppdefResourceValue = clsAppdefResourceValue  ;
		    if(clsAppdefResourceValue != null) { 
		        this.cachedPropertiesMutators = new ConcurrentHashMap<String,Object[]>() ;
		        this.initCachedPropertiesMutators() ; 
		    }//EO if the clsAppdefesourceValue was not null 
		}//EOM 
		
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
	
	
	public final static class Context  { 
	    public org.hyperic.hq.authz.server.session.Resource backendResource ; 
	    public AppdefResource resourceInstance ; 
		public AppdefEntityID entityID ; 
		public AuthzSubject subject ;
		public ConfigSchemaAndBaseResponse[] configResponses ; 
		public Properties cprops ; 
		public ResourceTypeStrategy resourceTypeStrategy ; 
		
		public ResourceTransfer visitor ; 
		public String internalID ;  
		public String naturalID ; 
		public ResourceType resourceType ;  
		public Set<ResourceDetailsTypeStrategy> resourceDetails ;  
		public Resource currResource ;
		//Resource resourceRoot ; 
		
		public Context(final AuthzSubject subject, final String naturalID, final ResourceType resourceType, final ResourceDetailsType[] responseMetadata, final ResourceTransfer visitor)  { 
			this(subject, null/*internalID*/,responseMetadata, visitor) ;  
			this.naturalID = naturalID ; 
			this.resourceType = resourceType ; 
		}//EOM
		
		Context(final AuthzSubject subject, final String internalID, final ResourceDetailsType[] responseMetadata, final ResourceTransfer visitor)  {
			this(subject, visitor) ;
			this.internalID  = internalID;  
			this.resourceDetails = ResourceDetailsTypeStrategy.valueOf(responseMetadata) ; 
		}//EOM 
		
		Context(final AuthzSubject subject, final ResourceTransfer visitor) {
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
			this.resourceInstance = null ; 
			this.resourceTypeStrategy = null ; 
		}//EOM 

        public ResourceTransfer getVisitor() {
            return this.visitor;
        }
		
	}//EO inner class Context 

	@Transactional (readOnly=true)
    public RegisteredResourceBatchResponse getResources(ApiMessageContext messageContext, ResourceDetailsType responseMetadata, final int hierarchyDepth, 
            final boolean register,final ResourceFilterRequest resourceFilterRequest) throws PermissionException, NotFoundException {
        if (resourceFilterRequest==null) {
            if (log.isDebugEnabled()) {
                log.debug("illegal request");
            }
            throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.BAD_REQ_BODY);
        }
        AuthzSubject authzSubject = messageContext.getAuthzSubject();
        final RegisteredResourceBatchResponse res = new RegisteredResourceBatchResponse(this.errorHandler) ; 
        List<Resource> resources = new ArrayList<Resource>();
        PageList<PlatformValue> platforms = this.platformManager.getAllPlatforms(authzSubject, PageControl.PAGE_ALL);
        for(PlatformValue pv:platforms) {
            try {
                String fqdn = pv.getFqdn();
                Resource r = this.getResourceInner(new Context(authzSubject, fqdn, ResourceType.PLATFORM, new ResourceDetailsType[] {responseMetadata}, this), hierarchyDepth) ;  
                resources.add(r);
            } catch (Throwable t) {
//TODO~                res.addFailedResource(resourceID, errorCode, additionalDescription, args)
            }
        }
        res.setResources(resources);
        if (register) {
            // not allowing sequential registrations
            if (this.isRegistered) {
                throw errorHandler.newWebApplicationException(Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.SEQUENTIAL_REGISTRATION);
            }
            this.isRegistered=true;
            List<Filter<InventoryNotification,? extends FilteringCondition<?>>> userFilters = this.resourceMapper.toResourceFilters(resourceFilterRequest, responseMetadata); 

            //TODO~ get the destination from the user
            Destination dest = this.notificationsTransfer.getDummyDestination();
            this.q.register(dest,ResourceDetailsType.valueOf(responseMetadata));
            this.evaluator.register(dest,userFilters);
            //TODO~ return a valid registration id
            res.setRegId(new RegistrationID(1));
        }
        return res;
    }
    public void unregister() {
        Destination dest = this.notificationsTransfer.getDummyDestination();
        this.q.unregister(dest);
        this.evaluator.unregisterAll(dest);
        this.isRegistered=false;
    }
    public ResourceMapper getResourceMapper() {
        return this.resourceMapper;
    }
    public PlatformManager getPlatformManager() {
        return this.platformManager;
    }
    public ResourceManager getResourceManager() {
        return this.resourceManager;
    }

    public final AppdefBoss getAppdefBoss() { return this.appdepBoss ; }//EOM
}//EOC 
