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
package org.hyperic.hq.api.transfer.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.ConfigurationValue;
import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.PropertyList;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourcePrototype;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.model.resources.ComplexIp;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.notifications.model.CreatedResourceNotification;
import org.hyperic.hq.notifications.model.InventoryNotification;
import org.hyperic.hq.notifications.model.RemovedResourceNotification;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 
 * Mapper between automatically discovered resources in API and in core Hyperic.
 * 
 * @since   4.5.0
 * @version 1.0 29 April 2012
 * @author Maya Anderson
 */
@Component
public class ResourceMapper {
    
    private PlatformManager platformManager;
    @Autowired  
    SessionFactory f;
    @Autowired  
    public ResourceMapper(final PlatformManager platformManager) { 
        this.platformManager = platformManager;   
    }//EOM     

	public AIResource mapAIPLarformValueToAIResource(AIPlatformValue aiPlatform,
			AIResource aiResource) {
		if (null == aiPlatform) {
			return aiResource;
		}
		
		if (null == aiResource) {
			aiResource = new AIResource();
		}		
		aiResource.setNaturalID(aiPlatform.getFqdn());
		// aiResource.setId(aiPlatform.get)
		aiResource.setName(aiPlatform.getName());
		aiResource.setResourceType(ResourceType.PLATFORM);

		ResourcePrototype resourcePrototype = new ResourcePrototype();
		resourcePrototype.setName(aiPlatform.getPlatformTypeName());
		aiResource.setResourcePrototype(resourcePrototype);

		AIServerValue[] aiServerValues = aiPlatform.getAIServerValues();
		if ((null != aiServerValues) && (aiServerValues.length > 0)) {
			List<Resource> subResources = new ArrayList<Resource>(aiServerValues.length);
			for (int i = 0; i < aiServerValues.length; i++) {
				AIResource aiServer = mapAIServerValueToAIResource(aiServerValues[i], null);
				subResources.add(aiServer);
			}
			aiResource.setSubResources(subResources);
		}
		
		return aiResource;
	}//EOM 

	public AIResource mapAIServerValueToAIResource(
			AIServerValue aiServerValue, AIResource aiResource) {
		if (null == aiServerValue) {
			return aiResource;
		}
		
		if (null == aiResource) {
			aiResource = new AIResource();
		}
		
		aiResource.setNaturalID(aiServerValue.getAutoinventoryIdentifier());
		aiResource.setId(aiServerValue.getId().toString());
		aiResource.setName(aiServerValue.getName());
		aiResource.setResourceType(ResourceType.SERVER);
		ResourcePrototype resourcePrototype = new ResourcePrototype();
		resourcePrototype.setName(aiServerValue.getServerTypeName());		
		aiResource.setResourcePrototype(resourcePrototype);		
		
		return aiResource;
	}//EOM
	
	
	public final Resource toResource(final org.hyperic.hq.authz.server.session.Resource backendResource) { 
		final Resource resource = new Resource(backendResource.getId().toString()) ; 
		//TODO: dont know how to map 
		//resource.setNaturalID(naturalID) ;
		resource.setName(backendResource.getName()) ; 
		try{ 
			resource.setResourceType(ResourceType.valueOf(backendResource.getResourceType().getAppdefType())) ;
			resource.setResourcePrototype(new ResourcePrototype(backendResource.getPrototype().getName())) ; 
		}catch(Throwable t) {
			throw (t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t)) ;
		}
		resource.setResourcePrototypeFromString(backendResource.getPrototype().getName()) ;
		
