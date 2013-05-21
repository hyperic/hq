package org.hyperic.hq.api.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.hq.api.services.AIResourceService;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/*@DirtiesContext
@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/spring/*-context.xml",
        "classpath:META-INF/hqapi-context.xml", 
        "classpath*:/org/hyperic/hq/api/rest/AIResourceServiceTest-context.xml" }, 
        loader = IntegrationTestContextLoader.class)
@Transactional*/ 
public class AIResourceServiceTest {


    @Autowired
    protected AIResourceService proxy;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }


//    @Test
    public final void testGetAIResource() {
        String discoveryId = "ubuntu.eng.vmware.com";
        AIResource aiResource = proxy.getAIResource(discoveryId, ResourceTypeModel.PLATFORM);
        Assert.assertNotNull("Haven't received the requested aiResource", aiResource);
    }

 //   @Test
    public final void testGetAIResourceNonExisting() {
        String discoveryId = "id1";
        AIResource aiResource = proxy.getAIResource(discoveryId, ResourceTypeModel.PLATFORM);
        Assert.assertNull("Have a non-existent aiResource", aiResource);
    }    
    
  //  @Test
    public final void testApproveAIResource() {        
        List<String> ids = new ArrayList<String>(2);
        ids.add("1");
        ids.add("2");
        List<ResourceModel> resources = proxy.approveAIResource(ids, ResourceTypeModel.PLATFORM);
        Assert.assertNotNull("Haven't received the requested Resource", resources);
    }

}
