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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.api.model.ConfigurationTemplate;
import org.hyperic.hq.api.model.ConfigurationValue;
import org.hyperic.hq.api.model.MetricTemplate;
import org.hyperic.hq.api.model.PropertyList;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.common.ExternalRegistrationStatus;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.model.measurements.HttpEndpointDefinition;
import org.hyperic.hq.api.model.resources.ComplexIp;
import org.hyperic.hq.api.model.resources.RegisteredResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceFilterRequest;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.NotificationsTransfer;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.ConfigurationTemplateMapper;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.MetricTemplateMapper;
import org.hyperic.hq.api.transfer.mapping.ResourceDetailsTypeStrategy;
import org.hyperic.hq.api.transfer.mapping.ResourceMapper;
import org.hyperic.hq.api.transfer.mapping.UnknownEndpointException;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.appdef.shared.IpManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.Property;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.DefaultEndpoint;
import org.hyperic.hq.notifications.HttpEndpoint;
import org.hyperic.hq.notifications.NotificationEndpoint;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilterChain;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.filtering.ResourceDestinationEvaluator;
import org.hyperic.hq.notifications.model.InventoryNotification;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.util.Transformer;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.transaction.annotation.Transactional;

public class ResourceTransferImpl implements ResourceTransfer {
    
    private static final Log log = LogFactory.getLog(ResourceTransferImpl.class);
    
    private static final String IP_MAC_ADDRESS_KEY = "IP_MAC_ADDRESS" ; 


	private ResourceManager resourceManager ; 
    private ResourceMapper resourceMapper;
    private ProductBoss productBoss ;
    private CPropManager cpropManager ;
    private AppdefBoss appdepBoss ;
    private PlatformManager platformManager ; 
    private ExceptionToErrorCodeMapper errorHandler ;
    private ResourceDestinationEvaluator evaluator;
    private ConfigManager configManager;
    private IpManager ipManager;
    protected NotificationsTransfer notificationsTransfer;
    protected ProductManager productManager;
    protected MeasurementManager measurementManager;
    protected ConfigurationTemplateMapper configTemplateMapper;
    @Autowired
    protected MetricTemplateMapper metricTemplateMapper;    
    protected PermissionManager permissionManager;

    @Autowired
    public ResourceTransferImpl(ResourceManager resourceManager, ResourceMapper resourceMapper,
                                ProductBoss productBoss, CPropManager cpropManager, AppdefBoss appdepBoss, 
                                PlatformManager platformManager, final ConfigurationTemplateMapper configTemplateMapper, ExceptionToErrorCodeMapper errorHandler,
                                ResourceDestinationEvaluator evaluator, ConfigManager configManager,
                                IpManager ipManager, ProductManager productManager, MeasurementManager measurementManager) {
    	this.resourceManager = resourceManager ;
    	this.resourceMapper = resourceMapper ; 
    	this.productBoss = productBoss ; 
    	this.cpropManager = cpropManager ; 
    	this.appdepBoss = appdepBoss ;
    	this.platformManager = platformManager ; 
    	this.configTemplateMapper = configTemplateMapper;
    	this.errorHandler = errorHandler ; 
    	this.evaluator = evaluator;
        this.configManager = configManager;
        this.ipManager = ipManager;
        this.productManager = productManager;
        this.measurementManager = measurementManager;
        this.permissionManager = PermissionManagerFactory.getInstance();
    }//EOM

    @PostConstruct
    public void init() {
        this.notificationsTransfer = (NotificationsTransfer) Bootstrap.getBean("notificationsTransfer");
    }
    
