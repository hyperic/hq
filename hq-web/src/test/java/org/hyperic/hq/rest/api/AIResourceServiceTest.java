package org.hyperic.hq.rest.api;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
//import org.apache.http.HttpHeaders;
import org.hyperic.hq.api.model.AIResource;
import org.hyperic.hq.api.model.MetricTemplate;
import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.ResourceConfig;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourcePrototype;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceTypeModel;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.model.PropertyList;
import org.hyperic.hq.api.model.resources.ComplexIp;
import org.hyperic.hq.api.services.AIResourceService;
import org.hyperic.hq.api.services.ResourceService;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.api.services.SessionManagementService;
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
//        providers.add(new org.apache.cxf.jaxrs.provider.JAXBElementProvider());
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
    public final void testGetApprovedResource() throws Throwable {
        String baseAddress = endpointUrl + "/rest/resource";
        ResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress, ResourceService.class, providers);

        Client client = WebClient.client(aiResourceSvc);
        client.type("application/json");

        addAdminAuthorizationHeader(client);

        String[] ids = { "ID1", "ID2" };
        ResourceTypeModel type = ResourceTypeModel.PLATFORM;
        ResourceStatusType resourceStatusType = ResourceStatusType.APPROVED;
        ResourceDetailsType[] responseStructure = { ResourceDetailsType.BASIC };
        ResourceModel result = aiResourceSvc.getResource(ids[1], type, resourceStatusType, 1, responseStructure);
        // assertEquals(type + ids[0] + ids[1], result);
        System.out.println(result.getId());
    }

    // @Test(expected=SessionNotFoundException.class)
//    @Test
    public final void testGetApprovedResource_NotAuth() throws Throwable {
        String baseAddress = endpointUrl + "/rest/resource";
        ResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress, ResourceService.class, providers);

        Client client = WebClient.client(aiResourceSvc);
        client.type("application/json");

        String[] ids = { "ID1", "ID2" };
        ResourceTypeModel type = ResourceTypeModel.PLATFORM;
        ResourceStatusType resourceStatusType = ResourceStatusType.APPROVED;
        ResourceDetailsType[] responseStructure = { ResourceDetailsType.BASIC };
        ResourceModel result = aiResourceSvc.getResource(ids[1], type, resourceStatusType, 1, responseStructure);
        // assertEquals(type + ids[0] + ids[1], result);
        System.out.println(result.getId());
    }

//    @Test
    public final void testGetApprovedResource_WrongPassword() throws Throwable {
        String baseAddress = endpointUrl + "/rest/resource";
        ResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress, ResourceService.class, providers);

        Client client = WebClient.client(aiResourceSvc);
        client.type("application/json");

        String authorizationHeader = "Basic "
                + org.apache.cxf.common.util.Base64Utility.encode("hqadmin:wrong_password".getBytes());
        client.header(HttpHeaders.AUTHORIZATION, authorizationHeader); // "Authorization"

        String[] ids = { "ID1", "ID2" };
        ResourceTypeModel type = ResourceTypeModel.PLATFORM;
        ResourceStatusType resourceStatusType = ResourceStatusType.APPROVED;
        ResourceDetailsType[] responseStructure = { ResourceDetailsType.BASIC };
        ResourceModel result = aiResourceSvc.getResource(ids[1], type, resourceStatusType, 1, responseStructure);
        // assertEquals(type + ids[0] + ids[1], result);
        System.out.println(result.getId());
    }

//    @Test
    public final void testJSessionId() throws Throwable {
        String baseAddress = endpointUrl + "/rest/resource";
        ResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress, ResourceService.class, providers);

        Client client = WebClient.client(aiResourceSvc);
        // client.type("application/json");

        addAdminAuthorizationHeader(client);

        String[] ids = { "ID1", "ID2" };
        Resources resources = new Resources();
        ResourceBatchResponse result = aiResourceSvc.updateResources(resources);

