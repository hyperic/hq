package org.hyperic.hq.api.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.hq.api.transfer.impl.ResourceTransferImpl;
import org.hyperic.hq.appdef.server.session.AIQueueManagerImpl;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//@DirtiesContext
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = 
//    {"classpath*:/META-INF/spring/*-context.xml",
//        "classpath:META-INF/hqapi-context.xml"})
//@Transactional
public class ResourceTransferTest {
    
    ResourceTransferImpl resourceTransfer;
    
    public ResourceTransferTest() { 
    	super() ; 
    }//EOM
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        //resourceTransfer = new ResourceTransferImpl();
      
        // this.serverConfigurator = EasyMock.createMock(ServerConfigurator.class);
        /*resourceTransfer.setAiQueueManager(EasyMock.createMock(AIQueueManagerImpl.class));
        resourceTransfer.setAuthzSubjectManager(EasyMock.createMock(AuthzSubjectManagerImpl.class));
        resourceTransfer.setAiResourceMapper(EasyMock.createMock(AIResourceMapper.class));*/

    }

    @After
    public void tearDown() throws Exception {
    }

    //@Test
   /* public final void testGetAIResource() {      
    	final String fqdn= "543" ;
        String discoveryId = "ubuntu.eng.vmware.com";
        //EasyMock.expect(resourceTransfer.getAiQueueManager().findAIPlatformByFqdn(resourceTransfer.getAuthzSubjectManager().getOverlordPojo(), fqdn)).andReturn(true);
        
        discoveryId = "ubuntu.eng.vmware.com";
        ResourceType type = ResourceType.PLATFORM;
        AIResource aiResource = resourceTransfer.getAIResource(discoveryId, type);
        assertNotNull("aiResource hasn't been found", aiResource);
        assertEquals("Returned ai resource of incorrect type", type, aiResource.getResourceType());
        assertEquals("Expected autoinventory id to be " + discoveryId + " but was " + aiResource.getUuid(), discoveryId, aiResource.getUuid());
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
        
    }*/

}