	public final ResourceModel getResource(ApiMessageContext messageContext, final String platformNaturalID, final ResourceTypeModel resourceType, 
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
	public final ResourceModel getResource(ApiMessageContext messageContext, final String platformID, final ResourceStatusType resourceStatusType, final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) throws ObjectNotFoundException {
	    AuthzSubject authzSubject = messageContext.getAuthzSubject();
		if(resourceStatusType == ResourceStatusType.AUTO_DISCOVERED) { 
			throw new UnsupportedOperationException("AI Resource load by internal ID is unsupported") ; 
		}else { 
			return this.getResourceInner(new Context(authzSubject, platformID, responseMetadata, this), hierarchyDepth) ;
		}//EO else if approved resource type 
		 
	}//EOM 
	

	/**
     * @param hierarchyDepth
	 * @return
	 * @throws ObjectNotFoundException 
	 */
	private final ResourceModel getResourceInner(final Context flowContext, int hierarchyDepth) throws ObjectNotFoundException { 
		
		ResourceModel currentResource =  null ; 
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
			if(--hierarchyDepth > 0) {
				//populate the resource's children 
				final List<Resource> listBackendResourceChildren =
								this.resourceManager.findChildren(flowContext.subject, flowContext.backendResource) ; 
				
				ResourceModel resourceChild = null ; 
				for(Resource backendResourceChild : listBackendResourceChildren) { 
					flowContext.reset() ; 
					
					//set the child in the flow's backend resource 
					flowContext.setBackendResource(backendResourceChild) ; 
					flowContext.resourceType = ResourceTypeModel.valueOf(backendResourceChild.getResourceType().getAppdefType())  ; 
					resourceChild = this.getResourceInner(flowContext, hierarchyDepth) ;
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
	
	
	@Transactional(readOnly=false)
	public final ResourceBatchResponse updateResources(ApiMessageContext messageContext, final Resources resources) {

		final ResourceBatchResponse response = new ResourceBatchResponse(this.errorHandler) ; 
		
		//iterate over the resources and for each resource, update the following metadata if exists and changed:
		//TODO: if no resources where provided (null or empty) should an excpetion be thrown? 
		final List<ResourceModel> resourcesList = resources.getResources() ; 
		if(resourcesList == null) return response ; 
		String resourceID = null ;
		final int noOfInputResources = resourcesList.size() ; 
		ResourceModel inputResource = null ; 
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
		
		final Resource resource = flowContext.backendResource  ; 
		final AppdefEntityID entityID = flowContext.entityID = AppdefUtil.newAppdefEntityId(resource) ;  
		
		//load existing resource config data 
		this.initResourceConfig(flowContext) ; 
		
		//merge cprops 
		final Resource prototype = resource.getPrototype() ; 
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
	throws ConfigFetchException, EncodingException, PluginNotFoundException, PluginException, PermissionException,
           AppdefEntityNotFoundException {
		
		final int iNoOfConfigTypes = ProductPlugin.CONFIGURABLE_TYPES.length ;
		ConfigSchemaAndBaseResponse configMetadata = null ; 
		
		//load all resource related config metadata&data resources
		String configurableType = null ; 
		for(int i=0; i < iNoOfConfigTypes; i++) { 
			configurableType = ProductPlugin.CONFIGURABLE_TYPES[i] ; 
			try {
				flowContext.configResponses[i] = configMetadata = productBoss.getConfigSchemaAndBaseResponse(
				    flowContext.subject, flowContext.entityID, configurableType, false/*validateFlow*/) ; 
				//init the schema map in the config response so as to support key recognition validation 
				configMetadata.getResponse().setSchema(configMetadata.getSchema()) ; 
			}catch(PluginNotFoundException pnfe) { 
				log.debug("Plugin Config Schema of type: " + configurableType + " was not defined for resource " + flowContext.entityID) ;	
			}//EO catch block 
			// XXX why are we catching an NPE???
			catch(NullPointerException npe) { 
				npe.printStackTrace() ; 
			}
		}//EO while there are more configurable types
		
		//load all cprop metadata
		final Resource prototype = flowContext.backendResource.getPrototype() ; 
		
		flowContext.cprops = cpropManager.getEntries(flowContext.entityID) ; 
		
		//TODO: pojo members data 
		
        return null ; 
	}//EOM 
		
    public ConfigurationTemplate getConfigurationTemplate(ApiMessageContext apiMessageContext, String resourceID)
            throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
            ConfigFetchException, PermissionException {

        final ResourceDetailsType[] detailsType = { ResourceDetailsType.BASIC };

        final AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();
        final Integer sessionId = apiMessageContext.getSessionId();

        final Resource resource = ResourceTypeStrategy.RESOURCE
                .getResourceByInternalID(new Context(authzSubject, resourceID, detailsType, this));

        final int numConfigTypes = ProductPlugin.CONFIGURABLE_TYPES.length;

        ConfigurationTemplate configTemplate = null;
        // load all prototype related config metadata
        String configurableType = null;
        for(int i = 0;i < numConfigTypes;i++) {
            configurableType = ProductPlugin.CONFIGURABLE_TYPES[i];
            try {
                if(resourceIsPrototype(resource)) {
                    final String protototypeName = resource.getName();

                    final Map<String, ConfigSchema> configurations = this.productBoss.getConfigSchemas(protototypeName,
                            configurableType);
                    // Add to the existing configTemplate
                    configTemplate = this.configTemplateMapper.toConfigurationTemplate(configurations,
                            configurableType, configTemplate);
                }else {// if resource is not a prototype

                    final AppdefEntityID entityID = AppdefUtil.newAppdefEntityId(resource);
                    
                    try {
                        ConfigSchema config = this.productBoss.getConfigSchema(sessionId, entityID, configurableType);
                        // Add to the existing configTemplate
                        configTemplate = this.configTemplateMapper.toConfigurationTemplate(config, configurableType,
                                configTemplate);
                    } catch(EncodingException e) {
                        throw new ConfigFetchException(e, ProductPlugin.CONFIGURABLE_TYPES[i], entityID);
                    }

                }// EO if resource is not a prototype
            }catch(PluginException e) {
                // not all plugins have all config types
                log.debug("Plugin config not found for config type " + configurableType, e);
            }
        }// EO while there are more configTypes
        return configTemplate;
    }// EOM
    
    public List<MetricTemplate> getMetricTemplates(ApiMessageContext apiMessageContext, String resourceID) 
            throws ObjectNotFoundException, SessionTimeoutException, SessionNotFoundException, PermissionException {
        final ResourceDetailsType[] detailsType = { ResourceDetailsType.BASIC };

        final AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();
        final Integer sessionId = apiMessageContext.getSessionId();

        final Resource resource = ResourceTypeStrategy.RESOURCE
                .getResourceByInternalID(new Context(authzSubject, resourceID, detailsType, this));  
        
        Collection<MeasurementTemplate> measurementTemplates;
        if(resourceIsPrototype(resource)) {
            measurementTemplates = measurementManager.getTemplatesByPrototype(resource);
        } else {// if resource is not a prototype
            throw new UnsupportedOperationException("Currently this operation is supported for resource prototypes only.");
        }// EO if resource is not a prototype
        List<MetricTemplate> metricTemplates = this.metricTemplateMapper.toMetricTemplates(resource, measurementTemplates);
        return metricTemplates;
    }// EOM
    
    private boolean resourceIsPrototype(final org.hyperic.hq.authz.server.session.Resource resource) {

            String name = resource.getResourceType().getName();

            return AuthzConstants.platformPrototypeTypeName.equals(name) ||
                AuthzConstants.serverPrototypeTypeName.equals(name) ||
                AuthzConstants.servicePrototypeTypeName.equals(name);

    }//EOM
	
	
	public enum ResourceTypeStrategy { 
		
		PLATFORM(AppdefEntityConstants.APPDEF_TYPE_PLATFORM, PlatformValue.class) { 
			
			@Override
			final Resource getResourceByNaturalID(final Context flowContext) throws PlatformNotFoundException, PermissionException {
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
		Resource getResourceByNaturalID(final Context flowContext) throws Exception{ 
			throw new UnsupportedOperationException("Resource Of Type " + this.name() + " does not support find by natural ID") ; 
		}//EOM 
		
		Resource getResourceByInternalID(final Context flowContext) { 
			final int iInternalResourceID = Integer.parseInt(flowContext.internalID) ; 
			return flowContext.visitor.getResourceManager().findResourceById(iInternalResourceID) ; 
		}//EOM
		
		Resource getResource(final Context flowContext) throws Exception{ 
			Resource resource = null ;  
			
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
                }
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
		
		static final ResourceTypeStrategy valueOf(final ResourceTypeModel enumResourceType) {
			return (enumResourceType == null ? RESOURCE : valueOf(enumResourceType.name()) ) ; 
		}//EOM 
		
		static final String getResourceIdentifier(final ResourceModel resource) {
			final String resourceIdentifier = resource.getId() ; 
			return (resourceIdentifier == null || resourceIdentifier.isEmpty() ? resource.getNaturalID() : resourceIdentifier) ; 
		}//EOM 
	
		
	}//EOE 
	
	private final ResourceModel getAIResource(final String platformNaturalID, final ResourceTypeModel resourceType, 
			final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) { 
		//TODO: NYI 
		return null ; 
	}//EOM 
	
	
	public final static class Context  { 
	    public Resource backendResource ; 
	    public AppdefResource resourceInstance ; 
		public AppdefEntityID entityID ;
		public AuthzSubject subject ;
		public ConfigSchemaAndBaseResponse[] configResponses ; 
		public Properties cprops ; 
		public ResourceTypeStrategy resourceTypeStrategy ; 
		
		public ResourceTransfer visitor ; 
		public String internalID ;  
		public String naturalID ; 
		public ResourceTypeModel resourceType ;  
		public Set<ResourceDetailsTypeStrategy> resourceDetails ;  
		public ResourceModel currResource ;
		//Resource resourceRoot ; 
		
		public Context(final AuthzSubject subject, final String naturalID, final ResourceTypeModel resourceType, final ResourceDetailsType[] responseMetadata, final ResourceTransfer visitor)  { 
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
		
		public final void setBackendResource(final Resource backendResource ) {
			this.backendResource  = backendResource  ;
			this.internalID = backendResource.getId() + "" ; 
		}//EOM 
		
		public final void setInputResource(final ResourceModel inputResource)  {
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
    public RegisteredResourceBatchResponse getResources(ApiMessageContext messageContext,
                                                        ResourceDetailsType[] responseMetadata,
                                                        int hierarchyDepth)
    throws PermissionException, NotFoundException {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        final RegisteredResourceBatchResponse res = new RegisteredResourceBatchResponse(errorHandler);
        if (responseMetadata==null) {
            log.warn("illegal request");
            throw errorHandler.newWebApplicationException(
                Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.BAD_REQ_BODY);
        }
        final List<ResourceDetailsType> responseMetadataList = Arrays.asList(responseMetadata);
        if (hierarchyDepth < 0) {
            log.warn("hierarchy Depth cannot be < 0");
            throw errorHandler.newWebApplicationException(
                Response.Status.NOT_ACCEPTABLE, ExceptionToErrorCodeMapper.ErrorCode.BAD_REQ_BODY);
        }
        final AuthzSubject authzSubject = messageContext.getAuthzSubject();        
        if (debug) watch.markTimeBegin("findViewablePSSResources");
        final Set<Integer> viewable = permissionManager.findViewablePSSResources(authzSubject);
        if (debug) watch.markTimeEnd("findViewablePSSResources");
        final List<Resource> platformResources = getPlatformsFromResourceIds(viewable);
        if (debug) watch.markTimeBegin("getResourceToChildren");
        final Map<Resource, Collection<Resource>> resourceToChildren =
            getResourceToChildren(viewable, platformResources, hierarchyDepth);
        if (debug) watch.markTimeEnd("getResourceToChildren");
        if (debug) watch.markTimeBegin("getResourceConfig");
        final Map<Resource, ConfigResponse> config =
            getResourceConfig(authzSubject, responseMetadataList, resourceToChildren.keySet());
        if (debug) watch.markTimeEnd("getResourceConfig");
        final Map<Resource, Collection<Ip>> ipInfo = getIpInfo(responseMetadataList, platformResources);
        if (debug) watch.markTimeBegin("getResourceConfigProps");
        final Map<AppdefEntityID, Properties> cProps =
            getResourceConfigProps(responseMetadataList);
        if (debug) watch.markTimeEnd("getResourceConfigProps");
        final List<ResourceModel> resources = new ArrayList<ResourceModel>(platformResources.size());
        for (final Resource platformResource : platformResources) {
            ResourceModel model = resourceMapper.toResource(platformResource);
            resources.add(model);
            setAllChildren(model, platformResource, resourceToChildren, config, cProps, ipInfo);
        }
        res.setResources(resources);
        if (debug) log.debug(watch);
        return res;
    }

    private Map<AppdefEntityID, Properties> getResourceConfigProps(List<ResourceDetailsType> responseMetadataList) {
        if (!responseMetadataList.contains(ResourceDetailsType.VIRTUALDATA) &&
            !responseMetadataList.contains(ResourceDetailsType.ALL)) {
            return Collections.emptyMap();
        }
        return cpropManager.getAllEntries(HQConstants.VCUUID, HQConstants.MOID);
    }
    
    private Map<Resource, Collection<Ip>> getIpInfo(List<ResourceDetailsType> responseMetadataList,
                                                    Collection<Resource> platformResources) {
        if (!responseMetadataList.contains(ResourceDetailsType.PROPERTIES) &&
            !responseMetadataList.contains(ResourceDetailsType.ALL)) {
            return Collections.emptyMap();
        }
        return ipManager.getIps(platformResources);
    }

    private Map<Resource, ConfigResponse> getResourceConfig(AuthzSubject subject, List<ResourceDetailsType> responseMetadataList,
                                                            Set<Resource> resources) {
        if (!responseMetadataList.contains(ResourceDetailsType.PROPERTIES) &&
            !responseMetadataList.contains(ResourceDetailsType.ALL)) {
            return Collections.emptyMap();
        }
        return configManager.getConfigResponses(resources, true);
    }

    private void setAllChildren(ResourceModel model, Resource platformResource,
                                Map<Resource, Collection<Resource>> resourceToChildren,
                                Map<Resource, ConfigResponse> config, Map<AppdefEntityID, Properties> cProps,
                                Map<Resource, Collection<Ip>> ipInfo) {
        Collection<Resource> tmp;
        addResourceConfig(platformResource, model, config, cProps, ipInfo);
        if (null == (tmp = resourceToChildren.get(platformResource)) || tmp.isEmpty()) {
            return;
        }
        for (final Resource child : tmp) {
            final ResourceModel childModel = resourceMapper.toResource(child);
            model.addSubResource(childModel);
            setAllChildren(childModel, child, resourceToChildren, config, cProps, ipInfo);
        }
    }

    private void addResourceConfig(Resource r, ResourceModel resourceModel, Map<Resource, ConfigResponse> configMap,
                                   Map<AppdefEntityID, Properties> cProps, Map<Resource, Collection<Ip>> ipInfo) {
        final ConfigResponse configResponse = configMap.get(r);
        @SuppressWarnings("unchecked")
        final Map<String, String> config = (configResponse == null) ?
            new HashMap<String, String>() : configResponse.getConfig();
        final AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(r);
        Properties properties = cProps.get(aeid);
        properties = (properties == null) ? new Properties() : properties;
        Object prop = properties.get(HQConstants.VCUUID);
        if (prop != null) {
            config.put(HQConstants.VCUUID, prop.toString());
        }
        prop = properties.get(HQConstants.MOID);
        if (prop != null) {
            config.put(HQConstants.MOID, prop.toString());
        }
        ResourceConfig resourceConfig = resourceModel.getResourceConfig();
        resourceConfig = (resourceConfig == null) ? new ResourceConfig() : resourceConfig;
        resourceConfig.setMapProps(config);
        resourceModel.setResourceConfig(resourceConfig);
        final Collection<Ip> ips = ipInfo.get(r);
        if (ips != null && !ips.isEmpty()) {
            Collection<ConfigurationValue> ipValues = new ArrayList<ConfigurationValue>(ips.size());
            for (Ip ip : ips) {
                ipValues.add(new ComplexIp(ip.getNetmask(), ip.getMacAddress(), ip.getAddress()));
            }
            resourceConfig.putMapListProps(IP_MAC_ADDRESS_KEY, ipValues);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Resource, Collection<Resource>> getResourceToChildren(Set<Integer> viewable,
	                                                                  List<Resource> platformResources,
	                                                                  int hierarchyDepth) {
        final List<Resource> currResources = new ArrayList<Resource>(platformResources);
        final Map<Resource, Collection<Resource>> rtn = new HashMap<Resource, Collection<Resource>>();
        for (Resource r : platformResources) {
            rtn.put(r, Collections.EMPTY_LIST);
        }
        final Map<Resource, Resource> systemResources = new HashMap<Resource, Resource>();
        for (int i=2; i<=hierarchyDepth; i++) {
            final Map<Resource, Collection<Resource>> childResources =
                resourceManager.findChildResources(currResources, viewable, true);
            rtn.putAll(childResources);
            currResources.removeAll(childResources.keySet());
            for (Resource r : currResources) {
                rtn.put(r, new ArrayList<Resource>(0));
            }
            currResources.clear();
            for (final Entry<Resource, Collection<Resource>> entry : childResources.entrySet()) {
                final Resource parent = entry.getKey();
                final Collection<Resource> children = entry.getValue();
                for (final Iterator<Resource> it=children.iterator(); it.hasNext(); ) {
                    final Resource child = it.next();
                    currResources.add(child);
                    // EMPTY_LIST usage is a place holder to avoid extra overhead where resources don't have child
                    // resources
                    rtn.put(child, Collections.EMPTY_LIST);
                    // remove place-holder resource so that Platform Services don't expose our internal implementation
                    // rather than exposing the system resource which is a place-holder
                    // In other words:
                    // desired output is "Platform --> Services"
                    // in HQ we store this relationship as "Platform --> System Server --> Services"
                    if (child.isSystem()) {
                        systemResources.put(child, parent);
                        it.remove();
                    }
                }
            }
        }
        // need to post process the data in order to map the platform services correctly
        final List<Resource> toFetch = new ArrayList<Resource>();
        for (final Iterator<Entry<Resource, Resource>> it=systemResources.entrySet().iterator(); it.hasNext(); ) {
            final Entry<Resource, Resource> entry = it.next();
            final Resource systemResource = entry.getKey();
            final Resource parent = entry.getValue();
            final Collection<Resource> children = rtn.remove(systemResource);
            if (children != null && !children.isEmpty()) {
                it.remove();
                final Collection<Resource> collection = rtn.get(parent);
                if (collection != null) {
                    collection.addAll(children);
                }
            } else if (children != null && children == Collections.EMPTY_LIST) {
                toFetch.add(systemResource);
            }
        }
        final Map<Resource, Collection<Resource>> systemResourceChildren =
            resourceManager.findChildResources(toFetch, viewable, true);
        for (final Entry<Resource, Resource> entry : systemResources.entrySet()) {
            final Resource systemResource = entry.getKey();
            final Resource parent = entry.getValue();
            final Collection<Resource> children = systemResourceChildren.get(systemResource);
            final Collection<Resource> parentChildren = rtn.get(parent);
            if (parentChildren != null && children != null) {
                parentChildren.addAll(children);
            }
        }
        return rtn;
    }

    private List<Resource> getPlatformsFromResourceIds(Set<Integer> viewable) {
        return new Transformer<Integer, Resource>() {
            public Resource transform(Integer resourceId) {
                final Resource r = resourceManager.getResourceById(resourceId);
                if (r != null && !r.isInAsyncDeleteState() && AuthzConstants.authzPlatform.equals(r.getResourceType()
                        .getId())) {
                    return r;
                }
                return null;
            }
        }.transform(viewable);
    }

    @Transactional (readOnly=true)
    public RegistrationID register(ApiMessageContext messageContext, ResourceDetailsType responseMetadata,
                                   ResourceFilterRequest resourceFilterRequest)
                                           throws PermissionException, NotFoundException {
        AuthzSubject authzSubject = messageContext.getAuthzSubject();
        this.permissionManager.checkIsSuperUser(authzSubject);
        List<Filter<InventoryNotification,? extends FilteringCondition<?>>> userFilters =
                resourceMapper.toResourceFilters(resourceFilterRequest, responseMetadata);

        RegistrationID registrationID = new RegistrationID();
        final HttpEndpointDefinition httpEndpointDefinition = resourceFilterRequest.getHttpEndpointDef();
        final NotificationEndpoint endpoint = (httpEndpointDefinition == null) ? new DefaultEndpoint(registrationID
                .getId()) : getHttpEndpoint(registrationID, httpEndpointDefinition);
        final Integer authzSubjectId = authzSubject.getId();
        notificationsTransfer.register(endpoint, ResourceDetailsType.valueOf(responseMetadata), authzSubjectId);
        evaluator.register(endpoint, userFilters);
        return registrationID;
    }

    public ExternalRegistrationStatus getRegistrationStatus(final ApiMessageContext messageContext,
            final String registrationID) throws PermissionException,NotFoundException, UnknownEndpointException{
        AuthzSubject authzSubject = messageContext.getAuthzSubject();
        this.permissionManager.checkIsSuperUser(authzSubject);
        FilterChain<InventoryNotification> filterChain = evaluator.getRegistration(registrationID);
        NotificationsTransferImpl.EndpointStatusAndDefinition endpointStatusAndDefinition = this.notificationsTransfer.getEndointStatus(registrationID);
        return new ExternalRegistrationStatus(endpointStatusAndDefinition.getEndpoint(),filterChain, registrationID, endpointStatusAndDefinition.getExternalEndpointStatus());
    }

    public void unregister(final ApiMessageContext messageContext,NotificationEndpoint endpoint) throws PermissionException {
        AuthzSubject authzSubject = messageContext.getAuthzSubject();
        this.permissionManager.checkIsSuperUser(authzSubject);
        evaluator.unregisterAll(endpoint);
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

    private HttpEndpoint getHttpEndpoint(RegistrationID registrationID, HttpEndpointDefinition def) {
        return new HttpEndpoint(registrationID.getId(), def.getUrl(), def.getUsername(), def.getPassword(),
                def.getContentType(), def.getEncoding(), def.getBodyPrepend());
    }
}//EOC 