//        extractAndSetJSessionId(client);
        String jsessionId = extractJSessionId(client);
        addJSessionId(jsessionId, client);

        result = aiResourceSvc.updateResources(resources);

        String sessionBaseAddress = endpointUrl + "/rest/session";
        SessionManagementService sessionService = JAXRSClientFactory.create(sessionBaseAddress,
                SessionManagementService.class, providers);

        Client sessionClient = WebClient.client(sessionService);
        addAdminAuthorizationHeader(sessionClient);
        addJSessionId(jsessionId, sessionClient);

        sessionService.logout();
    }

    // @Test
    public final void testUpdateResource() throws Throwable {
        String baseAddress = endpointUrl + "/rest/resource";
        ResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress, ResourceService.class, providers);

        Client client = WebClient.client(aiResourceSvc);
        // client.type("application/json");
        client.type(MediaType.APPLICATION_XML);

        addAdminAuthorizationHeader(client);

        ResourceTypeModel type = ResourceTypeModel.SERVER;
        ResourceStatusType resourceStatusType = ResourceStatusType.APPROVED;
        List<ResourceModel> resourceList = new ArrayList<ResourceModel>();

        ResourceModel updatedResource = new ResourceModel("1");
        ResourcePrototype resourcePrototype = new ResourcePrototype("Tomcat 6.0");
        updatedResource.setResourcePrototype(resourcePrototype);
        updatedResource.setResourceStatusType(resourceStatusType);
        updatedResource.setResourceType(type);
        HashMap<String, String> mapProps = new HashMap<String, String>(1);
        mapProps.put("key1", "value1");
        Map<String, PropertyList> mapListProps = new HashMap<String, PropertyList>(2);
        ComplexIp[] macAddresses = { new ComplexIp("netmask1", "mac1", "address1"), new ComplexIp("netmask2", "mac2", "address2") };
        mapListProps.put("key1", new PropertyList(macAddresses));        
        mapListProps.put("key2", new PropertyList(macAddresses));
        ResourceConfig resourceConfig = new ResourceConfig(updatedResource.getId(), mapProps, mapListProps);



        updatedResource.setResourceConfig(resourceConfig);
        resourceList.add(updatedResource);
        Resources resources = new Resources(resourceList);
        ResourceBatchResponse result = aiResourceSvc.updateResources(resources);
        System.out.println(result.getResources());
        // assertEquals(type + ids[0] + ids[1], result);

    }
   
    // @Test
    public final void testGetMeasurementTemplate() throws Throwable {
        String baseAddress = endpointUrl + "/rest/resource";
        ResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress, ResourceService.class, providers);

        Client client = WebClient.client(aiResourceSvc);
        // client.type("application/json");
        client.type(MediaType.APPLICATION_JSON);

        addAdminAuthorizationHeader(client);

        String protoId = "10003";
        List<MetricTemplate> result = aiResourceSvc.getMetricTemplate(protoId );
        System.out.println(result.size());
        // assertEquals(type + ids[0] + ids[1], result);

    }    

    // @Test
    public final void testGetAIResource() {

        String baseAddress = endpointUrl + "/rest/inventory/discovered-resources";
        AIResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress, AIResourceService.class, providers);

        Client client = WebClient.client(aiResourceSvc);
        client.type("application/json");

        addAdminAuthorizationHeader(client);

        String[] ids = { "ID1", "ID2" };
        ResourceTypeModel type = ResourceTypeModel.PLATFORM;
        AIResource result = aiResourceSvc.getAIResource(ids[0], type);
        // assertEquals(type + ids[0] + ids[1], result);
        System.out.println(result.getId());
    }

    // @Test
    public final void testApproveAIResource() {

        String baseAddress = endpointUrl + "/rest/inventory/discovered-resources";
        AIResourceService aiResourceSvc = JAXRSClientFactory.create(baseAddress, AIResourceService.class, providers);
        Client client = WebClient.client(aiResourceSvc);
        client.type("application/json");

        addAdminAuthorizationHeader(client);

        String[] ids = { "ID1", "ID2" };
        ResourceTypeModel type = ResourceTypeModel.PLATFORM;
        List<ResourceModel> result = aiResourceSvc.approveAIResource(Arrays.asList(ids), type);

        extractAndSetJSessionId(client);
        // response.getMetadata().put(HttpHeaders.COOKIE, cookies);

        result = aiResourceSvc.approveAIResource(Arrays.asList(ids), type);

        // assertEquals(type + ids[0] + ids[1], result);
        System.out.println(result.get(0).getId());
    }

    private void extractAndSetJSessionId(Client client) {
        extractAndSetJSessionId(client, client);
        // MultivaluedMap<String, String> headers = client.getHeaders();
        // Response response = client.getResponse();
        // List<Object> cookies =
        // response.getMetadata().get(HttpHeaders.SET_COOKIE);
        // if (cookies != null && !cookies.isEmpty()) {
        //
        // String cookie = null;
        // for (Object object : cookies) {
        // cookie = (String) object;
        // if (cookie.contains("JSESSIONID")) {
        // // cookie looks like that:
        // JSESSIONID=m4i8fbdufhiy12tlnpd1hfp3f;Path=/
        // cookie = cookie.substring(cookie.indexOf("=") + 1,
        // cookie.indexOf(";"));
        // }
        // }
        // Cookie cookieValue = new Cookie("JSESSIONID", cookie);
        // client.cookie(cookieValue);
        //
        // }
    }

    private void extractAndSetJSessionId(Client sourceClient, Client destinationClient) {
        MultivaluedMap<String, String> headers = sourceClient.getHeaders();
        Response response = sourceClient.getResponse();
        List<Object> cookies = response.getMetadata().get(HttpHeaders.SET_COOKIE);
        if (cookies != null && !cookies.isEmpty()) {

            String cookie = null;
            for (Object object : cookies) {
                cookie = (String) object;
                if (cookie.contains("JSESSIONID")) {
                    // cookie looks like that:
                    // JSESSIONID=m4i8fbdufhiy12tlnpd1hfp3f;Path=/
                    cookie = cookie.substring(cookie.indexOf("=") + 1, cookie.indexOf(";"));
                }
            }
            Cookie cookieValue = new Cookie("JSESSIONID", cookie);
            destinationClient.cookie(cookieValue);

        }
    }

    private String extractJSessionId(Client client) {
        String jsessionId = null;
        MultivaluedMap<String, String> headers = client.getHeaders();
        Response response = client.getResponse();
        List<Object> cookies = response.getMetadata().get(HttpHeaders.SET_COOKIE);
        if (cookies != null && !cookies.isEmpty()) {

            String cookie = null;
            for (Object object : cookies) {
                cookie = (String) object;
                if (cookie.contains("JSESSIONID")) {
                    // cookie looks like that:
                    // JSESSIONID=m4i8fbdufhiy12tlnpd1hfp3f;Path=/
                    cookie = cookie.substring(cookie.indexOf("=") + 1, cookie.indexOf(";"));
                    jsessionId = cookie;
                }
            }
        }
        return jsessionId;
    }
    
    private void addJSessionId(String jsessionIdValue, Client client) {
        Cookie cookieValue = new Cookie("JSESSIONID", jsessionIdValue);
        client.cookie(cookieValue);
    }
    
    

    private void addAdminAuthorizationHeader(Client client) {
        String authorizationHeader = "Basic "
                + org.apache.cxf.common.util.Base64Utility.encode("hqadmin:hqadmin".getBytes());
        client.header(HttpHeaders.AUTHORIZATION, authorizationHeader); // "Authorization"
    }

}
