package org.hyperic.hq.api.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.appdef.server.session.AIQueueManagerImpl;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class AIResourceTransferTest extends AIResourceTransfer {
    
    AIResourceTransfer resourceTransfer;
    AIQueueManager aiQueueManager;
    AuthzSubjectManager authzSubjectManager;
    
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        resourceTransfer = new AIResourceTransfer();
        // this.serverConfigurator = EasyMock.createMock(ServerConfigurator.class);
        aiQueueManager = EasyMock.createMock(AIQueueManagerImpl.class);
        resourceTransfer.setAiQueueManager(aiQueueManager);
        authzSubjectManager = EasyMock.createMock(AuthzSubjectManagerImpl.class);
        resourceTransfer.setAuthzSubjectManager(authzSubjectManager);
//        resourceTransfer.setAiResourceMapper(EasyMock.createMock(AIResourceMapper.class));

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testGetAIResourcePlatform() {
        resourceTransfer.setAiResourceMapper(new AIResourceMapper());
        String discoveryId = "ubuntu.eng.vmware.com";
        AIPlatformValue aiPlatformValue = getMockAIPlatform(discoveryId);
        EasyMock.expect(authzSubjectManager.getOverlordPojo()).andReturn(null);
        EasyMock.expect(aiQueueManager.findAIPlatformByFqdn(null, discoveryId)).andStubReturn(aiPlatformValue);
        EasyMock.replay(authzSubjectManager, aiQueueManager);        
        
        ResourceType type = ResourceType.PLATFORM;
        AIResource aiResource = resourceTransfer.getAIResource(discoveryId, type);
        assertNotNull("aiResource hasn't been found", aiResource);
        assertEquals("Returned ai resource of incorrect type", type, aiResource.getResourceType());
        assertEquals("Expected autoinventory id to be " + discoveryId + " but was " + aiResource.getAutoinventoryId(), discoveryId, aiResource.getAutoinventoryId());
    }
    
    @Test
    public final void testGetAIResourceServer() {
        resourceTransfer.setAiResourceMapper(new AIResourceMapper());
        String discoveryId = "ubuntu.eng.vmware.com Apache Tomcat 6.0";
        AIServerValue aiServerValue = getMockAIServer(discoveryId);
        EasyMock.expect(authzSubjectManager.getOverlordPojo()).andReturn(null);
        EasyMock.expect(aiQueueManager.findAIServerByName(null, discoveryId)).andStubReturn(aiServerValue);
        EasyMock.replay(authzSubjectManager, aiQueueManager);       
        
        ResourceType type = ResourceType.SERVER;
        AIResource aiResource = resourceTransfer.getAIResource(discoveryId, type);
        assertNotNull("aiResource hasn't been found", aiResource);
        assertEquals("Returned ai resource of incorrect type", type, aiResource.getResourceType());
        assertEquals("Expected name id to be " + discoveryId + " but was " + aiResource.getName(), discoveryId, aiResource.getName());
    }    

    private AIPlatformValue getMockAIPlatform(String discoveryId) {
        AIPlatformValue aiPlatformValue = new AIPlatformValue();
        aiPlatformValue.setFqdn(discoveryId);
        return aiPlatformValue;
    }
    
    private AIServerValue getMockAIServer(String discoveryId) {
        AIServerValue aiServerValue = new AIServerValue();
        aiServerValue.setName(discoveryId);
        return aiServerValue;
    }

    @Test
    public final void testApproveAIResource() {
        AIResourceTransfer resourceTransfer = new AIResourceTransfer();
      
        ResourceType type = ResourceType.PLATFORM;
        List<String> ids = new ArrayList<String>(2);
        ids.add("1");
        ids.add("2");
        List<Resource> approvedResource = resourceTransfer.approveAIResource(ids, type);
        assertEquals("Number of approved resources doesn't match that of the discovered resources.", ids.size(), approvedResource.size());
        
    }

}
