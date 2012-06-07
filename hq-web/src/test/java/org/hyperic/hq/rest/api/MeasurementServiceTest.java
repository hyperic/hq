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
//import org.hyperic.hq.context.IntegrationTestContextLoader;
//import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
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

import org.hyperic.hq.api.model.measurements.ResourceMeasurementsRequestsCollection;
import org.hyperic.hq.api.model.measurements.ResourceMeasurementsRequest;
import org.hyperic.hq.api.model.measurements.ResourcesMeasurementsBatchResponse;



public class MeasurementServiceTest {
	    private static String endpointUrl;
	    private static List<Object> providers;
	    protected MeasurementService proxy;

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
	    
	    @Test
	    public final void testGetMetrics() {
	        String baseAddress = endpointUrl +"/rest-api/inventory/discovered-resources" ;???
	        MeasurementService measurementService = JAXRSClientFactory.create(baseAddress , MeasurementService.class, providers);
	        WebClient.client(measurementService).type("application/json");
	        
	        List<String> resourceIdsList = new ArrayList<String>();
	        resourceIdsList.add("1");
	        List<String> metricTemplateNames = new ArrayList<String>();
	        metricTemplateNames.add("a");
	        List<ResourceMeasurementsRequest> resourceMeasurementsRequestList = new ArrayList<ResourceMeasurementsRequest>();
	        resourceMeasurementsRequestList.add(new ResourceMeasurementsRequest(ResourceType.PLATFORM,resourceIdsList,metricTemplateNames));
	        ResourceMeasurementsRequestsCollection resourceMeasurementsRequestsCollection = new ResourceMeasurementsRequestsCollection(resourceMeasurementsRequestList);
	        ResourcesMeasurementsBatchResponse  resourcesMeasurementsBatchResponse = measurementService.getMetrics(resourceMeasurementsRequestsCollection);
	        System.out.println(resourcesMeasurementsBatchResponse);
//	        Assert.assertNotNull("Haven't received the requested Resource", resourcesMeasurementsBatchResponse);
	    }
}
