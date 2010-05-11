package org.hyperic.hq.appdef.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.NonUniqueObjectException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.TestContextLoader;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of the {@link PlatformManagerImpl}
 * @author iperumal
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = TestContextLoader.class, locations = { "classpath*:META-INF/spring/*-context.xml" })
@Transactional
@DirtiesContext
public class PlatformManagerTest {

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

    private Agent testAgent;

    private List<PlatformType> testPlatformTypes;

    private PlatformType testPlatformType;

    private List<Platform> testPlatforms;

    private Platform testPlatform;

    private Server testServer;

    private Service testService;

    private ResourceGroup testGroup;

    private List<Platform> createPlatforms(String agentToken) throws ApplicationException {
        List<Platform> platforms = new ArrayList<Platform>(10);
        for (int i = 1; i < 10; i++) {
            AIPlatformValue aiPlatform = new AIPlatformValue();
            aiPlatform.setName("TestPlatform" + i);
            aiPlatform.setCpuCount(2);
            aiPlatform.setPlatformTypeName("pType" + i);
            aiPlatform.setAgentToken(agentToken);
            aiPlatform.setFqdn("Test Platform" + i);
            platforms.add(i - 1, platformManager.createPlatform(authzSubjectManager
                .getOverlordPojo(), aiPlatform));
        }
        // Create on Linux platform (supported platform)
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setName("TestLinux Platform");
        aiPlatform.setCpuCount(2);
        aiPlatform.setPlatformTypeName("Linux");
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn("Test Platform Linux");
        platforms.add(9, platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            aiPlatform));
        return platforms;
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

