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
package org.hyperic.hq.api.transfer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourcePrototype;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/** 
 * Mapper between automatically discovered resources in API and in core Hyperic.
 * 
 * @since   4.5.0
 * @version 1.0 29 April 2012
 * @author Maya Anderson
 */
public class AIResourceMapper {
    
    @Autowired
    @Qualifier("restApiLogger")
    private Log logger;    
    public Log getLogger() {
        return logger;
    }
    public void setLogger(Log logger) {
        this.logger = logger;
    }    

	public AIResource mapAIPLarformValueToAIResource(AIPlatformValue aiPlatform,
			AIResource aiResource) {
		if (null == aiPlatform) {
		    logger.debug("Note: Empty AI platform is being mapped.");		    
			return aiResource;
		}
		
		if (null == aiResource) {
			aiResource = new AIResource();
		}		
		aiResource.setNaturalID(aiPlatform.getFqdn());
		// aiResource.setId(aiPlatform.get)
		aiResource.setName(aiPlatform.getName());
		aiResource.setResourceType(ResourceTypeModel.PLATFORM);

		ResourcePrototype resourcePrototype = new ResourcePrototype();
		resourcePrototype.setName(aiPlatform.getPlatformTypeName());
		aiResource.setResourcePrototype(resourcePrototype);

		AIServerValue[] aiServerValues = aiPlatform.getAIServerValues();
		if ((null != aiServerValues) && (aiServerValues.length > 0)) {
			List<ResourceModel> subResources = new ArrayList<ResourceModel>(aiServerValues.length);
			for (int i = 0; i < aiServerValues.length; i++) {
				AIResource aiServer = mapAIServerValueToAIResource(aiServerValues[i], null);
				subResources.add(aiServer);
			}
			aiResource.setSubResources(subResources);
		}
		
		return aiResource;
	}

	public AIResource mapAIServerValueToAIResource(
			AIServerValue aiServerValue, AIResource aiResource) {
		if (null == aiServerValue) {
		    logger.debug("Note: Empty AI server is being mapped.");		    
			return aiResource;
		}
		
		if (null == aiResource) {
			aiResource = new AIResource();
		}
		
		aiResource.setNaturalID(aiServerValue.getAutoinventoryIdentifier());
		String aiServerId = (null == aiServerValue.getId() ? null : aiServerValue.getId().toString()) ;
		aiResource.setId(aiServerId);
		aiResource.setName(aiServerValue.getName());
		aiResource.setResourceType(ResourceTypeModel.SERVER);
		ResourcePrototype resourcePrototype = new ResourcePrototype();
		resourcePrototype.setName(aiServerValue.getServerTypeName());		
		aiResource.setResourcePrototype(resourcePrototype);		
		
		return aiResource;
	}

}
