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
package org.hyperic.hq.api.services.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.hq.api.services.AIResourceService;
import org.hyperic.hq.api.transfer.AIResourceTransfer;
import org.hyperic.util.config.ConfigOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


/** 
 * Automatically discovered resource service implementation.
 * 
 * @since   4.5.0
 * @version 1.0 29 April 2012
 * @author Maya Anderson
 */
public class AIResourceServiceImpl implements AIResourceService {

    @Autowired
    private AIResourceTransfer aiResourceTransfer;
    public AIResourceTransfer getAiResourceTransfer() {
        return aiResourceTransfer;
    }
    public void setAiResourceTransfer(AIResourceTransfer aiResourceTransfer) {
        this.aiResourceTransfer = aiResourceTransfer;
    }   
    
    @Autowired
    @Qualifier("restApiLogger")
    private Log logger;    
    public Log getLogger() {
        return logger;
    }    
    public void setLogger(Log logger) {
        this.logger = logger;
    }        
    
    public AIResource getAIResource(String discoveryId, ResourceTypeModel type) {        
        return aiResourceTransfer.getAIResource(discoveryId, type);
    }
    
    
    public List<ResourceModel> approveAIResource(List<String> ids, ResourceTypeModel type) {
        return aiResourceTransfer.approveAIResource(ids, type);
    }
    public List<ConfigOption> getConfigurationSchema() {
        // TODO Auto-generated method stub
        return null;
    }
}