		return resource ; 
	}//EOM 
	
	public final void mergeResource(final Resource inputResource, final org.hyperic.hq.authz.server.session.Resource backendResource) { 
		if(backendResource == null || inputResource == null) return ; 
		//else 
		
		//name 
		final String name = inputResource.getName() ; 
		if(this.compare(backendResource.getName(), name)) backendResource.setName(name) ; 
		
		//TODO: implement more attributes 
			
	}//EOM 
	
	private final boolean compare(final Object o1, final Object o2) { 
		return (o1 == o2 || (o1 != null && o1.equals(o2)) ) ; 
	}//EOM 
	
	public final Resource mergeConfig(ResourceType resourceType, org.hyperic.hq.authz.server.session.Resource backendResource, final Resource resource, final ConfigSchemaAndBaseResponse[] configResponses, Properties cprops) throws AppdefEntityNotFoundException {
		if(configResponses == null) return resource ;  
		
		final HashMap<String,String> configValues = new HashMap<String,String>() ; 
		
		//TODO: iterate over the resource properties and convert to key-val hashmap
		ConfigResponse configResponse = null ;
		String value = null ; 
		
		for(ConfigSchemaAndBaseResponse configMetadata : configResponses) {
			if(configMetadata == null) continue ; 
			configResponse = configMetadata.getResponse() ; 
			
			for(Map.Entry<String,String> entry : (Set<Map.Entry<String,String>>) configResponse.getConfig().entrySet() ) { 
				if( (value = entry.getValue()) == null || value.isEmpty()) continue ;
				configValues.put(entry.getKey(), value) ;  
			}//EO while there are more attributes 
		}//EO while there are more config responses 
        String moRefKey = HQConstants.MOREF;
        String moRef = cprops.getProperty(moRefKey);
        if (moRef!=null) {
            configValues.put(moRefKey,moRef);
        }
        String vcUuidKey = HQConstants.VCUUID;
        String vcUuid = cprops.getProperty(vcUuidKey);
        if (vcUuid!=null) {
            configValues.put(vcUuidKey,vcUuid);
        }
		final ResourceConfig resourceConfig = new ResourceConfig() ;
		resourceConfig.setMapProps(configValues) ; 
		resource.setResourceConfig(resourceConfig) ; 
		
		// Add resource-type specific properties
		
        //derive the resource type load strategy using the resource type enum 
        //Note: of the resourceType is null, then the generic resource resource type 
        //would be used 
		if (null == resourceType) {
		    resourceType = ResourceType.valueOf(backendResource.getResourceType().getAppdefType());
		}
        final ResourceTypeMapperStrategy resourceTypeStrategy = ResourceTypeMapperStrategy.valueOf(resourceType);        
        MappingContext context = new MappingContext(backendResource, configResponses, this, resourceType, resource);
        resourceTypeStrategy.mergeConfig(context);
        
		return resource ; 
	}//EOM
	
	public final static ComplexIp toIp(org.hyperic.hq.appdef.Ip backendIp) {
	    ComplexIp ip = new ComplexIp();
	    if (null != backendIp) {
	        ip.setAddress(backendIp.getAddress());
	        ip.setNetmask(backendIp.getNetmask());
	        ip.setMac(backendIp.getMacAddress());
	    }
	    return ip;
	}

	/**
	 * Resource-type specific mapping
	 *
	 */
    private enum ResourceTypeMapperStrategy { 
        
        PLATFORM(AppdefEntityConstants.APPDEF_TYPE_PLATFORM) { 

            @Override
            final Resource mergeConfig(final MappingContext mappingFlowContext) throws PlatformNotFoundException {
                org.hyperic.hq.authz.server.session.Resource backendResource = mappingFlowContext.backendResource;
                Platform platform = mappingFlowContext.visitor.platformManager.findPlatformById(backendResource.getInstanceId());
                Resource curResource = mappingFlowContext.currResource;

                // Add Mac Addresses to the resource multivalue property map
                Collection<org.hyperic.hq.appdef.Ip> backendMacAddresses = platform.getIps();
                if (null != backendMacAddresses) {
                    Collection<ConfigurationValue> ips = new ArrayList<ConfigurationValue>(backendMacAddresses.size());
                    for (org.hyperic.hq.appdef.Ip backendIp : backendMacAddresses) {
                        ips.add(toIp(backendIp));
                    }
                    curResource.getResourceConfig().putMapListProps(IP_MAC_ADDRESS, ips);
                }
                return curResource;
            }// EOM

        },//EO PLATFORM
        SERVER(AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            
        },//EO SERVER
        SERVICE(AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            
        },//EO SERVER
        RESOURCE(-999){
            
        };//EO RESOURCE
        
        
        private static final String IP_MAC_ADDRESS = "IP_MAC_ADDRESS";

        /**
         * Do configuration properties' mapping for specific resource types.
         * @param mappingflowContext
         * @return
         * @throws AppdefEntityNotFoundException
         */
        Resource mergeConfig(final org.hyperic.hq.api.transfer.mapping.ResourceMapper.MappingContext mappingflowContext) throws AppdefEntityNotFoundException {
            // Do nothing - just return the same resource
            return mappingflowContext.currResource;
        }
        
        private static final ResourceTypeMapperStrategy[] cachedValues ; 
        private static final int iNoOfStrategies ;
        
        private int appdefEntityType ;  
        
        static{ 
            cachedValues = values() ; 
            iNoOfStrategies = cachedValues.length ; 
        }//EO static block
        
        private ResourceTypeMapperStrategy(final int appdefEntityType) { 
            this.appdefEntityType = appdefEntityType ; 
        }//EOM 
        
        static final ResourceTypeMapperStrategy valueOf(final int iStrategyType) { 
            return (iStrategyType >= iNoOfStrategies ? RESOURCE : cachedValues[iStrategyType]) ; 
        }//EOM 
        
        static final ResourceTypeMapperStrategy valueOf(final ResourceType enumResourceType) {
            return (enumResourceType == null ? RESOURCE : valueOf(enumResourceType.name()) ) ; 
        }//EOM 

    
        
    }//EOE 
    

    
    
    final static class MappingContext  { 
        org.hyperic.hq.authz.server.session.Resource backendResource ; 
        ConfigSchemaAndBaseResponse[] configResponses ;         
        ResourceMapper visitor; 
        ResourceType resourceType;  
        Resource currResource ;               
        
        public MappingContext() {
            
        }        

        public MappingContext(org.hyperic.hq.authz.server.session.Resource backendResource,
                ConfigSchemaAndBaseResponse[] configResponses, ResourceMapper visitor, ResourceType resourceType,
                Resource currResource) {
            this.backendResource = backendResource;
            this.configResponses = configResponses;
            this.visitor = visitor;
            this.resourceType = resourceType;
            this.currResource = currResource;
        }

        public final void reset() { 
            this.backendResource = null;
            this.configResponses = new ConfigSchemaAndBaseResponse[ProductPlugin.CONFIGURABLE_TYPES.length];                        
            this.currResource = null;                        
        }//EOM 
        
    }//EO inner class Context 



    
    public ID toResource(RemovedResourceNotification n) {
        Integer id = n.getID();
        if (id==null) {
            return null;
        }
        ID removedResourceID = new ID();
        removedResourceID.setId(id);
        return removedResourceID;
    }
    public org.hyperic.hq.api.model.Resource toResource(CreatedResourceNotification n) {
        org.hyperic.hq.authz.server.session.Resource backendResource = n.getResource();
        if (backendResource==null) {
            return null;
        }
        Session hSession = f.getCurrentSession();
        hSession.update(backendResource);
        hSession.update(backendResource.getResourceType());
        Resource newResource = toResource(backendResource);
        Integer parentID = n.getParentID();
        // platforms wont have a parent
        if (parentID==null) {
            return newResource;
        }
        Resource parentResource = new Resource(String.valueOf(parentID));
        parentResource.addSubResource(newResource);
        return parentResource;
    }
	
	
	
}//EOC
