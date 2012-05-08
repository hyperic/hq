package org.hyperic.hq.rest.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.services.AIResourceService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//import org.hyperic.hq.api.services;

public class AIResourceServiceTest {
    private static String endpointUrl;
    private static List<Object> providers;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        endpointUrl = "http://localhost:7080";
        providers = new ArrayList<Object>();
        providers.add(new org.codehaus.jackson.jaxrs.JacksonJsonProvider());        
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
        String baseAddress = endpointUrl +"/rest-api/inventory/discovered-resources" ;
        AIResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress , AIResourceService.class, providers);
        WebClient.client(aiResourceSvc).type("application/json");
        
        
        String[] ids =  { "ID1", "ID2" };
        ResourceType type = ResourceType.PLATFORM;
        List<Resource> result = aiResourceSvc.approveAIResource(Arrays.asList(ids), type);
//      assertEquals(type + ids[0] + ids[1], result);       
        System.out.println(result.get(0).getId());
    }

//    @Test
    public final void testApproveAIResource() {
        String baseAddress = endpointUrl +"/rest-api/inventory/discovered-resources" ; 
        AIResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress , AIResourceService.class, providers);
        WebClient.client(aiResourceSvc).type("application/json");
        
        
        String[] ids =  { "ID1", "ID2" };
        ResourceType type = ResourceType.PLATFORM;
        List<Resource> result = aiResourceSvc.approveAIResource(Arrays.asList(ids), type);
//      assertEquals(type + ids[0] + ids[1], result);       
        System.out.println(result.get(0).getId());
    }
    
}
