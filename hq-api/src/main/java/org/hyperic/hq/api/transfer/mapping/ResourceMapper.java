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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourcePrototype;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.util.config.ConfigResponse;
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
	
	public final Resource mergeConfig(final Resource resource, final ConfigSchemaAndBaseResponse[] configResponses) {
		if(configResponses == null) return resource ;  
		
		final Map<String,String> configValues = new HashMap<String,String>() ; 
		
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
		
		final ResourceConfig resourceConfig = new ResourceConfig() ;
		resourceConfig.setMapProps(configValues) ; 
		resource.setResourceConfig(resourceConfig) ; 
		return resource ; 
	}//EOM
	
	
}//EOC
