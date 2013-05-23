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
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/** 
 * Transfer of automatically discovered resource messages between the API and Hyperic core.
 * 
 * @since   4.5.0
 * @version 1.0 29 April 2012
 * @author Maya Anderson
 */
public class AIResourceTransfer {

    @Autowired
    private AIQueueManager aiQueueManager;

    @Autowired
    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    private AIResourceMapper aiResourceMapper;
    
    @Autowired
    private SessionManager sessionManager;
    
    public AIQueueManager getAiQueueManager() {
        return aiQueueManager;
    }

    public void setAiQueueManager(AIQueueManager aiQueueManager) {
        this.aiQueueManager = aiQueueManager;
    }

    public AuthzSubjectManager getAuthzSubjectManager() {
        return authzSubjectManager;
    }

    public void setAuthzSubjectManager(AuthzSubjectManager authzSubjectManager) {
        this.authzSubjectManager = authzSubjectManager;
    }

    public AIResourceMapper getAiResourceMapper() {
        return aiResourceMapper;
    }

    public void setAiResourceMapper(AIResourceMapper aiResourceMapper) {
        this.aiResourceMapper = aiResourceMapper;
    }
    
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public AIResource getAIResource(String discoveryId, ResourceTypeModel type) {
        //TODO Maya replace with correct identity after implementing security 
//        AuthzSubject authzSubject = authzSubjectManager.getOverlordPojo();
        AuthzSubject authzSubject = null;
        try {
            authzSubject = sessionManager.getSubject(1);
        } catch (SessionNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SessionTimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        AIResource aiResource = null;
        if (ResourceTypeModel.PLATFORM == type) {
            AIPlatformValue aiPlatform = this.aiQueueManager.findAIPlatformByFqdn(
                    authzSubject, discoveryId);
            aiResource = this.aiResourceMapper.mapAIPLarformValueToAIResource(aiPlatform, aiResource);
        } else if (ResourceTypeModel.SERVER == type) {
            AIServerValue aiServer = this.aiQueueManager.findAIServerByName(authzSubject, discoveryId);
            aiResource = this.aiResourceMapper.mapAIServerValueToAIResource(aiServer, aiResource);
        }
        return aiResource;
    }

    public List<ResourceModel> approveAIResource(List<String> ids, ResourceTypeModel type) {
        // TODO method stub
        List<ResourceModel> approvedResources = new ArrayList<ResourceModel>();
        for (String aiServerId : ids) {
            ResourceModel server1 = createMockServer(aiServerId);
            approvedResources.add(server1);
        }
        return approvedResources;
    }

    private ResourceModel createMockServer(String aiServerId) {
        ResourceModel server = new ResourceModel();
        server.setNaturalID(aiServerId + "-" + aiServerId);
        server.setName("Server " + aiServerId);
        ResourcePrototype serverPrototype = new ResourcePrototype();
        serverPrototype.setName("A server");
        serverPrototype.setId("aServer");
        server.setResourceType(ResourceTypeModel.SERVER);
        server.setResourcePrototype(serverPrototype);
        return server;
    }

//    private AIResource createMockAIPlatform(String id) {
//        AIResource platform = new AIResource();
//        ResourcePrototype platformResourcePrototype = new ResourcePrototype();
//        platformResourcePrototype.setName("Linux");
//        platformResourcePrototype.setId("Linux");
//        platform.setResourcePrototype(platformResourcePrototype);
//        platform.setResourceType(ResourceType.PLATFORM);
//        platform.setAutoinventoryId(id);
//        platform.setId(id + id);
//        platform.setName("Platform " + id);
//        return platform;
//    }

    private AIResource createMockAIServer(String id) {
        AIResource server = new AIResource();
        ResourcePrototype serverResourcePrototype = new ResourcePrototype();

        serverResourcePrototype.setName("Tomcat 6.0");
        serverResourcePrototype.setId("tomcatID");
        server.setResourcePrototype(serverResourcePrototype);
        server.setResourceType(ResourceTypeModel.SERVER);
        server.setNaturalID(id);
        server.setId(id + id);
        server.setName("Server " + id);

        return server;
    }
}
