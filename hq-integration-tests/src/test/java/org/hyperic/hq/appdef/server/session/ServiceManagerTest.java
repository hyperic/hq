/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

/**
 * 
 */
package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author iperumal
 * 
 */
@DirtiesContext
public class ServiceManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    ApplicationManager applicationManager;

    private PlatformType testPlatformType;

    private List<Platform> testPlatforms;

    private List<Server> testServers;

    private ServerType testServerType;

    private AuthzSubject subject;

    private List<Platform> createPlatforms(String agentToken) throws ApplicationException {
        List<Platform> platforms = new ArrayList<Platform>(2);
        for (int i = 1; i <= 2; i++) {
            AIPlatformValue aiPlatform = new AIPlatformValue();
            aiPlatform.setName("Test Platform" + i);
            aiPlatform.setCpuCount(2);
            aiPlatform.setPlatformTypeName("Linux");
            aiPlatform.setAgentToken(agentToken);
            aiPlatform.setFqdn("Test Platform" + i);
            platforms.add(i - 1, createPlatform(agentToken, "Linux", "Test Platform" + i,
                "Test Platform" + i));
        }
        return platforms;
    }

    private List<Server> createServers(List<Platform> platforms, ServerType serverType)
        throws PlatformNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException, NotFoundException {
        List<Server> servers = new ArrayList<Server>(2);

        for (Platform platform : platforms) {
            servers.add(createServer(platform, serverType, "server"));
        }
        return servers;
    }

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        subject = authzSubjectManager.getOverlordPojo();
        String agentToken = "agentToken123";
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken,
            "4.5");
        testPlatformType = platformManager.createPlatformType("Linux", "Test Plugin");
        testPlatforms = createPlatforms(agentToken);
        // Create ServerType
        testServerType = createServerType("Test Server", "6.0", new String[] { "Linux" },
            "Test Server Plugin", false);
        // Create test server
        testServers = createServers(testPlatforms, testServerType);
        Set<Platform> platforms = new HashSet<Platform>(1);
        platforms.add(testPlatforms.get(0));
        createPlatformResourceGroup(platforms, "AllPlatformGroup");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#createServiceType(org.hyperic.hq.product.ServiceTypeInfo, java.lang.String, org.hyperic.hq.appdef.server.session.ServerType)}
     * .
     */
    @Test
    public void testCreateServiceType() throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServers.get(0).getServerType());
        assertEquals(serviceType.getName(), "Test ServiceType Name");
        assertEquals(serviceType.getDescription(), "Test ServiceType Desc");
        assertEquals(serviceType.getPlugin(), "Test Service Plugin");
        assertEquals(serviceType.getServerType(), testServerType);
        assertEquals(serviceType.isIsInternal(), false);
        Resource res = resourceManager.findResourceByTypeAndInstanceId(resourceManager
            .findResourceTypeByName(AuthzConstants.servicePrototypeTypeName).getName(), serviceType
            .getId());
        assertEquals(res.getName(), "Test ServiceType Name");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#createService(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.server.session.Server, org.hyperic.hq.appdef.server.session.ServiceType, java.lang.String, java.lang.String, java.lang.String, org.hyperic.hq.appdef.server.session.Service)}
     * .
     */
    @Test
    public void testCreateService() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service.getName(), "Test Service Name");
        assertEquals(service.getDescription(), "Test Service Desc");
        assertEquals(service.getServiceType(), serviceType);
        assertEquals(service.getServer(), testServers.get(0));
        assertEquals(service.getLocation(), "my computer");
        assertFalse(service.getServiceValue().getServiceRt());
        assertFalse(service.getServiceValue().getEndUserRt());
        assertFalse(service.isAutodiscoveryZombie());
        assertNotNull(service.getResource());
        assertEquals(service.getResource().getName(), "Test Service Name");
    }

    // TODO: Commenting out for now as I don't see moveService being used
    /**
     * // * Test method for // *
     * {@link //
 org.hyperic.hq.appdef.server.session.ServiceManagerImpl #moveService(org.hyperic.hq.authz.server.session.AuthzSubject, //
 org.hyperic.hq.appdef.server.session.Service, //
 org.hyperic.hq.appdef.server.session.Platform)}
     * // * . //
     */
    // @Test
    // public void testMoveServicePlatform() throws ApplicationException,
    // NotFoundException,
    // VetoException {
    // // Platform primaryPlatform = testPlatforms.get(0);
    // Platform destinationPlatform = testPlatforms.get(1);
    // // Now create a service for the primary platform
    // ServiceTypeInfo sinfo = new ServiceTypeInfo();
    // sinfo.setDescription("Test ServiceType Desc");
    // sinfo.setInternal(false);
    // sinfo.setName("Test ServiceType Name");
    // ServiceType serviceType = serviceManager.createServiceType(sinfo,
    // "Test Service Plugin",
    // testServerType);
    // // testServers.get(0) corresponds to the primary platform
    // Service targetService = serviceManager.createService(subject,
    // testServers.get(0).getId(),
    // serviceType.getId(), "Test Service Name", "Test Service Desc",
    // "my computer");
    // assertEquals(targetService.getServer(), testServers.get(0));
    // Collection<ResourceEdge> resEdges =
    // resourceManager.findResourceEdges(resourceManager
    // .getContainmentRelation(), testServers.get(0).getResource());
    // assertEquals(resEdges.size(), 1);
    // serviceManager.moveService(subject, targetService, destinationPlatform);
    // assertEquals(resourceManager.findResourceEdges(resourceManager.getContainmentRelation(),
    // testServers.get(0).getResource()).size(), 0);
    // assertEquals(resourceManager.findResourceEdges(resourceManager.getContainmentRelation(),
    // testServers.get(1).getResource()).size(), 1);
    // // Make sure the service doesn't exist with previous server
    // //Looks like a bug here as the service is not removed from prev server
    // assertEquals(testServers.get(0).getServices().size(), 0);
    // // Verify the service is moved to the destination server
    // assertEquals(targetService.getServer(), testServers.get(1));
    // }
    //
    // @Test
    // public void testMoveServiceIncompatServerType() throws
    // ApplicationException, NotFoundException,
    // VetoException {
    // // Now create a service for the primary platform
    // ServiceTypeInfo sinfo = new ServiceTypeInfo();
    // sinfo.setDescription("Test ServiceType Desc");
    // sinfo.setInternal(false);
    // sinfo.setName("Test ServiceType Name");
    // ServiceType serviceType = serviceManager.createServiceType(sinfo,
    // "Test Service Plugin",
    // testServerType);
    // // testServers.get(0) corresponds to the primary platform
    // Service targetService = serviceManager.createService(subject,
    // testServers.get(0).getId(),
    // serviceType.getId(), "Test Service Name", "Test Service Desc",
    // "my computer");
    //
    // // Create a new platform
    // Platform destinationPlatform = createPlatform("agentToken123",
    // "Destination Platform",
    // "Linux");
    // // Create ServerType
    // ServerType serverType = createServerType("Test Server1", "6.0", new
    // String[] { "Linux" },
    // "Test AnotherServer Plugin");
    // // Now create a new server with this server type to the destination
    // // platform so that the destination platform doesn't have the server
    // // type of the target service
    // ServerValue server = new ServerValue();
    // serverManager
    // .createServer(subject, destinationPlatform.getId(), serverType.getId(),
    // server);
    // assertEquals(targetService.getServer(), testServers.get(0));
    // try {
    // serviceManager.moveService(subject, targetService, destinationPlatform);
    // } catch (VetoException e) {
    // assertEquals(e.getMessage(), "Unable find applicable server on platform "
    // +
    // destinationPlatform.getName() + " as destination for " +
    // targetService.getName());
    // return;
    // }
    // fail("VetoException not thrown");
    // }
    //
    // /*
    // * Following usecase addresses the VetoException from
    // * moveService(AuthzSubject subject, Service target, Server destination)
    // */
    // @Test
    // public void testMoveServiceIncompatServerType1() throws
    // ApplicationException,
    // NotFoundException, VetoException {
    // // Now create a service for the primary platform
    // ServiceTypeInfo sinfo = new ServiceTypeInfo();
    // sinfo.setDescription("Test ServiceType Desc");
    // sinfo.setInternal(false);
    // sinfo.setName("Test ServiceType Name");
    // ServiceType serviceType = serviceManager.createServiceType(sinfo,
    // "Test Service Plugin",
    // testServerType);
    // // testServers.get(0) corresponds to the primary platform
    // Service targetService = serviceManager.createService(subject,
    // testServers.get(0).getId(),
    // serviceType.getId(), "Test Service Name", "Test Service Desc",
    // "my computer");
    //
    // // Create a new platform
    // Platform destinationPlatform = createPlatform("agentToken123",
    // "Destination Platform",
    // "Linux");
    // // Create ServerType
    // ServerType serverType = createServerType("Test Server1", "6.0", new
    // String[] { "Linux" },
    // "Test AnotherServer Plugin");
    // // Now create a new server with this server type to the destination
    // // platform so that the destination platform doesn't have the server
    // // type of the target service
    // ServerValue serverVal = new ServerValue();
    // Server destinationServer = serverManager.createServer(subject,
    // destinationPlatform.getId(),
    // serverType.getId(), serverVal);
    // assertEquals(targetService.getServer(), testServers.get(0));
    // try {
    // serviceManager.moveService(subject, targetService, destinationServer);
    // } catch (VetoException e) {
    // assertEquals(e.getMessage(), "Incompatible resources passed to move(), "
    // +
    // "cannot move service of type " +
    // targetService.getServiceType().getName() + " to " +
    // destinationServer.getServerType().getName());
    // return;
    // }
    // fail("VetoException not thrown");
    // }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceIds(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    @Test
    public void testGetServiceIds() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        List<Integer> serviceIds = new ArrayList<Integer>(10);
        for (int i = 1; i < 10; i++) {
            serviceIds.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service Desc" + i,
                "my computer").getId());
        }
        Integer[] ids = serviceManager.getServiceIds(subject, serviceType.getId());
        assertEquals(Arrays.equals(serviceIds.toArray(new Integer[9]), ids), true);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findServicesById(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer[])}
     * .
     */
    // TODO: Not being used now
    public void testFindServicesById() {

    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findServiceById(java.lang.Integer)}
     * .
     */
    @Test
    public void testFindServiceById() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service, serviceManager.findServiceById(service.getId()));
    }

    @Test(expected = ServiceNotFoundException.class)
    public void testFindServiceByInvalidId() throws ApplicationException, NotFoundException {
        serviceManager.findServiceById(12345789);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceById(java.lang.Integer)}
     * .
     */
    @Test
    public void testGetServiceByIdInteger() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service, serviceManager.getServiceById(service.getId()));
    }

    @Test
    public void testGetServiceByInvalidIdInteger() throws ApplicationException, NotFoundException {
        assertNull(serviceManager.getServiceById(123456789));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceById(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    @Test
    public void testGetServiceByIdAuthzSubjectInteger() throws ApplicationException,
        NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service, serviceManager.getServiceById(subject, service.getId()));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByAIID(org.hyperic.hq.appdef.server.session.Server, java.lang.String)}
     * .
     */
    @Test
    public void testGetServicesByAIID() throws ApplicationException, NotFoundException {
        // AutoInventory Identifier is same as that of the service name
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service Desc", "my computer");
        assertEquals(service, serviceManager.getServicesByAIID(testServers.get(0),
            service.getAutoinventoryIdentifier()).get(0));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceByName(org.hyperic.hq.appdef.server.session.Server, java.lang.String)}
     * .
     */
    @Test
    public void testGetServiceByNameServer() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        String serviceName = "Test Service Name";
        // Create two services with the same name/different desc and assign to
        // two different servers
        Service service1 = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), serviceName, "Test Service From Server1", "my computer");
        Service service2 = serviceManager.createService(subject, testServers.get(1).getId(),
            serviceType.getId(), serviceName, "Test Service From Server2", "my computer");
        assertEquals(serviceManager.getServiceByName(testServers.get(0), serviceName), service1);
        assertEquals(serviceManager.getServiceByName(testServers.get(1), serviceName)
            .getDescription(), service2.getDescription());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceByName(org.hyperic.hq.appdef.server.session.Platform, java.lang.String)}
     * .
     */
    @Test
    public void testGetServiceByNamePlatformString() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        String serviceName = "Test Service Name";
        // Create two services with the same name/different desc and assign to
        // two different servers
        Service service1 = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), serviceName, "Test Service From Server1", "my computer");
        Service service2 = serviceManager.createService(subject, testServers.get(1).getId(),
            serviceType.getId(), serviceName, "Test Service From Server2", "my computer");
        // testPlatforms.get(0) corresponds to testServers.get(0)
        assertEquals(serviceManager.getServiceByName(testPlatforms.get(0), serviceName), service1);
        // testPlatforms.get(1) corresponds to testServers.get(1)
        assertEquals(serviceManager.getServiceByName(testPlatforms.get(1), serviceName)
            .getDescription(), service2.getDescription());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findServiceType(java.lang.Integer)}
     * .
     */
    @Test
    public void testFindServiceType() throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        assertEquals(serviceType, serviceManager.findServiceType(serviceType.getId()));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findServiceTypeByName(java.lang.String)}
     * .
     */
    @Test
    public void testFindServiceTypeByName() throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        assertEquals(serviceType, serviceManager.findServiceTypeByName(serviceType.getName()));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findDeletedServices()}
     * .
     */
    // TODO
    public void testFindDeletedServices() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getAllServiceTypes(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetAllServiceTypes() throws NotFoundException {
        PageList<ServiceTypeValue> pgList = new PageList<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc");
            sinfo.setInternal(false);
            sinfo.setName("Test ServiceType Name");
            pgList.add(serviceManager.createServiceType(sinfo, "Test Service Plugin",
                testServerType).getServiceTypeValue());
        }
        PageControl pc = new PageControl();
        assertEquals(serviceManager.getAllServiceTypes(subject, pc), pgList);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getViewableServiceTypes(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO
    public void testGetViewableServiceTypes() throws NotFoundException, ApplicationException {
        PageList<ServiceTypeValue> pgList = new PageList<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        ServiceType serviceType = null;
        Resource res = null;
        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc");
            sinfo.setInternal(false);
            sinfo.setName("Test ServiceType Name");
            serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
                testServerType);
            pgList.add(serviceType.getServiceTypeValue());
            res = resourceManager.findResourceByTypeAndInstanceId(resourceManager
                .findResourceTypeByName(AuthzConstants.servicePrototypeTypeName).getName(),
                serviceType.getId());
        }
        PageControl pc = new PageControl();
        // TODO: Need to look at why
        // serviceManager.getViewableServiceTypes(subject, pc) returns empty
        // pageList
        assertEquals(serviceManager.getViewableServiceTypes(subject, pc), pgList);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceTypesByServerType(org.hyperic.hq.authz.server.session.AuthzSubject, int)}
     * .
     */
    @Test
    public void testGetServiceTypesByServerType() throws NotFoundException {
        PageList<ServiceTypeValue> pgList = new PageList<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc");
            sinfo.setInternal(false);
            sinfo.setName("Test ServiceType Name");
            pgList.add(serviceManager.createServiceType(sinfo, "Test Service Plugin",
                testServerType).getServiceTypeValue());
        }
        PageControl pc = new PageControl();
        
        final PageList<ServiceTypeValue> comparablePGList = serviceManager.getServiceTypesByServerType(subject, testServerType.getId()) ; 

        for(ServiceTypeValue serviceType : comparablePGList) { 
        	if(!pgList.contains(serviceType)) fail("Testbed pagelist does not contain element: " + serviceType) ;
        }//EO while there are more service types 
        
       // assertEquals(comparablePGList,pgList);
    }

    @Test
    public void testGetServiceTypesByInvalidServerType() throws NotFoundException {
        PageList<ServiceTypeValue> pgList = new PageList<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc");
            sinfo.setInternal(false);
            sinfo.setName("Test ServiceType Name");
            pgList.add(serviceManager.createServiceType(sinfo, "Test Service Plugin",
                testServerType).getServiceTypeValue());
        }
        PageControl pc = new PageControl();
        assertTrue(serviceManager.getServiceTypesByServerType(subject, 435878).isEmpty());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findVirtualServiceTypesByPlatform(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    @Test
    public void testFindVirtualServiceTypesByPlatform() throws NotFoundException {
        ServerType vServerType = createServerType("CPU Server", "1.0",
            new String[] { testPlatformType.getName() }, "Test virtual Server Plugin",true);
        PageList<ServiceTypeValue> pgList = new PageList<ServiceTypeValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        for (int i = 1; i <= 5; i++) {
            sinfo.setDescription("Test ServiceType Desc" + i);
            sinfo.setInternal(false);
            sinfo.setName("Test ServiceType Name" + i);
            pgList.add(serviceManager.createServiceType(sinfo, "Test Service Plugin", vServerType)
                .getServiceTypeValue());
        }
        sinfo.setDescription("Test ServiceType Desc6");
        sinfo.setName("Test ServiceType Name6");
        serviceManager.createServiceType(sinfo, "Test Service Plugin", testServerType)
            .getServiceTypeValue();
        PageList<ServiceTypeValue> pgList1 = serviceManager.findVirtualServiceTypesByPlatform(
            subject, testPlatforms.get(0).getId());
        assertEquals(pgList, pgList1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getAllServices(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetAllServices() throws NotFoundException, ApplicationException {
        PageList<ServiceValue> pgList = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            pgList.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageControl pc = new PageControl();
        assertEquals(pgList, serviceManager.getAllServices(subject, pc));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getAllClusterAppUnassignedServices(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO
    public void testGetAllClusterAppUnassignedServices() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByServer(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO
    public void testGetServicesByServer() throws ApplicationException, NotFoundException {
        List<Integer> svalues = new ArrayList<Integer>(5);
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getId());
        }
        List<Integer> svalues1 = (ArrayList<Integer>) serviceManager.getServicesByServer(subject,
            testServers.get(0));
        // TODO:Currently svalues1 has duplicate entries of service Ids HE-780
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByServer(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO:
    public void testGetServiceIdsByServerAndServiceTypeId() throws ApplicationException,
        NotFoundException {
        PageList<ServiceValue> svalues = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageList<ServiceValue> svalues1 = serviceManager.getServicesByServer(subject, testServers
            .get(0).getId(), serviceType.getId(), new PageControl());
        // TODO:Currently svalues1 has duplicate entries of service Ids HE-780
        assertEquals(svalues, svalues1);
        // TODO: Add null service Type Id parameter usecase
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByServer(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.server.session.Server)}
     * .
     */
    // TODO:
    public void testGetServicesByServer1() throws ApplicationException, NotFoundException {
        List<Integer> svalues = new ArrayList<Integer>(5);
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getId());
        }
        List<Integer> svalues1 = (ArrayList<Integer>) serviceManager.getServicesByServer(subject,
            testServers.get(0));
        // TODO:Currently svalues1 has duplicate entries of service Ids HE-780
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceIdsByServer(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer)}
     * .
     */
    @Test
    public void testGetServiceIdsByServer() throws ApplicationException, NotFoundException {
        List<Integer> svalues = new ArrayList<Integer>(5);
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getId());
        }
        Integer[] svalues1 = serviceManager.getServiceIdsByServer(subject, testServers.get(0)
            .getId(), serviceType.getId());
        // TODO:Currently svalues1 has duplicate entries of service Ids HE-780
        assertTrue(Arrays.equals(svalues.toArray(new Integer[5]), svalues1));
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByType(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.String, boolean)}
     * .
     */
    @Test
    public void testGetServicesByType() throws ApplicationException, NotFoundException {
        List<ServiceValue> svalues = new ArrayList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        // Now create another service with different service type so that this
        // will not be fetched
        sinfo.setName("Test AnotherServiceType Name");
        ServiceType anotherServiceType = serviceManager.createServiceType(sinfo,
            "Test AnotherService Plugin", testServerType);
        serviceManager.createService(subject, testServers.get(0).getId(), anotherServiceType
            .getId(), "Test AnotherService Name", "Test AnotherService From Server", "my computer");
        List<ServiceValue> svalues1 = serviceManager.getServicesByType(subject, serviceType
            .getName(), true);
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByService(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO: Looks like parentService is not being used as the createService()
    // method always sets the parentService to null
    public void testGetServicesByServiceAuthzSubjectIntegerPageControl() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByService(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO: Looks like parentService is not being used as the createService()
    // method always sets the parentService to null
    public void testGetServicesByServiceAuthzSubjectIntegerIntegerPageControl() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceIdsByService(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer)}
     * .
     */
    // TODO: Looks like parentService is not being used as the createService()
    // method always sets the parentService to null
    public void testGetServiceIdsByService() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByPlatform(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO
    public void testGetServicesByPlatform() throws ApplicationException, NotFoundException {
        PageList<ServiceValue> svalues = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageControl pc = new PageControl();
        pc.setSortattribute(SortAttribute.SERVICE_NAME);
        PageList<ServiceValue> svalues1 = serviceManager.getServicesByPlatform(subject,
            testPlatforms.get(0).getId(), pc);
        // TODO: Sorting has some issues; reported in HE-783
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getPlatformServices(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetPlatformServices() throws ApplicationException, NotFoundException {
        ServerType vServerType = createServerType("CPU Server", "1.0",
            new String[] { testPlatforms.get(0).getPlatformType().getName() },
            "Test virtual Server Plugin",true);
        ServerValue serverVal = new ServerValue();
        Server virtualServer = serverManager.createServer(subject, testPlatforms.get(0).getId(),
            vServerType.getId(), serverVal);
        PageList<ServiceValue> svalues = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            vServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, virtualServer.getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageList<ServiceValue> svalues1 = serviceManager.getPlatformServices(subject, testPlatforms
            .get(0).getId(), new PageControl());
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getPlatformServices(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetPlatformServicesAddServiceType() throws ApplicationException,
        NotFoundException {
        ServerType vServerType = createServerType("CPU Server", "1.0",
            new String[] { testPlatforms.get(0).getPlatformType().getName() },
            "Test virtual Server Plugin",true);
        ServerValue serverVal = new ServerValue();
        Server virtualServer = serverManager.createServer(subject, testPlatforms.get(0).getId(),
            vServerType.getId(), serverVal);
        PageList<ServiceValue> svalues = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            vServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, virtualServer.getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageList<ServiceValue> svalues1 = serviceManager.getPlatformServices(subject, testPlatforms
            .get(0).getId(), serviceType.getId(), new PageControl());
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findServicesByType(org.hyperic.hq.appdef.server.session.Server, org.hyperic.hq.appdef.server.session.ServiceType)}
     * .
     */
    @Test
    public void testFindServicesByType() throws ApplicationException, NotFoundException {
        List<Service> services = new ArrayList<Service>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            services.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer"));
        }
        PageControl pc = new PageControl();
        List<Service> services1 = serviceManager
            .findServicesByType(testServers.get(0), serviceType);
        assertEquals(services, services1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#findPlatformServicesByType(org.hyperic.hq.appdef.server.session.Platform, org.hyperic.hq.appdef.server.session.ServiceType)}
     * .
     */
    @Test
    public void testFindPlatformServicesByType() throws ApplicationException, NotFoundException {
        List<Service> services = new ArrayList<Service>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            services.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer"));
        }
        List<Service> services1 = serviceManager.findPlatformServicesByType(testPlatforms.get(0),
            serviceType);
        assertEquals(services, services1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getPlatformServices(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    @Test
    public void testGetPlatformServicesByPlatformId() throws ApplicationException,
        NotFoundException {
        ServerType vServerType = createServerType("CPU Server", "1.0",
            new String[] { testPlatforms.get(0).getPlatformType().getName() },
            "Test virtual Server Plugin",true);
        ServerValue serverVal = new ServerValue();
        Server virtualServer = serverManager.createServer(subject, testPlatforms.get(0).getId(),
            vServerType.getId(), serverVal);
        List<Service> svalues = new ArrayList<Service>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            vServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, virtualServer.getId(), serviceType
                .getId(), "Test Service Name" + i, "Test Service From Server" + i, "my computer"));
        }
        List<Service> svalues1 = (List<Service>) serviceManager.getPlatformServices(subject,
            testPlatforms.get(0).getId());
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getMappedPlatformServices(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    @Test
    public void testGetMappedPlatformServices() throws ApplicationException, NotFoundException {
        ServerType vServerType = createServerType("CPU Server", "1.0",
            new String[] { testPlatforms.get(0).getPlatformType().getName() },
            "Test virtual Server Plugin",true);
        ServerType vServerType1 = createServerType("CPU Server1", "1.1",
            new String[] { testPlatforms.get(0).getPlatformType().getName() },
            "Test virtual Server1 Plugin",true);
        ServerValue serverVal = new ServerValue();
        Server virtualServer = serverManager.createServer(subject, testPlatforms.get(0).getId(),
            vServerType.getId(), serverVal);
        List<ServiceValue> svalues = new ArrayList<ServiceValue>(5);
        List<ServiceValue> svalues1 = new ArrayList<ServiceValue>(5);
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            vServerType);
        ServiceType serviceType1 = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            vServerType1);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, virtualServer.getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
            svalues1.add(serviceManager.createService(subject, virtualServer.getId(),
                serviceType1.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        Map<Integer, List<ServiceValue>> mappedServices = new HashMap<Integer, List<ServiceValue>>();
        mappedServices.put(serviceType.getId(), svalues);
        mappedServices.put(serviceType1.getId(), svalues1);
        Map<Integer, List> mappedServices1 = (Map<Integer, List>) serviceManager
            .getMappedPlatformServices(subject, testPlatforms.get(0).getId(), new PageControl());
        for (Map.Entry<Integer, List> entry : mappedServices1.entrySet()) {
            Integer typeId = entry.getKey();
            List svcs = entry.getValue();
            if (typeId.equals(serviceType.getId())) {
                // TODO: Should the list contain ServiceValue?
                assertEquals(mappedServices1.get(serviceType.getId()), svalues);
            } else if (typeId.equals(serviceType1.getId())) {
                assertEquals(mappedServices1.get(serviceType.getId()), svalues);
            }
        }
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByPlatform(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO
    public void testGetServicesByPlatformServiceType() throws ApplicationException,
        NotFoundException {
        PageList<ServiceValue> svalues = new PageList<ServiceValue>();
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 5; i++) {
            svalues.add(serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer").getServiceValue());
        }
        PageControl pc = new PageControl();
        pc.setSortattribute(SortAttribute.SERVICE_NAME);
        PageList<ServiceValue> svalues1 = serviceManager.getServicesByPlatform(subject,
            testPlatforms.get(0).getId(), pc);
        // TODO: Sorting has some issues; reported in HE-783
        assertEquals(svalues, svalues1);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByApplication(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO: HE-781
    public void testGetServicesByApplicationAuthzSubjectIntegerPageControl() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByApplication(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO: HE-781
    public void testGetServicesByApplicationAuthzSubjectIntegerIntegerPageControl() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByApplication(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    // TODO: HE-781
    public void testGetServicesByApplicationAuthzSubjectInteger() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceInventoryByApplication(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO: HE-781
    public void testGetServiceInventoryByApplicationAuthzSubjectIntegerPageControl() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getFlattenedServicesByApplication(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO: HE-781
    public void testGetFlattenedServicesByApplication() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceInventoryByApplication(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer, java.lang.Integer, org.hyperic.util.pager.PageControl)}
     * .
     */
    // TODO: HE-781
    public void testGetServiceInventoryByApplicationAuthzSubjectIntegerIntegerPageControl() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getFlattenedServiceIdsByApplication(org.hyperic.hq.authz.server.session.AuthzSubject, java.lang.Integer)}
     * .
     */
    // TODO: HE-781
    public void testGetFlattenedServiceIdsByApplication() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#updateServiceZombieStatus(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.server.session.Service, boolean)}
     * .
     */
    @Test
    public void testUpdateServiceZombieStatus() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service From Server", "my computer");
        assertFalse(service.isAutodiscoveryZombie());
        serviceManager.updateServiceZombieStatus(subject, service, true);
        assertTrue(service.isAutodiscoveryZombie());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#updateService(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.shared.ServiceValue)}
     * .
     */
    @Test
    public void testUpdateService() throws ApplicationException, NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        Service service = serviceManager.createService(subject, testServers.get(0).getId(),
            serviceType.getId(), "Test Service Name", "Test Service From Server", "my computer");
        ServiceValue sValue = new ServiceValue();
        sValue.setName("Changed Name");
        sValue.setDescription("Changed Description");
        sValue.setLocation("Changed Location");
        sValue.setAutodiscoveryZombie(true);
        sValue.setServiceRt(true);
        sValue.setEndUserRt(true);
        sValue.setId(service.getId());
        serviceManager.updateService(subject, sValue);
        assertEquals(service.getName(), "Changed Name");
        assertEquals(service.getDescription(), "Changed Description");
        assertEquals(service.getServiceType(), serviceType);
        assertEquals(service.getServer(), testServers.get(0));
        assertEquals(service.getLocation(), "Changed Location");
        assertTrue(service.getServiceValue().getServiceRt());
        assertTrue(service.getServiceValue().getEndUserRt());
        assertTrue(service.isAutodiscoveryZombie());
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#updateServiceTypes(java.lang.String, org.hyperic.hq.product.ServiceTypeInfo[])}
     * .
     */
    // TODO
    public void testUpdateServiceTypes() throws NotFoundException, VetoException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServers.get(0).getServerType());
        sinfo.setDescription("Changed Description");
        sinfo.setInternal(true);
        sinfo.setName("Changed Name");
        ServerTypeInfo svrTypeInfo = new ServerTypeInfo(serviceType.getServerType().getName(),
            serviceType.getServerType().getDescription(), "1.0");
        sinfo.setServerTypeInfo(svrTypeInfo);
        // TODO: Deletion of service type gives issue
        serviceManager.updateServiceTypes(serviceType.getPlugin(), new ServiceTypeInfo[] { sinfo });
        assertEquals(serviceType.getName(), "Changed Name");
        assertEquals(serviceType.getDescription(), "Changed Description");
        assertEquals(serviceType.getPlugin(), "Test Service Plugin");
        assertEquals(serviceType.getServerType(), testServerType);
        assertEquals(serviceType.isIsInternal(), true);
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#deleteServiceType(org.hyperic.hq.appdef.server.session.ServiceType, org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.authz.shared.ResourceGroupManager, org.hyperic.hq.authz.shared.ResourceManager)}
     * .
     */
    // TODO
    public void testDeleteServiceType() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceCluster(org.hyperic.hq.authz.server.session.ResourceGroup)}
     * .
     */
    // TODO
    public void testGetServiceCluster() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#removeService(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.server.session.Service)}
     * .
     */
    // TODO
    public void testRemoveService() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceTypeCounts()}
     * .
     */
    @Test
    public void testGetServiceTypeCounts() throws NotFoundException, ApplicationException {
        List<Object[]> actuals = new ArrayList<Object[]>(10);
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        ServiceType serviceType;
        for (int i = 1; i <= 9; i++) {
            sinfo.setDescription("Test ServiceType Desc" + i);
            sinfo.setInternal(false);
            sinfo.setName("Test ServiceType Name" + i);
            serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin" + i,
                testServerType);
            // Create Services as well here as the query uses join from Service
            serviceManager.createService(subject, testServers.get(0).getId(), serviceType.getId(),
                "Test Service Name" + i, "Test Service From Server" + i, "my computer");
            actuals.add(i - 1, new Object[] { "Test ServiceType Name" + i, Long.valueOf("1") });
        }

        List<Object[]> counts = serviceManager.getServiceTypeCounts();
        for (int i = 0; i < 9; i++) {
            assertEquals((String) counts.get(i)[0], ((String) actuals.get(i)[0]));
            assertEquals((Long) counts.get(i)[1], ((Long) actuals.get(i)[1]));
        }
    }

    /**
     * Test method for
     * {@link org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceCount()}
     * .
     */
    @Test
    public void testGetServiceCount() throws NotFoundException, ApplicationException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription("Test ServiceType Desc");
        sinfo.setInternal(false);
        sinfo.setName("Test ServiceType Name");
        ServiceType serviceType = serviceManager.createServiceType(sinfo, "Test Service Plugin",
            testServerType);
        for (int i = 1; i <= 10; i++) {
            serviceManager.createService(subject, testServers.get(0).getId(),
                serviceType.getId(), "Test Service Name" + i, "Test Service From Server" + i,
                "my computer");
        }
        assertEquals(serviceManager.getServiceCount().intValue(), 10);
    }

}