    private Server createServer(Platform platform, ServerType serverType)
        throws PlatformNotFoundException, AppdefDuplicateNameException, ValidationException,
        PermissionException, NotFoundException {
        ServerValue server = new ServerValue();
        return serverManager.createServer(authzSubjectManager.getOverlordPojo(), platform.getId(),
            serverType.getId(), server);
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
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
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
        String agentToken = "agentToken123";
        testAgent = agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken,
            "4.5");
        testPlatformTypes = createTestPlatformTypes();
        // Get Linux platform type
        testPlatformType = testPlatformTypes.get(9);
        testPlatforms = createPlatforms(agentToken);
        // Get Linux platform
        testPlatform = testPlatforms.get(9);
        // Create ServerType
        ServerType testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" },
            "Test Server Plugin");
        // Create test server
        testServer = createServer(testPlatform, testServerType);
        // Create ServiceType
        ServiceType serviceType = createServiceType("Spring JDBC Template", "Test Server Plugin",
            testServerType);
        // Create test service
        testService = serviceManager.createService(authzSubjectManager.getOverlordPojo(),
            testServer.getId(), serviceType.getId(), "platformService jdbcTemplate",
            "Spring JDBC Template", "my computer");
        testGroup = createResourceGroup(testPlatform);
    }

    @Test
    public void testFindPlatformType() {
        PlatformType pType = platformManager.findPlatformType(testPlatformTypes.get(0).getId());
        assertEquals("Incorrect PlatformType Found ById", pType, testPlatformTypes.get(0));
    }

    @Test
    public void testFindPlatformTypeByName() throws PlatformNotFoundException {
        PlatformType pType = platformManager.findPlatformTypeByName("pType1");
        assertEquals("Incorrect PlatformType found ByName", pType, testPlatformTypes.get(0));
    }

    @Test(expected = PlatformNotFoundException.class)
    public void testFindPlatformTypeByNameNotFound() throws PlatformNotFoundException {
        platformManager.findPlatformTypeByName("Test");
    }

    @Test
    public void testFindAllPlatformTypes() {
        List<PlatformType> allPTypes = (List<PlatformType>) platformManager.findAllPlatformTypes();
        assertEquals("Not all platform types fetched", allPTypes, testPlatformTypes);
    }

    @Test
    public void testFindSupportedPlatformTypes() {
        List<PlatformType> supported = (List<PlatformType>) platformManager
            .findSupportedPlatformTypes();
        assertEquals("Support platform doesn't exist", supported.get(0), testPlatformType);
    }

    @Test
    public void testFindUnsupportedPlatformTypes() {
        List<PlatformType> unSupported = (List<PlatformType>) platformManager
            .findUnsupportedPlatformTypes();
        for (int i = 0; i < 9; i++) {
            assertEquals("Unsupported platform doesn't exist", unSupported.get(i),
                testPlatformTypes.get(i));
        }
    }

    @Test
    public void testFindResource() {
        // Get supported platform
        PlatformType pType = testPlatformType;
        // Find the resource
        Resource res = platformManager.findResource(pType);
        assertEquals("Resource name incorrect", res.getName(), pType.getName());
    }

    public void testGetAllPlatformTypes() {
        fail("Not yet implemented");
    }

    public void testGetViewablePlatformTypes() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPlatformPluginNameForPlatform() throws AppdefEntityNotFoundException {
        String platformPluginName = platformManager.getPlatformPluginName(testPlatform
            .getEntityId());
        assertEquals("PlatformPluginName incorrect for platform", "Linux", platformPluginName);
    }

    @Test
    public void testGetPlatformPluginNameForServer() throws AppdefEntityNotFoundException {
        String ppNameOfServer = platformManager.getPlatformPluginName(testServer.getEntityId());
        assertEquals("PlatformPluginName incorrect for server", "Tomcat Linux", ppNameOfServer);
    }

    @Test
    public void testGetPlatformPluginNameForService() throws AppdefEntityNotFoundException {
        String ppNameOfService = platformManager.getPlatformPluginName(testService.getEntityId());
        assertEquals("PlatformPluginName incorrect for service", "Spring JDBC Template Linux",
            ppNameOfService);
    }

    /*
     * @Test public void testGetPlatformPluginNameForGroup() throws
     * AppdefEntityNotFoundException { String ppNameOfGroup =
     * platformManager.getPlatformPluginName(AppdefEntityID
     * .newGroupID(testGroup.getId()));
     * assertEquals("PlatformPluginName incorrect for service",
     * "Spring JDBC Template Linux", ppNameOfGroup); }
     */

    /*
     * @Test public void testRemovePlatform() throws PermissionException,
     * VetoException, PlatformNotFoundException {
     * platformManager.removePlatform(authzSubjectManager.getOverlordPojo(),
     * testPlatform);
     * assertNull(platformManager.findPlatformById(testPlatform.getId())); }
     */

    /*
     * public void testHandleResourceDelete() { fail("Not yet implemented"); }
     */

    @Test
    public void testCreatePlatformByAIPlatformValues() throws ApplicationException {
        String agentToken = "agentToken123";
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setName("Test PlatformByAIValues");
        aiPlatform.setCpuCount(2);
        aiPlatform.setPlatformTypeName(testPlatformType.getName());
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn("Test Platform CreationByAIValues");
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            aiPlatform);
        assertNotNull(platform.getResource());
        assertEquals(platform.getName(), "Test PlatformByAIValues");
        assertEquals(platform.getAgent().getAgentToken(), agentToken);
        assertEquals(platform.getCpuCount(), new Integer(2));
        assertEquals(platform.getPlatformType().getName(), testPlatformType.getName());
        assertEquals(platform.getFqdn(), "Test Platform CreationByAIValues");
    }

    @Test
    public void testCreatePlatformIncorrectPlatformType() throws ApplicationException {
        String agentToken = "agentToken123";
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setName("Test Platform");
        aiPlatform.setCpuCount(2);
        // Provide a non-existent platform type
        aiPlatform.setPlatformTypeName("abcd");
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn("Test Platform Creation");
        try {
            platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
        } catch (SystemException e) {
            assertEquals(e.getMessage(), "Unable to find PlatformType [" +
                                         aiPlatform.getPlatformTypeName() + "]");
            return;
        }
        fail("Expected SystemException is not thrown");
    }

    @Test(expected = NonUniqueObjectException.class)
    public void testCreatePlatformDuplicate() throws ApplicationException {
        String agentToken = "agentToken123";
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setCpuCount(2);
        aiPlatform.setName("Test Platform");
        aiPlatform.setPlatformTypeName(testPlatformType.getName());
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn("Test Platform Creation");
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
    }

    @Test
    public void testCreatePlatformIncorrectAgentToken() throws ApplicationException {
        // Add an invalid agent token
        String agentToken = "agentToken";
        AIPlatformValue aiPlatform = new AIPlatformValue();
        aiPlatform.setCpuCount(2);
        aiPlatform.setPlatformTypeName(testPlatformType.getName());
        aiPlatform.setAgentToken(agentToken);
        aiPlatform.setFqdn("Test Platform Creation");
        try {
            platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
        } catch (ApplicationException e) {
            assertEquals(e.getMessage(), "Unable to find agent: " + aiPlatform.getAgentToken());
            return;
        }
        fail("Expected ApplicationException for invalid agent token is not thrown");
    }

    @Test
    public void testCreatePlatformByPlatformType() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pValue, testAgent.getId());
        assertNotNull(platform.getResource());
        assertEquals(platform.getName(), "Test Platform ByPlatformType");
        assertEquals(platform.getFqdn(), "Test Platform CreationByPlatformType");
        assertEquals(platform.getCpuCount(), new Integer(2));
    }

    @Test(expected = AppdefDuplicateNameException.class)
    public void testCreatePlatformDuplicateName() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), testPlatformType
            .getId(), pValue, testAgent.getId());
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), testPlatformType
            .getId(), pValue, testAgent.getId());
    }

    @Test(expected = AppdefDuplicateFQDNException.class)
    public void testCreatePlatformDuplicateFQDN() throws ApplicationException {
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), testPlatformType
            .getId(), pValue, testAgent.getId());
        pValue.setName("Test Platform ByPlatformType1");
        platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), testPlatformType
            .getId(), pValue, testAgent.getId());
    }

    @Test
    public void testCreatePlatformAddVirtualServers() throws ApplicationException,
        NotFoundException {
        // Create virtual server types for test platform type
        ServerType vServerType = createVirtualServerType("CPU Server", "1.0",
            new String[] { testPlatformType.getName() }, "Test virtual Server Plugin");
        PlatformValue pValue = new PlatformValue();
        pValue.setCpuCount(2);
        pValue.setName("Test Platform ByPlatformType");
        pValue.setFqdn("Test Platform CreationByPlatformType");
        Platform platform = platformManager.createPlatform(authzSubjectManager.getOverlordPojo(),
            testPlatformType.getId(), pValue, testAgent.getId());
        // Check if the virtual server is fetched with the expected virtual
        // server name
        Server server = serverManager.getServerByName(platform, pValue.getName() + " " +
                                                                vServerType.getName());
        assertNotNull(server);
        assertNotNull(platform.getResource());
        assertEquals(platform.getName(), "Test Platform ByPlatformType");
        assertEquals(platform.getFqdn(), "Test Platform CreationByPlatformType");
        assertEquals(platform.getCpuCount(), new Integer(2));
    }

    @Test
    public void testGetAllPlatforms() throws ApplicationException, NotFoundException {
        PageList<PlatformValue> pValues = platformManager.getAllPlatforms(authzSubjectManager
            .getOverlordPojo(), null);
        assertEquals(testPlatforms.size(), pValues.size());
    }

    @Test
    public void testGetRecentPlatforms() throws ApplicationException, NotFoundException {
        long setTime = System.currentTimeMillis();
        int i = 1;
        for (Platform p : testPlatforms) {
            // Set Platforms creation time 20 minutes before the current time.
            p.setCreationTime(setTime - 20 * 60000l);
            i++;
        }
        // Change two of the platform's creation time to recent
        testPlatforms.get(0).setCreationTime(setTime);
        testPlatforms.get(1).setCreationTime(setTime - 2 * 60000l);
        testPlatforms.get(2).setCreationTime(setTime - 3 * 60000l);
        PageList<PlatformValue> pValues = platformManager.getRecentPlatforms(authzSubjectManager
            .getOverlordPojo(), 5 * 60000l, 10);
        assertEquals(3, pValues.size());
    }

    @Test
    public void testGetPlatformById() throws ApplicationException, NotFoundException {
        Platform platform = platformManager.getPlatformById(authzSubjectManager.getOverlordPojo(),
            testPlatform.getId());
        assertEquals("Correct Platform is not fetched", platform, testPlatform);
    }

    @Test(expected = PlatformNotFoundException.class)
    public void testGetPlatformByInvalidId() throws ApplicationException, NotFoundException {
        platformManager.getPlatformById(authzSubjectManager.getOverlordPojo(), 12345);
    }

    @Test
    public void testFindPlatformById() throws ApplicationException {
        Platform platform = platformManager.findPlatformById(testPlatform.getId());
        assertEquals("Correct Platform is not found by Id", platform, testPlatform);
    }

    @Test(expected = PlatformNotFoundException.class)
    public void testFindPlatformByInvalidId() throws ApplicationException {
        platformManager.findPlatformById(12345);
    }

    public void testGetPlatformByAIPlatform() {
        fail("Not yet implemented");
    }

    public void testGetPhysPlatformByAgentToken() {
        fail("Not yet implemented");
    }

    public void testGetPlatformByNameAuthzSubjectString() {
        fail("Not yet implemented");
    }

    public void testGetPlatformByNameString() {
        fail("Not yet implemented");
    }

    public void testFindPlatformByFqdn() {
        fail("Not yet implemented");
    }

    public void testGetPlatformByIpAddr() {
        fail("Not yet implemented");
    }

    public void testGetPlatformPksByAgentToken() {
        fail("Not yet implemented");
    }

    public void testGetPlatformByService() {
        fail("Not yet implemented");
    }

    public void testGetPlatformIdByService() {
        fail("Not yet implemented");
    }

    public void testGetPlatformByServer() {
        fail("Not yet implemented");
    }

    public void testGetPlatformIdByServer() {
        fail("Not yet implemented");
    }

    public void testGetPlatformsByServers() {
        fail("Not yet implemented");
    }

    public void testGetPlatformsByApplication() {
        fail("Not yet implemented");
    }

    public void testGetViewablePlatformPKs() {
        fail("Not yet implemented");
    }

    public void testGetOperationByName() {
        fail("Not yet implemented");
    }

    public void testGetPlatformIds() {
        fail("Not yet implemented");
    }

    public void testGetPlatformsByType() {
        fail("Not yet implemented");
    }

    public void testGetViewablePlatforms() {
        fail("Not yet implemented");
    }

    public void testFindPlatformsByIpAddr() {
        fail("Not yet implemented");
    }

    public void testFindPlatformPojosByTypeAndName() {
        fail("Not yet implemented");
    }

    public void testFindParentPlatformPojosByNetworkRelation() {
        fail("Not yet implemented");
    }

    public void testFindPlatformPojosByNoNetworkRelation() {
        fail("Not yet implemented");
    }

    public void testFindPlatformPojosByIpAddr() {
        fail("Not yet implemented");
    }

    public void testFindDeletedPlatforms() {
        fail("Not yet implemented");
    }

    public void testUpdatePlatformImpl() {
        fail("Not yet implemented");
    }

    public void testUpdatePlatform() {
        fail("Not yet implemented");
    }

    public void testDeletePlatformType() {
        fail("Not yet implemented");
    }

    public void testUpdatePlatformTypes() {
        fail("Not yet implemented");
    }

    public void testCreatePlatformType() {
        fail("Not yet implemented");
    }

    public void testUpdateWithAI() {
        fail("Not yet implemented");
    }

    public void testAddIp() {
        fail("Not yet implemented");
    }

    public void testUpdateIp() {
        fail("Not yet implemented");
    }

    public void testRemoveIp() {
        fail("Not yet implemented");
    }

    public void testGetPlatformTypeCounts() {
        fail("Not yet implemented");
    }

    public void testGetPlatformCount() {
        fail("Not yet implemented");
    }

    public void testGetCpuCount() {
        fail("Not yet implemented");
    }

    public void testAfterPropertiesSet() {
        fail("Not yet implemented");
    }

}
