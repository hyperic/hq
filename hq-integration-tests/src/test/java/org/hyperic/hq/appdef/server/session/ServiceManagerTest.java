/**
 * 
 */
package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author iperumal
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = IntegrationTestContextLoader.class, locations = { "classpath*:META-INF/spring/*-context.xml" })
@Transactional
@DirtiesContext
public class ServiceManagerTest {

    @Autowired
    PlatformManager platformManager;

    @Autowired
    AuthzSubjectManager authzSubjectManager;

    @Autowired
    AgentManager agentManager;

    @Autowired
    ServerManager serverManager;

    @Autowired
    ServiceManager serviceManager;

    @Autowired
    ResourceGroupManager resourceGroupManager;

    @Autowired
    ResourceManager resourceManager;

    @Autowired
    ApplicationManager applicationManager;

    private Agent testAgent;

    private List<PlatformType> testPlatformTypes;

    private PlatformType testPlatformType;

    private List<Platform> testPlatforms;

    private Platform testPlatform;

    private List<Server> testServers;

    private ServerType testServerType;

    private Service testService;

    private ResourceGroup testGroup;

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
            platforms.add(i - 1, platformManager.createPlatform(authzSubjectManager
                .getOverlordPojo(), aiPlatform));
        }
        return platforms;
    }

    private Platform createPlatform(String agentToken, String platformName, String platformTypeName)
        throws ApplicationException {
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setName(platformName);
        aiPlatform.setCpuCount(2);
        aiPlatform.setPlatformTypeName(platformTypeName);
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn(platformName);
        return platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
    }

    private List<PlatformType> createTestPlatformTypes() throws NotFoundException {
        List<PlatformType> pTypes = new ArrayList<PlatformType>(10);
        String platformType;
        for (int i = 1; i < 10; i++) {
            platformType = "pType" + i;
            pTypes.add(i - 1, platformManager.createPlatformType(platformType, "Test Plugin" + i));
        }
        pTypes.add(9, platformManager.createPlatformType("Linux", "Test Plugin"));
        return pTypes;
    }

    private List<Server> createServers(List<Platform> platforms, ServerType serverType)
        throws PlatformNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException, NotFoundException {
        List<Server> servers = new ArrayList<Server>(2);
        ServerValue server = new ServerValue();
        for (Platform platform : platforms) {
            servers.add(serverManager.createServer(subject, platform.getId(), serverType.getId(),
                server));
        }
        return servers;
    }

    private ServerType createServerType(String serverName, String serverVersion,
                                        String[] validPlatformTypes, String plugin)
        throws NotFoundException {
        ServerTypeInfo serverTypeInfo = new ServerTypeInfo();
        serverTypeInfo.setDescription(serverName);
        serverTypeInfo.setName(serverName);
        serverTypeInfo.setVersion(serverVersion);
        serverTypeInfo.setVirtual(false);
        serverTypeInfo.setValidPlatformTypes(validPlatformTypes);
        return serverManager.createServerType(serverTypeInfo, plugin);
    }

    private ServerType createVirtualServerType(String serverName, String serverVersion,
                                               String[] validPlatformTypes, String plugin)
        throws NotFoundException {
        ServerTypeInfo serverTypeInfo = new ServerTypeInfo();
        serverTypeInfo.setDescription(serverName);
        serverTypeInfo.setName(serverName);
        serverTypeInfo.setVersion(serverVersion);
        serverTypeInfo.setVirtual(true);
        serverTypeInfo.setValidPlatformTypes(validPlatformTypes);
        return serverManager.createServerType(serverTypeInfo, plugin);
    }

    private ServiceType createServiceType(String serviceTypeName, String plugin,
                                          ServerType serverType) throws NotFoundException {
        ServiceTypeInfo sinfo = new ServiceTypeInfo();
        sinfo.setDescription(serviceTypeName);
        sinfo.setInternal(false);
        sinfo.setName(serviceTypeName);
        return serviceManager.createServiceType(sinfo, plugin, serverType);
    }

    private ResourceGroup createResourceGroup(Platform testPlatform) throws ApplicationException,
        PermissionException {
        AuthzSubject overlord = subject;
        Resource platformRes = testPlatform.getResource();

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(platformRes);

        AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, testPlatform.getResource()
                .getResourceType().getAppdefType());

        ResourceGroupCreateInfo gCInfo = new ResourceGroupCreateInfo("AllPlatformGroup", "",
            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP, resourceManager
                .findResourcePrototype(appDefEntTypeId), "", 0, false, false);
        ResourceGroup resGrp = resourceGroupManager.createResourceGroup(overlord, gCInfo,
            new ArrayList<Role>(0), resources);
        return resGrp;
    }

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        subject = authzSubjectManager.getOverlordPojo();
        String agentToken = "agentToken123";
        testAgent = agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken,
            "4.5");
        testPlatformType = platformManager.createPlatformType("Linux", "Test Plugin");
        testPlatforms = createPlatforms(agentToken);
        // Create ServerType
        testServerType = createServerType("Test Server", "6.0", new String[] { "Linux" },
            "Test Server Plugin");
        // Create test server
        testServers = createServers(testPlatforms, testServerType);
        testGroup = createResourceGroup(testPlatforms.get(0));
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
        assertEquals(serviceManager.getServiceTypesByServerType(subject, testServerType.getId()),
            pgList);
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
        ServerType vServerType = createVirtualServerType("CPU Server", "1.0",
            new String[] { testPlatformType.getName() }, "Test virtual Server Plugin");
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
            testPlatforms.get(0).getId(), serviceType.getId(), pc);
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
        ServerType vServerType = createVirtualServerType("CPU Server", "1.0",
            new String[] { testPlatforms.get(0).getPlatformType().getName() },
            "Test virtual Server Plugin");
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
        ServerType vServerType = createVirtualServerType("CPU Server", "1.0",
            new String[] { testPlatforms.get(0).getPlatformType().getName() },
            "Test virtual Server Plugin");
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
        ServerType vServerType = createVirtualServerType("CPU Server", "1.0",
            new String[] { testPlatforms.get(0).getPlatformType().getName() },
            "Test virtual Server Plugin");
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

    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getMappedPlatformServices(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer, org.hyperic.util.pager.PageControl)}
    // * .
    // */
    // @Test
    // public void testGetMappedPlatformServices() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByPlatform(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer, java.lang.Integer,
    // org.hyperic.util.pager.PageControl)}
    // * .
    // */
    // @Test
    // public void
    // testGetServicesByPlatformAuthzSubjectIntegerIntegerPageControl() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByApplication(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer, org.hyperic.util.pager.PageControl)}
    // * .
    // */
    // @Test
    // public void testGetServicesByApplicationAuthzSubjectIntegerPageControl()
    // {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByApplication(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer, java.lang.Integer,
    // org.hyperic.util.pager.PageControl)}
    // * .
    // */
    // @Test
    // public void
    // testGetServicesByApplicationAuthzSubjectIntegerIntegerPageControl() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServicesByApplication(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer)}
    // * .
    // */
    // @Test
    // public void testGetServicesByApplicationAuthzSubjectInteger() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceInventoryByApplication(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer, org.hyperic.util.pager.PageControl)}
    // * .
    // */
    // @Test
    // public void
    // testGetServiceInventoryByApplicationAuthzSubjectIntegerPageControl() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getFlattenedServicesByApplication(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer, java.lang.Integer,
    // org.hyperic.util.pager.PageControl)}
    // * .
    // */
    // @Test
    // public void testGetFlattenedServicesByApplication() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceInventoryByApplication(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer, java.lang.Integer,
    // org.hyperic.util.pager.PageControl)}
    // * .
    // */
    // @Test
    // public void
    // testGetServiceInventoryByApplicationAuthzSubjectIntegerIntegerPageControl()
    // {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getFlattenedServiceIdsByApplication(org.hyperic.hq.authz.server.session.AuthzSubject,
    // java.lang.Integer)}
    // * .
    // */
    // @Test
    // public void testGetFlattenedServiceIdsByApplication() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#updateServiceZombieStatus(org.hyperic.hq.authz.server.session.AuthzSubject,
    // org.hyperic.hq.appdef.server.session.Service, boolean)}
    // * .
    // */
    // @Test
    // public void testUpdateServiceZombieStatus() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#updateService(org.hyperic.hq.authz.server.session.AuthzSubject,
    // org.hyperic.hq.appdef.shared.ServiceValue)}
    // * .
    // */
    // @Test
    // public void testUpdateService() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#updateServiceTypes(java.lang.String,
    // org.hyperic.hq.product.ServiceTypeInfo[])}
    // * .
    // */
    // @Test
    // public void testUpdateServiceTypes() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#deleteServiceType(org.hyperic.hq.appdef.server.session.ServiceType,
    // org.hyperic.hq.authz.server.session.AuthzSubject,
    // org.hyperic.hq.authz.shared.ResourceGroupManager,
    // org.hyperic.hq.authz.shared.ResourceManager)}
    // * .
    // */
    // @Test
    // public void testDeleteServiceType() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceCluster(org.hyperic.hq.authz.server.session.ResourceGroup)}
    // * .
    // */
    // @Test
    // public void testGetServiceCluster() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#removeService(org.hyperic.hq.authz.server.session.AuthzSubject,
    // org.hyperic.hq.appdef.server.session.Service)}
    // * .
    // */
    // @Test
    // public void testRemoveService() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getOperationByName(org.hyperic.hq.authz.server.session.ResourceType,
    // java.lang.String)}
    // * .
    // */
    // @Test
    // public void testGetOperationByName() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#handleResourceDelete(org.hyperic.hq.authz.server.session.Resource)}
    // * .
    // */
    // @Test
    // public void testHandleResourceDelete() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceTypeCounts()}
    // * .
    // */
    // @Test
    // public void testGetServiceTypeCounts() {
    // fail("Not yet implemented");
    // }
    //
    // /**
    // * Test method for
    // * {@link
    // org.hyperic.hq.appdef.server.session.ServiceManagerImpl#getServiceCount()}
    // * .
    // */
    // @Test
    // public void testGetServiceCount() {
    // fail("Not yet implemented");
    // }

}
